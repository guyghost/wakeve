package com.guyghost.wakeve.ui.comment

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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