package pt.isec.diogo.safetysec.ui.screens.monitor

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
fun CreateRuleScreen(
    currentUserId: String?,
    rulesRepository: RulesRepository,
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("Fall Detection") }
    var selectedType by remember { mutableStateOf(RuleType.FALL_DETECTION) }
    var threshold by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Available types
    val availableTypes = listOf(
        RuleType.FALL_DETECTION,
        RuleType.ACCIDENT_DETECTION,
        RuleType.SPEED_LIMIT,
        RuleType.INACTIVITY
    )

    val needsThreshold = selectedType == RuleType.SPEED_LIMIT || selectedType == RuleType.INACTIVITY

    val thresholdLabel = when (selectedType) {
        RuleType.SPEED_LIMIT -> stringResource(R.string.max_speed_kmh)
        RuleType.INACTIVITY -> stringResource(R.string.time_minutes)
        else -> ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_rule)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.rule_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = getRuleTypeDisplayName(selectedType),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.rule_type)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(getRuleTypeDisplayName(type)) },
                            onClick = {
                                selectedType = type
                                expanded = false
                                
                                name = getRuleTypeDisplayName(type)
                                if (type != RuleType.SPEED_LIMIT && type != RuleType.INACTIVITY) {
                                    threshold = ""
                                }
                            }
                        )
                    }
                }
            }

            if (needsThreshold) {
                OutlinedTextField(
                    value = threshold,
                    onValueChange = { if (it.all { c -> c.isDigit() }) threshold = it },
                    label = { Text(thresholdLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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

                    currentUserId?.let { uid ->
                        scope.launch {
                            isLoading = true
                            val rule = Rule(
                                monitorId = uid,
                                name = name,
                                type = selectedType,
                                threshold = if (needsThreshold) threshold.toDoubleOrNull() else null
                            )
                            rulesRepository.createRule(rule)
                                .onSuccess {
                                    isLoading = false
                                    onSuccess()
                                }
                                .onFailure { e ->
                                    isLoading = false
                                    error = e.message
                                }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.create_rule))
            }
        }
    }
}

private fun getRuleTypeDisplayName(type: RuleType): String = when (type) {
    RuleType.FALL_DETECTION -> "Fall Detection"
    RuleType.ACCIDENT_DETECTION -> "Accident Detection"
    RuleType.SPEED_LIMIT -> "Speed Limit"
    RuleType.INACTIVITY -> "Inactivity Alert"
    RuleType.GEOFENCE -> "Safe Zone"
}
