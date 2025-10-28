package com.fingerdance

import android.animation.AnimatorInflater
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Bitmap
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
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.Serializable
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.UUID
import kotlin.system.exitProcess
import androidx.core.graphics.toColorInt
import androidx.core.view.children
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import java.time.Year
import java.time.temporal.ChronoUnit
import kotlin.collections.listOf

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
var valueOffset = 0L

val gson = Gson()

var numberUpdate = ""
var versionUpdate = ""
var flagActiveAllows = false

var showPadB : Int = 0
var hideImagesPadA : Boolean = false
var skinPad = ""
var alphaPadB = 1f

val API_KEY = "AIzaSyCL1ukVSzaKtIZZo3PFqfHXdlWIAxD1hGM"
private val FOLDER_ID = "19cM-WcAJyzo7w-7sbrPUzufMu_-gi9bS"
private val FOLDER_ID_THEMES = "1mqNKLVyhcQ8I7rXOD4z9Sg1Glrv8YVDX"
var listFilesDrive = arrayListOf<Pair<String, String>>()
var listThemesDrive = arrayListOf<Pair<String, String>>()

var userName = ""
var firebaseDatabase : FirebaseDatabase? = null
var listGlobalRanking = arrayListOf<Cancion>()

var listAllowDevices = arrayListOf<String>()

var deviceIdFind = ""

var idSala = ""

var medidaFlechas = 0f

var heightLayoutBtns = 0f
var heightBtns  = 0f
var widthBtns = 0f
var padPositions = listOf<Array<Float>>()
var padPositionsHD = listOf<Array<Float>>()
var touchAreas = listOf<Array<Float>>()

var colWidth = 0f

lateinit var bitmapNumber : Bitmap
lateinit var bitmapNumberMiss : Bitmap

lateinit var numberBitmaps: List<Bitmap>
lateinit var numberBitmapsMiss: List<Bitmap>

lateinit var mediPlayer : MediaPlayer
lateinit var playerSong: PlayerSong
var positionActualLvs: Int = 0
var displayBPM = 0f
var sizeLvs = 0

var isPlayer1 = true
var isOnline = false

lateinit var mediaPlayer : MediaPlayer
var ruta = ""
var ksf = KsfProccess()
var ksfHD = KsfProccessHD()

lateinit var salaRef: DatabaseReference
lateinit var activeSala : Sala

lateinit var db : DataBasePlayer

var decimoHeigtn = 0
var decimoWidth = 0
//var isFree = false
var isOffline = false
var isMidLine = false
var isCounter = false
var breakSong = true
var listEfectsDisplay: ArrayList<CommandValues> = ArrayList()

var bgaPathSelectChannel = ""
var bgaPathSelectSong = ""

private val ls = LoadSongsKsf()
lateinit var arrayGrades : ArrayList<Bitmap>
var TIME_ADJUST = 0L
var timeToPresiscionHD = 0

lateinit var validFolders : List<String>

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
    private lateinit var loadingLayout: LinearLayout

    private lateinit var linearDownload : ConstraintLayout
    private lateinit var lbDescargando : TextView
    private lateinit var progressBar : ProgressBar
    private var versionApp = ""
    private var idWithRegister = ""
    private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_main)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        firebaseDatabase = FirebaseDatabase.getInstance()

        db = DataBasePlayer(this)
        themes = getPreferences(Context.MODE_PRIVATE)
        tema = themes.getString("theme", "default").toString()
        skinSelected = themes.getString("skin", "").toString()
        speedSelected = themes.getString("speed", "").toString()
        showPadB = themes.getInt("showPadB", 0)
        hideImagesPadA = themes.getBoolean("hideImagesPadA", false)
        skinPad = themes.getString("skinPad", "default").toString()
        alphaPadB = themes.getFloat("alphaPadB", 1f)
        versionUpdate = themes.getString("versionUpdate", "0.0.0").toString()
        valueOffset = themes.getLong("valueOffset", 0)
        userName = themes.getString("userName","").toString()
        //isFree = themes.getBoolean("isFree",false)
        isMidLine = themes.getBoolean("isMidLine",false)
        isCounter = themes.getBoolean("isCounter",false)
        breakSong = themes.getBoolean("breakSong",true)
        idWithRegister = themes.getString("idWithRegister", "").toString()

        //isFree = false

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        width = metrics.widthPixels
        height = metrics.heightPixels

        decimoWidth = width / 10
        decimoHeigtn = height / 10

        if(tema == ""){
            tema ="default"
        }

        deviceIdFind = getDeviceId(this@MainActivity)

        validFolders = listOf(
            "000-Finger Dance",                     //0
            "03-SHORT CUT - V2",                    //1
            "04-REMIX - V2",                        //2
            "05-FULLSONGS - V2",                    //3
            "10-PIU 1st TO PERFECT COLLECTION",     //4
            "11-EXTRA TO PREX 3",                   //5
            "12-EXCEED TO ZERO",                    //6
            "13-NX TO NXA",                         //7
            "14-FIESTA TO FIESTA 2 - V2",           //8
            "15 - INFINITY",                        //9
            "17-PRIME",                             //10
            "18-PRIME 2",                           //11
            "19-XX ANIVERSARY",                     //12
            "20-PHOENIX",                           //13
            "21-RISE"                               //14
        )

        medidaFlechas = (width / 7f)

        heightLayoutBtns = height / 2f
        heightBtns = heightLayoutBtns / 2f
        widthBtns = width / 3f

        padPositions = listOf(
            arrayOf(0f, (heightLayoutBtns + heightBtns)),
            arrayOf(0f, heightBtns * 2f),
            arrayOf(widthBtns, heightLayoutBtns + heightLayoutBtns / 4f),
            arrayOf(widthBtns * 2f, heightBtns * 2f),
            arrayOf(widthBtns * 2f, heightLayoutBtns + heightBtns)
        )

        touchAreas = listOf(
            arrayOf(widthBtns, heightLayoutBtns + heightBtns + (heightBtns / 2)), //leffDown
            arrayOf(widthBtns + (widthBtns / 2), heightLayoutBtns + heightBtns + (heightBtns / 2)),  //rightDown
            arrayOf(widthBtns, heightLayoutBtns), //leftUp
            arrayOf(widthBtns + (widthBtns / 2), heightLayoutBtns) //rightUp
        )

        //HalfDouble
        colWidth = width / 4f
        padPositionsHD = listOf(
            arrayOf(0f,0f),
            arrayOf(0f,0f),
            arrayOf(0f, heightLayoutBtns + heightLayoutBtns / 4f),
            arrayOf(colWidth, heightBtns * 2f),
            arrayOf(colWidth, heightLayoutBtns + heightBtns),
            arrayOf(colWidth * 2f, (heightLayoutBtns + heightBtns)),
            arrayOf(colWidth * 2f, heightBtns * 2f),
            arrayOf(colWidth * 3f, heightLayoutBtns + heightLayoutBtns / 4f),
            arrayOf(0f,0f),
            arrayOf(0f,0f)
        )

        //themes.edit().putString("allTunes", "").apply()
        //themes.edit().putString("efects", "").apply()
        //themes.edit().putString("userName", "").apply()

        linearDownload = findViewById(R.id.linearDownload)

        if(descargando){
            linearDownload.setOnClickListener {
                creaDescarga()
            }
        }

        btnPlay = findViewById(R.id.btnPlay)
        btnPlayOnline = findViewById(R.id.btnPlayOnline)
        btnOptions = findViewById(R.id.btnOptions)
        btnExit = findViewById(R.id.btnExit)

        btnPlay.layoutParams.height = (decimoHeigtn * 1.2).toInt()
        btnPlayOnline.layoutParams.height = (decimoHeigtn * 1.2).toInt()
        btnOptions.layoutParams.height = (decimoHeigtn * 1.2).toInt()
        btnExit.layoutParams.height = (decimoHeigtn * 1.2).toInt()

        btnPlay.layoutParams.width = decimoWidth * 6
        btnPlayOnline.layoutParams.width = decimoWidth * 6
        btnOptions.layoutParams.width = decimoWidth * 6
        btnExit.layoutParams.width = decimoWidth * 6

        lbDescargando = findViewById(R.id.lbDescargando)
        progressBar = findViewById(R.id.downloadProgress)
        video_fondo = findViewById(R.id.video_fondo)
        bg_download = findViewById(R.id.bg_download)
        loadingLayout = findViewById(R.id.loadingLayout)

        loadingLayout.apply {
            gravity = Gravity.CENTER
            setBackgroundColor("#AA000000".toColorInt())
            visibility = View.INVISIBLE

            val progressBar = ProgressBar(this@MainActivity).apply {
                isIndeterminate = true
            }

            val text = TextView(this@MainActivity).apply {
                text = ""
                setTextColor(Color.WHITE)
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(0, 30, 0, 0)
            }

            addView(progressBar)
            addView(text)
        }

        val folder = File(getExternalFilesDir(null), "FingerDance")
        if (folder.exists()) {
            creaMain()
        } else {
            creaDescarga()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001 && resultCode == RESULT_OK) {
            val zipUri = data?.data ?: return

            lifecycleScope.launch {
                try {
                    val localZip = File(getExternalFilesDir(null), "FingerDance.zip")

                    contentResolver.openInputStream(zipUri)?.use { input ->
                        FileOutputStream(localZip).use { output ->
                            input.copyTo(output)
                        }
                    }

                    val unzip = Unzip(this@MainActivity)
                    unzip.performUnzip(localZip.absolutePath, "FingerDance.zip", true)
                    themes.edit().putString("versionUpdate", numberUpdate).apply()
                    themes.edit().putString("efects", "").apply()
                    versionUpdate = numberUpdate
                    Toast.makeText(this@MainActivity, "Pack cargado correctamente", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Error cargando pack: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
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
                    progressBar.progress = progress
                    lbDescargando.text = "Descargando $progress%"

                    if (progress == 100) {
                        lbDescargando.text = "Descarga finalizada, espere por favor..."
                    }
                }
            }

            if (downloadedFile != null) {
                lifecycleScope.launch {
                    val unzip = Unzip(this@MainActivity)
                    val rutaZip = getExternalFilesDir("FingerDance.zip").toString()
                    unzip.performUnzip(rutaZip, "FingerDance.zip", true)
                }
            } else {
                Toast.makeText(this@MainActivity, "Error en la descarga", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun iniciarDescargaDrive(idDownload: String, typeFile: String, isUpdate: Boolean = false, progressCallback: (Int) -> Unit): File? {
        descargando = false
        linearDownload.setOnClickListener { }

        lbDescargando.isVisible = true
        progressBar.isVisible = true

        return withContext(Dispatchers.IO) {
            try {
                val url = "https://www.googleapis.com/drive/v3/files/$idDownload?alt=media&key=$API_KEY"
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
                        withContext(Dispatchers.Main) {
                            progressCallback(progress)
                        }
                    }

                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()
                    connection.disconnect()

                    return@withContext localFile
                } else {
                    withContext(Dispatchers.Main) {
                        showAlertFail(isUpdate)
                    }
                    return@withContext null
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showAlertFail(isUpdate)
                }
                return@withContext null
            }
        }
    }

    private fun showAlertFail(isUpdate: Boolean) {
        val messageFail = if(isUpdate){
            "No se pudo realizar la descarga automatica, quieres descargar el pack de actualizacion manualmente? " +
                    "\n Si ya descargaste el pack, presiona el boton 'Cargar Pack'"
        }else{
            "No se pudo realizar la descarga automatica, quieres descargar el pack inicial manualmente?" +
                    "\n Si ya descargaste el pack, presiona el boton 'Cargar Pack'"

        }
        val urlManual = if(isUpdate){
            "https://drive.google.com/file/d/1D4sMohVuJ7aGOcSzNCijsdFGHUsAf-2R/view?usp=drive_link"
        }else{
            "https://drive.google.com/file/d/1WZ3rL20JGEKcPtoQi0dHrZ8qs8z8-7kI/view?usp=drive_link"
        }
        AlertDialog.Builder(this@MainActivity)
            .setMessage(messageFail)
            .setPositiveButton("Descarga manual"){ d, _ ->
                val intent = Intent(Intent.ACTION_VIEW, urlManual.toUri()).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    setPackage(null) // muy importante, evita abrir directamente Drive
                }

                val chooser = Intent.createChooser(intent, "Abrir con navegador")
                startActivity(chooser)
                d.dismiss()
            }
            .setNegativeButton("Cargar Pack"){ d, _ ->
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/zip"
                    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val uri = Uri.fromFile(downloads)
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
                }
                startActivityForResult(intent, 1001)
            }
            .setNeutralButton("salir") { d, _ ->
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_HOME)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                exitProcess(0)
            }
            .show()
    }

    private fun creaMain() {
        val channelShort = File(getExternalFilesDir("/FingerDance/Songs/Channels/03-SHORT CUT")!!.absolutePath)
        val channelRemix = File(getExternalFilesDir("/FingerDance/Songs/Channels/04-REMIX")!!.absolutePath)
        val channelFullsong = File(getExternalFilesDir("/FingerDance/Songs/Channels/05-FULLSONGS")!!.absolutePath)
        val channelFI = File(getExternalFilesDir("/FingerDance/Songs/Channels/14-FIESTA TO FIESTA 2")!!.absolutePath)

        showLoadingOverlay("Espere por favor...")
        if(isFileExists(channelShort) || channelShort.isDirectory){
            deleteRecursive(channelShort)
        }
        if(isFileExists(channelRemix) || channelRemix.isDirectory){
            deleteRecursive(channelRemix)
        }
        if(isFileExists(channelFullsong) || channelFullsong.isDirectory){
            deleteRecursive(channelFullsong)
        }
        if(isFileExists(channelFI) || channelFI.isDirectory){
            deleteRecursive(channelFI)
        }
        loadingLayout.visibility = View.INVISIBLE
        if(!isOffline){
            val databaseReference = firebaseDatabase!!.getReference("version")
            var version : String
            databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    version = snapshot.child("value").getValue(String::class.java)?: ""
                    flagActiveAllows = snapshot.child("flagActiveAllows").getValue(Boolean::class.java) ?: false
                    numberUpdate = snapshot.child("numberUpdate").getValue(String::class.java)?: ""
                    val startOnline = snapshot.child("startOnline").getValue(Boolean::class.java) ?: false
                    val paypalOn = snapshot.child("paypalOn").getValue(Boolean::class.java) ?: false
                    val timeAdjust = snapshot.child("time_adjust").getValue(String::class.java) ?: ""
                    TIME_ADJUST = timeAdjust.toLong()
                    val timeHalfDouble = snapshot.child("timeHalfDouble").getValue(String::class.java) ?: ""
                    timeToPresiscionHD = timeHalfDouble.toInt()
                    CoroutineScope(Dispatchers.Main).launch {
                        getFilesDrive()
                        listFilesDrive.sortBy { it.first }
                    }

                    CoroutineScope(Dispatchers.Main).launch {
                        getThemesDrive()
                        listThemesDrive.sortBy { it.first }
                    }

                    val packageInfo = packageManager.getPackageInfo(packageName, 0)
                    versionApp = packageInfo.versionName!!

                    if(version == versionApp){
                        if(versionUpdate != numberUpdate){
                            val lbDescargandoUpdate = TextView(this@MainActivity).apply {
                                id = View.generateViewId()
                                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                                setTextColor(Color.WHITE)
                                visibility = View.INVISIBLE
                            }
                            lbDescargandoUpdate.textSize = 16f

                            val progressBar = ProgressBar(this@MainActivity, null, android.R.attr.progressBarStyleHorizontal).apply {
                                id = View.generateViewId()
                                layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, 15.dpToPx())
                                visibility = View.INVISIBLE
                            }

                            val linearDowload = LinearLayout(this@MainActivity)
                            linearDowload.orientation = LinearLayout.VERTICAL // Alineación vertical
                            linearDowload.gravity = Gravity.CENTER // Centra los elementos horizontalmente
                            linearDowload.layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )

                            linearDowload.addView(lbDescargandoUpdate)
                            linearDowload.addView(progressBar)

                            val dialogUpdate = AlertDialog.Builder(this@MainActivity, R.style.TransparentDialog)
                                .setCancelable(false)
                                .setTitle("Atualización adicional")
                                .setMessage("Se descargaran los recursos adicionales, espera por favor.")
                                .setView(linearDowload)
                                .show()

                            CoroutineScope(Dispatchers.Main).launch {
                                val downloadedFile = iniciarDescargaUpdate { progress ->
                                    runOnUiThread {
                                        progressBar.visibility = View.VISIBLE
                                        progressBar.progress = progress
                                        lbDescargandoUpdate.text = "Descargando $progress%"

                                        if (progress == 100) {
                                            lbDescargandoUpdate.text = "Descarga finalizada, espere por favor..."
                                            themes.edit().putString("versionUpdate", numberUpdate).apply()
                                            themes.edit().putString("efects", "").apply()
                                            versionUpdate = numberUpdate
                                            dialogUpdate.dismiss()
                                        }
                                    }
                                }
                                if (downloadedFile != null) {
                                    lifecycleScope.launch {
                                        val unzip = Unzip(this@MainActivity)
                                        val rutaZip =
                                            getExternalFilesDir("FingerDance.zip").toString() //Environment.getExternalStorageDirectory().toString() + "/Android/data/com.fingerdance/files/FingerDance.zip"
                                        unzip.performUnzip(rutaZip, "FingerDance.zip", true)
                                    }
                                } else {
                                    Toast.makeText(this@MainActivity, "Error en la descarga", Toast.LENGTH_LONG).show()
                                }
                            }
                        }else{
                            runOnUiThread {
                                createMain(startOnline, paypalOn)
                            }
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
                            .setMessage(R.string.updateApp)
                            .setView(linearDowload)
                            .setCancelable(false)
                            .show()

                        btnAceptarDownload.setOnClickListener {
                            lbDescargando.visibility = View.VISIBLE
                            progressBar.visibility = View.VISIBLE
                            btnAceptarDownload.visibility = View.GONE
                            dialog.setMessage("Espere por favor")
                            CoroutineScope(Dispatchers.Main).launch {
                                val packageApp = iniciarDescargaDrive("199Y0lsIdAHmLdRWb3ZQJmUPLMl8GjqYM", "apk", true) { progress ->
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
                                }
                            }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    println("Error al leer la versión: ${error.message}")
                    CoroutineScope(Dispatchers.IO).launch {
                        delay(2000)
                        runOnUiThread {
                            creaMain()
                        }
                    }
                }

            })
        }else{
            createMain(false)
            val lpOptions = btnOptions.layoutParams as ConstraintLayout.LayoutParams
            lpOptions.verticalBias = 0.55f
            btnOptions.layoutParams = lpOptions

            val lpExit = btnExit.layoutParams as ConstraintLayout.LayoutParams
            lpExit.verticalBias = 0.70f
            btnExit.layoutParams = lpExit
        }
    }

    private fun createMain(startOnline: Boolean, paypalOn: Boolean =  false){
        linearDownload.isVisible = false
        lbDescargando.isVisible = false
        progressBar.isVisible = false
        val them = File(getExternalFilesDir(null), "FingerDance/Themes/$tema")
        if (!them.exists()) {
            tema ="default"
        }

        bgaPathSelectChannel = getExternalFilesDir("/FingerDance/Themes/$tema/BGAs/BgaSelectChannel.mp4")!!.absolutePath
        bgaPathSelectSong = getExternalFilesDir("/FingerDance/Themes/$tema/BGAs/BgaSelectSong.mp4")!!.absolutePath

        if(File(bgaPathSelectChannel).isDirectory){
            File(bgaPathSelectChannel).delete()
        }

        btnPlay.foreground = Drawable.createFromPath(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/play.png").toString())
        btnPlayOnline.foreground = Drawable.createFromPath(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/play_online.png").toString())

        val rutaGrades = getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/dance_grade/").toString()
        arrayGrades = getGrades(rutaGrades)

        if(!startOnline){
            btnPlayOnline.visibility = View.GONE
            val lpOptions = btnOptions.layoutParams as ConstraintLayout.LayoutParams
            lpOptions.verticalBias = 0.55f
            btnOptions.layoutParams = lpOptions

            val lpExit = btnExit.layoutParams as ConstraintLayout.LayoutParams
            lpExit.verticalBias = 0.70f
            btnExit.layoutParams = lpExit
        }
        btnOptions.foreground = Drawable.createFromPath(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/options.png").toString())
        btnExit.foreground = Drawable.createFromPath(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/exit.png").toString())

        val sound = MediaPlayer.create(this@MainActivity, Uri.fromFile(File(getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/screen_title_music.ogg").toString())))
        soundPlayer = sound
        soundPlayer!!.isLooping = true
        soundPlayer!!.start()

        animLogo = findViewById(R.id.imgLogo)
        //animLogo.layoutParams.height = decimoHeigtn * 2
        animLogo.layoutParams.width = width / 2
        val bmLogo = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/logo.png").toString())
        animLogo.setImageBitmap(bmLogo)
        bgaOff = getExternalFilesDir("/FingerDance/Themes/$tema/Movies/BGA_OFF.mp4").toString()
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

        animLogo.setOnLongClickListener {
            themes.edit().putString("idWithRegister", "").apply()
            idWithRegister = ""
            Toast.makeText(this@MainActivity, "Registro reiniciado", Toast.LENGTH_LONG).show()
            true
        }
        btnPlay.setOnClickListener {
            btnPlay.isEnabled = false
            //flagActiveAllows = true
            if(flagActiveAllows){
                if(idWithRegister == ""){
                    showLoadingOverlay("Espere por favor...")
                    getAllowDevices { toListFreeDevices ->
                        listAllowDevices = toListFreeDevices
                    }
                    Handler(Looper.getMainLooper()).postDelayed({
                        if(listAllowDevices.isNotEmpty()){
                            val deviceFree = listAllowDevices.find { it.substringBefore("-") == deviceIdFind }
                            if(deviceFree != null) {
                                idWithRegister = deviceFree
                                themes.edit().putString("idWithRegister", idWithRegister).apply()
                                val register = idWithRegister.substringAfterLast("-")
                                val lastRegister = LocalDate.parse(register, formatter)
                                loadingLayout.visibility = View.INVISIBLE
                                if(LocalDate.now().isAfter(lastRegister) ){
                                    showPaySuscription(paypalOn)
                                } else {
                                    goPlay(goSound, animation)
                                }
                            }else{
                                loadingLayout.visibility = View.INVISIBLE
                                showPaySuscription(paypalOn)
                            }
                        }else{
                            (loadingLayout.getChildAt(1) as TextView).text = "Ocurrio un error, verifica tu conexión a Internet e intentalo de nuevo"
                        }
                    }, 3000L)
                }else{
                    val register = idWithRegister.substringAfterLast("-")
                    val lastRegister = LocalDate.parse(register, formatter)
                    if(LocalDate.now().isAfter(lastRegister) ){
                        showPaySuscription(paypalOn)
                    } else {
                        goPlay(goSound, animation)
                    }
                }
            }else{
                goPlay(goSound, animation)
            }
        }
        btnPlayOnline.setOnClickListener{
            if(flagActiveAllows){
                if(idWithRegister == ""){
                    showLoadingOverlay("Espere por favor...")
                    getAllowDevices { toListFreeDevices ->
                        listAllowDevices = toListFreeDevices
                    }
                    Handler(Looper.getMainLooper()).postDelayed({
                        if(listAllowDevices.isNotEmpty()){
                            val deviceFree = listAllowDevices.find { it.substringBefore("-") == deviceIdFind }
                            if(deviceFree != null) {
                                idWithRegister = deviceFree
                                themes.edit().putString("idWithRegister", idWithRegister).apply()
                                val register = idWithRegister.substringAfterLast("-")
                                val lastRegister = LocalDate.parse(register, formatter)
                                loadingLayout.visibility = View.INVISIBLE
                                if(LocalDate.now().isAfter(lastRegister) ){
                                    showPaySuscription(paypalOn)
                                } else {
                                    showOnlineMode(animation, goSound)
                                }
                            }else{
                                loadingLayout.visibility = View.INVISIBLE
                                showPaySuscription(paypalOn)
                            }
                        } else{
                            (loadingLayout.getChildAt(1) as TextView).text = "Ocurrio un error, verifica tu conexión a Internet e intentalo de nuevo"
                        }
                    }, 3000L)
                }else{
                    val register = idWithRegister.substringAfterLast("-")
                    val lastRegister = LocalDate.parse(register, formatter)
                    if(LocalDate.now().isAfter(lastRegister)){
                        showPaySuscription(paypalOn)
                    } else {
                        showOnlineMode(animation, goSound)
                    }
                }
            }else{
                showOnlineMode(animation, goSound)
            }
        }

        val goOptionMP = MediaPlayer.create(this@MainActivity, Uri.fromFile(File(getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/option_sound.mp3").toString())))
        btnOptions.setOnClickListener {
            if(flagActiveAllows){
                if(idWithRegister == ""){
                    showLoadingOverlay("Espere por favor...")
                    getAllowDevices { toListFreeDevices ->
                        listAllowDevices = toListFreeDevices
                    }
                    Handler(Looper.getMainLooper()).postDelayed({
                        if(listAllowDevices.isNotEmpty()){
                            val deviceFree = listAllowDevices.find { it.substringBefore("-") == deviceIdFind }
                            if(deviceFree != null) {
                                idWithRegister = deviceFree
                                themes.edit().putString("idWithRegister", idWithRegister).apply()
                                val register = idWithRegister.substringAfterLast("-")
                                val lastRegister = LocalDate.parse(register, formatter)
                                loadingLayout.visibility = View.INVISIBLE
                                if(LocalDate.now().isAfter(lastRegister) ){
                                    showPaySuscription(paypalOn)
                                } else {
                                    goOption(goOptionMP, animation)
                                }
                            }else{
                                loadingLayout.visibility = View.INVISIBLE
                                showPaySuscription(paypalOn)
                            }
                        } else {
                            (loadingLayout.getChildAt(1) as TextView).text = "Ocurrio un error, verifica tu conexión a Internet e intentalo de nuevo"
                        }
                    }, 3000L)
                }else{
                    val register = idWithRegister.substringAfterLast("-")
                    val lastRegister = LocalDate.parse(register, formatter)
                    if(LocalDate.now().isAfter(lastRegister) ){
                        showPaySuscription(paypalOn)
                    } else {
                        goOption(goOptionMP, animation)
                    }
                }
            }else{
                goOption(goOptionMP, animation)
            }
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

    }

    private fun goPlay(goSound: MediaPlayer, animation: Animation){
        isOnline = false
        goSound.start()
        btnPlay.startAnimation(animation)
        if(themes.getString("allTunes", "").toString() != ""){
            val jsonListChannels = themes.getString("allTunes", "")
            listChannels = gson.fromJson(jsonListChannels, object : TypeToken<ArrayList<Channels>>() {}.type)
        }else{
            showLoadingOverlay("Espere por favor...")
            listCommands = ls.getFilesCW(this@MainActivity)
            val ordenEspecifico = listOf("-.05", "-.1", "-.5", "-1", "0", "1", ".5", ".1", ".05")
            val ordenMap = ordenEspecifico.withIndex().associate { it.value to it.index }
            listCommands.find { it.descripcion == "Cambiar la velocidad de la nota." }!!.listCommandValues.sortBy { ordenMap[it.value] ?: Int.MAX_VALUE }

            listChannels = ls.getChannels(this@MainActivity)
            themes.edit().putString("allTunes", gson.toJson(listChannels)).apply()
            themes.edit().putString("efects", gson.toJson(listCommands)).apply()
            loadingLayout.visibility = View.INVISIBLE
        }
        if(themes.getString("efects", "").toString() == ""){
            showLoadingOverlay("Espere por favor...")
            listCommands = ls.getFilesCW(this@MainActivity)
            val ordenEspecifico = listOf("-.05", "-.1", "-.5", "-1", "0", "1", ".5", ".1", ".05")
            val ordenMap = ordenEspecifico.withIndex().associate { it.value to it.index }
            listCommands.find { it.descripcion == "Cambiar la velocidad de la nota." }!!.listCommandValues.sortBy { ordenMap[it.value] ?: Int.MAX_VALUE }
            themes.edit().putString("efects", gson.toJson(listCommands)).apply()
            loadingLayout.visibility = View.INVISIBLE
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

    private fun goOption(goOption: MediaPlayer, animation: Animation){
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
        //btnOptions.isEnabled = true
        Toast.makeText(this@MainActivity, "Cargando...", Toast.LENGTH_SHORT).show()
        btnOptions.startAnimation(animation)
        goOption.start()
        soundPlayer!!.pause()

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(applicationContext, Options()::class.java)
            startActivity(intent)
            this@MainActivity.finish()
        }, 1000L)
    }

    private fun showLoadingOverlay(message: String) {
        loadingLayout.visibility = View.VISIBLE
        (loadingLayout.getChildAt(1) as TextView).text = message
    }

    private fun showOnlineMode(animation: Animation, goSound: MediaPlayer) {
        btnPlayOnline.isEnabled=false
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
        var listSalas = arrayListOf<String>()
        btnJoinRoom.setOnClickListener {
            getSalas { toListSalas ->
                listSalas = toListSalas
            }
            dialog.setMessage("Ingresa la clave para entrar a la sala")
            linearOnline.visibility = View.GONE
            linearClave.visibility = View.VISIBLE
        }

        btnCreateRoom.setOnClickListener {
            goSound.start()
            isPlayer1 = true
            isOnline = true
            idSala =  UUID.randomUUID().toString().substring(0, 8)
            salaRef = firebaseDatabase!!.getReference("rooms/$idSala")
            salaRef.child("jugador1").onDisconnect().removeValue()
            val jugador1 = Jugador(id = userName)
            val date = LocalDate.now().toString()

            activeSala = Sala(turno = userName, jugador1 = jugador1)
            salaRef.setValue(activeSala).addOnSuccessListener {
                mostrarCodigoSala(dialog)
            }
        }

        val dialogNoSala = AlertDialog.Builder(this@MainActivity)
            .setTitle("Aviso")
            .setCancelable(false)
            .setPositiveButton("Aceptar"){d ,_ ->
                d.dismiss()
            }

        btnGetRoom.setOnClickListener {
            if(editTextRoom.text.toString() != ""){
                if(listSalas.find { it == editTextRoom.text.toString() } != null){
                    idSala = editTextRoom.text.toString()
                    goSound.start()
                    isOnline = true
                    isPlayer1 = false
                    salaRef = firebaseDatabase!!.getReference("rooms/${editTextRoom.text}")
                    salaRef.child("jugador2/id").setValue(userName)
                    salaRef.child("jugador2").onDisconnect().removeValue()
                    salaRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            activeSala = snapshot.getValue(Sala::class.java)!!
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
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
                    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    startActivity(intent)
                    mediaPlayerMain.pause()
                    soundPlayer!!.pause()
                    btnPlayOnline.isEnabled = true
                    dialog.dismiss()
                }else{
                    dialogNoSala.setMessage("La clave de la sala no existe")
                    dialogNoSala.show()
                }
            }else{
                dialogNoSala.setMessage("Debe ingresa la clave de la sala")
                dialogNoSala.show()
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
        themes.edit().putBoolean("isFree", false).apply()
        startActivity(intent)
    }

    private fun cerrarApp() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        exitProcess(0)
    }

    private fun isFileExists(file: File): Boolean {
        return file.exists() && !file.isDirectory
    }

    private fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) {
            fileOrDirectory.listFiles()?.forEach { child ->
                deleteRecursive(child)
            }
        }
        fileOrDirectory.delete()
    }

    private fun showPaySuscription(paypalOn: Boolean) {
        mediaPlayerMain.pause()
        video_fondo.pause()
        idWithRegister = ""
        themes.edit().putString("idWithRegister", "").apply()
        soundPlayer?.takeIf { it.isPlaying }?.pause()

        val layoutAviso = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(64, 64, 64, 64)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val tvMensaje = TextView(this).apply {
            text = getString(R.string.PaySuscriptionInfo)
            setTextColor(Color.WHITE)
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            textSize = 18f
            setPadding(16, 16, 16, 32)
        }

        fun createStyledButton(text: String, iconRes: Int, bgColor: Int,textColor: Int = Color.WHITE, colorIconDraw: Boolean = false): Button {
            return Button(this).apply {
                this.text = text
                isAllCaps = false
                setTextColor(textColor)
                background = ContextCompat.getDrawable(this@MainActivity, bgColor)
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                textSize = 16f
                setPadding(24, 16, 24, 16)
                compoundDrawablePadding = 16
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                val icon = ContextCompat.getDrawable(this@MainActivity, iconRes)
                if(colorIconDraw){
                    icon?.setTint(textColor)
                }
                setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)

                layoutParams = LinearLayout.LayoutParams(
                    medidaFlechas.toInt() * 3,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 16, 0, 0)
                }
                stateListAnimator = AnimatorInflater.loadStateListAnimator(context, android.R.animator.fade_in)
            }
        }

        val btnFacebook = createStyledButton(
            "Facebook",
            R.drawable.facebook,
            R.drawable.bg_button_base,
            colorIconDraw = true
        ).apply {
            backgroundTintList = ColorStateList.valueOf(0xFF1877F2.toInt())
        }

        val btnWhatsapp = createStyledButton(
            "WhatsApp",
            R.drawable.whatsapp,
            R.drawable.bg_button_base,
            colorIconDraw = true
        ).apply {
            backgroundTintList = ColorStateList.valueOf(0xFF25D366.toInt())
        }

        val btnMercadoPago = createStyledButton(
            "Pagar con\nMercado Pago",
            R.drawable.mercado_pago,
            R.drawable.bg_button_base,
        ).apply {
            backgroundTintList = ColorStateList.valueOf("#009EE3".toColorInt())
        }
        val btnPaypal = createStyledButton(
            "Pagar con\nPayPal",
            R.drawable.paypal,
            R.drawable.bg_button_paypal,
            textColor = Color.BLACK,
        )

        val spaceBottom = Space(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val btnSalir = createStyledButton(
            "Salir",
            android.R.drawable.ic_menu_close_clear_cancel,
            R.drawable.bg_button_base
        ).apply {
            backgroundTintList = ColorStateList.valueOf(0xFFD32F2F.toInt())
            setTextColor(Color.WHITE)
            isAllCaps = true
            layoutParams = LinearLayout.LayoutParams(
                medidaFlechas.toInt() * 3,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        layoutAviso.addView(tvMensaje)
        layoutAviso.addView(btnFacebook)
        layoutAviso.addView(btnWhatsapp)
        layoutAviso.addView(btnMercadoPago)
        if(paypalOn){
            layoutAviso.addView(btnPaypal)
        }
        layoutAviso.addView(spaceBottom)
        layoutAviso.addView(btnSalir)
        setContentView(layoutAviso)
        layoutAviso.bringToFront()

        btnFacebook.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, "https://www.facebook.com/share/g/18pNnxajis/".toUri()))
            finishAffinity()
        }

        btnWhatsapp.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, "https://chat.whatsapp.com/JXXFQ1TpRnz0HfeRb71RuT".toUri()))
            finishAffinity()
        }

        btnMercadoPago.setOnClickListener {
            showLoadingOverlay("redirigiendo...")
            val jsonBody = JSONObject().apply {
                put("descripcion", "Suscripción mensual Finger Dance")
                put("monto", 25)
                put("deviceId", deviceIdFind)
                put("userName", userName)
            }

            val request = JsonObjectRequest(
                Request.Method.POST,
                "https://us-central1-fingerdance.cloudfunctions.net/createPreference",
                jsonBody,
                { response ->
                    val preferenceId = response.getString("preferenceId")
                    val mpUrl = "https://www.mercadopago.com.mx/checkout/v1/redirect?pref_id=$preferenceId"
                    startActivity(Intent(Intent.ACTION_VIEW, mpUrl.toUri()))
                    loadingLayout.visibility = View.INVISIBLE
                },
                { error ->
                    Toast.makeText(this, "Error al generar pago: ${error.message}", Toast.LENGTH_LONG).show()
                }
            )
            Volley.newRequestQueue(this).add(request)
        }

        btnPaypal.setOnClickListener {
            showLoadingOverlay("redirigiendo...")
            val url = "https://createpaypalorder-pc2otnl6da-uc.a.run.app"
            val jsonBody = JSONObject().apply {
                put("descripcion", "Suscripción Finger Dance")
                put("monto", 25)
                put("deviceId", deviceIdFind)
                put("userName", userName)
            }
            val request = object : JsonObjectRequest(
                Method.POST,
                url,
                jsonBody,
                { response ->
                    try {
                        val orderId = response.getString("orderId")
                        val paypalUrl = "https://www.paypal.com/checkoutnow?token=$orderId"
                        startActivity(Intent(Intent.ACTION_VIEW, paypalUrl.toUri()))
                        loadingLayout.visibility = View.INVISIBLE
                    } catch (e: Exception) {
                        Log.e("PAYPAL", "Error procesando respuesta: ${e.message}")
                    }
                },
                { error ->
                    Log.e("PAYPAL", "Error al crear orden PayPal: ${error.networkResponse?.statusCode ?: "?"}")
                    Log.e("PAYPAL", "Detalle: ${error.message}")
                    error.printStackTrace()
                }
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    return hashMapOf("Content-Type" to "application/json")
                }
            }

            Volley.newRequestQueue(this).add(request)
        }


        btnSalir.setOnClickListener { finishAffinity() }
    }

    private suspend fun iniciarDescargaUpdate(progressCallback: (Int) -> Unit): File? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://www.googleapis.com/drive/v3/files/1D4sMohVuJ7aGOcSzNCijsdFGHUsAf-2R?alt=media&key=$API_KEY"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                if (connection.responseCode == 200) {
                    val localFile = File(getExternalFilesDir(null), "FingerDance.zip")

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
                    withContext(Dispatchers.Main) {
                       showAlertFail(true)
                    }
                    return@withContext null
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                   showAlertFail(true)
                }
                return@withContext null
            }
        }
    }

    private fun getSalas(callback: (ArrayList<String>) -> Unit) {
        val databaseRef = firebaseDatabase!!.getReference("rooms")
        val listResult = arrayListOf<String>()
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (salas in snapshot.children) {
                    listResult.add(salas.key.toString())
                }
                callback(listResult)
                return
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al leer datos", error.toException())
            }
        })
    }

    private fun mostrarCodigoSala(alertDialog: AlertDialog) {
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
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                mediaPlayerMain.pause()
                soundPlayer!!.pause()
                btnPlayOnline.isEnabled = true
                mostrarDialogoCompartir(this)
            }
            .setNegativeButton("Cancelar"){_, _ ->
                firebaseDatabase!!.getReference("rooms/$idSala").removeValue()
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

    private fun getAllowDevices(callback: (ArrayList<String>) -> Unit) {
        val databaseRef = firebaseDatabase!!.getReference("freeDevices")
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

    private fun getDeviceId(context: Context): String {
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
        val regex = Regex("^(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d!@#\$%^&*()_+\\-={}:;\"'<>,.?/]{8,}$")
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

    suspend fun getThemesDrive() {
        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = URLEncoder.encode("'$FOLDER_ID_THEMES' in parents", "UTF-8")
                val url = "https://www.googleapis.com/drive/v3/files?q=$encodedQuery&key=$API_KEY"

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()

                    val jsonResponse = JSONObject(response)
                    val files = jsonResponse.getJSONArray("files")
                    listThemesDrive.clear()
                    for (i in 0 until files.length()) {
                        val file = files.getJSONObject(i)
                        val fileName = file.getString("name")
                        val fileId = file.getString("id")
                        listThemesDrive.add(Pair(fileName, fileId))
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

    private fun getGrades(rutaGrades: String): ArrayList<Bitmap> {
        val bit = BitmapFactory.decodeFile("$rutaGrades/evaluation_grades 1x8.png")
        val cellWidth = bit.width / 2
        val cellHeight = bit.height / 8

        return ArrayList<Bitmap>().apply {
            for (r in 0 until 8) {
                for (c in 0 until 2) {
                    val x = c * cellWidth
                    val y = r * cellHeight
                    add(Bitmap.createBitmap(bit, x, y, cellWidth, cellHeight))
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()

        if (::mediaPlayerMain.isInitialized) {
            try {
                if (mediaPlayerMain.isPlaying) {
                    currentVideoPosition = mediaPlayerMain.currentPosition
                    video_fondo.pause()
                    mediaPlayerMain.pause()
                    soundPlayer?.let {
                        if (it.isPlaying) it.pause()
                    }
                }
            } catch (e: IllegalStateException) {
                Log.w("MainActivity", "mediaPlayerMain no estaba listo para pausar: ${e.message}")
            }
        }

        if (::bg_download.isInitialized && linearDownload.isVisible) {
            try {
                if (bg_download.isPlaying) {
                    bg_download.pause()
                }
            } catch (e: IllegalStateException) {
                Log.w("MainActivity", "bg_download no estaba listo para pausar: ${e.message}")
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
        listEfectsDisplay.clear()
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
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        windowInsetsController.let { controller ->
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }

    }

    private fun animar() {
        val animation = AnimationUtils.loadAnimation(this, R.anim.scale)
        animLogo.startAnimation(animation)

    }
}

class ObjPuntaje(var cancion: String = "", var puntaje: String = "", var grade: String = "")

data class Resultado(
    var perfect: String = "0",
    var great: String = "0",
    var good: String = "0",
    var bad: String = "0",
    var miss: String = "0",
    var maxCombo: String = "0",
    var score: String = "0",
)

data class Jugador(
    var id: String = "",
    var listo: Boolean = false,
    var result: Resultado = Resultado(),
)

data class Sala(
    var cancion: CancionOnline = CancionOnline(),
    var jugador1: Jugador = Jugador(),
    var jugador2: Jugador = Jugador(),
    var turno: String = "",
    var date: String = "",
    var readyToResult: Boolean = false,
)

data class CancionOnline(
    var rutaKsf: String = "",
    var rutaCancion: String = "",
    var rutaBGA: String = "",
    var rutaPreview: String = "",
    var rutaBanner: String = "",
    var rutaDisc: String = "",
    var nivel: String = "",
    var artists: String = "",
    var nameSong: String = "",
    var bpm: String = "",
    var isHalf: Boolean = false,
)