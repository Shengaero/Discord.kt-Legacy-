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
@file:Suppress("MemberVisibilityCanBePrivate")
package me.kgustave.dkt.requests.ratelimits

import kotlinx.coroutines.experimental.*
import me.kgustave.dkt.requests.Requester
import me.kgustave.dkt.requests.RestRequest
import me.kgustave.dkt.requests.RestResponse
import me.kgustave.dkt.requests.Route
import me.kgustave.dkt.util.currentTime
import me.kgustave.kson.KSONObject
import me.kgustave.kson.KSONTokener
import okhttp3.Headers
import okhttp3.Response
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.math.max

/**
 * @author Kaidan Gustave
 */
class RateLimiter(private val requester: Requester, poolSize: Int, global: GlobalShardRateLimiter) {
    companion object {
        private const val DEFAULT_OFFSET = -1L
    }

    /*volatile*/ private var global: Long by global
    @Volatile    private var shuttingDown = false

    private val pool = ScheduledThreadPoolExecutor(poolSize, RateLimitThreadFactory())
    private val context = pool.asCoroutineDispatcher()
    private val buckets = mutableMapOf<String, Bucket>()
    private val running = mutableMapOf<Bucket, Job>()

    val isShutdown: Boolean
        get() = pool.isShutdown

    // If this is -1L, it means we haven't initialized it yet
    var offset = DEFAULT_OFFSET

    inline val time: Long
        inline get() = currentTime + max(offset, 0L)

    fun <T> queue(request: RestRequest<T>) {
        check()
        val bucket = getBucket(request.route)
        synchronized(bucket) {
            val isRunning = synchronized(bucket.queue) isRunning@ {
                val r = bucket.queue.isNotEmpty()
                bucket.queue.add(request)
                return@isRunning r
            }
            if(!isRunning) {
                running[bucket] = start(bucket, context)
            }
        }
    }

    fun getRateLimitFor(route: Route.FormattedRoute): Long {
        val bucket = getBucket(route)
        synchronized(bucket) {
            if(global > 0) {
                if(time > global) {
                    // Now done with global cooldown
                    global = -1L
                } else {
                    // Not done with global cooldown,
                    // return global cooldown
                    return global
                }
            }

            // We have no more uses left
            if(bucket.remaining <= 0) {
                // The current time is past the last recorded reset time
                if(time > bucket.reset) {
                    bucket.remaining = bucket.limit
                }
            }

            return bucket.reset - time
        }
    }

    private fun getBucket(route: Route.FormattedRoute): Bucket {
        return synchronized(buckets) {
            buckets[route.rateLimitEndpoint] ?: Bucket(route).also {
                buckets[route.rateLimitEndpoint] = it
            }
        }
    }

    fun handleResponse(route: Route.FormattedRoute, res: Response): Long? {
        val bucket = getBucket(route)
        synchronized(bucket) {
            val headers = res.headers()

            // We run this to give ourselves the proper offset from Discord's time.
            if(offset == DEFAULT_OFFSET) {
                headers["Date"]?.let { date ->
                    val dateTime = OffsetDateTime.parse(date, DateTimeFormatter.RFC_1123_DATE_TIME)
                    offset = currentTime - dateTime.toInstant().toEpochMilli()

                    Requester.LOGGER.debug("Set RateLimiter time offset to $offset ms")
                }
            }

            // We've just been ratelimited.
            // This is bad, but the best thing we can do now is respect
            // the API's guidelines and retry in a bit.
            if(res.code() == 429) {
                val isGlobal = headers["X-RateLimit-Global"]?.toBoolean()
                val retryAfter = headers["Retry-After"]?.takeIf { it.isNotEmpty() }?.toLong() ?:
                                 RestResponse.validEncodedBody(res)?.use {
                                     val rateLimitKSON = KSONObject(KSONTokener(it))
                                     rateLimitKSON["retry_after"].toString()
                                 }?.toLong() ?: 0L

                // Global ratelimit
                if(isGlobal == true) {
                    // Set the global ratelimit
                    Requester.LOGGER.debug("Encountered a global rate limit, locking up all queued requests...")
                    global = time + retryAfter
                }

                bucket.reset = time + retryAfter
                bucket.remaining = 0

                return retryAfter
            }

            update(bucket, headers)
            return null
        }
    }

    fun shutdown(delay: Long, unit: TimeUnit) {
        shuttingDown = true
        context.cancelChildren()
        context.cancel()
        pool.setKeepAliveTime(delay, unit)
        pool.allowCoreThreadTimeOut(true)
    }

    fun shutdownNow() {
        shuttingDown = true
        context.cancelChildren()
        context.cancel()
        pool.shutdownNow()
    }

    private fun check() {
        if(isShutdown || shuttingDown)
            throw RejectedExecutionException("Cannot queue requests while RateLimiter is closing or shutdown!")
    }

    private fun update(bucket: Bucket, headers: Headers) {
        try {
            // Very important!
            // Because of the way Discord HTTP request headers are provided,
            // discord rounds all rate-limit headers to the whole second when
            // sending a HTTP response.
            // What this means is that if discord has say... A 250 ms rate limit
            // on a request, it will send 1 second as the X-RateLimit-Reset.
            // This means that in order to respect Discord's rate limits while
            // simultaneously not limiting ourselves to just what they provide,
            // we must be able to hard code some rate limits in ourselves.
            if(bucket.rateLimit !== null) {
                // The current time plus the hardcoded reset time
                bucket.reset = time + bucket.rateLimit.resetTime

                // Unlike the else block below, we don't need to set
                // the limit because we have already hardcoded it in
                // once.
            } else {
                headers["X-RateLimit-Reset"]?.toLong()?.let {
                    bucket.reset = it * 1000L // Seconds -> Milliseconds
                }
                headers["X-RateLimit-Limit"]?.toInt()?.let {
                    bucket.limit = it
                }
            }

            // We check and set the remaining uses just in case
            headers["X-RateLimit-Remaining"]?.toInt()?.let {
                bucket.remaining = it
            }

            Requester.LOGGER.debug("Updated $bucket")
        } catch(e: NumberFormatException) {
            if(bucket.endpoint != "gateway" && bucket.endpoint != "users/@me") {
                Requester.LOGGER.debug(
                    "Encountered an error updating bucket with headers:\n" +
                    "Route: ${bucket.endpoint}\n" +
                    "Headers: $headers"
                )
            }
        }
    }

    private fun start(bucket: Bucket, context: CoroutineContext): Job = launch(context) {
        while(bucket.queue.isNotEmpty()) {
            delay(bucket)
            val request = bucket.queue.peek() ?: break
            if(requester.execute(request, retried = false)) {
                if(!requester.execute(request, retried = true)) {
                    Requester.LOGGER.warn("Failed to execute request after retrying")
                }
            }
            bucket.queue.poll()
        }

        synchronized(buckets) {
            running.remove(bucket)
            if(bucket.queue.isNotEmpty()) {
                running[bucket] = start(bucket, coroutineContext)
            }
        }
    }

    private suspend fun delay(bucket: Bucket) {
        if(bucket.remaining <= 0) {
            // The current time is past the last recorded reset time
            if(bucket.reset > time) {
                delay(bucket.reset - time, TimeUnit.MILLISECONDS)
            }
        }

        if(time < global) {
            delay(global - time, TimeUnit.MILLISECONDS)
        }
    }

    private inner class RateLimitThreadFactory : ThreadFactory {
        private val threadNo = AtomicInteger(1)
        override fun newThread(r: Runnable) = thread(
            name = "${requester.api.identifier} RateLimiter - Thread ${threadNo.getAndIncrement()}",
            isDaemon = true,
            start = false
        ) { r.run() }
    }
}
