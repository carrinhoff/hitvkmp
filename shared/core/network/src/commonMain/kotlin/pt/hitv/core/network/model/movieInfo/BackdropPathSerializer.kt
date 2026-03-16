package pt.hitv.core.network.model.movieInfo

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Custom serializer that handles both String and Array formats for backdrop_path.
 * Replaces the Gson BackdropPathAdapter.
 *
 * The Xtream Codes API sometimes returns backdrop_path as a single string
 * instead of an array, so this serializer normalizes both cases to List<String>.
 */
object BackdropPathSerializer : KSerializer<List<String>> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("BackdropPath")

    override fun serialize(encoder: Encoder, value: List<String>) {
        ListSerializer(String.serializer()).serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): List<String> {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return ListSerializer(String.serializer()).deserialize(decoder)

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> {
                val path = element.content
                if (path.isNotEmpty()) listOf(path) else emptyList()
            }
            is JsonArray -> {
                element.mapNotNull { item ->
                    if (item is JsonPrimitive && item !is JsonNull) {
                        val path = item.jsonPrimitive.content
                        if (path.isNotEmpty()) path else null
                    } else {
                        null
                    }
                }
            }
            is JsonNull -> emptyList()
            else -> emptyList()
        }
    }
}
