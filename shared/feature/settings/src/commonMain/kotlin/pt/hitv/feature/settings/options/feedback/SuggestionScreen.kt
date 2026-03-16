package pt.hitv.feature.settings.options.feedback

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.hitv.core.model.enums.SuggestionCategory
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * Suggestion screen for mobile. Strings are passed as parameters instead of stringResource.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionScreen(
    onSubmitClick: (category: SuggestionCategory, text: String) -> Unit,
    onNavigateBack: () -> Unit,
    isSubmitting: Boolean,
    titleText: String = "Feedback & Suggestions",
    promptHeader: String = "Share your ideas to help us improve",
    categoryLabel: String = "Category",
    textFieldLabel: String = "Write your suggestion...",
    submitButtonText: String = "Submit",
    submittingText: String = "Submitting...",
    backDescription: String = "Back",
    oneWayNotice: String = "This is one-way feedback. We read every message.",
    minLengthRequirement: String = "Minimum 10 characters"
) {
    val themeColors = getThemeColors()
    var suggestionText by remember { mutableStateOf("") }
    val categories = SuggestionCategory.entries.toList()
    var selectedCategory by remember { mutableStateOf(SuggestionCategory.OTHER) }
    val minLength = 10
    val maxLength = 1000
    val isSubmitEnabled = suggestionText.length > minLength && !isSubmitting

    Scaffold(
        containerColor = themeColors.backgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text(titleText, color = themeColors.textColor) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backDescription, tint = themeColors.textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = themeColors.backgroundSecondary)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues).fillMaxSize().imePadding().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, tint = themeColors.primaryColor, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = promptHeader, color = themeColors.textColor, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 40.dp))
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = categoryLabel, color = themeColors.textColor, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { category ->
                    FilterChip(
                        selected = (category == selectedCategory),
                        onClick = { selectedCategory = category },
                        label = { Text(category.displayText) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = themeColors.backgroundSecondary, labelColor = themeColors.textColor,
                            selectedContainerColor = themeColors.primaryColor, selectedLabelColor = themeColors.textColor
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = themeColors.textColor, modifier = Modifier.size(16.dp))
                Text(text = oneWayNotice, color = themeColors.textColor, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedTextField(
                value = suggestionText, onValueChange = { if (it.length <= maxLength) suggestionText = it },
                label = { Text(textFieldLabel) }, minLines = 5,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                supportingText = { Text(text = "${suggestionText.length}/$maxLength", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColors.primaryColor, unfocusedBorderColor = themeColors.textColor,
                    focusedLabelColor = themeColors.primaryColor, unfocusedLabelColor = themeColors.textColor,
                    cursorColor = themeColors.primaryColor, focusedTextColor = themeColors.textColor, unfocusedTextColor = themeColors.textColor
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            if (suggestionText.length <= minLength) {
                Text(text = minLengthRequirement, color = themeColors.textColor, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            }
            Button(
                onClick = { onSubmitClick(selectedCategory, suggestionText) }, enabled = isSubmitEnabled,
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primaryColor, disabledContainerColor = themeColors.textColor.copy(alpha = 0.5f))
            ) { Text(text = if (isSubmitting) submittingText else submitButtonText, fontSize = 16.sp) }
        }
    }
}
