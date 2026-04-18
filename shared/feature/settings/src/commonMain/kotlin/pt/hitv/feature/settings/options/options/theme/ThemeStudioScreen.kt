package pt.hitv.feature.settings.options.options.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.hitv.core.designsystem.theme.ThemeManager.AppTheme

/**
 * Theme Studio — faithful port of the original `ThemeSettingsScreen`.
 * Ships ungated (no IAP gate, no lock icons).
 *
 * Behavior: tapping a card PREVIEWS the theme live (background + cards recolor).
 * The Apply button at the bottom commits the preview to persistence.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeStudioScreen(
    viewModel: ThemeSettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val currentTheme by viewModel.currentTheme.collectAsState()
    var selectedTheme by remember { mutableStateOf(currentTheme) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Theme Studio",
                        color = selectedTheme.textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = selectedTheme.textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = selectedTheme.backgroundPrimary
                )
            )
        },
        containerColor = selectedTheme.backgroundPrimary
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            selectedTheme.backgroundSecondary,
                            selectedTheme.backgroundPrimary
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .padding(bottom = 80.dp)
            ) {
                // Theme grid (2 cols × 3 rows = 6 themes). Fixed height.
                val gridHeight = (3 * 180 + 2 * 12 + 20).dp
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridHeight),
                    userScrollEnabled = false
                ) {
                    items(AppTheme.values().toList(), key = { it.themeName }) { theme ->
                        EpicThemeCard(
                            theme = theme,
                            isSelected = theme == selectedTheme,
                            onSelect = { selectedTheme = theme }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Apply / Reset action row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { selectedTheme = currentTheme },
                        modifier = Modifier.weight(1f),
                        enabled = selectedTheme != currentTheme,
                        border = BorderStroke(1.dp, selectedTheme.textColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            "Reset",
                            color = selectedTheme.textColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.selectTheme(selectedTheme)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedTheme != currentTheme,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = selectedTheme.primaryColor,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Apply Theme", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EpicThemeCard(
    theme: AppTheme,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "themeCardScale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 12.dp else 4.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "themeCardElevation"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) theme.primaryColor else Color.Transparent,
        label = "themeCardBorder"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = if (isSelected) BorderStroke(3.dp, borderColor) else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(theme.backgroundPrimary, theme.backgroundSecondary),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Name + status icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = theme.displayName,
                        color = theme.textColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = theme.primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = null,
                            tint = theme.primaryColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Color palette preview
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorSwatch(theme.primaryColor, borderAlpha = 0.3f, bgReference = theme.textColor)
                    ColorSwatch(theme.backgroundSecondary, borderAlpha = 0.3f, bgReference = Color.White)
                    ColorSwatch(theme.cardColor, borderAlpha = 0.3f, bgReference = Color.White)
                }

                Spacer(modifier = Modifier.weight(1f))

                // Description line
                Text(
                    text = themeDescription(theme),
                    color = theme.textColor.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, borderAlpha: Float, bgReference: Color) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .border(1.dp, bgReference.copy(alpha = borderAlpha), RoundedCornerShape(8.dp))
    )
}

private fun themeDescription(theme: AppTheme): String = when (theme.themeName) {
    "default" -> "The iconic Prime Video look."
    "crimson_dark", "crimson" -> "Cinematic red on pure black."
    "classic_orange", "amazon" -> "The classic Amazon palette."
    "gaming_green", "gaming" -> "Neon accents for the gamer."
    "cinema_gold", "cinema" -> "Gold-on-black cinema vibes."
    "midnight_oled", "midnight" -> "True-black OLED mode."
    else -> "A fresh look for your library."
}
