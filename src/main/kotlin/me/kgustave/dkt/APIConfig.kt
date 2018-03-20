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
@file:Suppress("Unused")
package me.kgustave.dkt

import me.kgustave.dkt.entities.Activity
import me.kgustave.dkt.events.Event
import me.kgustave.dkt.handlers.SessionManager
import me.kgustave.dkt.hooks.EventDispatcher
import me.kgustave.dkt.hooks.EventListener
import okhttp3.OkHttpClient
import javax.security.auth.login.LoginException
import kotlin.coroutines.experimental.CoroutineContext

/**
 * @author Kaidan Gustave
 */
class APIConfig {
    private val _listeners: MutableList<Any> = ArrayList()
    val listeners: List<Any>
        get() = _listeners

    private lateinit var _token: String
    var token: String
        get() {
            if(::_token.isInitialized)
                return _token
            throw LoginException("Token was not specified!")
        }
        set(value) { _token = value }

    var httpClientBuilder: OkHttpClient.Builder = OkHttpClient.Builder()
    var sessionManager: SessionManager? = null
    var onlineStatus: OnlineStatus? = OnlineStatus.ONLINE
    var activity: Activity? = null
    var afk = false
    var shouldAutoReconnect = true
    var eventDispatcher = EventDispatcher.DefaultEventDispatcher
    var corePoolSize = 2
    var initialReconnectDelay = 2
    var maxReconnectDelay = 900
    var apiAsyncContext: CoroutineContext? = null

    fun addListener(listener: Any) {
        _listeners += listener
    }

    fun removeListener(listener: Any) {
        _listeners -= listener
    }

    inline fun <reified E: Event> on(crossinline block: (E) -> Unit) {
        addListener(object: EventListener {
            override fun onEvent(event: Event) { if(event is E) block(event) }
        })
    }
}
