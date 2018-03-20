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
@file:Suppress("MemberVisibilityCanBePrivate", "Unused")
package me.kgustave.dkt.events.message

import me.kgustave.dkt.entities.*

/**
 * @author Kaidan Gustave
 */
interface MessageReceivedEvent : MessageEvent {
    // General
    val message: Message

    val author: User
        get() = message.author
    override val messageId: Long
        get() = message.id
    override val channel: MessageChannel
        get() = message.channel

    // Text
    val textMessage: TextMessage?
        get() = message as? TextMessage
    val textChannel: TextChannel?
        get() = textMessage?.channel
    val guild: Guild?
        get() = textMessage?.guild
    val member: Member?
        get() = guild?.getMember(author)

    // Private
    val privateMessage: PrivateMessage?
        get() = message as? PrivateMessage
    val privateChannel: PrivateChannel?
        get() = privateMessage?.channel

    // Misc
    val isWebhook: Boolean
        get() = message.isWebhook
}
