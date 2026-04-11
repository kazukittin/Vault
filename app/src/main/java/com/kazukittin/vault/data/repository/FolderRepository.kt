package com.kazukittin.vault.data.repository

import android.util.Log
import com.kazukittin.vault.data.local.VaultAuthManager
import com.kazukittin.vault.data.local.db.FolderDao
import com.kazukittin.vault.data.local.db.FolderEntity
import com.kazukittin.vault.data.remote.SynologyPhotosApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FolderRepository(
    private val authManager: VaultAuthManager,
    private val photosApi: SynologyPhotosApi,
    private val dlSiteApi: com.kazukittin.vault.data.remote.DlSiteApi,
    private val folderDao: FolderDao,
    private val authRepository: com.kazukittin.vault.data.repository.AuthRepository
) {
    /** DLsiteのメタデータを取得する */
    suspend fun getDlSiteMetadata(rjCode: String) = withContext(Dispatchers.IO) {
        try {
            val response = dlSiteApi.getProductInfo(rjCode)
            response[rjCode]
        } catch (e: Exception) {
            null
        }
    }
    private suspend fun withAuthRetry(apiCall: suspend (String) -> com.kazukittin.vault.data.remote.FolderResponse): com.kazukittin.vault.data.remote.FolderResponse {
        val sid = authManager.getSessionId() ?: throw Exception("Not logged in")
        var response = apiCall(sid)
        // 105 or 119 is session expired/invalid. Let's retry auth.
        if (!response.success && (response.error?.code == 105 || response.error?.code == 119)) {
            Log.d("FolderRepository", "Session expired (${response.error?.code}). Attempting silent re-auth...")
            val newSidResult = authRepository.reAuthenticate()
            if (newSidResult.isSuccess) {
                val newSid = newSidResult.getOrNull()!!
                response = apiCall(newSid)
            } else {
                Log.e("FolderRepository", "Silent re-auth failed.")
            }
        }
        return response
    }

    // UI用のFlow（ローカルDBの監視）
    fun getAllFolders(): Flow<List<FolderEntity>> = folderDao.getAllFolders()
    fun getPinnedFolders(): Flow<List<FolderEntity>> = folderDao.getPinnedFolders()

    /**
     * フォルダ内のアイテムを1ページ分取得する（ページネーション対応）
     * @param offset 開始位置
     * @param limit  1回で取得する件数
     * @return ページのアイテムリストと全件数のPair
     */
    suspend fun getFolderContents(
        folderPath: String,
        offset: Int = 0,
        limit: Int = 200
    ): Pair<List<com.kazukittin.vault.data.remote.SynoFolder>, Int> {
        return withContext(Dispatchers.IO) {
            try {
                val response = withAuthRetry { sid ->
                    photosApi.getFolderContents(
                        folderPath = folderPath,
                        offset = offset,
                        limit = limit,
                        sessionId = sid
                    )
                }
                if (response.success && response.data != null) {
                    val items = response.data.files ?: emptyList()
                    val total = response.data.total
                    Log.d("FolderRepository", "取得: offset=$offset, ${items.size}件, 全${total}件")
                    Pair(items, total)
                } else {
                    Log.e("FolderRepository", "取得失敗: error=${response.error?.code}")
                    Pair(emptyList(), 0)
                }
            } catch (e: Exception) {
                Log.e("FolderRepository", "取得エラー: ${e.message}")
                Pair(emptyList(), 0)
            }
        }
    }

    // NASと通信してRoomを更新する処理
    suspend fun syncFolders() {
        withContext(Dispatchers.IO) {
            try {
                Log.d("FolderRepository", "フォルダ取得を開始します...")
                
                // Synology Photos WebAPIを叩く (自動再認証付き)
                val response = withAuthRetry { sid -> 
                    photosApi.getFolders(sessionId = sid) 
                }
                
                if (response.success && response.data != null) {
                    val networkFolders = response.data.shares ?: emptyList()
                    Log.d("FolderRepository", "取得成功! 件数: ${networkFolders.size}")
                    
                    // サーバーから来たデータをRoom用（FolderEntity）に変換
                    val entities = networkFolders.map { syno ->
                        FolderEntity(
                            id = syno.path,
                            name = syno.name,
                            parentId = "/", // Route node
                            isPinned = false
                        )
                    }
                    
                    // Roomに保存 (suspendを外したのでそのまま呼べる)
                    folderDao.insertFolders(entities)
                    Log.d("FolderRepository", "DBへの保存が完了しました")
                } else {
                    Log.e("FolderRepository", "APIリクエスト失敗: success = false, error code = ${response.error?.code}")
                }
            } catch (e: Exception) {
                Log.e("FolderRepository", "フォルダ同期エラー: ${e.message}", e)
            }
        }
    }


    /**
     * タイトルからRJコード（RJ123456など）を探し、DLsiteの公式サムネイルURLを返す
     */
    fun getDlSiteThumbnailUrl(name: String): String? {
        val rjMatch = Regex("RJ(\\d+)").find(name) ?: return null
        val rjCode = rjMatch.value
        val digits = rjMatch.groupValues[1]
        val rjNumber = digits.toLongOrNull() ?: 0L 

        val rounded = ((rjNumber + 999) / 1000) * 1000
        // 元の桁数（RJ01123456なら8桁）に合わせて0埋めする
        val roundedStr = "RJ" + "%0${digits.length}d".format(rounded)
        
        return "https://img.dlsite.jp/modpub/images2/work/doujin/$roundedStr/${rjCode}_img_main.jpg"
    }

    // サムネイル画像のURLを構築する
    fun getThumbnailUrl(path: String): String? {
        val ip = authManager.getNasIp() ?: return null
        val sid = authManager.getSessionId() ?: return null
        // パスを正しくエンコード（スラッシュはそのまま、スペースは%20）
        val encodedPath = path.split("/").joinToString("/") { 
            java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") 
        }
        return "http://$ip:5000/webapi/entry.cgi?api=SYNO.FileStation.Thumb&version=2&method=get&path=$encodedPath&size=small&_sid=$sid"
    }

    // フルスクリーン用の高画質画像のURLを構築する
    fun getOriginalImageUrl(path: String): String? {
        val ip = authManager.getNasIp() ?: return null
        val sid = authManager.getSessionId() ?: return null
        // パスを正しくエンコード（スラッシュはそのまま）
        val encodedPath = path.split("/").joinToString("/") { 
            java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") 
        }
        return "http://$ip:5000/webapi/entry.cgi?api=SYNO.FileStation.Download&version=2&method=download&path=$encodedPath&mode=download&_sid=$sid"
    }

    suspend fun updateFolderCategory(folderId: String, category: String?) {
        withContext(Dispatchers.IO) {
            folderDao.updateCategory(folderId, category)
        }
    }
}
