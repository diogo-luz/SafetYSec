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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import pt.isec.diogo.safetysec.R
import pt.isec.diogo.safetysec.data.model.User
import pt.isec.diogo.safetysec.ui.components.DrawerScaffold

/**
 * Ecrã de perfil do Protected com definições de PIN e timer
 */
@Composable
fun ProtectedProfileScreen(
    currentUser: User?,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onSwitchProfile: () -> Unit,
    onLogout: () -> Unit,
    onSaveProfile: (displayName: String, pin: String, timer: Int) -> Unit,
    onChangePassword: (String, () -> Unit, (String) -> Unit) -> Unit
) {
    var displayName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var cancellationPin by remember { mutableStateOf(currentUser?.cancellationPin ?: "0000") }
    var cancellationTimer by remember { 
        mutableFloatStateOf((currentUser?.cancellationTimer ?: 10).toFloat()) 
    }
    var isEditing by remember { mutableStateOf(false) }
    
    // Password change state
    var showPasswordChange by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var passwordSuccess by remember { mutableStateOf(false) }

    DrawerScaffold(
        currentUser = currentUser,
        currentRoute = currentRoute,
        title = stringResource(R.string.menu_profile),
        menuItems = getProtectedMenuItems(),
        onNavigate = onNavigate,
        onSwitchProfile = onSwitchProfile,
        onLogout = onLogout
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
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

            // Safety Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.safety_settings),
                        style = MaterialTheme.typography.titleMedium
                    )

                    // Cancellation PIN
                    OutlinedTextField(
                        value = cancellationPin,
                        onValueChange = { newValue ->
                            if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                                cancellationPin = newValue
                                isEditing = true
                            }
                        },
                        label = { Text(stringResource(R.string.cancellation_pin)) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        supportingText = { Text(stringResource(R.string.pin_hint)) }
                    )

                    // Cancellation Timer
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.cancellation_timer),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = stringResource(R.string.seconds_format, cancellationTimer.toInt()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = cancellationTimer,
                            onValueChange = { 
                                cancellationTimer = it
                                isEditing = true
                            },
                            valueRange = 5f..30f,
                            steps = 24
                        )

                        Text(
                            text = stringResource(R.string.timer_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Save button
            if (isEditing) {
                Button(
                    onClick = {
                        onSaveProfile(displayName, cancellationPin, cancellationTimer.toInt())
                        isEditing = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.save))
                }
            }

            HorizontalDivider()

            // Change Password Section
            Text(
                text = stringResource(R.string.change_password),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )

            if (!showPasswordChange) {
                OutlinedButton(
                    onClick = { showPasswordChange = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.change_password))
                }
            } else {
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { 
                        newPassword = it
                        passwordError = null
                        passwordSuccess = false
                    },
                    label = { Text(stringResource(R.string.new_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = confirmNewPassword,
                    onValueChange = { 
                        confirmNewPassword = it
                        passwordError = null
                    },
                    label = { Text(stringResource(R.string.confirm_new_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (passwordError != null) {
                    Text(
                        text = passwordError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (passwordSuccess) {
                    Text(
                        text = stringResource(R.string.password_changed_success),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        if (newPassword.length < 6) {
                            passwordError = "Password must be at least 6 characters"
                            return@Button
                        }
                        if (newPassword != confirmNewPassword) {
                            passwordError = "Passwords do not match"
                            return@Button
                        }
                        onChangePassword(
                            newPassword,
                            {
                                passwordSuccess = true
                                newPassword = ""
                                confirmNewPassword = ""
                                showPasswordChange = false
                            },
                            { error -> passwordError = error }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.save))
                }

                OutlinedButton(
                    onClick = {
                        showPasswordChange = false
                        newPassword = ""
                        confirmNewPassword = ""
                        passwordError = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}
