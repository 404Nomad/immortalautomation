package com.cfa.immortalautomation.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.cfa.immortalautomation.automation.AutomationAccessibilityService
import com.cfa.immortalautomation.data.ScriptRepository
import com.cfa.immortalautomation.ui.overlay.FloatingOverlayService
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel : ViewModel() {

    /* ---------- overlay ---------- */

    fun requestOverlay(ctx: Context) {
        if (!Settings.canDrawOverlays(ctx)) {
            ctx.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${ctx.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } else {
            ctx.startService(Intent(ctx, FloatingOverlayService::class.java))
        }
    }

    /* ---------- save current recording ---------- */

    fun saveCurrent(ctx: Context) {
        if (!ScriptRepository.currentExists(ctx)) {
            Toast.makeText(ctx, "No recording yet", Toast.LENGTH_SHORT).show()
            return
        }
        val name = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        ScriptRepository.commit(ctx, name)
        Toast.makeText(ctx, "Saved as $name", Toast.LENGTH_SHORT).show()
    }

    /* ---------- accessibility ---------- */

    fun requestAccessibility(ctx: Context) {
        ctx.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /* ---------- run the in‑progress recording ---------- */

    fun runScript(ctx: Context) {
        val svc = AutomationAccessibilityService.instance
        if (svc == null) {
            requestAccessibility(ctx)
            return
        }
        svc.playScript(ScriptRepository.currentFile(ctx))
    }
}
