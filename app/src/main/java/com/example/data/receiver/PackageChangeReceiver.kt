package com.example.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.PermoraApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        val packageName = intent.data?.schemeSpecificPart ?: return

        Timber.d("Package broadcast received: $action for $packageName")

        val app = context.applicationContext as? PermoraApplication ?: return
        val repo = app.container.appPermissionRepository

        val scope = CoroutineScope(Dispatchers.IO)
        when (action) {
            Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REPLACED -> {
                scope.launch {
                    repo.syncSinglePackage(packageName)
                }
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                if (!replacing) {
                    scope.launch {
                        repo.removePackage(packageName)
                    }
                }
            }
        }
    }
}
