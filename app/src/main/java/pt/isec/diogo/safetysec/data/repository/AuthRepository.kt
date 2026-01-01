package pt.isec.diogo.safetysec.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import pt.isec.diogo.safetysec.data.model.User

/**
 * Repositorio para autenticacao e gestão de utilizadores.
 */
class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val usersCollection = firestore.collection("users")

    /**
     * Retorna o utilizador atualmente logado, ou null se não estiver logado.
     */
    val currentFirebaseUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * Verifica se um utilizador está atualmente autenticado.
     */
    val isUserAuthenticated: Boolean
        get() = auth.currentUser != null

    /**
     * Regista um novo utilizador com email e password.
     * Cria um documento de utilizador correspondente no Firestore.
     *
     * @param email Email do utilizador
     * @param password Password do utilizador
     * @param displayName Nome de exibicao do utilizador
     * @return Result com o utilizador criado ou uma exceção
     */
    suspend fun register(
        email: String,
        password: String,
        displayName: String
    ): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Registration failed: user is null"))

            val user = User(
                uid = firebaseUser.uid,
                email = email,
                displayName = displayName,
                cancellationPin = "0000",
                cancellationTimer = 10
            )

            // Criar o documento de utilizador no Firestore
            usersCollection.document(firebaseUser.uid)
                .set(user.toMap())
                .await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Autentica um utilizador com email e password.
     *
     * @param email Email do utilizador
     * @param password Password do utilizador
     * @return Result com o utilizador autenticado ou uma exceção
     */
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Login failed: user is null"))

            // Vai buscar o documento de utilizador do Firestore
            val document = usersCollection.document(firebaseUser.uid).get().await()

            if (document.exists()) {
                val user = User.fromMap(document.data ?: emptyMap())
                Result.success(user)
            } else {
                // O documento do utilizador não existe, criamos um com os valores por defeito
                val user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: email,
                    displayName = firebaseUser.displayName ?: "",
                    cancellationPin = "0000",
                    cancellationTimer = 10
                )
                usersCollection.document(firebaseUser.uid)
                    .set(user.toMap())
                    .await()
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Termina a sessão do utilizador atual.
     */
    fun logout() {
        auth.signOut()
    }

    /**
     * Obtem os dados do utilizador atual do Firestore.
     *
     * @return Result com o utilizador ou uma exceção
     */
    suspend fun getCurrentUser(): Result<User> {
        val firebaseUser = auth.currentUser
            ?: return Result.failure(Exception("No authenticated user"))

        return try {
            val document = usersCollection.document(firebaseUser.uid).get().await()
            if (document.exists()) {
                val user = User.fromMap(document.data ?: emptyMap())
                Result.success(user)
            } else {
                Result.failure(Exception("User document not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
