package com.guyghost.wakeve.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.budget.BudgetRepository
import com.guyghost.wakeve.comment.CommentRepository
import com.guyghost.wakeve.models.Budget
import com.guyghost.wakeve.models.BudgetCategory
import com.guyghost.wakeve.models.BudgetItem
import com.guyghost.wakeve.models.CommentSection

/**
 * Budget Detail Screen - Detailed list of budget items.
 * 
 * Features:
 * - List all budget items
 * - Filter by category
 * - Filter by paid/unpaid status
 * - Add new items (FAB)
 * - Edit/delete items
 * - Mark as paid
 * - Visual indicators for payment status
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDetailScreen(
    budgetId: String,
    budgetRepository: BudgetRepository,
    commentRepository: CommentRepository,
    currentUserId: String,
    eventParticipants: List<String>,
    onNavigateBack: () -> Unit,
    onNavigateToComments: (eventId: String, section: CommentSection, sectionItemId: String?) -> Unit
) {
    var items by remember { mutableStateOf<List<BudgetItem>>(emptyList()) }
    var budget by remember { mutableStateOf<Budget?>(null) }
    var selectedCategory by remember { mutableStateOf<BudgetCategory?>(null) }
    var showPaidOnly by remember { mutableStateOf(false) }
    var showUnpaidOnly by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<BudgetItem?>(null) }
    var itemToDelete by remember { mutableStateOf<BudgetItem?>(null) }
    var commentCount by remember { mutableIntStateOf(0) }
    
    // Load data
    fun loadData() {
        budget = budgetRepository.getBudgetById(budgetId)
        items = budgetRepository.getBudgetItems(budgetId)
        budget?.let {
            commentCount = commentRepository.countCommentsBySection(it.eventId, CommentSection.BUDGET).toInt()
        }
    }
    
    LaunchedEffect(budgetId) {
        loadData()
    }
    
    // Filtered items
    val filteredItems = items.filter { item ->
        val categoryMatch = selectedCategory == null || item.category == selectedCategory
        val paidMatch = when {
            showPaidOnly -> item.isPaid
            showUnpaidOnly -> !item.isPaid
            else -> true
        }
        categoryMatch && paidMatch
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détails du budget") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                actions = {
                    // Comments icon with badge
                    if (budget != null) {
                        IconButton(onClick = {
                            onNavigateToComments(budget!!.eventId, CommentSection.BUDGET, null)
                        }) {
                            Box {
                                Icon(
                                    Icons.Outlined.Comment,
                                    contentDescription = if (commentCount == 0) "Aucun commentaire" else "$commentCount commentaires"
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddItemDialog = true }
            ) {
                Icon(Icons.Default.Add, "Ajouter un item")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Summary header
            budget?.let {
                BudgetSummaryHeader(it)
            }
            
            // Filter chips
            BudgetFiltersRow(
                selectedCategory = selectedCategory,
                showPaidOnly = showPaidOnly,
                showUnpaidOnly = showUnpaidOnly,
                onCategorySelected = { selectedCategory = it },
                onPaidFilterChanged = { showPaidOnly = it; if (it) showUnpaidOnly = false },
                onUnpaidFilterChanged = { showUnpaidOnly = it; if (it) showPaidOnly = false }
            )
            
            // Items list
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (items.isEmpty()) "Aucun item de budget" else "Aucun item correspondant aux filtres",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        BudgetItemCard(
                            item = item,
                            onEdit = { itemToEdit = item },
                            onDelete = { itemToDelete = item },
                            onMarkPaid = {
                                // Show mark as paid dialog
                                // For now, just mark with estimated cost
                                budgetRepository.markItemAsPaid(
                                    itemId = item.id,
                                    actualCost = item.estimatedCost,
                                    paidBy = currentUserId
                                )
                                loadData()
                            }
                        )
                    }
                }
            }
        }
        
        // Dialogs
        if (showAddItemDialog) {
            BudgetItemDialog(
                budgetId = budgetId,
                budgetRepository = budgetRepository,
                currentUserId = currentUserId,
                eventParticipants = eventParticipants,
                onDismiss = {
                    showAddItemDialog = false
                    loadData()
                }
            )
        }

        itemToEdit?.let { item ->
            BudgetItemDialog(
                budgetId = budgetId,
                existingItem = item,
                budgetRepository = budgetRepository,
                currentUserId = currentUserId,
                eventParticipants = eventParticipants,
                onDismiss = {
                    itemToEdit = null
                    loadData()
                }
            )
        }
        
        itemToDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text("Supprimer l'item ?") },
                text = { Text("Voulez-vous vraiment supprimer '${item.name}' ?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            budgetRepository.deleteBudgetItem(item.id)
                            itemToDelete = null
                            loadData()
                        }
                    ) {
                        Text("Supprimer", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDelete = null }) {
                        Text("Annuler")
                    }
                }
            )
        }
    }
}

@Composable
private fun BudgetSummaryHeader(budget: Budget) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "%.2f € / %.2f €".format(budget.totalActual, budget.totalEstimated),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            if (budget.isOverBudget) {
                AssistChip(
                    onClick = {},
                    label = { Text("⚠️ Dépassement") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        labelColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetFiltersRow(
    selectedCategory: BudgetCategory?,
    showPaidOnly: Boolean,
    showUnpaidOnly: Boolean,
    onCategorySelected: (BudgetCategory?) -> Unit,
    onPaidFilterChanged: (Boolean) -> Unit,
    onUnpaidFilterChanged: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Category filters
        item {
            Text(
                text = "Catégories",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelected(null) },
                    label = { Text("Toutes") }
                )
                
                BudgetCategory.values().forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { 
                            onCategorySelected(if (selectedCategory == category) null else category)
                        },
                        label = { 
                            Text(
                                category.name.lowercase().replaceFirstChar { it.uppercase() }
                            )
                        }
                    )
                }
            }
        }
        
        // Payment status filters
        item {
            Text(
                text = "Statut",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showPaidOnly,
                    onClick = { onPaidFilterChanged(!showPaidOnly) },
                    label = { Text("Payés") },
                    leadingIcon = if (showPaidOnly) {
                        { Icon(Icons.Default.CheckCircle, null) }
                    } else null
                )
                
                FilterChip(
                    selected = showUnpaidOnly,
                    onClick = { onUnpaidFilterChanged(!showUnpaidOnly) },
                    label = { Text("Non payés") },
                    leadingIcon = if (showUnpaidOnly) {
                        { Icon(Icons.Default.Schedule, null) }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun BudgetItemCard(
    item: BudgetItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMarkPaid: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = when (item.category) {
                            BudgetCategory.TRANSPORT -> Icons.Default.DirectionsCar
                            BudgetCategory.ACCOMMODATION -> Icons.Default.Hotel
                            BudgetCategory.MEALS -> Icons.Default.Restaurant
                            BudgetCategory.ACTIVITIES -> Icons.Default.LocalActivity
                            BudgetCategory.EQUIPMENT -> Icons.Default.ShoppingBag
                            BudgetCategory.OTHER -> Icons.Default.MoreHoriz
                        },
                        contentDescription = null,
                        tint = if (item.isPaid) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Column {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (item.description.isNotBlank()) {
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (item.isPaid) "%.2f €".format(item.actualCost) else "~%.2f €".format(item.estimatedCost),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isPaid) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (item.isPaid) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Payé",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Participants
            if (item.sharedBy.isNotEmpty()) {
                Text(
                    text = "Partagé par ${item.sharedBy.size} participant${if (item.sharedBy.size > 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (item.isPaid && item.paidBy != null) {
                    Text(
                        text = "Payé par ${item.paidBy}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Cost per person
            Text(
                text = "%.2f € / personne".format(item.costPerPerson),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!item.isPaid) {
                    AssistChip(
                        onClick = onMarkPaid,
                        label = { Text("Marquer payé") },
                        leadingIcon = { Icon(Icons.Default.CheckCircle, null) }
                    )
                }
                
                AssistChip(
                    onClick = onEdit,
                    label = { Text("Modifier") },
                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                )
                
                AssistChip(
                    onClick = onDelete,
                    label = { Text("Supprimer") },
                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.error
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetItemDialog(
    budgetId: String,
    existingItem: BudgetItem? = null,
    budgetRepository: BudgetRepository,
    currentUserId: String,
    eventParticipants: List<String>,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existingItem?.name ?: "") }
    var description by remember { mutableStateOf(existingItem?.description ?: "") }
    var estimatedCost by remember { mutableStateOf(existingItem?.estimatedCost?.toString() ?: "") }
    var selectedCategory by remember { mutableStateOf(existingItem?.category ?: BudgetCategory.OTHER) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingItem == null) "Ajouter un item" else "Modifier l'item") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null && name.isBlank()
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = estimatedCost,
                    onValueChange = { estimatedCost = it },
                    label = { Text("Coût estimé (€)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null && estimatedCost.toDoubleOrNull() == null
                )
                
                // Category dropdown
                var expanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.name.lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Catégorie") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        BudgetCategory.values().forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Validation
                    if (name.isBlank()) {
                        errorMessage = "Le nom est requis"
                        return@TextButton
                    }
                    
                    val cost = estimatedCost.toDoubleOrNull()
                    if (cost == null || cost < 0) {
                        errorMessage = "Le coût doit être un nombre positif"
                        return@TextButton
                    }
                    
                    try {
                        if (existingItem == null) {
                            // Create new item
                            // Default to sharing with all event participants if no specific selection
                            val defaultSharedBy = if (eventParticipants.isEmpty()) listOf(currentUserId) else eventParticipants
                            budgetRepository.createBudgetItem(
                                budgetId = budgetId,
                                category = selectedCategory,
                                name = name,
                                description = description,
                                estimatedCost = cost,
                                sharedBy = defaultSharedBy
                            )
                        } else {
                            // Update existing item
                            val updated = existingItem.copy(
                                category = selectedCategory,
                                name = name,
                                description = description,
                                estimatedCost = cost
                            )
                            budgetRepository.updateBudgetItem(updated)
                        }
                        onDismiss()
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Erreur lors de la sauvegarde"
                    }
                }
            ) {
                Text(if (existingItem == null) "Ajouter" else "Modifier")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
