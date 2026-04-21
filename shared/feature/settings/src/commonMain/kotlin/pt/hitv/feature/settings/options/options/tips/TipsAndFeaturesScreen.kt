package pt.hitv.feature.settings.options.options.tips

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.koin.compose.koinInject
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * Static content screen surfacing product tips and headline features. Ports the
 * text content from the original hitv `tips/` module — illustrations are omitted
 * intentionally as those assets are not in the KMP module.
 *
 * Per-tip dismissal is persisted as a comma-joined string in PreferencesHelper
 * under the `DISMISSED_TIPS_KEY` key. "Restore all tips" clears the set so the
 * full list shows up again on the next composition.
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
    val id: String,
    val icon: ImageVector,
    val title: String,
    val description: String,
)

private val tips: List<Tip> = listOf(
    Tip("switch_accounts", Icons.Rounded.SwitchAccount, "Switch Accounts Anytime",
        "Keep multiple IPTV playlists. Switch between them from the account card without losing your progress."),
    Tip("pin_hide_categories", Icons.Rounded.Category, "Pin and Hide Categories",
        "Use Manage Categories to keep your favorite categories on top and hide ones you never watch."),
    Tip("theme_studio", Icons.Rounded.Palette, "Theme Studio",
        "Pick from curated themes or tune colors yourself. The whole app restyles in real time."),
    Tip("parental_controls", Icons.Rounded.Lock, "Parental Controls",
        "Lock adult categories behind a PIN. Sessions time out automatically for extra safety."),
    Tip("live_epg", Icons.Rounded.LiveTv, "Live EPG Notifications",
        "Schedule notifications for upcoming programs so you never miss your favorites."),
    Tip("background_sync", Icons.Rounded.Sync, "Background Sync",
        "Let the app refresh your channels, movies, series and guide in the background — on Wi-Fi only if you prefer."),
    Tip("playback", Icons.Rounded.Speed, "Tune Playback",
        "Pick a Live Buffer Size that suits your network. Larger buffers handle bumpy connections."),
    Tip("channel_preview", Icons.Rounded.PlayCircle, "Channel Preview",
        "Peek at a channel before committing. Toggle it off to save bandwidth."),
)

private const val DISMISSED_TIPS_KEY = "dismissed_tips"

private fun PreferencesHelper.readDismissedTips(): Set<String> {
    val raw = getStoredTag(DISMISSED_TIPS_KEY)
    if (raw.isBlank()) return emptySet()
    return raw.split(',').mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() } }.toSet()
}

private fun PreferencesHelper.writeDismissedTips(ids: Set<String>) {
    setStoredTag(DISMISSED_TIPS_KEY, ids.joinToString(","))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TipsAndFeaturesContent(onBack: () -> Unit) {
    val themeColors = getThemeColors()
    val backgroundColor = themeColors.backgroundPrimary
    val secondaryBackgroundColor = themeColors.backgroundSecondary
    val primaryColor = themeColors.primaryColor
    val textColor = Color.White
    val textSecondary = Color.White.copy(alpha = 0.7f)

    val preferencesHelper: PreferencesHelper = koinInject()
    var dismissed by remember { mutableStateOf(preferencesHelper.readDismissedTips()) }

    fun dismiss(id: String) {
        val next = dismissed + id
        dismissed = next
        preferencesHelper.writeDismissedTips(next)
    }

    fun restoreAll() {
        dismissed = emptySet()
        preferencesHelper.writeDismissedTips(emptySet())
    }

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
            val visibleTips = tips.filter { it.id !in dismissed }
            if (visibleTips.isEmpty()) {
                Text(
                    text = "You've dismissed every tip. Tap Restore below to bring them back.",
                    color = textSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
            visibleTips.forEach { tip ->
                AnimatedVisibility(
                    visible = tip.id !in dismissed,
                    exit = fadeOut() + shrinkVertically(),
                    enter = fadeIn(),
                ) {
                    TipCard(
                        tip = tip,
                        primaryColor = primaryColor,
                        textColor = textColor,
                        textSecondary = textSecondary,
                        onDismiss = { dismiss(tip.id) },
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (dismissed.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = { restoreAll() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = primaryColor,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Restore all tips",
                        color = primaryColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
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
    textSecondary: Color,
    onDismiss: () -> Unit,
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
        // "Don't show again" — tap the × to hide this tip. Persists to prefs so the
        // tip stays dismissed across sessions. "Restore all tips" below brings the
        // full list back.
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Don't show this tip again",
                tint = textSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
