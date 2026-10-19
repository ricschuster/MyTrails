package ca.mytrails.app.maps

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.maps.Style
import java.io.File

/** The folder the owner copies region files into with adb (no storage permission needed). */
fun regionsDirectory(context: Context): File =
    File(checkNotNull(context.getExternalFilesDir(null)) { "External files directory unavailable" }, RegionStore.DIRECTORY)
        .also { it.mkdirs() }

/** Everything the map screens need to load the offline style. */
class LoadedStyle(
    val builder: Style.Builder,
    val regions: List<Region>,
)

suspend fun loadStyle(context: Context): LoadedStyle =
    withContext(Dispatchers.IO) {
        val regions = RegionStore.scan(regionsDirectory(context))
        val assets = context.assets
        val base = assets.open("style/base.json").bufferedReader().use { it.readText() }
        val layers = assets.open("style/region-layers.json").bufferedReader().use { it.readText() }
        LoadedStyle(Style.Builder().fromJson(MapStyle.build(base, layers, regions)), regions)
    }
