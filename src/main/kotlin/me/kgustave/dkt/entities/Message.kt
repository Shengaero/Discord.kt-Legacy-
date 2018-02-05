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
package me.kgustave.dkt.entities

import me.kgustave.dkt.requests.RestPromise

/**
 * @since  1.0.0
 * @author Kaidan Gustave
 */
interface Message : Snowflake {
    val type: Type
    val channel: MessageChannel
    val content: String
    val renderedContent: String
    val author: User
    val member: Member?
    val embeds: List<Embed>
    val attachments: List<Attachment>
    val mentionedEmotes: List<Emote>
    val mentionedUsers: List<User>
    val mentionedChannels: List<Channel>

    val textChannel: TextChannel?
        get() = channel as? TextChannel
    val privateChannel: PrivateChannel?
        get() = channel as? PrivateChannel

    fun edit(text: String): RestPromise<Message>
    fun edit(embed: Embed): RestPromise<Message>
    fun edit(message: Message): RestPromise<Message>

    fun delete(): RestPromise<Message>

    enum class Type {
        TEXT, PRIVATE
    }

    class Builder {
        // TODO Message.Builder
    }

    class Attachment {
        // TODO Message.Attachment
    }

    companion object {
        /**
         * The maximum number of text characters that
         * can be sent in a single message.
         */
        const val MAX_TEXT_LENGTH = 2000
    }
}

/**
 * @author Kaidan Gustave
 */
interface MessageChannel : Channel {
    fun send(text: String): RestPromise<Message>
    fun send(embed: Embed): RestPromise<Message>
    fun send(message: Message): RestPromise<Message>
}
