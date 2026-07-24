package com.hasanege.materialtv.download

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.hasanege.materialtv.data.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Foreground Service ile arka plan indirme
 */
class DownloadService : Service() {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val downloader = OkHttpDownloader()
    private lateinit var repository: DownloadRepository
    private lateinit var notificationHelper: DownloadNotificationHelper
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var coverScraper: CoverScraper
    
    // Wake locks: CPU ve WiFi uyku moduna geçmesin
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    
    // Aktif indirmeler
    private val activeJobs = mutableMapOf<String, Job>()
    
    // Queue processor - sadece tek bir instance çalışmalı
    private var queueProcessorJob: Job? = null
    
    // Kullanıcı pause yaptı mı? True ise kuyruk otomatik ilerlemesin
    @Volatile
    private var userPausedQueue = false
    
    companion object {
        private const val TAG = "DownloadService"
        
        const val ACTION_START = "com.hasanege.materialtv.download.START"
        const val ACTION_PAUSE = "com.hasanege.materialtv.download.PAUSE"
        const val ACTION_RESUME = "com.hasanege.materialtv.download.RESUME"
        const val ACTION_CANCEL = "com.hasanege.materialtv.download.CANCEL"
        const val ACTION_PAUSE_ALL = "com.hasanege.materialtv.download.PAUSE_ALL"
        const val ACTION_RESUME_ALL = "com.hasanege.materialtv.download.RESUME_ALL"
        const val ACTION_RETRY = "com.hasanege.materialtv.download.RETRY"
        
        const val EXTRA_DOWNLOAD_ID = "download_id"
        
        fun start(context: Context) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }
        
        fun pause(context: Context, downloadId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_PAUSE
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            context.startService(intent)
        }
        
        fun resume(context: Context, downloadId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_RESUME
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            // Use startForegroundService to ensure service starts properly
            context.startForegroundService(intent)
        }
        
        fun cancel(context: Context, downloadId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            context.startService(intent)
        }
        
        fun retry(context: Context, downloadId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_RETRY
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            context.startForegroundService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        repository = DownloadRepository(this)
        notificationHelper = DownloadNotificationHelper(this)
        settingsRepository = SettingsRepository.getInstance(this)
        coverScraper = CoverScraper(this)
        
        // Acquire wake locks to prevent CPU and WiFi from sleeping
        acquireWakeLocks()
        
        Log.d(TAG, "DownloadService created with wake locks")
    }
    
    /**
     * Wake lock ve WiFi lock al - arka plan indirmesi için gerekli
     */
    private fun acquireWakeLocks() {
        try {
            // CPU wake lock
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MaterialTV:DownloadWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(60 * 60 * 1000L) // Max 1 hour, will be renewed if needed
            }
            
            // WiFi lock
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiLock = wifiManager.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "MaterialTV:DownloadWifiLock"
            ).apply {
                setReferenceCounted(false)
                acquire()
            }
            
            Log.d(TAG, "Wake locks acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake locks: ${e.message}")
        }
    }
    
    /**
     * Wake lock ve WiFi lock bırak
     */
    private fun releaseWakeLocks() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
            wifiLock?.let {
                if (it.isHeld) it.release()
            }
            Log.d(TAG, "Wake locks released")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release wake locks: ${e.message}")
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // CRITICAL: Always start foreground IMMEDIATELY for ANY intent
        // This MUST happen within 5 seconds of startForegroundService() being called
        // Android 12+ is very strict about this - call it FIRST before any other logic
        startForegroundWithNotification()
        
        when (intent?.action) {
            ACTION_START -> {
                // Yeni indirme başlatıldığında kuyruğu aktif et
                userPausedQueue = false
                processDownloadQueue()
            }
            ACTION_PAUSE -> {
                val id = intent.getStringExtra(EXTRA_DOWNLOAD_ID)
                if (id != null) pauseDownload(id)
            }
            ACTION_RESUME -> {
                val id = intent.getStringExtra(EXTRA_DOWNLOAD_ID)
                if (id != null) resumeDownload(id)
            }
            ACTION_CANCEL -> {
                val id = intent.getStringExtra(EXTRA_DOWNLOAD_ID)
                if (id != null) cancelDownload(id)
            }
            ACTION_PAUSE_ALL -> {
                pauseAllDownloads()
            }
            ACTION_RESUME_ALL -> {
                resumeAllDownloads()
            }
            ACTION_RETRY -> {
                val id = intent.getStringExtra(EXTRA_DOWNLOAD_ID)
                if (id != null) retryDownload(id)
            }
        }
        
        return START_STICKY
    }
    
    private fun startForegroundWithNotification() {
        val notification = notificationHelper.createForegroundNotification(
            activeCount = 0,
            currentTitle = null,
            progress = 0
        )
        startForeground(DownloadNotificationHelper.FOREGROUND_NOTIFICATION_ID, notification)
    }
    
    /**
     * İndirme kuyruğunu işle
     * SADECE TEK BİR INSTANCE ÇALIŞIR - birden fazla çağrı yapılsa bile
     */
    private fun processDownloadQueue() {
        // Zaten çalışan bir queue processor varsa, yenisini başlatma
        if (queueProcessorJob?.isActive == true) {
            Log.d(TAG, "Queue processor already running, skipping new instance")
            return
        }
        
        queueProcessorJob = scope.launch {
            Log.d(TAG, "Queue processor started")
            while (isActive) {
                try {
                    val maxConcurrent = settingsRepository.maxConcurrentDownloads.first()
                    val activeCount = activeJobs.count { it.value.isActive }
                    
                    // Daha fazla indirme başlatabilir miyiz?
                    // userPausedQueue true ise otomatik olarak sıradaki indirmeyi başlatma
                    if (activeCount < maxConcurrent && !userPausedQueue) {
                        val pendingDownload = repository.getDownloadsByStatus(DownloadStatus.PENDING)
                            .first()
                            .firstOrNull { it.id !in activeJobs.keys }
                        
                        if (pendingDownload != null) {
                            startDownloadJob(pendingDownload)
                            // İndirme başlatıldıktan sonra biraz bekle ki activeJobs güncellensin
                            delay(500)
                            continue // Hemen bir sonraki iterasyona geç ama yeni indirme başlatmadan önce bekle
                        }
                    }
                    
                    // Servisi sadece HİÇ indirme yoksa durdur (aktif, bekleyen VEYA duraklatılmış)
                    val pendingCount = repository.getPendingCount()
                    val pausedCount = repository.getDownloadsByStatus(DownloadStatus.PAUSED)
                        .first()
                        .size
                    
                    if (activeJobs.isEmpty() && pendingCount == 0 && pausedCount == 0) {
                        Log.d(TAG, "No downloads at all, stopping service")
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        break
                    }
                    
                    // Bildirimi güncelle
                    updateNotification()
                    
                    delay(1000) // Her saniye kontrol et
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing queue", e)
                    delay(5000)
                }
            }
            Log.d(TAG, "Queue processor stopped")
        }
    }
    
    /**
     * İndirme işini başlat
     * Speed monitoring ayrı coroutine'de çalışıyor böylece race condition önleniyor
     */
    private fun startDownloadJob(item: DownloadItem) {
        Log.d(TAG, "Starting download: ${item.title}")
        
        // Retry sayacı - bu indirme için kaç kez hata aldık
        var downloadErrorRetryCount = 0
        val maxErrorRetries = 3
        
        val job = scope.launch {
            repository.updateStatus(item.id, DownloadStatus.DOWNLOADING)
            
            // Speed tracking için shared state
            var currentBytes = item.downloadedBytes
            var shouldRestartDueToSpeed = false
            var downloadCompleted = false
            
            // Speed monitoring job - ayrı coroutine'de çalışıyor
            val speedMonitorJob = launch {
                var lastBytesForSpeed = currentBytes
                var lastCheckTime = System.currentTimeMillis()
                val speedHistory = mutableListOf<Long>()
                val maxHistorySize = 10
                var consecutiveLowCount = 0
                val requiredLowReadings = 5
                
                try {
                    while (isActive && !downloadCompleted && !shouldRestartDueToSpeed) {
                        delay(1000) // Her saniye kontrol et
                        
                        val autoRestart = settingsRepository.autoRestartOnSpeedDrop.first()
                        if (!autoRestart) continue
                        
                        val currentItem = repository.getDownloadById(item.id) ?: continue
                        val progress = currentItem.progress
                        
                        // %1 ile %100 arasında speed monitoring yap (Kullanıcı isteği)
                        if (progress < 1) continue
                        
                        val now = System.currentTimeMillis()
                        val bytesDelta = currentItem.downloadedBytes - lastBytesForSpeed
                        val timeDeltaS = (now - lastCheckTime) / 1000.0
                        
                        // Gerçek hızı delta üzerinden hesapla (onProgress'ten bağımsız stall tespiti için)
                        val calculatedSpeed = if (timeDeltaS > 0) (bytesDelta / timeDeltaS).toLong() else 0L
                        
                        lastBytesForSpeed = currentItem.downloadedBytes
                        lastCheckTime = now
                        
                        val minSpeedKbps = settingsRepository.minDownloadSpeedKbps.first()
                        val minSpeedBytes = minSpeedKbps * 1024L
                        
                        speedHistory.add(calculatedSpeed)
                        if (speedHistory.size > maxHistorySize) {
                            speedHistory.removeAt(0)
                        }
                        
                        if (speedHistory.size >= 3) { // 3 saniye sonra kontrole başla
                            val avgSpeed = speedHistory.takeLast(5).average().toLong()
                            
                            if (avgSpeed < minSpeedBytes) {
                                consecutiveLowCount++
                                Log.w(TAG, "[SPEED-LOW] ${item.title}: progress=$progress% | avg=${avgSpeed/1024}KB/s | calculated=${calculatedSpeed/1024}KB/s | threshold=${minSpeedKbps}KB/s, count=$consecutiveLowCount/$requiredLowReadings")
                                
                                if (consecutiveLowCount >= requiredLowReadings) {
                                    Log.w(TAG, "[SPEED-RESTART] Triggering restart due to low speed/stall: ${item.title}")
                                    shouldRestartDueToSpeed = true
                                    // Downloader'ı durdur
                                    downloader.pause(item.id)
                                    break
                                }
                            } else {
                                if (consecutiveLowCount > 0) {
                                    Log.d(TAG, "[SPEED-OK] Speed recovered: ${avgSpeed/1024}KB/s >= ${minSpeedKbps}KB/s")
                                }
                                consecutiveLowCount = 0
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "[SPEED-MONITOR] Error: ${e.message}")
                }
            }
            
            downloader.download(
                id = item.id,
                url = item.url,
                filePath = item.filePath,
                onProgress = { downloadedBytes, totalBytes, speed ->
                    
                    scope.launch {
                        val progress = if (totalBytes > 0) {
                            ((downloadedBytes * 100) / totalBytes).toInt()
                        } else 0
                        repository.updateProgress(item.id, progress, downloadedBytes, speed)
                        
                        Log.d(TAG, "[PROGRESS] ${item.title}: $progress% | ${downloadedBytes/1024}KB / ${totalBytes/1024}KB | Speed: ${speed/1024}KB/s")
                    }
                },
                onComplete = {
                    downloadCompleted = true
                    speedMonitorJob.cancel()
                    
                    scope.launch {
                        // Video süresini al
                        val duration = extractVideoDuration(item.filePath)
                        
                        // Durumu ve süreyi güncelle
                        val updatedItem = item.copy(
                            status = DownloadStatus.COMPLETED,
                            duration = duration
                        )
                        repository.updateDownload(updatedItem)
                        
                        if (settingsRepository.downloadNotificationsEnabled.first()) {
                            notificationHelper.showCompletedNotification(item.id.hashCode(), item.title)
                        }
                        activeJobs.remove(item.id)
                        Log.d(TAG, "Download completed: ${item.title} (duration: ${duration}ms)")
                        
                        // Kapak resmini indir (Öncelik: Xtream/Sağlanan URL -> Fallback: Scraper)
                        when (item.contentType) {
                            ContentType.MOVIE -> {
                                var coverSuccess = false
                                val coverUrl = item.thumbnailUrl
                                if (!coverUrl.isNullOrEmpty()) {
                                    try {
                                        coverSuccess = downloadCoverImage(coverUrl, item.filePath, item.contentType)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to download provided movie cover: ${e.message}")
                                    }
                                }
                                
                                // Fallback to Scraper if provided cover failed or was missing
                                if (!coverSuccess) {
                                    try {
                                        Log.d(TAG, "Using Scraper fallback for Movie: ${item.title}")
                                        val result = coverScraper.findAndDownloadCover(item.title, item)
                                        if (result != null) {
                                            // Update DB with scraped title and cover
                                            val scrapedUpdate = item.copy(
                                                status = DownloadStatus.COMPLETED,
                                                duration = duration,
                                                title = result.successfulQuery, // Smart Local Title
                                                thumbnailUrl = result.coverPath
                                            )
                                            repository.updateDownload(scrapedUpdate)
                                            // Skip the final update below since we just updated
                                            return@launch
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Scraper fallback failed: ${e.message}")
                                    }
                                }
                                
                                // Also generate video thumbnail for movies (secondary fallback/addition)
                                try {
                                    VideoThumbnailHelper.extractAndSaveThumbnail(this@DownloadService, java.io.File(item.filePath))
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to generate video thumbnail: ${e.message}")
                                }
                            }
                            ContentType.EPISODE -> {
                                var coverSuccess = false
                                val seriesCover = item.seriesCoverUrl
                                if (!seriesCover.isNullOrEmpty()) {
                                    try {
                                        coverSuccess = downloadCoverImage(seriesCover, item.filePath, item.contentType)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to download provided series cover: ${e.message}")
                                    }
                                }
                                
                                // Fallback to Scraper if provided cover failed
                                if (!coverSuccess) {
                                    try {
                                        Log.d(TAG, "Using Scraper fallback for Series: ${item.seriesName}")
                                        val searchTitle = item.seriesName ?: item.title
                                        val result = coverScraper.findAndDownloadCover(searchTitle, item)
                                        if (result != null) {
                                            // Update DB with scraped cover (Series Name usually stays, or update it?)
                                            // For episodes, we usually update seriesName if it was generic
                                            val scrapedUpdate = item.copy(
                                                status = DownloadStatus.COMPLETED,
                                                duration = duration,
                                                seriesName = result.successfulQuery, // Update series name
                                                seriesCoverUrl = result.coverPath
                                                // Don't change item.title (Episode title)
                                            )
                                            repository.updateDownload(scrapedUpdate)
                                            return@launch
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Scraper fallback failed: ${e.message}")
                                    }
                                }
                                
                                // Generate video thumbnail for episodes
                                try {
                                    VideoThumbnailHelper.extractAndSaveThumbnail(this@DownloadService, java.io.File(item.filePath))
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to generate video thumbnail: ${e.message}")
                                }
                            }
                        }
                    }
                },
                onError = { error ->
                    downloadCompleted = true
                    speedMonitorJob.cancel()
                    
                    scope.launch {
                        val shouldAutoRetry = settingsRepository.autoRetryFailedDownloads.first()
                        downloadErrorRetryCount++
                        
                        if (shouldAutoRetry && downloadErrorRetryCount <= maxErrorRetries) {
                            Log.w(TAG, "[AUTO-RETRY] Download error, retrying ($downloadErrorRetryCount/$maxErrorRetries): ${item.title} - $error")
                            
                            // Exponential backoff ile bekle
                            val delayMs = 5000L * downloadErrorRetryCount
                            delay(delayMs)
                            
                            // Tekrar PENDING yap - kuyruk işleyicisi alacak
                            repository.updateStatus(item.id, DownloadStatus.PENDING)
                            activeJobs.remove(item.id)
                        } else {
                            // Max retry aşıldı veya auto-retry kapalı
                            repository.updateStatus(item.id, DownloadStatus.FAILED, error)
                            
                            // Clean up if it's a 0-byte ghost file
                            DownloadCleanupHelper.removeCorruptedOrEmptyFile(item.filePath)
                            
                            if (settingsRepository.downloadNotificationsEnabled.first()) {
                                notificationHelper.showFailedNotification(item.id.hashCode(), item.title, error)
                            }
                            activeJobs.remove(item.id)
                            Log.e(TAG, "Download permanently failed after $downloadErrorRetryCount retries: ${item.title} - $error")
                        }
                    }
                }
            )
            
            // downloader.download tamamlandıktan sonra speed restart kontrolü
            // Eğer speed düşüşü nedeniyle durdu ise, bekleyip yeniden başlat
            if (shouldRestartDueToSpeed && !downloadCompleted) {
                val restartDelaySeconds = settingsRepository.speedRestartDelaySeconds.first()
                Log.w(TAG, "[SPEED-RESTART] Download paused due to slow speed, waiting ${restartDelaySeconds}s before restart: ${item.title}")
                // NOT: activeJobs'tan çıkarmıyoruz - slot işgal etmeye devam etsin ki yeni indirme başlamasın
                repository.updateStatus(item.id, DownloadStatus.PAUSED)
                
                // IPTV sunucusunun bağlantı limitini sıfırlaması için bekle (kullanıcı ayarlı)
                delay(restartDelaySeconds * 1000L)
                
                // Şimdi activeJobs'tan çıkar ve PENDING yap
                // Speed restart otomatik bir işlem, userPausedQueue'u etkilememeli
                // Ama kuyruk ilerleyebilmesi için false yapmalıyız
                userPausedQueue = false
                activeJobs.remove(item.id)
                repository.updateStatus(item.id, DownloadStatus.PENDING)
                Log.d(TAG, "[SPEED-RESTART] Download queued for restart: ${item.title}")
            }
        }
        
        activeJobs[item.id] = job
    }
    
    /**
     * İndirmeyi duraklat
     * Kullanıcı pause yaptığında kuyruk otomatik ilerlemesin
     */
    private fun pauseDownload(id: String) {
        Log.d(TAG, "Pausing download: $id")
        // Kullanıcı pause yaptı, kuyruk otomatik ilerlemesin
        userPausedQueue = true
        downloader.pause(id)
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        scope.launch {
            val download = repository.getDownloadById(id)
            if (download != null) {
                // If the file is 0 bytes when paused, delete it to avoid ghost files
                DownloadCleanupHelper.removeCorruptedOrEmptyFile(download.filePath)
            }
            repository.updateStatus(id, DownloadStatus.PAUSED)
        }
    }
    
    /**
     * İndirmeye devam et
     * OkHttpDownloader otomatik olarak Range header'ı dener, başarısız olursa sıfırdan başlar
     * Resume edilen indirme hemen başlatılır (slot varsa)
     */
    private fun resumeDownload(id: String) {
        Log.d(TAG, "Resuming download: $id")
        // Kullanıcı resume yaptı, kuyruğu tekrar aktif et
        userPausedQueue = false
        // Start foreground notification first
        startForegroundWithNotification()
        scope.launch {
            val download = repository.getDownloadById(id)
            if (download == null) {
                Log.e(TAG, "Download not found: $id")
                return@launch
            }
            
            val maxConcurrent = settingsRepository.maxConcurrentDownloads.first()
            val activeCount = activeJobs.count { it.value.isActive }
            
            // Eğer aktif slot varsa, hemen başlat
            if (activeCount < maxConcurrent) {
                Log.d(TAG, "Slot available, starting resumed download immediately: ${download.title}")
                startDownloadJob(download)
            } else {
                // Slot yoksa, PENDING yap ve kuyruğa ekle
                Log.d(TAG, "No slot available, queuing resumed download: ${download.title}")
                repository.updateStatus(id, DownloadStatus.PENDING)
            }
            
            // Kuyruk işlemeyi güncelle
            processDownloadQueue()
        }
    }
    
    /**
     * Başarısız indirmeyi yeniden dene
     * Failed durumundaki bir indirmeyi PENDING yaparak kuyruğa ekler
     */
    private fun retryDownload(id: String) {
        Log.d(TAG, "Retrying failed download: $id")
        scope.launch {
            val download = repository.getDownloadById(id)
            if (download != null && download.status == DownloadStatus.FAILED) {
                // Error mesajını temizle ve PENDING yap
                repository.updateStatus(id, DownloadStatus.PENDING, null)
                Log.d(TAG, "Download queued for retry: ${download.title}")
                // Kuyruk işlemeyi başlat
                processDownloadQueue()
            } else {
                Log.w(TAG, "Cannot retry download $id - not in FAILED status")
            }
        }
    }
    
    /**
     * İndirmeyi iptal et
     */
    private fun cancelDownload(id: String) {
        Log.d(TAG, "Cancelling download: $id")
        scope.launch {
            val download = repository.getDownloadById(id)
            if (download != null) {
                // Eğer aktif indirme iptal ediliyorsa, kuyruk devam etsin
                if (download.status == DownloadStatus.DOWNLOADING) {
                    userPausedQueue = false
                }
                downloader.cancel(id, download.filePath)
                
                // 1. Physical Cleanup First
                val success = DownloadCleanupHelper.cleanupDownloadFiles(download)
                
                // 2. Only delete from DB if file is gone (or wasn't there)
                if (success) {
                    repository.deleteDownload(id)
                    Log.d(TAG, "Record deleted from DB (cancelled): ${download.title}")
                    
                    // 3. Series-level cleanup if needed
                    val seriesName = download.seriesName
                    if (download.contentType == ContentType.EPISODE && seriesName != null) {
                        val remaining = repository.getAllDownloads()
                            .first()
                            .filter { it.seriesName == seriesName }
                        
                        if (remaining.isEmpty()) {
                            val videoFile = java.io.File(download.filePath)
                            val seriesDir = videoFile.parentFile?.parentFile
                            DownloadCleanupHelper.cleanupSeriesCover(seriesName, seriesDir?.absolutePath)
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to delete files during cancellation, record kept in DB: ${download.title}")
                }
            }
            activeJobs[id]?.cancel()
            activeJobs.remove(id)
            
            // Bir slot boşaldı, kuyruktaki sonraki indirmeyi başlat
            processDownloadQueue()
        }
    }
    
    
    /**
     * Tüm indirmeleri duraklat
     */
    private fun pauseAllDownloads() {
        activeJobs.keys.toList().forEach { pauseDownload(it) }
    }
    
    /**
     * Tüm duraklatılmış indirmeleri devam ettir
     */
    private fun resumeAllDownloads() {
        Log.d(TAG, "Resuming all paused downloads")
        userPausedQueue = false
        scope.launch {
            val pausedDownloads = repository.getDownloadsByStatus(DownloadStatus.PAUSED).first()
            val maxConcurrent = settingsRepository.maxConcurrentDownloads.first()
            
            pausedDownloads.forEachIndexed { index, download ->
                if (index < maxConcurrent) {
                    // Slot varsa hemen başlat
                    startDownloadJob(download)
                } else {
                    // Slot yoksa PENDING yap
                    repository.updateStatus(download.id, DownloadStatus.PENDING)
                }
            }
            
            // Kuyruk işlemeyi başlat
            processDownloadQueue()
        }
    }
    
    /**
     * Son bildirim güncelleme zamanı - rate limiting için
     */
    private var lastNotificationTime = 0L
    private val NOTIFICATION_MIN_INTERVAL_MS = 500L // Minimum 500ms between notifications
    
    /**
     * Bildirimi güncelle (throttled - max 2 per second)
     */
    private fun updateNotification() {
        val now = System.currentTimeMillis()
        if (now - lastNotificationTime < NOTIFICATION_MIN_INTERVAL_MS) {
            return // Skip this update, too soon
        }
        lastNotificationTime = now
        
        scope.launch {
            try {
                val activeDownloads = repository.getActiveDownloads().first()
                val pausedDownloads = repository.getDownloadsByStatus(DownloadStatus.PAUSED).first()
                val current = activeDownloads.firstOrNull { it.status == DownloadStatus.DOWNLOADING }
                
                val notification = notificationHelper.createForegroundNotification(
                    activeCount = activeDownloads.count { it.status == DownloadStatus.DOWNLOADING },
                    currentTitle = current?.title,
                    progress = current?.progress ?: 0,
                    downloadedBytes = current?.downloadedBytes ?: 0L,
                    totalBytes = current?.totalBytes ?: 0L,
                    speed = current?.speed ?: 0L,
                    pausedCount = pausedDownloads.size,
                    currentId = current?.id ?: pausedDownloads.firstOrNull()?.id
                )
                
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.notify(DownloadNotificationHelper.FOREGROUND_NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update notification: ${e.message}")
            }
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        releaseWakeLocks()
        Log.d(TAG, "DownloadService destroyed")
    }
    
    /**
     * Kapak resmini indir ve video dosyasının yanına kaydet
     * Film: FilmAdi.png (video dosyasıyla aynı isim)
     * Dizi: cover.png (dizi klasörüne)
     */
    /**
     * Kapak resmini indir ve video dosyasının yanına kaydet
     * Film: FilmAdi.png (video dosyasıyla aynı isim)
     * Dizi: cover.png (dizi klasörüne)
     * @return Başarılı ise true
     */
    private suspend fun downloadCoverImage(coverUrl: String, videoFilePath: String, contentType: ContentType): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val videoFile = java.io.File(videoFilePath)
                val parentDir = videoFile.parentFile ?: return@withContext false
                
                // Kapak dosyası adını belirle
                val coverFile = when (contentType) {
                    ContentType.MOVIE -> {
                        // Film için: video dosyasıyla aynı isim, .png uzantısı
                        java.io.File(parentDir, "${videoFile.nameWithoutExtension}.png")
                    }
                    ContentType.EPISODE -> {
                        // Dizi için: Sezon klasörünün üstündeki dizi klasörüne cover.png
                        // Yapı: Series/DiziAdi/S01/E01.mp4 -> Series/DiziAdi/cover.png
                        val seriesDir = parentDir.parentFile ?: parentDir
                        java.io.File(seriesDir, "cover.png")
                    }
                }
                
                // Zaten varsa atla - Success sayılır
                if (coverFile.exists()) {
                    Log.d(TAG, "Cover already exists: ${coverFile.absolutePath}")
                    return@withContext true
                }
                
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                
                val request = okhttp3.Request.Builder()
                    .url(coverUrl)
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Cover download failed: ${response.code}")
                        return@withContext false
                    }
                    
                    response.body?.byteStream()?.use { inputStream ->
                        java.io.FileOutputStream(coverFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    
                    Log.d(TAG, "Cover saved: ${coverFile.absolutePath}")
                    return@withContext true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download cover: ${com.hasanege.materialtv.utils.StringUtils.sanitizeUrl(e.message)}")
                return@withContext false
            }
        }
    }
    
    /**
     * Video süresini MediaMetadataRetriever ile al
     */
    private fun extractVideoDuration(filePath: String): Long {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract video duration: ${e.message}")
            0L
        }
    }
}
