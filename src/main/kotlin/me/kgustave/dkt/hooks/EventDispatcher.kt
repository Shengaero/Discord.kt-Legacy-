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
package me.kgustave.dkt.hooks

import me.kgustave.dkt.entities.impl.APIImpl
import me.kgustave.dkt.events.Event

/**
 * @author Kaidan Gustave
 */
interface EventDispatcher {
    fun onEvent(event: Event)
    fun removeListener(listener: Any)
    fun addListener(listener: Any)

    operator fun minusAssign(listener: Any) = removeListener(listener)
    operator fun plusAssign(listener: Any) = addListener(listener)

    companion object DefaultEventDispatcher : EventDispatcher {
        private val listeners: MutableList<EventListener> = ArrayList()

        override fun onEvent(event: Event) {
            listeners.forEach {
                try {
                    it.onEvent(event)
                } catch(t: Throwable) {
                    APIImpl.LOG.warn("One of the EventListeners caught an exception:", t)
                }
            }
        }

        override fun removeListener(listener: Any) {
            if(listener is EventListener)
                listeners -= listener
        }

        override fun addListener(listener: Any) {
            listeners += requireNotNull(listener as? EventListener) {
                "${listener::class} must implement EventListener to be used as with this EventDispatcher!"
            }
        }
    }
}
