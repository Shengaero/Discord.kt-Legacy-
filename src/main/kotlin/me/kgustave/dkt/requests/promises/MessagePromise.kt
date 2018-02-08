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
@file:Suppress("MemberVisibilityCanBePrivate", "Unused")
package me.kgustave.dkt.requests.promises

import me.kgustave.dkt.Permission
import me.kgustave.dkt.entities.*
import me.kgustave.dkt.entities.impl.APIImpl
import me.kgustave.dkt.requests.*
import me.kgustave.kson.KSONObject
import me.kgustave.kson.kson
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import okio.Okio
import java.io.InputStream

/**
 * @author Kaidan Gustave
 */
class MessagePromise(
    val channel: MessageChannel,
    api: APIImpl,
    route: Route.FormattedRoute,
    base: String? = null
): RestPromise<Message>(api, route), Appendable {
    companion object {
        val OCTET_STREAM = MediaType.parse("application/octet-stream")
    }

    // IMPORTANT
    // When sending files, we're going to need to use a Multipart Form Body,
    // We should check which one is appropriate at the time we send it.
    private val files: MutableMap<String, InputStream> = HashMap()
    private val content = base?.let { StringBuilder(it) } ?: StringBuilder()

    var nonce: String? = null
    var embed: Embed? = null
        set(value) = TODO("Sending embeds is not implemented yet!")
    var tts: Boolean = false

    constructor(api: APIImpl, route: Route.FormattedRoute, message: Message): this(message.channel, api, route) {
        content.append(message.content)
        // TODO embed = message.embeds[0]
    }

    fun isEmpty(): Boolean {
        return Message.contentEmpty(content) && (embed == null || embed!!.isEmpty() || !checkForEmbedPermission())
    }

    inline fun embed(crossinline builder: Embed.Builder.() -> Unit): MessagePromise {
        embed = Embed { builder() }
        return this
    }

    private fun bodyAsMultiform(): RequestBody {
        val builder = MultipartBody.Builder()
        builder.setType(MultipartBody.FORM)
        for((index, key) in files.keys.withIndex()) {
            builder.addFormDataPart("file$index", key, object: RequestBody() {
                override fun contentType(): MediaType? = OCTET_STREAM
                override fun writeTo(sink: BufferedSink) {
                    Okio.source(files[key]!!).use { sink.writeAll(it) }
                }
            })
        }
        if(!isEmpty())
            builder.addFormDataPart("payload_json", getPayloadJSON().toString())
        return builder.build()
    }

    private fun bodyAsJSON(): RequestBody {
        return RequestBody.create(Requester.MEDIA_TYPE_JSON, getPayloadJSON().toString())
    }

    private fun getPayloadJSON(): KSONObject = kson {
        "embed" to getEmbedAsKSON()
        "content" to content.takeUnless { Message.contentEmpty(it) }?.toString()
        "nonce" to nonce
        "tts" to tts
    }

    private fun getEmbedAsKSON(): KSONObject? {
        if(embed == null)
            return null
        return kson {
        }
    }

    private fun checkForEmbedPermission(): Boolean {
        if(channel !is TextChannel)
            return false
        return channel.guild.self.hasPermission(channel, Permission.MESSAGE_EMBED_LINKS)
    }

    override fun append(c: Char): MessagePromise {
        content.append(c)
        return this
    }

    override val body: RequestBody get() {
        if(files.isNotEmpty())
            return bodyAsMultiform()
        else if(!isEmpty())
            return bodyAsJSON()
        throw IllegalStateException("Cannot send a message that is empty!")
    }

    override fun append(csq: CharSequence?): MessagePromise {
        content.append(csq)
        return this
    }

    override fun append(csq: CharSequence?, start: Int, end: Int): MessagePromise {
        content.append(csq, start, end)
        return this
    }

    fun append(mentionable: Mentionable): MessagePromise = append(mentionable.asMention)

    override suspend fun handle(response: RestResponse, request: RestRequest<Message>) {
        when {
            response.isOk -> request.succeed(api.entityBuilder.createMessage(response.obj as KSONObject, channel))
            else -> request.error(response)
        }
    }
}
