package com.example.wlvpn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.wlvpn.ui.screens.MainVpnScreen
import com.example.wlvpn.ui.theme.WLVPNTheme
import com.example.wlvpn.ui.viewmodels.VpnViewModel
import org.koin.android.ext.android.inject
import org.koin.compose.KoinContext

class MainActivity : ComponentActivity() {
    private val viewModel: VpnViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KoinContext {
                // observe theme setting from viewmodel
                val darkMode by viewModel.darkMode.collectAsState()

                WLVPNTheme(darkTheme = darkMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainVpnScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
