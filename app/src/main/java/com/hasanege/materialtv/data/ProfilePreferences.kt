package com.hasanege.materialtv.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import com.hasanege.materialtv.model.ContinueWatchingItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(name = "profile_preferences")

@Singleton
class ProfilePreferences @Inject constructor(@ApplicationContext private val context: Context) {
    companion object {
        val PROFILE_NAME = stringPreferencesKey("profile_name")
        val PROFILE_IMAGE_URL = stringPreferencesKey("profile_image_url")
        val PROFILE_IMAGE_LAST_UPDATED = stringPreferencesKey("profile_image_last_updated")
        val SELECTED_UPCOMING_CHANNEL_ID = stringPreferencesKey("selected_upcoming_channel_id")
        val SELECTED_UPCOMING_CHANNEL_NAME = stringPreferencesKey("selected_upcoming_channel_name")
        val SELECTED_UPCOMING_CHANNEL_ICON = stringPreferencesKey("selected_upcoming_channel_icon")
        val SELECTED_UPCOMING_CHANNELS = stringPreferencesKey("selected_upcoming_channels_json")
    }

    val profileName: Flow<String> = context.profileDataStore.data.map { preferences ->
        val encrypted = preferences[PROFILE_NAME] ?: ""
        if (encrypted.isEmpty()) "User" else com.hasanege.materialtv.utils.CryptoManager.decrypt(encrypted).takeIf { it.isNotEmpty() } ?: "User"
    }

    val profileImageUrl: Flow<String> = context.profileDataStore.data.map { preferences ->
        val encryptedPath = preferences[PROFILE_IMAGE_URL]
        val savedPath = if (!encryptedPath.isNullOrEmpty()) {
            com.hasanege.materialtv.utils.CryptoManager.decrypt(encryptedPath)
        } else ""
        
        val lastUpdated = preferences[PROFILE_IMAGE_LAST_UPDATED] ?: "0"
        val path = if (savedPath.isNotEmpty()) {
            savedPath
        } else {
            val file = java.io.File(context.filesDir, "pfp.png")
            if (file.exists()) file.absolutePath else ""
        }
        if (path.isNotEmpty()) {
            android.net.Uri.fromFile(java.io.File(path))
                .buildUpon()
                .appendQueryParameter("t", lastUpdated)
                .build()
                .toString()
        } else ""
    }.flowOn(Dispatchers.IO)

    val selectedUpcomingChannels: Flow<List<ContinueWatchingItem>> = context.profileDataStore.data.map { preferences ->
        val jsonEncrypted = preferences[SELECTED_UPCOMING_CHANNELS]
        if (!jsonEncrypted.isNullOrEmpty()) {
            val jsonDecrypted = com.hasanege.materialtv.utils.CryptoManager.decrypt(jsonEncrypted)
            if (jsonDecrypted.isNotEmpty()) {
                try {
                    Json.decodeFromString<List<ContinueWatchingItem>>(jsonDecrypted)
                } catch (e: Exception) {
                    emptyList()
                }
            } else emptyList()
        } else {
            // Fallback to legacy single selected channel
            val legacyId = preferences[SELECTED_UPCOMING_CHANNEL_ID]
            val legacyName = preferences[SELECTED_UPCOMING_CHANNEL_NAME]
            val legacyIcon = preferences[SELECTED_UPCOMING_CHANNEL_ICON]
            if (!legacyId.isNullOrEmpty() && !legacyName.isNullOrEmpty()) {
                val decId = com.hasanege.materialtv.utils.CryptoManager.decrypt(legacyId)
                val decName = com.hasanege.materialtv.utils.CryptoManager.decrypt(legacyName)
                val decIcon = if (legacyIcon != null) com.hasanege.materialtv.utils.CryptoManager.decrypt(legacyIcon) else ""
                if (decId.isNotEmpty() && decName.isNotEmpty()) {
                    listOf(
                        ContinueWatchingItem(
                            streamId = decId.toIntOrNull() ?: 0,
                            name = decName,
                            streamIcon = decIcon,
                            type = "live",
                            position = 0L,
                            duration = 0L,
                            actualWatchTime = 0L
                        )
                    )
                } else emptyList()
            } else emptyList()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun setProfileName(name: String) {
        val encrypted = com.hasanege.materialtv.utils.CryptoManager.encrypt(name)
        context.profileDataStore.edit { preferences ->
            preferences[PROFILE_NAME] = encrypted
        }
    }

    suspend fun setProfileImageFromUri(uriString: String) {
        withContext(Dispatchers.IO) {
            try {
                val uri = android.net.Uri.parse(uriString)
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = java.io.File(context.filesDir, "pfp.png")
                
                inputStream?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                val encryptedPath = com.hasanege.materialtv.utils.CryptoManager.encrypt(file.absolutePath)
                context.profileDataStore.edit { preferences ->
                    preferences[PROFILE_IMAGE_URL] = encryptedPath
                    preferences[PROFILE_IMAGE_LAST_UPDATED] = System.currentTimeMillis().toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun setProfileImageFromBitmap(bitmap: android.graphics.Bitmap) {
        withContext(Dispatchers.IO) {
            try {
                val file = java.io.File(context.filesDir, "pfp.png")
                file.outputStream().use { output ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
                }
                
                val encryptedPath = com.hasanege.materialtv.utils.CryptoManager.encrypt(file.absolutePath)
                context.profileDataStore.edit { preferences ->
                    preferences[PROFILE_IMAGE_URL] = encryptedPath
                    preferences[PROFILE_IMAGE_LAST_UPDATED] = System.currentTimeMillis().toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun clearProfile() {
        context.profileDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun setSelectedUpcomingChannel(id: String, name: String, icon: String) {
        val channel = ContinueWatchingItem(
            streamId = id.toIntOrNull() ?: 0,
            name = name,
            streamIcon = icon,
            type = "live",
            position = 0L,
            duration = 0L,
            actualWatchTime = 0L
        )
        setSelectedUpcomingChannels(listOf(channel))
    }

    suspend fun setSelectedUpcomingChannels(channels: List<ContinueWatchingItem>) {
        val json = Json.encodeToString(channels)
        val encrypted = com.hasanege.materialtv.utils.CryptoManager.encrypt(json)
        context.profileDataStore.edit { preferences ->
            preferences[SELECTED_UPCOMING_CHANNELS] = encrypted
        }
    }
}
