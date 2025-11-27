package com.guyghost.wakeve

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * iOS implementation of Google Sign-In button.
 *
 * Google Sign-In is not typically used on iOS (Apple prefers Apple Sign-In),
 * but can be supported if needed.
 */
@Composable
actual fun GoogleSignInButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier
) {
    // Google Sign-In not typically used on iOS
    // Return empty composable (or implement if needed)
}

/**
 * iOS implementation of Apple Sign-In button.
 *
 * Uses native iOS styling matching Apple's Human Interface Guidelines.
 */
@Composable
actual fun AppleSignInButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black,
            contentColor = Color.White,
            disabledContainerColor = Color.Black.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.5f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Apple logo
            Text(
                text = "",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Sign in with Apple",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = if (enabled) 1f else 0.5f)
            )
        }
    }
}

/**
 * Google Sign-In is not typically available on iOS.
 */
actual fun isGoogleSignInAvailable(): Boolean = false

/**
 * Apple Sign-In is available on iOS.
 */
actual fun isAppleSignInAvailable(): Boolean = true
