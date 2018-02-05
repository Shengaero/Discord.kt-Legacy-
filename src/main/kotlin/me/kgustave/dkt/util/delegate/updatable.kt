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
package me.kgustave.dkt.util.delegate

import kotlin.reflect.KProperty

/**
 * A simplistic update delegate that runs the [onSet] block
 * when the delegating property's setter is called.
 *
 * @author Kaidan Gustave
 */
class UpdatableDelegate<in I, T>
internal constructor(private var value: T, private val onSet: I.() -> Unit) {
    operator fun getValue(instance: I, property: KProperty<*>): T = value
    operator fun setValue(instance: I, property: KProperty<*>, value: T) {
        this.value = value
        onSet(instance)
    }
}

fun <I, T> updating(value: T, onSet: I.() -> Unit): UpdatableDelegate<I, T> = UpdatableDelegate(value, onSet)
