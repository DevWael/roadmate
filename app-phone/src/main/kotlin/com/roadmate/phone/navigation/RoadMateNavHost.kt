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
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.roadmate.core.repository.VehicleRepository
import com.roadmate.phone.ui.components.EmptyVehicleState
import com.roadmate.phone.ui.documents.DocumentDetailScreen
import com.roadmate.phone.ui.documents.DocumentListScreen
import com.roadmate.phone.ui.fuel.FuelLogScreen
import com.roadmate.phone.ui.hub.VehicleHubScreen
import com.roadmate.phone.ui.maintenance.MaintenanceListScreen
import com.roadmate.phone.ui.settings.VehicleManagementScreen
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
                    // TODO: Wire to MaintenanceDetailViewModel in Story 5-5
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Maintenance Detail: ${route.scheduleId}",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
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
            }
        }
    }
}
