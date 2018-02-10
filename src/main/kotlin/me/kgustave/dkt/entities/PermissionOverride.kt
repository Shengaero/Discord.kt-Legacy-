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

import me.kgustave.dkt.API
import me.kgustave.dkt.Permission

/**
 * @author Kaidan Gustave
 */
interface PermissionOverride {
    val guild: Guild
    val member: Member?
    val role: Role?
    val channel: GuildChannel
    val api: API

    val allowed: List<Permission>
        get() = Permission.fromRaw(allowedRaw)
    val denied: List<Permission>
        get() = Permission.fromRaw(deniedRaw)
    val effective: List<Permission>
        get() = Permission.fromRaw(effectiveRaw)

    val allowedRaw: Long
    val deniedRaw: Long
    val effectiveRaw: Long
        get() = (allowedRaw or deniedRaw).inv()
}

interface MemberPermissionOverride : PermissionOverride {
    override val member: Member
}

interface RolePermissionOverride : PermissionOverride {
    override val role: Role
}
