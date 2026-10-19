package ca.mytrails.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.mytrails.app.R
import ca.mytrails.app.data.TrackDatabase
import ca.mytrails.app.data.TrackEntity
import ca.mytrails.app.data.TrackState
import ca.mytrails.app.recording.formatDistance
import ca.mytrails.app.recording.formatElapsed
import kotlinx.coroutines.launch

@Composable
fun TracksScreen(
    onOpen: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val dao = TrackDatabase.get(LocalContext.current).trackDao()
    val tracks by remember { dao.observeTracks() }.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var renaming by remember { mutableStateOf<TrackEntity?>(null) }
    var deleting by remember { mutableStateOf<TrackEntity?>(null) }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                Text(stringResource(R.string.tracks), style = MaterialTheme.typography.titleLarge)
            }
            if (tracks.isEmpty()) {
                Text(
                    stringResource(R.string.no_tracks),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(tracks, key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        onOpen = { onOpen(track.id) },
                        onRename = { renaming = track },
                        onDelete = { deleting = track },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    renaming?.let { track ->
        var name by remember(track.id) { mutableStateOf(track.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text(stringResource(R.string.rename)) },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        scope.launch { dao.rename(track.id, name.trim()) }
                        renaming = null
                    },
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { renaming = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    deleting?.let { track ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(track.name) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { dao.deleteTrack(track.id) }
                        deleting = null
                    },
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun TrackRow(
    track: TrackEntity,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(track.name, style = MaterialTheme.typography.titleMedium)
            val stats = "${formatDistance(track.distanceMeters)}  ·  ${formatElapsed(track.durationMillis)}"
            Text(stats, style = MaterialTheme.typography.bodyMedium)
            statusLabel(track.state)?.let {
                Text(stringResource(it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
            }
        }
        TextButton(onClick = onRename) { Text(stringResource(R.string.rename)) }
        TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
    }
}

private fun statusLabel(state: String): Int? =
    when (state) {
        TrackState.INTERRUPTED -> R.string.state_interrupted
        TrackState.RECORDING, TrackState.PAUSED -> R.string.state_recording
        else -> null
    }
