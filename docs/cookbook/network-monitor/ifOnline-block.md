---
title: "How do I run a block only when online?"
reviewed_by:
  date: 2026-06
  version: 3.5.x
---

# How do I run a block only when online?

## Quick start (minimal MWE)

```kotlin
import com.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitorProvider
import com.mobilebytelabs.kmptoolkit.networkmonitor.ifOnline

suspend fun syncIfOnline(uploadPayload: suspend () -> Unit) {
    val monitor = NetworkMonitorProvider.get()
    monitor.ifOnline {
        uploadPayload()
    }
}
```

For UI threads that should wait for reconnect:

```kotlin
monitor.retryOnReconnect(max = 3) { uploadPayload() }
```

## Caveats / per-platform notes

- `ifOnline { }` is a one-shot gate — it samples `isOnline.value` at call time
  and runs the block (or no-ops) without subscribing. For "wait until online
  then run," use `monitor.ensureOnline()` then your block.
- `retryOnReconnect()` only retries on transitions Unavailable→Available; it
  does NOT retry on application-level errors thrown by your block.
- On `wasmJs`, the underlying StateFlow updates only when the browser tab is
  focused — a backgrounded tab can miss transitions.

## Related

- Module: [cmp-network-monitor](../../modules/cmp-network-monitor.md)
- Sample: [`samples/sample-cmp-network-monitor/composeApp/.../RetryDemo.kt`](https://github.com/MobileByteLabs/KmpToolkit/tree/development/samples/sample-cmp-network-monitor)
- See also: [React to the device going offline](react-to-offline.md) for reactive patterns.
