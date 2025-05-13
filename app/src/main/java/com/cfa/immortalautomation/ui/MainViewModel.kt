package com.cfa.immortalautomation.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import com.cfa.immortalautomation.automation.AutomationAccessibilityService
import com.cfa.immortalautomation.ui.overlay.FloatingOverlayService
import java.io.File

class MainViewModel : ViewModel() {

    /* ---------- public actions, called from MainScreen ---------- */

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

    fun requestAccessibility(ctx: Context) {
        ctx.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun runScript(ctx: Context) {
        val svc = AutomationAccessibilityService.instance
        if (svc == null) {
            // Service isn’t running yet → ask the user to enable it
            requestAccessibility(ctx)
            return
        }
        svc.playScript(File(ctx.filesDir, "script.json"))
    }

    // No ServiceConnection, nothing to unbind
    override fun onCleared() {
        super.onCleared()
    }
}
