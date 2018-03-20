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
import me.kgustave.dkt.requests.Route
import me.kgustave.dkt.requests.promises.MessagePromise

/**
 * @author Kaidan Gustave
 */
class TextChannelImpl(
    id: Long,
    api: APIImpl,
    guild: GuildImpl
): AbstractGuildChannelImpl(api, id, guild, Channel.Type.TEXT), TextChannel {
    override var topic: String? = null
    override val asMention: String get() = "<#$id>"
    override val position: Int get() {
        guild.textChannels.forEachIndexed { i, tc ->
            if(tc == this)
                return i
        }
        // If we somehow reach here, we are not in the guild.
        throw IllegalStateException("Unable to determine text channel position in Guild (ID: ${guild.id})")
    }

    override fun send(text: String): MessagePromise {
        return MessagePromise(this, api, Route.CreateMessage.format(id), text)
    }

    override fun send(embed: Embed): MessagePromise {
        return MessagePromise(this, api, Route.CreateMessage.format(id)).also {
            it.embed = embed
        }
    }

    override fun send(message: Message): MessagePromise {
        return MessagePromise(this, api, Route.CreateMessage.format(id), message.content.takeIf { it.isNotBlank() })
    }

    override fun compareTo(other: TextChannel): Int {
        // Same channel
        if(this == other)
            return 0

        // Check to make sure they are both from the same Guild
        require(guild == other.guild) { "Both text channels must be from the same Guild!" }

        if(rawPosition != other.rawPosition) {
            return rawPosition - other.rawPosition
        }

        // We compare based on creation time because the more recently a text channel was created,
        // the lower it will be in the hierarchy.
        return other.creationTime.compareTo(creationTime)
    }

    override fun toString(): String = Snowflake.toString("TextChannel", this)
}
