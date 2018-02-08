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
@file:Suppress("MemberVisibilityCanPrivate", "MemberVisibilityCanBePrivate", "Unused")
package me.kgustave.dkt.util.queue

import me.kgustave.dkt.entities.impl.APIImpl
import me.kgustave.dkt.util.createLogger
import me.kgustave.kson.KSONObject
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

/**
 * A Guild ID, [RawEventQueue] collection for properly awaiting
 * a [Guild][me.kgustave.dkt.entities.Guild]'s validation
 * before firing events related to it.
 *
 * This is primarily used before the [API][me.kgustave.dkt.API]
 * is [connected][me.kgustave.dkt.API.Status.CONNECTED], but
 * can also be used when the API has been suddenly
 * [disconnected][me.kgustave.dkt.API.Status.DISCONNECTED].
 *
 * @since  1.0.0
 * @author Kaidan Gustave
 */
class GuildEventQueue(private val api: APIImpl) {
    companion object {
        private val LOG = createLogger(GuildEventQueue::class)
    }

    private val registeredQueues = HashMap<Long, Queue<KSONObject>>()
    private val registered = HashSet<Long>()

    fun isRegistered(guildId: Long): Boolean = registered.contains(guildId)

    fun register(guildId: Long) {
        if(!isRegistered(guildId)) {
            LOG.debug("Queuing Guild with ID: $guildId")
            registered.add(guildId)
            registeredQueues[guildId] = RawEventQueue()
        }
    }

    fun unregister(guildId: Long) {
        if(isRegistered(guildId)) {
            LOG.debug("De-Queuing Guild with ID: $guildId")
            registered.remove(guildId)
            val events = registeredQueues[guildId]!!
            if(events.isNotEmpty()) {
                LOG.debug("Replaying ${events.size} events queued from Guild (ID: $guildId)")
                api.websocket.handle(events)
                LOG.debug("Done replaying events from Guild (ID: $guildId)")
            }
        }
    }

    fun queue(guildId: Long, event: KSONObject) {
        if(isRegistered(guildId)) {
            LOG.debug("Queuing event for Guild (ID: $guildId): $event")

            // We don't call null-safety on this because we should never have a time
            // where this can fail and we are not notified. If in the event this does
            // one day throw an NPE, we will be there to see it.
            registeredQueues[guildId]!!.offer(event)
        }
    }

    fun clear() {
        if(registeredQueues.isNotEmpty())
            LOG.debug("Clearing GuildEventQueue with ${registeredQueues.size} remaining queued.")
        registered.clear()
        registeredQueues.clear()
    }
}
