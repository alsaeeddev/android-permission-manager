package com.example.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.PermissionGroupType
import com.example.core.RiskLevel
import com.example.domain.model.AppInfo
import com.example.presentation.common.AppIconImage
import com.example.presentation.common.ModeBanner
import com.example.presentation.common.OnResumeEffect
import com.example.presentation.common.RiskBadge
import com.example.presentation.common.getPermissionGroupIcon
import com.example.ui.theme.RiskCriticalColor
import com.example.ui.theme.RiskHighColor
import com.example.ui.theme.RiskLowColor
import com.example.ui.theme.RiskMediumColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToApps: () -> Unit,
    onNavigateToAppDetail: (String) -> Unit,
    onNavigateToPermissionGroup: (PermissionGroupType) -> Unit,
    onOpenShizukuGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    OnResumeEffect {
        viewModel.refresh(force = false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Permora",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            modifier = Modifier.testTag("app_title")
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh(force = true) },
                        modifier = Modifier.testTag("btn_refresh_dashboard")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh installed apps"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading && uiState.totalApps == 0) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.testTag("dashboard_loading"))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Auditing installed permissions...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("dashboard_scroll_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        ModeBanner(
                            modeStatus = uiState.modeStatus,
                            onRequestShizukuPermission = { viewModel.requestShizukuPermission() },
                            onOpenGuide = onOpenShizukuGuide
                        )
                    }

                    item {
                        PrivacyScoreCard(
                            score = uiState.overallPrivacyScore,
                            totalApps = uiState.totalApps,
                            userApps = uiState.userAppsCount,
                            systemApps = uiState.systemAppsCount,
                            lastUpdatedTime = uiState.lastUpdatedTime,
                            timeFormat = timeFormat,
                            onViewApps = onNavigateToApps
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sensitive Permissions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap to inspect apps",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    item {
                        SensitivePermissionsGrid(
                            counts = uiState.sensitiveCounts,
                            onGroupSelected = onNavigateToPermissionGroup
                        )
                    }

                    if (uiState.riskyApps.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = RiskCriticalColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "High Privacy Risk Apps",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${uiState.riskyApps.size} apps",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        items(
                            count = uiState.riskyApps.size,
                            key = { index -> uiState.riskyApps[index].packageName }
                        ) { index ->
                            val app = uiState.riskyApps[index]
                            RiskyAppRowItem(
                                app = app,
                                onClick = { onNavigateToAppDetail(app.packageName) }
                            )
                        }
                    }
                }
            }

            if (uiState.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .testTag("dashboard_refresh_progress")
                )
            }
        }
    }
}

@Composable
fun PrivacyScoreCard(
    score: Int,
    totalApps: Int,
    userApps: Int,
    systemApps: Int,
    lastUpdatedTime: Long,
    timeFormat: SimpleDateFormat,
    onViewApps: () -> Unit
) {
    val (statusLabel, statusColor, advice) = when {
        score >= 80 -> Triple(
            "Excellent Protection",
            RiskLowColor,
            "Most sensitive permissions are restricted."
        )

        score >= 60 -> Triple(
            "Good Privacy",
            RiskMediumColor,
            "Several apps hold sensitive background access."
        )

        score >= 40 -> Triple(
            "Moderate Exposure",
            RiskHighColor,
            "Audit suggested: multiple apps have full hardware access."
        )

        else -> Triple(
            "High Exposure Risk",
            RiskCriticalColor,
            "Immediate review recommended for sensitive data leaks."
        )
    }

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("privacy_score_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Device Privacy Score",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = "$score",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { score / 100f },
                color = statusColor,
                trackColor = statusColor.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = advice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (lastUpdatedTime > 0) {
                    Text(
                        text = "Updated ${timeFormat.format(Date(lastUpdatedTime))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { onViewApps() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text(
                            "Total Apps",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("$totalApps", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column {
                        Text(
                            "User Apps",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("$userApps", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column {
                        Text(
                            "System Apps",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("$systemApps", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "View all apps",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SensitivePermissionsGrid(
    counts: Map<PermissionGroupType, Int>,
    onGroupSelected: (PermissionGroupType) -> Unit
) {
    val groups = listOf(
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

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in groups.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val group1 = groups[i]
                SensitivePermissionItem(
                    group = group1,
                    count = counts[group1] ?: 0,
                    onClick = { onGroupSelected(group1) },
                    modifier = Modifier.weight(1f)
                )

                if (i + 1 < groups.size) {
                    val group2 = groups[i + 1]
                    SensitivePermissionItem(
                        group = group2,
                        count = counts[group2] ?: 0,
                        onClick = { onGroupSelected(group2) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun SensitivePermissionItem(
    group: PermissionGroupType,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = getPermissionGroupIcon(group)
    val color = when (group.defaultRisk) {
        RiskLevel.CRITICAL -> RiskCriticalColor
        RiskLevel.HIGH -> RiskHighColor
        RiskLevel.MEDIUM -> RiskMediumColor
        RiskLevel.LOW -> RiskLowColor
    }

    OutlinedCard(
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("perm_group_${group.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = group.displayName,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = group.displayName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "$count apps granted",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RiskyAppRowItem(
    app: AppInfo,
    onClick: () -> Unit
) {
    OutlinedCard(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("risky_app_${app.packageName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconImage(
                packageName = app.packageName,
                modifier = Modifier.size(44.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "${app.grantedPermissionsCount} granted • ${app.sensitiveGrantedCount} sensitive",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            RiskBadge(riskLevel = app.riskLevel, score = app.riskScore)
        }
    }
}
