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
package me.kgustave.dkt.handlers

import kotlinx.coroutines.experimental.*
import me.kgustave.dkt.entities.impl.APIImpl
import me.kgustave.dkt.requests.DiscordWebSocket
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

typealias DefaultSessionManager = SessionManager.Default

/**
 * An implementable interface for safely
 *
 * @since  1.0.0
 * @author Kaidan Gustave
 */
interface SessionManager {
    fun add(websocket: DiscordWebSocket)
    fun remove(websocket: DiscordWebSocket)
    fun process()

    fun stop() {
        // An implementations might not require this
        // by default, it is empty, so they don't need
        // to override it
    }

    class Default : SessionManager {
        private val coroutineContext = newSingleThreadContext("SessionManager")

        @Volatile private var lock = false
        @Volatile private var queue: BlockingQueue<DiscordWebSocket> = LinkedBlockingQueue()

        override fun add(websocket: DiscordWebSocket) {
            if(!queue.offer(websocket))
                throw IllegalAccessException("Session was rejected by SessionManager")
        }

        override fun remove(websocket: DiscordWebSocket) {
            queue.remove(websocket)
        }

        override fun process() {
            // If there is a lock, the previously started reconnect job
            // will take care of it, no need to create a new job.
            if(!lock) {
                lock = true
                val job = launch(coroutineContext, start = CoroutineStart.LAZY) {
                    var first = true
                    while(queue.isNotEmpty()) {
                        val client = queue.poll()

                        client.reconnect(true, first)

                        first = false

                        if(!queue.isNotEmpty()) {
                            // We need to wait until we've identified
                            while(!client.api.status.hasIdentified)
                                delay(50)

                            delay(DiscordWebSocket.IDENTIFY_DELAY.toLong(), TimeUnit.SECONDS)
                        }
                    }
                }

                // In the event this process might be cancelled due to... Something,
                // we need to make sure that the lock ALWAYS unlocks (even on cancellation).
                // If this is cancelled, something REALLY broke (possibly even kotlin-internal
                // levels).
                job.invokeOnCompletion(onCancelling = true) {
                    if(it != null) {
                        APIImpl.LOG.warn("SessionManager had a job that ended due to " +
                                         "exception! Please report this to the developers!", it)
                    }
                    lock = false
                }

                job.start()
            }
        }

        override fun stop() {
            coroutineContext.cancelChildren() // Make sure to cancel a child process if this is active.
            coroutineContext.cancel()
            coroutineContext.close()
        }
    }
}
