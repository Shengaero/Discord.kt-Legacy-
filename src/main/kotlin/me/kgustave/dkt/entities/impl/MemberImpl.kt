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
package me.kgustave.dkt.entities.impl

import me.kgustave.dkt.API
import me.kgustave.dkt.Permission
import me.kgustave.dkt.entities.*
import me.kgustave.dkt.util.unmodifiableList
import java.util.Comparator

/**
 * @author Kaidan Gustave
 */
class MemberImpl(override val api: API, override val guild: Guild, override val user: User): Member {
    internal val internalRoles = ArrayList<Role>()

    override var nickname: String? = null

    override val name: String
        get() = nickname ?: username
    override val username: String
        get() = user.name
    override val roles: List<Role>
        get() = unmodifiableList(internalRoles.sortedWith(Comparator.reverseOrder()))
    override val permissions: List<Permission> get() {
        val dest = ArrayList<Permission>()
        internalRoles.forEach {
            it.permissions.forEach {
                if(it !in dest) // Not in destination
                    dest += it
            }
        }
        return dest
    }

    override fun canInteract(member: Member): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun canInteract(role: Role): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun canInteract(emote: Emote): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasPermission(vararg permissions: Permission): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hasPermission(channel: GuildChannel, vararg permissions: Permission): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun compareTo(other: Member): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
