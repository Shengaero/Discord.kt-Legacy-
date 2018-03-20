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

import me.kgustave.dkt.entities.*
import me.kgustave.dkt.handlers.event.EventHandler
import me.kgustave.dkt.handlers.event.GuildMembersChunkHandler
import me.kgustave.dkt.handlers.event.ReadyHandler
import me.kgustave.dkt.requests.OpCode
import me.kgustave.dkt.util.createLogger
import me.kgustave.dkt.util.snowflake
import me.kgustave.json.JSArray
import me.kgustave.json.JSObject
import me.kgustave.json.jsonObject
import java.awt.Color
import java.time.OffsetDateTime

/**
 * Internal builder class for all Kotlincord entities.
 *
 * You should not ever use this!
 *
 * @since  1.0.0
 * @author Kaidan Gustave
 */
internal class EntityBuilder(private val api: APIImpl) {
    companion object {
        private val LOG = createLogger(EntityBuilder::class)
    }

    private val rawGuilds = HashMap<Long, JSObject>()
    private val guildCallbacks = HashMap<Long, (Guild) -> Unit>()

    fun createSelf(self: JSObject): SelfUser {
        val id = snowflake(self["id"])
        val discriminator = self["discriminator"].toString().toInt()
        val name = self["username"] as String

        val selfImpl = SelfUserImpl(id, api, name, discriminator)
        selfImpl.avatarId = self.opt("avatar_id")

        LOG.debug("Created SelfUser, attaching to API")
        api.internalSelf = selfImpl
        api.userMap[id] = selfImpl
        return selfImpl
    }

    fun createUser(user: JSObject, shouldCache: Boolean = true): User {

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
        // This will also return our self user if it's referring to us.
        (api.userMap[id] as? UserImpl)?.let { return it }

        // Just in case something slips through, we do not want to have a
        // UserImpl created (and possibly cached) for our currently logged
        // in account. This should realistically never happen, but let's
        // make sure that we properly report an error if it does.
        if(id == api.self.id) {
            return api.self
        }

        val username = user.string("username")
        val discriminator = user.string("discriminator").toInt()
        val isBot = user.opt("bot") ?: false // If it is not included it is false
        val avatarId = user.opt<String>("avatar")

        val userImpl = UserImpl(id, api)

        userImpl.name = username
        userImpl.discriminator = discriminator
        userImpl.isBot = isBot
        userImpl.avatarId = avatarId

        // Cache the user?
        if(shouldCache) {
            api.userMap[id] = userImpl
        }

        return userImpl
    }

    fun createWebhookUser(webhook: JSObject): Webhook {
        TODO("implement webhook users")
    }

    fun createPermissionOverridesForChannel(channel: JSObject) {
        val id = snowflake(channel["id"])
        val typeInt = channel.int("type")
        val type = Channel.Type.typeOf(typeInt)
        val channelImpl: AbstractGuildChannelImpl? = when(type) {
            Channel.Type.TEXT -> api.textChannelMap[id] as? TextChannelImpl
            Channel.Type.VOICE -> api.voiceChannelMap[id] as? VoiceChannelImpl
            Channel.Type.CATEGORY -> api.categoryMap[id] as? CategoryImpl
            else -> return LOG.warn("Got request to create permission override for channel type: $typeInt\n$channel")
        }

        channelImpl?.let {
            val overrides = channel.array("permission_overwrites")
            createPermissionOverrides(overrides, channelImpl)
        }
    }

    fun createTextChannel(channel: JSObject, guildId: Long, loaded: Boolean = true): TextChannel {
        val id = snowflake(channel["id"])
        val textChannelImpl = api.textChannelMap[id] as? TextChannelImpl ?: let {
            val guildImpl = requireNotNull(api.guildMap[guildId] as? GuildImpl) {
                "Tried to create a TextChannel for a guild that was not cached"
            }
            TextChannelImpl(id, api, guildImpl).also { chan ->
                guildImpl.textChannelMap[id] = chan
                api.textChannelMap[id] = chan
            }
        }

        if(!channel.isNull("permission_overwrites") && loaded) {
            val overrides = channel.array("permission_overwrites")
            createPermissionOverrides(overrides, textChannelImpl)
        }

        with(textChannelImpl) {
            name = channel.string("name")
            topic = channel.opt("topic")
            parentId = channel.opt<String>("parent_id")?.let { snowflake(it) } ?: 0L
            rawPosition = channel.int("position")
        }

        return textChannelImpl
    }

    fun createVoiceChannel(channel: JSObject, guildId: Long, loaded: Boolean = true): VoiceChannel {
        val id = snowflake(channel["id"])
        val voiceChannelImpl = api.voiceChannelMap[id] as? VoiceChannelImpl ?: let {
            val guildImpl = requireNotNull(api.guildMap[guildId] as? GuildImpl) {
                "Tried to create a TextChannel for a guild that was not cached"
            }
            VoiceChannelImpl(api, id, guildImpl).also { chan ->
                guildImpl.voiceChannelMap[id] = chan
                api.voiceChannelMap[id] = chan
            }
        }

        if(!channel.isNull("permission_overwrites") && loaded) {
            val overrides = channel.array("permission_overwrites")
            createPermissionOverrides(overrides, voiceChannelImpl)
        }

        with(voiceChannelImpl) {
            name = channel["name"] as String
            userLimit = channel["user_limit"] as Int
            parentId = channel.opt<String>("parent_id")?.let { snowflake(it) } ?: 0L
            rawPosition = channel["position"] as Int
        }

        return voiceChannelImpl
    }

    fun createCategory(category: JSObject, guildId: Long, loaded: Boolean = true): Category {
        val id = snowflake(category["id"])

        val categoryImpl = api.categoryMap[id] as? CategoryImpl ?: let {
            val guildImpl = requireNotNull(api.guildMap[guildId] as? GuildImpl) {
                "Tried to create a Category for a guild that was not cached"
            }
            CategoryImpl(api, id, guildImpl).also { cat ->
                guildImpl.categoryMap[id] = cat
                api.categoryMap[id] = cat
            }
        }

        if(!category.isNull("permission_overwrites") && loaded) {
            val overrides = category.array("permission_overwrites")
            createPermissionOverrides(overrides, categoryImpl)
        }

        with(categoryImpl) {
            name = category["name"] as String
            rawPosition = category["position"] as Int
        }

        return categoryImpl
    }

    fun createPermissionOverrides(overrides: JSArray, guildChannelImpl: AbstractGuildChannelImpl) {
        overrides.forEach {
            val override = it as? JSObject
            if(override === null) {
                LOG.warn("Encountered a value in overrides array that was not a JSON object: $it")
                return@forEach
            }

            if(!createPermissionOverride(override, guildChannelImpl)) {
                // This means that we could not find a member, which could be caused by
                // discord sending us a payload that has one missing from our cache because
                // they left the guild.
                LOG.debug("Unable to create a permission override for Channel (ID: ${guildChannelImpl.id}) " +
                          "due to a missing member or role.")
            }
        }
    }

    fun createPermissionOverride(override: JSObject, guildChannelImpl: AbstractGuildChannelImpl): Boolean {
        val id = snowflake(override["id"])
        val rawAllow = snowflake(override["allow"])
        val rawDeny = snowflake(override["deny"])
        val type = override.string("type")

        when(type) {
            "member" -> {
                val member = guildChannelImpl.guild.getMemberById(id) ?: return false
                val overrideImpl = (guildChannelImpl.getPermissionOverride(member) as? MemberPermissionOverrideImpl) ?:
                                   MemberPermissionOverrideImpl(api, guildChannelImpl, id, member).also {
                                       guildChannelImpl.internalMemberOverrides[id] = it
                                   }
                overrideImpl.allowedRaw = rawAllow
                overrideImpl.deniedRaw = rawDeny
            }

            "role" -> {
                val role = guildChannelImpl.guild.getRoleById(id) ?: return false
                val overrideImpl = (guildChannelImpl.getPermissionOverride(role) as? RolePermissionOverrideImpl) ?:
                                   RolePermissionOverrideImpl(api, guildChannelImpl, id, role).also {
                                       guildChannelImpl.internalRoleOverrides[id] = it
                                   }
                overrideImpl.allowedRaw = rawAllow
                overrideImpl.deniedRaw = rawDeny
            }

            else -> LOG.warn("Encountered a permission override JSON with an unknown type: '$type'")
        }

        return true
    }

    fun createVoiceState(voiceState: JSObject, guildImpl: GuildImpl) {
        val voiceChannelImpl = guildImpl.getVoiceChannelById(snowflake(voiceState["channel_id"])) as? VoiceChannelImpl ?:
                               return LOG.error("Got a voice state for a voice channel that was not cached")
        val memberImpl = guildImpl.getMemberById(snowflake(voiceState["user_id"])) as? MemberImpl ?:
                         return LOG.error("Got a voice state for a member that was not cached")

        with(memberImpl.voiceState as GuildVoiceStateImpl) {
            channel = voiceChannelImpl
            sessionId = voiceState.string("session_id")
            mute = voiceState["mute"] as Boolean
            deaf = voiceState["deaf"] as Boolean
            selfMute = voiceState["self_mute"] as Boolean
            selfDeaf = voiceState["self_deaf"] as Boolean
        }
    }

    fun createPrivateChannel(channel: JSObject): PrivateChannel {
        // This is also checked in every EventHandler that might call this
        // method but just for consistency's sake, we check this anyways
        require("recipients" !in channel) { "Detected a PrivateChannel JSON that matched a Group " }

        val recipient = channel.obj("recipient")
        val userId = snowflake(recipient["id"])

        require(userId != api.self.id) { "Attempted to create a PrivateChannel where the recipient was SelfUser!" }

        val userImpl = (api.userMap[userId] as? UserImpl ?: createUser(recipient, shouldCache = false)) as UserImpl

        return createPrivateChannel(channel, userImpl)
    }

    fun createPrivateChannel(channel: JSObject, userImpl: UserImpl): PrivateChannel {
        val id = snowflake(channel["id"])
        return PrivateChannelImpl(id, api, userImpl).also {
            api.privateChannelMap[id] = it
        }
    }

    fun createMember(member: JSObject, guildImpl: GuildImpl) {
        val userKson = member.obj("user")
        val user = createUser(userKson)
        val memberImpl = MemberImpl(api, guildImpl, user)

        // Set nickname
        memberImpl.nickname = member.opt("nickname")

        val roles = member.array("roles")
        roles.forEach {
            val roleId = snowflake(it!!)
            val role = guildImpl.roleMap[roleId]

            if(role !== null) {
                memberImpl.internalRoles += role
            } else {
                // It is possible this might happen, so we cover it.
                LOG.warn("While creating member, originating payload had a role that was not registered. " +
                         "Please report this to the maintainers of the library! $member")
            }
        }

        guildImpl.memberMap[memberImpl.user.id] = memberImpl
    }

    fun createGuild(guild: JSObject, callback: ((Guild) -> Unit)? = null) {
        val guildId = snowflake(guild["id"]) // This should never fail

        val guildMap = api.guildMap

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

            name = guild.string("name")
            iconId = guild.opt("icon")
            splashId = guild.opt("splash")

            mfaLevel = Guild.MFALevel.typeOf(guild.int("mfa_level"))
            defaultNotificationLevel = Guild.NotificationLevel.typeOf(guild.int("default_message_notifications"))
            explicitContentFilter = Guild.ExplicitContentFilter.typeOf(guild.int("explicit_content_filter"))
            verificationLevel = Guild.VerificationLevel.typeOf(guild.int("verification_level"))
            features = guild.opt<JSArray>("features")?.mapTo(HashSet()) { it.toString() } ?: HashSet()
        }

        // Setup roles
        for(rawRole in guild.array("roles")) {
            // We need to wait for members to even be set up first
            createRole(rawRole as JSObject, guildImpl)
        }

        guild.opt<JSArray>("emojis")?.let { emojis ->
            for(rawEmoji in emojis) {
                val ksonEmoji = rawEmoji as JSObject

                // These are necessary and since emotes can (apparently) be kinda
                // flakey, we check if the two required parameters even exist.
                if("id" !in ksonEmoji) {
                    LOG.error("Got emote JSON without 'id' value: $ksonEmoji")
                    continue
                } else if("name" !in ksonEmoji) {
                    LOG.error("Got emote JSON without 'name' value: $ksonEmoji")
                    continue
                }

                createGuildEmote(ksonEmoji, guildImpl)
            }
        }

        val members = guild.opt<JSArray>("members")

        members?.forEach { member ->
            // Note: We do this after "roles" so that we
            // can sync up this user's roles with this call.
            createMember(member as JSObject, guildImpl)
        }

        guildImpl.getMemberById(snowflake(guild["owner_id"]))?.let {
            guildImpl.owner = it
        }

        guild.opt<JSArray>("channels")?.let { channels ->
            for(channel in channels) {
                val type = Channel.Type.typeOf((channel as JSObject).int("type"))
                when(type) {
                    Channel.Type.TEXT -> createTextChannel(channel, guildId, false)
                    Channel.Type.VOICE -> createVoiceChannel(channel, guildId, false)
                    Channel.Type.CATEGORY -> createCategory(channel, guildId, false)
                    else -> LOG.warn("Could not process unknown channel: $channel")
                }
            }
        }

        // Setup System Channel
        guild.opt<String>("system_channel_id")?.let {
            guildImpl.systemChannel = guildImpl.textChannelMap[snowflake(it)]
        }

        // Setup AFK Channel
        guild.opt<String>("afk_channel_id")?.let {
            guildImpl.afkChannel = guildImpl.voiceChannelMap[snowflake(it)]
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
                api.websocket.queueChunkRequest(jsonObject {
                    "op" to OpCode.REQUEST_GUILD_MEMBERS
                    "d" to jsonObject {
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

        // Setup permission overrides for channels
        (guild.array("channels")).forEach { createPermissionOverridesForChannel(it as JSObject) }
        (guild.array("voice_states")).forEach { createVoiceState(it as JSObject, guildImpl) }

        api.guildQueue.unregister(guildId)
        callback?.let { callback(guildImpl) }
    }

    fun handleGuildMemberChunks(guildId: Long, memberChunks: List<JSArray>) {
        val guild = requireNotNull(rawGuilds[guildId]) {
            "Attempted to handle guild member chunks for a null guild (ID: $guildId)"
        }
        val callback = requireNotNull(guildCallbacks[guildId]) {
            "Attempted to handle guild member chunks for a guild with a null callback (ID: $guildId)"
        }
        val guildImpl = requireNotNull(api.guildMap[guildId] as? GuildImpl) {
            "Attempted to handle guild member chunks for a guild with a null GuildImpl (ID: $guildId)"
        }

        memberChunks.forEach { memberChunk ->
            memberChunk.forEach { member ->
                createMember(member as JSObject, guildImpl)
            }
        }

        val owner = guildImpl.getMemberById(snowflake(guild["owner_id"]))
        if(owner !== null) { // Set the owner
            guildImpl.owner = owner
        }

        if(!guildImpl.ownerIsInitialized) {
            // This is pretty much impossible, but like a lot of things, we log it
            // just in case it does somehow break.
            LOG.error("Somehow the owner of a guild was not set even after chunking")
        }

        // Setup permission overrides for channels
        (guild.array("channels")).forEach { createPermissionOverridesForChannel(it as JSObject) }
        (guild.array("voice_states")).forEach { createVoiceState(it as JSObject, guildImpl) }

        callback(guildImpl)
        api.guildQueue.unregister(guildId)
    }

    fun createRole(role: JSObject, guildImpl: GuildImpl) {
        val roleImpl = RoleImpl(
            id = snowflake(role["id"]),
            api = api,
            guild = guildImpl,
            color = role.opt<Int>("color")?.let { Color(it) },
            name = role.string("name"),
            isMentionable = role.opt<Boolean>("mentionable") == true,
            rawPosition = role.int("position"),
            rawPermissions = role["permissions"].toString().toLong()
        )

        guildImpl.roleMap[roleImpl.id] = roleImpl

        if(roleImpl.id == guildImpl.id) {
            guildImpl.everyoneRole = roleImpl
        }
    }

    fun createGuildEmote(emote: JSObject, guildImpl: GuildImpl) {
        val id = snowflake(emote["id"])
        val emoteImpl = GuildEmoteImpl(
            guildImpl, api, id,
            name = emote.string("name"),
            isAnimated = emote.opt("animated") ?: false,
            isManaged = emote.opt("managed") ?: false
        )

        val rolesMap = guildImpl.roleMap
        emote.opt<JSArray>("roles")?.forEach {
            val roleId = snowflake(it!!)
            val role = rolesMap[roleId] ?:
                return@forEach LOG.warn("Could not map role (ID: $it) to emote (ID: $id) because " +
                                        "it could not be found in the guild (ID: ${guildImpl.id}) " +
                                        "role cache!")
            emoteImpl.internalRoles.add(role)
        }
    }

    fun createMessage(message: JSObject, channel: MessageChannel): Message {
        val messageId = snowflake(message["id"])
        val content = message.opt<String>("contentBuilder")

        val author = message.obj("author")
        val authorId = snowflake(author["id"])
        val isWebhook = message.opt<Boolean>("is_webhook")

        val embeds = message.opt<JSArray>("embeds")?.mapNotNull {
            val embed = it as? JSObject ?: let {
                LOG.warn("Found a value in embeds array that was not a JSON object!")
                return@mapNotNull null
            }
            createEmbed(embed)
        } ?: emptyList()

        val attachments = message.opt<JSArray>("attachments")?.mapNotNull {
            (it as? JSObject)?.let { createMessageAttachment(it) }
        } ?: emptyList()

        val userAuthor: User = when(channel) {
            is PrivateChannel -> if(authorId == api.self.id) api.self else channel.recipient
            is TextChannel -> {
                val guild = channel.guild
                val member = guild.getMemberById(authorId)
                member?.user ?: when(isWebhook) {
                    true -> createWebhookUser(author)
                    else -> throw IllegalStateException("Got a message from a un-cached user (User ID: $authorId)")
                }
            }
            else -> throw IllegalArgumentException("Invalid channel. Cannot handle channel type: ${channel.type}")
        }

        val messageTypeInt = message["type"] as Int
        val messageType = Message.Type.typeOf(messageTypeInt)

        val mentionedUserIds = message.opt<JSArray>("mentions")?.mapNotNullTo(HashSet()) {
            if(it !is JSObject) return@mapNotNullTo null
            it.long("id")
        } ?: emptySet<Long>()

        require(messageType != Message.Type.UNKNOWN) { "Invalid message. Cannot handle message type: $messageTypeInt" }

        if(messageType == Message.Type.DEFAULT) {
            @Suppress("USELESS_IS_CHECK") // TODO Remove when able
            if(channel is PrivateChannel) {
                return PrivateMessageImpl(
                    id = messageId,
                    api = api,
                    type = messageType,
                    author = userAuthor,
                    content = content ?: "",
                    embeds = embeds,
                    channel =  channel,
                    attachments = attachments,
                    mentionedUserIds = mentionedUserIds
                )
            } else if(channel is TextChannel) {
                val mentionedRoleIds = message.opt<JSArray>("mention_roles")?.mapNotNullTo(HashSet()) {
                    it as? Long
                } ?: emptySet<Long>()
                return TextMessageImpl(
                    id = messageId,
                    api = api,
                    type = messageType,
                    author = userAuthor,
                    content = content ?: "",
                    embeds = embeds,
                    channel = channel,
                    attachments = attachments,
                    mentionedUserIds = mentionedUserIds,
                    mentionedRoleIds = mentionedRoleIds,
                    isWebhook = isWebhook == true,
                    member = checkNotNull(channel.guild.getMember(userAuthor)) {
                        // TODO Error
                    }
                )
            } else {
                throw IllegalArgumentException("MessageChannel provided was of an unknown type: ${channel::class}")
            }
        } else {
            TODO("Unimplemented System Messages")
        }
    }

    fun createEmbed(embed: JSObject): Embed {
        val title = embed.opt<String>("title")
        val type = Embed.Type.of(embed.string("type"))
        val description = embed.opt<String>("description")
        val url = embed.opt<String>("url")
        val timestamp = embed.opt<String>("timestamp")?.let { OffsetDateTime.parse(it) }
        val color = embed.opt<Int>("color")?.let { Color(it) }

        val footer = embed.opt<JSObject>("footer")?.let { footer ->
            Embed.Footer(
                text = footer.string("text"),
                iconUrl = footer.opt("icon_url"),
                proxyIconUrl = footer.opt("proxy_icon_url")
            )
        }

        val image = embed.opt<JSObject>("image")?.let { image ->
            Embed.Image(
                url = image.string("url"),
                proxyUrl = image.opt("proxy_url"),
                height = image.opt("height") ?: -1,
                width = image.opt("width") ?: -1
            )
        }

        val thumbnail = embed.opt<JSObject>("thumbnail")?.let { thumbnail ->
            Embed.Thumbnail(
                url = thumbnail.string("url"),
                proxyUrl = thumbnail.opt("proxy_url"),
                height = thumbnail.opt("height") ?: -1,
                width = thumbnail.opt("width") ?: -1
            )
        }

        val video = embed.opt<JSObject>("video")?.let { video ->
            Embed.Video(
                url = video.string("url"),
                height = video.opt("height") ?: -1,
                width = video.opt("width") ?: -1
            )
        }

        val provider = embed.opt<JSObject>("provider")?.let { provider ->
            Embed.Provider(
                name = provider.opt("name"),
                url = provider.opt("url")
            )
        }

        val author = embed.opt<JSObject>("author")?.let { author ->
            Embed.Author(
                name = author.string("name"),
                url = author.opt("url"),
                proxyUrl = author.opt("proxy_url"),
                iconUrl = author.opt("icon_url"),
                proxyIconUrl = author.opt("proxy_icon_url")
            )
        }

        val fields = embed.opt<JSArray>("fields")?.mapNotNull {
            val jsonField = it as? JSObject ?: let {
                LOG.warn("Found a value in embed fields array that was not a JSON array!")
                return@mapNotNull null
            }

            Embed.Field(
                name = jsonField.string("name"),
                value = jsonField.string("value"),
                inline = jsonField["inline"] as Boolean
            )
        } ?: emptyList()

        return Embed(title, url, author, description,
            image, thumbnail, fields, timestamp,
            footer, color, type, video, provider)
    }

    fun createMessageAttachment(attachment: JSObject): Message.Attachment {
        return Message.Attachment(
            api = api,
            id = snowflake(attachment["id"]), // Note this isn't a snowflake but we can parse it safely in the same way
            url = attachment.opt("url"),
            proxyUrl = attachment.opt("proxy_url"),
            filename = attachment.string("filename"),
            size = attachment.int("size"),
            height = attachment.opt("height") ?: -1,
            width = attachment.opt("width") ?: -1
        )
    }
}
