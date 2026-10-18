package com.example

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.data.listener.SystemPermissionListener
import com.example.data.worker.ScanAppsWorker
import com.example.di.AppContainer
import com.example.di.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class PermoraApplication : Application() {

    lateinit var container: AppContainer
        private set

    private var systemPermissionListener: SystemPermissionListener? = null
    private val appScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        container = DefaultAppContainer(this)

        try {
            systemPermissionListener = SystemPermissionListener(this, container.appPermissionRepository, appScope).also {
                it.register()
            }
        } catch (e: Throwable) {
            Timber.w(e, "Could not register system permission listener")
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                Timber.d("ProcessLifecycleOwner ON_START: Refreshing permission status")
                appScope.launch {
                    try {
                        container.appPermissionRepository.refreshInstalledApps(force = true)
                    } catch (e: Throwable) {
                        Timber.w(e, "Failed refreshing installed apps on process start")
                    }
                }
            }
        })

        try {
            ScanAppsWorker.schedule(this, 24)
        } catch (e: Throwable) {
            Timber.w(e, "Could not schedule WorkManager task")
        }
    }
}
