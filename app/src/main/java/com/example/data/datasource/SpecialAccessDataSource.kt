package com.example.data.datasource

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.example.core.RiskLevel
import com.example.domain.model.SpecialAccessItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SpecialAccessDataSource(
    private val context: Context,
) {
    private val packageManager: PackageManager = context.packageManager
    private val appOpsManager: AppOpsManager by lazy {
        context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    }
    private val powerManager: PowerManager by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    suspend fun getSpecialAccessForPackage(packageName: String): List<SpecialAccessItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SpecialAccessItem>()

        val uid = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageUid(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageUid(packageName, 0)
            }
        } catch (_: Exception) {
            -1
        }

        val isSelf = (packageName == context.packageName) || (uid == Process.myUid())

        // 1. Overlay (Display over other apps)
        val canDrawOverlay = if (isSelf) {
            Settings.canDrawOverlays(context)
        } else {
            checkAppOpOrPermission(
                packageName = packageName,
                uid = uid,
                opStr = AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                permissionName = Manifest.permission.SYSTEM_ALERT_WINDOW
            )
        }
        list.add(
            SpecialAccessItem(
                id = "overlay",
                name = "Display over other apps",
                description = "Shows floating windows or overlays on top of active screens.",
                isGranted = canDrawOverlay,
                riskLevel = RiskLevel.CRITICAL,
                settingsAction = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
            )
        )

        // 2. Usage Stats Access
        val hasUsageAccess = checkAppOpOrPermission(
            packageName = packageName,
            uid = uid,
            opStr = AppOpsManager.OPSTR_GET_USAGE_STATS,
            permissionName = Manifest.permission.PACKAGE_USAGE_STATS
        )

        list.add(
            SpecialAccessItem(
                id = "usage_stats",
                name = "Usage Data Access",
                description = "Tracks which other apps you use and how frequently you open them.",
                isGranted = hasUsageAccess,
                riskLevel = RiskLevel.HIGH,
                settingsAction = Settings.ACTION_USAGE_ACCESS_SETTINGS
            )
        )

        // 3. Install Unknown Apps
        val canInstallUnknown = if (isSelf) {
            packageManager.canRequestPackageInstalls()
        } else {
            checkAppOpOrPermission(
                packageName = packageName,
                uid = uid,
                opStr = "android:request_install_packages",
                permissionName = Manifest.permission.REQUEST_INSTALL_PACKAGES
            )
        }

        list.add(
            SpecialAccessItem(
                id = "unknown_sources",
                name = "Install Unknown Apps",
                description = "Permits downloading and installing external APK packages directly.",
                isGranted = canInstallUnknown,
                riskLevel = RiskLevel.CRITICAL,
                settingsAction = Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES
            )
        )

        // 4. Notification Listener
        val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
        val hasNotificationListener = enabledListeners.contains(packageName)
        list.add(
            SpecialAccessItem(
                id = "notification_listener",
                name = "Notification Listener Access",
                description = "Reads all incoming notification titles, contents, and OTP messages.",
                isGranted = hasNotificationListener,
                riskLevel = RiskLevel.CRITICAL,
                settingsAction = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            )
        )

        // 5. All Files Access (Android 11+)
        val hasAllFiles = if (isSelf && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            checkAppOpOrPermission(
                packageName = packageName,
                uid = uid,
                opStr = "android:manage_external_storage",
                permissionName = "android.permission.MANAGE_EXTERNAL_STORAGE"
            )
        }
        list.add(
            SpecialAccessItem(
                id = "all_files",
                name = "All Files Access",
                description = "Full read and write access to all shared storage and files on the device.",
                isGranted = hasAllFiles,
                riskLevel = RiskLevel.CRITICAL,
                settingsAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                } else {
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                }
            )
        )

        // 6. Battery Optimization Exemption
        val isIgnoringBattery = powerManager.isIgnoringBatteryOptimizations(packageName)
        list.add(
            SpecialAccessItem(
                id = "battery_optimization",
                name = "Ignore Battery Optimizations",
                description = "Runs unlimited background tasks without system battery throttling.",
                isGranted = isIgnoringBattery,
                riskLevel = RiskLevel.MEDIUM,
                settingsAction = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            )
        )

        list
    }

    private fun checkAppOpOrPermission(
        packageName: String,
        uid: Int,
        opStr: String,
        permissionName: String
    ): Boolean {
        if (uid == -1) return false

        if (uid == Process.myUid()) {
            return try {
                val mode = checkOpRawCompat(appOpsManager, opStr, uid, packageName)
                mode == AppOpsManager.MODE_ALLOWED
            } catch (_: Exception) {
                false
            }
        }

        // Calling unsafeCheckOpRaw on another UID requires UPDATE_APP_OPS_STATS permission.
        // Catch SecurityException so non-system app execution never crashes.
        try {
            val mode = checkOpRawCompat(appOpsManager, opStr, uid, packageName)
            if (mode == AppOpsManager.MODE_ALLOWED) return true
        } catch (_: SecurityException) {
            // Expected for non-system caller without UPDATE_APP_OPS_STATS
        } catch (_: Exception) {
            // Any other platform exception
        }

        return isPermissionGrantedForPackage(packageName, permissionName)
    }

    private fun checkOpRawCompat(appOpsManager: AppOpsManager, opStr: String, uid: Int, packageName: String): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpRaw(opStr, uid, packageName)
        } else {
            @Suppress("DEPRECATION")
            appOpsManager.checkOpNoThrow(opStr, uid, packageName)
        }
    }

    private fun isPermissionGrantedForPackage(packageName: String, permission: String): Boolean {
        return try {
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }
            val requestedPerms = pkgInfo.requestedPermissions ?: return false
            val requestedFlags = pkgInfo.requestedPermissionsFlags ?: IntArray(0)
            for (i in requestedPerms.indices) {
                if (requestedPerms[i] == permission) {
                    return if (i < requestedFlags.size) {
                        (requestedFlags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                    } else false
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }
}
