package com.example.domain.usecase

import com.example.core.PermissionGroupType
import com.example.core.Result
import com.example.core.RiskLevel
import com.example.domain.model.AppInfo
import com.example.domain.model.AppPermission
import com.example.domain.model.PermissionUsageRecord
import com.example.domain.model.SpecialAccessItem
import com.example.domain.repository.AppPermissionRepository
import com.example.domain.repository.PermissionController
import kotlinx.coroutines.flow.Flow

class GetInstalledAppsUseCase(
    private val repository: AppPermissionRepository
) {
    operator fun invoke(): Flow<List<AppInfo>> = repository.observeInstalledApps()

    suspend fun refresh(force: Boolean = true, targetedPackage: String? = null): Result<List<AppInfo>> {
        return repository.refreshInstalledApps(force = force, targetedPackage = targetedPackage)
    }
}

class GetAppPermissionsUseCase(
    private val repository: AppPermissionRepository
) {
    fun observeApp(packageName: String): Flow<AppInfo?> {
        return repository.observeAppDetails(packageName)
    }

    fun observePermissions(packageName: String): Flow<List<AppPermission>> {
        return repository.observeAppPermissions(packageName)
    }

    suspend fun refreshApp(packageName: String): Result<Pair<AppInfo, List<AppPermission>>?> {
        return repository.refreshSinglePackage(packageName)
    }

    suspend operator fun invoke(packageName: String): Pair<AppInfo?, List<AppPermission>> {
        val app = repository.getAppDetails(packageName)
        val permissions = repository.getAppPermissions(packageName)
        return Pair(app, permissions)
    }

    suspend fun getSpecialAccess(packageName: String): List<SpecialAccessItem> {
        return repository.getSpecialAccessItems(packageName)
    }
}

class SetPermissionStateUseCase(
    private val controller: PermissionController,
    private val repository: AppPermissionRepository
) {
    suspend operator fun invoke(
        packageName: String,
        permission: String,
        enable: Boolean
    ): Result<Boolean> {
        val result = if (enable) {
            controller.grantPermission(packageName, permission)
        } else {
            controller.revokePermission(packageName, permission)
        }

        // Always re-read real system state into Room
        val freshPairResult = repository.refreshSinglePackage(packageName)
        val freshPair = (freshPairResult as? Result.Success)?.data
        val isVerified = freshPair?.second?.find { it.permission == permission }?.isGranted == enable

        return if (result is Result.Success && result.data && !isVerified) {
            Result.Error(IllegalStateException("Permission change was not applied"))
        } else if (result is Result.Success && !result.data && controller.isPrivilegedMode() && !isVerified) {
            Result.Error(IllegalStateException("Permission change was not applied"))
        } else if (result is Result.Success) {
            Result.Success(isVerified)
        } else {
            result
        }
    }
}

class RevokeAllPermissionsUseCase(
    private val controller: PermissionController,
    private val repository: AppPermissionRepository
) {
    suspend operator fun invoke(
        packageName: String,
        permissionsToRevoke: List<String>
    ): Result<Int> {
        if (permissionsToRevoke.isEmpty()) {
            return Result.Success(0)
        }
        repository.saveUndoSnapshot(packageName, permissionsToRevoke)
        val result = controller.revokeAllPermissions(packageName, permissionsToRevoke)
        repository.refreshSinglePackage(packageName)
        return result
    }
}

class UndoRevokeUseCase(
    private val controller: PermissionController,
    private val repository: AppPermissionRepository
) {
    suspend operator fun invoke(packageName: String): Result<Int> {
        val snapshot = repository.getLatestUndoSnapshot(packageName)
            ?: return Result.Error(IllegalStateException("No undo snapshot found for $packageName"))

        val permsToRestore = snapshot.permissionsJson
            .removeSurrounding("[", "]")
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotBlank() }

        var restoredCount = 0
        for (perm in permsToRestore) {
            val grantResult = controller.grantPermission(packageName, perm)
            if (grantResult is Result.Success && grantResult.data) {
                restoredCount++
            }
        }
        repository.deleteUndoSnapshot(snapshot.id)
        repository.refreshSinglePackage(packageName)
        return Result.Success(restoredCount)
    }
}

class GetPermissionUsageUseCase(
    private val repository: AppPermissionRepository
) {
    suspend operator fun invoke(): List<PermissionUsageRecord> {
        return repository.getPermissionUsageHistory()
    }
}

class GetAppsByPermissionUseCase(
    private val repository: AppPermissionRepository
) {
    fun observe(group: PermissionGroupType): Flow<List<AppInfo>> {
        return repository.observeAppsByPermissionGroup(group)
    }

    suspend operator fun invoke(group: PermissionGroupType): List<AppInfo> {
        return repository.getAppsByPermissionGroup(group)
    }

    suspend fun refreshGroup(group: PermissionGroupType, force: Boolean = false) {
        repository.refreshInstalledApps(force = force)
    }
}

enum class AppFilter {
    ALL,
    USER,
    SYSTEM,
    RISKY
}

enum class AppSort {
    NAME_ASC,
    NAME_DESC,
    PERMISSIONS_COUNT_DESC,
    RISK_DESC
}

class SearchFilterUseCase {
    operator fun invoke(
        apps: List<AppInfo>,
        query: String,
        filter: AppFilter,
        sort: AppSort
    ): List<AppInfo> {
        return apps
            .asSequence()
            .filter { app ->
                if (query.isBlank()) true
                else app.appName.contains(query, ignoreCase = true) ||
                        app.packageName.contains(query, ignoreCase = true)
            }
            .filter { app ->
                when (filter) {
                    AppFilter.ALL -> true
                    AppFilter.USER -> !app.isSystemApp
                    AppFilter.SYSTEM -> app.isSystemApp
                    AppFilter.RISKY -> app.riskLevel == RiskLevel.HIGH || app.riskLevel == RiskLevel.CRITICAL
                }
            }
            .sortedWith { a, b ->
                when (sort) {
                    AppSort.NAME_ASC -> a.appName.compareTo(b.appName, ignoreCase = true)
                    AppSort.NAME_DESC -> b.appName.compareTo(a.appName, ignoreCase = true)
                    AppSort.PERMISSIONS_COUNT_DESC -> b.grantedPermissionsCount.compareTo(a.grantedPermissionsCount)
                    AppSort.RISK_DESC -> b.riskScore.compareTo(a.riskScore)
                }
            }
            .toList()
    }
}

class ExportReportUseCase {
    fun generateJson(apps: List<AppInfo>, permissionsMap: Map<String, List<AppPermission>>): String {
        val sb = StringBuilder()
        sb.append("[\n")
        apps.forEachIndexed { appIndex, app ->
            val perms = permissionsMap[app.packageName] ?: emptyList()
            sb.append("  {\n")
            sb.append("    \"appName\": \"${escapeJson(app.appName)}\",\n")
            sb.append("    \"packageName\": \"${app.packageName}\",\n")
            sb.append("    \"versionName\": \"${app.versionName}\",\n")
            sb.append("    \"isSystemApp\": ${app.isSystemApp},\n")
            sb.append("    \"riskScore\": ${app.riskScore},\n")
            sb.append("    \"riskLevel\": \"${app.riskLevel}\",\n")
            sb.append("    \"permissions\": [\n")
            perms.forEachIndexed { pIndex, p ->
                sb.append("      {\n")
                sb.append("        \"permission\": \"${p.permission}\",\n")
                sb.append("        \"group\": \"${p.group.name}\",\n")
                sb.append("        \"isGranted\": ${p.isGranted},\n")
                sb.append("        \"riskLevel\": \"${p.riskLevel.name}\"\n")
                sb.append("      }${if (pIndex < perms.size - 1) "," else ""}\n")
            }
            sb.append("    ]\n")
            sb.append("  }${if (appIndex < apps.size - 1) "," else ""}\n")
        }
        sb.append("]\n")
        return sb.toString()
    }

    fun generateCsv(apps: List<AppInfo>, permissionsMap: Map<String, List<AppPermission>>): String {
        val sb = StringBuilder()
        sb.append("App Name,Package Name,Version,Type,Risk Score,Risk Level,Permission,Permission Group,Granted,Risk\n")
        apps.forEach { app ->
            val perms = permissionsMap[app.packageName] ?: emptyList()
            if (perms.isEmpty()) {
                sb.append("\"${escapeCsv(app.appName)}\",\"${app.packageName}\",\"${app.versionName}\",\"${if (app.isSystemApp) "System" else "User"}\",${app.riskScore},${app.riskLevel},\"None\",\"NONE\",false,LOW\n")
            } else {
                perms.forEach { p ->
                    sb.append("\"${escapeCsv(app.appName)}\",\"${app.packageName}\",\"${app.versionName}\",\"${if (app.isSystemApp) "System" else "User"}\",${app.riskScore},${app.riskLevel},\"${p.permission}\",\"${p.group.name}\",${p.isGranted},${p.riskLevel.name}\n")
                }
            }
        }
        return sb.toString()
    }

    private fun escapeJson(str: String): String = str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ")
    private fun escapeCsv(str: String): String = str.replace("\"", "\"\"")
}
