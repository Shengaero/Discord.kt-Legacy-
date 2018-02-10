/*
 * Copyright 2017 Kaidan Gustave
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
@file:Suppress("UNCHECKED_CAST", "Unused", "MemberVisibilityCanBePrivate", "NOTHING_TO_INLINE")
package me.kgustave.dkt.entities

@[DslMarker MustBeDocumented Retention(AnnotationRetention.SOURCE)]
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
internal annotation class MessageDsl

@MessageDsl
abstract class MessageDslComponent<out A: MessageDslComponent<A>>
internal constructor(): Appendable {
    protected val contentBuilder = StringBuilder()

    @MessageDsl
    val content: String
        get() = contentBuilder.toString()

    @MessageDsl
    operator fun String.unaryPlus() {
        append(this)
    }

    @MessageDsl
    inline operator fun String.invoke(block: () -> String): String = "```$this\n${block()}```"

    @MessageDsl
    override fun append(csq: CharSequence): A {
        check(csq)
        contentBuilder.append(csq)
        return this as A
    }

    @MessageDsl
    override fun append(csq: CharSequence, start: Int, end: Int): A {
        return append(csq.subSequence(start, end))
    }

    @MessageDsl
    override fun append(c: Char): A {
        return append(c.toString())
    }

    protected open fun check(csq: CharSequence) {}
}
