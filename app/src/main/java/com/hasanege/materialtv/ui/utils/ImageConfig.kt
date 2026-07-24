package com.hasanege.materialtv.ui.utils

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger

/**
 * Optimized Coil ImageLoader configuration for smooth scrolling performance
 */
object ImageConfig {
    
    @Volatile
    private var imageLoader: ImageLoader? = null
    
    /**
     * Get or create optimized ImageLoader instance
     */
    fun getImageLoader(context: Context): ImageLoader {
        return imageLoader ?: synchronized(this) {
            imageLoader ?: createImageLoader(context).also { imageLoader = it }
        }
    }
    
    private fun createImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            // Memory cache configuration - aggressive caching for smooth scrolling
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // Use 25% of app's memory
                    .strongReferencesEnabled(true) // Keep strong references
                    .build()
            }
            // Disk cache configuration - large cache for offline viewing
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(512 * 1024 * 1024) // 512 MB disk cache
                    .build()
            }
            // Performance optimizations
            .respectCacheHeaders(false) // Ignore server cache headers for better caching
            .crossfade(true) // Smooth crossfade animation
            .crossfade(200) // Fast crossfade duration
            // Enable all cache policies
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            // Debug logging (disable in production)
            // .logger(DebugLogger())
            .build()
    }
}
