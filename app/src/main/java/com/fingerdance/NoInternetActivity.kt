package com.fingerdance

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout

class NoInternetActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Crear UI programáticamente con un fondo moderno
        val rootLayout = ConstraintLayout(this).apply {
            setBackgroundColor(0xFF1a1a2e.toInt())
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            )
        }

        val titleText = TextView(this).apply {
            id = android.R.id.text1
            text = "SIN CONEXIÓN"
            textSize = 32f
            setTextColor(0xFFFF6B6B.toInt())
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = dpToPx(100)
            }
        }

        val messageText = TextView(this).apply {
            id = android.R.id.text2
            text = "Verifica tu conexión a internet\n\nFingerDance requiere internet\npara funcionar correctamente"
            textSize = 16f
            setTextColor(0xFFCCCCCC.toInt()) // Gris claro
            gravity = android.view.Gravity.CENTER
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToBottom = android.R.id.text1
                topMargin = dpToPx(40)
                leftMargin = dpToPx(30)
                rightMargin = dpToPx(30)
            }
        }

        val iconText = ImageView(this).apply {
            id = android.R.id.icon
            setImageBitmap(BitmapFactory.decodeStream(assets.open("no_wifi.png")))
            layoutParams = ConstraintLayout.LayoutParams(
                dpToPx(160),
                dpToPx(160)
            ).apply {
                topToBottom = android.R.id.text2
                topMargin = dpToPx(60)
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            }

        }

        /*
        val iconText = TextView(this).apply {
            id = android.R.id.icon
            text = "📡"
            textSize = 80f
            gravity = android.view.Gravity.CENTER
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToBottom = android.R.id.text2
                topMargin = dpToPx(60)
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            }
        }
        */

        val retryButton = Button(this).apply {
            id = android.R.id.button1
            text = "REINTENTAR"
            textSize = 16f
            setTextColor(0xFF1a1a2e.toInt())
            setBackgroundColor(0xFF4ECDC4.toInt()) // Turquesa moderno
            layoutParams = ConstraintLayout.LayoutParams(
                dpToPx(250),
                dpToPx(60)
            ).apply {
                topToBottom = android.R.id.icon
                topMargin = dpToPx(80)
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            }

            setOnClickListener {
                if (isConnectedToInternet()) {
                    startActivity(Intent(this@NoInternetActivity, SplashActivity::class.java))
                    finish()
                    overridePendingTransition(0, 0)
                } else {
                    android.widget.Toast.makeText(
                        this@NoInternetActivity,
                        "Aún no hay conexión. Por favor intenta de nuevo.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        val exitButton = Button(this).apply {
            id = android.R.id.button2
            text = "SALIR"
            textSize = 14f
            setTextColor(0xFFCCCCCC.toInt())
            setBackgroundColor(0xFF2c2c3e.toInt()) // Gris oscuro
            layoutParams = ConstraintLayout.LayoutParams(
                dpToPx(200),
                dpToPx(50)
            ).apply {
                topToBottom = android.R.id.button1
                topMargin = dpToPx(20)
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            }

            setOnClickListener {
                finish()
            }
        }

        rootLayout.addView(titleText)
        rootLayout.addView(messageText)
        rootLayout.addView(iconText)
        rootLayout.addView(retryButton)
        rootLayout.addView(exitButton)

        setContentView(rootLayout)

        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }

    private fun isConnectedToInternet(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo != null && networkInfo.isConnected
        }
    }

    /**
     * Convierte DP a píxeles
     */
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
