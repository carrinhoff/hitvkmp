package pt.hitv.core.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.database.CategoryQueries
import pt.hitv.core.database.CategoryVodQueries
import pt.hitv.core.database.CategoryTvShowQueries
import pt.hitv.core.database.CustomGroupQueries
import pt.hitv.core.domain.repositories.CategoryPreferenceRepository
import pt.hitv.core.model.CategoryPreference
import pt.hitv.core.model.ContentType

/**
 * Implementation of CategoryPreferenceRepository that coordinates
 * channel, movie, series, and custom group queries.
 */
class CategoryPreferenceRepositoryImpl(
    private val categoryQueries: CategoryQueries,
    private val categoryVodQueries: CategoryVodQueries,
    private val categoryTvShowQueries: CategoryTvShowQueries,
    private val customGroupQueries: CustomGroupQueries,
    private val preferencesHelper: PreferencesHelper
) : CategoryPreferenceRepository {

    private val userId: Int
        get() = preferencesHelper.getUserId()

    companion object {
        private const val CUSTOM_GROUP_PREFIX = "custom_group_"
    }

    /**
     * Reactive, not a one-shot snapshot.
     *
     * This was `flow { ...executeAsList()...; emit(all) }`, which emits once and completes. The
     * Manage Categories screen therefore never reflected a pin / hide / set-default toggle:
     * `ManageCategoriesViewModel.togglePin` writes to the DB and does not reload, so the list
     * stayed exactly as first loaded until the screen was rebuilt.
     *
     * Combining the four `asFlow()` sources means SQLDelight re-queries whichever table was
     * written and the combined list re-emits. Source order is preserved (channels, movies, series,
     * custom groups) so the screen's grouping is unchanged.
     */
    override fun getAllCategoryPreferences(): Flow<List<CategoryPreference>> {
        val uid = userId.toLong()
        val channels = categoryQueries.selectAllSorted(uid).asFlow().mapToList(Dispatchers.IO)
        val movies = categoryVodQueries.selectAllSorted(uid).asFlow().mapToList(Dispatchers.IO)
        val series = categoryTvShowQueries.selectAllSorted(uid).asFlow().mapToList(Dispatchers.IO)
        val groups = customGroupQueries.selectAllGroupsSorted().asFlow().mapToList(Dispatchers.IO)

        return combine(channels, movies, series, groups) { ch, mv, sr, cg ->
            buildList {
                ch.forEach {
                    add(
                        CategoryPreference(
                            categoryId = it.categoryId.toString(),
                            categoryName = it.categoryName,
                            contentType = ContentType.CHANNELS,
                            isPinned = it.isPinned != 0L,
                            isHidden = it.isHidden != 0L,
                            isDefault = it.isDefault != 0L
                        )
                    )
                }
                mv.forEach {
                    add(
                        CategoryPreference(
                            categoryId = it.categoryId.toString(),
                            categoryName = it.categoryName,
                            contentType = ContentType.MOVIES,
                            isPinned = it.isPinned != 0L,
                            isHidden = it.isHidden != 0L,
                            isDefault = it.isDefault != 0L
                        )
                    )
                }
                sr.forEach {
                    add(
                        CategoryPreference(
                            categoryId = it.categoryId.toString(),
                            categoryName = it.categoryName,
                            contentType = ContentType.SERIES,
                            isPinned = it.isPinned != 0L,
                            isHidden = it.isHidden != 0L,
                            isDefault = it.isDefault != 0L
                        )
                    )
                }
                cg.forEach {
                    add(
                        CategoryPreference(
                            categoryId = "$CUSTOM_GROUP_PREFIX${it.groupId}",
                            categoryName = it.groupName,
                            contentType = ContentType.CHANNELS,
                            isPinned = it.isPinned != 0L,
                            isHidden = it.isHidden != 0L,
                            isDefault = it.isDefault != 0L
                        )
                    )
                }
            }
        }
    }

    override suspend fun updateCategoryPinStatus(categoryId: String, contentType: ContentType, isPinned: Boolean) {
        withContext(Dispatchers.IO) {
            val isPinnedLong = if (isPinned) 1L else 0L
            if (categoryId.startsWith(CUSTOM_GROUP_PREFIX)) {
                val groupId = categoryId.removePrefix(CUSTOM_GROUP_PREFIX).toLong()
                customGroupQueries.updateGroupPinStatus(isPinnedLong, groupId)
            } else {
                val catId = categoryId.toLong()
                when (contentType) {
                    ContentType.CHANNELS -> categoryQueries.updatePinStatus(isPinnedLong, catId, userId.toLong())
                    ContentType.MOVIES -> categoryVodQueries.updatePinStatus(isPinnedLong, catId, userId.toLong())
                    ContentType.SERIES -> categoryTvShowQueries.updatePinStatus(isPinnedLong, catId, userId.toLong())
                }
            }
        }
    }

    override suspend fun updateCategoryHideStatus(categoryId: String, contentType: ContentType, isHidden: Boolean) {
        withContext(Dispatchers.IO) {
            val isHiddenLong = if (isHidden) 1L else 0L
            if (categoryId.startsWith(CUSTOM_GROUP_PREFIX)) {
                val groupId = categoryId.removePrefix(CUSTOM_GROUP_PREFIX).toLong()
                customGroupQueries.updateGroupHideStatus(isHiddenLong, groupId)
            } else {
                val catId = categoryId.toLong()
                when (contentType) {
                    ContentType.CHANNELS -> categoryQueries.updateHideStatus(isHiddenLong, catId, userId.toLong())
                    ContentType.MOVIES -> categoryVodQueries.updateHideStatus(isHiddenLong, catId, userId.toLong())
                    ContentType.SERIES -> categoryTvShowQueries.updateHideStatus(isHiddenLong, catId, userId.toLong())
                }
            }
        }
    }

    override suspend fun updateAllCategoriesHideStatus(contentType: ContentType, isHidden: Boolean) {
        withContext(Dispatchers.IO) {
            val isHiddenLong = if (isHidden) 1L else 0L
            when (contentType) {
                ContentType.CHANNELS -> categoryQueries.updateAllHideStatus(isHiddenLong, userId.toLong())
                ContentType.MOVIES -> categoryVodQueries.updateAllHideStatus(isHiddenLong, userId.toLong())
                ContentType.SERIES -> categoryTvShowQueries.updateAllHideStatus(isHiddenLong, userId.toLong())
            }
        }
    }

    override suspend fun updateAllCategoriesPinStatus(contentType: ContentType, isPinned: Boolean) {
        withContext(Dispatchers.IO) {
            val isPinnedLong = if (isPinned) 1L else 0L
            when (contentType) {
                ContentType.CHANNELS -> categoryQueries.updateAllPinStatus(isPinnedLong, userId.toLong())
                ContentType.MOVIES -> categoryVodQueries.updateAllPinStatus(isPinnedLong, userId.toLong())
                ContentType.SERIES -> categoryTvShowQueries.updateAllPinStatus(isPinnedLong, userId.toLong())
            }
        }
    }

    /**
     * Clear-then-set, and it must be atomic — the original marks the equivalent `@Transaction`.
     *
     * Two reasons, and the second only became visible once these flows went reactive:
     *  - A failure between the clear and the set leaves **no** default category at all, silently
     *    discarding a setting the user chose rather than leaving it alone.
     *  - SQLDelight holds change notifications until a transaction commits. Unwrapped, the
     *    `clearAllDefaults()` notified on its own, so every observer briefly saw a state with no
     *    default selected before the real one arrived — a visible flicker in Manage Categories.
     */
    override suspend fun setDefaultCategory(categoryId: String, contentType: ContentType) {
        withContext(Dispatchers.IO) {
            categoryQueries.transaction {
                if (categoryId.startsWith(CUSTOM_GROUP_PREFIX)) {
                    val groupId = categoryId.removePrefix(CUSTOM_GROUP_PREFIX).toLong()
                    customGroupQueries.clearAllDefaults()
                    customGroupQueries.updateGroupDefaultStatus(1L, groupId)
                    categoryQueries.clearAllDefaults(userId.toLong())
                } else {
                    val catId = categoryId.toLong()
                    when (contentType) {
                        ContentType.CHANNELS -> {
                            categoryQueries.clearAllDefaults(userId.toLong())
                            categoryQueries.updateDefaultStatus(1L, catId, userId.toLong())
                            customGroupQueries.clearAllDefaults()
                        }
                        ContentType.MOVIES -> {
                            categoryVodQueries.clearAllDefaults(userId.toLong())
                            categoryVodQueries.updateDefaultStatus(1L, catId, userId.toLong())
                        }
                        ContentType.SERIES -> {
                            categoryTvShowQueries.clearAllDefaults(userId.toLong())
                            categoryTvShowQueries.updateDefaultStatus(1L, catId, userId.toLong())
                        }
                    }
                }
            }
        }
    }

    override suspend fun clearDefaultCategory(contentType: ContentType) {
        withContext(Dispatchers.IO) {
            // CHANNELS clears two tables; one transaction so observers never see a half-cleared
            // state, and so a failure cannot clear one and leave the other.
            categoryQueries.transaction {
                when (contentType) {
                    ContentType.CHANNELS -> {
                        categoryQueries.clearAllDefaults(userId.toLong())
                        customGroupQueries.clearAllDefaults()
                    }
                    ContentType.MOVIES -> categoryVodQueries.clearAllDefaults(userId.toLong())
                    ContentType.SERIES -> categoryTvShowQueries.clearAllDefaults(userId.toLong())
                }
            }
        }
    }

    override suspend fun resetAllPreferences() {
        withContext(Dispatchers.IO) {
            categoryQueries.updateAllPinStatus(0L, userId.toLong())
            categoryQueries.updateAllHideStatus(0L, userId.toLong())
            categoryVodQueries.updateAllPinStatus(0L, userId.toLong())
            categoryVodQueries.updateAllHideStatus(0L, userId.toLong())
            categoryTvShowQueries.updateAllPinStatus(0L, userId.toLong())
            categoryTvShowQueries.updateAllHideStatus(0L, userId.toLong())
        }
    }
}
