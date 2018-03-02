package krtonga.github.io.location_provider_test

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.location.Location
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.annotation.ColorRes
import android.support.design.widget.FloatingActionButton
import android.support.v4.content.ContextCompat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.mapbox.mapboxsdk.annotations.Icon
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.activity_map.*
import krtonga.github.io.location_provider_test.location.LocationTracker
import krtonga.github.io.location_provider_test.settings.SettingsHelper
import timber.log.Timber


class MapActivity : AbstractMapActivity() {

    val locationTracker = LocationTracker()

    val settingsHelper by lazy {  SettingsHelper(
            PreferenceManager.getDefaultSharedPreferences(this), resources) }

    var disposables = CompositeDisposable()

    lateinit var keyLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_map)
        super.onCreate(savedInstanceState) // Please call after setting layout

        keyLayout = findViewById(R.id.key)
        setSupportActionBar(toolbar)

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener { _ ->
            if (keyLayout.visibility == View.GONE) {
                keyLayout.visibility = View.VISIBLE
            } else {
                keyLayout.visibility = View.GONE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_map, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> settingsHelper.startSettingsActivity(this)
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }

    override fun getMapboxMapView(): MapView {
        return findViewById(R.id.mapView)
    }

    override fun onMapReady(map: MapboxMap) {
        Timber.d("Map ready!")
        map.setStyle(settingsHelper.mapboxStyle())
        LocationTracker.requestPermissions(this, onPermissionsGranted)
    }

    private val onPermissionsGranted: Consumer<Boolean> = Consumer { granted ->
        if (granted) {
            // Clean up leftovers //TODO move map initialization
            keyLayout.removeAllViews()
            disposables.dispose()
            disposables = CompositeDisposable()



            for (track in locationTracker.startMany(applicationContext, settingsHelper)) {
                watch(track.key, track.value)
            }
//
//            val interval = updateInterval()
//
//
//
//            if (gpsProviderOn()) {
//                val gpsObservable = locationTracker.start(applicationContext, GPS_ONLY, interval)
//                watch(gpsObservable, LocationManager.GPS_PROVIDER)
//            }
//
//            if (networkProviderOn()) {
//                val networkObservable = locationTracker.start(applicationContext, NETWORK_ONLY, interval)
//                watch(networkObservable, LocationManager.NETWORK_PROVIDER)
//            }
//
//            if (passiveProviderOn()) {
//                val passiveObservable = locationTracker.start(applicationContext, PASSIVE_ONLY, interval)
//                watch(passiveObservable, LocationManager.PASSIVE_PROVIDER)
//            }
//
//            if (fusedProviderOn()) {
//                val fusedObservable = locationTracker.start(applicationContext, fusedProviderPriority(), fusedUpdateInterval())
//                watch(fusedObservable, "fused")
//            }
        } else {
            onMapReady(map)
        }
    }

    private fun watch(@LocationTracker.Companion.Provider provider: Long,
                      observable: Observable<Location>) {

        @ColorRes val color = getColorForProvider(provider)
        addKey(provider)

        disposables.add(
                observable.subscribe { location ->
                    val latLng = LatLng(location)
                    if (map.markers.isEmpty()) {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20.0))
                    }

                    val markerOptions = MarkerOptions()
                            .position(latLng)
                            .title(location.provider)
                            .snippet("Provider:"+location.provider+" Acc:"+location.accuracy)
                            .icon(createMarker(color))
                    map.addMarker(markerOptions)
                }
        )
    }

    @ColorRes
    private fun getColorForProvider(@LocationTracker.Companion.Provider provider: Long): Int {
        return when (provider) {
            LocationTracker.GPS_ONLY -> R.color.gps
            LocationTracker.NETWORK_ONLY -> R.color.network
            LocationTracker.PASSIVE_ONLY -> R.color.passive
            else -> R.color.fused
        }
    }

    private fun addKey(@LocationTracker.Companion.Provider provider: Long) {
        val params = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.0f
        )
        val view = TextView(applicationContext)
        view.layoutParams = params
        view.text = LocationTracker.getProviderName(provider)
        view.setBackgroundColor(ContextCompat.getColor(
                applicationContext, getColorForProvider(provider)))

        keyLayout.addView(view)
    }

    private fun createMarker(@ColorRes color: Int): Icon {
        val factory = IconFactory.getInstance(this)

        val oldIcon = factory.defaultMarker().bitmap.copy(null, true)

        val paint = Paint()
        val filter = PorterDuffColorFilter(
                ContextCompat.getColor(this, color), PorterDuff.Mode.SRC_IN)
        paint.colorFilter = filter
        val canvas = Canvas(oldIcon)
        canvas.drawBitmap(oldIcon, 0f, 0f, paint)

        return factory.fromBitmap(oldIcon)
    }
}
