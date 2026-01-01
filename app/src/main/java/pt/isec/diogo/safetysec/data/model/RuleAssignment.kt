package pt.isec.diogo.safetysec.data.model

data class RuleAssignment(
    val id: String = "",
    val ruleId: String = "",
    val protectedId: String = "",
    val isAccepted: Boolean = false,
    val startTime: String? = null,
    val endTime: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "ruleId" to ruleId,
        "protectedId" to protectedId,
        "isAccepted" to isAccepted,
        "startTime" to startTime,
        "endTime" to endTime,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(id: String, data: Map<String, Any?>): RuleAssignment = RuleAssignment(
            id = id,
            ruleId = data["ruleId"] as? String ?: "",
            protectedId = data["protectedId"] as? String ?: "",
            isAccepted = data["isAccepted"] as? Boolean ?: false,
            startTime = data["startTime"] as? String,
            endTime = data["endTime"] as? String,
            createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis()
        )
    }
}
