package com.guyghost.wakeve.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.LocationType
import com.guyghost.wakeve.models.PotentialLocation

/**
 * List of potential locations with add/remove functionality.
 * 
 * Shows:
 * - Empty state when no locations
 * - List of locations with type icons
 * - Add button to open location dialog
 * - Delete button for each location
 * 
 * Material You design system component.
 * 
 * @param locations List of potential locations
 * @param onAddLocation Callback to add a new location
 * @param onRemoveLocation Callback to remove a location by ID
 * @param modifier Modifier for the component
 * @param enabled Whether the list is interactive
 */
@Composable
fun PotentialLocationsList(
    locations: List<PotentialLocation>,
    onAddLocation: () -> Unit,
    onRemoveLocation: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Potential Locations",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (locations.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = locations.size.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                FilledTonalButton(
                    onClick = onAddLocation,
                    enabled = enabled,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add location",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Content
            AnimatedContent(
                targetState = locations.isEmpty(),
                label = "locations_list_animation"
            ) { isEmpty ->
                if (isEmpty) {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "No locations yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Add potential venues, cities, or regions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    // Locations list
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        locations.forEach { location ->
                            LocationListItem(
                                location = location,
                                onRemove = { onRemoveLocation(location.id) },
                                enabled = enabled
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single location item in the list.
 */
@Composable
private fun LocationListItem(
    location: PotentialLocation,
    onRemove: () -> Unit,
    enabled: Boolean
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Type icon
                Icon(
                    imageVector = when (location.locationType) {
                        LocationType.CITY -> Icons.Outlined.LocationCity
                        LocationType.REGION -> Icons.Outlined.Public
                        LocationType.SPECIFIC_VENUE -> Icons.Outlined.Place
                        LocationType.ONLINE -> Icons.Outlined.VideoCall
                    },
                    contentDescription = location.locationType.name,
                    tint = MaterialTheme.colorScheme.primary
                )
                
                // Location info
                Column {
                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (location.locationType) {
                                LocationType.CITY -> "City"
                                LocationType.REGION -> "Region"
                                LocationType.SPECIFIC_VENUE -> "Venue"
                                LocationType.ONLINE -> "Online"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        location.address?.let { addr ->
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = addr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            
            // Delete button
            IconButton(
                onClick = onRemove,
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove location",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
