This is a Kotlin Multiplatform project targeting Android, iOS, Desktop (JVM).

Demonstrates the `cmp-network-monitor` library:
- Real-time online/offline status with color indicator
- Rich network info (type, metered, bandwidth)
- Live event log showing Connected/Disconnected/TypeChanged/MeteredChanged events

### Run

```shell
# Android
./gradlew :samples:sample-network-monitor:composeApp:assembleDebug

# Desktop (JVM)
./gradlew :samples:sample-network-monitor:composeApp:run

# iOS
# Open in Xcode and run
```
