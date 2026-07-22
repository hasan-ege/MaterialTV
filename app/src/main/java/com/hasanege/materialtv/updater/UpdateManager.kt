package com.hasanege.materialtv.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.hasanege.materialtv.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val downloadUrl: String,
    val releaseNotes: String
)

class UpdateManager(private val context: Context) {
    private val client = OkHttpClient()
    private val repoUrl = "https://api.github.com/repos/hasan-ege/MaterialTV/releases/latest"

    suspend fun checkForUpdate(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(repoUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP Error: ${response.code}"))
                }
                
                val responseBody = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val json = JSONObject(responseBody)
                
                val tagName = json.getString("tag_name")
                val releaseNotes = json.optString("body", "No release notes available.")
                val assets = json.getJSONArray("assets")
                
                var downloadUrl = ""
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val url = asset.getString("browser_download_url")
                    if (url.endsWith(".apk")) {
                        downloadUrl = url
                        break
                    }
                }
                
                if (downloadUrl.isEmpty()) {
                    return@withContext Result.failure(Exception("No APK found in release"))
                }
                
                val latestVersionStr = tagName.removePrefix("v").removePrefix("V")
                val currentVersionStr = BuildConfig.VERSION_NAME.removePrefix("v").removePrefix("V")
                
                // Very basic semantic version comparison
                val hasUpdate = try {
                    val latestParts = latestVersionStr.split(".").map { it.toIntOrNull() ?: 0 }
                    val currentParts = currentVersionStr.split(".").map { it.toIntOrNull() ?: 0 }
                    
                    var isNewer = false
                    val length = maxOf(latestParts.size, currentParts.size)
                    for (i in 0 until length) {
                        val latest = latestParts.getOrElse(i) { 0 }
                        val current = currentParts.getOrElse(i) { 0 }
                        if (latest > current) {
                            isNewer = true
                            break
                        } else if (latest < current) {
                            break
                        }
                    }
                    isNewer
                } catch (e: Exception) {
                    // Fallback to simple string check if parsing fails
                    latestVersionStr != currentVersionStr
                }
                
                Result.success(UpdateInfo(hasUpdate, tagName, downloadUrl, releaseNotes))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun downloadAndInstall(updateInfo: UpdateInfo, onProgress: (Int) -> Unit) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(updateInfo.downloadUrl)
        
        // Remove previous apk if exists
        val apkName = "MaterialTV-Update-${updateInfo.latestVersion}.apk"
        val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), apkName)
        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        val request = DownloadManager.Request(uri)
            .setTitle("MaterialTV Güncellemesi")
            .setDescription("Sürüm ${updateInfo.latestVersion} indiriliyor...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, apkName)

        val downloadId = downloadManager.enqueue(request)

        // Listen for completion
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    context?.unregisterReceiver(this)
                    installApk(destinationFile)
                }
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun installApk(file: File) {
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(intent)
    }
}
