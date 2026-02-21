package com.alarmify.meetings.debug

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Debug logger — shows Toast at every step.
 * When app crashes, shows the crash details as a Toast.
 * Remove before production release.
 */
object CrashLogger {

    private const val TAG = "CrashLogger"

    fun install(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val msg = "CRASH: ${throwable.javaClass.simpleName}\n${throwable.message}\n\n${sw.toString().take(500)}"
                Log.e(TAG, msg)
                showToast(context, msg)
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun logDebug(context: Context, tag: String, msg: String) {
        val text = "✓ [$tag] $msg"
        Log.d(TAG, text)
        showToast(context, text)
    }

    fun logError(context: Context, tag: String, throwable: Throwable) {
        val text = "✗ [$tag] ${throwable.javaClass.simpleName}: ${throwable.message}"
        Log.e(TAG, text, throwable)
        showToast(context, text)
    }

    private fun showToast(context: Context, msg: String) {
        try {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context.applicationContext, msg, Toast.LENGTH_LONG).show()
            }
        } catch (_: Exception) {}
    }
}
