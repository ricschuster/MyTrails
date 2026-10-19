package ca.mytrails.app.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import ca.mytrails.app.R
import ca.mytrails.app.data.TrackDatabase
import ca.mytrails.app.data.TrackEntity
import ca.mytrails.app.data.TrackState
import ca.mytrails.app.recording.GpxWriter
import ca.mytrails.app.recording.TrackPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val GPX_MIME_TYPE = "application/gpx+xml"

/** A track that is still being recorded cannot be exported. */
fun canExport(track: TrackEntity) = track.state == TrackState.COMPLETED || track.state == TrackState.INTERRUPTED

/**
 * Returns a function that asks for a file location with the system picker and writes the
 * track there as GPX. Needs no storage permission.
 */
@Composable
fun rememberGpxExporter(): (TrackEntity) -> Unit {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<TrackEntity?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(GPX_MIME_TYPE)) { uri ->
            val track = pending
            pending = null
            if (uri != null && track != null) {
                scope.launch {
                    val ok = writeGpx(context, track, uri)
                    Toast.makeText(context, if (ok) R.string.export_done else R.string.export_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    return { track ->
        pending = track
        launcher.launch(GpxWriter.fileName(track.name))
    }
}

private suspend fun writeGpx(
    context: Context,
    track: TrackEntity,
    uri: Uri,
): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val points = TrackDatabase.get(context).trackDao().getPoints(track.id)
            val segments =
                points
                    .groupBy { it.segment }
                    .toSortedMap()
                    .values
                    .map { segment ->
                        segment.map {
                            TrackPoint(
                                it.latitude,
                                it.longitude,
                                it.timeMillis,
                                it.altitude,
                                it.accuracyMeters,
                                it.speedMetersPerSecond,
                            )
                        }
                    }
            val stream = checkNotNull(context.contentResolver.openOutputStream(uri)) { "No output stream" }
            stream.bufferedWriter(Charsets.UTF_8).use { GpxWriter.write(it, track.name, track.startTimeMillis, segments) }
        }.isSuccess
    }
