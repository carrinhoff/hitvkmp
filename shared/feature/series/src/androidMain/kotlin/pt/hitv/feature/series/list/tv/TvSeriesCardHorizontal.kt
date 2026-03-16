package pt.hitv.feature.series.list.tv

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import pt.hitv.core.model.TvShow
import pt.hitv.feature.series.list.SeriesViewModel
import pt.hitv.core.model.enums.ClickType
import pt.hitv.core.designsystem.theme.getThemeColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvSeriesCardHorizontal(
    series: TvShow,
    onSeriesClicked: (ClickType) -> Unit,
    viewModel: SeriesViewModel,
    showTopBadge: Boolean = false,
    modifier: Modifier = Modifier,
    shouldRequestFocus: Boolean = false,
    onFocused: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val animatedScale by animateFloatAsState(targetValue = if (isFocused) 1.08f else 1.0f, animationSpec = tween(durationMillis = 250), label = "scale")
    val animatedElevation by animateDpAsState(targetValue = if (isFocused) 16.dp else 4.dp, animationSpec = tween(durationMillis = 250), label = "elevation")
    val borderWidth by animateDpAsState(targetValue = if (isFocused) 3.dp else 0.dp, animationSpec = tween(durationMillis = 250), label = "border")
    val isFavorite = series.isFavorite
    val themeColors = getThemeColors()

    Card(
        modifier = modifier.width(280.dp).height(160.dp)
            .onFocusChanged { isFocused = it.isFocused; if (it.isFocused) onFocused?.invoke() }
            .focusRequester(focusRequester).focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER -> { onSeriesClicked(ClickType.CLICK); true }
                        android.view.KeyEvent.KEYCODE_MENU, android.view.KeyEvent.KEYCODE_STAR -> { onSeriesClicked(ClickType.LONG_CLICK); true }
                        else -> false
                    }
                } else false
            }
            .combinedClickable(onClick = { onSeriesClicked(ClickType.CLICK) }, onLongClick = { onSeriesClicked(ClickType.LONG_CLICK) })
            .graphicsLayer { scaleX = animatedScale; scaleY = animatedScale; clip = false }.zIndex(if (isFocused) 1f else 0f),
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(12.dp),
        border = if (isFocused) BorderStroke(borderWidth, themeColors.primaryColor) else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(model = series.cover, contentDescription = series.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.7f)))))
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                if (showTopBadge) { Box(modifier = Modifier.align(Alignment.TopStart).background(color = themeColors.primaryColor.copy(alpha = 0.9f), shape = RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) { Text(text = "TOP", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) } }
                if (isFavorite) { Box(modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(color = Color(0xFFFFA000), shape = RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Default.Star, contentDescription = "Favorite", tint = Color.White, modifier = Modifier.size(14.dp)) } }
                Column(modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(0.85f)) {
                    Text(text = series.name ?: "Unknown Series", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                    val rating = series.rating?.toDoubleOrNull() ?: 0.0
                    if (rating > 0) { Spacer(modifier = Modifier.height(8.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Icon(imageVector = Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFFFA000), modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(4.dp)); Text(text = String.format("%.1f", rating), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium) } }
                }
            }
        }
    }
}
