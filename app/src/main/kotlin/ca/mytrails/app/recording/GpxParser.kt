package ca.mytrails.app.recording

import org.xml.sax.Attributes
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.io.IOException
import java.io.InputStream
import java.io.StringReader
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.xml.parsers.SAXParserFactory

/** A track or route read from a GPX file. A time of 0 on a point means the file gave none. */
data class ParsedTrack(
    val name: String?,
    val segments: List<List<TrackPoint>>,
)

enum class GpxError { NOT_XML, NOT_GPX, NO_TRACKS, TOO_LARGE }

class GpxParseException(
    val error: GpxError,
    cause: Throwable? = null,
) : Exception(error.name, cause)

/**
 * Reads tracks (`trk`) and routes (`rte`) from GPX 1.0 or 1.1 (see docs/design/06_gpx_import.md).
 *
 * Plain Kotlin with no Android types. Streams the file with SAX, ignores waypoints and extensions,
 * skips points with bad coordinates, and refuses files over [MAX_POINTS] points.
 */
object GpxParser {
    const val MAX_POINTS = 500_000
    private const val DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl"

    /** Returns the tracks that have at least 2 valid points. Throws [GpxParseException] when none can be read. */
    fun parse(input: InputStream): List<ParsedTrack> {
        val handler = Handler()
        try {
            val factory = SAXParserFactory.newInstance().apply { isNamespaceAware = true }
            // Untrusted input: no doctype and no external entities. Not every parser knows the feature.
            runCatching { factory.setFeature(DISALLOW_DOCTYPE, true) }
            val reader = factory.newSAXParser().xmlReader
            reader.contentHandler = handler
            reader.entityResolver = EntityResolver { _, _ -> InputSource(StringReader("")) }
            reader.parse(InputSource(input))
        } catch (e: SAXException) {
            // The parser wraps whatever the handler throws, so the handler's own verdict wins.
            throw GpxParseException(handler.failure ?: GpxError.NOT_XML, e)
        } catch (e: IOException) {
            throw GpxParseException(GpxError.NOT_XML, e)
        }
        return handler.tracks.ifEmpty { throw GpxParseException(GpxError.NO_TRACKS) }
    }

    internal fun parseTime(text: String): Long {
        val value = text.trim()
        return runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
            .recoverCatching { LocalDateTime.parse(value).toInstant(ZoneOffset.UTC).toEpochMilli() }
            .getOrDefault(0L)
    }

    private class Handler : DefaultHandler() {
        val tracks = mutableListOf<ParsedTrack>()

        /** Set when the handler stops the parse on purpose. */
        var failure: GpxError? = null
            private set

        private val path = ArrayList<String>()
        private val text = StringBuilder()
        private var pointCount = 0

        private var name: String? = null
        private var segments = mutableListOf<List<TrackPoint>>()
        private var segment = mutableListOf<TrackPoint>()

        private var lat = Double.NaN
        private var lon = Double.NaN
        private var ele: Double? = null
        private var time = 0L

        override fun startElement(
            uri: String?,
            localName: String,
            qName: String?,
            attributes: Attributes,
        ) {
            if (path.isEmpty() && localName != "gpx") fail(GpxError.NOT_GPX)
            path.add(localName)
            text.setLength(0)
            when (localName) {
                "trk", "rte" -> if (path.size == 2) startTrack()
                "trkseg" -> segment = mutableListOf()
                "trkpt", "rtept" -> {
                    lat = attributes.getValue("lat")?.trim()?.toDoubleOrNull() ?: Double.NaN
                    lon = attributes.getValue("lon")?.trim()?.toDoubleOrNull() ?: Double.NaN
                    ele = null
                    time = 0L
                }
            }
        }

        override fun characters(
            ch: CharArray,
            start: Int,
            length: Int,
        ) {
            // Only the few small text elements we use are read; a long value is not kept.
            if (text.length < MAX_TEXT) text.append(ch, start, length)
        }

        override fun endElement(
            uri: String?,
            localName: String,
            qName: String?,
        ) {
            val parent = path.getOrNull(path.size - 2)
            when (localName) {
                "name" -> if (parent == "trk" || parent == "rte") name = text.toString().trim().ifEmpty { null }
                "ele" -> if (parent == "trkpt" || parent == "rtept") ele = text.toString().trim().toDoubleOrNull()?.takeIf { it.isFinite() }
                "time" -> if (parent == "trkpt" || parent == "rtept") time = parseTime(text.toString())
                "trkpt", "rtept" -> endPoint()
                "trkseg" -> endSegment()
                "trk" -> endTrack()
                "rte" -> {
                    // A route has no segments: its points form one.
                    endSegment()
                    endTrack()
                }
            }
            path.removeAt(path.size - 1)
            text.setLength(0)
        }

        private fun fail(error: GpxError): Nothing {
            failure = error
            throw SAXException(error.name)
        }

        private fun startTrack() {
            name = null
            segments = mutableListOf()
            segment = mutableListOf()
        }

        private fun endPoint() {
            if (!lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0) return
            if (++pointCount > MAX_POINTS) fail(GpxError.TOO_LARGE)
            segment.add(TrackPoint(lat, lon, time, ele, 0f, null))
        }

        private fun endSegment() {
            if (segment.isNotEmpty()) segments.add(segment)
            segment = mutableListOf()
        }

        private fun endTrack() {
            if (segments.sumOf { it.size } >= 2) tracks.add(ParsedTrack(name, segments))
            segments = mutableListOf()
        }
    }

    private const val MAX_TEXT = 256
}
