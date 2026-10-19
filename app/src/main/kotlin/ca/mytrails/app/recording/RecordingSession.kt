package ca.mytrails.app.recording

/** Tunable sampling constants from docs/design/02_track_recording.md. */
object Sampling {
    const val MIN_DISTANCE_METERS = 5.0
    const val MAX_INTERVAL_MILLIS = 10_000L
    const val MAX_ACCURACY_METERS = 30f
}

/**
 * Decides which fixes become stored points and tracks distance and segments.
 *
 * Plain Kotlin with no Android types so it can be unit tested on the JVM. Call
 * [onFix] for every fix while recording; it returns the point to store, or null
 * when the fix is dropped. A pause ends the current segment, so the next stored
 * point starts a new one and the gap is never measured as distance.
 */
class RecordingSession {
    private var last: TrackPoint? = null

    var segmentIndex: Int = 0
        private set

    var distanceMeters: Double = 0.0
        private set

    var storedPoints: Int = 0
        private set

    fun onFix(fix: TrackPoint): TrackPoint? {
        if (fix.accuracyMeters > Sampling.MAX_ACCURACY_METERS) return null
        val previous = last
        if (previous != null) {
            val moved = distanceMeters(previous, fix)
            val elapsed = fix.timeMillis - previous.timeMillis
            if (moved < Sampling.MIN_DISTANCE_METERS && elapsed < Sampling.MAX_INTERVAL_MILLIS) return null
            distanceMeters += moved
        }
        last = fix
        storedPoints++
        return fix
    }

    /** Ends the current segment; the next stored point starts a new one. */
    fun pause() {
        if (last != null) {
            last = null
            segmentIndex++
        }
    }
}
