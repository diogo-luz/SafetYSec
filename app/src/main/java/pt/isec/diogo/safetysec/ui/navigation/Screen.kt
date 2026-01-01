package pt.isec.diogo.safetysec.ui.navigation

/**
 * Destinos para navegação na app
 */
sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object ProfileSelection : Screen("profile_selection")
    data object MonitorDashboard : Screen("monitor_dashboard")
    data object ProtectedDashboard : Screen("protected_dashboard")
}
