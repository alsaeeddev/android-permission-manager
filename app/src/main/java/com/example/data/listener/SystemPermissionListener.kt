package com.example.data.listener

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import com.example.domain.repository.AppPermissionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.reflect.Proxy

class SystemPermissionListener(
    private val context: Context,
    private val repository: AppPermissionRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val packageManager: PackageManager = context.packageManager
    private val appOpsManager: AppOpsManager by lazy {
        context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    }

    private var permissionsProxyListener: Any? = null
    var opChangedListener: AppOpsManager.OnOpChangedListener? = null
        private set

    fun register() {
        // 1. Try registering OnPermissionsChangedListener via reflection if supported
        try {
            val listenerClass = Class.forName("android.content.pm.PackageManager\$OnPermissionsChangedListener")
            val proxy = Proxy.newProxyInstance(
                listenerClass.classLoader,
                arrayOf(listenerClass)
            ) { _, method, args ->
                if (method.name == "onPermissionsChanged" && args != null && args.isNotEmpty()) {
                    val uid = args[0] as? Int
                    if (uid != null) {
                        scope.launch {
                            try {
                                val packages = packageManager.getPackagesForUid(uid)
                                packages?.forEach { pkg ->
                                    repository.refreshSinglePackage(pkg)
                                }
                            } catch (e: Exception) {
                                Timber.w(e, "Failed refreshing package for changed uid $uid")
                            }
                        }
                    }
                }
                null
            }
            val addMethod = PackageManager::class.java.getMethod("addOnPermissionsChangeListener", listenerClass)
            addMethod.invoke(packageManager, proxy)
            permissionsProxyListener = proxy
        } catch (e: Throwable) {
            Timber.w("addOnPermissionsChangeListener reflection unavailable: ${e.message}")
        }

        // 2. Register AppOpsManager.OnOpChangedListener for common sensitive ops
        try {
            opChangedListener = AppOpsManager.OnOpChangedListener { op, packageName ->
                scope.launch {
                    try {
                        if (!packageName.isNullOrBlank()) {
                            repository.refreshSinglePackage(packageName)
                        } else {
                            repository.refreshInstalledApps(force = true)
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Failed refreshing package for op change: $op")
                    }
                }
            }

            val watchedOps = listOf(
                AppOpsManager.OPSTR_CAMERA,
                AppOpsManager.OPSTR_RECORD_AUDIO,
                AppOpsManager.OPSTR_FINE_LOCATION,
                AppOpsManager.OPSTR_COARSE_LOCATION,
                AppOpsManager.OPSTR_READ_CONTACTS,
                AppOpsManager.OPSTR_READ_SMS,
                AppOpsManager.OPSTR_READ_CALL_LOG
            )

            val listener = opChangedListener ?: return
            for (op in watchedOps) {
                try {
                    appOpsManager.startWatchingMode(op, null, listener)
                } catch (_: Throwable) {
                    // Restricted op watching on this device/version
                }
            }
        } catch (e: Throwable) {
            Timber.w(e, "AppOps startWatchingMode restricted or unavailable")
        }
    }

    fun unregister() {
        try {
            if (permissionsProxyListener != null) {
                val listenerClass = Class.forName("android.content.pm.PackageManager\$OnPermissionsChangedListener")
                val removeMethod = PackageManager::class.java.getMethod("removeOnPermissionsChangeListener", listenerClass)
                removeMethod.invoke(packageManager, permissionsProxyListener)
            }
        } catch (_: Throwable) {}

        try {
            opChangedListener?.let {
                appOpsManager.stopWatchingMode(it)
            }
        } catch (_: Throwable) {}
    }
}
