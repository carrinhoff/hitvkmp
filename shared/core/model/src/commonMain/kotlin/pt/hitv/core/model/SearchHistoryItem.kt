package pt.hitv.core.model

data class SearchHistoryItem(
    val id: Long,
    val userId: Int,
    val query: String,
    val kind: String,  // "channel" | "movie" | "series"
    val timestamp: Long
)
