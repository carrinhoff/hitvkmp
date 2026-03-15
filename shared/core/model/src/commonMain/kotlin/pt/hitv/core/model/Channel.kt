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
    val userId: Int = 0 // Which user/playlist this channel belongs to
) {
    //help on small channel list adapter
    var isSelected: Boolean = false
}
