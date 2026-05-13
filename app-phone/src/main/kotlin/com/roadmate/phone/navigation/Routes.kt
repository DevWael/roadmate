package com.roadmate.phone.navigation

import kotlinx.serialization.Serializable

@Serializable object VehicleHub
@Serializable object TripList
@Serializable data class TripDetail(val tripId: String)
@Serializable object MaintenanceList
@Serializable data class MaintenanceDetail(val scheduleId: String)
@Serializable object FuelLog
@Serializable object DocumentList
@Serializable data class DocumentDetail(val documentId: String)
@Serializable object VehicleManagement
