package pt.hitv.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkServerInfo(
    @SerialName("url") val url: String? = null,
    @SerialName("port") val port: String? = null,
    @SerialName("https_port") val httpsPort: String? = null,
    @SerialName("server_protocol") val serverProtocol: String? = null,
    @SerialName("rtmp_port") val rtmpPort: String? = null,
    @SerialName("timezone") val timezone: String? = null,
    @SerialName("timestamp_now") val timestampNow: String? = null,
    @SerialName("time_now") val timeNow: String? = null
)
