package pt.hitv.core.designsystem.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * Data class for category items used in TV layouts
 */
data class TvCategoryItemData(
    val id: String,
    val name: String,
    var count: String = "..."
)

/**
 * Shared TV category card component with focus management
 */
@Composable
fun TvCategoryCard(
    category: TvCategoryItemData,
    isSelected: Boolean,
    isFocused: Boolean,
    onClick: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val themeColors = getThemeColors()

    LaunchedEffect(isFocused) {
        if (isFocused) {
            focusRequester.requestFocus()
        }
    }

    val backgroundColor = when {
        isFocused && isSelected -> themeColors.primaryColor.copy(alpha = 0.4f)
        isFocused -> themeColors.primaryColor.copy(alpha = 0.2f)
        isSelected -> themeColors.primaryColor.copy(alpha = 0.15f)
        else -> ComposeColor.Transparent
    }

    val borderColor = when {
        isFocused -> themeColors.primaryColor
        isSelected -> themeColors.primaryColor.copy(alpha = 0.7f)
        else -> themeColors.textColor.copy(alpha = 0.1f)
    }

    val borderWidth = when {
        isFocused -> 3.dp
        isSelected -> 2.dp
        else -> 1.dp
    }

    val textColor = when {
        isFocused || isSelected -> themeColors.textColor
        else -> themeColors.textColor.copy(alpha = 0.8f)
    }

    val dotColor = when {
        isFocused || isSelected -> themeColors.primaryColor
        else -> themeColors.textColor.copy(alpha = 0.3f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { onFocusChanged(it.isFocused) }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(borderWidth, borderColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isFocused || isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (category.count.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isFocused -> themeColors.primaryColor.copy(alpha = 0.3f)
                            isSelected -> themeColors.primaryColor.copy(alpha = 0.2f)
                            else -> themeColors.textColor.copy(alpha = 0.1f)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = category.count,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = textColor.copy(alpha = 0.9f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Shared TV search button component
 */
@Composable
fun TvSearchButton(
    onClick: () -> Unit,
    contentDescription: String = "Search"
) {
    val themeColors = getThemeColors()
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    Card(
        modifier = Modifier
            .size(48.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER -> { onClick(); true }
                        else -> false
                    }
                } else false
            }
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) themeColors.primaryColor.copy(alpha = 0.2f)
            else ComposeColor.Transparent
        ),
        border = BorderStroke(
            if (isFocused) 3.dp else 1.dp,
            if (isFocused) themeColors.primaryColor else themeColors.textColor.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = contentDescription,
                tint = if (isFocused) themeColors.textColor else themeColors.textColor.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Shared TV manage categories button component
 */
@Composable
fun TvManageCategoriesButton(
    onClick: () -> Unit,
    label: String = "Manage Categories",
    modifier: Modifier = Modifier
) {
    val themeColors = getThemeColors()
    var isFocused by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER -> { onClick(); true }
                        else -> false
                    }
                } else false
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) themeColors.primaryColor.copy(alpha = 0.2f)
            else ComposeColor.Transparent
        ),
        border = BorderStroke(
            if (isFocused) 3.dp else 1.dp,
            if (isFocused) themeColors.primaryColor else themeColors.textColor.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = label,
                tint = if (isFocused) themeColors.primaryColor else themeColors.textColor.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isFocused) themeColors.textColor else themeColors.textColor.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Shared TV manage categories icon button component
 */
@Composable
fun TvManageCategoriesIconButton(
    onClick: () -> Unit,
    contentDescription: String = "Manage Categories"
) {
    val themeColors = getThemeColors()
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    Card(
        modifier = Modifier
            .size(48.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER -> { onClick(); true }
                        else -> false
                    }
                } else false
            }
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) themeColors.primaryColor.copy(alpha = 0.2f)
            else ComposeColor.Transparent
        ),
        border = BorderStroke(
            if (isFocused) 3.dp else 1.dp,
            if (isFocused) themeColors.primaryColor else themeColors.textColor.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = contentDescription,
                tint = if (isFocused) themeColors.textColor else themeColors.textColor.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Shared TV layout structure with sidebar and main content
 */
@Composable
fun TvSidebarLayout(
    sidebarContent: @Composable () -> Unit,
    mainContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = getThemeColors()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        themeColors.backgroundPrimary,
                        themeColors.backgroundSecondary.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                themeColors.backgroundPrimary.copy(alpha = 0.8f),
                                themeColors.backgroundPrimary.copy(alpha = 0.4f),
                                ComposeColor.Transparent
                            )
                        )
                    )
            ) {
                sidebarContent()
            }
            mainContent()
        }
    }
}

/**
 * Shared TV sidebar content with categories
 */
@Composable
fun TvSidebarContent(
    title: String,
    subtitle: String = "Choose your category",
    categoryItems: List<TvCategoryItemData>,
    selectedCategory: String,
    focusedCategoryIndex: Int,
    onCategorySelected: (TvCategoryItemData) -> Unit,
    onFocusChanged: (Int) -> Unit,
    onSearchClicked: () -> Unit,
    onManageCategoriesClicked: () -> Unit,
    searchContentDescription: String = "Search"
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        val themeColors = getThemeColors()

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.textColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = themeColors.textColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            TvSearchButton(onClick = onSearchClicked, contentDescription = searchContentDescription)
        }

        Text(
            text = "Categories",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = themeColors.textColor.copy(alpha = 0.9f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "manage_categories_button") {
                TvManageCategoriesButton(onClick = onManageCategoriesClicked)
            }

            itemsIndexed(
                items = categoryItems,
                key = { index, item -> "${item.id}_$index" }
            ) { index, category ->
                TvCategoryCard(
                    category = category,
                    isSelected = category.name == selectedCategory,
                    isFocused = index == focusedCategoryIndex,
                    onClick = {
                        onFocusChanged(index)
                        onCategorySelected(category)
                    },
                    onFocusChanged = { hasFocus ->
                        if (hasFocus) onFocusChanged(index)
                    }
                )
            }
        }
    }
}

/**
 * Shared TV content header with category name and count indicator
 */
@Composable
fun TvContentHeader(
    selectedCategory: String,
    itemCount: Int,
    contentType: String
) {
    val themeColors = getThemeColors()

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = selectedCategory,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = themeColors.textColor
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .height(24.dp)
                .width(3.dp)
                .background(themeColors.primaryColor, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.weight(1f))

        if (itemCount > 0) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = themeColors.primaryColor.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "$itemCount $contentType",
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.primaryColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * TV Search Bar Component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvSearchBar(
    searchQuery: String,
    isSearchActive: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onSearchActiveChanged: (Boolean) -> Unit,
    placeholder: String = "Search...",
    modifier: Modifier = Modifier
) {
    val themeColors = getThemeColors()
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) focusRequester.requestFocus()
    }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isSearchActive) {
            var buttonFocused by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .height(48.dp)
                    .onFocusChanged { buttonFocused = it.isFocused }
                    .focusable()
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.nativeKeyEvent.keyCode) {
                                android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                android.view.KeyEvent.KEYCODE_ENTER -> { onSearchActiveChanged(true); true }
                                else -> false
                            }
                        } else false
                    }
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onSearchActiveChanged(true) },
                colors = CardDefaults.cardColors(
                    containerColor = if (buttonFocused) themeColors.primaryColor.copy(alpha = 0.2f)
                    else themeColors.backgroundSecondary.copy(alpha = 0.6f)
                ),
                border = BorderStroke(
                    if (buttonFocused) 3.dp else 1.dp,
                    if (buttonFocused) themeColors.primaryColor else themeColors.textColor.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Search, "Search",
                        tint = if (buttonFocused) themeColors.textColor else themeColors.textColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp))
                    Text(placeholder, style = MaterialTheme.typography.bodyMedium,
                        color = if (buttonFocused) themeColors.textColor else themeColors.textColor.copy(alpha = 0.7f))
                }
            }
        } else {
            Card(
                modifier = Modifier.weight(1f).height(48.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFocused) themeColors.backgroundSecondary.copy(alpha = 0.9f)
                    else themeColors.backgroundSecondary.copy(alpha = 0.6f)
                ),
                border = BorderStroke(
                    if (isFocused) 3.dp else 1.dp,
                    if (isFocused) themeColors.primaryColor else themeColors.textColor.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, "Search", tint = themeColors.primaryColor, modifier = Modifier.size(20.dp))

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        modifier = Modifier.weight(1f).padding(start = 12.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { isFocused = it.isFocused },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = themeColors.textColor),
                        singleLine = true,
                        cursorBrush = SolidColor(themeColors.primaryColor),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(placeholder, style = MaterialTheme.typography.bodyMedium,
                                    color = themeColors.textColor.copy(alpha = 0.5f))
                            }
                            innerTextField()
                        }
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Clear, "Clear", tint = themeColors.textColor.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            var closeFocused by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier
                    .size(48.dp)
                    .onFocusChanged { closeFocused = it.isFocused }
                    .focusable()
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.nativeKeyEvent.keyCode) {
                                android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                android.view.KeyEvent.KEYCODE_ENTER -> { onSearchActiveChanged(false); onSearchQueryChanged(""); true }
                                else -> false
                            }
                        } else false
                    }
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onSearchActiveChanged(false); onSearchQueryChanged("") },
                colors = CardDefaults.cardColors(
                    containerColor = if (closeFocused) themeColors.primaryColor.copy(alpha = 0.2f) else ComposeColor.Transparent
                ),
                border = BorderStroke(
                    if (closeFocused) 3.dp else 1.dp,
                    if (closeFocused) themeColors.primaryColor else themeColors.textColor.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Close, "Close search",
                        tint = if (closeFocused) themeColors.textColor else themeColors.textColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
