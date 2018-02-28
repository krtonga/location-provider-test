package krtonga.github.io.location_provider_test

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.location.Location
import android.location.LocationManager
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
import krtonga.github.io.location_provider_test.location.LocationTracker.Companion.GPS_ONLY
import krtonga.github.io.location_provider_test.location.LocationTracker.Companion.NETWORK_ONLY
import krtonga.github.io.location_provider_test.location.LocationTracker.Companion.PASSIVE_ONLY
import krtonga.github.io.location_provider_test.settings.SettingsActivity
import timber.log.Timber


class MapActivity : AbstractMapActivity() {

    val locationTracker = LocationTracker()

    val sharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

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
            R.id.action_settings -> startSettingsActivity()
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
        map.setStyle(mapboxStyle())
        LocationTracker.requestPermissions(this, onPermissionsGranted)
    }

    private val onPermissionsGranted: Consumer<Boolean> = Consumer { granted ->
        if (granted) {
            // Clean up leftovers //TODO move map initialization
            keyLayout.removeAllViews()
            disposables.dispose()
            disposables = CompositeDisposable()


            val interval = updateInterval()

            if (gpsProviderOn()) {
                val gpsObservable = locationTracker.start(applicationContext, GPS_ONLY, interval)
                subscribe(gpsObservable, LocationManager.GPS_PROVIDER)
            }

            if (networkProviderOn()) {
                val networkObservable = locationTracker.start(applicationContext, NETWORK_ONLY, interval)
                subscribe(networkObservable, LocationManager.NETWORK_PROVIDER)
            }

            if (passiveProviderOn()) {
                val passiveObservable = locationTracker.start(applicationContext, PASSIVE_ONLY, interval)
                subscribe(passiveObservable, LocationManager.PASSIVE_PROVIDER)
            }

            if (fusedProviderOn()) {
                val fusedObservable = locationTracker.start(applicationContext, fusedProviderPriority(), fusedUpdateInterval())
                subscribe(fusedObservable, "fused")
            }
        } else {
            onMapReady(map)
        }
    }

    private fun subscribe(observable: Observable<Location>, provider: String) {
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
                            .title("Fused")
                            .icon(createMarker(color))
                    map.addMarker(markerOptions)
                }
        )
    }

    @ColorRes
    private fun getColorForProvider(provider: String): Int {
        return when (provider) {
            LocationManager.NETWORK_PROVIDER -> R.color.network
            LocationManager.GPS_PROVIDER -> R.color.gps
            LocationManager.PASSIVE_PROVIDER -> R.color.passive
            else -> R.color.fused
        }
    }

    private fun addKey(provider: String) {
        val params = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.0f
        )
        val view = TextView(applicationContext)
        view.layoutParams = params
        view.text = provider
        view.setBackgroundColor(ContextCompat.getColor(applicationContext, getColorForProvider(provider)))

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

    // TODO Move Settings And Shared Pref Methods into Utility Class
    private fun startSettingsActivity(): Boolean {
        startActivity(Intent(this, SettingsActivity::class.java))
        return true
    }

    private fun mapboxStyle(): String {
        return sharedPreferences.getString(getString(R.string.pref_key_mapbox_style),
                resources.getStringArray(R.array.pref_mapbox_style_values)[0])
    }

    private fun gpsProviderOn(): Boolean {
        return sharedPreferences.getBoolean(getString(R.string.pref_key_gps_only), true)
    }

    private fun networkProviderOn(): Boolean {
        return sharedPreferences.getBoolean(getString(R.string.pref_key_network_only), true)
    }

    private fun passiveProviderOn(): Boolean {
        return sharedPreferences.getBoolean(getString(R.string.pref_key_passive_only), true)
    }

    private fun fusedProviderOn(): Boolean {
        return sharedPreferences.getBoolean(getString(R.string.pref_key_fused), true)
    }

    private fun updateInterval(): Long {
        return sharedPreferences.getString(getString(R.string.pref_key_interval),
                getString(R.string.pref_interval_default)).toLong() * 1000
    }

    private fun fusedUpdateInterval(): Long {
        return sharedPreferences.getString(getString(R.string.pref_key_fused_interval),
                getString(R.string.pref_interval_default)).toLong() * 1000
    }

    private fun fusedProviderPriority(): Long {
        return sharedPreferences.getString(getString(R.string.pref_key_fused_priority), "3").toLong()
    }
}
