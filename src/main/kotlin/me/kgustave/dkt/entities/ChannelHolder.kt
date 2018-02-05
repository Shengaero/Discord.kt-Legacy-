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
package me.kgustave.dkt.entities

import me.kgustave.dkt.annotations.EntityTrait

/**
 * A trait interface applied to entities that would hold one or
 * more [TextChannels][TextChannel] and/or [VoiceChannels][VoiceChannel].
 *
 * This is implemented in both [Guild] and [Category].
 *
 * @since  1.0.0
 * @author Kaidan Gustave
 */
@EntityTrait
interface ChannelHolder {
    val textChannels: List<TextChannel>
    val voiceChannels: List<VoiceChannel>
}