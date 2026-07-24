package com.hasanege.materialtv.download

import android.util.Log
import java.io.File

/**
 * Helper for cleaning up download files and empty directories.
 */
object DownloadCleanupHelper {
    private const val TAG = "DownloadCleanupHelper"
    private const val BASE_FOLDER = "MaterialTV"

    /**
     * Cleans up all files associated with a download.
     * Returns true if the primary video file was deleted or was already missing.
     */
    fun cleanupDownloadFiles(download: DownloadItem): Boolean {
        var videoDeletedOrMissing = false
        try {
            val videoFile = File(download.filePath)
            val parentDir = videoFile.parentFile

            // 1. Delete Primary Video File
            if (videoFile.exists()) {
                if (videoFile.delete()) {
                    Log.d(TAG, "Deleted video: ${videoFile.name}")
                    videoDeletedOrMissing = true
                } else {
                    Log.e(TAG, "Failed to delete video: ${videoFile.name}")
                }
            } else {
                Log.d(TAG, "Video file already missing: ${videoFile.name}")
                videoDeletedOrMissing = true
            }

            // 2. Delete Thumbnails
            // VideoThumbnailHelper style: ${name}_thumb.jpg
            val videoThumb = VideoThumbnailHelper.getThumbnailFile(videoFile)
            if (videoThumb.exists()) {
                videoThumb.delete()
                Log.d(TAG, "Deleted video thumbnail: ${videoThumb.name}")
            }

            // Legacy style: E${number}_thumbnail.png
            if (download.contentType == ContentType.EPISODE && download.episodeNumber != null && parentDir != null) {
                val legacyThumb = File(parentDir, "E${download.episodeNumber}_thumbnail.png")
                if (legacyThumb.exists()) {
                    legacyThumb.delete()
                }
            }

            // 3. Delete Movie Cover (MovieName.png)
            if (download.contentType == ContentType.MOVIE && parentDir != null) {
                val movieCover = File(parentDir, "${videoFile.nameWithoutExtension}.png")
                if (movieCover.exists()) {
                    movieCover.delete()
                    Log.d(TAG, "Deleted movie cover: ${movieCover.name}")
                }
            }

            // 4. Cleanup Empty Folders Recursively
            if (parentDir != null && parentDir.exists()) {
                cleanupEmptyFoldersRecursive(parentDir)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}", e)
        }
        return videoDeletedOrMissing
    }

    /**
     * Traverses up and deletes empty folders until it hits the MaterialTV base or a non-empty folder.
     */
    private fun cleanupEmptyFoldersRecursive(dir: File) {
        try {
            if (!dir.exists() || !dir.isDirectory) return
            
            // Stop if we reached beyond MaterialTV folder or root
            if (dir.name == BASE_FOLDER || dir.parentFile == null) {
                // We might want to delete MaterialTV if it's empty too, 
                // but usually better to stop here to be safe.
                if (dir.name == BASE_FOLDER && isDirEmpty(dir)) {
                    // dir.delete() // Optional: Delete MaterialTV if totally empty
                }
                return
            }

            if (isDirEmpty(dir)) {
                val parent = dir.parentFile
                if (dir.delete()) {
                    Log.d(TAG, "Deleted empty directory: ${dir.absolutePath}")
                    if (parent != null) {
                        cleanupEmptyFoldersRecursive(parent)
                    }
                }
            } else {
                // Not empty, but check if it only contains other empty directories (handled by recursion usually)
                Log.d(TAG, "Directory not empty, stopping cleanup: ${dir.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning folders: ${e.message}")
        }
    }

    private fun isDirEmpty(dir: File): Boolean {
        val files = dir.listFiles()
        return files == null || files.isEmpty()
    }

    /**
     * Removes files that are empty (0 bytes) or corrupted.
     * This avoids "ghost" files in the downloads folder.
     */
    fun removeCorruptedOrEmptyFile(filePath: String) {
        try {
            val file = File(filePath)
            if (file.exists() && file.isFile) {
                if (file.length() == 0L) {
                    file.delete()
                    Log.d(TAG, "Deleted empty corrupted file: ${file.name}")
                    
                    // Cleanup parent if it became empty
                    val parent = file.parentFile
                    if (parent != null) {
                        cleanupEmptyFoldersRecursive(parent)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning corrupted file: ${e.message}")
        }
    }
    
    /**
     * Utility to check if a series-level cover should be deleted.
     * This should be called by the manager after knowing no more episodes exist in DB.
     */
    fun cleanupSeriesCover(seriesName: String, seriesPath: String?) {
        if (seriesPath == null) return
        try {
            val seriesDir = File(seriesPath)
            if (seriesDir.exists() && seriesDir.isDirectory) {
                val cover = File(seriesDir, "cover.png")
                if (cover.exists()) {
                    cover.delete()
                    Log.d(TAG, "Deleted series cover for: $seriesName")
                }
                // Try to delete the folder if now empty
                cleanupEmptyFoldersRecursive(seriesDir)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning series cover: ${e.message}")
        }
    }
}
