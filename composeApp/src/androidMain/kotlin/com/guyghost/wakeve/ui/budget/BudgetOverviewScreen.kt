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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sync
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
import com.guyghost.wakeve.payment.PaymentPotRecord
import com.guyghost.wakeve.payment.PaymentPotRepository
import com.guyghost.wakeve.payment.TricountHandoffRecord
import com.guyghost.wakeve.payment.TricountHandoffRepository
import com.guyghost.wakeve.repository.EventRepositoryInterface
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
    eventRepository: EventRepositoryInterface,
    onNavigateToDetail: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenPaymentPot: () -> Unit = {},
    onOpenTricount: () -> Unit = {},
    pendingSync: Boolean = false,
    isOnline: Boolean = true,
    isReadOnly: Boolean = false,
    canCreateBudget: Boolean = !isReadOnly,
    canManagePayment: Boolean = !isReadOnly,
    canManageTricount: Boolean = !isReadOnly
) {
    var budgetState by remember { mutableStateOf<BudgetWithItems?>(null) }
    var participantCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    // Load budget data and participant count
    LaunchedEffect(eventId) {
        val budget = budgetRepository.getBudgetByEventId(eventId)
        if (budget != null) {
            budgetState = budgetRepository.getBudgetWithItems(budget.id)
        }
        // Get participant count from event
        val event = eventRepository.getEvent(eventId)
        participantCount = event?.participants?.size ?: 0
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
                        if (canCreateBudget && !isReadOnly) {
                            Button(
                                enabled = canCreateBudget && !isReadOnly,
                                onClick = {
                                    val newBudget = budgetRepository.createBudget(eventId)
                                    budgetState = budgetRepository.getBudgetWithItems(newBudget.id)
                                }
                            ) {
                                Text("Créer un budget")
                            }
                        } else {
                            Text(
                                text = "Le budget est consultable une fois créé par l'organisateur.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            else -> {
                BudgetOverviewContent(
                    budgetWithItems = budgetState!!,
                    participantCount = participantCount,
                    pendingSync = pendingSync,
                    isOnline = isOnline,
                    onOpenPaymentPot = onOpenPaymentPot,
                    onOpenTricount = onOpenTricount,
                    isReadOnly = isReadOnly,
                    canManagePayment = canManagePayment,
                    canManageTricount = canManageTricount,
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
    pendingSync: Boolean,
    isOnline: Boolean,
    onOpenPaymentPot: () -> Unit,
    onOpenTricount: () -> Unit,
    isReadOnly: Boolean,
    canManagePayment: Boolean,
    canManageTricount: Boolean,
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
            OfflinePendingSyncBanner(pendingSync = pendingSync, isOnline = isOnline)
        }

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

        item {
            PaymentPotEntryCard(
                onOpenPaymentPot = onOpenPaymentPot,
                isReadOnly = isReadOnly,
                canManagePayment = canManagePayment
            )
        }

        item {
            TricountEntryCard(
                onOpenTricount = onOpenTricount,
                isReadOnly = isReadOnly,
                canManageTricount = canManageTricount
            )
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
@OptIn(ExperimentalMaterial3Api::class)
fun PaymentPotScreen(
    eventId: String,
    organizerId: String,
    isOrganizer: Boolean,
    isReadOnly: Boolean = false,
    canManagePayment: Boolean = isOrganizer && !isReadOnly,
    paymentPotRepository: PaymentPotRepository,
    pendingSync: Boolean,
    isOnline: Boolean,
    onOpenPaymentPot: (PaymentPotRecord) -> Unit = {},
    onNavigateBack: () -> Unit
) {
    var pot by remember(eventId) {
        mutableStateOf<PaymentPotRecord?>(paymentPotRepository.getActivePotForEvent(eventId))
    }
    val mutationsEnabled = canManagePayment && !isReadOnly
    fun createPaymentPot() {
        if (!mutationsEnabled || pot != null) return
        pot = paymentPotRepository.createPot(
            eventId = eventId,
            organizerId = organizerId,
            goalAmount = 0.0,
            title = "Cagnotte $eventId"
        )
    }
    fun activatePaymentPot() {
        if (!mutationsEnabled || pot != null) return
        createPaymentPot()
    }
    fun closePaymentPot() {
        val activePot = pot ?: return
        if (!mutationsEnabled) return
        pot = paymentPotRepository.closePot(activePot.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cagnotte") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { OfflinePendingSyncBanner(pendingSync = pendingSync, isOnline = isOnline) }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null)
                        Text("Cagnotte", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            pot?.let { "Cagnotte active - ${it.goalAmount} ${it.currency}. Les changements sont enregistrés sur cet appareil." }
                                ?: "Aucune cagnotte active pour l'événement $eventId.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = ::createPaymentPot,
                            enabled = mutationsEnabled && pot == null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Créer une cagnotte")
                        }
                        Button(
                            onClick = ::activatePaymentPot,
                            enabled = mutationsEnabled && pot == null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Activer la cagnotte")
                        }
                        Button(
                            onClick = { pot?.let(onOpenPaymentPot) },
                            enabled = pot != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ouvrir la cagnotte")
                        }
                        Button(
                            onClick = ::closePaymentPot,
                            enabled = mutationsEnabled && pot?.status == "ACTIVE",
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Clôturer la cagnotte")
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TricountHandoffScreen(
    eventId: String,
    currentUserId: String,
    isOrganizer: Boolean,
    isReadOnly: Boolean = false,
    canManageTricount: Boolean = isOrganizer && !isReadOnly,
    tricountHandoffRepository: TricountHandoffRepository,
    pendingSync: Boolean,
    isOnline: Boolean,
    onOpenSafeTricount: (SafeExternalLink) -> Unit = {},
    onNavigateBack: () -> Unit
) {
    var handoff by remember(eventId) {
        mutableStateOf<TricountHandoffRecord?>(null)
    }
    val safeExternalLink = remember(handoff) {
        handoff?.providerUrl?.let { url ->
            SafeExternalLink.sanitize(
                label = "Ouvrir Tricount",
                provider = "TRICOUNT",
                rawUrl = url,
                verifier = tricountHandoffRepository::isTrustedProviderUrl
            )
        }
    }

    LaunchedEffect(eventId) {
        handoff = tricountHandoffRepository.getHandoff(eventId)
    }
    val safeUrlOpener = remember(onOpenSafeTricount) {
        SafeUrlOpener { link -> onOpenSafeTricount(link) }
    }
    val mutationsEnabled = canManageTricount && !isReadOnly
    fun linkTricount() {
        if (!mutationsEnabled) return
        handoff = tricountHandoffRepository.linkHandoff(
            eventId = eventId,
            provider = "TRICOUNT",
            providerId = "tricount-$eventId",
            providerUrl = "https://tricount.com/group/$eventId",
            syncStatus = "LINKED"
        )
    }
    fun unlinkTricount() {
        if (!mutationsEnabled) return
        tricountHandoffRepository.unlinkHandoff(eventId)
        handoff = null
    }
    fun markTricountNotNeeded() {
        if (!mutationsEnabled) return
        handoff = tricountHandoffRepository.markNotNeeded(eventId, currentUserId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tricount") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { OfflinePendingSyncBanner(pendingSync = pendingSync, isOnline = isOnline) }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null)
                        Text("Lien Tricount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (handoff == null) {
                                "Aucun lien Tricount n'est encore associé. Ajoutez un lien vérifié avant de rediriger les participants."
                            } else {
                                "Lien ${safeExternalLink?.verificationStatus ?: "non vérifié"} pour ${handoff?.provider ?: "Tricount"}."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (safeExternalLink?.isVerified == true) {
                            Button(onClick = { safeExternalLink?.let(safeUrlOpener::openSafeUrl) }) {
                                Text(safeExternalLink.label)
                            }
                        }
                        Button(
                            onClick = ::linkTricount,
                            enabled = mutationsEnabled,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Associer Tricount")
                        }
                        Button(
                            onClick = ::unlinkTricount,
                            enabled = mutationsEnabled && handoff?.providerUrl != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Dissocier Tricount")
                        }
                        Button(
                            onClick = ::markTricountNotNeeded,
                            enabled = mutationsEnabled,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Tricount non requis")
                        }
                    }
                }
            }
        }
    }
}

fun interface SafeUrlOpener {
    fun openSafeUrl(link: SafeExternalLink)
}

data class SafeExternalLink(
    val label: String,
    val provider: String,
    val validatedURL: String,
    val verificationStatus: String,
    val isVerified: Boolean
) {
    companion object {
        fun sanitize(
            label: String,
            provider: String,
            rawUrl: String,
            verifier: (String, String) -> Boolean
        ): SafeExternalLink? {
            val trimmed = rawUrl.trim()
            return if (verifier(provider, trimmed)) {
                SafeExternalLink(
                    label = label,
                    provider = provider,
                    validatedURL = trimmed,
                    verificationStatus = "vérifié",
                    isVerified = true
                )
            } else {
                null
            }
        }
    }
}

@Composable
fun OfflinePendingSyncBanner(
    pendingSync: Boolean,
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    if (!pendingSync && isOnline) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Sync, contentDescription = null)
            Text(
                text = when {
                    pendingSync && !isOnline -> "Synchronisation en attente. Modifications locales en attente d'envoi."
                    pendingSync -> "Modifications locales en attente d'envoi."
                    else -> "Données locales disponibles hors ligne."
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun PaymentPotEntryCard(
    onOpenPaymentPot: () -> Unit,
    isReadOnly: Boolean,
    canManagePayment: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Cagnotte", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Suivre la cagnotte et les contributions participants.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = onOpenPaymentPot,
                enabled = canManagePayment || isReadOnly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ouvrir la cagnotte")
            }
        }
    }
}

@Composable
private fun TricountEntryCard(
    onOpenTricount: () -> Unit,
    isReadOnly: Boolean,
    canManageTricount: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Tricount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Préparer le règlement partagé via un lien vérifié.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = onOpenTricount,
                enabled = canManageTricount || isReadOnly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ouvrir Tricount")
            }
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
