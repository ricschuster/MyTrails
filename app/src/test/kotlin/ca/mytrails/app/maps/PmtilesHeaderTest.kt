package ca.mytrails.app.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PmtilesHeaderTest {
    private fun header(
        magic: String = "PMTiles",
        version: Int = 3,
        bounds: IntArray = intArrayOf(-1_237_200_000, 494_000_000, -1_235_800_000, 495_200_000),
        size: Int = PmtilesHeader.SIZE,
    ): ByteArray {
        val buf = ByteBuffer.allocate(PmtilesHeader.SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(magic.toByteArray(Charsets.US_ASCII))
        buf.put(7, version.toByte())
        bounds.forEachIndexed { i, v -> buf.putInt(102 + i * 4, v) }
        buf.put(118, 11)
        buf.putInt(119, -1_236_500_000)
        buf.putInt(123, 494_600_000)
        return buf.array().copyOf(size)
    }

    @Test
    fun readsBoundsAndCenter() {
        val b = PmtilesHeader.parse(header())
        assertNotNull(b)
        assertEquals(-123.72, b!!.minLon, 1e-9)
        assertEquals(49.40, b.minLat, 1e-9)
        assertEquals(-123.58, b.maxLon, 1e-9)
        assertEquals(49.52, b.maxLat, 1e-9)
        assertEquals(-123.65, b.centerLon, 1e-9)
        assertEquals(49.46, b.centerLat, 1e-9)
        assertEquals(11, b.centerZoom)
    }

    @Test
    fun rejectsWrongMagic() {
        assertNull(PmtilesHeader.parse(header(magic = "NotTiles")))
    }

    @Test
    fun rejectsOtherVersions() {
        assertNull(PmtilesHeader.parse(header(version = 2)))
    }

    @Test
    fun rejectsShortFiles() {
        assertNull(PmtilesHeader.parse(header(size = 50)))
    }

    @Test
    fun rejectsEmptyBounds() {
        assertNull(PmtilesHeader.parse(header(bounds = intArrayOf(0, 0, 0, 0))))
    }
}
