package com.guyghost.wakeve

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import kotlinx.coroutines.launch

/**
 * State for Scenario Detail Screen
 */
data class ScenarioDetailState(
    val scenario: Scenario? = null,
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String = "",
    val showDeleteDialog: Boolean = false,
    
    // Editable fields
    val editName: String = "",
    val editLocation: String = "",
    val editDateOrPeriod: String = "",
    val editDuration: String = "",
    val editParticipants: String = "",
    val editBudget: String = "",
    val editDescription: String = ""
)

/**
 * Scenario Detail Screen
 * 
 * View and edit details of a specific scenario.
 * Organizers can edit and delete scenarios.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ScenarioDetailScreen(
    scenarioId: String,
    repository: ScenarioRepository,
    isOrganizer: Boolean,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    var state by remember { mutableStateOf(ScenarioDetailState()) }
    val scope = rememberCoroutineScope()

    // Load scenario on mount
    LaunchedEffect(scenarioId) {
        state = state.copy(isLoading = true, isError = false)
        try {
            val scenario = repository.getScenarioById(scenarioId)
            if (scenario != null) {
                state = state.copy(
                    scenario = scenario,
                    isLoading = false,
                    editName = scenario.name,
                    editLocation = scenario.location,
                    editDateOrPeriod = scenario.dateOrPeriod,
                    editDuration = scenario.duration.toString(),
                    editParticipants = scenario.estimatedParticipants.toString(),
                    editBudget = scenario.estimatedBudgetPerPerson.toString(),
                    editDescription = scenario.description
                )
            } else {
                state = state.copy(
                    isLoading = false,
                    isError = true,
                    errorMessage = "Scenario not found"
                )
            }
        } catch (e: Exception) {
            state = state.copy(
                isLoading = false,
                isError = true,
                errorMessage = e.message ?: "Failed to load scenario"
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scenario Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isOrganizer && state.scenario != null) {
                        if (state.isEditing) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        state = state.copy(isSaving = true)
                                        try {
                                            val updated = state.scenario!!.copy(
                                                name = state.editName,
                                                location = state.editLocation,
                                                dateOrPeriod = state.editDateOrPeriod,
                                                duration = state.editDuration.toIntOrNull() ?: state.scenario!!.duration,
                                                estimatedParticipants = state.editParticipants.toIntOrNull() ?: state.scenario!!.estimatedParticipants,
                                                estimatedBudgetPerPerson = state.editBudget.toDoubleOrNull() ?: state.scenario!!.estimatedBudgetPerPerson,
                                                description = state.editDescription,
                                                updatedAt = java.time.Instant.now().toString()
                                            )
                                            val result = repository.updateScenario(updated)
                                            if (result.isSuccess) {
                                                state = state.copy(
                                                    scenario = updated,
                                                    isEditing = false,
                                                    isSaving = false
                                                )
                                            } else {
                                                state = state.copy(
                                                    isSaving = false,
                                                    isError = true,
                                                    errorMessage = "Failed to save changes"
                                                )
                                            }
                                        } catch (e: Exception) {
                                            state = state.copy(
                                                isSaving = false,
                                                isError = true,
                                                errorMessage = e.message ?: "Failed to save"
                                            )
                                        }
                                    }
                                },
                                enabled = !state.isSaving
                            ) {
                                if (state.isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.width(24.dp).height(24.dp)
                                    )
                                } else {
                                    Icon(Icons.Default.Save, contentDescription = "Save")
                                }
                            }
                        } else {
                            IconButton(onClick = { state = state.copy(isEditing = true) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { state = state.copy(showDeleteDialog = true) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .padding(paddingValues)
                .safeContentPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Loading State
            if (state.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading scenario...")
                }
                return@Scaffold
            }

            // Error State
            if (state.isError) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            state.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                return@Scaffold
            }

            val scenario = state.scenario ?: return@Scaffold

            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (state.isEditing) {
                        OutlinedTextField(
                            value = state.editName,
                            onValueChange = { state = state.copy(editName = it) },
                            label = { Text("Scenario Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    } else {
                        Text(
                            scenario.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Status",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        StatusBadge(status = scenario.status)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Location Section
            DetailSection(
                title = "Location",
                isEditing = state.isEditing
            ) {
                if (state.isEditing) {
                    OutlinedTextField(
                        value = state.editLocation,
                        onValueChange = { state = state.copy(editLocation = it) },
                        label = { Text("Location") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    Text(
                        scenario.location,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Date/Period Section
            DetailSection(
                title = "Date or Period",
                isEditing = state.isEditing
            ) {
                if (state.isEditing) {
                    OutlinedTextField(
                        value = state.editDateOrPeriod,
                        onValueChange = { state = state.copy(editDateOrPeriod = it) },
                        label = { Text("Date/Period (e.g., 2025-12-15/2025-12-17)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    Text(
                        scenario.dateOrPeriod,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Duration, Participants, Budget
            DetailSection(
                title = "Details",
                isEditing = state.isEditing
            ) {
                if (state.isEditing) {
                    OutlinedTextField(
                        value = state.editDuration,
                        onValueChange = { state = state.copy(editDuration = it) },
                        label = { Text("Duration (days)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.editParticipants,
                        onValueChange = { state = state.copy(editParticipants = it) },
                        label = { Text("Estimated Participants") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.editBudget,
                        onValueChange = { state = state.copy(editBudget = it) },
                        label = { Text("Budget per Person ($)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DetailItem(label = "Duration", value = "${scenario.duration} days")
                        DetailItem(label = "Participants", value = "${scenario.estimatedParticipants}")
                        DetailItem(label = "Budget", value = "$${scenario.estimatedBudgetPerPerson}/person")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description Section
            DetailSection(
                title = "Description",
                isEditing = state.isEditing
            ) {
                if (state.isEditing) {
                    OutlinedTextField(
                        value = state.editDescription,
                        onValueChange = { state = state.copy(editDescription = it) },
                        label = { Text("Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 5
                    )
                } else {
                    Text(
                        scenario.description,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Timestamps
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Created",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            scenario.createdAt,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Updated",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            scenario.updatedAt,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Cancel Button (when editing)
            if (state.isEditing) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        state = state.copy(
                            isEditing = false,
                            editName = scenario.name,
                            editLocation = scenario.location,
                            editDateOrPeriod = scenario.dateOrPeriod,
                            editDuration = scenario.duration.toString(),
                            editParticipants = scenario.estimatedParticipants.toString(),
                            editBudget = scenario.estimatedBudgetPerPerson.toString(),
                            editDescription = scenario.description
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { state = state.copy(showDeleteDialog = false) },
            title = { Text("Delete Scenario") },
            text = { Text("Are you sure you want to delete \"${state.scenario?.name}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val result = repository.deleteScenario(scenarioId)
                                if (result.isSuccess) {
                                    onDeleted()
                                } else {
                                    state = state.copy(
                                        showDeleteDialog = false,
                                        isError = true,
                                        errorMessage = "Failed to delete scenario"
                                    )
                                }
                            } catch (e: Exception) {
                                state = state.copy(
                                    showDeleteDialog = false,
                                    isError = true,
                                    errorMessage = e.message ?: "Failed to delete"
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { state = state.copy(showDeleteDialog = false) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Detail section container
 */
@Composable
fun DetailSection(
    title: String,
    isEditing: Boolean,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

/**
 * Detail item for key-value display
 */
@Composable
fun DetailItem(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}
