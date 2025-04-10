package com.fingerdance

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileInputStream


private lateinit var soundSelecctChannel : MediaPlayer

private lateinit var soundPool: SoundPool
private var channel_mov : Int = 0
private var channel_back : Int = 0
private var up_sound : Int = 0

private var press_start : Int = 0

//private lateinit var context: Context
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

var listSongsChannel: ArrayList<Song> = ArrayList()
var listSongsChannelKsf: ArrayList<SongKsf> = ArrayList()

private lateinit var recyclerChannels: ViewPager2

private lateinit var objectAnimator : ObjectAnimator

private var animIndicator: Animation? = null
private var position : Int = 0

var currentChannel = ""
var currentSong = ""
var currentLevel = ""
lateinit var db : DataBasePlayer
var positionCurrentChannel = 0

class SelectChannel : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_channel)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
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

        //context = this

        val ancho = (width * 0.7).toInt()
        recyclerChannels = findViewById(R.id.recyclerChannels)
        recyclerChannels.isUserInputEnabled = false

        recyclerChannels.clipToPadding = false
        recyclerChannels.adapter = CommandChannel(listChannels, ancho)
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

        val textSize = width / 20
        lbNombreChannel.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
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

        db = DataBasePlayer(this)

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
                position = listChannels.size - 1
            }
            isFocusChannel(position)
        }

        nav_der.setOnClickListener(){
            soundPool.play(channel_mov, 1.0f, 1.0f, 1, 0, 1.0f)
            nav_der.startAnimation(animIndicator)
            indicatorDer.startAnimation(animIndicator)
            iluminaIndicador(indicatorDer)

            position++
            if(position > listChannels.size - 1){
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
        val thisHandler = Handler(Looper.getMainLooper())
        imgAceptar.setOnClickListener(){
            imgAceptar.isEnabled=false
            soundPool.play(press_start, 1.0f, 1.0f, 1, 0, 1.0f)

            if(listChannels[position].listCanciones.size > 0 || listChannels[position].listCancionesKsf.size > 0){

                /*
                val gson = GsonBuilder().setPrettyPrinting().create()
                val i = position
                val newList = arrayListOf<Canal>()
                //for(i in 0 until listChannels.size){
                    val listCancion = arrayListOf<Cancion>()
                    for(a in 0 until listChannels[i].listCancionesKsf.size){
                        val listNivel = arrayListOf<Nivel>()
                        for(b in 0 until listChannels[i].listCancionesKsf[a].listKsf.size){
                            val n = Nivel(listChannels[i].listCancionesKsf[a].listKsf[b].level, "", "0", "")
                            listNivel.add(n)
                        }
                        val cancion = Cancion(listChannels[i].listCancionesKsf[a].title, listNivel)
                        listCancion.add(cancion)
                    }
                    val canal = Canal(listChannels[i].nombre, listCancion)
                    newList.add(canal)
                //}

                val json = gson.toJson(canal)
                */

                escucharPuntajesPorNombre(listChannels[position].nombre) { listSongs ->
                    listGlobalRanking = listSongs
                }

                listSongsChannel = listChannels[position].listCanciones
                listSongsChannelKsf = listChannels[position].listCancionesKsf

                currentChannel = listChannels[position].nombre

                Toast.makeText(this@SelectChannel, "Espere por favor...", Toast.LENGTH_SHORT).show()
                thisHandler.postDelayed({
                    val intent = Intent(this, SelectSong()::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.anim_command_window_on, 0)
                    imgAceptar.isEnabled=true
                    soundSelecctChannel.pause()
                }, 1500)


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

    private fun escucharPuntajesPorNombre(canalNombre: String, callback: (ArrayList<Cancion>) -> Unit) {
        val databaseRef = firebaseDatabase.getReference("channels")
        val listResult = arrayListOf<Cancion>()
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (canalSnapshot in snapshot.children) {
                    val canal = canalSnapshot.child("canal").getValue(String::class.java)
                    if (canal == canalNombre) {
                        for (cancionSnapshot in canalSnapshot.child("canciones").children) {
                            val nombreCancion = cancionSnapshot.child("cancion").getValue(String::class.java) ?: ""

                            val niveles = arrayListOf<Nivel>()
                            for (nivelSnapshot in cancionSnapshot.child("niveles").children) {
                                val grade = nivelSnapshot.child("grade").getValue(String::class.java) ?: ""
                                val nivel = nivelSnapshot.child("nivel").getValue(String::class.java) ?: ""
                                val nombre = nivelSnapshot.child("nombre").getValue(String::class.java) ?: ""
                                val puntaje = nivelSnapshot.child("puntaje").getValue(String::class.java) ?: "0"

                                niveles.add(Nivel(nivel, nombre, puntaje, grade))
                            }

                            listResult.add(Cancion(nombreCancion, niveles))
                        }
                        callback(listResult)
                        return
                    }
                }
                callback(arrayListOf())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al leer datos", error.toException())
            }
        })
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
        val item = listChannels[position]
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
        //val intent = Intent(this, MainActivity::class.java)
        //startActivity(intent)
        this.finish()
    }

    override fun onBackPressed() {
        goMain(nav_back_Izq)
    }

    override fun onResume() {
        super.onResume()
        soundSelecctChannel.start()
    }

    override fun onPause() {
        super.onPause()
        if(soundSelecctChannel.isPlaying){
            soundSelecctChannel.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
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

data class Nivel(
    val nivel: String = "??",
    val nombre: String = "NO DATA",
    val puntaje: String = "0",
    val grade: String = ""
)

data class Cancion(
    val cancion: String,
    val niveles: ArrayList<Nivel>
)

data class Canal(
    val canal: String,
    val canciones: ArrayList<Cancion>
)