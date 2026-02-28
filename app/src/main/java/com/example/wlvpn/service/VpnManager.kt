package com.example.wlvpn.service

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlin.runCatching

/**
 * VPN Manager that coordinates OpenVPN, Tor, and DNSCrypt
 */
class VpnManager(private val context: Context) {

    companion object {
        private const val TAG = "VpnManager"
        private const val CONNECTION_TYPE_KEY = "connection_type"
        private const val RESOLVER_KEY = "dns_resolver"
        private const val DARK_MODE_KEY = "dark_mode"
        private const val BYE_BYP_DPI_KEY = "bye_by_dpi"
        private const val AUTO_UPDATE_ENABLED_KEY = "auto_update_enabled"
        private const val AUTO_UPDATE_INTERVAL_KEY = "auto_update_interval"
        private const val CUSTOM_CONFIGS_KEY = "custom_configs"
        private const val AUTO_UPDATE_ENABLED_KEY = "auto_update_enabled"
        private const val AUTO_UPDATE_INTERVAL_KEY = "auto_update_interval"
    }

    enum class ConnectionType {
        OPENVPN,
        TOR,
        DNSCRYPT,
        OPENVPN_WITH_DNSCRYPT,
        OPENVPN_WITH_TOR,
        TOR_WITH_DNSCRYPT,
        ALL_COMBINED
    }

    /**
     * Settings helpers
     */
    fun getConnectionTypeFlow(): Flow<ConnectionType> {
        return context.dataStore.data.map { prefs ->
            val name = prefs[preferencesKey<String>(CONNECTION_TYPE_KEY)]
                ?: ConnectionType.OPENVPN.name
            ConnectionType.valueOf(name)
        }
    }

    suspend fun saveConnectionType(type: ConnectionType) {
        context.dataStore.edit { prefs ->
            prefs[preferencesKey<String>(CONNECTION_TYPE_KEY)] = type.name
        }
    }

    fun getDnsResolverFlow(): Flow<String> {
        return context.dataStore.data.map { prefs ->
            prefs[preferencesKey<String>(RESOLVER_KEY)] ?: ""
        }
    }

    suspend fun saveDnsResolver(resolver: String) {
        context.dataStore.edit { prefs ->
            prefs[preferencesKey<String>(RESOLVER_KEY)] = resolver
        }
    }

    fun getDarkModeFlow(): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs[preferencesKey<Boolean>(DARK_MODE_KEY)] ?: false
        }
    }

    suspend fun saveDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[preferencesKey<Boolean>(DARK_MODE_KEY)] = enabled
        }
    }
    fun getByeByDpiFlow(): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs[preferencesKey<Boolean>(BYE_BYP_DPI_KEY)] ?: false
        }
    }

    suspend fun saveByeByDpi(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[preferencesKey<Boolean>(BYE_BYP_DPI_KEY)] = enabled
        }
    }


    fun getCustomConfigsFlow(): Flow<List<com.example.wlvpn.data.models.VpnConfig>> {
        return context.dataStore.data.map { prefs ->
            val json = prefs[preferencesKey<String>(CUSTOM_CONFIGS_KEY)] ?: "[]"
            try {
                com.google.gson.Gson().fromJson(
                    json,
                    Array<com.example.wlvpn.data.models.VpnConfig>::class.java
                ).toList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun addCustomConfig(config: com.example.wlvpn.data.models.VpnConfig) {
        context.dataStore.edit { prefs ->
            val json = prefs[preferencesKey<String>(CUSTOM_CONFIGS_KEY)] ?: "[]"
            val list: MutableList<com.example.wlvpn.data.models.VpnConfig> = try {
                com.google.gson.Gson().fromJson(
                    json,
                    Array<com.example.wlvpn.data.models.VpnConfig>::class.java
                ).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            list.add(config)
            prefs[preferencesKey<String>(CUSTOM_CONFIGS_KEY)] =
                com.google.gson.Gson().toJson(list)
        }
    }

    fun getAutoUpdateEnabledFlow(): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            prefs[preferencesKey<Boolean>(AUTO_UPDATE_ENABLED_KEY)] ?: false
        }
    }

    suspend fun saveAutoUpdateEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[preferencesKey<Boolean>(AUTO_UPDATE_ENABLED_KEY)] = enabled
        }
    }

    fun getAutoUpdateIntervalFlow(): Flow<Long> {
        return context.dataStore.data.map { prefs ->
            prefs[preferencesKey<Long>(AUTO_UPDATE_INTERVAL_KEY)] ?: 24L
        }
    }

    suspend fun saveAutoUpdateInterval(hours: Long) {
        context.dataStore.edit { prefs ->
            prefs[preferencesKey<Long>(AUTO_UPDATE_INTERVAL_KEY)] = hours
        }
    }
    /**
     * End settings
     */
    fun startConnection(connectionType: ConnectionType, config: String = ""): Boolean {
        return try {
            // check if ByeByDPI is enabled and include in log/intent
            val byeDpiEnabled = runCatching { context.dataStore.data.first()[preferencesKey<Boolean>(BYE_BYP_DPI_KEY)] ?: false }.getOrDefault(false)
            if (byeDpiEnabled) {
                Log.i(TAG, "ByeByDPI enabled for this connection")
            }
            Log.i(TAG, "Starting connection: $connectionType")
            when (connectionType) {
                ConnectionType.OPENVPN -> startOpenVPN(config)
                ConnectionType.TOR -> startTor()
                ConnectionType.DNSCRYPT -> startDNSCrypt()
                ConnectionType.OPENVPN_WITH_DNSCRYPT -> {
                    startOpenVPN(config) && startDNSCrypt()
                }
                ConnectionType.OPENVPN_WITH_TOR -> {
                    startOpenVPN(config) && startTor()
                }
                ConnectionType.TOR_WITH_DNSCRYPT -> {
                    startTor() && startDNSCrypt()
                }
                ConnectionType.ALL_COMBINED -> {
                    startOpenVPN(config) && startTor() && startDNSCrypt()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting connection", e)
            false
        }
    }

    fun stopConnection(connectionType: ConnectionType? = null) {
        Log.i(TAG, "Stopping connection${connectionType?.let { ": $it" } ?: ""}")
        if (connectionType == null) {
            // Stop all connections
            stopOpenVPN()
            stopTor()
            stopDNSCrypt()
        } else {
            when (connectionType) {
                ConnectionType.OPENVPN, ConnectionType.OPENVPN_WITH_DNSCRYPT, 
                ConnectionType.OPENVPN_WITH_TOR, ConnectionType.ALL_COMBINED -> stopOpenVPN()
                
                ConnectionType.TOR, ConnectionType.OPENVPN_WITH_TOR,
                ConnectionType.TOR_WITH_DNSCRYPT, ConnectionType.ALL_COMBINED -> stopTor()
                
                ConnectionType.DNSCRYPT, ConnectionType.OPENVPN_WITH_DNSCRYPT,
                ConnectionType.TOR_WITH_DNSCRYPT, ConnectionType.ALL_COMBINED -> stopDNSCrypt()
            }
        }
    }

    private fun startOpenVPN(config: String): Boolean {
        return try {
            val intent = Intent(context, VpnService::class.java).apply {
                putExtra("vpn_config", config)
                action = "start_vpn"
            }
            context.startService(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting OpenVPN", e)
            false
        }
    }

    private fun stopOpenVPN() {
        try {
            val intent = Intent(context, VpnService::class.java).apply {
                action = "stop_vpn"
            }
            context.startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping OpenVPN", e)
        }
    }

    private fun startTor(): Boolean {
        return try {
            val intent = Intent(context, TorService::class.java)
            context.startService(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Tor", e)
            false
        }
    }

    private fun stopTor() {
        try {
            val intent = Intent(context, TorService::class.java)
            context.stopService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Tor", e)
        }
    }

    private fun startDNSCrypt(resolver: String = DNSCryptService.DEFAULT_RESOLVER): Boolean {
        return try {
            val intent = Intent(context, DNSCryptService::class.java).apply {
                putExtra("resolver", resolver)
            }
            context.startService(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting DNSCrypt", e)
            false
        }
    }

    private fun stopDNSCrypt() {
        try {
            val intent = Intent(context, DNSCryptService::class.java)
            context.stopService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping DNSCrypt", e)
        }
    }

    fun isConnected(connectionType: ConnectionType): Boolean {
        return when (connectionType) {
            ConnectionType.OPENVPN, ConnectionType.OPENVPN_WITH_DNSCRYPT,
            ConnectionType.OPENVPN_WITH_TOR, ConnectionType.ALL_COMBINED -> true // TODO: Implement check
            
            ConnectionType.TOR, ConnectionType.TOR_WITH_DNSCRYPT, 
            ConnectionType.OPENVPN_WITH_TOR, ConnectionType.ALL_COMBINED -> true // TODO: Implement check
            
            ConnectionType.DNSCRYPT, ConnectionType.OPENVPN_WITH_DNSCRYPT,
            ConnectionType.TOR_WITH_DNSCRYPT, ConnectionType.ALL_COMBINED -> true // TODO: Implement check
        }
    }

    fun getConnectionStatus(): String {
        return "Connected" // TODO: Implement proper status
    }

    fun getTorProxyInfo(): String {
        return "SOCKS5: ${TorService.TOR_SOCKS_PORT}"
    }

    fun getDNSCryptInfo(): String {
        return "DNS: 127.0.0.1:${DNSCryptService.DNSCRYPT_LOCAL_PORT}"
    }
}
