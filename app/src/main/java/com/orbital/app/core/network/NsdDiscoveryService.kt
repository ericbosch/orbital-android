package com.orbital.app.core.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

data class DiscoveredServer(val name: String, val host: String, val port: Int, val via: String = "LAN")

@Singleton
class NsdDiscoveryService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val TAG = "NsdDiscovery"

    fun discoverServers(): Flow<List<DiscoveredServer>> = callbackFlow {
        val discovered = mutableMapOf<String, DiscoveredServer>()

        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery start failed: $errorCode")
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "Discovery started: $serviceType")
            }
            override fun onDiscoveryStopped(serviceType: String) {}

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                discovered.remove(serviceInfo.serviceName)
                trySend(discovered.values.toList())
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(si: NsdServiceInfo, errorCode: Int) {
                        Log.e(TAG, "Resolve failed: $errorCode")
                    }
                    override fun onServiceResolved(si: NsdServiceInfo) {
                        val server = DiscoveredServer(
                            name = si.serviceName,
                            host = si.host?.hostAddress ?: return,
                            port = si.port
                        )
                        discovered[server.name] = server
                        trySend(discovered.values.toList())
                    }
                })
            }
        }

        nsdManager.discoverServices("_orbital._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)

        awaitClose {
            try { nsdManager.stopServiceDiscovery(discoveryListener) } catch (_: Exception) {}
        }
    }
}
