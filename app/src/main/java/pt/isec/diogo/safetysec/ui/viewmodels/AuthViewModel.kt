package pt.isec.diogo.safetysec.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pt.isec.diogo.safetysec.data.model.User
import pt.isec.diogo.safetysec.data.repository.AuthRepository

/**
 * ViewModel para estado e operações relacionadas com a autenticação.
 */
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    // Estado da UI
    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var confirmPassword by mutableStateOf("")
        private set

    var displayName by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var currentUser by mutableStateOf<User?>(null)
        private set

    var isAuthenticated by mutableStateOf(false)
        private set

    init {
        // Verifica se o utilizador está autenticado ao criar o ViewModel
        checkAuthenticationStatus()
    }

    private fun checkAuthenticationStatus() {
        if (authRepository.isUserAuthenticated) {
            viewModelScope.launch {
                authRepository.getCurrentUser()
                    .onSuccess { user ->
                        currentUser = user
                        isAuthenticated = true
                    }
                    .onFailure {
                        // Utilizador autenticado mas documento não encontrado, então faz logout
                        authRepository.logout()
                        isAuthenticated = false
                    }
            }
        }
    }

    fun updateEmail(value: String) {
        email = value
    }

    fun updatePassword(value: String) {
        password = value
    }

    fun updateConfirmPassword(value: String) {
        confirmPassword = value
    }

    fun updateDisplayName(value: String) {
        displayName = value
    }

    fun clearError() {
        error = null
    }

    /**
     * Tenta fazer login com o email e password atual.
     * @param onSuccess Callback invocado quando o login é bem-sucedido
     */
    fun login(onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            error = "Please fill in all fields"
            return
        }

        viewModelScope.launch {
            isLoading = true
            error = null

            authRepository.login(email, password)
                .onSuccess { user ->
                    currentUser = user
                    isAuthenticated = true
                    clearFormFields()
                    onSuccess()
                }
                .onFailure { exception ->
                    error = exception.message ?: "Login failed"
                }

            isLoading = false
        }
    }

    /**
     * Tenta registar um novo utilizador com os dados atuais.
     * @param onSuccess Callback invocado quando o registo é bem-sucedido
     */
    fun register(onSuccess: () -> Unit) {
        if (displayName.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            error = "Please fill in all fields"
            return
        }

        if (password != confirmPassword) {
            error = "Passwords do not match"
            return
        }

        if (password.length < 6) {
            error = "Password must be at least 6 characters"
            return
        }

        viewModelScope.launch {
            isLoading = true
            error = null

            authRepository.register(email, password, displayName)
                .onSuccess { user ->
                    currentUser = user
                    isAuthenticated = true
                    clearFormFields()
                    onSuccess()
                }
                .onFailure { exception ->
                    error = exception.message ?: "Registration failed"
                }

            isLoading = false
        }
    }

    /**
     * Termina a sessão do utilizador atual.
     * @param onSuccess Callback invocado quando o logout é bem-sucedido
     */
    fun logout(onSuccess: () -> Unit) {
        authRepository.logout()
        currentUser = null
        isAuthenticated = false
        clearFormFields()
        onSuccess()
    }

    /**
     * Atualizar os dados do utilizador atual no Firestore
     */
    fun refreshUser() {
        viewModelScope.launch {
            authRepository.getCurrentUser()
                .onSuccess { user ->
                    currentUser = user
                }
        }
    }

    private fun clearFormFields() {
        email = ""
        password = ""
        confirmPassword = ""
        displayName = ""
    }
}
