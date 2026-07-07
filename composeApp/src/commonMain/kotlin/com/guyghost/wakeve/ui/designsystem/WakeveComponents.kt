package com.guyghost.wakeve.ui.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WakeveScaffold(
    title: String,
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.semantics { heading() }
                        )
                    },
                    navigationIcon = {
                        if (onNavigateBack != null) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
                    actions = actions,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        floatingActionButton = floatingActionButton,
        containerColor = MaterialTheme.colorScheme.background,
        content = content
    )
}

@Composable
fun WakeveCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = CardDefaults.cardColors(
        containerColor = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    )
    val shape = MaterialTheme.shapes.medium
    val elevation = CardDefaults.cardElevation(
        defaultElevation = if (selected) WakeveElevation.level3 else WakeveElevation.level1
    )

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            content = { Box(modifier = Modifier.padding(WakeveSpacing.md)) { content() } }
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            content = { Box(modifier = Modifier.padding(WakeveSpacing.md)) { content() } }
        )
    }
}

@Composable
fun WakeveSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = WakeveSize.minTouchTarget),
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null
            )
        },
        placeholder = { Text(placeholder) }
    )
}

@Composable
fun <T> WakeveSegmentedOptions(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    layout: WakeveFilterLayout = WakeveFilterLayout.Scrollable
) {
    if (layout == WakeveFilterLayout.Wrapped) {
        WrappedSegmentedOptions(
            options = options,
            selected = selected,
            label = label,
            onSelected = onSelected,
            modifier = modifier
        )
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(WakeveSpacing.sm)
        ) {
            options.forEach { option ->
                WakeveFilterChip(
                    option = option,
                    selected = option == selected,
                    label = label,
                    onSelected = onSelected
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> WrappedSegmentedOptions(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WakeveSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)
    ) {
        options.forEach { option ->
            WakeveFilterChip(
                option = option,
                selected = option == selected,
                label = label,
                onSelected = onSelected
            )
        }
    }
}

@Composable
private fun <T> WakeveFilterChip(
    option: T,
    selected: Boolean,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = { onSelected(option) },
        label = { Text(label(option)) },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null
                )
            }
        } else {
            null
        }
    )
}

@Composable
fun WakeveButtonGroup(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WakeveSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun WakeveMenu(
    expanded: Boolean,
    items: List<String>,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        items.forEach { item ->
            DropdownMenuItem(
                text = { Text(item) },
                onClick = { onSelected(item) }
            )
        }
    }
}

@Composable
fun WakeveProgressIndicator(
    modifier: Modifier = Modifier,
    linear: Boolean = false,
    progress: Float? = null
) {
    if (linear) {
        if (progress == null) {
            LinearProgressIndicator(modifier = modifier.fillMaxWidth())
        } else {
            LinearProgressIndicator(progress = { progress }, modifier = modifier.fillMaxWidth())
        }
    } else {
        if (progress == null) {
            CircularProgressIndicator(modifier = modifier)
        } else {
            CircularProgressIndicator(progress = { progress }, modifier = modifier)
        }
    }
}

@Composable
fun WakeveStateMessage(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.ErrorOutline,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.padding(WakeveSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WakeveSpacing.md)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun WakeveStatusChip(
    label: String,
    modifier: Modifier = Modifier
) {
    AssistChip(
        modifier = modifier,
        onClick = {},
        label = { Text(label) }
    )
}

enum class WakeveSyncState {
    Synced,
    Pending,
    Offline
}

@Composable
fun WakeveSyncIndicator(
    state: WakeveSyncState,
    modifier: Modifier = Modifier
) {
    val (icon, label) = when (state) {
        WakeveSyncState.Synced -> Icons.Default.CloudDone to "Synced"
        WakeveSyncState.Pending -> Icons.Default.Sync to "Pending sync"
        WakeveSyncState.Offline -> Icons.Default.CloudOff to "Offline"
    }

    AssistChip(
        modifier = modifier,
        onClick = {},
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        },
        label = { Text(label) }
    )
}
