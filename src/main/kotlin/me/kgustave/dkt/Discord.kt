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
@file:Suppress("MemberVisibilityCanPrivate", "MemberVisibilityCanBePrivate", "Unused")
package me.kgustave.dkt

import kotlinx.coroutines.experimental.*
import me.kgustave.dkt.entities.impl.APIImpl
import me.kgustave.dkt.handlers.DefaultSessionManager
import me.kgustave.dkt.handlers.shard.impl.ShardControllerImpl
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Information on [discord.gg](https://discord.gg/) as well
 * as entry points for the Discord.kt API.
 *
 * @since  1.0.0
 * @author Kaidan Gustave
 */
object Discord {
    /** The first second of January 1, 2015, the moment Discord was officially live */
    const val EPOCH = 1420070400000L

    /** The base URL for Discord: `https://discordapp.com` */
    const val BASE_URL = "https://discordapp.com"

    /** The base API URL for Discord: `https://discordapp.com/api` */
    const val API_URL = "$BASE_URL/api"

    /** The base CDN URL for Discord: `https://cdn.discordapp.com` */
    const val CDN_URL = "https://cdn.discordapp.com"

    // We save a singular shard controller implementation to use
    // when a developer shards their bot. This is purely internal.
    private lateinit var shardController: ShardControllerImpl

    suspend fun awaitLogin(block: APIConfig.() -> Unit): API {
        val config = APIConfig().apply(block)
        val api = APIImpl(config)
        api.login(shardInfo = null, sessionManager = config.sessionManager ?: DefaultSessionManager())
        return api
    }

    suspend fun awaitShardLogin(shardId: Int, shardTotal: Int, block: APIConfig.() -> Unit): API {
        val config = APIConfig().apply(block)
        if(!::shardController.isInitialized)
            shardController = ShardControllerImpl(config.sessionManager ?: DefaultSessionManager())
        val api = APIImpl(config, shardController)
        api.login(shardInfo = API.ShardInfo(shardId, shardTotal), sessionManager = shardController.sessionManager)
        return api
    }

    fun beginLogin(block: APIConfig.() -> Unit): API {
        val config = APIConfig().apply(block)
        val api = APIImpl(config)
        val loginContext = newSingleThreadContext("Kotlincord Login-Thread")
        val loginJob = launch(loginContext, CoroutineStart.LAZY) {
            api.login(shardInfo = null, sessionManager = config.sessionManager ?: DefaultSessionManager())
        }
        loginJob.invokeOnCompletion { loginContext.close() }
        loginJob.start()
        return api
    }

    fun login(context: CoroutineContext = Unconfined, block: APIConfig.() -> Unit): API {
        val config = APIConfig().apply(block)
        val api = APIImpl(config)
        runBlocking(context) {
            api.login(shardInfo = null, sessionManager = config.sessionManager ?: DefaultSessionManager())
        }
        return api
    }

    object KtInfo {
        private const val DEV_VERSION = "DEV"

        /** Discord.kt's GitHub repository link. */
        const val GITHUB = "https://github.com/TheMonitorLizard/Discord.kt/"

        /** The version of REST Discord.kt uses. */
        const val REST_VERSION = 6

        /** The Discord Gateway version Discord.kt targets. */
        const val GATEWAY_VERSION = 6

        /** Discord.kt's official release version. */
        val VERSION = this::class.java.`package`.implementationVersion ?: DEV_VERSION

        /** Discord.kt's full version. */
        val FULL_VERSION = if(VERSION != DEV_VERSION) "${VERSION}_ALPHA" else DEV_VERSION
    }
}
