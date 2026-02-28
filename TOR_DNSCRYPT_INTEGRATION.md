# Tor and DNSCrypt Integration Guide

## Overview

The WLVPN Android application now includes integrated support for:
- **Tor**: Anonymous routing through the Tor network
- **DNSCrypt**: Encrypted DNS queries using the DNSCrypt protocol

## Components

### 1. TorService
Located in `service/TorService.kt`

**Features:**
- SOCKS5 proxy on port 9050
- Control port on 9051
- Automatic Tor process management
- Logging and error handling

**Usage:**
```kotlin
val intent = Intent(context, TorService::class.java)
context.startService(intent)
```

### 2. DNSCryptService
Located in `service/DNSCryptService.kt`

**Features:**
- DNS encryption on port 5353 (local)
- Support for multiple DNS resolvers
- Automatic service lifecycle management
- Real-time logging

**Supported Resolvers:**
- Cloudflare (Default: 1.1.1.1)
- Cisco OpenDNS
- Google DNS (8.8.8.8)
- Quad9
- NextDNS

**Usage:**
```kotlin
val intent = Intent(context, DNSCryptService::class.java).apply {
    putExtra("resolver", "2.dnscrypt-cert.cloudflare.com")
}
context.startService(intent)
```

### 3. VpnManager
Located in `service/VpnManager.kt`

**Purpose:** Coordinates all three connection types (OpenVPN, Tor, DNSCrypt)

**Connection Types:**
```kotlin
enum class ConnectionType {
    OPENVPN,                    // Standard VPN
    TOR,                        // Tor proxy only
    DNSCRYPT,                   // Encrypted DNS only
    OPENVPN_WITH_DNSCRYPT,      // VPN + DNS encryption
    OPENVPN_WITH_TOR,           // VPN + Tor routing
    TOR_WITH_DNSCRYPT,          // Tor + DNS encryption
    ALL_COMBINED                // VPN + Tor + DNS (Maximum privacy)
}
```

**Example:**
```kotlin
val vpnManager = VpnManager(context)
vpnManager.startConnection(
    VpnManager.ConnectionType.ALL_COMBINED,
    configString
)
```

## UI Components

### MainVpnScreen
Located in `ui/screens/MainVpnScreen.kt`

**Features:**
- Connection status display
- Connection type selector (dropdown)
- DNS resolver selector (for DNSCrypt)
- VPN config list
- Service information cards
- Main connect/disconnect button

**Composable Components:**
- `ConnectionStatusCard` - Shows connection status
- `ConnectionTypeSelectorCard` - Select connection type
- `DnsResolverSelectorCard` - Choose DNS resolver
- `VpnConfigList` - Available VPN configurations
- `ServiceInfoCard` - Tor/DNSCrypt info
- `MainConnectionButton` - Connect/Disconnect action

## Permissions Required

Add to `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.BIND_VPN_SERVICE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

## Service Configuration

### Tor Configuration (torrc)
```
SocksPort 127.0.0.1:9050
ControlPort 127.0.0.1:9051
DataDirectory /data/data/com.example.wlvpn/files/tor
PidFile /data/data/com.example.wlvpn/files/tor/tor.pid
Log notice file /data/data/com.example.wlvpn/files/tor/tor.log
RunAsDaemon 0
AutomapHostsOnResolve 1
```

### DNSCrypt Configuration (dnscrypt-proxy.toml)
```toml
listen_addresses = ["127.0.0.1:5353"]
[[static]]
stamp = "sdns://..."
[log]
level = 2
file = "/data/data/com.example.wlvpn/files/dnscrypt/dnscrypt.log"
```

## Implementation Example

### Basic Connection
```kotlin
// In your ViewModel or Activity
val vpnManager = VpnManager(context)

// Connect with Tor and DNSCrypt
vpnManager.startConnection(
    VpnManager.ConnectionType.TOR_WITH_DNSCRYPT
)

// Get proxy information
val torProxy = vpnManager.getTorProxyInfo() // "127.0.0.1:9050"
val dnsServer = vpnManager.getDNSCryptInfo() // "127.0.0.1:5353"
```

### Using VpnViewModel
```kotlin
// The ViewModel handles everything
val viewModel = VpnViewModel(repository, vpnManager)

// In Compose:
MainVpnScreen(viewModel)

// Set connection type
viewModel.setConnectionType(VpnManager.ConnectionType.ALL_COMBINED)

// Connect
viewModel.connectVpn(vpnConfig)

// Disconnect
viewModel.disconnectVpn()
```

## Security Considerations

1. **Tor Network**
   - Provides anonymity through multi-hop routing
   - Uses 3-hop circuit by default
   - Slower than direct VPN but higher privacy

2. **DNSCrypt**
   - Encrypts DNS queries
   - Prevents DNS snooping
   - Should be used with HTTPS for full privacy

3. **Combined Mode (ALL_COMBINED)**
   - Maximum privacy: VPN + Tor + DNSCrypt
   - Slower performance
   - Recommended for sensitive data

## Performance Notes

- **OPENVPN**: Fast, moderate privacy
- **TOR**: Slower, high privacy, good for anonymity
- **DNSCRYPT**: Minimal performance impact, adds DNS privacy
- **ALL_COMBINED**: Slowest, maximum privacy

## Troubleshooting

### Tor not starting
- Check `/files/tor/tor.log` for error messages
- Ensure sufficient storage space
- Verify binary architecture compatibility

### DNSCrypt connection fails
- Check resolver availability
- Verify DNS port 5353 is free
- Review `/files/dnscrypt/dnscrypt.log`

### General issues
- Check Android logs: `adb logcat | grep WLVPN`
- Verify all permissions are granted
- Ensure VPN permission in system settings

## Dependencies

```gradle
// Tor
implementation("org.torproject:tor-android-binary:0.4.7.15")

// DNSCrypt
implementation("dnscrypt:dnscrypt-android:2.1.5")

// Security
implementation("androidx.security:security-crypto:1.1.0-alpha06")
```

## Future Enhancements

- [ ] VPN circuit usage statistics
- [ ] Exit node selection for Tor
- [ ] Custom DNS resolver support
- [ ] Connection speed testing
- [ ] Auto-reconnect functionality
- [ ] Data usage monitoring
- [ ] Scheduled connections
- [ ] Kill switch on disconnect
