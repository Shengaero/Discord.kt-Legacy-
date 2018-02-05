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

import me.kgustave.dkt.entities.impl.APIImpl
import me.kgustave.dkt.handlers.shard.DefaultSessionManager
import me.kgustave.dkt.handlers.shard.impl.ShardControllerImpl
import kotlin.concurrent.thread
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * @author Kaidan Gustave
 */
object Discord {
    /**
     * The first second of January 1, 2015, the
     * moment Discord was officially live.
     */
    const val EPOCH = 1420070400000L
    const val BASE_URL = "https://discordapp.com"
    const val API_URL = "$BASE_URL/api"
    const val CDN_URL = "https://cdn.discordapp.com"

    // We save a singular shard controller implementation to use
    // when a developer shards their bot. This is purely internal.
    private lateinit var shardController: ShardControllerImpl

    suspend fun awaitLogin(block: APIConfig.() -> Unit): API = suspendCoroutine {
        try {
            it.resume(login(block))
        } catch(t: Throwable) {
            it.resumeWithException(t)
        }
    }

    fun beginLogin(block: APIConfig.() -> Unit): API {
        val config = APIConfig().apply(block)
        val api = APIImpl(config)

        thread(isDaemon = true, name = "Kotlincord Login-Thread") {
            api.login(shardInfo = null, sessionManager = config.sessionManager)
        }

        return api
    }

    fun login(block: APIConfig.() -> Unit): API {
        val config = APIConfig().apply(block)
        val api = APIImpl(config)

        api.login(shardInfo = null, sessionManager = config.sessionManager) // TODO Shard configurations
        return api
    }

    fun beginShardLogin(shardId: Int, shardTotal: Int, block: APIConfig.() -> Unit): API {
        val config = APIConfig().apply(block)

        if(!::shardController.isInitialized)
            shardController = ShardControllerImpl(config.sessionManager ?: DefaultSessionManager())

        val api = APIImpl(config, shardController)

        api.login(shardInfo = API.ShardInfo(shardId, shardTotal), sessionManager = shardController.sessionManager)

        return api
    }

    object KtInfo {
        /** Kotlincord's GitHub repository link. */
        const val GITHUB = "https://github.com/TheMonitorLizard/Kotlincord/"

        /** Kotlincord's official release version. */
        val VERSION: String = this::class.java.`package`.implementationVersion ?: "DEV"

        /** Kotlincord's full version. */
        val FULL_VERSION = if(VERSION != "DEV") "${VERSION}_ALPHA" else "DEV"

        /** The version of REST Kotlincord uses. */
        const val REST_VERSION = 6

        /** The Discord Gateway version Kotlincord targets. */
        const val GATEWAY_VERSION = 6
    }
}
