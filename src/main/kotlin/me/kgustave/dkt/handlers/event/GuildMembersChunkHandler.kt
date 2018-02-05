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
import me.kgustave.dkt.util.snowflake
import me.kgustave.kson.KSONArray
import me.kgustave.kson.KSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

/**
 * @author Kaidan Gustave
 */
class GuildMembersChunkHandler(override val api: APIImpl): EventHandler(Type.GUILD_MEMBERS_CHUNK) {
    companion object {
        val LOG: Logger = LoggerFactory.getLogger(GuildMembersChunkHandler::class.java)
    }

    private val expectedGuildMemberMap = HashMap<Long, Int>()
    private val memberChunkCache = HashMap<Long, MutableList<KSONArray>>()

    override fun handle(event: KSONObject, responseNumber: Long, rawKSON: KSONObject) {
        val guildId = snowflake(event["guild_id"])

        val expected = expectedGuildMemberMap[guildId]!!
        val memberChunks = memberChunkCache[guildId]!!

        val members = event["members"] as KSONArray

        LOG.debug("Chunking ${members.size} members for Guild ID: $guildId")
        memberChunks.add(members)

        if(memberChunks.sumBy { it.size } >= expected) {
            LOG.debug("Finished Chunking for Guild ID: $guildId")
            // TODO Add entity setup
            expectedGuildMemberMap.remove(guildId)
            memberChunkCache.remove(guildId)
        }
    }

    fun addExpectedGuildMembers(guildId: Long, expectedCount: Int) {
        val expected = if(expectedGuildMemberMap.containsKey(guildId))
            expectedGuildMemberMap[guildId]!! + expectedCount
        else expectedCount

        expectedGuildMemberMap[guildId] = expected

        if(!memberChunkCache.containsKey(guildId))
            memberChunkCache[guildId] = LinkedList()
    }

    fun clear() {
        expectedGuildMemberMap.clear()
        memberChunkCache.clear()
    }
}
