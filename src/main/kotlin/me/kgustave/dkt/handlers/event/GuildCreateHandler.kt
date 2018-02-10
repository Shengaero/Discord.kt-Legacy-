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
import me.kgustave.dkt.entities.impl.GuildImpl
import me.kgustave.dkt.events.guild.GuildAvailableEvent
import me.kgustave.dkt.events.guild.GuildJoinEvent
import me.kgustave.dkt.events.guild.UnavailableGuildJoinEvent
import me.kgustave.dkt.handlers.EventCache
import me.kgustave.dkt.util.snowflake
import me.kgustave.kson.KSONObject

/**
 * @author Kaidan Gustave
 */
class GuildCreateHandler(override val api: APIImpl): EventHandler(Type.GUILD_CREATE) {
    private val readyHandler get() = api.websocket.handlers[Type.READY] as ReadyHandler

    override fun handle(event: KSONObject, responseNumber: Long, rawKSON: KSONObject) {
        val id = snowflake(event["id"])
        val guildCreated = api.internalGuildCache.entityMap[id] as? GuildImpl
        val wasUnavailable = guildCreated?.unavailable

        api.entityBuilder.createGuild(event) { guild ->
            if(guild.unavailable) {
                if(!api.websocket.ready) {
                    // This guild is now unavailable during READY setup.
                    // We set it to reflect this.
                    readyHandler.acknowledgeGuild(guild.id, unavailable = true, shouldChunk = false)
                } else {
                    // We joined an unavailable guild
                    api.dispatchEvent(UnavailableGuildJoinEvent(api, responseNumber, guild.id))
                }
            } else {
                if(!api.websocket.ready) {
                    return@createGuild readyHandler.completeGuild(guild.id)
                }

                if(wasUnavailable == false) {
                    // Now the guild that we have created never existed this
                    // means that it's new to us, so we fire a GuildJoinEvent
                    api.dispatchEvent(GuildJoinEvent(api, responseNumber, guild))
                    api.eventCache.run(EventCache.Type.GUILD, guild.id)
                } else {
                    // The guild was not new to us, it did exist previous to now
                    // and it is now available to us, so we fire a GuildAvailableEvent
                    api.dispatchEvent(GuildAvailableEvent(api, responseNumber, guild))
                }
            }
        }
    }
}
