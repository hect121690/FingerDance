package com.fingerdance

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import android.os.Looper
import android.util.Log

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Log.e("CrashHandler", "App crashed", throwable)

        if (context is Activity) {
            showCrashDialog(context, throwable)
        } else {
            android.os.Handler(Looper.getMainLooper()).post {
                if (context is Activity) {
                    showCrashDialog(context, throwable)
                }
            }
        }

        // No cerramos la app; dejamos que siga viva (por si el usuario quiere copiar el error)
    }

    private fun showCrashDialog(activity: Activity, throwable: Throwable) {
        val message = throwable.message ?: "Unknown error"
        val stackTrace = throwable.stackTraceToString()

        AlertDialog.Builder(activity)
            .setTitle("⚠️ Ocurrió un error inesperado")
            .setMessage("Detalles del error:\n$message\n\nTraza:\n$stackTrace")
            .setCancelable(true)
            .setPositiveButton("Copiar") { _, _ ->
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Stacktrace", stackTrace)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(activity, "Error copiado al portapapeles", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cerrar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
