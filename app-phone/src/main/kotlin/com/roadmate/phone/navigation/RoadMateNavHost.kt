package com.roadmate.phone.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.navDeepLink
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.roadmate.core.model.UiState
import com.roadmate.core.repository.VehicleRepository
import com.roadmate.phone.ui.components.EmptyVehicleState
import com.roadmate.phone.ui.documents.DocumentDetailScreen
import com.roadmate.phone.ui.documents.DocumentListScreen
import com.roadmate.phone.ui.fuel.FuelLogScreen
import com.roadmate.phone.ui.hub.VehicleHubScreen
import com.roadmate.phone.ui.maintenance.MaintenanceDetailScreen
import com.roadmate.phone.ui.maintenance.MaintenanceListScreen
import com.roadmate.phone.ui.maintenance.MaintenanceCompletionSheetState
import com.roadmate.phone.ui.maintenance.MaintenanceDetailViewModel
import com.roadmate.phone.ui.settings.VehicleManagementScreen
import com.roadmate.phone.ui.settings.ExportScreen
import com.roadmate.phone.ui.statistics.StatisticsScreen
import com.roadmate.phone.ui.trips.TripDetailScreen
import com.roadmate.phone.ui.trips.TripListScreen

@Composable
fun RoadMateNavHost(
    vehicleRepository: VehicleRepository,
    modifier: Modifier = Modifier,
) {
    val vehicleCount by vehicleRepository.getVehicleCount().collectAsState(initial = -1)

    when {
        vehicleCount < 0 -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        vehicleCount == 0 -> {
            EmptyVehicleState(modifier = Modifier.fillMaxSize())
        }
        else -> {
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = VehicleHub,
                modifier = modifier,
            ) {
                composable<VehicleHub> {
                    VehicleHubScreen(
                        onTripListClick = { navController.navigate(TripList) },
                        onTripClick = { tripId -> navController.navigate(TripDetail(tripId)) },
                        onMaintenanceListClick = { navController.navigate(MaintenanceList) },
                        onMaintenanceClick = { scheduleId ->
                            navController.navigate(MaintenanceDetail(scheduleId))
                        },
                        onFuelLogClick = { navController.navigate(FuelLog) },
                        onDocumentListClick = { navController.navigate(DocumentList) },
                        onVehicleManagementClick = { navController.navigate(VehicleManagement) },
                        onStatisticsClick = { navController.navigate(Statistics) },
                        onExportClick = { navController.navigate(ExportData) },
                    )
                }

                composable<TripList> {
                    TripListScreen(
                        onTripClick = { tripId ->
                            navController.navigate(TripDetail(tripId))
                        },
                    )
                }

                composable<TripDetail>(
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "roadmate://trip/{tripId}" },
                    ),
                ) { backStackEntry ->
                    val route: TripDetail = backStackEntry.toRoute()
                    TripDetailScreen(
                        tripId = route.tripId,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<MaintenanceList> {
                    MaintenanceListScreen(
                        onMaintenanceClick = { scheduleId ->
                            navController.navigate(MaintenanceDetail(scheduleId))
                        },
                    )
                }

                composable<MaintenanceDetail>(
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "roadmate://maintenance/{scheduleId}" },
                    ),
                ) { backStackEntry ->
                    val route: MaintenanceDetail = backStackEntry.toRoute()
                    val detailViewModel: MaintenanceDetailViewModel = hiltViewModel()
                    val detailUiState by detailViewModel.uiState.collectAsStateWithLifecycle()
                    val showSheet by detailViewModel.showCompletionSheet.collectAsStateWithLifecycle()
                    val completionDate by detailViewModel.completionDate.collectAsStateWithLifecycle()
                    val completionOdo by detailViewModel.completionOdometerKm.collectAsStateWithLifecycle()
                    val completionCost by detailViewModel.completionCost.collectAsStateWithLifecycle()
                    val completionLocation by detailViewModel.completionLocation.collectAsStateWithLifecycle()
                    val completionNotes by detailViewModel.completionNotes.collectAsStateWithLifecycle()
                    val completionErrors by detailViewModel.completionErrors.collectAsStateWithLifecycle()
                    val isSaveEnabled by detailViewModel.isSaveEnabled.collectAsStateWithLifecycle()

                    LaunchedEffect(route.scheduleId) {
                        detailViewModel.loadSchedule(route.scheduleId)
                    }

                    MaintenanceDetailScreen(
                        uiState = detailUiState,
                        onBack = { navController.popBackStack() },
                        completionSheetState = MaintenanceCompletionSheetState(
                            isVisible = showSheet,
                            datePerformed = completionDate,
                            odometerKm = completionOdo,
                            vehicleOdometerKm = detailViewModel.getVehicleOdometerKm(),
                            cost = completionCost,
                            location = completionLocation,
                            notes = completionNotes,
                            errors = completionErrors,
                            isSaveEnabled = isSaveEnabled,
                            onShowSheet = detailViewModel::onShowCompletionSheet,
                            onDateChange = detailViewModel::onCompletionDateChange,
                            onOdometerKmChange = detailViewModel::onCompletionOdometerKmChange,
                            onCostChange = detailViewModel::onCompletionCostChange,
                            onLocationChange = detailViewModel::onCompletionLocationChange,
                            onNotesChange = detailViewModel::onCompletionNotesChange,
                            onSave = detailViewModel::completeMaintenance,
                            onDismiss = detailViewModel::onDismissCompletionSheet,
                        ),
                    )
                }

                composable<FuelLog> {
                    FuelLogScreen(
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<DocumentList> {
                    DocumentListScreen(
                        onDocumentClick = { documentId ->
                            navController.navigate(DocumentDetail(documentId))
                        },
                    )
                }

                composable<DocumentDetail>(
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "roadmate://document/{documentId}" },
                    ),
                ) { backStackEntry ->
                    val route: DocumentDetail = backStackEntry.toRoute()
                    DocumentDetailScreen(
                        documentId = route.documentId,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<VehicleManagement> {
                    VehicleManagementScreen(
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<Statistics> {
                    StatisticsScreen(
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<ExportData> {
                    ExportScreen(
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
