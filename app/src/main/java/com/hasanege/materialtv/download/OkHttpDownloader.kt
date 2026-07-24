package com.hasanege.materialtv.download

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * OkHttp tabanlı indirme motoru
 * Byte-level kontrollü, resume desteği ile
 * Connection leak olmadan düzgün response yönetimi
 */
class OkHttpDownloader {
    
    companion object {
        private const val TAG = "OkHttpDownloader"
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    
    private val activeDownloads = mutableMapOf<String, Boolean>()
    
    /**
     * İndirme başlat/devam et
     */
    suspend fun download(
        id: String,
        url: String,
        filePath: String,
        onProgress: (Long, Long, Long) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        activeDownloads[id] = true
        
        Log.i(TAG, "[START] Download initiated: id=$id")
        Log.d(TAG, "[START] URL: ${com.hasanege.materialtv.utils.StringUtils.sanitizeUrl(url)}")
        Log.d(TAG, "[START] FilePath: $filePath")
        
        val file = File(filePath)
        val parentDir = file.parentFile
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs()
            Log.d(TAG, "[START] Created directory: ${parentDir.absolutePath}")
        }
        
        var totalBytes = 0L
        var downloadedBytes = if (file.exists()) file.length() else 0L
        var lastProgressTime = System.currentTimeMillis()
        var lastBytes = downloadedBytes
        var retryCount = 0
        val maxRetries = 20
        val baseRetryDelayMs = 1000L
        val maxRetryDelayMs = 30000L
        var useRangeHeader = downloadedBytes > 0
        var rangeRetryCount = 0 // Track Range-specific retries
        val maxRangeRetries = 3 // Try Range 3 times before giving up
        
        Log.i(TAG, "[START] Initial state: existingBytes=$downloadedBytes, useRange=$useRangeHeader")
        
        while (isActive && activeDownloads[id] == true && retryCount < maxRetries) {
            var response: Response? = null
            try {
                val requestBuilder = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "*/*")
                    .header("Connection", "keep-alive")
                
                if (useRangeHeader && downloadedBytes > 0) {
                    requestBuilder.header("Range", "bytes=$downloadedBytes-")
                    Log.d(TAG, "[RESUME] Range header: bytes=$downloadedBytes-")
                }
                
                val request = requestBuilder.build()
                response = client.newCall(request).execute()
                
                val code = response.code
                
                // Handle non-successful responses
                if (!response.isSuccessful) {
                    // 401/403: Authentication/Rate-limit error
                    // IPTV servers often throttle connections - DO NOT delete file!
                    // Keep existing progress and retry with exponential backoff
                    if (code == 401 || code == 403) {
                        response.close()
                        retryCount++
                        
                        // If using Range header, retry with longer delays
                        // IPTV servers often rate-limit and need time between connections
                        if (downloadedBytes > 0) {
                            rangeRetryCount++
                            
                            if (rangeRetryCount <= maxRangeRetries) {
                                // Wait with exponential backoff: 10s, 20s, 30s
                                val waitTime = 10000L * rangeRetryCount
                                Log.w(TAG, "[AUTH-RETRY] Got $code at ${downloadedBytes/1024/1024}MB, waiting ${waitTime/1000}s before retry $rangeRetryCount/$maxRangeRetries")
                                kotlinx.coroutines.delay(waitTime)
                                // Keep Range header - don't delete progress!
                                useRangeHeader = true
                                continue
                            } else {
                                // Range header retries exhausted - try without Range but KEEP the file
                                // Maybe server doesn't support resume, we'll append after getting total size
                                Log.w(TAG, "[AUTH-NO-RANGE] Range failed $maxRangeRetries times, trying without Range header but keeping ${downloadedBytes/1024/1024}MB progress")
                                useRangeHeader = false
                                rangeRetryCount = 0
                                // Wait longer before this attempt
                                kotlinx.coroutines.delay(15000L)
                                continue
                            }
                        }
                        
                        // No progress yet - check if we should retry or fail
                        if (retryCount < maxRetries) {
                            val waitTime = 10000L * retryCount
                            Log.w(TAG, "[AUTH-WAIT] Server returned $code, waiting ${waitTime/1000}s before retry $retryCount/$maxRetries")
                            kotlinx.coroutines.delay(waitTime)
                            continue
                        }
                        
                        // Auth failed after all retries - this is a real auth error
                        Log.e(TAG, "[AUTH-FAIL] Server returned $code after $retryCount retries - authentication failed")
                        onError("Kimlik doğrulama hatası (HTTP $code). Lütfen hesabınızı kontrol edin.")
                        return@withContext
                    }
                    
                    // 416: Range Not Satisfiable - file might be complete or server doesn't support resume
                    if (code == 416) {
                        Log.w(TAG, "[RANGE-416] Server returned 416 - Range not satisfiable at ${downloadedBytes/1024/1024}MB")
                        response.close()
                        
                        // First try: Maybe server doesn't support resume - try without Range header
                        // but KEEP the file in case we can append later
                        if (useRangeHeader && downloadedBytes > 0) {
                            Log.w(TAG, "[RANGE-416] Trying without Range header, keeping ${downloadedBytes/1024/1024}MB progress")
                            useRangeHeader = false
                            continue
                        }
                        
                        // If still failing without Range, this might mean:
                        // 1. Server has different file size - in that case retry logic will handle
                        // 2. Server error - exponential backoff will help
                        retryCount++
                        val delayMs = (baseRetryDelayMs * (1 shl minOf(retryCount - 1, 5))).coerceAtMost(maxRetryDelayMs)
                        Log.w(TAG, "[RANGE-416] Still failing, retry $retryCount/$maxRetries, waiting ${delayMs}ms")
                        kotlinx.coroutines.delay(delayMs)
                        continue
                    }
                    
                    // Other errors - retry with exponential backoff
                    response.close()
                    retryCount++
                    val delayMs = (baseRetryDelayMs * (1 shl minOf(retryCount - 1, 5))).coerceAtMost(maxRetryDelayMs)
                    Log.w(TAG, "[RETRY] HTTP $code, retry $retryCount/$maxRetries, waiting ${delayMs}ms")
                    kotlinx.coroutines.delay(delayMs)
                    continue
                }
                
                // Success! Reset range retry count
                rangeRetryCount = 0
                
                // Success! Get response body
                val responseBody = response.body
                if (responseBody == null) {
                    response.close()
                    throw IOException("Empty response body")
                }
                
                // Calculate total size
                val contentLength = responseBody.contentLength()
                if (totalBytes == 0L || !useRangeHeader) {
                    totalBytes = if (downloadedBytes > 0 && code == 206) {
                        val contentRange = response.header("Content-Range")
                        contentRange?.substringAfterLast("/")?.toLongOrNull() ?: (downloadedBytes + contentLength)
                    } else {
                        contentLength
                    }
                }
                
                // Reset retry count on successful connection
                retryCount = 0
                Log.d(TAG, "[CONNECTED] totalBytes=$totalBytes, downloadedBytes=$downloadedBytes, code=$code")
                
                // Send immediate progress on resume
                if (downloadedBytes > 0 && totalBytes > 0) {
                    onProgress(downloadedBytes, totalBytes, 0L)
                }
                
                // Write to file
                val appendMode = downloadedBytes > 0 && code == 206
                FileOutputStream(file, appendMode).use { output ->
                    responseBody.byteStream().use { inputStream ->
                        val buffer = ByteArray(32 * 1024)
                        
                        while (isActive && activeDownloads[id] == true) {
                            val bytesRead = inputStream.read(buffer)
                            if (bytesRead == -1) break
                            
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            
                            // Update progress every 500ms
                            val now = System.currentTimeMillis()
                            if (now - lastProgressTime >= 500) {
                                val elapsed = (now - lastProgressTime) / 1000.0
                                val speed = if (elapsed > 0) {
                                    ((downloadedBytes - lastBytes) / elapsed).toLong()
                                } else 0L
                                
                                onProgress(downloadedBytes, totalBytes, speed)
                                lastProgressTime = now
                                lastBytes = downloadedBytes
                            }
                        }
                    }
                }
                
                // Close response after reading
                response.close()
                response = null
                
                // Check if paused/cancelled
                if (activeDownloads[id] != true) {
                    Log.d(TAG, "[PAUSED] Download paused at $downloadedBytes bytes")
                    return@withContext
                }
                
                // Check if completed
                if (totalBytes > 0 && downloadedBytes >= totalBytes) {
                    Log.i(TAG, "[COMPLETE] Download finished: $downloadedBytes bytes")
                    onComplete()
                    return@withContext
                }
                
                // Stream ended unexpectedly, retry
                if (downloadedBytes < totalBytes) {
                    retryCount++
                    val delayMs = (baseRetryDelayMs * (1 shl minOf(retryCount - 1, 5))).coerceAtMost(maxRetryDelayMs)
                    useRangeHeader = true
                    Log.w(TAG, "[RETRY] Stream ended at $downloadedBytes/$totalBytes, retry $retryCount/$maxRetries")
                    kotlinx.coroutines.delay(delayMs)
                } else {
                    onComplete()
                    return@withContext
                }
                
            } catch (e: java.net.SocketTimeoutException) {
                response?.close()
                retryCount++
                val delayMs = (baseRetryDelayMs * (1 shl minOf(retryCount - 1, 5))).coerceAtMost(maxRetryDelayMs)
                useRangeHeader = downloadedBytes > 0
                Log.w(TAG, "[RETRY] Timeout, retry $retryCount/$maxRetries, waiting ${delayMs}ms")
                kotlinx.coroutines.delay(delayMs)
            } catch (e: java.net.SocketException) {
                response?.close()
                retryCount++
                val delayMs = (baseRetryDelayMs * (1 shl minOf(retryCount - 1, 5))).coerceAtMost(maxRetryDelayMs)
                useRangeHeader = downloadedBytes > 0
                Log.w(TAG, "[RETRY] Socket error: ${com.hasanege.materialtv.utils.StringUtils.sanitizeUrl(e.message)}, retry $retryCount/$maxRetries")
                kotlinx.coroutines.delay(delayMs)
            } catch (e: IOException) {
                response?.close()
                retryCount++
                val delayMs = (baseRetryDelayMs * (1 shl minOf(retryCount - 1, 5))).coerceAtMost(maxRetryDelayMs)
                useRangeHeader = downloadedBytes > 0
                Log.w(TAG, "[RETRY] IO error: ${com.hasanege.materialtv.utils.StringUtils.sanitizeUrl(e.message)}, retry $retryCount/$maxRetries")
                kotlinx.coroutines.delay(delayMs)
            } catch (e: Exception) {
                response?.close()
                if (activeDownloads[id] == true) {
                    val sanitizedError = com.hasanege.materialtv.utils.StringUtils.sanitizeUrl(e.message ?: "Bilinmeyen hata")
                    Log.e(TAG, "[ERROR] Unexpected error: $sanitizedError")
                    onError(sanitizedError)
                }
                return@withContext
            }
        }
        
        // Max retries reached
        if (retryCount >= maxRetries && activeDownloads[id] == true) {
            Log.e(TAG, "[FAILED] Max retries ($maxRetries) reached")
            onError("Bağlantı başarısız. Lütfen internet bağlantınızı kontrol edin ve tekrar deneyin.")
        }
        
        activeDownloads.remove(id)
    }
    
    /**
     * İndirmeyi duraklat
     */
    fun pause(id: String) {
        activeDownloads[id] = false
    }
    
    /**
     * İndirmeyi iptal et ve dosyayı sil
     */
    fun cancel(id: String, filePath: String) {
        activeDownloads[id] = false
        try {
            File(filePath).delete()
        } catch (_: Exception) {}
    }
    
    /**
     * İndirme aktif mi kontrol et
     */
    fun isActive(id: String): Boolean {
        return activeDownloads[id] == true
    }
}
