package krtonga.github.io.location_provider_test.location

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.annotation.IntDef
import android.support.v4.content.ContextCompat
import com.google.android.gms.location.*
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import krtonga.github.io.location_provider_test.location.permissions.RxGpsPermissions
import timber.log.Timber
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationCallback
import com.jakewharton.rxrelay2.BehaviorRelay

/**
 * This attempts to put location logic in one place, and returns observables.
 * Ensure you check for permissions, before attempting to request updates.
 *
 * For more info see: https://developer.android.com/training/location/index.html
 */
class LocationTracker {

    /**
     * This can be subscribed to to get the latest location from any provider
     */
    val locationsRelay: BehaviorRelay<Location> = BehaviorRelay.create()
    val latestLocation = locationsRelay.hide()

    companion object {
        const val REQUEST_CHECK_FOR_GPS: Int = 0
        const val DEFAULT_INTERVAL: Long = 5000

        /**
         * This is required before requesting updates.
         */
        fun requestPermissions(activity: Activity, consumer: Consumer<Boolean>) {
            val permissions = RxGpsPermissions(activity, true)
            permissions.permissionsGranted.subscribe(consumer)
        }

        /**
         * This IntDef provides a convenient list of the different Location
         * Providers available.
         */
        @IntDef(GPS_ONLY, NETWORK_ONLY, PASSIVE_ONLY,
                FUSED_HIGH_ACCURACY, FUSED_BALANCED_POWER_ACCURACY, FUSED_LOW_POWER, FUSED_NO_POWER)
        @Retention(AnnotationRetention.SOURCE)
        annotation class Provider

        const val GPS_ONLY = 0L
        const val NETWORK_ONLY = 1L
        const val PASSIVE_ONLY = 2L
        const val FUSED_HIGH_ACCURACY = 3L
        const val FUSED_BALANCED_POWER_ACCURACY = 4L
        const val FUSED_LOW_POWER = 5L
        const val FUSED_NO_POWER = 6L

        fun getProviderName(@Provider provider: Long) : String {
            when (provider) {
                GPS_ONLY -> return "GPS"
                NETWORK_ONLY -> return "NETWORK"
                PASSIVE_ONLY -> return "FUSED"
                FUSED_HIGH_ACCURACY -> return "FUSED - high accuracy"
                FUSED_BALANCED_POWER_ACCURACY -> return "FUSED - balanced power"
                FUSED_LOW_POWER -> return "FUSED - lower power"
                FUSED_NO_POWER -> return "FUSED - no power"
            }
            return ""
        }
    }

    /**
     * Returns map of observables based on user settings. Each observable, when subscribed to,
     * starts location updates.
     */
    fun startMany(context: Context, settings: LocationSettingsInterface)
            : Map<Long, Observable<Location>> {

        val map = HashMap<Long, Observable<Location>>()
        if (settings.isGpsProviderEnabled()) {
            map[GPS_ONLY] = start(context, GPS_ONLY, settings.getInterval())
        }

        if (settings.isNetworkProviderEnabled()) {
            map[NETWORK_ONLY] = start(context, NETWORK_ONLY, settings.getInterval())
        }

        if (settings.isPassiveProviderEnabled()) {
            map[PASSIVE_ONLY] = start(context, PASSIVE_ONLY, settings.getInterval())
        }

        if (settings.isFusedProviderEnabled()) {
            val fused = settings.getFusedProviderPriority()
            map[fused] = start(context, fused, settings.getFusedProviderInterval())
        }
        return map
    }

    /**
     * Returns observable, that when subscribed to starts location updates.
     * Defaults interval to 5 seconds.
     */
    fun start(context: Context, @Provider provider: Long): Observable<Location> {
        return start(context, provider, DEFAULT_INTERVAL)
    }

    /**
     * Returns observable, that when subscribed to starts location updates.
     */
    fun start(context: Context, @Provider provider: Long, interval: Long): Observable<Location> {
        Timber.d("Interval %s: %d", provider, interval)
        when (provider) {
            GPS_ONLY ->
                return wrapLocationManagerUpdates(context, LocationManager.GPS_PROVIDER, interval)
            NETWORK_ONLY ->
                return wrapLocationManagerUpdates(context, LocationManager.NETWORK_PROVIDER, interval)
            PASSIVE_ONLY ->
                return wrapLocationManagerUpdates(context, LocationManager.PASSIVE_PROVIDER, interval)
            FUSED_HIGH_ACCURACY ->
                return wrapFusedUpdates(context, LocationRequest.PRIORITY_HIGH_ACCURACY, interval)
            FUSED_BALANCED_POWER_ACCURACY ->
                return wrapFusedUpdates(context, LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, interval)
            FUSED_LOW_POWER ->
                return wrapFusedUpdates(context, LocationRequest.PRIORITY_LOW_POWER, interval)
            FUSED_NO_POWER ->
                return wrapFusedUpdates(context, LocationRequest.PRIORITY_NO_POWER, interval)
        }
        return wrapFusedUpdates(context, LocationRequest.PRIORITY_HIGH_ACCURACY, interval)
    }

    /**
     * Uses Google Play to provide FUSED location updates at given priority. This
     * is what google recommends.
     */
    private fun startFusedLocationUpdates(context: Context,
                                  priority: Int,
                                  interval: Long,
                                  callback: LocationCallback) {
        Timber.d("Interval Fused: %d", interval)

        val request = LocationRequest.create()
                .setPriority(priority)
                .setInterval(interval)

        val fusedClient: FusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(context)
        if (ContextCompat.checkSelfPermission(context,
                        "android.permission.ACCESS_FINE_LOCATION") == 0) {
            fusedClient.requestLocationUpdates(request, callback, null)
        } else {
            Timber.e("Permissions not yet granted")
        }
    }

    /**
     * Uses lower level LocationManager to provide GPS_ONLY, NETWORK_ONLY, or PASSIVE_ONLY
     * Location updates.
     */
    private fun startOldLocationUpdates(context: Context,
                                provider: String,
                                minTime: Long,
                                minDistance: Float,
                                listener: LocationListener) {

        val manager: LocationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(context,
                        "android.permission.ACCESS_FINE_LOCATION") == 0) {
            manager.requestLocationUpdates(provider, minTime, minDistance, listener)
        } else {
            Timber.e("Permissions not yet granted")
        }
    }

    /**
     * Wraps Google Play FusedLocationProvider update in observer.
     */
    private fun wrapFusedUpdates(context: Context,
                                 priority: Int,
                                 interval: Long)
            : Observable<Location> {

        return Observable.create { emitter ->
            startFusedLocationUpdates(context,
                    priority,
                    interval,
                    object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult?) {
                            for (location in locationResult!!.locations) {
                                emitter.onNext(location)
                                locationsRelay.accept(location)
                            }
                        }
                    })
        }
    }

    /**
     * Wraps LocationManager update in observer.
     */
    private fun wrapLocationManagerUpdates(context: Context,
                                           provider: String,
                                           interval: Long)
            : Observable<Location> {

        return Observable.create { emitter ->
            startOldLocationUpdates(context,
                    provider,
                    interval,
                    0f,
                    object : LocationListener {

                        override fun onLocationChanged(location: Location?) {
                            if (location != null) {
                                emitter.onNext(location)
                                locationsRelay.accept(location)
                            }
                        }

                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                            // TODO: Throw error if necessary
                        }

                        override fun onProviderEnabled(provider: String?) {
                            // TODO: wrapLocationManagerUpdates as necessary
                        }

                        override fun onProviderDisabled(provider: String?) {
                            // TODO: Throw error if necessary
                        }
                    })
        }
    }
}