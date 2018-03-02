package krtonga.github.io.location_provider_test.settings

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import krtonga.github.io.location_provider_test.R
import krtonga.github.io.location_provider_test.location.LocationSettingsInterface

class SettingsHelper(val sharedPreferences: SharedPreferences, val resources: Resources)
    : LocationSettingsInterface {

    fun startSettingsActivity(activity: Activity): Boolean {
        activity.startActivity(Intent(activity, SettingsActivity::class.java))
        return true
    }

    fun mapboxStyle(): String {
        return sharedPreferences.getString(resources.getString(R.string.pref_key_mapbox_style),
                resources.getStringArray(R.array.pref_mapbox_style_values)[0])
    }

    override fun isGpsProviderEnabled(): Boolean {
        return sharedPreferences.getBoolean(
                resources.getString(R.string.pref_key_gps_only), true)
    }

    override fun isNetworkProviderEnabled(): Boolean {
        return sharedPreferences.getBoolean(
                resources.getString(R.string.pref_key_gps_only), true)
    }

    override fun isPassiveProviderEnabled(): Boolean {
        return sharedPreferences.getBoolean(
                resources.getString(R.string.pref_key_passive_only), true)
    }

    override fun getInterval(): Long {
        return sharedPreferences.getString(
                resources.getString(R.string.pref_key_interval),
                resources.getString(R.string.pref_interval_default)).toLong() * 1000    }

    override fun isFusedProviderEnabled(): Boolean {
        return sharedPreferences.getBoolean(
                resources.getString(R.string.pref_key_fused), true)
    }

    override fun getFusedProviderInterval(): Long {
        return sharedPreferences.getString(
                resources.getString(R.string.pref_key_fused_interval),
                resources.getString(R.string.pref_interval_default)).toLong() * 1000
    }

    override fun getFusedProviderPriority(): Long {
        return sharedPreferences.getString(
                resources.getString(R.string.pref_key_fused_priority), "3").toLong()
    }

    override fun getAccuracy(): Float {
        TODO("not implemented")
    }
}