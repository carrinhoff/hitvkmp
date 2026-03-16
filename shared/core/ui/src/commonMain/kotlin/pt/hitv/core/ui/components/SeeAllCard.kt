package pt.hitv.core.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * A placeholder card shown at the end of horizontal lists with "See All" text
 * and the total count of items in the category.
 *
 * @param totalCount Total number of items in the category
 * @param onClicked Callback when the card is clicked
 * @param modifier Optional modifier
 * @param seeAllLabel Label text for "See All"
 * @param itemsCountLabel Format string for items count (receives totalCount)
 */
@Composable
fun SeeAllCard(
    totalCount: Int,
    onClicked: () -> Unit,
    modifier: Modifier = Modifier,
    seeAllLabel: String = "See All",
    itemsCountLabel: String = "$totalCount items"
) {
    val themeColors = getThemeColors()
    val cardShape = RoundedCornerShape(12.dp)

    Card(
        modifier = modifier
            .width(280.dp)
            .height(160.dp)
            .padding(4.dp)
            .clickable(onClick = onClicked),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = themeColors.backgroundSecondary
        ),
        shape = cardShape
    ) {
        val backgroundBrush = remember(themeColors.primaryColor) {
            Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    themeColors.primaryColor.copy(alpha = 0.2f)
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = backgroundBrush),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = seeAllLabel,
                    tint = themeColors.primaryColor,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = seeAllLabel,
                    color = themeColors.textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = itemsCountLabel,
                    color = themeColors.textColor.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
