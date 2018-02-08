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

/**
 * @author Kaidan Gustave
 */
class PrivateMessageImpl(
    override val id: Long,
    override val api: APIImpl,
    override val type: Message.Type,
    override val author: User,
    override val channel: PrivateChannel,
    override val content: String,
    override val embeds: List<Embed>,
    override val attachments: List<Message.Attachment>
): PrivateMessage, AbstractMessageImpl() {

    // Private Messages cannot be from Guilds
    override val member: Member?
        get() = null
    override val isWebhook: Boolean
        get() = false
    override val channelType: Channel.Type
        get() = Channel.Type.PRIVATE

    override fun edit(text: String): MessagePromise {
        return MessagePromise(channel, api, Route.EditMessage.format(channel.id, id), text)
    }

    override fun edit(embed: Embed): MessagePromise {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun edit(message: Message): MessagePromise {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(): RestPromise<Message> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean = other is PrivateMessage && Snowflake.equals(this, other)
    override fun toString(): String = Snowflake.toString("PrivateMessage", this)
}
