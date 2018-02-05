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
package me.kgustave.dkt.requests.promises

import me.kgustave.dkt.entities.Message
import me.kgustave.dkt.entities.MessageChannel
import me.kgustave.dkt.entities.impl.APIImpl
import me.kgustave.dkt.requests.*
import java.io.InputStream

/**
 * @author Kaidan Gustave
 */
class MessagePromise(
    val channel: MessageChannel,
    api: APIImpl,
    route: Route.FormattedRoute,
    base: String? = null
): RestPromise<Message>(api, route) {
    // IMPORTANT
    // When sending files, we're going to need to use a Multiform Body,
    // We should check which one is appropriate at the time we send it.
    private val files: MutableMap<String, InputStream> = HashMap()
    private val content: StringBuilder = base?.let { StringBuilder(it) } ?: StringBuilder()

    override suspend fun handle(response: RestResponse, request: RestRequest<Message>) {}
}
