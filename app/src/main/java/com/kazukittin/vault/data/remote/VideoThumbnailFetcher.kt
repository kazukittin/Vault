package com.kazukittin.vault.data.remote

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * 動画 URL から先頭フレームを取得する Coil Fetcher。
 * - MediaMetadataRetriever.setDataSource(url) はネイティブ HTTP Range リクエストを使うため
 *   OkHttp 経由で全ファイルをバッファせず OOM を回避できる。
 * - 取得したフレームは Coil の既存ディスクキャッシュ（VaultApplication で設定済み）に
 *   JPEG として保存し、次回以降はネットワークアクセスなしで返す。
 */
data class VideoThumbnailUrl(val url: String)

class VideoThumbnailFetcher(
    private val url: String,
    private val options: Options,
    private val imageLoader: ImageLoader
) : Fetcher {

    private val diskCache get() = imageLoader.diskCache
    private val cacheKey get() = options.diskCacheKey ?: url

    override suspend fun fetch(): FetchResult = withContext(Dispatchers.IO) {
        // 1. ディスクキャッシュを確認（セマフォ不要）
        diskCache?.openSnapshot(cacheKey)?.use { snapshot ->
            val bitmap = BitmapFactory.decodeFile(snapshot.data.toFile().absolutePath)
            if (bitmap != null) {
                return@withContext DrawableResult(
                    drawable = BitmapDrawable(Resources.getSystem(), bitmap),
                    isSampled = false,
                    dataSource = DataSource.DISK
                )
            }
        }

        // 2. MediaMetadataRetriever でフレームを取得（binder IPC 使用のため同時実行数を制限）
        semaphore.withPermit {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(url, emptyMap())
                val bitmap = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: error("フレーム取得失敗: $url")

                // 3. ディスクキャッシュに JPEG として保存
                diskCache?.openEditor(cacheKey)?.let { editor ->
                    try {
                        editor.data.toFile().outputStream().use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        }
                        editor.commit()
                    } catch (e: Exception) {
                        editor.abort()
                    }
                }

                DrawableResult(
                    drawable = BitmapDrawable(Resources.getSystem(), bitmap),
                    isSampled = false,
                    dataSource = DataSource.NETWORK
                )
            } finally {
                retriever.release()
            }
        }
    }

    class Factory : Fetcher.Factory<VideoThumbnailUrl> {
        override fun create(data: VideoThumbnailUrl, options: Options, imageLoader: ImageLoader): Fetcher =
            VideoThumbnailFetcher(data.url, options, imageLoader)
    }

    companion object {
        // アプリ全体で同時に実行できる MediaMetadataRetriever の数を制限
        private val semaphore = Semaphore(2)
    }
}
