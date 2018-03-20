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
package me.kgustave.dkt.entities

/**
 * Represents a Discord activity status, such as `Playing with Wumpus`
 * or `Listening to Music`.
 *
 * You can use the following top level functions to create a Activity
 * instance:
 *
 * - [playing]:     Displayed as `Playing X`.
 * - [streaming]:   Displayed as `Streaming X` and providing a valid twitch URL to link.
 * - [listeningTo]: Displayed as `Listening to X`.
 * - [watching]:    Displayed as `Watching X`.
 *
 * @since  1.0.0
 * @author Kaidan Gustave
 */
data class Activity internal constructor(
    val name: String,
    val type: Activity.Type,
    val url: String? = null
) {

    /**
     * The type of [Activity].
     */
    enum class Type {
        PLAYING,
        STREAMING,
        LISTENING,
        WATCHING;
    }
}

/**
 * Creates a [Activity] that can be displayed as `Playing X` where
 * `X` is the specified [name].
 *
 * @param  name The name of the Activity.
 *
 * @return The newly created [Activity].
 */
fun playing(name: String): Activity = Activity(name, Activity.Type.PLAYING)

/**
 * Creates a [Activity] that can be displayed as `Streaming X` where
 * `X` is the specified [name].
 *
 * Additionally, Discord's streaming status will link to the
 * provided [url].
 *
 * **Note:** This must be a valid [twitch.tv](https://twitch.tv/) URL!
 *
 * @param  name The name of the Activity.
 * @param  url  A valid [twitch.tv](https://twitch.tv/) URL to link to.
 *
 * @return The newly created [Activity].
 */
fun streaming(name: String, url: String): Activity = Activity(name, Activity.Type.STREAMING, url)

/**
 * Creates a [Activity] that can be displayed as `Listening to X` where
 * `X` is the specified [name].
 *
 * @param  name The name of the Activity.
 *
 * @return The newly created [Activity].
 */
fun listeningTo(name: String): Activity = Activity(name, Activity.Type.LISTENING)

/**
 * Creates a [Activity] that can be displayed as `Watching X` where
 * `X` is the specified [name].
 *
 * @param  name The name of the Activity.
 *
 * @return The newly created [Activity].
 */
fun watching(name: String): Activity = Activity(name, Activity.Type.WATCHING)
