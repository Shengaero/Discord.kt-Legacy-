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
abstract class AbstractGuildChannelImpl(
    override val api: APIImpl,
    override val id: Long,
    override val guild: GuildImpl,
    final override val type: Channel.Type // Fuck you intellij...
): GuildChannel {
    internal val internalMemberOverrides = HashMap<Long, MemberPermissionOverride>()
    internal val internalRoleOverrides = HashMap<Long, RolePermissionOverride>()
    internal var parentId = 0L

    override lateinit var name: String
    override var rawPosition = 0

    override val category: Category? get() = guild.getCategoryById(parentId)

    override val permissionOverrides: List<PermissionOverride> get() {
        val members = internalMemberOverrides.values.toTypedArray()
        val roles = internalRoleOverrides.values.toTypedArray()
        return unmodifiableList(*members, *roles)
    }

    override val memberPermissionOverrides: List<MemberPermissionOverride> get() {
        return unmodifiableList(*internalMemberOverrides.values.toTypedArray())
    }

    override val rolePermissionOverrides: List<RolePermissionOverride> get() {
        return unmodifiableList(*internalRoleOverrides.values.toTypedArray())
    }


    init {
        require(type.isGuild) { "Specified type '$type' is not a guild channel type" }
    }

    override fun getPermissionOverride(member: Member): MemberPermissionOverride? {
        return internalMemberOverrides.values.find { it.member == member }
    }

    override fun getPermissionOverride(role: Role): RolePermissionOverride? {
        return internalRoleOverrides.values.find { it.role == role }
    }

    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean = other is PrivateChannel && Snowflake.equals(this, other)
}
