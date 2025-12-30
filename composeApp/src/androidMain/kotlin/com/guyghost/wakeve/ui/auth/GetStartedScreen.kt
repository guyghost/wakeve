package com.guyghost.wakeve.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Get Started screen - Welcome/branding screen before login.
 * 
 * Displays the Wakeve branding, tagline, and CTA to proceed to login.
 * Matches iOS ModernGetStartedView with Material You design.
 * 
 * ## Features
 * - Animated logo and text fade-in
 * - Material You gradient background
 * - Primary CTA button
 * - Accessibility labels
 * 
 * @param onGetStarted Callback when user taps "Get Started" button
 */
@Composable
fun GetStartedScreen(
    onGetStarted: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    
    // Trigger animation on first composition
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = EaseInOut),
        label = "fade_in_animation"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
    ) {
        // Gradient Background (Material You colors)
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Logo placeholder (replace with actual logo)
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_dialog_info), // TODO: Replace with Wakeve logo
                        contentDescription = "Wakeve Logo",
                        modifier = Modifier.size(120.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // App Name
                    Text(
                        text = "Wakeve",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 48.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Tagline
                    Text(
                        text = "Planification d'événements collaborative",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    // CTA Button
                    Button(
                        onClick = onGetStarted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Commencer",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Features list
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        FeatureItem(
                            icon = "✅",
                            text = "Sondages collaboratifs pour trouver la meilleure date"
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FeatureItem(
                            icon = "🌍",
                            text = "Planification de destinations et d'activités"
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FeatureItem(
                            icon = "💰",
                            text = "Gestion de budget et cagnotte partagée"
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FeatureItem(
                            icon = "📱",
                            text = "Disponible hors ligne, synchronisation automatique"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Feature item displaying an icon and text.
 */
@Composable
private fun FeatureItem(
    icon: String,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}
