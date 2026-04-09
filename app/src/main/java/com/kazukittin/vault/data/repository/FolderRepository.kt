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
    private val folderDao: FolderDao
) {
    // UI用のFlow（ローカルDBの監視）
    fun getAllFolders(): Flow<List<FolderEntity>> = folderDao.getAllFolders()
    fun getPinnedFolders(): Flow<List<FolderEntity>> = folderDao.getPinnedFolders()

    // NASと通信してRoomを更新する処理
    suspend fun syncFolders() {
        withContext(Dispatchers.IO) {
            try {
                val sid = authManager.getSessionId() ?: throw Exception("Not logged in")
                Log.d("FolderRepository", "フォルダ取得を開始します SID: \${sid.take(5)}...")
                
                // Synology Photos WebAPIを叩く
                val response = photosApi.getFolders(sessionId = sid)
                
                if (response.success && response.data != null) {
                    val networkFolders = response.data.shares ?: emptyList()
                    Log.d("FolderRepository", "取得成功! 件数: \${networkFolders.size}")
                    
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
                    Log.e("FolderRepository", "APIリクエスト失敗: success = false, error code = \${response.error?.code}")
                }
            } catch (e: Exception) {
                Log.e("FolderRepository", "フォルダ同期エラー: \${e.message}", e)
            }
        }
    }

    // フォルダ内のアイテムを取得する（Room保存なし、直接UIへ）
    suspend fun getFolderContents(folderPath: String): List<com.kazukittin.vault.data.remote.SynoFolder> {
        return withContext(Dispatchers.IO) {
            try {
                val sid = authManager.getSessionId() ?: throw Exception("Not logged in")
                val response = photosApi.getFolderContents(
                    folderPath = folderPath,
                    sessionId = sid
                )
                if (response.success && response.data != null) {
                    response.data.files ?: emptyList()
                } else {
                    Log.e("FolderRepository", "サブフォルダ取得失敗: error = \${response.error?.code}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("FolderRepository", "サブフォルダ取得エラー: \${e.message}")
                emptyList()
            }
        }
    }

    // サムネイル画像のURLを構築する
    fun getThumbnailUrl(path: String): String? {
        val ip = authManager.getNasIp() ?: return null
        val sid = authManager.getSessionId() ?: return null
        val encodedPath = java.net.URLEncoder.encode(path, "UTF-8")
        return "http://$ip:5000/webapi/entry.cgi?api=SYNO.FileStation.Thumb&version=2&method=get&path=$encodedPath&size=small&_sid=$sid"
    }
}
