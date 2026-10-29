package com.simonslauncher.app.util

import android.content.Context

/**
 * The launcher is the TV's only home: if it crashed at every start the TV would have no Home.
 * Two crashes within [WINDOW_MS] switch the next start to the bare safe mode.
 */
object CrashGuard {
    private const val PREFS = "crash_guard"
    private const val KEY = "times"
    private const val WINDOW_MS = 2 * 60 * 1000L

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val now = System.currentTimeMillis()
                val recent = times(app).filter { now - it < WINDOW_MS } + now
                // commit, not apply: the process is about to die.
                app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(KEY, recent.takeLast(3).joinToString(",")).commit()
            }
            previous?.uncaughtException(thread, error)
        }
    }

    fun inCrashLoop(context: Context): Boolean {
        val now = System.currentTimeMillis()
        return times(context).count { now - it < WINDOW_MS } >= 2
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).commit()
    }

    private fun times(context: Context): List<Long> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            .orEmpty().split(',').mapNotNull { it.toLongOrNull() }
}
