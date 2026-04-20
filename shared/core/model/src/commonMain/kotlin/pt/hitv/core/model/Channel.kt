package pt.hitv.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Channel (
    val name:String?="",
    val streamIcon:String?="",
    val streamUrl: String?="",
    val categoryId: String?="",
    val isFavorite: Boolean = false,
    val epgChannelId:String? = "",
    val lastViewedTimestamp:Long  =0L,
    val id: String? = "",
    val licenseKey: String? = null,
    val userId: Int = 0, // Which user/playlist this channel belongs to
    val tvArchive: Int = 0, // 1 if channel supports catch-up / TV archive
    val tvArchiveDuration: Int = 0, // catch-up window length in days
    // Xtream catchup_type: "xc" (default), "flussonic"/"fs", "shift", "default", "append".
    // Drives the URL format used by CatchUpUrlBuilder for the replay stream.
    val catchupType: String? = null,
    // Xtream catchup_source: template URL / query string for non-XC formats.
    val catchupSource: String? = null,
) {
    //help on small channel list adapter
    var isSelected: Boolean = false
}
