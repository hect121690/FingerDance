package com.fingerdance

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout

class NoInternetActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screenWidth = resources.displayMetrics.widthPixels
        val iconSize = (screenWidth * 0.25).toInt()

        val rootLayout = ConstraintLayout(this).apply {
            setBackgroundColor(0xFF1a1a2e.toInt())
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            )
        }

        val container = LinearLayout(this).apply {
            id = android.R.id.content
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            }
        }

        val titleText = TextView(this).apply {
            text = "SIN CONEXIÓN"
            textSize = 30f
            setTextColor(0xFFFF6B6B.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        val messageText = TextView(this).apply {
            text = "Verifica tu conexión a internet\n\nFingerDance requiere internet\npara funcionar correctamente"
            textSize = 16f
            setTextColor(0xFFCCCCCC.toInt())
            gravity = Gravity.CENTER
            setPadding(dpToPx(30), dpToPx(20), dpToPx(30), dpToPx(20))
        }

        val iconImage = ImageView(this).apply {
            setImageBitmap(BitmapFactory.decodeStream(assets.open("no_wifi.png")))
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                topMargin = dpToPx(20)
                bottomMargin = dpToPx(30)
                gravity = Gravity.CENTER
            }
        }

        val retryButton = Button(this).apply {
            text = "REINTENTAR"
            textSize = 16f
            setTextColor(0xFF1a1a2e.toInt())
            setBackgroundColor(0xFF4ECDC4.toInt())
            layoutParams = LinearLayout.LayoutParams(dpToPx(240), dpToPx(55)).apply {
                gravity = Gravity.CENTER
                bottomMargin = dpToPx(15)
            }

            setOnClickListener {
                if (isConnectedToInternet()) {
                    startActivity(Intent(this@NoInternetActivity, SplashActivity::class.java))
                    finish()
                    overridePendingTransition(0,0)
                } else {
                    android.widget.Toast.makeText(
                        this@NoInternetActivity,
                        "Aún no hay conexión. Intenta nuevamente.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        val exitButton = Button(this).apply {
            text = "SALIR"
            textSize = 14f
            setTextColor(0xFFCCCCCC.toInt())
            setBackgroundColor(0xFF2c2c3e.toInt())
            layoutParams = LinearLayout.LayoutParams(dpToPx(200), dpToPx(50)).apply {
                gravity = Gravity.CENTER
            }

            setOnClickListener {
                finish()
            }
        }

        container.addView(titleText)
        container.addView(messageText)
        container.addView(iconImage)
        container.addView(retryButton)
        container.addView(exitButton)

        rootLayout.addView(container)

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

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}