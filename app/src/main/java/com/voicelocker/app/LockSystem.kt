package com.voicelocker.app

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** Full-screen lock shown over the lock screen / other apps. Back is disabled. */
class LockActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Store.init(this)
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        else @Suppress("DEPRECATION") window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) { override fun handleOnBackPressed() {} })
        setContent {
            VoiceLockTheme {
                LockScreen(Store.themeIndex, onUnlock = { LockService.lastUnlock = System.currentTimeMillis(); finishAndRemoveTask() })
            }
        }
    }
}

/** Foreground service that shows [LockActivity] whenever the screen turns on / the device is unlocked. */
class LockService : Service() {
    private val rx = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (!Store.lockEnabled) return
            if (i.action == Intent.ACTION_USER_PRESENT && System.currentTimeMillis() - lastUnlock < 30_000) return
            c.startActivity(Intent(c, LockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }
    }

    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        ContextCompat.registerReceiver(this, rx, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel("lock", "Screen lock", NotificationManager.IMPORTANCE_LOW))
        val n = NotificationCompat.Builder(this, "lock").setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("Voice lock is active").setContentText("Your screen is protected").setOngoing(true).build()
        ServiceCompat.startForeground(this, 1, n, if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
        return START_STICKY
    }

    override fun onDestroy() { runCatching { unregisterReceiver(rx) }; super.onDestroy() }
    override fun onBind(i: Intent?): IBinder? = null

    companion object {
        @Volatile var lastUnlock = 0L
        fun sync(c: Context) {
            Store.init(c)
            val i = Intent(c, LockService::class.java)
            if (Store.lockEnabled) runCatching { ContextCompat.startForegroundService(c, i) } else c.stopService(i)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) { if (i.action == Intent.ACTION_BOOT_COMPLETED) LockService.sync(c) }
}
