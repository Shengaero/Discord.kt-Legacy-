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

import me.kgustave.dkt.API
import me.kgustave.dkt.entities.impl.InternalMessage
import me.kgustave.dkt.requests.RestPromise
import me.kgustave.dkt.requests.promises.MessagePromise

/**
 * @since  1.0.0
 * @author Kaidan Gustave
 */
interface Message : Snowflake {
    companion object {
        /**
         * The maximum number of text characters that
         * can be sent in a single message.
         */
        const val MAX_TEXT_LENGTH = 2000

        fun contentEmpty(text: CharSequence?): Boolean {
            if(text == null || text.isEmpty())
                return true

            for(c in text) {
                if(!c.isWhitespace()) {
                    return false
                }
            }
            return true
        }
    }

    val type: Type
    val channel: MessageChannel
    val channelType: Channel.Type
    val content: String
    val renderedContent: String
    val author: User
    val member: Member?
    val embeds: List<Embed>
    val attachments: List<Attachment>
    val mentionedUsers: List<User>
    val mentionedEmotes: List<Emote>
    val mentionedChannels: List<TextChannel>
    val isWebhook: Boolean

    fun edit(text: String): MessagePromise
    fun edit(embed: Embed): MessagePromise
    fun edit(message: Message): MessagePromise

    fun delete(): RestPromise<Message>

    infix fun mentions(mentionable: Mentionable): Boolean


    class Builder: MessageDslComponent<Message.Builder>() {
        var embed: Embed? = null

        inline fun embed(block: Embed.Builder.() -> Unit) {
            val builder = Embed.Builder()
            builder.block()
            this.embed = Embed(builder)
        }
    }

    data class Attachment(
        val api: API, val id: Long, val url: String?, val proxyUrl: String?,
        val filename: String, val size: Int, val height: Int, val width: Int
    )

    enum class Type(val type: Int) {
        DEFAULT(0),
        CHANNEL_PINNED_ADD(6),
        GUILD_MEMBER_JOIN(7),
        UNKNOWN(-1);

        companion object {
            fun typeOf(type: Int): Message.Type = values().firstOrNull { it.type == type } ?: UNKNOWN
        }
    }

    enum class FormatSyntax(val syntax: String) {
        ITALICS("*"),
        BOLD("**"),
        UNDERLINE("__"),
        STRIKE_THROUGH("~~"),
        CODE("`"),
        CODE_BLOCK("```");

        companion object {
            fun surroundText(text: String, format: FormatSyntax): String {
                return "${format.syntax}$text${format.syntax}"
            }
        }
    }
}

inline fun message(block: Message.Builder.() -> Unit): Message {
    val builder = Message.Builder()
    builder.block()
    return InternalMessage(builder.content, builder.embed)
}
