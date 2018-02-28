package krtonga.github.io.location_provider_test.location.permissions

import android.app.Activity
import android.app.Fragment
import com.jakewharton.rxrelay2.PublishRelay
import com.tbruyelle.rxpermissions2.RxPermissions
import timber.log.Timber

/**
 * This class checks both for Android permissions for ACCESS_FINE_LOCATION (using
 * the RxPermissions library) and if the GPS is on (using a RxGpsPermissionFragment).
 * It is set to recursively prompt for permission, and recursively prompt to turn on GPS.
 *
 * To watch for permission changes, subscribe to the `permissionsGranted` Observable.
 *
 * To use all Location services in your app, ensure you have added the following
 * permissions in your Android Manifest:
 *
 *  <uses-permission android:name="android.permission.INTERNET" />
 *  <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 *  <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
 *  <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
 **/
class RxGpsPermissions(activity: Activity, gpsRequired: Boolean) {

    /**
     * Relay is like a subject, but can never terminate (no onError or onComplete, only onNext)
     * Hiding a relay makes it read only (and returns an observable)
     *
     * When this relay is subscribed to, it checks for location permissions.
     * It can be used to watch for Permission Status changes.
     */
    private val relay = PublishRelay.create<Boolean>()

    val permissionsGranted = relay
            .hide()
            .doOnSubscribe {
                Timber.d("Subscribed to permission changes...")
                request(activity, gpsRequired)
            }
    /**
     * A retain fragment is added to the activity, and used for GPS permission callbacks.
     */
    private val FRAG_TAG = "GpsFragment"
    private val mGpsPermissionsFragment: RxGpsPermissionFragment by lazy {
        getRxPermissionsFragment(activity)
    }

    private fun request(activity: Activity, gpsRequired: Boolean) {
        RxPermissions(activity)
                .request(android.Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe({ granted ->
                    if (granted) {
                        if (gpsRequired) {
                            val gpsObserver = mGpsPermissionsFragment.getGpsOnObservable()
                            gpsObserver.subscribe {
                                relay.accept(it)
                            }
                        }
                    } else {
                        request(activity, gpsRequired)
                        relay.accept(false)
                    }
                })
    }

    private fun getRxPermissionsFragment(activity: Activity) : RxGpsPermissionFragment {
        var fragment = findRxPermissionsFragment(activity)
        if (fragment == null) {
            fragment = RxGpsPermissionFragment()
            val fragManager = activity.fragmentManager
            fragManager.beginTransaction()
                    .add(fragment, FRAG_TAG)
                    .commitAllowingStateLoss()
            fragManager.executePendingTransactions()
        }
        return fragment as RxGpsPermissionFragment
    }

    private fun findRxPermissionsFragment(activity: Activity) : Fragment? {
        return activity.fragmentManager.findFragmentByTag(FRAG_TAG)
    }
}