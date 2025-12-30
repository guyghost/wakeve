package com.guyghost.wakeve

import androidx.compose.runtime.Composable

/**
 * Type alias for GetStartedScreen to maintain package structure compatibility.
 * Actual implementation is in com.guyghost.wakeve.ui.auth.GetStartedScreen
 */
@Composable
fun GetStartedScreen(onGetStarted: () -> Unit) {
    com.guyghost.wakeve.ui.auth.GetStartedScreen(onGetStarted = onGetStarted)
}
