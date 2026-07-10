package com.guyghost.wakeve.ui.meal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.R
import com.guyghost.wakeve.meal.MealRepository
import com.guyghost.wakeve.models.AutoMealPlanRequest
import com.guyghost.wakeve.models.DietaryRestriction
import com.guyghost.wakeve.models.DietaryRestrictionRequest
import com.guyghost.wakeve.models.Meal
import com.guyghost.wakeve.models.MealRequest
import com.guyghost.wakeve.models.MealStatus
import com.guyghost.wakeve.models.MealType
import com.guyghost.wakeve.models.ParticipantDietaryRestriction
import java.time.LocalDate

/**
 * Dialog for adding/editing a meal
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMealDialog(
    meal: Meal?,
    eventId: String,
    onDismiss: () -> Unit,
    onSave: (MealRequest) -> Unit
) {
    val requiredName = stringResource(R.string.meal_error_name_required)
    val requiredDate = stringResource(R.string.meal_error_date_required)
    val requiredTime = stringResource(R.string.meal_error_time_required)
    val invalidServings = stringResource(R.string.meal_error_people_invalid)
    val invalidEstimatedCost = stringResource(R.string.meal_error_estimated_cost_invalid)
    var selectedType by remember { mutableStateOf(meal?.type ?: MealType.DINNER) }
    var name by remember { mutableStateOf(meal?.name ?: "") }
    var date by remember { mutableStateOf(meal?.date ?: LocalDate.now().toString()) }
    var time by remember { mutableStateOf(meal?.time ?: "19:00") }
    var location by remember { mutableStateOf(meal?.location ?: "") }
    var estimatedCost by remember { mutableStateOf((meal?.estimatedCost ?: 0L).toString()) }
    var actualCost by remember { mutableStateOf((meal?.actualCost ?: 0L).toString()) }
    var servings by remember { mutableStateOf((meal?.servings ?: 4).toString()) }
    var selectedStatus by remember { mutableStateOf(meal?.status ?: MealStatus.PLANNED) }
    var notes by remember { mutableStateOf(meal?.notes ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (meal == null) R.string.meal_add_title else R.string.meal_edit_title)) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Type selector
                item {
                    Text(
                        stringResource(R.string.meal_type_label),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MealType.entries.take(3).forEach { type ->
                            val typeLabel = getMealTypeLabel(type)
                            val selectedState = stringResource(if (selectedType == type) R.string.meal_selected_state else R.string.meal_unselected_state)
                            val typeDescription = stringResource(R.string.a11y_meal_type_selection, typeLabel, selectedState)
                            FilterChip(
                                modifier = Modifier.clearAndSetSemantics { contentDescription = typeDescription },
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(typeLabel) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MealType.entries.drop(3).forEach { type ->
                            val typeLabel = getMealTypeLabel(type)
                            val selectedState = stringResource(if (selectedType == type) R.string.meal_selected_state else R.string.meal_unselected_state)
                            val typeDescription = stringResource(R.string.a11y_meal_type_selection, typeLabel, selectedState)
                            FilterChip(
                                modifier = Modifier.clearAndSetSemantics { contentDescription = typeDescription },
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(typeLabel) }
                            )
                        }
                    }
                }
                
                // Name
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.meal_name_label)) },
                        placeholder = { Text(stringResource(R.string.meal_name_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                // Date
                item {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text(stringResource(R.string.meal_date_label)) },
                        placeholder = { Text(stringResource(R.string.meal_date_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                // Time
                item {
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text(stringResource(R.string.meal_time_label)) },
                        placeholder = { Text(stringResource(R.string.meal_time_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                // Location (optional)
                item {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text(stringResource(R.string.meal_location_label)) },
                        placeholder = { Text(stringResource(R.string.meal_location_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                // Servings
                item {
                    OutlinedTextField(
                        value = servings,
                        onValueChange = { servings = it },
                        label = { Text(stringResource(R.string.meal_people_label)) },
                        placeholder = { Text(stringResource(R.string.meal_people_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                
                // Estimated cost
                item {
                    OutlinedTextField(
                        value = estimatedCost,
                        onValueChange = { estimatedCost = it },
                        label = { Text(stringResource(R.string.meal_estimated_cost_label)) },
                        placeholder = { Text(stringResource(R.string.meal_estimated_cost_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { 
                            Text(stringResource(R.string.meal_currency_amount, estimatedCost.toLongOrNull()?.div(100.0) ?: 0.0))
                        }
                    )
                }
                
                // Actual cost (optional)
                item {
                    OutlinedTextField(
                        value = actualCost,
                        onValueChange = { actualCost = it },
                        label = { Text(stringResource(R.string.meal_actual_cost_label)) },
                        placeholder = { Text(stringResource(R.string.meal_actual_cost_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { 
                            Text(stringResource(R.string.meal_currency_amount, actualCost.toLongOrNull()?.div(100.0) ?: 0.0))
                        }
                    )
                }
                
                // Status selector
                item {
                    Text(
                        stringResource(R.string.meal_status_label),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MealStatus.entries.forEach { status ->
                            val statusLabel = getMealStatusLabel(status)
                            val selectedState = stringResource(if (selectedStatus == status) R.string.meal_selected_state else R.string.meal_unselected_state)
                            val statusDescription = stringResource(R.string.a11y_meal_status_selection, statusLabel, selectedState)
                            FilterChip(
                                modifier = Modifier.clearAndSetSemantics { contentDescription = statusDescription },
                                selected = selectedStatus == status,
                                onClick = { selectedStatus = status },
                                label = { Text(statusLabel) }
                            )
                        }
                    }
                }
                
                // Notes (optional)
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(stringResource(R.string.meal_notes_label)) },
                        placeholder = { Text(stringResource(R.string.meal_notes_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }
                
                // Error message
                errorMessage?.let { error ->
                    item {
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Validate
                    val servingsInt = servings.toIntOrNull()
                    val estimatedCostLong = estimatedCost.toLongOrNull()
                    val actualCostLong = actualCost.toLongOrNull()
                    
                    when {
                        name.isBlank() -> errorMessage = requiredName
                        date.isBlank() -> errorMessage = requiredDate
                        time.isBlank() -> errorMessage = requiredTime
                        servingsInt == null || servingsInt <= 0 -> errorMessage = invalidServings
                        estimatedCostLong == null || estimatedCostLong < 0 -> errorMessage = invalidEstimatedCost
                        else -> {
                            val request = MealRequest(
                                eventId = eventId,
                                type = selectedType,
                                name = name.trim(),
                                date = date.trim(),
                                time = time.trim(),
                                location = location.trim().ifBlank { null },
                                responsibleParticipantIds = emptyList(), // TODO: Add participant selector
                                estimatedCost = estimatedCostLong,
                                actualCost = actualCostLong,
                                servings = servingsInt,
                                status = selectedStatus,
                                notes = notes.trim().ifBlank { null }
                            )
                            onSave(request)
                        }
                    }
                }
            ) {
                Text(stringResource(if (meal == null) R.string.meal_action_add else R.string.meal_action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.meal_action_cancel))
            }
        }
    )
}

/**
 * Dialog for auto-generating meals
 */
@Composable
fun AutoGenerateMealsDialog(
    eventId: String,
    onDismiss: () -> Unit,
    onGenerate: (AutoMealPlanRequest) -> Unit
) {
    val requiredStartDate = stringResource(R.string.meal_error_start_date_required)
    val requiredEndDate = stringResource(R.string.meal_error_end_date_required)
    val invalidParticipants = stringResource(R.string.meal_error_participants_invalid)
    val invalidCost = stringResource(R.string.meal_error_cost_invalid)
    val requiredType = stringResource(R.string.meal_error_type_required)
    var startDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusDays(3).toString()) }
    var participantCount by remember { mutableStateOf("4") }
    var costPerMeal by remember { mutableStateOf("1500") } // 15€ per person
    var selectedTypes by remember { mutableStateOf(setOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.meal_generate_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Start date
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text(stringResource(R.string.meal_start_date_label)) },
                    placeholder = { Text(stringResource(R.string.meal_start_date_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // End date
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text(stringResource(R.string.meal_end_date_label)) },
                    placeholder = { Text(stringResource(R.string.meal_end_date_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Participant count
                OutlinedTextField(
                    value = participantCount,
                    onValueChange = { participantCount = it },
                    label = { Text(stringResource(R.string.meal_participant_count_label)) },
                    placeholder = { Text(stringResource(R.string.meal_people_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                // Cost per meal per person
                OutlinedTextField(
                    value = costPerMeal,
                    onValueChange = { costPerMeal = it },
                    label = { Text(stringResource(R.string.meal_cost_per_person_label)) },
                    placeholder = { Text(stringResource(R.string.meal_cost_per_person_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { 
                        Text(stringResource(R.string.meal_cost_per_person_value, costPerMeal.toLongOrNull()?.div(100.0) ?: 0.0))
                    }
                )
                
                // Meal types to include
                Text(
                    stringResource(R.string.meal_types_to_generate),
                    style = MaterialTheme.typography.labelMedium
                )
                
                MealType.entries.forEach { type ->
                    val typeLabel = getMealTypeLabel(type)
                    val selectedState = stringResource(if (selectedTypes.contains(type)) R.string.meal_selected_state else R.string.meal_unselected_state)
                    val typeDescription = stringResource(R.string.a11y_meal_type_selection, typeLabel, selectedState)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            modifier = Modifier.clearAndSetSemantics { contentDescription = typeDescription },
                            checked = selectedTypes.contains(type),
                            onCheckedChange = { checked ->
                                selectedTypes = if (checked) {
                                    selectedTypes + type
                                } else {
                                    selectedTypes - type
                                }
                            }
                        )
                        Text(typeLabel)
                    }
                }
                
                // Preview
                if (selectedTypes.isNotEmpty()) {
                    val days = try {
                        val start = LocalDate.parse(startDate)
                        val end = LocalDate.parse(endDate)
                        java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt() + 1
                    } catch (e: Exception) {
                        0
                    }
                    
                    if (days > 0) {
                        val totalMeals = days * selectedTypes.size
                        val totalCost = totalMeals * (costPerMeal.toLongOrNull() ?: 0L) * (participantCount.toIntOrNull() ?: 0)
                        
                        HorizontalDivider()
                        
                        Text(
                            stringResource(R.string.meal_preview_title),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            stringResource(R.string.meal_preview_summary, days, totalMeals, totalCost / 100.0),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Error message
                errorMessage?.let { error ->
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val participantCountInt = participantCount.toIntOrNull()
                    val costPerMealLong = costPerMeal.toLongOrNull()
                    
                    when {
                        startDate.isBlank() -> errorMessage = requiredStartDate
                        endDate.isBlank() -> errorMessage = requiredEndDate
                        participantCountInt == null || participantCountInt <= 0 -> errorMessage = invalidParticipants
                        costPerMealLong == null || costPerMealLong <= 0 -> errorMessage = invalidCost
                        selectedTypes.isEmpty() -> errorMessage = requiredType
                        else -> {
                            val request = AutoMealPlanRequest(
                                eventId = eventId,
                                startDate = startDate.trim(),
                                endDate = endDate.trim(),
                                participantCount = participantCountInt,
                                includeMealTypes = selectedTypes.toList(),
                                estimatedCostPerMeal = costPerMealLong
                            )
                            onGenerate(request)
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.meal_action_generate))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.meal_action_cancel))
            }
        }
    )
}

/**
 * Dialog for managing dietary restrictions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietaryRestrictionsDialog(
    eventId: String,
    restrictions: List<ParticipantDietaryRestriction>,
    mealRepository: MealRepository,
    onDismiss: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var restrictionToDelete by remember { mutableStateOf<ParticipantDietaryRestriction?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.meal_restrictions_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (restrictions.isEmpty()) {
                    Text(
                        stringResource(R.string.meal_restrictions_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(restrictions) { restriction ->
                            val restrictionLabel = getDietaryRestrictionLabel(restriction.restriction)
                            val deleteDescription = stringResource(R.string.a11y_meal_delete_restriction, restrictionLabel, restriction.participantId)
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            restrictionLabel,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                        Text(
                                            stringResource(R.string.meal_participant_value, restriction.participantId),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        restriction.notes?.let { notes ->
                                            Text(
                                                notes,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    
                                    IconButton(
                                        onClick = { restrictionToDelete = restriction },
                                        modifier = Modifier.clearAndSetSemantics { contentDescription = deleteDescription }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.meal_add_restriction_action))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.meal_action_close))
            }
        }
    )
    
    // Add restriction dialog
    if (showAddDialog) {
        AddDietaryRestrictionDialog(
            eventId = eventId,
            onDismiss = { showAddDialog = false },
            onAdd = { request ->
                mealRepository.addDietaryRestriction(request)
                showAddDialog = false
            }
        )
    }
    
    // Delete confirmation
    restrictionToDelete?.let { restriction ->
        AlertDialog(
            onDismissRequest = { restrictionToDelete = null },
            title = { Text(stringResource(R.string.meal_delete_restriction_title)) },
            text = { Text(stringResource(R.string.meal_delete_restriction_message, getDietaryRestrictionLabel(restriction.restriction), restriction.participantId)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        mealRepository.deleteDietaryRestriction(restriction.id)
                        restrictionToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.meal_action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { restrictionToDelete = null }) {
                    Text(stringResource(R.string.meal_action_cancel))
                }
            }
        )
    }
}

/**
 * Dialog for adding a dietary restriction
 */
@Composable
fun AddDietaryRestrictionDialog(
    eventId: String,
    onDismiss: () -> Unit,
    onAdd: (DietaryRestrictionRequest) -> Unit
) {
    val participantRequired = stringResource(R.string.meal_error_participant_required)
    var participantId by remember { mutableStateOf("") }
    var selectedRestriction by remember { mutableStateOf(DietaryRestriction.VEGETARIAN) }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.meal_add_restriction_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = participantId,
                    onValueChange = { participantId = it },
                    label = { Text(stringResource(R.string.meal_participant_id_label)) },
                    placeholder = { Text(stringResource(R.string.meal_participant_id_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text(
                    stringResource(R.string.meal_restriction_type_label),
                    style = MaterialTheme.typography.labelMedium
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                        DietaryRestriction.entries.forEach { restriction ->
                            val restrictionLabel = getDietaryRestrictionLabel(restriction)
                            val selectedState = stringResource(if (selectedRestriction == restriction) R.string.meal_selected_state else R.string.meal_unselected_state)
                            val restrictionDescription = stringResource(R.string.a11y_meal_restriction_selection, restrictionLabel, selectedState)
                            FilterChip(
                            modifier = Modifier.clearAndSetSemantics { contentDescription = restrictionDescription },
                            selected = selectedRestriction == restriction,
                            onClick = { selectedRestriction = restriction },
                            label = { Text(restrictionLabel) }
                        )
                    }
                }
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.meal_notes_label)) },
                    placeholder = { Text(stringResource(R.string.meal_restriction_notes_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                
                errorMessage?.let { error ->
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        participantId.isBlank() -> errorMessage = participantRequired
                        else -> {
                            val request = DietaryRestrictionRequest(
                                participantId = participantId.trim(),
                                eventId = eventId,
                                restriction = selectedRestriction,
                                notes = notes.trim().ifBlank { null }
                            )
                            onAdd(request)
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.meal_action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.meal_action_cancel))
            }
        }
    )
}

@Composable
fun getDietaryRestrictionLabel(restriction: DietaryRestriction): String = stringResource(when (restriction) {
    DietaryRestriction.VEGETARIAN -> R.string.meal_restriction_vegetarian
    DietaryRestriction.VEGAN -> R.string.meal_restriction_vegan
    DietaryRestriction.GLUTEN_FREE -> R.string.meal_restriction_gluten_free
    DietaryRestriction.LACTOSE_INTOLERANT -> R.string.meal_restriction_lactose_intolerant
    DietaryRestriction.NUT_ALLERGY -> R.string.meal_restriction_nut_allergy
    DietaryRestriction.SHELLFISH_ALLERGY -> R.string.meal_restriction_shellfish_allergy
    DietaryRestriction.KOSHER -> R.string.meal_restriction_kosher
    DietaryRestriction.HALAL -> R.string.meal_restriction_halal
    DietaryRestriction.DIABETIC -> R.string.meal_restriction_diabetic
    DietaryRestriction.OTHER -> R.string.meal_restriction_other
})
