package ca.mytrails.app.recording

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoTest {
    @Test
    fun samePointIsZero() {
        assertEquals(0.0, distanceMeters(49.28, -123.12, 49.28, -123.12), 0.0)
    }

    @Test
    fun oneDegreeOfLatitudeIsAbout111Km() {
        assertEquals(111_195.0, distanceMeters(0.0, 0.0, 1.0, 0.0), 50.0)
    }

    @Test
    fun vancouverToSeattleIsAbout195Km() {
        assertEquals(195_000.0, distanceMeters(49.2827, -123.1207, 47.6062, -122.3321), 2_000.0)
    }

    @Test
    fun isSymmetric() {
        val ab = distanceMeters(49.0, -123.0, 49.1, -123.2)
        val ba = distanceMeters(49.1, -123.2, 49.0, -123.0)
        assertEquals(ab, ba, 1e-6)
    }
}
