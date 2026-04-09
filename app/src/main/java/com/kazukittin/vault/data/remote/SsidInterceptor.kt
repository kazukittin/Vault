package com.kazukittin.vault.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class SsidInterceptor(
    private val context: Context,
    private val targetSsid: String = "YOUR_HOME_SSID" // TODO: ここにご自宅のWi-Fi名を後で設定します
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!isConnectedToWifi(context)) {
            // Wi-Fiに繋がっていない場合は通信を行わずエラーを投げる
            throw IOException("Wi-Fiに接続されていません。自宅のWi-Fiに接続してください。")
        }

        // ※Android 10以降で正確なSSIDを取得するには位置情報権限(ACCESS_FINE_LOCATION)が必要です。
        // 将来的に厳格なSSIDチェックを行う場合は、ここにSSIDの判定処理を追加します。
        
        return chain.proceed(chain.request())
    }

    private fun isConnectedToWifi(context: Context): Boolean {
        val connectivityManager = 
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
