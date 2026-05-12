package com.mobilebytelabs.remoteconfig.dispatch

/**
 * Handler invoked when a remote-config CTA with a registered [com.mobilebytelabs.remoteconfig.model.ActionType] fires.
 *
 * Register handlers via the `action("type") { value, ctx -> … }` DSL inside `remoteConfig { … }`.
 */
fun interface ActionHandler {
    operator fun invoke(value: String?, context: ActionContext)
}
