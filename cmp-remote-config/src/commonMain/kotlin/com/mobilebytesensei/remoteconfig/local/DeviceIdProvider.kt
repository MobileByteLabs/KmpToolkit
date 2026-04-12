package com.mobilebytesensei.remoteconfig.local

import com.russhwolf.settings.Settings
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class DeviceIdProvider(
    private val settings: Settings = Settings(),
) {
    companion object {
        private const val KEY = "rc_device_id"
    }

    @OptIn(ExperimentalUuidApi::class)
    fun getDeviceId(): String {
        val existing = settings.getStringOrNull(KEY)
        if (existing != null) return existing
        val newId = Uuid.random().toString()
        settings.putString(KEY, newId)
        return newId
    }
}
