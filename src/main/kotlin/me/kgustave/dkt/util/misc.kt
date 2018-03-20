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
package me.kgustave.dkt.util

import me.kgustave.dkt.Discord
import java.time.OffsetDateTime
import java.util.*

private val ENUM_SPLIT_REGEX = Regex("_+")
const val TIMESTAMP_OFFSET = 22

val Long.toCreationTime: OffsetDateTime get() {
    Calendar.getInstance(TimeZone.getTimeZone("GMT")).let {
        it.timeInMillis = (this ushr TIMESTAMP_OFFSET) + Discord.EPOCH
        return OffsetDateTime.ofInstant(it.toInstant(), it.timeZone.toZoneId())
    }
}

val <E: Enum<E>> E.niceName: String get() {
    return name.split(ENUM_SPLIT_REGEX).joinToString(" ") { "${it[0]}${it.substring(1)}" }
}

inline fun <reified T> Array<out T?>.toStringArray(): Array<String> = Array(size) { this[it].toString() }

/**
 * Checks whether the provided [String] or [Long] can be expressed as a [Long] for the
 * purpose of converting it to a [Snowflake ID][me.kgustave.dkt.entities.Snowflake.id].
 *
 * Due to complications, Discord returns String IDs as representations of a Snowflake as
 * opposed to longs, and thus this util is used to easily convert these.
 *
 * @param any Either a [String] or [Long], anything else will throw an [IllegalStateException]
 */
@PublishedApi
internal fun snowflake(any: Any?): Long {
    return when(any) {
        is String -> {
            try {
                any.toString().toLong()
            } catch(e: NumberFormatException) {
                throw IllegalStateException("Snowflake was a String, but could not be parsed to a Long!", e)
            }
        }

        is Long -> any

        is Int -> any.toLong()

        else -> throw IllegalStateException("Provided value was not a String or Long: $any")
    }
}
