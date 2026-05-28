// CmpAppIntentBridge.swift — cmp-app-intents v0.2 consumer-side adapter
//
// Drop this file into your iOS / macOS Xcode app target. Per ADR-04 (ship-source-file
// pattern; not an SPM Package). At app launch, call `CmpAppIntentBridge.shared.bootstrap(framework:)`
// (e.g. from your SwiftUI `@main App init` or UIKit `AppDelegate.didFinishLaunchingWithOptions`).
//
// `bootstrap(framework:)` does three things:
//   1. Loads the manifest JSON written by Kotlin's `AppIntents.register(config)`.
//   2. Wires the Kotlin↔Swift callback (`CmpAppIntentsCallback.shared.handler`) so
//      per-intent `@AppIntent.perform()` bodies route back into the Kotlin DSL.
//   3. Indexes every `searchable: true` manifest entry into Spotlight via
//      `CSSearchableIndex.defaultSearchableIndex()` — surfacing your intents in
//      iOS Search + Siri Suggestions.
//
// For each intent declared in your Kotlin DSL, copy `templates/AppIntentStub.swift.template`
// into this target, replace `${INTENT_ID}` + `${PARAMS}` placeholders, and register
// it with your `AppShortcutsProvider`.
//
// Requires:
// - iOS 16+ / macOS 13+ for `@AppIntent`
// - iOS 9+ / macOS 10.13+ for CoreSpotlight (iOS & macOS only — NOT tvOS, NOT watchOS)
// - watchOS 10+ for App Intents (Shortcuts on watchOS — manifest works, Spotlight skipped)
// - tvOS 14+ for App Intents (Siri Suggestions on Apple TV — manifest works, Spotlight skipped)
// - Kotlin/Native framework linked (default alias: `kmptoolkit`)
//
// v0.2 sub-plan 10.C: `CoreSpotlight.framework` is iOS+macOS only. The
// `#if canImport(CoreSpotlight)` guards below let this same file compile for
// tvOS and watchOS targets — indexing is silently skipped on those platforms;
// manifest write + Kotlin callback wiring still work.

import Foundation
#if canImport(CoreSpotlight)
import CoreSpotlight
#endif
#if canImport(UIKit)
import UIKit
#endif

@available(iOS 16, macOS 13, *)
public final class CmpAppIntentBridge {

    public static let shared = CmpAppIntentBridge()

    public private(set) var manifest: [ManifestEntry] = []

    /// Reserved user-activity prefix for Spotlight item taps. Consumer apps must
    /// declare `<prefix>.<intent_id>` strings in their Info.plist `NSUserActivityTypes`
    /// array (e.g. `com.mobilebytelabs.kmptoolkit.appintents.openTransfer`) and forward
    /// the activity to `CmpAppIntentBridge.shared.handleContinue(_:)`.
    public static let activityPrefix = "com.mobilebytelabs.kmptoolkit.appintents"

    /// Callback resolver — wired by `bootstrap(callbackResolver:)`. Returns the
    /// Kotlin/Native `CmpAppIntentsCallback.shared` exposed via `@ObjCName`. Stored
    /// as a closure rather than a hard import so this file compiles cleanly even
    /// when the Kotlin framework alias is renamed by the consumer build.
    private var callbackResolver: (() -> Any?)?

    private init() {}

    // MARK: - One-shot bootstrap

    /// Call once at app launch. Idempotent — re-indexing the same identifiers
    /// overwrites prior Spotlight entries.
    ///
    /// - Parameter callbackResolver: a closure returning the Kotlin/Native
    ///   `CmpAppIntentsCallback.shared` singleton. Example after linking the
    ///   `kmptoolkit` framework:
    ///   ```
    ///   CmpAppIntentBridge.shared.bootstrap {
    ///       CmpAppIntentsCallback.shared
    ///   }
    ///   ```
    public func bootstrap(callbackResolver: @escaping () -> Any?) {
        self.callbackResolver = callbackResolver
        loadManifest()
        indexSpotlightItems()
    }

    // MARK: - Manifest

    public func loadManifest() {
        guard let docsUrl = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first else {
            print("[cmp-app-intents] could not resolve documents dir")
            return
        }
        let manifestUrl = docsUrl.appendingPathComponent("cmp-app-intents-manifest.json")
        guard let data = try? Data(contentsOf: manifestUrl) else {
            print("[cmp-app-intents] manifest file not present — did you call AppIntents.register(config) from Kotlin?")
            return
        }
        do {
            self.manifest = try JSONDecoder().decode([ManifestEntry].self, from: data)
            print("[cmp-app-intents] loaded \(manifest.count) intents from manifest")
        } catch {
            print("[cmp-app-intents] manifest decode failed: \(error)")
        }
    }

    // MARK: - Spotlight indexing (Siri Suggestions + iOS Search)

    /// Indexes every `searchable: true` manifest entry into the default Spotlight
    /// index. Tapping a Spotlight result delivers an `NSUserActivity` whose
    /// `activityType` is `\(activityPrefix).\(intent.id)`. Apps handle these via
    /// SwiftUI `.onContinueUserActivity` or UIKit
    /// `application(_:continue:restorationHandler:)` — then call
    /// `CmpAppIntentBridge.shared.handleContinue(_:)` to route into Kotlin.
    public func indexSpotlightItems() {
        #if canImport(CoreSpotlight)
        let searchable = manifest.filter { $0.searchable }
        guard !searchable.isEmpty else { return }

        let items: [CSSearchableItem] = searchable.map { entry in
            let attrs = CSSearchableItemAttributeSet(contentType: .item)
            attrs.title = entry.title
            attrs.contentDescription = entry.description
            return CSSearchableItem(
                uniqueIdentifier: entry.id,
                domainIdentifier: entry.searchableCategory,
                attributeSet: attrs
            )
        }

        CSSearchableIndex.default().indexSearchableItems(items) { error in
            if let error = error {
                print("[cmp-app-intents] Spotlight indexing failed: \(error)")
            } else {
                print("[cmp-app-intents] indexed \(items.count) Spotlight items")
            }
        }
        #else
        // tvOS / watchOS: CoreSpotlight unavailable — skip indexing silently.
        // App Intents still register via AppShortcutsProvider; Siri Suggestions on
        // tvOS 14+ and watchOS Shortcuts on watchOS 10+ surface from manifest alone.
        print("[cmp-app-intents] Spotlight indexing skipped (CoreSpotlight not available on this platform)")
        #endif
    }

    /// Removes all previously-indexed intents. Useful on user logout or feature-flag
    /// disable. Idempotent. No-op on tvOS / watchOS (no Spotlight to clear).
    public func clearSpotlightIndex() {
        #if canImport(CoreSpotlight)
        guard !manifest.isEmpty else { return }
        let ids = manifest.map { $0.id }
        CSSearchableIndex.default().deleteSearchableItems(withIdentifiers: ids) { error in
            if let error = error {
                print("[cmp-app-intents] Spotlight clear failed: \(error)")
            }
        }
        #endif
    }

    // MARK: - NSUserActivity continuation (from Spotlight tap / Siri / Shortcuts)

    /// Inspect an incoming `NSUserActivity` and, if it matches a registered intent,
    /// invoke the Kotlin `perform` lambda via `CmpAppIntentsCallback.shared.handler`.
    ///
    /// Returns `true` if the activity was handled, allowing the caller to short-circuit
    /// other continuation handlers.
    ///
    /// SwiftUI usage:
    /// ```
    /// .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { _ in /* universal links */ }
    /// .onContinueUserActivity("\(CmpAppIntentBridge.activityPrefix).openTransfer") { activity in
    ///     _ = CmpAppIntentBridge.shared.handleContinue(activity)
    /// }
    /// ```
    @discardableResult
    public func handleContinue(_ activity: NSUserActivity) -> Bool {
        let prefix = Self.activityPrefix
        let type = activity.activityType
        guard type.hasPrefix(prefix + ".") else { return false }
        let intentId = String(type.dropFirst(prefix.count + 1))
        guard manifest.contains(where: { $0.id == intentId }) else { return false }
        let params = (activity.userInfo as? [String: Any]) ?? [:]
        _ = perform(id: intentId, params: params)
        return true
    }

    // MARK: - Kotlin callback bridge

    /// Invoked from per-intent Swift stub's `perform()` body. Routes the call back into
    /// the Kotlin DSL's registered `perform` lambda via the Kotlin/Native exposed
    /// `CmpAppIntentsCallback.shared.handler`.
    public func perform(id: String, params: [String: Any]) -> AppIntentBridgeResult {
        guard let callback = callbackResolver?() else {
            return .failed(message: "cmp-app-intents not bootstrapped — call CmpAppIntentBridge.shared.bootstrap(callbackResolver:) at app launch")
        }
        // The callback is a Kotlin/Native object exposed via @ObjCName("CmpAppIntentsCallback").
        // It carries a `handler: ((String, [String: Any]) -> Any?)?` property. We use KVC to
        // remain framework-alias agnostic — if the Kotlin module is renamed by the consumer,
        // this still resolves via the runtime metadata.
        let handler = (callback as AnyObject).value(forKey: "handler")
        guard handler != nil else {
            return .failed(message: "AppIntents.register(config) not called from Kotlin")
        }

        // Invoke handler.invoke(id, params) via ObjC dynamic dispatch. Kotlin lambdas exposed
        // to ObjC are subclasses of `Kotlin.Function2`; their selector is `invoke:`.
        let selector = NSSelectorFromString("invoke::")
        let target = handler as AnyObject
        guard target.responds(to: selector) else {
            return .failed(message: "Kotlin callback handler does not respond to invoke::")
        }
        let result = target.perform(selector, with: id as NSString, with: params as NSDictionary)?.takeUnretainedValue()
        return mapKotlinResult(result)
    }

    /// Maps a Kotlin `AppIntentResult` (ObjC-exposed as a class with `description`/`message`
    /// properties on the variants) into the Swift `AppIntentBridgeResult` enum.
    ///
    /// The Kotlin types are:
    /// - `AppIntentResultDialog`   → `.dialog(message:)`
    /// - `AppIntentResultSnippet`  → `.snippet(markdown:)`
    /// - `AppIntentResultDone`     → `.done(message: nil)`
    /// - `AppIntentResultFailed`   → `.failed(message:)`
    private func mapKotlinResult(_ raw: Any?) -> AppIntentBridgeResult {
        guard let obj = raw as AnyObject? else {
            return .done(message: nil)
        }
        let className = String(describing: type(of: obj))
        if className.contains("Dialog") {
            return .dialog(message: (obj.value(forKey: "message") as? String) ?? "")
        }
        if className.contains("Snippet") {
            return .snippet(markdown: (obj.value(forKey: "markdown") as? String) ?? "")
        }
        if className.contains("Failed") {
            return .failed(message: (obj.value(forKey: "message") as? String) ?? "Unknown failure")
        }
        return .done(message: nil)
    }
}

// MARK: - Public Swift types

public enum AppIntentBridgeResult {
    case dialog(message: String)
    case snippet(markdown: String)
    case done(message: String?)
    case failed(message: String)
}

public struct ManifestEntry: Codable {
    public let id: String
    public let title: String
    public let description: String
    public let parameters: [ManifestParam]
    public let searchable: Bool
    public let searchableCategory: String?
}

public struct ManifestParam: Codable {
    public let name: String
    public let type: String           // "Text" / "Integer" / "Number" / "Bool" / "Entity:<Name>"
    public let isRequired: Bool
}
