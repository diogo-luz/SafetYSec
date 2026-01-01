package pt.isec.diogo.safetysec

import android.app.Application
import pt.isec.diogo.safetysec.data.repository.AuthRepository

/**
 * inicialização lazy para singletons como na app da lista de contactos
 */
class SafetYSecApp : Application() {

    /**
     * Instancia Singleton AuthRepository.
     * Lazy no primeiro acesso
     */
    val authRepository: AuthRepository by lazy {
        AuthRepository()
    }

    override fun onCreate() {
        super.onCreate()
        // Firebase é iniciado com o google-services.json
    }
}
