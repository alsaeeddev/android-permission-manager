package com.example.data.controller

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import com.example.core.Result
import com.example.domain.model.ControllerMode
import com.example.domain.model.ModeStatus
import com.example.domain.repository.AppPermissionRepository
import com.example.domain.repository.PermissionController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader

class ShizukuPermissionController(
    private val context: Context
) {
    fun isAvailable(): Boolean {
        return try {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
    }

    fun isRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    fun hasPermission(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
    }

    suspend fun executeCommand(command: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            while (errorReader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            val exitCode = process.waitFor()
            Pair(exitCode, output.toString().trim())
        } catch (e: Throwable) {
            Timber.e(e, "Error executing Shizuku command: $command")
            Pair(-1, e.message ?: "Execution failed")
        }
    }

    suspend fun grant(packageName: String, permission: String): Result<Boolean> {
        val (code, out) = executeCommand("pm grant $packageName $permission")
        return if (code == 0) {
            Result.Success(true)
        } else {
            Result.Error(RuntimeException("Grant failed: $out"))
        }
    }

    suspend fun revoke(packageName: String, permission: String): Result<Boolean> {
        val (code, out) = executeCommand("pm revoke $packageName $permission")
        return if (code == 0) {
            Result.Success(true)
        } else {
            Result.Error(RuntimeException("Revoke failed: $out"))
        }
    }

    suspend fun revokeAll(packageName: String, permissions: List<String>): Result<Int> {
        var revokedCount = 0
        for (perm in permissions) {
            val (code, _) = executeCommand("pm revoke $packageName $perm")
            if (code == 0) revokedCount++
        }
        return Result.Success(revokedCount)
    }
}

class SettingsRedirectPermissionController(
    private val context: Context,
    private val repositoryProvider: (() -> AppPermissionRepository?)? = null
) {
    fun openAppPermissionSettings(packageName: String) {
        repositoryProvider?.invoke()?.pendingPackageName = packageName
        try {
            val intent = Intent("android.intent.action.MANAGE_APP_PERMISSIONS").apply {
                putExtra("android.intent.extra.PACKAGE_NAME", packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openAppDetailsSettings(packageName)
        }
    }

    fun openAppDetailsSettings(packageName: String) {
        repositoryProvider?.invoke()?.pendingPackageName = packageName
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openSpecialAccessSettings(action: String) {
        val intent = Intent(action).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Cannot open settings action $action")
        }
    }
}

class CompositePermissionController(
    private val context: Context,
    private val repositoryProvider: (() -> AppPermissionRepository?)? = null
) : PermissionController {

    private val shizukuController = ShizukuPermissionController(context)
    private val redirectController = SettingsRedirectPermissionController(context, repositoryProvider)

    override fun getModeStatus(): ModeStatus {
        val running = shizukuController.isRunning()
        val hasPerm = shizukuController.hasPermission()
        return if (running && hasPerm) {
            ModeStatus(
                mode = ControllerMode.SHIZUKU_PRIVILEGED,
                isShizukuRunning = true,
                hasShizukuPermission = true,
                message = "Shizuku Privileged Mode: Instant grant & revoke enabled without opening system settings."
            )
        } else if (running && !hasPerm) {
            ModeStatus(
                mode = ControllerMode.SETTINGS_REDIRECT,
                isShizukuRunning = true,
                hasShizukuPermission = false,
                message = "Shizuku service detected, but permission not granted. Tap to grant permission."
            )
        } else {
            ModeStatus(
                mode = ControllerMode.SETTINGS_REDIRECT,
                isShizukuRunning = false,
                hasShizukuPermission = false,
                message = "Standard Mode: Redirects to system permission settings. Setup Shizuku for 1-tap toggling."
            )
        }
    }

    override fun isPrivilegedMode(): Boolean = shizukuController.isAvailable()

    override fun requestShizukuPermission() {
        try {
            if (shizukuController.isRunning() && !shizukuController.hasPermission()) {
                Shizuku.requestPermission(1001)
            }
        } catch (e: Throwable) {
            Timber.e(e, "Failed to request Shizuku permission")
        }
    }

    override suspend fun grantPermission(packageName: String, permission: String): Result<Boolean> {
        return if (shizukuController.isAvailable()) {
            shizukuController.grant(packageName, permission)
        } else {
            redirectController.openAppPermissionSettings(packageName)
            Result.Success(false)
        }
    }

    override suspend fun revokePermission(packageName: String, permission: String): Result<Boolean> {
        return if (shizukuController.isAvailable()) {
            shizukuController.revoke(packageName, permission)
        } else {
            redirectController.openAppPermissionSettings(packageName)
            Result.Success(false)
        }
    }

    override suspend fun revokeAllPermissions(packageName: String, permissions: List<String>): Result<Int> {
        return if (shizukuController.isAvailable()) {
            shizukuController.revokeAll(packageName, permissions)
        } else {
            redirectController.openAppPermissionSettings(packageName)
            Result.Success(0)
        }
    }

    override fun openAppPermissionSettings(packageName: String) {
        redirectController.openAppPermissionSettings(packageName)
    }

    override fun openAppDetailsSettings(packageName: String) {
        redirectController.openAppDetailsSettings(packageName)
    }

    override fun openSpecialAccessSettings(action: String) {
        redirectController.openSpecialAccessSettings(action)
    }
}
