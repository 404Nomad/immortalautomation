package com.cfa.immortalautomation.ui.overlay

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.cfa.immortalautomation.data.ScriptRepository
import com.cfa.immortalautomation.model.ClickAction

class FloatingOverlayService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var overlay: ImageView
    private lateinit var params: WindowManager.LayoutParams

    /* ---------- lifecycle ---------- */

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()

        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        overlay = ImageView(this).apply { setImageResource(android.R.drawable.ic_input_add) }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        overlay.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_DOWN) {
                savePoint(e.rawX, e.rawY)
                placeMarker(e.rawX, e.rawY)
                Toast.makeText(
                    this,
                    "Captured (${e.rawX.toInt()}, ${e.rawY.toInt()})",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnTouchListener true
            }
            false
        }

        wm.addView(overlay, params)
    }

    override fun onDestroy() {
        wm.removeView(overlay)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /* ---------- helpers ---------- */

    private fun savePoint(x: Float, y: Float) {
        ScriptRepository.savePoint(this, ClickAction(x, y))
    }

    /** draw a small green dot where the user tapped */
    private fun placeMarker(x: Float, y: Float) {
        val size = (16 * resources.displayMetrics.density).toInt()
        val dot  = View(this).apply { setBackgroundResource(android.R.drawable.presence_online) }

        val lp = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x  = x.toInt() - size / 2
            this.y  = y.toInt() - size / 2
        }
        wm.addView(dot, lp)
    }

    /* ---------- foreground‑service boilerplate ---------- */

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT < 34) return   // fg‑service type not required < 14
        val channelId = "overlay"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(channelId, "Overlay", NotificationManager.IMPORTANCE_MIN)
        )
        val notif = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Overlay running")
            .build()
        startForeground(
            1,
            notif,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
    }
}
