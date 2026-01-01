package pt.isec.diogo.safetysec.ui.screens.monitor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.isec.diogo.safetysec.R
import pt.isec.diogo.safetysec.data.model.User

/**
 * Ecrã de perfil do Monitor
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorProfileScreen(
    currentUser: User?,
    onNavigateBack: () -> Unit,
    onSaveProfile: (String) -> Unit
) {
    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var isEditing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_profile)) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Email
            OutlinedTextField(
                value = currentUser?.email ?: "",
                onValueChange = {},
                label = { Text(stringResource(R.string.email)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                singleLine = true
            )

            // Display Name
            OutlinedTextField(
                value = displayName,
                onValueChange = { 
                    displayName = it
                    isEditing = true
                },
                label = { Text(stringResource(R.string.display_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save button
            if (isEditing) {
                Button(
                    onClick = {
                        onSaveProfile(displayName)
                        isEditing = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.save))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Info text
            Text(
                text = stringResource(R.string.profile_monitor_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
