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
package me.kgustave.dkt.managers

import me.kgustave.dkt.entities.Icon
import me.kgustave.dkt.entities.impl.APIImpl
import me.kgustave.dkt.entities.impl.SelfUserImpl
import me.kgustave.dkt.requests.RestPromise
import me.kgustave.dkt.requests.Route
import me.kgustave.dkt.requests.promises.PreCompletedPromise
import me.kgustave.json.JSObject
import me.kgustave.json.jsonObject

/**
 * @author Kaidan Gustave
 */
class AccountManager internal constructor(val self: SelfUserImpl, api: APIImpl): AbstractAPIManager(api) {
    private var _username: String = self.name
    private var _avatar: Icon? = null
    private var modified = false

    fun username(username: String): AccountManager {
        if(_username != username) {
            _username = username
            if(!modified)
                modified = true
        }
        return this
    }

    fun avatar(avatar: Icon): AccountManager {
        _avatar = avatar
        if(!modified)
            modified = true
        return this
    }

    fun update(): RestPromise<Unit> {
        if(!modified) {
            return PreCompletedPromise(apiImpl, Unit)
        }

        val body = jsonObject {
            "username" to _username
            if(_avatar !== null)
                "avatar" to _avatar
        }

        return RestPromise.simple(apiImpl, Route.ModifySelf.format(), body = body) { res, req ->
            if(!res.isOk) {
                return@simple req.error(res)
            }

            val obj = res.obj as JSObject

            val name = obj["username"] as String
            self.name = name
            _username = name

            val avatarId = obj["avatar_id"] as String
            self.avatarId = avatarId
            _avatar = null

            self.discriminator = obj["discriminator"].toString().toInt()
            req.succeed(Unit)
        }
    }
}
