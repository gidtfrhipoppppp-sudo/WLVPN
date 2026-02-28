package com.example.wlvpn.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.context.GlobalContext

class UpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    companion object {
        const val TAG = "UpdateWorker"
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Performing update check")
        return try {
            // fetch VPN configs using repository from Koin
            val koin = GlobalContext.get()
            val repository: com.example.wlvpn.data.repository.VpnRepository = koin.get()
            val configs = repository.fetchVpnConfigs()
            Log.i(TAG, "Fetched ${configs.size} configs during auto-update")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during auto-update", e)
            Result.retry()
        }
    }
}
