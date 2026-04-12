package pt.hitv.core.designsystem.compose

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pt.hitv.core.designsystem.theme.ThemeManager
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.model.Category

/**
 * Advanced category bottom sheet content for channels.
 *
 * Displays a searchable list of categories with icons, counts, and selected state.
 * This is the CMP-compatible version — no Android string resources, no LocalConfiguration.
 *
 * Use inside a ModalBottomSheet.
 *
 * @param categories The list of server categories.
 * @param selectedCategoryFilter The current filter ID (e.g. "all", "favorites", "recently_viewed", or category ID).
 * @param categorySearchQuery Current search query for filtering categories.
 * @param onCategorySearchQueryChanged Callback when search text changes.
 * @param onCategorySelected Callback with the filter ID when a category is selected.
 * @param onDismiss Callback to close the bottom sheet.
 * @param allCount Display count for the "All" category.
 * @param favoritesCount Display count for Favorites.
 * @param recentCount Display count for Recently Viewed.
 * @param categoryCounts Map of category ID string to display count.
 */
@Composable
fun AdvancedCategoryBottomSheetContent(
    categories: List<Category>,
    selectedCategoryFilter: String,
    categorySearchQuery: String,
    onCategorySearchQueryChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    allFilterId: String = "All",
    favoritesFilterId: String = "Favorites",
    recentlyViewedFilterId: String = "RecentlyViewed",
    allCount: String = "...",
    favoritesCount: String = "0",
    recentCount: String = "0",
    categoryCounts: Map<String, String> = emptyMap()
) {
    val themeColors = getThemeColors()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        var isSearchVisible by remember { mutableStateOf(categorySearchQuery.isNotEmpty()) }

        LaunchedEffect(categorySearchQuery) {
            if (categorySearchQuery.isNotEmpty() && !isSearchVisible) {
                isSearchVisible = true
            }
        }

        // --- Header: search mode or browse mode ---
        if (isSearchVisible) {
            SearchHeader(
                categorySearchQuery = categorySearchQuery,
                onCategorySearchQueryChanged = onCategorySearchQueryChanged,
                onCloseSearch = {
                    onCategorySearchQueryChanged("")
                    isSearchVisible = false
                },
                themeColors = themeColors
            )
        } else {
            BrowseHeader(
                categoryCount = categories.size + 3, // All + Favorites + Recently Viewed + categories
                onSearchClick = { isSearchVisible = true },
                onDismiss = onDismiss,
                themeColors = themeColors
            )
        }

        // --- Filtered category list ---
        val filteredCategories = if (categorySearchQuery.isBlank()) {
            categories
        } else {
            categories.filter {
                it.categoryName.contains(categorySearchQuery, ignoreCase = true)
            }
        }

        // Search results count
        if (categorySearchQuery.isNotBlank() && isSearchVisible) {
            val headerExtra = listOf("all", "favorites", "recently viewed").count {
                it.contains(categorySearchQuery, ignoreCase = true)
            }
            Text(
                "Showing ${filteredCategories.size + headerExtra} of ${categories.size + 3}",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.textColor.copy(alpha = 0.7f)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // All
            if (categorySearchQuery.isBlank() || "all".contains(categorySearchQuery, ignoreCase = true)) {
                item(key = "__all") {
                    ChannelCategoryItemCard(
                        icon = Icons.Default.Apps,
                        name = "All",
                        count = allCount,
                        isSelected = selectedCategoryFilter == allFilterId,
                        onClick = { onCategorySelected(allFilterId) },
                        themeColors = themeColors
                    )
                }
            }

            // Favorites
            if (categorySearchQuery.isBlank() || "favorites".contains(categorySearchQuery, ignoreCase = true)) {
                item(key = "__favorites") {
                    ChannelCategoryItemCard(
                        icon = Icons.Default.Star,
                        name = "Favorites",
                        count = favoritesCount,
                        isSelected = selectedCategoryFilter == favoritesFilterId,
                        onClick = { onCategorySelected(favoritesFilterId) },
                        themeColors = themeColors
                    )
                }
            }

            // Recently Viewed
            if (categorySearchQuery.isBlank() || "recently viewed".contains(categorySearchQuery, ignoreCase = true)) {
                item(key = "__recently_viewed") {
                    ChannelCategoryItemCard(
                        icon = Icons.Default.History,
                        name = "Recently Viewed",
                        count = recentCount,
                        isSelected = selectedCategoryFilter == recentlyViewedFilterId,
                        onClick = { onCategorySelected(recentlyViewedFilterId) },
                        themeColors = themeColors
                    )
                }
            }

            // Server categories
            items(filteredCategories, key = { it.categoryId }) { category ->
                ChannelCategoryItemCard(
                    icon = Icons.Default.Category,
                    name = category.categoryName,
                    count = categoryCounts[category.categoryId.toString()] ?: "...",
                    isSelected = selectedCategoryFilter == category.categoryId.toString(),
                    onClick = { onCategorySelected(category.categoryId.toString()) },
                    themeColors = themeColors
                )
            }

            // Empty state
            if (
                categorySearchQuery.isNotBlank()
                && filteredCategories.isEmpty()
                && !listOf("all", "favorites", "recently viewed").any {
                    it.contains(categorySearchQuery, ignoreCase = true)
                }
            ) {
                item(key = "__empty") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = themeColors.backgroundSecondary.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = themeColors.textColor
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No categories found",
                                style = MaterialTheme.typography.titleMedium,
                                color = themeColors.textColor
                            )
                            Text(
                                "Try a different search term",
                                style = MaterialTheme.typography.bodyMedium,
                                color = themeColors.textColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Internal components ---

@Composable
private fun SearchHeader(
    categorySearchQuery: String,
    onCategorySearchQueryChanged: (String) -> Unit,
    onCloseSearch: () -> Unit,
    themeColors: ThemeManager.AppTheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            val focusRequester = remember { FocusRequester() }
            var isSearchFocused by remember { mutableStateOf(false) }

            var textFieldValue by remember(categorySearchQuery) {
                mutableStateOf(
                    TextFieldValue(
                        text = categorySearchQuery,
                        selection = TextRange(categorySearchQuery.length)
                    )
                )
            }

            LaunchedEffect(categorySearchQuery) {
                if (textFieldValue.text != categorySearchQuery) {
                    textFieldValue = textFieldValue.copy(
                        text = categorySearchQuery,
                        selection = TextRange(categorySearchQuery.length)
                    )
                }
            }

            LaunchedEffect(Unit) {
                delay(100)
                focusRequester.requestFocus()
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = themeColors.backgroundSecondary.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isSearchFocused) themeColors.primaryColor else themeColors.textColor,
                        modifier = Modifier.size(18.dp)
                    )
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = {
                            textFieldValue = it
                            if (it.text != categorySearchQuery) {
                                onCategorySearchQueryChanged(it.text)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                isSearchFocused = focusState.isFocused
                            },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = themeColors.textColor,
                            fontSize = 16.sp
                        ),
                        cursorBrush = SolidColor(themeColors.primaryColor),
                        decorationBox = { innerTextField ->
                            if (textFieldValue.text.isEmpty()) {
                                Text(
                                    "Search categories...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = themeColors.textColor.copy(alpha = 0.7f)
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (textFieldValue.text.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onCategorySearchQueryChanged("")
                                textFieldValue = TextFieldValue("")
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = themeColors.textColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onCloseSearch,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = themeColors.textColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun BrowseHeader(
    categoryCount: Int,
    onSearchClick: () -> Unit,
    onDismiss: () -> Unit,
    themeColors: ThemeManager.AppTheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Browse categories",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "$categoryCount categories available",
                style = MaterialTheme.typography.bodyMedium,
                color = themeColors.textColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSearchClick) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search categories",
                    tint = themeColors.textColor
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = themeColors.textColor
                )
            }
        }
    }
}

/**
 * Category item card with icon, name, count, and selected state.
 */
@Composable
private fun ChannelCategoryItemCard(
    icon: ImageVector,
    name: String,
    count: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    themeColors: ThemeManager.AppTheme
) {
    val animatedElevation by animateDpAsState(
        targetValue = if (isSelected) 6.dp else 1.dp,
        animationSpec = tween(200),
        label = "categoryElevation"
    )

    if (isSelected) {
        // Selected state: primary-tinted background with border and check icon
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = themeColors.primaryColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    border = BorderStroke(2.dp, themeColors.primaryColor.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onClick() }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = themeColors.primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.primaryColor
                        )
                        if (count.isNotEmpty() && count != "..." && count != "0") {
                            Text(
                                text = "$count channels",
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.primaryColor.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (count.isNotEmpty() && count != "..." && count != "0") {
                        Text(
                            text = count,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.primaryColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = themeColors.primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    } else {
        // Unselected state: card with icon box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            colors = CardDefaults.cardColors(
                containerColor = themeColors.cardColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, themeColors.primaryColor.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = themeColors.primaryColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = themeColors.primaryColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = themeColors.textColor
                        )
                        if (count.isNotEmpty() && count != "..." && count != "0") {
                            Text(
                                text = "$count channels",
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.textColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (count.isNotEmpty() && count != "..." && count != "0") {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = themeColors.primaryColor.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = count,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = themeColors.primaryColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
