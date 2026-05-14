package com.roadmate.headunit.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadmate.core.database.entity.EngineType
import com.roadmate.core.database.entity.FuelType
import com.roadmate.core.database.entity.OdometerUnit
import com.roadmate.core.database.entity.Vehicle
import com.roadmate.core.model.UiState
import com.roadmate.core.repository.ActiveVehicleRepository
import com.roadmate.core.repository.VehicleRepository
import com.roadmate.headunit.service.RoadMateService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class WelcomeFormState(
    val name: String = "",
    val odometer: String = "",
    val errors: Map<String, String> = emptyMap(),
)

fun WelcomeFormState.validate(): WelcomeFormState {
    val errors = mutableMapOf<String, String>()
    if (name.isBlank()) errors["name"] = "Required"
    if (odometer.isBlank()) errors["odometer"] = "Required"
    else odometer.toDoubleOrNull()
        ?.takeIf { it >= 0 }
        ?: run { errors["odometer"] = "Invalid" }
    return copy(errors = errors)
}

fun WelcomeFormState.isValid(): Boolean = validate().errors.isEmpty()

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val application: Application,
    private val vehicleRepository: VehicleRepository,
    private val activeVehicleRepository: ActiveVehicleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<WelcomeFormState>>(
        UiState.Success(WelcomeFormState())
    )
    val uiState: StateFlow<UiState<WelcomeFormState>> = _uiState.asStateFlow()

    private val currentForm: WelcomeFormState
        get() = (_uiState.value as? UiState.Success)?.data ?: WelcomeFormState()

    fun updateName(name: String) = updateForm { it.copy(name = name) }

    fun updateOdometer(odometer: String) = updateForm { it.copy(odometer = odometer) }

    private fun updateForm(transform: (WelcomeFormState) -> WelcomeFormState) {
        _uiState.update { current ->
            val state = (current as? UiState.Success)?.data ?: return@update current
            UiState.Success(transform(state))
        }
    }

    fun startTracking() {
        if (_uiState.value is UiState.Loading) return

        val form = currentForm.validate()
        if (form.errors.isNotEmpty()) {
            _uiState.update { UiState.Success(form) }
            return
        }

        val odometerKm = form.odometer.toDoubleOrNull()
        if (odometerKm == null || odometerKm < 0) {
            _uiState.update { UiState.Success(form.copy(errors = mapOf("odometer" to "Invalid"))) }
            return
        }

        val vehicle = Vehicle(
            name = form.name.trim(),
            make = "",
            model = "",
            year = 0,
            engineType = EngineType.INLINE_4,
            engineSize = 0.0,
            fuelType = FuelType.GASOLINE,
            plateNumber = "",
            odometerKm = odometerKm,
            odometerUnit = OdometerUnit.KM,
            cityConsumption = 0.0,
            highwayConsumption = 0.0,
        )

        viewModelScope.launch {
            _uiState.update { UiState.Loading }

            vehicleRepository.saveVehicle(vehicle)
                .onSuccess {
                    try {
                        activeVehicleRepository.setActiveVehicle(vehicle.id)
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to set active vehicle ${vehicle.id}")
                    }
                    try {
                        if (com.roadmate.headunit.ui.permissions.hasLocationPermission(application)) {
                            RoadMateService.start(application)
                        } else {
                            Timber.i("Location permission not granted yet, deferring service start")
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to start RoadMateService")
                    }
                    // Stay on Loading — reactive navigation gate in MainActivity
                    // will route to dashboard once vehicles/activeVehicleId flows update.
                }
                .onFailure { e ->
                    _uiState.update { UiState.Error(e.message ?: "Setup failed", e) }
                }
        }
    }

    fun resetToForm() {
        _uiState.update { UiState.Success(currentForm) }
    }
}
