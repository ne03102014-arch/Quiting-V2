package com.quiting.app.data

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder

/** Builds a sing-box configuration (JSON) from a profile link. */
object ConfigBuilder {

    fun build(profile: VpnProfile): String {
        val proxy = outbound(profile).put("tag", "proxy")
        val root = JSONObject()
            .put("log", JSONObject().put("level", "warn"))
            .put(
                "dns",
                JSONObject()
                    .put(
                        "servers",
                        JSONArray()
                            .put(JSONObject().put("tag", "remote").put("address", "https://1.1.1.1/dns-query").put("detour", "proxy"))
                            .put(JSONObject().put("tag", "local").put("address", "local").put("detour", "direct"))
                    )
                    .put("final", "remote")
            )
            .put(
                "inbounds",
                JSONArray().put(
                    JSONObject()
                        .put("type", "tun")
                        .put("tag", "tun-in")
                        .put("interface_name", "tun0")
                        .put("address", JSONArray().put("172.19.0.1/30"))
                        .put("mtu", 1500)
                        .put("auto_route", false)
                        .put("stack", "system")
                )
            )
            .put(
                "outbounds",
                JSONArray()
                    .put(proxy)
                    .put(JSONObject().put("type", "direct").put("tag", "direct"))
            )
            .put(
                "route",
                JSONObject()
                    .put("auto_detect_interface", true)
                    .put("final", "proxy")
                    .put("rules", JSONArray().put(JSONObject().put("protocol", "dns").put("action", "hijack-dns")))
            )
        return root.toString()
    }

    private fun outbound(p: VpnProfile): JSONObject = when (p.protocol) {
        Protocol.VLESS -> vless(p)
        Protocol.VMESS -> vmess(p)
        Protocol.TROJAN -> trojan(p)
        Protocol.SHADOWSOCKS -> shadowsocks(p)
    }

    private fun vless(p: VpnProfile): JSONObject {
        val uri = URI(p.raw)
        val q = query(uri)
        val o = JSONObject()
            .put("type", "vless")
            .put("server", p.host)
            .put("server_port", p.port)
            .put("uuid", uri.userInfo.orEmpty())
        q["flow"]?.takeIf { it.isNotBlank() }?.let { o.put("flow", it) }
        tls(q["security"], q, p.host)?.let { o.put("tls", it) }
        transport(q["type"], q["path"], q["host"], q["serviceName"], p.host)?.let { o.put("transport", it) }
        return o
    }

    private fun vmess(p: VpnProfile): JSONObject {
        val j = JSONObject(decodeBase64(p.raw.removePrefix("vmess://")))
        val path = j.optString("path")
        val host = j.optString("host")
        val o = JSONObject()
            .put("type", "vmess")
            .put("server", p.host)
            .put("server_port", p.port)
            .put("uuid", j.optString("id"))
            .put("security", j.optString("scy").ifBlank { "auto" })
            .put("alter_id", j.optString("aid", "0").toIntOrNull() ?: 0)
        if (j.optString("tls") == "tls") {
            tls("tls", mapOf("sni" to j.optString("sni"), "alpn" to j.optString("alpn")), p.host)?.let { o.put("tls", it) }
        }
        transport(j.optString("net"), path, host, path, p.host)?.let { o.put("transport", it) }
        return o
    }

    private fun trojan(p: VpnProfile): JSONObject {
        val uri = URI(p.raw)
        val q = query(uri)
        val o = JSONObject()
            .put("type", "trojan")
            .put("server", p.host)
            .put("server_port", p.port)
            .put("password", URLDecoder.decode(uri.userInfo.orEmpty(), "UTF-8"))
        tls(q["security"] ?: "tls", q, p.host)?.let { o.put("tls", it) }
        transport(q["type"], q["path"], q["host"], q["serviceName"], p.host)?.let { o.put("transport", it) }
        return o
    }

    private fun shadowsocks(p: VpnProfile): JSONObject {
        val body = p.raw.removePrefix("ss://").substringBefore("#")
        val hasAt = body.contains("@")
        // SIP002 links keep base64("method:password") before "@"; legacy links base64 the whole part.
        val creds = if (hasAt) body.substringBeforeLast("@") else decodeBase64(body).substringBeforeLast("@")
        val plain = if (creds.contains(":")) creds else decodeBase64(creds)
        return JSONObject()
            .put("type", "shadowsocks")
            .put("server", p.host)
            .put("server_port", p.port)
            .put("method", plain.substringBefore(":"))
            .put("password", plain.substringAfter(":"))
    }

    private fun tls(security: String?, q: Map<String, String>, host: String): JSONObject? {
        if (security.isNullOrBlank() || security == "none") return null
        val sni = q["sni"]?.takeIf { it.isNotBlank() } ?: host
        val o = JSONObject().put("enabled", true).put("server_name", sni)
        q["alpn"]?.takeIf { it.isNotBlank() }?.let { o.put("alpn", JSONArray(it.split(","))) }
        q["fp"]?.takeIf { it.isNotBlank() }?.let {
            o.put("utls", JSONObject().put("enabled", true).put("fingerprint", it))
        }
        if (security == "reality") {
            o.put(
                "reality",
                JSONObject()
                    .put("enabled", true)
                    .put("public_key", q["pbk"].orEmpty())
                    .put("short_id", q["sid"].orEmpty())
            )
        }
        if (q["allowInsecure"] == "1" || q["insecure"] == "1") o.put("insecure", true)
        return o
    }

    private fun transport(type: String?, path: String?, host: String?, service: String?, fallbackHost: String): JSONObject? =
        when (type) {
            "ws" -> JSONObject()
                .put("type", "ws")
                .put("path", path?.takeIf { it.isNotBlank() } ?: "/")
                .put("headers", JSONObject().put("Host", host?.takeIf { it.isNotBlank() } ?: fallbackHost))
            "grpc" -> JSONObject()
                .put("type", "grpc")
                .put("service_name", service.orEmpty())
            "http", "h2" -> JSONObject()
                .put("type", "http")
                .put("host", JSONArray().put(host?.takeIf { it.isNotBlank() } ?: fallbackHost))
                .put("path", path?.takeIf { it.isNotBlank() } ?: "/")
            else -> null
        }

    private fun query(uri: URI): Map<String, String> =
        (uri.rawQuery ?: "")
            .split("&")
            .filter { it.contains("=") }
            .associate {
                URLDecoder.decode(it.substringBefore("="), "UTF-8") to
                    URLDecoder.decode(it.substringAfter("="), "UTF-8")
            }

    private fun decodeBase64(s: String): String {
        val clean = s.trim().replace('-', '+').replace('_', '/')
        return String(Base64.decode(clean, Base64.DEFAULT), Charsets.UTF_8)
    }
}
