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
@file:Suppress("CanBePrimaryConstructorProperty")
package me.kgustave.dkt.entities.impl

import me.kgustave.dkt.OnlineStatus
import me.kgustave.dkt.entities.Activity
import me.kgustave.dkt.entities.Presence
import me.kgustave.dkt.requests.OpCode
import me.kgustave.dkt.util.currentTime
import me.kgustave.dkt.util.delegate.updating
import me.kgustave.json.JSObject
import me.kgustave.json.jsonObject

class PresenceImpl(
    api: APIImpl,
    status: OnlineStatus? = null,
    activity: Activity? = null,
    afk: Boolean = false
): Presence {
    // Create a lock for bulk presence modification
    internal var updateLocked: Boolean = false

    override val api = api
    override var status: OnlineStatus by updating(status ?: OnlineStatus.ONLINE) { if(!updateLocked) update() }
    override var activity: Activity? by updating(activity) { if(!updateLocked) update() }
    override var afk: Boolean by updating(afk) { if(!updateLocked) update() }

    val json: JSObject get() = jsonObject {
        "aft" to afk
        "status" to status
        "game" to activity?.let { activity ->
            jsonObject game@ {
                this@game["name"] = activity.name
                this@game["type"] = activity.type.ordinal
                activity.url?.let { this@game["url"] = it }
            }
        }
        "since" to currentTime
    }

    internal inline fun updateInBulk(block: Presence.() -> Unit) {
        // Lock the presence so we can make changes
        updateLocked = true

        val exception: Throwable? = try {
            block() // Apply the block
            null
        } catch(t: Throwable) { t } finally {
            // Unlock the presence
            updateLocked = false
        }

        exception?.let { throw it }
        update()
    }

    internal fun update() {
        api.websocket.sendMessage(jsonObject {
            this["d"] = json
            this["op"] = OpCode.STATUS_UPDATE
        }.toString())
    }
}
