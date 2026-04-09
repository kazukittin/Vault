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
        val request = chain.request()
        val url = request.url.toString()

        // NASへのリクエスト（例: 5000番ポート）の場合のみWi-Fiチェックを行う
        if (url.contains(":5000")) {
            if (!isConnectedToWifi(context)) {
                throw IOException("Wi-Fiに接続されていません。NASにアクセスするには自宅のWi-Fiに接続してください。")
            }
        }
        
        return chain.proceed(request)
    }

    private fun isConnectedToWifi(context: Context): Boolean {
        val connectivityManager = 
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
