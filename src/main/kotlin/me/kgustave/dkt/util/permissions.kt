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
@file:[JvmName("PermissionUtil")]
package me.kgustave.dkt.util

// File Note:

// Due to the way that kotlin handle's receiver types, I decided to set this up as
// a set of methods that take two parameters as opposed to extending one as the receiver
// for the target parameter (IE: "xCanInteract(x: X, target: Y)" as opposed to
// "X.canInteract(target: Y)"). Also note that depending on the final product of the majority
// of entities in Kotlincord, this may be disappear in favor of simply having member functions
// call the code directly as opposed to statically.

import me.kgustave.dkt.entities.Member
import me.kgustave.dkt.entities.Role

/**
 * Checks whether the issuing [member] can interact with the [target] [Member].
 *
 * @param member The issuing [Member].
 * @param target The target [Member].
 *
 * @return Based on the following procedure:
 * 1) `true` if the [member] is the [guild owner][me.kgustave.dkt.entities.Guild.owner].
 * 2) `false` if the [target] is the [guild owner][me.kgustave.dkt.entities.Guild.owner].
 * 3) `true` if the [member] has at least one [Role], and either the target has no Roles *or*
 *    the member's highest Role [can interact][roleCanInteract] with the target's highest Role.
 *
 * @throws IllegalArgumentException If the [member] and [target] are not from the same
 *                                  [Guild][me.kgustave.kcord.entities.Guild]
 */
fun memberCanInteract(member: Member, target: Member): Boolean {
    val guild = member.guild

    require(guild == target.guild) { "Both members must be from the same Guild!" }

    // Member is owner
    if(guild.owner == member)
        return true

    // Target is owner
    if(guild.owner == target)
        return false

    val memberRoles = member.roles
    val targetRoles = target.roles

    // Interaction is based off of:
    // 1) The member has at least one role
    // 2) The target has no roles OR the member's highest role can interact with the target's highest role.
    return memberRoles.isNotEmpty() && (targetRoles.isEmpty() || roleCanInteract(memberRoles[0], targetRoles[0]))
}

fun memberCanInteract(member: Member, target: Role): Boolean {
    val guild = member.guild

    require(guild == target.guild) { "Both the member and role must be from the same Guild!" }

    // Member is owner
    if(guild.owner == member)
        return true

    val memberRoles = member.roles

    // Interaction is based off of:
    // 1) The member has at least one role
    // 2) Member's highest role can interact with the target role.
    return memberRoles.isNotEmpty() && roleCanInteract(memberRoles[0], target)
}

fun roleCanInteract(role: Role, target: Role): Boolean {
    require(role.guild == target.guild) { "Both roles must be from the same Guild!" }

    // Interaction is based off of:
    // 1) The role's position is greater than the target's position
    return role.position > target.position
}

private fun applies(permissions: Long, others: Long): Boolean = (permissions and others) == others

private fun apply(perm: Long, allow: Long, deny: Long): Long = (perm and deny.inv()) or allow
