package com.fingerdance

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Typeface
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.io.File
import java.io.FileInputStream
import androidx.core.graphics.toColorInt


private lateinit var soundSelecctChannel : MediaPlayer

private lateinit var soundPool: SoundPool
private var channel_mov : Int = 0
private var channel_back : Int = 0
private var up_sound : Int = 0

private var press_start : Int = 0

private lateinit var context: Context
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

private lateinit var imageCircle : ImageView
private lateinit var channel : String

//var listSongsChannel: ArrayList<Song> = ArrayList()
var listSongsChannelKsfOnline: ArrayList<SongKsf> = ArrayList()

private lateinit var recyclerChannels: ViewPager2

private lateinit var objectAnimator : ObjectAnimator

private var animIndicator: Animation? = null
private var position : Int = 0

private lateinit var valueEventListener: ValueEventListener

private lateinit var linearWaitPlayer : LinearLayout
private lateinit var txWaitForPlayer: TextView

var victoriesP1 = 0
var victoriesP2 = 0

var getSelectChannel = false
var readyPlay = false

val handlerChannelOnline = Handler(Looper.getMainLooper())
class SelectChannelOnline : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_channel_online)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        onWindowFocusChanged(true)

        soundSelecctChannel = MediaPlayer.create(this, Uri.fromFile(File(getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/channel_song.mp3")!!.absolutePath)))
        soundSelecctChannel.isLooping = true
        if(!soundSelecctChannel.isPlaying){
            soundSelecctChannel.start()
        }

        animIndicator = AnimationUtils.loadAnimation(this, R.anim.press_nav)

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_GAME)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        val filePathChannelmov = File(getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/sound_navegation.mp3")!!.absolutePath)
        val fileDecriptorChannelMov =FileInputStream(filePathChannelmov).fd
        channel_mov = soundPool.load(fileDecriptorChannelMov, 0, filePathChannelmov.length(),1)

        val filePathChannelBack = File(getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/exit_select_channel.ogg")!!.absolutePath)
        val fileDecriptorChannelBack =FileInputStream(filePathChannelBack).fd
        channel_back = soundPool.load(fileDecriptorChannelBack, 0, filePathChannelBack.length(),1)

        val filePathUpSound = File(getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/up_sound.ogg")!!.absolutePath)
        val fileDecriptorUpSound =FileInputStream(filePathUpSound).fd
        up_sound = soundPool.load(fileDecriptorUpSound, 0, filePathUpSound.length(), 1)

        val filePathStart = File(getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/start.ogg")!!.absolutePath)
        val fileDecriptorStart =FileInputStream(filePathStart).fd
        press_start = soundPool.load(fileDecriptorStart, 0, filePathStart.length(), 1)

        nav_izq = findViewById(R.id.nav_izq)
        nav_der = findViewById(R.id.nav_der)
        nav_back_Izq = findViewById(R.id.nav_izq_gray)
        nav_back_der = findViewById(R.id.nav_der_gray)
        imgFloor = findViewById(R.id.floor)
        val bmFloor = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/floor.png")!!.absolutePath)
        imgFloor.setImageBitmap(bmFloor)

        indicatorIzq = findViewById(R.id.indicatorIzq)
        val bmIzq = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/indicator.png")!!.absolutePath)
        indicatorIzq.setImageBitmap(bmIzq)
        indicatorIzq.rotation = 180f

        indicatorDer = findViewById(R.id.indicatorDer)
        val bmDer = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/indicator.png")!!.absolutePath)
        indicatorDer.setImageBitmap(bmDer)

        imgAceptar = findViewById(R.id.imgAceptar)
        val bm = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/press_floor.png")!!.absolutePath)
        imgAceptar.setImageBitmap(bm)

        imageCircle = findViewById(R.id.imageCircle)
        val bmCircle = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/preview_circle.png")!!.absolutePath)
        imageCircle.setImageBitmap(bmCircle)
        lbNombreChannel = findViewById(R.id.lbChannel)
        lbNombreChannel.isSelected = true

        val animatorSetRotation = AnimationUtils.loadAnimation(this, R.anim.animator_set_rotation)
        imageCircle.startAnimation(animatorSetRotation)

        val arrowNavIzq = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/ArrowsNav/ArrowNavIzq.png")!!.absolutePath)
        val arrowNavDer = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/ArrowsNav/ArrowNavDer.png")!!.absolutePath)
        val arrowBackIzq = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/ArrowsNav/ArrowBackIzq.png")!!.absolutePath)
        val arrowBackDer = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/ArrowsNav/ArrowBackDer.png")!!.absolutePath)

        val spriteWidth = arrowNavIzq.width / 2
        val spriteHeight = arrowNavIzq.height / 2
        val frameDuration = 800

        val navIzq = animaNavs(arrowNavIzq, spriteWidth, spriteHeight, frameDuration)
        val navDer = animaNavs(arrowNavDer, spriteWidth, spriteHeight, frameDuration)
        val navBackIzq = animaNavs(arrowBackIzq, spriteWidth, spriteHeight, frameDuration)
        val navBackDer = animaNavs(arrowBackDer, spriteWidth, spriteHeight, frameDuration)

        nav_izq.setImageDrawable(navIzq)
        nav_der.setImageDrawable(navDer)
        nav_back_Izq.setImageDrawable(navBackIzq)
        nav_back_der.setImageDrawable(navBackDer)

        context = this

        val ancho = (width * 0.6).toInt()
        val imageViewTheme = findViewById<ImageView>(R.id.imgChannel)
        val bitTheme = BitmapFactory.decodeFile(this.getExternalFilesDir("/FingerDance/Themes/$tema/logo_theme.png")!!.absolutePath)
        imageViewTheme.setImageBitmap(bitTheme)
        imageViewTheme.layoutParams.width = ancho

        recyclerChannels = findViewById(R.id.recyclerChannels)
        recyclerChannels.isUserInputEnabled = false

        recyclerChannels.clipToPadding = false
        recyclerChannels.adapter = CommandChannel(listChannelsOnline, ancho, this)
        recyclerChannels.offscreenPageLimit = 3

        objectAnimator = ObjectAnimator.ofFloat(recyclerChannels.getChildAt(position), "rotationY", 180f, 360f)
        objectAnimator.duration = 500
        objectAnimator.interpolator = AccelerateDecelerateInterpolator()

        isFocusChannel(position)

        imageCircle.layoutParams.width = width / 10 * 9
        imageCircle.layoutParams.height = width / 10 * 9

        val medidaNavs = height / 8

        nav_back_Izq.layoutParams.width = medidaNavs
        nav_back_Izq.layoutParams.height = medidaNavs

        nav_back_der.layoutParams.width = medidaNavs
        nav_back_der.layoutParams.height = medidaNavs

        nav_izq.layoutParams.width = medidaNavs
        nav_izq.layoutParams.height = medidaNavs

        nav_der.layoutParams.width = medidaNavs
        nav_der.layoutParams.height = medidaNavs

        val textSizeLbls = width / 20
        lbNombreChannel.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizeLbls.toFloat())
        lbNombreChannel.layoutParams.width = (width * 0.8).toInt()

        indicatorDer.layoutParams.width = height / 10
        indicatorDer.layoutParams.height = height / 10

        indicatorIzq.layoutParams.width = height / 10
        indicatorIzq.layoutParams.height = height / 10

        imgFloor.layoutParams.width = (width * 0.4).toInt()
        imgAceptar.layoutParams.width = (width * 0.2).toInt()
        val yDelta = width / 30
        val animateSetTraslation = TranslateAnimation(0f, 0f, -yDelta.toFloat(), (yDelta * 1.5).toFloat())
        animateSetTraslation.duration = 400
        animateSetTraslation.repeatCount = Animation.INFINITE
        animateSetTraslation.repeatMode = Animation.REVERSE
        imgAceptar.startAnimation(animateSetTraslation)
        imgAceptar.bringToFront()

        txWaitForPlayer = TextView(this).apply {
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTextColor((Color.WHITE))
            textSize = textSizeLbls / 2f
            setTypeface(typeface, Typeface.BOLD)
            setShadowLayer(2.6f, 2.5f, 1.3f, Color.GREEN)
        }
        linearWaitPlayer = findViewById(R.id.linearWaitPlayer)
        linearWaitPlayer.setBackgroundColor("#CC000000".toColorInt())
        linearWaitPlayer.addView(txWaitForPlayer)
        linearWaitPlayer.bringToFront()
        linearWaitPlayer.setOnClickListener {
            // No hace nada
        }

        val txPlayer1 = findViewById<TextView>(R.id.txPlayer1)
        val txPlayer2 = findViewById<TextView>(R.id.txPlayer2)

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                activeSala = snapshot.getValue(Sala::class.java)!!
                if(activeSala.jugador1.listo && activeSala.turno != userName && !readyPlay){
                    val intent = Intent(this@SelectChannelOnline, SelectSongOnlineWait()::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                }
                if(activeSala.jugador2.listo && activeSala.turno != userName && !readyPlay){
                    val intent = Intent(this@SelectChannelOnline, SelectSongOnlineWait()::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                }

                if(activeSala.jugador1.listo && activeSala.jugador2.listo && readyPlay){
                    handlerChannelOnline.postDelayed({
                        getSelectChannel = false
                        val intent = Intent(this@SelectChannelOnline, GameScreenActivity::class.java)
                        startActivity(intent)
                    }, 3000L)
                }
                if(isPlayer1){
                    txPlayer1.text = "Player 1 \n $userName"
                    txPlayer2.text = "Player 2 \n ${activeSala.jugador2.id}"
                }else{
                    txPlayer1.text = "Player 1 \n ${activeSala.jugador1.id}"
                    txPlayer2.text = "Player 2 \n $userName"
                }

                if (!snapshot.hasChild("jugador1") && !snapshot.hasChild("jugador2")) {
                    firebaseDatabase!!.getReference("rooms/$idSala").removeValue()
                }

                if (isPlayer1 && activeSala.jugador2.id == "") {
                    txWaitForPlayer.text = "Esperando jugador 2"
                    linearWaitPlayer.visibility = View.VISIBLE
                    return
                }

                if (activeSala.turno != userName) {
                    txWaitForPlayer.text = "Esperando selección de \n ${activeSala.turno}"
                    linearWaitPlayer.visibility = View.VISIBLE
                } else {
                    linearWaitPlayer.visibility = View.GONE
                }

            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al obtener datos: ${error.message}")
            }
        }

        if(isPlayer1){
            txPlayer1.text = "Player 1 \n $userName"
            txPlayer2.text = "Player 2 \n ${activeSala.jugador2.id}"
        }else{
            txPlayer1.text = "Player 1 \n ${activeSala.jugador1.id}"
            txPlayer2.text = "Player 2 \n $userName"
        }

        salaRef.addValueEventListener(valueEventListener)

        nav_back_Izq.setOnClickListener {
            goMain(nav_back_Izq)
        }

        nav_back_der.setOnClickListener {
            goMain(nav_back_der)
        }

        nav_izq.setOnClickListener(){
            soundPool.play(channel_mov, 1.0f, 1.0f, 1, 0, 1.0f)
            nav_izq.startAnimation(animIndicator)
            indicatorIzq.startAnimation(animIndicator)
            iluminaIndicador(indicatorIzq)

            position --
            if(position < 0){
                position = listChannelsOnline.size - 1
            }
            isFocusChannel(position)
        }

        nav_der.setOnClickListener(){
            soundPool.play(channel_mov, 1.0f, 1.0f, 1, 0, 1.0f)
            nav_der.startAnimation(animIndicator)
            indicatorDer.startAnimation(animIndicator)
            iluminaIndicador(indicatorDer)

            position++
            if(position > listChannelsOnline.size - 1){
                position = 0
            }
            isFocusChannel(position)
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Aviso")
        builder.setMessage("Este canal no contiene canciones.")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }

        imgAceptar.setOnClickListener(){
            imgAceptar.isEnabled=false
            soundPool.play(press_start, 1.0f, 1.0f, 1, 0, 1.0f)

            if(listChannelsOnline[position].listCancionesKsf.size > 0){
                listSongsChannelKsf = listChannelsOnline[position].listCancionesKsf
                currentChannel = listChannelsOnline[position].nombre

                handlerChannelOnline.postDelayed({
                    val intent = Intent(this, SelectSongOnline()::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    overridePendingTransition(R.anim.anim_command_window_on, 0)
                    imgAceptar.isEnabled=true
                    soundSelecctChannel.pause()
                }, 500)


            }else{
                builder.show()
                imgAceptar.isEnabled=true
            }
        }

        imgFloor.setOnClickListener {
            imgAceptar.performClick()
        }

        linearLayout = findViewById(R.id.background)
        linearLayout.foreground = Drawable.createFromPath(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/bg_select_channel.png")!!.absolutePath)

    }

    private fun iluminaIndicador(imageView: ImageView?) {
        val originalColorFilter = imageView!!.colorFilter

        val colorMatrix = ColorMatrix()
        val intensidad = 100000f
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float

                colorMatrix.set(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, intensidad * value,
                        0f, 1f, 0f, 0f, intensidad * value,
                        0f, 0f, 1f, 0f, intensidad * value,
                        0f, 0f, 0f, 1f, 0f
                    )
                )

                val colorFilter = ColorMatrixColorFilter(colorMatrix)
                imageView.colorFilter = colorFilter
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    imageView.colorFilter = originalColorFilter
                }
            })
        }
        animator.start()
    }

    private fun isFocusChannel (position: Int){
        val item = listChannelsOnline[position]
        recyclerChannels.setCurrentItem(position, false)
        channel = item.nombre
        lbNombreChannel.text = item.descripcion
        objectAnimator.start()
        positionCurrentChannel = position

    }

    fun animaNavs(bitmap : Bitmap, spriteWidth : Int, spriteHeight : Int, frameDuration : Int): AnimationDrawable{
        val arrowSpritesRD = arrayOf(
            Bitmap.createBitmap(bitmap, 0, 0, spriteWidth, spriteHeight),
            Bitmap.createBitmap(bitmap, spriteWidth, 0, spriteWidth, spriteHeight),
            Bitmap.createBitmap(bitmap, 0, spriteHeight, spriteWidth, spriteHeight),
            Bitmap.createBitmap(bitmap, spriteWidth, spriteHeight, spriteWidth, spriteHeight))
            val animation = AnimationDrawable().apply {
            arrowSpritesRD.forEach {
                addFrame(BitmapDrawable(it), frameDuration / 4)
            }
            isOneShot = false
        }
        animation.start()
        return animation
    }

    private fun goMain(flecha: ImageView) {
        soundPool.play(channel_back, 1.0f, 1.0f, 1, 0, 1.0f)
        flecha.startAnimation(animIndicator)
        soundSelecctChannel.pause()

        val dialog = AlertDialog.Builder(this@SelectChannelOnline)
            .setTitle("Aviso")
            .setMessage("Deseas abandonar la sala?")
            .setPositiveButton("Aceptar"){d, _ ->
                /*
                val jugador = Jugador()
                if(activeSala.jugador1.id == userName){
                    activeSala.jugador1 = jugador
                    salaRef.setValue(activeSala)
                }
                if(activeSala.jugador2.id == userName){
                    activeSala.jugador2 = jugador
                    salaRef.setValue(activeSala)
                }
                */
                isOnline = false
                victoriesP1 = 0
                victoriesP2 = 0
                this.finish()
            }
            .setNegativeButton("Cancelar"){d, _ ->
                d.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    override fun onBackPressed() {
        goMain(nav_back_Izq)
    }

    override fun onResume() {
        super.onResume()
        soundSelecctChannel.start()
        /*
        if (activeSala.turno != userName){
            txWaitForPlayer.text = "Esperando selección de \n ${activeSala.turno}"
            linearWaitPlayer.visibility = View.VISIBLE
            linearWaitPlayer.setOnClickListener {
                // No hace nada
            }
        }else{
            linearWaitPlayer.visibility = View.GONE
        }

        if(isPlayer1){
            if (activeSala.jugador2.id == "" && !firstOpen){
                txWaitForPlayer.text = "Esperando jugador 2"
                linearWaitPlayer.visibility = View.VISIBLE
                linearWaitPlayer.setOnClickListener {
                    // No hace nada
                }
            }else{
                firstOpen = true
                linearWaitPlayer.visibility = View.GONE
            }
        }
        */
        getSelectChannel = false
    }

    override fun onPause() {
        super.onPause()
        if(soundSelecctChannel.isPlaying){
            soundSelecctChannel.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handlerChannelOnline.removeCallbacksAndMessages(null)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        val decorView: View = window.decorView
        decorView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }
}
