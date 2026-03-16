package pt.hitv.core.ui.categories

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.hitv.core.model.ContentType

/**
 * Screen mode enum for Categories vs Custom Groups
 */
enum class ManagementScreenMode {
    CATEGORIES,
    CUSTOM_GROUPS
}

/**
 * Screen for managing category preferences (pin, hide, set default).
 * Ported from Android-specific to multiplatform. Removed hiltViewModel() calls,
 * Android-specific platform detection, and stringResource() usage.
 *
 * @param viewModel The manage categories ViewModel
 * @param backgroundColor Background color
 * @param primaryColor Primary accent color
 * @param secondaryBackgroundColor Secondary background color
 * @param textColor Primary text color
 * @param textSecondaryColor Secondary text color
 * @param onNavigateBack Callback for back navigation
 * @param onCategoryPinToggled Callback when pin toggled
 * @param onCategoryHideToggled Callback when hide toggled
 * @param onCategorySetAsDefault Callback when default set
 * @param onTabChanged Callback when tab changes
 * @param onSearchUsed Callback when search is used
 * @param onShowAllCategories Callback for show all
 * @param onHideAllCategories Callback for hide all
 * @param singleContentType If non-null, shows only this content type
 * @param customGroupsContent Optional composable for custom groups management
 * @param modifier Optional modifier
 * @param manageCategoriesLabel Screen title
 * @param manageCategoriesDesc Screen description
 * @param backLabel Back button content description
 */
@Composable
fun ManageCategoriesScreen(
    viewModel: ManageCategoriesViewModel,
    backgroundColor: Color = Color(0xFF121212),
    primaryColor: Color = Color(0xFF1DB954),
    secondaryBackgroundColor: Color = Color(0xFF1E1E1E),
    textColor: Color = Color.White,
    textSecondaryColor: Color = Color(0xFFB3B3B3),
    onNavigateBack: () -> Unit,
    onCategoryPinToggled: (String, String, ContentType, Boolean) -> Unit = { _, _, _, _ -> },
    onCategoryHideToggled: (String, String, ContentType, Boolean) -> Unit = { _, _, _, _ -> },
    onCategorySetAsDefault: (String, String, ContentType) -> Unit = { _, _, _ -> },
    onTabChanged: (ContentType) -> Unit = {},
    onSearchUsed: (String, Int) -> Unit = { _, _ -> },
    onShowAllCategories: (ContentType) -> Unit = {},
    onHideAllCategories: (ContentType) -> Unit = {},
    singleContentType: ContentType? = null,
    customGroupsContent: (@Composable (Modifier) -> Unit)? = null,
    modifier: Modifier = Modifier,
    manageCategoriesLabel: String = "Manage Categories",
    manageCategoriesDesc: String = "Organize your content categories",
    backLabel: String = "Back"
) {
    val categories by viewModel.categories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var screenMode by remember { mutableStateOf(ManagementScreenMode.CATEGORIES) }
    var selectedTab by remember { mutableStateOf(singleContentType ?: ContentType.CHANNELS) }
    var searchQuery by remember { mutableStateOf("") }

    var isVisible by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(600, delayMillis = 100),
        label = "contentFade"
    )

    LaunchedEffect(Unit) {
        isVisible = true
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            kotlinx.coroutines.delay(500)
            val filteredCount = categories
                .filter { it.contentType == selectedTab }
                .count { it.categoryName.contains(searchQuery, ignoreCase = true) }
            onSearchUsed(searchQuery, filteredCount)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor,
                        secondaryBackgroundColor,
                        backgroundColor.copy(alpha = 0.9f)
                    )
                )
            )
            .statusBarsPadding()
    ) {
        // Portrait layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .alpha(contentAlpha)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = textColor.copy(alpha = 0.08f),
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .clickable(onClick = onNavigateBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = backLabel,
                        tint = textColor.copy(alpha = 0.9f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column {
                    Text(
                        text = manageCategoriesLabel,
                        color = textColor,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = manageCategoriesDesc,
                        color = textSecondaryColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Mode Switcher
            ModernModeSelector(
                selectedMode = screenMode,
                onModeSelected = { screenMode = it },
                primaryColor = primaryColor,
                textColor = textColor
            )

            Spacer(modifier = Modifier.height(20.dp))

            when (screenMode) {
                ManagementScreenMode.CATEGORIES -> {
                    if (singleContentType == null) {
                        ModernTabSelector(
                            selectedTab = selectedTab,
                            onTabSelected = {
                                selectedTab = it
                                onTabChanged(it)
                            },
                            primaryColor = primaryColor,
                            textColor = textColor,
                            categories = categories
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Search bar
                    SearchBar(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        primaryColor = primaryColor,
                        textColor = textColor,
                        textSecondaryColor = textSecondaryColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ModernBulkActionsBar(
                        selectedTab = selectedTab,
                        onShowAll = {
                            viewModel.showAllCategories(selectedTab)
                            onShowAllCategories(selectedTab)
                        },
                        onHideAll = {
                            viewModel.hideAllCategories(selectedTab)
                            onHideAllCategories(selectedTab)
                        },
                        primaryColor = primaryColor,
                        textColor = textColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = primaryColor,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    } else {
                        val filteredCategories = categories
                            .filter { it.contentType == selectedTab }
                            .filter { category ->
                                if (searchQuery.isEmpty()) true
                                else category.categoryName.contains(searchQuery, ignoreCase = true)
                            }

                        if (filteredCategories.isEmpty()) {
                            EmptyState(
                                primaryColor = primaryColor,
                                textColor = textColor,
                                textSecondaryColor = textSecondaryColor,
                                searchQuery = searchQuery,
                                onClearSearch = { searchQuery = "" }
                            )
                        } else {
                            ModernCategoriesList(
                                categories = filteredCategories,
                                primaryColor = primaryColor,
                                textColor = textColor,
                                textSecondaryColor = textSecondaryColor,
                                onTogglePin = { categoryId, categoryName, contentType, isPinned ->
                                    viewModel.togglePin(categoryId, contentType)
                                    onCategoryPinToggled(categoryId, categoryName, contentType, !isPinned)
                                },
                                onToggleHide = { categoryId, categoryName, contentType, isHidden ->
                                    viewModel.toggleHide(categoryId, contentType)
                                    onCategoryHideToggled(categoryId, categoryName, contentType, !isHidden)
                                },
                                onSetAsDefault = { categoryId, categoryName, contentType ->
                                    viewModel.toggleDefault(categoryId, contentType)
                                    onCategorySetAsDefault(categoryId, categoryName, contentType)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                ManagementScreenMode.CUSTOM_GROUPS -> {
                    customGroupsContent?.invoke(Modifier.weight(1f))
                        ?: Box(
                            modifier = Modifier.weight(1f).fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Custom Groups",
                                color = textColor.copy(alpha = 0.5f)
                            )
                        }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
