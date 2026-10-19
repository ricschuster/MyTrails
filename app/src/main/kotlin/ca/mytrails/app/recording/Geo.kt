package ca.mytrails.app.recording

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_008.8

/** Great-circle distance in meters between two points (haversine). */
fun distanceMeters(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double,
): Double {
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val dPhi = phi2 - phi1
    val dLambda = Math.toRadians(lon2 - lon1)
    val a = sin(dPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(dLambda / 2).pow(2)
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(a))
}

fun distanceMeters(
    a: TrackPoint,
    b: TrackPoint,
): Double = distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
