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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pt.isec.diogo.safetysec.R
import pt.isec.diogo.safetysec.data.model.Rule
import pt.isec.diogo.safetysec.data.model.RuleType
import pt.isec.diogo.safetysec.data.repository.RulesRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeZonesScreen(
    currentUserId: String?,
    rulesRepository: RulesRepository,
    onNavigateBack: () -> Unit,
    onCreateZone: () -> Unit,
    onEditZone: (String) -> Unit
) {
    var zones by remember { mutableStateOf<List<Rule>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var zoneToDelete by remember { mutableStateOf<Rule?>(null) }
    val scope = rememberCoroutineScope()

    fun loadZones() {
        currentUserId?.let { uid ->
            scope.launch {
                rulesRepository.getRulesByMonitor(uid)
                    .onSuccess { result ->
                        zones = result.filter { it.type == RuleType.GEOFENCE }
                        isLoading = false
                    }
                    .onFailure {
                        isLoading = false
                    }
            }
        }
    }

    LaunchedEffect(currentUserId) {
        loadZones()
    }

    // Delete confirmation dialog
    zoneToDelete?.let { zone ->
        AlertDialog(
            onDismissRequest = { zoneToDelete = null },
            title = { Text(stringResource(R.string.delete_zone_title)) },
            text = { Text(stringResource(R.string.delete_zone_confirm, zone.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            rulesRepository.deleteRule(zone.id)
                            zoneToDelete = null
                            loadZones()
                        }
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { zoneToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_safe_zones)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateZone) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_safe_zone))
            }
        }
    ) { innerPadding ->
        if (zones.isEmpty() && !isLoading) {
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
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = stringResource(R.string.no_safe_zones),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.tap_create_zone),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
                items(zones) { zone ->
                    SafeZoneCard(
                        zone = zone,
                        onEdit = { onEditZone(zone.id) },
                        onDelete = { zoneToDelete = zone }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun SafeZoneCard(
    zone: Rule,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Location icon
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Zone details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = zone.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                zone.threshold?.let { radius ->
                    Text(
                        text = stringResource(R.string.radius_format, radius.toInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (zone.centerLatitude != null && zone.centerLongitude != null) {
                    Text(
                        text = "%.4f, %.4f".format(zone.centerLatitude, zone.centerLongitude),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Edit button
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
