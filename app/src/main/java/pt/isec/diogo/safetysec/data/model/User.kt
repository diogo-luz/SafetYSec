package pt.isec.diogo.safetysec.data.model

/**
 * Mapeia um user para a estrutura de documento da coleção 'users' no Firestore.
 */
data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val cancellationPin: String = "0000",
    val cancellationTimer: Int = 10
) {
    /**
     * Converter um objeto User para um Map para armazenamento no Firestore.
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "email" to email,
            "displayName" to displayName,
            "cancellationPin" to cancellationPin,
            "cancellationTimer" to cancellationTimer
        )
    }

    companion object {
        /**
         * Cria um objeto User a partir de um Map de um documento Firestore.
         */
        fun fromMap(map: Map<String, Any?>): User {
            return User(
                uid = map["uid"] as? String ?: "",
                email = map["email"] as? String ?: "",
                displayName = map["displayName"] as? String ?: "",
                cancellationPin = map["cancellationPin"] as? String ?: "0000",
                cancellationTimer = (map["cancellationTimer"] as? Long)?.toInt() ?: 10
            )
        }
    }
}
