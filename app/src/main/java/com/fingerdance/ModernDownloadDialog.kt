package com.fingerdance

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.min

class ModernDownloadDialog(private val context: Context) {
    private lateinit var dialog: AlertDialog
    private lateinit var circularProgressView: CircularProgressView
    private lateinit var textFileName: TextView
    private var currentProgress = 0

    fun show(fileName: String = "Descargando...") {
        // Container principal
        val mainContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(40, 40, 40, 40)
            setBackgroundColor(Color.WHITE)
        }

        // Nombre del archivo
        textFileName = TextView(context).apply {
            text = fileName
            textSize = 14f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 32
            }
        }

        // Custom Circular Progress View
        circularProgressView = CircularProgressView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                350,
                350
            ).apply {
                gravity = Gravity.CENTER
                bottomMargin = 24
            }
        }

        // Línea separadora
        val separatorLine = android.view.View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            ).apply {
                topMargin = 24
                bottomMargin = 16
            }
            setBackgroundColor(Color.parseColor("#EEEEEE"))
        }

        // Información adicional
        val infoText = TextView(context).apply {
            text = "Por favor, no cierres la aplicación"
            textSize = 12f
            setTextColor(Color.parseColor("#9E9E9E"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Añadir vistas al container principal
        mainContainer.addView(textFileName)
        mainContainer.addView(circularProgressView)
        mainContainer.addView(separatorLine)
        mainContainer.addView(infoText)

        // Crear diálogo
        dialog = AlertDialog.Builder(context, R.style.TransparentDialog)
            .setView(mainContainer)
            .setCancelable(false)
            .show()
    }

    fun updateProgress(progress: Int) {
        currentProgress = min(progress, 100)
        circularProgressView.setProgress(currentProgress.toFloat())
    }

    fun dismiss() {
        if (::dialog.isInitialized && dialog.isShowing) {
            dialog.dismiss()
        }
    }

    fun isShowing(): Boolean = ::dialog.isInitialized && dialog.isShowing
}
