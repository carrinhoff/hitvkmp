package pt.hitv.core.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import pt.hitv.core.data.mapper.toParentalControl
import pt.hitv.core.database.ParentalControlQueries
import pt.hitv.core.domain.repositories.ParentalControlRepository
import pt.hitv.core.model.ParentalControl

/**
 * Repository implementation for parental control operations.
 */
class ParentalControlRepositoryImpl(
    private val parentalControlQueries: ParentalControlQueries
) : ParentalControlRepository {

    override fun getAllParentalControls(userId: Int): Flow<List<ParentalControl>> {
        // Reactive for the same reason as ParentalControlManagerImpl.getAllParentalControls:
        // protecting or un-protecting a category has to move the list it came from.
        return parentalControlQueries.selectAllByUserId(userId.toLong())
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toParentalControl() } }
    }

    override suspend fun getParentalControlByCategory(categoryId: Int, userId: Int): ParentalControl? {
        return parentalControlQueries.selectByCategory(categoryId.toLong(), userId.toLong())
            .executeAsOneOrNull()?.toParentalControl()
    }

    override fun getParentalControlByCategoryFlow(categoryId: Int, userId: Int): Flow<ParentalControl?> {
        return parentalControlQueries.selectByCategory(categoryId.toLong(), userId.toLong())
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.toParentalControl() }
    }

    override suspend fun insertParentalControl(parentalControl: ParentalControl) {
        parentalControlQueries.insertOrReplace(
            categoryId = parentalControl.categoryId.toLong(),
            categoryName = parentalControl.categoryName,
            userId = parentalControl.userId.toLong(),
            isProtected = if (parentalControl.isProtected) 1L else 0L,
            createdAt = parentalControl.createdAt
        )
    }

    override suspend fun updateProtectionStatus(categoryId: Int, userId: Int, isProtected: Boolean) {
        parentalControlQueries.updateProtectionStatus(if (isProtected) 1L else 0L, categoryId.toLong(), userId.toLong())
    }

    override suspend fun deleteParentalControl(categoryId: Int, userId: Int) {
        parentalControlQueries.deleteByCategory(categoryId.toLong(), userId.toLong())
    }

    override suspend fun deleteAllParentalControls(userId: Int) {
        parentalControlQueries.deleteAllByUserId(userId.toLong())
    }

    override fun getProtectedCategoriesCount(userId: Int): Flow<Int> {
        return parentalControlQueries.countProtected(userId.toLong())
            .asFlow()
            .mapToOne(Dispatchers.IO)
            .map { it.toInt() }
    }

    override suspend fun isCategoryProtected(categoryId: Int, userId: Int): Boolean {
        return parentalControlQueries.isCategoryProtected(categoryId.toLong(), userId.toLong())
            .executeAsOneOrNull()?.let { it != 0L } ?: false
    }
}
