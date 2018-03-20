/*
 * Copyright 2017 Kaidan Gustave
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

import me.kgustave.dkt.entities.Channel
import me.kgustave.dkt.entities.impl.APIImpl
import me.kgustave.dkt.events.channel.category.CategoryCreateEvent
import me.kgustave.dkt.events.channel.priv.PrivateChannelCreateEvent
import me.kgustave.dkt.events.channel.text.TextChannelCreateEvent
import me.kgustave.dkt.events.channel.voice.VoiceChannelCreateEvent
import me.kgustave.dkt.handlers.EventCache
import me.kgustave.dkt.util.createLogger
import me.kgustave.dkt.util.snowflake
import me.kgustave.json.JSObject

/**
 * @author Kaidan Gustave
 */
class ChannelCreateHandler(override val api: APIImpl): EventHandler(EventHandler.Type.CHANNEL_CREATE) {
    companion object {
        val LOG = createLogger(ChannelCreateHandler::class)
    }

    override fun handle(event: JSObject, responseNumber: Long, rawKSON: JSObject) {
        val typeInt = event.int("type")
        val type = Channel.Type.typeOf(typeInt)
        val guildId = if(type.isGuild) snowflake(event["guild_id"]) else 0L

        if(guildId != 0L) {
            if(api.guildQueue.isRegistered(guildId)) {
                return queueEventForGuild(guildId, rawKSON)
            }
        }

        when(type) {
            Channel.Type.TEXT -> {
                val channel = api.entityBuilder.createTextChannel(event, guildId)
                api.dispatchEvent(TextChannelCreateEvent(api, responseNumber, channel))
            }
            Channel.Type.VOICE -> {
                val channel = api.entityBuilder.createVoiceChannel(event, guildId)
                api.dispatchEvent(VoiceChannelCreateEvent(api, responseNumber, channel))
            }
            Channel.Type.CATEGORY -> {
                val category = api.entityBuilder.createCategory(event, guildId)
                api.dispatchEvent(CategoryCreateEvent(api, responseNumber, category))
            }
            Channel.Type.PRIVATE -> {
                val channel = api.entityBuilder.createPrivateChannel(event)
                api.dispatchEvent(PrivateChannelCreateEvent(api, responseNumber, channel))
            }
            Channel.Type.UNKNOWN -> {
                LOG.warn("Received a '$type' event with unknown channel type: $typeInt\n$event")
            }
        }

        api.eventCache.run(EventCache.Type.CHANNEL, snowflake(event["id"]))
    }
}
