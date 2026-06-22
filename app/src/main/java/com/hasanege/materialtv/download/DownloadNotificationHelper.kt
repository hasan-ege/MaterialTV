package com.hasanege.materialtv.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.hasanege.materialtv.MainActivity
import com.hasanege.materialtv.R

/**
 * İndirme bildirimleri yöneticisi
 * Material Design 3 stili ile modern bildirimler
 */
class DownloadNotificationHelper(private val context: Context) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        const val CHANNEL_ID = "download_channel"
        const val FOREGROUND_NOTIFICATION_ID = 1001
        
        // Action codes for resume all
        const val ACTION_RESUME_ALL = "com.hasanege.materialtv.download.RESUME_ALL"
    }
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Bildirim kanalını oluştur
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Dosya boyutunu okunabilir formata çevir
     */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> context.getString(R.string.unit_format_b, bytes)
            bytes < 1024 * 1024 -> context.getString(R.string.unit_format_kb, bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> context.getString(R.string.unit_format_mb, bytes / (1024.0 * 1024.0))
            else -> context.getString(R.string.unit_format_gb, bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
    
    /**
     * Hızı okunabilir formata çevir
     */
    private fun formatSpeed(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond < 1024 -> context.getString(R.string.unit_format_speed_bps, bytesPerSecond)
            bytesPerSecond < 1024 * 1024 -> context.getString(R.string.unit_format_speed_kbps, bytesPerSecond / 1024.0)
            else -> context.getString(R.string.unit_format_speed_mbps, bytesPerSecond / (1024.0 * 1024.0))
        }
    }
    
    /**
     * Kalan süreyi hesapla ve formatla
     */
    fun formatTimeRemaining(remainingBytes: Long, speed: Long): String? {
        if (speed <= 0 || remainingBytes <= 0) return null
        val seconds = remainingBytes / speed
        return when {
            seconds < 60 -> context.getString(R.string.notification_time_left_sec, seconds)
            seconds < 3600 -> context.getString(R.string.notification_time_left_min_sec, seconds / 60, seconds % 60)
            else -> context.getString(R.string.notification_time_left_hour_min, seconds / 3600, (seconds % 3600) / 60)
        }
    }
    
    /**
     * Foreground servis için ana bildirim
     * Daha detaylı ve güzel görünüm
     */
    fun createForegroundNotification(
        activeCount: Int,
        currentTitle: String?,
        progress: Int,
        downloadedBytes: Long = 0L,
        totalBytes: Long = 0L,
        speed: Long = 0L,
        pausedCount: Int = 0,
        currentId: String? = null
    ): Notification {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "downloads")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Pause All action
        val pauseAllIntent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_PAUSE_ALL
        }
        val pauseAllPendingIntent = PendingIntent.getService(
            context, 1, pauseAllIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Resume All action (Başlat butonu için)
        val resumeAllIntent = Intent(context, DownloadService::class.java).apply {
            action = DownloadService.ACTION_RESUME_ALL
        }
        val resumeAllPendingIntent = PendingIntent.getService(
            context, 2, resumeAllIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Başlık oluştur
        val title = when {
            activeCount == 0 && pausedCount > 0 -> context.getString(R.string.notification_paused_count, pausedCount)
            activeCount > 1 -> context.getString(R.string.notification_active_count, activeCount)
            activeCount == 1 && currentTitle != null -> {
                val ellipses = "..."
                currentTitle.take(35) + if (currentTitle.length > 35) ellipses else ""
            }
            else -> context.getString(R.string.notification_downloading)
        }
        
        // Alt metin oluştur - kalan süre dahil
        val contentText = buildString {
            if (progress > 0 && progress < 100) {
                append("$progress%")
                
                // Hız bilgisi
                if (speed > 0) {
                    append(" • ${formatSpeed(speed)}")
                }
            } else if (activeCount == 0 && pausedCount > 0) {
                append(context.getString(R.string.notification_tap_to_resume))
            } else {
                append(context.getString(R.string.notification_preparing))
            }
        }
        
        // BigText stili için genişletilmiş metin
        val bigText = buildString {
            if (progress > 0 && progress < 100) {
                if (currentTitle != null) {
                    appendLine(currentTitle)
                }
                appendLine()
                append(context.getString(R.string.notification_progress_label, progress))
                
                if (downloadedBytes > 0 && totalBytes > 0) {
                    appendLine()
                    append(context.getString(R.string.notification_size_label, formatFileSize(downloadedBytes), formatFileSize(totalBytes)))
                }
                
                if (speed > 0) {
                    appendLine()
                    append(context.getString(R.string.notification_speed_label, formatSpeed(speed)))
                    
                    // Kalan süre
                    val remainingBytes = totalBytes - downloadedBytes
                    formatTimeRemaining(remainingBytes, speed)?.let { timeRemaining ->
                        appendLine()
                        append(context.getString(R.string.notification_remaining_label, timeRemaining))
                    }
                }
                
                if (pausedCount > 0) {
                    appendLine()
                    append(context.getString(R.string.notification_paused_downloads_label, pausedCount))
                }
            } else if (activeCount == 0 && pausedCount > 0) {
                append(context.getString(R.string.notification_paused_count, pausedCount))
                append("\n")
                append(context.getString(R.string.notification_tap_to_resume))
            }
        }
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_download)
            .setOngoing(activeCount > 0 || pausedCount > 0)
            .setProgress(100, progress, progress == 0 && activeCount > 0)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
        
        // Genişletilmiş görünüm
        if (bigText.isNotEmpty()) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
        }
        
        // Butonlar
        if (activeCount == 1 && currentId != null) {
            // Tek bir aktif indirme varsa: Pause ve Cancel butonları
            val pauseIntent = Intent(context, DownloadService::class.java).apply {
                action = DownloadService.ACTION_PAUSE
                putExtra(DownloadService.EXTRA_DOWNLOAD_ID, currentId)
            }
            val pausePendingIntent = PendingIntent.getService(
                context, 3, pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val cancelIntent = Intent(context, DownloadService::class.java).apply {
                action = DownloadService.ACTION_CANCEL
                putExtra(DownloadService.EXTRA_DOWNLOAD_ID, currentId)
            }
            val cancelPendingIntent = PendingIntent.getService(
                context, 4, cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            builder.addAction(
                R.drawable.ic_pause,
                context.getString(R.string.notification_action_pause),
                pausePendingIntent
            )
            builder.addAction(
                R.drawable.ic_close,
                context.getString(R.string.notification_action_cancel),
                cancelPendingIntent
            )
        } else if (activeCount > 1) {
            // Birden fazla aktif indirme varsa: Tümünü duraklat butonu
            builder.addAction(
                R.drawable.ic_pause,
                context.getString(R.string.notification_action_pause),
                pauseAllPendingIntent
            )
        } else if (pausedCount > 0) {
            if (activeCount == 0 && pausedCount == 1 && currentId != null) {
                // Tek bir duraklatılmış indirme varsa: Resume ve Cancel butonları
                val resumeIntent = Intent(context, DownloadService::class.java).apply {
                    action = DownloadService.ACTION_RESUME
                    putExtra(DownloadService.EXTRA_DOWNLOAD_ID, currentId)
                }
                val resumePendingIntent = PendingIntent.getService(
                    context, 5, resumeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                val cancelIntent = Intent(context, DownloadService::class.java).apply {
                    action = DownloadService.ACTION_CANCEL
                    putExtra(DownloadService.EXTRA_DOWNLOAD_ID, currentId)
                }
                val cancelPendingIntent = PendingIntent.getService(
                    context, 6, cancelIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                builder.addAction(
                    R.drawable.ic_play,
                    context.getString(R.string.notification_action_resume),
                    resumePendingIntent
                )
                builder.addAction(
                    R.drawable.ic_close,
                    context.getString(R.string.notification_action_cancel),
                    cancelPendingIntent
                )
            } else {
                // Birden fazla duraklatılmış indirme varsa: Tümünü başlat butonu
                builder.addAction(
                    R.drawable.ic_play,
                    context.getString(R.string.notification_action_start),
                    resumeAllPendingIntent
                )
            }
        }
        
        return builder.build()
    }
    
    /**
     * İndirme tamamlandı bildirimi
     */
    fun showCompletedNotification(id: Int, title: String, fileSize: Long = 0L) {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "downloads")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val contentText = if (fileSize > 0) {
            context.getString(R.string.notification_completed_desc, formatFileSize(fileSize))
        } else {
            context.getString(R.string.downloads_completed)
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_completed_title, title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_download)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        
        notificationManager.notify(id, notification)
    }
    
    /**
     * İndirme başarısız bildirimi
     */
    fun showFailedNotification(id: Int, title: String, error: String?) {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "downloads")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val contentText = error?.take(100) ?: context.getString(R.string.unknown)
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_failed_title, title))
            .setContentText(context.getString(R.string.notification_failed_desc, contentText))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(context.getString(R.string.notification_failed_big_text, title, contentText)))
            .setSmallIcon(R.drawable.ic_download)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .build()
        
        notificationManager.notify(id, notification)
    }
    
    /**
     * İndirme duraklatıldı bildirimi
     */
    fun showPausedNotification(id: Int, title: String, progress: Int) {
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "downloads")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_paused_title, title))
            .setContentText(context.getString(R.string.downloads_completed_count, progress)) // Reusing count string or better add new one
            .setSmallIcon(R.drawable.ic_download)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(100, progress, false)
            .build()
        
        notificationManager.notify(id, notification)
    }
    
    /**
     * Bildirimi kaldır
     */
    fun cancelNotification(id: Int) {
        notificationManager.cancel(id)
    }
}
