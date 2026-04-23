package com.mobilebytelabs.producttickets.config

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
    }
}
