package pt.hitv.feature.settings.options.options.about

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import pt.hitv.core.common.AppInfoProvider
import pt.hitv.core.designsystem.theme.getThemeColors

private const val PRIVACY_POLICY_URL = "https://hitv.pt/privacy"
private const val WEBSITE_URL = "https://hitv.pt"

/**
 * Lightweight about screen. Shows app version, a "powered by" credits line and
 * links to the privacy policy / website via [UrlOpener].
 */
class AboutScreen : Screen {
    override val key = "About"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val appInfo: AppInfoProvider = koinInject()
        val uriHandler = LocalUriHandler.current
        AboutContent(
            versionName = appInfo.versionName,
            versionCode = appInfo.versionCode,
            onPrivacyPolicyClick = { runCatching { uriHandler.openUri(PRIVACY_POLICY_URL) } },
            onWebsiteClick = { runCatching { uriHandler.openUri(WEBSITE_URL) } },
            onBack = { navigator.pop() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutContent(
    versionName: String,
    versionCode: Int,
    onPrivacyPolicyClick: () -> Unit,
    onWebsiteClick: () -> Unit,
    onBack: () -> Unit
) {
    val themeColors = getThemeColors()
    val backgroundColor = themeColors.backgroundPrimary
    val secondaryBackgroundColor = themeColors.backgroundSecondary
    val primaryColor = themeColors.primaryColor
    val textColor = Color.White
    val textSecondary = Color.White.copy(alpha = 0.7f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", color = textColor, fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "HITV",
                color = textColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Version $versionName ($versionCode)",
                color = primaryColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Powered by Kotlin Multiplatform and Compose.",
                color = textSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            AboutLinkRow(
                label = "Privacy Policy",
                icon = Icons.Rounded.PrivacyTip,
                primaryColor = primaryColor,
                textColor = textColor,
                textSecondary = textSecondary,
                onClick = onPrivacyPolicyClick
            )
            Spacer(modifier = Modifier.height(12.dp))
            AboutLinkRow(
                label = "Website",
                icon = Icons.Rounded.Public,
                primaryColor = primaryColor,
                textColor = textColor,
                textSecondary = textSecondary,
                onClick = onWebsiteClick
            )
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun AboutLinkRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    primaryColor: Color,
    textColor: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(textColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = primaryColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = textSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}
