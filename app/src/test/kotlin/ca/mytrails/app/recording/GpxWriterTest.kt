package ca.mytrails.app.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

class GpxWriterTest {
    // 2026-10-19T03:54:32.812Z
    private val t0 = 1_792_382_072_812L

    private fun point(
        lat: Double = 49.28,
        lon: Double = -123.12,
        time: Long = t0,
        altitude: Double? = null,
    ) = TrackPoint(lat, lon, time, altitude, 5f, 1.2f)

    private fun gpx(
        name: String = "Hike",
        segments: List<List<TrackPoint>>,
    ): String = StringBuilder().also { GpxWriter.write(it, name, t0, segments) }.toString()

    private fun parse(xml: String) =
        DocumentBuilderFactory
            .newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(xml.byteInputStream())
            .documentElement

    private fun Element.all(tag: String) = getElementsByTagNameNS("http://www.topografix.com/GPX/1/1", tag)

    @Test
    fun outputIsWellFormedGpx11() {
        val root = parse(gpx(segments = listOf(listOf(point()))))
        assertEquals("gpx", root.localName)
        assertEquals("1.1", root.getAttribute("version"))
        assertEquals("MyTrails", root.getAttribute("creator"))
        assertEquals("http://www.topografix.com/GPX/1/1", root.namespaceURI)
    }

    @Test
    fun oneTrkSegPerSegment() {
        val root = parse(gpx(segments = listOf(listOf(point(), point()), listOf(point()))))
        assertEquals(1, root.all("trk").length)
        assertEquals(2, root.all("trkseg").length)
        assertEquals(3, root.all("trkpt").length)
    }

    @Test
    fun emptySegmentIsSkipped() {
        val root = parse(gpx(segments = listOf(emptyList(), listOf(point()))))
        assertEquals(1, root.all("trkseg").length)
    }

    @Test
    fun elevationOnlyWhenKnown() {
        val root = parse(gpx(segments = listOf(listOf(point(altitude = 123.456), point()))))
        val ele = root.all("ele")
        assertEquals(1, ele.length)
        assertEquals("123.5", ele.item(0).textContent)
    }

    @Test
    fun timesAreUtcWithMilliseconds() {
        val root = parse(gpx(segments = listOf(listOf(point(), point(time = t0 + 1_000)))))
        assertEquals("2026-10-19T03:54:32.812Z", root.all("time").item(0).textContent)
        assertEquals("2026-10-19T03:54:33.812Z", root.all("time").item(2).textContent)
    }

    @Test
    fun wholeSecondKeepsMilliseconds() {
        val root = parse(gpx(segments = listOf(listOf(point(time = 1_792_382_072_000L)))))
        assertEquals("2026-10-19T03:54:32.000Z", root.all("time").item(1).textContent)
    }

    @Test
    fun coordinatesUseSevenDecimalsAndDotRegardlessOfLocale() {
        val saved = Locale.getDefault()
        Locale.setDefault(Locale.GERMANY)
        try {
            val xml = gpx(segments = listOf(listOf(point(lat = 49.2827, lon = -123.1207))))
            assertTrue(xml.contains("lat=\"49.2827000\""))
            assertTrue(xml.contains("lon=\"-123.1207000\""))
        } finally {
            Locale.setDefault(saved)
        }
    }

    @Test
    fun nameIsEscaped() {
        val root = parse(gpx(name = "Tom & \"Jerry\" <1>", segments = listOf(listOf(point()))))
        assertEquals("Tom & \"Jerry\" <1>", root.all("name").item(0).textContent)
    }

    @Test
    fun controlCharactersInNameAreDropped() {
        val root = parse(gpx(name = "a\u0001b", segments = listOf(listOf(point()))))
        assertEquals("ab", root.all("name").item(0).textContent)
    }

    @Test
    fun accuracyAndSpeedAreNotWritten() {
        val xml = gpx(segments = listOf(listOf(point())))
        assertFalse(xml.contains("speed"))
        assertFalse(xml.contains("extensions"))
    }

    @Test
    fun fileNameKeepsSafeNames() {
        assertEquals("Morning Hike.gpx", GpxWriter.fileName("Morning Hike"))
    }

    @Test
    fun fileNameReplacesUnsafeCharacters() {
        assertEquals("Oct 19, 2026 3-54 AM.gpx", GpxWriter.fileName("Oct 19, 2026 3:54 AM"))
        assertEquals("a-b-c.gpx", GpxWriter.fileName("a/b\\c"))
    }

    @Test
    fun fileNameCollapsesWhitespaceAndFallsBack() {
        assertEquals("a b.gpx", GpxWriter.fileName("  a \t  b  "))
        assertEquals("track.gpx", GpxWriter.fileName("   "))
        assertEquals("track.gpx", GpxWriter.fileName("..."))
    }
}
