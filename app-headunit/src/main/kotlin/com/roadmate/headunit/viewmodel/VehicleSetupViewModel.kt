package com.roadmate.headunit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadmate.core.database.entity.EngineType
import com.roadmate.core.database.entity.FuelType
import com.roadmate.core.database.entity.OdometerUnit
import com.roadmate.core.database.template.MaintenanceTemplates
import com.roadmate.core.model.UiState
import com.roadmate.core.repository.ActiveVehicleRepository
import com.roadmate.core.repository.MaintenanceRepository
import com.roadmate.core.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class VehicleSetupViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val activeVehicleRepository: ActiveVehicleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<VehicleFormState>>(
        UiState.Success(VehicleFormState())
    )
    val uiState: StateFlow<UiState<VehicleFormState>> = _uiState.asStateFlow()

    private val currentForm: VehicleFormState
        get() = (_uiState.value as? UiState.Success)?.data ?: VehicleFormState()

    fun updateName(name: String) = updateForm { it.copy(name = name) }
    fun updateMake(make: String) = updateForm { it.copy(make = make) }
    fun updateModel(model: String) = updateForm { it.copy(model = model) }
    fun updateYear(year: String) = updateForm { it.copy(year = year) }
    fun updateEngineType(engineType: EngineType) = updateForm { it.copy(engineType = engineType) }
    fun updateEngineSize(engineSize: String) = updateForm { it.copy(engineSize = engineSize) }
    fun updateFuelType(fuelType: FuelType) = updateForm { it.copy(fuelType = fuelType) }
    fun updatePlateNumber(plateNumber: String) = updateForm { it.copy(plateNumber = plateNumber) }
    fun updateOdometerKm(odometerKm: String) = updateForm { it.copy(odometerKm = odometerKm) }
    fun updateOdometerUnit(unit: OdometerUnit) = updateForm { it.copy(odometerUnit = unit) }
    fun updateCityConsumption(consumption: String) = updateForm { it.copy(cityConsumption = consumption) }
    fun updateHighwayConsumption(consumption: String) = updateForm { it.copy(highwayConsumption = consumption) }
    fun selectTemplate(selection: TemplateSelection) = updateForm { it.copy(templateSelection = selection) }

    private fun updateForm(transform: (VehicleFormState) -> VehicleFormState) {
        _uiState.update { current ->
            val state = (current as? UiState.Success)?.data ?: return@update current
            UiState.Success(transform(state))
        }
    }

    fun save() {
        val form = currentForm.validate()
        if (form.errors.isNotEmpty()) {
            _uiState.update { UiState.Success(form) }
            return
        }

        val vehicle = form.toVehicle() ?: run {
            _uiState.update { UiState.Error("Failed to create vehicle from form data") }
            return
        }

        viewModelScope.launch {
            _uiState.update { UiState.Loading }

            vehicleRepository.saveVehicle(vehicle)
                .onSuccess {
                    try {
                        saveMaintenanceSchedules(form.templateSelection, vehicle.id, vehicle.odometerKm)
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to save maintenance schedules for vehicle ${vehicle.id}")
                    }
                    try {
                        activeVehicleRepository.setActiveVehicle(vehicle.id)
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to set active vehicle ${vehicle.id}")
                    }
                    _uiState.update { UiState.Success(form.copy(errors = emptyMap())) }
                }
                .onFailure { e ->
                    _uiState.update { UiState.Error(e.message ?: "Save failed", e) }
                }
        }
    }

    private suspend fun saveMaintenanceSchedules(
        selection: TemplateSelection,
        vehicleId: String,
        odometerKm: Double,
    ) {
        val schedules = when (selection) {
            TemplateSelection.MITSUBISHI_LANCER_EX_2015 ->
                MaintenanceTemplates.mitsubishiLancerEx2015(vehicleId, odometerKm)
            TemplateSelection.CUSTOM, TemplateSelection.NONE -> emptyList()
        }
        if (schedules.isNotEmpty()) {
            maintenanceRepository.saveSchedules(schedules)
        }
    }
}
