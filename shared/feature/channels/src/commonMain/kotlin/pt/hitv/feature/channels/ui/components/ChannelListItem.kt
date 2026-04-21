package pt.hitv.feature.channels.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import pt.hitv.core.model.Channel
import pt.hitv.core.model.ChannelEpgInfo
import pt.hitv.feature.channels.StreamViewModel
import pt.hitv.core.model.enums.ClickType
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * A list item component for displaying a channel in list layouts.
 * Ported to CMP - uses theme colors and cross-platform composables.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelListItem(
    channel: Channel,
    onChannelClicked: (ClickType) -> Unit,
    modifier: Modifier = Modifier,
    shouldRequestFocus: Boolean = false,
    epgCache: SnapshotStateMap<String, ChannelEpgInfo?>,
    epgLoadingSet: SnapshotStateList<String>,
    isExpanded: Boolean = false,
    isCompact: Boolean = false,
    viewModel: StreamViewModel
) {
    // Fetch EPG data for this channel only if not already cached or loading.
    // Only cache a non-null result — caching `null` would poison the cache for
    // rows that recomposed before EPG sync completed: subsequent recompositions
    // would see the cached `null`, skip the fetch, and render "No EPG" forever
    // (until the user force-closed the app to clear the in-memory cache).
    LaunchedEffect(channel.epgChannelId) {
        val epgId = channel.epgChannelId
        if (!epgId.isNullOrBlank() && epgCache[epgId] == null && !epgLoadingSet.contains(epgId)) {
            try {
                epgLoadingSet.add(epgId)
                val epgData = withContext(Dispatchers.IO) {
                    viewModel.fetchCurrentEpgSuspend(channel, Clock.System.now().toEpochMilliseconds())
                }
                if (epgData != null) {
                    epgCache[epgId] = epgData
                }
            } catch (_: Exception) {
                // Don't cache failures — let the next recomposition retry.
            } finally {
                epgLoadingSet.remove(epgId)
            }
        }
    }

    val epgData = channel.epgChannelId?.let { epgCache[it] }
    val isLoadingEpg = channel.epgChannelId?.let { epgLoadingSet.contains(it) } ?: false

    val elevation by animateDpAsState(
        targetValue = if (isExpanded) 8.dp else 2.dp,
        animationSpec = tween(200), label = "elevation"
    )

    val themeColors = getThemeColors()

    val height = if (isCompact) 70.dp else 100.dp
    val logoSize = if (isCompact) 50.dp else 76.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .combinedClickable(
                onClick = { onChannelClicked(ClickType.CLICK) },
                onLongClick = { onChannelClicked(ClickType.LONG_CLICK) }
            )
            .focusable(),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(
            containerColor = themeColors.backgroundSecondary
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isExpanded) {
            androidx.compose.foundation.BorderStroke(2.dp, themeColors.primaryColor)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isCompact) 8.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel Logo — loads actual image via Coil, with placeholder
            Card(
                modifier = Modifier.size(logoSize),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = themeColors.textColor.copy(alpha = 0.1f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (!channel.streamIcon.isNullOrBlank()) {
                        coil3.compose.AsyncImage(
                            model = channel.streamIcon,
                            contentDescription = channel.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        // Placeholder with channel name initial
                        Text(
                            text = "#${channel.name?.firstOrNull()?.uppercase() ?: ""}",
                            fontSize = if (isCompact) 14.sp else 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.textColor.copy(alpha = 0.5f)
                        )
                    }

                    if (channel.isFavorite) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(20.dp)
                                .background(themeColors.primaryColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Favorite",
                                tint = themeColors.textColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // Channel Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = if (isCompact) Arrangement.Center else Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = channel.name.orEmpty(),
                        color = themeColors.textColor,
                        fontSize = if (isCompact) 14.sp else 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (channel.tvArchive > 0) {
                        Box(
                            modifier = Modifier
                                .background(themeColors.primaryColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "CATCH-UP",
                                color = themeColors.textColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (epgData != null && epgData.programmeTitle != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(if (isCompact) 0.dp else 4.dp)) {
                        Text(
                            text = epgData.programmeTitle.orEmpty(),
                            color = themeColors.textColor.copy(alpha = 0.8f),
                            fontSize = if (isCompact) 12.sp else 14.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val startTime = epgData.startTime
                            val endTime = epgData.endTime
                            if (startTime != null && endTime != null) {
                                val progress = remember(startTime, endTime) {
                                    viewModel.calculateEpgTime(
                                        startTime, endTime,
                                        Clock.System.now().toEpochMilliseconds()
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { progress / 100f },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = themeColors.primaryColor,
                                    trackColor = themeColors.textColor.copy(alpha = 0.15f),
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = if (isLoadingEpg) "Loading..." else "No EPG data available",
                        color = themeColors.textColor.copy(alpha = 0.5f),
                        fontSize = if (isCompact) 11.sp else 13.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
