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
package me.kgustave.dkt.requests.ratelimits

import me.kgustave.dkt.requests.RestRequest
import me.kgustave.dkt.requests.Route
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @author Kaidan Gustave
 */
class Bucket(route: Route.FormattedRoute) {
    val endpoint = route.rateLimitEndpoint
    val rateLimit = route.base.rateLimit
    val queue = ConcurrentLinkedQueue<RestRequest<*>>()

    @Volatile var limit = rateLimit?.maxUses ?: 1
    @Volatile var remaining = rateLimit?.maxUses ?: 1
    @Volatile var reset = 0L // ms

    override fun equals(other: Any?): Boolean {
        if(other !is Bucket)
            return false
        return endpoint == other.endpoint
    }

    override fun hashCode(): Int = endpoint.hashCode()
    override fun toString(): String = "Bucket('$endpoint' [${queue.size}], $remaining/$limit, reset=${reset}ms)"
}
