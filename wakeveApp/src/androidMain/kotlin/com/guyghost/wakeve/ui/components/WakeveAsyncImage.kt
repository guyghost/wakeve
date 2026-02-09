package com.guyghost.wakeve.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.guyghost.wakeve.R

/**
 * Enum defining shape transformations for images
 */
enum class ImageShape {
    RECTANGLE,
    CIRCLE,
    ROUNDED_CORNERS
}

/**
 * Wakeve Async Image component with Coil optimization
 *
 * Features:
 * - Memory cache (25% of available memory)
 * - Disk cache (50MB)
 * - Crossfade animation (200ms)
 * - Placeholder and error states
 * - Shape transformations (Rectangle, Circle, Rounded Corners)
 *
 * @param imageUrl URL of the image to load
 * @param contentDescription Content description for accessibility
 * @param modifier Modifier to apply to the image
 * @param shape Shape transformation to apply
 * @param cornerRadius Corner radius in dp (only used for ROUNDED_CORNERS)
 * @param contentScale Scale type for the image
 */
@Composable
fun WakeveAsyncImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: ImageShape = ImageShape.RECTANGLE,
    cornerRadius: Int = 8,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    // Apply shape transformation
    val shapeModifier = when (shape) {
        ImageShape.CIRCLE -> Modifier.clip(CircleShape)
        ImageShape.ROUNDED_CORNERS -> Modifier.clip(RoundedCornerShape(cornerRadius.dp))
        ImageShape.RECTANGLE -> Modifier
    }

    Box(
        modifier = modifier.then(shapeModifier),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .crossfade(200)
                .build(),
            contentDescription = contentDescription,
            modifier = Modifier.matchParentSize(),
            contentScale = contentScale,
            placeholder = painterResource(R.drawable.image_placeholder),
            error = painterResource(R.drawable.image_error),
            onLoading = {
                isLoading = true
                hasError = false
            },
            onSuccess = {
                isLoading = false
                hasError = false
            },
            onError = {
                isLoading = false
                hasError = true
            }
        )

        // Show loading indicator if needed
        if (isLoading && !hasError) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

/**
 * Event Cover Image component with rounded corners
 *
 * Specialized version of WakeveAsyncImage for event cover images
 * with default rounded corner styling
 *
 * @param imageUrl URL of the event cover image
 * @param modifier Modifier to apply to the image
 */
@Composable
fun EventCoverImage(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    WakeveAsyncImage(
        imageUrl = imageUrl,
        contentDescription = "Event cover image",
        modifier = modifier,
        shape = ImageShape.ROUNDED_CORNERS,
        cornerRadius = 12
    )
}

/**
 * User Avatar component with circular shape
 *
 * Specialized version of WakeveAsyncImage for user avatars
 * with default circular styling
 *
 * @param imageUrl URL of the user avatar
 * @param modifier Modifier to apply to the avatar
 */
@Composable
fun UserAvatar(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    WakeveAsyncImage(
        imageUrl = imageUrl,
        contentDescription = "User avatar",
        modifier = modifier,
        shape = ImageShape.CIRCLE
    )
}
