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
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pt.isec.diogo.safetysec.R
import pt.isec.diogo.safetysec.data.model.Rule
import pt.isec.diogo.safetysec.data.model.RuleAssignment
import pt.isec.diogo.safetysec.data.model.RuleType
import pt.isec.diogo.safetysec.data.model.User
import pt.isec.diogo.safetysec.data.repository.RulesRepository
import pt.isec.diogo.safetysec.services.BackgroundLocationService
import pt.isec.diogo.safetysec.ui.components.DrawerScaffold
import pt.isec.diogo.safetysec.ui.screens.monitor.getRuleTypeDisplayName
import android.content.Context
import android.content.Intent
import androidx.compose.ui.platform.LocalContext

@Composable
fun MyRulesScreen(
    currentUser: User?,
    currentUserId: String?,
    currentRoute: String,
    rulesRepository: RulesRepository,
    onNavigate: (String) -> Unit,
    onSwitchProfile: () -> Unit,
    onLogout: () -> Unit,
    onEditRule: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var rulesWithAssignments by remember { mutableStateOf<List<Pair<Rule, RuleAssignment>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadRules() {
        currentUserId?.let { uid ->
            scope.launch {
                rulesRepository.getAssignmentsForProtected(uid)
                    .onSuccess { result ->
                        rulesWithAssignments = result
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

    DrawerScaffold(
        currentUser = currentUser,
        currentRoute = currentRoute,
        title = stringResource(R.string.menu_my_rules),
        menuItems = getProtectedMenuItems(),
        onNavigate = onNavigate,
        onSwitchProfile = onSwitchProfile,
        onLogout = onLogout
    ) { innerPadding ->
        if (rulesWithAssignments.isEmpty() && !isLoading) {
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
                        text = stringResource(R.string.no_rules_assigned),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Get regular rules and safe zones
            val regularRules = rulesWithAssignments.filter { it.first.type != RuleType.GEOFENCE }
            val safeZones = rulesWithAssignments.filter { it.first.type == RuleType.GEOFENCE }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Rules Section
                if (regularRules.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.section_rules),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(regularRules) { (rule, assignment) ->
                        val context = LocalContext.current
                        MyRuleCard(
                            rule = rule,
                            assignment = assignment,
                            onToggleAccepted = { accepted ->
                                scope.launch {
                                    rulesRepository.updateAssignment(assignment.copy(isAccepted = accepted))
                                    loadRules()
                                    // Recarregar regras no serviço se estiver a correr
                                    if (BackgroundLocationService.isServiceRunning) {
                                        val intent = Intent(context, BackgroundLocationService::class.java)
                                        intent.action = BackgroundLocationService.ACTION_RELOAD_RULES
                                        context.startService(intent)
                                    }
                                }
                            },
                            onEdit = { onEditRule(assignment.id) }
                        )
                    }
                }

                // Safe Zones Section
                if (safeZones.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.section_safe_zones),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp).padding(top = 16.dp)
                        )
                    }
                    items(safeZones) { (rule, assignment) ->
                        val context = LocalContext.current
                        MyRuleCard(
                            rule = rule,
                            assignment = assignment,
                            onToggleAccepted = { accepted ->
                                scope.launch {
                                    rulesRepository.updateAssignment(assignment.copy(isAccepted = accepted))
                                    loadRules()
                                    // Recarregar regras no serviço se estiver a correr
                                    if (BackgroundLocationService.isServiceRunning) {
                                        val intent = Intent(context, BackgroundLocationService::class.java)
                                        intent.action = BackgroundLocationService.ACTION_RELOAD_RULES
                                        context.startService(intent)
                                    }
                                }
                            },
                            onEdit = { onEditRule(assignment.id) }
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
private fun MyRuleCard(
    rule: Rule,
    assignment: RuleAssignment,
    onToggleAccepted: (Boolean) -> Unit,
    onEdit: () -> Unit
) {
    Card(
        onClick = onEdit,
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
                // Show threshold info for Speed Limit and Inactivity
                rule.threshold?.let { threshold ->
                    when (rule.type) {
                        RuleType.SPEED_LIMIT -> {
                            Text(
                                text = stringResource(R.string.speed_limit_value, threshold.toInt()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        RuleType.INACTIVITY -> {
                            Text(
                                text = stringResource(R.string.inactivity_value, threshold.toInt()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        else -> {}
                    }
                }
                if (assignment.startTime != null && assignment.endTime != null) {
                    Text(
                        text = "${assignment.startTime} - ${assignment.endTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Switch(
                checked = assignment.isAccepted,
                onCheckedChange = onToggleAccepted
            )
        }
    }
}
