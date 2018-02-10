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
package me.kgustave.dkt.handlers.event

import me.kgustave.dkt.entities.impl.APIImpl
import me.kgustave.dkt.requests.OpCode
import me.kgustave.dkt.util.createLogger
import me.kgustave.dkt.util.snowflake
import me.kgustave.kson.KSONArray
import me.kgustave.kson.KSONObject
import me.kgustave.kson.kson

/**
 * @author Kaidan Gustave
 */
class ReadyHandler(override val api: APIImpl): EventHandler(Type.READY) {
    companion object {
        val LOG = createLogger(ReadyHandler::class)
    }

    private val incompleteGuilds = HashSet<Long>()
    private val unavailableGuilds = HashSet<Long>()
    private val acknowledgedGuilds = HashSet<Long>()
    private val chunkingGuilds = HashSet<Long>()

    private lateinit var readyEvent: KSONObject

    override fun handle(event: KSONObject, responseNumber: Long, rawKSON: KSONObject) {
        this.readyEvent = event

        val rawSelf = event["user"] as KSONObject
        val rawGuilds = (event["guilds"] as KSONArray).mapNotNull { it as? KSONObject }
        val entityBuilder = api.entityBuilder

        entityBuilder.createSelf(rawSelf)

        // First we cache all guild IDs that are incomplete
        rawGuilds.forEach { incompleteGuilds += snowflake(it["id"]) }

        // Then we create all of them
        rawGuilds.forEach {
            if(it.opt<Boolean>("unavailable") == true) {
                entityBuilder.createGuild(it) { completeGuild(it.id) }
            } else {
                entityBuilder.createGuild(it)
            }
        }

        // We have no guilds, and thus we have to complete manually
        if(rawGuilds.isEmpty()) {
            completeGuildLoading()
        }
    }

    fun acknowledgeGuild(guildId: Long, unavailable: Boolean = false, shouldChunk: Boolean = false) {
        acknowledgedGuilds += guildId
        if(!unavailable) {
            unavailableGuilds -= guildId
            if(shouldChunk) {
                chunkingGuilds += guildId
            }
        } else {
            unavailableGuilds += guildId
        }
    }

    fun completeGuild(id: Long) {
        if(!incompleteGuilds.remove(id))
            LOG.warn("Attempted to acknowledge a Guild that did not exist or was completed!")
        if(incompleteGuilds.size == unavailableGuilds.size) {
            completeGuildLoading()
        } else {
            attemptDoChunking()
        }
    }

    private fun completeGuildLoading(event: KSONObject = readyEvent) {
        api.websocket.chunkingGuildMembers = false
        val entityBuilder = api.entityBuilder

        // Private Channel Setup
        val rawPcs = event["private_channels"] as KSONArray
        if(rawPcs.isNotEmpty()) {
            val filtered = ArrayList<KSONObject>()
            for(value in rawPcs) {
                val pc = value as? KSONObject
                if(pc === null) {
                    LOG.warn("PrivateChannel array of READY data was not a JSON Object")
                    continue
                }
                // This represents a Group as opposed to a DM, this is not
                // possible, but we check and filter regardless for consistency
                // and error safety.
                if("recipients" !in pc) {
                    LOG.debug("Found object in PrivateChannel array of READY data matching a Group DM. Removing...")
                    continue
                }
                filtered.add(pc)
            }
            filtered.forEach { entityBuilder.createPrivateChannel(it) }
        }

        api.websocket.ready()
    }

    @Suppress("Unused")
    fun clear() {
        incompleteGuilds.clear()
        unavailableGuilds.clear()
        acknowledgedGuilds.clear()
        chunkingGuilds.clear()
    }

    private fun attemptDoChunking() {
        if(acknowledgedGuilds.size == incompleteGuilds.size) {
            api.websocket.chunkingGuildMembers = true
            doChunking()
        }
    }

    private fun doChunking() {
        if(chunkingGuilds.isEmpty())
            return

        var guildIds = KSONArray()
        chunkingGuilds.forEach { id ->
            guildIds.put(id)

            // Only 50 guilds per request
            if(guildIds.size == 50) {
                api.websocket.queueChunkRequest(kson {
                    "op" to OpCode.REQUEST_GUILD_MEMBERS
                    "d" to kson {
                        "guild_id" to guildIds
                        "query" to ""
                        "limit" to 0
                    }
                })
                guildIds = KSONArray()
            }
        }

        // If we have any remaining guilds
        if(guildIds.isNotEmpty()) {
            api.websocket.queueChunkRequest(kson {
                "op" to OpCode.REQUEST_GUILD_MEMBERS
                "d" to kson {
                    "guild_id" to guildIds
                    "query" to ""
                    "limit" to 0
                }
            })
        }

        chunkingGuilds.clear()
    }
}
