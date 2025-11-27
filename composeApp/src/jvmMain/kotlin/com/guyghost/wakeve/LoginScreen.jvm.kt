package com.guyghost.wakeve

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * JVM implementation of Google Sign-In button.
 *
 * Opens system browser for OAuth flow via BrowserOAuthHelper.
 */
@Composable
actual fun GoogleSignInButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black,
            disabledContainerColor = Color.White.copy(alpha = 0.5f),
            disabledContentColor = Color.Black.copy(alpha = 0.5f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp,
            disabledElevation = 0.dp
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Google logo placeholder
            Surface(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "G",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF4285F4), // Google Blue
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Sign in with Google",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black.copy(alpha = if (enabled) 1f else 0.5f)
            )
        }
    }
}

/**
 * JVM implementation of Apple Sign-In button.
 *
 * Apple Sign-In is not typically available on JVM/Desktop,
 * so this returns an empty composable.
 */
@Composable
actual fun AppleSignInButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier
) {
    // Apple Sign-In not available on JVM
    // Return empty composable
}

/**
 * Google Sign-In is available on JVM via browser OAuth.
 */
actual fun isGoogleSignInAvailable(): Boolean = true

/**
 * Apple Sign-In is not available on JVM.
 */
actual fun isAppleSignInAvailable(): Boolean = false
