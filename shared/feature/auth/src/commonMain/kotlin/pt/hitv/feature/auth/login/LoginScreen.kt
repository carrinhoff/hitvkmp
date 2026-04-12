package pt.hitv.feature.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.core.common.analytics.ScreenName
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.model.UserCredentials
import pt.hitv.feature.auth.login.components.ErrorDialog
import pt.hitv.feature.auth.util.LoginValidator

/**
 * KMP LoginScreen — faithful port of the original PortraitLoginLayoutWithTabs.
 *
 * Field order matches original: Username, Password, Server URL
 * Uses filled TextFields with backgroundSecondary container color.
 * TabRow for Xtream/M3U switching with primary color indicator.
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    analyticsHelper: AnalyticsHelper,
    preferencesHelper: PreferencesHelper,
    onLoginSuccess: () -> Unit
) {
    val themeColors = getThemeColors()
    val uiState by viewModel.uiState.collectAsState()
    val signInResult = uiState.signInResult
    val savedUserId = uiState.savedUserId
    val m3uResult = uiState.m3uResult
    val isLoading = uiState.isLoading
    val saveCredentialsError = uiState.saveCredentialsError

    // Error dialog state
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Form fields — pre-filled with debug credentials
    var username by remember { mutableStateOf("b7be78a330") }
    var password by remember { mutableStateOf("8ba28474b8") }
    var url by remember { mutableStateOf("http://xdooh.com/") }

    // Analytics
    LaunchedEffect(Unit) {
        analyticsHelper.logScreenView(ScreenName.LOGIN, "LoginScreen")
    }

    // Error dialog
    if (showErrorDialog) {
        ErrorDialog(
            errorType = LoginValidator.ErrorType.UNKNOWN_ERROR,
            message = errorMessage,
            onDismiss = { showErrorDialog = false }
        )
    }

    // Handle sign-in result
    LaunchedEffect(signInResult) {
        when (signInResult) {
            is SignInResult.SignInSuccess -> {
                val response = signInResult.response
                response.userInfo?.let { userInfo ->
                    if (userInfo.username != null && userInfo.password != null) {
                        val userHostUrl = preferencesHelper.getHostUrl()
                        preferencesHelper.setStoredTag("expirationDate", userInfo.expDate ?: "")
                        preferencesHelper.setStoredTag("username", userInfo.username)
                        preferencesHelper.setStoredTag("password", userInfo.password)
                        preferencesHelper.setStoredTag("output", userInfo.allowedOutputFormats?.firstOrNull() ?: "")

                        viewModel.saveCredentials(
                            UserCredentials(
                                userId = 0,
                                username = userInfo.username,
                                password = userInfo.password,
                                hostname = userHostUrl,
                                expirationDate = userInfo.expDate,
                                allowedOutputFormats = userInfo.allowedOutputFormats
                            )
                        )
                        analyticsHelper.logLogin("XtreamCredentials")
                    } else {
                        viewModel.changeLoadingState(false)
                        errorMessage = "Server returned incomplete information. Please verify your service provider settings."
                        showErrorDialog = true
                    }
                } ?: run {
                    viewModel.changeLoadingState(false)
                    errorMessage = "Server returned incomplete login information. Please try again or contact your provider."
                    showErrorDialog = true
                }
            }
            is SignInResult.SignInError -> {
                viewModel.changeLoadingState(false)
                errorMessage = signInResult.message
                showErrorDialog = true
            }
            null -> {}
        }
    }

    // Handle saved userId -> navigate to main
    LaunchedEffect(savedUserId) {
        savedUserId?.let { onLoginSuccess() }
    }

    // Handle M3U result
    LaunchedEffect(m3uResult) {
        when (m3uResult) {
            is M3uResult.M3uSuccess -> onLoginSuccess()
            is M3uResult.M3uError -> {
                errorMessage = m3uResult.message
                showErrorDialog = true
            }
            null -> {}
        }
    }

    // Handle save credentials error
    LaunchedEffect(saveCredentialsError) {
        saveCredentialsError?.let {
            errorMessage = "Failed to save account. Please try again."
            showErrorDialog = true
        }
    }

    // === UI — Matches original PortraitLoginLayoutWithTabs ===

    Box(modifier = Modifier.fillMaxSize()) {
        var selectedTabIndex by remember { mutableIntStateOf(0) }
        val tabTitles = listOf("Xtream Login", "M3U URL")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(themeColors.backgroundPrimary)
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // WelcomeSection — matches original
            Text(
                text = "Welcome to AxonStream IPTV",
                fontSize = 24.sp,
                style = MaterialTheme.typography.headlineSmall,
                color = themeColors.textColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please login to continue using our app",
                fontSize = 16.sp,
                style = MaterialTheme.typography.bodyLarge,
                color = themeColors.textColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // TabRow — matches original exactly
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = themeColors.primaryColor,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = themeColors.primaryColor
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                title,
                                color = if (selectedTabIndex == index) themeColors.textColor else Color.Gray
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Content area
            when (selectedTabIndex) {
                0 -> CredentialsForm(
                    username = username,
                    onUsernameChange = { username = it },
                    password = password,
                    onPasswordChange = { password = it },
                    url = url,
                    onUrlChange = { url = it.replace(Regex("[\\n\\r]+"), "").trim() },
                    onLoginClick = {
                        val validation = LoginValidator.validateXtreamCredentials(
                            KmpStringResources, url, username, password
                        )
                        when (validation) {
                            is LoginValidator.ValidationResult.Success -> {
                                val sanitizedUrl = LoginValidator.sanitizeUrl(url)
                                val sanitizedUsername = LoginValidator.sanitizeUsername(username)
                                val sanitizedPassword = LoginValidator.sanitizePassword(password)
                                preferencesHelper.setStoredTag("hostUrl", sanitizedUrl)
                                viewModel.signIn(sanitizedUsername, sanitizedPassword)
                            }
                            is LoginValidator.ValidationResult.Error -> {
                                errorMessage = validation.message
                                showErrorDialog = true
                            }
                        }
                    },
                    themeColors = themeColors
                )

                1 -> M3uForm(
                    onConfirm = { m3uLink, playlistName, epgUrl ->
                        val validation = LoginValidator.validateM3uPlaylist(
                            KmpStringResources, playlistName, m3uLink
                        )
                        when (validation) {
                            is LoginValidator.ValidationResult.Success -> {
                                val sanitizedName = LoginValidator.sanitizePlaylistName(playlistName)
                                viewModel.processDirectM3uUrl(sanitizedName, m3uLink, epgUrl)
                            }
                            is LoginValidator.ValidationResult.Error -> {
                                errorMessage = validation.message
                                showErrorDialog = true
                            }
                        }
                    },
                    onValidationError = { msg ->
                        errorMessage = msg
                        showErrorDialog = true
                    },
                    themeColors = themeColors
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // LoginLinks — matches original (Suggestions & Discord)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 16.dp, top = 24.dp)
            ) {
                TextButton(
                    onClick = { /* TODO: Navigate to feedback */ },
                    colors = ButtonDefaults.textButtonColors(contentColor = themeColors.textColor)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Suggestions & Feedback")
                }

                OutlinedButton(
                    onClick = { /* TODO: Open Discord link */ },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = themeColors.backgroundSecondary.copy(alpha = 0.3f),
                        contentColor = themeColors.textColor.copy(alpha = 0.7f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = themeColors.textColor.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(52.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Join Discord Community",
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Loading overlay
        if (isLoading && (uiState.isSigningIn || uiState.isProcessingM3u || uiState.isSavingCredentials)) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = themeColors.primaryColor)
            }
        }
    }
}

// === CredentialsForm — matches original field order: Username, Password, Server URL ===

@Composable
private fun CredentialsForm(
    username: String, onUsernameChange: (String) -> Unit,
    password: String, onPasswordChange: (String) -> Unit,
    url: String, onUrlChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    themeColors: pt.hitv.core.designsystem.theme.ThemeManager.AppTheme,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val textFieldColors = getTextFieldColors(themeColors)

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = textFieldColors
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            colors = textFieldColors
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            label = { Text("Server URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            colors = textFieldColors
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onLoginClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primaryColor)
        ) {
            Text("Sign In", fontSize = 18.sp, color = themeColors.textColor)
        }
    }
}

// === M3uForm — matches original ===

@Composable
private fun M3uForm(
    onConfirm: (m3uLink: String, playlistName: String, epgUrl: String) -> Unit,
    onValidationError: (String) -> Unit,
    themeColors: pt.hitv.core.designsystem.theme.ThemeManager.AppTheme,
    modifier: Modifier = Modifier
) {
    var m3uLink by remember { mutableStateOf("") }
    var playlistName by remember { mutableStateOf("") }
    var epgUrl by remember { mutableStateOf("") }
    val textFieldColors = getTextFieldColors(themeColors)

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = playlistName,
            onValueChange = { playlistName = it },
            label = { Text("Playlist Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = textFieldColors
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = m3uLink,
            onValueChange = { m3uLink = it },
            label = { Text("M3U URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
            colors = textFieldColors
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = epgUrl,
            onValueChange = { epgUrl = it },
            label = { Text("EPG URL (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            colors = textFieldColors
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                if (m3uLink.isBlank() || playlistName.isBlank()) {
                    onValidationError("Please fill in all required fields")
                } else {
                    onConfirm(m3uLink, playlistName, epgUrl)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.buttonColors(containerColor = themeColors.primaryColor)
        ) {
            Text("Add Playlist", fontSize = 18.sp, color = themeColors.textColor)
        }
    }
}

// === TextField colors — matches original getTextFieldColors() exactly ===

@Composable
private fun getTextFieldColors(themeColors: pt.hitv.core.designsystem.theme.ThemeManager.AppTheme): TextFieldColors {
    return TextFieldDefaults.colors(
        focusedTextColor = themeColors.textColor,
        unfocusedTextColor = themeColors.textColor,
        focusedContainerColor = themeColors.backgroundSecondary,
        unfocusedContainerColor = themeColors.backgroundSecondary,
        cursorColor = themeColors.primaryColor,
        focusedIndicatorColor = themeColors.primaryColor,
        unfocusedIndicatorColor = Color.Transparent,
        focusedLabelColor = themeColors.textColor,
        unfocusedLabelColor = themeColors.textColor
    )
}

// === KMP String Resources for LoginValidator ===

private object KmpStringResources : LoginValidator.StringResources {
    override val urlRequired = "Please enter a server URL"
    override val usernameRequired = "Please enter a username"
    override val passwordRequired = "Please enter a password"
    override val invalidUrlFormat = "Invalid URL format. Please check the server address."
    override val playlistNameRequired = "Please enter a playlist name"
    override val m3uUrlRequired = "Please enter an M3U URL"
    override val playlistInvalidCharacters = "Playlist name contains invalid characters"
    override val authInvalidCredentials = "Authentication failed. Please check your username and password."
    override val authAccessDenied = "Access denied. Your account may be expired or suspended."
    override val networkCannotReachServer = "Cannot reach the server. Please check the address and your internet connection."
    override val networkTimeout = "Connection timed out. The server may be unreachable."
    override val networkConnectionRefused = "Connection refused. The server may be down."
    override val networkNoInternet = "No internet connection. Please check your network settings."
    override val sslCertificate = "SSL certificate error. The server's security certificate is invalid."
    override val serverInternal = "Server error. Please try again later."
    override val serverUnavailable = "Server is temporarily unavailable. Please try again later."
    override val serverNotFound = "Server not found. Please check the URL."
    override val m3uEmpty = "The M3U playlist is empty."
    override val m3uInvalidFormat = "Invalid M3U format."
    override val serverEmptyResponse = "Server returned an empty response."
    override val genericError = "An unexpected error occurred. Please try again."
}
