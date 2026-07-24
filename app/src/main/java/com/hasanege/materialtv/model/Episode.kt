@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
package com.hasanege.materialtv.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

object LenientEpisodeInfoSerializer : KSerializer<EpisodeInfo?> {
    private val delegate = EpisodeInfo.serializer()
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: EpisodeInfo?) {
        if (value != null) {
            encoder.encodeSerializableValue(delegate, value)
        } else {
            encoder.encodeNull()
        }
    }

    override fun deserialize(decoder: Decoder): EpisodeInfo? {
        val jsonDecoder = decoder as? JsonDecoder ?: return null
        val element = jsonDecoder.decodeJsonElement()
        if (element is JsonObject) {
            return jsonDecoder.json.decodeFromJsonElement(delegate, element)
        }
        return null
    }
}

@Serializable
data class Episode(
    @SerialName("id") val id: String,
    @SerialName("episode_num") val episodeNum: String? = null,
    @Serializable(with = SeasonSerializer::class)
    @SerialName("season") val season: Int? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("container_extension") val containerExtension: String? = null,
    @Serializable(with = LenientEpisodeInfoSerializer::class)
    val info: EpisodeInfo? = null
) {
    val duration: String? get() = info?.duration
}

object SeasonSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("season", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value)
    }

    override fun deserialize(decoder: Decoder): Int {
        return try {
            decoder.decodeInt()
        } catch (e: Exception) {
            -1 // Or handle appropriately
        }
    }
}

@Serializable
data class EpisodeInfo(
    @SerialName("movie_image")
    val movieImage: String? = null,
    @SerialName("duration")
    val duration: String? = null
)
