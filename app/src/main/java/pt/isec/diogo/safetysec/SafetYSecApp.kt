package pt.isec.diogo.safetysec

import android.app.Application
import pt.isec.diogo.safetysec.data.repository.AssociationRepository
import pt.isec.diogo.safetysec.data.repository.AuthRepository
import pt.isec.diogo.safetysec.data.repository.UserRepository

/**
 * inicialização lazy para singletons como na app da lista de contactos
 */
class SafetYSecApp : Application() {

    val authRepository: AuthRepository by lazy {
        AuthRepository()
    }

    val userRepository: UserRepository by lazy {
        UserRepository()
    }

    val associationRepository: AssociationRepository by lazy {
        AssociationRepository()
    }

    override fun onCreate() {
        super.onCreate()
    }
}

