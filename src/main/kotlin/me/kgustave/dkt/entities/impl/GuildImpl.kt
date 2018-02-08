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
@file:Suppress("Unused")
package me.kgustave.dkt.entities.impl

import me.kgustave.dkt.Discord
import me.kgustave.dkt.entities.*
import me.kgustave.dkt.entities.caching.impl.MemberCacheImpl
import me.kgustave.dkt.entities.caching.impl.OrderedSnowflakeCache
import me.kgustave.dkt.entities.caching.impl.SnowflakeCacheImpl
import me.kgustave.dkt.exceptions.UnloadedPropertyException
import me.kgustave.dkt.requests.RestPromise
import me.kgustave.dkt.requests.Route
import java.util.Comparator.*

/**
 * @author Kaidan Gustave
 */
class GuildImpl(
    override val id: Long,
    override val api: APIImpl,
    override var unavailable: Boolean = true
): Guild {
    companion object {
        // 1: ID, 2: hash
        private const val ICON_CDN   = "${Discord.CDN_URL}/icons/%d/%s.png"
        private const val SPLASH_CDN = "${Discord.CDN_URL}/splashes/%d/%s.png"
        private fun GuildImpl.checkUnavailable() {
            if(unavailable)
                throw UnloadedPropertyException("Could not get property for unloaded Guild (ID: $id)")
        }
    }

    private lateinit var _name: String
    private lateinit var _owner: Member
    private lateinit var _everyoneRole: Role
    private lateinit var _features: Set<String>
    private lateinit var _mfaLevel: Guild.MFALevel
    private lateinit var _verificationLevel: Guild.VerificationLevel
    private lateinit var _defaultNotificationLevel: Guild.NotificationLevel
    private lateinit var _explicitContentFilter: Guild.ExplicitContentFilter

    // Used purely for debugging if somehow an owner doesn't get set for a guild
    internal val ownerIsInitialized: Boolean
        get() = ::_owner.isInitialized

    override var name: String
        internal set(value) { _name = value }
        get() {
            checkUnavailable()
            return _name
        }
    override var owner: Member
        internal set(value) { _owner = value }
        get() {
            checkUnavailable()
            return _owner
        }
    override var everyoneRole: Role
        internal set(value) { _everyoneRole = value }
        get() {
            checkUnavailable()
            return _everyoneRole
        }
    override var features: Set<String>
        internal set(value) { _features = value }
        get() {
            checkUnavailable()
            return _features
        }
    override var mfaLevel: Guild.MFALevel
        internal set(value) { _mfaLevel = value }
        get() {
            checkUnavailable()
            return _mfaLevel
        }
    override var verificationLevel: Guild.VerificationLevel
        internal set(value) { _verificationLevel = value }
        get() {
            checkUnavailable()
            return _verificationLevel
        }
    override var defaultNotificationLevel: Guild.NotificationLevel
        internal set(value) { _defaultNotificationLevel = value }
        get() {
            checkUnavailable()
            return _defaultNotificationLevel
        }
    override var explicitContentFilter: Guild.ExplicitContentFilter
        internal set(value) { _explicitContentFilter = value }
        get() {
            checkUnavailable()
            return _explicitContentFilter
        }
    override var systemChannel: TextChannel? = null
        internal set
        get() {
            checkUnavailable()
            return field
        }
    override var afkChannel: VoiceChannel? = null
        internal set
        get() {
            checkUnavailable()
            return field
        }
    override var iconId: String? = null
        internal set
        get() {
            checkUnavailable()
            return field
        }
    override var splashId: String? = null
        internal set
        get() {
            checkUnavailable()
            return field
        }
    override var hasWidget: Boolean = false
        internal set
        get() {
            checkUnavailable()
            return field
        }
    override var hasElevatedMFALevel: Boolean = false
        internal set
        get() {
            checkUnavailable()
            return field
        }

    override val self: Member
        get() = TODO("not implemented")
    override val iconUrl: String? get() {
        checkUnavailable()
        return iconId?.let { ICON_CDN.format(id, it) }
    }
    override val splashUrl: String? get() {
        checkUnavailable()
        return splashId?.let { SPLASH_CDN.format(id, it) }
    }

    override val categoryCache = OrderedSnowflakeCache(naturalOrder(), Category::name)
    override val textChannelCache = OrderedSnowflakeCache(naturalOrder(), TextChannel::name)
    override val voiceChannelCache = OrderedSnowflakeCache(naturalOrder(), VoiceChannel::name)
    override val roleCache = OrderedSnowflakeCache(reverseOrder(), Role::name)
    override val emoteCache = SnowflakeCacheImpl(GuildEmote::name)
    override val memberCache = MemberCacheImpl()

    override val roles: List<Role>
        get() = roleCache.toList()
    override val emotes: List<Emote>
        get() = emoteCache.toList()
    override val members: List<Member>
        get() = memberCache.toList()
    override val categories: List<Category>
        get() = categoryCache.toList()
    override val textChannels: List<TextChannel>
        get() = textChannelCache.toList()
    override val voiceChannels: List<VoiceChannel>
        get() = voiceChannelCache.toList()

    // Used for direct setup, usually after joining a guild
    constructor(id: Long, api: APIImpl, block: GuildImpl.() -> Unit): this(id, api, false) {
        this.block()
    }

    override fun getChannelsByName(name: String, ignoreCase: Boolean): List<GuildChannel> {
        TODO("not implemented")
    }

    override fun getCategoriesByName(name: String, ignoreCase: Boolean): List<Category> {
        return categoryCache.getByName(name, ignoreCase)
    }

    override fun getTextChannelsByName(name: String, ignoreCase: Boolean): List<TextChannel> {
        return textChannelCache.getByName(name, ignoreCase)
    }

    override fun getVoiceChannelsByName(name: String, ignoreCase: Boolean): List<VoiceChannel> {
        return voiceChannelCache.getByName(name, ignoreCase)
    }

    override fun getChannelById(id: Long): GuildChannel? {
        getTextChannelById(id)?.let { return it }
        getVoiceChannelById(id)?.let { return it }
        return getCategoryById(id)
    }

    override fun getCategoryById(id: Long): Category? {
        return categoryCache[id]
    }

    override fun getTextChannelById(id: Long): TextChannel? {
        return textChannelCache[id]
    }

    override fun getVoiceChannelById(id: Long): VoiceChannel? {
        return voiceChannelCache[id]
    }

    override fun getMemberById(id: Long): Member? {
        return memberCache[id]
    }

    override fun getMembersByName(name: String, ignoreCase: Boolean): List<Member> {
        return memberCache.getByName(name, ignoreCase)
    }

    override fun getMembersByUsername(name: String, ignoreCase: Boolean): List<Member> {
        return memberCache.getByUsername(name, ignoreCase)
    }

    override fun getMembersByNickname(name: String, ignoreCase: Boolean): List<Member> {
        return memberCache.getByNickname(name, ignoreCase)
    }

    override fun getMembersByAnyName(name: String, ignoreCase: Boolean): List<Member> {
        TODO("not implemented")
    }

    override fun leave(): RestPromise<Unit> = RestPromise.simple(api, Route.LeaveGuild.format()) { res, req ->
        // 204 -> Successfully left guild
        when {
            res.code == 204 -> req.succeed(Unit)
            else -> req.error(res)
        }
    }

    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean = other is Guild && Snowflake.equals(this, other)
    override fun toString(): String = Snowflake.toString("Guild", this)
}
