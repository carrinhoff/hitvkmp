package pt.hitv.feature.settings.options.feedback

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pt.hitv.core.model.enums.SuggestionCategory
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * TV-specific suggestion screen with split layout for Android TV.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionScreenTv(
    onSubmitClick: (category: SuggestionCategory, text: String) -> Unit,
    onNavigateBack: () -> Unit,
    isSubmitting: Boolean,
    titleText: String = "Feedback & Suggestions",
    promptHeader: String = "Share your ideas",
    categoryLabel: String = "Category",
    textFieldLabel: String = "Write your suggestion...",
    submitButtonText: String = "Submit",
    submittingText: String = "Submitting...",
    backDescription: String = "Back",
    oneWayNotice: String = "This is one-way feedback."
) {
    val themeColors = getThemeColors()
    var suggestionText by remember { mutableStateOf("") }
    val categories = SuggestionCategory.entries.toList()
    var selectedCategory by remember { mutableStateOf(SuggestionCategory.OTHER) }
    val focusRequester = remember { FocusRequester() }
    val minLength = 10
    val maxLength = 1000
    val isSubmitEnabled = suggestionText.length > minLength && !isSubmitting

    LaunchedEffect(Unit) { delay(300); focusRequester.requestFocus() }

    Scaffold(
        containerColor = themeColors.backgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text(titleText, color = themeColors.textColor) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backDescription, tint = themeColors.textColor) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = themeColors.backgroundSecondary)
            )
        }
    ) { paddingValues ->
        Row(modifier = Modifier.padding(paddingValues).fillMaxSize().padding(horizontal = 48.dp, vertical = 32.dp), horizontalArrangement = Arrangement.spacedBy(48.dp)) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, tint = themeColors.primaryColor, modifier = Modifier.size(80.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = promptHeader, color = themeColors.textColor, fontSize = 22.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = oneWayNotice, color = themeColors.textColor.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            }
            Column(modifier = Modifier.weight(1.5f), horizontalAlignment = Alignment.Start) {
                Text(text = categoryLabel, color = themeColors.textColor.copy(alpha = 0.9f), fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    categories.forEachIndexed { index, category ->
                        FilterChip(
                            selected = (category == selectedCategory), onClick = { selectedCategory = category },
                            label = { Text(category.displayText, fontSize = 16.sp) },
                            modifier = if (index == 0) Modifier.focusRequester(focusRequester) else Modifier,
                            colors = FilterChipDefaults.filterChipColors(containerColor = themeColors.backgroundSecondary, labelColor = themeColors.textColor, selectedContainerColor = themeColors.primaryColor, selectedLabelColor = themeColors.textColor)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = suggestionText, onValueChange = { if (it.length <= maxLength) suggestionText = it },
                    label = { Text(textFieldLabel) }, minLines = 8, maxLines = 8, modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text(text = "${suggestionText.length}/$maxLength", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColors.primaryColor, unfocusedBorderColor = themeColors.textColor.copy(alpha = 0.5f), cursorColor = themeColors.primaryColor, focusedTextColor = themeColors.textColor, unfocusedTextColor = themeColors.textColor)
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { onSubmitClick(selectedCategory, suggestionText) }, enabled = isSubmitEnabled,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColors.primaryColor, disabledContainerColor = themeColors.textColor.copy(alpha = 0.3f))
                ) { Text(text = if (isSubmitting) submittingText else submitButtonText, fontSize = 18.sp) }
            }
        }
    }
}
