package com.kazukittin.vault

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.kazukittin.vault.data.remote.VaultNetworkClient

class VaultApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            // 共通のOkHttpClient（SsidInterceptor等を含む）を使用
            .okHttpClient { VaultNetworkClient.getOkHttpClient(this) }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    // 目標である 5GB (5L * 1024 * 1024 * 1024) のディスクキャッシュを割り当て
                    .maxSizeBytes(5L * 1024 * 1024 * 1024)
                    .build()
            }
            // ディスクキャッシュを積極的に利用するポリシー
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false) // NAS側のCache-Control(no-cache)を無視して強制キャッシュ
            .crossfade(true)
            .build()
    }
}
