# Android Setup — cmp-deep-link

## 1. Dependency

```kotlin
// build.gradle.kts (app)
implementation("io.github.mobilebytelabs:kmp-deep-link:<version>")
```

## 2. Intent Filter (AndroidManifest.xml)

Add to the Activity that should receive your deep link:

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:launchMode="singleTask">

    <!-- Custom URL scheme: myapp://open/... -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="myapp" android:host="open" />
    </intent-filter>

    <!-- Android App Links (https://): requires assetlinks.json on your server -->
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="https" android:host="yourapp.example.com" />
    </intent-filter>

</activity>
```

## 3. Auto-init (zero setup — recommended)

The library ships a `ContentProvider` (`DeepLinkInitProvider`) that registers
`DeepLinkLifecycleObserver` on every `ComponentActivity` automatically via
manifest merger. **You do not need to add any code** — just add the dependency.

Test it works:
```kotlin
// Anywhere after setContent {} or in a composable:
LaunchedEffect(Unit) {
    DeepLinkHandler.incoming.collect { link ->
        println("Received: ${link.raw}")
    }
}
```

## 4. Manual setup (optional)

If you disable manifest merger or need explicit control:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLinkIntent()           // handles launch intent
        setContent { MyApp() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLinkIntent(intent)     // handles while-running intent
    }
}
```

## 5. Test via ADB

```bash
# Custom scheme
adb shell am start -W -a android.intent.action.VIEW \
    -d "myapp://open/product/42" com.example.myapp

# App Link
adb shell am start -W -a android.intent.action.VIEW \
    -d "https://yourapp.example.com/product/42" com.example.myapp
```
