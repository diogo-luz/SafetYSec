package pt.isec.diogo.safetysec.ui.navigation

/**
 * Rotas de navegação para o perfil Monitor
 */
sealed class MonitorScreen(val route: String) {
    data object Dashboard : MonitorScreen("monitor_dashboard")
    data object MyProtected : MonitorScreen("monitor_my_protected")
    data object AddProtected : MonitorScreen("monitor_add_protected")
    data object Rules : MonitorScreen("monitor_rules")
    data object SafeZones : MonitorScreen("monitor_safe_zones")
    data object Alerts : MonitorScreen("monitor_alerts")
    data object Profile : MonitorScreen("monitor_profile")
}
