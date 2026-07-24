package com.hasanege.materialtv.data

import android.content.Context
import android.content.SharedPreferences
import com.hasanege.materialtv.network.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: SessionManager.LoginType,
    val url: String? = null, // For M3U
    val serverUrl: String? = null, // For Xtream
    val username: String? = null, // For Xtream
    val password: String? = null // For Xtream
)

@Singleton
class PlaylistManager @Inject constructor(@ApplicationContext context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun addPlaylist(playlist: Playlist) {
        val playlists = getPlaylists().toMutableList()
        playlists.add(playlist)
        savePlaylists(playlists)
    }

    fun removePlaylist(id: String) {
        val playlists = getPlaylists().toMutableList()
        playlists.removeAll { it.id == id }
        savePlaylists(playlists)
    }

    fun getPlaylists(): List<Playlist> {
        val jsonString = sharedPreferences.getString(KEY_PLAYLISTS, "[]") ?: "[]"
        return try {
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun savePlaylists(playlists: List<Playlist>) {
        val jsonString = json.encodeToString(playlists)
        sharedPreferences.edit().putString(KEY_PLAYLISTS, jsonString).apply()
    }

    fun setActivePlaylist(id: String) {
        sharedPreferences.edit().putString(KEY_ACTIVE_PLAYLIST_ID, id).apply()
    }

    fun getActivePlaylistId(): String? {
        return sharedPreferences.getString(KEY_ACTIVE_PLAYLIST_ID, null)
    }

    companion object {
        private const val PREFS_NAME = "playlist_prefs"
        private const val KEY_PLAYLISTS = "playlists"
        private const val KEY_ACTIVE_PLAYLIST_ID = "active_playlist_id"
    }
}
