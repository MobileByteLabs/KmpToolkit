package io.github.template.sample.appupdate.components

import platform.UIKit.UIDevice

actual fun getPlatformName(): String = UIDevice.currentDevice.systemName() +
    " " + UIDevice.currentDevice.systemVersion
