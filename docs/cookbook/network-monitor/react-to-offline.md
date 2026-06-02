---
title: "How do I react to the device going offline?"
reviewed_by:
  date: 2026-06
  version: 3.5.x
---

# How do I react to the device going offline?

## Quick start (minimal MWE)

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.material3.Text
import com.mobilebytelabs.kmptoolkit.networkmonitor.compose.collectIsOnlineAsState

@Composable
fun OfflineBanner() {
    val online by collectIsOnlineAsState()
    if (!online) {
        Text("You're offline — changes will sync when reconnected.")
    }
}
```

## Caveats / per-platform notes

- **JS / wasmJs:** signal source is `navigator.onLine` — known to lie on some
  desktop browsers (returns true even on captive-portal failures); pair with a
  background-ping for high-stakes flows.
- **iOS:** monitors NWPathMonitor on a dedicated dispatch queue; first emission
  arrives ~50ms after first observer subscribes.
- **Linux:** parses `/proc/net/route` for default-gateway presence on a 5s tick.

## Related

- Module: [cmp-network-monitor-compose](../../modules/cmp-network-monitor-compose.md) + [cmp-network-monitor](../../modules/cmp-network-monitor.md)
- Sample: [`samples/sample-cmp-network-monitor/composeApp/.../OfflineDemo.kt`](https://github.com/MobileByteLabs/KmpToolkit/tree/development/samples/sample-cmp-network-monitor)
- See also: [Run a block only when online](ifOnline-block.md) for one-shot patterns.
