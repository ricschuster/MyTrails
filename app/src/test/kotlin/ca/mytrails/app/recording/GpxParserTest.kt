package ca.mytrails.app.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GpxParserTest {
    private fun gpx(
        body: String,
        version: String = "1.1",
        ns: String = "http://www.topografix.com/GPX/1/1",
    ) = "<?xml version=\"1.0\"?><gpx version=\"$version\" creator=\"t\" xmlns=\"$ns\">$body</gpx>"

    private fun parse(xml: String) = GpxParser.parse(xml.byteInputStream())

    private fun errorOf(xml: String): GpxError =
        try {
            parse(xml)
            fail("expected GpxParseException")
            error("unreachable")
        } catch (e: GpxParseException) {
            e.error
        }

    private fun trkpt(
        lat: String,
        lon: String,
        extra: String = "",
    ) = "<trkpt lat=\"$lat\" lon=\"$lon\">$extra</trkpt>"

    @Test
    fun readsTrackWithTwoSegments() {
        val tracks =
            parse(
                gpx(
                    "<trk><name>Hike</name>" +
                        "<trkseg>${trkpt("49.1", "-123.1", "<ele>12.5</ele><time>2026-10-19T10:00:00Z</time>")}${trkpt("49.2", "-123.2")}</trkseg>" +
                        "<trkseg>${trkpt("49.3", "-123.3")}${trkpt("49.4", "-123.4")}</trkseg></trk>",
                ),
            )
        val t = tracks.single()
        assertEquals("Hike", t.name)
        assertEquals(2, t.segments.size)
        val p = t.segments[0][0]
        assertEquals(49.1, p.latitude, 1e-9)
        assertEquals(-123.1, p.longitude, 1e-9)
        assertEquals(12.5, p.altitude!!, 1e-9)
        assertEquals(1_792_404_000_000L, p.timeMillis)
        assertNull(t.segments[0][1].altitude)
        assertEquals(0L, t.segments[0][1].timeMillis)
    }

    @Test
    fun routeBecomesOneSegmentTrack() {
        val t = parse(gpx("<rte><name>Plan</name><rtept lat=\"49\" lon=\"-123\"/><rtept lat=\"49.1\" lon=\"-123.1\"/></rte>")).single()
        assertEquals("Plan", t.name)
        assertEquals(1, t.segments.size)
        assertEquals(2, t.segments[0].size)
    }

    @Test
    fun waypointsAreIgnored() {
        val xml = gpx("<wpt lat=\"1\" lon=\"1\"><name>W</name></wpt><trk><trkseg>${trkpt("49", "-123")}${trkpt("49.1", "-123.1")}</trkseg></trk>")
        val t = parse(xml).single()
        assertNull(t.name)
        assertEquals(2, t.segments.single().size)
    }

    @Test
    fun metadataNameIsNotTheTrackName() {
        val xml = gpx("<metadata><name>File</name></metadata><trk><trkseg>${trkpt("49", "-123")}${trkpt("49.1", "-123.1")}</trkseg></trk>")
        assertNull(parse(xml).single().name)
    }

    @Test
    fun acceptsGpx10WithoutNamespaceCheck() {
        val xml = gpx("<trk><trkseg>${trkpt("49", "-123")}${trkpt("49.1", "-123.1")}</trkseg></trk>", "1.0", "http://www.topografix.com/GPX/1/0")
        assertEquals(1, parse(xml).size)
    }

    @Test
    fun extensionsAreIgnored() {
        val pt = trkpt("49", "-123", "<extensions><gpxtpx:hr xmlns:gpxtpx=\"x\">150</gpxtpx:hr><name>zz</name></extensions>")
        val t = parse(gpx("<trk><name>N</name><trkseg>$pt${trkpt("49.1", "-123.1")}</trkseg></trk>")).single()
        assertEquals("N", t.name)
        assertEquals(2, t.segments.single().size)
    }

    @Test
    fun timeWithOffsetIsConverted() {
        assertEquals(GpxParser.parseTime("2026-10-19T10:00:00Z"), GpxParser.parseTime("2026-10-19T03:00:00-07:00"))
        assertEquals(GpxParser.parseTime("2026-10-19T10:00:00Z"), GpxParser.parseTime("2026-10-19T10:00:00"))
        assertEquals(0L, GpxParser.parseTime("yesterday"))
    }

    @Test
    fun badCoordinatesAreSkipped() {
        val pts =
            trkpt("91", "0") + trkpt("0", "181") + trkpt("abc", "1") + "<trkpt lon=\"1\"/>" +
                trkpt("49", "-123") + trkpt("49.1", "-123.1")
        val t = parse(gpx("<trk><trkseg>$pts</trkseg></trk>")).single()
        assertEquals(2, t.segments.single().size)
    }

    @Test
    fun trackWithOneValidPointIsDropped() {
        val xml = gpx("<trk><trkseg>${trkpt("49", "-123")}</trkseg></trk><trk><trkseg>${trkpt("49", "-123")}${trkpt("49.1", "-123.1")}</trkseg></trk>")
        assertEquals(1, parse(xml).size)
    }

    @Test
    fun noTracksIsAnError() {
        assertEquals(GpxError.NO_TRACKS, errorOf(gpx("<wpt lat=\"1\" lon=\"1\"/>")))
    }

    @Test
    fun malformedXmlIsAnError() {
        assertEquals(GpxError.NOT_XML, errorOf("<gpx><trk><trkseg><trkpt lat=\"1\""))
        assertEquals(GpxError.NOT_XML, errorOf(""))
        assertEquals(GpxError.NOT_XML, errorOf("just some text"))
    }

    @Test
    fun otherXmlIsNotGpx() {
        assertEquals(GpxError.NOT_GPX, errorOf("<kml><Placemark/></kml>"))
    }

    @Test
    fun doctypeAndExternalEntitiesAreNotResolved() {
        val xml =
            "<?xml version=\"1.0\"?><!DOCTYPE gpx [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>" +
                gpx("<trk><name>&x;</name><trkseg>${trkpt("49", "-123")}${trkpt("49.1", "-123.1")}</trkseg></trk>").substringAfter("?>")
        try {
            val name = parse(xml).singleOrNull()?.name
            assertTrue("entity must not be read: $name", name.isNullOrEmpty())
        } catch (e: GpxParseException) {
            assertEquals(GpxError.NOT_XML, e.error)
        }
    }

    @Test
    fun overThePointLimitIsRefused() {
        val xml = StringBuilder("<gpx><trk><trkseg>")
        repeat(GpxParser.MAX_POINTS + 1) { xml.append("<trkpt lat=\"49\" lon=\"-123\"/>") }
        xml.append("</trkseg></trk></gpx>")
        assertEquals(GpxError.TOO_LARGE, errorOf(xml.toString()))
    }

    @Test
    fun largeFileParses() {
        val xml = StringBuilder("<gpx><trk><trkseg>")
        repeat(200_000) { i -> xml.append("<trkpt lat=\"${49 + i * 1e-6}\" lon=\"-123\"><ele>10</ele><time>2026-10-19T10:00:00Z</time></trkpt>") }
        xml.append("</trkseg></trk></gpx>")
        assertEquals(200_000, parse(xml.toString()).single().segments.single().size)
    }

    @Test
    fun writerOutputParsesBack() {
        val points =
            listOf(
                listOf(
                    TrackPoint(49.1234567, -123.7654321, 1_792_382_072_812L, 33.3, 5f, 1f),
                    TrackPoint(49.2, -123.8, 1_792_382_082_812L, null, 5f, 1f),
                ),
                listOf(TrackPoint(49.3, -123.9, 1_792_382_092_812L, 40.0, 5f, 1f), TrackPoint(49.4, -124.0, 1_792_382_102_812L, null, 5f, 1f)),
            )
        val out = StringBuilder().also { GpxWriter.write(it, "A & B", 1_792_382_072_812L, points) }.toString()
        val t = parse(out).single()
        assertEquals("A & B", t.name)
        assertEquals(2, t.segments.size)
        assertEquals(49.1234567, t.segments[0][0].latitude, 1e-9)
        assertEquals(33.3, t.segments[0][0].altitude!!, 1e-9)
        assertEquals(1_792_382_072_812L, t.segments[0][0].timeMillis)
        assertEquals(1_792_382_102_812L, t.segments[1][1].timeMillis)
    }
}
