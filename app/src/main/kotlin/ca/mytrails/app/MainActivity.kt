package ca.mytrails.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.mytrails.app.recording.RecordingService
import ca.mytrails.app.recording.RecordingStatus
import ca.mytrails.app.recording.formatDistance
import ca.mytrails.app.recording.formatElapsed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.engine.LocationEngineProxy
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

// Placeholder online style for the first milestone. Offline tiles are a later ADR.
private const val STYLE_URL = "https://demotiles.maplibre.org/style.json"
private const val TRACKING_ZOOM = 12.0

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        setContent { HelloMap() }
    }
}

@Composable
private fun HelloMap() {
    val context = LocalContext.current
    var hasLocation by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { result -> hasLocation = result[Manifest.permission.ACCESS_FINE_LOCATION] == true }

    LaunchedEffect(Unit) {
        if (!hasLocation) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    val mapView = rememberMapView()
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { m ->
            m.cameraPosition =
                CameraPosition
                    .Builder()
                    .target(LatLng(49.28, -123.12))
                    .zoom(4.0)
                    .build()
            m.setStyle(STYLE_URL) { s ->
                map = m
                style = s
            }
        }
    }

    LaunchedEffect(map, style, hasLocation) {
        val m = map
        val s = style
        if (m != null && s != null && hasLocation) enableLocation(m, s, mapView)
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        RecordingControls(
            hasLocation = hasLocation,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun RecordingControls(
    hasLocation: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by RecordingService.state.collectAsStateWithLifecycle()
    var confirmDiscard by remember { mutableStateOf(false) }

    // Notification permission is asked on the first record tap; recording works without it.
    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            RecordingService.send(context, RecordingService.ACTION_START)
        }

    Surface(
        modifier = modifier.navigationBarsPadding().padding(16.dp),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.status == RecordingStatus.IDLE) {
                Button(
                    enabled = hasLocation,
                    onClick = {
                        val needsAsk =
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                                PackageManager.PERMISSION_GRANTED
                        if (needsAsk) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            RecordingService.send(context, RecordingService.ACTION_START)
                        }
                    },
                ) { Text(stringResource(R.string.record)) }
            } else {
                Text("${formatElapsed(state.elapsedMillis)}  \u00b7  ${formatDistance(state.distanceMeters)}")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            val action =
                                if (state.status == RecordingStatus.PAUSED) {
                                    RecordingService.ACTION_RESUME
                                } else {
                                    RecordingService.ACTION_PAUSE
                                }
                            RecordingService.send(context, action)
                        },
                    ) {
                        Text(
                            stringResource(
                                if (state.status == RecordingStatus.PAUSED) R.string.resume else R.string.pause,
                            ),
                        )
                    }
                    Button(
                        onClick = {
                            if (state.pointCount < 2) {
                                confirmDiscard = true
                            } else {
                                RecordingService.send(context, RecordingService.ACTION_STOP)
                            }
                        },
                    ) { Text(stringResource(R.string.stop)) }
                }
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.discard_title)) },
            text = { Text(stringResource(R.string.discard_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDiscard = false
                        RecordingService.send(context, RecordingService.ACTION_STOP)
                    },
                ) { Text(stringResource(R.string.discard)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@SuppressLint("MissingPermission")
private fun enableLocation(
    map: MapLibreMap,
    style: Style,
    mapView: MapView,
) {
    map.locationComponent.apply {
        activateLocationComponent(
            LocationComponentActivationOptions
                .builder(mapView.context, style)
                .locationEngine(LocationEngineProxy(GpsLocationEngine(mapView.context)))
                .locationEngineRequest(
                    LocationEngineRequest
                        .Builder(1_000L)
                        .setFastestInterval(500L)
                        .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                        .build(),
                ).build(),
        )
        isLocationComponentEnabled = true
        cameraMode = CameraMode.TRACKING
        renderMode = RenderMode.COMPASS
    }
    map.locationComponent.zoomWhileTracking(TRACKING_ZOOM)
}

@Composable
private fun rememberMapView(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> Unit
                }
            }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }
    return mapView
}
