package com.quiting.app.data

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.util.UUID

enum class Protocol(val label: String) {
    VLESS("VLESS"),
    VMESS("VMess"),
    TROJAN("Trojan"),
    SHADOWSOCKS("Shadowsocks")
}

data class VpnProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val protocol: Protocol,
    val host: String,
    val port: Int,
    val raw: String,
    val favorite: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("protocol", protocol.name)
        .put("host", host)
        .put("port", port)
        .put("raw", raw)
        .put("favorite", favorite)

    companion object {
        fun fromJson(o: JSONObject) = VpnProfile(
            id = o.getString("id"),
            name = o.getString("name"),
            protocol = Protocol.valueOf(o.getString("protocol")),
            host = o.getString("host"),
            port = o.getInt("port"),
            raw = o.getString("raw"),
            favorite = o.optBoolean("favorite")
        )
    }
}

/** Parses share links (vless://, vmess://, trojan://, ss://) and subscription URLs. */
object LinkParser {
    private val schemes = listOf("vless://", "vmess://", "trojan://", "ss://")

    fun parseMany(text: String): List<VpnProfile> =
        text.lines()
            .map { it.trim() }
            .filter { line -> schemes.any { line.startsWith(it) } }
            .mapNotNull { parseOne(it) }

    fun parseOne(link: String): VpnProfile? = runCatching {
        when {
            link.startsWith("vmess://") -> parseVmess(link)
            link.startsWith("ss://") -> parseShadowsocks(link)
            link.startsWith("vless://") -> parseStandard(link, Protocol.VLESS)
            link.startsWith("trojan://") -> parseStandard(link, Protocol.TROJAN)
            else -> null
        }
    }.getOrNull()

    fun fetchSubscription(url: String): List<VpnProfile> {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("User-Agent", "Quiting/1.0")
            val body = conn.inputStream.bufferedReader().use { it.readText() }.trim()
            val text = if (body.contains("://")) body
            else runCatching { decodeBase64(body) }.getOrDefault(body)
            return parseMany(text)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseStandard(link: String, protocol: Protocol): VpnProfile {
        val uri = URI(link)
        val name = URLDecoder.decode(uri.rawFragment ?: "", "UTF-8").ifBlank { uri.host }
        val port = if (uri.port > 0) uri.port else 443
        return VpnProfile(name = name, protocol = protocol, host = uri.host, port = port, raw = link)
    }

    private fun parseVmess(link: String): VpnProfile {
        val json = JSONObject(decodeBase64(link.removePrefix("vmess://")))
        val host = json.getString("add")
        val name = json.optString("ps").ifBlank { host }
        val port = json.optString("port", "443").toInt()
        return VpnProfile(name = name, protocol = Protocol.VMESS, host = host, port = port, raw = link)
    }

    private fun parseShadowsocks(link: String): VpnProfile {
        val body = link.removePrefix("ss://")
        val main = body.substringBefore("#")
        val label = body.substringAfter("#", "")
        val hostPart = if (main.contains("@")) main.substringAfterLast("@")
        else decodeBase64(main).substringAfterLast("@")
        val host = hostPart.substringBeforeLast(":")
        val port = hostPart.substringAfterLast(":").substringBefore("?").toInt()
        val name = URLDecoder.decode(label, "UTF-8").ifBlank { host }
        return VpnProfile(name = name, protocol = Protocol.SHADOWSOCKS, host = host, port = port, raw = link)
    }

    private fun decodeBase64(s: String): String {
        val clean = s.trim().replace('-', '+').replace('_', '/')
        return String(Base64.decode(clean, Base64.DEFAULT), Charsets.UTF_8)
    }
}

/** Persists profiles and preferences in SharedPreferences. */
class ProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("quiting", Context.MODE_PRIVATE)

    fun loadProfiles(): MutableList<VpnProfile> {
        val arr = runCatching { JSONArray(prefs.getString("profiles", "[]")) }.getOrDefault(JSONArray())
        return MutableList(arr.length()) { VpnProfile.fromJson(arr.getJSONObject(it)) }
    }

    fun saveProfiles(list: List<VpnProfile>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("profiles", arr.toString()).apply()
    }

    fun getString(key: String, default: String): String = prefs.getString(key, default) ?: default

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
}
