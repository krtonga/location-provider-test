package krtonga.github.io.location_provider_test.location

interface LocationSettingsInterface {
    fun isGpsProviderEnabled() : Boolean
    fun isNetworkProviderEnabled() : Boolean
    fun isPassiveProviderEnabled() : Boolean
    fun getInterval() : Long

    fun isFusedProviderEnabled() : Boolean
    fun getFusedProviderInterval() : Long
    @LocationTracker.Companion.Provider
    fun getFusedProviderPriority() : Long

    fun getAccuracy() : Float
}