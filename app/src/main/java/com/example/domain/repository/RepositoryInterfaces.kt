package com.example.domain.repository

import com.example.core.PermissionGroupType
import com.example.core.Result
import com.example.domain.model.AppInfo
import com.example.domain.model.AppPermission
import com.example.domain.model.ModeStatus
import com.example.domain.model.PermissionUsageRecord
import com.example.domain.model.SpecialAccessItem
import com.example.domain.model.UndoSnapshot
import kotlinx.coroutines.flow.Flow

interface PermissionController {
    fun getModeStatus(): ModeStatus
    fun isPrivilegedMode(): Boolean
    fun requestShizukuPermission()
    suspend fun grantPermission(packageName: String, permission: String): Result<Boolean>
    suspend fun revokePermission(packageName: String, permission: String): Result<Boolean>
    suspend fun revokeAllPermissions(packageName: String, permissions: List<String>): Result<Int>
    fun openAppPermissionSettings(packageName: String)
    fun openAppDetailsSettings(packageName: String)
    fun openSpecialAccessSettings(action: String)
}

interface AppPermissionRepository {
    var pendingPackageName: String?
    fun observeInstalledApps(): Flow<List<AppInfo>>
    fun observeAppDetails(packageName: String): Flow<AppInfo?>
    fun observeAppPermissions(packageName: String): Flow<List<AppPermission>>
    fun observeAppsByPermissionGroup(group: PermissionGroupType): Flow<List<AppInfo>>
    suspend fun refreshInstalledApps(force: Boolean = false, targetedPackage: String? = null): Result<List<AppInfo>>
    suspend fun refreshSinglePackage(packageName: String): Result<Pair<AppInfo, List<AppPermission>>?>
    suspend fun getAppDetails(packageName: String): AppInfo?
    suspend fun getAppPermissions(packageName: String): List<AppPermission>
    suspend fun getAppsByPermissionGroup(group: PermissionGroupType): List<AppInfo>
    suspend fun getPermissionUsageHistory(): List<PermissionUsageRecord>
    suspend fun getSpecialAccessItems(packageName: String): List<SpecialAccessItem>
    suspend fun saveUndoSnapshot(packageName: String, grantedPermissions: List<String>): Long
    suspend fun getLatestUndoSnapshot(packageName: String): UndoSnapshot?
    suspend fun deleteUndoSnapshot(id: Long)
    suspend fun syncSinglePackage(packageName: String)
    suspend fun removePackage(packageName: String)
}

interface SettingsRepository {
    val themeMode: Flow<String>
    suspend fun setThemeMode(mode: String)
    val scanFrequencyHours: Flow<Int>
    suspend fun setScanFrequencyHours(hours: Int)
}
