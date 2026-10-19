package ca.mytrails.app.recording

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import ca.mytrails.app.MainActivity
import ca.mytrails.app.R
import ca.mytrails.app.data.PointEntity
import ca.mytrails.app.data.TrackDatabase
import ca.mytrails.app.data.TrackEntity
import ca.mytrails.app.data.TrackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale

enum class RecordingStatus { IDLE, RECORDING, PAUSED }

/** Live recording state for the UI and notification. */
data class RecordingState(
    val status: RecordingStatus = RecordingStatus.IDLE,
    val elapsedMillis: Long = 0,
    val distanceMeters: Double = 0.0,
    val pointCount: Int = 0,
)

private const val CHANNEL_ID = "recording"
private const val NOTIFICATION_ID = 1
private const val GPS_INTERVAL_MILLIS = 1_000L
private const val TICK_MILLIS = 1_000L

/**
 * Owns track recording so it survives the activity being closed. Started from a
 * user action while the app is visible; never restarts itself (START_NOT_STICKY).
 * Points are written to Room one at a time, in order, through a single writer.
 */
class RecordingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val dao by lazy { TrackDatabase.get(this).trackDao() }
    private val locationManager by lazy { getSystemService(LOCATION_SERVICE) as LocationManager }

    private var session = RecordingSession()
    private var trackId: Long? = null
    private var segment = 0
    private var activeMillis = 0L
    private var resumedAt = 0L
    private var ticker: Job? = null
    private var lastNotificationText: String? = null

    private val listener = LocationListener { location -> onLocation(location) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        scope.launch { for (job in writes) job() }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_PAUSE -> pause()
            ACTION_RESUME -> resume()
            ACTION_STOP -> stop()
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        locationManager.removeUpdates(listener)
        scope.cancel()
        mutableState.value = RecordingState()
        super.onDestroy()
    }

    // The type constant is inlined at compile time and ignored below API 29.
    @SuppressLint("InlinedApi")
    private fun start() {
        if (mutableState.value.status != RecordingStatus.IDLE) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }
        createChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(RecordingState(RecordingStatus.RECORDING)),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )
        session = RecordingSession()
        segment = 0
        activeMillis = 0
        val startMillis = System.currentTimeMillis()
        writes.trySend {
            // Anything still open belongs to a previous run that died.
            dao.markOpenTracksInterrupted()
            trackId =
                dao.insertTrack(
                    TrackEntity(
                        name = defaultName(startMillis),
                        startTimeMillis = startMillis,
                        state = TrackState.RECORDING,
                    ),
                )
        }
        beginListening()
    }

    private fun pause() {
        if (mutableState.value.status != RecordingStatus.RECORDING) return
        activeMillis += SystemClock.elapsedRealtime() - resumedAt
        locationManager.removeUpdates(listener)
        ticker?.cancel()
        session.pause()
        segment = session.segmentIndex
        writes.trySend { trackId?.let { dao.setState(it, TrackState.PAUSED) } }
        publish(RecordingStatus.PAUSED)
    }

    private fun resume() {
        if (mutableState.value.status != RecordingStatus.PAUSED) return
        writes.trySend { trackId?.let { dao.setState(it, TrackState.RECORDING) } }
        beginListening()
    }

    private fun stop() {
        locationManager.removeUpdates(listener)
        ticker?.cancel()
        val endMillis = System.currentTimeMillis()
        if (mutableState.value.status == RecordingStatus.RECORDING) {
            activeMillis += SystemClock.elapsedRealtime() - resumedAt
        }
        val distance = session.distanceMeters
        val duration = activeMillis
        writes.trySend {
            trackId?.let { id ->
                // A track with fewer than 2 points is discarded; the UI confirms first.
                if (dao.pointCount(id) < 2) {
                    dao.deleteTrack(id)
                } else {
                    dao.finish(id, TrackState.COMPLETED, endMillis, distance, duration)
                }
            }
            stopForegroundAndSelf()
        }
    }

    private fun stopForegroundAndSelf() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @SuppressLint("MissingPermission")
    private fun beginListening() {
        resumedAt = SystemClock.elapsedRealtime()
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            GPS_INTERVAL_MILLIS,
            0f,
            listener,
            mainLooper,
        )
        publish(RecordingStatus.RECORDING)
        ticker?.cancel()
        ticker =
            scope.launch {
                while (true) {
                    delay(TICK_MILLIS)
                    publish(RecordingStatus.RECORDING)
                }
            }
    }

    private fun onLocation(location: Location) {
        val stored =
            session.onFix(
                TrackPoint(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timeMillis = location.time,
                    altitude = if (location.hasAltitude()) location.altitude else null,
                    accuracyMeters = if (location.hasAccuracy()) location.accuracy else Float.MAX_VALUE,
                    speedMetersPerSecond = if (location.hasSpeed()) location.speed else null,
                ),
            ) ?: return
        val seg = segment
        val distance = session.distanceMeters
        val duration = activeMillis + SystemClock.elapsedRealtime() - resumedAt
        writes.trySend {
            trackId?.let { id ->
                dao.updateProgress(id, distance, duration)
                dao.insertPoint(
                    PointEntity(
                        trackId = id,
                        segment = seg,
                        latitude = stored.latitude,
                        longitude = stored.longitude,
                        timeMillis = stored.timeMillis,
                        altitude = stored.altitude,
                        accuracyMeters = stored.accuracyMeters,
                        speedMetersPerSecond = stored.speedMetersPerSecond,
                    ),
                )
            }
        }
        publish(RecordingStatus.RECORDING)
    }

    private fun publish(status: RecordingStatus) {
        val elapsed =
            if (status == RecordingStatus.RECORDING) {
                activeMillis + SystemClock.elapsedRealtime() - resumedAt
            } else {
                activeMillis
            }
        val state = RecordingState(status, elapsed, session.distanceMeters, session.storedPoints)
        mutableState.value = state
        // Only touch the notification when its text changes (at most about once a second).
        val text = notificationText(state)
        if (text != lastNotificationText) {
            lastNotificationText = text
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(state))
        }
    }

    private fun createChannel() {
        val channel =
            NotificationChannel(CHANNEL_ID, getString(R.string.recording_channel), NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(state: RecordingState): Notification {
        val open =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE,
            )
        val builder =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle(
                    getString(
                        if (state.status == RecordingStatus.PAUSED) R.string.recording_paused else R.string.recording_active,
                    ),
                ).setContentText(notificationText(state))
                .setContentIntent(open)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
        if (state.status == RecordingStatus.PAUSED) {
            builder.addAction(0, getString(R.string.resume), actionIntent(this, ACTION_RESUME))
        } else {
            builder.addAction(0, getString(R.string.pause), actionIntent(this, ACTION_PAUSE))
        }
        builder.addAction(0, getString(R.string.stop), actionIntent(this, ACTION_STOP))
        return builder.build()
    }

    private fun notificationText(state: RecordingState) =
        "${formatElapsed(state.elapsedMillis)} · ${formatDistance(state.distanceMeters)}"

    private fun defaultName(startMillis: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(startMillis))

    companion object {
        const val ACTION_START = "ca.mytrails.app.action.START"
        const val ACTION_PAUSE = "ca.mytrails.app.action.PAUSE"
        const val ACTION_RESUME = "ca.mytrails.app.action.RESUME"
        const val ACTION_STOP = "ca.mytrails.app.action.STOP"

        private val mutableState = MutableStateFlow(RecordingState())
        val state: StateFlow<RecordingState> = mutableState.asStateFlow()

        private fun actionIntent(
            context: Context,
            action: String,
        ): PendingIntent =
            PendingIntent.getService(
                context,
                action.hashCode(),
                Intent(context, RecordingService::class.java).setAction(action),
                PendingIntent.FLAG_IMMUTABLE,
            )

        /** Sends a command; START uses startForegroundService and must come from a visible app. */
        fun send(
            context: Context,
            action: String,
        ) {
            val intent = Intent(context, RecordingService::class.java).setAction(action)
            if (action == ACTION_START) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }
    }
}

fun formatElapsed(millis: Long): String {
    val total = millis / 1_000
    return String.format(Locale.US, "%d:%02d:%02d", total / 3_600, total / 60 % 60, total % 60)
}

fun formatDistance(meters: Double): String =
    if (meters < 1_000) {
        String.format(Locale.US, "%d m", meters.toInt())
    } else {
        String.format(Locale.US, "%.2f km", meters / 1_000)
    }
