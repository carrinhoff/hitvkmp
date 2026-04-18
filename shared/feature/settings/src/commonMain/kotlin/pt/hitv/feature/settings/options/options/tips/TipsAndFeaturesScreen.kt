package pt.hitv.feature.settings.options.options.tips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * Static content screen surfacing product tips and headline features. Ports the
 * text content from the original hitv `tips/` module — illustrations are omitted
 * intentionally as those assets are not in the KMP module.
 */
class TipsAndFeaturesScreen : Screen {
    override val key = "TipsAndFeatures"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        TipsAndFeaturesContent(onBack = { navigator.pop() })
    }
}

private data class Tip(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private val tips: List<Tip> = listOf(
    Tip(
        icon = Icons.Rounded.SwitchAccount,
        title = "Switch Accounts Anytime",
        description = "Keep multiple IPTV playlists. Switch between them from the account card without losing your progress."
    ),
    Tip(
        icon = Icons.Rounded.Category,
        title = "Pin and Hide Categories",
        description = "Use Manage Categories to keep your favorite categories on top and hide ones you never watch."
    ),
    Tip(
        icon = Icons.Rounded.Palette,
        title = "Theme Studio",
        description = "Pick from curated themes or tune colors yourself. The whole app restyles in real time."
    ),
    Tip(
        icon = Icons.Rounded.Lock,
        title = "Parental Controls",
        description = "Lock adult categories behind a PIN. Sessions time out automatically for extra safety."
    ),
    Tip(
        icon = Icons.Rounded.LiveTv,
        title = "Live EPG Notifications",
        description = "Schedule notifications for upcoming programs so you never miss your favorites."
    ),
    Tip(
        icon = Icons.Rounded.Sync,
        title = "Background Sync",
        description = "Let the app refresh your channels, movies, series and guide in the background — on Wi-Fi only if you prefer."
    ),
    Tip(
        icon = Icons.Rounded.Speed,
        title = "Tune Playback",
        description = "Pick a Live Buffer Size that suits your network. Larger buffers handle bumpy connections."
    ),
    Tip(
        icon = Icons.Rounded.PlayCircle,
        title = "Channel Preview",
        description = "Peek at a channel before committing. Toggle it off to save bandwidth."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TipsAndFeaturesContent(onBack: () -> Unit) {
    val themeColors = getThemeColors()
    val backgroundColor = themeColors.backgroundPrimary
    val secondaryBackgroundColor = themeColors.backgroundSecondary
    val primaryColor = themeColors.primaryColor
    val textColor = Color.White
    val textSecondary = Color.White.copy(alpha = 0.7f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tips & Features", color = textColor, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.background(
            Brush.verticalGradient(
                listOf(backgroundColor, secondaryBackgroundColor, backgroundColor.copy(alpha = 0.9f))
            )
        )
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Discover what you can do",
                color = textSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            tips.forEach { tip ->
                TipCard(tip, primaryColor, textColor, textSecondary)
                Spacer(modifier = Modifier.height(12.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TipCard(
    tip: Tip,
    primaryColor: Color,
    textColor: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(textColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(primaryColor.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tip.icon,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tip.title,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tip.description,
                color = textSecondary,
                fontSize = 13.sp
            )
        }
    }
}
