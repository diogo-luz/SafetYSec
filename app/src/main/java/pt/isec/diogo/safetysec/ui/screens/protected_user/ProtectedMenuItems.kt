package pt.isec.diogo.safetysec.ui.screens.protected_user

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Rule
import pt.isec.diogo.safetysec.R
import pt.isec.diogo.safetysec.ui.components.DrawerMenuItem
import pt.isec.diogo.safetysec.ui.navigation.ProtectedScreen

/**
 * Menu do perfil Protected
 */
fun getProtectedMenuItems(): List<DrawerMenuItem> = listOf(
    DrawerMenuItem(
        icon = Icons.Default.Home,
        labelResId = R.string.menu_dashboard,
        route = ProtectedScreen.Dashboard.route
    ),
    DrawerMenuItem(
        icon = Icons.Default.People,
        labelResId = R.string.menu_my_monitors,
        route = ProtectedScreen.MyMonitors.route
    ),
    DrawerMenuItem(
        icon = Icons.Default.Rule,
        labelResId = R.string.menu_my_rules,
        route = ProtectedScreen.MyRules.route
    ),
    DrawerMenuItem(
        icon = Icons.Default.Person,
        labelResId = R.string.menu_profile,
        route = ProtectedScreen.Profile.route
    )
)
