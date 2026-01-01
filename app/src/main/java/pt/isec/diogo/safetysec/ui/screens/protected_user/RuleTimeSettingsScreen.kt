package pt.isec.diogo.safetysec.ui.screens.protected_user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Checkbox
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
import pt.isec.diogo.safetysec.data.model.RuleAssignment
import pt.isec.diogo.safetysec.data.repository.RulesRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleTimeSettingsScreen(
    assignmentId: String,
    rulesRepository: RulesRepository,
    onNavigateBack: () -> Unit
) {
    var assignment by remember { mutableStateOf<RuleAssignment?>(null) }
    var rule by remember { mutableStateOf<Rule?>(null) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var isAccepted by remember { mutableStateOf(false) }
    var isScheduled by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(assignmentId) {
        rulesRepository.getAssignmentById(assignmentId)
            .onSuccess { (fetchedRule, fetchedAssignment) ->
                rule = fetchedRule
                assignment = fetchedAssignment
                startTime = fetchedAssignment.startTime ?: ""
                endTime = fetchedAssignment.endTime ?: ""
                isAccepted = fetchedAssignment.isAccepted
                isScheduled = fetchedAssignment.startTime != null && fetchedAssignment.endTime != null
                isLoading = false
            }
            .onFailure {
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rule_settings)) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Rule info
            rule?.let { r ->
                Text(
                    text = r.name,
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Accept toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.accept_rule),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isAccepted,
                    onCheckedChange = { isAccepted = it }
                )
            }

            Text(
                text = stringResource(R.string.accept_rule_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Schedule checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isScheduled,
                    onCheckedChange = { checked ->
                        isScheduled = checked
                        if (!checked) {
                            startTime = ""
                            endTime = ""
                        }
                    }
                )
                Text(
                    text = stringResource(R.string.enable_schedule),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Text(
                text = stringResource(R.string.schedule_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Time fields (only if scheduled)
            if (isScheduled) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { value ->
                            if (value.length <= 5 && value.all { it.isDigit() || it == ':' }) {
                                startTime = value
                                // Auto-suggest end time (+8 hours)
                                if (value.length == 5 && endTime.isEmpty()) {
                                    val parts = value.split(":")
                                    if (parts.size == 2) {
                                        val hour = (parts[0].toIntOrNull() ?: 0)
                                        val endHour = (hour + 8) % 24
                                        endTime = "%02d:%s".format(endHour, parts[1])
                                    }
                                }
                            }
                        },
                        label = { Text(stringResource(R.string.start_time)) },
                        placeholder = { Text("09:00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { value ->
                            if (value.length <= 5 && value.all { it.isDigit() || it == ':' }) {
                                endTime = value
                            }
                        },
                        label = { Text(stringResource(R.string.end_time)) },
                        placeholder = { Text("17:00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    assignment?.let { a ->
                        scope.launch {
                            isSaving = true
                            val updated = a.copy(
                                isAccepted = isAccepted,
                                startTime = if (isScheduled && startTime.isNotEmpty()) startTime else null,
                                endTime = if (isScheduled && endTime.isNotEmpty()) endTime else null
                            )
                            rulesRepository.updateAssignment(updated)
                            isSaving = false
                            onNavigateBack()
                        }
                    }
                },
                enabled = !isLoading && !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
