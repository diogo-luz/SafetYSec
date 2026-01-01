package pt.isec.diogo.safetysec.ui.navigation

/**
 * Rotas de navegação para o perfil Protected
 */
sealed class ProtectedScreen(val route: String) {
    data object Dashboard : ProtectedScreen("protected_dashboard")
    data object AlertsSafety : ProtectedScreen("protected_alerts_safety")
    data object MyMonitors : ProtectedScreen("protected_my_monitors")
    data object AddMonitor : ProtectedScreen("protected_add_monitor")
    data object MyRules : ProtectedScreen("protected_my_rules")
    data object Profile : ProtectedScreen("protected_profile")
}
