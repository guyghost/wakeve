package com.guyghost.wakeve.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.theme.WakeveTheme

/**
 * Preview component for WakeveAsyncImage
 *
 * Demonstrates various use cases of the WakeveAsyncImage component:
 * - Event cover with rounded corners
 * - User avatar with circular shape
 * - Rectangle image
 */
@Preview(showBackground = true, widthDp = 320)
@Composable
fun WakeveAsyncImagePreview() {
    WakeveTheme {
        Column {
            // Rectangle with rounded corners (Event Cover)
            EventCoverImage(
                imageUrl = "https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=400",
                modifier = Modifier.size(200.dp, 120.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Circle avatar
            UserAvatar(
                imageUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200",
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Rectangle shape
            WakeveAsyncImage(
                imageUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=300",
                contentDescription = "Landscape photo",
                modifier = Modifier.size(200.dp, 150.dp),
                shape = ImageShape.RECTANGLE
            )
        }
    }
}

/**
 * Preview with error state
 */
@Preview(showBackground = true, widthDp = 320)
@Composable
fun WakeveAsyncImageErrorPreview() {
    WakeveTheme {
        Column {
            // Invalid URL - should show error placeholder
            EventCoverImage(
                imageUrl = "https://invalid-url-that-does-not-exist.com/image.jpg",
                modifier = Modifier.size(200.dp, 120.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Null URL - should show placeholder
            UserAvatar(
                imageUrl = null,
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

/**
 * Preview with different corner radius
 */
@Preview(showBackground = true, widthDp = 320)
@Composable
fun WakeveAsyncImageCustomRadiusPreview() {
    WakeveTheme {
        WakeveAsyncImage(
            imageUrl = "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=400",
            contentDescription = "Custom rounded corners",
            modifier = Modifier.size(200.dp, 150.dp),
            shape = ImageShape.ROUNDED_CORNERS,
            cornerRadius = 20
        )
    }
}
