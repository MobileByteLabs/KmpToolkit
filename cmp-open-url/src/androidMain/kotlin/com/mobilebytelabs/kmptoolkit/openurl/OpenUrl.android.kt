package com.mobilebytelabs.kmptoolkit.openurl

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

actual fun openUrl(url: String): Boolean = try {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    OpenUrlContext.context.startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: Exception) {
    false
}

actual fun openInBrowser(url: String): Boolean = try {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        .addCategory(Intent.CATEGORY_BROWSABLE)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    OpenUrlContext.context.startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: Exception) {
    false
}

// Configured with `apply`, not by chaining the builder's return value. Intent.addFlags /
// addCategory / setPackage return the same Intent, so the return value carries no information
// — but binding it makes the call a platform-type expression, and Kotlin's null check then
// throws "addFlags(...) must not be null" wherever the framework returns null. That is exactly
// what android.jar's host-test stub does, so the chained form turned every openWithApp call
// into OpenUrlResult.Error in a JVM host test while working fine on device. `apply` returns
// the receiver, is the idiomatic way to configure an object, and behaves identically on device.
actual fun openWithApp(url: String, appHint: AppHint): OpenUrlResult = try {
    val uri = Uri.parse(url)
    val baseIntent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val intent = when (appHint) {
        AppHint.DEFAULT -> baseIntent

        AppHint.BROWSER -> baseIntent.apply { addCategory(Intent.CATEGORY_BROWSABLE) }

        AppHint.EMAIL -> Intent(Intent.ACTION_SENDTO, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        AppHint.MAPS -> baseIntent

        AppHint.PHONE -> Intent(Intent.ACTION_DIAL, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        AppHint.SMS -> Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        is AppHint.Custom -> baseIntent.apply { setPackage(appHint.packageName) }
    }

    OpenUrlContext.context.startActivity(intent)
    OpenUrlResult.Success
} catch (_: ActivityNotFoundException) {
    // If explicit package failed, retry without package restriction
    if (appHint is AppHint.Custom) {
        openWithApp(url, AppHint.DEFAULT)
    } else {
        OpenUrlResult.NoHandler
    }
} catch (e: Exception) {
    OpenUrlResult.Error(e.message ?: "Unknown error opening URL on Android")
}

actual fun canOpen(url: String): Boolean = try {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    val flags = PackageManager.MATCH_DEFAULT_ONLY
    OpenUrlContext.context.packageManager.resolveActivity(intent, flags) != null
} catch (_: Exception) {
    false
}
