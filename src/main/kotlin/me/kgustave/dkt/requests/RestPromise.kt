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
@file:Suppress("MemberVisibilityCanBePrivate", "unused")
package me.kgustave.dkt.requests

import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.launch
import me.kgustave.dkt.entities.impl.APIImpl
import me.kgustave.dkt.util.HeaderMap
import me.kgustave.dkt.util.createLogger
import me.kgustave.json.JSObject
import okhttp3.RequestBody
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * @author Kaidan Gustave
 */
abstract class RestPromise<T>(protected val api: APIImpl, protected open val route: Route.FormattedRoute) {
    companion object {
        internal val LOG = createLogger(RestPromise::class)
        private val DEFAULT_FAILURE: (Throwable) -> Unit = {
            LOG.error("Promise could not be completed due to a failed execution: $it")
        }

        fun <T> simple(
            api: APIImpl, route: Route.FormattedRoute, body: JSObject? = null, headers: HeaderMap? = null,
            block: suspend RestPromise<T>.(RestResponse, RestRequest<T>) -> Unit
        ): RestPromise<T> = object : RestPromise<T>(api, route) {
            override val body = body?.let { RequestBody.create(Requester.MEDIA_TYPE_JSON, "$it") }
            override val headers = headers

            override suspend fun handle(response: RestResponse, request: RestRequest<T>) {
                block(response, request)
            }
        }
    }

    protected open val body: RequestBody? get() = null
    protected open val headers: HeaderMap? get() = null

    infix fun then(then: (T) -> Unit) = promise(then, DEFAULT_FAILURE)
    infix fun catch(catch: (Throwable) -> Unit) = promise({}, catch)

    fun promise(then: (T) -> Unit = {}) = promise(then, DEFAULT_FAILURE)

    fun promise(then: (T) -> Unit, catch: (Throwable) -> Unit) {
        val job = launch(api.context, CoroutineStart.LAZY) {
            val result = await()
            // We should always assume that the person invoking RestPromise#promise()
            // intends to use these methods as if they were a normal callback in a
            // typical java library. This means we should always run this as if they
            // intend to block the thread inside, or otherwise perform blocking/non-coroutine
            // based operations. As such, the safest way to invoke a blocking operation
            // in relation to coroutines is to run it in a suspendCoroutine block.
            suspendCoroutine<Unit> { cont ->
                try {
                    cont.resume(then(result))
                } catch(e: Throwable) {
                    cont.resumeWithException(e)
                }
            }
        }
        job.invokeOnCompletion { t -> if(t !== null) catch(t) }
        job.start()
    }

    open fun complete(): T {
        val completion = CompletableFuture<T>()

        promise(then = { completion.complete(it) }, catch = { completion.completeExceptionally(it) })

        try {
            return completion.get()
        } catch(e: Exception) {
            throw RuntimeException(e)
        }
    }

    open fun async(): Deferred<T> {
        return api.requester.queue(RestRequest(this, route, CompletableDeferred(), body, headers))
    }

    suspend fun await(): T = async().await()

    abstract suspend fun handle(response: RestResponse, request: RestRequest<T>)
}
