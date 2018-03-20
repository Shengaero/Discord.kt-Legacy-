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
import me.kgustave.dkt.util.createLogger
import me.kgustave.dkt.util.snowflake
import me.kgustave.json.JSArray
import me.kgustave.json.JSObject
import java.util.*

/**
 * @author Kaidan Gustave
 */
class GuildMembersChunkHandler(override val api: APIImpl): EventHandler(Type.GUILD_MEMBERS_CHUNK) {
    companion object {
        val LOG = createLogger(GuildMembersChunkHandler::class)
    }

    private val expectedGuildMemberMap = HashMap<Long, Int>()
    private val memberChunkCache = HashMap<Long, MutableList<JSArray>>()

    override fun handle(event: JSObject, responseNumber: Long, rawKSON: JSObject) {
        val guildId = snowflake(event["guild_id"])

        val expected = expectedGuildMemberMap[guildId]!!
        val memberChunks = memberChunkCache[guildId]!!

        val members = event.array("members")

        LOG.debug("Chunking ${members.size} members for Guild ID: $guildId")
        memberChunks.add(members)

        if(memberChunks.sumBy { it.size } >= expected) {
            LOG.debug("Finished Chunking for Guild ID: $guildId")
            api.entityBuilder.handleGuildMemberChunks(guildId, memberChunks)
            memberChunkCache.remove(guildId)
            expectedGuildMemberMap.remove(guildId)
        }
    }

    fun setExpectedGuildMembers(guildId: Long, expectedCount: Int) {
        if(guildId in expectedGuildMemberMap) {
            LOG.warn("Set the expected user count for a guild that was already mapped (ID: $guildId)")
        }

        if(guildId in memberChunkCache) {
            LOG.warn("Set the expected member chunk for a guild that was already setup (ID: $guildId)")
        }

        expectedGuildMemberMap[guildId] = expectedCount
        memberChunkCache[guildId] = LinkedList()
    }

    fun addExpectedGuildMembers(guildId: Long, addition: Int) {
        expectedGuildMemberMap[guildId]?.let { expected ->
            expectedGuildMemberMap[guildId] = expected + addition
        }
    }

    @Suppress("Unused")
    fun clear() {
        expectedGuildMemberMap.clear()
        memberChunkCache.clear()
    }
}
