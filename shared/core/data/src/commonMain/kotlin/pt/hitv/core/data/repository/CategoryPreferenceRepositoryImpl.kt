package pt.hitv.core.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
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

    override fun getAllCategoryPreferences(): Flow<List<CategoryPreference>> {
        // Since SQLDelight doesn't have reactive queries like Room's LiveData/Flow by default,
        // we emit a snapshot. For reactive updates, consider using asFlow() from sqldelight-coroutines.
        return flow {
            val allCategories = mutableListOf<CategoryPreference>()

            val channelCategories = categoryQueries.selectAllSorted(userId.toLong()).executeAsList()
            channelCategories.forEach { category ->
                allCategories.add(
                    CategoryPreference(
                        categoryId = category.categoryId.toString(),
                        categoryName = category.categoryName,
                        contentType = ContentType.CHANNELS,
                        isPinned = category.isPinned != 0L,
                        isHidden = category.isHidden != 0L,
                        isDefault = category.isDefault != 0L
                    )
                )
            }

            val movieCategories = categoryVodQueries.selectAllSorted(userId.toLong()).executeAsList()
            movieCategories.forEach { category ->
                allCategories.add(
                    CategoryPreference(
                        categoryId = category.categoryId.toString(),
                        categoryName = category.categoryName,
                        contentType = ContentType.MOVIES,
                        isPinned = category.isPinned != 0L,
                        isHidden = category.isHidden != 0L,
                        isDefault = category.isDefault != 0L
                    )
                )
            }

            val seriesCategories = categoryTvShowQueries.selectAllSorted(userId.toLong()).executeAsList()
            seriesCategories.forEach { category ->
                allCategories.add(
                    CategoryPreference(
                        categoryId = category.categoryId.toString(),
                        categoryName = category.categoryName,
                        contentType = ContentType.SERIES,
                        isPinned = category.isPinned != 0L,
                        isHidden = category.isHidden != 0L,
                        isDefault = category.isDefault != 0L
                    )
                )
            }

            val customGroups = customGroupQueries.selectAllGroupsSorted().executeAsList()
            customGroups.forEach { customGroup ->
                allCategories.add(
                    CategoryPreference(
                        categoryId = "$CUSTOM_GROUP_PREFIX${customGroup.groupId}",
                        categoryName = customGroup.groupName,
                        contentType = ContentType.CHANNELS,
                        isPinned = customGroup.isPinned != 0L,
                        isHidden = customGroup.isHidden != 0L,
                        isDefault = customGroup.isDefault != 0L
                    )
                )
            }

            emit(allCategories)
        }.flowOn(Dispatchers.IO)
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

    override suspend fun setDefaultCategory(categoryId: String, contentType: ContentType) {
        withContext(Dispatchers.IO) {
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

    override suspend fun clearDefaultCategory(contentType: ContentType) {
        withContext(Dispatchers.IO) {
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
