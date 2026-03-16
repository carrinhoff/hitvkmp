package pt.hitv.core.designsystem.compose

import pt.hitv.core.designsystem.theme.ThemeManager

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pt.hitv.core.model.Category
import pt.hitv.core.model.CustomGroup
import pt.hitv.core.designsystem.theme.getThemeColors

// Data class for category selection (used by all fragments)
data class CategorySelectionItem(
    val id: String,
    val name: String,
    val count: String = ""
)

/**
 * Advanced category bottom sheet component used across all fragments.
 *
 * String parameters replace Android string resources for cross-platform compatibility.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedCategoryBottomSheet(
    categories: List<Category>,
    selectedCategory: String,
    categorySearchQuery: String,
    onCategorySearchQueryChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    allCount: String = "...",
    favoritesCount: String = "0",
    recentCount: String = "0",
    lastAddedCount: String = "0",
    continueWatchingCount: String = "0",
    categoryCounts: Map<String, String> = emptyMap(),
    contentType: String = "items",
    onManageCategories: (() -> Unit)? = null,
    customGroups: List<CustomGroup> = emptyList(),
    // String parameters replacing Android string resources
    allLabel: String = "All",
    favoritesLabel: String = "Favorites",
    recentlyViewedLabel: String = "Recently Viewed",
    lastAddedLabel: String = "Last Added",
    continueWatchingLabel: String = "Continue Watching",
    browseCategoriesTitle: String = "Browse Categories",
    categoriesAvailableLabel: String = "categories available",
    manageCategoriesLabel: String = "Manage Categories",
    searchPlaceholder: String = "Search categories...",
    noCategoriesFoundLabel: String = "No categories found",
    tryDifferentSearchLabel: String = "Try a different search term",
    showingCategoriesFormat: String = "Showing %d of %d",
    channelsLabel: String = "channels",
    moviesLabel: String = "movies",
    seriesLabel: String = "series",
    itemsLabel: String = "items"
) {
    val themeColors = getThemeColors()

    val contentTypeText = when (contentType) {
        "channels" -> channelsLabel
        "movies" -> moviesLabel
        "series" -> seriesLabel
        else -> itemsLabel
    }

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

        if (isSearchVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    val focusRequester = remember { FocusRequester() }
                    var isSearchFocused by remember { mutableStateOf(false) }

                    var textFieldValue by remember(categorySearchQuery, isSearchVisible) {
                        mutableStateOf(
                            TextFieldValue(
                                text = categorySearchQuery,
                                selection = TextRange(categorySearchQuery.length)
                            )
                        )
                    }

                    LaunchedEffect(categorySearchQuery, isSearchVisible) {
                        if (textFieldValue.text != categorySearchQuery) {
                            textFieldValue = textFieldValue.copy(
                                text = categorySearchQuery,
                                selection = TextRange(categorySearchQuery.length)
                            )
                        }
                    }

                    LaunchedEffect(isSearchVisible) {
                        if (isSearchVisible) {
                            delay(100)
                            focusRequester.requestFocus()
                        }
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
                                            searchPlaceholder,
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
                    onClick = {
                        onCategorySearchQueryChanged("")
                        isSearchVisible = false
                    },
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
        } else {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            browseCategoriesTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "${categories.size + 4} $categoriesAvailableLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = themeColors.textColor.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onManageCategories != null) {
                            Surface(
                                onClick = onManageCategories,
                                modifier = Modifier.padding(end = 8.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = themeColors.primaryColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, themeColors.primaryColor.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = manageCategoriesLabel,
                                        tint = themeColors.primaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = manageCategoriesLabel,
                                        color = themeColors.primaryColor,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { isSearchVisible = true }) {
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
        }

        val filteredCategories = if (categorySearchQuery.isBlank()) {
            categories
        } else {
            categories.filter {
                it.categoryName.contains(categorySearchQuery, ignoreCase = true)
            }
        }

        val filteredCustomGroups = if (categorySearchQuery.isBlank()) {
            customGroups
        } else {
            customGroups.filter { it.name.contains(categorySearchQuery, ignoreCase = true) }
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
            if (categorySearchQuery.isBlank() || "all".contains(categorySearchQuery, ignoreCase = true)) {
                item {
                    CategoryItemCard(
                        icon = Icons.Default.Apps,
                        name = allLabel,
                        count = allCount,
                        isSelected = selectedCategory == allLabel,
                        onClick = { onCategorySelected(allLabel) },
                        contentType = contentTypeText,
                        themeColors = themeColors
                    )
                }
            }

            if (categorySearchQuery.isBlank() || "favorites".contains(categorySearchQuery, ignoreCase = true)) {
                item {
                    CategoryItemCard(
                        icon = Icons.Default.Star,
                        name = favoritesLabel,
                        count = favoritesCount,
                        isSelected = selectedCategory == favoritesLabel,
                        onClick = { onCategorySelected(favoritesLabel) },
                        contentType = contentTypeText,
                        themeColors = themeColors
                    )
                }
            }

            if (categorySearchQuery.isBlank() || "recently viewed".contains(categorySearchQuery, ignoreCase = true)) {
                item {
                    CategoryItemCard(
                        icon = Icons.Default.History,
                        name = recentlyViewedLabel,
                        count = recentCount,
                        isSelected = selectedCategory == recentlyViewedLabel,
                        onClick = { onCategorySelected(recentlyViewedLabel) },
                        contentType = contentTypeText,
                        themeColors = themeColors
                    )
                }
            }

            if (categorySearchQuery.isBlank() || "last added".contains(categorySearchQuery, ignoreCase = true)) {
                item {
                    CategoryItemCard(
                        icon = Icons.Default.NewReleases,
                        name = lastAddedLabel,
                        count = lastAddedCount,
                        isSelected = selectedCategory == lastAddedLabel,
                        onClick = { onCategorySelected(lastAddedLabel) },
                        contentType = contentTypeText,
                        themeColors = themeColors
                    )
                }
            }

            if (categorySearchQuery.isBlank() || "continue watching".contains(categorySearchQuery, ignoreCase = true)) {
                item {
                    CategoryItemCard(
                        icon = Icons.Default.PlayCircle,
                        name = continueWatchingLabel,
                        count = continueWatchingCount,
                        isSelected = selectedCategory == continueWatchingLabel,
                        onClick = { onCategorySelected(continueWatchingLabel) },
                        contentType = contentTypeText,
                        themeColors = themeColors
                    )
                }
            }

            items(filteredCustomGroups, key = { it.id }) { group ->
                CategoryItemCard(
                    icon = Icons.Default.Folder,
                    name = group.name,
                    count = group.channelCount?.toString() ?: "...",
                    isSelected = selectedCategory == group.name,
                    onClick = { onCategorySelected(group.name) },
                    contentType = contentTypeText,
                    themeColors = themeColors
                )
            }

            items(filteredCategories) { category ->
                CategoryItemCard(
                    icon = Icons.Default.Category,
                    name = category.categoryName,
                    count = categoryCounts[category.categoryId.toString()] ?: "...",
                    isSelected = selectedCategory == category.categoryName,
                    onClick = { onCategorySelected(category.categoryName) },
                    contentType = contentTypeText,
                    themeColors = themeColors
                )
            }

            if (
                categorySearchQuery.isNotBlank()
                && filteredCategories.isEmpty()
                && filteredCustomGroups.isEmpty()
                && !listOf("all", "favorites", "recently viewed").any {
                    it.contains(categorySearchQuery, ignoreCase = true)
                }
            ) {
                item {
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
                                noCategoriesFoundLabel,
                                style = MaterialTheme.typography.titleMedium,
                                color = themeColors.textColor
                            )
                            Text(
                                tryDifferentSearchLabel,
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

/**
 * Category item card component used in bottom sheets
 */
@Composable
fun CategoryItemCard(
    icon: ImageVector,
    name: String,
    count: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    contentType: String = "items",
    themeColors: ThemeManager.AppTheme,
    selectedLabel: String = "Selected"
) {
    val animatedElevation by animateDpAsState(
        targetValue = if (isSelected) 6.dp else 1.dp,
        animationSpec = tween(200), label = ""
    )

    if (isSelected) {
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
                                text = "$count $contentType",
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.primaryColor.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        contentDescription = selectedLabel,
                        tint = themeColors.primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    } else {
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
                                text = "$count $contentType",
                                style = MaterialTheme.typography.bodySmall,
                                color = themeColors.textColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
