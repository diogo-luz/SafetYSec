package pt.isec.diogo.safetysec.ui.screens.monitor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pt.isec.diogo.safetysec.R
import pt.isec.diogo.safetysec.data.model.Rule
import pt.isec.diogo.safetysec.data.model.RuleType
import pt.isec.diogo.safetysec.data.repository.RulesRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRuleScreen(
    ruleId: String,
    rulesRepository: RulesRepository,
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit
) {
    var rule by remember { mutableStateOf<Rule?>(null) }
    var name by remember { mutableStateOf("") }
    var threshold by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Load rule data
    LaunchedEffect(ruleId) {
        rulesRepository.getRuleById(ruleId).onSuccess { loadedRule ->
            rule = loadedRule
            name = loadedRule.name
            threshold = loadedRule.threshold?.toString() ?: ""
            isActive = loadedRule.isActive
            isLoading = false
        }.onFailure {
            error = it.message
            isLoading = false
        }
    }

    val needsThreshold = rule?.type == RuleType.SPEED_LIMIT || rule?.type == RuleType.INACTIVITY

    val thresholdLabel = when (rule?.type) {
        RuleType.SPEED_LIMIT -> stringResource(R.string.max_speed_kmh)
        RuleType.INACTIVITY -> stringResource(R.string.time_minutes)
        else -> ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_rule)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (rule == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = error ?: stringResource(R.string.rule_not_found),
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Rule type display (read-only)
                OutlinedTextField(
                    value = getRuleTypeDisplayName(rule!!.type),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.rule_type)) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Rule name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.rule_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Threshold if needed
                if (needsThreshold) {
                    OutlinedTextField(
                        value = threshold,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) threshold = it },
                        label = { Text(thresholdLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Active toggle
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.rule_active),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                }

                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (name.isBlank()) {
                            error = "Please enter a name"
                            return@Button
                        }
                        if (needsThreshold && threshold.isBlank()) {
                            error = "Please enter a value"
                            return@Button
                        }

                        scope.launch {
                            isSaving = true
                            val updatedRule = rule!!.copy(
                                name = name,
                                threshold = if (needsThreshold) threshold.toDoubleOrNull() else rule!!.threshold,
                                isActive = isActive
                            )
                            rulesRepository.updateRule(updatedRule)
                                .onSuccess {
                                    isSaving = false
                                    onSuccess()
                                }
                                .onFailure { e ->
                                    isSaving = false
                                    error = e.message
                                }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}
