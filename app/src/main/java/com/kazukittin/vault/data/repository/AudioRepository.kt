package com.kazukittin.vault.data.repository

import android.util.Log
import com.kazukittin.vault.data.local.VaultAuthManager
import com.kazukittin.vault.data.local.db.*
import com.kazukittin.vault.data.remote.DlSiteApi
import com.kazukittin.vault.data.remote.SynologyPhotosApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AudioRepository(
    private val authManager: VaultAuthManager,
    private val photosApi: SynologyPhotosApi,
    private val dlSiteApi: DlSiteApi,
    val audioDao: AudioDao,
    private val authRepository: com.kazukittin.vault.data.repository.AuthRepository
) {
    private suspend fun withAuthRetry(apiCall: suspend (String) -> com.kazukittin.vault.data.remote.FolderResponse): com.kazukittin.vault.data.remote.FolderResponse {
        val sid = authManager.getSessionId() ?: throw Exception("Not logged in")
        var response = apiCall(sid)
        if (!response.success && (response.error?.code == 105 || response.error?.code == 119)) {
            Log.d("AudioRepository", "Session expired (${response.error?.code}). Attempting silent re-auth...")
            val newSidResult = authRepository.reAuthenticate()
            if (newSidResult.isSuccess) {
                val newSid = newSidResult.getOrNull()!!
                response = apiCall(newSid)
            }
        }
        return response
    }

    private val rjRegex = Regex("RJ(\\d{6,8})", RegexOption.IGNORE_CASE)
    private val knownTags = listOf("ASMR", "バイノーラル", "催眠", "耳舐め", "囁き", "睡眠", "耳かき", "添い寝")

    // Works
    fun getAllWorks(): Flow<List<WorkEntity>> = audioDao.getAllWorks()
    fun getFavoriteWorks(): Flow<List<WorkEntity>> = audioDao.getFavoriteWorks()
    fun searchWorks(query: String): Flow<List<WorkEntity>> = audioDao.searchWorks(query)
    
    // Tracks
    fun getTracksForWork(workPath: String): Flow<List<TrackEntity>> = audioDao.getTracksForWork(workPath)

    // Playlists
    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = audioDao.getAllPlaylists()
    suspend fun createPlaylist(name: String) = audioDao.insertPlaylist(PlaylistEntity(id = java.util.UUID.randomUUID().toString(), name = name))
    suspend fun deletePlaylist(playlist: PlaylistEntity) = audioDao.deletePlaylist(playlist)
    fun getWorksForPlaylist(playlistId: String): Flow<List<WorkEntity>> = audioDao.getWorksForPlaylist(playlistId)
    suspend fun addWorkToPlaylist(playlistId: String, workPath: String, order: Int) = audioDao.addWorkToPlaylist(PlaylistWorkEntity(playlistId, workPath, order))
    suspend fun removeWorkFromPlaylist(playlistId: String, workPath: String) = audioDao.removeWorkFromPlaylist(playlistId, workPath)

    /**
     * 指定されたルートフォルダ内をスキャンして作品を登録する
     */
    suspend fun scanAudioRoot(rootPath: String): Int = withContext(Dispatchers.IO) {
        val response = withAuthRetry { sid -> 
            photosApi.getFolderContents(folderPath = rootPath, sessionId = sid)
        }
        
        if (!response.success || response.data?.files == null) return@withContext 0
        
        val folders = response.data.files!!.filter { it.isDir }
        var count = 0

        for (folder in folders) {
            try {
                // 1. RJコード抽出
                val rjMatch = rjRegex.find(folder.name)
                val rjCode = rjMatch?.value?.uppercase()

                var title = folder.name
                var circle = "Unknown Circle"
                var coverUrl: String? = null
                var cv = ""
                var tags = knownTags.filter { folder.name.contains(it, ignoreCase = true) }.joinToString(",")

                // 2. RJコードがあればDLsiteから取得
                if (rjCode != null) {
                    // まず、APIを使わずに直接画像URLを構成（確実なフォールバックとして）
                    val digits = rjCode.substring(2)
                    val rjNumber = digits.toLongOrNull() ?: 0L
                    val rounded = ((rjNumber + 999) / 1000) * 1000
                    val roundedStr = "RJ" + "%0${digits.length}d".format(rounded)
                    coverUrl = "https://img.dlsite.jp/modpub/images2/work/doujin/$roundedStr/${rjCode}_img_main.jpg"

                    try {
                        val infoMap = dlSiteApi.getProductInfo(rjCode)
                        val info = infoMap[rjCode]
                        if (info != null) {
                            title = info.work_name ?: title
                            circle = info.maker_name ?: circle
                            // APIから画像が取れれば上書き（解像度が高い場合があるため）
                            if (info.image_main?.url != null) {
                                coverUrl = info.image_main.url
                            }
                            cv = info.creaters?.voice_by?.joinToString(",") { it.name ?: "" } ?: ""
                            val dlsiteTags = info.genres?.mapNotNull { it.name } ?: emptyList()
                            tags = (tags.split(",") + dlsiteTags).filter { it.isNotBlank() }.distinct().joinToString(",")
                        }
                    } catch (e: Exception) {
                        Log.e("AudioRepository", "DLsite API Error for $rjCode: ${e.message}")
                    }
                }

                val work = WorkEntity(
                    folderPath = folder.path,
                    rjCode = rjCode,
                    title = title,
                    circle = circle,
                    coverUrl = coverUrl,
                    cv = cv,
                    tags = tags
                )

                audioDao.insertWork(work)

                // 3. トラックのスキャン
                scanTracks(folder.path)
                
                count++
            } catch (e: Exception) {
                Log.e("AudioRepository", "Error scanning folder ${folder.path}: ${e.message}")
            }
        }
        count
    }

    private suspend fun scanTracks(workPath: String) {
        val response = withAuthRetry { sid ->
            photosApi.getFolderContents(folderPath = workPath, sessionId = sid)
        }
        if (!response.success || response.data?.files == null) return

        val audioFiles = response.data.files!!.filter { 
            val name = it.name.lowercase()
            !it.isDir && (name.endsWith(".mp3") || name.endsWith(".flac") || name.endsWith(".wav") || name.endsWith(".m4a"))
        }

        val tracks = audioFiles.mapIndexed { index, file ->
            TrackEntity(
                path = file.path,
                workFolderPath = workPath,
                title = file.name.substringBeforeLast("."),
                order = index,
                durationMs = 0 // 後で取得
            )
        }
        audioDao.insertTracks(tracks)
    }

    fun getDownloadUrl(path: String): String? {
        val ip = authManager.getNasIp() ?: return null
        val sid = authManager.getSessionId() ?: return null
        val encodedPath = java.net.URLEncoder.encode(path, "UTF-8").replace("+", "%20")
        return "http://$ip:5000/webapi/entry.cgi?api=SYNO.FileStation.Download&version=2&method=download&path=$encodedPath&mode=download&_sid=$sid"
    }
}
