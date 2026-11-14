package com.quiting.app.vpn

import android.net.VpnService
import java.lang.reflect.Proxy

/**
 * Bridge to the sing-box core (libbox).
 *
 * The core is NOT included in this project. To enable real tunneling:
 *  1. Download libbox.aar for your sing-box release (SagerNet/sing-box, Android build)
 *     and put it in app/libs/.
 *  2. Add to app/build.gradle.kts: implementation(files("libs/libbox.aar"))
 *  3. Check the class and method names below against that libbox version.
 *
 * Calls go through reflection so the app still compiles without the AAR and reports a clear error.
 * This path has not been run against a real libbox build.
 */
class LibboxCore(private val vpn: VpnService) {

    private var service: Any? = null

    fun start(configJson: String, tunFd: Int) {
        val libbox = try {
            Class.forName("io.nekohasekai.libbox.Libbox")
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException("Tunnel core is missing: put libbox.aar into app/libs")
        }
        val platformClass = Class.forName("io.nekohasekai.libbox.PlatformInterface")

        val platform = Proxy.newProxyInstance(platformClass.classLoader, arrayOf(platformClass)) { _, method, args ->
            when (method.name) {
                // The core asks for the TUN file descriptor created by VpnService.Builder.establish().
                "openTun" -> tunFd
                // Outbound sockets must be protected so they bypass the VPN itself.
                "autoDetectInterfaceControl" -> {
                    (args?.getOrNull(0) as? Int)?.let { vpn.protect(it) }
                    null
                }
                else -> defaultFor(method.returnType)
            }
        }

        val newService = libbox.getMethod("newService", String::class.java, platformClass)
        val svc = newService.invoke(null, configJson, platform)
        svc.javaClass.getMethod("start").invoke(svc)
        service = svc
    }

    fun stop() {
        val svc = service ?: return
        runCatching { svc.javaClass.getMethod("close").invoke(svc) }
        service = null
    }

    private fun defaultFor(type: Class<*>): Any? = when (type) {
        java.lang.Boolean.TYPE -> false
        java.lang.Integer.TYPE -> 0
        java.lang.Long.TYPE -> 0L
        else -> null
    }
}
