package ca.mytrails.app.ui

import android.text.format.Formatter
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import ca.mytrails.app.R
import ca.mytrails.app.maps.Region
import ca.mytrails.app.maps.RegionStore
import ca.mytrails.app.maps.regionsDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/** Lists the installed offline map regions. Regions are added by copying files with adb. */
@Composable
fun RegionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var regions by remember { mutableStateOf<List<Region>?>(null) }
    var deleting by remember { mutableStateOf<Region?>(null) }

    suspend fun refresh() {
        regions = withContext(Dispatchers.IO) { RegionStore.scan(regionsDirectory(context)) }
    }
    LaunchedEffect(Unit) { refresh() }

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                Text(stringResource(R.string.regions), style = MaterialTheme.typography.titleLarge)
            }
            val list = regions
            if (list != null && list.isEmpty()) {
                Text(stringResource(R.string.no_regions), Modifier.padding(16.dp))
            }
            LazyColumn {
                items(list.orEmpty(), key = { it.file.absolutePath }) { region ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(region.name, style = MaterialTheme.typography.titleMedium)
                            val size = Formatter.formatShortFileSize(context, region.sizeBytes)
                            Text(
                                if (region.bounds == null) {
                                    stringResource(R.string.region_unreadable, size)
                                } else {
                                    "$size  ·  ${boundsText(region)}"
                                },
                            )
                        }
                        TextButton(onClick = { deleting = region }) { Text(stringResource(R.string.delete)) }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    deleting?.let { region ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.delete_region_title, region.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleting = null
                        scope.launch {
                            withContext(Dispatchers.IO) { region.file.delete() }
                            refresh()
                        }
                    },
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private fun boundsText(region: Region): String {
    val b = checkNotNull(region.bounds)
    return String.format(Locale.US, "%.2f, %.2f to %.2f, %.2f", b.minLat, b.minLon, b.maxLat, b.maxLon)
}
