package com.hasanege.materialtv.download

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object VideoThumbnailHelper {
    
    /**
     * Extracts a thumbnail from a video file and saves it as a JPG.
     * Returns the File object of the saved thumbnail, or null if failed.
     * Checks if thumbnail already exists to avoid re-extraction.
     */
    suspend fun extractAndSaveThumbnail(context: Context, videoFile: File): File? {
        if (!videoFile.exists()) return null
        
        val thumbFile = File(videoFile.parentFile, "${videoFile.nameWithoutExtension}_thumb.jpg")
        if (thumbFile.exists() && thumbFile.length() > 0) return thumbFile
        
        return withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                // Use File Provider or absolute path? 
                // Since we have the file object and permission, absolute path string is often enough for MMR 
                // but setDataSource(Context, Uri) is safer.
                retriever.setDataSource(context, Uri.fromFile(videoFile))
                
                // Extract at 15 seconds to avoid intro black screens or studio logos
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val duration = durationStr?.toLongOrNull() ?: 0L
                
                // If video is short, take 10%. If long, take 15 seconds.
                val timeUs = if (duration > 30000) 15000000L else (duration * 0.1 * 1000).toLong()
                
                // Get frame
                val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                
                if (bitmap != null) {
                    FileOutputStream(thumbFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    thumbFile
                } else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                try { retriever.release() } catch (e: Exception) {}
            }
        }
    }

    /**
     * Gets the path to the thumbnail file for a given video file.
     * Does NOT generate it if missing.
     */
    fun getThumbnailFile(videoFile: File): File {
        return File(videoFile.parentFile, "${videoFile.nameWithoutExtension}_thumb.jpg")
    }
}
