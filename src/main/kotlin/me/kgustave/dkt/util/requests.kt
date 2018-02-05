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
package me.kgustave.dkt.util

import me.kgustave.dkt.requests.Method
import me.kgustave.dkt.requests.Route
import okhttp3.*
import okhttp3.Response
import okhttp3.Request
import java.io.IOException
import kotlin.coroutines.experimental.suspendCoroutine

inline fun request(builder: Request.Builder = Request.Builder(), block: Request.Builder.() -> Unit): Request {
    return builder.apply(block).build()
}

inline infix fun OkHttpClient.request(block: Request.Builder.() -> Unit): Call {
    return newCall(request(Request.Builder(), block))
}

fun Request.Builder.method(method: Method, body: RequestBody? = null): Request.Builder {
    return method(method.name, body)
}

internal fun Request.Builder.onRoute(route: Route, body: RequestBody? = null): Request.Builder {
    method(route.method, body)
    url("${Route.BASE_URL}/${route.endpoint}")
    return this
}

suspend inline fun <reified C: Call> C.await(): Response = suspendCoroutine { cont ->
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            cont.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            cont.resume(response)
        }
    })
}

inline fun <reified T: AutoCloseable?, reified R> using(closeable: T, block: T.() -> R): R {
    return closeable.use(block)
}

operator fun Request.Builder.set(name: String, value: String): Request.Builder = header(name, value)
