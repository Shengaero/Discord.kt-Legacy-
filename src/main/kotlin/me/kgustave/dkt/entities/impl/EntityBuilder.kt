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

package me.kgustave.dkt.entities.impl

import me.kgustave.dkt.entities.Channel
import me.kgustave.dkt.entities.Guild
import me.kgustave.dkt.entities.User
import me.kgustave.dkt.util.snowflake
import me.kgustave.kson.KSONArray
import me.kgustave.kson.KSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Internal builder class for all Kotlincord entities.
 *
 * You should not ever use this!
 *
 * @since  1.0.0
 * @author Kaidan Gustave
 */
@Suppress("LoopToCallChain", "MemberVisibilityCanPrivate")
class EntityBuilder(val api: APIImpl) {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(EntityBuilder::class.java)
    }

    fun createSelf(kson: KSONObject) {
        val id = snowflake(kson["id"])
        val discriminator = kson["discriminator"].toString().toInt()
        val name = kson["username"] as String

        val self = SelfUserImpl(id, api, name, discriminator)
        self.avatarId = kson.opt("avatar_id")

        LOG.debug("Created SelfUser, attaching to API")
        api.self = self
    }

    fun createUser(kson: KSONObject, shouldCache: Boolean = true): User {

        /*
        "user": {
          "username": "Yggdrasil",
          "id": "247283454440374274",
          "discriminator": "4195",
          "bot": true,
          "avatar": "d6ec7e9e1658b246d0a1e50684df689e"
        }
         */

        val id = snowflake(kson["id"])

        // We don't get the internal cache if we are not going to cache it anyways
        // userMap?.let == if(shouldCache)
        val userMap = if(shouldCache) api.internalUserCache.entityMap else null

        userMap?.let {
            if(it.containsKey(id))
                return it[id]!!
        }

        val username = kson["username"] as String
        val discriminator = (kson["discriminator"] as String).toInt()
        val isBot = kson.opt("bot") ?: false // If it is not included it is false
        val avatarId = kson.opt<String>("avatar")

        val user = UserImpl(id, api)

        user.name = username
        user.discriminator = discriminator
        user.isBot = isBot
        user.avatarId = avatarId

        // Cache the user?
        userMap?.let { it[id] = user }

        return user
    }

    fun createMember(kson: KSONObject, guild: GuildImpl) {
        val userKson = kson["user"] as KSONObject

        val user = createUser(userKson)

        val member = MemberImpl(api, guild, user)

        val rolesArray = kson["roles"] as KSONArray
        val internalRoles = member.internalRoles
        val roleMap = guild.roleCache.entityMap

        rolesArray.forEach {
            val roleId = snowflake(it!!)
            val role = roleMap[roleId]!!

            internalRoles.add(role)
        }

        member.nickname = kson.opt("nickname")
    }

    fun createNewGuild(kson: KSONObject) {
        val guildId = snowflake(kson["id"]) // This should never fail

        val guildMap = api.internalGuildCache.entityMap

        // We are either dealing with a previously unavailable
        // Guild, or we are dealing with a brand new Guild.
        val guild = guildMap[guildId] as? GuildImpl ?: GuildImpl(guildId, api, false).also {
            // If we already haven't registered the Guild to the
            // guildMap, we need to now.
            guildMap[guildId] = it
        }

        val u = kson.containsKey("unavailable") && !kson.isNull("unavailable") && kson["unavailable"] as Boolean

        // We are unavailable, we need to create a new unavailable guild,
        // and register it to the queue. This will most often be on READY
        if(u) {
            guild.unavailable = true
            return api.guildQueue.register(guildId)
        }

        // Now that we are passed making sure that this Guild has been properly mapped
        // we should start setting it up.
        with(guild) {
            unavailable = u // This should always be true

            name = kson["name"] as String
            iconId = kson.opt("icon")
            splashId = kson.opt("splash")

            mfaLevel = Guild.MFALevel.typeOf(kson["mfa_level"] as Int)
            defaultNotificationLevel = Guild.NotificationLevel.typeOf(kson["default_message_notifications"] as Int)
            explicitContentFilter = Guild.ExplicitContentFilter.typeOf(kson["explicit_content_filter"] as Int)
            verificationLevel = Guild.VerificationLevel.typeOf(kson["verification_level"] as Int)
            features = kson.opt<KSONArray>("features")?.mapTo(HashSet()) { it.toString() } ?: HashSet()
        }

        val rolesMap = guild.roleCache.entityMap

        // Setup roles
        for(rawRole in kson["roles"] as KSONArray) {
            val role = createRole(rawRole as KSONObject, guild.id)
            rolesMap[role.id] = role.also {
                if(it.id == guild.id) {
                    guild.everyoneRole = it // This also sets RoleImpl.isEveryoneRole
                }
            }
        }

        kson.opt<KSONArray>("emojis")?.let { emojis ->
            for(rawEmoji in emojis) {
                // TODO Emoji Setup for Guilds
            }
        }

        kson.opt<KSONArray>("members")?.let { members ->
            for(rawMember in members) {
                // Note: We do this after "roles" so that we
                // can sync up this user's roles with this call.
                createMember(rawMember as KSONObject, guild)
            }
        }

        guild.getMemberById(snowflake(kson["owner_id"]))?.let {
            guild.owner = it
        }

        kson.opt<KSONArray>("channels")?.let { channels ->
            for(rawChannel in channels) {
                val channelKson = rawChannel as KSONObject
                val type = Channel.Type.typeOf(channelKson["type"] as Int)
                when(type) {
                    Channel.Type.TEXT -> {
                        // TODO TextChannel Setup for Guilds
                    }

                    Channel.Type.VOICE -> {
                        // TODO VoiceChannel Setup for Guilds
                    }

                    Channel.Type.CATEGORY -> {
                        // TODO Category Setup for Guilds
                    }

                    else -> LOG.warn("Could not process unknown channel: $channelKson")
                }
            }
        }

        // Setup System Channel
        kson.opt<String>("system_channel_id")?.let {
            guild.systemChannel = guild.textChannelCache[snowflake(it)]
        }

        // Setup AFK Channel
        kson.opt<String>("afk_channel_id")?.let {
            guild.afkChannel = guild.voiceChannelCache[snowflake(it)]
        }
    }

    fun createRole(rawRole: KSONObject, guildId: Long): RoleImpl {
        return RoleImpl(snowflake(rawRole["id"]), api).also {
            val guild = api.internalGuildCache.entityMap[guildId]

            // This shouldn't ever happen (I hope) but just in case
            // it does we need to make sure that we make it error.
            it.guild = requireNotNull(guild) {
                "Tried to create a role for a Guild that didn't exist?"
            }
        }
    }
}
