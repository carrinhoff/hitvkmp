package pt.hitv.feature.settings.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.core.common.analytics.ScreenName
import pt.hitv.feature.settings.options.feedback.SuggestionScreen
import pt.hitv.feature.settings.options.feedback.SuggestionViewModel

/**
 * Voyager wrapper for [SuggestionScreen], registered against `HitvScreen.FEEDBACK`.
 *
 * Without this registration `ScreenRegistry.create(HitvScreen.FEEDBACK)` hits the
 * `?: error(...)` in `HitvNavigation.kt:33-36` and the "Feedback & Support" row in More
 * Options kills the app — on iOS as an uncaught Kotlin/Native exception.
 *
 * Ported from the original's `FeedbackRoute`
 * (hitv/app/.../navigation/screens/SettingsRoutes.kt:105-160): same screen-view analytics,
 * the same submissionStatus true→notify-and-pop / false→notify-and-stay handling, and the
 * same 10-character minimum guard before submitting. Toasts become a snackbar, since Toast
 * is Android-only.
 *
 * Note: [SuggestionViewModel] is bound in `SettingsModule.kt:13` as `SuggestionViewModel()`,
 * i.e. with its default no-op `submitFeedback` that always returns `false`, so submission
 * currently always reports failure. The original posted to Firebase Firestore and Firebase
 * was intentionally dropped from the port, so choosing a replacement backend is a product
 * decision — tracked in KMP_MIGRATION_AUDIT.md §5. Registering the screen is still strictly
 * better than crashing.
 */
class FeedbackVoyagerScreen : Screen {
    override val key = "Feedback"

    @Composable
    override fun Content() {
        val viewModel: SuggestionViewModel = koinInject()
        val analyticsHelper: AnalyticsHelper = koinInject()
        val navigator = LocalNavigator.currentOrThrow
        val uiState by viewModel.uiState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            analyticsHelper.logScreenView(ScreenName.FEEDBACK, "SuggestionScreen")
        }

        val submissionStatus = uiState.submissionStatus
        LaunchedEffect(submissionStatus) {
            when (submissionStatus) {
                true -> {
                    snackbarHostState.showSnackbar("Feedback received. Thank you!")
                    viewModel.onSubmissionHandled()
                    navigator.pop()
                }
                false -> {
                    snackbarHostState.showSnackbar("Could not submit your feedback. Please try again later.")
                    viewModel.onSubmissionHandled()
                }
                null -> Unit // Initial state — nothing to report.
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            SuggestionScreen(
                onSubmitClick = { category, text ->
                    // Matches the original's guard at SettingsRoutes.kt:148.
                    if (text.length > 10) {
                        viewModel.submitSuggestion(category, text)
                    }
                },
                onNavigateBack = { navigator.pop() },
                isSubmitting = uiState.isSubmitting
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
