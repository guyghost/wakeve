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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
        title = { Text(if (meal == null) "Ajouter un repas" else "Modifier le repas") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Type selector
                item {
                    Text(
                        "Type de repas",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MealType.entries.take(3).forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(getMealTypeLabel(type)) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MealType.entries.drop(3).forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(getMealTypeLabel(type)) }
                            )
                        }
                    }
                }
                
                // Name
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nom du repas") },
                        placeholder = { Text("Ex: Barbecue du samedi soir") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                // Date
                item {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        placeholder = { Text("2025-12-25") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                // Time
                item {
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Heure (HH:MM)") },
                        placeholder = { Text("19:00") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                // Location (optional)
                item {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Lieu (optionnel)") },
                        placeholder = { Text("Restaurant, maison, etc.") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                // Servings
                item {
                    OutlinedTextField(
                        value = servings,
                        onValueChange = { servings = it },
                        label = { Text("Nombre de personnes") },
                        placeholder = { Text("4") },
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
                        label = { Text("Coût estimé (centimes)") },
                        placeholder = { Text("2000 = 20€") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { 
                            Text("${estimatedCost.toLongOrNull()?.div(100.0) ?: 0.0}€") 
                        }
                    )
                }
                
                // Actual cost (optional)
                item {
                    OutlinedTextField(
                        value = actualCost,
                        onValueChange = { actualCost = it },
                        label = { Text("Coût réel (optionnel, en centimes)") },
                        placeholder = { Text("2150") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { 
                            Text("${actualCost.toLongOrNull()?.div(100.0) ?: 0.0}€") 
                        }
                    )
                }
                
                // Status selector
                item {
                    Text(
                        "Statut",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MealStatus.entries.forEach { status ->
                            FilterChip(
                                selected = selectedStatus == status,
                                onClick = { selectedStatus = status },
                                label = { Text(getMealStatusLabel(status)) }
                            )
                        }
                    }
                }
                
                // Notes (optional)
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optionnel)") },
                        placeholder = { Text("Menu, préparation, etc.") },
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
                        name.isBlank() -> errorMessage = "Le nom est requis"
                        date.isBlank() -> errorMessage = "La date est requise"
                        time.isBlank() -> errorMessage = "L'heure est requise"
                        servingsInt == null || servingsInt <= 0 -> errorMessage = "Nombre de personnes invalide"
                        estimatedCostLong == null || estimatedCostLong < 0 -> errorMessage = "Coût estimé invalide"
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
                Text(if (meal == null) "Ajouter" else "Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
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
    var startDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusDays(3).toString()) }
    var participantCount by remember { mutableStateOf("4") }
    var costPerMeal by remember { mutableStateOf("1500") } // 15€ per person
    var selectedTypes by remember { mutableStateOf(setOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Générer un plan de repas") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Start date
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Date de début") },
                    placeholder = { Text("2025-12-25") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // End date
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("Date de fin") },
                    placeholder = { Text("2025-12-28") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Participant count
                OutlinedTextField(
                    value = participantCount,
                    onValueChange = { participantCount = it },
                    label = { Text("Nombre de participants") },
                    placeholder = { Text("4") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                
                // Cost per meal per person
                OutlinedTextField(
                    value = costPerMeal,
                    onValueChange = { costPerMeal = it },
                    label = { Text("Coût par repas/personne (centimes)") },
                    placeholder = { Text("1500 = 15€") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { 
                        Text("${costPerMeal.toLongOrNull()?.div(100.0) ?: 0.0}€ par personne") 
                    }
                )
                
                // Meal types to include
                Text(
                    "Types de repas à générer",
                    style = MaterialTheme.typography.labelMedium
                )
                
                MealType.entries.forEach { type ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = selectedTypes.contains(type),
                            onCheckedChange = { checked ->
                                selectedTypes = if (checked) {
                                    selectedTypes + type
                                } else {
                                    selectedTypes - type
                                }
                            }
                        )
                        Text(getMealTypeLabel(type))
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
                            "Aperçu",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            "• $days jour(s)\n• $totalMeals repas au total\n• Coût total estimé: ${totalCost / 100.0}€",
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
                        startDate.isBlank() -> errorMessage = "Date de début requise"
                        endDate.isBlank() -> errorMessage = "Date de fin requise"
                        participantCountInt == null || participantCountInt <= 0 -> errorMessage = "Nombre de participants invalide"
                        costPerMealLong == null || costPerMealLong <= 0 -> errorMessage = "Coût invalide"
                        selectedTypes.isEmpty() -> errorMessage = "Sélectionnez au moins un type de repas"
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
                Text("Générer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
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
        title = { Text("Contraintes alimentaires") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (restrictions.isEmpty()) {
                    Text(
                        "Aucune contrainte alimentaire enregistrée",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(restrictions) { restriction ->
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
                                            getDietaryRestrictionLabel(restriction.restriction),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                        Text(
                                            "Participant: ${restriction.participantId}",
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
                                    
                                    IconButton(onClick = { restrictionToDelete = restriction }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Supprimer",
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
                    Text("Ajouter une contrainte")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
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
            title = { Text("Supprimer la contrainte ?") },
            text = { Text("Voulez-vous vraiment supprimer cette contrainte alimentaire ?") },
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
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { restrictionToDelete = null }) {
                    Text("Annuler")
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
    var participantId by remember { mutableStateOf("") }
    var selectedRestriction by remember { mutableStateOf(DietaryRestriction.VEGETARIAN) }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter une contrainte") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = participantId,
                    onValueChange = { participantId = it },
                    label = { Text("ID du participant") },
                    placeholder = { Text("participant-123") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text(
                    "Type de contrainte",
                    style = MaterialTheme.typography.labelMedium
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DietaryRestriction.entries.forEach { restriction ->
                        FilterChip(
                            selected = selectedRestriction == restriction,
                            onClick = { selectedRestriction = restriction },
                            label = { Text(getDietaryRestrictionLabel(restriction)) }
                        )
                    }
                }
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optionnel)") },
                    placeholder = { Text("Détails supplémentaires") },
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
                        participantId.isBlank() -> errorMessage = "ID participant requis"
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
                Text("Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

fun getDietaryRestrictionLabel(restriction: DietaryRestriction): String = when (restriction) {
    DietaryRestriction.VEGETARIAN -> "Végétarien"
    DietaryRestriction.VEGAN -> "Végétalien"
    DietaryRestriction.GLUTEN_FREE -> "Sans gluten"
    DietaryRestriction.LACTOSE_INTOLERANT -> "Intolérant lactose"
    DietaryRestriction.NUT_ALLERGY -> "Allergie noix"
    DietaryRestriction.SHELLFISH_ALLERGY -> "Allergie fruits de mer"
    DietaryRestriction.KOSHER -> "Casher"
    DietaryRestriction.HALAL -> "Halal"
    DietaryRestriction.DIABETIC -> "Diabétique"
    DietaryRestriction.OTHER -> "Autre"
}
