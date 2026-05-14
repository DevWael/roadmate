package com.roadmate.headunit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.roadmate.core.ui.theme.RoadMateCorners
import com.roadmate.core.ui.theme.RoadMatePanelBackground
import com.roadmate.core.ui.theme.RoadMatePanelBorder
import com.roadmate.core.ui.theme.RoadMatePanelDivider
import com.roadmate.core.ui.theme.RoadMateSpacing

/**
 * Cockpit instrument panel container — a bordered surface that creates
 * the "instrument cluster" look for head-unit dashboard sections.
 *
 * Each major dashboard column is wrapped in this composable to visually
 * separate it as a distinct instrument panel with its own #121212
 * background and subtle #1E1E1E border.
 */
@Composable
fun CockpitPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val panelShape = RoundedCornerShape(RoadMateCorners.panel)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(panelShape)
            .background(RoadMatePanelBackground, panelShape)
            .border(1.dp, RoadMatePanelBorder, panelShape)
            .padding(RoadMateSpacing.xl),
    ) {
        content()
    }
}

/**
 * Horizontal divider styled for use inside cockpit panels.
 * Thinner and subtler than the standard M3 divider.
 */
@Composable
fun PanelDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = 0.5.dp,
        color = RoadMatePanelDivider,
    )
}
