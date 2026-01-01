package pt.isec.diogo.safetysec.data.model

enum class RuleType {
    FALL_DETECTION,
    ACCIDENT_DETECTION,
    SPEED_LIMIT,
    INACTIVITY,
    GEOFENCE
}

data class Rule(
    val id: String = "",
    val monitorId: String = "",
    val name: String = "",
    val type: RuleType = RuleType.FALL_DETECTION,
    val threshold: Double? = null,
    val centerLatitude: Double? = null,
    val centerLongitude: Double? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "monitorId" to monitorId,
        "name" to name,
        "type" to type.name,
        "threshold" to threshold,
        "centerLatitude" to centerLatitude,
        "centerLongitude" to centerLongitude,
        "isActive" to isActive,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(id: String, data: Map<String, Any?>): Rule = Rule(
            id = id,
            monitorId = data["monitorId"] as? String ?: "",
            name = data["name"] as? String ?: "",
            type = try {
                RuleType.valueOf(data["type"] as? String ?: "FALL_DETECTION")
            } catch (e: Exception) {
                RuleType.FALL_DETECTION
            },
            threshold = (data["threshold"] as? Number)?.toDouble(),
            centerLatitude = (data["centerLatitude"] as? Number)?.toDouble(),
            centerLongitude = (data["centerLongitude"] as? Number)?.toDouble(),
            isActive = data["isActive"] as? Boolean ?: true,
            createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis()
        )
    }
}
