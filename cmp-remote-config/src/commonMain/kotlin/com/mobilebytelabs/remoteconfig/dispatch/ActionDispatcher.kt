package com.mobilebytelabs.remoteconfig.dispatch

import co.touchlab.kermit.Logger
import com.mobilebytelabs.remoteconfig.model.ActionType

internal object ActionDispatcher {
    private val handlers = mutableMapOf<ActionType, ActionHandler>()
    private val log = Logger.withTag("RemoteConfig")

    fun register(map: Map<ActionType, ActionHandler>) {
        handlers.clear()
        handlers.putAll(map)
    }

    fun dispatch(type: ActionType, value: String?) {
        val handler = handlers[type]
        if (handler == null) {
            if (type !in BUILT_IN) {
                log.w { "No handler registered for action_type='${type.value}' (value=$value)" }
            }
            return
        }
        handler(value, ActionContext())
    }

    internal fun clear() {
        handlers.clear()
    }

    private val BUILT_IN = setOf(
        ActionType.NONE,
        ActionType.URL,
        ActionType.DEEPLINK,
        ActionType.STORE,
        ActionType.DISMISS,
        ActionType.PREMIUM,
    )
}
