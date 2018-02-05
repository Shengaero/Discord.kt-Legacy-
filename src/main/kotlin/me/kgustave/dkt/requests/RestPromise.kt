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
package me.kgustave.dkt.requests

import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.launch
import me.kgustave.dkt.entities.impl.APIImpl
import okhttp3.RequestBody
import org.apache.commons.collections4.map.CaseInsensitiveMap
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

/**
 * @author Kaidan Gustave
 */
@Suppress("MemberVisibilityCanPrivate")
abstract class RestPromise<T>(val api: APIImpl, protected open val route: Route.FormattedRoute) {
    companion object {
        internal val LOG: Logger = LoggerFactory.getLogger(RestPromise::class.java)
        private val DEFAULT_FAILURE: suspend (Throwable) -> Unit = {
            LOG.error("Promise could not be completed due to a failed execution: $it")
        }

        /**
         * Creates a simple [RestPromise] of type [T].
         *
         * This is typically used for trivial Promises like the one below:
         * ```
         * Promise.simple<String>(api, Route.GetSomething.format("parameter")) { res, req ->
         *     if(res.isOk)
         *         req.onSuccess("Success")
         *     else
         *         req.onSuccess("Also Success!")
         * }
         * ```
         */
        inline fun <reified T> simple(
            api: APIImpl, route: Route.FormattedRoute, body: RequestBody? = null,
            headers: CaseInsensitiveMap<String, Any>? = null,
            noinline block: suspend RestPromise<T>.(RestResponse, RestRequest<T>) -> Unit
        ): RestPromise<T> = object : RestPromise<T>(api, route) {
            override val body: RequestBody? = body
            override val headers: CaseInsensitiveMap<String, Any>? = headers

            override suspend fun handle(response: RestResponse, request: RestRequest<T>) {
                block(response, request)
            }
        }
    }

    protected open val body: RequestBody?
        get() = null
    protected open val headers: CaseInsensitiveMap<String, Any>?
        get() = null

    fun promise(then: suspend (T) -> Unit) = promise(then, DEFAULT_FAILURE)

    open fun promise(then: suspend (T) -> Unit, catch: suspend (Throwable) -> Unit) {
        launch(api.context) {
            try {
                then(await())
            } catch(t: Throwable) {
                catch(t)
            }
        }
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
