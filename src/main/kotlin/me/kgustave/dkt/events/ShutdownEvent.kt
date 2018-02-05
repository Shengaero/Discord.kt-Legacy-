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
@file:Suppress("CanBeParameter", "MemberVisibilityCanPrivate", "MemberVisibilityCanBePrivate")
package me.kgustave.dkt.events

import me.kgustave.dkt.API
import me.kgustave.dkt.requests.CloseCode
import java.time.OffsetDateTime

/**
 * @author Kaidan Gustave
 */
class ShutdownEvent(override val api: API, val rawCloseCode: Int): Event {
    override val responseNumber: Long = -1
    val shutdownTime: OffsetDateTime = OffsetDateTime.now()
    val closeCode: CloseCode? = CloseCode.of(rawCloseCode)
}
