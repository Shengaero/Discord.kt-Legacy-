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
@file:Suppress("MemberVisibilityCanPrivate")
package me.kgustave.dkt

import kotlinx.coroutines.experimental.CoroutineScope
import me.kgustave.dkt.entities.*
import me.kgustave.dkt.entities.caching.SnowflakeCache
import me.kgustave.dkt.handlers.shard.ShardController
import me.kgustave.dkt.requests.RestPromise

/**
 * @author Kaidan Gustave
 */
interface API {
    val responses: Long
    val token: String
    var presence: Presence
    val ping: Long
    val status: Status
    var shouldAutoReconnect: Boolean
    val shardInfo: ShardInfo?
    val shardController: ShardController
    val isSharded: Boolean
        get() = shardInfo != null
    val self: SelfUser

    // CACHES //

    val userCache: SnowflakeCache<User>
    val guildCache: SnowflakeCache<Guild>
    val textChannelCache: SnowflakeCache<TextChannel>
    val voiceChannelCache: SnowflakeCache<VoiceChannel>
    val categoryCache: SnowflakeCache<Category>
    val privateChannelCache: SnowflakeCache<PrivateChannel>

    val users: List<User>
    val guilds: List<Guild>
    val textChannels: List<TextChannel>
    val voiceChannels: List<VoiceChannel>
    val categories: List<Category>
    val privateChannels: List<PrivateChannel>

    fun getUserById(id: Long): User?

    fun getTextChannelById(id: Long): TextChannel?

    fun findUserById(id: Long): RestPromise<User>

    fun async(block: suspend CoroutineScope.() -> Unit)

    fun modifyPresence(block: Presence.() -> Unit)

    fun shutdown()

    enum class Status {
        /**
         * The API is initializing. This is the first status we will have.
         */
        INITIALIZING,

        /**
         * The API has finished pre-login initialization.
         */
        INITIALIZED,

        /**
         * The API is logging in.
         */
        LOGGING_IN,

        /**
         * The API is attempting to connect to Discord's websocket.
         */
        CONNECTING_TO_WEBSOCKET,

        /**
         * The API is identifying it's session.
         */
        IDENTIFYING,

        /**
         * The API is awaiting confirmation from it's IDENTIFY authentication.
         */
        AWAITING_IDENTIFY_CONFIRMATION,

        /**
         * The API is validating and creating any and all entities, as well as
         * caching events to replay after.
         */
        SETTING_UP,

        /**
         * The API is now connected.
         */
        CONNECTED,

        /**
         * The API has disconnected, either because it shut down, or due to some other phenomena.
         * This status is short-lived, and typically followed by a change to either
         */
        DISCONNECTED,

        /**
         * The API is queued in the [SessionManager][me.kgustave.dkt.handlers.shard.SessionManager]
         * and waiting to start reconnecting safely.
         */
        QUEUED_TO_RECONNECT,

        /**
         * An error occurred while trying to reconnect to Discord, such as poor internet connection,
         * and the API is waiting until it's allowed to reconnect again.
         */
        WAITING_TO_RECONNECT,

        /**
         * The API has [disconnected][DISCONNECTED], possibly waited until it was safe,
         * and is now attempting to reconnect to Discord's websocket.
         */
        ATTEMPTING_TO_RECONNECT,

        /**
         * The API is currently shutting down.
         */
        SHUTTING_DOWN,

        /**
         * The API has completely shut down, further interaction with Discord is impossible.
         */
        SHUTDOWN,

        /**
         * The API failed to login, but Discord told us the authentication information we provided
         * was invalid.
         */
        LOGIN_FAILED;

        val hasIdentified: Boolean by lazy { ordinal > AWAITING_IDENTIFY_CONFIRMATION.ordinal }
        val initializing: Boolean by lazy { ordinal <= CONNECTED.ordinal }
    }

    data class ShardInfo(val shardId: Int, val shardTotal: Int) {
        val shardString = "[$shardId / $shardTotal]"

        override fun toString(): String = "Shard $shardString"
    }
}
