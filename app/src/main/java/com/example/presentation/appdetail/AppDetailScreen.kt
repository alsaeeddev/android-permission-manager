package com.example.presentation.appdetail

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.PermissionGroupType
import com.example.domain.model.AppInfo
import com.example.domain.model.AppPermission
import com.example.domain.model.ControllerMode
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
fun AppDetailScreen(
    viewModel: AppDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSpecialAccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    OnResumeEffect {
        viewModel.refresh(force = true)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AppDetailUiEvent.ShowSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed && event.actionLabel == "UNDO") {
                        viewModel.undoRevoke()
                    }
                }
            }
        }
    }

    if (uiState.showRevokeAllDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showRevokeAllConfirmation(false) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = RiskCriticalColor
                )
            },
            title = {
                Text("Revoke All Permissions?")
            },
            text = {
                Text("This will revoke all granted permissions for this app. You can restore them using the Undo action.")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.revokeAllPermissions() },
                    colors = ButtonDefaults.buttonColors(containerColor = RiskCriticalColor),
                    modifier = Modifier.testTag("btn_confirm_revoke_all")
                ) {
                    Text("Revoke All")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showRevokeAllConfirmation(false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.appInfo?.appName ?: "App Details",
                        maxLines = 1,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh(force = true) },
                        modifier = Modifier.testTag("btn_refresh_detail")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isRefreshing || uiState.isToggling) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detail_refresh_progress")
                )
            }

            if (uiState.isLoading && uiState.appInfo == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val app = uiState.appInfo
                val groupedPermissions = remember(uiState.permissions) {
                    uiState.permissions.groupBy { it.group }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("app_detail_lazy_column"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (app != null) {
                        item {
                            AppDetailHeaderCard(
                                app = app,
                                lastUpdatedTime = uiState.lastUpdatedTime,
                                timeFormat = timeFormat,
                                onOpenSystemSettings = { viewModel.openAppDetailsSettings() }
                            )
                        }

                        item {
                            AppDetailActions(
                                grantedCount = app.grantedPermissionsCount,
                                onRevokeAllClick = { viewModel.showRevokeAllConfirmation(true) },
                                onSpecialAccessClick = { onNavigateToSpecialAccess(app.packageName) },
                                isPrivileged = uiState.modeStatus.mode == ControllerMode.SHIZUKU_PRIVILEGED
                            )
                        }
                    }

                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (uiState.modeStatus.mode == ControllerMode.SHIZUKU_PRIVILEGED) {
                                        "Shizuku Active: Toggles immediately execute pm grant/revoke commands."
                                    } else {
                                        "Standard Mode: Toggles redirect to Android system settings to review and confirm changes."
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Declared Permissions (${uiState.permissions.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    groupedPermissions.forEach { (group, perms) ->
                        item(key = group.name) {
                            PermissionGroupSection(
                                group = group,
                                permissions = perms,
                                onToggle = { perm, enable ->
                                    viewModel.togglePermission(
                                        perm,
                                        enable
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppDetailHeaderCard(
    app: AppInfo,
    lastUpdatedTime: Long,
    timeFormat: SimpleDateFormat,
    onOpenSystemSettings: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppIconImage(
                        packageName = app.packageName,
                        modifier = Modifier.size(60.dp)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.appName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = app.packageName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "v${app.versionName} • ${if (app.isSystemApp) "System App" else "User Installed"}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // RiskBadge(riskLevel = app.riskLevel, score = app.riskScore)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (app.firstInstallTime > 0) {
                            Text(
                                text = "Installed: ${dateFormat.format(Date(app.firstInstallTime))}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (lastUpdatedTime > 0) {
                            Text(
                                text = "Last checked: ${timeFormat.format(Date(lastUpdatedTime))}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.testTag("detail_last_checked_text")
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenSystemSettings,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_open_app_info")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("App Info", fontSize = 12.sp)
                    }
                }
            }

            RiskBadge(
                riskLevel = app.riskLevel, score = app.riskScore,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(bottom = 8.dp),
                bottomCornerRound = true,
            )
        }
    }
}

@Composable
fun AppDetailActions(
    grantedCount: Int,
    onRevokeAllClick: () -> Unit,
    onSpecialAccessClick: () -> Unit,
    isPrivileged: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalButton(
            onClick = onRevokeAllClick,
            enabled = grantedCount > 0,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = RiskCriticalColor.copy(alpha = 0.15f),
                contentColor = RiskCriticalColor
            ),
            modifier = Modifier
                .weight(1f)
                .testTag("btn_revoke_all")
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Revoke All ($grantedCount)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        FilledTonalButton(
            onClick = onSpecialAccessClick,
            modifier = Modifier
                .weight(1f)
                .testTag("btn_special_access")
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Special Access", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PermissionGroupSection(
    group: PermissionGroupType,
    permissions: List<AppPermission>,
    onToggle: (String, Boolean) -> Unit
) {
    val icon = getPermissionGroupIcon(group)

    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = group.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${permissions.count { it.isGranted }}/${permissions.size} Granted",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            permissions.forEach { perm ->
                PermissionRowItem(
                    permission = perm,
                    onToggle = { enable -> onToggle(perm.permission, enable) }
                )
                if (perm != permissions.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun PermissionRowItem(
    permission: AppPermission,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = permission.userFriendlyName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    RiskBadge(riskLevel = permission.riskLevel)
                }

                Text(
                    text = permission.description,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = permission.isGranted,
                onCheckedChange = { onToggle(it) },
                modifier = Modifier.testTag("switch_${permission.permission}")
            )
        }
    }
}
