@file:OptIn(ExperimentalAnimationApi::class)

package pt.hitv.core.ui.components

import pt.hitv.core.designsystem.theme.ThemeManager

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import pt.hitv.core.designsystem.theme.getThemeColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Full-screen data loading dialog with circular progress, percentage, stage info, and cancel button.
 *
 * @param isVisible Whether the dialog is shown
 * @param percentage Progress percentage string (e.g., "45%")
 * @param stageTitle Current loading stage title
 * @param statusText Current status description
 * @param onCancelClick Callback when user cancels
 * @param cancelLabel Label for the cancel button
 * @param modifier Optional modifier
 */
@Composable
fun DataPercentageLoader(
    isVisible: Boolean,
    percentage: String,
    stageTitle: String,
    statusText: String,
    onCancelClick: () -> Unit,
    cancelLabel: String = "Cancel",
    modifier: Modifier = Modifier
) {
    val themeColors = getThemeColors()

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(500, easing = EaseOutCubic)),
        exit = fadeOut(animationSpec = tween(300, easing = EaseInCubic)),
        modifier = modifier
    ) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            // Modern glass-morphism background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                themeColors.backgroundPrimary,
                                themeColors.backgroundSecondary.copy(alpha = 0.95f),
                                themeColors.backgroundPrimary
                            )
                        )
                    )
            ) {
                // Animated background particles/dots
                AnimatedBackgroundDots(themeColors = themeColors)

                // Close button
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 24.dp, end = 24.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = themeColors.backgroundSecondary.copy(alpha = 0.8f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    IconButton(
                        onClick = onCancelClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = cancelLabel,
                            tint = themeColors.textColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Portrait layout - vertical arrangement
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = themeColors.backgroundSecondary.copy(alpha = 0.9f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Modern circular progress indicator
                        ModernCircularProgress(
                            percentage = percentage,
                            themeColors = themeColors
                        )

                        // Animated percentage text
                        AnimatedContent(
                            targetState = percentage,
                            transitionSpec = {
                                slideInVertically { -it } + fadeIn() togetherWith
                                    slideOutVertically { it } + fadeOut()
                            },
                            label = "percentage_animation"
                        ) { animatedPercentage ->
                            Text(
                                text = animatedPercentage,
                                color = themeColors.textColor,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Stage title
                        AnimatedContent(
                            targetState = stageTitle,
                            transitionSpec = {
                                slideInVertically { -it / 2 } + fadeIn() togetherWith
                                    slideOutVertically { it / 2 } + fadeOut()
                            },
                            label = "stage_animation"
                        ) { animatedStageTitle ->
                            Text(
                                text = animatedStageTitle,
                                color = themeColors.primaryColor,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Status text
                        AnimatedContent(
                            targetState = statusText,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(300)) togetherWith
                                    fadeOut(animationSpec = tween(300))
                            },
                            label = "status_animation"
                        ) { animatedStatusText ->
                            Text(
                                text = animatedStatusText,
                                color = themeColors.textColor.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernCircularProgress(
    percentage: String,
    themeColors: ThemeManager.AppTheme,
    modifier: Modifier = Modifier
) {
    val primaryColor = themeColors.primaryColor
    val backgroundColor = themeColors.backgroundPrimary

    val progress = percentage.replace("%", "").toFloatOrNull() ?: 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress / 100f,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "progress_animation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_animation"
    )

    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            drawCircle(
                color = backgroundColor.copy(alpha = 0.3f),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            if (animatedProgress > 0f) {
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            if (progress < 100f) {
                val dotCount = 8
                for (i in 0 until dotCount) {
                    val angle = (rotationAngle + (i * 45f)) * (kotlin.math.PI / 180.0).toFloat()
                    val dotRadius = 3.dp.toPx()
                    val circleRadius = radius + 12.dp.toPx()
                    val dotCenter = Offset(
                        center.x + circleRadius * cos(angle),
                        center.y + circleRadius * sin(angle)
                    )
                    val alpha = (sin((rotationAngle + i * 45f) * (kotlin.math.PI / 180.0).toFloat()) + 1f) / 2f
                    drawCircle(
                        color = primaryColor.copy(alpha = alpha * 0.6f),
                        radius = dotRadius,
                        center = dotCenter
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedBackgroundDots(themeColors: ThemeManager.AppTheme) {
    val infiniteTransition = rememberInfiniteTransition(label = "background_animation")

    val animatedOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset1"
    )

    val animatedOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -80f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset2"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val primaryColor = themeColors.primaryColor

        drawCircle(
            color = primaryColor.copy(alpha = 0.1f),
            radius = 40.dp.toPx(),
            center = Offset(size.width * 0.1f + animatedOffset1, size.height * 0.2f)
        )
        drawCircle(
            color = primaryColor.copy(alpha = 0.08f),
            radius = 60.dp.toPx(),
            center = Offset(size.width * 0.9f + animatedOffset2, size.height * 0.8f)
        )
        drawCircle(
            color = primaryColor.copy(alpha = 0.05f),
            radius = 80.dp.toPx(),
            center = Offset(size.width * 0.2f - animatedOffset1, size.height * 0.7f)
        )
    }
}

/**
 * EPG loading toaster notification.
 *
 * @param isVisible Whether the toaster is visible
 * @param message Status message text
 * @param titleLabel Title text for the toaster
 * @param modifier Optional modifier
 */
@Composable
fun EpgLoadingToaster(
    isVisible: Boolean,
    message: String,
    titleLabel: String = "EPG Update",
    modifier: Modifier = Modifier
) {
    val themeColors = getThemeColors()

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(350, easing = EaseOutCubic),
        label = "toaster_alpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 40f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "toaster_slide"
    )

    if (alpha > 0f) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(bottom = 104.dp)
                    .alpha(alpha)
                    .offset(y = offsetY.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, themeColors.textColor.copy(alpha = 0.12f)),
                colors = CardDefaults.cardColors(
                    containerColor = themeColors.backgroundSecondary.copy(alpha = 0.93f)
                ),
                elevation = CardDefaults.cardElevation(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = themeColors.primaryColor,
                                shape = CircleShape
                            )
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = titleLabel,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = themeColors.textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = message,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = themeColors.textColor.copy(alpha = 0.8f),
                            fontSize = 12.5.sp,
                            lineHeight = 15.sp,
                        )
                    }
                }
            }
        }
    }
}
