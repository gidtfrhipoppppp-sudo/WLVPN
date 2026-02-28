package com.example.wlvpn.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import java.io.File

/**
 * Tor Service that handles Tor proxy connection
 * Manages SOCKS5 proxy for anonymous browsing
 */
class TorService : Service() {

    private val binder = TorBinder()
    private var torProcess: Process? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var isRunning = false
    
    companion object {
        private const val TAG = "TorService"
        const val TOR_SOCKS_PORT = 9050
        const val TOR_CONTROL_PORT = 9051
    }

    inner class TorBinder : Binder() {
        fun getService(): TorService = this@TorService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            startTor()
        }
        return START_STICKY
    }

    private suspend fun startTor() {
        if (isRunning) {
            Log.w(TAG, "Tor already running")
            return
        }

        try {
            Log.i(TAG, "Starting Tor...")
            
            // Создаем директорию для данных Tor
            val torDataDir = File(filesDir, "tor")
            if (!torDataDir.exists()) {
                torDataDir.mkdirs()
            }

            // Конфигурация Tor
            val torConfig = StringBuilder()
            torConfig.append("SocksPort 127.0.0.1:$TOR_SOCKS_PORT\n")
            torConfig.append("ControlPort 127.0.0.1:$TOR_CONTROL_PORT\n")
            torConfig.append("DataDirectory ${torDataDir.absolutePath}\n")
            torConfig.append("PidFile ${torDataDir.absolutePath}/tor.pid\n")
            torConfig.append("Log notice file ${torDataDir.absolutePath}/tor.log\n")
            torConfig.append("RunAsDaemon 0\n")
            torConfig.append("AutomapHostsOnResolve 1\n")

            // Сохраняем конфигурацию
            val configFile = File(torDataDir, "torrc")
            configFile.writeText(torConfig.toString())

            // Запускаем Tor процесс
            val pb = ProcessBuilder("/system/bin/sh", "-c", "tor -f ${configFile.absolutePath}")
            pb.redirectErrorStream(true)
            pb.directory(torDataDir)
            
            torProcess = pb.start()
            isRunning = true
            
            Log.i(TAG, "Tor started successfully on port $TOR_SOCKS_PORT")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Tor", e)
            isRunning = false
        }
    }

    fun stopTor() {
        scope.launch {
            try {
                Log.i(TAG, "Stopping Tor...")
                torProcess?.destroy()
                torProcess = null
                isRunning = false
                Log.i(TAG, "Tor stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping Tor", e)
            }
        }
    }

    fun isTorRunning(): Boolean {
        return isRunning
    }

    fun getTorSocksProxy(): String {
        return "127.0.0.1:$TOR_SOCKS_PORT"
    }

    override fun onDestroy() {
        stopTor()
        scope.cancel()
        super.onDestroy()
    }
}
