package pt.isec.diogo.safetysec

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pt.isec.diogo.safetysec.ui.navigation.MonitorScreen
import pt.isec.diogo.safetysec.ui.navigation.ProtectedScreen
import pt.isec.diogo.safetysec.ui.navigation.Screen
import pt.isec.diogo.safetysec.ui.screens.common.LoginScreen
import pt.isec.diogo.safetysec.ui.screens.common.ProfileSelectionScreen
import pt.isec.diogo.safetysec.ui.screens.common.RegisterScreen
import pt.isec.diogo.safetysec.ui.screens.monitor.MonitorDashboardScreen
import pt.isec.diogo.safetysec.ui.screens.monitor.MonitorProfileScreen
import pt.isec.diogo.safetysec.ui.screens.protected_user.ProtectedDashboardScreen
import pt.isec.diogo.safetysec.ui.screens.protected_user.ProtectedProfileScreen
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

                // Determina o perfil atual para o drawer selection
                var currentMonitorRoute by remember { mutableStateOf(MonitorScreen.Dashboard.route) }
                var currentProtectedRoute by remember { mutableStateOf(ProtectedScreen.Dashboard.route) }

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
                                    currentMonitorRoute = MonitorScreen.Dashboard.route
                                    navController.navigate(Screen.MonitorDashboard.route) {
                                        popUpTo(Screen.ProfileSelection.route) { inclusive = false }
                                    }
                                },
                                onSelectProtected = {
                                    currentProtectedRoute = ProtectedScreen.Dashboard.route
                                    navController.navigate(Screen.ProtectedDashboard.route) {
                                        popUpTo(Screen.ProfileSelection.route) { inclusive = false }
                                    }
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

                        // Monitor Dashboard
                        composable(Screen.MonitorDashboard.route) {
                            currentMonitorRoute = MonitorScreen.Dashboard.route
                            MonitorDashboardScreen(
                                currentUser = authViewModel.currentUser,
                                currentRoute = currentMonitorRoute,
                                onNavigate = { route ->
                                    currentMonitorRoute = route
                                    when (route) {
                                        MonitorScreen.Dashboard.route -> { /* ja esta aqui */ }
                                        MonitorScreen.Profile.route -> {
                                            navController.navigate("monitor_profile")
                                        }
                                        else -> {
                                            // Placeholder para outras screens
                                            navController.navigate("monitor_placeholder/$route")
                                        }
                                    }
                                },
                                onSwitchProfile = {
                                    navController.navigate(Screen.ProfileSelection.route) {
                                        popUpTo(Screen.MonitorDashboard.route) { inclusive = true }
                                    }
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

                        // Monitor Profile
                        composable("monitor_profile") {
                            currentMonitorRoute = MonitorScreen.Profile.route
                            MonitorProfileScreen(
                                currentUser = authViewModel.currentUser,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onSaveProfile = { displayName ->
                                    // TODO: Save profile via UserRepository
                                }
                            )
                        }

                        // Monitor Placeholder screens
                        composable("monitor_placeholder/{route}") { backStackEntry ->
                            val route = backStackEntry.arguments?.getString("route") ?: ""
                            PlaceholderScreen("Monitor: $route\n(Coming Soon)")
                        }

                        // Protected Dashboard
                        composable(Screen.ProtectedDashboard.route) {
                            currentProtectedRoute = ProtectedScreen.Dashboard.route
                            ProtectedDashboardScreen(
                                currentUser = authViewModel.currentUser,
                                currentRoute = currentProtectedRoute,
                                onNavigate = { route ->
                                    currentProtectedRoute = route
                                    when (route) {
                                        ProtectedScreen.Dashboard.route -> { /* Already here */ }
                                        ProtectedScreen.Profile.route -> {
                                            navController.navigate("protected_profile")
                                        }
                                        else -> {
                                            navController.navigate("protected_placeholder/$route")
                                        }
                                    }
                                },
                                onSwitchProfile = {
                                    navController.navigate(Screen.ProfileSelection.route) {
                                        popUpTo(Screen.ProtectedDashboard.route) { inclusive = true }
                                    }
                                },
                                onLogout = {
                                    authViewModel.logout {
                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                },
                                onTriggerSOS = {
                                    // TODO: Implement SOS flow
                                }
                            )
                        }

                        // Protected Profile
                        composable("protected_profile") {
                            currentProtectedRoute = ProtectedScreen.Profile.route
                            ProtectedProfileScreen(
                                currentUser = authViewModel.currentUser,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onSaveProfile = { displayName, pin, timer ->
                                    // TODO: Save profile via UserRepository
                                }
                            )
                        }

                        // Protected Placeholder screens
                        composable("protected_placeholder/{route}") { backStackEntry ->
                            val route = backStackEntry.arguments?.getString("route") ?: ""
                            PlaceholderScreen("Protected: $route\n(Coming Soon)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
    }
}