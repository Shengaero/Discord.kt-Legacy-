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

import me.kgustave.dkt.entities.Snowflake
import me.kgustave.dkt.util.unmodifiableList
import java.util.*
import java.util.stream.Stream

/**
 * @author Kaidan Gustave
 */
class OrderedSnowflakeCache<S>(
    private val comparator: Comparator<S>,
    nameFunction: ((S) -> String?)? = null
): SnowflakeCacheImpl<S>(nameFunction) where S: Snowflake, S: Comparable<S> {

    override fun toList(): List<S> {
        val list = ArrayList<S>()
        list.addAll(entityMap.values)
        list.sortWith(comparator)
        return unmodifiableList(list)
    }

    override fun iterator(): Iterator<S> = toList().iterator()
    override fun spliterator(): Spliterator<S> = Spliterators.spliterator(toList(),
        Spliterator.IMMUTABLE xor Spliterator.ORDERED xor Spliterator.NONNULL)

    override fun stream(): Stream<S> = super.stream().sorted(comparator)
    override fun parallelStream(): Stream<S> = super.parallelStream().sorted(comparator)
}
