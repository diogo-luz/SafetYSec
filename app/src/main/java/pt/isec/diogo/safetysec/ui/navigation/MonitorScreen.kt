package pt.isec.diogo.safetysec.ui.navigation

/**
 * Rotas de navegação para o perfil Monitor
 */
sealed class MonitorScreen(val route: String) {
    data object Dashboard : MonitorScreen("monitor_dashboard")
    data object MyProtected : MonitorScreen("monitor_my_protected")
    data object AddProtected : MonitorScreen("monitor_add_protected")
    data object Rules : MonitorScreen("monitor_rules")
    data object CreateRule : MonitorScreen("monitor_create_rule")
    data object AssignRule : MonitorScreen("monitor_assign_rule")
    data object SafeZones : MonitorScreen("monitor_safe_zones")
    data object CreateSafeZone : MonitorScreen("monitor_create_safe_zone")
    data object Alerts : MonitorScreen("monitor_alerts")
    data object Profile : MonitorScreen("monitor_profile")
}
