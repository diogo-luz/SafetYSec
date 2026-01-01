package pt.isec.diogo.safetysec.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationHandler(context: Context) {
    private val locationProvider: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    var currentLocation: Location? = null
        private set
    
    var locationEnabled = false
        private set
    
    var onLocationUpdate: ((Location) -> Unit)? = null

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (locationEnabled) return
        
        // tentar obter a última localização conhecida primeiro
        locationProvider.lastLocation
            .addOnSuccessListener { location ->
                location?.let {
                    currentLocation = it
                    onLocationUpdate?.invoke(it)
                }
            }
        
        // updates contínuos
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()
        
        locationProvider.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        
        locationEnabled = true
    }
    
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            result.lastLocation?.let { location ->
                currentLocation = location
                onLocationUpdate?.invoke(location)
            }
        }
    }
    
    fun stopLocationUpdates() {
        if (!locationEnabled) return
        locationProvider.removeLocationUpdates(locationCallback)
        locationEnabled = false
    }
    
    /**
     * última localização conhecida
     */
    @SuppressLint("MissingPermission")
    fun getLastLocation(callback: (Location?) -> Unit) {
        locationProvider.lastLocation
            .addOnSuccessListener { location ->
                currentLocation = location
                callback(location)
            }
            .addOnFailureListener {
                callback(null)
            }
    }
}
