package ca.mytrails.app.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MapStyleTest {
    private val assets = File("src/main/assets/style")
    private val base = File(assets, "base.json").readText()
    private val layers = File(assets, "region-layers.json").readText()

    private val bounds = RegionBounds(-123.72, 49.40, -123.58, 49.52, -123.65, 49.46, 11)

    private fun region(
        path: String,
        readable: Boolean = true,
    ) = Region(File(path), 1L, if (readable) bounds else null)

    private fun count(
        text: String,
        needle: String,
    ) = text.windowed(needle.length).count { it == needle }

    @Test
    fun noRegionsGivesBackgroundOnly() {
        val style = MapStyle.build(base, layers, emptyList())
        assertTrue(style.contains("\"background\""))
        assertFalse(style.contains("pmtiles://"))
        assertFalse(style.contains("{{"))
    }

    @Test
    fun oneSourceAndLayerSetPerReadableRegion() {
        val style = MapStyle.build(base, layers, listOf(region("/a/one.pmtiles"), region("/a/two.pmtiles")))
        assertTrue(style.contains("pmtiles://file:///a/one.pmtiles"))
        assertTrue(style.contains("pmtiles://file:///a/two.pmtiles"))
        assertEquals(1, count(style, "\"id\": \"trails0\""))
        assertEquals(1, count(style, "\"id\": \"trails1\""))
        assertFalse(style.contains("{{"))
    }

    @Test
    fun unreadableRegionIsSkipped() {
        val style = MapStyle.build(base, layers, listOf(region("/a/bad.pmtiles", readable = false)))
        assertFalse(style.contains("bad.pmtiles"))
    }

    @Test
    fun pathIsEscapedInJson() {
        val style = MapStyle.build(base, layers, listOf(region("/a/\"q\".pmtiles")))
        assertTrue(style.contains("/a/\\\"q\\\".pmtiles"))
    }

    @Test
    fun everyLayerIdIsUniquePerRegion() {
        val style = MapStyle.build(base, layers, listOf(region("/a.pmtiles"), region("/b.pmtiles")))
        val ids = Regex("\"id\": \"([^\"]+)\"").findAll(style).map { it.groupValues[1] }.toList()
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun terrainFileAddsHillshadeUnderTheTrails() {
        val style = MapStyle.build(base, layers, listOf(region("/a/one.pmtiles"), region("/a/one-terrain.pmtiles")))
        assertTrue(style.contains("\"type\": \"raster-dem\""))
        assertTrue(style.contains("pmtiles://file:///a/one-terrain.pmtiles"))
        assertEquals(1, count(style, "\"id\": \"hillshade0\""))
        assertEquals(1, count(style, "\"id\": \"trails0\""))
        assertTrue(style.indexOf("hillshade0") < style.indexOf("\"id\": \"landuse0\""))
        assertFalse(style.contains("{{"))
    }

    @Test
    fun terrainFileIsNotDrawnAsTrailRegion() {
        val style = MapStyle.build(base, layers, listOf(region("/a/one-terrain.pmtiles")))
        assertFalse(style.contains("\"type\": \"vector\""))
        assertFalse(style.contains("landuse"))
    }
}
