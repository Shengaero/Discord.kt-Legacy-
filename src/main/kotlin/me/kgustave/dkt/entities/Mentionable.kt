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

import me.kgustave.dkt.annotations.EntityTrait

/**
 * @since  1.0.0
 * @author Kaidan Gustave
 */
@EntityTrait
interface Mentionable {
    val asMention: String

    /**
     * Enumerated constants for various forms of [mentions][Mentionable.asMention].
     */
    enum class Format(val regex: Regex) {
        USER(Regex("<@!?(\\d{17,21})>")),
        CHANNEL(Regex("<#(\\d{17,21})>")),
        ROLE(Regex("<@&(\\d{17,21})>")),
        EMOTE(Regex("<a?:(\\S{2,32}):(\\d{17,21})>"));
    }
}
