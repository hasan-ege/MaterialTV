package com.hasanege.materialtv.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class XtreamUser(
    @SerialName("user_info")
    val userInfo: UserInfo,
    @SerialName("server_info")
    val serverInfo: ServerInfo
)

@Serializable
data class UserInfo(
    val username: String,
    val status: String,
    @SerialName("is_trial")
    val isTrial: String,
    @SerialName("max_connections")
    val maxConnections: String
)
