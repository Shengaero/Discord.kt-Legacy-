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
@file:Suppress("CanBePrimaryConstructorProperty")
package me.kgustave.dkt.entities.impl

import me.kgustave.dkt.API
import me.kgustave.dkt.entities.GuildEmote
import me.kgustave.dkt.entities.Role

/**
 * @author Kaidan Gustave
 */
class GuildEmoteImpl(
    guild: GuildImpl,
    api: API,
    id: Long,
    name: String,
    isAnimated: Boolean,
    isManaged: Boolean
): GuildEmote, AbstractEmoteImpl(api, id, name, isAnimated, isManaged) {
    internal val internalRoles = ArrayList<Role>()

    override var name: String = super.name

    override val guild = guild
    override val roles: List<Role>
        get() = internalRoles
}
