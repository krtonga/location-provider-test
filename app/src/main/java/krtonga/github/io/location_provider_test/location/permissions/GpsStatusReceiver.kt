package krtonga.github.io.location_provider_test.location.permissions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * This is used to catch changes to the GPS status. It can be overwritten in
 * app code to handle your specific use case. For an example, see the
 * RxGpsPermissionsFragment.
 *
 * To make everything work, don't forget to add the following to the android manifest:
 *
 *  <uses-permission android:name="android.permission.PROVIDERS_CHANGED" />
 *  <application>
 *      ...
 *      <receiver android:name=".GpsStatusReceiver">
 *          <intent-filter>
 *              <action android:name="android.location.PROVIDERS_CHANGED" />
 *              <category android:name="android.intent.category.DEFAULT" />
 *          </intent-filter>
 *      </receiver>
 *  </application>
 *
 *
 **/
open class GpsStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "android.location.PROVIDERS_CHANGED") {
            Timber.d("Provider Status Changed!")
        }
    }

}