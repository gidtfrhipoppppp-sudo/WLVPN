package com.example.wlvpn.data.api

import com.example.wlvpn.data.models.VpnConfig
import retrofit2.http.GET
import retrofit2.http.Path

interface GitHubVpnApi {
    /**
     * Fetch the list of VPN configs from the Russia VPN configs repository
     * Repository: https://github.com/igareck/vpn-configs-for-russia
     */
    @GET("repos/igareck/vpn-configs-for-russia/contents")
    suspend fun getVpnConfigs(): List<GitHubContent>

    @GET("repos/igareck/vpn-configs-for-russia/contents/{path}")
    suspend fun getVpnConfigFile(@Path("path") path: String): GitHubContent
}

data class GitHubContent(
    val name: String,
    val path: String,
    val type: String,
    val download_url: String?,
    val size: Int?,
    val content: String? // Base64 encoded for files
)
