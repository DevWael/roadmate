package com.roadmate.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// ── Surface ─────────────────────────────────────────────────────────
val RoadMateSurface = Color(0xFF0A0A0A)
val RoadMateSurfaceVariant = Color(0xFF1A1A1A)
val RoadMateSurfaceContainer = Color(0xFF121212)

// ── Panel (cockpit instrument containers) ───────────────────────────
val RoadMatePanelBackground = Color(0xFF121212)
val RoadMatePanelBorder = Color(0xFF1E1E1E)
val RoadMatePanelDivider = Color(0xFF2A2A2A)

// ── Accent ──────────────────────────────────────────────────────────
val RoadMatePrimary = Color(0xFF4FC3F7)
val RoadMateSecondary = Color(0xFF80CBC4)
val RoadMateTertiary = Color(0xFFFFB74D)

// ── Semantic ────────────────────────────────────────────────────────
val RoadMateError = Color(0xFFEF5350)
val RoadMateSuccess = Color(0xFF66BB6A)

// ── Content ─────────────────────────────────────────────────────────
val RoadMateOnSurface = Color(0xFFE8E8E8)
val RoadMateOnSurfaceVariant = Color(0xFF9E9E9E)
val RoadMateOutline = Color(0xFF4A4A4A)

/**
 * The single M3 dark [ColorScheme] for RoadMate.
 *
 * No light theme, no dynamic color — dark-only automotive UI.
 * Cached as a val to avoid allocating on every recomposition.
 */
private val RoadMateDarkColors = darkColorScheme(
    primary = RoadMatePrimary,
    onPrimary = RoadMateSurface,
    secondary = RoadMateSecondary,
    onSecondary = RoadMateSurface,
    tertiary = RoadMateTertiary,
    onTertiary = RoadMateSurface,
    error = RoadMateError,
    onError = RoadMateSurface,
    surface = RoadMateSurface,
    onSurface = RoadMateOnSurface,
    surfaceVariant = RoadMateSurfaceVariant,
    onSurfaceVariant = RoadMateOnSurfaceVariant,
    surfaceContainer = RoadMateSurfaceContainer,
    outline = RoadMateOutline,
    background = RoadMateSurface,
    onBackground = RoadMateOnSurface,
)

/** Returns the cached M3 dark [ColorScheme] for RoadMate. */
fun roadMateDarkColorScheme() = RoadMateDarkColors
