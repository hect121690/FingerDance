package com.fingerdance

//import com.google.firebase.auth.FirebaseAuth

import android.content.*
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.Serializable
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
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
var valueOffset = 0L

var showAddActive = false

val gson = Gson()

var numberUpdate = ""
var versionUpdate = ""

var showPadB : Int = 0
var hideImagesPadA : Boolean = false
var skinPad = ""
var alphaPadB = 1f
//var listNoteSkinAdditionals = ArrayList<CommandValues>()

//var countSongsPlayed = 0

/*
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
*/

val API_KEY = "AIzaSyCL1ukVSzaKtIZZo3PFqfHXdlWIAxD1hGM"
private val FOLDER_ID = "19cM-WcAJyzo7w-7sbrPUzufMu_-gi9bS"
var listFilesDrive = arrayListOf<Pair<String, String>>()

var userName = ""
val firebaseDatabase = FirebaseDatabase.getInstance()
var listGlobalRanking = arrayListOf<Cancion>()

var freeDevices = arrayListOf<String>()

var deviceIdFind = ""

var idSala = ""

class MainActivity : AppCompatActivity(), Serializable {
    private lateinit var video_fondo : VideoView
    private lateinit var bg_download : VideoView
    private lateinit var mediaPlayerMain : MediaPlayer
    private var soundPlayer : MediaPlayer? = null
    private var currentVideoPosition : Int = 0
    private lateinit var animLogo : ImageView
    private lateinit var btnPlay : Button
    private lateinit var btnPlayOnline : Button
    private lateinit var btnOptions : Button
    private lateinit var btnExit : Button

    private lateinit var linearDownload : ConstraintLayout
    private lateinit var imageIcon : ImageView
    private lateinit var lbDescargando : TextView
    private lateinit var progressBar : ProgressBar
    private var versionApp = ""

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
        versionUpdate = themes.getString("versionUpdate", "0.0.0").toString()
        valueOffset = themes.getLong("valueOffset", 0)
        userName = themes.getString("userName","").toString()

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        width = metrics.widthPixels
        height = metrics.heightPixels

        if(tema == ""){
            tema ="default"
        }

        //themes.edit().putString("allTunes", "").apply()
        //themes.edit().putString("efects", "").apply()
        //themes.edit().putString("userName", "").apply()

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
        CoroutineScope(Dispatchers.Main).launch {
            val downloadedFile = iniciarDescargaDrive("1WZ3rL20JGEKcPtoQi0dHrZ8qs8z8-7kI", "zip") { progress ->
                runOnUiThread {
                    descargando = false
                    linearDownload.setOnClickListener {

                    }
                    imageIcon.isVisible = true
                    lbDescargando.isVisible = true
                    progressBar.isVisible = true

                    progressBar.progress = progress
                    lbDescargando.text = "Descargando $progress%"

                    if (progress == 100) {
                        lbDescargando.text = "Descarga finalizada, espere por favor..."
                    }
                }
            }

            if (downloadedFile != null) {
                val unzip = Unzip(this@MainActivity)
                val rutaZip = Environment.getExternalStorageDirectory().toString() + "/Android/data/com.fingerdance/files/FingerDance.zip"
                unzip.performUnzip(rutaZip, "FingerDance.zip")
            } else {
                Toast.makeText(this@MainActivity, "Error en la descarga", Toast.LENGTH_LONG).show()
            }

        }
    }

    private suspend fun iniciarDescargaDrive(idDonwnload: String, typeFile: String, progressCallback: (Int) -> Unit): File? {
        descargando = false
        linearDownload.setOnClickListener {

        }
        imageIcon.isVisible = true
        lbDescargando.isVisible = true
        progressBar.isVisible = true
        val fallo = AlertDialog.Builder(this)
        fallo.setMessage("Ocurrio un error durante la descarga, favor de reintentar")
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://www.googleapis.com/drive/v3/files/$idDonwnload?alt=media&key=$API_KEY"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                if (connection.responseCode == 200) {
                    val localFile = File(getExternalFilesDir(null), "FingerDance.$typeFile")

                    val inputStream = connection.inputStream
                    val outputStream = FileOutputStream(localFile)
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    var totalBytes = 0
                    val totalSize = connection.contentLength

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                        val progress = (100.0 * totalBytes / totalSize).toInt()
                        progressCallback(progress)
                    }

                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()
                    connection.disconnect()

                    return@withContext localFile
                } else {
                    //println("Error en la descarga: Código ${connection.responseCode}")
                    fallo.show()
                    return@withContext null
                }
            } catch (e: Exception) {
                //println("Error descargando archivo: ${e.message}")
                fallo.show()
                return@withContext null
            }
        }

    }

    private fun instalarAPK(filePath: String) {
        val file = File(filePath)
        val uri: Uri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }


    private fun cerrarApp() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        exitProcess(0)
    }


    private fun creaMain() {
        val databaseReference = firebaseDatabase.getReference("version")
        var version = ""
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                version = snapshot.child("value").getValue(String::class.java).toString()
                showAddActive = snapshot.child("showAdd").getValue(Boolean::class.java) ?: false
                numberUpdate = snapshot.child("numberUpdate").getValue(String::class.java).toString()
                val startOnline = snapshot.child("startOnline").getValue(Boolean::class.java) ?: false

                CoroutineScope(Dispatchers.Main).launch {
                    getFilesDrive()
                    listFilesDrive.sortBy { it.first }
                }

                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                versionApp = packageInfo.versionName

                if(version == versionApp){
                    linearDownload.isVisible = false
                    imageIcon.isVisible = false
                    lbDescargando.isVisible = false
                    progressBar.isVisible = false
                    val them = File(getExternalFilesDir(null), "FingerDance/Themes/$tema")
                    if (!them.exists()) {
                        tema ="default"
                    }

                    val linearButtons = findViewById<LinearLayout>(R.id.linearButtons)
                    linearButtons.orientation = LinearLayout.VERTICAL // Alineación vertical
                    linearButtons.gravity = Gravity.CENTER // Centra los elementos horizontalmente
                    linearButtons.layoutParams = LinearLayout.LayoutParams(
                        width,
                        height / 2
                    )
                    linearButtons.y = height / 4f

                    btnPlay = findViewById(R.id.btnPlay)
                    btnPlay.foreground = Drawable.createFromPath(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/play.png").toString())
                    btnPlayOnline = findViewById(R.id.btnPlayOnline)
                    btnPlayOnline.foreground = Drawable.createFromPath(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/play_online.png").toString())

                    if(!startOnline){
                        btnPlayOnline.visibility = View.GONE
                    }

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
                        val intent = Intent(this@MainActivity, SelectChannel::class.java)
                        startActivity(intent)
                        mediaPlayerMain.pause()
                        soundPlayer!!.pause()
                        btnPlay.isEnabled = true
                    }

                    btnPlayOnline.setOnClickListener{
                        btnPlayOnline.isEnabled=false
                        goSound.start()
                        btnPlayOnline.startAnimation(animation)
                        val btnParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        btnParams.setMargins(16, 16, 16, 16)
                        val btnCreateRoom = Button(this@MainActivity).apply {
                            text = "Crear Sala"
                            setPadding(20, 10, 20, 10)
                            background = ContextCompat.getDrawable(context, R.drawable.button_online)
                            setTextColor(Color.WHITE)
                        }
                        val btnJoinRoom = Button(this@MainActivity).apply {
                            text = "Unirme a Sala"
                            setPadding(20, 10, 20, 10)
                            background = ContextCompat.getDrawable(context, R.drawable.button_online)
                            setTextColor(Color.WHITE)
                        }

                        val btnGetRoom = Button(this@MainActivity).apply {
                            text = "Entrar"
                            setPadding(20, 10, 20, 10)
                            background = ContextCompat.getDrawable(context, R.drawable.button_pink)
                            setTextColor(Color.WHITE)
                        }

                        val editTextRoom = EditText(this@MainActivity).apply {
                            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                            hint = "Ingresar clave"
                        }

                        val linearOnline = LinearLayout(this@MainActivity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                        }

                        btnCreateRoom.layoutParams = btnParams
                        btnJoinRoom.layoutParams = btnParams

                        linearOnline.addView(btnCreateRoom)
                        linearOnline.addView(btnJoinRoom)

                        val linearClave = LinearLayout(this@MainActivity)
                        linearClave.orientation = LinearLayout.VERTICAL // Alineación vertical
                        linearClave.gravity = Gravity.CENTER // Centra los elementos horizontalmente
                        linearClave.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )

                        linearClave.addView(editTextRoom)
                        linearClave.addView(btnGetRoom)
                        linearClave.visibility = View.GONE

                        val linearLayouts = LinearLayout(this@MainActivity)
                        linearLayouts.orientation = LinearLayout.VERTICAL // Alineación vertical
                        linearLayouts.gravity = Gravity.CENTER // Centra los elementos horizontalmente
                        linearLayouts.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        linearLayouts.addView(linearOnline)
                        linearLayouts.addView(linearClave)

                        val dialog = AlertDialog.Builder(this@MainActivity)
                            .setTitle("Modo Online 1 vs 1")
                            .setMessage("Elige una opción:")
                            .setView(linearLayouts)
                            .setCancelable(false)
                            .setNegativeButton("Cancelar") { d, _ ->
                                d.dismiss()
                                btnPlayOnline.isEnabled = true
                            }
                            .show()

                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                            textSize = 18f
                            setTextColor(Color.RED)
                            val layoutParams = this.layoutParams as LinearLayout.LayoutParams
                            layoutParams.gravity = Gravity.CENTER
                            layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
                            this.layoutParams = layoutParams
                        }

                        btnJoinRoom.setOnClickListener {
                            dialog.setMessage("Ingresa la clave para entrar a la sala")
                            linearOnline.visibility = View.GONE
                            linearClave.visibility = View.VISIBLE
                        }

                        btnCreateRoom.setOnClickListener {
                            idSala = UUID.randomUUID().toString()
                            val salaRef = firebaseDatabase.getReference("rooms/$idSala")
                            val jugador1 = Jugador(id = userName)
                            val formato = SimpleDateFormat("dd-MM-yyyy-HH-mm", Locale.getDefault())

                            val nuevaSala = Sala(turno = "jugador1", jugador1 = jugador1, date = formato.format(Date()))

                            salaRef.setValue(nuevaSala).addOnSuccessListener {
                                mostrarCodigoSala(dialog)
                            }
                        }

                        btnGetRoom.setOnClickListener {
                            if(editTextRoom.text.toString() != ""){
                                val ls = LoadSongsKsf()
                                val salaRef = firebaseDatabase.getReference("rooms/${editTextRoom.text}")
                                salaRef.child("jugador2/id").setValue(userName)
                                listChannelsOnline = ls.getChannelsOnline(this@MainActivity)
                                if(themes.getString("efects", "").toString() == ""){
                                    listCommands = ls.getFilesCW(this@MainActivity)
                                    val ordenEspecifico = listOf("-.05", "-.1", "-.5", "-1", "0", "1", ".5", ".1", ".05")
                                    val ordenMap = ordenEspecifico.withIndex().associate { it.value to it.index }
                                    listCommands.find { it.descripcion == "Cambiar la velocidad de la nota." }!!.listCommandValues.sortBy { ordenMap[it.value] ?: Int.MAX_VALUE }
                                    themes.edit().putString("efects", gson.toJson(listCommands)).apply()
                                }else{
                                    val jsonListCommands = themes.getString("efects", "")
                                    listCommands = gson.fromJson(jsonListCommands, object : TypeToken<ArrayList<Command>>() {}.type)
                                }
                                ls.loadSounds(this@MainActivity)
                                val intent = Intent(this@MainActivity, SelectChannelOnline::class.java)
                                startActivity(intent)
                                mediaPlayerMain.pause()
                                soundPlayer!!.pause()
                                btnPlayOnline.isEnabled = true
                            }else{
                                Toast.makeText(this@MainActivity, "Debe ingresa la clave de la sala", Toast.LENGTH_SHORT).show()
                            }
                        }

                    }

                    val goOption = MediaPlayer.create(this@MainActivity, Uri.fromFile(File(getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/option_sound.mp3").toString())))

                    btnOptions.setOnClickListener {
                        //btnOptions.isEnabled = true
                        Toast.makeText(this@MainActivity, "Cargando...", Toast.LENGTH_SHORT).show()
                        btnOptions.startAnimation(animation)
                        goOption.start()
                        soundPlayer!!.pause()

                        val tiempoTranscurrir :Long = 1000
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

                    deviceIdFind = getDeviceId(this@MainActivity)
                    btnExit.setOnLongClickListener {
                        val txDeviceId = TextView(this@MainActivity).apply {
                            setTextColor(Color.BLACK)
                            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                            setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
                            text = deviceIdFind + "-$userName"
                        }

                        val dialog = AlertDialog.Builder(this@MainActivity)
                            .setTitle("ID COMPRA")
                            .setMessage("Por favor envia esta clave al desarrollador")
                            .setView(txDeviceId)
                            .setCancelable(false)
                            .setPositiveButton("Copiar") { _, _ ->
                                val clipboard: ClipboardManager = this@MainActivity.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("", txDeviceId.text.toString())
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(this@MainActivity, "Texto copiado al portapapeles!", Toast.LENGTH_LONG).show()
                            }
                            .create()
                        dialog.show()
                        true
                    }

                    if(userName == ""){
                        ingresaNameUser()
                    }else{
                        Toast.makeText(this@MainActivity, "Bienvenido $userName", Toast.LENGTH_SHORT).show()
                    }

                    getFreeDevices { toListFreeDevices ->
                        freeDevices = toListFreeDevices
                    }

                }else{
                    val lbDescargando = TextView(this@MainActivity).apply {
                        id = View.generateViewId()
                        text = "Descargando:"
                        textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                        setTextColor(Color.BLACK)
                        visibility = View.GONE
                    }
                    lbDescargando.textSize = 16f

                    val progressBar = ProgressBar(this@MainActivity, null, android.R.attr.progressBarStyleHorizontal).apply {
                        id = View.generateViewId()
                        layoutParams = ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.MATCH_PARENT,
                            25.dpToPx()
                        )
                        visibility = View.GONE
                    }
                    val btnAceptarDownload = Button(this@MainActivity).apply {
                        text = "Descargar"
                        setBackgroundColor(Color.BLUE)
                        setTextColor(Color.WHITE)
                    }

                    val linearDowload = LinearLayout(this@MainActivity)
                    linearDowload.orientation = LinearLayout.VERTICAL // Alineación vertical
                    linearDowload.gravity = Gravity.CENTER // Centra los elementos horizontalmente
                    linearDowload.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )

                    linearDowload.addView(lbDescargando)
                    linearDowload.addView(progressBar)
                    linearDowload.addView(btnAceptarDownload)

                    val dialog = AlertDialog.Builder(this@MainActivity)
                        .setTitle("Actualizar")
                        .setMessage("Se requiere actualizar la aplicacion, descargar?")
                        .setView(linearDowload)
                        .setCancelable(false)
                        .show()

                    btnAceptarDownload.setOnClickListener {
                        lbDescargando.visibility = View.VISIBLE
                        progressBar.visibility = View.VISIBLE
                        btnAceptarDownload.visibility = View.GONE
                        dialog.setMessage("Espere por favor")
                        CoroutineScope(Dispatchers.Main).launch {
                            val packageApp = iniciarDescargaDrive("199Y0lsIdAHmLdRWb3ZQJmUPLMl8GjqYM", "apk") { progress ->
                                runOnUiThread {
                                    progressBar.progress = progress
                                    lbDescargando.text = "Descargando $progress%"
                                    if (progress == 100) {
                                        lbDescargando.text = "Descarga finalizada, espere por favor..."
                                        dialog.dismiss()
                                    }
                                }
                            }
                            if (packageApp != null) {
                                instalarAPK(File(getExternalFilesDir(null), "FingerDance.apk").absolutePath)
                                //versionApp = version
                            }
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                println("Error al leer la versión: ${error.message}")
            }
        })
    }

    private fun mostrarCodigoSala(alertDialog: AlertDialog) {
        val ls = LoadSongsKsf()
        val txSalaId = TextView(this@MainActivity).apply {
            setTextColor(Color.BLACK)
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
            text = idSala
        }

        val dialog = AlertDialog.Builder(this@MainActivity)
            .setTitle("Crear sala")
            .setMessage("Envia esta clave al jugador que quieras invitar a la sala")
            .setView(txSalaId)
            .setCancelable(false)
            .setPositiveButton("Compartir") { d, _ ->
                alertDialog.dismiss()
                d.dismiss()

                listChannelsOnline = ls.getChannelsOnline(this@MainActivity)
                if(themes.getString("efects", "").toString() == ""){
                    listCommands = ls.getFilesCW(this@MainActivity)
                    val ordenEspecifico = listOf("-.05", "-.1", "-.5", "-1", "0", "1", ".5", ".1", ".05")
                    val ordenMap = ordenEspecifico.withIndex().associate { it.value to it.index }
                    listCommands.find { it.descripcion == "Cambiar la velocidad de la nota." }!!.listCommandValues.sortBy { ordenMap[it.value] ?: Int.MAX_VALUE }
                    themes.edit().putString("efects", gson.toJson(listCommands)).apply()
                }else{
                    val jsonListCommands = themes.getString("efects", "")
                    listCommands = gson.fromJson(jsonListCommands, object : TypeToken<ArrayList<Command>>() {}.type)
                }
                ls.loadSounds(this@MainActivity)
                val intent = Intent(this@MainActivity, SelectChannelOnline::class.java)
                startActivity(intent)
                mediaPlayerMain.pause()
                soundPlayer!!.pause()
                btnPlayOnline.isEnabled = true
                mostrarDialogoCompartir(this)
            }
            .setNegativeButton("Cancelar"){_, _ ->
                firebaseDatabase.getReference("rooms/$idSala").removeValue()
            }
            .create()
        dialog.show()
    }

    private fun mostrarDialogoCompartir(context: Context) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, idSala)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir con"))
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun getFreeDevices(callback: (ArrayList<String>) -> Unit) {
        val databaseRef = firebaseDatabase.getReference("freeDevices")
        val listResult = arrayListOf<String>()
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (devices in snapshot.children) {
                    listResult.add(devices.value.toString())
                }
                callback(listResult)
                return
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al leer datos", error.toException())
            }
        })
    }


    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun ingresaNameUser(){
        val editTextName = EditText(this).apply {
            hint = "Nombre de perfil"
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        }
        val dialogError = AlertDialog.Builder(this)
            .setTitle("Aviso!")
            .setMessage("Tu nombre debe tener al menos 8 caracteres, una letra mayuscula y al menos 1 número. \n  \n Por favor intentalo de nuevo")
            .setCancelable(false)
            .setPositiveButton("Aceptar") { d, _ ->
                d.dismiss()
                ingresaNameUser()
            }
            .create()

        val dialog = AlertDialog.Builder(this)
            .setTitle("Registrate")
            .setMessage("Por favor ingresa el nombre con el que te identeificaras en Finger Dance")
            .setView(editTextName)
            .setCancelable(false)
            .setPositiveButton("Aceptar") { _, _ ->
                if(validarNombre(editTextName.text.toString())){
                    themes.edit().putString("userName", editTextName.text.toString()).apply()
                    userName = editTextName.text.toString()
                    Toast.makeText(this, "Nombre registrado con exito", Toast.LENGTH_SHORT).show()
                }else{
                    dialogError.show()
                }
            }
            .create()
        dialog.show()
    }

    private fun validarNombre(nombre: String): Boolean {
        val regex = Regex("^(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d]{8,}$")
        return regex.matches(nombre)
    }


    suspend fun getFilesDrive() {
        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = URLEncoder.encode("'$FOLDER_ID' in parents", "UTF-8")
                val url = "https://www.googleapis.com/drive/v3/files?q=$encodedQuery&key=$API_KEY"

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()

                    val jsonResponse = JSONObject(response)
                    val files = jsonResponse.getJSONArray("files")
                    listFilesDrive.clear()
                    for (i in 0 until files.length()) {
                        val file = files.getJSONObject(i)
                        val fileName = file.getString("name")
                        val fileId = file.getString("id")
                        listFilesDrive.add(Pair(fileName, fileId))
                    }
                } else {
                    Log.d("Drive Files","Error en la petición: Código $responseCode")
                }
            } catch (e: Exception) {
                Log.d("Drive Files","Error: ${e.message}")
            }
        }
    }


    private fun isUsingWifi(context: Context): Boolean {
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

    private fun isUsingMobileData(context: Context): Boolean {
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

class ObjPuntaje(var puntaje: String = "", var grade: String = "")

data class Resultado(
    val bad: String = "",
    val good: String = "",
    val grade: String = "",
    val great: String = "",
    val maxCombo: String = "",
    val miss: String = "",
    val perfect: String = "",
    val score: String = ""
)

data class Jugador(
    val id: String = "",
    val listo: Boolean = false,
    val result: Resultado = Resultado(),
    val winner: Boolean = false
)

data class Sala(
    val cancion: String = "",
    val jugador1: Jugador = Jugador(),
    val jugador2: Jugador = Jugador(),
    val nivel: String = "",
    val turno: String = "",
    val date: String = ""
)
