package pt.hitv.core.designsystem.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * Shared mobile top app bar with category selection and search
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedMobileTopAppBar(
    selectedCategoryName: String,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onCategoryClick: () -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    onBackPressed: () -> Unit,
    showBackButton: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        navigationIcon = {
            if (showBackButton) {
                IconButton(
                    onClick = onBackPressed,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = getThemeColors().textColor
                    )
                }
            }
        },
        title = {
            Box(modifier = Modifier.padding(start = 4.dp)) {
                if (isSearchActive) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.7f
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )

                                val focusRequester = remember { FocusRequester() }

                                var textFieldValue by remember {
                                    mutableStateOf(
                                        TextFieldValue(
                                            text = searchQuery,
                                            selection = TextRange(searchQuery.length)
                                        )
                                    )
                                }

                                LaunchedEffect(searchQuery) {
                                    if (textFieldValue.text != searchQuery) {
                                        textFieldValue = TextFieldValue(
                                            text = searchQuery,
                                            selection = TextRange(searchQuery.length)
                                        )
                                    }
                                }

                                LaunchedEffect(isSearchActive) {
                                    if (isSearchActive) {
                                        delay(100)
                                        focusRequester.requestFocus()
                                    }
                                }

                                BasicTextField(
                                    value = textFieldValue,
                                    onValueChange = { newValue ->
                                        textFieldValue = newValue
                                        onSearchQueryChanged(newValue.text)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 8.dp)
                                        .focusRequester(focusRequester),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = getThemeColors().textColor
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    decorationBox = { innerTextField ->
                                        if (textFieldValue.text.isEmpty()) {
                                            Text(
                                                "Search...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = getThemeColors().textColor.copy(alpha = 0.7f)
                                            )
                                        }
                                        innerTextField()
                                    }
                                )

                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onSearchQueryChanged("") },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = getThemeColors().textColor.copy(alpha = 0.6f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        onClick = onCategoryClick,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Category,
                                contentDescription = "Category",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = selectedCategoryName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = getThemeColors().textColor,
                                modifier = Modifier.weight(1f)
                            )

                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Select category",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        actions = {
            if (!isSearchActive) {
                actions()

                IconButton(onClick = { onSearchToggle(true) }) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = getThemeColors().textColor
                    )
                }
            } else {
                IconButton(onClick = {
                    onSearchToggle(false)
                    onSearchQueryChanged("")
                }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close search",
                        tint = getThemeColors().textColor
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = getThemeColors().backgroundPrimary,
            titleContentColor = getThemeColors().textColor,
            navigationIconContentColor = getThemeColors().textColor,
            actionIconContentColor = getThemeColors().textColor
        )
    )
}

/**
 * Shared mobile bottom sheet wrapper
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedMobileBottomSheet(
    showSheet: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = getThemeColors().backgroundSecondary,
            contentColor = getThemeColors().textColor,
            dragHandle = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                getThemeColors().textColor.copy(alpha = 0.4f),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        ) {
            content()
        }
    }
}

/**
 * Shared error message component
 */
@Composable
fun SharedErrorMessage(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

/**
 * Shared empty message component with beautiful design
 */
@Composable
fun SharedEmptyMessage(
    message: String,
    subtitle: String? = null
) {
    val themeColors = getThemeColors()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = themeColors.textColor.copy(alpha = 0.3f)
        )

        Text(
            text = message,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = themeColors.textColor,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = themeColors.textColor.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
