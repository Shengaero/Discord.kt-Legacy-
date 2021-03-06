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
package me.kgustave.dkt.requests.promises

import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.Deferred
import me.kgustave.dkt.entities.impl.APIImpl
import me.kgustave.dkt.requests.*
import me.kgustave.dkt.util.HeaderMap
import me.kgustave.dkt.util.unsupported
import okhttp3.RequestBody

/**
 * A pre-completed "fake" promise.
 *
 * @author Kaidan Gustave
 */
class PreCompletedPromise<T>(api: APIImpl, private val value: T? = null, private val exception: Throwable? = null):
    // This is always the Fake endpoint, as it never calls to discord
    RestPromise<T>(api, Route.Fake.format()) {

    override val body: RequestBody? = null
    override val headers: HeaderMap? = null

    // A null exception means the pre-completed promise was a success
    private val exceptionally = exception !== null

    override fun complete(): T = if(exceptionally) throw exception!! else value!!

    override fun async(): Deferred<T> {
        return value?.let { CompletableDeferred(it) } ?:
               CompletableDeferred<T>().also { it.completeExceptionally(exception!!) }
    }

    override suspend fun handle(response: RestResponse, request: RestRequest<T>) {
        unsupported { "Handling pre-completed promises is unsupported!" }
    }
}
