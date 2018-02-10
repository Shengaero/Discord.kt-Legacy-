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

/**
 * @since  1.0.0
 * @author Kaidan Gustave
 */
interface Channel : Snowflake {

    /** The [Type] of Channel. */
    val type: Type

    /**
     * The [Guild] this Channel is from, or `null` if this Channel
     * is not from a Guild.
     *
     * @see GuildChannel
     */
    val guild: Guild?

    /** `true` if this Channel is a [GuildChannel], `false` otherwise. */
    val isGuildChannel: Boolean
        get() = guild == null

    /**
     * Constants defining the specific type of [Channel].
     *
     * @since  1.0.0
     * @author Kaidan Gustave
     */
    enum class Type(val number: Int, val isGuild: Boolean = false) {
        /** Type constant corresponding to a [TextChannel]. */
        TEXT(0, true),

        /** Type constant corresponding to a [PrivateChannel]. */
        PRIVATE(1),

        /** Type constant corresponding to a [VoiceChannel]. */
        VOICE(2, true),

        // Unsupported, bots cannot use groups.
        // If bots are allowed to use groups this will be implemented, otherwise
        // it will be left commented out.
        // GROUP(3, false)

        /** Type constant corresponding to a [Category]. */
        CATEGORY(4, true),

        UNKNOWN(-1);
        companion object {
            fun typeOf(number: Int): Type? = values().firstOrNull { number == it.number }
        }
    }
}

/**
 * @since  1.0.0
 * @author Kaidan Gustave
 */
interface GuildChannel : Channel {
    val name: String
    val position: Int
    val rawPosition: Int
    val category: Category?

    val permissionOverrides: List<PermissionOverride>
    val memberPermissionOverrides: List<MemberPermissionOverride>
    val rolePermissionOverrides: List<RolePermissionOverride>

    /** The [Guild] this Channel is from, never-null. */
    override val guild: Guild

    fun getPermissionOverride(member: Member): MemberPermissionOverride?
    fun getPermissionOverride(role: Role): RolePermissionOverride?
}
