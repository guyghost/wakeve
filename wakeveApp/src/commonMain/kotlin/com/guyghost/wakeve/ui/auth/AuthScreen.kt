package com.guyghost.wakeve.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guyghost.wakeve.R
import com.guyghost.wakeve.ui.auth.components.AppleSignInButton
import com.guyghost.wakeve.ui.auth.components.GoogleSignInButton
import com.guyghost.wakeve.ui.auth.components.SkipButton

/**
 * Main authentication screen for Wakeve.
 * 
 * This screen displays:
 * - Welcome message and branding
 * - Sign in with Google button
 * - Sign in with Apple button
 * - Sign in with Email button
 * - Skip button (top-right) for guest mode
 * 
 * @param onGoogleSignIn Callback when Google sign-in is requested
 * @param onAppleSignIn Callback when Apple sign-in is requested
 * @param onEmailSignIn Callback when email sign-in is requested
 * @param onSkip Callback when user wants to skip (guest mode)
 * @param isLoading Whether authentication is in progress
 * @param errorMessage Error message to display (if any)
 */
@Composable
fun AuthScreen(
    onGoogleSignIn: () -> Unit,
    onAppleSignIn: () -> Unit,
    onEmailSignIn: () -> Unit,
    onSkip: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Skip button at top-right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                SkipButton(
                    onClick = onSkip,
                    enabled = !isLoading
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Welcome message
            Text(
                text = stringResource(R.string.welcome_to_wakeve),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.auth_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Error message
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Google Sign-In
            GoogleSignInButton(
                onClick = onGoogleSignIn,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Apple Sign-In
            AppleSignInButton(
                onClick = onAppleSignIn,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Divider with "or"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.or),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Email Sign-In
            OutlinedButton(
                onClick = onEmailSignIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.sign_in_with_email),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Terms and conditions text
            Text(
                text = stringResource(R.string.terms_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Email authentication screen with OTP verification.
 * 
 * This screen has two states:
 * 1. Email input: User enters their email address
 * 2. OTP input: User enters the OTP code sent to their email
 * 
 * @param email Current email being used
 * @param isLoading Whether authentication is in progress
 * @param isOTPStage Whether we're in OTP input stage
 * @param remainingTime Remaining time for OTP validity (seconds)
 * @param attemptsRemaining Number of OTP attempts remaining
 * @param errorMessage Error message to display
 * @param onSubmitEmail Callback when email is submitted
 * @param onSubmitOTP Callback when OTP is submitted
 * @param onResendOTP Callback when user requests new OTP
 * @param onBack Callback when user goes back
 * @param onClearError Callback to clear error message
 */
@Composable
fun EmailAuthScreen(
    email: String,
    isLoading: Boolean,
    isOTPStage: Boolean,
    remainingTime: Int,
    attemptsRemaining: Int,
    errorMessage: String?,
    onSubmitEmail: (String) -> Unit,
    onSubmitOTP: (String) -> Unit,
    onResendOTP: () -> Unit,
    onBack: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputValue by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isOTPStage) {
            // OTP Input Stage
            OTPInputContent(
                email = email,
                inputValue = inputValue,
                onValueChange = {
                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                        inputValue = it
                    }
                },
                remainingTime = remainingTime,
                attemptsRemaining = attemptsRemaining,
                errorMessage = errorMessage,
                isLoading = isLoading,
                onSubmit = {
                    focusManager.clearFocus()
                    onSubmitOTP(inputValue)
                },
                onResendOTP = onResendOTP,
                onClearError = onClearError
            )
        } else {
            // Email Input Stage
            EmailInputContent(
                inputValue = inputValue,
                onValueChange = { inputValue = it },
                errorMessage = errorMessage,
                isLoading = isLoading,
                onSubmit = {
                    focusManager.clearFocus()
                    onSubmitEmail(inputValue)
                },
                onClearError = onClearError
            )
        }
    }
}

@Composable
private fun EmailInputContent(
    inputValue: String,
    onValueChange: (String) -> Unit,
    errorMessage: String?,
    isLoading: Boolean,
    onSubmit: () -> Unit,
    onClearError: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.enter_your_email),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.email_otp_notice),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = inputValue,
            onValueChange = {
                onClearError()
                onValueChange(it)
            },
            label = { Text(stringResource(R.string.email)) },
            placeholder = { Text(stringResource(R.string.email_placeholder)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onSubmit() }
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage != null,
            enabled = !isLoading
        )

        errorMessage?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading && inputValue.isNotBlank(),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = stringResource(R.string.send_otp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun OTPInputContent(
    email: String,
    inputValue: String,
    onValueChange: (String) -> Unit,
    remainingTime: Int,
    attemptsRemaining: Int,
    errorMessage: String?,
    isLoading: Boolean,
    onSubmit: () -> Unit,
    onResendOTP: () -> Unit,
    onClearError: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.enter_otp_code),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.otp_sent_to, email),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Timer display
        val minutes = remainingTime / 60
        val seconds = remainingTime % 60
        Text(
            text = stringResource(R.string.otp_validity, minutes, seconds),
            style = MaterialTheme.typography.bodyMedium,
            color = if (remainingTime < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // OTP input field (6 digits)
        OutlinedTextField(
            value = inputValue,
            onValueChange = {
                onClearError()
                onValueChange(it)
            },
            label = { Text(stringResource(R.string.otp_code)) },
            placeholder = { Text("000000") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onSubmit() }
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage != null,
            enabled = !isLoading
        )

        errorMessage?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp)
            )
        }

        // Attempts remaining warning
        if (attemptsRemaining > 0 && attemptsRemaining <= 1) {
            Text(
                text = stringResource(R.string.otp_attempts_warning, attemptsRemaining),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading && inputValue.length == 6,
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = stringResource(R.string.verify_otp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Resend OTP button
        TextButton(
            onClick = onResendOTP,
            enabled = remainingTime <= 0 && !isLoading
        ) {
            Text(stringResource(R.string.resend_otp))
        }
    }
}
