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
package me.kgustave.dkt.entities.impl

import me.kgustave.dkt.entities.PrivateChannel
import me.kgustave.dkt.entities.SelfUser
import me.kgustave.dkt.entities.Snowflake
import me.kgustave.dkt.entities.User
import me.kgustave.dkt.requests.RestPromise
import me.kgustave.dkt.util.unsupported

/**
 * @author Kaidan Gustave
 */
class SelfUserImpl(
    override val id: Long,
    override val api: APIImpl,
    override var name: String,
    override var discriminator: Int
): SelfUser {
    // Kotlincord does not and will never support client
    // accounts so this should always be true.
    override val isBot = true
    override var avatarId: String? = null

    override val avatarUrl: String
        get() = avatarId?.let { UserImpl.AVY_URL.format(id, it) } ?: defaultAvatarUrl
    override val defaultAvatarId: String
        get() = with(UserImpl) { DEFAULT_AVATAR_HASHES[discriminator % DEFAULT_AVATAR_HASHES.size] }
    override val defaultAvatarUrl: String
        get() = UserImpl.DEFAULT_AVY_URL.format(defaultAvatarId)
    override val privateChannel: PrivateChannel
        get() = unsupported { "Cannot get a PrivateChannel with self!" }

    override fun openPrivateChannel(): RestPromise<PrivateChannel> {
        unsupported { "Cannot open a PrivateChannel with self!" }
    }

    override fun hashCode(): Int = id.hashCode()

    override fun equals(other: Any?): Boolean = other is User && Snowflake.equals(this, other)
}
