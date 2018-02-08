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
@file:Suppress("Unused")
package me.kgustave.dkt.requests

import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.delay
import me.kgustave.dkt.Discord
import me.kgustave.dkt.entities.impl.APIImpl
import me.kgustave.dkt.requests.ratelimits.GlobalShardRateLimiter
import me.kgustave.dkt.requests.ratelimits.RateLimiter
import me.kgustave.dkt.util.await
import me.kgustave.dkt.util.createLogger
import me.kgustave.dkt.util.method
import me.kgustave.dkt.util.set
import me.kgustave.dkt.util.request
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.Response
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * @author Kaidan Gustave
 */
class Requester constructor(internal val api: APIImpl) {
    companion object {
        val DEFAULT_BODY = RequestBody.create(null, byteArrayOf())!!
        val LOGGER = createLogger(Requester::class)
        val MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8")!!

        @JvmStatic val USER_AGENT = "DiscordBot (${Discord.KtInfo.GITHUB}, ${Discord.KtInfo.VERSION})"
    }

    private val rateLimiter = RateLimiter(this, 5, GlobalShardRateLimiter())
    private val httpClient = api.httpClientBuilder.build()

    fun <T> queue(request: RestRequest<T>): Deferred<T> {
        if(rateLimiter.isShutdown)
            throw IllegalStateException("Attempted to submit a request when the RateLimiter was shutdown!")
        rateLimiter.queue(request)
        return request.deferred
    }

    fun shutdown(delay: Long, unit: TimeUnit) {
        rateLimiter.shutdown(delay, unit)
    }

    fun shutdownNow() {
        rateLimiter.shutdownNow()
    }

    // Returns true if we should retry the request, false if we should not
    // Note: not retrying does not indicate the request succeeded in any way
    suspend fun <T> execute(request: RestRequest<T>, handleRateLimit: Boolean = false, retried: Boolean = false): Boolean {
        val rateLimit = rateLimiter.getRateLimitFor(request.route)
        if(rateLimit > 0) {
            if(handleRateLimit) {
                request.handle(RestResponse.rateLimit(rateLimit, emptySet()))
                return false
            }
            return true
        }

        val route = request.route
        val okRequest = createRequest(request, route)
        val rays = LinkedHashSet<String>()
        val attempts = arrayOfNulls<Response>(4)
        var successful: Response? = null

        try {
            var attempt = 0

            do {
                // The request was cancelled
                if(request.isCancelled)
                    return false

                successful = httpClient.newCall(okRequest).await()
                attempts[attempt] = successful
                successful.header("CF-RAY")?.let { rays += it }

                if(successful.code() < 500)
                    break // We succeeded in our request

                attempt++
                LOGGER.debug("Request (attempt ${attempt + 1}) ${request.route} was unsuccessful. Retrying...")

                // Wait a bit
                delay(50L * attempt)
            } while(attempt < 3 && successful!!.code() >= 500)

            successful!! // Assert past here that it's not null

            // At this point we've attempted 4 times with no success
            if(successful.code() >= 500) {
                LOGGER.debug("Attempted request ${request.route} 4 times without success.\n" +
                             "This could be due to Discord API issue such as an outage. If " +
                             "you see this consistently for a long time, you should contact " +
                             "the library maintainers!")
                return false
            }

            val retryAfter = rateLimiter.handleResponse(request.route, successful)

            if(rays.isNotEmpty())
                LOGGER.debug("Received a response with the following CloudFlare rays: [${rays.joinToString()}]")

            request.handle(RestResponse(successful, retryAfter ?: -1L, rays))
            return false
        } catch(e: SocketTimeoutException) {
            if(!retried) { // Try again
                LOGGER.debug("Requester experienced a timeout, will retry again...")
                return true
            }

            LOGGER.error("Requester experienced a timeout after retrying.")

            request.handle(RestResponse(successful, e, rays))
            return false
        } catch(e: Exception) {
            LOGGER.error("Requester experienced an exception while executing a request!", e)
            request.handle(RestResponse(successful, e, rays))
            return false
        } finally {
            for(res in attempts) {
                if(res == null)
                    break
                res.close()
            }
        }
    }

    private fun createRequest(request: RestRequest<*>, route: Route.FormattedRoute) = request {
        url("${Route.BASE_URL}${route.formattedEndpoint}")
        method(route.method, if(request.body == null && route.method.requiresBody) DEFAULT_BODY else request.body)

        this["user-agent"] = USER_AGENT
        this["accept-encoding"] = "gzip"
        this["authorization"] = "Bot ${api.token}"

        request.headers?.let {
            for((key, value) in it)
                this[key] = value.toString()
        }
    }
}
