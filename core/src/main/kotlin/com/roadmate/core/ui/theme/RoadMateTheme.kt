package com.roadmate.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Top-level theme for all RoadMate surfaces.
 *
 * @param isHeadUnit `true` for the car head-unit app (larger typography),
 *                   `false` for the phone companion app.
 * @param content composable content wrapped by the theme.
 */
@Composable
fun RoadMateTheme(
    isHeadUnit: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = roadMateDarkColorScheme(),
        typography = roadMateTypography(isHeadUnit),
        shapes = RoadMateShapes,
        content = content,
    )
}
