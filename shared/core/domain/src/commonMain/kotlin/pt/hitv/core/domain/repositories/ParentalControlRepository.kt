package pt.hitv.core.domain.repositories

import kotlinx.coroutines.flow.Flow
import pt.hitv.core.model.ParentalControl

/**
 * Repository interface for parental control operations.
 */
interface ParentalControlRepository {

    fun getAllParentalControls(userId: Int): Flow<List<ParentalControl>>

    suspend fun getParentalControlByCategory(categoryId: Int, userId: Int): ParentalControl?

    fun getParentalControlByCategoryFlow(categoryId: Int, userId: Int): Flow<ParentalControl?>

    suspend fun insertParentalControl(parentalControl: ParentalControl)

    suspend fun updateProtectionStatus(categoryId: Int, userId: Int, isProtected: Boolean)

    suspend fun deleteParentalControl(categoryId: Int, userId: Int)

    suspend fun deleteAllParentalControls(userId: Int)

    fun getProtectedCategoriesCount(userId: Int): Flow<Int>

    suspend fun isCategoryProtected(categoryId: Int, userId: Int): Boolean
}
