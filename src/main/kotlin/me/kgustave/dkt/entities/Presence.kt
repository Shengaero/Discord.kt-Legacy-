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
package me.kgustave.dkt.entities

import me.kgustave.dkt.API
import me.kgustave.dkt.OnlineStatus

/**
 * A Discord Presence for the account currently logged into by the [API].
 *
 * This contains mutable properties for all aspects of the presence,
 * including the account's displayed [online status][status], [activity],
 * and whether or not the account is currently [afk].
 *
 * @since  1.0.0
 * @author Kaidan Gustave
 */
interface Presence {

    /**
     * The [API] that is responsible for managing the changes
     * made to this [Presence] instance.
     */
    val api: API

    /**
     * The current [OnlineStatus] for this [Presence] instance.
     *
     * **Note:** Attempting to set this to [OnlineStatus.UNKNOWN]
     * will result in an [IllegalArgumentException] being thrown.
     */
    var status: OnlineStatus

    /**
     * The current [Activity] for this [Presence] instance, or
     * `null` if there is no `Activity` currently specified.
     */
    var activity: Activity?

    /**
     * Whether or not this [Presence] is set to AFK.
     */
    var afk: Boolean
}
