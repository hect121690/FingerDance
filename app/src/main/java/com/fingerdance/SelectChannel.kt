package com.fingerdance

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.gson.GsonBuilder
import java.io.File

class SelectChannel : AppCompatActivity() {

    private lateinit var lbNombreChannel: TextView
    private lateinit var linearLayout: LinearLayout
    private lateinit var nav_izq: ImageView
    private lateinit var nav_der: ImageView
    private lateinit var nav_back_Izq: ImageView
    private lateinit var nav_back_der: ImageView
    private lateinit var imgAceptar: ImageView
    private lateinit var imgFloor: ImageView
    private lateinit var indicatorIzq: ImageView
    private lateinit var indicatorDer: ImageView
    private lateinit var bgaSelectChannel: VideoView
    private lateinit var imageCircle: ImageView

    private var handlerSelectChannel = Handler(Looper.getMainLooper())
    private var position = 0
    private lateinit var recyclerChannels: ViewPager2
    private var animIndicator: Animation? = null

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.repeatCount > 0) return true
        if (event.action == KeyEvent.ACTION_DOWN) {
            return when (event.keyCode) {
                KeyEvent.KEYCODE_1 -> nav_izq.performClick()
                KeyEvent.KEYCODE_3 -> nav_der.performClick()
                KeyEvent.KEYCODE_5 -> imgAceptar.performClick()
                KeyEvent.KEYCODE_7 -> nav_back_Izq.performClick()
                KeyEvent.KEYCODE_9 -> nav_back_der.performClick()
                else -> super.dispatchKeyEvent(event)
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_select_channel)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        animIndicator = AnimationUtils.loadAnimation(this, R.anim.press_nav)

        setupViews()
        setupResources()
        setupListeners()
    }

    private fun setupViews() {
        linearLayout = findViewById(R.id.background)
        nav_izq = findViewById(R.id.nav_izq)
        nav_der = findViewById(R.id.nav_der)
        nav_back_Izq = findViewById(R.id.nav_izq_gray)
        nav_back_der = findViewById(R.id.nav_der_gray)
        imgFloor = findViewById(R.id.floor)
        indicatorIzq = findViewById(R.id.indicatorIzq)
        indicatorDer = findViewById(R.id.indicatorDer)
        imgAceptar = findViewById(R.id.imgAceptar)
        imageCircle = findViewById(R.id.imageCircle)
        val ancho = (width * 0.6).toInt()
        imageCircle.layoutParams.width = ancho

        lbNombreChannel = findViewById(R.id.lbChannel)
        recyclerChannels = findViewById(R.id.recyclerChannels)
        bgaSelectChannel = findViewById(R.id.bgaSelectChannel)
    }

    private fun setupResources() {
        AppResources.soundSelectChannel?.start()
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

            linearLayout.foreground = AppResources.backgroundDrawable
        }

        indicatorIzq.setImageBitmap(AppResources.indicatorBitmap)
        indicatorIzq.rotation = 180f
        indicatorDer.setImageBitmap(AppResources.indicatorBitmap)
        imageCircle.setImageBitmap(AppResources.logoTheme)

        imgFloor.setImageBitmap(AppResources.bmFloor)
        imgAceptar.setImageBitmap(AppResources.bmAceptar)

        imgFloor.layoutParams.width = (width * 0.4).toInt()
        imgAceptar.layoutParams.width = (width * 0.2).toInt()
        val yDelta = width / 30
        val animateSetTraslation = TranslateAnimation(0f, 0f, -yDelta.toFloat(), (yDelta * 1.5).toFloat())
        animateSetTraslation.duration = 400
        animateSetTraslation.repeatCount = Animation.INFINITE
        animateSetTraslation.repeatMode = Animation.REVERSE
        imgAceptar.startAnimation(animateSetTraslation)
        imgAceptar.bringToFront()

        val arrowNavIzq = animaNavs(AppResources.arrowNavIzq!!)
        val arrowNavDer = animaNavs(AppResources.arrowNavDer!!)
        val arrowBackIzq = animaNavs(AppResources.arrowBackIzq!!)
        val arrowBackDer = animaNavs(AppResources.arrowBackDer!!)

        nav_izq.setImageDrawable(arrowNavIzq)
        nav_der.setImageDrawable(arrowNavDer)
        nav_back_Izq.setImageDrawable(arrowBackIzq)
        nav_back_der.setImageDrawable(arrowBackDer)

        recyclerChannels.adapter = CommandChannel(listChannels, (width * 0.6).toInt(), this)

        isFocusChannel(position)
    }

    private fun isFileExists(file: File): Boolean {
        return file.exists() && !file.isDirectory
    }

    private fun setupListeners() {
        nav_back_Izq.setOnClickListener { goMain(nav_back_Izq) }
        nav_back_der.setOnClickListener { goMain(nav_back_der) }

        nav_izq.setOnClickListener {
            AppResources.soundPool?.play(AppResources.channelMov, 1f,1f,1,0,1f)

            nav_izq.startAnimation(animIndicator)
            indicatorIzq.startAnimation(animIndicator)

            iluminaIndicador(indicatorIzq)

            position--

            if (position < 0) position = listChannels.size - 1

            isFocusChannel(position)
        }

        nav_der.setOnClickListener {

            AppResources.soundPool?.play(
                AppResources.channelMov,
                1f,1f,1,0,1f
            )

            nav_der.startAnimation(animIndicator)
            indicatorDer.startAnimation(animIndicator)

            iluminaIndicador(indicatorDer)

            position++

            if (position > listChannels.size - 1) position = 0

            isFocusChannel(position)
        }

        imgAceptar.setOnClickListener {
            imgAceptar.isEnabled = false
            AppResources.soundPool?.play(AppResources.pressStart, 1f,1f,1,0,1f)
            if (listChannels[position].listCanciones.isNotEmpty()) {
                AppResources.listSongsChannelKsf = listChannels[position].listCanciones
                currentChannel = listChannels[position].nombre
                Toast.makeText(this, "Espere por favor...", Toast.LENGTH_SHORT).show()
                if(listChannels[position].listCanciones.first().isSSC){
                    isOffline = true
                }


                /*
                val gson = GsonBuilder().setPrettyPrinting().create()
                //val i = position
                val listCanales = arrayListOf<Canal>()
                for(i in 0 until listChannels.size) {
                    val listCanciones = arrayListOf<Cancion>()
                    for (a in 0 until listChannels[i].listCancionesKsf.size) {
                        val listNiveles = arrayListOf<Nivel>()
                        for (b in 0 until listChannels[i].listCancionesKsf[a].listKsf.size) {
                            val checkedValues = listChannels[i].listCancionesKsf[a].listKsf[b].checkedValues
                            val level = listChannels[i].listCancionesKsf[a].listKsf[b].level
                            val type = listChannels[i].listCancionesKsf[a].listKsf[b].typeSteps
                            val player = listChannels[i].listCancionesKsf[a].listKsf[b].typePlayer
                            val n = Nivel(level, checkedValues, type, player, ArrayList(List(3) { FirstRank() }))
                            listNiveles.add(n)
                        }
                        val cancion = Cancion(listChannels[i].listCancionesKsf[a].title, listNiveles)
                        listCanciones.add(cancion)
                    }
                    val canal = Canal(listChannels[i].nombre, listCanciones)
                    listCanales.add(canal)
                }

                val json = gson.toJson(listCanales)
                Log.d("JSON", json)
                */


                if(!isOffline){
                    listenScoreChannel(listChannels[position].nombre) { listSongs ->
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

                imgAceptar.isEnabled = true
            }
        }

        imgFloor.setOnClickListener { imgAceptar.performClick() }
    }


    private fun animaNavs(bitmap: android.graphics.Bitmap): AnimationDrawable {

        val spriteWidth = bitmap.width / 2
        val spriteHeight = bitmap.height / 2

        val animation = AnimationDrawable()

        for (r in 0 until 2) {
            for (c in 0 until 2) {

                val frame = android.graphics.Bitmap.createBitmap(
                    bitmap,
                    c * spriteWidth,
                    r * spriteHeight,
                    spriteWidth,
                    spriteHeight
                )

                animation.addFrame(BitmapDrawable(resources, frame), 200)
            }
        }

        animation.isOneShot = false
        animation.start()

        return animation
    }

    private fun iluminaIndicador(imageView: ImageView?) {
        imageView ?: return
        ObjectAnimator.ofFloat(imageView, "alpha", 1f, 0.3f, 1f).apply {
            duration = 500
        }.start()
    }

    private fun isFocusChannel(position: Int) {
        val item = listChannels[position]
        recyclerChannels.setCurrentItem(position, false)
        channel = item.nombre
        lbNombreChannel.text = item.descripcion
        positionCurrentChannel = position
    }

    private fun goMain(flecha: ImageView) {

        AppResources.soundPool?.play(
            AppResources.channelBack,
            1f,1f,1,0,1f
        )

        flecha.startAnimation(animIndicator)

        AppResources.soundSelectChannel?.pause()

        finish()
    }

    private fun navigateToSelectSong() {
        val intent = Intent(this, SelectSong::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.anim_command_window_on, 0)
        imgAceptar.isEnabled = true
        AppResources.soundSelectChannel?.pause()
    }

    override fun onResume() {
        super.onResume()
        AppResources.soundSelectChannel?.start()
        try {
            bgaSelectChannel.resume()
            if (!bgaSelectChannel.isPlaying) {
                bgaSelectChannel.start()
            }

        } catch (_: Exception) { }
    }

    override fun onPause() {
        super.onPause()

        AppResources.soundSelectChannel?.pause()

        if (bgaSelectChannel.isPlaying) {
            bgaSelectChannel.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        handlerSelectChannel.removeCallbacksAndMessages(null)
    }

    override fun onBackPressed() {
        goMain(nav_back_Izq)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {

        val controller = WindowCompat.getInsetsController(window, window.decorView)

        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}