package com.guyghost.wakeve.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.ui.theme.WakevColorScheme
import org.jetbrains.compose.resources.painterResource

/**
 * Email Authentication Screen with OTP verification.
 *
 * This screen handles two stages:
 * 1. Email input - User enters email address
 * 2. OTP input - User enters 6-digit code sent to email
 *
 * Material You Design System:
 * - Primary color for actions
 * - Surface colors for background
 * - Error colors for validation feedback
 *
 * Architecture: Imperative Shell (UI) ↔ AuthViewModel (Orchestration) ↔ StateMachine (Logic)
 *
 * @param email Current email value (empty if OTP stage)
 * @param isLoading True if operation is in progress
 * @param isOTPStage True if showing OTP input
 * @param remainingTime Time remaining for OTP validity (seconds)
 * @param attemptsRemaining Number of OTP attempts remaining
 * @param errorMessage Error message to display (null if none)
 * @param onSubmitEmail Callback when user submits email
 * @param onSubmitOTP Callback when user submits OTP
 * @param onResendOTP Callback when user requests new OTP
 * @param onBack Callback when user navigates back
 * @param onClearError Callback to clear error message
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    onClearError: () -> Unit
) {
    var emailInput by remember { mutableStateOf(email) }
    var otpInput by remember { mutableStateOf("") }

    // Focus requesters for auto-focus behavior
    val emailFocusRequester = remember { FocusRequester() }
    val otpFocusRequester = remember { FocusRequester() }

    // Keyboard controller for auto-dismiss
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-focus on appropriate field based on stage
    LaunchedEffect(isOTPStage) {
        if (isOTPStage) {
            otpFocusRequester.requestFocus()
        } else {
            emailFocusRequester.requestFocus()
        }
    }

    // Clear error when user starts typing (based on current stage)
    LaunchedEffect(isOTPStage, emailInput, otpInput) {
        if (errorMessage != null) {
            if (!isOTPStage && emailInput.isNotBlank()) {
                // Email stage - clear error when email changes
                onClearError()
            } else if (isOTPStage && otpInput.isNotBlank()) {
                // OTP stage - clear error when OTP changes
                onClearError()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connexion par email") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Email Stage
                AnimatedVisibility(
                    visible = !isOTPStage,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    EmailStageContent(
                        emailInput = emailInput,
                        onEmailChange = { emailInput = it },
                        onSubmitEmail = {
                            keyboardController?.hide()
                            onSubmitEmail(emailInput.trim())
                        },
                        isLoading = isLoading,
                        focusRequester = emailFocusRequester
                    )
                }

        // OTP Stage
        AnimatedVisibility(
            visible = isOTPStage,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            OTPStageContent(
                email = email,
                otpInput = otpInput,
                onOTPChange = { otpInput = it },
                onSubmitOTP = {
                    keyboardController?.hide()
                    onSubmitOTP(otpInput.trim())
                },
                onResendOTP = onResendOTP,
                isLoading = isLoading,
                remainingTime = remainingTime,
                attemptsRemaining = attemptsRemaining,
                errorMessage = errorMessage,
                focusRequester = otpFocusRequester,
                onClearError = onClearError
            )
        }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * Email input stage content.
 */
@Composable
private fun EmailStageContent(
    emailInput: String,
    onEmailChange: (String) -> Unit,
    onSubmitEmail: () -> Unit,
    isLoading: Boolean,
    focusRequester: FocusRequester
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Email Icon
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        // Instructions
        Text(
            text = "Entrez votre adresse email",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Nous vous enverrons un code de vérification par email",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Nous ne stockerons que votre adresse email de manière sécurisée.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Email Input
        OutlinedTextField(
            value = emailInput,
            onValueChange = onEmailChange,
            label = { Text("Adresse email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Submit Button
        Button(
            onClick = onSubmitEmail,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = emailInput.isNotBlank() && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Envoyer le code", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/**
 * OTP input stage content.
 */
@Composable
private fun OTPStageContent(
    email: String,
    otpInput: String,
    onOTPChange: (String) -> Unit,
    onSubmitOTP: () -> Unit,
    onResendOTP: () -> Unit,
    isLoading: Boolean,
    remainingTime: Int,
    attemptsRemaining: Int,
    errorMessage: String?,
    focusRequester: FocusRequester,
    onClearError: () -> Unit
) {
    var countdown by remember { mutableIntStateOf(remainingTime) }

    // Countdown timer
    LaunchedEffect(remainingTime) {
        countdown = remainingTime
        while (countdown > 0) {
            kotlinx.coroutines.delay(1000)
            countdown--
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Instructions
        Text(
            text = "Vérification du code",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Envoyé à $email",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // OTP Input (6-digit code)
        OutlinedTextField(
            value = otpInput,
            onValueChange = {
                // Limit to 6 digits and clear error when typing
                if (it.length <= 6) {
                    onOTPChange(it.filter { ch -> ch.isDigit() })
                    if (errorMessage != null && it.isNotBlank()) {
                        onClearError()
                    }
                }
            },
            label = { Text("Code à 6 chiffres") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            enabled = !isLoading,
            isError = errorMessage != null,
            supportingText = errorMessage?.let {
                {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (attemptsRemaining > 0) {
                            Text(
                                text = "($attemptsRemaining tentative${if (attemptsRemaining > 1) "s" else ""} restante${if (attemptsRemaining > 1) "s" else ""})",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Verify Button
        Button(
            onClick = onSubmitOTP,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = otpInput.length == 6 && !isLoading && attemptsRemaining > 0,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Vérifier", style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Resend OTP Button (disabled during countdown)
        TextButton(
            onClick = onResendOTP,
            enabled = !isLoading && countdown <= 0
        ) {
            if (countdown > 0) {
                Text(
                    text = "Renvoyer dans ${countdown}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Renvoyer le code",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
