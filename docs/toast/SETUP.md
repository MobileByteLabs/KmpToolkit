# cmp-toast — Integration Guide

> `io.github.mobilebytelabs:kmp-toast:2.1.0`

No Supabase, no DI module, no config class. 3 steps.

---

## Step 1 — Add Gradle Dependency

### `gradle/libs.versions.toml`

```toml
[versions]
kmp-toast = "2.1.0"

[libraries]
kmp-toast = { module = "io.github.mobilebytelabs:kmp-toast", version.ref = "kmp-toast" }
```

### `shared/build.gradle.kts`

```kotlin
commonMain.dependencies {
    implementation(libs.kmp.toast)
}
```

---

## Step 2 — Set Up ToastHost in Root Composable / Scaffold

Place `ToastHost` at the root level so it renders on top of all screens:

### With Scaffold

```kotlin
import com.mobilebytelabs.kmptoolkit.toast.ToastHost
import com.mobilebytelabs.kmptoolkit.toast.rememberToastHostState

@Composable
fun App() {
    val toastState = rememberToastHostState()

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavHost(...)  // or your main content

            ToastHost(hostState = toastState)
        }
    }
}
```

### Without Scaffold (simple)

```kotlin
@Composable
fun App() {
    val toastState = rememberToastHostState()

    Box(modifier = Modifier.fillMaxSize()) {
        MainContent()
        ToastHost(hostState = toastState)
    }
}
```

Pass `toastState` down through your composable tree or inject via a shared ViewModel.

---

## Step 3 — Show Toasts

### Simple message

```kotlin
// In ViewModel or event handler — needs CoroutineScope
scope.launch {
    toastState.showToast("Copied to clipboard")
}
```

### With action button

```kotlin
scope.launch {
    val result = toastState.showToast(
        message     = "Message deleted",
        actionLabel = "Undo",
        duration    = ToastDuration.LONG,
    )
    if (result == ToastResult.ACTION_PERFORMED) {
        undoDelete()
    }
}
```

### Styled toast

```kotlin
scope.launch {
    toastState.showToast(
        message  = "Upload failed",
        style    = ToastStyle.ERROR,
        position = ToastPosition.TOP,
        duration = ToastDuration.LONG,
    )
}
```

### Custom toast composable

```kotlin
ToastHost(
    hostState = toastState,
    toast = { data ->
        // Your custom toast UI
        MyCustomToast(data)
    }
)
```

---

## ViewModel Pattern (recommended)

Expose `ToastHostState` from your ViewModel so screens don't need the state directly:

```kotlin
class AppViewModel : ViewModel() {
    val toastState = ToastHostState()

    fun showError(message: String) {
        viewModelScope.launch {
            toastState.showToast(message, style = ToastStyle.ERROR)
        }
    }
}

// In composable:
val viewModel: AppViewModel = koinViewModel()
ToastHost(hostState = viewModel.toastState)
```

---

## AI-Assisted Setup

```
/sync-toast           # Verify Gradle + ToastHost placement
/sync-toast --check   # Dry run
```

See [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) for full docs.
