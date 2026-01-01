package pt.isec.diogo.safetysec

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pt.isec.diogo.safetysec.ui.navigation.Screen
import pt.isec.diogo.safetysec.ui.screens.common.LoginScreen
import pt.isec.diogo.safetysec.ui.screens.common.ProfileSelectionScreen
import pt.isec.diogo.safetysec.ui.screens.common.RegisterScreen
import pt.isec.diogo.safetysec.ui.theme.SafetYSecTheme
import pt.isec.diogo.safetysec.ui.viewmodels.AuthViewModel
import pt.isec.diogo.safetysec.ui.viewmodels.AuthViewModelFactory

/**
 * Main activity da aplicação. -> manifest.xml
 */
class MainActivity : ComponentActivity() {

    private val app by lazy { application as SafetYSecApp }

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(app.authRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SafetYSecTheme {
                val navController = rememberNavController()

                // Determina o destino inicial com base no estado de autenticação
                val startDestination = if (authViewModel.isAuthenticated) {
                    Screen.ProfileSelection.route
                } else {
                    Screen.Login.route
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // Login Screen
                        composable(Screen.Login.route) {
                            LoginScreen(
                                viewModel = authViewModel,
                                onNavigateToRegister = {
                                    navController.navigate(Screen.Register.route)
                                },
                                onLoginSuccess = {
                                    navController.navigate(Screen.ProfileSelection.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Register Screen
                        composable(Screen.Register.route) {
                            RegisterScreen(
                                viewModel = authViewModel,
                                onNavigateToLogin = {
                                    navController.popBackStack()
                                },
                                onRegisterSuccess = {
                                    navController.navigate(Screen.ProfileSelection.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Profile Selection Screen
                        composable(Screen.ProfileSelection.route) {
                            ProfileSelectionScreen(
                                viewModel = authViewModel,
                                onSelectMonitor = {
                                    // TODO: Navigar para Monitor Dashboard na Phase 2
                                    navController.navigate(Screen.MonitorDashboard.route)
                                },
                                onSelectProtected = {
                                    // TODO: Navigar para Protected Dashboard na Phase 2
                                    navController.navigate(Screen.ProtectedDashboard.route)
                                },
                                onLogout = {
                                    authViewModel.logout {
                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        // Placeholder Monitor Dashboard (Phase 2)
                        composable(Screen.MonitorDashboard.route) {
                            // TODO: Implementar
                            PlaceholderScreen("Monitor Dashboard")
                        }

                        // Placeholder Protected Dashboard (Phase 2)
                        composable(Screen.ProtectedDashboard.route) {
                            // TODO: Implementar
                            PlaceholderScreen("Protected Dashboard")
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun PlaceholderScreen(title: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = "$title\n(Coming in Phase 2)",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}