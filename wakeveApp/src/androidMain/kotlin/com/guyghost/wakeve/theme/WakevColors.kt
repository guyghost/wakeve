package com.guyghost.wakeve.theme

import androidx.compose.ui.graphics.Color

/**
 * Legacy color aliases used by collaboration screens.
 * Values map to the existing Wakeve palette.
 */
object WakevColors {
    val primary: Color = WakevePrimary
    val primaryContainer: Color = WakevePrimaryLight
    val onPrimaryContainer: Color = WakevePrimaryDark
    val background: Color = WakeveBackgroundLight
    val surface: Color = WakeveSurfaceLight
    val surfaceVariant: Color = Color(0xFFE2E8F0)
    val onSurface: Color = WakeveTextPrimaryLight
    val onSurfaceVariant: Color = WakeveTextSecondaryLight
    val outline: Color = WakeveBorderLight
}
