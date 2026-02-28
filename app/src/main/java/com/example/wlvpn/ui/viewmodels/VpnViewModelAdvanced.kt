package com.example.wlvpn.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.wlvpn.data.models.VpnConfig
import com.example.wlvpn.data.repository.VpnRepository
import com.example.wlvpn.service.VpnManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.Exception

/**
 * ViewModel для управления VPN подключением с поддержкой OpenVPN, Tor и DNSCrypt
 */
class VpnViewModel(
    private val repository: VpnRepository,
    private val vpnManager: VpnManager,
    private val updateManager: com.example.wlvpn.service.UpdateManager
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(val configs: List<VpnConfig>) : UiState()
        data class Error(val message: String) : UiState()
    }

    data class ConnectionState(
        val isConnected: Boolean = false,
        val connectionType: VpnManager.ConnectionType = VpnManager.ConnectionType.OPENVPN,
        val currentConfig: VpnConfig? = null,
        val status: String = "Disconnected",
        val torInfo: String = "",
        val dnsInfo: String = "",
        val isLoading: Boolean = false
    )

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _selectedConnectionType = MutableStateFlow<VpnManager.ConnectionType>(VpnManager.ConnectionType.OPENVPN)
    val selectedConnectionType: StateFlow<VpnManager.ConnectionType> = _selectedConnectionType.asStateFlow()

    private val _selectedDnsResolver = MutableStateFlow("")
    val selectedDnsResolver: StateFlow<String> = _selectedDnsResolver.asStateFlow()

    private val _darkMode = MutableStateFlow(false)
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _byeByDpi = MutableStateFlow(false)
    val byeByDpi: StateFlow<Boolean> = _byeByDpi.asStateFlow()

    private val _autoUpdateEnabled = MutableStateFlow(false)
    val autoUpdateEnabled: StateFlow<Boolean> = _autoUpdateEnabled.asStateFlow()

    private val _autoUpdateInterval = MutableStateFlow(24L)
    val autoUpdateInterval: StateFlow<Long> = _autoUpdateInterval.asStateFlow()

    init {
        fetchVpnConfigs()
        // load persisted settings
        viewModelScope.launch {
            vpnManager.getConnectionTypeFlow().collect { type ->
                _selectedConnectionType.value = type
            }
        }
        viewModelScope.launch {
            vpnManager.getDnsResolverFlow().collect { resolver ->
                _selectedDnsResolver.value = resolver
            }
        }
        viewModelScope.launch {
            vpnManager.getDarkModeFlow().collect { enabled ->
                _darkMode.value = enabled
            }
        }
        viewModelScope.launch {
            vpnManager.getByeByDpiFlow().collect { enabled ->
                _byeByDpi.value = enabled
            }
        }
        viewModelScope.launch {
            vpnManager.getAutoUpdateEnabledFlow().collect { enabled ->
                _autoUpdateEnabled.value = enabled
                updateManager.scheduleAutoUpdate(enabled, _autoUpdateInterval.value)
            }
        }
        viewModelScope.launch {
            vpnManager.getAutoUpdateIntervalFlow().collect { hours ->
                _autoUpdateInterval.value = hours
                if (_autoUpdateEnabled.value) {
                    updateManager.scheduleAutoUpdate(true, hours)
                }
            }
        }
    }

    fun fetchVpnConfigs() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val configs = repository.fetchVpnConfigs()
                // merge with any custom configs stored in preferences
                val custom = vpnManager.getCustomConfigsFlow().first()
                _uiState.value = UiState.Success(configs + custom)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Import a VPN config text provided by user (Ovpn or conf). Parses minimal
     * metadata and persists via VpnManager.
     */
    fun importCustomConfig(rawConfig: String) {
        viewModelScope.launch {
            val parsed = parseConfig(rawConfig)
            if (parsed != null) {
                vpnManager.addCustomConfig(parsed)
                // refresh list
                fetchVpnConfigs()
            }
        }
    }

    private fun parseConfig(configContent: String): VpnConfig? {
        return try {
            // attempt to extract a name and remote host/port
            val remoteRegex = """remote\s+([^\s]+)\s+(\d+)""".toRegex()
            val remoteMatch = remoteRegex.find(configContent)
            val serverAddress = remoteMatch?.groupValues?.get(1) ?: "custom"
            val port = remoteMatch?.groupValues?.get(2)?.toIntOrNull() ?: 1194
            val name = serverAddress
            val country = "Custom"
            VpnConfig(
                id = configContent.hashCode().toString(),
                name = name,
                country = country,
                city = name,
                protocol = if (configContent.contains("proto tcp")) "TCP" else "UDP",
                configContent = configContent,
                serverAddress = serverAddress,
                port = port
            )
        } catch (_: Exception) {
            null
        }
    }

    fun setConnectionType(type: VpnManager.ConnectionType) {
        _selectedConnectionType.value = type
        viewModelScope.launch {
            vpnManager.saveConnectionType(type)
        }
    }

    fun setDnsResolver(resolver: String) {
        _selectedDnsResolver.value = resolver
        viewModelScope.launch {
            vpnManager.saveDnsResolver(resolver)
        }
    }

    fun connectVpn(config: VpnConfig? = null) {
        viewModelScope.launch {
            _connectionState.value = _connectionState.value.copy(isLoading = true)
            
            try {
                val connectionType = _selectedConnectionType.value
                val configString = config?.let { 
                    // Конвертируем конфиг в строку для передачи в сервис
                    it.config
                } ?: ""

                val success = vpnManager.startConnection(connectionType, configString)
                
                if (success) {
                    _connectionState.value = _connectionState.value.copy(
                        isConnected = true,
                        connectionType = connectionType,
                        currentConfig = config,
                        status = "Connected to ${connectionType.name}",
                        torInfo = if (connectionType.name.contains("TOR")) vpnManager.getTorProxyInfo() else "",
                        dnsInfo = if (connectionType.name.contains("DNSCRYPT")) vpnManager.getDNSCryptInfo() else "",
                        isLoading = false
                    )
                } else {
                    _connectionState.value = _connectionState.value.copy(
                        isLoading = false,
                        status = "Connection failed"
                    )
                }
            } catch (e: Exception) {
                _connectionState.value = _connectionState.value.copy(
                    isLoading = false,
                    status = "Error: ${e.message}"
                )
            }
        }
    }

    fun disconnectVpn() {
        viewModelScope.launch {
            _connectionState.value = _connectionState.value.copy(isLoading = true)
            
            try {
                vpnManager.stopConnection(_connectionState.value.connectionType)
                _connectionState.value = ConnectionState()
            } catch (e: Exception) {
                _connectionState.value = _connectionState.value.copy(
                    isLoading = false,
                    status = "Disconnect error: ${e.message}"
                )
            }
        }
    }

    fun toggleConnection(config: VpnConfig? = null) {
        if (_connectionState.value.isConnected) {
            disconnectVpn()
        } else {
            connectVpn(config)
        }
    }

    fun getAvailableConnectionTypes(): List<VpnManager.ConnectionType> {
        return VpnManager.ConnectionType.values().toList()
    }

    fun setDarkMode(enabled: Boolean) {
        _darkMode.value = enabled
        viewModelScope.launch {
            vpnManager.saveDarkMode(enabled)
        }
    }

    fun setByeByDpi(enabled: Boolean) {
        _byeByDpi.value = enabled
        viewModelScope.launch {
            vpnManager.saveByeByDpi(enabled)
        }
    }

    fun setAutoUpdateEnabled(enabled: Boolean) {
        _autoUpdateEnabled.value = enabled
        viewModelScope.launch {
            vpnManager.saveAutoUpdateEnabled(enabled)
        }
        updateManager.scheduleAutoUpdate(enabled, _autoUpdateInterval.value)
    }

    fun setAutoUpdateInterval(hours: Long) {
        _autoUpdateInterval.value = hours
        viewModelScope.launch {
            vpnManager.saveAutoUpdateInterval(hours)
        }
        if (_autoUpdateEnabled.value) {
            updateManager.scheduleAutoUpdate(true, hours)
        }
    }

    fun getAvailableDnsResolvers(): List<String> {
        return listOf(
            "Cloudflare (Default)",
            "Cisco OpenDNS",
            "Google DNS",
            "Quad9",
            "NextDNS"
        )
    }
}

class VpnViewModelFactory(
    private val repository: VpnRepository,
    private val vpnManager: VpnManager,
    private val updateManager: com.example.wlvpn.service.UpdateManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VpnViewModel::class.java)) {
            return VpnViewModel(repository, vpnManager, updateManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
