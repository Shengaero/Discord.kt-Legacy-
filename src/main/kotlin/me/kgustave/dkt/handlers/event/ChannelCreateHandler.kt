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
import me.kgustave.dkt.events.channel.text.TextChannelCreateEvent
import me.kgustave.dkt.util.createLogger
import me.kgustave.dkt.util.snowflake
import me.kgustave.kson.KSONObject

/**
 * @author Kaidan Gustave
 */
class ChannelCreateHandler(override val api: APIImpl): EventHandler(EventHandler.Type.CHANNEL_CREATE) {
    companion object {
        val LOG = createLogger(ChannelCreateHandler::class)
    }

    override fun handle(event: KSONObject, responseNumber: Long, rawKSON: KSONObject) {
        val typeInt = event["type"] as Int
        val type = Channel.Type.typeOf(typeInt)
        when(type) {
            Channel.Type.TEXT -> handleText(event, responseNumber)
            Channel.Type.VOICE -> handleVoice(event, responseNumber)
            Channel.Type.CATEGORY -> handleCategory(event, responseNumber)
            Channel.Type.PRIVATE -> handlePrivate(event, responseNumber)
            Channel.Type.UNKNOWN ->
                LOG.warn("Received a '$type' event with unknown channel type: $typeInt\n" +
                         event.toString())
        }
    }

    private fun handleText(event: KSONObject, responseNumber: Long) {
        val guildId = snowflake(event["guild_id"])
        val channel = api.entityBuilder.createTextChannel(event, guildId)

        api.dispatchEvent(TextChannelCreateEvent(api, responseNumber, channel))
    }

    private fun handleVoice(event: KSONObject, responseNumber: Long) {
        val guildId = snowflake(event["guild_id"])
        val channel = api.entityBuilder.createVoiceChannel(event, guildId)
    }

    private fun handleCategory(event: KSONObject, responseNumber: Long) {

    }

    private fun handlePrivate(event: KSONObject, responseNumber: Long) {

    }
}
