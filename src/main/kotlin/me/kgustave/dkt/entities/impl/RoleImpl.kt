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
@file:Suppress("MemberVisibilityCanBePrivate")
package me.kgustave.dkt.entities.impl

import me.kgustave.dkt.API
import me.kgustave.dkt.Permission
import me.kgustave.dkt.entities.*
import java.awt.Color

/**
 * @author Kaidan Gustave
 */
class RoleImpl(
    override val id: Long,
    override val api: API,
    override val guild: Guild,
    override var name: String,
    override var color: Color?,
    override var isMentionable: Boolean,
    override var rawPosition: Int,
    override var rawPermissions: Long
): Role {
    private val effectivePermissions: Long
        get() = rawPermissions xor guild.everyoneRole.rawPermissions

    internal val internalPermissions = ArrayList<Permission>()

    override val isEveryoneRole = guild.id == id
    override val asMention: String
        get() = "<@&$id>"
    override val permissions: List<Permission>
        get() = internalPermissions
    override val position: Int get() {
        if(isEveryoneRole)
            return -1

        // subtract 1 for 0 indexed, and 1 again to disregard @everyone
        var i = guild.roleCache.size - 2

        guild.roleCache.forEach {
            if(it == this)
                return i
            i--
        }

        // If we somehow reach here, we are not in the guild.
        throw IllegalStateException("Unable to determine role position in Guild (ID: ${guild.id})")
    }

    override fun canInteract(member: Member): Boolean {
        TODO("not implemented")
    }

    override fun canInteract(role: Role): Boolean {
        TODO("not implemented")
    }

    override fun canInteract(emote: Emote): Boolean {
        TODO("not implemented")
    }

    override fun hasPermission(vararg permissions: Permission): Boolean {
        val effectivePermissions = effectivePermissions
        internalPermissions.forEach { perm ->
            val rawValue = perm.rawOffset
            if((effectivePermissions and rawValue) != rawValue)
                return false
        }

        return true
    }

    override fun hasPermission(channel: GuildChannel, vararg permissions: Permission): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun compareTo(other: Role): Int {
        if(this == other) // Same role as this
            return 0

        // Check to make sure they are both from the same Guild
        require(guild == other.guild) { "Both roles must be from the same Guild!" }

        if(rawPosition != other.rawPosition)
            return rawPosition - other.rawPosition

        // We compare based on creation time because the more recently a role was created,
        // the lower it will be in the hierarchy.
        return other.creationTime.compareTo(creationTime)
    }

    override fun hashCode(): Int = (guild.id + id).hashCode()
    override fun equals(other: Any?): Boolean = other is Role && Snowflake.equals(this, other)
}
