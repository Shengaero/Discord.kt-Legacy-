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
import me.kgustave.json.JSObject

/**
 * @author Kaidan Gustave
 */
abstract class EventHandler(val type: Type) {
    companion object {
        internal fun newEventHandlerMap(api: APIImpl) = mapOf(
            Type.CHANNEL_CREATE to ChannelCreateHandler(api),
            Type.GUILD_CREATE to GuildCreateHandler(api),
            Type.GUILD_MEMBERS_CHUNK to GuildMembersChunkHandler(api),
            Type.PRESENCE_UPDATE to PresenceUpdateHandler(api),
            Type.READY to ReadyHandler(api)
        )
    }

    protected abstract val api: APIImpl

    fun handle(rawEvent: JSObject) {
        handle(rawEvent.obj("d"), api.responses, rawEvent)
    }

    abstract fun handle(event: JSObject, responseNumber: Long, rawKSON: JSObject)

    // Queues the event for a given guildId
    protected fun queueEventForGuild(guildId: Long, rawKSON: JSObject) {
        api.guildQueue.queue(guildId, rawKSON)
    }

    enum class Type {
        CHANNEL_CREATE,
        GUILD_CREATE,
        GUILD_MEMBERS_CHUNK,
        PRESENCE_UPDATE,
        READY;

        companion object {
            fun of(t: String): Type? = values().firstOrNull { it.name == t }
        }
    }
}
