package com.guyghost.wakeve.ui.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.budget.BudgetRepository
import com.guyghost.wakeve.models.Budget
import com.guyghost.wakeve.models.BudgetCategory
import com.guyghost.wakeve.models.BudgetCategoryDetails
import com.guyghost.wakeve.models.BudgetWithItems

/**
 * Budget Overview Screen - Main budget dashboard.
 * 
 * Features:
 * - Budget summary (total estimated vs actual)
 * - Per-person cost breakdown
 * - Category breakdown with progress bars
 * - Participant balances
 * - Navigation to detailed views
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetOverviewScreen(
    eventId: String,
    budgetRepository: BudgetRepository,
    onNavigateToDetail: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var budgetState by remember { mutableStateOf<BudgetWithItems?>(null) }
    var participantCount by remember { mutableStateOf(3) } // TODO: Get from event
    var isLoading by remember { mutableStateOf(true) }
    
    // Load budget data
    LaunchedEffect(eventId) {
        val budget = budgetRepository.getBudgetByEventId(eventId)
        if (budget != null) {
            budgetState = budgetRepository.getBudgetWithItems(budget.id)
        }
        isLoading = false
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { budgetState?.let { onNavigateToDetail(it.budget.id) } }) {
                        Icon(Icons.Default.ViewList, "Voir détails")
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            budgetState == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Aucun budget pour cet événement")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val newBudget = budgetRepository.createBudget(eventId)
                                budgetState = budgetRepository.getBudgetWithItems(newBudget.id)
                            }
                        ) {
                            Text("Créer un budget")
                        }
                    }
                }
            }
            else -> {
                BudgetOverviewContent(
                    budgetWithItems = budgetState!!,
                    participantCount = participantCount,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun BudgetOverviewContent(
    budgetWithItems: BudgetWithItems,
    participantCount: Int,
    modifier: Modifier = Modifier
) {
    val budget = budgetWithItems.budget
    val categoryBreakdown = budgetWithItems.categoryBreakdown
    
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Total Budget Summary Card
        item {
            BudgetSummaryCard(budget, participantCount)
        }
        
        // Per-Person Card
        item {
            PerPersonCard(budget, participantCount)
        }
        
        // Status Card
        item {
            BudgetStatusCard(budget)
        }
        
        // Category Breakdown
        item {
            Text(
                text = "Répartition par catégorie",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        items(categoryBreakdown) { category ->
            CategoryBreakdownCard(category)
        }
    }
}

@Composable
private fun BudgetSummaryCard(
    budget: Budget,
    participantCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Budget Total",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Estimé",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%.2f €".format(budget.totalEstimated),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Dépensé",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%.2f €".format(budget.totalActual),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (budget.isOverBudget) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Progress Bar
            if (budget.totalEstimated > 0) {
                LinearProgressIndicator(
                    progress = (budget.totalActual / budget.totalEstimated).toFloat().coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = if (budget.isOverBudget)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "%.1f%% utilisé".format(budget.budgetUsagePercentage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PerPersonCard(
    budget: Budget,
    participantCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Par personne ($participantCount participants)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Estimé",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%.2f €".format(
                            if (participantCount > 0) budget.totalEstimated / participantCount else 0.0
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Dépensé",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%.2f €".format(
                            if (participantCount > 0) budget.totalActual / participantCount else 0.0
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetStatusCard(budget: Budget) {
    val remaining = budget.remainingBudget
    val statusColor = when {
        budget.isOverBudget -> MaterialTheme.colorScheme.error
        remaining < budget.totalEstimated * 0.1 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    
    val statusText = when {
        budget.isOverBudget -> "⚠️ Dépassement de budget"
        remaining < budget.totalEstimated * 0.1 -> "⚡ Budget presque épuisé"
        budget.totalActual == 0.0 -> "💡 Aucune dépense enregistrée"
        else -> "✓ Dans le budget"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
            
            if (!budget.isOverBudget && budget.totalActual > 0) {
                Text(
                    text = "Reste disponible : %.2f €".format(remaining),
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor
                )
            } else if (budget.isOverBudget) {
                Text(
                    text = "Dépassement : %.2f €".format(-remaining),
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(category: BudgetCategoryDetails) {
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (category.category) {
                            BudgetCategory.TRANSPORT -> Icons.Default.DirectionsCar
                            BudgetCategory.ACCOMMODATION -> Icons.Default.Hotel
                            BudgetCategory.MEALS -> Icons.Default.Restaurant
                            BudgetCategory.ACTIVITIES -> Icons.Default.LocalActivity
                            BudgetCategory.EQUIPMENT -> Icons.Default.ShoppingBag
                            BudgetCategory.OTHER -> Icons.Default.MoreHoriz
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Column {
                        Text(
                            text = category.category.name.lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${category.paidItemCount}/${category.itemCount} payés",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "%.2f €".format(category.actual),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (category.isOverBudget)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "/ %.2f €".format(category.estimated),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Progress bar
            if (category.estimated > 0) {
                LinearProgressIndicator(
                    progress = (category.actual / category.estimated).toFloat().coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = if (category.isOverBudget)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            }
            
            if (category.isOverBudget) {
                Text(
                    text = "⚠️ Dépassement : %.2f €".format(category.actual - category.estimated),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
