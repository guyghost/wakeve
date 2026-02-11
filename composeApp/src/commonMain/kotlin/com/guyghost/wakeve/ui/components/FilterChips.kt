package com.guyghost.wakeve.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.models.InboxFilter

/**
 * Wakeve Design System Colors for FilterChips
 */
private object FilterChipColors {
    val Primary = Color(0xFF2563EB)
    val PrimaryContainer = Color(0xFFDBEAFE)
    val OnPrimaryContainer = Color(0xFF1E40AF)
    val Surface = Color(0xFFF8FAFC)
    val OnSurface = Color(0xFF0F172A)
    val OnSurfaceVariant = Color(0xFF475569)
    val Outline = Color(0xFFE2E8F0)
}

/**
 * FilterChipRow - Horizontal scrollable row of filter chips
 * 
 * Inspired by GitHub Mobile's filter bar (Inbox, Focused, Unread, Repository),
 * adapted to Wakeve's inbox filtering needs.
 * 
 * @param selectedFilter Currently selected filter
 * @param onFilterSelected Callback when a filter is selected
 * @param filters List of available filters (defaults to all InboxFilter values)
 * @param modifier Modifier for the component
 */
@Composable
fun FilterChipRow(
    selectedFilter: InboxFilter,
    onFilterSelected: (InboxFilter) -> Unit,
    modifier: Modifier = Modifier,
    filters: List<InboxFilter> = InboxFilter.entries,
    showDropdownOnFirst: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        filters.forEachIndexed { index, filter ->
            WakevFilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = filter.label,
                showDropdown = showDropdownOnFirst && index == 0
            )
        }
    }
}

/**
 * WakevFilterChip - Individual filter chip with Wakeve styling
 * 
 * @param selected Whether this chip is currently selected
 * @param onClick Callback when chip is clicked
 * @param label Text label for the chip
 * @param showDropdown Whether to show dropdown indicator
 * @param modifier Modifier for the component
 */
@Composable
fun WakevFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    showDropdown: Boolean = false
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) FilterChipColors.PrimaryContainer else FilterChipColors.Surface,
        animationSpec = tween(durationMillis = 200),
        label = "chipBackground"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (selected) FilterChipColors.OnPrimaryContainer else FilterChipColors.OnSurfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "chipContent"
    )
    
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 0.dp else 1.dp,
        animationSpec = tween(durationMillis = 200),
        label = "chipBorder"
    )

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        modifier = modifier.height(36.dp),
        trailingIcon = if (showDropdown) {
            {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Dropdown",
                    tint = contentColor
                )
            }
        } else null,
        shape = RoundedCornerShape(18.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = FilterChipColors.Surface,
            labelColor = FilterChipColors.OnSurfaceVariant,
            selectedContainerColor = FilterChipColors.PrimaryContainer,
            selectedLabelColor = FilterChipColors.OnPrimaryContainer
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = FilterChipColors.Outline,
            selectedBorderColor = Color.Transparent,
            borderWidth = borderWidth,
            selectedBorderWidth = 0.dp,
            enabled = true,
            selected = selected
        )
    )
}

/**
 * Segmented filter row - for fewer options in a more compact layout
 * Uses Surface to create a pill-shaped segmented control
 */
@Composable
fun SegmentedFilterRow(
    selectedFilter: InboxFilter,
    onFilterSelected: (InboxFilter) -> Unit,
    modifier: Modifier = Modifier,
    filters: List<InboxFilter> = listOf(
        InboxFilter.ALL,
        InboxFilter.UNREAD,
        InboxFilter.ACTIONS
    )
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = FilterChipColors.Surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            filters.forEach { filter ->
                SegmentedButton(
                    selected = selectedFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = filter.label,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) FilterChipColors.PrimaryContainer else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "segmentBackground"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (selected) FilterChipColors.OnPrimaryContainer else FilterChipColors.OnSurfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "segmentContent"
    )

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
