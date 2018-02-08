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
package me.kgustave.dkt.util.queue

import me.kgustave.kson.KSONObject
import java.util.*

/**
 * Simplistic [Queue][java.util.Queue] implementation
 * for queuing raw Discord WebSocket events.
 *
 * @since  1.0.0
 * @author Kaidan Gustave
 */
class RawEventQueue : AbstractQueue<KSONObject>() {
    private val list: MutableList<KSONObject> = LinkedList()
    override val size: Int
        get() = list.size

    override fun iterator(): MutableIterator<KSONObject> = list.iterator()
    override fun poll(): KSONObject? = if(list.isEmpty()) null else list.removeAt(0)
    override fun peek(): KSONObject? = if(list.isEmpty()) null else list[0]
    override fun offer(e: KSONObject): Boolean = list.add(e)
}
