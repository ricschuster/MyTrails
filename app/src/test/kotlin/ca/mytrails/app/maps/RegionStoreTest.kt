package ca.mytrails.app.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class RegionStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun validHeader(): ByteArray {
        val buf = ByteBuffer.allocate(PmtilesHeader.SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("PMTiles".toByteArray(Charsets.US_ASCII))
        buf.put(7, 3)
        intArrayOf(-1_237_200_000, 494_000_000, -1_235_800_000, 495_200_000).forEachIndexed { i, v -> buf.putInt(102 + i * 4, v) }
        return buf.array()
    }

    @Test
    fun missingFolderIsEmpty() {
        assertTrue(RegionStore.scan(File(tmp.root, "nope")).isEmpty())
    }

    @Test
    fun listsPmtilesFilesSortedAndIgnoresOthers() {
        tmp.newFile("b.pmtiles").writeBytes(validHeader())
        tmp.newFile("A.PMTILES").writeBytes(validHeader())
        tmp.newFile("notes.txt").writeText("x")
        val regions = RegionStore.scan(tmp.root)
        assertEquals(listOf("A", "b"), regions.map { it.name })
        assertTrue(regions.all { it.isReadable })
    }

    @Test
    fun corruptFileIsListedAsUnreadable() {
        tmp.newFile("bad.pmtiles").writeText("not a map")
        val region = RegionStore.scan(tmp.root).single()
        assertFalse(region.isReadable)
        assertEquals(9L, region.sizeBytes)
    }
}
