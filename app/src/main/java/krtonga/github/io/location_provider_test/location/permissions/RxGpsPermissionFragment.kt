package krtonga.github.io.location_provider_test.location.permissions

import android.app.Activity
import android.app.Fragment
import android.content.*
import android.os.Bundle
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import krtonga.github.io.location_provider_test.location.LocationTracker
import timber.log.Timber
import android.location.LocationManager


/**
 * This is a fragment, dynamically added by the RxGpsPermissions class,
 * which checks if GPS is on, prompts the user if GPS is required, and
 * watches for changes in GPS status. , if the user refuses to turn on
 * GPS, It is set up to ask again and again.
 *
 * This was inspired by the RxPermissions library. Please see:
 *  https://github.com/tbruyelle/RxPermissions for more information.
**/

class RxGpsPermissionFragment : Fragment() {

    private val GPS_REQUEST_CODE = 32
    private lateinit var mTurnOnGps: Observable<Boolean>
    private var mSubscriber: ObservableEmitter<Boolean>? = null

    /**
     * This is triggered automatically when user changes whether GPS is on or off.
     **/
    private val mGpsChangeReceiver = object : GpsStatusReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (mSubscriber == null) {
                return
            }
            if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                val locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                mSubscriber?.onNext(isGpsEnabled && isNetworkEnabled)
            }
        }
    }

    /**
     * This returns an observable, which can be used to keep track of GPS changes.
     * On subscribe, a GPS check and prompt (if necessary) is triggered.
     **/
    fun getGpsOnObservable(): Observable<Boolean> {
        mTurnOnGps = Observable.create {
            mSubscriber = it
            Timber.d("\n\nAttempting to turn on GPS...\n\n")
            val request = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(5000)

            val settingsRequest = LocationSettingsRequest.Builder()
                    .addLocationRequest(request)
                    .build()
            val startGpsTask = LocationServices
                    .getSettingsClient(activity.baseContext)
                    .checkLocationSettings(settingsRequest)

            startGpsTask.addOnSuccessListener {
                mSubscriber?.onNext(true)
            }.addOnFailureListener { e ->
                if (e is ResolvableApiException) {
                    try {
                        e.startResolutionForResult(activity, LocationTracker.REQUEST_CHECK_FOR_GPS)
                    } catch (e: IntentSender.SendIntentException) {
                        mSubscriber?.onNext(false)
                    }
                }
            }
        }
        return mTurnOnGps
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        activity.registerReceiver(
                mGpsChangeReceiver,
                IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
        Timber.d("RxGpsPermissionFragment created")
    }

    override fun onDestroy() {
        super.onDestroy()
        activity.unregisterReceiver(mGpsChangeReceiver)
        mSubscriber?.onComplete()
        mSubscriber = null
    }

    /**
     * This will be triggered when been asked to turn on GPS,
     * after subscribing to the Observable.
     **/
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == GPS_REQUEST_CODE) {
            mTurnOnGps
            mSubscriber?.onNext(resultCode == Activity.RESULT_OK)
        }
    }
}