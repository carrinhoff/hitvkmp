package pt.hitv.feature.premium

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

@Composable
fun TrialOfferDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    primaryColor: Color,
    textColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    titleText: String = "Try Premium Free",
    subtitleText: String = "Experience all premium features",
    feature1: String = "Premium themes",
    feature2: String = "Parental controls",
    feature3: String = "Clean navigation",
    feature4: String = "Early access",
    noChargeText: String = "No charge during trial period",
    cancelAnytimeText: String = "Cancel anytime before trial ends",
    startTrialText: String = "Start Free Trial",
    maybeLaterText: String = "Maybe Later"
) {
    var isVisible by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false)) {
        val slideOffset by animateFloatAsState(targetValue = if (isVisible) 0f else 1000f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "slideOffset")
        val alpha by animateFloatAsState(targetValue = if (isVisible) 1f else 0f, animationSpec = tween(300), label = "alpha")

        LaunchedEffect(Unit) { delay(300); isVisible = true }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f * alpha)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}))
            Box(modifier = Modifier.align(Alignment.Center).offset(y = slideOffset.dp).padding(horizontal = 24.dp, vertical = 24.dp)) {
                Card(modifier = modifier.fillMaxWidth().wrapContentHeight(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = backgroundColor), elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Brush.radialGradient(colors = listOf(primaryColor.copy(alpha = 0.3f), primaryColor.copy(alpha = 0.1f)))), contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Rounded.Star, contentDescription = null, tint = primaryColor, modifier = Modifier.size(32.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = titleText, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = subtitleText, color = textColor.copy(alpha = 0.7f), fontSize = 13.sp, lineHeight = 16.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                TrialFeatureItem(feature1, primaryColor, textColor); TrialFeatureItem(feature2, primaryColor, textColor)
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                TrialFeatureItem(feature3, primaryColor, textColor); TrialFeatureItem(feature4, primaryColor, textColor)
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Box(modifier = Modifier.fillMaxWidth().background(primaryColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = noChargeText, color = textColor, fontSize = 13.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = cancelAnytimeText, color = textColor.copy(alpha = 0.6f), fontSize = 12.sp, textAlign = TextAlign.Center)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(onClick = onAccept, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryColor), shape = RoundedCornerShape(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    Icon(imageVector = Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = startTrialText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                                Text(text = maybeLaterText, color = textColor.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrialFeatureItem(text: String, primaryColor: Color, textColor: Color) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
        Text(text = text, color = textColor, fontSize = 12.sp, lineHeight = 16.sp)
    }
}
