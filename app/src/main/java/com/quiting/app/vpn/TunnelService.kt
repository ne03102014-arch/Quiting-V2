package com.quiting.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.content.ContextCompat
import com.quiting.app.MainActivity
import com.quiting.app.data.ConfigBuilder
import com.quiting.app.data.LinkParser

/** Foreground VPN service: owns the TUN interface and the sing-box core. */
class TunnelService : VpnService() {

    private var tun: ParcelFileDescriptor? = null
    private var core: LibboxCore? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTunnel()
            return START_NOT_STICKY
        }
        val link = intent?.getStringExtra(EXTRA_LINK)
        if (link.isNullOrBlank()) {
            stopTunnel("No configuration provided")
            return START_NOT_STICKY
        }
        startInForeground(LinkParser.parseOne(link)?.name ?: "Quiting")
        startTunnel(link)
        return START_STICKY
    }

    override fun onRevoke() {
        stopTunnel("VPN was disabled by the system")
    }

    override fun onDestroy() {
        stopTunnel()
        super.onDestroy()
    }

    private fun startTunnel(link: String) {
        TunnelState.set(TunnelState.Status.CONNECTING)

        val profile = LinkParser.parseOne(link)
        if (profile == null) {
            stopTunnel("Invalid configuration")
            return
        }

        val descriptor = Builder()
            .setSession("Quiting")
            .setMtu(MTU)
            .addAddress("172.19.0.1", 30)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("1.1.1.1")
            .addDnsServer("1.0.0.1")
            .establish()
        if (descriptor == null) {
            stopTunnel("VPN permission is missing")
            return
        }
        tun = descriptor

        val c = LibboxCore(this)
        core = c
        try {
            c.start(ConfigBuilder.build(profile), descriptor.fd)
            TunnelState.set(TunnelState.Status.CONNECTED)
        } catch (e: Throwable) {
            stopTunnel(e.message ?: "The tunnel core failed to start")
        }
    }

    private fun stopTunnel(error: String? = null) {
        runCatching { core?.stop() }
        core = null
        runCatching { tun?.close() }
        tun = null
        TunnelState.set(
            if (error == null) TunnelState.Status.DISCONNECTED else TunnelState.Status.ERROR,
            error
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startInForeground(name: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "VPN status", NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Quiting VPN")
            .setContentText(name)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val ACTION_STOP = "com.quiting.app.action.STOP"
        const val EXTRA_LINK = "com.quiting.app.extra.LINK"
        private const val CHANNEL_ID = "quiting_vpn"
        private const val NOTIFICATION_ID = 1
        private const val MTU = 1500

        fun start(context: Context, link: String) {
            val intent = Intent(context, TunnelService::class.java).putExtra(EXTRA_LINK, link)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, TunnelService::class.java).setAction(ACTION_STOP))
        }
    }
}
