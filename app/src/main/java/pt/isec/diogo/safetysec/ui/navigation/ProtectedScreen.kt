package pt.isec.diogo.safetysec.ui.navigation

/**
 * Rotas de navegação para o perfil Protected
 */
sealed class ProtectedScreen(val route: String) {
    data object Dashboard : ProtectedScreen("protected_dashboard")
    data object AlertsSafety : ProtectedScreen("protected_alerts_safety")
    data object SOSCountdown : ProtectedScreen("protected_sos_countdown")
    data object Recording : ProtectedScreen("protected_recording/{alertId}") {
        fun createRoute(alertId: String) = "protected_recording/$alertId"
    }
    data object MyMonitors : ProtectedScreen("protected_my_monitors")
    data object AddMonitor : ProtectedScreen("protected_add_monitor")
    data object MyRules : ProtectedScreen("protected_my_rules")
    data object RuleTimeSettings : ProtectedScreen("protected_rule_settings")
    data object Profile : ProtectedScreen("protected_profile")
}
