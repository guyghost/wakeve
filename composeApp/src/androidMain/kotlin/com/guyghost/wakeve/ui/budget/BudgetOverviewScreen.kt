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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.payment.PaymentPotRecord
import com.guyghost.wakeve.payment.PaymentPotRepository
import com.guyghost.wakeve.payment.TricountHandoffRecord
import com.guyghost.wakeve.payment.TricountHandoffRepository
import com.guyghost.wakeve.repository.EventRepositoryInterface
import com.guyghost.wakeve.budget.BudgetCalculator
import com.guyghost.wakeve.budget.BudgetRepository
import com.guyghost.wakeve.models.Budget
import com.guyghost.wakeve.models.BudgetCategory
import com.guyghost.wakeve.models.BudgetCategoryDetails
import com.guyghost.wakeve.models.BudgetItem
import com.guyghost.wakeve.models.BudgetWithItems
import com.guyghost.wakeve.R

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
                title = { Text(budgetOverviewTitle()) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, budgetBackContentDescription())
                    }
                },
                actions = {
                    IconButton(onClick = { budgetState?.let { onNavigateToDetail(it.budget.id) } }) {
                        Icon(Icons.AutoMirrored.Filled.ViewList, budgetDetailsContentDescription())
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
                        Text(budgetEmptyTitle())
                        Spacer(modifier = Modifier.height(16.dp))
                        if (canCreateBudget && !isReadOnly) {
                            Button(
                                enabled = canCreateBudget && !isReadOnly,
                                onClick = {
                                    val newBudget = budgetRepository.createBudget(eventId)
                                    budgetState = budgetRepository.getBudgetWithItems(newBudget.id)
                                }
                            ) {
                                Text(budgetCreateActionLabel())
                            }
                        } else {
                            Text(
                                text = budgetReadOnlyEmptyMessage(),
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
    val settlementSummary = budgetSettlementSummary(budgetWithItems.items)
    
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
            SettlementSummaryCard(summary = settlementSummary)
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
                text = budgetCategoryBreakdownTitle(),
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
private fun SettlementSummaryCard(summary: BudgetSettlementSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = summary.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = summary.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            summary.lines.forEach { line ->
                Text(
                    text = line.sentence,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
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
    val defaultPotTitle = paymentPotDefaultTitle()
    fun createPaymentPot() {
        if (!mutationsEnabled || pot != null) return
        pot = paymentPotRepository.createPot(
            eventId = eventId,
            organizerId = organizerId,
            goalAmount = 0.0,
            title = defaultPotTitle
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
                title = { Text(paymentPotScreenTitle()) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, budgetBackContentDescription())
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
                        Text(paymentPotScreenTitle(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            pot?.let { paymentPotActiveMessage(it.goalAmount, it.currency) }
                                ?: paymentPotInactiveMessage(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = ::createPaymentPot,
                            enabled = mutationsEnabled && pot == null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(paymentPotCreateActionLabel())
                        }
                        Button(
                            onClick = ::activatePaymentPot,
                            enabled = mutationsEnabled && pot == null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(paymentPotActivateActionLabel())
                        }
                        Button(
                            onClick = { pot?.let(onOpenPaymentPot) },
                            enabled = pot != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(paymentPotOpenActionLabel())
                        }
                        Button(
                            onClick = ::closePaymentPot,
                            enabled = mutationsEnabled && pot?.status == "ACTIVE",
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(paymentPotCloseActionLabel())
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
    val tricountOpenLabel = tricountOpenActionLabel()
    val safeExternalLink = remember(handoff, tricountOpenLabel) {
        handoff?.providerUrl?.let { url ->
            SafeExternalLink.sanitize(
                label = tricountOpenLabel,
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
                title = { Text(tricountScreenTitle()) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, budgetBackContentDescription())
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
                        Text(tricountLinkTitle(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (handoff == null) {
                                tricountMissingLinkMessage()
                            } else {
                                tricountLinkStatusMessage(
                                    safeExternalLink?.verificationStatus,
                                    handoff?.provider
                                )
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
                            Text(tricountLinkActionLabel())
                        }
                        Button(
                            onClick = ::unlinkTricount,
                            enabled = mutationsEnabled && handoff?.providerUrl != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(tricountUnlinkActionLabel())
                        }
                        Button(
                            onClick = ::markTricountNotNeeded,
                            enabled = mutationsEnabled,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(tricountNotNeededActionLabel())
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
                    verificationStatus = "verified",
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
                    pendingSync && !isOnline -> budgetPendingOfflineSyncMessage()
                    pendingSync -> budgetPendingSyncMessage()
                    else -> budgetOfflineAvailableMessage()
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
            Text(paymentPotScreenTitle(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(paymentPotEntryMessage(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = onOpenPaymentPot,
                enabled = canManagePayment || isReadOnly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(paymentPotOpenActionLabel())
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
            Text(tricountScreenTitle(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(tricountEntryMessage(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = onOpenTricount,
                enabled = canManageTricount || isReadOnly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(tricountOpenActionLabel())
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
                text = budgetTotalTitle(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = budgetEstimatedLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.currency_amount, String.format("%.2f", budget.totalEstimated)),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = budgetSpentLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.currency_amount, String.format("%.2f", budget.totalActual)),
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
                    progress = { (budget.totalActual / budget.totalEstimated).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = if (budget.isOverBudget)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = budgetUsageLabel(budget.budgetUsagePercentage),
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
                text = budgetPerPersonTitle(participantCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = budgetEstimatedLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.currency_amount, String.format("%.2f", if (participantCount > 0) budget.totalEstimated / participantCount else 0.0)),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = budgetSpentLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.currency_amount, String.format("%.2f", if (participantCount > 0) budget.totalActual / participantCount else 0.0)),
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
    
    val statusText = budgetStatusText(
        isOverBudget = budget.isOverBudget,
        remaining = remaining,
        totalEstimated = budget.totalEstimated,
        totalActual = budget.totalActual
    )
    
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
                    text = budgetRemainingLabel(remaining),
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor
                )
            } else if (budget.isOverBudget) {
                Text(
                    text = budgetOverspendLabel(-remaining),
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
                            text = budgetCategoryLabel(category.category),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.budget_paid_ratio, category.paidItemCount, category.itemCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.currency_amount, String.format("%.2f", category.actual)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (category.isOverBudget)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.budget_estimated_ratio_amount, category.estimated),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Progress bar
            if (category.estimated > 0) {
                LinearProgressIndicator(
                    progress = { (category.actual / category.estimated).toFloat().coerceIn(0f, 1f) },
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
                    text = stringResource(R.string.budget_category_overspend, category.actual - category.estimated),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

internal data class BudgetSettlementSummary(
    val title: String,
    val body: String,
    val lines: List<BudgetSettlementLine>
)

internal data class BudgetSettlementLine(
    val fromLabel: String,
    val toLabel: String,
    val amountLabel: String,
    val sentence: String
)

@Composable internal fun budgetSettlementSummary(
    items: List<BudgetItem>,
    maxLines: Int = 3
): BudgetSettlementSummary {
    val paidItems = items.filter { item ->
        item.isPaid && item.actualCost > 0.0 && item.paidBy != null && item.sharedBy.isNotEmpty()
    }
    if (paidItems.isEmpty()) {
        return BudgetSettlementSummary(
            title = budgetSettlementTitle(0, hasPaidItems = false),
            body = stringResource(R.string.budget_settlement_no_paid_items),
            lines = emptyList()
        )
    }

    val settlements = BudgetCalculator.calculateSettlements(paidItems)
        .sortedWith(compareByDescending<Triple<String, String, Double>> { it.third }.thenBy { it.first }.thenBy { it.second })

    if (settlements.isEmpty()) {
        return BudgetSettlementSummary(
            title = budgetSettlementTitle(0, hasPaidItems = true),
            body = stringResource(R.string.budget_settlement_balanced),
            lines = emptyList()
        )
    }

    val lines = settlements.take(maxLines).map { (from, to, amount) ->
        val fromLabel = budgetParticipantDisplayName(from)
        val toLabel = budgetParticipantDisplayName(to)
        val amountLabel = budgetMoneyLabel(amount)
        BudgetSettlementLine(
            fromLabel = fromLabel,
            toLabel = toLabel,
            amountLabel = amountLabel,
            sentence = budgetSettlementLineLabel(fromLabel, toLabel, amountLabel)
        )
    }

    return BudgetSettlementSummary(
        title = budgetSettlementTitle(settlements.size, hasPaidItems = true),
        body = budgetSettlementBody(settlements.size, lines.size),
        lines = lines
    )
}

@Composable internal fun budgetSettlementTitle(count: Int, hasPaidItems: Boolean): String = when {
    !hasPaidItems -> stringResource(R.string.budget_settlement_title)
    count == 0 -> stringResource(R.string.budget_settlement_up_to_date)
    else -> pluralStringResource(R.plurals.budget_settlement_count, count, count)
}

@Composable internal fun budgetSettlementBody(totalCount: Int, displayedCount: Int): String =
    if (totalCount > displayedCount) {
        stringResource(R.string.budget_settlement_priority_body, displayedCount, totalCount)
    } else {
        pluralStringResource(R.plurals.budget_settlement_body, totalCount, totalCount)
    }

@Composable internal fun budgetSettlementLineLabel(
    fromParticipant: String,
    toParticipant: String,
    amount: String
): String = stringResource(R.string.budget_settlement_line, fromParticipant, amount, toParticipant)

@Composable internal fun budgetParticipantDisplayName(participantId: String): String {
    val trimmed = participantId.trim()
    if (trimmed.isBlank()) return stringResource(R.string.budget_participant_fallback)

    val localPart = trimmed.substringBefore("@")
    val userPrefixMatch = Regex("^user[-_ ]?(.+)$", RegexOption.IGNORE_CASE).matchEntire(localPart)
    val rawLabel = userPrefixMatch?.groupValues?.get(1) ?: localPart
    val display = rawLabel
        .replace(Regex("[-_]+"), " ")
        .trim()
        .replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() }

    val fallback = stringResource(R.string.budget_participant_fallback)
    return if (userPrefixMatch != null) stringResource(R.string.budget_participant_numbered, display) else display.ifBlank { fallback }
}

@Composable internal fun budgetMoneyLabel(amount: Double): String = stringResource(R.string.currency_amount, String.format("%.2f", amount))

@Composable internal fun budgetOverviewTitle(): String = stringResource(R.string.budget_overview_title)

@Composable internal fun budgetBackContentDescription(): String = stringResource(R.string.budget_back_action)

@Composable internal fun budgetDetailsContentDescription(): String = stringResource(R.string.a11y_budget_view_details)

@Composable internal fun budgetEmptyTitle(): String = stringResource(R.string.budget_empty_title)

@Composable internal fun budgetCreateActionLabel(): String = stringResource(R.string.budget_create_action)

@Composable internal fun budgetReadOnlyEmptyMessage(): String = stringResource(R.string.budget_read_only_empty)

@Composable internal fun paymentPotDefaultTitle(): String = stringResource(R.string.budget_payment_pot_default_title)

@Composable internal fun paymentPotScreenTitle(): String = stringResource(R.string.budget_payment_pot_title)

@Composable internal fun paymentPotActiveMessage(goalAmount: Double, currency: String): String = stringResource(R.string.budget_payment_pot_active, goalAmount, currency)

@Composable internal fun paymentPotInactiveMessage(): String = stringResource(R.string.budget_payment_pot_inactive)

@Composable internal fun paymentPotCreateActionLabel(): String = stringResource(R.string.budget_payment_pot_create)

@Composable internal fun paymentPotActivateActionLabel(): String = stringResource(R.string.budget_payment_pot_activate)

@Composable internal fun paymentPotOpenActionLabel(): String = stringResource(R.string.budget_payment_pot_open)

@Composable internal fun paymentPotCloseActionLabel(): String = stringResource(R.string.budget_payment_pot_close)

@Composable internal fun paymentPotEntryMessage(): String = stringResource(R.string.budget_payment_pot_entry)

@Composable internal fun tricountScreenTitle(): String = stringResource(R.string.budget_tricount_title)

@Composable internal fun tricountOpenActionLabel(): String = stringResource(R.string.budget_tricount_open)

@Composable internal fun tricountLinkTitle(): String = stringResource(R.string.budget_tricount_link_title)

@Composable internal fun tricountMissingLinkMessage(): String = stringResource(R.string.budget_tricount_missing_link)

@Composable internal fun tricountLinkStatusMessage(verificationStatus: String?, provider: String?): String = stringResource(
    R.string.budget_tricount_link_status,
    if (verificationStatus == "verified") stringResource(R.string.budget_tricount_verified) else stringResource(R.string.budget_tricount_unverified),
    provider ?: stringResource(R.string.budget_tricount_title)
)

@Composable internal fun tricountLinkActionLabel(): String = stringResource(R.string.budget_tricount_link_action)

@Composable internal fun tricountUnlinkActionLabel(): String = stringResource(R.string.budget_tricount_unlink_action)

@Composable internal fun tricountNotNeededActionLabel(): String = stringResource(R.string.budget_tricount_not_needed)

@Composable internal fun tricountEntryMessage(): String = stringResource(R.string.budget_tricount_entry)

@Composable internal fun budgetPendingOfflineSyncMessage(): String = stringResource(R.string.budget_pending_offline_sync)

@Composable internal fun budgetPendingSyncMessage(): String = stringResource(R.string.budget_pending_sync)

@Composable internal fun budgetOfflineAvailableMessage(): String = stringResource(R.string.budget_offline_available)

@Composable internal fun budgetCategoryBreakdownTitle(): String = stringResource(R.string.budget_category_breakdown)

@Composable internal fun budgetTotalTitle(): String = stringResource(R.string.budget_total_title)

@Composable internal fun budgetEstimatedLabel(): String = stringResource(R.string.budget_estimated_label)

@Composable internal fun budgetSpentLabel(): String = stringResource(R.string.budget_spent_label)

@Composable internal fun budgetUsageLabel(usagePercentage: Double): String = stringResource(R.string.budget_usage, usagePercentage)

@Composable internal fun budgetPerPersonTitle(participantCount: Int): String = pluralStringResource(R.plurals.budget_per_person_title, participantCount, participantCount)

@Composable internal fun budgetStatusText(
    isOverBudget: Boolean,
    remaining: Double,
    totalEstimated: Double,
    totalActual: Double
): String {
    return when {
        isOverBudget -> stringResource(R.string.budget_status_over)
        remaining < totalEstimated * 0.1 -> stringResource(R.string.budget_status_nearly_used)
        totalActual == 0.0 -> stringResource(R.string.budget_status_empty)
        else -> stringResource(R.string.budget_status_within)
    }
}

@Composable internal fun budgetRemainingLabel(remaining: Double): String = stringResource(R.string.budget_remaining, remaining)

@Composable internal fun budgetOverspendLabel(overspend: Double): String = stringResource(R.string.budget_overspend, overspend)
