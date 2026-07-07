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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.ui.designsystem.WakeveSize
import com.guyghost.wakeve.ui.designsystem.WakeveSpacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = onboardingSteps()
    val pagerState = rememberPagerState { steps.size }
    val scope = rememberCoroutineScope()
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.onboarding_title)) },
                actions = {
                    TextButton(onClick = onOnboardingComplete) {
                        Text(stringResource(R.string.skip))
                    }
                }
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
                OnboardingStepContent(step = steps[page])
            }

            PageIndicators(
                pageCount = steps.size,
                currentPage = currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WakeveSpacing.lg, vertical = WakeveSpacing.md)
            )

            Button(
                onClick = {
                    scope.launch {
                        if (currentPage < steps.lastIndex) {
                            pagerState.animateScrollToPage(currentPage + 1)
                        } else {
                            onOnboardingComplete()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WakeveSpacing.lg, vertical = WakeveSpacing.lg)
                    .heightIn(min = WakeveSize.minTouchTarget),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    text = if (currentPage < steps.lastIndex) {
                        stringResource(R.string.next)
                    } else {
                        stringResource(R.string.get_started_cta)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun OnboardingStepContent(
    step: OnboardingStep,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(WakeveSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(WakeveSpacing.xxl))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = step.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(modifier = Modifier.height(WakeveSpacing.xl))

        Text(
            text = step.title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )

        Spacer(modifier = Modifier.height(WakeveSpacing.md))

        Text(
            text = step.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(WakeveSpacing.md)
        ) {
            step.features.forEach { feature ->
                FeatureRow(feature = feature)
            }
        }
    }
}

@Composable
private fun PageIndicators(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    )
                    .height(8.dp)
                    .width(if (isSelected) 32.dp else 8.dp)
            )
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
        Spacer(modifier = Modifier.width(WakeveSpacing.md))
        Text(
            text = feature,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private data class OnboardingStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val features: List<String>
)

@Composable
private fun onboardingSteps(): List<OnboardingStep> {
    return listOf(
        OnboardingStep(
            title = stringResource(R.string.onboarding_create_event_title),
            description = stringResource(R.string.onboarding_create_event_description),
            icon = Icons.Default.EventAvailable,
            features = listOf(
                stringResource(R.string.onboarding_create_event_feature_fast),
                stringResource(R.string.onboarding_create_event_feature_poll),
                stringResource(R.string.onboarding_create_event_feature_best_slot)
            )
        ),
        OnboardingStep(
            title = stringResource(R.string.onboarding_collaborate_title),
            description = stringResource(R.string.onboarding_collaborate_description),
            icon = Icons.Default.Groups,
            features = listOf(
                stringResource(R.string.onboarding_collaborate_feature_participants),
                stringResource(R.string.onboarding_collaborate_feature_roles),
                stringResource(R.string.onboarding_collaborate_feature_progress)
            )
        ),
        OnboardingStep(
            title = stringResource(R.string.onboarding_organize_title),
            description = stringResource(R.string.onboarding_organize_description),
            icon = Icons.Default.Route,
            features = listOf(
                stringResource(R.string.onboarding_organize_feature_lodging),
                stringResource(R.string.onboarding_organize_feature_meals),
                stringResource(R.string.onboarding_organize_feature_budget)
            )
        ),
        OnboardingStep(
            title = stringResource(R.string.onboarding_enjoy_title),
            description = stringResource(R.string.onboarding_enjoy_description),
            icon = Icons.Default.CheckCircle,
            features = listOf(
                stringResource(R.string.onboarding_enjoy_feature_overview),
                stringResource(R.string.onboarding_enjoy_feature_reminders),
                stringResource(R.string.onboarding_enjoy_feature_calendar)
            )
        )
    )
}
