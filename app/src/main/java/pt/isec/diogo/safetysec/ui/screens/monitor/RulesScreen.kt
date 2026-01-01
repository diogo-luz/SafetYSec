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
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
fun RulesScreen(
    currentUserId: String?,
    rulesRepository: RulesRepository,
    onNavigateBack: () -> Unit,
    onCreateRule: () -> Unit,
    onAssignRule: (String) -> Unit
) {
    var rules by remember { mutableStateOf<List<Rule>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var ruleToDelete by remember { mutableStateOf<Rule?>(null) }
    val scope = rememberCoroutineScope()

    fun loadRules() {
        currentUserId?.let { uid ->
            scope.launch {
                rulesRepository.getRulesByMonitor(uid)
                    .onSuccess { result ->
                        rules = result
                        isLoading = false
                    }
                    .onFailure {
                        isLoading = false
                    }
            }
        }
    }

    LaunchedEffect(currentUserId) {
        loadRules()
    }

    // Delete confirmation dialog
    ruleToDelete?.let { rule ->
        AlertDialog(
            onDismissRequest = { ruleToDelete = null },
            title = { Text(stringResource(R.string.delete_rule_title)) },
            text = { Text(stringResource(R.string.delete_rule_confirm, rule.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            rulesRepository.deleteRule(rule.id)
                            ruleToDelete = null
                            loadRules()
                        }
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { ruleToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_rules)) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateRule) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_rule))
            }
        }
    ) { innerPadding ->
        if (rules.isEmpty() && !isLoading) {
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
                        imageVector = Icons.AutoMirrored.Filled.Rule,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = stringResource(R.string.no_rules),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.tap_create_rule),
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
                items(rules) { rule ->
                    RuleCard(
                        rule = rule,
                        onToggle = { isActive ->
                            scope.launch {
                                rulesRepository.updateRule(rule.copy(isActive = isActive))
                                loadRules()
                            }
                        },
                        onAssign = { onAssignRule(rule.id) },
                        onDelete = { ruleToDelete = rule }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleCard(
    rule: Rule,
    onToggle: (Boolean) -> Unit,
    onAssign: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onAssign,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = getRuleTypeDisplayName(rule.type),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                rule.threshold?.let { threshold ->
                    Text(
                        text = getThresholdLabel(rule.type, threshold),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }

            Switch(
                checked = rule.isActive,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun getThresholdLabel(type: RuleType, threshold: Double): String = when (type) {
    RuleType.SPEED_LIMIT -> "${threshold.toInt()} km/h"
    RuleType.INACTIVITY -> "${threshold.toInt()} min"
    RuleType.GEOFENCE -> "${threshold.toInt()} m radius"
    else -> ""
}
