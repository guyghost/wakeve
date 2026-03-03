package com.guyghost.wakeve.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.guyghost.wakeve.preview.PreviewTheme

// ---------------------------------------------------------------------------
// AuthScreen previews
// ---------------------------------------------------------------------------

/**
 * Preview: default (idle) authentication screen.
 */
@Preview(showBackground = true)
@Composable
fun AuthScreenPreview() {
    PreviewTheme {
        AuthScreen(
            onGoogleSignIn = {},
            onAppleSignIn = {},
            onEmailSignIn = {},
            onSkip = {},
            isLoading = false,
            errorMessage = null
        )
    }
}

/**
 * Preview: authentication screen in loading state.
 */
@Preview(showBackground = true)
@Composable
fun AuthScreenLoadingPreview() {
    PreviewTheme {
        AuthScreen(
            onGoogleSignIn = {},
            onAppleSignIn = {},
            onEmailSignIn = {},
            onSkip = {},
            isLoading = true,
            errorMessage = null
        )
    }
}

/**
 * Preview: authentication screen displaying an error message.
 */
@Preview(showBackground = true)
@Composable
fun AuthScreenErrorPreview() {
    PreviewTheme {
        AuthScreen(
            onGoogleSignIn = {},
            onAppleSignIn = {},
            onEmailSignIn = {},
            onSkip = {},
            isLoading = false,
            errorMessage = "Authentication failed. Please try again."
        )
    }
}

// ---------------------------------------------------------------------------
// GetStartedScreen preview
// ---------------------------------------------------------------------------

/**
 * Preview: Get Started / welcome branding screen.
 */
@Preview(showBackground = true)
@Composable
fun GetStartedScreenPreview() {
    PreviewTheme {
        GetStartedScreen(
            onGetStarted = {}
        )
    }
}
