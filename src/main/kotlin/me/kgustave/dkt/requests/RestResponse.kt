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
@file:Suppress("MemberVisibilityCanPrivate", "MemberVisibilityCanBePrivate", "Unused")
package me.kgustave.dkt.requests

import me.kgustave.kson.KSONObject
import me.kgustave.kson.KSONTokener
import okhttp3.Response
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.stream.Collectors
import java.util.zip.GZIPInputStream

/**
 * @author Kaidan Gustave
 */
class RestResponse {

    // Borrowed the structure of Responses from JDA.
    // All credit to them for this code.

    val okResponse: Response?
    val code: Int
    val message: String
    val retryAfter: Long
    val cfRays: Set<String>
    val exception: Exception?
    val obj: Any?

    constructor(okResponse: Response?, code: Int, message: String, retryAfter: Long,
                cfRays: Set<String>, exception: Exception? = null) {
        this.okResponse = okResponse
        this.code = code
        this.message = message
        this.retryAfter = retryAfter
        this.cfRays = cfRays
        this.exception = exception

        if(okResponse == null || okResponse.body()?.contentLength().let { it == null || it == 0L }) {
            this.obj = null
            return
        }

        try {
            obj = okResponse.validEncodedBody?.use {
                val reader = BufferedReader(InputStreamReader(it))

                var begin: Char
                var mark = 1
                do {
                    reader.mark(mark++)
                    begin = reader.read().toChar()
                } while(begin.isWhitespace())

                reader.reset()

                when(begin) {
                    '{'  -> // It's an object
                        KSONObject(KSONTokener(reader))
                    '['  -> // It's an array
                        KSONObject(KSONTokener(reader))
                    else -> // Something else
                        reader.lines().collect(Collectors.joining())
                }
            }
        } catch(e: Exception) {
            throw IllegalStateException("Encountered an error when instantiating a Response", e)
        }
    }

    constructor(okResponse: Response?, exception: Exception, cfRays: Set<String>):
        this(okResponse, okResponse?.code() ?: -1, "ERROR", -1, cfRays, exception)

    constructor(okResponse: Response, retryAfter: Long, cfRays: Set<String>):
        this(okResponse, okResponse.code(), okResponse.message(), retryAfter, cfRays)

    override fun toString(): String {
        if(exception != null) {
            return "Exception: $code - $message"
        }
        return "HTTP: $code $message${if(obj == null) "" else ", $obj"}"
    }

    companion object {
        val Response.validEncodedBody: InputStream?
            get() {
                val encoding = header("content-encoding", "")
                if(encoding == "gzip")
                    return body()?.let { GZIPInputStream(it.byteStream()) }
                return body()?.byteStream()
            }

        fun validEncodedBody(okResponse: Response): InputStream? = okResponse.validEncodedBody

        fun rateLimit(retryAfter: Long, cfRays: Set<String>) = RestResponse(null, 429, "TOO MANY REQUESTS", retryAfter, cfRays)
    }

    inline val isOk
        inline get() = code in 200 until 300

    inline val isRateLimit
        inline get() = code == 429 // TOO MANY REQUESTS

    inline val isError
        inline get() = code == -1

    inline val isUnauthorized: Boolean
        inline get() = code == 401
}

