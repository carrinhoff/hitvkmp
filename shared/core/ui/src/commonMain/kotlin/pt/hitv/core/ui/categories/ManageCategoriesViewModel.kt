package pt.hitv.core.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.hitv.core.domain.repositories.CategoryPreferenceRepository
import pt.hitv.core.model.CategoryPreference
import pt.hitv.core.model.ContentType

/**
 * ViewModel for managing category preferences (pin, hide, default).
 * Ported from Hilt to plain ViewModel for Koin injection.
 */
class ManageCategoriesViewModel(
    private val categoryPreferenceRepository: CategoryPreferenceRepository
) : ViewModel() {

    private val _categories = MutableStateFlow<List<CategoryPreference>>(emptyList())
    val categories: StateFlow<List<CategoryPreference>> = _categories.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                categoryPreferenceRepository.getAllCategoryPreferences()
                    .collect { allCategories ->
                        _categories.value = allCategories
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }

    fun togglePin(categoryId: String, contentType: ContentType) {
        viewModelScope.launch {
            try {
                val currentCategory = _categories.value.find {
                    it.categoryId == categoryId && it.contentType == contentType
                }
                val newPinStatus = !(currentCategory?.isPinned ?: false)
                categoryPreferenceRepository.updateCategoryPinStatus(categoryId, contentType, newPinStatus)
            } catch (e: Exception) {
                // Error handled silently
            }
        }
    }

    fun toggleHide(categoryId: String, contentType: ContentType) {
        viewModelScope.launch {
            try {
                val currentCategory = _categories.value.find {
                    it.categoryId == categoryId && it.contentType == contentType
                }
                val newHideStatus = !(currentCategory?.isHidden ?: false)
                categoryPreferenceRepository.updateCategoryHideStatus(categoryId, contentType, newHideStatus)
            } catch (e: Exception) {
                // Error handled silently
            }
        }
    }

    fun getCategoriesByType(contentType: ContentType): List<CategoryPreference> {
        return _categories.value.filter { it.contentType == contentType }
    }

    fun showAllCategories(contentType: ContentType) {
        viewModelScope.launch {
            try {
                categoryPreferenceRepository.updateAllCategoriesHideStatus(contentType, false)
            } catch (_: Exception) {}
        }
    }

    fun hideAllCategories(contentType: ContentType) {
        viewModelScope.launch {
            try {
                categoryPreferenceRepository.updateAllCategoriesHideStatus(contentType, true)
            } catch (_: Exception) {}
        }
    }

    fun pinAllCategories(contentType: ContentType) {
        viewModelScope.launch {
            try {
                categoryPreferenceRepository.updateAllCategoriesPinStatus(contentType, true)
            } catch (_: Exception) {}
        }
    }

    fun unpinAllCategories(contentType: ContentType) {
        viewModelScope.launch {
            try {
                categoryPreferenceRepository.updateAllCategoriesPinStatus(contentType, false)
            } catch (_: Exception) {}
        }
    }

    fun resetAllPreferences() {
        viewModelScope.launch {
            try {
                categoryPreferenceRepository.resetAllPreferences()
            } catch (_: Exception) {}
        }
    }

    fun toggleDefault(categoryId: String, contentType: ContentType) {
        viewModelScope.launch {
            try {
                val currentCategory = _categories.value.find {
                    it.categoryId == categoryId && it.contentType == contentType
                }

                if (currentCategory?.isHidden == true && !(currentCategory.isDefault)) {
                    return@launch
                }

                val isCurrentlyDefault = currentCategory?.isDefault ?: false

                if (isCurrentlyDefault) {
                    categoryPreferenceRepository.clearDefaultCategory(contentType)
                } else {
                    categoryPreferenceRepository.setDefaultCategory(categoryId, contentType)
                }
            } catch (_: Exception) {}
        }
    }

    fun clearDefault(contentType: ContentType) {
        viewModelScope.launch {
            try {
                categoryPreferenceRepository.clearDefaultCategory(contentType)
            } catch (_: Exception) {}
        }
    }
}
