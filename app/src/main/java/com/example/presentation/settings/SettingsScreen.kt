package com.example.presentation.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.ControllerMode
import com.example.ui.theme.PrivilegedAccentColor
import com.example.ui.theme.RiskLowColor
import com.example.ui.theme.RiskMediumColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var pendingExportIsJson by remember { mutableStateOf(true) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            if (pendingExportIsJson) "application/json" else "text/csv"
        )
    ) { uri ->
        if (uri != null) {
            viewModel.exportReport(context, uri, pendingExportIsJson)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    // Shizuku Setup Guide Dialog
    if (uiState.showGuideDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setGuideDialogVisible(false) },
            icon = {
                Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = PrivilegedAccentColor)
            },
            title = { Text("Shizuku Wireless Debugging Setup") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Shizuku allows Permora to grant and revoke permissions instantly in 1-tap without requiring Root privileges.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    GuideStepItem(
                        number = "1",
                        title = "Install Shizuku",
                        description = "Get the Shizuku app from GitHub or Google Play Store."
                    )
                    GuideStepItem(
                        number = "2",
                        title = "Enable Developer Options",
                        description = "Go to System Settings > About Phone, and tap 'Build number' 7 times."
                    )
                    GuideStepItem(
                        number = "3",
                        title = "Start via Wireless Debugging",
                        description = "Open Developer options > Enable Wireless Debugging > Pair device with pairing code in Shizuku."
                    )
                    GuideStepItem(
                        number = "4",
                        title = "Authorize Permora",
                        description = "Return to Permora and tap 'Authorize Shizuku' to activate Direct Control mode."
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.setGuideDialogVisible(false) }) {
                    Text("Got It")
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (uiState.showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setPrivacyDialogVisible(false) },
            icon = {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            title = { Text("Permora Privacy Guarantee") },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "• Zero Internet Permissions: Permora does not request the INTERNET permission in its AndroidManifest. It is physically impossible for the app to send data to any remote server.",
                        fontSize = 13.sp
                    )
                    Text(
                        "• 100% On-Device Audit: All permission calculations, AppOps queries, and database caches reside exclusively in local encrypted app storage.",
                        fontSize = 13.sp
                    )
                    Text(
                        "• Zero Analytics & Trackers: No third-party SDKs, crash analytics, or advertising telemetry are embedded in this application.",
                        fontSize = 13.sp
                    )
                    Text(
                        "• User Sovereignty: You maintain complete control over export and deletion of local audit records.",
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.setPrivacyDialogVisible(false) }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Privacy", fontWeight = FontWeight.Bold) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Section
            item {
                ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Appearance", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark").forEach { (mode, label) ->
                                FilterChip(
                                    selected = uiState.themeMode == mode,
                                    onClick = { viewModel.setTheme(mode) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("theme_chip_${mode.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }

            // Shizuku Privileged Mode Section
            item {
                val isPrivileged = uiState.modeStatus.mode == ControllerMode.SHIZUKU_PRIVILEGED
                ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = PrivilegedAccentColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Privileged Mode (Shizuku)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Surface(
                                color = if (isPrivileged) RiskLowColor.copy(alpha = 0.15f) else RiskMediumColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isPrivileged) "Active" else "Inactive",
                                    color = if (isPrivileged) RiskLowColor else RiskMediumColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.modeStatus.message,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.setGuideDialogVisible(true) },
                                modifier = Modifier.testTag("btn_view_guide")
                            ) {
                                Text("Setup Guide", fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            FilledTonalButton(
                                onClick = {
                                    if (uiState.modeStatus.isShizukuRunning && !uiState.modeStatus.hasShizukuPermission) {
                                        viewModel.requestShizukuPermission()
                                    } else {
                                        viewModel.refreshModeStatus()
                                    }
                                },
                                modifier = Modifier.testTag("btn_check_shizuku")
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Check Status", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Export Reports Section
            item {
                ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Audit Report", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Save a comprehensive inventory of all apps and declared permission statuses for security audits.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    pendingExportIsJson = true
                                    createDocumentLauncher.launch("permora_audit_report.json")
                                },
                                modifier = Modifier.weight(1f).testTag("btn_export_json")
                            ) {
                                Text("Export JSON", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    pendingExportIsJson = false
                                    createDocumentLauncher.launch("permora_audit_report.csv")
                                },
                                modifier = Modifier.weight(1f).testTag("btn_export_csv")
                            ) {
                                Text("Export CSV", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Background Audits
            item {
                ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Background Scan Schedule", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "WorkManager automatically checks for newly installed apps and updates the local security cache.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(12 to "Every 12h", 24 to "Daily (24h)", 48 to "Every 2 days").forEach { (hours, label) ->
                                FilterChip(
                                    selected = uiState.scanFrequencyHours == hours,
                                    onClick = { viewModel.setScanFrequency(hours) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            // Privacy Policy & About
            item {
                ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Privacy Guarantee", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Permora requires zero internet access. All telemetry and permission inspection is conducted 100% on-device.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        FilledTonalButton(
                            onClick = { viewModel.setPrivacyDialogVisible(true) },
                            modifier = Modifier.testTag("btn_privacy_policy")
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Privacy Policy", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Permora v1.0.0 • Clean Architecture + Jetpack Compose\nDual Mode Shizuku / Settings Redirect Support",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GuideStepItem(
    number: String,
    title: String,
    description: String
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = PrivilegedAccentColor.copy(alpha = 0.2f),
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = number, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrivilegedAccentColor)
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(text = description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
