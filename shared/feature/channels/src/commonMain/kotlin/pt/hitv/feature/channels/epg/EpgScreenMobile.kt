package pt.hitv.feature.channels.epg

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import pt.hitv.epg.EpgUtils
import pt.hitv.epg.data.EPGData
import pt.hitv.epg.domain.EPGChannel
import pt.hitv.epg.domain.EPGEvent
import kotlin.math.max

// --- Configuration Constants (identical to the original) ---
private val ChannelHeaderWidth = 120.dp
private val TimeIntervalWidth = 140.dp
private val ChannelHeight = 60.dp
private val TimelineHeight = 48.dp
private val ProgramPadding = 4.dp
private val ProgramCornerRadius = 8.dp
private val DetailsCardHeight = 180.dp

/** Pre-calculated horizontal layout for one programme block. */
data class EventLayout(val epgEvent: EPGEvent, val widthPx: Int, val xOffsetPx: Int)

/**
 * Maps programmes onto horizontal pixel positions relative to the grid's left edge [startTime].
 *
 * Extracted from the composable so it can be tested: this arithmetic decides where every block
 * lands, and an error here skews the entire grid against the timeline labels in a way that is
 * easy to miss by eye. Behaviour matches the original exactly:
 *
 * - programmes that ended before [startTime] are dropped;
 * - a programme already in progress is clipped to start at [startTime] (so it renders from the
 *   left edge rather than off-screen), which is what `max(event.start, startTime)` does;
 * - zero/negative-duration programmes are dropped.
 */
internal fun computeEventLayouts(
    events: List<EPGEvent>,
    startTime: Long,
    pxPerMinute: Float,
): List<EventLayout> = events.mapNotNull { event ->
    if (event.end < startTime) return@mapNotNull null
    val visualStartTime = max(event.start, startTime)
    val xOffsetMillis = visualStartTime - startTime
    val durationMillis = event.end - visualStartTime
    if (durationMillis <= 0) return@mapNotNull null
    EventLayout(
        epgEvent = event,
        widthPx = (durationMillis / 60_000f * pxPerMinute).toInt(),
        xOffsetPx = (xOffsetMillis / 60_000f * pxPerMinute).toInt(),
    )
}

/** Colour set the grid draws with. Defaults mirror the original's hardcoded fallback. */
data class EpgThemeColors(
    val backgroundPrimary: Color = Color(0xFF141414),
    val backgroundSecondary: Color = Color(0xFF2A2A2A),
    val cardColor: Color = Color(0xFF1E1E1E),
    val primaryColor: Color = Color(0xFFF34213),
    val textColor: Color = Color.White,
    val textSecondaryColor: Color = Color.White.copy(alpha = 0.8f),
)

/**
 * Scrolling programme-guide grid: a fixed channel column on the left, a horizontally draggable
 * 24-hour timeline of programme blocks on the right, a pulsing now-indicator, and a tap-to-expand
 * details card per row.
 *
 * Faithful port of the original's `epg/EpgScreenMobile.kt` — same constants, same layout maths
 * (`pxPerMinute` derived from a 30-minute `TimeIntervalWidth`), same Canvas drawing, same tap
 * hit-testing against absolute offsets, same "Now" jump button.
 *
 * ## Deliberate deviations, all flagged
 *
 * 1. **Lives in `feature:channels`, not the `epg` module.** In the original the grid sits in the
 *    `epg` module, but here `core:data` depends on `:shared:epg` (for `EpgParser`), so adding
 *    Compose + navigation there would invert the dependency graph. `feature:channels` already
 *    depends on `:shared:epg` and owns the EPG ViewModel, so the UI belongs here.
 * 2. **`java.util.Calendar` / `SimpleDateFormat` → [EpgUtils]** (kotlinx-datetime), so it runs on
 *    iOS. Output shapes are unchanged.
 * 3. **`stringResource` → String parameters with English defaults**, consistent with the rest of
 *    this port. Localisation is a separate outstanding item.
 * 4. **No "Set reminder" button on future programmes.** The original schedules an `AlarmManager`
 *    alarm; that needs an iOS `expect`/`actual` over `UNUserNotificationCenter` plus a permission
 *    flow and an Info.plist usage string, which is deferred. Rather than render a button that
 *    silently does nothing, the action is omitted for future programmes — see
 *    KMP_MIGRATION_AUDIT.md.
 * 5. **No catch-up paywall.** The original gates catch-up playback behind premium and otherwise
 *    shows `CatchUpPaywallSheet`. Billing is not wired on either platform, and
 *    `UngatedPremiumStatusProvider` currently grants premium to everyone, so the paywall branch
 *    would be unreachable. Catch-up plays directly; restore the gate when billing lands.
 */
@Composable
fun EpgScreenMobile(
    epgData: EPGData,
    onChannelClick: (EPGChannel) -> Unit,
    onChannelLongClick: ((EPGChannel) -> Unit)? = null,
    onProgramActionClick: (event: EPGEvent, channel: EPGChannel) -> Unit,
    onBackPressed: () -> Unit,
    themeColors: EpgThemeColors = EpgThemeColors(),
    nowLabel: String = "Now",
    goToChannelLabel: String = "Go to channel",
    playCatchUpLabel: String = "Play catch-up",
) {
    val startTime = remember { EpgUtils.roundedHalfHourStart(Clock.System.now().toEpochMilliseconds()) }
    var currentTime by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    var expandedChannelId by remember { mutableStateOf<String?>(null) }
    var eventForDetails by remember { mutableStateOf<EPGEvent?>(null) }
    var horizontalOffset by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            currentTime = Clock.System.now().toEpochMilliseconds()
        }
    }

    val channels = remember(epgData) {
        (0 until epgData.channelCount).mapNotNull { epgData.getChannel(it) }
    }

    val pxPerMinute = with(LocalDensity.current) { (TimeIntervalWidth.value / 30f).dp.toPx() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.backgroundPrimary)
    ) {
        val programsViewWidthPx = with(LocalDensity.current) { (maxWidth - ChannelHeaderWidth).toPx() }
        val twentyFourHourWidthPx = 24 * 60 * pxPerMinute
        val maxHorizontalOffset = (twentyFourHourWidthPx - programsViewWidthPx).coerceAtLeast(0f)
        val screenPaddingPx = with(LocalDensity.current) { 20.dp.toPx() }

        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TimelineHeight)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                themeColors.backgroundSecondary,
                                themeColors.backgroundPrimary
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(0f, with(LocalDensity.current) { TimelineHeight.toPx() })
                        )
                    )
                    .shadow(4.dp, shape = RoundedCornerShape(0.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.width(ChannelHeaderWidth),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBackPressed) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = themeColors.textColor
                            )
                        }
                        DateHeader(
                            currentTime = currentTime,
                            themeColors = themeColors,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clipToBounds()
                    ) {
                        TimelineCanvas(
                            intervals = 48,
                            startTime = startTime,
                            currentTime = currentTime,
                            themeColors = themeColors,
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(with(LocalDensity.current) { twentyFourHourWidthPx.toDp() })
                                .graphicsLayer { translationX = -horizontalOffset }
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(maxHorizontalOffset) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            horizontalOffset =
                                (horizontalOffset - dragAmount.x).coerceIn(0f, maxHorizontalOffset)
                        }
                    }
            ) {
                itemsIndexed(
                    items = channels,
                    key = { _, channel -> channel.channelID }
                ) { index, channel ->
                    val eventLayouts = remember(startTime, pxPerMinute, epgData, index) {
                        computeEventLayouts(
                            events = epgData.getEvents(index).orEmpty().filterNotNull(),
                            startTime = startTime,
                            pxPerMinute = pxPerMinute,
                        )
                    }

                    Column {
                        EpgChannelRow(
                            channel = channel,
                            eventLayouts = eventLayouts,
                            horizontalOffset = horizontalOffset,
                            currentTime = currentTime,
                            themeColors = themeColors,
                            onChannelClick = { onChannelClick(channel) },
                            onChannelLongClick = onChannelLongClick?.let { callback -> { callback(channel) } },
                            onEventSelected = { newEvent ->
                                if (expandedChannelId == channel.channelID && eventForDetails == newEvent) {
                                    expandedChannelId = null
                                    eventForDetails = null
                                } else {
                                    expandedChannelId = channel.channelID
                                    eventForDetails = newEvent
                                }
                            }
                        )

                        AnimatedVisibility(
                            visible = expandedChannelId == channel.channelID,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            eventForDetails?.let { event ->
                                Row {
                                    Spacer(modifier = Modifier.width(ChannelHeaderWidth))
                                    ExpandedInfoCard(
                                        event = event,
                                        channel = channel,
                                        currentTime = currentTime,
                                        themeColors = themeColors,
                                        goToChannelLabel = goToChannelLabel,
                                        playCatchUpLabel = playCatchUpLabel,
                                        onActionClick = { onProgramActionClick(event, channel) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val indicatorAbsolutePx = (currentTime - startTime) / 60_000f * pxPerMinute
        val indicatorVisiblePx = indicatorAbsolutePx - horizontalOffset
        if (indicatorVisiblePx >= 0 && indicatorVisiblePx <= programsViewWidthPx) {
            CurrentTimeIndicator(
                themeColors = themeColors,
                modifier = Modifier
                    .padding(top = TimelineHeight, start = ChannelHeaderWidth)
                    .offset(x = with(LocalDensity.current) { indicatorVisiblePx.toDp() })
            )
        }

        Button(
            onClick = {
                val timeSinceStartMillis = Clock.System.now().toEpochMilliseconds() - startTime
                val currentTimeOffsetPx = (timeSinceStartMillis / 60_000f * pxPerMinute)
                horizontalOffset = (currentTimeOffsetPx - screenPaddingPx).coerceIn(0f, maxHorizontalOffset)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
                .height(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = themeColors.primaryColor,
                contentColor = themeColors.textColor
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(text = nowLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ExpandedInfoCard(
    event: EPGEvent,
    channel: EPGChannel,
    currentTime: Long,
    themeColors: EpgThemeColors,
    goToChannelLabel: String,
    playCatchUpLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val startTime = EpgUtils.getShortTime(event.start)
    val endTime = EpgUtils.getShortTime(event.end)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(DetailsCardHeight)
            .background(themeColors.cardColor)
    ) {
        coil3.compose.AsyncImage(
            model = event.imageURL,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        val isLive = currentTime in event.start..event.end
        val isPast = currentTime > event.end
        // Future programmes intentionally have no action — see the class KDoc, deviation 4.
        val buttonText = when {
            isLive -> goToChannelLabel
            isPast && channel.hasCatchUp -> playCatchUpLabel
            else -> null
        }

        if (buttonText != null) {
            Button(
                onClick = onActionClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .height(32.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeColors.primaryColor,
                    contentColor = themeColors.textColor
                )
            ) {
                Text(text = buttonText, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = themeColors.textColor,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Today, $startTime - $endTime",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = themeColors.textColor.copy(alpha = 0.9f)
                ),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium.copy(color = themeColors.textSecondaryColor),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EpgChannelRow(
    channel: EPGChannel,
    eventLayouts: List<EventLayout>,
    horizontalOffset: Float,
    currentTime: Long,
    themeColors: EpgThemeColors,
    onChannelClick: () -> Unit,
    onChannelLongClick: (() -> Unit)? = null,
    onEventSelected: (EPGEvent) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ChannelHeight)
    ) {
        ChannelHeader(
            name = channel.name,
            logoUrl = channel.imageURL,
            themeColors = themeColors,
            onClick = onChannelClick,
            onLongClick = onChannelLongClick,
            modifier = Modifier.width(ChannelHeaderWidth)
        )
        ProgramsCanvas(
            eventLayouts = eventLayouts,
            horizontalOffset = horizontalOffset,
            currentTime = currentTime,
            themeColors = themeColors,
            hasCatchUp = channel.hasCatchUp,
            modifier = Modifier.fillMaxSize(),
            onEventClick = onEventSelected
        )
    }
}

@Composable
private fun ProgramsCanvas(
    eventLayouts: List<EventLayout>,
    horizontalOffset: Float,
    currentTime: Long,
    themeColors: EpgThemeColors,
    hasCatchUp: Boolean = false,
    modifier: Modifier = Modifier,
    onEventClick: (EPGEvent) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val programPaddingPx = with(LocalDensity.current) { ProgramPadding.toPx() }
    val cornerRadius = CornerRadius(with(LocalDensity.current) { ProgramCornerRadius.toPx() })
    val textStyle = remember(themeColors) {
        TextStyle(color = themeColors.textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }

    Canvas(
        modifier = modifier
            .pointerInput(horizontalOffset, eventLayouts) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        val absoluteTapX = tapOffset.x + horizontalOffset
                        eventLayouts.find { layout ->
                            absoluteTapX in layout.xOffsetPx.toFloat()..(layout.xOffsetPx + layout.widthPx).toFloat()
                        }?.let { onEventClick(it.epgEvent) }
                    }
                )
            }
    ) {
        clipRect {
            val firstVisibleIndex = eventLayouts.indexOfFirst {
                it.xOffsetPx + it.widthPx >= horizontalOffset
            }.coerceAtLeast(0)

            for (i in firstVisibleIndex until eventLayouts.size) {
                val layout = eventLayouts[i]
                val itemX = layout.xOffsetPx - horizontalOffset
                if (itemX > size.width) break
                val isCurrent = currentTime in layout.epgEvent.start..layout.epgEvent.end
                val isPast = currentTime > layout.epgEvent.end
                val pastAlpha = if (isPast && hasCatchUp) 0.85f else if (isPast) 0.5f else 1f
                val bgColor = when {
                    isCurrent -> themeColors.backgroundSecondary
                    else -> themeColors.backgroundSecondary.copy(alpha = pastAlpha)
                }
                val programWidth = layout.widthPx.toFloat() - (2 * programPaddingPx)
                if (programWidth <= 0) continue
                drawRoundRect(
                    color = bgColor,
                    topLeft = Offset(itemX + programPaddingPx, programPaddingPx),
                    size = Size(programWidth, size.height - (2 * programPaddingPx)),
                    cornerRadius = cornerRadius
                )

                // Replay affordance on past programmes for catch-up channels.
                if (isPast && hasCatchUp) {
                    val iconSize = 14.dp.toPx()
                    val iconPadding = 4.dp.toPx()
                    val iconX = itemX + programPaddingPx + programWidth - iconSize - iconPadding
                    val iconY = programPaddingPx + iconPadding
                    if (iconX > itemX + programPaddingPx + iconPadding) {
                        drawCircle(
                            color = themeColors.primaryColor.copy(alpha = 0.9f),
                            radius = iconSize / 2f,
                            center = Offset(iconX + iconSize / 2f, iconY + iconSize / 2f)
                        )
                        val arcRadius = iconSize * 0.3f
                        val arcCenter = Offset(iconX + iconSize / 2f, iconY + iconSize / 2f)
                        drawArc(
                            color = themeColors.textColor,
                            startAngle = -90f,
                            sweepAngle = 270f,
                            useCenter = false,
                            topLeft = Offset(arcCenter.x - arcRadius, arcCenter.y - arcRadius),
                            size = Size(arcRadius * 2, arcRadius * 2),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }

                val textMaxWidth = (layout.widthPx - (4 * programPaddingPx)).toInt()
                if (textMaxWidth > 0) {
                    val adjustedTextStyle =
                        if (isPast) textStyle.copy(color = themeColors.textColor.copy(alpha = pastAlpha))
                        else textStyle
                    val textLayoutResult = textMeasurer.measure(
                        text = AnnotatedString(layout.epgEvent.title),
                        style = adjustedTextStyle,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        constraints = Constraints.fixedWidth(textMaxWidth)
                    )
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            itemX + (2 * programPaddingPx),
                            (size.height - textLayoutResult.size.height) / 2
                        )
                    )
                }

                if (isCurrent) {
                    val span = (layout.epgEvent.end - layout.epgEvent.start).toFloat()
                    val progress =
                        if (span <= 0f) 0f
                        else ((currentTime - layout.epgEvent.start).toFloat() / span).coerceIn(0f, 1f)
                    val progressBarWidth = programWidth * progress
                    if (progressBarWidth > 0) {
                        drawRoundRect(
                            color = themeColors.primaryColor,
                            topLeft = Offset(
                                itemX + programPaddingPx,
                                size.height - programPaddingPx - 3.dp.toPx()
                            ),
                            size = Size(progressBarWidth, 3.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelHeader(
    name: String,
    logoUrl: String?,
    themeColors: EpgThemeColors,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxHeight()
            .pointerInput(onLongClick) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick?.invoke() }
                )
            }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        coil3.compose.AsyncImage(
            model = logoUrl,
            contentDescription = name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(themeColors.backgroundSecondary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            color = themeColors.textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DateHeader(
    currentTime: Long,
    themeColors: EpgThemeColors,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(start = 4.dp, end = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = EpgUtils.formatDayAndMonth(currentTime),
            color = themeColors.textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            textAlign = TextAlign.Start,
            maxLines = 2,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun TimelineCanvas(
    intervals: Int,
    startTime: Long,
    currentTime: Long,
    themeColors: EpgThemeColors,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val timeIntervalWidthPx = with(LocalDensity.current) { TimeIntervalWidth.toPx() }
    val textStyle = remember(themeColors) {
        TextStyle(
            textAlign = TextAlign.Start,
            color = themeColors.textColor.copy(alpha = 0.9f),
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
    val paddingStartPx = with(LocalDensity.current) { 4.dp.toPx() }

    Canvas(modifier = modifier) {
        for (i in 0 until intervals) {
            val timeMillis = startTime + i * 30L * 60_000L
            val formattedTime = EpgUtils.getShortTime(timeMillis)
            val isCurrentInterval =
                timeMillis <= currentTime && timeMillis + 30L * 60_000L > currentTime
            val textColor =
                if (isCurrentInterval) themeColors.textColor else themeColors.textSecondaryColor

            val textLayoutResult = textMeasurer.measure(
                text = AnnotatedString(formattedTime),
                style = textStyle.copy(color = textColor)
            )

            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    x = i * timeIntervalWidthPx + paddingStartPx,
                    y = (size.height - textLayoutResult.size.height) / 2
                )
            )
        }
    }
}

@Composable
private fun CurrentTimeIndicator(themeColors: EpgThemeColors, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val timelineHeightPx = with(density) { TimelineHeight.toPx() }
    val strokeWidthPx = with(density) { 2.dp.toPx() }
    val radiusPx = with(density) { 5.dp.toPx() }

    val infiniteTransition = rememberInfiniteTransition(label = "epgNowIndicator")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "epgNowAlpha"
    )

    Canvas(modifier = modifier.fillMaxHeight()) {
        drawLine(
            color = themeColors.primaryColor.copy(alpha = alpha),
            start = Offset(x = 0f, y = -timelineHeightPx),
            end = Offset(x = 0f, y = size.height),
            strokeWidth = strokeWidthPx
        )
        drawCircle(
            color = themeColors.primaryColor.copy(alpha = alpha),
            radius = radiusPx,
            center = Offset(x = 0f, y = 0f)
        )
    }
}
