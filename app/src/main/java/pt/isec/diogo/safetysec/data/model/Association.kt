package pt.isec.diogo.safetysec.data.model

/**
 * Associação entre um Monitor e um Protected user
 */
data class Association(
    val id: String = "",
    val monitorId: String = "",
    val protectedId: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "monitorId" to monitorId,
        "protectedId" to protectedId,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(id: String, data: Map<String, Any?>): Association = Association(
            id = id,
            monitorId = data["monitorId"] as? String ?: "",
            protectedId = data["protectedId"] as? String ?: "",
            createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis()
        )
    }
}
