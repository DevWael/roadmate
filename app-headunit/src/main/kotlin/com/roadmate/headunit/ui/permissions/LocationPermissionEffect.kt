package com.roadmate.headunit.ui.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Checks whether foreground location permissions are granted.
 */
fun hasLocationPermission(context: Context): Boolean {
    return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
}

private val FOREGROUND_LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

/**
 * Composable effect that requests foreground location permissions when they are missing.
 *
 * Background location (`ACCESS_BACKGROUND_LOCATION`) is NOT requested here.
 * The HU app uses a foreground service with `FOREGROUND_SERVICE_TYPE_LOCATION`,
 * which already has full location access without background permission.
 *
 * @param onResult called with `true` when foreground location is granted.
 */
@Composable
fun LocationPermissionEffect(
    onResult: (granted: Boolean) -> Unit,
) {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val allGranted = results.all { it.value }
        onResult(allGranted)
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) {
            onResult(true)
            return@LaunchedEffect
        }

        val activity = context as? android.app.Activity
        val shouldShow = activity != null &&
            FOREGROUND_LOCATION_PERMISSIONS.any {
                activity.shouldShowRequestPermissionRationale(it)
            }

        if (shouldShow) {
            showRationale = true
        } else {
            launcher.launch(FOREGROUND_LOCATION_PERMISSIONS)
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = {
                showRationale = false
                onResult(false)
            },
            title = {
                Text(
                    text = "Location Permission Required",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    text = "RoadMate needs location access to track your trips and update " +
                        "the odometer automatically. Location is used only on-device — " +
                        "no data is sent to any server.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                        launcher.launch(FOREGROUND_LOCATION_PERMISSIONS)
                    },
                ) { Text("Continue") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                        onResult(false)
                    },
                ) { Text("Skip") }
            },
        )
    }
}
