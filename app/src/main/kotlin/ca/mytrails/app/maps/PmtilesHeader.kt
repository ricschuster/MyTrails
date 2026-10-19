package ca.mytrails.app.maps

import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Area a region file covers, in degrees. */
data class RegionBounds(
    val minLon: Double,
    val minLat: Double,
    val maxLon: Double,
    val maxLat: Double,
    val centerLon: Double,
    val centerLat: Double,
    val centerZoom: Int,
)

/** Reads the fixed 127 byte header of a PMTiles v3 archive. Returns null if the file is not one. */
object PmtilesHeader {
    const val SIZE = 127
    private const val MAGIC = "PMTiles"
    private const val VERSION_OFFSET = 7
    private const val BOUNDS_OFFSET = 102
    private const val CENTER_ZOOM_OFFSET = 118
    private const val E7 = 1e7

    fun parse(bytes: ByteArray): RegionBounds? {
        if (bytes.size < SIZE) return null
        if (String(bytes, 0, MAGIC.length, Charsets.US_ASCII) != MAGIC) return null
        if (bytes[VERSION_OFFSET].toInt() != 3) return null
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        fun e7(offset: Int) = buf.getInt(offset) / E7
        val minLon = e7(BOUNDS_OFFSET)
        val minLat = e7(BOUNDS_OFFSET + 4)
        val maxLon = e7(BOUNDS_OFFSET + 8)
        val maxLat = e7(BOUNDS_OFFSET + 12)
        if (minLon >= maxLon || minLat >= maxLat) return null
        return RegionBounds(
            minLon,
            minLat,
            maxLon,
            maxLat,
            centerLon = e7(CENTER_ZOOM_OFFSET + 1),
            centerLat = e7(CENTER_ZOOM_OFFSET + 5),
            centerZoom = buf.get(CENTER_ZOOM_OFFSET).toInt(),
        )
    }

    fun read(file: File): RegionBounds? =
        try {
            file.inputStream().use { input ->
                val header = ByteArray(SIZE)
                var read = 0
                while (read < SIZE) {
                    val n = input.read(header, read, SIZE - read)
                    if (n < 0) break
                    read += n
                }
                parse(header.copyOf(read))
            }
        } catch (_: IOException) {
            null
        }
}
