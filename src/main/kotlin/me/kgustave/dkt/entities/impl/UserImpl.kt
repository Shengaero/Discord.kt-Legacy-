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

import me.kgustave.dkt.Discord
import me.kgustave.dkt.entities.PrivateChannel
import me.kgustave.dkt.entities.Snowflake
import me.kgustave.dkt.entities.User
import me.kgustave.dkt.exceptions.UnloadedPropertyException
import me.kgustave.dkt.requests.RestPromise
import me.kgustave.dkt.requests.Route
import me.kgustave.dkt.requests.promises.PreCompletedPromise
import me.kgustave.json.JSObject
import me.kgustave.json.jsonObject

/**
 * @author Kaidan Gustave
 */
class UserImpl(override val id: Long, override val api: APIImpl) : User {
    companion object {
        internal const val DEFAULT_AVY_URL = "${Discord.CDN_URL}/embed/avatars/%s.png"
        internal const val AVY_URL = "${Discord.CDN_URL}/avatars/%d/%s.png"

        internal val DEFAULT_AVATAR_HASHES = arrayOf(
            "6debd47ed13483642cf09e832ed0bc1b", // Blurple
            "322c936a8c8be1b803cd94861bdfa868", // Gray
            "dd4dbc0016779df1378e7812eabaa04d", // Green
            "0e291f67c9274a1abdddeb3fd919cbaa", // Orange
            "1cbd08c76f8af6dddce02c5138971129"  // Red
        )
    }

    internal var internalPrivateChannel: PrivateChannel? = null

    override var name = ""
        internal set
    override var discriminator = -1
        internal set
    override var isBot = false
        internal set
    override var avatarId: String? = null
        internal set

    override val privateChannel: PrivateChannel get() {
        return internalPrivateChannel ?: throw UnloadedPropertyException("Private channel has not been opened yet!")
    }

    override val avatarUrl: String get() {
        return avatarId?.let { AVY_URL.format(id, it) } ?: defaultAvatarUrl
    }

    override val defaultAvatarId: String get() {
        return DEFAULT_AVATAR_HASHES[discriminator % DEFAULT_AVATAR_HASHES.size]
    }

    override val defaultAvatarUrl: String get() {
        return DEFAULT_AVY_URL.format(defaultAvatarId)
    }

    override fun openPrivateChannel(): RestPromise<PrivateChannel> {
        // We already have this channel opened
        internalPrivateChannel?.let { return PreCompletedPromise(api, it, null) }

        val requestBody = jsonObject { "recipient_id" to id.toString() }
        return RestPromise.simple(api, Route.CreateDM.format(), body = requestBody) { res, req ->
            when {
                res.isOk -> req.succeed(api.entityBuilder.createPrivateChannel(res.obj as JSObject, this@UserImpl))
                res.isError -> req.error(res)
            }
        }
    }

    override fun hashCode(): Int = id.hashCode()

    override fun equals(other: Any?): Boolean = other is User && Snowflake.equals(this, other)
}
