package com.kazukittin.vault.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class VaultAuthManager(context: Context) {

    // Create a master key using AES256_GCM
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // Initialize EncryptedSharedPreferences
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "vault_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveCredentials(nasIp: String, account: String, password: String) {
        prefs.edit()
            .putString("KEY_NAS_IP", nasIp)
            .putString("KEY_ACCOUNT", account)
            .putString("KEY_PASSWORD", password)
            .apply()
    }

    fun saveSessionId(sid: String) {
        prefs.edit()
            .putString("KEY_SID", sid)
            .apply()
    }

    fun getSessionId(): String? = prefs.getString("KEY_SID", null)
    fun getNasIp(): String? = prefs.getString("KEY_NAS_IP", null)
    fun getAccount(): String? = prefs.getString("KEY_ACCOUNT", null)
    fun getPassword(): String? = prefs.getString("KEY_PASSWORD", null)

    fun clearSession() {
        prefs.edit().remove("KEY_SID").apply()
    }
}
