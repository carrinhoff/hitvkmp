package pt.hitv.feature.player.helpers

import kotlinx.coroutines.flow.StateFlow
import pt.hitv.core.model.Category
import pt.hitv.core.model.Channel

class ChannelNavigationHelper(
    private val allChannels: StateFlow<List<Channel>>,
    private val onChannelClick: (Channel) -> Unit
) {
    fun normalizeUrlForComparison(url: String?): String = url?.trim()?.removeSuffix(".m3u8") ?: ""

    fun navigateToPreviousChannel(currentChannelUrl: String) {
        getPreviousChannel(currentChannelUrl, allChannels.value)?.let { onChannelClick(it) }
    }

    fun navigateToNextChannel(currentChannelUrl: String) {
        getNextChannel(currentChannelUrl, allChannels.value)?.let { onChannelClick(it) }
    }

    internal fun getPreviousChannel(currentChannelUrl: String, allChannels: List<Channel>, currentCategoryId: String? = null): Channel? {
        if (allChannels.isEmpty()) return null
        val normalizedCurrentUrl = normalizeUrlForComparison(currentChannelUrl)
        val currentChannel = allChannels.find { normalizeUrlForComparison(it.streamUrl) == normalizedCurrentUrl }
        val categoryId = currentCategoryId ?: currentChannel?.categoryId
        val filteredChannels = if (categoryId != null) allChannels.filter { it.categoryId == categoryId } else allChannels
        if (filteredChannels.isEmpty()) return null
        val currentIdx = filteredChannels.indexOfFirst { normalizeUrlForComparison(it.streamUrl) == normalizedCurrentUrl }
        val prevIdx = if (currentIdx > 0) currentIdx - 1 else filteredChannels.size - 1
        return filteredChannels.getOrNull(prevIdx)
    }

    internal fun getNextChannel(currentChannelUrl: String, allChannels: List<Channel>, currentCategoryId: String? = null): Channel? {
        if (allChannels.isEmpty()) return null
        val normalizedCurrentUrl = normalizeUrlForComparison(currentChannelUrl)
        val currentChannel = allChannels.find { normalizeUrlForComparison(it.streamUrl) == normalizedCurrentUrl }
        val categoryId = currentCategoryId ?: currentChannel?.categoryId
        val filteredChannels = if (categoryId != null) allChannels.filter { it.categoryId == categoryId } else allChannels
        if (filteredChannels.isEmpty()) return null
        val currentIdx = filteredChannels.indexOfFirst { normalizeUrlForComparison(it.streamUrl) == normalizedCurrentUrl }
        val nextIdx = if (currentIdx >= 0 && currentIdx < filteredChannels.size - 1) currentIdx + 1 else 0
        return filteredChannels.getOrNull(nextIdx)
    }

    companion object {
        fun parseSearchWords(query: String): List<String> = query.lowercase().trim().split(" ").filter { it.isNotBlank() }

        fun filterChannels(channels: List<Channel>, searchWords: List<String>, selectedCategoryId: String?, categoryMap: Map<String, Category>): List<Channel> {
            val isSearching = searchWords.isNotEmpty()
            return channels.filter { channel ->
                val matchesCategory = if (isSearching) true else selectedCategoryId == null || channel.categoryId?.toString() == selectedCategoryId
                val matchesSearch = if (searchWords.isEmpty()) true else searchWords.all { word ->
                    val inName = channel.name?.lowercase()?.contains(word) == true
                    val inCategory = channel.categoryId?.let { catId -> categoryMap[catId]?.categoryName?.lowercase()?.contains(word) == true } ?: false
                    inName || inCategory
                }
                matchesCategory && matchesSearch
            }
        }

        fun filterCategories(channels: List<Channel>, categoryMap: Map<String, Category>, searchWords: List<String>): List<Pair<String, String>> {
            return channels.mapNotNull { channel -> channel.categoryId?.takeIf { it.isNotBlank() }?.let { id -> id to (categoryMap[id]?.categoryName ?: id) } }
                .distinctBy { it.first }
                .filter { (_, name) -> searchWords.isEmpty() || searchWords.all { word -> word.isNotBlank() && name.lowercase().contains(word) } }
                .sortedBy { it.second }
        }

        fun isChannelPlaying(channelUrl: String?, currentUrl: String): Boolean {
            if (channelUrl.isNullOrEmpty()) return false
            if (channelUrl == currentUrl) return true
            return currentUrl.contains(channelUrl) || channelUrl.contains(currentUrl)
        }
    }
}
