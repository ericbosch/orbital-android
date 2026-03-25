package com.orbital.app.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TailscaleDiscoveryService @Inject constructor() {

    private val TAG = "TailscaleDiscovery"
    private val ORBITAL_PORT = 3001

    // Tailscale MagicDNS suffixes to try resolving
    private val TS_SUFFIXES = listOf("", ".local", ".tail353084.ts.net")

    suspend fun discoverServers(savedHostnames: List<String> = emptyList()): List<DiscoveredServer> {
        // Collect candidate hostnames: saved servers + Tailscale peers via MagicDNS
        val candidates = resolveMagicDnsHostnames(savedHostnames)
        if (candidates.isEmpty()) {
            Log.d(TAG, "No Tailscale candidates resolved")
            return emptyList()
        }
        Log.d(TAG, "Probing ${candidates.size} Tailscale candidates")
        return coroutineScope {
            candidates.map { (hostname, ip) ->
                async {
                    if (probeOrbital(ip)) {
                        Log.d(TAG, "Found orbital at $hostname ($ip)")
                        DiscoveredServer(name = hostname, host = ip, port = ORBITAL_PORT, via = "Tailscale")
                    } else null
                }
            }.awaitAll().filterNotNull()
        }
    }

    // Resolve hostnames that are likely Tailscale peers via MagicDNS.
    // Resolves to 100.x.x.x (CGNAT range = Tailscale).
    private suspend fun resolveMagicDnsHostnames(
        extras: List<String>
    ): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        // Extract bare hostnames from saved URLs like "http://100.x.x.x:3001"
        // and try to reverse-map them; plus any saved names
        val candidates = extras.mapNotNull { url ->
            url.removePrefix("http://").substringBefore(":").takeIf { it.isNotBlank() }
        }.toMutableList()

        // Try to resolve them: if they're IPs in the Tailscale range, accept directly
        candidates.mapNotNull { host ->
            try {
                val addr = InetAddress.getByName(host)
                val ip = addr.hostAddress ?: return@mapNotNull null
                // Accept Tailscale CGNAT range 100.64.0.0/10
                if (isTailscaleIp(ip)) {
                    val name = addr.hostName.substringBefore(".").ifBlank { host }
                    Log.d(TAG, "Resolved $host -> $ip")
                    name to ip
                } else null
            } catch (e: Exception) {
                Log.d(TAG, "Could not resolve $host: ${e.message}")
                null
            }
        }
    }

    // 100.64.0.0/10: first octet 100, second octet 64–127
    private fun isTailscaleIp(ip: String): Boolean {
        val parts = ip.split(".")
        if (parts.size != 4) return false
        val a = parts[0].toIntOrNull() ?: return false
        val b = parts[1].toIntOrNull() ?: return false
        return a == 100 && b in 64..127
    }

    private suspend fun probeOrbital(ip: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL("http://$ip:$ORBITAL_PORT/api/projects").openConnection() as HttpURLConnection
            conn.connectTimeout = 2000
            conn.readTimeout    = 2000
            conn.requestMethod  = "GET"
            val code = conn.responseCode
            conn.disconnect()
            code in 200..403  // 200 ok, 401 auth required — both mean orbital is there
        } catch (e: Exception) {
            false
        }
    }
}
