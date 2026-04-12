package pt.hitv.feature.premium.mobile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.feature.premium.CurrentPremiumCard
import pt.hitv.feature.premium.SubscriptionTier

// Product IDs matching the original BillingConstants
private const val ANNUAL_PREMIUM = "annual_premium"
private const val LIFETIME_PREMIUM = "lifetime_premium"

/**
 * Common mobile portrait premium layout, usable from all platforms.
 * Matches the original Android MobilePremiumPortrait visual design.
 */
@Composable
fun MobilePremiumPortraitCommon(
    onNavigateBack: () -> Unit,
    onPurchaseClick: (productId: String) -> Unit = {},
    isRootDestination: Boolean = false,
    scrollToTopSignal: Int = 0,
    hasAnnualPremium: Boolean = false,
    hasLifetimePremium: Boolean = false,
    isInTrial: Boolean = false,
    trialExpirationTime: Long? = null,
    annualPrice: String = "4,99 \u20AC",
    lifetimePrice: String = "9,99 \u20AC",
    // Text labels (defaults match English originals)
    headerTitle: String = "Upgrade to Premium",
    headerSubtitle: String = "Unlock exclusive features and personalize your experience",
    backDescription: String = "Back",
    annualLabel: String = "Annual Premium",
    lifetimeLabel: String = "Lifetime Premium",
    bestValueLabel: String = "BEST VALUE",
    perYearLabel: String = "/year",
    oneTimeLabel: String = "One-time payment",
    catchUpTvFeature: String = "\uD83D\uDCFA Catch-Up TV",
    unlockThemesFeature: String = "\u2728 Unlock 5 premium themes",
    parentalControlFeature: String = "\uD83D\uDD10 Parental Control",
    cleanNavBarFeature: String = "\uD83E\uDDF9 Clean navigation bar (no Premium tab)",
    payOnceFeature: String = "Pay once, enjoy forever",
    subscribeNowText: String = "Subscribe Now",
    getLifetimeText: String = "Get Lifetime",
    alreadyPremiumText: String = "You're already a premium member!"
) {
    val themeColors = getThemeColors()
    val backgroundColor = themeColors.backgroundPrimary
    val secondaryBackgroundColor = themeColors.backgroundSecondary
    val primaryColor = themeColors.primaryColor
    val textColor = themeColors.textColor
    val textSecondaryColor = themeColors.textColor.copy(alpha = 0.7f)

    val listState = rememberLazyListState()

    // Scroll to top handling
    var lastScrollToTopSignal by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0 && scrollToTopSignal != lastScrollToTopSignal) {
            lastScrollToTopSignal = scrollToTopSignal
            listState.animateScrollToItem(index = 0, scrollOffset = 0)
        }
    }

    // Fade-in animation
    var isVisible by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(600, delayMillis = 100),
        label = "contentFade"
    )
    LaunchedEffect(Unit) { isVisible = true }

    // Build subscription tiers matching the original
    val subscriptionTiers = remember(annualPrice, lifetimePrice) {
        listOf(
            SubscriptionTier(
                id = "annual",
                title = annualLabel,
                price = annualPrice,
                period = perYearLabel,
                features = listOf(
                    catchUpTvFeature,
                    unlockThemesFeature,
                    parentalControlFeature,
                    cleanNavBarFeature
                ),
                badge = bestValueLabel,
                badgeColor = Color(0xFFFF6B35),
                productId = ANNUAL_PREMIUM,
                isHighlighted = true
            ),
            SubscriptionTier(
                id = "lifetime",
                title = lifetimeLabel,
                price = lifetimePrice,
                period = oneTimeLabel,
                features = listOf(
                    catchUpTvFeature,
                    unlockThemesFeature,
                    parentalControlFeature,
                    cleanNavBarFeature,
                    payOnceFeature
                ),
                productId = LIFETIME_PREMIUM,
                isHighlighted = false
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor,
                        secondaryBackgroundColor,
                        backgroundColor.copy(alpha = 0.9f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
                .alpha(contentAlpha)
        ) {
            // Header
            MobilePremiumHeader(
                onNavigateBack = onNavigateBack,
                primaryColor = primaryColor,
                textColor = textColor,
                textSecondaryColor = textSecondaryColor,
                isRootDestination = isRootDestination,
                headerTitle = headerTitle,
                headerSubtitle = headerSubtitle,
                backDescription = backDescription
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Content
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Premium status card if already subscribed
                if (hasLifetimePremium || hasAnnualPremium) {
                    item {
                        CurrentPremiumCard(
                            hasLifetime = hasLifetimePremium,
                            hasAnnual = hasAnnualPremium,
                            primaryColor = primaryColor,
                            textColor = textColor,
                            isInTrial = isInTrial,
                            trialExpirationTime = trialExpirationTime
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Subscription tiers
                items(subscriptionTiers) { tier ->
                    MobileSubscriptionTierCard(
                        tier = tier,
                        primaryColor = primaryColor,
                        textColor = textColor,
                        textSecondaryColor = textSecondaryColor,
                        isOwned = when (tier.productId) {
                            LIFETIME_PREMIUM -> hasLifetimePremium
                            ANNUAL_PREMIUM -> hasAnnualPremium
                            else -> false
                        },
                        onPurchaseClick = { onPurchaseClick(tier.productId) },
                        subscribeNowText = subscribeNowText,
                        getLifetimeText = getLifetimeText,
                        alreadyPremiumText = alreadyPremiumText
                    )
                }
            }
        }
    }
}

@Composable
private fun MobilePremiumHeader(
    onNavigateBack: () -> Unit,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    isRootDestination: Boolean,
    headerTitle: String,
    headerSubtitle: String,
    backDescription: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isRootDestination) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = textColor.copy(alpha = 0.08f),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .clickable(onClick = onNavigateBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = backDescription,
                    tint = textColor.copy(alpha = 0.9f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))
        }

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = headerTitle,
                    color = textColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = headerSubtitle,
                color = textSecondaryColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
private fun MobileSubscriptionTierCard(
    tier: SubscriptionTier,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    isOwned: Boolean,
    onPurchaseClick: () -> Unit,
    subscribeNowText: String,
    getLifetimeText: String,
    alreadyPremiumText: String,
    modifier: Modifier = Modifier
) {
    val cardBackgroundColor = when {
        tier.isHighlighted -> primaryColor.copy(alpha = 0.15f)
        else -> textColor.copy(alpha = 0.05f)
    }

    val borderColor = when {
        tier.isHighlighted -> primaryColor
        else -> textColor.copy(alpha = 0.15f)
    }

    val borderWidth = when {
        tier.isHighlighted -> 2.dp
        else -> 1.dp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(cardBackgroundColor, RoundedCornerShape(16.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tier.title,
                        color = textColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Column {
                        Text(
                            text = tier.price,
                            color = primaryColor,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                        if (tier.period.isNotEmpty()) {
                            Text(
                                text = tier.period,
                                color = textSecondaryColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }

                // Badge
                tier.badge?.let { badgeText ->
                    Box(
                        modifier = Modifier
                            .background(
                                tier.badgeColor ?: primaryColor,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Features
            tier.features.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = if (tier.isHighlighted) primaryColor else Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = feature,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Purchase button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(
                        if (isOwned) Color.Gray
                        else if (tier.isHighlighted) primaryColor
                        else primaryColor.copy(alpha = 0.85f),
                        RoundedCornerShape(12.dp)
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .then(
                        if (!isOwned) Modifier.clickable(onClick = onPurchaseClick)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isOwned) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = alreadyPremiumText,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = when (tier.id) {
                                "lifetime" -> getLifetimeText
                                else -> subscribeNowText
                            },
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
