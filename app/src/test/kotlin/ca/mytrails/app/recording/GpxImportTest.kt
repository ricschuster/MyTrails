package ca.mytrails.app.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GpxImportTest {
    private fun p(
        lat: Double,
        time: Long = 0,
    ) = TrackPoint(lat, -123.0, time, null, 0f, null)

    private val now = 5_000L

    @Test
    fun usesTrackNameThenFileNameThenFallback() {
        val seg = listOf(listOf(p(49.0), p(49.1)))
        assertEquals("Named", GpxImport.toTracks(listOf(ParsedTrack("Named", seg)), "file.gpx", now).single().name)
        assertEquals("file", GpxImport.toTracks(listOf(ParsedTrack(null, seg)), "file.gpx", now).single().name)
        assertEquals("Imported track", GpxImport.toTracks(listOf(ParsedTrack(null, seg)), null, now).single().name)
    }

    @Test
    fun severalUnnamedTracksAreNumbered() {
        val seg = listOf(listOf(p(49.0), p(49.1)))
        val names = GpxImport.toTracks(listOf(ParsedTrack(null, seg), ParsedTrack("X", seg), ParsedTrack(null, seg)), "ride.gpx", now).map { it.name }
        assertEquals(listOf("ride 1", "X", "ride 2"), names)
    }

    @Test
    fun timesGiveStartEndAndDuration() {
        val t = GpxImport.toTracks(listOf(ParsedTrack("T", listOf(listOf(p(49.0, 1_000), p(49.1, 61_000))))), null, now).single()
        assertEquals(1_000L, t.startTimeMillis)
        assertEquals(61_000L, t.endTimeMillis)
        assertEquals(60_000L, t.durationMillis)
    }

    @Test
    fun missingTimesUseImportTimeAndZeroDuration() {
        val t = GpxImport.toTracks(listOf(ParsedTrack("T", listOf(listOf(p(49.0), p(49.1))))), null, now).single()
        assertEquals(now, t.startTimeMillis)
        assertNull(t.endTimeMillis)
        assertEquals(0L, t.durationMillis)
    }

    @Test
    fun distanceIsSummedWithinSegmentsOnly() {
        val oneSegment = GpxImport.toTracks(listOf(ParsedTrack("T", listOf(listOf(p(49.0), p(49.1), p(49.2))))), null, now).single()
        val twoSegments = GpxImport.toTracks(listOf(ParsedTrack("T", listOf(listOf(p(49.0), p(49.1)), listOf(p(49.1), p(49.2))))), null, now).single()
        assertEquals(oneSegment.distanceMeters, twoSegments.distanceMeters, 1e-6)
        assertTrue(oneSegment.distanceMeters in 22_000.0..22_500.0)
        // A gap between segments is not counted.
        val gap = GpxImport.toTracks(listOf(ParsedTrack("T", listOf(listOf(p(49.0), p(49.1)), listOf(p(50.0), p(50.1))))), null, now).single()
        assertTrue(gap.distanceMeters < 23_000.0)
    }
}
