package pt.hitv.feature.settings.options.options.more

import androidx.compose.ui.graphics.vector.ImageVector

data class FeatureItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onClick: (() -> Unit)? = null,
    val isToggle: Boolean = false,
    val isExpanded: Boolean = false,
    val highlight: FeatureHighlight = FeatureHighlight.NONE,
    val requiresPremium: Boolean = false
)

enum class FeatureHighlight { NONE, IMPORTANT, PREMIUM }

data class FeatureGroup(
    val title: String,
    val items: List<FeatureItem>
)
