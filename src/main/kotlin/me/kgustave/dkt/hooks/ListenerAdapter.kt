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

import me.kgustave.dkt.events.DisconnectEvent
import me.kgustave.dkt.events.Event
import me.kgustave.dkt.events.ReadyEvent
import me.kgustave.dkt.events.ShutdownEvent

/**
 * @since  1.0.0
 * @author Kaidan Gustave
 */
abstract class ListenerAdapter : EventListener {
    final override fun onEvent(event: Event) {
        when(event) {
            is ReadyEvent -> onReady(event)
            is DisconnectEvent -> onDisconnect(event)
            is ShutdownEvent -> onShutdown(event)
        }
    }

    open fun onReady(event: ReadyEvent) {}
    open fun onDisconnect(event: DisconnectEvent) {}
    open fun onShutdown(event: ShutdownEvent) {}
}
