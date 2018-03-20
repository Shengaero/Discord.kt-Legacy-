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

import me.kgustave.dkt.entities.caching.EntityCache
import me.kgustave.dkt.util.doNotSupport
import me.kgustave.dkt.util.unmodifiableList
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * An abstract implementation of a [EntityCache].
 *
 * @since  1.0.0
 * @author Kaidan Gustave
 */
@Suppress("LoopToCallChain") // Kotlin pls
abstract class AbstractEntityCache<T>(private val nameFunction: ((T) -> String?)?): EntityCache<T> {

    val entityMap: MutableMap<Long, T> = HashMap()

    override val size: Int
        get() = entityMap.size

    override fun contains(element: T): Boolean = entityMap.containsValue(element)
    override fun iterator(): Iterator<T> = entityMap.values.iterator()
    override fun toList(): List<T> {
        val list = ArrayList<T>(size)
        list.addAll(entityMap.values)
        return unmodifiableList(list)
    }

    override fun getByName(name: String, ignoreCase: Boolean): List<T> {
        doNotSupport(nameFunction === null) { "Getting entities from this cache by name is unsupported." }

        if(entityMap.isEmpty())
            return emptyList()

        val list = LinkedList<T>()

        entityMap.values.forEach { entity ->
            if(name.equals(nameFunction!!(entity), ignoreCase))
                list += entity
        }

        return unmodifiableList(list)
    }
}
