package pt.hitv.core.ui.categories

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Modern Mode Selector - Toggle between Categories and Custom Groups (Horizontal for Portrait/Mobile)
 */
@Composable
internal fun ModernModeSelector(
    selectedMode: ManagementScreenMode,
    onModeSelected: (ManagementScreenMode) -> Unit,
    primaryColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    categoriesLabel: String = "Categories",
    customGroupsLabel: String = "Custom Groups"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(
                color = textColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ModeButton(
            text = categoriesLabel,
            icon = Icons.Rounded.Category,
            isSelected = selectedMode == ManagementScreenMode.CATEGORIES,
            onClick = { onModeSelected(ManagementScreenMode.CATEGORIES) },
            primaryColor = primaryColor,
            textColor = textColor,
            modifier = Modifier.weight(1f)
        )

        ModeButton(
            text = customGroupsLabel,
            icon = Icons.Rounded.Folder,
            isSelected = selectedMode == ManagementScreenMode.CUSTOM_GROUPS,
            onClick = { onModeSelected(ManagementScreenMode.CUSTOM_GROUPS) },
            primaryColor = primaryColor,
            textColor = textColor,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Modern Mode Selector - Vertical for Landscape/TV layouts.
 */
@Composable
internal fun ModernModeSelectorVertical(
    selectedMode: ManagementScreenMode,
    onModeSelected: (ManagementScreenMode) -> Unit,
    primaryColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    categoriesLabel: String = "Categories",
    customGroupsLabel: String = "Custom Groups"
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ModeButtonVertical(
            text = categoriesLabel,
            icon = Icons.Rounded.Category,
            isSelected = selectedMode == ManagementScreenMode.CATEGORIES,
            onClick = { onModeSelected(ManagementScreenMode.CATEGORIES) },
            primaryColor = primaryColor,
            textColor = textColor
        )

        ModeButtonVertical(
            text = customGroupsLabel,
            icon = Icons.Rounded.Folder,
            isSelected = selectedMode == ManagementScreenMode.CUSTOM_GROUPS,
            onClick = { onModeSelected(ManagementScreenMode.CUSTOM_GROUPS) },
            primaryColor = primaryColor,
            textColor = textColor
        )
    }
}

@Composable
private fun ModeButton(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> primaryColor
            else -> Color.Transparent
        },
        label = "modeButtonBackground"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isSelected -> textColor
            else -> textColor.copy(alpha = 0.6f)
        },
        label = "modeButtonContent"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(backgroundColor, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
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
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ModeButtonVertical(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> primaryColor
            else -> Color.Transparent
        },
        label = "modeButtonBackground"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isSelected -> textColor
            else -> textColor.copy(alpha = 0.6f)
        },
        label = "modeButtonContent"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(backgroundColor, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}
