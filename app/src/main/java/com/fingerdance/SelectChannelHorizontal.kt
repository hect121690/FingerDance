package com.fingerdance

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.fingerdance.SelectSongHorizontal.SpriteAnimator
import java.io.File
import kotlin.random.Random

private val handlerSelectChannelHorizontal = Handler(Looper.getMainLooper())

class SelectChannelHorizontal : AppCompatActivity() {

    private lateinit var bgMain: ImageView
    private lateinit var btnBackLeft: ImageView
    private lateinit var btnBackRight: ImageView
    private lateinit var btnMoveLeft: ImageView
    private lateinit var btnMoveRight: ImageView
    private lateinit var bgaSelectChannel: VideoView
    private lateinit var indicatorMoveLeft: ImageView
    private lateinit var indicatorMoveRight: ImageView
    private lateinit var lbChannelDescription: TextView

    private var animIndicator: Animation? = null

    private lateinit var animNavIzq: SpriteAnimator
    private lateinit var animNavDer: SpriteAnimator
    private lateinit var animBackIzq: SpriteAnimator
    private lateinit var animBackDer: SpriteAnimator

    private lateinit var flipAnimator: ObjectAnimator
    private lateinit var imgFlipChannel: ImageView

    private var currentPosition = 0

    private lateinit var carouselView: ChannelCarouselView

    private val choreographer = Choreographer.getInstance()

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (::carouselView.isInitialized) {
                carouselView.update()
            }
            choreographer.postFrameCallback(this)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.activity_select_channel_horizontal)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        bgMain = findViewById(R.id.imgMain)

        btnBackLeft = findViewById(R.id.btnBackLeft)
        btnBackRight = findViewById(R.id.btnBackRight)
        btnMoveLeft = findViewById(R.id.btnMoveLeft)
        btnMoveRight = findViewById(R.id.btnMoveRight)

        lbChannelDescription = findViewById(R.id.lbChannelDescription)

        indicatorMoveLeft = findViewById(R.id.indicatorMoveLeft)
        indicatorMoveRight = findViewById(R.id.indicatorMoveRight)

        imgFlipChannel = findViewById(R.id.imgFlipChannel)

        carouselView = findViewById(R.id.channelCarousel)

        bgaSelectChannel = findViewById(R.id.bgVideoSelectChannelHorizontal)
        bgaSelectChannel.visibility = View.INVISIBLE

        val valueExpand = (decimoHeigtn * 1.1).toInt()
        btnBackLeft.layoutParams.width = valueExpand
        btnBackRight.layoutParams.width = valueExpand
        btnMoveLeft.layoutParams.width = valueExpand
        btnMoveRight.layoutParams.width = valueExpand

        btnBackLeft.layoutParams.height = valueExpand
        btnBackRight.layoutParams.height = valueExpand
        btnMoveLeft.layoutParams.height = valueExpand
        btnMoveRight.layoutParams.height = valueExpand

        indicatorMoveLeft.layoutParams.width = (decimoWidth * 1.5).toInt()
        indicatorMoveRight.layoutParams.width = (decimoWidth * 1.5).toInt()

        imgFlipChannel.layoutParams.width = (height * 0.18).toInt()

        currentChannel = listChannels[0].nombre

        animIndicator = AnimationUtils.loadAnimation(this, R.anim.press_nav)

        setupResources()

        setupListeners()
    }

    private fun setupResources() {

        carouselView.setChannels(listChannels)

        imgFlipChannel.setImageBitmap(AppResources.logoTheme)

        flipAnimator = ObjectAnimator.ofFloat(imgFlipChannel, "rotationX", 0f, 360f).apply {
            duration = 600
            interpolator = AccelerateDecelerateInterpolator()
        }

        imgFlipChannel.cameraDistance = 20000f

        indicatorMoveLeft.setImageBitmap(AppResources.indicatorBitmap)
        indicatorMoveLeft.rotation = 180f

        indicatorMoveRight.setImageBitmap(AppResources.indicatorBitmap)

        animNavIzq = SpriteAnimator(btnMoveLeft, AppResources.arrowNavIzq)
        animNavDer = SpriteAnimator(btnMoveRight, AppResources.arrowNavDer)
        animBackIzq = SpriteAnimator(btnBackLeft, AppResources.arrowBackIzq)
        animBackDer = SpriteAnimator(btnBackRight, AppResources.arrowBackDer)

        animNavIzq.start()
        animNavDer.start()
        animBackIzq.start()
        animBackDer.start()

        if (isFileExists(File(bgaPathSelectChannel))) {
            bgaSelectChannel.visibility = View.VISIBLE
            bgaSelectChannel.setVideoPath(bgaPathSelectChannel)
            bgaSelectChannel.setOnPreparedListener {
                it.setVolume(0f, 0f)
            }

            bgaSelectChannel.start()

            bgaSelectChannel.setOnCompletionListener {
                bgaSelectChannel.start()
            }

        } else {
            var n = Random.nextInt(listChannels[0].listCancionesKsf.size)
            bgMain.setImageDrawable(Drawable.createFromPath(listChannels[0].listCancionesKsf[n].rutaDisc))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                bgMain.setRenderEffect(RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP))
            }
        }

        lbChannelDescription.text = listChannels[currentPosition].descripcion
    }

    private fun isFileExists(file: File): Boolean {
        return file.exists() && !file.isDirectory
    }

    private fun setupListeners() {

        btnBackLeft.setOnClickListener { goMain(btnBackLeft) }
        btnBackRight.setOnClickListener { goMain(btnBackRight) }

        btnMoveLeft.setOnClickListener {

            AppResources.soundPool?.play(
                AppResources.channelMov,
                1f, 1f, 1, 0, 1f
            )

            btnMoveLeft.startAnimation(animIndicator)
            indicatorMoveLeft.startAnimation(animIndicator)

            iluminaIndicador(indicatorMoveLeft)

            flipAnimator.cancel()
            imgFlipChannel.rotationX = 0f
            flipAnimator.start()

            carouselView.moveLeft()

            currentPosition = carouselView.getFocusedIndex()

            lbChannelDescription.text = listChannels[currentPosition].descripcion
        }

        btnMoveRight.setOnClickListener {

            AppResources.soundPool?.play(
                AppResources.channelMov,
                1f, 1f, 1, 0, 1f
            )

            btnMoveRight.startAnimation(animIndicator)
            indicatorMoveRight.startAnimation(animIndicator)

            iluminaIndicador(indicatorMoveRight)

            flipAnimator.cancel()
            imgFlipChannel.rotationX = 0f
            flipAnimator.start()

            carouselView.moveRight()

            currentPosition = carouselView.getFocusedIndex()

            lbChannelDescription.text = listChannels[currentPosition].descripcion
        }

        imgFlipChannel.setOnClickListener {
            AppResources.soundPool?.play(AppResources.pressStart, 1f, 1f, 1, 0, 1f)
            if (listChannels[currentPosition].listCancionesKsf.isNotEmpty()) {
                AppResources.listSongsChannelKsf = listChannels[currentPosition].listCancionesKsf
                currentChannel = listChannels[currentPosition].nombre
                Toast.makeText(this, "Espere por favor...", Toast.LENGTH_SHORT).show()

                if(!isOffline){
                    listenScoreChannel(listChannels[currentPosition].nombre) { listSongs ->
                        listGlobalRanking = listSongs
                        navigateToSelectSong()
                    }
                } else {
                    navigateToSelectSong()
                }

            } else {

                AlertDialog.Builder(this)
                    .setTitle("Aviso")
                    .setMessage("Este canal no contiene canciones.")
                    .setPositiveButton("OK") { d,_ -> d.dismiss() }
                    .show()
            }
        }
    }

    private fun navigateToSelectSong() {

        val intent = Intent(this, SelectSongHorizontal::class.java)

        startActivity(intent)

        overridePendingTransition(R.anim.anim_command_window_on, 0)

        AppResources.soundSelectChannel?.pause()
    }

    private fun iluminaIndicador(imageView: ImageView?) {

        imageView ?: return

        ObjectAnimator.ofFloat(imageView, "alpha", 1f, 0.3f, 1f).apply {
            duration = 500
        }.start()
    }

    private fun goMain(flecha: ImageView) {

        AppResources.soundPool?.play(
            AppResources.channelBack,
            1f, 1f, 1, 0, 1f
        )

        flecha.startAnimation(animIndicator)

        AppResources.soundSelectChannel?.pause()

        finish()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onResume() {
        super.onResume()
        choreographer.postFrameCallback(frameCallback)
        AppResources.soundSelectChannel?.start()

        try {
            bgaSelectChannel.resume()
            if (!bgaSelectChannel.isPlaying) {
                bgaSelectChannel.start()
            }

        } catch (_: Exception) {
        }
        if(AppResources.listSongsChannelKsf.isNotEmpty() && !isFileExists(File(bgaPathSelectChannel))){
            bgaSelectChannel.visibility = View.INVISIBLE
            val bitmap = BitmapFactory.decodeFile(AppResources.listSongsChannelKsf[oldValue % AppResources.listSongsChannelKsf.size].rutaDisc)
            val bit = trimTransparentEdges(bitmap)
            bgMain.setImageBitmap(bit)
            bgMain.setRenderEffect(RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP))
        }
    }

    override fun onPause() {
        super.onPause()

        choreographer.removeFrameCallback(frameCallback)

        AppResources.soundSelectChannel?.pause()

        if (bgaSelectChannel.isPlaying) {
            bgaSelectChannel.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::animNavIzq.isInitialized) animNavIzq.stop()
        if (::animNavDer.isInitialized) animNavDer.stop()
        if (::animBackIzq.isInitialized) animBackIzq.stop()
        if (::animBackDer.isInitialized) animBackDer.stop()

        handlerSelectChannelHorizontal.removeCallbacksAndMessages(null)
    }
}

class SpriteAnimator(private val imageView: ImageView, sprite: Bitmap) {

    private val frames: List<Bitmap> = SpriteCache.getFrames(sprite)

    private var frame = 0

    private val runnable = object : Runnable {
        override fun run() {

            imageView.setImageBitmap(frames[frame])

            frame = (frame + 1) % frames.size

            handlerSelectChannelHorizontal.postDelayed(this, 200)
        }
    }

    fun start() {
        handlerSelectChannelHorizontal.post(runnable)
    }

    fun stop() {
        handlerSelectChannelHorizontal.removeCallbacks(runnable)
    }
}