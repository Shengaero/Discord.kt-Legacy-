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
package me.kgustave.dkt.events.message.guild

import me.kgustave.dkt.API
import me.kgustave.dkt.entities.Guild
import me.kgustave.dkt.entities.Member
import me.kgustave.dkt.entities.TextChannel
import me.kgustave.dkt.entities.TextMessage
import me.kgustave.dkt.events.message.MessageReceivedEvent

/**
 * @author Kaidan Gustave
 */
class GuildMessageReceivedEvent(
    override val api: API,
    override val responseNumber: Long,
    override val message: TextMessage
): MessageReceivedEvent {
    override val textMessage: TextMessage
        get() = message
    override val textChannel: TextChannel
        get() = message.channel
    override val guild: Guild
        get() = message.guild
    override val member: Member
        get() = message.member
}
