package com.hasanege.materialtv

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hasanege.materialtv.download.DownloadItem
import com.hasanege.materialtv.download.DownloadManagerImpl
import com.hasanege.materialtv.download.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import android.net.Uri

/**
 * DownloadsViewModel - Yeni indirme sistemi
 */
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private var downloadManager: DownloadManagerImpl? = null
    
    // Raw downloads from database (unfiltered)
    private val _rawDownloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    
    // Filtered downloads for UI
    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _selectedFilter = MutableStateFlow(DownloadFilter.ALL)
    val selectedFilter: StateFlow<DownloadFilter> = _selectedFilter.asStateFlow()
    
    private val _scanMessage = MutableStateFlow<String?>(null)
    val scanMessage: StateFlow<String?> = _scanMessage.asStateFlow()
    
    init {
        initialize()
    }

    fun clearScanMessage() {
        _scanMessage.value = null
    }

    fun initialize() {
        downloadManager = DownloadManagerImpl.getInstance(context)
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
             downloadManager?.downloads?.collect { items ->
                 _rawDownloads.value = items
                 val filtered = filterDownloads(items, _selectedFilter.value)
                 _downloads.value = filtered
             }
        }
        
        // Listen to scan status updates
        viewModelScope.launch {
            downloadManager?.scanStatus?.collect { status ->
                if (status != null) {
                    _scanMessage.value = status
                }
            }
        }
    }
    
    // ... filtering methods ...
    
    fun setFilter(filter: DownloadFilter) {
        _selectedFilter.value = filter
        // Always filter from raw data, not from already filtered data
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val filtered = filterDownloads(_rawDownloads.value, filter)
            _downloads.value = filtered
        }
    }
    
    private fun filterDownloads(items: List<DownloadItem>, filter: DownloadFilter): List<DownloadItem> {
        // First, exclude CANCELLED and FAILED downloads from the list
        val activeItems = items.filter { 
            it.status != DownloadStatus.CANCELLED && it.status != DownloadStatus.FAILED 
        }
        
        return when (filter) {
            DownloadFilter.ALL -> activeItems
            DownloadFilter.DOWNLOADING -> activeItems.filter { 
                it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING 
            }
            DownloadFilter.COMPLETED -> activeItems.filter { it.status == DownloadStatus.COMPLETED }
            DownloadFilter.PAUSED -> activeItems.filter { it.status == DownloadStatus.PAUSED }
        }
    }
    
    fun pauseDownload(id: String) {
        downloadManager?.pauseDownload(id)
    }
    
    fun resumeDownload(id: String) {
        downloadManager?.resumeDownload(id)
    }
    
    fun cancelDownload(id: String) {
        downloadManager?.cancelDownload(id)
    }
    
    fun deleteDownload(id: String) {
        downloadManager?.deleteDownload(id)
    }
    
    fun renameDownload(id: String, newTitle: String) {
        downloadManager?.renameDownload(id, newTitle)
    }
    
    /**
     * Mevcut indirmeleri yeniden tara
     */
    fun rescanDownloads() {
        viewModelScope.launch {
            _isLoading.value = true
            // Artificial delay for aesthetic checking (as requested by user)
            kotlinx.coroutines.delay(1500)
            val count = downloadManager?.scanExistingDownloads() ?: 0
            _isLoading.value = false
            _scanMessage.value = "Bulunan dosya: $count"
        }
    }

    fun setCustomDownloadFolder(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            downloadManager?.setCustomDownloadFolder(uri)
            // Artificial delay for aesthetic checking
            kotlinx.coroutines.delay(1500)
            val count = downloadManager?.scanExistingDownloads() ?: 0
            _isLoading.value = false
            _scanMessage.value = "Klasör seçildi. Bulunan dosya: $count"
        }
    }
    
    fun playDownload(context: Context, download: DownloadItem) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val file = File(download.filePath)
            val exists = file.exists()
            
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (exists) {
                    // Find existing watch history for this download
                    val downloadId = WatchHistoryManager.getDownloadId(download.filePath)
                    val historyItem = WatchHistoryManager.getHistory().find { it.streamId == downloadId }
                    val resumePosition = historyItem?.position ?: 0L
    
                    val intent = Intent(context, PlayerActivity::class.java).apply {
                        // Use URI for local file playback (PlayerActivity expects this)
                        putExtra("URI", download.filePath)
                        putExtra("TITLE", download.title)
                        putExtra("IS_DOWNLOADED_FILE", true)
                        putExtra("position", resumePosition)
                    }
                    context.startActivity(intent)
                } else {
                    android.widget.Toast.makeText(context, "Dosya bulunamadı", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

enum class DownloadFilter {
    ALL, DOWNLOADING, COMPLETED, PAUSED
}
