package com.fingerdance

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.LruCache
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import android.view.Choreographer
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.fingerdance.SelectChannel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.io.File
import java.io.FileInputStream
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
    private lateinit var linearWait: LinearLayout
    private lateinit var imgWait: ImageView
    private lateinit var lbChannelDescription: TextView

    private var rutaBase = ""
    private lateinit var soundSelecctChannel : MediaPlayer
    private var animIndicator: Animation? = null
    private lateinit var soundPool: SoundPool

    private lateinit var animNavIzq: SpriteAnimator
    private lateinit var animNavDer: SpriteAnimator
    private lateinit var animBackIzq: SpriteAnimator
    private lateinit var animBackDer: SpriteAnimator

    private var channelMov : Int = 0
    private var channelBack : Int = 0
    private var upSound : Int = 0
    private var pressStart : Int = 0

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

        rutaBase = getExternalFilesDir(null)!!.absolutePath

        // ---------- UI references ----------
        bgMain = findViewById(R.id.imgMain)

        btnBackLeft = findViewById(R.id.btnBackLeft)
        btnBackRight = findViewById(R.id.btnBackRight)
        btnMoveLeft = findViewById(R.id.btnMoveLeft)
        btnMoveRight = findViewById(R.id.btnMoveRight)
        lbChannelDescription = findViewById(R.id.lbChannelDescription)


        indicatorMoveLeft = findViewById(R.id.indicatorMoveLeft)
        indicatorMoveRight = findViewById(R.id.indicatorMoveRight)

        imgFlipChannel = findViewById(R.id.imgFlipChannel)

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

        carouselView = findViewById(R.id.channelCarousel)

        bgaSelectChannel = findViewById(R.id.bgVideoSelectChannelHorizontal)
        bgaSelectChannel.visibility = View.INVISIBLE

        // pantalla de espera
        linearWait = findViewById(R.id.linearWait)
        imgWait = findViewById(R.id.imgWait)

        val numberWait = Random.nextInt(1, 10)
        imgWait.setImageBitmap(
            BitmapFactory.decodeFile(
                "$rutaBase/FingerDance/Themes/$tema/GraphicsStatics/dance_grade/img_dance_grade ($numberWait).png"
            )
        )
        currentChannel = listChannels[0].nombre
        linearWait.visibility = View.VISIBLE

        // animación botones
        animIndicator = AnimationUtils.loadAnimation(this, R.anim.press_nav)

        // listeners
        btnBackLeft.setOnClickListener { goMain(btnBackLeft) }
        btnBackRight.setOnClickListener { goMain(btnBackRight) }

        btnMoveLeft.setOnClickListener {
            soundPool.play(channelMov,1f,1f,1,0,1f)
            btnMoveLeft.startAnimation(animIndicator)
            indicatorMoveLeft.startAnimation(animIndicator)
            iluminaIndicador(indicatorMoveLeft)
            flipAnimator.cancel()
            imgFlipChannel.rotationX = 0f
            flipAnimator.start()
            carouselView.moveLeft()
            currentPosition = carouselView.getFocusedIndex()
            lbChannelDescription.text = listChannels[currentPosition].descripcion //carouselView.getFocusedChannel()?.descripcion

        }

        btnMoveRight.setOnClickListener {
            soundPool.play(channelMov,1f,1f,1,0,1f)
            btnMoveRight.startAnimation(animIndicator)
            indicatorMoveRight.startAnimation(animIndicator)
            iluminaIndicador(indicatorMoveRight)
            flipAnimator.cancel()
            imgFlipChannel.rotationX = 0f
            flipAnimator.start()
            carouselView.moveRight()
            currentPosition = carouselView.getFocusedIndex()
            lbChannelDescription.text = listChannels[currentPosition].descripcion //carouselView.getFocusedChannel()?.descripcion
        }

        imgFlipChannel.setOnClickListener {
            soundPool.play(pressStart,1f,1f,1,0,1f)
            listSongsChannelKsf = listChannels[currentPosition].listCancionesKsf
            channel = listChannels[currentPosition].nombre
            currentChannel = channel
            positionCurrentChannel = currentPosition
            channelIndex = currentPosition
            Toast.makeText(this, "Espere por favor...", Toast.LENGTH_SHORT).show()
            if(!isOffline){
                listenScoreChannel(channel){ listSongs ->
                    listGlobalRanking = listSongs
                    navigateToSelectSong()
                }
            }else{
                navigateToSelectSong()

            }
        }

        soundSelecctChannel = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setDataSource("$rutaBase/FingerDance/Themes/$tema/Sounds/channel_song.mp3")
            prepare()
            isLooping = true
            //start()
        }

        Thread {

            loadHeavyResources()

        }.start()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun loadHeavyResources() {

        // ---------- BITMAPS ----------
        val circleBitmap = BitmapFactory.decodeFile("$rutaBase/FingerDance/Themes/$tema/logo_theme.png")

        val arrowNavIzq = BitmapFactory.decodeFile("$rutaBase/FingerDance/Themes/$tema/ArrowsNav/ArrowNavIzq.png")
        val arrowNavDer = BitmapFactory.decodeFile("$rutaBase/FingerDance/Themes/$tema/ArrowsNav/ArrowNavDer.png")
        val arrowBackIzq = BitmapFactory.decodeFile("$rutaBase/FingerDance/Themes/$tema/ArrowsNav/ArrowBackIzq.png")
        val arrowBackDer = BitmapFactory.decodeFile("$rutaBase/FingerDance/Themes/$tema/ArrowsNav/ArrowBackDer.png")

        val indicatorBitmap = BitmapFactory.decodeFile("$rutaBase/FingerDance/Themes/$tema/GraphicsStatics/indicator.png")

        // ---------- AUDIO ----------
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_GAME)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        getSondsPool()

        // ---------- aplicar en UI ----------
        runOnUiThread {

            carouselView.setChannels(listChannels)

            imgFlipChannel.setImageBitmap(circleBitmap)

            flipAnimator = ObjectAnimator.ofFloat(imgFlipChannel,"rotationX",0f,360f).apply{
                duration = 600
                interpolator = AccelerateDecelerateInterpolator()
            }

            imgFlipChannel.cameraDistance = 20000f

            indicatorMoveLeft.setImageBitmap(indicatorBitmap)
            indicatorMoveLeft.rotation = 180f

            indicatorMoveRight.setImageBitmap(indicatorBitmap)

            animNavIzq = SpriteAnimator(btnMoveLeft,arrowNavIzq)
            animNavDer = SpriteAnimator(btnMoveRight,arrowNavDer)

            animBackIzq = SpriteAnimator(btnBackLeft,arrowBackIzq)
            animBackDer = SpriteAnimator(btnBackRight,arrowBackDer)

            animNavIzq.start()
            animNavDer.start()
            animBackIzq.start()
            animBackDer.start()

            // video
            if (isFileExists(File(bgaPathSelectChannel))) {
                bgaSelectChannel.visibility = View.VISIBLE
                bgaSelectChannel.setVideoPath(bgaPathSelectChannel)
                bgaSelectChannel.setOnPreparedListener { md ->
                    md.setVolume(0f,0f)
                }

                bgaSelectChannel.start()
                bgaSelectChannel.setOnCompletionListener {
                    bgaSelectChannel.start()
                }

            } else {
                if(listSongsChannelKsf.isEmpty()){
                    bgaSelectChannel.visibility = View.INVISIBLE
                    val n = Random.nextInt(listChannels[0].listCancionesKsf.size)
                    bgMain.setImageDrawable(Drawable.createFromPath(listChannels[0].listCancionesKsf[n].rutaDisc))
                    bgMain.setRenderEffect(RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP))
                }
            }

            hideLoadingAndStartMusic()
            lbChannelDescription.text = listChannels[currentPosition].descripcion//carouselView.getFocusedChannel()?.descripcion
        }
    }


    private fun hideLoadingAndStartMusic() {
        linearWait.animate()
            .translationY(linearWait.height.toFloat())
            .alpha(0f)
            .setDuration(400)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                linearWait.visibility = View.GONE
                soundSelecctChannel.start()
            }
    }

    private fun navigateToSelectSong() {
        val intent = Intent(this, SelectSong()::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.anim_command_window_on, 0)
        soundSelecctChannel.pause()
    }

    private fun listenScoreChannel(canalNombre: String, callback: (ArrayList<Cancion>) -> Unit) {
        val canalRef = firebaseDatabase!!.getReference("channels").orderByChild("canal").equalTo(canalNombre)
        val listResult = arrayListOf<Cancion>()
        canalRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (canalSnapshot in snapshot.children) {
                    val cancionesSnapshot = canalSnapshot.child("canciones")
                    for (cancionSnapshot in cancionesSnapshot.children) {
                        val nombreCancion = cancionSnapshot.child("cancion").getValue(String::class.java) ?: ""
                        val niveles = arrayListOf<Nivel>()

                        for (nivelSnapshot in cancionSnapshot.child("niveles").children) {
                            val numberNivel = nivelSnapshot.child("nivel").getValue(String::class.java) ?: ""
                            val checkedValues = nivelSnapshot.child("checkedValues").getValue(String::class.java) ?: ""
                            val type = nivelSnapshot.child("type").getValue(String::class.java) ?: ""
                            val player = nivelSnapshot.child("player").getValue(String::class.java) ?: ""
                            val rankings = arrayListOf<FirstRank>()
                            for (rankingSnapshot in nivelSnapshot.child("fisrtRank").children) {
                                val nombre = rankingSnapshot.child("nombre").getValue(String::class.java) ?: ""
                                val puntaje = rankingSnapshot.child("puntaje").getValue(String::class.java) ?: "0"
                                val grade = rankingSnapshot.child("grade").getValue(String::class.java) ?: ""
                                rankings.add(FirstRank(nombre, puntaje, grade))
                            }

                            niveles.add(Nivel(numberNivel, checkedValues, type, player, rankings))
                        }

                        listResult.add(Cancion(nombreCancion, niveles))
                    }
                }

                callback(listResult)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al leer canciones del canal $canalNombre", error.toException())
                callback(listResult)
            }
        })
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

    private fun getSondsPool(){
        val filePathChannelmov = File("$rutaBase/FingerDance/Themes/$tema/Sounds/sound_navegation.mp3")
        val fileDecriptorChannelMov =FileInputStream(filePathChannelmov).fd
        channelMov = soundPool.load(fileDecriptorChannelMov, 0, filePathChannelmov.length(),1)

        val filePathChannelBack = File("$rutaBase/FingerDance/Themes/$tema/Sounds/exit_select_channel.ogg")
        val fileDecriptorChannelBack =FileInputStream(filePathChannelBack).fd
        channelBack = soundPool.load(fileDecriptorChannelBack, 0, filePathChannelBack.length(),1)

        val filePathUpSound = File("$rutaBase/FingerDance/Themes/$tema/Sounds/up_sound.ogg")
        val fileDecriptorUpSound =FileInputStream(filePathUpSound).fd
        upSound = soundPool.load(fileDecriptorUpSound, 0, filePathUpSound.length(), 1)

        val filePathStart = File("$rutaBase/FingerDance/Themes/$tema/Sounds/start.ogg")
        val fileDecriptorStart =FileInputStream(filePathStart).fd
        pressStart = soundPool.load(fileDecriptorStart, 0, filePathStart.length(), 1)
    }

    private fun iluminaIndicador(imageView: ImageView?) {
        imageView ?: return
        val animator = ObjectAnimator.ofFloat(imageView, "alpha", 1f, 0.3f, 1f).apply {
            duration = 500
        }
        animator.start()
    }

    private fun goMain(flecha: ImageView) {
        soundPool.play(channelBack, 1.0f, 1.0f, 1, 0, 1.0f)
        flecha.startAnimation(animIndicator)
        soundSelecctChannel.pause()
        this.finish()
    }

    private fun isFileExists(file: File): Boolean {
        return file.exists() && !file.isDirectory
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onResume() {
        super.onResume()

        if(listSongsChannelKsf.isNotEmpty()){
            bgaSelectChannel.visibility = View.INVISIBLE
            val bitmap = BitmapFactory.decodeFile(listSongsChannelKsf[oldValue % listSongsChannelKsf.size].rutaDisc)
            val bit = trimTransparentEdges(bitmap)
            bgMain.setImageBitmap(bit)
            bgMain.setRenderEffect(RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP))
        }

        choreographer.postFrameCallback(frameCallback)

        try {
            soundSelecctChannel.start()

            bgaSelectChannel.resume() // si usas API 26+
            if (!bgaSelectChannel.isPlaying) {
                bgaSelectChannel.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            bgaSelectChannel.setVideoPath(bgaPathSelectChannel)
            bgaSelectChannel.start()
        }
    }

    override fun onPause() {
        super.onPause()

        choreographer.removeFrameCallback(frameCallback)

        if(soundSelecctChannel.isPlaying){
            soundSelecctChannel.pause()
        }
        if(bgaSelectChannel.isPlaying){
            bgaSelectChannel.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::animNavIzq.isInitialized) animNavIzq.stop()
        if (::animNavDer.isInitialized) animNavDer.stop()
        if (::animBackIzq.isInitialized) animBackIzq.stop()
        if (::animBackDer.isInitialized) animBackDer.stop()

        if (::soundSelecctChannel.isInitialized) {
            try {
                soundSelecctChannel.stop()
                soundSelecctChannel.reset()
                soundSelecctChannel.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        handlerSelectChannelHorizontal.removeCallbacksAndMessages(null)
    }
}

object BannerCache {

    private val cache = LruCache<String, Bitmap>(20)

    fun get(path: String): Bitmap? {
        return cache.get(path)
    }

    fun load(path: String): Bitmap? {

        val cached = cache.get(path)
        if (cached != null) return cached

        val file = File(path)
        if (!file.exists()) return null

        val bitmap = BitmapFactory.decodeFile(path)

        if (bitmap != null) {
            cache.put(path, bitmap)
        }

        return bitmap
    }
}