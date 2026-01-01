package pt.isec.diogo.safetysec.ui.screens.monitor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Rule
import pt.isec.diogo.safetysec.R
import pt.isec.diogo.safetysec.ui.components.DrawerMenuItem
import pt.isec.diogo.safetysec.ui.navigation.MonitorScreen

/**
 * Menu perfil Monitor
 */
fun getMonitorMenuItems(): List<DrawerMenuItem> = listOf(
    DrawerMenuItem(
        icon = Icons.Default.Home,
        labelResId = R.string.menu_dashboard,
        route = MonitorScreen.Dashboard.route
    ),
    DrawerMenuItem(
        icon = Icons.Default.People,
        labelResId = R.string.menu_my_protected,
        route = MonitorScreen.MyProtected.route
    ),
    DrawerMenuItem(
        icon = Icons.AutoMirrored.Filled.Rule,
        labelResId = R.string.menu_rules,
        route = MonitorScreen.Rules.route
    ),
    DrawerMenuItem(
        icon = Icons.Default.Place,
        labelResId = R.string.menu_safe_zones,
        route = MonitorScreen.SafeZones.route
    ),
    DrawerMenuItem(
        icon = Icons.Default.Notifications,
        labelResId = R.string.menu_alerts,
        route = MonitorScreen.Alerts.route
    ),
    DrawerMenuItem(
        icon = Icons.Default.Person,
        labelResId = R.string.menu_profile,
        route = MonitorScreen.Profile.route
    )
)
