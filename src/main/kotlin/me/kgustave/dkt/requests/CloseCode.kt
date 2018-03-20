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
package me.kgustave.dkt.requests

/**
 * @author Kaidan Gustave
 */
enum class CloseCode(val code: Int, val message: String, val isReconnect: Boolean = true) {
    GRACEFUL_CLOSE(       1000, "The connection was closed gracefully or your heartbeats timed out."),
    CLOUD_FLARE_LOAD(     1001, "The connection was closed due to CloudFlare load balancing."),
    INTERNAL_SERVER_ERROR(1006, "Something broke on the remote's end, sorry 'bout that... Try reconnecting!"),
    UNKNOWN_ERROR(        4000, "The server is not sure what went wrong. Try reconnecting?"),
    UNKNOWN_OPCODE(       4001, "You sent an invalid Gateway OP Code. Don't do that!"),
    DECODE_ERROR(         4002, "You sent an invalid payload to the server. Don't do that!"),
    NOT_AUTHENTICATED(    4003, "You sent a payload prior to identifying."),
    AUTHENTICATION_FAILED(4004, "The account token sent with your identify payload is incorrect.", false),
    ALREADY_AUTHENTICATED(4005, "You sent more than one identify payload. Don't do that!"),
    INVALID_SEQ(          4007, "The sent sent when resuming the session was invalid. Reconnect and start a new session."),
    RATE_LIMITED(         4008, "Woah nelly! You're sending payloads to us too quickly. Slow it down!"),
    SESSION_TIMEOUT(      4009, "Your session timed out. Reconnect and start a new one."),
    INVALID_SHARD(        4010, "You sent an invalid shard when identifying.", false),
    SHARDING_REQUIRED(    4011, "The session would have handled too many guilds - " +
                                "you are required to shard your connection in order to connect.", false);
    companion object {
        fun of(code: Int): CloseCode? = values().find { it.code == code }
    }
}
