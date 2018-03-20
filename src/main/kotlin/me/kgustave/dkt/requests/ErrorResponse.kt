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
@file:Suppress("MemberVisibilityCanBePrivate")
package me.kgustave.dkt.requests

import me.kgustave.dkt.util.niceName
import me.kgustave.json.JSObject

/**
 * @author Kaidan Gustave
 */
enum class ErrorResponse(val code: Int, meaning: String? = null) {
    UNKNOWN_ACCOUNT(                10001),
    UNKNOWN_APPLICATION(            10002),
    UNKNOWN_CHANNEL(                10003),
    UNKNOWN_GUILD(                  10004),
    UNKNOWN_INTEGRATION(            10005),
    UNKNOWN_INVITE(                 10006),
    UNKNOWN_MEMBER(                 10007),
    UNKNOWN_MESSAGE(                10008),
    UNKNOWN_OVERRIDE(               10009),
    UNKNOWN_PROVIDER(               10010),
    UNKNOWN_ROLE(                   10011),
    UNKNOWN_TOKEN(                  10012),
    UNKNOWN_USER(                   10013),
    UNKNOWN_EMOJI(                  10014),
    UNKNOWN_WEBHOOK(                10015),
    // Client accounts are unsupported
    // both these errors should never occur anyways
    //
    //BOTS_NOT_ALLOWED(               20001, "Bots cannot use this endpoint"),
    //ONLY_BOTS_ALLOWED(              20002, "Only bots can use this endpoint"),
    MAX_GUILDS(                     30001, "Maximum number of Guilds reached (100)"),
    MAX_FRIENDS(                    30002, "Maximum number of Friends reached (1000)"),
    MAX_MESSAGE_PINS(               30003, "Maximum number of pinned messages reached (50)"),
    MAX_USERS_PER_DM(               30004, "Maximum number of recipients reached. (10)"),
    MAX_ROLES_PER_GUILD(            30005, "Maximum number of guild roles reached (250)"),
    TOO_MANY_REACTIONS(             30010),
    UNAUTHORIZED(                   40001),
    MISSING_ACCESS(                 50001),
    INVALID_ACCOUNT_TYPE(           50002),
    INVALID_DM_ACTION(              50003, "Cannot execute action on a DM channel"),
    EMBED_DISABLED(                 50004),
    INVALID_AUTHOR_EDIT(            50005, "Cannot edit a message authored by another user"),
    EMPTY_MESSAGE(                  50006, "Cannot send an empty message"),
    CANNOT_SEND_TO_USER(            50007, "Cannot send messages to this user"),
    CANNOT_MESSAGE_VC(              50008, "Cannot send messages in a voice channel"),
    VERIFICATION_ERROR(             50009, "Channel verification level is too high"),
    OAUTH_NOT_BOT(                  50010, "OAuth2 application does not have a bot"),
    MAX_OAUTH_APPS(                 50011, "OAuth2 application limit reached"),
    INVALID_OAUTH_STATE(            50012),
    MISSING_PERMISSIONS(            50013),
    INVALID_TOKEN(                  50014, "Invalid Authentication Token"),
    NOTE_IS_TOO_LONG(               50015),
    INVALID_BULK_DELETE(            50016, "Provided too few or too many messages to delete. Must provided " +
                                           "at least 2 and fewer than 100 messages to delete"),
    INVALID_MFA_LEVEL(              50017, "Provided MFA level was invalid."),
    INVALID_PASSWORD(               50018, "Provided password was invalid"),
    INVALID_PIN(                    50019, "A message can only be pinned to the channel it was sent in"),
    INVALID_MESSAGE_TARGET(         50021, "Cannot execute action on a system message"),
    INVALID_BULK_DELETE_MESSAGE_AGE(50034, "A Message provided to bulk_delete was older than 2 weeks"),
    MFA_NOT_ENABLED(                60003, "MFA auth required but not enabled"),
    REACTION_BLOCKED(               90001),
    SERVER_ERROR(                   0,     "Discord encountered an internal server error! Not good!");

    val meaning = meaning ?: niceName

    companion object {
        fun ofCode(code: Int): ErrorResponse = values().firstOrNull { it.code == code } ?: SERVER_ERROR
        fun from(kson: JSObject?): ErrorResponse {
            val code = kson?.opt<Int>("code") ?: return SERVER_ERROR

            return ofCode(code)
        }
    }
}
