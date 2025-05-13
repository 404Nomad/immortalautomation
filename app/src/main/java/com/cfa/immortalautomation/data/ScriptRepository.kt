package com.cfa.immortalautomation.data

import android.content.Context
import com.cfa.immortalautomation.model.ClickAction
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File



object ScriptRepository {

    private const val DIR = "scripts"
    private const val TMP = "current.json"

    private fun dir(ctx: Context): File = File(ctx.filesDir, DIR).apply { mkdirs() }
    private fun tmp(ctx: Context): File = File(ctx.filesDir, TMP)

    /* ---------- CRUD ---------- */

    fun savePoint(ctx: Context, action: ClickAction) {
        val file = tmp(ctx)
        val list: List<ClickAction> =
            if (file.exists()) Json.decodeFromString(file.readText()) else emptyList()
        file.writeText(Json.encodeToString(list + action))
    }

    /** rename current.json -> scripts/<name>.json */
    fun commit(ctx: Context, name: String) {
        tmp(ctx).renameTo(File(dir(ctx), "$name.json"))
    }

    fun all(ctx: Context): List<File> =
        dir(ctx).listFiles { f -> f.extension == "json" }?.toList().orEmpty()

    fun delete(file: File) { file.delete() }

    fun load(file: File): List<ClickAction> =
        Json.decodeFromString(file.readText())
}
