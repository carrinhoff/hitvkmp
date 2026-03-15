package pt.hitv.core.model

data class ServerInfo(
    val url: String?,
    val port: String?,
    val httpsPort: String?,
    val serverProtocol: String?,
    val rtmpPort: String?,
    val timezone: String?,
    val timestampNow: String?,
    val timeNow: String?
)
