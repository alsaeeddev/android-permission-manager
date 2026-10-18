package com.example.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.PermoraApplication
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ScanAppsWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Timber.i("ScanAppsWorker starting background permissions scan")
        return try {
            val app = applicationContext as? PermoraApplication
            val repo = app?.container?.appPermissionRepository
            if (repo != null) {
                repo.refreshInstalledApps()
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "ScanAppsWorker failed")
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "permora_periodic_scan"

        fun schedule(context: Context, repeatIntervalHours: Long = 24) {
            val request = PeriodicWorkRequestBuilder<ScanAppsWorker>(repeatIntervalHours, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
