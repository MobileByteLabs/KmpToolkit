# cmp-app-review

> **Target support:** see [TARGET_MATRIX.md](../TARGET_MATRIX.md) — the single source of truth for
> which KMP targets every module ships and why.

Ask the user to review your app, from `commonMain`, on every Kotlin Multiplatform target.

> **Stable API.** No opt-in annotation.

One call in shared code resolves to the right thing per platform — a native in-app prompt where the
OS has one, your store listing everywhere else, opened through [`cmp-open-url`](../cmp-open-url).

> **Integrating into an existing app?** See [CONSUMPTION.md](CONSUMPTION.md) —
> dependency, where store ids live, DI setup, and replacing a hand-written wrapper.

## Install

```toml
cmp-app-review = { module = "io.github.mobilebytelabs:cmp-app-review", version.ref = "kmptoolkit" }
```

```kotlin
commonMain.dependencies { implementation(libs.cmp.app.review) }
```

`cmp-open-url` comes with it — the store fallback is built on it, so you do not add it separately.

## Use

```kotlin
// once at startup, in commonMain
AppReview.configure(
    StoreListing(
        appStoreId = "1234567890",                  // iOS, macOS, tvOS, watchOS
        microsoftStoreProductId = "9NBLGGH4NNS1",   // Windows
        webUrl = "https://example.com/app",         // desktop, Linux, browser
    ),
)

// anywhere in shared code
when (val result = AppReview.requestReview()) {
    is AppReviewResult.NativeFlowRequested -> Unit            // the OS took it from here
    is AppReviewResult.StoreOpened         -> Unit            // sent to the listing
    is AppReviewResult.NoStoreConfigured   -> Unit            // you have a config gap
    is AppReviewResult.Failed              -> log(result.message)
}
```

`playStorePackage` is optional — Android reads the running app's package when you omit it.

## Dependency injection

Configure it where you set up DI — registering the module *is* the configuration step:

```kotlin
startKoin {
    modules(
        platformModule,
        appReviewModule(
            StoreListing(
                appStoreId = BuildKonfig.APP_STORE_ID,
                webUrl = BuildKonfig.APP_WEB_URL,
            ),
        ),
    )
}
```

Then inject it like any other capability:

```kotlin
class SettingsViewModel(private val review: AppReviewManager) {
    fun onRateTapped() = viewModelScope.launch { review.requestReview() }
}
```

Prefer everything in one module? `module { }` bodies run eagerly, so this is equivalent:

```kotlin
val platformModule = module {
    AppReview.configure(StoreListing(appStoreId = BuildKonfig.APP_STORE_ID))

    single<UrlLauncher> { UrlLauncherImpl() }
    single<ShareManager> { ShareManagerImpl() }
    single<AppReviewManager> { AppReviewManagerImpl() }
}
```

`AppReviewManagerImpl()` takes no arguments — no Activity, no listing — because the listing is
resolved when a review is requested, not when the manager is built. Registration order therefore
does not matter.

> **`appReviewModule()` with no listing resets a previously configured one to `None`.** DI setup is
> the source of truth, so the module applies whatever it was given. Pass the listing to the module,
> or configure inside your own module — don't do both in the other order.

Store ids are deployment data: keep them where your build already keeps app identity (a generated
BuildKonfig constant, an app-profile YAML), so a white-label fork changes a profile value and no code.

## What each target does

| Target | Route |
|---|---|
| Android | Play In-App Review → `market://details?id=…` |
| iOS / macOS | `SKStoreReviewController` → store review composer |
| tvOS / watchOS | store listing (watchOS hands it to the paired iPhone) |
| Windows | `ms-windows-store://review/` → web listing |
| JVM · Linux · JS · wasmJs | web listing in the browser |
| wasmWasi | `NoStoreConfigured` — no store, no browser, no user |

**`NativeFlowRequested` does not mean a prompt appeared.** Neither Play nor StoreKit tells the app
whether anything rendered or what the user did, and both silently no-op once their quota is spent.
Never gate a reward on it, and never show "thanks for rating!" in response. If you need a button that
reliably goes somewhere, call `openStoreListing()`.

## Testing

```kotlin
val review = FakeAppReviewManager()
repeat(3) { viewModel.onExportSucceeded() }
assertEquals(1, review.requestCount)   // asked once, not three times
```

The gating logic is yours and is the part worth testing; the prompt itself is unobservable by design.
Inject `AppReviewManager` where you want that, or `AppReview.configure(fake)` for the global path.

## Requirements

- **Android** — Play In-App Review works only in builds installed by Google Play. Sideloaded and
  debug builds get a failure, which falls through to `market://`. No manual setup: a `ContentProvider`
  captures the app context and an `ActivityLifecycleCallbacks` tracks the Activity the flow needs.
- **iOS 10.3+ / macOS 10.14+** for `SKStoreReviewController`.
