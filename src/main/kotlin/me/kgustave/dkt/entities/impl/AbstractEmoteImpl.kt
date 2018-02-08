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
import me.kgustave.dkt.Discord
import me.kgustave.dkt.entities.Emote

/**
 * @author Kaidan Gustave
 */
abstract class AbstractEmoteImpl(
    override val api: API,
    override val id: Long,
    override val name: String,
    override val isAnimated: Boolean,
    override val isManaged: Boolean
): Emote {
    companion object {
        private const val EMOJI_CDN = "${Discord.CDN_URL}/emojis/"
    }
    override val imageUrl: String
        get() = "$EMOJI_CDN$id${if(isAnimated) ".gif" else ".png"}"
    override val asMention: String
        get() = "<${if(isAnimated) "a" else ""}:$name:$id>"
}
