package com.example.wlvpn.data.repository

import com.example.wlvpn.data.api.GitHubVpnApi
import com.example.wlvpn.data.models.VpnConfig
import com.example.wlvpn.data.models.VpnServer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Base64

class VpnRepository(private val githubApi: GitHubVpnApi) {
    
    private val _vpnServers = MutableStateFlow<List<VpnServer>>(emptyList())
    val vpnServers: Flow<List<VpnServer>> = _vpnServers.asStateFlow()
    
    private val _selectedServer = MutableStateFlow<VpnServer?>(null)
    val selectedServer: Flow<VpnServer?> = _selectedServer.asStateFlow()

    suspend fun fetchVpnConfigs(): Result<List<VpnConfig>> = try {
        val contents = githubApi.getVpnConfigs()
        val configs = mutableListOf<VpnConfig>()
        
        for (content in contents) {
            if (content.type == "file" && (content.name.endsWith(".ovpn") || content.name.endsWith(".conf"))) {
                try {
                    val fileContent = githubApi.getVpnConfigFile(content.path)
                    val configText = fileContent.content?.let {
                        // Decode base64 content
                        String(Base64.getDecoder().decode(it))
                    } ?: ""
                    
                    val server = parseServerFromConfig(content.name, configText)
                    if (server != null) {
                        configs.add(server)
                    }
                } catch (e: Exception) {
                    // Continue with next config
                }
            }
        }
        
        _vpnServers.value = configs.map { 
            VpnServer(
                id = it.id,
                name = it.name,
                country = it.country
            )
        }
        
        Result.success(configs)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun selectServer(server: VpnServer) {
        _selectedServer.value = server
    }

    private fun parseServerFromConfig(filename: String, configContent: String): VpnConfig? {
        return try {
            val name = filename.replace(".ovpn", "").replace(".conf", "")
            val parts = name.split("-")
            val country = if (parts.size > 1) parts[0] else "Russia"
            val city = if (parts.size > 1) parts.joinToString("-") { it.capitalize() } else name
            
            // Extract server address from config
            val remoteRegex = """remote\s+([^\s]+)\s+(\d+)""".toRegex()
            val remoteMatch = remoteRegex.find(configContent)
            
            if (remoteMatch != null) {
                val serverAddress = remoteMatch.groupValues[1]
                val port = remoteMatch.groupValues[2].toIntOrNull() ?: 1194
                
                VpnConfig(
                    id = filename.hashCode().toString(),
                    name = city,
                    country = country,
                    city = city,
                    protocol = if (configContent.contains("proto tcp")) "TCP" else "UDP",
                    configContent = configContent,
                    serverAddress = serverAddress,
                    port = port
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveVpnConfig(config: VpnConfig) {
        // Implementation for saving config locally
    }

    suspend fun getLastUsedServer(): VpnServer? {
        // Implementation for retrieving last used server
        return null
    }
}
