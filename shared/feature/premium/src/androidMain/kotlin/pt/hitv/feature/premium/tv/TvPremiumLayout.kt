package pt.hitv.feature.premium.tv

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * TV Premium Layout - Android TV specific with D-pad navigation.
 * Full implementation with BillingManager integration is wired in the Android app module.
 */
@Composable
fun TvPremiumLayout(
    activity: Activity? = null,
    onNavigateBack: () -> Unit,
    isRootDestination: Boolean = false
) {
    val themeColors = getThemeColors()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(colors = listOf(themeColors.backgroundPrimary, themeColors.backgroundSecondary, themeColors.backgroundPrimary.copy(alpha = 0.9f)))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 48.dp, vertical = 40.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Star, null, tint = themeColors.primaryColor, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = "Upgrade to Premium", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Unlock all features and themes", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text("TV Premium layout - BillingManager integration in app module", color = Color.White.copy(alpha = 0.5f))
        }
    }
}
