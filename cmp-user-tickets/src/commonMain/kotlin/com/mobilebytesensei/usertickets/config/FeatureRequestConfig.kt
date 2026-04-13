package com.mobilebytesensei.usertickets.config

object FeatureRequestConfig {
    var supabaseUrl: String = ""
        private set
    var supabaseAnonKey: String = ""
        private set
    var productType: String = ""
        private set
    var userId: String? = null

    fun init(supabaseUrl: String, supabaseAnonKey: String, productType: String, userId: String? = null) {
        this.supabaseUrl = supabaseUrl
        this.supabaseAnonKey = supabaseAnonKey
        this.productType = productType
        this.userId = userId
    }
}
