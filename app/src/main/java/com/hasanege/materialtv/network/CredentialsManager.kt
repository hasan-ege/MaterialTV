package com.hasanege.materialtv.network

import android.content.Context
import android.content.SharedPreferences

import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialsManager @Inject constructor(@ApplicationContext context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveCredentials(serverUrl: String, username: String, password: String) {
        with(sharedPreferences.edit()) {
            putString(KEY_SERVER_URL, serverUrl)
            putString(KEY_USERNAME, username)
            putString(KEY_PASSWORD, password)
            remove(KEY_M3U_URL) // Clear M3U URL when saving Xtream credentials
            apply()
        }
    }
    
    fun saveM3uUrl(url: String) {
        with(sharedPreferences.edit()) {
            putString(KEY_M3U_URL, url)
            remove(KEY_SERVER_URL) // Clear Xtream credentials when saving M3U
            remove(KEY_USERNAME)
            remove(KEY_PASSWORD)
            apply()
        }
    }

    fun getServerUrl(): String? {
        return sharedPreferences.getString(KEY_SERVER_URL, null)
    }

    fun getUsername(): String? {
        return sharedPreferences.getString(KEY_USERNAME, null)
    }

    fun getPassword(): String? {
        return sharedPreferences.getString(KEY_PASSWORD, null)
    }
    
    fun getM3uUrl(): String? {
        return sharedPreferences.getString(KEY_M3U_URL, null)
    }

    fun clearCredentials() {
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "user_credentials"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_M3U_URL = "m3u_url"
    }
}
