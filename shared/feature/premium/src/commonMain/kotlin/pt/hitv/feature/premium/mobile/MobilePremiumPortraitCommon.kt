package pt.hitv.feature.premium.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.feature.premium.SubscriptionTier

/**
 * Common mobile portrait premium layout, usable from all platforms.
 */
@Composable
fun MobilePremiumPortraitCommon(
    onNavigateBack: () -> Unit,
    onPurchaseClick: (productId: String) -> Unit = {},
    isRootDestination: Boolean = false,
    scrollToTopSignal: Int = 0,
    subscriptionTiers: List<SubscriptionTier> = emptyList(),
    hasLifetimePremium: Boolean = false,
    hasAnnualPremium: Boolean = false,
    headerTitle: String = "Upgrade to Premium",
    headerSubtitle: String = "Unlock all features",
    backDescription: String = "Back"
) {
    val themeColors = getThemeColors()

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(themeColors.backgroundPrimary, themeColors.backgroundSecondary, themeColors.backgroundPrimary.copy(alpha = 0.9f))))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (!isRootDestination) {
                    Box(
                        modifier = Modifier.size(48.dp).background(color = Color.White.copy(alpha = 0.08f), shape = CircleShape).clip(CircleShape).clickable(onClick = onNavigateBack),
                        contentAlignment = Alignment.Center
                    ) { Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = backDescription, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(24.dp)) }
                    Spacer(modifier = Modifier.width(20.dp))
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Rounded.Star, contentDescription = null, tint = themeColors.primaryColor, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = headerTitle, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = headerSubtitle, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (subscriptionTiers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Premium features loading...", color = Color.White.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(subscriptionTiers) { tier ->
                        MobileSubscriptionTierCardCommon(tier = tier, primaryColor = themeColors.primaryColor, textColor = Color.White, textSecondaryColor = Color.White.copy(alpha = 0.7f), isOwned = false, onPurchaseClick = { onPurchaseClick(tier.productId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileSubscriptionTierCardCommon(
    tier: SubscriptionTier, primaryColor: Color, textColor: Color, textSecondaryColor: Color, isOwned: Boolean, onPurchaseClick: () -> Unit, modifier: Modifier = Modifier
) {
    val backgroundColor = if (tier.isHighlighted) primaryColor.copy(alpha = 0.15f) else textColor.copy(alpha = 0.05f)
    val borderColor = if (tier.isHighlighted) primaryColor else textColor.copy(alpha = 0.15f)
    val borderWidth = if (tier.isHighlighted) 2.dp else 1.dp

    Box(modifier = modifier.fillMaxWidth().background(backgroundColor, RoundedCornerShape(16.dp)).border(borderWidth, borderColor, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp))) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = tier.title, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = tier.price, color = primaryColor, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    if (tier.period.isNotEmpty()) Text(text = tier.period, color = textSecondaryColor, fontSize = 13.sp)
                }
                tier.badge?.let { badgeText ->
                    Box(modifier = Modifier.background(tier.badgeColor ?: primaryColor, RoundedCornerShape(6.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(text = badgeText, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.8.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            tier.features.forEach { feature ->
                Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null, tint = if (tier.isHighlighted) primaryColor else Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = feature, color = textColor, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(50.dp)
                    .background(if (isOwned) Color.Gray else if (tier.isHighlighted) primaryColor else primaryColor.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)).then(if (!isOwned) Modifier.clickable(onClick = onPurchaseClick) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    if (isOwned) {
                        Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Already Premium", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text(text = if (tier.id == "lifetime") "Get Lifetime" else "Subscribe Now", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(imageVector = Icons.Rounded.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
