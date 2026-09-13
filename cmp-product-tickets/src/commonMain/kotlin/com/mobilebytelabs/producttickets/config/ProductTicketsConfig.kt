package com.mobilebytelabs.producttickets.config

import com.mobilebytelabs.producttickets.cmpMetadata
import io.github.mobilebytelabs.kmptoolkit.observe.observeLifecycle

object ProductTicketsConfig {
    var supabaseUrl: String = ""
        private set
    var supabaseAnonKey: String = ""
        private set
    var userId: String? = null
    var boardType: String = "default"
        private set

    fun init(supabaseUrl: String, supabaseAnonKey: String, userId: String? = null, boardType: String = "default") {
        this.supabaseUrl = supabaseUrl
        this.supabaseAnonKey = supabaseAnonKey
        this.userId = userId
        this.boardType = boardType
        // Reports configuration, not credentials: the Supabase URL and anon key never leave here.
        // `hasUser` matters because it is what gates Contact Support + My Tickets.
        observeLifecycle(
            cmpMetadata(),
            "configured",
            mapOf("boardType" to boardType, "hasUser" to (userId != null)),
        )
    }
}
