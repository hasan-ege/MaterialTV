package com.hasanege.materialtv.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject

object LenientServerInfoSerializer : KSerializer<ServerInfo?> {
    private val delegate = ServerInfo.serializer()

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: ServerInfo?) {
        if (value != null) {
            encoder.encodeSerializableValue(delegate, value)
        } else {
            encoder.encodeNull()
        }
    }

    override fun deserialize(decoder: Decoder): ServerInfo? {
        val jsonDecoder = decoder as? JsonDecoder ?: return null
        val element = jsonDecoder.decodeJsonElement()
        if (element is JsonObject) {
            return jsonDecoder.json.decodeFromJsonElement(delegate, element)
        }
        return null
    }
}

@Serializable
data class XtreamAuthResponse(
    @SerialName("user_info")
    val userInfo: AuthUserInfo? = null,
    @SerialName("server_info")
    @Serializable(with = LenientServerInfoSerializer::class)
    val serverInfo: ServerInfo? = null
)

@Serializable
data class AuthUserInfo(
    val auth: Int, // This is the key. 1 for success, 0 for failure.
    val username: String? = null,
    val status: String? = null,
    @SerialName("is_trial")
    val isTrial: String? = null,
    @SerialName("max_connections")
    val maxConnections: String? = null
)

@Serializable
data class ServerInfo(
    val url: String? = null,
    val port: String? = null,
    val https_port: String? = null,
    val time_now: String? = null,
    val timezone: String? = null
)
