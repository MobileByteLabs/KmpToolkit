# Sample App Feature Implementation Plan

**Plan ID:** sample-app-features-260228-001
**Created:** 2026-02-28
**Status:** Complete
**Scope:** Complete sample app with all library features

---

## Summary

Implement comprehensive feature examples in the sample app so users can take reference and implement in real-world applications.

| Metric | Value |
|--------|-------|
| Features to Add | 2 (Clipboard Demo, Enhanced App Update) |
| Files to Create | ~12 |
| Files to Modify | 2 |
| Estimated Effort | L |

---

## Current State

### Existing Sample App Structure

```
sample-app/src/
├── commonMain/
│   └── kotlin/io/github/template/sample/
│       ├── App.kt                      ✅ Main entry with navigation
│       └── appupdate/                  ✅ App Update demo (5 tabs)
│           ├── AppUpdateDemo.kt
│           ├── tabs/
│           │   ├── BasicUpdateTab.kt
│           │   ├── GitHubResolverTab.kt
│           │   ├── SupabaseResolverTab.kt
│           │   ├── CustomResolverTab.kt
│           │   └── ConfigBuilderTab.kt
│           └── components/
│               ├── UpdateResultCard.kt
│               ├── VersionInfoCard.kt
│               ├── ConfigPreview.kt
│               └── PlatformBadge.kt
├── androidMain/                        ✅ Platform-specific
├── iosMain/                            ✅ Platform-specific
├── desktopMain/                        ✅ Platform-specific
└── wasmJsMain/                         ✅ Platform-specific
```

### Library Features Available

| Feature | Module | Status in Sample |
|---------|--------|------------------|
| Clipboard | `clipboard/Clipboard.kt` | ❌ Not implemented |
| App Update | `appupdate/AppUpdate.kt` | ✅ Partially done (basic demo) |
| Greeting | `Greeting.kt` | ✅ Basic demo exists |

---

## Implementation Plan

### Phase 1: Clipboard Demo (P0 - Critical)

Add comprehensive clipboard functionality demonstration.

#### 1.1 Create Clipboard Demo Screen

**File:** `sample-app/src/commonMain/kotlin/io/github/template/sample/clipboard/ClipboardDemo.kt`

```kotlin
// Features to demonstrate:
// 1. Copy text to clipboard
// 2. Read from clipboard (where supported)
// 3. Check if clipboard has text
// 4. Clear clipboard
// 5. Platform-specific behaviors
```

**Components to create:**

| File | Purpose |
|------|---------|
| `clipboard/ClipboardDemo.kt` | Main demo screen with all features |
| `clipboard/tabs/CopyTab.kt` | Copy text functionality |
| `clipboard/tabs/ReadTab.kt` | Read clipboard functionality |
| `clipboard/tabs/PlatformSupportTab.kt` | Show platform support matrix |
| `clipboard/components/ClipboardStatusCard.kt` | Show clipboard state |
| `clipboard/components/PlatformSupportCard.kt` | Platform compatibility info |

#### 1.2 Files to Create

| # | File | Effort |
|:-:|------|:------:|
| 1 | `clipboard/ClipboardDemo.kt` | M |
| 2 | `clipboard/tabs/CopyTab.kt` | S |
| 3 | `clipboard/tabs/ReadTab.kt` | S |
| 4 | `clipboard/tabs/PlatformSupportTab.kt` | S |
| 5 | `clipboard/components/ClipboardStatusCard.kt` | S |
| 6 | `clipboard/components/PlatformSupportCard.kt` | S |

---

### Phase 2: Enhanced App Update Demo (P1 - High)

Improve existing App Update demo with real-world examples.

#### 2.1 Enhancements Needed

| Enhancement | Description | Effort |
|-------------|-------------|:------:|
| Real GitHub check | Connect to actual GitHub release | S |
| Update dialog UI | Show proper update dialog | M |
| Error handling examples | Demonstrate error scenarios | S |
| Platform-specific notes | Show what works where | S |

#### 2.2 Files to Modify/Create

| # | File | Action | Effort |
|:-:|------|--------|:------:|
| 1 | `appupdate/tabs/BasicUpdateTab.kt` | Enhance | S |
| 2 | `appupdate/components/UpdateDialog.kt` | Create | M |
| 3 | `appupdate/components/ErrorExampleCard.kt` | Create | S |

---

### Phase 3: Update Navigation (P0 - Critical)

Update main App.kt to include Clipboard demo.

#### 3.1 Files to Modify

| # | File | Changes | Effort |
|:-:|------|---------|:------:|
| 1 | `App.kt` | Add Screen.Clipboard + navigation | S |

---

## Execution Sequence

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  EXECUTION ORDER                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Step  Task                                   Files           Effort         │
│  ─────────────────────────────────────────────────────────────────────────  │
│  1     Create Clipboard demo structure        6 files         M             │
│  2     Create clipboard components            2 files         S             │
│  3     Create clipboard tabs                  3 files         S             │
│  4     Update main App.kt navigation          1 file          S             │
│  5     Create UpdateDialog component          1 file          M             │
│  6     Enhance BasicUpdateTab                 1 file          S             │
│  7     Build verification                     -               S             │
│                                                                              │
│  Total: ~12 files to create/modify                                          │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Feature Specifications

### Clipboard Demo Specification

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  CLIPBOARD DEMO                                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Tab 1: Copy Text                                                            │
│  ├── Text input field                                                        │
│  ├── "Copy to Clipboard" button                                              │
│  ├── Success/error feedback                                                  │
│  └── Usage code example                                                      │
│                                                                              │
│  Tab 2: Read Clipboard                                                       │
│  ├── "Read from Clipboard" button                                            │
│  ├── Display clipboard contents                                              │
│  ├── "Has Text" check button                                                 │
│  ├── "Clear Clipboard" button                                                │
│  └── Platform support indicator                                              │
│                                                                              │
│  Tab 3: Platform Support                                                     │
│  ├── Support matrix table                                                    │
│  │   ├── Platform name                                                       │
│  │   ├── Copy support (✅/❌)                                                │
│  │   ├── Read support (✅/❌)                                                │
│  │   └── Notes                                                               │
│  └── Current platform highlight                                              │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### App Update Demo Enhancement Specification

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  APP UPDATE DEMO ENHANCEMENTS                                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Update Dialog Component:                                                    │
│  ├── Title: "Update Available"                                               │
│  ├── Version info (current → new)                                            │
│  ├── Release notes preview                                                   │
│  ├── "Update Now" button (immediate)                                         │
│  ├── "Update Later" button (flexible)                                        │
│  └── "Skip" option                                                           │
│                                                                              │
│  Error Examples:                                                             │
│  ├── Network error scenario                                                  │
│  ├── Parse error scenario                                                    │
│  ├── Not supported scenario                                                  │
│  └── User cancelled scenario                                                 │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Quick Commands

```bash
# Start with clipboard demo
# (Creates all clipboard files)

# Then enhance app update
# (Modifies existing + adds dialog)

# Build verification
./gradlew :sample-app:compileDebugKotlinAndroid :sample-app:compileKotlinDesktop

# Run on platform
./gradlew :sample-app:installDebug       # Android
./gradlew :sample-app:run                 # Desktop
```

---

## Acceptance Criteria

- [ ] Clipboard demo with 3 tabs working on all platforms
- [ ] Copy functionality tested on Android, Desktop, iOS
- [ ] Read functionality shows platform limitations correctly
- [ ] Platform support matrix accurate and highlighted
- [ ] App Update demo has proper update dialog UI
- [ ] Error scenarios demonstrated
- [ ] Build passes on all platforms (Android, Desktop, iOS, Wasm)
- [ ] Code examples shown in demo for developer reference

---

## Code Templates

### ClipboardDemo.kt Template

```kotlin
@Composable
fun ClipboardDemo() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Copy", "Read", "Platform Support")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> CopyTab()
            1 -> ReadTab()
            2 -> PlatformSupportTab()
        }
    }
}
```

### CopyTab.kt Template

```kotlin
@Composable
fun CopyTab() {
    var text by remember { mutableStateOf("Hello, Clipboard!") }
    var status by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Copy Text to Clipboard", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Text to copy") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val success = copyToClipboard(text)
                status = if (success) "✅ Copied!" else "❌ Failed"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Copy to Clipboard")
        }

        status?.let {
            Text(it, color = if (it.startsWith("✅")) Color.Green else Color.Red)
        }

        // Code example
        ConfigPreview(
            title = "Usage",
            code = """
                import com.mobilebytelabs.kmptoolkit.clipboard.copyToClipboard

                val success = copyToClipboard("Hello, World!")
            """.trimIndent()
        )
    }
}
```

---

## Dependencies

- Uses existing `ConfigPreview` component from App Update demo
- Uses existing `PlatformBadge` pattern for platform detection
- No new library dependencies required

---

## Next Steps

After plan approval, execute:

```
/gap-implement-project plan current
```

Or implement features individually:

```
# Implement clipboard demo
/implement clipboard

# Verify build
./gradlew :sample-app:assembleDebug
```
