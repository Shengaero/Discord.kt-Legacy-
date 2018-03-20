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
@file:Suppress("MemberVisibilityCanBePrivate", "unused")
package me.kgustave.dkt.entities

//
// I'd like to write something as a future note for people looking
// to develop kotlin-based APIs who read this source for ideas.
//
// I'd highly recommend you use @PublishedApi for internal builders and
// provide builders as parameters for entities in the following style:
//
//                                               |
// ----------------------------------------------+--------------------------------------------------------------
// builderFunction ---+                          |
//                    |                          | inline fun build(block: Entity.Builder.() -> Unit): Entity {
//                    | <| apply to builder      |     val builder = Entity.Builder()
//                    V                          |     builder.block()
//       Builder() ---+-------+                  |     return Entity(builder)
//                            |                  | }
// BuildEntity(Builder) <-----+                  |
//                                               |

import me.kgustave.dkt.util.requireNotBlank
import me.kgustave.dkt.util.requireNotLonger
import me.kgustave.json.JSObject
import me.kgustave.json.emptyJSArray
import me.kgustave.json.emptyJSObject
import me.kgustave.json.jsonObject
import java.awt.Color
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

/**
 * @author Kaidan Gustave
 */
data class Embed internal constructor(
    val title: String?,
    val url: String?,
    val author: Embed.Author?,
    val description: String?,
    val image: Embed.Image?,
    val thumbnail: Embed.Thumbnail?,
    val fields: List<Embed.Field>,
    val timestamp: OffsetDateTime?,
    val footer: Embed.Footer?,
    val color: Color?,

    val type: Embed.Type = Type.RICH,
    val video: Embed.Video? = null,
    val provider: Embed.Provider? = null
) {
    // Currently we centralize all embed related entities in this
    // file, most of which are under this class (IE: Embed.Author
    // is an inner class of Embed).
    //
    // This is done for a couple reasons:
    //
    // 1) To avoid confusion and inconsistencies when developers
    //    want to use embeds and other embed functionality.
    // 2) To properly organize checks for embed limits.
    //
    // Because all of the limits on text are subject to change
    // and embeds come in so many shapes and sizes to begin with,
    // it's pretty much a no-brainer that we don't bother checking
    // the content of embeds we receive from Discord.
    //
    // If you want to know the specifics regarding these limits, I recommend
    // you read more here:
    //
    // https://discordapp.com/developers/docs/resources/channel#embed-limits

    companion object {
        /**
         * The max character length a [embed title][Embed.Title.text]
         * can contain.
         *
         * Currently this is 256 characters.
         */
        const val MAX_TITLE_LENGTH = 256

        /**
         * The max character length a [embed description][Embed.description]
         * or [embed footer][Embed.Footer.text] can contain.
         *
         * Currently this is 2048 characters.
         */
        const val MAX_TEXT_LENGTH = 2048

        /**
         * The max character length any URL in an [Embed] can be.
         *
         * Currently this is 2000 characters.
         */
        const val MAX_URL_LENGTH = 2000

        /**
         * The cumulative max character length of all text areas that an [Embed] can contain.
         *
         * Currently this is 6000 characters.
         */
        const val MAX_TOTAL_LENGTH = 6000

        /**
         * The max character length that a [embed field][Embed.Field.value] can contain.
         *
         * Currently this is 1024 characters.
         */
        const val MAX_FIELD_VALUE_LENGTH = 1024

        /**
         * "Zero Width Space" character that can make some embed
         * text areas appear empty in the discord interface.
         */
        const val ZWSP = "\u200E"

        private val URL_REGEX = Regex("\\s*(https?|attachment)://.+\\..{2,}\\s*", RegexOption.IGNORE_CASE)
        private val RGB_RANGE get() = 0..255

        private fun String.assureNotEmpty(): String = if(isBlank()) ZWSP else this
        private fun checkUrl(url: String?) {
            if(url === null) return

            requireNotLonger(url, MAX_URL_LENGTH, "URL")
            require(url matches URL_REGEX) { "URL is not a valid embeddable URL" }
        }
    }

    @PublishedApi
    internal constructor(builder: Embed.Builder): this(
        builder.title?.text,
        builder.title?.url,
        builder.author,
        builder.content.takeIf { it.isNotBlank() },
        builder.image,
        builder.thumbnail,
        builder.fields,
        builder.timestamp,
        builder.footer,
        builder.color
    )

    fun isEmpty(): Boolean {
        return fields.isEmpty() &&
               author === null &&
               title === null &&
               description === null &&
               thumbnail === null &&
               image === null
    }

    internal val json: JSObject by lazy { // Construct lazily so we don't have to reconstruct it for multiple calls
        val embed = emptyJSObject()

        title?.let {
            embed["title"] = title
            embed["url"] = url
        }

        author?.let {
            embed["author"] = jsonObject {
                "name" to author.name
                author.url?.let { "url" to author.url }
                author.iconUrl?.let { "icon_url" to author.iconUrl }
            }
        }

        description?.let {
            embed["description"] = description
        }

        image?.let {
            embed["image"] = jsonObject { "url" to image.url }
        }

        thumbnail?.let {
            embed["thumbnail"] = jsonObject { "url" to thumbnail.url }
        }

        video?.let {
            embed["video"] = jsonObject { "url" to video.url }
        }

        color?.let {
            embed["color"] = (color.rgb and 0xFFFFFF)
        }

        timestamp?.let {
            embed["timestamp"] = timestamp.format(DateTimeFormatter.RFC_1123_DATE_TIME)
        }

        footer?.let {
            embed["footer"] = jsonObject {
                "text" to footer.text
                footer.iconUrl?.let { "icon_url" to footer.iconUrl }
            }
        }

        provider?.let {
            embed["provider"] = jsonObject {
                "name" to provider.name
                "url" to provider.url
            }
        }

        if(fields.isNotEmpty()) {
            val fieldsArray = emptyJSArray()
            fields.forEach { field ->
                fieldsArray += jsonObject(
                    "name" to field.name,
                    "value" to field.value,
                    "inline" to field.inline
                )
            }
            embed["fields"] = fieldsArray
        }

        return@lazy embed
    }

    @MessageDsl
    class Builder @PublishedApi internal constructor(): MessageDslComponent<Embed.Builder>() {
        internal val fields = ArrayList<Embed.Field>()
        internal var title: Embed.Title? = null
        internal var author: Embed.Author? = null
        internal var image: Embed.Image? = null
        internal var thumbnail: Embed.Thumbnail? = null
        internal var footer: Embed.Footer? = null

        @MessageDsl
        var color: Color? = null

        @MessageDsl
        var timestamp: OffsetDateTime? = null

        @MessageDsl
        val length: Int get() {
            var l = contentBuilder.length
            l += fields.sumBy { it.value.length }
            title?.text?.length?.let { l += it }
            author?.name?.length?.let { l += it }
            footer?.text?.length?.let { l += it }
            return l
        }

        @MessageDsl
        fun isEmpty(): Boolean {
            return contentBuilder.isBlank() &&
                   fields.isEmpty() &&
                   title === null &&
                   author === null &&
                   image === null &&
                   thumbnail === null &&
                   footer === null &&
                   timestamp === null
        }

        @MessageDsl
        fun title(text: String?, url: String? = null) {
            this.title = text?.let {
                requireNotBlank(text, "Title")
                Embed.Title(text, url)
            }
        }

        @MessageDsl
        fun author(name: String?, url: String? = null, iconUrl: String? = null) {
            checkUrl(url)
            checkUrl(iconUrl)
            this.author = name?.let { Embed.Author(name, url, iconUrl) }
        }

        @MessageDsl
        fun field(name: String, value: String, inline: Boolean = true) {
            val sanitizedName = name.assureNotEmpty()
            val sanitizedValue = value.assureNotEmpty()
            requireNotLonger(sanitizedName, MAX_TITLE_LENGTH, "Field name")
            requireNotLonger(sanitizedValue, MAX_FIELD_VALUE_LENGTH, "Field value")
            this.fields += Embed.Field(sanitizedName, sanitizedValue, inline)
        }

        @MessageDsl
        fun image(url: String?) {
            checkUrl(url)
            this.image = url?.let { Embed.Image(url) }
        }

        @MessageDsl
        fun thumbnail(url: String?) {
            checkUrl(url)
            this.thumbnail = url?.let { Embed.Thumbnail(url) }
        }

        @MessageDsl
        fun footer(text: String?, iconUrl: String? = null) {
            if(text === null) {
                this.footer = null
            } else {
                val sanitizedText = text.assureNotEmpty()
                requireNotLonger(sanitizedText, MAX_TEXT_LENGTH, "Footer text")
                checkUrl(iconUrl)
                this.footer = Embed.Footer(sanitizedText, iconUrl)
            }
        }

        @MessageDsl
        fun timestamp(accessor: TemporalAccessor?) {
            if(accessor === null) {
                this.timestamp = null
            } else {
                if(accessor is OffsetDateTime) {
                    this.timestamp = accessor
                } else {
                    var offset: ZoneOffset = ZoneOffset.UTC
                    try {
                        offset = ZoneOffset.from(accessor)
                        val local = LocalDateTime.from(accessor)
                        this.timestamp = OffsetDateTime.of(local, offset)
                    } catch(e: DateTimeException) {
                        try {
                            val instant = Instant.from(accessor)
                            this.timestamp = OffsetDateTime.ofInstant(instant, offset)
                        } catch(e: DateTimeException) {
                            throw IllegalStateException("Unable to get offset from accessor type: ${accessor::class}", e)
                        }
                    }
                }
            }
        }

        @MessageDsl
        fun color(color: Color?) {
            this.color = color
        }

        @MessageDsl
        fun color(rgb: Int) {
            color(Color(rgb))
        }

        @MessageDsl
        fun color(red: Int, blue: Int, green: Int, alpha: Int = 255) {
            require(red   in RGB_RANGE) {   "Red value $red outside of range 0-255"   }
            require(blue  in RGB_RANGE) {  "Blue value $blue outside of range 0-255"  }
            require(green in RGB_RANGE) { "Green value $green outside of range 0-255" }
            require(alpha in RGB_RANGE) { "Alpha value $alpha outside of range 0-255" }

            color(Color(red, blue, green, alpha))
        }

        @MessageDsl
        inline fun title(block: Title.Builder.() -> Unit) {
            val title = Title.Builder()
            title.block()
            val text = requireNotNull(title.text?.takeIf { it.isNotBlank() }) {
                "Cannot set title to blank or null"
            }
            title(text, title.url)
        }

        @MessageDsl
        inline fun author(name: String, block: Author.Builder.() -> Unit) {
            val builder = Author.Builder(name)
            builder.block()
            author(builder.name, builder.url, builder.imageUrl)
        }

        @MessageDsl
        inline fun field(name: String, inline: Boolean = true, block: Field.Builder.() -> Unit) {
            val builder = Field.Builder(name, inline)
            builder.block()
            field(builder.name, builder.content, builder.inline)
        }

        @MessageDsl
        inline fun image(block: () -> String?) {
            image(block())
        }

        @MessageDsl
        inline fun thumbnail(block: () -> String?) {
            thumbnail(block())
        }

        @MessageDsl
        inline fun footer(block: Footer.Builder.() -> Unit) {
            val builder = Footer.Builder()
            builder.block()
            footer(builder.text, builder.iconUrl)
        }

        @MessageDsl
        inline fun timestamp(block: () -> TemporalAccessor?) {
            timestamp(block())
        }

        @MessageDsl
        inline fun color(block: () -> Color) {
            color(block())
        }

        @MessageDsl
        inline fun colorRGB(block: () -> Int) {
            color(block())
        }

        /** Shorthand converter for a masked link.*/
        @MessageDsl
        operator fun String.invoke(link: String): String = "[$this]($link)"

        override fun check(csq: CharSequence) {
            require(length + csq.length < MAX_TOTAL_LENGTH) {
                "Embed total length cannot be greater than $MAX_TEXT_LENGTH characters"
            }
            require(contentBuilder.length + csq.length < MAX_TEXT_LENGTH) {
                "Embed description cannot be longer than $MAX_TEXT_LENGTH characters"
            }
        }
    }

    data class Title internal constructor(
        val text: String,
        val url: String? = null
    ) {
        @MessageDsl
        class Builder @PublishedApi internal constructor(
            @MessageDsl var text: String? = null,
            @MessageDsl var url: String? = null
        ) {
            @MessageDsl
            inline fun text(block: () -> String) {
                this.text = block()
            }

            @MessageDsl
            inline fun url(block: () -> String?) {
                this.url = block()
            }
        }
    }

    data class Field internal constructor(
        val name: String,
        val value: String,
        val inline: Boolean
    ) {
        @MessageDsl
        class Builder @PublishedApi internal constructor(
            @MessageDsl var name: String,
            @MessageDsl var inline: Boolean = true
        ): MessageDslComponent<Field.Builder>() {
            @MessageDsl
            inline fun name(block: () -> String) {
                this.name = block()
            }

            @MessageDsl
            inline fun inline(block: () -> Boolean) {
                this.inline = block()
            }

            /** Shorthand converter for a masked link. */
            @MessageDsl
            operator fun String.invoke(link: String): String = "[$this]($link)"
        }
    }

    data class Author internal constructor(
        val name: String,
        val url: String?,
        val iconUrl: String?,
        val proxyUrl: String? = null,
        val proxyIconUrl: String? = null
    ) {
        @MessageDsl
        class Builder @PublishedApi internal constructor(
            @MessageDsl var name: String,
            @MessageDsl var url: String? = null,
            @MessageDsl var imageUrl: String? = null
        ) {
            @MessageDsl
            inline fun name(block: () -> String) {
                this.name = block()
            }

            @MessageDsl
            inline fun url(block: () -> String?) {
                this.url = block()
            }

            @MessageDsl
            inline fun iconUrl(block: () -> String?) {
                this.imageUrl = block()
            }
        }
    }

    data class Footer internal constructor(
        val text: String,
        val iconUrl: String? = null,
        val proxyIconUrl: String? = null
    ) {
        @MessageDsl
        class Builder @PublishedApi internal constructor(
            @MessageDsl var text: String = ZWSP,
            @MessageDsl var iconUrl: String? = null
        ) {
            @MessageDsl
            inline fun text(block: () -> String) {
                this.text = block()
            }

            @MessageDsl
            inline fun iconUrl(block: () -> String?) {
                this.iconUrl = block()
            }
        }
    }

    // As upsetting as this is, there is no guarantee that
    // an Discord Embed Image will remain the same structure
    // as a Discord Embed Thumbnail. For the sake of compatibility
    // if one ever changes, I created two identical classes....
    // -_-

    data class Image internal constructor(
        val url: String,
        val proxyUrl: String? = null,
        val height: Int = 0,
        val width: Int = 0
    )

    data class Thumbnail internal constructor(
        val url: String,
        val proxyUrl: String? = null,
        val height: Int = 0,
        val width: Int = 0
    )

    data class Video internal constructor(
        val url: String,
        val height: Int = 0,
        val width: Int = 0
    )

    data class Provider internal constructor(
        val name: String?,
        val url: String?
    )

    enum class Type(type: String? = null) {
        IMAGE,
        VIDEO,
        LINK,
        RICH,
        UNKNOWN("");

        val type = type ?: name.toLowerCase()

        companion object {
            fun of(type: String): Embed.Type = values().firstOrNull { it.type == type } ?: UNKNOWN
        }
    }
}

@MessageDsl
inline fun embed(build: Embed.Builder.() -> Unit): Embed {
    val builder = Embed.Builder()
    builder.build()
    return Embed(builder)
}
