package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_apps")
data class CachedAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val iconUri: String,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val totalPermissionsCount: Int,
    val grantedPermissionsCount: Int,
    val sensitiveGrantedCount: Int,
    val riskScore: Int,
    val riskLevelString: String,
    val sensitiveGroupsCsv: String,
    val lastCheckedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cached_permissions",
    primaryKeys = ["packageName", "permission"]
)
data class CachedPermissionEntity(
    val packageName: String,
    val permission: String,
    val groupName: String,
    val isGranted: Boolean,
    val isRuntime: Boolean,
    val isSpecialAccess: Boolean,
    val riskLevelString: String,
    val userFriendlyName: String,
    val description: String,
    val lastUsedEpochMs: Long?,
    val lastCheckedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "undo_snapshots")
data class UndoSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageName: String,
    val timestamp: Long,
    val permissionsJson: String
)

@Entity(
    tableName = "permission_usage_logs",
    primaryKeys = ["packageName", "permission", "lastAccessEpochMs"]
)
data class PermissionUsageEntity(
    val packageName: String,
    val appName: String,
    val permission: String,
    val groupName: String,
    val lastAccessEpochMs: Long
)
