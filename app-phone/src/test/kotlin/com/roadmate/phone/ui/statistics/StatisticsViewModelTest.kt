package com.roadmate.phone.ui.statistics

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import app.cash.turbine.test
import com.roadmate.core.database.dao.FuelDao
import com.roadmate.core.database.dao.MaintenanceDao
import com.roadmate.core.database.dao.TripDao
import com.roadmate.core.database.dao.VehicleDao
import com.roadmate.core.database.entity.FuelLog
import com.roadmate.core.database.entity.MaintenanceRecord
import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.database.entity.Trip
import com.roadmate.core.database.entity.TripPoint
import com.roadmate.core.database.entity.TripStatus
import com.roadmate.core.model.UiState
import com.roadmate.core.repository.ActiveVehicleRepository
import com.roadmate.core.repository.FuelRepository
import com.roadmate.core.repository.MaintenanceRepository
import com.roadmate.core.repository.TripRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("StatisticsViewModel")
class StatisticsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeTripDao: StatsFakeTripDao
    private lateinit var fakeFuelDao: StatsFakeFuelDao
    private lateinit var fakeMaintenanceDao: StatsFakeMaintenanceDao
    private lateinit var fakeDataStore: StatsFakePreferencesDataStore
    private lateinit var tripRepository: TripRepository
    private lateinit var fuelRepository: FuelRepository
    private lateinit var maintenanceRepository: MaintenanceRepository
    private lateinit var activeVehicleRepository: ActiveVehicleRepository
    private lateinit var viewModel: StatisticsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeTripDao = StatsFakeTripDao()
        fakeFuelDao = StatsFakeFuelDao()
        fakeMaintenanceDao = StatsFakeMaintenanceDao()
        fakeDataStore = StatsFakePreferencesDataStore()
        tripRepository = TripRepository(fakeTripDao)
        fuelRepository = FuelRepository(fakeFuelDao)
        maintenanceRepository = MaintenanceRepository(fakeMaintenanceDao)
        activeVehicleRepository = ActiveVehicleRepository(fakeDataStore)
    }

    private fun createViewModel() {
        viewModel = StatisticsViewModel(
            activeVehicleRepository = activeVehicleRepository,
            tripRepository = tripRepository,
            fuelRepository = fuelRepository,
            maintenanceRepository = maintenanceRepository,
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("initial load")
    inner class InitialLoad {

        @Test
        fun `shows error when no active vehicle`() = runTest {
            fakeTripDao.updateFlow()
            fakeFuelDao.updateFlow()
            fakeMaintenanceDao.updateRecordFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Error)
            }
        }

        @Test
        fun `loads statistics for default month period`() = runTest {
            activeVehicleRepository.setActiveVehicle("v-1")
            val now = LocalDate.now()
            val zone = ZoneId.systemDefault()
            val monthStart = now.withDayOfMonth(1)
                .atStartOfDay(zone).toInstant().toEpochMilli()

            fakeTripDao.trips["t-1"] = testTrip(
                startTime = monthStart + 1000,
                distanceKm = 50.0,
                status = TripStatus.COMPLETED,
            )
            fakeFuelDao.fuelLogs["f-1"] = testFuelLog(
                date = monthStart + 2000,
                totalCost = 100.0,
            )
            fakeMaintenanceDao.records["mr-1"] = testMaintenanceRecord(
                datePerformed = monthStart + 3000,
                cost = 50.0,
            )

            fakeTripDao.updateFlow()
            fakeFuelDao.updateFlow()
            fakeMaintenanceDao.updateRecordFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                assertEquals(StatisticsPeriod.MONTH, data.period)
                assertEquals(50.0, data.statistics.totalDistanceKm, 0.01)
                assertEquals(1, data.statistics.totalTrips)
                assertEquals(100.0, data.statistics.totalFuelCost, 0.01)
                assertEquals(50.0, data.statistics.totalMaintenanceCost, 0.01)
                assertNull(data.weekComparison)
                assertNull(data.yearBreakdown)
            }
        }
    }

    @Nested
    @DisplayName("empty state")
    inner class EmptyState {

        @Test
        fun `shows zeros when no data in period`() = runTest {
            activeVehicleRepository.setActiveVehicle("v-1")

            fakeTripDao.updateFlow()
            fakeFuelDao.updateFlow()
            fakeMaintenanceDao.updateRecordFlow()

            createViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                assertEquals(0.0, data.statistics.totalDistanceKm, 0.01)
                assertEquals(0, data.statistics.totalTrips)
                assertEquals(0.0, data.statistics.totalFuelCost, 0.01)
                assertEquals(0.0, data.statistics.totalMaintenanceCost, 0.01)
            }
        }
    }

    @Nested
    @DisplayName("week comparison")
    inner class WeekComparison {

        @Test
        fun `provides week comparison when period is week`() = runTest {
            activeVehicleRepository.setActiveVehicle("v-1")
            val zone = ZoneId.systemDefault()
            val now = LocalDate.now()
            val (weekStart, weekEnd) = com.roadmate.core.util.StatisticsCalculator.weekRange(now)
            val prevWeekStart = weekStart - (weekEnd - weekStart)

            fakeTripDao.trips["t-1"] = testTrip(
                startTime = weekStart + 1000,
                distanceKm = 100.0,
                status = TripStatus.COMPLETED,
            )
            fakeTripDao.trips["t-2"] = testTrip(
                startTime = prevWeekStart + 1000,
                distanceKm = 50.0,
                status = TripStatus.COMPLETED,
            )
            fakeFuelDao.fuelLogs["f-1"] = testFuelLog(
                date = weekStart + 2000,
                totalCost = 200.0,
            )
            fakeFuelDao.fuelLogs["f-2"] = testFuelLog(
                date = prevWeekStart + 2000,
                totalCost = 100.0,
            )

            fakeTripDao.updateFlow()
            fakeFuelDao.updateFlow()
            fakeMaintenanceDao.updateRecordFlow()

            createViewModel()
            viewModel.setPeriod(StatisticsPeriod.WEEK)

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                assertNotNull(data.weekComparison)
                assertEquals(100.0, data.weekComparison!!.currentDistanceKm, 0.01)
                assertEquals(50.0, data.weekComparison!!.previousDistanceKm, 0.01)
                assertNotNull(data.weekComparison!!.distanceChangePercent)
                assertEquals(100.0, data.weekComparison!!.distanceChangePercent!!, 0.01)
            }
        }
    }

    @Nested
    @DisplayName("year breakdown")
    inner class YearBreakdown {

        @Test
        fun `provides year breakdown when period is year`() = runTest {
            activeVehicleRepository.setActiveVehicle("v-1")

            fakeTripDao.updateFlow()
            fakeFuelDao.updateFlow()
            fakeMaintenanceDao.updateRecordFlow()

            createViewModel()
            viewModel.setPeriod(StatisticsPeriod.YEAR)

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state is UiState.Success)
                val data = (state as UiState.Success).data
                assertNotNull(data.yearBreakdown)
                assertEquals(12, data.yearBreakdown!!.size)
                assertNotNull(data.yearRunningTotal)
            }
        }
    }

    private fun testTrip(
        id: String = "t-1",
        vehicleId: String = "v-1",
        startTime: Long = System.currentTimeMillis(),
        distanceKm: Double = 25.0,
        status: TripStatus = TripStatus.COMPLETED,
    ) = Trip(
        id = id,
        vehicleId = vehicleId,
        startTime = startTime,
        endTime = startTime + 1800000,
        distanceKm = distanceKm,
        durationMs = 1800000,
        maxSpeedKmh = 80.0,
        avgSpeedKmh = 50.0,
        estimatedFuelL = 2.0,
        startOdometerKm = 85000.0,
        endOdometerKm = 85000.0 + distanceKm,
        status = status,
    )

    private fun testFuelLog(
        id: String = "f-1",
        vehicleId: String = "v-1",
        date: Long = System.currentTimeMillis(),
        totalCost: Double = 60.0,
    ) = FuelLog(
        id = id,
        vehicleId = vehicleId,
        date = date,
        odometerKm = 85000.0,
        liters = 40.0,
        pricePerLiter = 1.5,
        totalCost = totalCost,
        isFullTank = true,
    )

    private fun testMaintenanceRecord(
        id: String = "mr-1",
        vehicleId: String = "v-1",
        scheduleId: String = "s-1",
        datePerformed: Long = System.currentTimeMillis(),
        cost: Double = 100.0,
    ) = MaintenanceRecord(
        id = id,
        scheduleId = scheduleId,
        vehicleId = vehicleId,
        datePerformed = datePerformed,
        odometerKm = 85000.0,
        cost = cost,
    )
}

private class StatsFakePreferencesDataStore : androidx.datastore.core.DataStore<Preferences> {
    private val _data = MutableStateFlow<Preferences>(emptyPreferences())
    override val data: Flow<Preferences> = _data

    override suspend fun updateData(
        transformer: suspend (Preferences) -> Preferences,
    ): Preferences {
        val newValue = transformer(_data.value)
        _data.value = newValue
        return newValue
    }
}

private class StatsFakeTripDao : TripDao() {
    val trips = mutableMapOf<String, Trip>()
    private val tripFlow = MutableStateFlow<List<Trip>>(emptyList())
    fun updateFlow() { tripFlow.value = trips.values.toList() }
    override fun getTripsForVehicle(vehicleId: String): Flow<List<Trip>> =
        tripFlow.map { it.filter { t -> t.vehicleId == vehicleId } }
    override fun getTripPointsForTrip(tripId: String): Flow<List<TripPoint>> = MutableStateFlow(emptyList())
    override fun getActiveTrip(vehicleId: String): Flow<Trip?> = MutableStateFlow(null)
    override fun getTrip(tripId: String): Flow<Trip?> = tripFlow.map { it.find { t -> t.id == tripId } }
    override suspend fun upsertTrip(trip: Trip) { trips[trip.id] = trip; updateFlow() }
    override suspend fun upsertTrips(trips: List<Trip>) { trips.forEach { this.trips[it.id] = it }; updateFlow() }
    override suspend fun deleteTrip(trip: Trip) { trips.remove(trip.id); updateFlow() }
    override suspend fun deleteTripById(tripId: String) { trips.remove(tripId); updateFlow() }
    override suspend fun upsertTripPoint(tripPoint: TripPoint) {}
    override suspend fun upsertTripPoints(tripPoints: List<TripPoint>) {}
    override suspend fun deleteTripPoint(tripPoint: TripPoint) {}
    override suspend fun getTripsModifiedSince(since: Long): List<Trip> = emptyList()
    override suspend fun getTripPointsModifiedSince(since: Long): List<TripPoint> = emptyList()
    override suspend fun getTripById(id: String): Trip? = trips[id]
    override suspend fun getTripPointById(id: String): TripPoint? = null
}

private class StatsFakeFuelDao : FuelDao {
    val fuelLogs = mutableMapOf<String, FuelLog>()
    private val fuelFlow = MutableStateFlow<List<FuelLog>>(emptyList())
    fun updateFlow() { fuelFlow.value = fuelLogs.values.toList() }
    override fun getFuelLogsForVehicle(vehicleId: String): Flow<List<FuelLog>> =
        fuelFlow.map { it.filter { f -> f.vehicleId == vehicleId } }
    override fun getLastFullTankEntry(vehicleId: String): Flow<FuelLog?> = MutableStateFlow(null)
    override fun getLatestFuelEntry(vehicleId: String): Flow<FuelLog?> = MutableStateFlow(null)
    override fun getTwoLastFullTankEntries(vehicleId: String): Flow<List<FuelLog>> = MutableStateFlow(emptyList())
    override fun getFuelLog(fuelLogId: String): Flow<FuelLog?> = MutableStateFlow(null)
    override suspend fun upsertFuelLog(fuelLog: FuelLog) { fuelLogs[fuelLog.id] = fuelLog; updateFlow() }
    override suspend fun upsertFuelLogs(fuelLogs: List<FuelLog>) { fuelLogs.forEach { this.fuelLogs[it.id] = it }; updateFlow() }
    override suspend fun deleteFuelLog(fuelLog: FuelLog) { fuelLogs.remove(fuelLog.id); updateFlow() }
    override suspend fun deleteFuelLogById(fuelLogId: String) { fuelLogs.remove(fuelLogId); updateFlow() }
    override suspend fun getFuelLogsModifiedSince(since: Long): List<FuelLog> = emptyList()
    override suspend fun getFuelLogById(id: String): FuelLog? = fuelLogs[id]
}

private class StatsFakeMaintenanceDao : MaintenanceDao() {
    val records = mutableMapOf<String, MaintenanceRecord>()
    private val recordFlow = MutableStateFlow<List<MaintenanceRecord>>(emptyList())
    fun updateRecordFlow() { recordFlow.value = records.values.toList() }
    override fun getSchedulesForVehicle(vehicleId: String): Flow<List<MaintenanceSchedule>> = MutableStateFlow(emptyList())
    override fun getSchedule(scheduleId: String): Flow<MaintenanceSchedule?> = MutableStateFlow(null)
    override suspend fun upsertSchedule(schedule: MaintenanceSchedule) {}
    override suspend fun upsertSchedules(schedules: List<MaintenanceSchedule>) {}
    override suspend fun deleteSchedule(schedule: MaintenanceSchedule) {}
    override suspend fun deleteScheduleById(scheduleId: String) {}
    override fun getRecordsForSchedule(scheduleId: String): Flow<List<MaintenanceRecord>> =
        recordFlow.map { it.filter { r -> r.scheduleId == scheduleId } }
    override fun getRecordsForVehicle(vehicleId: String): Flow<List<MaintenanceRecord>> =
        recordFlow.map { it.filter { r -> r.vehicleId == vehicleId } }
    override fun getRecord(recordId: String): Flow<MaintenanceRecord?> =
        recordFlow.map { it.find { r -> r.id == recordId } }
    override suspend fun upsertRecord(record: MaintenanceRecord) { records[record.id] = record; updateRecordFlow() }
    override suspend fun upsertRecords(records: List<MaintenanceRecord>) { records.forEach { this.records[it.id] = it }; updateRecordFlow() }
    override suspend fun deleteRecord(record: MaintenanceRecord) { records.remove(record.id); updateRecordFlow() }
    override suspend fun deleteRecordById(recordId: String) { records.remove(recordId); updateRecordFlow() }
    override suspend fun deleteRecordsByScheduleId(scheduleId: String) {}
    override suspend fun getSchedulesModifiedSince(since: Long): List<MaintenanceSchedule> = emptyList()
    override suspend fun getRecordsModifiedSince(since: Long): List<MaintenanceRecord> = emptyList()
    override suspend fun getScheduleById(id: String): MaintenanceSchedule? = null
    override suspend fun getRecordById(id: String): MaintenanceRecord? = records[id]
}
