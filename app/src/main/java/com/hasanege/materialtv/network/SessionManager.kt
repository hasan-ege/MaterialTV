package com.hasanege.materialtv.network

import com.hasanege.materialtv.model.AuthUserInfo

object SessionManager {
    enum class LoginType {
        XTREAM,
        M3U
    }

    var loginType: LoginType = LoginType.XTREAM

    // Xtream Specific
    var serverUrl: String? = null
    var username: String? = null
    var password: String? = null
    var userInfo: AuthUserInfo? = null

    // M3U Specific
    var m3uUrl: String? = null

    private var apiService: XtreamApiService? = null

    fun initialize(baseUrl: String, user: String, pass: String) {
        loginType = LoginType.XTREAM
        val correctedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        serverUrl = correctedBaseUrl
        username = user
        password = pass
        apiService = RetrofitClient.getClient(correctedBaseUrl)
    }

    fun initializeM3u(url: String) {
        loginType = LoginType.M3U
        m3uUrl = url
        // No API service needed for M3U
    }

    fun getApiService(): XtreamApiService {
        return apiService ?: throw IllegalStateException("SessionManager not initialized. Call initialize() first.")
    }

    fun isInitialized(): Boolean {
        return if (loginType == LoginType.XTREAM) {
            apiService != null
        } else {
            !m3uUrl.isNullOrBlank()
        }
    }

    fun clear() {
        loginType = LoginType.XTREAM
        serverUrl = null
        username = null
        password = null
        userInfo = null
        apiService = null
        m3uUrl = null
    }
}
