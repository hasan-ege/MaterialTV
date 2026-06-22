package com.hasanege.materialtv

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.data.M3uRepository
import com.hasanege.materialtv.model.AuthUserInfo
import com.hasanege.materialtv.network.CredentialsManager
import com.hasanege.materialtv.network.RetrofitClient
import com.hasanege.materialtv.network.SessionManager
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel

@HiltViewModel
class MainViewModel @Inject constructor(
    private val credentialsManager: CredentialsManager,
    private val application: Application
) : ViewModel() {
    // UI State
    var serverUrl by mutableStateOf("")
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    
    var m3uUrl by mutableStateOf("")
    var isM3uLogin by mutableStateOf(false)

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    init {
        serverUrl = credentialsManager.getServerUrl() ?: ""
        username = credentialsManager.getUsername() ?: ""
        password = credentialsManager.getPassword() ?: ""
    }

    fun onLoginClick(onSuccess: () -> Unit) {
        if (isM3uLogin) {
            loginM3u(onSuccess)
        } else {
            loginXtream(onSuccess)
        }
    }

    private fun loginM3u(onSuccess: () -> Unit) {
        android.util.Log.d("MainViewModel", "=== M3U Login Started ===")
        android.util.Log.d("MainViewModel", "M3U URL: $m3uUrl")
        
        if (m3uUrl.isBlank()) {
            error = "Please enter M3U URL"
            android.util.Log.e("MainViewModel", "M3U URL is blank")
            return
        }

        // Basic URL validation
        if (!m3uUrl.startsWith("http://") && !m3uUrl.startsWith("https://")) {
            error = "URL must start with http:// or https://"
            android.util.Log.e("MainViewModel", "Invalid URL format: $m3uUrl")
            return
        }

        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                android.util.Log.d("MainViewModel", "Fetching M3U playlist...")
                // Fetch and parse the playlist
                M3uRepository.fetchPlaylist(m3uUrl, application)
                
                android.util.Log.d("MainViewModel", "Playlist fetched, size: ${M3uRepository.getPlaylistSize()}")
                
                // Verify playlist is not empty
                if (M3uRepository.getPlaylistSize() == 0) {
                    val errorMsg = "Playlist is empty or invalid format"
                    error = errorMsg
                    android.util.Log.e("MainViewModel", errorMsg)
                    return@launch
                }
                
                // Save M3U URL for auto-login
                android.util.Log.d("MainViewModel", "Saving M3U URL to credentials...")
                credentialsManager.saveM3uUrl(m3uUrl)
                
                // Initialize session and navigate
                android.util.Log.d("MainViewModel", "Initializing SessionManager with M3U...")
                SessionManager.initializeM3u(m3uUrl)
                
                android.util.Log.d("MainViewModel", "=== M3U Login Successful ===")
                onSuccess()
            } catch (e: java.net.UnknownHostException) {
                val errorMsg = "Cannot reach server. Check your internet connection."
                error = errorMsg
                android.util.Log.e("MainViewModel", errorMsg, e)
                android.util.Log.e("MainViewModel", "Host: ${e.message}")
            } catch (e: java.net.MalformedURLException) {
                val errorMsg = "Invalid URL format"
                error = errorMsg
                android.util.Log.e("MainViewModel", errorMsg, e)
                android.util.Log.e("MainViewModel", "URL that failed: $m3uUrl")
            } catch (e: java.io.IOException) {
                val errorMsg = "Network error: ${e.message}"
                error = errorMsg
                android.util.Log.e("MainViewModel", errorMsg, e)
            } catch (e: Exception) {
                val errorMsg = "Failed to load playlist: ${e.javaClass.simpleName}: ${e.message}"
                error = errorMsg
                android.util.Log.e("MainViewModel", "=== M3U Login Failed ===", e)
                android.util.Log.e("MainViewModel", "Exception type: ${e.javaClass.name}")
                android.util.Log.e("MainViewModel", "Stack trace:")
                e.printStackTrace()
            } finally {
                isLoading = false
                android.util.Log.d("MainViewModel", "M3U login process completed. isLoading=false")
            }
        }
    }

    private fun loginXtream(onSuccess: () -> Unit) {
        if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
            error = "Please fill all fields"
            return
        }
        val finalUrl = if (serverUrl.startsWith("http://") || serverUrl.startsWith("https://")) {
            serverUrl
        } else {
            "http://$serverUrl"
        }
        authenticateXtream(finalUrl, username, password, onSuccess)
    }

    fun authenticateXtream(server: String, user: String, pass: String, onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                SessionManager.initialize(server, username, password)
                val apiService = SessionManager.getApiService()
                val response = apiService.authenticate(user, pass)
                val json = Json { ignoreUnknownKeys = true }

                if (response is JsonObject && "user_info" in response) {
                    val userInfoJson = response["user_info"]
                    if (userInfoJson is JsonObject) {
                        val authUserInfo = json.decodeFromJsonElement(AuthUserInfo.serializer(), userInfoJson)

                        if (authUserInfo.auth == 1) {
                            credentialsManager.saveCredentials(server, user, pass)
                            SessionManager.serverUrl = server
                            SessionManager.username = user
                            SessionManager.password = pass
                            SessionManager.userInfo = authUserInfo
                            onLoginSuccess()
                        } else {
                            error = "Invalid credentials or inactive user."
                            SessionManager.clear()
                            credentialsManager.clearCredentials()
                        }
                    } else {
                        error = "Login failed: Malformed user info in server response."
                        SessionManager.clear()
                        credentialsManager.clearCredentials()
                    }
                } else {
                    error = "Login failed: Unexpected response from server."
                    SessionManager.clear()
                    credentialsManager.clearCredentials()
                }
            } catch (e: Exception) {
                error = "Login failed: ${e.message}"
                SessionManager.clear()
            } finally {
                isLoading = false
            }
        }
    }
}
