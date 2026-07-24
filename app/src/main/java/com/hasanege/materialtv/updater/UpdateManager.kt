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
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val downloadUrl: String,
    val releaseNotes: String
)

class UpdateManager(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val apiRepoUrl = "https://api.github.com/repos/hasan-ege/MaterialTV/releases/latest"
    private val webLatestUrl = "https://github.com/hasan-ege/MaterialTV/releases/latest"

    suspend fun checkForUpdate(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            // Method 1: Try GitHub REST API
            val apiResult = checkViaGitHubApi()
            if (apiResult.isSuccess) {
                return@withContext apiResult
            }

            // Method 2: Fallback to GitHub Web Redirect (bypasses GitHub API Rate Limit 403)
            val webResult = checkViaWebRedirect()
            if (webResult.isSuccess) {
                return@withContext webResult
            }

            return@withContext Result.failure(
                apiResult.exceptionOrNull() ?: webResult.exceptionOrNull() ?: Exception("Güncelleme bilgisi alınamadı")
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun checkViaGitHubApi(): Result<UpdateInfo> {
        return try {
            val request = Request.Builder()
                .url(apiRepoUrl)
                .header("User-Agent", "MaterialTV-App/${BuildConfig.VERSION_NAME}")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(Exception("GitHub API HTTP Error: ${response.code}"))
                }

                val responseBody = response.body?.string() ?: return Result.failure(Exception("Empty API body"))
                val json = JSONObject(responseBody)

                val tagName = json.getString("tag_name")
                val releaseNotes = json.optString("body", "Yeni güncelleme mevcut.")
                val assets = json.getJSONArray("assets")

                var downloadUrl = ""
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val url = asset.optString("browser_download_url", "")
                    val name = asset.optString("name", "")
                    if (url.endsWith(".apk", ignoreCase = true) || name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = url
                        break
                    }
                }

                if (downloadUrl.isEmpty()) {
                    val cleanTag = tagName.removePrefix("v").removePrefix("V")
                    downloadUrl = "https://github.com/hasan-ege/MaterialTV/releases/download/$tagName/MaterialTV-$cleanTag.apk"
                }

                val currentVersion = BuildConfig.VERSION_NAME
                val hasUpdate = isVersionNewer(tagName, currentVersion)

                Result.success(UpdateInfo(hasUpdate, tagName, downloadUrl, releaseNotes))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun checkViaWebRedirect(): Result<UpdateInfo> {
        return try {
            val noRedirectClient = client.newBuilder().followRedirects(false).build()
            val request = Request.Builder()
                .url(webLatestUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            noRedirectClient.newCall(request).execute().use { response ->
                val redirectUrl = response.header("Location")
                    ?: response.request.url.toString()

                val tagName = redirectUrl.substringAfterLast("/").trim()
                if (tagName.isBlank() || tagName == "latest") {
                    return Result.failure(Exception("Son sürüm etiketi alınamadı"))
                }

                val cleanTag = tagName.removePrefix("v").removePrefix("V")
                val downloadUrl = "https://github.com/hasan-ege/MaterialTV/releases/download/$tagName/MaterialTV-$cleanTag.apk"
                val currentVersion = BuildConfig.VERSION_NAME
                val hasUpdate = isVersionNewer(tagName, currentVersion)

                Result.success(
                    UpdateInfo(
                        hasUpdate = hasUpdate,
                        latestVersion = tagName,
                        downloadUrl = downloadUrl,
                        releaseNotes = "Sürüm $tagName"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isVersionNewer(latestStr: String, currentStr: String): Boolean {
        val latestClean = latestStr.trim().removePrefix("v").removePrefix("V")
        val currentClean = currentStr.trim().removePrefix("v").removePrefix("V")
        if (latestClean.equals(currentClean, ignoreCase = true)) return false

        val regex = Regex("(\\d+)|([a-zA-Z]+)")
        val latestTokens = regex.findAll(latestClean).map { m -> m.value.toIntOrNull() ?: m.value }.toList()
        val currentTokens = regex.findAll(currentClean).map { m -> m.value.toIntOrNull() ?: m.value }.toList()

        val maxLen = maxOf(latestTokens.size, currentTokens.size)
        for (i in 0 until maxLen) {
            val lTok = latestTokens.getOrNull(i)
            val cTok = currentTokens.getOrNull(i)

            if (lTok == null) return false
            if (cTok == null) return true

            if (lTok is Int && cTok is Int) {
                if (lTok > cTok) return true
                if (lTok < cTok) return false
            } else if (lTok is String && cTok is String) {
                val cmp = lTok.compareTo(cTok, ignoreCase = true)
                if (cmp > 0) return true
                if (cmp < 0) return false
            } else if (lTok is Int && cTok is String) {
                return true
            } else if (lTok is String && cTok is Int) {
                return false
            }
        }
        return false
    }

    fun downloadAndInstall(updateInfo: UpdateInfo, onProgress: (Int) -> Unit) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(updateInfo.downloadUrl)
        
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
