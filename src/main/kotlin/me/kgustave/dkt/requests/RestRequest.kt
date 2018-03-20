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
@file:Suppress("MemberVisibilityCanBePrivate", "Unused")
package me.kgustave.dkt.requests

import kotlinx.coroutines.experimental.CompletableDeferred
import me.kgustave.dkt.exceptions.ErrorResponseException
import me.kgustave.dkt.exceptions.RateLimitedException
import me.kgustave.dkt.util.HeaderMap
import me.kgustave.json.JSObject
import okhttp3.RequestBody

/**
 * @author Kaidan Gustave
 */
class RestRequest<T>(
    val promise: RestPromise<T>,
    val route: Route.FormattedRoute,
    val deferred: CompletableDeferred<T>,
    internal val body: RequestBody? = null,
    internal val headers: HeaderMap? = null
) {
    var isCancelled = false

    fun succeed(value: T) {
        try {
            deferred.complete(value)
        } catch(t: Throwable) {
            RestPromise.LOG.error("An error occurred while processing the success function.", t)
        }
    }

    fun failure(t: Throwable) {
        try {
            deferred.completeExceptionally(t)
        } catch(t: Throwable) {
            RestPromise.LOG.error("An error occurred while processing the success function.", t)
        }
    }

    fun error(res: RestResponse) {
        if(res.isRateLimit) {
            failure(RateLimitedException(route, res.retryAfter))
        } else {
            failure(ErrorResponseException(ErrorResponse.from(res.obj as? JSObject), res))
        }
    }

    fun cancel(cause: Throwable? = null) {
        deferred.cancel(cause)
        isCancelled = true
    }

    suspend fun handle(response: RestResponse) {
        promise.handle(response, this)
    }
}
