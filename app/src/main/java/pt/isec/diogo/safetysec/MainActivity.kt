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
import androidx.compose.runtime.rememberCoroutineScope
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
import pt.isec.diogo.safetysec.ui.screens.monitor.AddProtectedScreen
import pt.isec.diogo.safetysec.ui.screens.monitor.AssignRuleScreen
import pt.isec.diogo.safetysec.ui.screens.monitor.CreateRuleScreen
import pt.isec.diogo.safetysec.ui.screens.monitor.MonitorDashboardScreen
import pt.isec.diogo.safetysec.ui.screens.monitor.MonitorProfileScreen
import pt.isec.diogo.safetysec.ui.screens.monitor.MyProtectedScreen
import pt.isec.diogo.safetysec.ui.screens.monitor.RulesScreen
import pt.isec.diogo.safetysec.ui.screens.protected_user.AddMonitorScreen
import pt.isec.diogo.safetysec.ui.screens.protected_user.MyMonitorsScreen
import pt.isec.diogo.safetysec.ui.screens.protected_user.MyRulesScreen
import pt.isec.diogo.safetysec.ui.screens.protected_user.ProtectedDashboardScreen
import pt.isec.diogo.safetysec.ui.screens.protected_user.ProtectedProfileScreen
import pt.isec.diogo.safetysec.ui.screens.protected_user.RuleTimeSettingsScreen
import pt.isec.diogo.safetysec.ui.theme.SafetYSecTheme
import pt.isec.diogo.safetysec.ui.viewmodels.AuthViewModel
import pt.isec.diogo.safetysec.ui.viewmodels.AuthViewModelFactory
import kotlinx.coroutines.launch

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

                // Para operações assíncronas (save profile etc etc)
                val scope = rememberCoroutineScope()

                // Determina o destino inicial com base no estado de autenticação
                val startDestination = if (authViewModel.isAuthenticated) {
                    Screen.ProfileSelection.route
                } else {
                    Screen.Login.route
                }

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.fillMaxSize()
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
                                        MonitorScreen.MyProtected.route -> {
                                            navController.navigate("monitor_my_protected")
                                        }
                                        MonitorScreen.Rules.route -> {
                                            navController.navigate("monitor_rules")
                                        }
                                        else -> {
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
                                    authViewModel.currentUser?.uid?.let { uid ->
                                        scope.launch {
                                            app.userRepository.updateProfile(uid, displayName)
                                            authViewModel.refreshUser()
                                        }
                                    }
                                }
                            )
                        }

                        // My Protected Users (Monitor)
                        composable("monitor_my_protected") {
                            currentMonitorRoute = MonitorScreen.MyProtected.route
                            MyProtectedScreen(
                                currentUserId = authViewModel.currentUser?.uid,
                                associationRepository = app.associationRepository,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onAddProtected = {
                                    navController.navigate("monitor_add_protected")
                                }
                            )
                        }

                        // Add Protected User (Monitor - OTP input)
                        composable("monitor_add_protected") {
                            AddProtectedScreen(
                                currentUserId = authViewModel.currentUser?.uid,
                                associationRepository = app.associationRepository,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onSuccess = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // Rules (Monitor)
                        composable("monitor_rules") {
                            currentMonitorRoute = MonitorScreen.Rules.route
                            RulesScreen(
                                currentUserId = authViewModel.currentUser?.uid,
                                rulesRepository = app.rulesRepository,
                                onNavigateBack = { navController.popBackStack() },
                                onCreateRule = { navController.navigate("monitor_create_rule") },
                                onAssignRule = { ruleId -> navController.navigate("monitor_assign_rule/$ruleId") }
                            )
                        }

                        // Create Rule (Monitor)
                        composable("monitor_create_rule") {
                            CreateRuleScreen(
                                currentUserId = authViewModel.currentUser?.uid,
                                rulesRepository = app.rulesRepository,
                                onNavigateBack = { navController.popBackStack() },
                                onSuccess = { navController.popBackStack() }
                            )
                        }

                        // Assign Rule (Monitor)
                        composable("monitor_assign_rule/{ruleId}") { backStackEntry ->
                            val ruleId = backStackEntry.arguments?.getString("ruleId") ?: ""
                            AssignRuleScreen(
                                currentUserId = authViewModel.currentUser?.uid,
                                ruleId = ruleId,
                                rulesRepository = app.rulesRepository,
                                associationRepository = app.associationRepository,
                                onNavigateBack = { navController.popBackStack() }
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
                                        ProtectedScreen.MyMonitors.route -> {
                                            navController.navigate("protected_my_monitors")
                                        }
                                        ProtectedScreen.MyRules.route -> {
                                            navController.navigate("protected_my_rules")
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
                                    authViewModel.currentUser?.uid?.let { uid ->
                                        scope.launch {
                                            app.userRepository.updateProtectedProfile(uid, displayName, pin, timer)
                                            authViewModel.refreshUser()
                                        }
                                    }
                                }
                            )
                        }

                        // My Monitors (Protected)
                        composable("protected_my_monitors") {
                            currentProtectedRoute = ProtectedScreen.MyMonitors.route
                            MyMonitorsScreen(
                                currentUserId = authViewModel.currentUser?.uid,
                                associationRepository = app.associationRepository,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onAddMonitor = {
                                    navController.navigate("protected_add_monitor")
                                }
                            )
                        }

                        // Add Monitor (Protected - OTP generation)
                        composable("protected_add_monitor") {
                            AddMonitorScreen(
                                currentUserId = authViewModel.currentUser?.uid,
                                associationRepository = app.associationRepository,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // My Rules (Protected)
                        composable("protected_my_rules") {
                            currentProtectedRoute = ProtectedScreen.MyRules.route
                            MyRulesScreen(
                                currentUserId = authViewModel.currentUser?.uid,
                                rulesRepository = app.rulesRepository,
                                onNavigateBack = { navController.popBackStack() },
                                onEditRule = { assignmentId ->
                                    navController.navigate("protected_rule_settings/$assignmentId")
                                }
                            )
                        }

                        // Rule Time Settings (Protected)
                        composable("protected_rule_settings/{assignmentId}") { backStackEntry ->
                            val assignmentId = backStackEntry.arguments?.getString("assignmentId") ?: ""
                            RuleTimeSettingsScreen(
                                assignmentId = assignmentId,
                                rulesRepository = app.rulesRepository,
                                onNavigateBack = { navController.popBackStack() }
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