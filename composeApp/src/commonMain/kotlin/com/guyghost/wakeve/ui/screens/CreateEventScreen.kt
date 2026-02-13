package com.guyghost.wakeve.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.ui.components.LocationSelectionBottomSheet
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * Full-screen page for creating an event with immersive gradient design.
 * Updated to match event.png design.
 * 
 * Features:
 * - Gradient background (orange to purple to blue)
 * - Background image selector
 * - Event title input with large typography
 * - Date/Time picker
 * - Location selector
 * - Organizer display
 * - Description input
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    userId: String,
    userName: String? = null,
    userPhotoUrl: String? = null,
    onClose: () -> Unit = {},
    onEventCreated: (Event) -> Unit = {}
) {
    // Form state
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var selectedLocation by remember { mutableStateOf<String?>(null) }
    var hasBackgroundImage by remember { mutableStateOf(false) }
    
    // Location sheet state
    var showLocationSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Gradient background - matching the screenshot colors (orange to purple to blue)
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFF6B35), // Orange
            Color(0xFFFF4757), // Red-orange
            Color(0xFF8B5CF6), // Purple
            Color(0xFF6366F1), // Indigo
            Color(0xFF3B82F6), // Blue
            Color(0xFF1E3A8A), // Dark blue
        ),
        startY = 0f,
        endY = Float.POSITIVE_INFINITY
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            HeaderRow(onClose = onClose)
            
            // Background Image Selector
            BackgroundImageSelector(
                hasImage = hasBackgroundImage,
                onAddImage = { hasBackgroundImage = true }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Main Event Card (contains title, date, location)
            MainEventCard(
                title = title,
                onTitleChange = { title = it },
                selectedDate = selectedDate,
                onDateClick = { /* TODO: Show date picker */ },
                selectedLocation = selectedLocation,
                onLocationClick = { showLocationSheet = true },
                userName = userName,
                userPhotoUrl = userPhotoUrl
            )
            
            // Location Selection Bottom Sheet
            if (showLocationSheet) {
                LocationSelectionBottomSheet(
                    onDismiss = { showLocationSheet = false },
                    onConfirm = { location ->
                        selectedLocation = location.name
                        showLocationSheet = false
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description Card (linked to main card)
            DescriptionCard(
                description = description,
                onDescriptionChange = { description = it }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Create Button
            CreateEventButton(
                enabled = title.isNotBlank(),
                onClick = {
                    val event = Event(
                        id = "event-${Clock.System.now().toEpochMilliseconds()}",
                        title = title,
                        description = description,
                        organizerId = userId,
                        participants = emptyList(),
                        proposedSlots = emptyList(),
                        deadline = Clock.System.now().toString(),
                        status = EventStatus.DRAFT,
                        createdAt = Clock.System.now().toString(),
                        updatedAt = Clock.System.now().toString(),
                        finalDate = null,
                        eventType = EventType.OTHER,
                        eventTypeCustom = null,
                        minParticipants = null,
                        maxParticipants = null,
                        expectedParticipants = null
                    )
                    onEventCreated(event)
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeaderRow(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Close button (X)
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Fermer",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Preview button
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .clickable { /* Preview mode */ },
            color = Color.White.copy(alpha = 0.15f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                text = "Aperçu",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun BackgroundImageSelector(
    hasImage: Boolean,
    onAddImage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Image icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
                .clickable(onClick = onAddImage),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(36.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Add background button
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onAddImage),
            color = Color.White.copy(alpha = 0.15f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                text = "Ajouter un arrière-plan",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun MainEventCard(
    title: String,
    onTitleChange: (String) -> Unit,
    selectedDate: String?,
    onDateClick: () -> Unit,
    selectedLocation: String?,
    onLocationClick: () -> Unit,
    userName: String?,
    userPhotoUrl: String?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF1A1A3E).copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Event Title Input (in the card)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (title.isEmpty()) {
                                Text(
                                    text = "Titre de\nl'évènement",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 40.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
            
            // Divider
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp),
                thickness = 1.dp,
                color = Color.White.copy(alpha = 0.1f)
            )
            
            // Date & Time Row
            DetailRow(
                icon = Icons.Default.DateRange,
                label = selectedDate ?: "Date et heure",
                isPlaceholder = selectedDate == null,
                onClick = onDateClick,
                iconColor = Color(0xFF8B5CF6) // Purple tint
            )
            
            // Divider
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp),
                thickness = 1.dp,
                color = Color.White.copy(alpha = 0.1f)
            )
            
            // Location Row
            DetailRow(
                icon = Icons.Default.LocationOn,
                label = selectedLocation ?: "Lieu",
                isPlaceholder = selectedLocation == null,
                onClick = onLocationClick,
                iconColor = Color(0xFF6366F1) // Indigo tint
            )
            
            // Divider
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp),
                thickness = 1.dp,
                color = Color.White.copy(alpha = 0.1f)
            )
            
            // Organizer Section
            OrganizerSection(
                userName = userName,
                userPhotoUrl = userPhotoUrl
            )
        }
    }
}

@Composable
private fun OrganizerSection(
    userName: String?,
    userPhotoUrl: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile photo placeholder
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF6B35)), // Orange background
            contentAlignment = Alignment.Center
        ) {
            if (userPhotoUrl != null) {
                // TODO: Load actual image with Coil
                Text(
                    text = userName?.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = userName?.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Organizer text
        Text(
            text = "Organisé par ${userName ?: "Vous"}",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isPlaceholder: Boolean,
    onClick: () -> Unit,
    iconColor: Color = Color(0xFF8B5CF6)
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(28.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            color = if (isPlaceholder) Color.White.copy(alpha = 0.9f) else Color.White,
            fontSize = 18.sp,
            fontWeight = if (isPlaceholder) FontWeight.Normal else FontWeight.Medium
        )
    }
}

@Composable
private fun DescriptionCard(
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFF1A1A3E).copy(alpha = 0.7f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            if (description.isEmpty()) {
                Surface(
                    modifier = Modifier.clip(RoundedCornerShape(24.dp)),
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = "Ajouter une description",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }
            } else {
                BasicTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        innerTextField()
                    }
                )
            }
        }
    }
}

@Composable
private fun CreateEventButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        color = if (enabled) Color.White else Color.White.copy(alpha = 0.3f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Créer l'évènement",
                color = if (enabled) Color(0xFF6366F1) else Color.White.copy(alpha = 0.6f),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
