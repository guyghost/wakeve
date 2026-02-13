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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.datetime.Clock

/**
 * Full-screen page for creating an event with immersive gradient design.
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
    
    // Gradient background - matching the screenshot colors
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFF6B35), // Orange
            Color(0xFFFF8C42), // Light orange
            Color(0xFF9B59B6), // Purple
            Color(0xFF6C5CE7), // Deep purple
            Color(0xFF4834D4), // Blue-purple
            Color(0xFF0984E3), // Blue
            Color(0xFF0C2461), // Dark blue
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
            
            // Event Title Input
            EventTitleInput(
                title = title,
                onTitleChange = { title = it }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Event Details Card
            EventDetailsCard(
                selectedDate = selectedDate,
                onDateClick = { /* TODO: Show date picker */ },
                selectedLocation = selectedLocation,
                onLocationClick = { /* TODO: Show location picker */ },
                userName = userName,
                userPhotoUrl = userPhotoUrl
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description Input
            DescriptionInput(
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
        // Close button
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
        TextButton(
            onClick = { /* Preview mode */ }
        ) {
            Text(
                text = "Aperçu",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
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
private fun EventTitleInput(
    title: String,
    onTitleChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = title,
            onValueChange = onTitleChange,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 36.sp,
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
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 44.sp
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun EventDetailsCard(
    selectedDate: String?,
    onDateClick: () -> Unit,
    selectedLocation: String?,
    onLocationClick: () -> Unit,
    userName: String?,
    userPhotoUrl: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E).copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Date & Time Row
            DetailRow(
                icon = Icons.Default.DateRange,
                label = selectedDate ?: "Date et heure",
                isPlaceholder = selectedDate == null,
                onClick = onDateClick
            )
            
            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            )
            
            // Location Row
            DetailRow(
                icon = Icons.Default.LocationOn,
                label = selectedLocation ?: "Lieu",
                isPlaceholder = selectedLocation == null,
                onClick = onLocationClick
            )
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isPlaceholder: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp, horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF6C5CE7),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.size(12.dp))
        
        Text(
            text = label,
            color = if (isPlaceholder) Color.White.copy(alpha = 0.9f) else Color.White,
            fontSize = 18.sp,
            fontWeight = if (isPlaceholder) FontWeight.Normal else FontWeight.Medium
        )
    }
}

@Composable
private fun DescriptionInput(
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E).copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (description.isEmpty()) {
                Surface(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)),
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "Ajouter une description",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            } else {
                Text(
                    text = description,
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth()
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
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            disabledContainerColor = Color.White.copy(alpha = 0.3f)
        )
    ) {
        Text(
            text = "Créer l'évènement",
            color = if (enabled) Color(0xFF6C5CE7) else Color.White.copy(alpha = 0.6f),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
