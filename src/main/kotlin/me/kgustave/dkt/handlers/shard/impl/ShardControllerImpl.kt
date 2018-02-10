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
package me.kgustave.dkt.handlers.shard.impl

import me.kgustave.dkt.API
import me.kgustave.dkt.entities.SelfUser
import me.kgustave.dkt.handlers.DefaultSessionManager
import me.kgustave.dkt.handlers.SessionManager
import me.kgustave.dkt.handlers.shard.ShardController

/**
 * @author Kaidan Gustave
 */
class ShardControllerImpl(override val sessionManager: SessionManager = DefaultSessionManager()): ShardController {
    private val shardMap: MutableMap<Int, API> = HashMap()

    override lateinit var self: SelfUser

    val selfUserIsInit: Boolean
        get() = ::self.isInitialized

    override val shards: List<API>
        get() = shardMap.values.toList()

    override fun getShardById(id: Int): API {
        return requireNotNull(shardMap[id]) { "Could not find shard instance for shard ID: $id" }
    }
}
