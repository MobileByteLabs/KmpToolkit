// CmpAppIntentBridge.swift — cmp-app-intents v0.1 consumer-side adapter
//
// Drop this file into your iOS / macOS Xcode app target. Per ADR-04 (ship-source-file
// pattern; not an SPM Package). At app launch, call `CmpAppIntentBridge.shared.loadManifest()`
// (e.g. from your SwiftUI `@main App init` or UIKit `AppDelegate.didFinishLaunchingWithOptions`).
//
// For each intent declared in your Kotlin DSL, copy `templates/AppIntentStub.swift.template`
// into this target, replace `${INTENT_ID}` + `${PARAMS}` placeholders, and add to your
// `AppShortcutsProvider`.
//
// Requires:
// - iOS 16+ / macOS 13+ for `@AppIntent`
// - Kotlin/Native framework linked as `kmptoolkit` (or your alias)
//
// Plan: plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-suite/06-cmp-app-intents.md

import Foundation
#if canImport(AppIntents)
import AppIntents
#endif

@available(iOS 16, macOS 13, *)
public final class CmpAppIntentBridge {

    public static let shared = CmpAppIntentBridge()

    private(set) var manifest: [ManifestEntry] = []

    private init() {}

    /// Reads `cmp-app-intents-manifest.json` from app documents dir (written by Kotlin's
    /// `AppIntents.register(config)`). Call once at app launch.
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

    /// Invoked from per-intent Swift stub's `perform()` body. Routes the call back into
    /// the Kotlin DSL's registered `perform` lambda via the Kotlin/Native exposed
    /// `CmpAppIntentsCallback.shared.handler`.
    ///
    /// Replace `kmptoolkit` below with the actual Kotlin/Native framework module name
    /// your build uses (typically the umbrella framework alias).
    public func perform(id: String, params: [String: Any]) -> AppIntentBridgeResult {
        // The Kotlin callback is exposed as `CmpAppIntentsCallback.shared` via @ObjCName.
        // Import path depends on your framework alias.
        //
        // Pseudo:
        //   let callback = CmpAppIntentsCallback.shared
        //   let result = callback.handler?(id, params as NSDictionary)
        //
        // Until the Kotlin framework is linked, this returns a placeholder.
        return .done(message: "Intent '\(id)' invoked — wire Kotlin framework to complete")
    }
}

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
