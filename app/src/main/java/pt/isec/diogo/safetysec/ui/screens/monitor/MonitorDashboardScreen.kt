package pt.isec.diogo.safetysec.ui.screens.monitor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import pt.isec.diogo.safetysec.R
import pt.isec.diogo.safetysec.data.model.Alert
import pt.isec.diogo.safetysec.data.model.AlertStatus
import pt.isec.diogo.safetysec.data.model.User
import pt.isec.diogo.safetysec.data.repository.AlertsRepository
import pt.isec.diogo.safetysec.data.repository.AssociationRepository
import pt.isec.diogo.safetysec.data.repository.RulesRepository
import pt.isec.diogo.safetysec.ui.components.AppDrawer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dashboard do Monitor com protected users
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorDashboardScreen(
    currentUser: User?,
    currentUserId: String?,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onSwitchProfile: () -> Unit,
    onLogout: () -> Unit,
    onAlertClick: (String) -> Unit = {}
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val associationRepository = remember { AssociationRepository() }
    val rulesRepository = remember { RulesRepository() }
    
    var protectedUsers by remember { mutableStateOf<List<ProtectedUserItem>>(emptyList()) }
    var recentAlerts by remember { mutableStateOf<List<Alert>>(emptyList()) }
    var activeRulesCount by remember { mutableStateOf(0) }
    var activeAlertsCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    
    val alertsRepository = remember { AlertsRepository() }

    // Fetch data
    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            isLoading = true
            
            // Fetch protected users
            associationRepository.getMyProtectedUsers(currentUserId).onSuccess { users ->
                val protectedIds = users.map { it.uid }
                
                // Fetch alertas
                var alertsMap = emptyMap<String, Boolean>()
                if (protectedIds.isNotEmpty()) {
                    alertsRepository.getAlertsForMonitor(protectedIds).onSuccess { alerts ->
                        val activeAlerts = alerts.filter { it.status == AlertStatus.ACTIVE }
                        activeAlertsCount = activeAlerts.size
                        alertsMap = activeAlerts.groupBy { it.protectedUserId }
                            .mapValues { true }
                        // Store recent alerts (top 5 sorted by date)
                        recentAlerts = alerts.sortedByDescending { it.createdAt }.take(5)
                    }
                }
                
                val items = users.map { user ->
                    ProtectedUserItem(
                        uid = user.uid,
                        displayName = user.displayName,
                        lastLocation = null,
                        lastUpdateTime = null,
                        isOnline = true,
                        hasActiveAlert = alertsMap.containsKey(user.uid)
                    )
                }
                protectedUsers = items
            }
            
            // Fetch rules count
            rulesRepository.getRulesByMonitor(currentUserId).onSuccess { rules ->
                activeRulesCount = rules.count { it.isActive }
            }
            
            isLoading = false
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                userName = currentUser?.displayName ?: "",
                userEmail = currentUser?.email ?: "",
                menuItems = getMonitorMenuItems(),
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
                    title = { Text(stringResource(R.string.monitor_dashboard_title)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.menu))
                        }
                    }
                )
            }
        ) { innerPadding ->
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Greeting Header
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.hello_greeting, currentUser?.displayName ?: ""),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.monitor_welcome_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Stats Cards Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Shield,
                                value = protectedUsers.size.toString(),
                                label = stringResource(R.string.protected_users_label),
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Warning,
                                value = activeAlertsCount.toString(),
                                label = stringResource(R.string.active_alerts_label),
                                containerColor = if (activeAlertsCount > 0)
                                    MaterialTheme.colorScheme.errorContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (activeAlertsCount > 0)
                                    MaterialTheme.colorScheme.onErrorContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.AutoMirrored.Filled.Rule,
                                value = activeRulesCount.toString(),
                                label = stringResource(R.string.rules_active_label),
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    
                    // Protected Users Section Header
                    item {
                        Text(
                            text = stringResource(R.string.my_protected_users),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    if (protectedUsers.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = stringResource(R.string.no_protected_users),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(R.string.add_protected_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    } else {
                        items(protectedUsers) { protectedUser ->
                            ProtectedUserCard(protectedUser = protectedUser)
                        }
                    }
                    
                    // Recent Alerts Section
                    if (recentAlerts.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.recent_alerts),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                        
                        items(recentAlerts.take(5)) { alert ->
                            RecentAlertCard(
                                alert = alert,
                                onClick = { onAlertClick(alert.id) }
                            )
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

/**
 * Card de alerta recente
 */
@Composable
private fun RecentAlertCard(
    alert: Alert,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (alert.status == AlertStatus.ACTIVE)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = if (alert.status == AlertStatus.ACTIVE)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.protectedUserName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = alert.triggerType.name.replace("_", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = dateFormat.format(Date(alert.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (alert.status == AlertStatus.ACTIVE)
                        stringResource(R.string.status_active)
                    else
                        stringResource(R.string.status_resolved),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (alert.status == AlertStatus.ACTIVE)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Card de estatísticas
 */
@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = contentColor
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}

data class ProtectedUserItem(
    val uid: String,
    val displayName: String,
    val lastLocation: String?,
    val lastUpdateTime: String?,
    val isOnline: Boolean,
    val hasActiveAlert: Boolean
)

/**
 * Card protected user
 */
@Composable
private fun ProtectedUserCard(
    protectedUser: ProtectedUserItem
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (protectedUser.hasActiveAlert) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = protectedUser.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (protectedUser.hasActiveAlert) 
                            Icons.Default.Warning 
                        else 
                            Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (protectedUser.hasActiveAlert)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (protectedUser.hasActiveAlert)
                            stringResource(R.string.status_alert)
                        else
                            stringResource(R.string.status_safe),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (protectedUser.hasActiveAlert)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Status indicator
            Surface(
                modifier = Modifier.size(12.dp),
                shape = CircleShape,
                color = if (protectedUser.isOnline)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            ) {}
        }
    }
}
