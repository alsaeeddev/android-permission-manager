package com.example.data.repository

import com.example.core.PermissionConstants
import com.example.core.PermissionGroupType
import com.example.core.Result
import com.example.core.RiskLevel
import com.example.data.datasource.PackageManagerDataSource
import com.example.data.datasource.SpecialAccessDataSource
import com.example.data.local.PermoraDatabase
import com.example.data.local.entity.CachedAppEntity
import com.example.data.local.entity.CachedPermissionEntity
import com.example.data.local.entity.PermissionUsageEntity
import com.example.data.local.entity.UndoSnapshotEntity
import com.example.domain.model.AppInfo
import com.example.domain.model.AppPermission
import com.example.domain.model.PermissionUsageRecord
import com.example.domain.model.SpecialAccessItem
import com.example.domain.model.UndoSnapshot
import com.example.domain.repository.AppPermissionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

class AppPermissionRepositoryImpl(
    private val database: PermoraDatabase,
    private val packageManagerDataSource: PackageManagerDataSource,
    private val specialAccessDataSource: SpecialAccessDataSource
) : AppPermissionRepository {

    override var pendingPackageName: String? = null

    private val appDao = database.appDao()
    private val undoDao = database.undoDao()
    private val usageDao = database.usageDao()
    private val refreshMutex = Mutex()

    override fun observeInstalledApps(): Flow<List<AppInfo>> {
        return appDao.getAllAppsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeAppDetails(packageName: String): Flow<AppInfo?> {
        return appDao.getAppFlow(packageName).map { entity ->
            entity?.toDomain()
        }
    }

    override fun observeAppPermissions(packageName: String): Flow<List<AppPermission>> {
        return appDao.getPermissionsForAppFlow(packageName).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeAppsByPermissionGroup(group: PermissionGroupType): Flow<List<AppInfo>> {
        return appDao.getGrantedPermissionsByGroupFlow(group.name).map { matchingPerms ->
            val packageNames = matchingPerms.map { it.packageName }.distinct()
            val result = mutableListOf<AppInfo>()
            for (pkg in packageNames) {
                val app = appDao.getApp(pkg)
                if (app != null) {
                    result.add(app.toDomain())
                }
            }
            result.sortedBy { it.appName.lowercase() }
        }
    }

    override suspend fun refreshInstalledApps(force: Boolean, targetedPackage: String?): Result<List<AppInfo>> = withContext(Dispatchers.IO) {
        refreshMutex.withLock {
            try {
                val pkgToRefresh = targetedPackage ?: pendingPackageName
                if (pkgToRefresh != null) {
                    refreshSinglePackageInternal(pkgToRefresh)
                    pendingPackageName = null
                }

                val cachedApps = appDao.getAllApps()
                if (!force && cachedApps.isNotEmpty()) {
                    return@withContext Result.Success(cachedApps.map { it.toDomain() })
                }

                val appPairs = packageManagerDataSource.loadAllInstalledApps()
                val appEntities = mutableListOf<CachedAppEntity>()
                val permEntities = mutableListOf<CachedPermissionEntity>()

                appPairs.forEach { (app, perms) ->
                    appEntities.add(app.toEntity())
                    perms.forEach { perm ->
                        permEntities.add(perm.toEntity())
                    }
                }

                appDao.insertApps(appEntities)
                if (permEntities.isNotEmpty()) {
                    appDao.insertPermissions(permEntities)
                }

                // Sync usage
                try {
                    val usageLogs = packageManagerDataSource.queryRecentAppOpsUsage()
                    usageDao.insertUsageLogs(usageLogs.map { it.toEntity() })
                } catch (e: Exception) {
                    Timber.w(e, "Failed to cache AppOps usage logs")
                }

                Result.Success(appEntities.map { it.toDomain() })
            } catch (e: Exception) {
                Timber.e(e, "Failed refreshing installed apps")
                Result.Error(e)
            }
        }
    }

    override suspend fun refreshSinglePackage(packageName: String): Result<Pair<AppInfo, List<AppPermission>>?> = withContext(Dispatchers.IO) {
        try {
            val fresh = refreshSinglePackageInternal(packageName)
            if (fresh != null) {
                Result.Success(fresh)
            } else {
                Result.Success(null)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed refreshing single package $packageName")
            Result.Error(e)
        }
    }

    private suspend fun refreshSinglePackageInternal(packageName: String): Pair<AppInfo, List<AppPermission>>? {
        val fresh = packageManagerDataSource.loadSinglePackage(packageName)
        if (fresh != null) {
            appDao.replaceAppData(fresh.first.toEntity(), fresh.second.map { it.toEntity() })
        }
        return fresh
    }

    override suspend fun getAppDetails(packageName: String): AppInfo? = withContext(Dispatchers.IO) {
        val cached = appDao.getApp(packageName)
        val fresh = packageManagerDataSource.loadSinglePackage(packageName)
        if (fresh != null) {
            appDao.replaceAppData(fresh.first.toEntity(), fresh.second.map { it.toEntity() })
            fresh.first
        } else {
            cached?.toDomain()
        }
    }

    override suspend fun getAppPermissions(packageName: String): List<AppPermission> = withContext(Dispatchers.IO) {
        val cached = appDao.getPermissionsForApp(packageName)
        val fresh = packageManagerDataSource.loadSinglePackage(packageName)
        if (fresh != null) {
            appDao.replaceAppData(fresh.first.toEntity(), fresh.second.map { it.toEntity() })
            fresh.second
        } else {
            cached.map { it.toDomain() }
        }
    }

    override suspend fun getAppsByPermissionGroup(group: PermissionGroupType): List<AppInfo> = withContext(Dispatchers.IO) {
        val matchingPerms = appDao.getGrantedPermissionsByGroup(group.name)
        val packageNames = matchingPerms.map { it.packageName }.distinct()
        val result = mutableListOf<AppInfo>()
        for (pkg in packageNames) {
            val app = appDao.getApp(pkg)
            if (app != null) {
                result.add(app.toDomain())
            }
        }
        result.sortedBy { it.appName.lowercase() }
    }

    override suspend fun getPermissionUsageHistory(): List<PermissionUsageRecord> = withContext(Dispatchers.IO) {
        val cached = usageDao.getRecentUsageLogs(100)
        val fresh = packageManagerDataSource.queryRecentAppOpsUsage()
        if (fresh.isNotEmpty()) {
            usageDao.insertUsageLogs(fresh.map { it.toEntity() })
            fresh
        } else {
            cached.map { it.toDomain() }
        }
    }

    override suspend fun getSpecialAccessItems(packageName: String): List<SpecialAccessItem> {
        return specialAccessDataSource.getSpecialAccessForPackage(packageName)
    }

    override suspend fun saveUndoSnapshot(packageName: String, grantedPermissions: List<String>): Long = withContext(Dispatchers.IO) {
        val json = grantedPermissions.joinToString(prefix = "[\"", separator = "\",\"", postfix = "\"]")
        val entity = UndoSnapshotEntity(
            packageName = packageName,
            timestamp = System.currentTimeMillis(),
            permissionsJson = json
        )
        undoDao.insertSnapshot(entity)
    }

    override suspend fun getLatestUndoSnapshot(packageName: String): UndoSnapshot? = withContext(Dispatchers.IO) {
        undoDao.getLatestSnapshot(packageName)?.let {
            UndoSnapshot(
                id = it.id,
                packageName = it.packageName,
                timestamp = it.timestamp,
                permissionsJson = it.permissionsJson
            )
        }
    }

    override suspend fun deleteUndoSnapshot(id: Long) = withContext(Dispatchers.IO) {
        undoDao.deleteSnapshot(id)
    }

    override suspend fun syncSinglePackage(packageName: String) = withContext(Dispatchers.IO) {
        refreshSinglePackageInternal(packageName)
        Unit
    }

    override suspend fun removePackage(packageName: String) = withContext(Dispatchers.IO) {
        appDao.deleteApp(packageName)
        appDao.deletePermissionsForApp(packageName)
    }

    // Converters
    private fun CachedAppEntity.toDomain(): AppInfo {
        val groups = if (sensitiveGroupsCsv.isBlank()) emptyList()
        else sensitiveGroupsCsv.split(",").mapNotNull {
            try { PermissionGroupType.valueOf(it) } catch (_: Exception) { null }
        }
        val risk = try { RiskLevel.valueOf(riskLevelString) } catch (_: Exception) { RiskLevel.LOW }
        return AppInfo(
            packageName = packageName,
            appName = appName,
            versionName = versionName,
            isSystemApp = isSystemApp,
            iconUri = iconUri,
            firstInstallTime = firstInstallTime,
            lastUpdateTime = lastUpdateTime,
            totalPermissionsCount = totalPermissionsCount,
            grantedPermissionsCount = grantedPermissionsCount,
            sensitiveGrantedCount = sensitiveGrantedCount,
            riskScore = riskScore,
            riskLevel = risk,
            sensitiveGroups = groups,
            lastCheckedAt = lastCheckedAt
        )
    }

    private fun AppInfo.toEntity(): CachedAppEntity {
        return CachedAppEntity(
            packageName = packageName,
            appName = appName,
            versionName = versionName,
            isSystemApp = isSystemApp,
            iconUri = iconUri,
            firstInstallTime = firstInstallTime,
            lastUpdateTime = lastUpdateTime,
            totalPermissionsCount = totalPermissionsCount,
            grantedPermissionsCount = grantedPermissionsCount,
            sensitiveGrantedCount = sensitiveGrantedCount,
            riskScore = riskScore,
            riskLevelString = riskLevel.name,
            sensitiveGroupsCsv = sensitiveGroups.joinToString(",") { it.name },
            lastCheckedAt = lastCheckedAt
        )
    }

    private fun CachedPermissionEntity.toDomain(): AppPermission {
        val group = try { PermissionGroupType.valueOf(groupName) } catch (_: Exception) { PermissionGroupType.OTHER }
        val risk = try { RiskLevel.valueOf(riskLevelString) } catch (_: Exception) { RiskLevel.LOW }
        return AppPermission(
            permission = permission,
            packageName = packageName,
            group = group,
            isGranted = isGranted,
            isRuntime = isRuntime,
            isSpecialAccess = isSpecialAccess,
            riskLevel = risk,
            userFriendlyName = userFriendlyName,
            description = description,
            lastUsedEpochMs = lastUsedEpochMs,
            lastCheckedAt = lastCheckedAt
        )
    }

    private fun AppPermission.toEntity(): CachedPermissionEntity {
        return CachedPermissionEntity(
            packageName = packageName,
            permission = permission,
            groupName = group.name,
            isGranted = isGranted,
            isRuntime = isRuntime,
            isSpecialAccess = isSpecialAccess,
            riskLevelString = riskLevel.name,
            userFriendlyName = userFriendlyName,
            description = description,
            lastUsedEpochMs = lastUsedEpochMs,
            lastCheckedAt = lastCheckedAt
        )
    }

    private fun PermissionUsageEntity.toDomain(): PermissionUsageRecord {
        val group = try { PermissionGroupType.valueOf(groupName) } catch (_: Exception) { PermissionGroupType.OTHER }
        return PermissionUsageRecord(
            packageName = packageName,
            appName = appName,
            permission = permission,
            group = group,
            lastAccessEpochMs = lastAccessEpochMs
        )
    }

    private fun PermissionUsageRecord.toEntity(): PermissionUsageEntity {
        return PermissionUsageEntity(
            packageName = packageName,
            appName = appName,
            permission = permission,
            groupName = group.name,
            lastAccessEpochMs = lastAccessEpochMs
        )
    }
}
