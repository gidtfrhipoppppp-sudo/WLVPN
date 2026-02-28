package com.example.wlvpn.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * DNSCrypt Service that handles encrypted DNS
 * Provides DNS privacy through DNSCrypt protocol
 */
class DNSCryptService : Service() {

    private val binder = DNSCryptBinder()
    private var dnscryptProcess: Process? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var isRunning = false
    
    companion object {
        private const val TAG = "DNSCryptService"
        const val DNSCRYPT_PORT = 53
        const val DNSCRYPT_LOCAL_PORT = 5353
        
        // Популярные DNSCrypt серверы
        const val DEFAULT_RESOLVER = "2.dnscrypt-cert.cloudflare.com"
        const val ALTERNATE_RESOLVER = "2.dnscrypt-cert.cisco.com"
    }

    inner class DNSCryptBinder : Binder() {
        fun getService(): DNSCryptService = this@DNSCryptService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            val resolverIntent = intent?.getStringExtra("resolver") ?: DEFAULT_RESOLVER
            startDNSCrypt(resolverIntent)
        }
        return START_STICKY
    }

    private suspend fun startDNSCrypt(resolver: String) {
        if (isRunning) {
            Log.w(TAG, "DNSCrypt already running")
            return
        }

        try {
            Log.i(TAG, "Starting DNSCrypt with resolver: $resolver")
            
            // Создаем директорию для данных DNSCrypt
            val dnscryptDataDir = File(filesDir, "dnscrypt")
            if (!dnscryptDataDir.exists()) {
                dnscryptDataDir.mkdirs()
            }

            // Конфигурация DNSCrypt
            val configContent = """
                # DNSCrypt Конфигурация
                listen_addresses = ["127.0.0.1:$DNSCRYPT_LOCAL_PORT"]
                
                [[static]]
                stamp = "sdns://${buildStamp(resolver)}"
                
                [log]
                level = 2
                file = "${dnscryptDataDir.absolutePath}/dnscrypt.log"
            """.trimIndent()

            // Сохраняем конфигурацию
            val configFile = File(dnscryptDataDir, "dnscrypt-proxy.toml")
            configFile.writeText(configContent)

            // Запускаем DNSCrypt
            val pb = ProcessBuilder(
                "/system/bin/sh", "-c",
                "dnscrypt-proxy -config ${configFile.absolutePath}"
            )
            pb.redirectErrorStream(true)
            pb.directory(dnscryptDataDir)
            
            dnscryptProcess = pb.start()
            isRunning = true
            
            // Читаем логи в отдельном потоке
            scope.launch {
                val reader = BufferedReader(InputStreamReader(dnscryptProcess!!.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    Log.d(TAG, line!!)
                }
            }
            
            Log.i(TAG, "DNSCrypt started successfully on port $DNSCRYPT_LOCAL_PORT")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting DNSCrypt", e)
            isRunning = false
        }
    }

    private fun buildStamp(resolver: String): String {
        // Простая конструкция DNSStamp для целей демонстрации
        // В реальном приложении нужно использовать правильный формат
        return resolver.replace(".", "-").lowercase()
    }

    fun stopDNSCrypt() {
        scope.launch {
            try {
                Log.i(TAG, "Stopping DNSCrypt...")
                dnscryptProcess?.destroy()
                dnscryptProcess = null
                isRunning = false
                Log.i(TAG, "DNSCrypt stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping DNSCrypt", e)
            }
        }
    }

    fun isDNSCryptRunning(): Boolean {
        return isRunning
    }

    fun getDNSCryptLocalServer(): String {
        return "127.0.0.1:$DNSCRYPT_LOCAL_PORT"
    }

    fun getAvailableResolvers(): List<String> {
        return listOf(
            DEFAULT_RESOLVER,
            ALTERNATE_RESOLVER,
            "1.1.1.1",  // Cloudflare
            "8.8.8.8",  // Google
            "1.0.0.1"   // Альтернативный Cloudflare
        )
    }

    override fun onDestroy() {
        stopDNSCrypt()
        scope.cancel()
        super.onDestroy()
    }
}
