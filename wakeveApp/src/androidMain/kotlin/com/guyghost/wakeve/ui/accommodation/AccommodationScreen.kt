package com.guyghost.wakeve.ui.accommodation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.accommodation.AccommodationRepository
import com.guyghost.wakeve.comment.CommentRepository
import com.guyghost.wakeve.models.Accommodation
import com.guyghost.wakeve.models.AccommodationType
import com.guyghost.wakeve.models.BookingStatus
import com.guyghost.wakeve.models.CommentSection
import java.util.UUID

/**
 * Accommodation Management Screen
 * 
 * Features:
 * - List of all accommodations for an event
 * - Add/Edit/Delete accommodation
 * - Room assignment interface
 * - Cost per person display
 * - Booking status tracking
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccommodationScreen(
    eventId: String,
    accommodationRepository: AccommodationRepository,
    commentRepository: CommentRepository,
    onNavigateBack: () -> Unit,
    onNavigateToComments: (eventId: String, section: CommentSection, sectionItemId: String?) -> Unit
) {
    var accommodations by remember { mutableStateOf<List<Accommodation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedAccommodation by remember { mutableStateOf<Accommodation?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var commentCount by remember { mutableIntStateOf(0) }

    // Load accommodations from repository
    LaunchedEffect(eventId) {
        isLoading = true
        accommodations = accommodationRepository.getAccommodationsByEventId(eventId)
        commentCount = commentRepository.countCommentsBySection(eventId, CommentSection.ACCOMMODATION).toInt()
        isLoading = false
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hébergement") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Retour")
                    }
                },
                actions = {
                    // Comments icon with badge
                    IconButton(onClick = {
                        onNavigateToComments(eventId, CommentSection.ACCOMMODATION, null)
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
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.Add, "Ajouter hébergement")
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (accommodations.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Hotel,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Aucun hébergement",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "Ajoutez votre premier hébergement",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ajouter")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(accommodations) { accommodation ->
                    AccommodationCard(
                        accommodation = accommodation,
                        onEdit = { 
                            selectedAccommodation = accommodation
                            showAddDialog = true
                        },
                        onDelete = {
                            selectedAccommodation = accommodation
                            showDeleteDialog = true
                        },
                        onClick = { /* Navigate to room assignment */ }
                    )
                }
            }
        }
    }
    
    // Add/Edit Dialog
    if (showAddDialog) {
        AccommodationDialog(
            accommodation = selectedAccommodation,
            eventId = eventId,
            onDismiss = {
                showAddDialog = false
                selectedAccommodation = null
            },
            onSave = { newAccommodation ->
                if (selectedAccommodation == null) {
                    // Create new accommodation
                    accommodationRepository.createAccommodation(newAccommodation)
                } else {
                    // Update existing accommodation
                    accommodationRepository.updateAccommodation(newAccommodation)
                }
                accommodations = accommodationRepository.getAccommodationsByEventId(eventId)
                showAddDialog = false
                selectedAccommodation = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                selectedAccommodation = null
            },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Supprimer l'hébergement ?") },
            text = { Text("Cette action est irréversible. Toutes les affectations de chambres seront également supprimées.") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedAccommodation?.let { accommodation ->
                            accommodationRepository.deleteAccommodation(accommodation.id)
                            accommodations = accommodationRepository.getAccommodationsByEventId(eventId)
                        }
                        showDeleteDialog = false
                        selectedAccommodation = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        selectedAccommodation = null
                    }
                ) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun AccommodationCard(
    accommodation: Accommodation,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with name and type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = accommodation.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            getAccommodationTypeIcon(accommodation.type),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = getAccommodationTypeLabel(accommodation.type),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Booking status badge
                BookingStatusBadge(status = accommodation.bookingStatus)
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Details grid
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Address
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = accommodation.address,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Dates
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${accommodation.checkInDate} → ${accommodation.checkOutDate} (${accommodation.totalNights} nuits)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // Capacity
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Capacité: ${accommodation.capacity} personnes",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // Cost
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AttachMoney,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formatPrice(accommodation.totalCost),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "(${formatPrice(accommodation.pricePerNight)}/nuit)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Notes if present
            accommodation.notes?.let { notes ->
                if (notes.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Actions
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Modifier")
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Supprimer")
                }
            }
        }
    }
}

@Composable
fun BookingStatusBadge(status: BookingStatus) {
    val (color, label) = when (status) {
        BookingStatus.SEARCHING -> MaterialTheme.colorScheme.surfaceVariant to "Recherche"
        BookingStatus.RESERVED -> Color(0xFFF59E0B) to "Réservé"
        BookingStatus.CONFIRMED -> Color(0xFF059669) to "Confirmé"
        BookingStatus.CANCELLED -> MaterialTheme.colorScheme.error to "Annulé"
    }
    
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccommodationDialog(
    accommodation: Accommodation?,
    eventId: String,
    onDismiss: () -> Unit,
    onSave: (Accommodation) -> Unit
) {
    var name by remember { mutableStateOf(accommodation?.name ?: "") }
    var selectedType by remember { mutableStateOf(accommodation?.type ?: AccommodationType.HOTEL) }
    var address by remember { mutableStateOf(accommodation?.address ?: "") }
    var capacity by remember { mutableStateOf(accommodation?.capacity?.toString() ?: "") }
    var pricePerNight by remember { mutableStateOf((accommodation?.pricePerNight?.div(100))?.toString() ?: "") }
    var totalNights by remember { mutableStateOf(accommodation?.totalNights?.toString() ?: "") }
    var checkInDate by remember { mutableStateOf(accommodation?.checkInDate ?: "") }
    var checkOutDate by remember { mutableStateOf(accommodation?.checkOutDate ?: "") }
    var bookingUrl by remember { mutableStateOf(accommodation?.bookingUrl ?: "") }
    var notes by remember { mutableStateOf(accommodation?.notes ?: "") }
    var selectedStatus by remember { mutableStateOf(accommodation?.bookingStatus ?: BookingStatus.SEARCHING) }
    var showTypeDropdown by remember { mutableStateOf(false) }
    var showStatusDropdown by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (accommodation == null) "Ajouter hébergement" else "Modifier hébergement") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nom") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                item {
                    // Type dropdown
                    ExposedDropdownMenuBox(
                        expanded = showTypeDropdown,
                        onExpandedChange = { showTypeDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = getAccommodationTypeLabel(selectedType),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTypeDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = showTypeDropdown,
                            onDismissRequest = { showTypeDropdown = false }
                        ) {
                            AccommodationType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(getAccommodationTypeLabel(type)) },
                                    onClick = {
                                        selectedType = type
                                        showTypeDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Adresse") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
                
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = capacity,
                            onValueChange = { capacity = it.filter { c -> c.isDigit() } },
                            label = { Text("Capacité") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = totalNights,
                            onValueChange = { totalNights = it.filter { c -> c.isDigit() } },
                            label = { Text("Nuits") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = pricePerNight,
                        onValueChange = { pricePerNight = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Prix/nuit (€)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = checkInDate,
                            onValueChange = { checkInDate = it },
                            label = { Text("Check-in") },
                            placeholder = { Text("YYYY-MM-DD") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = checkOutDate,
                            onValueChange = { checkOutDate = it },
                            label = { Text("Check-out") },
                            placeholder = { Text("YYYY-MM-DD") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
                
                item {
                    // Status dropdown
                    ExposedDropdownMenuBox(
                        expanded = showStatusDropdown,
                        onExpandedChange = { showStatusDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = getBookingStatusLabel(selectedStatus),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Statut") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showStatusDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = showStatusDropdown,
                            onDismissRequest = { showStatusDropdown = false }
                        ) {
                            BookingStatus.values().forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(getBookingStatusLabel(status)) },
                                    onClick = {
                                        selectedStatus = status
                                        showStatusDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                item {
                    OutlinedTextField(
                        value = bookingUrl,
                        onValueChange = { bookingUrl = it },
                        label = { Text("URL de réservation (optionnel)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optionnel)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val capacityInt = capacity.toIntOrNull() ?: 0
                    val priceInCents = (pricePerNight.toDoubleOrNull() ?: 0.0) * 100
                    val nights = totalNights.toIntOrNull() ?: 0
                    val totalCost = (priceInCents * nights).toLong()
                    
                    val newAccommodation = Accommodation(
                        id = accommodation?.id ?: UUID.randomUUID().toString(),
                        eventId = eventId,
                        name = name,
                        type = selectedType,
                        address = address,
                        capacity = capacityInt,
                        pricePerNight = priceInCents.toLong(),
                        totalNights = nights,
                        totalCost = totalCost,
                        bookingStatus = selectedStatus,
                        bookingUrl = bookingUrl.ifBlank { null },
                        checkInDate = checkInDate,
                        checkOutDate = checkOutDate,
                        notes = notes.ifBlank { null },
                        createdAt = accommodation?.createdAt ?: java.time.Instant.now().toString(),
                        updatedAt = java.time.Instant.now().toString()
                    )
                    onSave(newAccommodation)
                },
                enabled = name.isNotBlank() && address.isNotBlank() && 
                          capacity.isNotBlank() && pricePerNight.isNotBlank() &&
                          totalNights.isNotBlank() && checkInDate.isNotBlank() && 
                          checkOutDate.isNotBlank()
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

// Helper functions
fun getAccommodationTypeIcon(type: AccommodationType) = when (type) {
    AccommodationType.HOTEL -> Icons.Default.Hotel
    AccommodationType.AIRBNB -> Icons.Default.Home
    AccommodationType.CAMPING -> Icons.Default.Terrain
    AccommodationType.HOSTEL -> Icons.Default.MeetingRoom
    AccommodationType.VACATION_RENTAL -> Icons.Default.House
    AccommodationType.OTHER -> Icons.Default.Place
}

fun getAccommodationTypeLabel(type: AccommodationType) = when (type) {
    AccommodationType.HOTEL -> "Hôtel"
    AccommodationType.AIRBNB -> "Airbnb"
    AccommodationType.CAMPING -> "Camping"
    AccommodationType.HOSTEL -> "Auberge"
    AccommodationType.VACATION_RENTAL -> "Location de vacances"
    AccommodationType.OTHER -> "Autre"
}

fun getBookingStatusLabel(status: BookingStatus) = when (status) {
    BookingStatus.SEARCHING -> "Recherche"
    BookingStatus.RESERVED -> "Réservé"
    BookingStatus.CONFIRMED -> "Confirmé"
    BookingStatus.CANCELLED -> "Annulé"
}

fun formatPrice(cents: Long): String {
    val euros = cents / 100.0
    return "%.2f €".format(euros)
}
