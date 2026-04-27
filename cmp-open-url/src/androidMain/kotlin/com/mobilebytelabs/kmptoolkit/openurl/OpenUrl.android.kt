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

actual fun openWithApp(url: String, appHint: AppHint): OpenUrlResult = try {
    val uri = Uri.parse(url)
    val baseIntent = Intent(Intent.ACTION_VIEW, uri)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    val intent = when (appHint) {
        AppHint.DEFAULT -> baseIntent

        AppHint.BROWSER -> baseIntent.addCategory(Intent.CATEGORY_BROWSABLE)

        AppHint.EMAIL -> Intent(Intent.ACTION_SENDTO, uri)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        AppHint.MAPS -> baseIntent

        AppHint.PHONE -> Intent(Intent.ACTION_DIAL, uri)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        AppHint.SMS -> Intent(Intent.ACTION_VIEW, uri)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        is AppHint.Custom -> baseIntent.setPackage(appHint.packageName)
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
