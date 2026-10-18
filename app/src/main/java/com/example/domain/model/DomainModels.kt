package com.example.domain.model

import com.example.core.PermissionGroupType
import com.example.core.RiskLevel

data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val iconUri: String = "",
    val firstInstallTime: Long = 0L,
    val lastUpdateTime: Long = 0L,
    val totalPermissionsCount: Int = 0,
    val grantedPermissionsCount: Int = 0,
    val sensitiveGrantedCount: Int = 0,
    val riskScore: Int = 0,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val sensitiveGroups: List<PermissionGroupType> = emptyList(),
    val lastCheckedAt: Long = System.currentTimeMillis()
)

data class AppPermission(
    val permission: String,
    val packageName: String,
    val group: PermissionGroupType,
    val isGranted: Boolean,
    val isRuntime: Boolean = true,
    val isSpecialAccess: Boolean = false,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val userFriendlyName: String = "",
    val description: String = "",
    val lastUsedEpochMs: Long? = null,
    val lastCheckedAt: Long = System.currentTimeMillis()
)

data class SpecialAccessItem(
    val id: String,
    val name: String,
    val description: String,
    val isGranted: Boolean,
    val riskLevel: RiskLevel,
    val settingsAction: String
)

data class PermissionUsageRecord(
    val packageName: String,
    val appName: String,
    val permission: String,
    val group: PermissionGroupType,
    val lastAccessEpochMs: Long
)

data class UndoSnapshot(
    val id: Long = 0L,
    val packageName: String,
    val timestamp: Long,
    val permissionsJson: String
)

enum class ControllerMode {
    SHIZUKU_PRIVILEGED,
    SETTINGS_REDIRECT
}

data class ModeStatus(
    val mode: ControllerMode,
    val isShizukuRunning: Boolean,
    val hasShizukuPermission: Boolean,
    val message: String
)
