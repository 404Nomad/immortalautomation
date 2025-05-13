package com.cfa.immortalautomation.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import com.cfa.immortalautomation.model.ClickAction
import kotlinx.coroutines.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

class AutomationAccessibilityService : AccessibilityService() {

    /* -------- static access to the running instance -------- */
    companion object {
        @Volatile
        var instance: AutomationAccessibilityService? = null
            private set
    }

    private val job   = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    /** Play a script file */
    fun playScript(file: File) = scope.launch {
        if (!file.exists()) return@launch
        val actions: List<ClickAction> = Json.decodeFromString(file.readText())
        for (a in actions) {
            gestureTap(a.x, a.y)
            delay(a.delayAfter)
        }
    }

    private fun gestureTap(x: Float, y: Float) {
        val path  = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        dispatchGesture(
            GestureDescription.Builder().addStroke(stroke).build(),
            null,
            null
        )
    }

    override fun onDestroy() {
        instance = null
        job.cancel()
        super.onDestroy()
    }
}
