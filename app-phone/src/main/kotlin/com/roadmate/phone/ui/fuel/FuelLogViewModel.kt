package com.roadmate.phone.ui.fuel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadmate.core.database.entity.FuelLog
import com.roadmate.core.database.entity.Vehicle
import com.roadmate.core.model.UiState
import com.roadmate.core.repository.ActiveVehicleRepository
import com.roadmate.core.repository.FuelRepository
import com.roadmate.core.repository.VehicleRepository
import com.roadmate.core.util.FuelConsumptionCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class FuelLogEntryUi(
    val fuelLog: FuelLog,
    val consumptionLPer100km: Double?,
    val isOverConsumption: Boolean,
)

data class FuelSummary(
    val totalCostThisMonth: Double,
    val avgLPer100km: Double?,
    val avgCostPerKm: Double?,
)

data class FuelLogUiState(
    val entries: List<FuelLogEntryUi>,
    val summary: FuelSummary,
    val vehicle: Vehicle,
    val latestOdometerKm: Double?,
    val estimatedLPer100km: Double = 0.0,
)

@HiltViewModel
class FuelLogViewModel @Inject constructor(
    private val activeVehicleRepository: ActiveVehicleRepository,
    private val fuelRepository: FuelRepository,
    private val vehicleRepository: VehicleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<FuelLogUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<FuelLogUiState>> = _uiState.asStateFlow()

    private val _showAddSheet = MutableStateFlow(false)
    val showAddSheet: StateFlow<Boolean> = _showAddSheet.asStateFlow()

    private val _formDate = MutableStateFlow(System.currentTimeMillis())
    val formDate: StateFlow<Long> = _formDate.asStateFlow()

    private val _formOdometerKm = MutableStateFlow("")
    val formOdometerKm: StateFlow<String> = _formOdometerKm.asStateFlow()

    private val _formLiters = MutableStateFlow("")
    val formLiters: StateFlow<String> = _formLiters.asStateFlow()

    private val _formPricePerLiter = MutableStateFlow("")
    val formPricePerLiter: StateFlow<String> = _formPricePerLiter.asStateFlow()

    private val _formIsFullTank = MutableStateFlow(true)
    val formIsFullTank: StateFlow<Boolean> = _formIsFullTank.asStateFlow()

    private val _formStation = MutableStateFlow("")
    val formStation: StateFlow<String> = _formStation.asStateFlow()

    private val _formErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val formErrors: StateFlow<Map<String, String>> = _formErrors.asStateFlow()

    private val _isSaving = MutableStateFlow(false)

    val totalCost: StateFlow<String> = combine(_formLiters, _formPricePerLiter) { l, p ->
        calculateTotalCost(l, p)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val isSaveEnabled: StateFlow<Boolean> = combine(
        _formOdometerKm,
        _formLiters,
        _formPricePerLiter,
        _isSaving,
        _formErrors,
    ) { odo, liters, price, saving, errors ->
        !saving && odo.isNotBlank() && liters.isNotBlank() && price.isNotBlank() && errors.isEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    @Volatile
    private var latestOdoForValidation: Double? = null

    init {
        loadData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadData() {
        viewModelScope.launch {
            activeVehicleRepository.activeVehicleId
                .flatMapLatest { vehicleId ->
                    if (vehicleId == null) {
                        flowOf(UiState.Error("No active vehicle") as UiState<FuelLogUiState>)
                    } else {
                        combine(
                            fuelRepository.getFuelLogsForVehicle(vehicleId),
                            vehicleRepository.getVehicle(vehicleId),
                        ) { logs, vehicle ->
                            if (vehicle == null) {
                                UiState.Error("Vehicle not found") as UiState<FuelLogUiState>
                            } else {
                                val fullTankPairs = FuelConsumptionCalculator.findFullTankPairs(logs)
                                val estimated = FuelConsumptionCalculator.calculateEstimatedConsumption(
                                    vehicle.cityConsumption, vehicle.highwayConsumption
                                )
                                val entries = buildEntryUiList(logs, vehicle, fullTankPairs, estimated)
                                val summary = buildSummary(logs, fullTankPairs)
                                val latestOdo = logs.maxOfOrNull { it.odometerKm }
                                latestOdoForValidation = latestOdo
                                UiState.Success(
                                    FuelLogUiState(
                                        entries = entries,
                                        summary = summary,
                                        vehicle = vehicle,
                                        latestOdometerKm = latestOdo,
                                        estimatedLPer100km = estimated,
                                    )
                                ) as UiState<FuelLogUiState>
                            }
                        }
                    }
                }
                .collect { state -> _uiState.value = state }
        }
    }

    private fun buildEntryUiList(
        logs: List<FuelLog>,
        vehicle: Vehicle,
        fullTankPairs: List<Pair<FuelLog, FuelLog>>,
        estimated: Double,
    ): List<FuelLogEntryUi> {
        val pairMap = mutableMapOf<String, Pair<FuelLog, FuelLog>>()
        for (pair in fullTankPairs) {
            pairMap[pair.first.id] = pair
        }

        return logs.map { log ->
            val pair = pairMap[log.id]
            val consumption = if (pair != null) {
                FuelConsumptionCalculator.calculateActualConsumption(pair.first, pair.second)
            } else null
            val isOver = consumption != null && FuelConsumptionCalculator.isOverConsumption(consumption, estimated)
            FuelLogEntryUi(
                fuelLog = log,
                consumptionLPer100km = consumption,
                isOverConsumption = isOver,
            )
        }
    }

    private fun buildSummary(
        logs: List<FuelLog>,
        fullTankPairs: List<Pair<FuelLog, FuelLog>>,
    ): FuelSummary {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val monthStart = cal.timeInMillis

        val totalCostMonth = FuelConsumptionCalculator.calculateTotalCostThisMonth(logs, monthStart)
        val avgL = FuelConsumptionCalculator.calculateAvgLPer100km(fullTankPairs)
        val avgCost = FuelConsumptionCalculator.calculateAvgCostPerKm(logs)

        return FuelSummary(
            totalCostThisMonth = totalCostMonth,
            avgLPer100km = avgL,
            avgCostPerKm = avgCost,
        )
    }

    fun onAddClick() {
        _formDate.value = System.currentTimeMillis()
        val currentState = _uiState.value
        val vehicleOdo = (currentState as? UiState.Success)?.data?.vehicle?.odometerKm
        _formOdometerKm.value = vehicleOdo?.toInt()?.toString() ?: ""
        _formLiters.value = ""
        _formPricePerLiter.value = ""
        _formIsFullTank.value = true
        _formStation.value = ""
        _formErrors.value = emptyMap()
        _showAddSheet.value = true
    }

    fun onDismissSheet() {
        _showAddSheet.value = false
    }

    fun onOdometerKmChange(value: String) {
        _formOdometerKm.value = value
        validateOdo(value)
    }

    fun onLitersChange(value: String) {
        _formLiters.value = value
    }

    fun onPricePerLiterChange(value: String) {
        _formPricePerLiter.value = value
    }

    fun onDateChange(date: Long) {
        _formDate.value = date
    }

    fun onIsFullTankChange(value: Boolean) {
        _formIsFullTank.value = value
    }

    fun onStationChange(value: String) {
        _formStation.value = value
    }

    private fun validateOdo(value: String) {
        val errors = _formErrors.value.toMutableMap()
        if (value.isBlank()) {
            errors.remove("odometerKm")
        } else {
            val odo = value.toDoubleOrNull()
            if (odo == null) {
                errors["odometerKm"] = "Enter a valid number"
            } else if (latestOdoForValidation != null && odo < latestOdoForValidation!!) {
                errors["odometerKm"] = "ODO must be greater than or equal to previous entry (${latestOdoForValidation!!.toInt()} km)"
            } else {
                errors.remove("odometerKm")
            }
        }
        _formErrors.value = errors
    }

    fun saveFuelEntry() {
        val litersVal = _formLiters.value.toDoubleOrNull() ?: return
        val priceVal = _formPricePerLiter.value.toDoubleOrNull() ?: return
        val odoVal = _formOdometerKm.value.toDoubleOrNull() ?: return

        if (_formErrors.value.isNotEmpty()) return

        viewModelScope.launch {
            _isSaving.value = true
            val vehicleId = activeVehicleRepository.activeVehicleId.first() ?: run {
                _isSaving.value = false
                return@launch
            }

            val fuelLog = FuelLog(
                vehicleId = vehicleId,
                date = _formDate.value,
                odometerKm = odoVal,
                liters = litersVal,
                pricePerLiter = priceVal,
                totalCost = litersVal * priceVal,
                isFullTank = _formIsFullTank.value,
                station = _formStation.value.ifBlank { null },
            )

            val result = fuelRepository.saveFuelLog(fuelLog)
            _isSaving.value = false
            if (result.isSuccess) {
                _showAddSheet.value = false
            } else {
                val errors = _formErrors.value.toMutableMap()
                errors["save"] = "Failed to save. Please try again."
                _formErrors.value = errors
            }
        }
    }
}
