package com.example.wlvpn.domain.usecase

import com.example.wlvpn.data.models.VpnConfig
import com.example.wlvpn.data.repository.VpnRepository

class FetchVpnConfigsUseCase(private val repository: VpnRepository) {
    suspend operator fun invoke(): Result<List<VpnConfig>> {
        return repository.fetchVpnConfigs()
    }
}
