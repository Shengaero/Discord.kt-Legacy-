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
package me.kgustave.dkt.entities.caching

/**
 * @author Kaidan Gustave
 */
@Suppress("LoopToCallChain")
interface EntityCache<T> : Collection<T> {
    fun getByName(name: String, ignoreCase: Boolean = false): List<T>

    operator fun get(name: String): List<T> = getByName(name)

    fun toList(): List<T>

    override fun isEmpty(): Boolean = size == 0

    override fun containsAll(elements: Collection<T>): Boolean {
        elements.forEach { if(it !in this) return false }
        return true
    }
}
