package pt.hitv.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkLiveStream(
    @SerialName("num") val num: Int = 0,
    @SerialName("name") val name: String? = null,
    @SerialName("stream_type") val streamType: String? = null,
    @SerialName("stream_id") val streamId: Int = 0,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("epg_channel_id") val epgChannelId: String? = null,
    @SerialName("added") val added: Int = 0,
    @SerialName("category_id") val categoryId: Int = 0,
    @SerialName("custom_sid") val customSid: String? = null,
    @SerialName("tv_archive") val tvArchive: Int = 0,
    @SerialName("direct_source") val directSource: String? = null,
    @SerialName("tv_archive_duration") val tvArchiveDuration: Int = 0,
    @SerialName("catchup_type") val catchupType: String? = null,
    @SerialName("catchup_source") val catchupSource: String? = null,
)
