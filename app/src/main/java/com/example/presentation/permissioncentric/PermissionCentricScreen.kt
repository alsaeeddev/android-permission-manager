package com.example.presentation.permissioncentric

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.PermissionGroupType
import com.example.domain.model.AppInfo
import com.example.presentation.common.AppIconImage
import com.example.presentation.common.OnResumeEffect
import com.example.presentation.common.RiskBadge
import com.example.presentation.common.getPermissionGroupIcon
import com.example.ui.theme.RiskCriticalColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionCentricScreen(
    viewModel: PermissionCentricViewModel,
    onNavigateToAppDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    val sensitiveGroups = listOf(
        PermissionGroupType.CAMERA,
        PermissionGroupType.MICROPHONE,
        PermissionGroupType.LOCATION,
        PermissionGroupType.CONTACTS,
        PermissionGroupType.SMS,
        PermissionGroupType.CALL_LOG,
        PermissionGroupType.STORAGE,
        PermissionGroupType.PHONE,
        PermissionGroupType.CALENDAR,
        PermissionGroupType.SENSORS,
        PermissionGroupType.NEARBY_DEVICES,
        PermissionGroupType.NOTIFICATIONS
    )

    OnResumeEffect {
        viewModel.refresh(force = false)
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (uiState.showBulkRevokeDialog) {
        AlertDialog(onDismissRequest = { viewModel.showBulkRevokeConfirmation(false) }, icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = RiskCriticalColor
            )
        }, title = {
            Text("Bulk Revoke ${uiState.selectedGroup.displayName}?")
        }, text = {
            Text("This will attempt to revoke this permission across all ${uiState.apps.size} installed apps holding it.")
        }, confirmButton = {
            Button(
                onClick = { viewModel.bulkRevokeCurrentGroup() },
                colors = ButtonDefaults.buttonColors(containerColor = RiskCriticalColor)
            ) {
                Text("Revoke Across All")
            }
        }, dismissButton = {
            TextButton(onClick = { viewModel.showBulkRevokeConfirmation(false) }) {
                Text("Cancel")
            }
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permission Explorer", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh(force = true) },
                        modifier = Modifier.testTag("btn_refresh_perm_centric")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                })
        }, snackbarHost = { SnackbarHost(snackbarHostState) }, modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Horizontal selector of permission categories
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sensitiveGroups.forEach { group ->
                        val isSelected = uiState.selectedGroup == group
                        val icon = getPermissionGroupIcon(group)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectGroup(group) },
                            label = { Text(group.displayName) },
                            leadingIcon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("chip_${group.name.lowercase()}")
                        )
                    }
                }

                // Group summary card
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = uiState.selectedGroup.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${uiState.apps.size} apps have this permission granted",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = uiState.selectedGroup.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Apps holding this permission
                if (uiState.isLoading && uiState.apps.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.apps.isEmpty() && !uiState.isRefreshing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No apps have ${uiState.selectedGroup.displayName} granted.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.apps, key = { it.packageName }) { app ->
                            ElevatedCard(
                                onClick = { onNavigateToAppDetail(app.packageName) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("perm_app_${app.packageName}")
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {

                                    // Main content
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AppIconImage(
                                            packageName = app.packageName,
                                            modifier = Modifier.size(42.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp)
                                        ) {
                                            Text(
                                                text = app.appName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = app.packageName,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Spacer(modifier = Modifier.height(18.dp))
                                        }

                                        /*                            Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )*/
                                    }

                                    // Independent badge: bottom-right
                                    RiskBadge(
                                        riskLevel = app.riskLevel,
                                        score = app.riskScore,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(bottom = 8.dp),
                                        bottomCornerRound = true,
                                    )


                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(end = 12.dp, bottom = 12.dp)
                                            .size(14.dp)
                                            .rotate(45f)

                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            if (uiState.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .testTag("perm_centric_refresh_progress")
                )
            }
        }
    }
}
