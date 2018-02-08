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

import me.kgustave.dkt.requests.RestPromise

/**
 * Represents a Discord User as defined by the API.
 *
 * Users are separate from any sort of [Guild], and contain the
 * most basic global info about an account provided by the API.
 *
 * Those looking to perform actions or get info on a User specific
 * to a Guild should be directed towards the [Member] interface.
 *
 * @since  1.0.0
 * @author Kaidan Gustave
 */
interface User : Mentionable, Snowflake {
    /** The User's account name. */
    val name: String

    /** The User's four digit discriminator. */
    val discriminator: Int

    /**
     * The User's avatar URL.
     *
     * If the user has not set their avatar, this
     * will return their [defaultAvatarUrl].
     */
    val avatarUrl: String

    /**
     * The User's avatar ID.
     *
     * If the user has not set their avatar, this
     * will return their [defaultAvatarId].
     */
    val avatarId: String?

    /**
     * The User's default avatar URL.
     *
     * This is one of five generic Avatars, and the
     * value of this is determined by the following
     * equation: `AVATAR_ORDINAL = D % 5`, where `D`
     * is the User's [discriminator].
     *
     * Note: when getting [avatarUrl], if the User
     * has not set their Avatar, the value of this
     * will be returned instead.
     */
    val defaultAvatarUrl: String

    /**
     * The User's default avatar ID.
     *
     * This is one of five generic Avatars, and the
     * value of this is determined by the following
     * equation: `AVATAR_ORDINAL = D % 5`, where `D`
     * is the User's [discriminator].
     *
     * Note: when getting [avatarId], if the User
     * has not set their Avatar, the value of this
     * will be returned instead.
     */
    val defaultAvatarId: String

    /**
     * `true` if the User is a "bot" account or
     * [Webhook], `false` otherwise.
     */
    val isBot: Boolean

    val privateChannel: PrivateChannel

    override val asMention: String
        get() = "<@$id>"

    fun openPrivateChannel(): RestPromise<PrivateChannel>
}
