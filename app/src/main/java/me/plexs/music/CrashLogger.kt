package me.plexs.music

import android.content.Context
import java.io.File

object CrashLogger {
    private const val FILE = "crash.log"

    private fun logFile(context: Context): File =
        File(context.filesDir, FILE)

    fun capture(context: Context, tag: String, throwable: Throwable) {
        try {
            val f = logFile(context)
            val text = buildString {
                appendLine("===== $tag @ ${System.currentTimeMillis()} =====")
                appendLine(throwable.toString())
                appendLine(android.util.Log.getStackTraceString(throwable))
                appendLine()
            }
            f.appendText(text)
        } catch (_: Throwable) {
        }
    }

    fun readLatest(context: Context): String? {
        return try {
            val f = logFile(context)
            if (!f.exists()) return null
            val lines = f.readLines()
            val sb = StringBuilder()
            for (i in lines.indices) {
                if (lines[i].startsWith("=====")) {
                    sb.clear()
                }
                sb.appendLine(lines[i])
            }
            sb.toString().trim().takeIf { it.isNotBlank() }
        } catch (_: Throwable) {
            null
        }
    }

    fun clear(context: Context) {
        try {
            logFile(context).delete()
        } catch (_: Throwable) {
        }
    }
}