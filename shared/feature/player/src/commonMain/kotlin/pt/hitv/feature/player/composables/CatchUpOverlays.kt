package pt.hitv.feature.player.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.model.ChannelEpgInfo
import pt.hitv.feature.player.CatchUpState

// ==============================================================================
// Catch-up overlays — mobile. Ported from the original hitv PlayerComposables.kt.
// All three composables are used by ChannelPlayerScreen (shared, so iOS picks
// them up for free). Strings are hardcoded (English) since the KMP port has no
// string-resource infra yet; i18n can swap them out later.
// ==============================================================================

/**
 * Overlay shown at the bottom of the player while streaming live on a
 * catch-up-enabled channel. "From start" jumps to the beginning of the
 * currently-airing programme; "Browse Archive" opens the full day browser.
 */
@Composable
fun CatchUpEpgOverlayMobile(
    epgInfo: ChannelEpgInfo,
    pastPrograms: List<ChannelEpgInfo>,
    onRewindToStart: () -> Unit,
    onBrowseArchive: () -> Unit = {},
) {
    val themeColors = getThemeColors()

    Card(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = epgInfo.programmeTitle.orEmpty(),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val startStr = epgInfo.startTime?.let { formatTimeHHmm(it) }.orEmpty()
                    val endStr = epgInfo.endTime?.let { formatTimeHHmm(it) }.orEmpty()
                    if (startStr.isNotEmpty()) {
                        Text(
                            text = "Now \u00B7 $startStr \u2013 $endStr",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onRewindToStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColors.primaryColor,
                        contentColor = themeColors.textColor,
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Rewind to start", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("From start", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            val progress = epgInfo.calculateProgress(Clock.System.now().toEpochMilliseconds())
            if (progress > 0f) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = themeColors.primaryColor,
                    trackColor = Color.White.copy(alpha = 0.15f),
                )
            }

            if (pastPrograms.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onBrowseArchive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColors.textColor.copy(alpha = 0.08f),
                        contentColor = themeColors.textColor.copy(alpha = 0.7f),
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Browse Archive (${pastPrograms.size} programs)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

/**
 * Controls overlay while actively playing archived content.
 * Shows programme info, seek slider with elapsed/total labels, speed chip,
 * LIVE pill, and previous / browse archive / next buttons.
 */
@Composable
fun CatchUpControlsOverlayMobile(
    catchUpState: CatchUpState,
    onBackToLive: () -> Unit,
    onPreviousProgram: () -> Unit,
    onNextProgram: () -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit = {},
    onSeekTo: (Long) -> Unit = {},
    onBrowseArchive: () -> Unit = {},
) {
    val themeColors = getThemeColors()
    val startTime = remember(catchUpState.programStart) { formatTimeHHmm(catchUpState.programStart) }
    val endTime = remember(catchUpState.programEnd) { formatTimeHHmm(catchUpState.programEnd) }
    val date = remember(catchUpState.programStart) { formatDateWeekday(catchUpState.programStart) }

    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }
    val effectiveDuration = if (catchUpState.playbackDurationMs > 0) {
        catchUpState.playbackDurationMs
    } else {
        (catchUpState.programEnd - catchUpState.programStart).coerceAtLeast(0L)
    }
    val sliderPosition = if (isSeeking) seekPosition else {
        if (effectiveDuration > 0) catchUpState.playbackPositionMs.toFloat() / effectiveDuration.toFloat() else 0f
    }

    Card(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            // Top row: programme info + speed chip + LIVE pill
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = catchUpState.programTitle,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "$date \u00B7 $startTime \u2013 $endTime",
                        color = Color.White.copy(alpha = 0.45f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                PlaybackSpeedChip(currentSpeed = catchUpState.playbackSpeed, onSpeedChange = onPlaybackSpeedChange)
                Spacer(modifier = Modifier.width(8.dp))
                LivePillButton(onClick = onBackToLive)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Seek bar + time labels
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = sliderPosition.coerceIn(0f, 1f),
                    onValueChange = { v ->
                        isSeeking = true
                        seekPosition = v
                    },
                    onValueChangeFinished = {
                        val seekMs = (seekPosition * effectiveDuration).toLong()
                        onSeekTo(seekMs)
                        isSeeking = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = themeColors.primaryColor,
                        activeTrackColor = themeColors.primaryColor,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                    ),
                    modifier = Modifier.fillMaxWidth().height(24.dp),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val currentPos = if (isSeeking) (seekPosition * effectiveDuration).toLong() else catchUpState.playbackPositionMs
                    Text(
                        text = formatDuration(currentPos),
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        text = formatDuration(effectiveDuration),
                        color = Color.White.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Previous / Browse Archive / Next
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onPreviousProgram,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    border = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = themeColors.textColor.copy(alpha = 0.6f),
                    ),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Previous", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = onBrowseArchive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColors.primaryColor.copy(alpha = 0.15f),
                        contentColor = themeColors.primaryColor,
                    ),
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Browse Archive", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onNextProgram,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    border = null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = themeColors.textColor.copy(alpha = 0.6f),
                    ),
                ) {
                    Text("Next", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

/**
 * Full-screen immersive dialog for browsing past programmes. Day chips at
 * the top, programme list below. Tapping a programme fires onProgramClick
 * with the start/end/title/description, so the caller can kick the URL swap.
 */
@Composable
fun CatchUpArchiveSheet(
    pastPrograms: List<ChannelEpgInfo>,
    channelName: String,
    onDismiss: () -> Unit,
    onProgramClick: (programStart: Long, programEnd: Long, title: String, description: String) -> Unit,
) {
    val themeColors = getThemeColors()

    val groupedPrograms = remember(pastPrograms) {
        pastPrograms
            .sortedByDescending { it.startTime ?: 0L }
            .groupBy { startOfDayLocal(it.startTime ?: 0L) }
    }
    val days = remember(groupedPrograms) { groupedPrograms.keys.sortedDescending() }
    var selectedDay by remember { mutableStateOf(days.firstOrNull() ?: 0L) }
    val selectedPrograms = remember(selectedDay, groupedPrograms) { groupedPrograms[selectedDay].orEmpty() }

    val todayMillis = remember { startOfDayLocal(Clock.System.now().toEpochMilliseconds()) }
    val yesterdayMillis = remember { todayMillis - 86_400_000L }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE6000000))
                .systemBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = themeColors.primaryColor,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = channelName,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${pastPrograms.size} programs \u00B7 ${days.size} days",
                            color = Color.White.copy(alpha = 0.45f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.6f),
                        )
                    }
                }

                // Day tabs
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                ) {
                    items(days) { dayMillis ->
                        val isSelected = dayMillis == selectedDay
                        val programCount = groupedPrograms[dayMillis]?.size ?: 0
                        val primary = when (dayMillis) {
                            todayMillis -> "Today"
                            yesterdayMillis -> "Yesterday"
                            else -> formatWeekdayShort(dayMillis)
                        }
                        val secondary = formatDateDayMonth(dayMillis)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDay = dayMillis },
                            label = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 2.dp),
                                ) {
                                    Text(
                                        text = primary,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                    Text(
                                        text = secondary,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                        else
                                            Color.White.copy(alpha = 0.4f),
                                    )
                                }
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Text(
                                        text = "$programCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White.copy(alpha = 0.06f),
                                labelColor = Color.White.copy(alpha = 0.8f),
                                selectedContainerColor = themeColors.primaryColor,
                                selectedLabelColor = Color.White,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = Color.White.copy(alpha = 0.1f),
                                selectedBorderColor = Color.Transparent,
                                enabled = true,
                                selected = isSelected,
                            ),
                            modifier = Modifier.height(48.dp),
                        )
                    }
                }

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.06f),
                    modifier = Modifier.padding(horizontal = 20.dp),
                )

                if (selectedPrograms.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No archived programs for this day",
                            color = Color.White.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(selectedPrograms) { program ->
                            val startStr = program.startTime?.let { formatTimeHHmm(it) }.orEmpty()
                            val endStr = program.endTime?.let { formatTimeHHmm(it) }.orEmpty()
                            val durationMin = remember(program.startTime, program.endTime) {
                                val s = program.startTime ?: 0L
                                val e = program.endTime ?: 0L
                                if (e > s) "${(e - s) / 60_000}min" else ""
                            }
                            Surface(
                                onClick = {
                                    val s = program.startTime ?: return@Surface
                                    val e = program.endTime ?: return@Surface
                                    onProgramClick(s, e, program.programmeTitle.orEmpty(), program.programmeDescription.orEmpty())
                                },
                                color = Color.White.copy(alpha = 0.04f),
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.width(56.dp)) {
                                        Text(
                                            text = startStr,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White,
                                        )
                                        Text(
                                            text = endStr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.3f),
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp)
                                            .width(2.dp)
                                            .height(32.dp)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(themeColors.primaryColor.copy(alpha = 0.3f)),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = program.programmeTitle.orEmpty(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            if (durationMin.isNotEmpty()) {
                                                Text(
                                                    text = durationMin,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.35f),
                                                )
                                            }
                                            if (!program.programmeDescription.isNullOrBlank()) {
                                                Text(
                                                    text = "\u00B7",
                                                    color = Color.White.copy(alpha = 0.2f),
                                                    style = MaterialTheme.typography.labelSmall,
                                                )
                                                Text(
                                                    text = program.programmeDescription.orEmpty(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.35f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play catch-up",
                                        tint = themeColors.primaryColor,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Tap-to-cycle chip showing the current playback speed (0.5x – 2.0x). */
@Composable
fun PlaybackSpeedChip(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val themeColors = getThemeColors()
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    val currentIndex = speeds.indexOf(currentSpeed).takeIf { it >= 0 } ?: 2
    Surface(
        onClick = { onSpeedChange(speeds[(currentIndex + 1) % speeds.size]) },
        color = if (currentSpeed != 1.0f) themeColors.primaryColor else themeColors.textColor.copy(alpha = 0.2f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.height(28.dp),
    ) {
        Text(
            text = "${currentSpeed}x",
            color = themeColors.textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

/** Red "● LIVE" pill — tap to exit catch-up and resume live playback. */
@Composable
fun LivePillButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp),
    ) {
        Text(text = "\u25CF LIVE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ============================ KMP date helpers ===============================
// The original used SimpleDateFormat / java.util.Calendar. kotlinx-datetime has
// no locale-aware formatter in commonMain, so we hand-roll the few shapes we
// need. They're short on purpose — fancier i18n can swap these later.

private fun formatTimeHHmm(epochMillis: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    val h = dt.hour.toString().padStart(2, '0')
    val m = dt.minute.toString().padStart(2, '0')
    return "$h:$m"
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    val mm = m.toString().padStart(2, '0')
    val ss = s.toString().padStart(2, '0')
    return if (h > 0) "$h:$mm:$ss" else "$m:$ss"
}

private fun formatDateWeekday(epochMillis: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${weekdayShort(dt.dayOfWeek)}, ${dt.dayOfMonth} ${monthShort(dt.month)}"
}

private fun formatWeekdayShort(epochMillis: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    return weekdayShort(dt.dayOfWeek)
}

private fun formatDateDayMonth(epochMillis: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.dayOfMonth} ${monthShort(dt.month)}"
}

private fun startOfDayLocal(epochMillis: Long): Long {
    val zone = TimeZone.currentSystemDefault()
    val date: LocalDate = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone).date
    return date.atStartOfDayIn(zone).toEpochMilliseconds()
}

private fun weekdayShort(dow: DayOfWeek): String = when (dow) {
    DayOfWeek.MONDAY -> "Mon"
    DayOfWeek.TUESDAY -> "Tue"
    DayOfWeek.WEDNESDAY -> "Wed"
    DayOfWeek.THURSDAY -> "Thu"
    DayOfWeek.FRIDAY -> "Fri"
    DayOfWeek.SATURDAY -> "Sat"
    DayOfWeek.SUNDAY -> "Sun"
    else -> ""
}

private fun monthShort(m: Month): String = when (m) {
    Month.JANUARY -> "Jan"
    Month.FEBRUARY -> "Feb"
    Month.MARCH -> "Mar"
    Month.APRIL -> "Apr"
    Month.MAY -> "May"
    Month.JUNE -> "Jun"
    Month.JULY -> "Jul"
    Month.AUGUST -> "Aug"
    Month.SEPTEMBER -> "Sep"
    Month.OCTOBER -> "Oct"
    Month.NOVEMBER -> "Nov"
    Month.DECEMBER -> "Dec"
    else -> ""
}
