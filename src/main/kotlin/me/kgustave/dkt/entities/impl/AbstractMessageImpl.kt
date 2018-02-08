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

import me.kgustave.dkt.entities.Channel
import me.kgustave.dkt.entities.Emote
import me.kgustave.dkt.entities.Message
import me.kgustave.dkt.entities.User

/**
 * @author Kaidan Gustave
 */
abstract class AbstractMessageImpl : Message {
    override val renderedContent: String by lazy {
        content // TODO Rendered Content
    }
    override val mentionedChannels: List<Channel> by lazy {
        emptyList<Channel>() // TODO Mentioned Channels
    }
    override val mentionedEmotes: List<Emote> by lazy {
        emptyList<Emote>() // TODO Mentioned Emotes
    }
    override val mentionedUsers: List<User> by lazy {
        emptyList<User>() // TODO Mentioned Users
    }
}
