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
package me.kgustave.dkt.handlers.event

import me.kgustave.dkt.entities.impl.APIImpl
import me.kgustave.dkt.util.snowflake
import me.kgustave.kson.KSONArray
import me.kgustave.kson.KSONObject

/**
 * @author Kaidan Gustave
 */
class ReadyHandler(override val api: APIImpl): EventHandler(Type.READY) {

    private val incompleteGuilds = HashSet<Long>()
    private val unavailableGuilds = HashSet<Long>()

    override fun handle(event: KSONObject, responseNumber: Long, rawKSON: KSONObject) {
        val rawSelf = event["user"] as KSONObject
        val rawGuilds = event["guilds"] as KSONArray
        val entityBuilder = api.entityBuilder

        entityBuilder.createSelf(rawSelf)

        // First we cache all guild IDs that are incomplete
        for(raw in rawGuilds)
            incompleteGuilds += snowflake((raw as KSONObject)["id"])

        rawGuilds.forEach {
            entityBuilder.createNewGuild(it as KSONObject)
        }
    }
}
