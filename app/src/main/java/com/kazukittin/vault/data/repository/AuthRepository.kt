package com.kazukittin.vault.data.repository

import android.content.Context
import com.kazukittin.vault.data.local.VaultAuthManager
import com.kazukittin.vault.data.remote.SynologyAuthApi
import com.kazukittin.vault.data.remote.VaultNetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val authManager: VaultAuthManager
) {
    // 既存の再認証用メソッド（Phase 3や起動時などに使用）
    // 後ほど VaultNetworkClient からAPIを生成するように変更します。

    /**
     * UI（ログイン画面）からの初回ログイン
     */
    suspend fun loginFirstTime(context: Context, ip: String, account: String, pass: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 動的にRetrofit APIを作成
            val api = VaultNetworkClient.createAuthApi(context, ip)
            val response = api.login(account = account, passwd = pass)
            val body = response.body()

            if (response.isSuccessful && body?.success == true && body.data != null) {
                // 成功したら、全ての情報をセキュアに保存
                val sid = body.data.sid
                authManager.saveCredentials(ip, account, pass)
                authManager.saveSessionId(sid)
                Result.success(sid)
            } else {
                val errorCode = body?.error?.code ?: -1
                Result.failure(Exception("Synology Auth Failed. Code: $errorCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
