package ca.mytrails.app.recording

/** One GPS fix as the recording logic sees it, independent of the Android Location class. */
data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val timeMillis: Long,
    val altitude: Double?,
    val accuracyMeters: Float,
    val speedMetersPerSecond: Float?,
)
