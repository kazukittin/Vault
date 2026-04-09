package com.kazukittin.vault.data.repository

import com.kazukittin.vault.data.local.VaultAuthManager
import com.kazukittin.vault.data.remote.SynologyAuthApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val authApi: SynologyAuthApi,
    private val authManager: VaultAuthManager
) {
    suspend fun authenticate(): Result<String> = withContext(Dispatchers.IO) {
        val account = authManager.getAccount()
        val password = authManager.getPassword()
        
        if (account.isNullOrBlank() || password.isNullOrBlank()) {
            return@withContext Result.failure(Exception("No credentials found"))
        }

        try {
            val response = authApi.login(account = account, passwd = password)
            val body = response.body()

            if (response.isSuccessful && body?.success == true && body.data != null) {
                val sid = body.data.sid
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
