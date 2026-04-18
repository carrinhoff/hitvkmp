package pt.hitv.feature.settings.options.options.parental

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * Category lock screen — shows the full list of categories (currently channel
 * categories as exposed by [ParentalControlViewModel.uiState.categories]) with
 * a checkbox per row. Toggling a row calls
 * [ParentalControlViewModel.toggleCategoryProtection].
 *
 * Movies/series categories require additional repository flows in the existing
 * view model; those are out of scope for Team γ (the VM already exists and we
 * respect its API).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryLockScreen(
    viewModel: ParentalControlViewModel,
    onNavigateBack: () -> Unit,
    titleText: String = "Locked Categories",
    emptyText: String = "No categories available yet.",
    modifier: Modifier = Modifier
) {
    val themeColors = getThemeColors()
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = themeColors.backgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text(titleText, color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColors.backgroundPrimary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (state.categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(emptyText, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.categories, key = { it.category.categoryId }) { row ->
                    CategoryLockRow(
                        name = row.category.categoryName,
                        isProtected = row.isProtected,
                        onToggle = { locked ->
                            viewModel.toggleCategoryProtection(
                                categoryId = row.category.categoryId,
                                categoryName = row.category.categoryName,
                                isProtected = locked
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryLockRow(
    name: String,
    isProtected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val themeColors = getThemeColors()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isProtected) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.backgroundSecondary)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(themeColors.primaryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = themeColors.primaryColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Checkbox(
                checked = isProtected,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = themeColors.primaryColor,
                    uncheckedColor = Color.White.copy(alpha = 0.5f),
                    checkmarkColor = Color.White
                )
            )
        }
    }
}
