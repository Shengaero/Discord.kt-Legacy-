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
package me.kgustave.dkt.entities.caching.impl

import me.kgustave.dkt.entities.Member
import me.kgustave.dkt.entities.Role
import me.kgustave.dkt.entities.caching.MemberCache
import me.kgustave.dkt.util.unmodifiableList
import java.util.*

/**
 * @author Kaidan Gustave
 */
@Suppress("LoopToCallChain")
class MemberCacheImpl : MemberCache, AbstractEntityCache<Member>(Member::name) {
    override fun getById(id: Long): Member? = entityMap[id]

    override fun getByNickname(name: String, ignoreCase: Boolean): List<Member> {
        val members = LinkedList<Member>()

        entityMap.values.forEach { member ->
            if(name.equals(member.nickname, ignoreCase))
                members += member
        }

        return unmodifiableList(members)
    }

    override fun getByUsername(name: String, ignoreCase: Boolean): List<Member> {
        val members = LinkedList<Member>()

        entityMap.values.forEach { member ->
            if(name.equals(member.user.name, ignoreCase))
                members += member
        }

        return unmodifiableList(members)
    }

    override fun getWithRoles(vararg roles: Role): List<Member> {
        val members = LinkedList<Member>()

        entityMap.values.forEach m@ { member ->
            val memberRoles = member.roles
            roles.forEach { role ->
                if(role !in memberRoles)
                    return@m
            }
            members += member
        }

        return unmodifiableList(members)
    }

    override fun getWithRoles(roles: Collection<Role>): List<Member> {
        val members = LinkedList<Member>()

        entityMap.values.forEach {
            if(it.roles.containsAll(roles))
                members += it
        }

        return unmodifiableList(members)
    }

    override fun get(id: Long): Member? = getById(id)
}
