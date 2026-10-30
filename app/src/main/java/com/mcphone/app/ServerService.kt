package com.mcphone.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager

/** Foreground service agar server tetap hidup saat aplikasi ditutup / layar mati. */
class ServerService : Service() {
    private var wake: PowerManager.WakeLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { ServerManager.stop(); wake?.release(); stopForeground(true); stopSelf(); return START_NOT_STICKY }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel("srv", "Server", NotificationManager.IMPORTANCE_LOW))
        val n = (if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, "srv") else Notification.Builder(this))
            .setContentTitle("MCPhone").setContentText("Server Minecraft berjalan")
            .setSmallIcon(android.R.drawable.ic_media_play).build()
        startForeground(1, n)
        wake = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mcphone:srv").apply { acquire() }
        return START_STICKY
    }
    override fun onBind(i: Intent?): IBinder? = null

    companion object {
        fun start(c: Context) { val i = Intent(c, ServerService::class.java)
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i) else c.startService(i) }
        fun stop(c: Context) = c.startService(Intent(c, ServerService::class.java).setAction("STOP"))
    }
}
