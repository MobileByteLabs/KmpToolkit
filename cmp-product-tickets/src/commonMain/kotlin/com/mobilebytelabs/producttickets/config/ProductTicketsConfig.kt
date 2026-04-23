package com.mobilebytelabs.producttickets.config

object ProductTicketsConfig {
    var supabaseUrl: String = ""
        private set
    var supabaseAnonKey: String = ""
        private set
    var userId: String? = null

    fun init(supabaseUrl: String, supabaseAnonKey: String, userId: String? = null) {
        this.supabaseUrl = supabaseUrl
        this.supabaseAnonKey = supabaseAnonKey
        this.userId = userId
    }
}
