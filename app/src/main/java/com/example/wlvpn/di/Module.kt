package com.example.wlvpn.di

import android.content.Context
import com.example.wlvpn.data.api.GitHubVpnApi
import com.example.wlvpn.data.repository.VpnRepository
import com.example.wlvpn.domain.usecase.FetchVpnConfigsUseCase
import com.example.wlvpn.service.VpnManager
import com.example.wlvpn.ui.viewmodels.VpnViewModel
import com.example.wlvpn.ui.viewmodels.VpnViewModelFactory
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val appModule = module {
    
    // OkHttp Client
    single {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Retrofit
    single {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // GitHub API
    single {
        get<Retrofit>().create(GitHubVpnApi::class.java)
    }

    // Repository
    single {
        VpnRepository(get())
    }

    // Use Cases
    single {
        FetchVpnConfigsUseCase(get())
    }

    // VPN Manager
    single {
        VpnManager(androidContext())
    }

    // Update manager for scheduling auto‑update jobs
    single {
        UpdateManager(androidContext())
    }

    // ViewModels
    viewModel {
        VpnViewModel(
            repository = get(),
            vpnManager = get(),
            updateManager = get()
        )
    }
}
