package pt.hitv.feature.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock

data class SubscriptionTier(
    val id: String,
    val title: String,
    val price: String,
    val period: String,
    val features: List<String>,
    val badge: String? = null,
    val badgeColor: Color? = null,
    val productId: String,
    val isHighlighted: Boolean = false
)

@Composable
fun CurrentPremiumCard(
    hasLifetime: Boolean,
    hasAnnual: Boolean,
    primaryColor: Color,
    textColor: Color,
    isInTrial: Boolean = false,
    trialExpirationTime: Long? = null,
    lifetimeLabel: String = "Lifetime Premium",
    annualLabel: String = "Annual Premium",
    trialLabel: String = "Trial",
    thankYouText: String = "Thank you for your support!",
    trialDaysRemainingFormat: String = "Trial active - %d days remaining"
) {
    val statusText = when {
        hasLifetime -> lifetimeLabel
        hasAnnual && isInTrial -> "$annualLabel ($trialLabel)"
        hasAnnual -> annualLabel
        else -> ""
    }
    val subtitleText = when {
        hasAnnual && isInTrial && trialExpirationTime != null -> {
            val daysRemaining = ((trialExpirationTime - Clock.System.now().toEpochMilliseconds()) / (1000 * 60 * 60 * 24)).toInt()
            if (daysRemaining > 0) trialDaysRemainingFormat.replace("%d", daysRemaining.toString()) else thankYouText
        }
        else -> thankYouText
    }

    Box(
        modifier = Modifier.fillMaxWidth()
            .background(Brush.horizontalGradient(colors = listOf(primaryColor.copy(alpha = 0.3f), primaryColor.copy(alpha = 0.15f))), RoundedCornerShape(16.dp))
            .border(1.dp, primaryColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)).padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null, tint = primaryColor, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = statusText, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitleText, color = textColor.copy(alpha = 0.8f), fontSize = 13.sp, fontWeight = FontWeight.Normal)
            }
        }
    }
}

@Composable
fun PremiumDialog(
    title: String, message: String, icon: ImageVector, iconTint: Color,
    onDismiss: () -> Unit, primaryColor: Color, textColor: Color, backgroundColor: Color,
    okText: String = "OK"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(text = okText, color = primaryColor, fontWeight = FontWeight.Bold) } },
        icon = { Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(48.dp)) },
        title = { Text(text = title, color = textColor, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center) },
        text = { Text(text = message, color = textColor.copy(alpha = 0.9f), fontSize = 16.sp, lineHeight = 22.sp, textAlign = TextAlign.Center) },
        containerColor = backgroundColor, shape = RoundedCornerShape(20.dp)
    )
}
