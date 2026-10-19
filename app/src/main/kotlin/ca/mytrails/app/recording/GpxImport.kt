package ca.mytrails.app.recording

/** A parsed track turned into the values the database stores (see docs/design/06_gpx_import.md). */
data class ImportedTrack(
    val name: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long?,
    val distanceMeters: Double,
    val durationMillis: Long,
    val segments: List<List<TrackPoint>>,
)

object GpxImport {
    const val FALLBACK_NAME = "Imported track"

    /**
     * [fileName] is the picked file's display name, used when a track has no name. [nowMillis] is the
     * start time of a track whose points carry no time.
     */
    fun toTracks(
        parsed: List<ParsedTrack>,
        fileName: String?,
        nowMillis: Long,
    ): List<ImportedTrack> {
        val fallback =
            fileName
                ?.substringBeforeLast('.')
                ?.trim()
                ?.ifEmpty { null }
                ?: FALLBACK_NAME
        var unnamed = 0
        val unnamedCount = parsed.count { it.name == null }
        return parsed.map { track ->
            val name =
                track.name ?: run {
                    unnamed++
                    if (unnamedCount > 1) "$fallback $unnamed" else fallback
                }
            val points = track.segments.flatten()
            val firstTime = points.firstOrNull { it.timeMillis > 0 }?.timeMillis
            val lastTime = points.lastOrNull { it.timeMillis > 0 }?.timeMillis
            ImportedTrack(
                name = name,
                startTimeMillis = firstTime ?: nowMillis,
                endTimeMillis = lastTime,
                distanceMeters = track.segments.sumOf { seg -> seg.zipWithNext { a, b -> distanceMeters(a, b) }.sum() },
                durationMillis = if (firstTime != null && lastTime != null) lastTime - firstTime else 0L,
                segments = track.segments,
            )
        }
    }
}
