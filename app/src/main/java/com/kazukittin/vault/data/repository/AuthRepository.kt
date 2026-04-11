package com.kazukittin.vault.data.repository

import android.content.Context
import android.util.Log
import com.kazukittin.vault.data.local.VaultAuthManager
import com.kazukittin.vault.data.remote.VaultNetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val authManager: VaultAuthManager,
    private val context: Context  // ApplicationContextを受け取る
) {
    /**
     * UI（ログイン画面）からの初回ログイン
     */
    suspend fun loginFirstTime(ip: String, account: String, pass: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val api = VaultNetworkClient.createAuthApi(context, ip)
            val response = api.login(account = account, passwd = pass)
            val body = response.body()

            if (response.isSuccessful && body?.success == true && body.data != null) {
                val sid = body.data.sid
                authManager.saveCredentials(ip, account, pass)
                authManager.saveSessionId(sid)
                Log.d("AuthRepository", "ログイン成功 SID取得")
                Result.success(sid)
            } else {
                val errorCode = body?.error?.code ?: -1
                Result.failure(Exception("Synology Auth Failed. Code: $errorCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 保存済み認証情報で自動的に再ログインする（サイレント）
     */
    suspend fun reAuthenticate(): Result<String> = withContext(Dispatchers.IO) {
        val ip = authManager.getNasIp()
        val account = authManager.getAccount()
        val pass = authManager.getPassword()

        if (ip == null || account == null || pass == null) {
            return@withContext Result.failure(Exception("No saved credentials"))
        }

        Log.d("AuthRepository", "サイレント再ログイン実行中...")
        loginFirstTime(ip, account, pass)
    }

    /**
     * 起動時にセッションが有効かをNASに問い合わせ、
     * 期限切れの場合は自動で再ログインする（先手を打つ）
     */
    suspend fun validateOrRefreshSession(): Boolean = withContext(Dispatchers.IO) {
        val ip = authManager.getNasIp() ?: return@withContext false
        val sid = authManager.getSessionId() ?: run {
            Log.d("AuthRepository", "SIDなし、再認証を試みます...")
            return@withContext reAuthenticate().isSuccess
        }

        try {
            // FileStation.List でセッションが生きているかテストする
            val api = VaultNetworkClient.createPhotosApi(context, ip)
            val response = api.getFolders(sessionId = sid)
            if (response.success) {
                Log.d("AuthRepository", "セッション有効 ✅")
                true
            } else if (response.error?.code == 105 || response.error?.code == 119) {
                Log.d("AuthRepository", "セッション期限切れ(${response.error?.code})、自動再ログイン...")
                reAuthenticate().isSuccess
            } else {
                Log.w("AuthRepository", "セッション確認失敗: error=${response.error?.code}")
                false
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "セッション確認エラー（ネットワーク不通？）: ${e.message}")
            // ネットワーク不通の場合はfalseを返す（ログアウトさせない）
            false
        }
    }
}
