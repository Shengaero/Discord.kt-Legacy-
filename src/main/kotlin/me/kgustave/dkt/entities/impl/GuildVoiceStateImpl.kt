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
import me.kgustave.dkt.entities.*

/**
 * @author Kaidan Gustave
 */
data class GuildVoiceStateImpl(override val guild: Guild, override val member: Member): GuildVoiceState {
    /*
    guild_id?       snowflake // We're not allowed in calls, so this should be in payloads.
    channel_id      snowflake
    user_id         snowflake
    session_id      string
    deaf            bool
    mute            bool
    self_deaf       bool
    self_mute       bool
    suppress        bool
    */

    override val api: API
        get() = guild.api
    override val user: User
        get() = member.user

    override lateinit var channel: VoiceChannel
        internal set
    override lateinit var sessionId: String
        internal set
    override var deaf = false
        internal set
    override var mute = false
        internal set
    override var selfDeaf = false
        internal set
    override var selfMute = false
        internal set
    override var suppress = false
        internal set

}
