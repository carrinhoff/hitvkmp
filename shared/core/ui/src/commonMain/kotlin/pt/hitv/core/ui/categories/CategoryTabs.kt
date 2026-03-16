package pt.hitv.core.ui.categories

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.hitv.core.model.CategoryPreference
import pt.hitv.core.model.ContentType

/**
 * Horizontal tab selector for content type (TV, VOD, Series).
 */
@Composable
internal fun ModernTabSelector(
    selectedTab: ContentType,
    onTabSelected: (ContentType) -> Unit,
    primaryColor: Color,
    textColor: Color,
    categories: List<CategoryPreference>,
    modifier: Modifier = Modifier,
    tvLabel: String = "Live TV",
    vodLabel: String = "Movies",
    seriesLabel: String = "Series"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ModernTab(
            text = tvLabel,
            count = categories.count { it.contentType == ContentType.CHANNELS },
            isSelected = selectedTab == ContentType.CHANNELS,
            onClick = { onTabSelected(ContentType.CHANNELS) },
            primaryColor = primaryColor,
            textColor = textColor,
            modifier = Modifier.weight(1f)
        )
        ModernTab(
            text = vodLabel,
            count = categories.count { it.contentType == ContentType.MOVIES },
            isSelected = selectedTab == ContentType.MOVIES,
            onClick = { onTabSelected(ContentType.MOVIES) },
            primaryColor = primaryColor,
            textColor = textColor,
            modifier = Modifier.weight(1f)
        )
        ModernTab(
            text = seriesLabel,
            count = categories.count { it.contentType == ContentType.SERIES },
            isSelected = selectedTab == ContentType.SERIES,
            onClick = { onTabSelected(ContentType.SERIES) },
            primaryColor = primaryColor,
            textColor = textColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ModernTab(
    text: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> primaryColor
            else -> textColor.copy(alpha = 0.05f)
        },
        label = "tabBackground"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            if (count > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "($count)",
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

/**
 * Vertical tab selector for landscape/TV layouts.
 */
@Composable
internal fun ModernTabSelectorVertical(
    selectedTab: ContentType,
    onTabSelected: (ContentType) -> Unit,
    primaryColor: Color,
    textColor: Color,
    categories: List<CategoryPreference>,
    modifier: Modifier = Modifier,
    tvLabel: String = "Live TV",
    vodLabel: String = "Movies",
    seriesLabel: String = "Series"
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ModernTabVertical(
            text = tvLabel,
            count = categories.count { it.contentType == ContentType.CHANNELS },
            isSelected = selectedTab == ContentType.CHANNELS,
            onClick = { onTabSelected(ContentType.CHANNELS) },
            primaryColor = primaryColor,
            textColor = textColor
        )
        ModernTabVertical(
            text = vodLabel,
            count = categories.count { it.contentType == ContentType.MOVIES },
            isSelected = selectedTab == ContentType.MOVIES,
            onClick = { onTabSelected(ContentType.MOVIES) },
            primaryColor = primaryColor,
            textColor = textColor
        )
        ModernTabVertical(
            text = seriesLabel,
            count = categories.count { it.contentType == ContentType.SERIES },
            isSelected = selectedTab == ContentType.SERIES,
            onClick = { onTabSelected(ContentType.SERIES) },
            primaryColor = primaryColor,
            textColor = textColor
        )
    }
}

@Composable
private fun ModernTabVertical(
    text: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> primaryColor
            else -> textColor.copy(alpha = 0.05f)
        },
        label = "tabBackground"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (count > 0) {
                Text(
                    text = "($count)",
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
