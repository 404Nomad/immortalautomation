package com.cfa.immortalautomation.data

import android.content.Context
import com.cfa.immortalautomation.model.ClickAction
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object ScriptRepository {

    private const val DIR     = "scripts"
    private const val CURRENT = "current.json"

    private fun dir(ctx: Context): File = File(ctx.filesDir, DIR).apply { mkdirs() }
    private fun current(ctx: Context): File = File(ctx.filesDir, "$DIR/$CURRENT")

    /* ---------- record ---------- */

    fun savePoint(ctx: Context, action: ClickAction) {
        val file = current(ctx)
        val previous = runCatching {
            if (file.exists() && file.length() > 0) {
                Json.decodeFromString<List<ClickAction>>(file.readText())
            } else emptyList()
        }.getOrElse {
            file.delete()
            emptyList()
        }
        file.writeText(Json.encodeToString(previous + action))
    }

    /* ---------- commit / list / delete ---------- */

    fun commit(ctx: Context, name: String) =
        current(ctx).renameTo(File(dir(ctx), "$name.json"))

    fun all(ctx: Context): List<File> =
        dir(ctx).listFiles { f -> f.extension == "json" }?.toList().orEmpty()

    fun delete(file: File) { file.delete() }

    fun load(file: File): List<ClickAction> =
        Json.decodeFromString(file.readText())

    /* ---------- helpers ---------- */

    fun currentFile(ctx: Context): File = current(ctx)
    fun currentExists(ctx: Context): Boolean = current(ctx).exists()
}
