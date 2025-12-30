package com.guyghost.wakeve

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.comment.CommentRepository
import com.guyghost.wakeve.models.CommentSection
import com.guyghost.wakeve.presentation.state.ScenarioManagementContract
import com.guyghost.wakeve.viewmodel.ScenarioManagementViewModel
import kotlinx.coroutines.launch

/**
 * Local UI state for editing
 */
data class ScenarioDetailUIState(
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
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
 * 
 * Uses ViewModel with StateFlow for state management following MVI/FSM pattern.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ScenarioDetailScreen(
    scenarioId: String,
    viewModel: ScenarioManagementViewModel,
    commentRepository: CommentRepository,
    isOrganizer: Boolean,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onNavigateToComments: (eventId: String, section: CommentSection, sectionItemId: String?) -> Unit
) {
    // Collect state from ViewModel
    val vmState by viewModel.state.collectAsStateWithLifecycle()
    
    // Local UI state for editing (not persisted in ViewModel)
    var uiState by remember { mutableStateOf(ScenarioDetailUIState()) }
    val scope = rememberCoroutineScope()
    var commentCount by remember { mutableIntStateOf(0) }

    // Load scenario when this screen appears
    LaunchedEffect(scenarioId) {
        viewModel.selectScenario(scenarioId)
    }

    // Handle side effects from ViewModel
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is ScenarioManagementContract.SideEffect.NavigateBack -> {
                    onBack()
                }
                is ScenarioManagementContract.SideEffect.ShowError -> {
                    // Error is shown in UI from vmState.error
                }
                is ScenarioManagementContract.SideEffect.ShowToast -> {
                    // Toast is shown via snackbar from vmState
                }
                else -> {} // Handle other side effects if needed
            }
        }
    }

    // Update UI state when scenario is selected
    LaunchedEffect(vmState.selectedScenario) {
        vmState.selectedScenario?.let { scenario ->
            uiState = uiState.copy(
                editName = scenario.name,
                editLocation = scenario.location,
                editDateOrPeriod = scenario.dateOrPeriod,
                editDuration = scenario.duration.toString(),
                editParticipants = scenario.estimatedParticipants.toString(),
                editBudget = scenario.estimatedBudgetPerPerson.toString(),
                editDescription = scenario.description
            )
            // Load comment count for this scenario
            commentCount = commentRepository.countCommentsBySection(
                scenario.eventId,
                CommentSection.SCENARIO,
                scenarioId
            ).toInt()
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
                    if (isOrganizer && vmState.selectedScenario != null) {
                        if (uiState.isEditing) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        uiState = uiState.copy(isSaving = true)
                                        try {
                                            val scenario = vmState.selectedScenario ?: return@launch
                                            val updated = scenario.copy(
                                                name = uiState.editName,
                                                location = uiState.editLocation,
                                                dateOrPeriod = uiState.editDateOrPeriod,
                                                duration = uiState.editDuration.toIntOrNull() ?: scenario.duration,
                                                estimatedParticipants = uiState.editParticipants.toIntOrNull() ?: scenario.estimatedParticipants,
                                                estimatedBudgetPerPerson = uiState.editBudget.toDoubleOrNull() ?: scenario.estimatedBudgetPerPerson,
                                                description = uiState.editDescription
                                            )
                                            // Dispatch update intent to ViewModel
                                            viewModel.updateScenario(updated)
                                            // Update UI state after successful update
                                            uiState = uiState.copy(
                                                isEditing = false,
                                                isSaving = false
                                            )
                                        } catch (e: Exception) {
                                            uiState = uiState.copy(
                                                isSaving = false
                                            )
                                        }
                                    }
                                },
                                enabled = !uiState.isSaving
                            ) {
                                if (uiState.isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.width(24.dp).height(24.dp)
                                    )
                                } else {
                                    Icon(Icons.Default.Save, contentDescription = "Save")
                                }
                            }
                        } else {
                            IconButton(onClick = { uiState = uiState.copy(isEditing = true) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { uiState = uiState.copy(showDeleteDialog = true) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                    // Comments icon with badge
                    if (vmState.selectedScenario != null) {
                        IconButton(onClick = {
                            onNavigateToComments(
                                vmState.selectedScenario!!.eventId,
                                CommentSection.SCENARIO,
                                scenarioId
                            )
                        }) {
                            Box {
                                Icon(
                                    Icons.Outlined.Comment,
                                    contentDescription = if (commentCount == 0) "No comments" else "$commentCount comments"
                                )
                                if (commentCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.error,
                                                shape = CircleShape
                                            )
                                            .align(Alignment.TopEnd),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = commentCount.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onError,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
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
            if (vmState.isLoading) {
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
            if (vmState.error != null) {
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
                            vmState.error ?: "Unknown error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                return@Scaffold
            }

            val scenario = vmState.selectedScenario ?: return@Scaffold

            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (uiState.isEditing) {
                        OutlinedTextField(
                            value = uiState.editName,
                            onValueChange = { uiState = uiState.copy(editName = it) },
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
                isEditing = uiState.isEditing
            ) {
                if (uiState.isEditing) {
                    OutlinedTextField(
                        value = uiState.editLocation,
                        onValueChange = { uiState = uiState.copy(editLocation = it) },
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
                isEditing = uiState.isEditing
            ) {
                if (uiState.isEditing) {
                    OutlinedTextField(
                        value = uiState.editDateOrPeriod,
                        onValueChange = { uiState = uiState.copy(editDateOrPeriod = it) },
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
                isEditing = uiState.isEditing
            ) {
                if (uiState.isEditing) {
                    OutlinedTextField(
                        value = uiState.editDuration,
                        onValueChange = { uiState = uiState.copy(editDuration = it) },
                        label = { Text("Duration (days)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.editParticipants,
                        onValueChange = { uiState = uiState.copy(editParticipants = it) },
                        label = { Text("Estimated Participants") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.editBudget,
                        onValueChange = { uiState = uiState.copy(editBudget = it) },
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
                isEditing = uiState.isEditing
            ) {
                if (uiState.isEditing) {
                    OutlinedTextField(
                        value = uiState.editDescription,
                        onValueChange = { uiState = uiState.copy(editDescription = it) },
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
            if (uiState.isEditing) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        uiState = uiState.copy(
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
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { uiState = uiState.copy(showDeleteDialog = false) },
            title = { Text("Delete Scenario") },
            text = { Text("Are you sure you want to delete \"${vmState.selectedScenario?.name}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                // Dispatch delete intent to ViewModel
                                viewModel.deleteScenario(scenarioId)
                                uiState = uiState.copy(showDeleteDialog = false)
                                // ViewModel will emit NavigateBack side effect
                                onDeleted()
                            } catch (e: Exception) {
                                uiState = uiState.copy(showDeleteDialog = false)
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
                TextButton(onClick = { uiState = uiState.copy(showDeleteDialog = false) }) {
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
