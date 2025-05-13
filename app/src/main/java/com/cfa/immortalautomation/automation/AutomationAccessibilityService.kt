package com.cfa.immortalautomation.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

class AutomationAccessibilityService : AccessibilityService() {

    /* ── static access ─────────────────────────────── */
    companion object {
        @Volatile
        var instance: AutomationAccessibilityService? = null
            private set
    }

    /* ── coroutines ─────────────────────────────────── */
    private val serviceJob   = SupervisorJob()
    private val scope        = CoroutineScope(Dispatchers.Default + serviceJob)
    private var playJob: Job? = null

    /* ── lifecycle ─────────────────────────────────── */
    override fun onServiceConnected() { instance = this }
    override fun onInterrupt() = Unit
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onDestroy() {
        playJob?.cancel()
        serviceJob.cancel()
        instance = null
        super.onDestroy()
    }

    /* ── public API ─────────────────────────────────── */
    /** Play a script file (cancels any previous playback). */
    fun playScript(file: File) {
        if (!file.exists()) return
        playJob?.cancel()
        playJob = scope.launch {
            val actions = Json.decodeFromString<List<com.cfa.immortalautomation.model.ClickAction>>(file.readText())
            actions.forEach { action ->
                injectTap(action.x, action.y)
                delay(action.delayAfter)
            }
        }
    }

    /** Inject a single tap gesture. */
    fun injectTap(x: Float, y: Float, duration: Long = 50L) {
        val path   = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, /*startTime*/0, duration)
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }
}
