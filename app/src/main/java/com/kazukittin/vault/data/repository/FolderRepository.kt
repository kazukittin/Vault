package com.kazukittin.vault.data.repository

import android.util.Log
import com.kazukittin.vault.data.local.VaultAuthManager
import com.kazukittin.vault.data.local.db.DlSiteCacheDao
import com.kazukittin.vault.data.local.db.DlSiteCacheEntity
import com.kazukittin.vault.data.local.db.FolderDao
import com.kazukittin.vault.data.local.db.FolderEntity
import com.kazukittin.vault.data.remote.DlSiteProductInfo
import com.kazukittin.vault.data.remote.SynologyPhotosApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class FolderRepository(
    private val authManager: VaultAuthManager,
    private val photosApi: SynologyPhotosApi,
    private val dlSiteApi: com.kazukittin.vault.data.remote.DlSiteApi,
    private val folderDao: FolderDao,
    private val dlSiteCacheDao: DlSiteCacheDao,
    private val authRepository: com.kazukittin.vault.data.repository.AuthRepository
) {
    private val dlSiteHttpClient = OkHttpClient()

    /** DLsiteのメタデータを取得する（DBキャッシュ優先） */
    suspend fun getDlSiteMetadata(rjCode: String): DlSiteProductInfo? = withContext(Dispatchers.IO) {
        // 1. ローカルDBキャッシュを確認
        // workName が null → 前回の取得失敗とみなして再取得
        // genres が null → ジャンル未取得（古いキャッシュ）とみなして再取得
        val cached = dlSiteCacheDao.getCache(rjCode)  // suspend なし・IO スレッドで呼ぶ
        if (cached != null && cached.workName != null && cached.genres != null) {
            Log.d("DLsite", "$rjCode → キャッシュヒット: ${cached.makerName}")
            return@withContext cached.toProductInfo()
        }

        // 2. DLSiteから取得（DNS エラー等の一時的な障害に備えてリトライ）
        val maxRetries = 3
        for (attempt in 1..maxRetries) {
            try {
                val response = dlSiteApi.getProductInfo(rjCode)
                var info = response[rjCode]

                // maker_name が null の場合はプロダクトページ HTML からサークル名を抽出
                if (info != null && info.maker_name == null) {
                    val makerName = fetchMakerNameFromPage(rjCode)
                    if (makerName != null) {
                        info = info.copy(maker_name = makerName)
                    }
                }

                // 3. DBに保存（suspend なし・IO スレッドで呼ぶ）
                if (info != null) {
                    dlSiteCacheDao.insertCache(info.toCacheEntity(rjCode))
                    Log.d("DLsite", "$rjCode → 取得・保存: ${info.maker_name}")
                }
                return@withContext info
            } catch (e: java.net.UnknownHostException) {
                Log.w("DLsite", "$rjCode DNS解決失敗 (${attempt}/${maxRetries}): ${e.message}")
                if (attempt < maxRetries) kotlinx.coroutines.delay(2000L * attempt)
            } catch (e: java.io.IOException) {
                Log.w("DLsite", "$rjCode ネットワークエラー (${attempt}/${maxRetries}): ${e.message}")
                if (attempt < maxRetries) kotlinx.coroutines.delay(2000L * attempt)
            } catch (e: Exception) {
                Log.e("DLsite", "$rjCode fetch error: ${e.message}")
                return@withContext null  // リトライ不要なエラー（パースエラー等）は即終了
            }
        }
        Log.e("DLsite", "$rjCode → ${maxRetries}回リトライ後も失敗")
        null
    }

    private fun DlSiteCacheEntity.toProductInfo() = DlSiteProductInfo(
        work_name = workName,
        maker_id = makerId,
        maker_name = makerName,
        image_main = imageUrl?.let { com.kazukittin.vault.data.remote.DlSiteImage(it) },
        genres = genres?.split(",")?.filter { it.isNotBlank() }
            ?.map { com.kazukittin.vault.data.remote.DlSiteGenre(it) },
        creaters = null,
        work_type = workType
    )

    private fun DlSiteProductInfo.toCacheEntity(rjCode: String) = DlSiteCacheEntity(
        rjCode = rjCode,
        workName = work_name,
        makerId = maker_id,
        makerName = maker_name,
        imageUrl = image_main?.url,
        // 空リストでも "" で保存して "null = 未取得" と区別する
        genres = genres?.mapNotNull { it.name }?.joinToString(",") ?: "",
        workType = work_type
    )

    /** maker_id を使ってサークルページの <title> からサークル名を抽出する */
    private fun fetchMakerNameFromPage(rjCode: String): String? {
        // まず product page から maker_id を含む URL を探す（リダイレクト込み）
        val productUrl = "https://www.dlsite.com/home/work/=/product_id/$rjCode.html"
        return try {
            val productHtml = dlSiteHttpClient.newCall(Request.Builder().url(productUrl).build())
                .execute().use { it.body?.string() } ?: return null

            // maker_id を抽出 (例: /maker_id/RG15758.html)
            val makerIdMatch = Regex("""maker_id/(RG\d+)\.html""").find(productHtml)
            val makerId = makerIdMatch?.groupValues?.get(1) ?: return null

            // サークルページを取得して <title> からサークル名を抽出
            // DLSite のタイトル形式: "サークル名 | DLsite" or "「サークル名」のサークルページ..."
            val circleUrl = "https://www.dlsite.com/home/circle/profile/=/maker_id/$makerId.html"
            val circleHtml = dlSiteHttpClient.newCall(Request.Builder().url(circleUrl).build())
                .execute().use { it.body?.string() } ?: return null

            // <title>サークル名 | DLsite...</title>
            val titleMatch = Regex("""<title>\s*([^|｜\r\n<]+?)\s*[|｜]""").find(circleHtml)
            titleMatch?.groupValues?.get(1)
                ?.replace(Regex("""（[^）]*）"""), "")  // フリガナ（全角カッコ）を除去
                ?.replace("サークルプロフィール", "")
                ?.trim()
        } catch (e: Exception) {
            Log.e("DLsite", "$rjCode HTML fetch error: ${e.message}")
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

                    // 既存のカテゴリ・ピン設定を保持する
                    val existing = folderDao.getAllFoldersSnapshot().associateBy { it.id }

                    // サーバーから来たデータをRoom用（FolderEntity）に変換
                    val entities = networkFolders.map { syno ->
                        val prev = existing[syno.path]
                        FolderEntity(
                            id = syno.path,
                            name = syno.name,
                            parentId = "/", // Route node
                            isPinned = prev?.isPinned ?: false,
                            category = prev?.category ?: autoDetectCategory(syno.name)
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
     * フォルダ名からカテゴリを自動判定する。
     * 新規フォルダ（既存カテゴリなし）にのみ適用される。
     */
    private fun autoDetectCategory(folderName: String): String? {
        val name = folderName.lowercase()
        return when {
            name.contains("manga") -> "マンガ"
            name.contains("picture") -> "画像"
            name.contains("video") -> "ビデオ"
            name.contains("voice") -> "ボイス"
            else -> null
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
        return "http://$ip:5000/webapi/entry.cgi?api=SYNO.FileStation.Thumb&version=3&method=get&path=$encodedPath&size=medium&_sid=$sid"
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
