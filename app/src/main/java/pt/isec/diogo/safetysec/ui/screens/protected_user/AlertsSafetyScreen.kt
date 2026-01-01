package pt.isec.diogo.safetysec.ui.screens.protected_user

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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import pt.isec.diogo.safetysec.ui.components.DrawerScaffold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Histórico de alertas do Protected.
 */
@Composable
fun AlertsSafetyScreen(
    currentUser: User?,
    currentUserId: String?,
    currentRoute: String,
    alertsRepository: AlertsRepository,
    onNavigate: (String) -> Unit,
    onSwitchProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var alerts by remember { mutableStateOf<List<Alert>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(currentUserId) {
        currentUserId?.let { uid ->
            scope.launch {
                alertsRepository.getAlertHistory(uid)
                    .onSuccess { result ->
                        alerts = result
                        isLoading = false
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
        title = stringResource(R.string.menu_alerts_safety),
        menuItems = getProtectedMenuItems(),
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
                        text = stringResource(R.string.no_alerts_history),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.you_are_safe),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(alerts) { alert ->
                    AlertHistoryCard(alert = alert)
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun AlertHistoryCard(alert: Alert) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (alert.status == AlertStatus.ACTIVE)
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
                imageVector = if (alert.status == AlertStatus.ACTIVE)
                    Icons.Default.Warning
                else
                    Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (alert.status == AlertStatus.ACTIVE)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Tipo de alerta
                Text(
                    text = getTriggerTypeDisplayName(alert.triggerType),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                // Date
                Text(
                    text = dateFormat.format(Date(alert.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Monitor que tratou
                if (alert.status == AlertStatus.RESOLVED && alert.resolvedByName != null) {
                    Text(
                        text = stringResource(R.string.resolved_by, alert.resolvedByName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Status badge
            Text(
                text = getStatusDisplayName(alert.status),
                style = MaterialTheme.typography.labelMedium,
                color = if (alert.status == AlertStatus.ACTIVE)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun getTriggerTypeDisplayName(type: AlertTriggerType): String {
    return when (type) {
        AlertTriggerType.MANUAL_SOS -> stringResource(R.string.trigger_manual_sos)
        AlertTriggerType.FALL_DETECTION -> stringResource(R.string.trigger_fall_detection)
        AlertTriggerType.ACCIDENT_DETECTION -> stringResource(R.string.trigger_accident_detection)
        AlertTriggerType.SPEED_LIMIT -> stringResource(R.string.trigger_speed_limit)
        AlertTriggerType.INACTIVITY -> stringResource(R.string.trigger_inactivity)
        AlertTriggerType.GEOFENCE_VIOLATION -> stringResource(R.string.trigger_geofence)
    }
}

@Composable
fun getStatusDisplayName(status: AlertStatus): String {
    return when (status) {
        AlertStatus.ACTIVE -> stringResource(R.string.status_active)
        AlertStatus.RESOLVED -> stringResource(R.string.status_resolved)
        AlertStatus.CANCELLED -> stringResource(R.string.status_cancelled)
    }
}
