package com.example.wlvpn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wlvpn.data.models.VpnConfig
import com.example.wlvpn.service.VpnManager
import com.example.wlvpn.ui.viewmodels.VpnViewModel

/**
 * Main VPN configuration screen with support for OpenVPN, Tor, and DNSCrypt
 */
@Composable
fun MainVpnScreen(
    viewModel: VpnViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val selectedConnectionType by viewModel.selectedConnectionType.collectAsState()
    val selectedDnsResolver by viewModel.selectedDnsResolver.collectAsState()
    val darkMode by viewModel.darkMode.collectAsState()
    val byeByDpi by viewModel.byeByDpi.collectAsState()
    val autoUpdateEnabled by viewModel.autoUpdateEnabled.collectAsState()
    val autoUpdateInterval by viewModel.autoUpdateInterval.collectAsState()

    var showConnectionTypeMenu by remember { mutableStateOf(false) }
    var showDnsResolverMenu by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top bar with settings
        TopAppBar(
            title = { Text("WLVPN Client") },
            actions = {
                IconButton(onClick = { showSettingsDialog = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        )

        // Connection Status Card
        ConnectionStatusCard(connectionState)

        // Connection Type Selection
        ConnectionTypeSelectorCard(
            selectedConnectionType = selectedConnectionType,
            onTypeSelected = { viewModel.setConnectionType(it) }
        )

        // DNS Resolver Selection
        if (selectedConnectionType.name.contains("DNSCRYPT")) {
            DnsResolverSelectorCard(
                selectedResolver = selectedDnsResolver,
                onResolverSelected = { viewModel.setDnsResolver(it) }
            )
        }

        // Connection Info
        when (uiState) {
            is VpnViewModel.UiState.Loading -> {
                LoadingIndicator()
            }
            is VpnViewModel.UiState.Success -> {
                val configs = (uiState as VpnViewModel.UiState.Success).configs
                VpnConfigList(
                    configs = configs,
                    onConfigSelected = { viewModel.connectVpn(it) },
                    isConnected = connectionState.isConnected
                )
            }
            is VpnViewModel.UiState.Error -> {
                ErrorCard((uiState as VpnViewModel.UiState.Error).message)
            }
        }

        // Connection Button
        MainConnectionButton(
            isConnected = connectionState.isConnected,
            isLoading = connectionState.isLoading,
            onClick = { viewModel.toggleConnection() }
        )

        // Info Cards for Active Services
        if (connectionState.isConnected) {
            if (connectionState.torInfo.isNotEmpty()) {
                ServiceInfoCard("Tor Proxy", connectionState.torInfo)
            }
            if (connectionState.dnsInfo.isNotEmpty()) {
                ServiceInfoCard("DNSCrypt", connectionState.dnsInfo)
            }
        }
    }

    // show dialog outside of main column
    if (showSettingsDialog) {
        SettingsDialog(
            currentConnectionType = selectedConnectionType,
            currentResolver = selectedDnsResolver,
            darkMode = darkMode,
            byeByDpi = byeByDpi,
            onConnectionTypeChange = { viewModel.setConnectionType(it) },
            onResolverChange = { viewModel.setDnsResolver(it) },
            onDarkModeChange = { viewModel.setDarkMode(it) },
            onByeByDpiChange = { viewModel.setByeByDpi(it) },
            autoUpdateEnabled = autoUpdateEnabled,
            autoUpdateInterval = autoUpdateInterval,
            onAutoUpdateEnabledChange = { viewModel.setAutoUpdateEnabled(it) },
            onAutoUpdateIntervalChange = { viewModel.setAutoUpdateInterval(it) },
            onImportConfig = { showImportDialog = true },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
fun ConnectionStatusCard(connectionState: VpnViewModel.ConnectionState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (connectionState.isConnected)
                Color(0xFF4CAF50) else Color(0xFFF44336)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (connectionState.isConnected)
                    Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                connectionState.status,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                connectionState.connectionType.name,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun ConnectionTypeSelectorCard(
    selectedConnectionType: VpnManager.ConnectionType,
    onTypeSelected: (VpnManager.ConnectionType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Connection Type",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Box {
                Button(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    )
                ) {
                    Text(selectedConnectionType.name)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    VpnManager.ConnectionType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name) },
                            onClick = {
                                onTypeSelected(type)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                getConnectionTypeDescription(selectedConnectionType),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun DnsResolverSelectorCard(
    selectedResolver: String,
    onResolverSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val resolvers = listOf(
        "Cloudflare",
        "Cisco OpenDNS",
        "Google DNS",
        "Quad9",
        "NextDNS"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "DNS Resolver",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Box {
                Button(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF9C27B0)
                    )
                ) {
                    Text(selectedResolver.ifEmpty { "Select Resolver" })
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    resolvers.forEach { resolver ->
                        DropdownMenuItem(
                            text = { Text(resolver) },
                            onClick = {
                                onResolverSelected(resolver)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    currentConnectionType: VpnManager.ConnectionType,
    currentResolver: String,
    darkMode: Boolean,
    byeByDpi: Boolean,
    autoUpdateEnabled: Boolean,
    autoUpdateInterval: Long,
    onConnectionTypeChange: (VpnManager.ConnectionType) -> Unit,
    onResolverChange: (String) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onByeByDpiChange: (Boolean) -> Unit,
    onAutoUpdateEnabledChange: (Boolean) -> Unit,
    onAutoUpdateIntervalChange: (Long) -> Unit,
    onImportConfig: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ConnectionTypeSelectorCard(
                    selectedConnectionType = currentConnectionType,
                    onTypeSelected = onConnectionTypeChange
                )
                DnsResolverSelectorCard(
                    selectedResolver = currentResolver,
                    onResolverSelected = onResolverChange
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Dark theme")
                    Switch(
                        checked = darkMode,
                        onCheckedChange = onDarkModeChange
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Enable ByeByDPI")
                    Switch(
                        checked = byeByDpi,
                        onCheckedChange = onByeByDpiChange
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Auto-update configs")
                    Switch(
                        checked = autoUpdateEnabled,
                        onCheckedChange = onAutoUpdateEnabledChange
                    )
                }
                if (autoUpdateEnabled) {
                    // simple interval selector
                    val options = listOf(24L to "Every day", 168L to "Every week")
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(options.first { it.first == autoUpdateInterval }.second)
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            options.forEach { (hours, label) ->
                                DropdownMenuItem(text = { Text(label) }, onClick = {
                                    onAutoUpdateIntervalChange(hours)
                                    expanded = false
                                })
                            }
                        }
                    }
                }
                Button(onClick = onImportConfig, modifier = Modifier.fillMaxWidth()) {
                    Text("Import custom config")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
fun ImportConfigDialog(
    onImport: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var configText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import VPN Config") },
        text = {
            Column {
                Text("Paste your .ovpn or .conf below:")
                TextField(
                    value = configText,
                    onValueChange = { configText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onImport(configText); onDismiss() }) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun VpnConfigList(
    configs: List<VpnConfig>,
    onConfigSelected: (VpnConfig) -> Unit,
    isConnected: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Available VPN Configs",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(configs.take(5)) { config ->
                    VpnConfigItem(
                        config = config,
                        onClick = { onConfigSelected(config) },
                        isHighlighted = isConnected
                    )
                }
            }
        }
    }
}

@Composable
fun VpnConfigItem(
    config: VpnConfig,
    onClick: () -> Unit,
    isHighlighted: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isHighlighted) Color(0xFFE3F2FD) else Color.White),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VpnLock,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    config.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    config.protocol ?: "OpenVPN",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun MainConnectionButton(
    isConnected: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isConnected) Color(0xFFF44336) else Color(0xFF4CAF50)
        ),
        shape = RoundedCornerShape(12.dp),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                imageVector = if (isConnected) Icons.Default.Close else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isConnected) "Disconnect" else "Connect",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ServiceInfoCard(title: String, info: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(info, fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = Color(0xFFF44336),
                modifier = Modifier.size(24.dp)
            )
            Text(
                message,
                color = Color(0xFFC62828),
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun getConnectionTypeDescription(type: VpnManager.ConnectionType): String {
    return when (type) {
        VpnManager.ConnectionType.OPENVPN -> "Connect via OpenVPN protocol"
        VpnManager.ConnectionType.TOR -> "Route traffic through Tor network"
        VpnManager.ConnectionType.DNSCRYPT -> "Use encrypted DNS (DNSCrypt)"
        VpnManager.ConnectionType.OPENVPN_WITH_DNSCRYPT -> "OpenVPN + Encrypted DNS"
        VpnManager.ConnectionType.OPENVPN_WITH_TOR -> "OpenVPN + Tor network routing"
        VpnManager.ConnectionType.TOR_WITH_DNSCRYPT -> "Tor network + Encrypted DNS"
        VpnManager.ConnectionType.ALL_COMBINED -> "OpenVPN + Tor + DNSCrypt (Maximum Privacy)"
    }
}
