# Desktop Setup — cmp-deep-link (JVM / Linux / Windows)

## Usage

```kotlin
fun main(args: Array<String>) {
    DeepLinkHandler.handleLaunchArgs(args)
    // start your Compose Desktop window...
}
```

The OS passes the URI as a command-line argument. `handleLaunchArgs` scans `args`
for the first entry containing `://` and forwards it to `DeepLinkHandler.handle`.

---

## Linux (.desktop file)

Create `~/.local/share/applications/myapp.desktop`:

```ini
[Desktop Entry]
Version=1.0
Type=Application
Name=My App
Exec=/opt/myapp/bin/myapp %u
MimeType=x-scheme-handler/myapp;
```

Register:
```bash
xdg-mime default myapp.desktop x-scheme-handler/myapp
update-desktop-database ~/.local/share/applications
```

Test:
```bash
xdg-open "myapp://open/product/42"
```

---

## Windows (Registry)

Classic Win32:
```
HKEY_CLASSES_ROOT\myapp
  (Default) = "URL:myapp Protocol"
  URL Protocol = ""
  \shell\open\command
    (Default) = "\"C:\Program Files\MyApp\myapp.exe\" \"%1\""
```

PowerShell one-liner:
```powershell
New-Item -Path "HKCU:\Software\Classes\myapp" -Force | Out-Null
Set-ItemProperty -Path "HKCU:\Software\Classes\myapp" -Name "(Default)" -Value "URL:myapp Protocol"
Set-ItemProperty -Path "HKCU:\Software\Classes\myapp" -Name "URL Protocol" -Value ""
New-Item -Path "HKCU:\Software\Classes\myapp\shell\open\command" -Force | Out-Null
Set-ItemProperty -Path "HKCU:\Software\Classes\myapp\shell\open\command" -Name "(Default)" `
    -Value "`"C:\Program Files\MyApp\myapp.exe`" `"%1`""
```

MSIX (AppxManifest.xml):
```xml
<Extensions>
  <uap:Extension Category="windows.protocol">
    <uap:Protocol Name="myapp">
      <uap:DisplayName>My App</uap:DisplayName>
    </uap:Protocol>
  </uap:Extension>
</Extensions>
```

---

## macOS (CFBundleURLTypes in Info.plist)

Same as `docs/MACOS.md` — the JVM entry point `handleLaunchArgs` is used for
Compose Desktop on macOS when not using the native macOS Kotlin target.

```xml
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLSchemes</key>
        <array><string>myapp</string></array>
    </dict>
</array>
```

Package with `./gradlew packageDmg` — the launcher script handles `%u` forwarding.
