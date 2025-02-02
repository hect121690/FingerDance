package com.fingerdance

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.DisplayMetrics
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.Serializable
import kotlin.system.exitProcess


lateinit var themes : SharedPreferences
var tema : String = ""
private var descargando = true
var height: Int = 0
var width: Int = 0

var skinSelected : String = ""
var speedSelected : String = ""
//var typeSpeedSelected : String = ""
var bgaOff : String = ""

var latency = 0L
var configLatency = false
var countAdd = 0

var showAddActive = false

val gson = Gson()

var numberUpdate = ""
var versionUpdate = ""

var showPadB : Int = 0
var hideImagesPadA : Boolean = false
var skinPad = ""
var alphaPadB = 1f
var listNoteSkinAdditionals = ArrayList<CommandValues>()

var countSongsPlayed = 0

//PHOENIX
const val ONE_ADDITIONAL_NOTESKIN = 30
//MISILE
const val TWO_ADDITIONAL_NOTESKIN = 91
//INIFINITY
const val THREE_ADDITIONAL_NOTESKIN = 152
//NEW EXTRA
const val FOUR_ADDITIONAL_NOTESKIN = 233
//NEXT XENESIS 2
const val FIVE_ADDITIONAL_NOTESKIN = 284
//INTERFERENCE
const val SIX_ADDITIONAL_NOTESKIN = 345

class MainActivity : AppCompatActivity(), Serializable {
    private lateinit var video_fondo : VideoView
    private lateinit var bg_download : VideoView
    private lateinit var mediaPlayerMain : MediaPlayer
    private var soundPlayer : MediaPlayer? = null
    private var currentVideoPosition : Int = 0
    private lateinit var animLogo : ImageView
    private lateinit var btnPlay : Button
    private lateinit var btnOptions : Button
    private lateinit var btnExit : Button

    private lateinit var linearDownload : ConstraintLayout
    private lateinit var imageIcon : ImageView
    private lateinit var lbDescargando : TextView
    private lateinit var progressBar : ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_main)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        themes = getPreferences(Context.MODE_PRIVATE)
        tema = themes.getString("theme", "default").toString()
        skinSelected = themes.getString("skin", "").toString()
        speedSelected = themes.getString("speed", "").toString()
        countAdd = themes.getInt("countAdd", 0)
        showPadB = themes.getInt("showPadB", 0)
        hideImagesPadA = themes.getBoolean("hideImagesPadA", false)
        skinPad = themes.getString("skinPad", "default").toString()
        alphaPadB = themes.getFloat("alphaPadB", 1f)
        countSongsPlayed = themes.getInt("countSongsPlayed", 0)
        versionUpdate = themes.getString("versionUpdate", "0.0.0").toString()

        val jsonListCommandsValues = themes.getString("listNoteSkinAdditionals", "")
        listNoteSkinAdditionals = if (!jsonListCommandsValues.isNullOrEmpty()) {
            gson.fromJson(jsonListCommandsValues, object : TypeToken<List<CommandValues>>() {}.type)
        } else {
            ArrayList()
        }

        //themes.edit().putString("versionUpdate", "0.0.0").apply()

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        width = metrics.widthPixels
        height = metrics.heightPixels

        if(tema == ""){
            tema ="default"
        }

        //themes.edit().putString("allTunes", "").apply()
        //themes.edit().putString("efects", "").apply()

        linearDownload = findViewById(R.id.linearDownload)

        if(descargando){
            linearDownload.setOnClickListener {
                creaDescarga()
            }
        }

        imageIcon = findViewById(R.id.imageIcon)
        lbDescargando = findViewById(R.id.lbDescargando)
        progressBar = findViewById(R.id.downloadProgress)
        video_fondo = findViewById(R.id.video_fondo)
        bg_download = findViewById(R.id.bg_download)
        val folder = File(getExternalFilesDir(null), "FingerDance")
        if (folder.exists()) {
            creaMain()
        } else {
            creaDescarga()
        }
    }

    private fun creaDescarga() {
        linearDownload.isVisible = true
        bg_download.setVideoURI(Uri.parse("android.resource://${packageName}/${R.raw.bg_download}"))
        bg_download.start()
        bg_download.setOnCompletionListener {
            bg_download.start()
        }

        val builder = AlertDialog.Builder(this, R.style.TransparentDialog)
        builder.setTitle("Aviso")
        builder.setMessage("Se descargarán los recursos de la Aplicación. Se recomienda usar una conexión Wi-Fi")
        builder.setCancelable(false)
        builder.setPositiveButton("Aceptar") { dialog, which ->
            when {
                !isUsingWifi(this) && !isUsingMobileData(this) -> {
                    mostrarDialogoSinConexion()
                }
                isUsingWifi(this) -> {
                    iniciarDescarga()
                }
                isUsingMobileData(this) -> {
                    mostrarDialogoDatosMoviles()
                }
            }
        }
        builder.setNegativeButton("Cerrar") { dialog, which ->
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            exitProcess(0)
        }
        builder.show()
    }

    private fun mostrarDialogoSinConexion() {
        val noWifi = AlertDialog.Builder(this, R.style.TransparentDialog)
        noWifi.setMessage("No hay conexión a Internet. Reintentar?")
        noWifi.setPositiveButton("Reintentar") { dialog, which ->
            creaDescarga()
        }
        noWifi.setNegativeButton("Cerrar") { dialog, which ->
            cerrarApp()
        }
        noWifi.show()
    }

    private fun mostrarDialogoDatosMoviles() {
        val datosMoviles = AlertDialog.Builder(this, R.style.TransparentDialog)
        datosMoviles.setMessage("Está utilizando datos móviles. ¿Desea continuar?")
        datosMoviles.setPositiveButton("Aceptar") { dialog, which ->
            iniciarDescarga()
        }
        datosMoviles.setNegativeButton("Cancelar", null)
        datosMoviles.show()
    }

    private fun iniciarDescarga() {
        descargando = false
        linearDownload.setOnClickListener {

        }
        imageIcon.isVisible = true
        lbDescargando.isVisible = true
        progressBar.isVisible = true

        val storage = FirebaseStorage.getInstance()
        val storageReference = storage.reference
        val storageRef = storageReference.child("FingerDance.zip")

        val localFile = File(getExternalFilesDir(null), "FingerDance.zip")

        val fallo = AlertDialog.Builder(this)
        fallo.setMessage("Ocurrio un error durante la descarga, favor de reintentar")

        storageRef.getFile(localFile).addOnSuccessListener {
            val unzip = Unzip(this)
            val rutaZip = Environment.getExternalStorageDirectory().toString() + "/Android/data/com.fingerdance/files/FingerDance.zip"
            unzip.performUnzip(rutaZip, "FingerDance.zip")
        }.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            progressBar.progress = progress
            lbDescargando.text = "Descargando $progress%"

            if (progress == 100) {
                lbDescargando.text = "Descarga finalizada, espere por favor..."
            }
        }.addOnFailureListener {
            fallo.show()
        }
    }

    private fun cerrarApp() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        exitProcess(0)
    }

    fun creaMain() {
        val databaseReference = FirebaseDatabase.getInstance().getReference("version")
        var version = ""
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                version = snapshot.child("value").getValue(String::class.java).toString()
                showAddActive = snapshot.child("showAdd").getValue(Boolean::class.java) ?: false
                numberUpdate = snapshot.child("numberUpdate").getValue(String::class.java).toString()

                if(version == "1.1.2"){
                    linearDownload.isVisible = false
                    imageIcon.isVisible = false
                    lbDescargando.isVisible = false
                    progressBar.isVisible = false
                    val them = File(getExternalFilesDir(null), "FingerDance/Themes/$tema")
                    if (!them.exists()) {
                        tema ="default"
                    }

                    btnPlay = findViewById(R.id.btnPlay)
                    btnPlay.foreground = Drawable.createFromPath(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/play.png").toString())
                    btnOptions = findViewById(R.id.btnOptions)
                    btnOptions.foreground = Drawable.createFromPath(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/options.png").toString())
                    btnExit = findViewById(R.id.btnExit)
                    btnExit.foreground = Drawable.createFromPath(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/exit.png").toString())

                    val sound = MediaPlayer.create(this@MainActivity, Uri.fromFile(File(getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/screen_title_music.ogg").toString())))
                    soundPlayer = sound
                    soundPlayer!!.isLooping = true
                    soundPlayer!!.start()

                    animLogo = findViewById(R.id.imgLogo)
                    val bmLogo = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/logo.png").toString())
                    animLogo.setImageBitmap(bmLogo)
                    bgaOff = this@MainActivity.getExternalFilesDir("/FingerDance/Themes/$tema/Movies/BGA_OFF.mp4").toString()
                    video_fondo.setVideoPath(getExternalFilesDir("/FingerDance/Themes/$tema/BGAs/fondo.mp4").toString())

                    video_fondo.start()
                    video_fondo.setOnPreparedListener{ mp ->
                        mediaPlayerMain = mp
                        mediaPlayerMain.isLooping = true}
                    if(currentVideoPosition != 0){
                        mediaPlayerMain.seekTo(currentVideoPosition)
                        mediaPlayerMain.start()
                    }
                    animar()
                    val goSound = MediaPlayer.create(this@MainActivity, Uri.fromFile(File(getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/hitme.mp3").toString())))
                    val animation = AnimationUtils.loadAnimation(this@MainActivity, R.anim.press_button)
                    btnPlay.setOnClickListener {
                        btnPlay.isEnabled=false
                        val ls = LoadSongsKsf()
                        goSound.start()
                        btnPlay.startAnimation(animation)
                        if(themes.getString("allTunes", "").toString() != ""){
                            val jsonListChannels = themes.getString("allTunes", "")
                            listChannels = gson.fromJson(jsonListChannels, object : TypeToken<ArrayList<Channels>>() {}.type)
                        }else{
                            listCommands = ls.getFilesCW(this@MainActivity)
                            val ordenEspecifico = listOf("-.05", "-.1", "-.5", "-1", "0", "1", ".5", ".1", ".05")
                            val ordenMap = ordenEspecifico.withIndex().associate { it.value to it.index }
                            listCommands.find { it.descripcion == "Cambiar la velocidad de la nota." }!!.listCommandValues.sortBy { ordenMap[it.value] ?: Int.MAX_VALUE }

                            listChannels = ls.getChannels(this@MainActivity)
                            themes.edit().putString("allTunes", gson.toJson(listChannels)).apply()
                            themes.edit().putString("efects", gson.toJson(listCommands)).apply()

                        }
                        if(themes.getString("efects", "").toString() == ""){
                            listCommands = ls.getFilesCW(this@MainActivity)
                            val ordenEspecifico = listOf("-.05", "-.1", "-.5", "-1", "0", "1", ".5", ".1", ".05")
                            val ordenMap = ordenEspecifico.withIndex().associate { it.value to it.index }
                            listCommands.find { it.descripcion == "Cambiar la velocidad de la nota." }!!.listCommandValues.sortBy { ordenMap[it.value] ?: Int.MAX_VALUE }
                            themes.edit().putString("efects", gson.toJson(listCommands)).apply()
                        }else{
                            val jsonListCommands = themes.getString("efects", "")
                            listCommands = gson.fromJson(jsonListCommands, object : TypeToken<ArrayList<Command>>() {}.type)
                            //themes.edit().putString("efects", gson.toJson(listCommands)).apply()
                        }
                        ls.loadSounds(this@MainActivity)
                        val intent = Intent(applicationContext, SelectChannel::class.java)
                        startActivity(intent)
                        mediaPlayerMain.pause()
                        soundPlayer!!.pause()
                        btnPlay.isEnabled = true
                    }
                    val goOption = MediaPlayer.create(this@MainActivity, Uri.fromFile(File(getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/option_sound.mp3").toString())))
                    btnOptions.setOnClickListener {
                        //btnOptions.isEnabled = true
                        Toast.makeText(this@MainActivity, "Cargando...", Toast.LENGTH_SHORT).show()
                        btnOptions.startAnimation(animation)
                        goOption.start()
                        soundPlayer!!.pause()

                        val tiempoTranscurrir :Long = 500
                        val handler = Handler()
                        handler.postDelayed({
                            val intent = Intent(applicationContext, Options()::class.java)
                            startActivity(intent)
                            this@MainActivity.finish()
                        }, tiempoTranscurrir)
                    }

                    val builder = AlertDialog.Builder(this@MainActivity)

                    btnExit.setOnClickListener {
                        builder.setTitle("Aviso")
                        builder.setMessage("Deseas salir del juego?")
                        builder.setPositiveButton(android.R.string.yes) { dialog, which ->
                            val intent = Intent(Intent.ACTION_MAIN)
                            intent.addCategory(Intent.CATEGORY_HOME)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            exitProcess(0)
                        }
                        builder.setNegativeButton(android.R.string.no) { dialog, which ->

                        }
                        builder.show()
                    }
                }else{
                    Toast.makeText(this@MainActivity, "Se requiere actualizar la aplicacion, descarga la ultima version", Toast.LENGTH_LONG).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                println("Error al leer la versión: ${error.message}")
            }
        })
    }

    fun isUsingWifi(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI
        }
    }

    fun isUsingMobileData(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.type == ConnectivityManager.TYPE_MOBILE
        }
    }

    override fun onPause() {
        super.onPause()
        if(::mediaPlayerMain.isInitialized) {
            if (mediaPlayerMain.isPlaying) {
                currentVideoPosition = mediaPlayerMain.currentPosition
                video_fondo.pause()
                mediaPlayerMain.pause()
                soundPlayer!!.pause()
            }
        }
        if(linearDownload.isVisible){
            if(bg_download.isPlaying){
                bg_download.pause()
            }
        }
    }
    override fun onResume() {
        super.onResume()
        if(::mediaPlayerMain.isInitialized){
            video_fondo.start()
            soundPlayer!!.start()
        }
        if(linearDownload.isVisible){
            if(!bg_download.isPlaying){
                bg_download.start()
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayerMain.release()
        soundPlayer!!.release()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        btnExit.performClick()
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

    private fun animar() {
        val animation = AnimationUtils.loadAnimation(this, R.anim.scale)
        animLogo.startAnimation(animation)

    }
}

class ObjPuntaje(var puntaje: String = "", var grade: String = "", var offset: String = "0")