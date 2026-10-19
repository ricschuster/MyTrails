package ca.mytrails.app.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RecordingSessionTest {
    // 0.0001 degrees of latitude is about 11.1 m.
    private fun fix(
        northSteps: Int = 0,
        timeSeconds: Long = 0,
        accuracy: Float = 5f,
    ) = TrackPoint(49.0 + northSteps * 0.0001, -123.0, timeSeconds * 1_000, null, accuracy, null)

    @Test
    fun firstGoodFixIsStored() {
        assertNotNull(RecordingSession().onFix(fix()))
    }

    @Test
    fun poorAccuracyIsDropped() {
        val session = RecordingSession()
        assertNull(session.onFix(fix(accuracy = 31f)))
        assertEquals(0, session.storedPoints)
    }

    @Test
    fun accuracyAtLimitIsKept() {
        assertNotNull(RecordingSession().onFix(fix(accuracy = 30f)))
    }

    @Test
    fun closeAndRecentFixIsDropped() {
        val session = RecordingSession()
        session.onFix(fix())
        // About 1 m away, 1 s later.
        val near = TrackPoint(49.00001, -123.0, 1_000, null, 5f, null)
        assertNull(session.onFix(near))
        assertEquals(1, session.storedPoints)
    }

    @Test
    fun movingFarEnoughIsStored() {
        val session = RecordingSession()
        session.onFix(fix())
        assertNotNull(session.onFix(fix(northSteps = 1, timeSeconds = 1)))
    }

    @Test
    fun stationaryFixIsStoredAfterTenSeconds() {
        val session = RecordingSession()
        session.onFix(fix())
        assertNull(session.onFix(fix(timeSeconds = 9)))
        assertNotNull(session.onFix(fix(timeSeconds = 10)))
    }

    @Test
    fun droppedFixDoesNotMoveTheReference() {
        val session = RecordingSession()
        session.onFix(fix())
        // Each hop is about 2 m; the reference stays at the origin until 5 m accumulates.
        assertNull(session.onFix(TrackPoint(49.00002, -123.0, 1_000, null, 5f, null)))
        assertNull(session.onFix(TrackPoint(49.00004, -123.0, 2_000, null, 5f, null)))
        assertNotNull(session.onFix(TrackPoint(49.00006, -123.0, 3_000, null, 5f, null)))
    }

    @Test
    fun distanceAccumulatesOnStoredPointsOnly() {
        val session = RecordingSession()
        session.onFix(fix())
        session.onFix(fix(northSteps = 1, timeSeconds = 1))
        session.onFix(fix(northSteps = 2, timeSeconds = 2))
        assertEquals(22.2, session.distanceMeters, 0.5)
    }

    @Test
    fun pauseStartsNewSegmentWithoutCountingTheGap() {
        val session = RecordingSession()
        session.onFix(fix())
        session.onFix(fix(northSteps = 1, timeSeconds = 1))
        val before = session.distanceMeters
        session.pause()
        assertEquals(1, session.segmentIndex)
        // Resume far away; the jump must not be added.
        val far = TrackPoint(50.0, -123.0, 600_000, null, 5f, null)
        assertNotNull(session.onFix(far))
        assertEquals(before, session.distanceMeters, 1e-9)
        assertEquals(3, session.storedPoints)
    }

    @Test
    fun repeatedPauseWithoutPointsDoesNotAddEmptySegments() {
        val session = RecordingSession()
        session.onFix(fix())
        session.pause()
        session.pause()
        assertEquals(1, session.segmentIndex)
    }
}
