package pt.hitv.feature.channels.epg

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.hitv.core.model.Category

/**
 * Step 1 of the EPG flow: pick a category, then [EpgScreenMobile] renders its grid.
 *
 * Faithful port of the original's `feature/channels/epg/EpgCategoryScreen.kt` — same header,
 * search bar with clear button, scroll-position preservation, and the three distinct empty states
 * (loading / no EPG at all / no search results), which the original deliberately kept separate so
 * "you have no guide data" never looks like "your search matched nothing".
 *
 * Deviations: `stringResource` → String parameters with English defaults (consistent with the rest
 * of this port), and the TV focus/scale affordances on the back button are dropped since this
 * project is mobile-only.
 */
@Composable
fun EpgCategorySelectionScreen(
    categoriesWithEpg: List<Pair<Category, Int>>,
    themeColors: EpgThemeColors,
    onCategorySelected: (String) -> Unit,
    onBackPressed: () -> Unit,
    isLoading: Boolean = false,
    scrollIndex: Int = 0,
    scrollOffset: Int = 0,
    searchQuery: String = "",
    onSearchQueryChanged: (String) -> Unit = {},
    onScrollPositionChanged: (Int, Int) -> Unit = { _, _ -> },
    titleLabel: String = "EPG Categories",
    subtitleLabel: String = "Select a category to view its guide",
    searchHint: String = "Search categories",
    noEpgTitle: String = "No guide data",
    noEpgSubtitle: String = "There are no programmes available at the moment.",
    noResultsTitle: String = "No categories",
    noResultsSubtitle: String = "Try a different search.",
) {
    val filteredCategories = remember(categoriesWithEpg, searchQuery) {
        if (searchQuery.isBlank()) {
            categoriesWithEpg
        } else {
            categoriesWithEpg.filter { (category, _) ->
                matchesFlexibleSearch(category.categoryName, searchQuery)
            }
        }
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = scrollIndex,
        initialFirstVisibleItemScrollOffset = scrollOffset
    )

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        onScrollPositionChanged(
            listState.firstVisibleItemIndex,
            listState.firstVisibleItemScrollOffset
        )
    }

    // No statusBarsPadding() here: every pushed Voyager screen already sits inside
    // TabContentHost's `Box(modifier.statusBarsPadding())` (AdaptiveScaffold.kt:292, Navigator at
    // :315), so this screen does not own that inset.
    //
    // Note it was NOT double-padding before — `statusBarsPadding()` is consumption-aware, so a
    // nested call sees the insets already consumed and contributes zero. Verified on-device:
    // removing it left the layout pixel-identical. Dropped purely so the inset has one owner.
    // The grid never had it, for the same reason.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.backgroundPrimary)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // The original had no horizontal padding here, which left the 48dp circular
                    // back button flush against the screen edge — visibly clipped on-device, and
                    // inside the display cutout on iOS. 8.dp aligns it with the search field
                    // below (16.dp outer padding minus the button's own 8.dp visual inset).
                    .padding(start = 8.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = themeColors.textColor.copy(alpha = 0.08f),
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .clickable(onClick = onBackPressed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = themeColors.textColor.copy(alpha = 0.9f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column {
                    Text(
                        text = titleLabel,
                        color = themeColors.textColor,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = subtitleLabel,
                        color = themeColors.textColor.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(50.dp)
                    .background(color = themeColors.cardColor, shape = RoundedCornerShape(25.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = themeColors.textSecondaryColor,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        modifier = Modifier.weight(1f),
                        textStyle = LocalTextStyle.current.copy(
                            color = themeColors.textColor,
                            fontSize = 16.sp
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(themeColors.primaryColor),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = searchHint,
                                    color = themeColors.textSecondaryColor.copy(alpha = 0.6f),
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChanged("") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = themeColors.textSecondaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = themeColors.primaryColor)
                    }
                }

                categoriesWithEpg.isEmpty() -> {
                    EpgEmptyState(
                        icon = Icons.Default.EventBusy,
                        title = noEpgTitle,
                        subtitle = noEpgSubtitle,
                        themeColors = themeColors
                    )
                }

                filteredCategories.isEmpty() -> {
                    EpgEmptyState(
                        icon = Icons.Default.Search,
                        title = noResultsTitle,
                        subtitle = noResultsSubtitle,
                        themeColors = themeColors
                    )
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = filteredCategories,
                            key = { it.first.categoryId }
                        ) { (category, channelCount) ->
                            CategoryCard(
                                categoryName = category.categoryName,
                                channelCount = channelCount,
                                themeColors = themeColors,
                                onClick = { onCategorySelected(category.categoryId.toString()) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpgEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    themeColors: EpgThemeColors
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = themeColors.textSecondaryColor.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = title,
                color = themeColors.textColor,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                color = themeColors.textSecondaryColor,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CategoryCard(
    categoryName: String,
    channelCount: Int,
    themeColors: EpgThemeColors,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.cardColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                themeColors.primaryColor.copy(alpha = 0.1f),
                                Color.Transparent
                            ),
                            startX = 0f,
                            endX = 500f
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    themeColors.primaryColor.copy(alpha = 0.2f),
                                    themeColors.primaryColor.copy(alpha = 0.1f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = themeColors.primaryColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = themeColors.textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(themeColors.primaryColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$channelCount ${if (channelCount != 1) "channels" else "channel"}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = themeColors.textSecondaryColor,
                                fontSize = 14.sp
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(themeColors.primaryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View guide",
                        tint = themeColors.primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Every whitespace-separated term in [query] must appear somewhere in [text], in any order.
 * Ports the original's `Utils.matchesFlexibleSearch`, which has no equivalent in this repo yet.
 */
internal fun matchesFlexibleSearch(text: String?, query: String): Boolean {
    if (query.isBlank()) return true
    val haystack = text?.lowercase() ?: return false
    return query.lowercase()
        .split(' ')
        .filter { it.isNotBlank() }
        .all { term -> haystack.contains(term) }
}
