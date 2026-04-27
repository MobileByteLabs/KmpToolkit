# Web Setup — cmp-deep-link (JS / Wasm/JS)

## Usage

```kotlin
fun main() {
    DeepLinkHandler.initBrowser(BrowserRoutingMode.HASH)
    // start your Compose Web app...
}
```

`initBrowser` does three things:
1. Parses the current URL on startup
2. Registers a `hashchange` or `popstate` listener for subsequent navigation
3. Forwards every detected URL to `DeepLinkHandler.handle`

---

## BrowserRoutingMode

| Mode | Listens to | URL format | Use when |
|------|-----------|-----------|---------|
| `HASH` (default) | `hashchange` | `https://yourapp.com/#/product/42` | SPA with hash routing |
| `HISTORY` | `popstate` | `https://yourapp.com/product/42` | SPA with history API routing |

---

## HASH mode

```kotlin
DeepLinkHandler.initBrowser(BrowserRoutingMode.HASH)
```

Navigate programmatically:
```javascript
window.location.hash = "/product/42";
// → DeepLinkHandler receives "https://yourapp.com/product/42"
```

---

## HISTORY mode

```kotlin
DeepLinkHandler.initBrowser(BrowserRoutingMode.HISTORY)
```

Navigate programmatically:
```javascript
history.pushState({}, "", "/product/42");
window.dispatchEvent(new PopStateEvent("popstate"));
// → DeepLinkHandler receives "https://yourapp.com/product/42"
```

---

## wasmWasi

`wasmWasi` targets server-side WASI environments (Node.js WASI, wasmtime).
No browser APIs are available. `initBrowser` does not exist in `wasmWasiMain`.
If the host environment delivers a URI string, call `DeepLinkHandler.handle(uri)` directly.
