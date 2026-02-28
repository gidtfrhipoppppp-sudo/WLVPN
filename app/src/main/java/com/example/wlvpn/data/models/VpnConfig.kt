package com.example.wlvpn.data.models

data class VpnConfig(
    val id: String,
    val name: String,
    val country: String,
    val city: String,
    val protocol: String,
    val configContent: String,
    val serverAddress: String,
    val port: Int,
    val isPremium: Boolean = false
)

data class VpnServer(
    val id: String,
    val name: String,
    val country: String,
    val ping: Int? = null,
    val load: Int? = null,
    val isConnected: Boolean = false
)

data class ConnectionState(
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val currentServer: VpnServer? = null,
    val connectionTime: Long = 0L,
    val error: String? = null
)
