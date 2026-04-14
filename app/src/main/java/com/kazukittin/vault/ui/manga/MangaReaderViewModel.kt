package com.kazukittin.vault.ui.manga

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kazukittin.vault.data.local.db.MangaBookmarkDao
import com.kazukittin.vault.data.local.db.MangaBookmarkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

sealed class MangaLoadState {
    object Idle : MangaLoadState()
    data class Downloading(val progress: Float) : MangaLoadState()
    object Extracting : MangaLoadState() // 全展開中
    data class Ready(val pages: List<File>) : MangaLoadState()
    data class Error(val message: String) : MangaLoadState()
}

class MangaReaderViewModel(
    private val bookmarkDao: MangaBookmarkDao
) : ViewModel() {

    private val _state = MutableStateFlow<MangaLoadState>(MangaLoadState.Idle)
    val state: StateFlow<MangaLoadState> = _state.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    // しおりから復元したページ（バナー表示用）
    private val _restoredPage = MutableStateFlow<Int?>(null)
    val restoredPage: StateFlow<Int?> = _restoredPage.asStateFlow()

    private var zipPath: String = ""
    private var cachedZipFile: File? = null

    fun loadZip(context: Context, downloadUrl: String, zipName: String, zipPath: String) {
        if (_state.value is MangaLoadState.Ready) return
        this.zipPath = zipPath

        viewModelScope.launch {
            try {
                // しおりを読み込む
                val bookmark = withContext(Dispatchers.IO) { bookmarkDao.getBookmark(zipPath) }
                if (bookmark != null) {
                    _currentPage.value = bookmark.page
                    _restoredPage.value = bookmark.page
                }

                // 1. ZIPをキャッシュにダウンロード
                val zipFile = downloadZip(context, downloadUrl, zipName)
                cachedZipFile = zipFile

                // 2. ZIPの中身を全て展開
                _state.value = MangaLoadState.Extracting
                val pages = extractImages(context, zipFile, zipName)

                if (pages.isEmpty()) {
                    _state.value = MangaLoadState.Error("ZIPの中に画像が見つかりませんでした")
                } else {
                    _state.value = MangaLoadState.Ready(pages)
                }
            } catch (e: Exception) {
                Log.e("MangaReader", "エラー: ${e.message}", e)
                _state.value = MangaLoadState.Error("読み込みエラー: ${e.message}")
            }
        }
    }

    fun goToPage(page: Int) {
        _currentPage.value = page
        if (zipPath.isNotEmpty()) {
            val totalPages = (_state.value as? MangaLoadState.Ready)?.pages?.size ?: 0
            viewModelScope.launch(Dispatchers.IO) {
                bookmarkDao.insertBookmark(
                    MangaBookmarkEntity(zipPath = zipPath, page = page, totalPages = totalPages)
                )
            }
        }
    }

    fun clearRestoredPage() {
        _restoredPage.value = null
    }

    private suspend fun downloadZip(context: Context, downloadUrl: String, zipName: String): File =
        withContext(Dispatchers.IO) {
            val cacheFile = File(context.cacheDir, "manga_${zipName.hashCode()}.zip")
            if (cacheFile.exists() && cacheFile.length() > 0) return@withContext cacheFile

            val connection = URL(downloadUrl).openConnection() as HttpURLConnection
            connection.connect()
            val fileSize = connection.contentLengthLong
            var downloaded = 0L

            connection.inputStream.use { input ->
                FileOutputStream(cacheFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloaded += bytesRead
                        if (fileSize > 0) {
                            _state.value = MangaLoadState.Downloading(downloaded.toFloat() / fileSize)
                        }
                    }
                }
            }
            cacheFile
        }

    private suspend fun extractImages(context: Context, zipFile: File, zipName: String): List<File> =
        withContext(Dispatchers.IO) {
            val outputDir = File(context.cacheDir, "manga_pages_${zipName.hashCode()}")
            if (outputDir.exists() && (outputDir.listFiles()?.isNotEmpty() == true)) {
                return@withContext outputDir.listFiles()!!
                    .filter { it.name.lowercase().let { n -> n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp") || n.endsWith(".gif") } }
                    .sortedWith(naturalOrder())
            }
            
            outputDir.mkdirs()
            val imageFiles = mutableListOf<File>()
            val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif")

            ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    val extension = entryName.substringAfterLast('.', "").lowercase()
                    if (!entry.isDirectory && extension in imageExtensions && !entryName.startsWith("__")) {
                        val safeFileName = entryName.replace('/', '_').replace('\\', '_')
                        val outFile = File(outputDir, safeFileName)
                        FileOutputStream(outFile).use { out -> zip.copyTo(out) }
                        imageFiles.add(outFile)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            imageFiles.sortWith(naturalOrder())
            imageFiles
        }
}

private fun naturalOrder(): Comparator<File> = Comparator { a, b ->
    val partsA = a.name.split(Regex("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)"))
    val partsB = b.name.split(Regex("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)"))
    for (i in 0 until minOf(partsA.size, partsB.size)) {
        val pa = partsA[i]; val pb = partsB[i]
        val cmp = if (pa.firstOrNull()?.isDigit() == true && pb.firstOrNull()?.isDigit() == true) {
            pa.toLong().compareTo(pb.toLong())
        } else {
            pa.compareTo(pb)
        }
        if (cmp != 0) return@Comparator cmp
    }
    partsA.size.compareTo(partsB.size)
}
