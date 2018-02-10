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
package me.kgustave.dkt.handlers

import me.kgustave.dkt.util.createLogger

/**
 * @author Kaidan Gustave
 */
class EventCache {
    companion object {
        private val LOG = createLogger(EventCache::class)
    }

    private val map = HashMap<EventCache.Type, HashMap<Long, ArrayList<() -> Unit>>>()

    val size: Int
        get() = map.values.sumBy { it.values.sumBy { it.size } }

    fun add(type: EventCache.Type, id: Long, function: () -> Unit) {
        val cache = map[type] ?: HashMap<Long, ArrayList<() -> Unit>>().also { map[type] = it }
        val functions = cache[id] ?: ArrayList<() -> Unit>().also { cache[id] = it }
        functions += function
    }

    fun run(type: EventCache.Type, id: Long) {
        val functions = map[type]?.get(id)
        if(functions !== null && functions.isNotEmpty()) {
            LOG.info("Replaying ${functions.size} events...")
            functions.onEach { it() }.clear()
        }
    }

    fun clear(type: EventCache.Type? = null, id: Long? = null) {
        if(type === null && id === null) {
            map.clear()
            LOG.debug("Cleared cache of all events")
        } else if(type !== null) {
            if(id === null) {
                map.remove(type)
                LOG.debug("Cleared cache of '$type' type events")
            } else {
                map[type]?.remove(id)
                LOG.debug("Cleared cache of '$type' type events with ID: $id")
            }
        } else if(id !== null) {
            map.values.forEach { it.remove(id) }
            LOG.debug("Cleared cache of events with ID: $id")
        }
    }

    enum class Type {
        USER, CHANNEL, GUILD, ROLE
    }
}
