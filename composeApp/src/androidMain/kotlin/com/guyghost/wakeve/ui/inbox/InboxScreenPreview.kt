package com.guyghost.wakeve.ui.inbox

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.guyghost.wakeve.preview.PreviewTheme
import com.guyghost.wakeve.preview.factories.InboxItemFactory

/**
 * Preview: populated inbox with a mix of item types and statuses.
 */
@Preview(showBackground = true)
@Composable
fun InboxScreenPreview() {
    PreviewTheme {
        InboxScreen(
            items = InboxItemFactory.mixedList(),
            onRefresh = {},
            onItemClick = {},
            onMarkAllRead = {}
        )
    }
}

/**
 * Preview: empty inbox (no items at all).
 */
@Preview(showBackground = true)
@Composable
fun InboxScreenEmptyPreview() {
    PreviewTheme {
        InboxScreen(
            items = emptyList(),
            onRefresh = {},
            onItemClick = {},
            onMarkAllRead = {}
        )
    }
}

/**
 * Preview: initial loading state (spinner, no items yet).
 */
@Preview(showBackground = true)
@Composable
fun InboxScreenLoadingPreview() {
    PreviewTheme {
        InboxScreen(
            items = emptyList(),
            isLoading = true,
            onRefresh = {},
            onItemClick = {},
            onMarkAllRead = {}
        )
    }
}

/**
 * Preview: refreshing state (items visible with progress indicator).
 */
@Preview(showBackground = true)
@Composable
fun InboxScreenRefreshingPreview() {
    PreviewTheme {
        InboxScreen(
            items = InboxItemFactory.mixedList(),
            isRefreshing = true,
            onRefresh = {},
            onItemClick = {},
            onMarkAllRead = {}
        )
    }
}
