package pt.isec.diogo.safetysec.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import pt.isec.diogo.safetysec.R
import pt.isec.diogo.safetysec.data.model.User

/**
 * Wrapper drawer navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerScaffold(
    currentUser: User?,
    currentRoute: String,
    title: String,
    menuItems: List<DrawerMenuItem>,
    onNavigate: (String) -> Unit,
    onSwitchProfile: () -> Unit,
    onLogout: () -> Unit,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                userName = currentUser?.displayName ?: "",
                userEmail = currentUser?.email ?: "",
                menuItems = menuItems,
                selectedRoute = currentRoute,
                onItemClick = { route ->
                    scope.launch { drawerState.close() }
                    onNavigate(route)
                },
                onSwitchProfile = {
                    scope.launch { drawerState.close() }
                    onSwitchProfile()
                },
                onLogout = {
                    scope.launch { drawerState.close() }
                    onLogout()
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.menu))
                        }
                    }
                )
            },
            floatingActionButton = floatingActionButton
        ) { innerPadding ->
            content(innerPadding)
        }
    }
}
