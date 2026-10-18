package ca.mytrails.app

import android.app.PendingIntent
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineImpl
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult

/**
 * Location engine backed directly by the platform GPS provider.
 *
 * MapLibre's default engines pick the fused provider at balanced accuracy, which
 * can resolve to Wi-Fi or cell positions. A trail app wants satellite fixes, and
 * this also avoids any dependency on Google Play services.
 */
class GpsLocationEngine(context: Context) : LocationEngineImpl<LocationListener> {
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    override fun createListener(
        callback: LocationEngineCallback<LocationEngineResult>,
    ): LocationListener = LocationListener { location -> callback.onSuccess(LocationEngineResult.create(location)) }

    @Suppress("MissingPermission")
    override fun getLastLocation(callback: LocationEngineCallback<LocationEngineResult>) {
        val last: Location? = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (last != null) {
            callback.onSuccess(LocationEngineResult.create(last))
        } else {
            callback.onFailure(Exception("Last location unavailable"))
        }
    }

    @Suppress("MissingPermission")
    override fun requestLocationUpdates(
        request: LocationEngineRequest,
        listener: LocationListener,
        looper: Looper?,
    ) {
        manager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            request.interval,
            request.displacement,
            listener,
            looper,
        )
    }

    override fun requestLocationUpdates(request: LocationEngineRequest, pendingIntent: PendingIntent) {
        // Not used: updates are delivered to the in-process listener only.
    }

    override fun removeLocationUpdates(listener: LocationListener) {
        manager.removeUpdates(listener)
    }

    override fun removeLocationUpdates(pendingIntent: PendingIntent) {
        // Not used.
    }
}
