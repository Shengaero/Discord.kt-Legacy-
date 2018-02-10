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
@file:Suppress("MemberVisibilityCanPrivate", "CanBeParameter", "Unused",
    "LoopToCallChain", "MemberVisibilityCanBePrivate")
package me.kgustave.dkt

import me.kgustave.dkt.util.niceName

/**
 * @author Kaidan Gustave
 */
enum class Permission(val offset: Int, val isGuild: Boolean, val isChannel: Boolean, name: String? = null) {
    CREATE_INSTANT_INVITE(0, true, true),
    KICK_MEMBERS(1, true, false),
    BAN_MEMBERS(2, true, false),
    ADMINISTRATOR(3, true, false),
    MANAGE_CHANNEL(4, true, true),
    MANAGE_SERVER(5, true, false),
    MESSAGE_ADD_REACTION(6, true, true, "Add Reactions"),
    VIEW_AUDIT_LOGS(7, true, false),

    // All Channels
    VIEW_CHANNEL(10, true, true),

    // Text Channels
    MESSAGE_READ(10, true, true, "Read Messages"),
    MESSAGE_WRITE(11, true, true, "Send Messages"),
    MESSAGE_TTS(12, true, true, "Send TTS Messages"),
    MESSAGE_MANAGE(13, true, true, "Manage Messages"),
    MESSAGE_EMBED_LINKS(14, true, true, "Embed Links"),
    MESSAGE_ATTACH_FILES(15, true, true, "Attach Files"),
    MESSAGE_HISTORY(16, true, true, "Read History"),
    MESSAGE_MENTION_EVERYONE(17, true, true, "Mention Everyone"),
    MESSAGE_EXT_EMOJI(18, true, true, "Use External Emojis"),

    // Voice Channels
    VOICE_CONNECT(20, true, true, "Connect"),
    VOICE_SPEAK(21, true, true, "Speak"),
    VOICE_MUTE_OTHERS(22, true, true, "Mute Members"),
    VOICE_DEAF_OTHERS(23, true, true, "Deafen Members"),
    VOICE_MOVE_OTHERS(24, true, true, "Move Members"),
    VOICE_USE_VAD(25, true, true, "Use Voice Activity"),

    NICKNAME_CHANGE(26, true, false, "Change Nickname"),
    NICKNAME_MANAGE(27, true, false, "Manage Nicknames"),

    MANAGE_ROLES(28, true, false),
    MANAGE_PERMISSIONS(28, false, true),
    MANAGE_WEBHOOKS(29, true, true),
    MANAGE_EMOTES(30, true, false, "Manage Emojis"),

    UNKNOWN(-1, false, false);

    val rawOffset = (1 shl offset).toLong()
    val title = name ?: niceName
    val isText = offset in 10..19
    val isVoice = offset == 10 || offset in 20..25

    companion object {
        val ALL         = rawPerms(*values())
        val ALL_GUILD   = rawPerms(*permsMatching { it.isGuild })
        val ALL_CHANNEL = rawPerms(*permsMatching { it.isChannel })
        val ALL_TEXT    = rawPerms(*permsMatching { it.isText })
        val ALL_VOICE   = rawPerms(*permsMatching { it.isVoice })

        inline fun permsMatching(condition: (Permission) -> Boolean): Array<Permission>
            = values().filter(condition).toTypedArray()

        fun fromRaw(rawPerms: Long): List<Permission> {
            return values().mapNotNull {
                if(it == UNKNOWN) null
                else it.takeIf { (rawPerms shr it.offset) and 1 == 1L }
            }
        }

        fun rawPerms(vararg perms: Permission): Long {
            var i = 0L
            for(perm in perms)
                i += perm.rawOffset
            return i
        }
    }
}
