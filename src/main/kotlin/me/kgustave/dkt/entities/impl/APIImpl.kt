/*
 * Copyright 2018 Kaidan Gustave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("MemberVisibilityCanBePrivate")

package me.kgustave.dkt.entities.impl

import kotlinx.coroutines.experimental.*
import me.kgustave.dkt.API
import me.kgustave.dkt.APIConfig
import me.kgustave.dkt.entities.*
import me.kgustave.dkt.entities.caching.impl.SnowflakeCacheImpl
import me.kgustave.dkt.events.Event
import me.kgustave.dkt.exceptions.RateLimitedException
import me.kgustave.dkt.exceptions.UnloadedPropertyException
import me.kgustave.dkt.handlers.EventCache
import me.kgustave.dkt.handlers.SessionManager
import me.kgustave.dkt.handlers.shard.ShardController
import me.kgustave.dkt.handlers.shard.impl.ShardControllerImpl
import me.kgustave.dkt.hooks.EventDispatcher
import me.kgustave.dkt.requests.DiscordWebSocket
import me.kgustave.dkt.requests.Requester
import me.kgustave.dkt.requests.RestPromise
import me.kgustave.dkt.requests.Route
import me.kgustave.dkt.requests.promises.PreCompletedPromise
import me.kgustave.dkt.util.createLogger
import me.kgustave.dkt.util.ignored
import me.kgustave.dkt.util.queue.GuildEventQueue
import me.kgustave.dkt.util.unsupported
import me.kgustave.json.JSObject
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import javax.security.auth.login.LoginException
import kotlin.concurrent.thread
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * @author Kaidan Gustave
 */
class APIImpl
@Throws(LoginException::class)
constructor(config: APIConfig, internal val internalShardController: ShardControllerImpl? = null): API {
    companion object {
        const val ASYNC_CONTEXT_NAME = "Discord API Async-Context"

        val LOG = createLogger(API::class)
    }

    private lateinit var readyContinuation: Continuation<Unit>

    override var responses = 0L
    override var ping = 0L

    @Volatile
    override var status = API.Status.INITIALIZING
        internal set(value) {
            synchronized(field) {
                // TODO Add an event handle here
                field = value
            }
        }

    override var shardInfo: API.ShardInfo? = null
        private set

    override var shouldAutoReconnect = config.shouldAutoReconnect
        set(value) {
            field = value
            websocket.shouldReconnect = value
        }

    override var presence: Presence = PresenceImpl(this, config.onlineStatus, config.activity, config.afk)
        set(value) = synchronized(field) {
            val impl = requireNotNull(value as? PresenceImpl) {
                "Presence must be a valid instance of PresenceImpl"
            }

            // TODO Add an event handle here
            field = impl
            impl.update()
        }

    override val token = config.token

    override val userCache = SnowflakeCacheImpl(User::name)
    override val guildCache = SnowflakeCacheImpl(Guild::name)
    override val textChannelCache = SnowflakeCacheImpl(TextChannel::name)
    override val voiceChannelCache = SnowflakeCacheImpl(VoiceChannel::name)
    override val categoryCache = SnowflakeCacheImpl(Category::name)
    override val privateChannelCache = SnowflakeCacheImpl<PrivateChannel>(null)

    // TODO Make this optional
    private val shutdownThread: Thread? = thread(false, name = "Kotlincord Shutdown Thread") { shutdown() }
    private val eventDispatcher: EventDispatcher = config.eventDispatcher
    private val apiAsyncContext: CoroutineContext = config.apiAsyncContext ?: newSingleThreadContext(ASYNC_CONTEXT_NAME)

    // If the self-user is attached to the shard-controller
    // we should take that one immediately and use it.
    @Volatile internal var internalSelf: SelfUser? = internalShardController?.takeIf { it.selfUserIsInit }?.self
        get() {
            return internalShardController?.run {
                if(selfUserIsInit) self else field?.also { self = it }
            } ?: field
        }
        set(value) {
            // Never set this field twice
            if(field !== null) return

            // Make sure this is not null
            val self = requireNotNull(value) { "Cannot set SelfUser to null!" }

            // Have not initialized self-user for the shard-controller yet
            internalShardController?.run { if(!selfUserIsInit) this.self = self }

            // Set field
            field = self
        }

    internal lateinit var websocket: DiscordWebSocket private set
    internal lateinit var gatewayUrl: String private set
    internal val guildQueue = GuildEventQueue(this)
    internal val eventCache = EventCache()
    internal val httpClientBuilder = config.httpClientBuilder
    internal val requester = Requester(this)
    internal val entityBuilder = EntityBuilder(this)
    internal val context = ScheduledThreadPoolExecutor(config.corePoolSize, APIThreadFactory()).asCoroutineDispatcher()
    internal val identifier = "Discord Bot${if(isSharded) " ($shardInfo)" else ""}"

    init {
        // TODO ShardController setup
        config.listeners.forEach { eventDispatcher.addListener(it) }
    }

    override val self: SelfUser get() {
        return internalSelf ?: throw UnloadedPropertyException("API has not loaded SelfUser yet!")
    }

    override val shardController: ShardController get() {
        return internalShardController ?: unsupported { "Cannot get ShardController for a non-sharded bot instance!" }
    }

    override val users: List<User> get() = userCache.toList()
    override val guilds: List<Guild> get() = guildCache.toList()
    override val textChannels: List<TextChannel> get() = textChannelCache.toList()
    override val voiceChannels: List<VoiceChannel> get() = voiceChannelCache.toList()
    override val categories: List<Category> get() = categoryCache.toList()
    override val privateChannels: List<PrivateChannel> get() = privateChannelCache.toList()

    override fun getUserById(id: Long): User? = userMap[id]
    override fun getTextChannelById(id: Long): TextChannel? = textChannelMap[id]

    internal val userMap get() = userCache.entityMap
    internal val guildMap get() = guildCache.entityMap
    internal val textChannelMap get() = textChannelCache.entityMap
    internal val voiceChannelMap get() = voiceChannelCache.entityMap
    internal val categoryMap get() = categoryCache.entityMap
    internal val privateChannelMap get() = privateChannelCache.entityMap

    // TODO Add SessionManager configurability
    internal suspend fun login(shardInfo: API.ShardInfo?, sessionManager: SessionManager) {
        require(token.isNotBlank()) { "Token provided was empty!" } // TODO Token Verification

        // Get gateway information
        val gatewayInfo = requestGatewayBot().await()

        this.shardInfo = shardInfo
        this.gatewayUrl = gatewayInfo.first

        // Attach the shutdown hook
        shutdownThread?.let {
            LOG.debug("Attaching shutdown hook")
            Runtime.getRuntime().addShutdownHook(it)
        }

        // We are now logging in
        this.status = API.Status.LOGGING_IN

        // This suspends the login call until signalReady is fired.
        suspendCoroutine<Unit> { cont ->
            LOG.debug("Initializing WebSocket${shardInfo?.let { " for $shardInfo" } ?: ""}...")
            this.readyContinuation = cont
            try {
                this.websocket = DiscordWebSocket(this, sessionManager)
            } catch(t: Throwable) {
                readyContinuation.resumeWithException(t)
            }
        }
    }

    internal fun signalReady(error: Throwable? = null) {
        return error?.let { readyContinuation.resumeWithException(it) } ?: readyContinuation.resume(Unit)
    }

    internal fun dispatchEvent(event: Event) {
        LOG.trace("Firing ${event::class} (Response ${event.responseNumber})")
        eventDispatcher.onEvent(event)
    }

    override fun findUserById(id: Long): RestPromise<User> {
        userCache[id]?.let { return PreCompletedPromise(this@APIImpl, it) }

        return RestPromise.simple(this, Route.GetUser.format(id)) { res, req ->
            if(!res.isOk) {
                req.error(res)
            } else {
                req.succeed(entityBuilder.createUser(res.obj as JSObject, shouldCache = false))
            }
        }
    }

    override fun async(block: suspend CoroutineScope.() -> Unit) {
        async(apiAsyncContext, block = block)
    }

    override fun modifyPresence(block: Presence.() -> Unit) {
        (presence as? PresenceImpl)?.updateInBulk(block)
    }

    override fun shutdown() {
        // We are already shutting down, return
        if(status == API.Status.SHUTDOWN || status == API.Status.SHUTTING_DOWN)
            return

        // We are now shutting down
        this.status = API.Status.SHUTTING_DOWN

        // Make sure it's initialized, this shouldn't
        // ever happen, because the conditions that would
        // allow it would mean the laws of threading
        // had to be reinvented or some bullshit, but
        // just in case...
        if(::websocket.isInitialized)
            websocket.shutdown()
        requester.shutdown(5, TimeUnit.SECONDS)
        context.cancel()

        if(apiAsyncContext is AutoCloseable) {
            apiAsyncContext.close()
        }

        // Remove the shutdown hook
        shutdownThread?.let { ignored { Runtime.getRuntime().removeShutdownHook(it) } }

        // We are now done shutting down
        this.status = API.Status.SHUTDOWN
    }

    override fun toString(): String {
        shardInfo?.apply {
            return "API Connection(url=\"$gatewayUrl\", shard_id=$shardId, shard_total=$shardTotal)"
        }

        return "API Connection(url=\"$gatewayUrl\")"
    }

    private fun requestGatewayBot(): RestPromise<Pair<String, Int>> = RestPromise.simple(this, Route.GatewayBot.format()) { res, req ->
        when {
            res.isOk -> {
                val json = res.obj as JSObject
                req.succeed(json.string("url") to json.int("shards"))
            }

            res.isRateLimit -> {
                req.failure(RateLimitedException(req.route, res.retryAfter))
            }

            res.isUnauthorized -> {
                req.failure(LoginException("Attempted to login, but failed due to unauthorized access."))
            }

            else -> {
                req.failure(LoginException("Attempted to login, but failed due to an unknown error: " +
                                           "${res.code} - ${res.message}"))
            }
        }
    }

    private inner class APIThreadFactory : ThreadFactory {
        override fun newThread(r: Runnable): Thread = Thread(r, "$identifier API-Thread").also {
            it.isDaemon = true
        }
    }
}
