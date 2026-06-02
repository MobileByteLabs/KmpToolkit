---
title: "How do I show different UI on WiFi vs cellular?"
reviewed_by:
  date: 2026-06
  version: 3.5.x
---

# How do I show different UI on WiFi vs cellular?

## Quick start (minimal MWE)

```kotlin
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.mobilebytelabs.kmptoolkit.networkmonitor.NetworkStatus
import com.mobilebytelabs.kmptoolkit.networkmonitor.compose.collectNetworkStatusAsState

@Composable
fun DataSaverNotice() {
    val status: NetworkStatus by collectNetworkStatusAsState()
    when (status) {
        is NetworkStatus.Available -> {
            val s = status as NetworkStatus.Available
            if (s.transports.contains("cellular") && !s.transports.contains("wifi")) {
                Text("On cellular — large downloads paused. Tap to override.")
            }
        }
        is NetworkStatus.CaptivePortal -> Text("Captive portal detected — sign in to continue.")
        is NetworkStatus.Unavailable -> Text("No network.")
    }
}
```

## Caveats / per-platform notes

- **iOS / Android:** transport detection requires `ACCESS_NETWORK_STATE` on
  Android; iOS provides it via NWPath without entitlements.
- **JS / wasmJs:** browsers don't expose transport type — `transports` is always
  `setOf("unknown")` when online; degrade gracefully.
- **Desktop:** transport is "ethernet" on JVM Linux/Windows/macOS if a wired
  interface is the default route, else "wifi"; cellular is not detectable.

## Related

- Module: [cmp-network-monitor-compose](../../modules/cmp-network-monitor-compose.md)
- Sample: [`samples/sample-cmp-network-monitor/composeApp/.../TransportDemo.kt`](https://github.com/MobileByteLabs/KmpToolkit/tree/development/samples/sample-cmp-network-monitor)
