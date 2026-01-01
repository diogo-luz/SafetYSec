package pt.isec.diogo.safetysec.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import pt.isec.diogo.safetysec.data.model.User

/**
 * Repositório para operações de perfil de utilizador
 */
class UserRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    /**
     * Atualiza o perfil do utilizador (display name)
     */
    suspend fun updateProfile(uid: String, displayName: String): Result<Unit> {
        return try {
            usersCollection.document(uid)
                .update("displayName", displayName)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Atualiza as definições do Protected user
     */
    suspend fun updateSafetySettings(
        uid: String,
        cancellationPin: String,
        cancellationTimer: Int
    ): Result<Unit> {
        return try {
            usersCollection.document(uid)
                .update(
                    mapOf(
                        "cancellationPin" to cancellationPin,
                        "cancellationTimer" to cancellationTimer
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Atualiza o perfil completo do Protected user
     */
    suspend fun updateProtectedProfile(
        uid: String,
        displayName: String,
        cancellationPin: String,
        cancellationTimer: Int
    ): Result<Unit> {
        return try {
            usersCollection.document(uid)
                .update(
                    mapOf(
                        "displayName" to displayName,
                        "cancellationPin" to cancellationPin,
                        "cancellationTimer" to cancellationTimer
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtém os dados do utilizador
     */
    suspend fun getUser(uid: String): Result<User> {
        return try {
            val document = usersCollection.document(uid).get().await()
            if (document.exists()) {
                val user = User.fromMap(document.data ?: emptyMap())
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
