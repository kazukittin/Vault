package com.kazukittin.vault.data.remote

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object VaultNetworkClient {

    private var cachedOkHttpClient: OkHttpClient? = null

    // IPアドレスを受け取って動的にRetrofitを作成する
    fun getOkHttpClient(context: Context): OkHttpClient {
        if (cachedOkHttpClient == null) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY // 開発用: 通信ログを出力
            }

            cachedOkHttpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(SsidInterceptor(context))
                // タイムアウト設定 (HDDスピンアップ考慮)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }
        return cachedOkHttpClient!!
    }

    // IPアドレスを受け取って動的にRetrofitを作成する
    fun createAuthApi(context: Context, nasIp: String): SynologyAuthApi {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://$nasIp:5000/")
            .client(getOkHttpClient(context))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(SynologyAuthApi::class.java)
    }

    fun createPhotosApi(context: Context, nasIp: String): SynologyPhotosApi {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://$nasIp:5000/")
            .client(getOkHttpClient(context))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(SynologyPhotosApi::class.java)
    }
}
