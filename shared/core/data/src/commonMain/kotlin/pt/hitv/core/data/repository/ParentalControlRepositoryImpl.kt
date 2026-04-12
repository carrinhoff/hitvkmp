package pt.hitv.core.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
        return flow {
            val controls = parentalControlQueries.selectAllByUserId(userId.toLong())
                .executeAsList()
                .map { it.toParentalControl() }
            emit(controls)
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getParentalControlByCategory(categoryId: Int, userId: Int): ParentalControl? {
        return parentalControlQueries.selectByCategory(categoryId.toLong(), userId.toLong())
            .executeAsOneOrNull()?.toParentalControl()
    }

    override fun getParentalControlByCategoryFlow(categoryId: Int, userId: Int): Flow<ParentalControl?> {
        return flow {
            emit(
                parentalControlQueries.selectByCategory(categoryId.toLong(), userId.toLong())
                    .executeAsOneOrNull()?.toParentalControl()
            )
        }.flowOn(Dispatchers.IO)
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
        return flow {
            emit(parentalControlQueries.countProtected(userId.toLong()).executeAsOne().toInt())
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun isCategoryProtected(categoryId: Int, userId: Int): Boolean {
        return parentalControlQueries.isCategoryProtected(categoryId.toLong(), userId.toLong())
            .executeAsOneOrNull()?.let { it != 0L } ?: false
    }
}
