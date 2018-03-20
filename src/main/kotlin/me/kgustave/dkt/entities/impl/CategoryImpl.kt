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
import me.kgustave.dkt.util.unmodifiableList

/**
 * @author Kaidan Gustave
 */
internal class CategoryImpl(
    api: APIImpl,
    id: Long,
    guild: GuildImpl
): AbstractGuildChannelImpl(api, id, guild, Channel.Type.CATEGORY), Category {
    internal val internalTextChannels = HashMap<Long, TextChannel>()
    internal val internalVoiceChannels = HashMap<Long, VoiceChannel>()

    override val category: Category? get() = null

    override val position: Int get() {
        guild.categories.forEachIndexed { i, cat ->
            if(cat == this) {
                return i
            }
        }
        // If we somehow reach here, we are not in the guild.
        throw IllegalStateException("Unable to determine category position in Guild (ID: ${guild.id})")
    }

    override val textChannels: List<TextChannel> get() {
        return unmodifiableList(*internalTextChannels.values.toTypedArray())
    }

    override val voiceChannels: List<VoiceChannel> get() {
        return unmodifiableList(*internalVoiceChannels.values.toTypedArray())
    }

    override fun compareTo(other: Category): Int {
        if(this == other)
            return 0

        require(guild == this.guild) { "Both categories must be from the same Guild!" }

        if(rawPosition != other.rawPosition) {
            return rawPosition - other.rawPosition
        }

        return other.creationTime.compareTo(creationTime)
    }

    override fun toString(): String = Snowflake.toString("Category", this)
}
