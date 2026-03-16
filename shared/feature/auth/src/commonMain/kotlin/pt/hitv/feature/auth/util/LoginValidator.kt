package pt.hitv.feature.auth.util

/**
 * Utility class for validating and sanitizing login inputs.
 * Pure Kotlin - no platform dependencies.
 */
object LoginValidator {

    /**
     * Sealed class for validation results with specific error types
     */
    sealed class ValidationResult {
        data object Success : ValidationResult()
        data class Error(val type: ErrorType, val message: String) : ValidationResult()
    }

    /**
     * Error types for better error handling and user feedback
     */
    enum class ErrorType {
        EMPTY_FIELD,
        INVALID_URL,
        INVALID_CREDENTIALS,
        NETWORK_ERROR,
        TIMEOUT_ERROR,
        SSL_ERROR,
        AUTH_ERROR,
        SERVER_ERROR,
        PARSING_ERROR,
        UNKNOWN_ERROR
    }

    /**
     * String resources interface for localized error messages
     */
    interface StringResources {
        val urlRequired: String
        val usernameRequired: String
        val passwordRequired: String
        val invalidUrlFormat: String
        val playlistNameRequired: String
        val m3uUrlRequired: String
        val playlistInvalidCharacters: String
        val authInvalidCredentials: String
        val authAccessDenied: String
        val networkCannotReachServer: String
        val networkTimeout: String
        val networkConnectionRefused: String
        val networkNoInternet: String
        val sslCertificate: String
        val serverInternal: String
        val serverUnavailable: String
        val serverNotFound: String
        val m3uEmpty: String
        val m3uInvalidFormat: String
        val serverEmptyResponse: String
        val genericError: String
    }

    /**
     * Validates Xtream Codes credentials
     */
    fun validateXtreamCredentials(
        strings: StringResources,
        url: String,
        username: String,
        password: String
    ): ValidationResult {
        if (url.isBlank()) {
            return ValidationResult.Error(ErrorType.EMPTY_FIELD, strings.urlRequired)
        }
        if (username.isBlank()) {
            return ValidationResult.Error(ErrorType.EMPTY_FIELD, strings.usernameRequired)
        }
        if (password.isBlank()) {
            return ValidationResult.Error(ErrorType.EMPTY_FIELD, strings.passwordRequired)
        }

        val urlValidation = validateUrl(strings, url)
        if (urlValidation !is ValidationResult.Success) {
            return urlValidation
        }

        val credentialsValidation = validateCredentials(strings, username, password)
        if (credentialsValidation !is ValidationResult.Success) {
            return credentialsValidation
        }

        return ValidationResult.Success
    }

    /**
     * Validates M3U playlist inputs
     */
    fun validateM3uPlaylist(
        strings: StringResources,
        playlistName: String,
        m3uUrl: String
    ): ValidationResult {
        if (playlistName.isBlank()) {
            return ValidationResult.Error(ErrorType.EMPTY_FIELD, strings.playlistNameRequired)
        }
        if (m3uUrl.isBlank()) {
            return ValidationResult.Error(ErrorType.EMPTY_FIELD, strings.m3uUrlRequired)
        }

        if (!isValidPlaylistName(playlistName)) {
            return ValidationResult.Error(ErrorType.INVALID_CREDENTIALS, strings.playlistInvalidCharacters)
        }

        val urlValidation = validateUrl(strings, m3uUrl, allowFileExtension = true)
        if (urlValidation !is ValidationResult.Success) {
            return urlValidation
        }

        return ValidationResult.Success
    }

    /**
     * Validates and sanitizes a URL
     */
    fun validateUrl(
        strings: StringResources,
        url: String,
        allowFileExtension: Boolean = false
    ): ValidationResult {
        val trimmedUrl = url.trim()

        if (trimmedUrl.length < 3) {
            return ValidationResult.Error(ErrorType.INVALID_URL, strings.invalidUrlFormat)
        }

        val urlPattern = Regex(
            "^(https?://)?([a-zA-Z0-9]([a-zA-Z0-9\\-]*[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}(:[0-9]+)?(/.*)?$|" +
                "^(https?://)?([0-9]{1,3}\\.){3}[0-9]{1,3}(:[0-9]+)?(/.*)?$"
        )

        if (!urlPattern.matches(trimmedUrl)) {
            return ValidationResult.Error(ErrorType.INVALID_URL, strings.invalidUrlFormat)
        }

        return ValidationResult.Success
    }

    /**
     * Validates username and password format
     */
    private fun validateCredentials(
        strings: StringResources,
        username: String,
        password: String
    ): ValidationResult {
        val trimmedUsername = username.trim()
        val trimmedPassword = password.trim()

        if (trimmedUsername.isEmpty()) {
            return ValidationResult.Error(ErrorType.EMPTY_FIELD, strings.usernameRequired)
        }

        if (trimmedPassword.isEmpty()) {
            return ValidationResult.Error(ErrorType.EMPTY_FIELD, strings.passwordRequired)
        }

        return ValidationResult.Success
    }

    /**
     * Sanitizes URL by trimming and normalizing
     */
    fun sanitizeUrl(url: String): String {
        var sanitized = url.replace(Regex("\\s+"), "")

        if (!sanitized.startsWith("http://", ignoreCase = true) &&
            !sanitized.startsWith("https://", ignoreCase = true)
        ) {
            sanitized = "http://$sanitized"
        }

        while (sanitized.endsWith("/") && sanitized.length > 8) {
            sanitized = sanitized.dropLast(1)
        }

        if (!sanitized.contains("?") && !sanitized.endsWith(".m3u") && !sanitized.endsWith(".m3u8")) {
            sanitized = "$sanitized/"
        }

        return sanitized
    }

    fun sanitizeUsername(username: String): String = username.trim()

    fun sanitizePassword(password: String): String = password.trim()

    fun sanitizePlaylistName(name: String): String {
        return name.trim()
            .replace(Regex("[/\\\\:*?\"<>|]"), "_")
            .take(50)
    }

    private fun isValidPlaylistName(name: String): Boolean {
        return name.matches(Regex("^[a-zA-Z0-9\\s._-]+$"))
    }

    /**
     * Converts technical error messages to user-friendly ones
     */
    fun getUserFriendlyErrorMessage(
        strings: StringResources,
        technicalError: String
    ): Pair<ErrorType, String> {
        val lowerError = technicalError.lowercase()

        return when {
            lowerError.contains("401") || lowerError.contains("unauthorized") -> {
                ErrorType.AUTH_ERROR to strings.authInvalidCredentials
            }
            lowerError.contains("403") || lowerError.contains("forbidden") -> {
                ErrorType.AUTH_ERROR to strings.authAccessDenied
            }
            lowerError.contains("unknown host") || lowerError.contains("cannot resolve") -> {
                ErrorType.NETWORK_ERROR to strings.networkCannotReachServer
            }
            lowerError.contains("timeout") || lowerError.contains("timed out") -> {
                ErrorType.TIMEOUT_ERROR to strings.networkTimeout
            }
            lowerError.contains("connection refused") -> {
                ErrorType.NETWORK_ERROR to strings.networkConnectionRefused
            }
            lowerError.contains("no internet") || lowerError.contains("no network") -> {
                ErrorType.NETWORK_ERROR to strings.networkNoInternet
            }
            lowerError.contains("ssl") || lowerError.contains("certificate") -> {
                ErrorType.SSL_ERROR to strings.sslCertificate
            }
            lowerError.contains("500") || lowerError.contains("internal server") -> {
                ErrorType.SERVER_ERROR to strings.serverInternal
            }
            lowerError.contains("502") || lowerError.contains("503") -> {
                ErrorType.SERVER_ERROR to strings.serverUnavailable
            }
            lowerError.contains("404") || lowerError.contains("not found") -> {
                ErrorType.SERVER_ERROR to strings.serverNotFound
            }
            lowerError.contains("m3u") && (lowerError.contains("empty") || lowerError.contains("no channels")) -> {
                ErrorType.PARSING_ERROR to strings.m3uEmpty
            }
            lowerError.contains("invalid") && lowerError.contains("m3u") -> {
                ErrorType.PARSING_ERROR to strings.m3uInvalidFormat
            }
            lowerError.contains("response body is null") -> {
                ErrorType.SERVER_ERROR to strings.serverEmptyResponse
            }
            else -> {
                ErrorType.UNKNOWN_ERROR to strings.genericError
            }
        }
    }
}
