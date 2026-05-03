package com.kazukittin.vault.data.remote

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class NasDiscoveryManager(context: Context) {

    private val nsdManager = context.getSystemService(NsdManager::class.java)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _discoveredNases = MutableStateFlow<List<String>>(emptyList())
    val discoveredNases: StateFlow<List<String>> = _discoveredNases

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val resolving = AtomicBoolean(false)
    private val pendingServices = ConcurrentLinkedQueue<NsdServiceInfo>()

    fun startDiscovery() {
        _discoveredNases.value = emptyList()
        _isScanning.value = true
        pendingServices.clear()
        resolving.set(false)

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("NasDiscovery", "検出開始失敗: $errorCode")
                _isScanning.value = false
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w("NasDiscovery", "検出停止失敗: $errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String) {
                Log.d("NasDiscovery", "mDNS検出開始")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d("NasDiscovery", "mDNS検出停止")
                _isScanning.value = false
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d("NasDiscovery", "サービス発見: ${serviceInfo.serviceName}")
                pendingServices.add(serviceInfo)
                resolveNext()
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
        }

        nsdManager.discoverServices("_http._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener!!)
    }

    private fun resolveNext() {
        if (!resolving.compareAndSet(false, true)) return
        val service = pendingServices.poll() ?: run {
            resolving.set(false)
            return
        }

        nsdManager.resolveService(service, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.w("NasDiscovery", "解決失敗: ${serviceInfo.serviceName} ($errorCode)")
                resolving.set(false)
                resolveNext()
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                val hostAddress = serviceInfo.host?.hostAddress
                // IPv6 (コロンを含む) は除外してIPv4のみ使用
                val ip = if (hostAddress != null && !hostAddress.contains(":")) hostAddress else null
                Log.d("NasDiscovery", "解決: ${serviceInfo.serviceName} -> $ip")
                if (ip != null) verifySynologyNas(ip)
                resolving.set(false)
                resolveNext()
            }
        })
    }

    private fun verifySynologyNas(ip: String) {
        Thread {
            try {
                val request = Request.Builder()
                    .url("http://$ip:5000/webapi/query.cgi?api=SYNO.API.Info&version=1&method=query")
                    .build()
                val response = httpClient.newCall(request).execute()
                val body = response.body?.string() ?: ""
                if (response.isSuccessful && body.contains("SYNO.API.Auth")) {
                    Log.d("NasDiscovery", "Synology NAS確認: $ip")
                    val current = _discoveredNases.value
                    if (!current.contains(ip)) {
                        _discoveredNases.value = current + ip
                    }
                }
            } catch (e: Exception) {
                Log.d("NasDiscovery", "$ip はSynologyではない: ${e.message}")
            }
        }.start()
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {
                Log.w("NasDiscovery", "停止エラー: ${e.message}")
            }
            discoveryListener = null
        }
        _isScanning.value = false
    }
}
