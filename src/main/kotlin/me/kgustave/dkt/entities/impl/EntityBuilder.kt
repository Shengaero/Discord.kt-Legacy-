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
@file:Suppress("LoopToCallChain", "MemberVisibilityCanBePrivate")

package me.kgustave.dkt.entities.impl

import me.kgustave.dkt.annotations.InternalOnly
import me.kgustave.dkt.entities.*
import me.kgustave.dkt.handlers.event.EventHandler
import me.kgustave.dkt.handlers.event.GuildMembersChunkHandler
import me.kgustave.dkt.handlers.event.ReadyHandler
import me.kgustave.dkt.requests.OpCode
import me.kgustave.dkt.util.createLogger
import me.kgustave.dkt.util.snowflake
import me.kgustave.kson.KSONArray
import me.kgustave.kson.KSONObject
import me.kgustave.kson.kson
import java.awt.Color

/**
 * Internal builder class for all Kotlincord entities.
 *
 * You should not ever use this!
 *
 * @since  1.0.0
 * @author Kaidan Gustave
 */
@InternalOnly
class EntityBuilder(private val api: APIImpl) {
    companion object {
        private val LOG = createLogger(EntityBuilder::class)
    }

    private val rawGuilds = HashMap<Long, KSONObject>()
    private val guildCallbacks = HashMap<Long, (Guild) -> Unit>()

    fun createSelf(self: KSONObject): SelfUser {
        val id = snowflake(self["id"])
        val discriminator = self["discriminator"].toString().toInt()
        val name = self["username"] as String

        val selfImpl = SelfUserImpl(id, api, name, discriminator)
        selfImpl.avatarId = self.opt("avatar_id")

        LOG.debug("Created SelfUser, attaching to API")
        api.self = selfImpl
        api.internalUserCache.entityMap[id] = selfImpl
        return selfImpl
    }

    fun createUser(user: KSONObject, shouldCache: Boolean = true): User {

        /*
        "user": {
          "username": "Yggdrasil",
          "id": "247283454440374274",
          "discriminator": "4195",
          "bot": true,
          "avatar": "d6ec7e9e1658b246d0a1e50684df689e"
        }
         */

        val id = snowflake(user["id"])

        // If we already have the user
        // This will also return ourself if it's referring to us.
        getUser(id)?.let { return it }

        // Just in case something slips through, we do not want to have a
        // UserImpl created (and possibly cached) for our currently logged
        // in account. This should realistically never happen, but let's
        // make sure that we properly report an error if it does.
        if(id == api.self.id) {
            return api.self
        }

        val username = user["username"] as String
        val discriminator = (user["discriminator"] as String).toInt()
        val isBot = user.opt("bot") ?: false // If it is not included it is false
        val avatarId = user.opt<String>("avatar")

        val userImpl = UserImpl(id, api)

        userImpl.name = username
        userImpl.discriminator = discriminator
        userImpl.isBot = isBot
        userImpl.avatarId = avatarId

        // Cache the user?
        if(shouldCache) {
            api.internalUserCache.entityMap[id] = userImpl
        }

        return userImpl
    }

    fun createPrivateChannel(channel: KSONObject): PrivateChannel {
        // This is also checked in every EventHandler that might call this
        // method but just for consistency's sake, we check this anyways
        require("recipients" !in channel) { "Detected a PrivateChannel JSON that matched a Group " }

        val recipient = channel["recipient"] as KSONObject
        val userId = snowflake(recipient["id"])

        require(userId != api.self.id) { "Attempted to create a PrivateChannel where the recipient was SelfUser!" }

        val userImpl = (getUser(userId) ?: createUser(recipient, shouldCache = false)) as UserImpl

        return createPrivateChannel(channel, userImpl)
    }

    fun createPrivateChannel(channel: KSONObject, userImpl: UserImpl): PrivateChannel {
        val id = snowflake(channel["id"])
        return PrivateChannelImpl(api, id, userImpl)
    }

    fun createMember(member: KSONObject, guildImpl: GuildImpl): Member {
        val userKson = member["user"] as KSONObject
        val user = createUser(userKson)
        val memberImpl = MemberImpl(api, guildImpl, user)

        // Set nickname
        memberImpl.nickname = member.opt("nickname")

        val roles = member["roles"] as KSONArray
        roles.forEach {
            val roleId = snowflake(it!!)
            val role = guildImpl.roleCache.entityMap[roleId]

            if(role !== null) {
                memberImpl.internalRoles += role
            } else {
                // It is possible this might happen, so we cover it.
                LOG.warn("While creating member, originating payload had a role that was not registered. " +
                         "Please report this to the maintainers of the library! $member")
            }
        }

        return memberImpl
    }

    fun createGuild(guild: KSONObject, callback: ((Guild) -> Unit)? = null) {
        val guildId = snowflake(guild["id"]) // This should never fail

        val guildMap = api.internalGuildCache.entityMap

        // We are either dealing with a previously unavailable
        // Guild, or we are dealing with a brand new Guild.
        val guildImpl = guildMap[guildId] as? GuildImpl ?: GuildImpl(guildId, api, false).also {
            // If we already haven't registered the Guild
            // to the guildMap, we need to now.
            guildMap[guildId] = it
        }

        val isUnavailable = guild.containsKey("unavailable") && !guild.isNull("unavailable")
                            && guild["unavailable"] as Boolean

        // We are unavailable, we need to create a new unavailable guild,
        // and register it to the queue. This will most often be on READY
        if(isUnavailable) {
            guildImpl.unavailable = true
            callback?.let { it(guildImpl) }
            return api.guildQueue.register(guildId)
        }

        // Now that we are passed making sure that this Guild has been properly mapped
        // we should start setting it up.
        with(guildImpl) {
            unavailable = isUnavailable // This should always be true

            name = guild["name"] as String
            iconId = guild.opt("icon")
            splashId = guild.opt("splash")

            mfaLevel = Guild.MFALevel.typeOf(guild["mfa_level"] as Int)
            defaultNotificationLevel = Guild.NotificationLevel.typeOf(guild["default_message_notifications"] as Int)
            explicitContentFilter = Guild.ExplicitContentFilter.typeOf(guild["explicit_content_filter"] as Int)
            verificationLevel = Guild.VerificationLevel.typeOf(guild["verification_level"] as Int)
            features = guild.opt<KSONArray>("features")?.mapTo(HashSet()) { it.toString() } ?: HashSet()
        }

        val roleMap = guildImpl.roleCache.entityMap

        // Setup roles
        for(rawRole in guild["roles"] as KSONArray) {
            // We need to wait for members to even be set up first
            val roleImpl = createRole(rawRole as KSONObject, guildImpl)
            roleMap[roleImpl.id] = roleImpl
        }

        guild.opt<KSONArray>("emojis")?.let { emojis ->
            val emoteMap = guildImpl.emoteCache.entityMap
            for(rawEmoji in emojis) {
                val ksonEmoji = rawEmoji as KSONObject

                // These are necessary and since emotes can (apparently) be kinda
                // flakey, we check if the two required parameters even exist.
                if("id" !in ksonEmoji) {
                    LOG.error("Got emote JSON without 'id' value: $ksonEmoji")
                    continue
                } else if("name" !in ksonEmoji) {
                    LOG.error("Got emote JSON without 'name' value: $ksonEmoji")
                    continue
                }

                val emote = createGuildEmote(ksonEmoji, guildImpl)
                emoteMap[emote.id] = emote
            }
        }

        val members = guild.opt<KSONArray>("members")

        members?.forEach { member ->
            // Note: We do this after "roles" so that we
            // can sync up this user's roles with this call.
            createMember(member as KSONObject, guildImpl)
        }

        guildImpl.getMemberById(snowflake(guild["owner_id"]))?.let {
            guildImpl.owner = it
        }

        guild.opt<KSONArray>("channels")?.let { channels ->
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
        guild.opt<String>("system_channel_id")?.let {
            guildImpl.systemChannel = guildImpl.textChannelCache[snowflake(it)]
        }

        // Setup AFK Channel
        guild.opt<String>("afk_channel_id")?.let {
            guildImpl.afkChannel = guildImpl.voiceChannelCache[snowflake(it)]
        }

        val handlers = api.websocket.handlers
        val expected = guild["member_count"] as Int

        if(members !== null && members.size != expected) {
            rawGuilds[guildId] = guild
            callback?.let { guildCallbacks[guildId] = it }

            val guildChunkHandler = handlers[EventHandler.Type.GUILD_MEMBERS_CHUNK] as GuildMembersChunkHandler
            guildChunkHandler.setExpectedGuildMembers(guildId, expected)

            // We are past ready, just add the request and it will happen when it does
            if(api.websocket.ready) {
                api.websocket.queueChunkRequest(kson {
                    "op" to OpCode.REQUEST_GUILD_MEMBERS
                    "d" to kson {
                        "guild_id" to guildId
                        "query" to ""
                        "limit" to 0
                    }
                })
            } else {
                val readyHandler = handlers[EventHandler.Type.READY] as ReadyHandler
                readyHandler.acknowledgeGuild(guildId, unavailable = false, shouldChunk = true)
            }

            // Put this guild into the queue.
            // It will be free once it is done chunking.
            return api.guildQueue.register(guildId)
        }

        api.guildQueue.unregister(guildId)
        callback?.let { it(guildImpl) }
    }

    fun handleGuildMemberChunks(guildId: Long, memberChunks: List<KSONArray>) {
        val guild = requireNotNull(rawGuilds[guildId]) {
            "Attempted to handle guild member chunks for a null guild (ID: $guildId)"
        }
        val callback = requireNotNull(guildCallbacks[guildId]) {
            "Attempted to handle guild member chunks for a guild with a null callback (ID: $guildId)"
        }
        val guildImpl = requireNotNull(api.internalGuildCache.entityMap[guildId] as? GuildImpl) {
            "Attempted to handle guild member chunks for a guild with a null GuildImpl (ID: $guildId)"
        }

        memberChunks.forEach { memberChunk ->
            memberChunk.forEach { member ->
                createMember(member as KSONObject, guildImpl)
            }
        }

        val owner = guildImpl.memberCache[snowflake(guild["owner_id"])]
        if(owner !== null) { // Set the owner
            guildImpl.owner = owner
        } else if(!guildImpl.ownerIsInitialized) {
            // This is pretty much impossible, but like a lot of things, we log it
            // just in case it does somehow break.
            LOG.error("Somehow the owner of a guild was not set even after chunking")
        }

        callback(guildImpl)
        api.guildQueue.unregister(guildId)
    }

    fun createRole(role: KSONObject, guildImpl: GuildImpl): Role {
        val roleImpl = RoleImpl(
            id = snowflake(role["id"]),
            api = api,
            guild = guildImpl,
            color = role.opt<Int>("color")?.let { Color(it) },
            name = role["name"] as String,
            isMentionable = role.opt<Boolean>("mentionable") == true,
            rawPosition = role["position"] as Int,
            rawPermissions = role["permissions"].toString().toLong()
        )
        if(roleImpl.id == guildImpl.id) {
            guildImpl.everyoneRole = roleImpl
        }
        return roleImpl
    }

    fun createGuildEmote(emote: KSONObject, guildImpl: GuildImpl): GuildEmote {
        val id = snowflake(emote["id"])
        val emoteImpl = GuildEmoteImpl(
            guildImpl, api, id,
            name = emote["name"] as String,
            isAnimated = emote.opt("animated") ?: false,
            isManaged = emote.opt("managed") ?: false
        )

        val rolesMap = guildImpl.roleCache.entityMap
        emote.opt<KSONArray>("roles")?.forEach {
            val roleId = snowflake(it!!)
            val role = rolesMap[roleId] ?:
                return@forEach LOG.warn("Could not map role (ID: $it) to emote (ID: $id) because " +
                                        "it could not be found in the guild (ID: ${guildImpl.id}) " +
                                        "role cache!")
            emoteImpl.internalRoles.add(role)
        }

        return emoteImpl
    }

    fun createMessage(message: KSONObject, channel: MessageChannel): Message {
        val messageId = snowflake(message["id"])
        val content = message.opt<String>("content")

        val author = message["author"] as KSONObject
        val authorId = snowflake(author["id"])

        val embeds = emptyList<Embed>() // TODO Embeds
        val attachments = message.opt<KSONArray>("attachments")?.mapNotNull {
            (it as? KSONObject)?.let { createMessageAttachment(it) }
        } ?: emptyList()

        val userAuthor: User = when(channel) {
            is PrivateChannel -> if(authorId == api.self.id) api.self else channel.recipient
            is TextChannel -> TODO("Unimplemented")
            else -> throw IllegalArgumentException("Invalid channel. Cannot handle channel type: ${channel.type}")
        }

        val messageTypeInt = message["type"] as Int
        val messageType = Message.Type.typeOf(messageTypeInt)

        require(messageType != Message.Type.UNKNOWN) { "Invalid message. Cannot handle message type: $messageTypeInt" }

        if(messageType == Message.Type.DEFAULT) {
            @Suppress("USELESS_IS_CHECK") // TODO Remove when able
            if(channel is PrivateChannel) {
                return PrivateMessageImpl(
                    id = messageId,
                    type = messageType,
                    api = api,
                    channel =  channel,
                    author = userAuthor,
                    content = content ?: "",
                    embeds = embeds,
                    attachments = attachments
                )
            } else {
                TODO("Unimplemented Text Messages")
            }
        } else {
            TODO("Unimplemented System Messages")
        }
    }

    fun createMessageAttachment(attachment: KSONObject): Message.Attachment {
        return Message.Attachment(
            api = api,
            id = snowflake(attachment["id"]), // Note this isn't a snowflake but we can parse it safely in the same way
            url = attachment.opt("url"),
            proxyUrl = attachment.opt("proxy_url"),
            filename = attachment["filename"] as String,
            size = attachment["size"] as Int,
            height = attachment.opt("height") ?: -1,
            width = attachment.opt("width") ?: -1
        )
    }

    private fun getUser(id: Long): UserImpl? = api.internalUserCache.entityMap[id] as? UserImpl
}
