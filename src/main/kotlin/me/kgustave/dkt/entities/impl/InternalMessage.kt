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

import me.kgustave.dkt.API
import me.kgustave.dkt.entities.*
import me.kgustave.dkt.requests.RestPromise
import me.kgustave.dkt.requests.promises.MessagePromise
import me.kgustave.dkt.util.singletonList
import me.kgustave.dkt.util.unsupported

/**
 * @author Kaidan Gustave
 */
class InternalMessage
@PublishedApi internal constructor(content: String, private val embed: Embed?): AbstractMessageImpl(content) {
    private companion object {
        private const val UNSUPPORTED_ERROR = "not supported for messages built by a message-builder!"
    }

    override val type: Message.Type get() = Message.Type.DEFAULT
    override val embeds: List<Embed> get() = embed?.let { singletonList(it) } ?: emptyList()

    override val id: Long get() = unsupported { "Message IDs are $UNSUPPORTED_ERROR" }
    override val api: API get() = unsupported { "API instances are $UNSUPPORTED_ERROR" }
    override val channel: MessageChannel get() = unsupported { "MessageChannels are $UNSUPPORTED_ERROR" }
    override val channelType: Channel.Type get() = unsupported { "ChannelTypes are $UNSUPPORTED_ERROR" }
    override val author: User get() = unsupported { "Authors are $UNSUPPORTED_ERROR" }
    override val member: Member? get() = unsupported { "Members are $UNSUPPORTED_ERROR" }
    override val attachments: List<Message.Attachment> get() = unsupported { "Attachments are $UNSUPPORTED_ERROR" }
    override val isWebhook: Boolean get() = unsupported { "Webhooks are $UNSUPPORTED_ERROR" }
    override val renderedContent: String get() = unsupported { "Rendered content is $UNSUPPORTED_ERROR" }
    override val mentionedUsers: List<User> get() = unsupported { "Mentioned users are $UNSUPPORTED_ERROR" }
    override val mentionedEmotes: List<Emote> get() = unsupported { "Mentioned emotes are $UNSUPPORTED_ERROR" }
    override val mentionedChannels: List<TextChannel> get() = unsupported { "Mentioned channels are $UNSUPPORTED_ERROR" }

    override fun edit(text: String): MessagePromise {
        noEdit()
    }

    override fun edit(embed: Embed): MessagePromise {
        noEdit()
    }

    override fun edit(message: Message): MessagePromise {
        noEdit()
    }

    override fun delete(): RestPromise<Message> {
        unsupported { "Deleting is $UNSUPPORTED_ERROR" }
    }

    override infix fun mentions(mentionable: Mentionable): Boolean {
        unsupported { "Mentions are $UNSUPPORTED_ERROR" }
    }

    private fun noEdit(): Nothing = unsupported { "Editing is $UNSUPPORTED_ERROR" }
}
