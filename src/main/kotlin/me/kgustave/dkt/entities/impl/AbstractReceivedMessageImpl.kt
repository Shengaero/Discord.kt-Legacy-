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
package me.kgustave.dkt.entities.impl

import me.kgustave.dkt.entities.*
import me.kgustave.dkt.util.snowflake
import me.kgustave.dkt.util.unmodifiableList

/**
 * @author Kaidan Gustave
 */
abstract class AbstractReceivedMessageImpl<out C: MessageChannel>(
    override val id: Long,
    override val api: APIImpl,
    override val type: Message.Type,
    override val author: User,
    override val embeds: List<Embed>,
    override val channel: C,
    override val attachments: List<Message.Attachment>,
    mentionedUserIds: Set<Long>,
    content: String
): AbstractMessageImpl(content) {

    override val renderedContent: String by lazy(::renderContent)

    override val mentionedUsers: List<User> by lazy {
        if(content.isBlank()) {
            return@lazy emptyList<User>()
        }

        val mentions = ArrayList<User>()
        val seq = Mentionable.Format.USER.regex.findAll(content)

        for(matchResult in seq) {
            try {
                val id = snowflake(matchResult.groupValues[1])
                if(id !in mentionedUserIds) {
                    continue
                }

                if(this is TextMessage) {
                    val user = guild.getMemberById(id)?.user
                    if(user !== null) {
                        mentions += user
                        continue
                    }
                }

                api.getUserById(id)?.let {
                    mentions += it
                }
            } catch(ignored: NumberFormatException) {}
        }

        return@lazy unmodifiableList(mentions)
    }

    override val mentionedChannels: List<TextChannel> by lazy {
        if(content.isBlank()) {
            return@lazy emptyList<TextChannel>()
        }
        val mentions = ArrayList<TextChannel>()
        val seq = Mentionable.Format.CHANNEL.regex.findAll(content)

        for(matchResult in seq) {
            try {
                val id = snowflake(matchResult.groupValues[1])
                if(id !in mentionedUserIds) {
                    continue
                }

                if(this is TextMessage) {
                    val channel = guild.getTextChannelById(id)
                    if(channel !== null) {
                        mentions += channel
                        continue
                    }
                }

                api.getTextChannelById(id)?.let {
                    mentions += it
                }
            } catch(ignored: NumberFormatException) {}
        }

        return@lazy unmodifiableList(mentions)
    }

    override val mentionedEmotes: List<Emote> by lazy {
        emptyList<Emote>() // TODO Mentioned Emotes
    }

    protected open fun renderContent(): String {
        var tmp = content
        mentionedUsers.forEach { user ->
            val name = if(this@AbstractReceivedMessageImpl is TextMessage) {
                guild.getMember(user)?.nickname ?: user.name
            } else user.name
            tmp = tmp.replace(Regex("<@!?${Regex.escape(name)}>"), "@$name")
        }

        mentionedEmotes.forEach { emote ->
            tmp = tmp.replace(emote.asMention, ":${emote.name}:")
        }

        mentionedChannels.forEach { channel ->
            tmp = tmp.replace(channel.asMention, "#${channel.name}")
        }

        return tmp // TODO Rendered Content
    }
}
