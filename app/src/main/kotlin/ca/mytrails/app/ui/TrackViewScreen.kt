package ca.mytrails.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ca.mytrails.app.R
import ca.mytrails.app.STYLE_URL
import ca.mytrails.app.data.TrackDatabase
import ca.mytrails.app.data.TrackEntity
import ca.mytrails.app.recording.formatDistance
import ca.mytrails.app.recording.formatElapsed
import ca.mytrails.app.rememberMapView
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.MultiLineString
import org.maplibre.geojson.Point

private const val SOURCE_ID = "track"
private const val LAYER_ID = "track-line"
private const val FIT_PADDING_PX = 120
private const val LINE_WIDTH = 5f

/** Shows one saved track on the map. Each pause starts a new segment, drawn as a separate line. */
@Composable
fun TrackViewScreen(
    trackId: Long,
    onBack: () -> Unit,
) {
    val dao = TrackDatabase.get(LocalContext.current).trackDao()
    var track by remember { mutableStateOf<TrackEntity?>(null) }
    val mapView = rememberMapView()
    val exportGpx = rememberGpxExporter()
    val lineColor = MaterialTheme.colorScheme.primary.toArgb()

    LaunchedEffect(trackId) {
        track = dao.getTrack(trackId)
        val segments =
            dao
                .getPoints(trackId)
                .groupBy { it.segment }
                .values
                .map { points -> points.map { Point.fromLngLat(it.longitude, it.latitude) } }
        mapView.getMapAsync { map ->
            map.setStyle(STYLE_URL) { style ->
                // A segment needs 2 points to be a line.
                val lines = segments.filter { it.size >= 2 }.map { LineString.fromLngLats(it) }
                style.addSource(GeoJsonSource(SOURCE_ID, Feature.fromGeometry(MultiLineString.fromLineStrings(lines))))
                style.addLayer(
                    LineLayer(LAYER_ID, SOURCE_ID).withProperties(
                        PropertyFactory.lineColor(lineColor),
                        PropertyFactory.lineWidth(LINE_WIDTH),
                        PropertyFactory.lineCap("round"),
                        PropertyFactory.lineJoin("round"),
                    ),
                )
                val all = segments.flatten()
                if (all.size >= 2) {
                    val bounds =
                        LatLngBounds
                            .Builder()
                            .includes(all.map { LatLng(it.latitude(), it.longitude()) })
                            .build()
                    map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, FIT_PADDING_PX))
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        Surface(
            modifier = Modifier.safeDrawingPadding().padding(8.dp).align(Alignment.TopStart),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
        ) {
            Box(Modifier.padding(horizontal = 4.dp)) {
                Column(Modifier.padding(8.dp)) {
                    Row {
                        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                        track?.let {
                            TextButton(enabled = canExport(it), onClick = { exportGpx(it) }) {
                                Text(stringResource(R.string.export))
                            }
                        }
                    }
                    track?.let {
                        Text(it.name, style = MaterialTheme.typography.titleMedium)
                        Text("${formatDistance(it.distanceMeters)}  ·  ${formatElapsed(it.durationMillis)}")
                    }
                }
            }
        }
    }
}
