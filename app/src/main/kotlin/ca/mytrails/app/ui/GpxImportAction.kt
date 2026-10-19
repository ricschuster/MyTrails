package ca.mytrails.app.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import ca.mytrails.app.R
import ca.mytrails.app.data.TrackDatabase
import ca.mytrails.app.data.saveImported
import ca.mytrails.app.recording.GpxError
import ca.mytrails.app.recording.GpxImport
import ca.mytrails.app.recording.GpxParseException
import ca.mytrails.app.recording.GpxParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

private sealed interface ImportResult {
    data class Done(
        val ids: List<Long>,
    ) : ImportResult

    data class Failed(
        val messageRes: Int,
    ) : ImportResult
}

/**
 * Returns a function that opens the system file picker and imports the chosen GPX file as tracks.
 * [onImported] gets the ids of the new tracks. Needs no storage permission.
 */
@Composable
fun rememberGpxImporter(onImported: (List<Long>) -> Unit): () -> Unit {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    // Phones often label .gpx files as octet-stream, so the picker accepts any type.
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    when (val result = importGpx(context, uri)) {
                        is ImportResult.Done -> {
                            val count = result.ids.size
                            Toast
                                .makeText(
                                    context,
                                    context.resources.getQuantityString(R.plurals.import_done, count, count),
                                    Toast.LENGTH_LONG,
                                ).show()
                            onImported(result.ids)
                        }
                        is ImportResult.Failed -> Toast.makeText(context, result.messageRes, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    return { launcher.launch(arrayOf("*/*")) }
}

private suspend fun importGpx(
    context: Context,
    uri: Uri,
): ImportResult =
    withContext(Dispatchers.IO) {
        try {
            val parsed =
                checkNotNull(context.contentResolver.openInputStream(uri)) { "No input stream" }
                    .buffered()
                    .use { GpxParser.parse(it) }
            val tracks = GpxImport.toTracks(parsed, displayName(context, uri), System.currentTimeMillis())
            ImportResult.Done(TrackDatabase.get(context).saveImported(tracks))
        } catch (e: GpxParseException) {
            ImportResult.Failed(
                when (e.error) {
                    GpxError.NOT_XML -> R.string.import_not_valid
                    GpxError.NOT_GPX -> R.string.import_not_gpx
                    GpxError.NO_TRACKS -> R.string.import_no_tracks
                    GpxError.TOO_LARGE -> R.string.import_too_large
                },
            )
        } catch (_: IOException) {
            ImportResult.Failed(R.string.import_unreadable)
        } catch (_: IllegalStateException) {
            ImportResult.Failed(R.string.import_unreadable)
        }
    }

private fun displayName(
    context: Context,
    uri: Uri,
): String? =
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }.getOrNull()
