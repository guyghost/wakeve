package com.guyghost.wakeve.preview

import androidx.compose.runtime.Composable
import com.guyghost.wakeve.theme.WakeveTheme

/**
 * A lightweight wrapper around [WakeveTheme] designed for Compose @Preview functions.
 *
 * Key differences from using WakeveTheme directly:
 * - [dynamicColor] is disabled by default so previews render consistently
 *   across devices and Android versions.
 * - The [WakeveTheme] SideEffect block is already guarded by `view.isInEditMode`,
 *   so no additional fix is needed here.
 *
 * Usage:
 * ```
 * @Preview
 * @Composable
 * fun MyScreenPreview() {
 *     PreviewTheme {
 *         MyScreen()
 *     }
 * }
 * ```
 */
@Composable
fun PreviewTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    WakeveTheme(
        darkTheme = darkTheme,
        dynamicColor = false,
        content = content
    )
}
