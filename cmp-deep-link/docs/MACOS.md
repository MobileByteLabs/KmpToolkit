# macOS Setup — cmp-deep-link

## 1. Info.plist — URL Scheme Registration

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

## 2. AppKit AppDelegate — NSAppleEventManager

```swift
func applicationWillFinishLaunching(_ notification: Notification) {
    NSAppleEventManager.shared().setEventHandler(
        self,
        andSelector: #selector(handleGetURLEvent(_:withReplyEvent:)),
        forEventClass: AEEventClass(kInternetEventClass),
        andEventID: AEEventID(kAEGetURL)
    )
}

@objc func handleGetURLEvent(_ event: NSAppleEventDescriptor,
                              withReplyEvent: NSAppleEventDescriptor) {
    if let urlString = event.paramDescriptor(forKeyword: AEKeyword(keyDirectObject))?.stringValue {
        DeepLinkAppleHelper.shared.handleUrl(url: urlString)
    }
}
```

## 3. SwiftUI

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

## 4. Test via Terminal

```bash
open "myapp://open/product/42"
```
