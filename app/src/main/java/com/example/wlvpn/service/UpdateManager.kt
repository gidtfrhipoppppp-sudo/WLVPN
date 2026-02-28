package com.example.wlvpn.service

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class UpdateManager(private val context: Context) {
    companion object {
        private const val WORK_NAME = "auto_update_work"
    }

    fun scheduleAutoUpdate(enabled: Boolean, intervalHours: Long) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(NetworkType.UNMETERED) // minimise data usage
            .build()

        val request = PeriodicWorkRequestBuilder<UpdateWorker>(
            intervalHours, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                PeriodicWorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
