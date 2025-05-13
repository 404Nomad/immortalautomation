package com.cfa.immortalautomation.ui.overlay

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.*
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.cfa.immortalautomation.automation.AutomationAccessibilityService
import com.cfa.immortalautomation.data.ScriptRepository
import com.cfa.immortalautomation.model.ClickAction
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.hypot

class FloatingOverlayService : Service() {

    /* ─── constantes UI ─── */
    private val bubbleDp = 48
    private val dotDp    = 9
    private val slopPx   by lazy { 9 * resources.displayMetrics.density }
    private val minMs    = 120L
    private val unblockDelayMs = 180L

    /* ─── état ─── */
    private lateinit var wm: WindowManager
    private lateinit var overlay: View
    private lateinit var mainBtn: ImageView
    private val childBtns = mutableListOf<View>()
    private var isExpanded  = false
    private var isRecording = false

    private var lastTs = 0L
    private var lastX = Float.NaN
    private var lastY = Float.NaN

    private val h = Handler(Looper.getMainLooper())
    private val bubblePx by lazy { (bubbleDp * resources.displayMetrics.density).toInt() }

    /* ─── lifecycle ─── */
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

    /* ─── overlay enregistrement ─── */
    private fun addRecorderOverlay() {
        overlay = View(this).apply { visibility = View.GONE }
        val lp = baseLp().apply {
            width  = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            flags  = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        }

        overlay.setOnTouchListener { _, e ->
            if (!isRecording) {
                // on ne consomme pas quand on n'enregistre pas
                return@setOnTouchListener false
            }

            if (e.actionMasked == MotionEvent.ACTION_DOWN) {
                val now = SystemClock.uptimeMillis()
                if (now - lastTs > minMs || hypot(e.rawX - lastX, e.rawY - lastY) > slopPx) {
                    lastTs = now; lastX = e.rawX; lastY = e.rawY
                    recordPoint(e.rawX, e.rawY)

                    // 1) on bloque l'overlay
                    setOverlayTouchable(false)

                    // 2) on injecte le tap (Unit)
                    AutomationAccessibilityService.instance
                        ?.injectTap(e.rawX, e.rawY)

                    // 3) on ré-active après un délai pour laisser passer DOWN+UP
                    h.postDelayed({ setOverlayTouchable(true) }, unblockDelayMs)
                }
                // on consomme le DOWN original
                return@setOnTouchListener true
            }

            // on consomme tout le reste pendant l'enregistrement
            return@setOnTouchListener true
        }

        wm.addView(overlay, lp)
    }

    private fun setOverlayTouchable(enabled: Boolean) {
        val lp = overlay.layoutParams as WindowManager.LayoutParams
        val flag = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        val newFlags = if (enabled) lp.flags and flag.inv() else lp.flags or flag
        if (newFlags != lp.flags) {
            lp.flags = newFlags
            wm.updateViewLayout(overlay, lp)
        }
    }

    private fun recordPoint(x: Float, y: Float) {
        ScriptRepository.savePoint(this, ClickAction(x, y))
        flashDot(x, y)
    }

    /* ─── bulle flottante ─── */
    private fun addMainButton() {
        mainBtn = ImageView(this).apply { setImageResource(android.R.drawable.presence_online) }
        val lp = baseLp().apply { width = bubblePx; height = bubblePx; x = 0; y = 400 }
        var sx=0; var sy=0; var rx=0f; var ry=0f; var moved=false

        mainBtn.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    sx = lp.x; sy = lp.y; rx = e.rawX; ry = e.rawY; moved = false
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - rx).toInt(); val dy = (e.rawY - ry).toInt()
                    if (dx*dx+dy*dy > 16) moved = true
                    lp.x = sx + dx; lp.y = sy + dy
                    wm.updateViewLayout(mainBtn, lp)
                    if (isExpanded) positionChildren(lp)
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        v.performClick()
                        toggleMenu(lp)
                    }
                    return@setOnTouchListener true
                }
                else -> return@setOnTouchListener false
            }
        }

        wm.addView(mainBtn, lp)
    }

    /* ─── enfants (▶ 💾 ➕) ─── */
    private fun addChildButtons() {
        val icons = intArrayOf(
            android.R.drawable.ic_media_play,
            android.R.drawable.ic_menu_save,
            android.R.drawable.ic_input_add
        )
        icons.forEachIndexed { idx, res ->
            val btn = ImageView(this).apply { setImageResource(res); visibility = View.GONE }
            val lp  = baseLp().apply { width = bubblePx; height = bubblePx }
            when (idx) {
                2 -> btn.setOnClickListener { startRec(); collapse() }
                1 -> btn.setOnClickListener { saveRec();  collapse() }
                0 -> btn.setOnClickListener { playRec();  collapse() }
            }
            childBtns += btn
            wm.addView(btn, lp)
        }
    }

    /* ─── enregistrement ─── */
    private fun startRec() {
        lastTs = 0L; lastX = Float.NaN; lastY = Float.NaN
        isRecording = true
        overlay.visibility = View.VISIBLE
        toast("Enregistrement… touchez l’écran")
    }

    /* ─── sauvegarde ─── */
    private fun saveRec() {
        if (!ScriptRepository.currentExists(this)) {
            toast("Rien à sauvegarder")
            return
        }
        val input = EditText(this).apply { hint = "Nom du script"; setSingleLine() }
        AlertDialog.Builder(this)
            .setTitle("Sauvegarder l’enregistrement")
            .setView(input)
            .setPositiveButton("Sauvegarder") { _, _ ->
                var name = input.text.toString().trim()
                if (name.isEmpty())
                    name = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                ScriptRepository.commit(this, name)
                isRecording = false
                overlay.visibility = View.GONE
                toast("Sauvegardé sous $name")
            }
            .setNegativeButton("Annuler") { _, _ ->
                isRecording = false
                overlay.visibility = View.GONE
            }
            .create()
            .apply { window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY) }
            .show()
    }

    /* ─── lecture ─── */
    private fun playRec() {
        val scripts = ScriptRepository.all(this)
        if (scripts.isEmpty() && !ScriptRepository.currentExists(this)) {
            toast("Aucun script enregistré")
            return
        }

        val curr  = ScriptRepository.currentExists(this)
        val files = if (curr) listOf(ScriptRepository.currentFile(this)) + scripts else scripts
        val labels = files.mapIndexed { i, f ->
            if (curr && i == 0) "Enregistrement en cours" else f.nameWithoutExtension
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Choisir un script")
            .setItems(labels) { _, which ->
                AutomationAccessibilityService.instance
                    ?.playScript(files[which])
                    ?: toast("Activez le service d’accessibilité")
            }
            .setNegativeButton("Annuler", null)
            .create()
            .apply { window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY) }
            .show()
    }

    /* ─── helpers menu ─── */
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
    private fun positionChildren(lp: WindowManager.LayoutParams) =
        childBtns.forEachIndexed { i, v ->
            (v.layoutParams as WindowManager.LayoutParams).apply {
                x = lp.x; y = lp.y - (i + 1) * (bubblePx + 12)
            }.also { wm.updateViewLayout(v, it) }
        }

    /* ─── point rouge ─── */
    private fun flashDot(x: Float, y: Float) {
        val d = (dotDp * resources.displayMetrics.density).toInt()
        val v = View(this).apply { setBackgroundResource(android.R.color.holo_red_light) }
        val lp = baseLp().apply {
            width  = d; height = d; gravity = Gravity.TOP or Gravity.START
            this.x = (x - d/2).toInt()
            this.y = (y - d/2).toInt()
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        runCatching { wm.addView(v, lp) }
        h.postDelayed({ runCatching { wm.removeView(v) } }, 750)
    }

    /* ─── utils ─── */
    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    private fun baseLp() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    )

    /* ─── notification FG ─── */
    private fun startForegroundNotif() {
        if (Build.VERSION.SDK_INT < 26) return
        val id = "overlay"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(id) == null)
            nm.createNotificationChannel(
                NotificationChannel(id, "Overlay", NotificationManager.IMPORTANCE_MIN)
            )
        val n = NotificationCompat.Builder(this, id)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Overlay actif")
            .build()
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else
            startForeground(1, n)
    }
}
