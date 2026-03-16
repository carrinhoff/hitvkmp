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
                        isPinned = category.isPinned,
                        isHidden = category.isHidden,
                        isDefault = category.isDefault
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
                        isPinned = category.isPinned,
                        isHidden = category.isHidden,
                        isDefault = category.isDefault
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
                        isPinned = category.isPinned,
                        isHidden = category.isHidden,
                        isDefault = category.isDefault
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
                        isPinned = customGroup.isPinned,
                        isHidden = customGroup.isHidden,
                        isDefault = customGroup.isDefault
                    )
                )
            }

            emit(allCategories)
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun updateCategoryPinStatus(categoryId: String, contentType: ContentType, isPinned: Boolean) {
        withContext(Dispatchers.IO) {
            if (categoryId.startsWith(CUSTOM_GROUP_PREFIX)) {
                val groupId = categoryId.removePrefix(CUSTOM_GROUP_PREFIX).toLong()
                customGroupQueries.updateGroupPinStatus(isPinned, groupId)
            } else {
                val catId = categoryId.toInt()
                when (contentType) {
                    ContentType.CHANNELS -> categoryQueries.updatePinStatus(isPinned, catId, userId)
                    ContentType.MOVIES -> categoryVodQueries.updatePinStatus(isPinned, catId, userId)
                    ContentType.SERIES -> categoryTvShowQueries.updatePinStatus(isPinned, catId, userId)
                }
            }
        }
    }

    override suspend fun updateCategoryHideStatus(categoryId: String, contentType: ContentType, isHidden: Boolean) {
        withContext(Dispatchers.IO) {
            if (categoryId.startsWith(CUSTOM_GROUP_PREFIX)) {
                val groupId = categoryId.removePrefix(CUSTOM_GROUP_PREFIX).toLong()
                customGroupQueries.updateGroupHideStatus(isHidden, groupId)
            } else {
                val catId = categoryId.toInt()
                when (contentType) {
                    ContentType.CHANNELS -> categoryQueries.updateHideStatus(isHidden, catId, userId)
                    ContentType.MOVIES -> categoryVodQueries.updateHideStatus(isHidden, catId, userId)
                    ContentType.SERIES -> categoryTvShowQueries.updateHideStatus(isHidden, catId, userId)
                }
            }
        }
    }

    override suspend fun updateAllCategoriesHideStatus(contentType: ContentType, isHidden: Boolean) {
        withContext(Dispatchers.IO) {
            when (contentType) {
                ContentType.CHANNELS -> categoryQueries.updateAllHideStatus(isHidden, userId)
                ContentType.MOVIES -> categoryVodQueries.updateAllHideStatus(isHidden, userId)
                ContentType.SERIES -> categoryTvShowQueries.updateAllHideStatus(isHidden, userId)
            }
        }
    }

    override suspend fun updateAllCategoriesPinStatus(contentType: ContentType, isPinned: Boolean) {
        withContext(Dispatchers.IO) {
            when (contentType) {
                ContentType.CHANNELS -> categoryQueries.updateAllPinStatus(isPinned, userId)
                ContentType.MOVIES -> categoryVodQueries.updateAllPinStatus(isPinned, userId)
                ContentType.SERIES -> categoryTvShowQueries.updateAllPinStatus(isPinned, userId)
            }
        }
    }

    override suspend fun setDefaultCategory(categoryId: String, contentType: ContentType) {
        withContext(Dispatchers.IO) {
            if (categoryId.startsWith(CUSTOM_GROUP_PREFIX)) {
                val groupId = categoryId.removePrefix(CUSTOM_GROUP_PREFIX).toLong()
                customGroupQueries.clearAllDefaults()
                customGroupQueries.updateGroupDefaultStatus(true, groupId)
                categoryQueries.clearAllDefaults(userId)
            } else {
                val catId = categoryId.toInt()
                when (contentType) {
                    ContentType.CHANNELS -> {
                        categoryQueries.clearAllDefaults(userId)
                        categoryQueries.updateDefaultStatus(true, catId, userId)
                        customGroupQueries.clearAllDefaults()
                    }
                    ContentType.MOVIES -> {
                        categoryVodQueries.clearAllDefaults(userId)
                        categoryVodQueries.updateDefaultStatus(true, catId, userId)
                    }
                    ContentType.SERIES -> {
                        categoryTvShowQueries.clearAllDefaults(userId)
                        categoryTvShowQueries.updateDefaultStatus(true, catId, userId)
                    }
                }
            }
        }
    }

    override suspend fun clearDefaultCategory(contentType: ContentType) {
        withContext(Dispatchers.IO) {
            when (contentType) {
                ContentType.CHANNELS -> {
                    categoryQueries.clearAllDefaults(userId)
                    customGroupQueries.clearAllDefaults()
                }
                ContentType.MOVIES -> categoryVodQueries.clearAllDefaults(userId)
                ContentType.SERIES -> categoryTvShowQueries.clearAllDefaults(userId)
            }
        }
    }

    override suspend fun resetAllPreferences() {
        withContext(Dispatchers.IO) {
            categoryQueries.updateAllPinStatus(false, userId)
            categoryQueries.updateAllHideStatus(false, userId)
            categoryVodQueries.updateAllPinStatus(false, userId)
            categoryVodQueries.updateAllHideStatus(false, userId)
            categoryTvShowQueries.updateAllPinStatus(false, userId)
            categoryTvShowQueries.updateAllHideStatus(false, userId)
        }
    }
}
