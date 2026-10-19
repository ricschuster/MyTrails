package ca.mytrails.app.recording

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Writes a recorded track as GPX 1.1 (see docs/design/03_gpx_export.md).
 *
 * Plain Kotlin with no Android types. One `trkseg` per segment, so a pause is a
 * gap in other apps; empty segments are skipped.
 */
object GpxWriter {
    private val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)

    fun write(
        out: Appendable,
        name: String,
        startMillis: Long,
        segments: List<List<TrackPoint>>,
    ) {
        val title = escape(name)
        out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        out.append("<gpx version=\"1.1\" creator=\"MyTrails\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        out.append("  <metadata>\n")
        out.append("    <name>$title</name>\n")
        out.append("    <time>${formatTime(startMillis)}</time>\n")
        out.append("  </metadata>\n")
        out.append("  <trk>\n")
        out.append("    <name>$title</name>\n")
        for (segment in segments.filter { it.isNotEmpty() }) {
            out.append("    <trkseg>\n")
            for (p in segment) {
                out.append("      <trkpt lat=\"${coordinate(p.latitude)}\" lon=\"${coordinate(p.longitude)}\">\n")
                p.altitude?.let { out.append("        <ele>${String.format(Locale.ROOT, "%.1f", it)}</ele>\n") }
                out.append("        <time>${formatTime(p.timeMillis)}</time>\n")
                out.append("      </trkpt>\n")
            }
            out.append("    </trkseg>\n")
        }
        out.append("  </trk>\n")
        out.append("</gpx>\n")
    }

    /** A file name for the track: unsafe characters become `-`, with a fallback of `track.gpx`. */
    fun fileName(trackName: String): String {
        val cleaned =
            trackName
                .replace(Regex("\\s+"), " ")
                .replace(Regex("[/\\\\:*?\"<>|\\p{Cntrl}]"), "-")
                .trim()
                .trim('.', '-', ' ')
        return (cleaned.ifEmpty { "track" }) + ".gpx"
    }

    private fun coordinate(value: Double) = String.format(Locale.ROOT, "%.7f", value)

    private fun formatTime(millis: Long) = timeFormat.format(java.time.Instant.ofEpochMilli(millis))

    private fun escape(text: String): String =
        buildString {
            for (c in text) {
                when {
                    c == '&' -> append("&amp;")
                    c == '<' -> append("&lt;")
                    c == '>' -> append("&gt;")
                    c == '"' -> append("&quot;")
                    c == '\'' -> append("&apos;")
                    // Control characters other than tab, newline and carriage return are not valid XML.
                    c < ' ' && c != '\t' && c != '\n' && c != '\r' -> Unit
                    else -> append(c)
                }
            }
        }
}
