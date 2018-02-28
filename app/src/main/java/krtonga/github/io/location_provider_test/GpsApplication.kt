package krtonga.github.io.location_provider_test

import android.app.Application
import com.crashlytics.android.Crashlytics
import com.mapbox.mapboxsdk.Mapbox
import io.fabric.sdk.android.Fabric
import timber.log.Timber



class GpsApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Mapbox.getInstance(applicationContext, getString(R.string.mapbox_token))

        Fabric.with(this, Crashlytics())
    }
}