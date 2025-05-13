package com.cfa.immortalautomation.ui.overlay

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.*
import android.view.*
import android.widget.ImageView
import android.widget.Toast
//import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import com.cfa.immortalautomation.automation.AutomationAccessibilityService
import com.cfa.immortalautomation.data.ScriptRepository
import com.cfa.immortalautomation.model.ClickAction
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FloatingOverlayService : Service() {

    /* ── overlay / UI state ────────────────────────── */
    private lateinit var wm: WindowManager
    private lateinit var mainBtn: ImageView
    private val childBtns = mutableListOf<View>()
    private var isExpanded  = false
    private var isRecording = false

    private lateinit var overlay: View
    private val h = Handler(Looper.getMainLooper())
    private val sizePx by lazy { (48 * resources.displayMetrics.density).toInt() }
    private val OVERLAY_HIDE_MS = 80L

    /* ── lifecycle ─────────────────────────────────── */
    override fun onCreate() {
        super.onCreate()
        startForegroundNotif()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        addRecorderOverlay()
        addMainButton()
        addChildButtons()
    }

    override fun onDestroy() {
        (listOf(mainBtn, overlay) + childBtns).forEach { runCatching { wm.removeView(it) } }
        super.onDestroy()
    }

    override fun onBind(i: Intent?): IBinder? = null

    /* ── full‑screen invisible overlay ─────────────── */
    private fun addRecorderOverlay() {
        overlay = View(this).apply { visibility = View.GONE }

        val lp = baseLp().apply {
            width  = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            flags  = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            gravity = Gravity.TOP or Gravity.START
        }

        overlay.setOnTouchListener { v, e ->
            if (!isRecording) return@setOnTouchListener false

            if (e.actionMasked == MotionEvent.ACTION_DOWN) {
                recordPoint(e.rawX, e.rawY)
                v.performClick()
            }
            false
        }
        overlay.setOnClickListener { }
        wm.addView(overlay, lp)
    }

    private fun setOverlayTouchable(enabled: Boolean) {
        val lp = overlay.layoutParams as WindowManager.LayoutParams
        val touchableFlag = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        val newFlags = if (enabled) lp.flags and touchableFlag.inv()
        else lp.flags or  touchableFlag
        if (newFlags != lp.flags) {
            lp.flags = newFlags
            wm.updateViewLayout(overlay, lp)
        }
    }

    /* ── main floating bubble ──────────────────────── */
    private fun addMainButton() {
        mainBtn = ImageView(this).apply { setImageResource(android.R.drawable.presence_online) }

        val lp = baseLp().apply {
            width = sizePx; height = sizePx
            x = 0; y = 400
        }

        var sx = 0; var sy = 0; var rx = 0f; var ry = 0f; var moved = false

        mainBtn.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { sx = lp.x; sy = lp.y; rx = e.rawX; ry = e.rawY; moved = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - rx).toInt(); val dy = (e.rawY - ry).toInt()
                    if (dx*dx + dy*dy > 16) moved = true
                    lp.x = sx + dx; lp.y = sy + dy
                    wm.updateViewLayout(mainBtn, lp)
                    if (isExpanded) positionChildren(lp)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) { v.performClick(); toggleMenu(lp) }
                    true
                }
                else -> false
            }
        }
        wm.addView(mainBtn, lp)
    }

    /* ── child buttons (play/save/record) ──────────── */
    private fun addChildButtons() {
        val icons = intArrayOf(
            android.R.drawable.ic_media_play,   // ▶
            android.R.drawable.ic_menu_save,    // 💾
            android.R.drawable.ic_input_add     // +
        )

        icons.forEachIndexed { idx, res ->
            val btn = ImageView(this).apply {
                setImageResource(res)
                visibility = View.GONE
            }
            val lp  = baseLp().apply { width = sizePx; height = sizePx }

            when (idx) {
                2 -> btn.setOnClickListener { startRec(); collapse() }
                1 -> btn.setOnClickListener { saveRec();  collapse() }
                0 -> btn.setOnClickListener { playRec();  collapse() }
            }
            childBtns += btn
            wm.addView(btn, lp)
        }
    }

    /* ── recording logic ───────────────────────────── */
    private fun startRec() {
        isRecording = true
        overlay.visibility = View.VISIBLE
        setOverlayTouchable(true)          // ← capte les touchs
        toast("Enregistrement… touchez l’écran")
    }


    private fun recordPoint(x: Float, y: Float) {
        ScriptRepository.savePoint(this, ClickAction(x, y))
        flashDot(x, y)

        // Laisse passer le tap
        setOverlayTouchable(false)
        AutomationAccessibilityService.instance?.injectTap(x, y)
        h.postDelayed({ setOverlayTouchable(true) }, OVERLAY_HIDE_MS)
    }


    private fun saveRec() {
        if (!ScriptRepository.currentExists(this)) { toast("Rien à sauvegarder"); return }
        val name = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        ScriptRepository.commit(this, name)
        isRecording = false
        overlay.visibility = View.GONE
        toast("Sauvegardé sous $name")
    }

    /* ── NEW: choose & run a script ───────────────── */
    private fun playRec() {
        val scripts = ScriptRepository.all(this)
        if (scripts.isEmpty()) {
            toast("Aucun script enregistré")
            return
        }

        // Construire la liste des noms (+ option « Enregistrement en cours » si présent)
        val currentExists = ScriptRepository.currentExists(this)
        val files = if (currentExists) listOf(ScriptRepository.currentFile(this)) + scripts else scripts
        val labels = files.mapIndexed { i, f ->
            if (currentExists && i == 0) "Enregistrement en cours" else f.nameWithoutExtension
        }.toTypedArray()

        val dlg = AlertDialog.Builder(this)
            .setTitle("Choisir un script")
            .setItems(labels) { _, which ->
                AutomationAccessibilityService.instance
                    ?.playScript(files[which])
                    ?: toast("Activez le service d’accessibilité")
            }
            .setNegativeButton("Annuler", null)
            .create()

        // Autoriser l’affichage hors de l’app
        dlg.window?.setType(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE
        )
        dlg.show()
    }

    /* ── menu helpers ─────────────────────────────── */
    private fun toggleMenu(lp: WindowManager.LayoutParams) =
        if (isExpanded) collapse() else expand(lp)

    private fun expand(lp: WindowManager.LayoutParams) {
        isExpanded = true
        positionChildren(lp)
        childBtns.forEach { it.visibility = View.VISIBLE }
    }

    private fun collapse() {
        isExpanded = false
        childBtns.forEach { it.visibility = View.GONE }
    }

    private fun positionChildren(lp: WindowManager.LayoutParams) {
        childBtns.forEachIndexed { i, v ->
            val clp = v.layoutParams as WindowManager.LayoutParams
            clp.x = lp.x
            clp.y = lp.y - (i + 1) * (sizePx + 12)
            wm.updateViewLayout(v, clp)
        }
    }

    /* ── visual helpers ───────────────────────────── */
    private fun flashDot(x: Float, y: Float) {
        val d  = (12 * resources.displayMetrics.density).toInt()
        val v  = View(this).apply { setBackgroundResource(android.R.color.holo_red_light) }
        val lp = baseLp().apply {
            width = d; height = d; gravity = Gravity.TOP or Gravity.START
            this.x = (x - d / 2).toInt(); this.y = (y - d / 2).toInt()
            flags  = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        runCatching { wm.addView(v, lp) }
        h.postDelayed({ runCatching { wm.removeView(v) } }, 750)
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun baseLp() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    )

    /* ── foreground notification ──────────────────── */
    private fun startForegroundNotif() {
        if (Build.VERSION.SDK_INT < 26) return

        val channelId = "overlay"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Overlay", NotificationManager.IMPORTANCE_MIN)
            )
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Overlay actif")
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(1, notification)
        }
    }
}
