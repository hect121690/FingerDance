package com.fingerdance

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.fingerdance.SelectChannelHorizontal.SpriteAnimator
import java.io.File

private val handlerSelectSongHorizontal = Handler(Looper.getMainLooper())

class SelectSongHorizontal : AppCompatActivity() {

    private var rutaBase = ""
    private lateinit var imgPreview: ImageView
    private lateinit var btnBackLeft: ImageView
    private lateinit var btnBackRight: ImageView
    private lateinit var btnMoveLeft: ImageView
    private lateinit var btnMoveRight: ImageView
    private lateinit var bgaPreview: VideoView
    private lateinit var imgCircleRotate: ImageView

    private lateinit var animOn: Animation
    private lateinit var animOff: Animation
    private lateinit var animPressNav: Animation
    private lateinit var animNameSong: Animation

    private lateinit var animNavIzq: SpriteAnimator
    private lateinit var animNavDer: SpriteAnimator
    private lateinit var animBackIzq: SpriteAnimator
    private lateinit var animBackDer: SpriteAnimator



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_select_song_horizontal)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        rutaBase = getExternalFilesDir(null)!!.absolutePath

        animPressNav = AnimationUtils.loadAnimation(this, R.anim.press_nav)
        animNameSong = AnimationUtils.loadAnimation(this, R.anim.anim_name_song)
        animOn = AnimationUtils.loadAnimation(this, R.anim.anim_command_window_on)
        animOff = AnimationUtils.loadAnimation(this, R.anim.anim_command_window_off)

        imgPreview = findViewById(R.id.imgPreview)
        bgaPreview = findViewById(R.id.bgaPreview)
        imgCircleRotate = findViewById(R.id.imgCircleRotate)
        btnBackLeft = findViewById(R.id.btnBackLeftSong)
        btnBackRight = findViewById(R.id.btnBackRightSong)
        btnMoveLeft = findViewById(R.id.btnMoveLeftSong)
        btnMoveRight = findViewById(R.id.btnMoveRightSong)

        val valueExpand = (decimoHeigtn * 1.1).toInt()

        val buttons = listOf(
            btnBackLeft,
            btnBackRight,
            btnMoveLeft,
            btnMoveRight
        )

        buttons.forEach { button ->
            button.layoutParams.width = valueExpand
            button.layoutParams.height = valueExpand
            button.requestLayout()
        }

        if(isFileExists(File(listSongsChannelKsf[0].rutaPreview))){

        }

        val arrowNavIzq = BitmapFactory.decodeFile("$rutaBase/FingerDance/Themes/$tema/ArrowsNav/ArrowNavIzq.png")
        val arrowNavDer = BitmapFactory.decodeFile("$rutaBase/FingerDance/Themes/$tema/ArrowsNav/ArrowNavDer.png")
        val arrowBackIzq = BitmapFactory.decodeFile("$rutaBase/FingerDance/Themes/$tema/ArrowsNav/ArrowBackIzq.png")
        val arrowBackDer = BitmapFactory.decodeFile("$rutaBase/FingerDance/Themes/$tema/ArrowsNav/ArrowBackDer.png")

        animNavIzq = SpriteAnimator(btnMoveLeft,arrowNavIzq)
        animNavDer = SpriteAnimator(btnMoveRight,arrowNavDer)

        animBackIzq = SpriteAnimator(btnBackLeft,arrowBackIzq)
        animBackDer = SpriteAnimator(btnBackRight,arrowBackDer)

        animNavIzq.start()
        animNavDer.start()
        animBackIzq.start()
        animBackDer.start()

        btnBackLeft.setOnClickListener {
            it.startAnimation(animPressNav)
            goSelectChannelHorizontal()
        }
        btnBackRight.setOnClickListener {
            it.startAnimation(animPressNav)
            goSelectChannelHorizontal()
        }



    }

    private fun isFileExists(file: File): Boolean {
        return file.exists() && !file.isDirectory
    }

    class SpriteAnimator(imageView: ImageView, sprite: Bitmap) {

        private val frames: List<Bitmap>
        private var frame = 0

        init {

            val frameWidth = sprite.width / 2
            val frameHeight = sprite.height / 2

            val tempFrames = mutableListOf<Bitmap>()

            for (r in 0 until 2) {
                for (c in 0 until 2) {

                    val rawFrame = Bitmap.createBitmap(
                        sprite,
                        c * frameWidth,
                        r * frameHeight,
                        frameWidth,
                        frameHeight
                    )

                    val trimmed = trimTransparentEdges(rawFrame)

                    tempFrames.add(trimmed)
                }
            }

            frames = tempFrames
        }

        private val runnable = object : Runnable {
            override fun run() {

                imageView.setImageBitmap(frames[frame])

                frame = (frame + 1) % frames.size

                handlerSelectSongHorizontal.postDelayed(this, 200)
            }
        }

        fun start() {
            handlerSelectSongHorizontal.post(runnable)
        }

        fun stop() {
            handlerSelectSongHorizontal.removeCallbacks(runnable)
        }
    }

    private fun goSelectChannelHorizontal() {
        handlerSelectSongHorizontal.removeCallbacksAndMessages(null)

        this.finish()
        overridePendingTransition(0,R.anim.anim_command_window_off)
    }
}