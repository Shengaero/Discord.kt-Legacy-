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
@file:Suppress("MemberVisibilityCanPrivate", "LeakingThis", "MemberVisibilityCanBePrivate", "Unused")
package me.kgustave.dkt.requests

import me.kgustave.dkt.Discord
import me.kgustave.dkt.requests.Method.*
import me.kgustave.dkt.util.emptyIntArray
import me.kgustave.dkt.util.toStringArray

/**
 * Represents a Discord request endpoint and holds information regarding it.
 *
 * Otherwise they hold data relating to endpoints and rate-limits respectively.
 *
 * @since  1.0
 * @author Kaidan Gustave
 */
sealed class Route
constructor(val method: Method, val endpoint: String, vararg val majorParameters: String,
            val rateLimit: RateLimit? = null) {
    companion object {
        // The base URL for all requests
        internal const val BASE_URL = "${Discord.API_URL}/v${Discord.KtInfo.REST_VERSION}"
        internal val PARAM_REPLACE_PATTERN = Regex("\\{(.*)}", RegexOption.DOT_MATCHES_ALL)
    }

    /**
     * The indices of all [majorParameters] on this route.
     *
     * Indices specifically means which position they occur on the [formattableEndpoint],
     * and is essentially a map between the formatting points of the route's
     * [formattableRateLimitEndpoint] and [formattableEndpoint], where each index of this
     * array is the flag number in the [formattableRateLimitEndpoint], and the value of
     * that index is the flag number in the [formattableEndpoint].
     */
    val majorParameterIndices: IntArray

    /**
     * An endpoint that can be formatted with parameters.
     *
     * This could be called the base for an "actual request URL".
     */
    val formattableEndpoint: String = endpoint.replace(PARAM_REPLACE_PATTERN, "%s")

    /**
     * The format for a endpoint that we keep to track and properly handle ratelimits
     * when dealing with requests.
     *
     * This is similar to the [formattableEndpoint], except it only has formatting
     * flags for the [majorParameters] of the Route.
     */
    val formattableRateLimitEndpoint: String

    /**
     * The number of parameters that this route has.
     *
     * Note: This is not always equal to the number of [majorParameters]
     * this route has, and should not be treated as such.
     */
    val paramCount = endpoint.count { it == '{' }

    init {
        require(paramCount == endpoint.count { it == '}' }) {
            "Incorrect param count! The number of {'s and }'s does not match!"
        }

        // If we have any major parameters
        if(majorParameters.isNotEmpty()) {
            val majorParameterIndices: MutableList<Int> = ArrayList()
            val matcher = PARAM_REPLACE_PATTERN.findAll(endpoint)
            var formattableEndpoint: String = endpoint

            matcher.forEachIndexed { paramIndex, result ->
                val paramName = result.groupValues[1]
                majorParameters.forEach { majorParam ->
                    if(majorParam == paramName) {
                        formattableEndpoint = formattableEndpoint.replaceFirst(result.groupValues[0], "%s")
                        majorParameterIndices.add(paramIndex)
                    }
                }
            }

            this.majorParameterIndices = majorParameterIndices.toIntArray()
            this.formattableRateLimitEndpoint = formattableEndpoint
        } else {
            this.majorParameterIndices = emptyIntArray()
            this.formattableRateLimitEndpoint = endpoint
        }
    }

    fun format(vararg params: Any): FormattedRoute {
        require(params.size == paramCount) {
            "Invalid parameter count for '$endpoint'! Expected: $paramCount, Provided: ${params.size}"
        }

        val formatted = formattableEndpoint.format(*params.toStringArray())

        val formattedRateLimit = formattableRateLimitEndpoint.run {
            return@run if(majorParameterIndices.isNotEmpty())
                format(*Array(majorParameterIndices.size) { params[majorParameterIndices[it]].toString() })
            else this
        }

        return FormattedRoute(this, formattedRateLimit, formatted)
    }

    override fun toString(): String = "$method - $endpoint"

    /**
     * A properly formatted request route which wraps and finalizes a base [Route]
     * as well as both it's [formattable rate limit endpoint][formattableRateLimitEndpoint]
     * and a [formattable full endpoint][formattableEndpoint].
     *
     * Additional query parameters can be specified through [FormattedRoute.query].
     *
     * @since  1.0
     * @author Kaidan Gustave
     */
    class FormattedRoute
    internal constructor(val base: Route, val rateLimitEndpoint: String, val formattedEndpoint: String) {
        val method: Method
            get() = base.method

        fun query(vararg params: Pair<String, Any>): FormattedRoute {
            val withQueryParams = buildString {
                append(formattedEndpoint)
                for(i in 0 until params.size) {
                    if(i == 0) append("?") else append("&")
                    append(params[i].let { "${it.first}=${it.second}" })
                }
            }

            return FormattedRoute(base, rateLimitEndpoint, withQueryParams)
        }

        override fun equals(other: Any?): Boolean {
            if(other !is FormattedRoute)
                return false

            return base == other.base && formattedEndpoint == other.formattedEndpoint
        }

        override fun hashCode(): Int = "$formattedEndpoint$method".hashCode()
        override fun toString(): String = "$method - $formattedEndpoint"
    }

    class RateLimit(val maxUses: Int, val resetTime: Long)

    // ROUTES //

    // A note regarding user accounts:
    //
    // This API does and will never support the automation of user accounts in any way,
    // shape, or form. When taking a look at the Discord Developer documentation you'll
    // find it clearly states:
    //
    // "Automating normal user accounts (generally called "self-bots") outside of the
    // OAuth2/bot API is forbidden, and can result in an account termination if found."
    //
    // Source: https://discordapp.com/developers/docs/topics/oauth2#bot-vs-user-accounts
    //
    // Whether you agree with it or not, this practice is heavily frowned upon and can
    // result in your discord account being deleted.
    // So for the respect of Discord and their wishes this library WILL NEVER support
    // automation of /gateway or other non-bot routes.

    object GatewayBot : Route(GET, "/gateway/bot")





    // Self
    object GetSelf : Route(GET, "/users/@me")
    object GetSelfGuilds : Route(GET, "/users/@me/guilds")
    object GetSelfDMs : Route(GET, "/users/@me/channels")
    object ModifySelf : Route(PATCH, "/users/@me")
    object LeaveGuild : Route(DELETE, "/users/@me/guilds/{guild.id}", "guild.id")









    object GetUser : Route(GET, "/users/{user.id}")


    // Channel
    object CreateMessage : Route(POST, "/channels/{channel.id}/messages", "channel.id")

    // Member
    object ModifyGuildMember : Route(PATCH, "/guilds/{guild.id}/members/{user.id}", "guild.id")

    // Guild
    object GetGuild : Route(GET, "/guilds/{guild.id}" , "guild.id")

    // Custom
    object Fake : Route(GET, "FAKE")
}
