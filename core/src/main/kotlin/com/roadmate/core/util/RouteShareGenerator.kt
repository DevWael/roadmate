package com.roadmate.core.util

import com.roadmate.core.database.entity.TripPoint
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object RouteShareGenerator {

    private const val MAX_WAYPOINTS = 25
    private const val OSM_BASE_URL = "https://www.openstreetmap.org/directions?route="

    fun sampleWaypoints(points: List<TripPoint>, maxPoints: Int = MAX_WAYPOINTS): List<TripPoint> {
        if (points.size <= maxPoints) return points
        val step = (points.size - 2).toDouble() / (maxPoints - 2)
        return buildList {
            add(points.first())
            for (i in 1 until maxPoints - 1) {
                add(points[(i * step).roundToInt()])
            }
            add(points.last())
        }
    }

    fun generateOsmUrl(points: List<TripPoint>): String {
        val sampled = sampleWaypoints(points)
        val route = sampled.joinToString(";") { "${it.latitude},${it.longitude}" }
        return "$OSM_BASE_URL$route"
    }

    fun formatShareText(
        startTimeMs: Long,
        distanceKm: Double,
        durationMs: Long,
        url: String,
    ): String {
        val date = java.time.Instant.ofEpochMilli(startTimeMs)
            .atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
        val duration = formatDuration(durationMs)
        return "My trip on $date: ${String.format(Locale.US, "%.1f", distanceKm)} km, $duration — $url"
    }

    fun generateRouteShare(
        points: List<TripPoint>,
        startTimeMs: Long,
        distanceKm: Double,
        durationMs: Long,
    ): String {
        val url = generateOsmUrl(points)
        return formatShareText(startTimeMs, distanceKm, durationMs, url)
    }

    private fun formatDuration(durationMs: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        return String.format(Locale.US, "%d:%02d", hours, minutes)
    }
}
