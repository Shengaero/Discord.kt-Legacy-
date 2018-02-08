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
import me.kgustave.dkt.entities.caching.SnowflakeCache
import me.kgustave.dkt.entities.caching.impl.SnowflakeCacheImpl
import me.kgustave.dkt.events.Event
import me.kgustave.dkt.exceptions.RateLimitedException
import me.kgustave.dkt.exceptions.UnloadedPropertyException
import me.kgustave.dkt.handlers.shard.SessionManager
import me.kgustave.dkt.handlers.shard.ShardController
import me.kgustave.dkt.handlers.shard.impl.ShardControllerImpl
import me.kgustave.dkt.hooks.EventDispatcher
import me.kgustave.dkt.requests.DiscordWebSocket
import me.kgustave.dkt.requests.RestPromise
import me.kgustave.dkt.requests.Requester
import me.kgustave.dkt.requests.Route
import me.kgustave.dkt.requests.promises.PreCompletedPromise
import me.kgustave.dkt.util.queue.GuildEventQueue
import me.kgustave.dkt.util.unsupported
import me.kgustave.kson.KSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
constructor(config: APIConfig, private val _shardController: ShardControllerImpl? = null): API {
    companion object {
        val LOG: Logger = LoggerFactory.getLogger(API::class.java)
    }

    private lateinit var readyContinuation: Continuation<Unit>

    // If the self-user is attached to the shard-controller we should
    // take that one immediately and use it.
    @Volatile private var _self: SelfUser? = _shardController?.let { if(it.selfUserIsInit) it.self else null }
        set(value) {
            // Never set this field twice
            if(field != null)
                return

            // Make sure this is not null
            val self = requireNotNull(value) { "Cannot set SelfUser to null!" }

            // Have not initialized self-user for the shard-controller yet
            if(_shardController?.selfUserIsInit == false)
                _shardController.self = self

            // Set field
            field = self
        }

    override var responses = 0L
    override var ping = 0L

    override var shardInfo: API.ShardInfo? = null
        private set
    @Volatile override var status: API.Status = API.Status.INITIALIZING
        set(value) {
            synchronized(field) {
                // TODO Add an event handle here
                field = value
            }
        }
    override var shouldAutoReconnect: Boolean = config.shouldAutoReconnect
        set(value) {
            field = value
            websocket.shouldReconnect = value
        }
    override var presence: Presence = PresenceImpl(this, config.onlineStatus, config.activity, config.afk)
        set(value) {
            synchronized(field) {
                val impl = requireNotNull(value as? PresenceImpl) {
                    "Presence must be a valid instance of PresenceImpl!"
                }

                // TODO Add an event handle here
                field = impl
                impl.update()
            }
        }

    lateinit var websocket: DiscordWebSocket

    override val token: String = config.token

    // TODO Make this optional
    private val shutdownThread: Thread? = thread(false, name = "Kotlincord Shutdown Thread") { shutdown() }
    private val eventDispatcher: EventDispatcher = config.eventDispatcher
    private val apiAsyncContext: CoroutineContext = config.apiAsyncContext ?:
                                                    newSingleThreadContext("Kotlincord Async-Context")

    internal val internalUserCache = SnowflakeCacheImpl(User::name)
    internal val internalGuildCache = SnowflakeCacheImpl(Guild::name)
    internal val internalTextChannelCache = SnowflakeCacheImpl(TextChannel::name)
    internal val internalVoiceChannelCache = SnowflakeCacheImpl(VoiceChannel::name)
    internal val internalPrivateChannelCache = SnowflakeCacheImpl<PrivateChannel>(null)

    internal val guildQueue = GuildEventQueue(this)

    val httpClientBuilder = config.httpClientBuilder
    val requester = Requester(this)
    val entityBuilder = EntityBuilder(this)
    val pool = ScheduledThreadPoolExecutor(config.corePoolSize, APIThreadFactory())
    val context = pool.asCoroutineDispatcher() // We use the same pool as a context.
    val identifier = "Discord Bot${if(isSharded) " ($shardInfo)" else ""}"
    val gatewayUrl: String = requestGatewayBot().complete().first

    init {
        // TODO ShardController setup
        config.listeners.forEach { eventDispatcher.addListener(it) }
    }

    override var self: SelfUser
        internal set(value) { _self = value }
        get() = _self ?: throw UnloadedPropertyException("API has not loaded SelfUser yet!")

    override val userCache: SnowflakeCache<User>
        get() = internalUserCache
    override val guildCache: SnowflakeCache<Guild>
        get() = internalGuildCache
    override val textChannelCache: SnowflakeCache<TextChannel>
        get() = internalTextChannelCache
    override val voiceChannelCache: SnowflakeCache<VoiceChannel>
        get() = internalVoiceChannelCache
    override val privateChannelCache: SnowflakeCache<PrivateChannel>
        get() = internalPrivateChannelCache
    override val users: List<User>
        get() = internalUserCache.toList()
    override val guilds: List<Guild>
        get() = internalGuildCache.toList()
    override val textChannels: List<TextChannel>
        get() = internalTextChannelCache.toList()
    override val voiceChannels: List<VoiceChannel>
        get() = internalVoiceChannelCache.toList()
    override val privateChannels: List<PrivateChannel>
        get() = privateChannelCache.toList()
    override val shardController: ShardController
        get() = _shardController ?: unsupported { "Cannot get ShardController for a non-sharded bot instance!" }

    // TODO Add SessionManager configurability
    suspend fun login(shardInfo: API.ShardInfo?, sessionManager: SessionManager?) {
        this.shardInfo = shardInfo

        // We are now logging in
        status = API.Status.LOGGING_IN

        if(token.isEmpty())
            throw IllegalArgumentException("Token provided was empty!") // TODO Token Verification

        shutdownThread?.let { Runtime.getRuntime().addShutdownHook(it) }

        if(shardInfo != null)
            LOG.debug("Initializing WebSocket for $shardInfo")
        else
            LOG.debug("Initializing WebSocket...")

        websocket = DiscordWebSocket(this, sessionManager)

        return suspendCoroutine {
            readyContinuation = it
        }
    }

    fun signalReady(exception: Throwable? = null) {
        if(exception != null)
            readyContinuation.resumeWithException(exception)
        else
            readyContinuation.resume(Unit)
    }

    fun dispatchEvent(event: Event) {
        LOG.trace("Firing ${event::class} (Response ${event.responseNumber})")
        eventDispatcher.onEvent(event)
    }

    override fun findUserById(id: Long): RestPromise<User> {
        internalUserCache[id]?.let { return PreCompletedPromise(this@APIImpl, it) }

        return RestPromise.simple(this, Route.GetUser.format(id)) { res, req ->
            if(!res.isOk) {
                req.error(res)
            } else {
                req.succeed(entityBuilder.createUser(res.obj as KSONObject, shouldCache = false))
            }
        }
    }

    override fun async(block: suspend CoroutineScope.() -> Unit) {
        async(apiAsyncContext) { block() }
    }

    override fun modifyPresence(block: Presence.() -> Unit) {
        (presence as? PresenceImpl)?.updateInBulk(block)
    }

    override fun shutdown() {
        // We are already shutting down, return
        if(status == API.Status.SHUTDOWN || status == API.Status.SHUTTING_DOWN)
            return

        // We are now shutting down
        status = API.Status.SHUTTING_DOWN

        // Make sure it's initialized, this shouldn't
        // ever happen, because the conditions that would
        // allow it would mean the laws of threading
        // had to be reinvented or some bullshit, but
        // just in case...
        if(::websocket.isInitialized)
            websocket.shutdown()
        requester.shutdown(5, TimeUnit.SECONDS)
        pool.setKeepAliveTime(5, TimeUnit.SECONDS)
        pool.allowCoreThreadTimeOut(true)

        if(apiAsyncContext is AutoCloseable) {
            apiAsyncContext.close()
        }

        // Remove the shutdown hook
        shutdownThread?.let {
            try { Runtime.getRuntime().removeShutdownHook(it) } catch(ignored: Throwable) {}
        }

        // We are now done shutting down
        status = API.Status.SHUTDOWN
    }

    override fun toString(): String {
        shardInfo?.apply { return "API Connection(url=\"$gatewayUrl\", shard_id=$shardId, shard_total=$shardTotal)" }

        return "API Connection(url=\"$gatewayUrl\")"
    }

    private fun requestGatewayBot(): RestPromise<Pair<String, Int>> = RestPromise.simple(this, Route.GatewayBot.format()) { res, req ->
        when {
            res.isOk -> {
                val kson = res.obj as KSONObject
                req.succeed((kson["url"] as String) to (kson["shards"] as Int))
            }

            res.isRateLimit ->
                req.failure(RateLimitedException(req.route, res.retryAfter))

            res.isUnauthorized ->
                req.failure(LoginException("Attempted to login, but failed due to unauthorized access."))

            else ->
                req.failure(LoginException("Attempted to login, but failed due to an unknown error: ${res.code} - ${res.message}"))
        }
    }

    inner class APIThreadFactory : ThreadFactory {
        override fun newThread(r: Runnable): Thread {
            return Thread(r, "$identifier API-Thread").also {
                it.isDaemon = true
            }
        }
    }
}
