package com.guyghost.wakeve.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guyghost.wakeve.models.EventType

/**
 * Full-screen preview overlay for an event being created.
 *
 * Shown when the user taps "Aperçu" in CreateEventScreen.
 * Displays the event as participants would see it — matching the iOS EventPreviewSheet design.
 *
 * @param title Event title (from form)
 * @param description Event description (from form)
 * @param userName Organizer display name
 * @param userPhotoUrl Organizer avatar URL (unused for now)
 * @param selectedDate Formatted date string (from date picker)
 * @param selectedLocation Location text (from location picker)
 * @param eventType Selected event type
 * @param expectedParticipants Estimated participant count
 * @param onDismiss Called when user closes the preview
 */
@Composable
fun CreateEventPreview(
    title: String,
    description: String,
    userName: String?,
    userPhotoUrl: String? = null,
    selectedDate: String? = null,
    selectedLocation: String? = null,
    eventType: EventType = EventType.OTHER,
    expectedParticipants: Int? = null,
    onDismiss: () -> Unit
) {
    // Gradient matching the iOS design: orange → purple → dark blue
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFF6B35),
            Color(0xFFFF4757),
            Color(0xFF8B5CF6),
            Color(0xFF6366F1),
            Color(0xFF3B82F6),
            Color(0xFF1E3A8A),
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A1A))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ── Gradient Hero Section ────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(gradientBrush)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    PreviewHeader(onDismiss = onDismiss)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Event Title
                    Text(
                        text = title.ifBlank { "Titre de l'événement" },
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Event type badge
                    if (eventType != EventType.OTHER && eventType != EventType.CUSTOM) {
                        EventTypeBadge(eventType = eventType)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Date & Location summary
                    if (selectedDate != null || selectedLocation != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            selectedDate?.let {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = it,
                                        fontSize = 15.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            selectedLocation?.let {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = it,
                                        fontSize = 15.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // RSVP Card
                    RsvpCard()

                    Spacer(modifier = Modifier.height(16.dp))

                    // Organizer Card
                    OrganizerPreviewCard(
                        userName = userName,
                        description = description,
                        expectedParticipants = expectedParticipants
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // ── Fixed Bottom — Invitation info ──────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0A1A))
                    .navigationBarsPadding()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Invitation envoyée",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                InvitationInfoRow(
                    icon = Icons.Default.Person,
                    text = "Les participants recevront une invitation à voter sur les créneaux."
                )

                Spacer(modifier = Modifier.height(8.dp))

                InvitationInfoRow(
                    icon = Icons.Default.CalendarToday,
                    text = "Une fois la date confirmée, l'événement sera finalisé automatiquement."
                )
            }
        }
    }
}

// ── Private composables ──────────────────────────────────────────────────────

@Composable
private fun PreviewHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back / close
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Fermer l'aperçu",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = "Aperçu",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.7f)
        )

        // Spacer to balance layout
        Box(modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun EventTypeBadge(eventType: EventType) {
    val emoji = when (eventType) {
        EventType.BIRTHDAY -> "🎂"
        EventType.WEDDING -> "💍"
        EventType.TEAM_BUILDING -> "🤝"
        EventType.CONFERENCE -> "🎤"
        EventType.WORKSHOP -> "🛠"
        EventType.PARTY -> "🎉"
        EventType.SPORTS_EVENT, EventType.SPORT_EVENT -> "⚽"
        EventType.CULTURAL_EVENT -> "🎭"
        EventType.FAMILY_GATHERING -> "👨‍👩‍👧"
        EventType.OUTDOOR_ACTIVITY -> "🏔"
        EventType.FOOD_TASTING -> "🍷"
        EventType.TECH_MEETUP -> "💻"
        EventType.WELLNESS_EVENT -> "🧘"
        EventType.CREATIVE_WORKSHOP -> "🎨"
        else -> "📅"
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.2f),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "$emoji ${eventType.displayName}",
            fontSize = 13.sp,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun RsvpCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF2D1B69)),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        RsvpColumn(icon = Icons.Default.Check, label = "Oui")
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(60.dp)
                .background(Color.White.copy(alpha = 0.15f))
        )
        RsvpColumn(icon = Icons.Default.Close, label = "Non")
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(60.dp)
                .background(Color.White.copy(alpha = 0.15f))
        )
        RsvpColumn(icon = Icons.Default.QuestionMark, label = "Peut-être")
    }
}

@Composable
private fun RsvpColumn(icon: ImageVector, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(vertical = 14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun OrganizerPreviewCard(
    userName: String?,
    description: String,
    expectedParticipants: Int?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF12143A).copy(alpha = 0.7f))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF6B35)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = userName?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Organisé par ${userName ?: "vous"}",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        if (description.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        if (expectedParticipants != null && expectedParticipants > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF8B9FFF),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "~$expectedParticipants participants attendus",
                    fontSize = 14.sp,
                    color = Color(0xFF8B9FFF)
                )
            }
        }
    }
}

@Composable
private fun InvitationInfoRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF8B5CF6),
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}
