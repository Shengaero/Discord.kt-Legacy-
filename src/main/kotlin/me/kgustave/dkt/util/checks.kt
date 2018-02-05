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

inline fun checkState(condition: Boolean, msg: () -> String) {
    if(condition)
        throw IllegalStateException(msg())
}

inline fun <reified T, reified R: T> T.checkCast(msg: () -> String): R {
    if(this !is R)
        throw IllegalStateException(msg())
    return this
}

inline fun <reified T> nonNullState(t: T?, msg: () -> String): T {
    if(t == null)
        throw IllegalStateException(msg())
    return t
}

inline fun doNotSupport(condition: Boolean, msg: () -> String) {
    if(condition)
        throw UnsupportedOperationException(msg())
}

inline fun unsupported(msg: () -> String): Nothing = throw UnsupportedOperationException(msg())
