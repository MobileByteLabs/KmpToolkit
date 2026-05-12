package com.mobilebytelabs.remoteconfig.dispatch

/**
 * Context passed to [ActionHandler]s when an action fires.
 *
 * Currently empty — extension point for future enrichment (NavController, SnackbarHostState,
 * app coroutine scope, etc.). Handlers reach for app-level singletons until this evolves.
 */
class ActionContext internal constructor()
