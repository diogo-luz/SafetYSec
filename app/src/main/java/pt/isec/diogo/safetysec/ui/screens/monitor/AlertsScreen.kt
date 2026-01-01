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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pt.isec.diogo.safetysec.R
import pt.isec.diogo.safetysec.data.model.Alert
import pt.isec.diogo.safetysec.data.model.AlertStatus
import pt.isec.diogo.safetysec.data.model.AlertTriggerType
import pt.isec.diogo.safetysec.data.model.User
import pt.isec.diogo.safetysec.data.repository.AlertsRepository
import pt.isec.diogo.safetysec.data.repository.AssociationRepository
import pt.isec.diogo.safetysec.ui.components.DrawerScaffold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Alertas do Monitor
 */
@Composable
fun AlertsScreen(
    currentUser: User?,
    currentUserId: String?,
    currentRoute: String,
    alertsRepository: AlertsRepository,
    associationRepository: AssociationRepository,
    onNavigate: (String) -> Unit,
    onSwitchProfile: () -> Unit,
    onLogout: () -> Unit,
    onAlertClick: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var alerts by remember { mutableStateOf<List<Alert>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentUserId) {
        currentUserId?.let { uid ->
            scope.launch {
                // IDs dos protected users
                associationRepository.getMyProtectedUsers(uid)
                    .onSuccess { protectedUsers ->
                        val protectedIds = protectedUsers.map { it.uid }
                        if (protectedIds.isNotEmpty()) {
                            alertsRepository.getAlertsForMonitor(protectedIds)
                                .onSuccess { result ->
                                    alerts = result
                                    isLoading = false
                                }
                                .onFailure {
                                    isLoading = false
                                }
                        } else {
                            isLoading = false
                        }
                    }
                    .onFailure {
                        isLoading = false
                    }
            }
        }
    }

    DrawerScaffold(
        currentUser = currentUser,
        currentRoute = currentRoute,
        title = stringResource(R.string.menu_alerts),
        menuItems = getMonitorMenuItems(),
        onNavigate = onNavigate,
        onSwitchProfile = onSwitchProfile,
        onLogout = onLogout
    ) { innerPadding ->
        if (alerts.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Text(
                        text = stringResource(R.string.no_alerts),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.all_protected_safe),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            // Separar alertas ativos dos resolvidos
            val activeAlerts = alerts.filter { it.status == AlertStatus.ACTIVE }
            val resolvedAlerts = alerts.filter { it.status == AlertStatus.RESOLVED }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Active alerts
                if (activeAlerts.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.active_alerts),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(activeAlerts) { alert ->
                        MonitorAlertCard(
                            alert = alert,
                            onClick = { onAlertClick(alert.id) }
                        )
                    }
                }

                // Resolved alerts
                if (resolvedAlerts.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.resolved_alerts),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 0.dp)
                                .padding(top = if (activeAlerts.isNotEmpty()) 16.dp else 0.dp)
                        )
                    }
                    items(resolvedAlerts) { alert ->
                        MonitorAlertCard(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonitorAlertCard(
    alert: Alert,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val isActive = alert.status == AlertStatus.ACTIVE

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = if (isActive)
                    Icons.Default.Warning
                else
                    Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isActive)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Protected username
                Text(
                    text = alert.protectedUserName.ifEmpty { stringResource(R.string.unknown_user) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                // Trigger type
                Text(
                    text = getMonitorTriggerTypeDisplayName(alert.triggerType),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isActive)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Date
                Text(
                    text = dateFormat.format(Date(alert.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isActive)
                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Status
            if (isActive) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = stringResource(R.string.status_active),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun getMonitorTriggerTypeDisplayName(type: AlertTriggerType): String {
    return when (type) {
        AlertTriggerType.MANUAL_SOS -> stringResource(R.string.trigger_manual_sos)
        AlertTriggerType.GEOFENCE_VIOLATION -> stringResource(R.string.trigger_geofence)
        AlertTriggerType.RULE_VIOLATION -> stringResource(R.string.trigger_rule)
    }
}
