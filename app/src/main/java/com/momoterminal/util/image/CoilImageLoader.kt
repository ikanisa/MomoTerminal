package com.momoterminal.util.image

import android.content.Context
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.momoterminal.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory class for creating a custom Coil ImageLoader with optimized configuration.
 */
@Singleton
class CoilImageLoaderFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    
    companion object {
        /**
         * Memory cache size as a percentage of available memory.
         */
        private const val MEMORY_CACHE_PERCENT = 0.25
        
        /**
         * Disk cache size in bytes (50MB).
         */
        private const val DISK_CACHE_SIZE = 50L * 1024 * 1024
        
        /**
         * Disk cache folder name.
         */
        private const val DISK_CACHE_FOLDER = "image_cache"
    }
    
    /**
     * Create a configured ImageLoader instance.
     *
     * Features:
     * - Memory cache (25% of available memory)
     * - Disk cache (50MB)
     * - OkHttpClient with certificate pinning integration
     * - SVG decoder support
     * - Crossfade animation enabled
     * - Error and placeholder drawables
     */
    fun create(): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(MEMORY_CACHE_PERCENT)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve(DISK_CACHE_FOLDER))
                    .maxSizeBytes(DISK_CACHE_SIZE)
                    .build()
            }
            .okHttpClient(okHttpClient)
            .components {
                add(SvgDecoder.Factory())
            }
            .crossfade(true)
            .crossfade(300)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(true)
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }
    
    /**
     * Create a minimal ImageLoader for low-memory situations.
     */
    fun createMinimal(): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.10) // 10% for minimal
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve(DISK_CACHE_FOLDER))
                    .maxSizeBytes(20L * 1024 * 1024) // 20MB for minimal
                    .build()
            }
            .okHttpClient(okHttpClient)
            .crossfade(false)
            .build()
    }
    
    /**
     * Clear all image caches.
     */
    fun clearCaches(imageLoader: ImageLoader) {
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
    }
}
