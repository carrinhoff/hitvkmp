package pt.hitv.core.model

data class LiveStream(
    val num: Int,
    val name: String,
    val streamType: String,
    val streamId: Int,
    val streamIcon: String,
    val epgChannelId: String,
    val added: Int,
    val categoryId: Int,
    val customSid: String,
    val tvArchive: Int,
    val directSource: String,
    val tvArchiveDuration: Int,
    val catchupType: String? = null,
    val catchupSource: String? = null,
)
