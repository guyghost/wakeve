package com.guyghost.wakeve.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Card for estimating participant counts.
 * Shows three fields: minimum, maximum, and expected participants.
 * 
 * Includes validation:
 * - All values must be positive integers
 * - Maximum must be >= minimum
 * - Expected should be between min and max (warning only)
 * 
 * Material You design system component.
 * 
 * @param minParticipants Minimum expected participants (nullable)
 * @param maxParticipants Maximum allowed participants (nullable)
 * @param expectedParticipants Most likely participant count (nullable)
 * @param onMinChanged Callback when minimum changes
 * @param onMaxChanged Callback when maximum changes
 * @param onExpectedChanged Callback when expected changes
 * @param modifier Modifier for the card
 * @param enabled Whether the inputs are enabled
 */
@Composable
fun ParticipantsEstimationCard(
    minParticipants: Int?,
    maxParticipants: Int?,
    expectedParticipants: Int?,
    onMinChanged: (Int?) -> Unit,
    onMaxChanged: (Int?) -> Unit,
    onExpectedChanged: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var minText by remember(minParticipants) { 
        mutableStateOf(minParticipants?.toString() ?: "") 
    }
    var maxText by remember(maxParticipants) { 
        mutableStateOf(maxParticipants?.toString() ?: "") 
    }
    var expectedText by remember(expectedParticipants) { 
        mutableStateOf(expectedParticipants?.toString() ?: "") 
    }
    
    // Validation
    val minValue = minText.toIntOrNull()
    val maxValue = maxText.toIntOrNull()
    val expectedValue = expectedText.toIntOrNull()
    
    val isMaxValid = maxValue == null || minValue == null || maxValue >= minValue
    val expectedOutOfRange = expectedValue != null && (
        (minValue != null && expectedValue < minValue) || 
        (maxValue != null && expectedValue > maxValue)
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Group,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Participants Estimation",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = "Help us plan better by estimating participant counts",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            
            // Minimum
            OutlinedTextField(
                value = minText,
                onValueChange = { newValue ->
                    minText = newValue
                    onMinChanged(newValue.toIntOrNull()?.takeIf { it > 0 })
                },
                label = { Text("Minimum Participants") },
                placeholder = { Text("e.g., 5") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.People,
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = minValue != null && minValue < 1
            )
            
            // Maximum
            OutlinedTextField(
                value = maxText,
                onValueChange = { newValue ->
                    maxText = newValue
                    onMaxChanged(newValue.toIntOrNull()?.takeIf { it > 0 })
                },
                label = { Text("Maximum Participants") },
                placeholder = { Text("e.g., 50") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Group,
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = !isMaxValid || (maxValue != null && maxValue < 1),
                supportingText = {
                    if (!isMaxValid) {
                        Text(
                            text = "Maximum must be greater than or equal to minimum",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
            
            // Expected
            OutlinedTextField(
                value = expectedText,
                onValueChange = { newValue ->
                    expectedText = newValue
                    onExpectedChanged(newValue.toIntOrNull()?.takeIf { it > 0 })
                },
                label = { Text("Expected Participants") },
                placeholder = { Text("e.g., 20") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.TrendingUp,
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = expectedValue != null && expectedValue < 1,
                supportingText = {
                    when {
                        expectedValue != null && expectedValue < 1 -> {
                            Text(
                                text = "Expected must be at least 1",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        expectedOutOfRange -> {
                            Text(
                                text = "Expected is outside min-max range",
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            )
            
            // Helper text
            if (minValue != null || maxValue != null || expectedValue != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = buildString {
                            append("💡 This helps us suggest appropriate venues")
                            if (expectedValue != null) {
                                append(" and estimate costs for ~$expectedValue people")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}
