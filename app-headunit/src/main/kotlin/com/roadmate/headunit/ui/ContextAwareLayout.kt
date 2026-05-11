package com.roadmate.headunit.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.Vehicle
import com.roadmate.core.model.DrivingState
import com.roadmate.core.model.GpsState
import com.roadmate.headunit.ui.driving.DrivingHUD
import com.roadmate.headunit.ui.parked.DashboardShell

internal const val TRANSITION_DURATION_MS = 400

@Composable
fun ContextAwareLayout(
    drivingState: DrivingState,
    gpsState: GpsState,
    vehicle: Vehicle?,
    trips: List<Trip>,
    alertMessage: String?,
    onSwitchVehicle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val reduceMotion = rememberReduceMotion(context)

    val isDriving = drivingState is DrivingState.Driving

    AnimatedContent(
        targetState = isDriving,
        modifier = modifier,
        transitionSpec = {
            if (reduceMotion) {
                fadeIn(tween(0)) togetherWith fadeOut(tween(0))
            } else {
                fadeIn(animationSpec = tween(TRANSITION_DURATION_MS)) togetherWith
                    fadeOut(animationSpec = tween(TRANSITION_DURATION_MS))
            }
        },
        label = "context-aware",
    ) { driving ->
        if (driving && drivingState is DrivingState.Driving) {
            DrivingHUD(
                drivingState = drivingState,
                vehicle = vehicle,
                gpsState = gpsState,
                alertMessage = alertMessage,
            )
        } else {
            DashboardShell(
                vehicle = vehicle,
                trips = trips,
                onSwitchVehicle = onSwitchVehicle,
            )
        }
    }
}

@Composable
private fun rememberReduceMotion(context: Context): Boolean {
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}
