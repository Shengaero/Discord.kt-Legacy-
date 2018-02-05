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
package me.kgustave.dkt.exceptions

import me.kgustave.dkt.requests.ErrorResponse
import me.kgustave.dkt.requests.RestResponse
import me.kgustave.kson.KSONObject

/**
 * @author Kaidan Gustave
 */
class ErrorResponseException(val error: ErrorResponse, val response: RestResponse) : RuntimeException("${error.code} - ${error.meaning}") {
    val code: Int
    val meaning: String
    val isServerError = error == ErrorResponse.SERVER_ERROR

    init {
        when {
            response.isError -> {
                code = response.code
                meaning = response.exception!!.javaClass.name
            }

            response.obj != null -> {
                val obj = response.obj as KSONObject
                code = when {
                    !obj.isNull("code") -> obj["code"] as Int
                    else -> response.code
                }

                meaning = when {
                    !obj.isNull("message") -> obj["message"] as String
                    else -> response.toString()
                }
            }

            else -> {
                code = response.code
                meaning = error.meaning
            }
        }
    }
}
