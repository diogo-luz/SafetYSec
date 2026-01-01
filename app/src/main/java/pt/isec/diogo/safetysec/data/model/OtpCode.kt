package pt.isec.diogo.safetysec.data.model

/**
 * Código OTP para associação
 */
data class OtpCode(
    val code: String = "",
    val protectedId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (10 * 60 * 1000) // 10 min
) {
    fun toMap(): Map<String, Any> = mapOf(
        "protectedId" to protectedId,
        "createdAt" to createdAt,
        "expiresAt" to expiresAt
    )

    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt

    companion object {
        fun fromMap(code: String, data: Map<String, Any?>): OtpCode = OtpCode(
            code = code,
            protectedId = data["protectedId"] as? String ?: "",
            createdAt = data["createdAt"] as? Long ?: 0,
            expiresAt = data["expiresAt"] as? Long ?: 0
        )

        fun generate(protectedId: String): OtpCode {
            val code = (100000..999999).random().toString()
            return OtpCode(
                code = code,
                protectedId = protectedId
            )
        }
    }
}
