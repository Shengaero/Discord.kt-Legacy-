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
package me.kgustave.dkt.entities.impl

import me.kgustave.dkt.API
import me.kgustave.dkt.entities.Guild
import me.kgustave.dkt.entities.PrivateChannel
import me.kgustave.dkt.entities.TextChannel
import me.kgustave.dkt.entities.Webhook
import me.kgustave.dkt.requests.RestPromise

/**
 * @author Kaidan Gustave
 */
class WebhookImpl : Webhook {
    override val id: Long
        get() = TODO("not implemented")
    override val api: API
        get() = TODO("not implemented")
    override val guild: Guild
        get() = TODO("not implemented")
    override val channel: TextChannel
        get() = TODO("not implemented")
    override val name: String
        get() = TODO("not implemented")
    override val discriminator: Int
        get() = TODO("not implemented")
    override val avatarUrl: String
        get() = TODO("not implemented")
    override val avatarId: String?
        get() = TODO("not implemented")
    override val defaultAvatarUrl: String
        get() = TODO("not implemented")
    override val defaultAvatarId: String
        get() = TODO("not implemented")
    override val isBot: Boolean
        get() = TODO("not implemented")
    override val privateChannel: PrivateChannel
        get() = TODO("not implemented")

    override fun openPrivateChannel(): RestPromise<PrivateChannel> {
        TODO("not implemented")
    }

}
