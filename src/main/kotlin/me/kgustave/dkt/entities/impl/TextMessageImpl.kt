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
import me.kgustave.dkt.requests.RestPromise
import me.kgustave.dkt.requests.Route
import me.kgustave.dkt.requests.promises.MessagePromise
import me.kgustave.dkt.util.snowflake
import me.kgustave.dkt.util.unmodifiableList
import kotlin.LazyThreadSafetyMode.PUBLICATION

/**
 * @author Kaidan Gustave
 */
class TextMessageImpl(
    id: Long,
    api: APIImpl,
    type: Message.Type,
    author: User,
    embeds: List<Embed>,
    channel: TextChannel,
    attachments: List<Message.Attachment>,
    content: String,
    mentionedUserIds: Set<Long>,
    mentionedRoleIds: Set<Long>,
    override val isWebhook: Boolean,
    override val member: Member
): TextMessage,
    AbstractReceivedMessageImpl<TextChannel>(
        id, api, type, author, embeds,
        channel, attachments, mentionedUserIds,
        content
    ) {

    override val channelType: Channel.Type get() = Channel.Type.TEXT
    override val guild: Guild get() = channel.guild

    override val mentionedRoles: List<Role> by lazy {
        if(content.isBlank()) {
            return@lazy emptyList<Role>()
        }

        val mentions = ArrayList<Role>()
        val seq = Mentionable.Format.USER.regex.findAll(content)

        for(matchResult in seq) {
            try {
                val roleId = snowflake(matchResult.groupValues[1])
                if(roleId !in mentionedRoleIds) {
                    continue
                }

                guild.getRoleById(roleId)?.let {
                    mentions += it
                }
            } catch(ignored: NumberFormatException) {}
        }

        return@lazy unmodifiableList(mentions)
    }

    override val mentionedMembers: List<Member> by lazy(PUBLICATION) {
        return@lazy unmodifiableList(mentionedUsers.mapNotNull { guild.getMember(it) })
    }

    override fun edit(text: String): MessagePromise {
        check(author == api.self) {
            "Cannot edit message from a user that is not the same as the currently logged in user."
        }

        return MessagePromise(channel, api, Route.EditMessage.format(channel.id, id), text)
    }

    override fun edit(embed: Embed): MessagePromise {
        check(author == api.self) {
            "Cannot edit message from a user that is not the same as the currently logged in user."
        }

        return MessagePromise(channel, api, Route.EditMessage.format(channel.id, id)).also { it.embed = embed }
    }

    override fun edit(message: Message): MessagePromise {
        check(author == api.self) {
            "Cannot edit message from a user that is not the same as the currently logged in user."
        }

        return MessagePromise(api, Route.EditMessage.format(channel.id, id), message)
    }

    override fun delete(): RestPromise<Message> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun mentions(mentionable: Mentionable): Boolean {
        if(super.mentions(mentionable)) {
            return true
        }

        return when(mentionable) {
            is Member -> mentionable in mentionedMembers
            is Role -> mentionable in mentionedRoles

            else -> false
        }
    }
}
