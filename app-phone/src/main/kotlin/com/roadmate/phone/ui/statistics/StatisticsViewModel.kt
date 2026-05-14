package com.roadmate.phone.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadmate.core.database.entity.FuelLog
import com.roadmate.core.database.entity.MaintenanceRecord
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.model.UiState
import com.roadmate.core.repository.ActiveVehicleRepository
import com.roadmate.core.repository.FuelRepository
import com.roadmate.core.repository.MaintenanceRepository
import com.roadmate.core.repository.TripRepository
import com.roadmate.core.util.StatisticsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class StatisticsPeriod {
    DAY, WEEK, MONTH, YEAR,
}

data class StatisticsUiState(
    val period: StatisticsPeriod,
    val selectedDate: LocalDate,
    val statistics: StatisticsCalculator.DrivingStatistics,
    val weekComparison: StatisticsCalculator.WeekComparison?,
    val yearBreakdown: List<StatisticsCalculator.MonthBreakdown>?,
    val yearRunningTotal: StatisticsCalculator.DrivingStatistics?,
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val activeVehicleRepository: ActiveVehicleRepository,
    private val tripRepository: TripRepository,
    private val fuelRepository: FuelRepository,
    private val maintenanceRepository: MaintenanceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<StatisticsUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<StatisticsUiState>> = _uiState.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(StatisticsPeriod.MONTH)
    private val _selectedDate = MutableStateFlow(LocalDate.now())

    init {
        loadData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadData() {
        viewModelScope.launch {
            combine(
                activeVehicleRepository.activeVehicleId,
                _selectedPeriod,
                _selectedDate,
            ) { vehicleId, period, date -> Triple(vehicleId, period, date) }
                .flatMapLatest { (vehicleId, period, date) ->
                    if (vehicleId == null) {
                        flowOf(UiState.Error("No active vehicle") as UiState<StatisticsUiState>)
                    } else {
                        combine(
                            tripRepository.getTripsForVehicle(vehicleId),
                            fuelRepository.getFuelLogsForVehicle(vehicleId),
                            maintenanceRepository.getRecordsForVehicle(vehicleId),
                        ) { trips, fuelLogs, records ->
                            UiState.Success(
                                buildUiState(trips, fuelLogs, records, period, date)
                            ) as UiState<StatisticsUiState>
                        }
                    }
                }
                .collect { state -> _uiState.value = state }
        }
    }

    private fun buildUiState(
        trips: List<Trip>,
        fuelLogs: List<FuelLog>,
        records: List<MaintenanceRecord>,
        period: StatisticsPeriod,
        date: LocalDate,
    ): StatisticsUiState {
        val (from, to) = when (period) {
            StatisticsPeriod.DAY -> StatisticsCalculator.dayRange(date)
            StatisticsPeriod.WEEK -> StatisticsCalculator.weekRange(date)
            StatisticsPeriod.MONTH -> StatisticsCalculator.monthRange(date)
            StatisticsPeriod.YEAR -> StatisticsCalculator.yearRange(date)
        }

        val statistics = StatisticsCalculator.calculateStatistics(
            trips, fuelLogs, records, from, to,
        )

        val weekComparison = if (period == StatisticsPeriod.WEEK) {
            val (weekStart, weekEnd) = StatisticsCalculator.weekRange(date)
            val previousWeekStart = weekStart - (weekEnd - weekStart)
            StatisticsCalculator.calculateWeekComparison(
                trips, fuelLogs, weekStart, weekEnd, previousWeekStart, weekStart,
            )
        } else null

        val yearBreakdown: List<StatisticsCalculator.MonthBreakdown>?
        val yearRunningTotal: StatisticsCalculator.DrivingStatistics?

        if (period == StatisticsPeriod.YEAR) {
            val (breakdown, total) = StatisticsCalculator.calculateYearBreakdown(
                trips, fuelLogs, records, date.year,
            )
            yearBreakdown = breakdown
            yearRunningTotal = total
        } else {
            yearBreakdown = null
            yearRunningTotal = null
        }

        return StatisticsUiState(
            period = period,
            selectedDate = date,
            statistics = statistics,
            weekComparison = weekComparison,
            yearBreakdown = yearBreakdown,
            yearRunningTotal = yearRunningTotal,
        )
    }

    fun setPeriod(period: StatisticsPeriod) {
        _selectedPeriod.value = period
    }

    fun setDate(date: LocalDate) {
        _selectedDate.value = date
    }
}
