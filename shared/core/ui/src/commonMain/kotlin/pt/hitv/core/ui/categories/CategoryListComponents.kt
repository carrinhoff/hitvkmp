package pt.hitv.core.ui.categories

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.hitv.core.model.CategoryPreference
import pt.hitv.core.model.ContentType

/**
 * Empty state display when no categories match the filter.
 */
@Composable
internal fun EmptyState(
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    searchQuery: String,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier,
    emptyLabel: String = "No categories found",
    clearSearchLabel: String = "Clear search"
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.FolderOff,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.3f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (searchQuery.isEmpty()) emptyLabel
                else "$emptyLabel: \"$searchQuery\"",
                color = textSecondaryColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            if (searchQuery.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onClearSearch) {
                    Text(
                        text = clearSearchLabel,
                        color = primaryColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * List of category cards with pin, hide, and default actions.
 */
@Composable
internal fun ModernCategoriesList(
    categories: List<CategoryPreference>,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    onTogglePin: (String, String, ContentType, Boolean) -> Unit,
    onToggleHide: (String, String, ContentType, Boolean) -> Unit,
    onSetAsDefault: (String, String, ContentType) -> Unit,
    modifier: Modifier = Modifier,
    unpinLabel: String = "Unpin category",
    pinLabel: String = "Pin category",
    showLabel: String = "Show category",
    hideLabel: String = "Hide category",
    removeDefaultLabel: String = "Remove default",
    setDefaultLabel: String = "Set as default"
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(categories) { _, category ->
            ModernCategoryCard(
                category = category,
                primaryColor = primaryColor,
                textColor = textColor,
                textSecondaryColor = textSecondaryColor,
                onTogglePin = {
                    onTogglePin(category.categoryId, category.categoryName, category.contentType, category.isPinned)
                },
                onToggleHide = {
                    onToggleHide(category.categoryId, category.categoryName, category.contentType, category.isHidden)
                },
                onSetAsDefault = {
                    onSetAsDefault(category.categoryId, category.categoryName, category.contentType)
                },
                unpinLabel = unpinLabel,
                pinLabel = pinLabel,
                showLabel = showLabel,
                hideLabel = hideLabel,
                removeDefaultLabel = removeDefaultLabel,
                setDefaultLabel = setDefaultLabel
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun ModernCategoryCard(
    category: CategoryPreference,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    onTogglePin: () -> Unit,
    onToggleHide: () -> Unit,
    onSetAsDefault: () -> Unit,
    unpinLabel: String,
    pinLabel: String,
    showLabel: String,
    hideLabel: String,
    removeDefaultLabel: String,
    setDefaultLabel: String,
    modifier: Modifier = Modifier
) {
    val cardAlpha = if (category.isHidden) 0.4f else 1f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .alpha(cardAlpha)
            .background(
                color = textColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (category.isPinned) {
            Icon(
                imageVector = Icons.Rounded.PushPin,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Text(
            text = category.categoryName,
            color = if (category.isHidden) textColor.copy(alpha = 0.5f) else textColor,
            fontSize = 16.sp,
            fontWeight = if (category.isPinned) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryIconButton(
                icon = Icons.Rounded.PushPin,
                isActive = category.isPinned,
                onClick = onTogglePin,
                primaryColor = primaryColor,
                textColor = textColor,
                contentDescription = if (category.isPinned) unpinLabel else pinLabel
            )

            CategoryIconButton(
                icon = if (category.isHidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                isActive = category.isHidden,
                onClick = onToggleHide,
                primaryColor = primaryColor,
                textColor = textColor,
                contentDescription = if (category.isHidden) showLabel else hideLabel
            )

            CategoryIconButton(
                icon = if (category.isDefault) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                isActive = category.isDefault,
                onClick = onSetAsDefault,
                primaryColor = primaryColor,
                textColor = textColor,
                contentDescription = if (category.isDefault) removeDefaultLabel else setDefaultLabel,
                isEnabled = !category.isHidden
            )
        }
    }
}

@Composable
private fun CategoryIconButton(
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    primaryColor: Color,
    textColor: Color,
    contentDescription: String,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !isEnabled -> textColor.copy(alpha = 0.03f)
            isActive -> primaryColor.copy(alpha = 0.2f)
            else -> textColor.copy(alpha = 0.08f)
        },
        label = "iconBackground"
    )

    val iconTint by animateColorAsState(
        targetValue = when {
            !isEnabled -> textColor.copy(alpha = 0.2f)
            isActive -> primaryColor
            else -> textColor.copy(alpha = 0.6f)
        },
        label = "iconTint"
    )

    Box(
        modifier = modifier
            .size(40.dp)
            .background(backgroundColor, CircleShape)
            .clip(CircleShape)
            .then(
                if (isEnabled) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Search bar for filtering categories.
 */
@Composable
internal fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    modifier: Modifier = Modifier,
    placeholder: String = "Search categories...",
    clearLabel: String = "Clear"
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(textColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = LocalTextStyle.current.copy(
                    color = textColor,
                    fontSize = 16.sp
                ),
                singleLine = true,
                cursorBrush = SolidColor(primaryColor),
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        Text(
                            placeholder,
                            color = textColor.copy(alpha = 0.5f),
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                )
            )
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = {
                        onSearchQueryChange("")
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = clearLabel,
                        tint = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Bulk actions bar for show all / hide all.
 */
@Composable
internal fun ModernBulkActionsBar(
    selectedTab: ContentType,
    onShowAll: () -> Unit,
    onHideAll: () -> Unit,
    primaryColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    showAllLabel: String = "Show All",
    hideAllLabel: String = "Hide All"
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = textColor.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BulkActionButton(
                text = showAllLabel,
                icon = Icons.Rounded.Visibility,
                onClick = onShowAll,
                primaryColor = primaryColor,
                textColor = textColor,
                modifier = Modifier.weight(1f)
            )

            BulkActionButton(
                text = hideAllLabel,
                icon = Icons.Rounded.VisibilityOff,
                onClick = onHideAll,
                primaryColor = primaryColor,
                textColor = textColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BulkActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    primaryColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .background(textColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
