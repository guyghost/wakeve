package com.guyghost.wakeve

import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState { OnboardingStep.entries.size }
    val scope = rememberCoroutineScope()
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Découverte") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) { page ->
                OnboardingStepContent(
                    step = OnboardingStep.entries[page],
                    onNext = {
                        scope.launch {
                            if (page < OnboardingStep.entries.size - 1) {
                                pagerState.animateScrollToPage(page + 1)
                            } else {
                                onOnboardingComplete()
                            }
                        }
                    },
                    onSkip = onOnboardingComplete
                )
            }

            // Page indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(OnboardingStep.entries.size) { index ->
                    val (width, color) = if (index == currentPage) {
                        32.dp to MaterialTheme.colorScheme.primary
                    } else {
                        8.dp to MaterialTheme.colorScheme.secondary
                    }

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .height(8.dp)
                            .width(width)
                    )
                }
            }

            // Bottom button
            Button(
                onClick = {
                    scope.launch {
                        if (currentPage < OnboardingStep.entries.size - 1) {
                            pagerState.animateScrollToPage(currentPage + 1)
                        } else {
                            onOnboardingComplete()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                enabled = true
            ) {
                Text(if (currentPage < OnboardingStep.entries.size - 1) "Suivant" else "Commencer")
            }
        }
    }
}

@Composable
fun OnboardingStepContent(
    step: OnboardingStep,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step.icon,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = step.title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = step.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        step.features?.let { features ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                features.forEach { feature ->
                    FeatureRow(feature = feature)
                }
            }
        }
    }
}

@Composable
fun FeatureRow(
    feature: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = feature,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

enum class OnboardingStep(
    val title: String,
    val description: String,
    val icon: String,
    val features: List<String>?
) {
    CREATE_EVENT(
        title = "Créez vos événements",
        description = "Organisez facilement des événements entre amis et collègues. Définissez des dates, proposez des créneaux horaires et laissez les participants voter.",
        icon = "📅",
        features = listOf(
            "Création rapide d'événements",
            "Sondage de disponibilité",
            "Calcul automatique du meilleur créneau"
        )
    ),
    COLLABORATE(
        title = "Collaborez en équipe",
        description = "Travaillez ensemble sur l'organisation de l'événement. Partagez les responsabilités et suivez la progression en temps réel.",
        icon = "👥",
        features = listOf(
            "Gestion des participants",
            "Attribution des tâches",
            "Suivi en temps réel"
        )
    ),
    ORGANIZE(
        title = "Organisez tout en un",
        description = "Gérez l'hébergement, les repas, les activités et le budget. Tout au même endroit pour une organisation sans faille.",
        icon = "🎯",
        features = listOf(
            "Planification d'hébergement",
            "Organisation des repas",
            "Suivi du budget"
        )
    ),
    ENJOY(
        title = "Profitez de vos événements",
        description = "Une fois l'organisation terminée, profitez de l'événement avec vos proches sans stress.",
        icon = "🎉",
        features = listOf(
            "Vue d'ensemble",
            "Rappels intégrés",
            "Calendrier natif"
        )
    )
}
