package pt.isec.diogo.safetysec.ui.screens.monitor

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import pt.isec.diogo.safetysec.R
import pt.isec.diogo.safetysec.data.model.Rule
import pt.isec.diogo.safetysec.data.model.RuleType
import pt.isec.diogo.safetysec.data.model.User
import pt.isec.diogo.safetysec.data.repository.AssociationRepository
import pt.isec.diogo.safetysec.data.repository.RulesRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSafeZoneScreen(
    currentUserId: String?,
    ruleId: String? = null, // null = create, preenchido = edit
    rulesRepository: RulesRepository,
    associationRepository: AssociationRepository,
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val defaultName = stringResource(R.string.rule_type_geofence)
    var name by remember { mutableStateOf(defaultName) }
    var latitude by remember { mutableStateOf("40.2033") } // Default: Coimbra
    var longitude by remember { mutableStateOf("-8.4103") }
    var radius by remember { mutableFloatStateOf(200f) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Protected users for assignment
    var protectedUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var selectedUsers by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    val scope = rememberCoroutineScope()

    // Map state
    val markerPosition = remember(latitude, longitude) {
        val lat = latitude.toDoubleOrNull() ?: 40.2033
        val lng = longitude.toDoubleOrNull() ?: -8.4103
        LatLng(lat, lng)
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(markerPosition, 15f)
    }
    val markerState = remember { MarkerState(position = markerPosition) }

    // Load protected users
    LaunchedEffect(currentUserId) {
        currentUserId?.let { uid ->
            associationRepository.getMyProtectedUsers(uid)
                .onSuccess { users ->
                    protectedUsers = users
                }
        }
        
        // load no ecra de edicao
        ruleId?.let { id ->
            rulesRepository.getRuleById(id)
                .onSuccess { rule ->
                    name = rule.name
                    rule.centerLatitude?.let { latitude = it.toString() }
                    rule.centerLongitude?.let { longitude = it.toString() }
                    rule.threshold?.let { radius = it.toFloat() }
                }
            
            // Load existing assignments
            rulesRepository.getAssignmentsForRule(id)
                .onSuccess { assignedIds ->
                    selectedUsers = assignedIds.toSet()
                }
        }
    }

    // mudar o marker quando muda a localizacao
    LaunchedEffect(latitude, longitude) {
        val lat = latitude.toDoubleOrNull() ?: return@LaunchedEffect
        val lng = longitude.toDoubleOrNull() ?: return@LaunchedEffect
        markerState.position = LatLng(lat, lng)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (ruleId == null) stringResource(R.string.create_safe_zone) 
                        else stringResource(R.string.edit_safe_zone)
                    ) 
                },
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
            // Zone Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.zone_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Lat/Lng
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = { Text(stringResource(R.string.latitude)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text(stringResource(R.string.longitude)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Radius
            Text(
                text = stringResource(R.string.radius_label, radius.toInt()),
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = radius,
                onValueChange = { radius = it },
                valueRange = 50f..1000f,
                steps = 18,
                modifier = Modifier.fillMaxWidth()
            )

            // Map Preview
            Text(
                text = stringResource(R.string.tap_map_pick_location),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapClick = { latLng ->
                        latitude = "%.6f".format(latLng.latitude)
                        longitude = "%.6f".format(latLng.longitude)
                        markerState.position = latLng
                    }
                ) {
                    Marker(
                        state = markerState,
                        title = name
                    )
                    Circle(
                        center = markerState.position,
                        radius = radius.toDouble(),
                        fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        strokeColor = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2f
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Assign to Protected Users
            if (protectedUsers.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.assign_to_protected),
                    style = MaterialTheme.typography.titleMedium
                )
                
                protectedUsers.forEach { user ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedUsers.contains(user.uid),
                            onCheckedChange = { checked ->
                                selectedUsers = if (checked) {
                                    selectedUsers + user.uid
                                } else {
                                    selectedUsers - user.uid
                                }
                            }
                        )
                        Text(
                            text = user.displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    val lat = latitude.toDoubleOrNull()
                    val lng = longitude.toDoubleOrNull()
                    
                    if (name.isBlank()) {
                        error = "Please enter a name"
                        return@Button
                    }
                    if (lat == null || lng == null) {
                        error = "Invalid coordinates"
                        return@Button
                    }

                    currentUserId?.let { uid ->
                        scope.launch {
                            isLoading = true
                            
                            val rule = Rule(
                                id = ruleId ?: "",
                                monitorId = uid,
                                name = name,
                                type = RuleType.GEOFENCE,
                                threshold = radius.toDouble(),
                                centerLatitude = lat,
                                centerLongitude = lng
                            )
                            
                            val result = if (ruleId == null) {
                                rulesRepository.createRule(rule)
                            } else {
                                rulesRepository.updateRule(rule).map { rule }
                            }
                            
                            result.onSuccess { savedRule ->
                                // assignments
                                val ruleIdToUse = savedRule.id.ifEmpty { ruleId ?: "" }
                                
                                // Get current assignments
                                val currentAssignments = rulesRepository.getAssignmentsForRule(ruleIdToUse)
                                    .getOrDefault(emptyList()).toSet()
                                
                                // Add new assignments
                                selectedUsers.forEach { userId ->
                                    if (!currentAssignments.contains(userId)) {
                                        rulesRepository.assignRule(ruleIdToUse, userId)
                                    }
                                }
                                
                                // Remove assignments (removidos)
                                currentAssignments.forEach { userId ->
                                    if (!selectedUsers.contains(userId)) {
                                        rulesRepository.removeAssignment(ruleIdToUse, userId)
                                    }
                                }
                                
                                isLoading = false
                                onSuccess()
                            }.onFailure { e ->
                                isLoading = false
                                error = e.message
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
