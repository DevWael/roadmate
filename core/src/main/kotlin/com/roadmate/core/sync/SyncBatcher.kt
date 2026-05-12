package com.roadmate.core.sync

import com.roadmate.core.database.entity.TripPoint
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncBatcher @Inject constructor() {

    companion object {
        const val BATCH_SIZE = 100
    }

    fun batchTripPoints(points: List<TripPoint>): List<List<TripPoint>> {
        if (points.isEmpty()) return emptyList()
        if (points.size <= BATCH_SIZE) return listOf(points)
        return points.chunked(BATCH_SIZE)
    }
}
