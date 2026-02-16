package com.fingerdance

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import kotlin.math.min

class DownloadProgressDialog(private val context: Context) {
    private lateinit var dialog: AlertDialog
    private lateinit var progressBar: ProgressBar
    private lateinit var textProgress: TextView
    private lateinit var textStatus: TextView
    private lateinit var textFileName: TextView
    private var currentProgress = 0

    fun show(title: String = "Descargando...", fileName: String = "Archivo") {
        // Container principal con padding
        val mainContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(40, 30, 40, 30)
            setBackgroundColor(Color.WHITE)
        }

        // Nombre del archivo
        textFileName = TextView(context).apply {
            text = fileName
            textSize = 14f
            setTextColor(Color.GRAY)
            gravity = Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
        }

        // ProgressBar lineal con estilo mejorado
        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            progress = 0
            max = 100
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                10
            ).apply {
                bottomMargin = 20
            }
            // Color cian/turquesa moderno
            progressDrawable.setColorFilter(
                Color.parseColor("#00BCD4"),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        }

        // Container para porcentaje y estado
        val progressContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
            gravity = Gravity.CENTER_VERTICAL
        }

        // Texto del porcentaje (grande y prominente)
        textProgress = TextView(context).apply {
            text = "0%"
            textSize = 32f
            setTextColor(Color.parseColor("#00BCD4"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        // Divider visual
        val divider = TextView(context).apply {
            text = " | "
            textSize = 24f
            setTextColor(Color.LTGRAY)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = 12
                rightMargin = 12
            }
        }

        // Texto del estado
        textStatus = TextView(context).apply {
            text = "Conectando..."
            textSize = 13f
            setTextColor(Color.GRAY)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        progressContainer.addView(textProgress)
        progressContainer.addView(divider)
        progressContainer.addView(textStatus)

        // Línea separadora visual
        val separatorLine = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            ).apply {
                topMargin = 16
            }
            setBackgroundColor(Color.parseColor("#EEEEEE"))
        }

        // Información adicional
        val infoText = TextView(context).apply {
            text = "Por favor, no cierres la aplicación"
            textSize = 11f
            setTextColor(Color.parseColor("#999999"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 12
            }
        }

        // Añadir vistas al container principal
        mainContainer.addView(textFileName)
        mainContainer.addView(progressBar)
        mainContainer.addView(progressContainer)
        mainContainer.addView(separatorLine)
        mainContainer.addView(infoText)

        // Crear diálogo
        dialog = AlertDialog.Builder(context, R.style.TransparentDialog)
            .setView(mainContainer)
            .setCancelable(false)
            .show()
    }

    fun updateProgress(progress: Int, statusText: String = "") {
        currentProgress = progress
        progressBar.progress = min(progress, 100)
        textProgress.text = "$progress%"

        when {
            progress < 100 -> {
                if (statusText.isNotEmpty()) {
                    textStatus.text = statusText
                } else {
                    val speed = when {
                        progress < 25 -> "Iniciando..."
                        progress < 50 -> "Descargando..."
                        progress < 75 -> "Casi listo..."
                        else -> "Finalizando..."
                    }
                    textStatus.text = speed
                }
                textProgress.setTextColor(Color.parseColor("#00BCD4"))
            }
            progress >= 100 -> {
                // Color verde para completado
                val greenColor = Color.parseColor("#4CAF50")
                textProgress.setTextColor(greenColor)
                progressBar.progressDrawable.setColorFilter(
                    greenColor,
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
                textStatus.text = "Completado"
                textStatus.setTextColor(greenColor)
            }
        }
    }

    fun dismiss() {
        if (::dialog.isInitialized && dialog.isShowing) {
            dialog.dismiss()
        }
    }

    fun isShowing(): Boolean = ::dialog.isInitialized && dialog.isShowing
}



