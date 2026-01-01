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
import pt.isec.diogo.safetysec.ui.screens.monitor.CreateSafeZoneScreen
import pt.isec.diogo.safetysec.ui.screens.monitor.MonitorDashboardScreen
import pt.isec.diogo.safetysec.ui.screens.monitor.MonitorProfileScreen
import pt.isec.diogo.safetysec.ui.screens.monitor.MyProtectedScreen
import pt.isec.diogo.safetysec.ui.screens.monitor.RulesScreen
import pt.isec.diogo.safetysec.ui.screens.monitor.SafeZonesScreen
import pt.isec.diogo.safetysec.ui.screens.protected_user.AddMonitorScreen
import pt.isec.diogo.safetysec.ui.screens.protected_user.MyMonitorsScreen
import pt.isec.diogo.safetysec.ui.screens.protected_user.MyRulesScreen
import pt.isec.diogo.safetysec.ui.screens.protected_user.ProtectedDashboardScreen
import pt.isec.diogo.safetysec.ui.screens.protected_user.ProtectedProfileScreen
import pt.isec.diogo.safetysec.ui.screens.protected_user.RuleTimeSettingsScreen
import pt.isec.diogo.safetysec.ui.screens.protected_user.SOSCountdownScreen
import pt.isec.diogo.safetysec.ui.screens.protected_user.AlertsSafetyScreen
import pt.isec.diogo.safetysec.ui.screens.monitor.AlertsScreen
import pt.isec.diogo.safetysec.ui.screens.monitor.AlertDetailScreen
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
                                        MonitorScreen.SafeZones.route -> {
                                            navController.navigate("monitor_safe_zones")
                                        }
                                        MonitorScreen.Alerts.route -> {
                                            navController.navigate(MonitorScreen.Alerts.route)
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
                                currentRoute = currentMonitorRoute,
                                onNavigate = { route ->
                                    currentMonitorRoute = route
                                    when (route) {
                                        MonitorScreen.Dashboard.route -> navController.navigate(Screen.MonitorDashboard.route) { popUpTo(Screen.MonitorDashboard.route) { inclusive = true } }
                                        MonitorScreen.Profile.route -> { /* Already here */ }
                                        MonitorScreen.MyProtected.route -> navController.navigate("monitor_my_protected") { popUpTo("monitor_profile") { inclusive = true } }
                                        MonitorScreen.Rules.route -> navController.navigate("monitor_rules") { popUpTo("monitor_profile") { inclusive = true } }
                                        MonitorScreen.SafeZones.route -> navController.navigate("monitor_safe_zones") { popUpTo("monitor_profile") { inclusive = true } }
                                        MonitorScreen.Alerts.route -> navController.navigate(MonitorScreen.Alerts.route) { popUpTo("monitor_profile") { inclusive = true } }
                                        else -> navController.navigate("monitor_placeholder/$route")
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
                                currentUser = authViewModel.currentUser,
                                currentUserId = authViewModel.currentUser?.uid,
                                currentRoute = currentMonitorRoute,
                                associationRepository = app.associationRepository,
                                onNavigate = { route ->
                                    currentMonitorRoute = route
                                    when (route) {
                                        MonitorScreen.Dashboard.route -> navController.navigate(Screen.MonitorDashboard.route) { popUpTo(Screen.MonitorDashboard.route) { inclusive = true } }
                                        MonitorScreen.Profile.route -> navController.navigate("monitor_profile") { popUpTo("monitor_my_protected") { inclusive = true } }
                                        MonitorScreen.MyProtected.route -> { /* Already here */ }
                                        MonitorScreen.Rules.route -> navController.navigate("monitor_rules") { popUpTo("monitor_my_protected") { inclusive = true } }
                                        MonitorScreen.SafeZones.route -> navController.navigate("monitor_safe_zones") { popUpTo("monitor_my_protected") { inclusive = true } }
                                        MonitorScreen.Alerts.route -> navController.navigate(MonitorScreen.Alerts.route) { popUpTo("monitor_my_protected") { inclusive = true } }
                                        else -> navController.navigate("monitor_placeholder/$route")
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
                                currentUser = authViewModel.currentUser,
                                currentUserId = authViewModel.currentUser?.uid,
                                currentRoute = currentMonitorRoute,
                                rulesRepository = app.rulesRepository,
                                onNavigate = { route ->
                                    currentMonitorRoute = route
                                    when (route) {
                                        MonitorScreen.Dashboard.route -> navController.navigate(Screen.MonitorDashboard.route) { popUpTo(Screen.MonitorDashboard.route) { inclusive = true } }
                                        MonitorScreen.Profile.route -> navController.navigate("monitor_profile") { popUpTo("monitor_rules") { inclusive = true } }
                                        MonitorScreen.MyProtected.route -> navController.navigate("monitor_my_protected") { popUpTo("monitor_rules") { inclusive = true } }
                                        MonitorScreen.Rules.route -> { /* Already here */ }
                                        MonitorScreen.SafeZones.route -> navController.navigate("monitor_safe_zones") { popUpTo("monitor_rules") { inclusive = true } }
                                        MonitorScreen.Alerts.route -> navController.navigate(MonitorScreen.Alerts.route) { popUpTo("monitor_rules") { inclusive = true } }
                                        else -> navController.navigate("monitor_placeholder/$route")
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
                                },
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

                        // Safe Zones (Monitor)
                        composable("monitor_safe_zones") {
                            currentMonitorRoute = MonitorScreen.SafeZones.route
                            SafeZonesScreen(
                                currentUser = authViewModel.currentUser,
                                currentUserId = authViewModel.currentUser?.uid,
                                currentRoute = currentMonitorRoute,
                                rulesRepository = app.rulesRepository,
                                onNavigate = { route ->
                                    currentMonitorRoute = route
                                    when (route) {
                                        MonitorScreen.Dashboard.route -> navController.navigate(Screen.MonitorDashboard.route) { popUpTo(Screen.MonitorDashboard.route) { inclusive = true } }
                                        MonitorScreen.Profile.route -> navController.navigate("monitor_profile") { popUpTo("monitor_safe_zones") { inclusive = true } }
                                        MonitorScreen.MyProtected.route -> navController.navigate("monitor_my_protected") { popUpTo("monitor_safe_zones") { inclusive = true } }
                                        MonitorScreen.Rules.route -> navController.navigate("monitor_rules") { popUpTo("monitor_safe_zones") { inclusive = true } }
                                        MonitorScreen.SafeZones.route -> { /* Already here */ }
                                        MonitorScreen.Alerts.route -> navController.navigate(MonitorScreen.Alerts.route) { popUpTo("monitor_safe_zones") { inclusive = true } }
                                        else -> navController.navigate("monitor_placeholder/$route")
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
                                },
                                onCreateZone = { navController.navigate("monitor_create_safe_zone") },
                                onEditZone = { ruleId -> navController.navigate("monitor_edit_safe_zone/$ruleId") }
                            )
                        }

                        // Create Safe Zone (Monitor)
                        composable("monitor_create_safe_zone") {
                            CreateSafeZoneScreen(
                                currentUserId = authViewModel.currentUser?.uid,
                                ruleId = null,
                                rulesRepository = app.rulesRepository,
                                associationRepository = app.associationRepository,
                                onNavigateBack = { navController.popBackStack() },
                                onSuccess = { navController.popBackStack() }
                            )
                        }

                        // Edit Safe Zone (Monitor)
                        composable("monitor_edit_safe_zone/{ruleId}") { backStackEntry ->
                            val ruleId = backStackEntry.arguments?.getString("ruleId") ?: ""
                            CreateSafeZoneScreen(
                                currentUserId = authViewModel.currentUser?.uid,
                                ruleId = ruleId,
                                rulesRepository = app.rulesRepository,
                                associationRepository = app.associationRepository,
                                onNavigateBack = { navController.popBackStack() },
                                onSuccess = { navController.popBackStack() }
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
                                        ProtectedScreen.AlertsSafety.route -> {
                                            navController.navigate(ProtectedScreen.AlertsSafety.route)
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
                                    navController.navigate(ProtectedScreen.SOSCountdown.route)
                                }
                            )
                        }

                        // Protected Profile
                        composable("protected_profile") {
                            currentProtectedRoute = ProtectedScreen.Profile.route
                            ProtectedProfileScreen(
                                currentUser = authViewModel.currentUser,
                                currentRoute = currentProtectedRoute,
                                onNavigate = { route ->
                                    currentProtectedRoute = route
                                    when (route) {
                                        ProtectedScreen.Dashboard.route -> navController.navigate(Screen.ProtectedDashboard.route) { popUpTo(Screen.ProtectedDashboard.route) { inclusive = true } }
                                        ProtectedScreen.Profile.route -> { /* Already here */ }
                                        ProtectedScreen.MyMonitors.route -> navController.navigate("protected_my_monitors") { popUpTo("protected_profile") { inclusive = true } }
                                        ProtectedScreen.MyRules.route -> navController.navigate("protected_my_rules") { popUpTo("protected_profile") { inclusive = true } }
                                        ProtectedScreen.AlertsSafety.route -> navController.navigate(ProtectedScreen.AlertsSafety.route) { popUpTo("protected_profile") { inclusive = true } }
                                        else -> navController.navigate("protected_placeholder/$route")
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
                                currentUser = authViewModel.currentUser,
                                currentUserId = authViewModel.currentUser?.uid,
                                currentRoute = currentProtectedRoute,
                                associationRepository = app.associationRepository,
                                onNavigate = { route ->
                                    currentProtectedRoute = route
                                    when (route) {
                                        ProtectedScreen.Dashboard.route -> navController.navigate(Screen.ProtectedDashboard.route) { popUpTo(Screen.ProtectedDashboard.route) { inclusive = true } }
                                        ProtectedScreen.Profile.route -> navController.navigate("protected_profile") { popUpTo("protected_my_monitors") { inclusive = true } }
                                        ProtectedScreen.MyMonitors.route -> { /* Already here */ }
                                        ProtectedScreen.MyRules.route -> navController.navigate("protected_my_rules") { popUpTo("protected_my_monitors") { inclusive = true } }
                                        ProtectedScreen.AlertsSafety.route -> navController.navigate(ProtectedScreen.AlertsSafety.route) { popUpTo("protected_my_monitors") { inclusive = true } }
                                        else -> navController.navigate("protected_placeholder/$route")
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
                                currentUser = authViewModel.currentUser,
                                currentUserId = authViewModel.currentUser?.uid,
                                currentRoute = currentProtectedRoute,
                                rulesRepository = app.rulesRepository,
                                onNavigate = { route ->
                                    currentProtectedRoute = route
                                    when (route) {
                                        ProtectedScreen.Dashboard.route -> navController.navigate(Screen.ProtectedDashboard.route) { popUpTo(Screen.ProtectedDashboard.route) { inclusive = true } }
                                        ProtectedScreen.Profile.route -> navController.navigate("protected_profile") { popUpTo("protected_my_rules") { inclusive = true } }
                                        ProtectedScreen.MyMonitors.route -> navController.navigate("protected_my_monitors") { popUpTo("protected_my_rules") { inclusive = true } }
                                        ProtectedScreen.MyRules.route -> { /* Already here */ }
                                        ProtectedScreen.AlertsSafety.route -> navController.navigate(ProtectedScreen.AlertsSafety.route) { popUpTo("protected_my_rules") { inclusive = true } }
                                        else -> navController.navigate("protected_placeholder/$route")
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

                        // SOS Countdown (Protected)
                        composable(ProtectedScreen.SOSCountdown.route) {
                            SOSCountdownScreen(
                                currentUser = authViewModel.currentUser,
                                onAlertTriggered = {
                                    // Create alert and navigate back to dashboard
                                    scope.launch {
                                        authViewModel.currentUser?.let { user ->
                                            val alert = pt.isec.diogo.safetysec.data.model.Alert(
                                                protectedUserId = user.uid,
                                                protectedUserName = user.displayName,
                                                triggerType = pt.isec.diogo.safetysec.data.model.AlertTriggerType.MANUAL_SOS,
                                                status = pt.isec.diogo.safetysec.data.model.AlertStatus.ACTIVE
                                            )
                                            app.alertsRepository.createAlert(alert)
                                        }
                                    }
                                    navController.navigate(Screen.ProtectedDashboard.route) {
                                        popUpTo(ProtectedScreen.SOSCountdown.route) { inclusive = true }
                                    }
                                },
                                onCancelled = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // Alerts Safety (Protected)
                        composable(ProtectedScreen.AlertsSafety.route) {
                            currentProtectedRoute = ProtectedScreen.AlertsSafety.route
                            AlertsSafetyScreen(
                                currentUser = authViewModel.currentUser,
                                currentUserId = authViewModel.currentUser?.uid,
                                currentRoute = currentProtectedRoute,
                                alertsRepository = app.alertsRepository,
                                onNavigate = { route ->
                                    currentProtectedRoute = route
                                    when (route) {
                                        ProtectedScreen.Dashboard.route -> navController.navigate(Screen.ProtectedDashboard.route) { popUpTo(Screen.ProtectedDashboard.route) { inclusive = true } }
                                        ProtectedScreen.AlertsSafety.route -> { /* Already here */ }
                                        ProtectedScreen.Profile.route -> navController.navigate("protected_profile") { popUpTo(ProtectedScreen.AlertsSafety.route) { inclusive = true } }
                                        ProtectedScreen.MyMonitors.route -> navController.navigate("protected_my_monitors") { popUpTo(ProtectedScreen.AlertsSafety.route) { inclusive = true } }
                                        ProtectedScreen.MyRules.route -> navController.navigate("protected_my_rules") { popUpTo(ProtectedScreen.AlertsSafety.route) { inclusive = true } }
                                        else -> navController.navigate("protected_placeholder/$route")
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
                                }
                            )
                        }

                        // Alerts (Monitor)
                        composable(MonitorScreen.Alerts.route) {
                            currentMonitorRoute = MonitorScreen.Alerts.route
                            AlertsScreen(
                                currentUser = authViewModel.currentUser,
                                currentUserId = authViewModel.currentUser?.uid,
                                currentRoute = currentMonitorRoute,
                                alertsRepository = app.alertsRepository,
                                associationRepository = app.associationRepository,
                                onNavigate = { route ->
                                    currentMonitorRoute = route
                                    when (route) {
                                        MonitorScreen.Dashboard.route -> navController.navigate(Screen.MonitorDashboard.route) { popUpTo(Screen.MonitorDashboard.route) { inclusive = true } }
                                        MonitorScreen.Alerts.route -> { /* Already here */ }
                                        MonitorScreen.Profile.route -> navController.navigate("monitor_profile") { popUpTo(MonitorScreen.Alerts.route) { inclusive = true } }
                                        MonitorScreen.MyProtected.route -> navController.navigate("monitor_my_protected") { popUpTo(MonitorScreen.Alerts.route) { inclusive = true } }
                                        MonitorScreen.Rules.route -> navController.navigate("monitor_rules") { popUpTo(MonitorScreen.Alerts.route) { inclusive = true } }
                                        MonitorScreen.SafeZones.route -> navController.navigate("monitor_safe_zones") { popUpTo(MonitorScreen.Alerts.route) { inclusive = true } }
                                        else -> navController.navigate("monitor_placeholder/$route")
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
                                },
                                onAlertClick = { alertId ->
                                    navController.navigate(MonitorScreen.AlertDetail.createRoute(alertId))
                                }
                            )
                        }

                        // Alert Detail (Monitor)
                        composable(MonitorScreen.AlertDetail.route) { backStackEntry ->
                            val alertId = backStackEntry.arguments?.getString("alertId") ?: ""
                            AlertDetailScreen(
                                alertId = alertId,
                                currentUser = authViewModel.currentUser,
                                alertsRepository = app.alertsRepository,
                                onNavigateBack = { navController.popBackStack() },
                                onResolved = { navController.popBackStack() }
                            )
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