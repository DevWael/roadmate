package com.roadmate.headunit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roadmate.core.model.UiState
import com.roadmate.core.ui.theme.RoadMateTheme
import com.roadmate.headunit.ui.WelcomeContent
import com.roadmate.headunit.ui.parked.DashboardShell
import com.roadmate.headunit.ui.parked.VehicleSetupContent
import com.roadmate.headunit.ui.parked.VehicleSwitcherDialog
import com.roadmate.headunit.viewmodel.VehicleSetupViewModel
import com.roadmate.headunit.viewmodel.WelcomeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RoadMateTheme(isHeadUnit = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    RoadMateMainScreen()
                }
            }
        }
    }
}

@Composable
fun RoadMateMainScreen() {
    val mainViewModel: MainViewModel = hiltViewModel()
    val activeVehicleId by mainViewModel.activeVehicleId.collectAsStateWithLifecycle()
    val vehicles by mainViewModel.vehicles.collectAsStateWithLifecycle()
    val isReady by mainViewModel.isReady.collectAsStateWithLifecycle()
    var showSetup by remember { mutableStateOf(false) }
    var showSwitcher by remember { mutableStateOf(false) }

    val currentVehicle = vehicles.find { it.id == activeVehicleId }
    val hasVehicles = vehicles.isNotEmpty()

    val hasActiveVehicle = activeVehicleId != null && currentVehicle != null

    if (!isReady) return

    when {
        !hasVehicles && !hasActiveVehicle && !showSetup -> {
            val welcomeViewModel: WelcomeViewModel = hiltViewModel()
            val welcomeState by welcomeViewModel.uiState.collectAsStateWithLifecycle()

            WelcomeContent(
                uiState = welcomeState,
                onNameChange = welcomeViewModel::updateName,
                onOdometerChange = welcomeViewModel::updateOdometer,
                onStartTracking = welcomeViewModel::startTracking,
                onRetry = welcomeViewModel::resetToForm,
            )
        }
        showSetup -> {
            val setupViewModel: VehicleSetupViewModel = hiltViewModel()
            val uiState by setupViewModel.uiState.collectAsStateWithLifecycle()

            VehicleSetupContent(
                uiState = uiState,
                onNameChange = setupViewModel::updateName,
                onMakeChange = setupViewModel::updateMake,
                onModelChange = setupViewModel::updateModel,
                onYearChange = setupViewModel::updateYear,
                onEngineTypeChange = setupViewModel::updateEngineType,
                onEngineSizeChange = setupViewModel::updateEngineSize,
                onFuelTypeChange = setupViewModel::updateFuelType,
                onPlateNumberChange = setupViewModel::updatePlateNumber,
                onOdometerKmChange = setupViewModel::updateOdometerKm,
                onOdometerUnitChange = setupViewModel::updateOdometerUnit,
                onCityConsumptionChange = setupViewModel::updateCityConsumption,
                onHighwayConsumptionChange = setupViewModel::updateHighwayConsumption,
                onTemplateSelected = setupViewModel::selectTemplate,
                onSave = setupViewModel::save,
            )

            if (uiState is UiState.Success && activeVehicleId != null && currentVehicle != null) {
                showSetup = false
            }
        }
        showSwitcher -> {
            VehicleSwitcherDialog(
                vehicles = vehicles,
                activeVehicleId = activeVehicleId,
                onVehicleSelected = { mainViewModel.switchVehicle(it) },
                onAddVehicle = {
                    showSwitcher = false
                    showSetup = true
                },
                onDismiss = { showSwitcher = false },
            )
        }
        else -> {
            DashboardShell(
                vehicle = currentVehicle,
                onSwitchVehicle = { showSwitcher = true },
            )
        }
    }
}
