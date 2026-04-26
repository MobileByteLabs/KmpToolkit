# iOS Setup — cmp-deep-link

## 1. Dependency

```kotlin
// In your shared KMP module's build.gradle.kts:
implementation("io.github.mobilebytelabs:kmp-deep-link:<version>")
```

## 2. Info.plist — URL Scheme Registration

```xml
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLName</key>
        <string>com.example.myapp</string>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>myapp</string>
        </array>
    </dict>
</array>
```

## 3. AppDelegate (UIKit)

```swift
func application(_ app: UIApplication, open url: URL,
                 options: [UIApplication.OpenURLOptionsKey: Any] = [:]) -> Bool {
    DeepLinkAppleHelper.shared.handleUrl(url: url.absoluteString)
    return true
}
```

## 4. SwiftUI

```swift
@main
struct MyApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    DeepLinkAppleHelper.shared.handleUrl(url: url.absoluteString)
                }
        }
    }
}
```

## 5. Universal Links (NSUserActivity)

Add `Associated Domains` capability in Xcode → Signing & Capabilities:
```
applinks:yourapp.example.com
```

Then in AppDelegate / SceneDelegate:
```swift
func application(_ application: UIApplication,
                 continue userActivity: NSUserActivity,
                 restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void) -> Bool {
    if userActivity.activityType == NSUserActivityTypeBrowsingWeb,
       let url = userActivity.webpageURL {
        DeepLinkAppleHelper.shared.handleUserActivity(webpageUrl: url.absoluteString)
    }
    return true
}
```

## 6. Collect in Shared Code

```kotlin
// In shared ViewModel or composable:
LaunchedEffect(Unit) {
    DeepLinkHandler.incoming.collect { link ->
        // react to link
    }
}
```

## Note: tvOS / watchOS

These platforms do not support URL scheme handling or Universal Links.
`DeepLinkHandler.handle()` is still callable but no OS delivery occurs.
