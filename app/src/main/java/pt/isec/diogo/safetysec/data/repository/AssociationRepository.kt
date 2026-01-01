package pt.isec.diogo.safetysec.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import pt.isec.diogo.safetysec.data.model.Association
import pt.isec.diogo.safetysec.data.model.OtpCode
import pt.isec.diogo.safetysec.data.model.User

/**
 * Repositório para associações Monitor-Protected e gestão dos OTP
 */
class AssociationRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val associationsCollection = firestore.collection("associations")
    private val otpCollection = firestore.collection("otp_codes")
    private val usersCollection = firestore.collection("users")

    suspend fun generateOtp(protectedId: String): Result<OtpCode> {
        return try {
            // Remover códigos anteriores do user
            val existingCodes = otpCollection
                .whereEqualTo("protectedId", protectedId)
                .get()
                .await()
            
            existingCodes.documents.forEach { doc ->
                otpCollection.document(doc.id).delete().await()
            }

            val otp = OtpCode.generate(protectedId)
            otpCollection.document(otp.code).set(otp.toMap()).await()
            
            Result.success(otp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun validateOtpAndAssociate(monitorId: String, code: String): Result<User> {
        return try {
            val otpDoc = otpCollection.document(code).get().await()
            
            if (!otpDoc.exists()) {
                return Result.failure(Exception("Invalid code"))
            }

            val otp = OtpCode.fromMap(code, otpDoc.data ?: emptyMap())
            
            if (otp.isExpired()) {
                otpCollection.document(code).delete().await()
                return Result.failure(Exception("Code expired"))
            }

            // Verifica se já existe associação
            val existingAssoc = associationsCollection
                .whereEqualTo("monitorId", monitorId)
                .whereEqualTo("protectedId", otp.protectedId)
                .get()
                .await()

            if (!existingAssoc.isEmpty) {
                otpCollection.document(code).delete().await()
                return Result.failure(Exception("Already associated"))
            }

            val association = Association(
                monitorId = monitorId,
                protectedId = otp.protectedId
            )
            associationsCollection.add(association.toMap()).await()

            // Remover OTP
            otpCollection.document(code).delete().await()

            // Retornar dados do Protected user
            val protectedDoc = usersCollection.document(otp.protectedId).get().await()
            if (protectedDoc.exists()) {
                Result.success(User.fromMap(protectedDoc.data ?: emptyMap()))
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyProtectedUsers(monitorId: String): Result<List<User>> {
        return try {
            val associations = associationsCollection
                .whereEqualTo("monitorId", monitorId)
                .get()
                .await()

            val users = mutableListOf<User>()
            for (doc in associations.documents) {
                val protectedId = doc.getString("protectedId") ?: continue
                val userDoc = usersCollection.document(protectedId).get().await()
                if (userDoc.exists()) {
                    users.add(User.fromMap(userDoc.data ?: emptyMap()))
                }
            }

            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyMonitors(protectedId: String): Result<List<User>> {
        return try {
            val associations = associationsCollection
                .whereEqualTo("protectedId", protectedId)
                .get()
                .await()

            val users = mutableListOf<User>()
            for (doc in associations.documents) {
                val monitorId = doc.getString("monitorId") ?: continue
                val userDoc = usersCollection.document(monitorId).get().await()
                if (userDoc.exists()) {
                    users.add(User.fromMap(userDoc.data ?: emptyMap()))
                }
            }

            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeAssociation(monitorId: String, protectedId: String): Result<Unit> {
        return try {
            val associations = associationsCollection
                .whereEqualTo("monitorId", monitorId)
                .whereEqualTo("protectedId", protectedId)
                .get()
                .await()

            associations.documents.forEach { doc ->
                associationsCollection.document(doc.id).delete().await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
