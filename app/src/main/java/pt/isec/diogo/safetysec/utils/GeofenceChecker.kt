package pt.isec.diogo.safetysec.utils

import pt.isec.diogo.safetysec.data.model.Rule
import pt.isec.diogo.safetysec.data.model.RuleType
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class GeofenceChecker {
    
    companion object {
        private const val EARTH_RADIUS_METERS = 6371000.0
    }
    
    fun isInsideAnyZone(
        latitude: Double,
        longitude: Double,
        geofenceRules: List<Rule>
    ): Boolean {
        val activeGeofences = geofenceRules.filter { 
            it.type == RuleType.GEOFENCE && it.isActive 
        }
        if (activeGeofences.isEmpty()) return true
        
        return activeGeofences.any { rule ->
            isInsideZone(latitude, longitude, rule)
        }
    }
    
    fun isInsideZone(
        latitude: Double,
        longitude: Double,
        rule: Rule
    ): Boolean {
        val centerLat = rule.centerLatitude ?: return true
        val centerLon = rule.centerLongitude ?: return true
        val radius = rule.threshold ?: return true
        
        val distance = calculateDistance(
            lat1 = latitude,
            lon1 = longitude,
            lat2 = centerLat,
            lon2 = centerLon
        )
        return distance <= radius
    }
    
    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return EARTH_RADIUS_METERS * c
    }
}
