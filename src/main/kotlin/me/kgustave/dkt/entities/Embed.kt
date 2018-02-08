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
package me.kgustave.dkt.entities

/**
 * @author Kaidan Gustave
 */
class Embed private constructor(builder: Embed.Builder) {
    constructor(build: Embed.Builder.() -> Unit): this(Embed.Builder().apply(build))

    fun isEmpty(): Boolean {
        TODO("Unimplemented")
    }

    data class Field(val title: String, val value: String, val inline: Boolean)

    class Builder : Appendable {
        private val fields = ArrayList<Embed.Field>()
        private val description = StringBuilder()

        fun field(title: String, value: String, inline: Boolean = true) {
            fields += Field(title, value, inline)
        }

        override fun append(csq: CharSequence): Embed.Builder {
            description.append(csq)
            return this
        }

        override fun append(csq: CharSequence, start: Int, end: Int): Embed.Builder {
            description.append(csq, start, end)
            return this
        }

        override fun append(c: Char): Embed.Builder {
            description.append(c)
            return this
        }
    }
}
