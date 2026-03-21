package com.fingerdance

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.random.Random

class LoadResourcesActivity : AppCompatActivity() {

    private lateinit var linearWait: LinearLayout
    private lateinit var imgWait: ImageView
    private var rutaBase: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        if (isHorizontalMode) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_load_resources)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        rutaBase = getExternalFilesDir(null)!!.absolutePath
        // pantalla de espera
        linearWait = findViewById(R.id.linearWait)
        imgWait = findViewById(R.id.imgWait)

        val numberWait = Random.nextInt(1, 10)
        imgWait.setImageBitmap(
            BitmapFactory.decodeFile(
                "$rutaBase/FingerDance/Themes/$tema/GraphicsStatics/dance_grade/img_dance_grade ($numberWait).png"
            )
        )

        if (AppResources.isLoaded) {
            openSelector()
            return
        }

        Thread {

            AppResources.load()

            AppResources.animPressNav = AnimationUtils.loadAnimation(this, R.anim.press_nav)
            AppResources.animNameSong = AnimationUtils.loadAnimation(this, R.anim.anim_name_song)

            runOnUiThread {
                openSelector()
            }

        }.start()

    }

    private fun openSelector() {
        val intent = if (isHorizontalMode) {
            Intent(this, SelectChannelHorizontal::class.java)
        } else {
            Intent(this, SelectChannel::class.java)
        }
        startActivity(intent)
        finish()
    }
}