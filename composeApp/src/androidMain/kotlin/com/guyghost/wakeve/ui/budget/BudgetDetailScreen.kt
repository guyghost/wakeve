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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.budget.BudgetRepository
import com.guyghost.wakeve.comment.CommentRepository
import com.guyghost.wakeve.models.Budget
import com.guyghost.wakeve.models.BudgetCategory
import com.guyghost.wakeve.models.BudgetItem
import com.guyghost.wakeve.models.CommentSection
import com.guyghost.wakeve.R

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
    onNavigateToComments: (eventId: String, section: CommentSection, sectionItemId: String?) -> Unit,
    pendingSync: Boolean = false,
    isOnline: Boolean = true,
    isReadOnly: Boolean = false,
    canManageBudget: Boolean = !isReadOnly,
    canMutateBudget: Boolean = canManageBudget && !isReadOnly
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
                title = { Text(budgetDetailTitle()) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, budgetDetailBackContentDescription())
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
                                    Icons.AutoMirrored.Outlined.Comment,
                                    contentDescription = budgetCommentContentDescription(commentCount)
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
            if (canMutateBudget) {
                FloatingActionButton(
                    onClick = { showAddItemDialog = true }
                ) {
                    Icon(Icons.Default.Add, budgetAddItemContentDescription())
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Summary header
            OfflinePendingSyncBanner(pendingSync = pendingSync, isOnline = isOnline)

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
                            text = budgetDetailEmptyMessage(items.isEmpty()),
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
                            currentUserId = currentUserId,
                            canMutateBudget = canMutateBudget,
                            onEdit = { if (canMutateBudget) itemToEdit = item },
                            onDelete = { if (canMutateBudget) itemToDelete = item },
                            onMarkPaid = {
                                if (!canMutateBudget) return@BudgetItemCard
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
        if (showAddItemDialog && canMutateBudget) {
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

        itemToEdit?.takeIf { canMutateBudget }?.let { item ->
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
        
        itemToDelete?.takeIf { canMutateBudget }?.let { item ->
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text(budgetDeleteItemTitle()) },
                text = { Text(budgetDeleteItemMessage(item.name)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            budgetRepository.deleteBudgetItem(item.id)
                            itemToDelete = null
                            loadData()
                        }
                    ) {
                        Text(budgetDeleteActionLabel(), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDelete = null }) {
                        Text(budgetCancelActionLabel())
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
                    text = budgetDetailTotalLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(R.string.budget_amount_ratio, budget.totalActual, budget.totalEstimated),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            if (budget.isOverBudget) {
                AssistChip(
                    onClick = {},
                    label = { Text(budgetDetailOverBudgetLabel()) },
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
                text = budgetCategoryFilterTitle(),
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
                    label = { Text(budgetAllCategoriesLabel()) }
                )
                
                BudgetCategory.values().forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { 
                            onCategorySelected(if (selectedCategory == category) null else category)
                        },
                        label = { 
                            Text(budgetCategoryLabel(category))
                        }
                    )
                }
            }
        }
        
        // Payment status filters
        item {
            Text(
                text = budgetStatusFilterTitle(),
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
                    label = { Text(budgetPaidFilterLabel()) },
                    leadingIcon = if (showPaidOnly) {
                        { Icon(Icons.Default.CheckCircle, null) }
                    } else null
                )
                
                FilterChip(
                    selected = showUnpaidOnly,
                    onClick = { onUnpaidFilterChanged(!showUnpaidOnly) },
                    label = { Text(budgetUnpaidFilterLabel()) },
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
    currentUserId: String,
    canMutateBudget: Boolean,
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
                        text = if (item.isPaid) {
                            stringResource(R.string.currency_amount, String.format("%.2f", item.actualCost))
                        } else {
                            stringResource(R.string.budget_estimated_amount, item.estimatedCost)
                        },
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
                            contentDescription = budgetItemPaidContentDescription(),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Participants
            if (item.sharedBy.isNotEmpty()) {
                Text(
                    text = budgetSharedByLabel(item.sharedBy.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (item.isPaid && item.paidBy != null) {
                    Text(
                        text = budgetPaidByLabel(item.paidBy, currentUserId),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Cost per person
            Text(
                text = budgetCostPerPersonLabel(item.costPerPerson),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (canMutateBudget && !item.isPaid) {
                    val paidState = stringResource(R.string.a11y_budget_unpaid_state, item.name)
                    val markPaidDescription = stringResource(R.string.a11y_budget_mark_paid, item.name, paidState)
                    AssistChip(
                        onClick = onMarkPaid,
                        modifier = Modifier.semantics { contentDescription = markPaidDescription },
                        label = { Text(budgetMarkPaidActionLabel()) },
                        leadingIcon = { Icon(Icons.Default.CheckCircle, null) }
                    )
                }
                
                if (canMutateBudget) {
                    AssistChip(
                        onClick = onEdit,
                        label = { Text(budgetEditActionLabel()) },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )

                    AssistChip(
                        onClick = onDelete,
                        label = { Text(budgetDeleteActionLabel()) },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = MaterialTheme.colorScheme.error
                        )
                    )
                }
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
    val missingNameMessage = budgetItemMissingNameMessage()
    val invalidCostMessage = budgetItemInvalidCostMessage()
    val saveFailureMessage = budgetItemSaveFailureMessage()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(budgetItemDialogTitle(isNewItem = existingItem == null)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(budgetItemNameLabel()) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null && name.isBlank()
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(budgetItemDescriptionLabel()) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = estimatedCost,
                    onValueChange = { estimatedCost = it },
                    label = { Text(budgetItemEstimatedCostLabel()) },
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
                        value = budgetCategoryLabel(selectedCategory),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(budgetItemCategoryLabel()) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(
                                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = true
                            )
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        BudgetCategory.values().forEach { category ->
                            DropdownMenuItem(
                                text = { Text(budgetCategoryLabel(category)) },
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
                        errorMessage = missingNameMessage
                        return@TextButton
                    }
                    
                    val cost = estimatedCost.toDoubleOrNull()
                    if (cost == null || cost < 0) {
                        errorMessage = invalidCostMessage
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
                        errorMessage = saveFailureMessage
                    }
                }
            ) {
                Text(budgetItemDialogConfirmLabel(isNewItem = existingItem == null))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(budgetCancelActionLabel())
            }
        }
    )
}

@Composable internal fun budgetDetailTitle(): String = stringResource(R.string.budget_detail_title)

@Composable internal fun budgetDetailBackContentDescription(): String = stringResource(R.string.budget_back_action)

@Composable internal fun budgetCommentContentDescription(commentCount: Int): String =
    if (commentCount <= 0) stringResource(R.string.a11y_budget_comments_empty)
    else pluralStringResource(R.plurals.a11y_budget_comments_count, commentCount, commentCount)

@Composable internal fun budgetAddItemContentDescription(): String = stringResource(R.string.a11y_budget_add_item)

@Composable internal fun budgetDetailEmptyMessage(noItems: Boolean): String = stringResource(
    if (noItems) R.string.budget_empty_items else R.string.budget_empty_filtered
)

@Composable internal fun budgetDeleteItemTitle(): String = stringResource(R.string.budget_delete_item_title)

@Composable internal fun budgetDeleteItemMessage(itemName: String): String = stringResource(R.string.budget_delete_item_message, itemName)

@Composable internal fun budgetDeleteActionLabel(): String = stringResource(R.string.budget_delete_action)

@Composable internal fun budgetCancelActionLabel(): String = stringResource(R.string.budget_cancel_action)

@Composable internal fun budgetDetailTotalLabel(): String = stringResource(R.string.budget_total_label)

@Composable internal fun budgetDetailOverBudgetLabel(): String = stringResource(R.string.budget_over_limit)

@Composable internal fun budgetCategoryFilterTitle(): String = stringResource(R.string.budget_category_filter)

@Composable internal fun budgetAllCategoriesLabel(): String = stringResource(R.string.budget_all_categories)

@Composable internal fun budgetStatusFilterTitle(): String = stringResource(R.string.budget_status_filter)

@Composable internal fun budgetPaidFilterLabel(): String = stringResource(R.string.budget_paid_filter)

@Composable internal fun budgetUnpaidFilterLabel(): String = stringResource(R.string.budget_unpaid_filter)

@Composable internal fun budgetCategoryLabel(category: BudgetCategory): String = stringResource(when (category) {
    BudgetCategory.TRANSPORT -> R.string.budget_category_transport
    BudgetCategory.ACCOMMODATION -> R.string.budget_category_accommodation
    BudgetCategory.MEALS -> R.string.budget_category_meals
    BudgetCategory.ACTIVITIES -> R.string.budget_category_activities
    BudgetCategory.EQUIPMENT -> R.string.budget_category_equipment
    BudgetCategory.OTHER -> R.string.budget_category_other
})

@Composable internal fun budgetItemPaidContentDescription(): String = stringResource(R.string.a11y_budget_item_paid)

@Composable internal fun budgetSharedByLabel(sharedByCount: Int): String =
    pluralStringResource(R.plurals.budget_shared_by, sharedByCount, sharedByCount)

@Composable internal fun budgetPaidByLabel(paidBy: String?, currentUserId: String): String = stringResource(
    if (paidBy != null && paidBy == currentUserId) R.string.budget_paid_by_you else R.string.budget_paid_by_participant
)

@Composable internal fun budgetCostPerPersonLabel(costPerPerson: Double): String = stringResource(R.string.budget_cost_per_person, costPerPerson)

@Composable internal fun budgetMarkPaidActionLabel(): String = stringResource(R.string.budget_mark_paid_action)

@Composable internal fun budgetEditActionLabel(): String = stringResource(R.string.budget_edit_action)

@Composable internal fun budgetItemDialogTitle(isNewItem: Boolean): String = stringResource(if (isNewItem) R.string.budget_add_item_title else R.string.budget_edit_item_title)

@Composable internal fun budgetItemNameLabel(): String = stringResource(R.string.budget_item_name)

@Composable internal fun budgetItemDescriptionLabel(): String = stringResource(R.string.budget_item_description)

@Composable internal fun budgetItemEstimatedCostLabel(): String = stringResource(R.string.budget_item_estimated_cost)

@Composable internal fun budgetItemCategoryLabel(): String = stringResource(R.string.budget_item_category)

@Composable internal fun budgetItemMissingNameMessage(): String = stringResource(R.string.budget_item_name_required)

@Composable internal fun budgetItemInvalidCostMessage(): String = stringResource(R.string.budget_item_cost_invalid)

@Composable internal fun budgetItemDialogConfirmLabel(isNewItem: Boolean): String = stringResource(if (isNewItem) R.string.budget_add_action else R.string.budget_edit_action)

@Composable internal fun budgetItemSaveFailureMessage(): String = stringResource(R.string.budget_item_save_error)
