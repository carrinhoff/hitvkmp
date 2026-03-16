package pt.hitv.feature.settings.options.options

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.hitv.core.designsystem.theme.getThemeColors

private data class PrimaryOption(val title: String, val icon: ImageVector, val onClick: () -> Unit)
private data class SecondaryOption(val title: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
fun OptionsScreen(
    expirationText: String,
    onLiveClick: () -> Unit,
    onMoviesClick: () -> Unit,
    onTvShowsClick: () -> Unit,
    onEpgClick: () -> Unit,
    onSwitchAccountClick: () -> Unit,
    onRefreshDataClick: () -> Unit,
    onOthersClick: () -> Unit,
    onThemeSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    liveTvLabel: String = "Live TV",
    moviesLabel: String = "Movies",
    tvShowsLabel: String = "TV Shows",
    switchPlaylistLabel: String = "Switch Playlist",
    premiumLabel: String = "Premium",
    moreLabel: String = "More Options",
    welcomeText: String = "Welcome to HITV"
) {
    val currentTheme = getThemeColors()
    val backgroundColor = currentTheme.backgroundPrimary
    val primaryColor = currentTheme.primaryColor

    val liveOption = PrimaryOption(liveTvLabel, Icons.Rounded.LiveTv, onLiveClick)
    val otherPrimaryOptions = listOf(
        PrimaryOption(moviesLabel, Icons.Rounded.Movie, onMoviesClick),
        PrimaryOption(tvShowsLabel, Icons.Rounded.Tv, onTvShowsClick)
    )
    val secondaryOptions = listOf(
        SecondaryOption(switchPlaylistLabel, Icons.Rounded.SwitchAccount, onSwitchAccountClick),
        SecondaryOption(premiumLabel, Icons.Rounded.Palette, onThemeSettingsClick),
        SecondaryOption(moreLabel, Icons.Rounded.Settings, onOthersClick)
    )

    Box(modifier = modifier.fillMaxSize().background(backgroundColor)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 16.dp)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(text = welcomeText, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
                    Text(text = expirationText, color = primaryColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                // Live TV card
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), modifier = Modifier.fillMaxWidth().height(180.dp).clickable(onClick = liveOption.onClick)) {
                    Box(modifier = Modifier.fillMaxSize().background(brush = Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.05f))))) {
                        Column(modifier = Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.SpaceBetween) {
                            Icon(imageVector = liveOption.icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.White)
                            Text(text = liveOption.title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, letterSpacing = 1.2.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    otherPrimaryOptions.forEach { option ->
                        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), modifier = Modifier.weight(1f).clickable(onClick = option.onClick)) {
                            Box(modifier = Modifier.fillMaxSize().background(brush = Brush.radialGradient(colors = listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.04f)), radius = 200f))) {
                                Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Icon(imageVector = option.icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = option.title, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                secondaryOptions.forEach { option ->
                    Box(
                        modifier = Modifier.fillMaxWidth(0.9f).height(52.dp)
                            .background(brush = Brush.horizontalGradient(colors = listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.06f))), shape = RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp)).clickable(onClick = option.onClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(imageVector = option.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = option.title, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}
