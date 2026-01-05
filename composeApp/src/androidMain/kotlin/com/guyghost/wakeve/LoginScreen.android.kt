package com.guyghost.wakeve

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Android implementation of Google Sign-In button.
 *
 * Uses Material 3 styling with Google branding colors.
 */
@Composable
fun GoogleSignInButton(
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
            // Google logo (placeholder - would use actual resource)
            Surface(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                shape = MaterialTheme.shapes.small
            ) {
                // TODO: Replace with actual Google logo resource
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
 * Android implementation of Apple Sign-In button.
 *
 * Apple Sign-In is not typically available on Android,
 * so this returns an empty composable.
 */
@Composable
fun AppleSignInButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier
) {
    // Apple Sign-In not available on Android
    // Return empty composable
}

/**
 * Google Sign-In is available on Android.
 */
fun isGoogleSignInAvailable(): Boolean = true

/**
 * Apple Sign-In is not available on Android.
 */
fun isAppleSignInAvailable(): Boolean = false
