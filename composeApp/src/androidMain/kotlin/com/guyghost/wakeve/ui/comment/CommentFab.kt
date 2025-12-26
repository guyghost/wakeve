package com.guyghost.wakeve.ui.comment

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Comment

/**
 * Reusable Floating Action Button for accessing comments.
 * Shows comment count with a badge if there are comments.
 */
@Composable
fun CommentFab(
    commentCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        BadgedBox(
            badge = {
                if (commentCount > 0) {
                    Badge {
                        Text(commentCount.toString())
                    }
                }
            }
        ) {
            Icon(
                imageVector = Icons.Outlined.Comment,
                contentDescription = if (commentCount == 0) "Aucun commentaire" else "$commentCount commentaires"
            )
        }
    }
}