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
import android.graphics.PointF
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
import android.text.InputType
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
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
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.Serializable
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.abs
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
var valueOffset = 0L

val gson = Gson()
/*
var numberUpdate = ""
var versionUpdate = ""
var flagActiveAllows = false

var showPadB : Int = 0
var hideImagesPadA : Boolean = false
var skinPad = ""
var alphaPadB = 1f
*/
// NOTA: API_KEY, FOLDER_ID, FOLDER_ID_THEMES, FOLDER_ID_CHANNELS_BGA
// listFilesDrive, listThemesDrive, listChannelsDrive,
// userName, firebaseDatabase, listGlobalRanking
// están centralizados en GlobalConstants.kt
/*
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
*/
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
var timeToPresiscion = 0

lateinit var validFolders : List<String>

lateinit var arrGradesDesc : ArrayList<Bitmap>
lateinit var arrGradesDescAbrev : ArrayList<Bitmap>
lateinit var channelFavorites : Channels
lateinit var listFavorites : ArrayList<SongKsf>
lateinit var mockListChannels : ArrayList<Canal>

private var bmLogo : Bitmap? = null  // Guardar para reciclar en onDestroy

var isSsc = false
lateinit var sscCharData: Parser.ChartData
lateinit var sscSong : Song

//private const val KEY_CHANNELS_CACHE = "channels_cache"

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

    private lateinit var gestureDetector: GestureDetector
    private val directions = mutableListOf<Direction>()
    private var lastX = 0f
    private var lastY = 0f

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            return true // IMPORTANTE: si no regresas true, no detecta nada
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            return false
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            return false
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                directions.clear()
                lastX = ev.x
                lastY = ev.y
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - lastX
                val dy = ev.y - lastY

                val direction = getDirection(dx, dy)
                if (direction != null) {
                    if (directions.isEmpty() || directions.last() != direction) {
                        directions.add(direction)
                    }
                }

                lastX = ev.x
                lastY = ev.y
            }

            MotionEvent.ACTION_UP -> {
                if (isZGesture()) {
                    showPasswordDialog()
                }
            }
        }

        return super.dispatchTouchEvent(ev)
    }

    enum class Direction {
        RIGHT,
        DOWN_RIGHT,
        OTHER
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))
        setContentView(R.layout.activity_main)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        firebaseDatabase = FirebaseDatabase.getInstance()
        gestureDetector = GestureDetector(this, gestureListener)
        db = DataBasePlayer(this)
        themes = getPreferences(MODE_PRIVATE)
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
        //themes.edit().putString("favorites", "").apply()



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

        getValidFolders { folders ->
            validFolders = folders
        }

        /*
        validFolders = listOf(
            "000-Finger Dance",
            "03-SHORT CUT - V2",
            "04-REMIX - V2",
            "05-FULLSONGS - V2",
            "10-PIU 1st TO PERFECT COLLECTION",
            "11-EXTRA TO PREX 3",
            "12-EXCEED TO ZERO",
            "13-NX TO NXA",
            "14-FIESTA TO FIESTA 2 - V2",
            "15 - INFINITY",
            "17-PRIME",
            "18-PRIME 2",
            "19-XX ANIVERSARY",
            "20-PHOENIX",
            "21-RISE",
            "50-Prex Metal"
        )
        */

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val json = assets.open("mockChannels.json")
                    .bufferedReader().use { it.readText() }

                val listChannelsJson: ArrayList<Canal> = Gson().fromJson(
                    json,
                    object : TypeToken<ArrayList<Canal>>() {}.type
                )

                withContext(Dispatchers.Main) {
                    mockListChannels = listChannelsJson
                }

            } catch (e: Exception) {
                Log.e("MOCK", "Error cargando JSON: ${e.message}")
            }
        }

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
            setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    // No hace nada
                }
            })

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

            addView(progressBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(text, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
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

    private fun getDirection(dx: Float, dy: Float): Direction? {
        val threshold = 20f
        if (abs(dx) < threshold && abs(dy) < threshold) return null

        return when {
            dx > 0 && abs(dy) < abs(dx) * 0.5f -> Direction.RIGHT
            dx > 0 && dy > 0 -> Direction.DOWN_RIGHT
            else -> Direction.OTHER
        }
    }

    private fun isZGesture(): Boolean {
        val filtered = directions.filter { it != Direction.OTHER }

        // Simplificamos secuencia (ej: RIGHT,RIGHT,RIGHT → RIGHT)
        val simplified = mutableListOf<Direction>()
        for (d in filtered) {
            if (simplified.isEmpty() || simplified.last() != d) {
                simplified.add(d)
            }
        }

        return simplified == listOf(
            Direction.RIGHT,
            Direction.DOWN_RIGHT,
            Direction.RIGHT
        )
    }

    private fun showPasswordDialog() {
        val input = EditText(this)
        input.inputType =
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_TEXT_VARIATION_PASSWORD

        AlertDialog.Builder(this)
            .setTitle("Acceso oculto")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                if (input.text.toString() == "2416") {
                    doSecretAccion()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun doSecretAccion() {
        val editText = EditText(this).apply {
            hint = "Pega el valor del dispositivo"
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            inputType = InputType.TYPE_CLASS_TEXT
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Validar Dispositivo")
            .setMessage("Ingresa el código del dispositivo")
            .setView(editText)
            .setCancelable(true)
            .setPositiveButton("Validar") { _, _ ->
                val inputValue = editText.text.toString().trim()
                if (inputValue.isNotEmpty()) {
                    processDeviceValidation(inputValue)
                } else {
                    Toast.makeText(this, "Por favor ingresa un valor", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun processDeviceValidation(inputValue: String) {
        // Extraer la parte antes del primer "-"
        val inputPrefix = inputValue.substringBefore("-")

        getAllowDevices { listAllowDevices ->
            var foundDevice: String? = null
            var foundMatch: Boolean = false

            // Buscar coincidencia en listAllowDevices
            for (device in listAllowDevices) {
                val devicePrefix = device.substringBefore("-")
                if (devicePrefix == inputPrefix) {
                    foundDevice = device
                    foundMatch = true
                    break
                }
            }

            if (foundMatch && foundDevice != null) {
                // Encontramos coincidencia, actualizar la fecha
                val updatedDevice = updateDeviceDate(foundDevice)
                updateDeviceInFirebase(foundDevice, updatedDevice)
            } else {
                // No encontramos coincidencia, insertar un nuevo registro
                // Extraer solo la parte sin fecha (antes de la última "-" si existe una fecha)
                val deviceWithoutDate = if (inputValue.matches(Regex(".*-\\d{2}/\\d{2}/\\d{4}$"))) {
                    // Si el input tiene formato de fecha al final, extraer sin ella
                    inputValue.substringBeforeLast("-")
                } else {
                    // Si no tiene fecha, usar como está
                    inputValue
                }
                val newDevice = deviceWithoutDate + "-" + getDatePlusOneMonth()
                insertDeviceInFirebase(newDevice)
            }
        }
    }

    private fun updateDeviceDate(device: String): String {
        // Formato: d6b1286955eda83b-Hectbaren1216$-31/12/2099
        val parts = device.split("-")

        if (parts.size >= 2) {
            val prefix = parts[0] // d6b1286955eda83b
            val middle = parts[1] // Hectbaren1216$

            // Calcular nueva fecha: hoy + 1 mes - 1 día
            val newDate = getDatePlusOneMonth()

            return "$prefix-$middle-$newDate"
        }

        return device
    }

    private fun addOneMonthAndSubtractOneDay(dateString: String): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val date = LocalDate.parse(dateString, formatter)

            // Sumar 1 mes
            val dateAfterMonth = date.plusMonths(1)

            // Restar 1 día
            val finalDate = dateAfterMonth.minusDays(1)

            finalDate.format(formatter)
        } catch (e: Exception) {
            Log.e("DateParsing", "Error al parsear la fecha: ${e.message}")
            dateString
        }
    }

    private fun getDatePlusOneMonth(): String {
        return try {
            val today = LocalDate.now()
            val nextMonth = today.plusMonths(1).minusDays(1)
            nextMonth.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        } catch (e: Exception) {
            Log.e("DateParsing", "Error al calcular la fecha: ${e.message}")
            ""
        }
    }

    private fun updateDeviceInFirebase(oldDevice: String, newDevice: String) {
        val databaseRef = firebaseDatabase!!.getReference("freeDevices")

        // Obtener toda la lista de dispositivos
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val devicesList = mutableListOf<String>()
                var found = false

                // Recorrer y actualizar la lista
                for (childSnapshot in snapshot.children) {
                    val device = childSnapshot.value.toString()
                    if (device == oldDevice) {
                        devicesList.add(newDevice)
                        found = true
                    } else {
                        devicesList.add(device)
                    }
                }

                if (found) {
                    // Reescribir el nodo completo con la lista actualizada
                    databaseRef.setValue(devicesList)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this@MainActivity,
                                "Dispositivo actualizado correctamente",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.d("Firebase", "Dispositivo actualizado: $oldDevice -> $newDevice")
                        }
                        .addOnFailureListener { error ->
                            Toast.makeText(
                                this@MainActivity,
                                "Error al actualizar: ${error.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.e("Firebase", "Error al actualizar: ${error.message}")
                        }
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "No se encontró el dispositivo en Firebase",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@MainActivity,
                    "Error al leer Firebase: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("Firebase", "Error al leer: ${error.message}")
            }
        })
    }

    private fun insertDeviceInFirebase(newDevice: String) {
        val databaseRef = firebaseDatabase!!.getReference("freeDevices")

        // Obtener toda la lista de dispositivos
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val devicesList = mutableListOf<String>()

                // Recorrer y agregar todos los dispositivos existentes
                for (childSnapshot in snapshot.children) {
                    devicesList.add(childSnapshot.value.toString())
                }

                // Agregar el nuevo dispositivo
                devicesList.add(newDevice)

                // Reescribir el nodo completo con la lista actualizada
                databaseRef.setValue(devicesList)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this@MainActivity,
                            "Nuevo dispositivo registrado correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d("Firebase", "Nuevo dispositivo insertado: $newDevice")
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(
                            this@MainActivity,
                            "Error al insertar: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e("Firebase", "Error al insertar: ${error.message}")
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@MainActivity,
                    "Error al leer Firebase: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("Firebase", "Error al leer: ${error.message}")
            }
        })
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
        val downloadDialog = WebDownloadDialog(this@MainActivity)
        downloadDialog.show("FingerDance.zip")

        CoroutineScope(Dispatchers.Main).launch {
            val downloadedFile = iniciarDescargaDrive("1WZ3rL20JGEKcPtoQi0dHrZ8qs8z8-7kI", "zip") { progress ->
                runOnUiThread {
                    downloadDialog.updateProgress(progress)
                }
            }

            if (downloadedFile != null) {
                downloadDialog.dismiss()
                lifecycleScope.launch {
                    val unzip = Unzip(this@MainActivity)
                    val rutaZip = getExternalFilesDir("FingerDance.zip").toString()
                    unzip.performUnzip(rutaZip, "FingerDance.zip", true)
                }
            } else {
                downloadDialog.dismiss()
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

    data class ChannelsDrive(
        val name: String,
        val id: String,
        val songs: ArrayList<SongsDrive>
    )

    data class SongsDrive(
        val name: String,
        val id: String,
        val videos: ArrayList<VideosDrive>
    )

    data class VideosDrive(
        val name: String,
        val id: String,
        val size: String,
    )

    private fun creaMain() {

        showLoadingOverlay("Espere por favor...")

        cleanChannels()

        loadingLayout.visibility = View.INVISIBLE

        // Los datos YA fueron cargados en SplashActivity
        // Solo procesar según el modo
        val preferences = getPreferences(MODE_PRIVATE)
        var flagActiveAllows = preferences.getString("flagActiveAllows", "false").toBoolean()
        val resetRegister = preferences.getBoolean("resetRegister", false)
        val paypalOn = preferences.getBoolean("paypalOn", false)
        val mpOn = preferences.getBoolean("mpOn", false)

        // Actualizar variables globales con los datos cargados en Splash
        TIME_ADJUST = preferences.getLong("TIME_ADJUST", 0)
        timeToPresiscion = preferences.getInt("timeToPresiscion", 0)
        timeToPresiscionHD = preferences.getInt("timeToPresiscionHD", 0)
        flagActiveAllows = flagActiveAllows

        if (resetRegister) {
            themes.edit().putString("idWithRegister", "").apply()
            idWithRegister = ""
        }

        if (!isOffline && flagActiveAllows) {
            createMain(true, paypalOn, mpOn)
        } else {
            createMain(false)
        }
    }

    private fun cleanChannels() {

        val pathChannels = "/FingerDance/Songs/Channels"
        val channels = listOf(
            "$pathChannels/03-SHORT CUT",
            "$pathChannels/04-REMIX",
            "$pathChannels/05-FULLSONGS",
            "$pathChannels/14-FIESTA TO FIESTA 2"
        )

        channels.forEach { path ->
            val dir = File(getExternalFilesDir(path)!!.absolutePath)
            if (dir.exists() && dir.isDirectory) {
                deleteRecursive(dir)
            }
        }
    }

    private fun createMain(startOnline: Boolean, paypalOn: Boolean =  false, mpOn: Boolean = false){
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

        val gradeDescription = "${rutaGrades.replace("dance_grade", "game_play")}/grade_description.png"
        val gradeDescriptionAbrev = "${rutaGrades.replace("dance_grade", "game_play")}/grade_description_abrev.png"

        arrGradesDesc = getGradesDescription(gradeDescription)
        arrGradesDescAbrev = getGradesDescription(gradeDescriptionAbrev)

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
        bmLogo = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/logo.png").toString())
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
            if(flagActiveAllows){
                if(idWithRegister == ""){
                    showLoadingOverlay("Espere por favor...")
                    getAllowDevices { toListFreeDevices ->
                        listAllowDevices = toListFreeDevices
                        if(listAllowDevices.isNotEmpty()){
                            val deviceFree = listAllowDevices.find { it.substringBefore("-") == deviceIdFind }
                            if(deviceFree != null) {
                                idWithRegister = deviceFree
                                themes.edit().putString("idWithRegister", idWithRegister).apply()
                                val register = idWithRegister.substringAfterLast("-")
                                val lastRegister = LocalDate.parse(register, formatter)
                                loadingLayout.visibility = View.INVISIBLE
                                if(LocalDate.now().isAfter(lastRegister) ){
                                    showPaySuscription(paypalOn, mpOn)
                                } else {
                                    lifecycleScope.launch {
                                        goPlay(goSound, animation)
                                    }
                                }
                            }else{
                                loadingLayout.visibility = View.INVISIBLE
                                showPaySuscription(paypalOn, mpOn)
                            }
                        }else{
                            (loadingLayout.getChildAt(1) as TextView).text = "Ocurrio un error, verifica tu conexión a Internet e intentalo de nuevo"
                        }
                    }
                }else{
                    val register = idWithRegister.substringAfterLast("-")
                    val lastRegister = LocalDate.parse(register, formatter)
                    if(LocalDate.now().isAfter(lastRegister) ){
                        showPaySuscription(paypalOn, mpOn)
                    } else {
                        lifecycleScope.launch {
                            goPlay(goSound, animation)
                        }
                    }
                }
            }else{
                lifecycleScope.launch {
                    goPlay(goSound, animation)
                }
            }
        }
        btnPlayOnline.setOnClickListener{
            if(flagActiveAllows){
                if(idWithRegister == ""){
                    showLoadingOverlay("Espere por favor...")
                    getAllowDevices { toListFreeDevices ->
                        listAllowDevices = toListFreeDevices
                        if(listAllowDevices.isNotEmpty()){
                            val deviceFree = listAllowDevices.find { it.substringBefore("-") == deviceIdFind }
                            if(deviceFree != null) {
                                idWithRegister = deviceFree
                                themes.edit().putString("idWithRegister", idWithRegister).apply()
                                val register = idWithRegister.substringAfterLast("-")
                                val lastRegister = LocalDate.parse(register, formatter)
                                loadingLayout.visibility = View.INVISIBLE
                                if(LocalDate.now().isAfter(lastRegister) ){
                                    showPaySuscription(paypalOn, mpOn)
                                } else {
                                    showOnlineMode(animation, goSound)
                                }
                            }else{
                                loadingLayout.visibility = View.INVISIBLE
                                showPaySuscription(paypalOn, mpOn)
                            }
                        } else{
                            (loadingLayout.getChildAt(1) as TextView).text = "Ocurrio un error, verifica tu conexión a Internet e intentalo de nuevo"
                        }
                    }
                }else{
                    val register = idWithRegister.substringAfterLast("-")
                    val lastRegister = LocalDate.parse(register, formatter)
                    if(LocalDate.now().isAfter(lastRegister)){
                        showPaySuscription(paypalOn, mpOn)
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
                        if(listAllowDevices.isNotEmpty()){
                            val deviceFree = listAllowDevices.find { it.substringBefore("-") == deviceIdFind }
                            if(deviceFree != null) {
                                idWithRegister = deviceFree
                                themes.edit().putString("idWithRegister", idWithRegister).apply()
                                val register = idWithRegister.substringAfterLast("-")
                                val lastRegister = LocalDate.parse(register, formatter)
                                loadingLayout.visibility = View.INVISIBLE
                                if(LocalDate.now().isAfter(lastRegister) ){
                                    showPaySuscription(paypalOn, mpOn)
                                } else {
                                    lifecycleScope.launch {
                                        goOption(goOptionMP, animation)
                                    }
                                }
                            }else{
                                loadingLayout.visibility = View.INVISIBLE
                                showPaySuscription(paypalOn, mpOn)
                            }
                        } else {
                            (loadingLayout.getChildAt(1) as TextView).text = "Ocurrio un error, verifica tu conexión a Internet e intentalo de nuevo"
                        }
                    }
                }else{
                    val register = idWithRegister.substringAfterLast("-")
                    val lastRegister = LocalDate.parse(register, formatter)
                    if(LocalDate.now().isAfter(lastRegister) ){
                        showPaySuscription(paypalOn, mpOn)
                    } else {
                        lifecycleScope.launch {
                            goOption(goOptionMP, animation)
                        }
                    }
                }
            }else{
                lifecycleScope.launch {
                    goOption(goOptionMP, animation)
                }
            }
        }

        val builder = AlertDialog.Builder(this@MainActivity)
        btnExit.setOnClickListener {
            /*
            val ls = LoadingSongs(this@MainActivity)
            val listChannelsSsc = ls.getChannels(this@MainActivity)
            val parser = Parser()
            sscSong = listChannelsSsc[0].listCanciones[9]
            sscCharData = parser.parseSSC(sscSong.listLvs[2].steps)
            isSsc = true
            btnPlay.performClick()

            for(i in 0 until steps.notes.size){
                Log.d("Steps: ", "${steps.notes[i]}")
            }
            */
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

    private suspend fun goPlay(goSound: MediaPlayer, animation: Animation){
        isOnline = false
        goSound.start()
        btnPlay.startAnimation(animation)
        if(themes.getString("allTunes", "").toString() != ""){
            val jsonListChannels = themes.getString("allTunes", "")
            listChannels = gson.fromJson(jsonListChannels, object : TypeToken<ArrayList<Channels>>() {}.type)
        }else{
            showLoadingOverlay("Espere por favor...")
            getFilesDrive()
            listCommands = ls.getFilesCW(this@MainActivity)
            val ordenEspecifico = listOf("-.05", "-.1", "-.5", "-1", "0", "1", ".5", ".1", ".05")
            val ordenMap = ordenEspecifico.withIndex().associate { it.value to it.index }
            listCommands.find { it.descripcion == "Cambiar la velocidad de la nota." }!!.listCommandValues.sortBy { ordenMap[it.value] ?: Int.MAX_VALUE }

            listChannels = ls.getChannels(this@MainActivity)
            themes.edit().putString("allTunes", gson.toJson(listChannels)).apply()
            themes.edit().putString("efects", gson.toJson(listCommands)).apply()
        }
        if(themes.getString("efects", "").toString() == ""){
            showLoadingOverlay("Espere por favor...")
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
        if(themes.getString("favorites", "").toString() != ""){
            val jsonListFavoites = themes.getString("favorites", "")
            listFavorites = gson.fromJson(jsonListFavoites, object : TypeToken<ArrayList<SongKsf>>() {}.type)
            val pathBannerFavorites = getExternalFilesDir("/FingerDance/Themes/favorites_banner.png")!!.absolutePath
            channelFavorites = Channels("06-FAVORITES", getString(R.string.favorites_description), pathBannerFavorites, listCancionesKsf = listFavorites)
            listChannels.add(channelFavorites)
            listChannels.sortBy { it.nombre.substringBefore("-").trim() }
        }else{
            listFavorites = arrayListOf()
            val pathBannerFavorites = getExternalFilesDir("/FingerDance/Themes/favorites_banner.png")!!.absolutePath
            channelFavorites = Channels("06-FAVORITES", getString(R.string.favorites_description), pathBannerFavorites, listCancionesKsf = listFavorites)
            listChannels.add(channelFavorites)
            listChannels.sortBy { it.nombre.substringBefore("-").trim() }
        }

        loadingLayout.visibility = View.INVISIBLE
        val intent = Intent(this@MainActivity, SelectChannel::class.java)
        startActivity(intent)
        mediaPlayerMain.pause()
        soundPlayer!!.pause()
        btnPlay.isEnabled = true
    }

    private suspend fun goOption(goOption: MediaPlayer, animation: Animation){
        if(themes.getString("allTunes", "").toString() != ""){
            val jsonListChannels = themes.getString("allTunes", "")
            listChannels = gson.fromJson(jsonListChannels, object : TypeToken<ArrayList<Channels>>() {}.type)
        }else{
            showLoadingOverlay("Espere por favor...")
            getFilesDrive()
            listCommands = ls.getFilesCW(this@MainActivity)
            val ordenEspecifico = listOf("-.05", "-.1", "-.5", "-1", "0", "1", ".5", ".1", ".05")
            val ordenMap = ordenEspecifico.withIndex().associate { it.value to it.index }
            listCommands.find { it.descripcion == "Cambiar la velocidad de la nota." }!!.listCommandValues.sortBy { ordenMap[it.value] ?: Int.MAX_VALUE }
            listChannels = ls.getChannels(this@MainActivity)
            themes.edit().putString("allTunes", gson.toJson(listChannels)).apply()
            themes.edit().putString("efects", gson.toJson(listCommands)).apply()

        }
        Toast.makeText(this@MainActivity, "Cargando...", Toast.LENGTH_SHORT).show()
        btnOptions.startAnimation(animation)
        goOption.start()
        soundPlayer!!.pause()

        loadingLayout.visibility = View.INVISIBLE
        val intent = Intent(applicationContext, OptionsActivity()::class.java)
        startActivity(intent)
        this@MainActivity.finish()
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

    private fun cerrarApp() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        exitProcess(0)
    }


    private fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) {
            fileOrDirectory.listFiles()?.forEach { child ->
                deleteRecursive(child)
            }
        }
        fileOrDirectory.delete()
    }

    private fun showPaySuscription(paypalOn: Boolean, mpOn: Boolean) {
        /*
        val deviceFree = listAllowDevices.find { it.substringBefore("-") == deviceIdFind }
        val isPass = deviceFree == deviceIdFind
        val dialog = AlertDialog.Builder(this)
            .setTitle("Validacion Device ID")
            .setMessage("Dispositivo ID: $deviceIdFind | Dispositivo encontrado en base: $deviceFree | $isPass")
            .setPositiveButton("Aceptar") { d, _ ->
                d.dismiss()
            }
            .show()
        */
        mediaPlayerMain.pause()
        video_fondo.pause()
        idWithRegister = ""
        themes.edit().putString("idWithRegister", "").apply()
        soundPlayer?.takeIf { it.isPlaying }?.pause()

        val layoutAviso = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor("#121212".toColorInt())
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

        fun createStyledButton(
            text: String,
            iconRes: Int,
            bgColor: Int,
            textColor: Int = Color.WHITE,
            colorIconDraw: Boolean = false
        ): Button {
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
                if (colorIconDraw) {
                    icon?.setTint(textColor)
                }
                setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)

                layoutParams = LinearLayout.LayoutParams(
                    medidaFlechas.toInt() * 3,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 16, 0, 0)
                }
                stateListAnimator =
                    AnimatorInflater.loadStateListAnimator(context, android.R.animator.fade_in)
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
        if (mpOn) {
            layoutAviso.addView(btnMercadoPago)
        }
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
            Handler(Looper.getMainLooper()).postDelayed({
                loadingLayout.visibility = View.INVISIBLE
            }, 250)
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
                },
                { error ->
                    Toast.makeText(this, "Error al generar pago: ${error.message}", Toast.LENGTH_LONG).show()
                }
            )
            Volley.newRequestQueue(this).add(request)
        }

        btnPaypal.setOnClickListener {
            showLoadingOverlay("redirigiendo...")
            Handler(Looper.getMainLooper()).postDelayed({
                loadingLayout.visibility = View.INVISIBLE
            }, 250)
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
                        val intent = Intent(Intent.ACTION_VIEW, paypalUrl.toUri()).apply {
                            addCategory(Intent.CATEGORY_BROWSABLE)
                            setPackage("com.android.chrome") // fuerza Chrome
                        }
                        try {
                            startActivity(intent)
                        } catch (e: Exception) {
                            // si no hay Chrome, abre con cualquier navegador disponible
                            startActivity(Intent(Intent.ACTION_VIEW, paypalUrl.toUri()))
                        }
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

    private fun getValidFolders(callback: (ArrayList<String>) -> Unit) {
        val databaseRef = firebaseDatabase!!.getReference("version").child("validFolders")
        val listResult = arrayListOf<String>()

        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (folder in snapshot.children) {
                    listResult.add(folder.value.toString())
                }
                callback(listResult)
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

        val gradesList = ArrayList<Bitmap>()

        for (r in 0 until 8) {
            for (c in 0 until 2) {
                val x = c * cellWidth
                val y = r * cellHeight
                val original = Bitmap.createBitmap(bit, x, y, cellWidth, cellHeight)
                val trimmed = trimTransparentEdges(original)
                gradesList.add(trimmed)
            }
        }

        return gradesList
    }

    private fun getGradesDescription(rutaGrades: String): ArrayList<Bitmap> {
        val bit = BitmapFactory.decodeFile(rutaGrades)
        val cellWidth = bit.width
        val cellHeight = bit.height / 8

        val gradesList = ArrayList<Bitmap>()

        for (r in 0 until 8) {
            val y = r * cellHeight
            val original = Bitmap.createBitmap(bit, 0, y, cellWidth, cellHeight)
            val trimmed = trimTransparentEdges(original)
            gradesList.add(trimmed)
        }

        return gradesList
    }

    private fun trimTransparentEdges(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        var top = 0
        var left = 0
        var right = width - 1
        var bottom = height - 1

        loop@ for (y in 0 until height) {
            for (x in 0 until width) {
                if ((source.getPixel(x, y) shr 24) != 0) {
                    top = y
                    break@loop
                }
            }
        }

        loop@ for (y in height - 1 downTo 0) {
            for (x in 0 until width) {
                if ((source.getPixel(x, y) shr 24) != 0) {
                    bottom = y
                    break@loop
                }
            }
        }

        loop@ for (x in 0 until width) {
            for (y in 0 until height) {
                if ((source.getPixel(x, y) shr 24) != 0) {
                    left = x
                    break@loop
                }
            }
        }

        loop@ for (x in width - 1 downTo 0) {
            for (y in 0 until height) {
                if ((source.getPixel(x, y) shr 24) != 0) {
                    right = x
                    break@loop
                }
            }
        }

        // Asegurar que los valores son válidos
        if (right < left || bottom < top) return source

        return Bitmap.createBitmap(source, left, top, right - left + 1, bottom - top + 1)
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
            try {
                // Solo iniciar si no está reproduciendo
                if (!video_fondo.isPlaying) {
                    video_fondo.start()
                }
                if (soundPlayer != null && !soundPlayer!!.isPlaying) {
                    soundPlayer!!.start()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error en onResume al iniciar videos: ${e.message}")
            }
        }
        if(linearDownload.isVisible){
            try {
                if(!bg_download.isPlaying){
                    bg_download.start()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al iniciar bg_download en onResume: ${e.message}")
            }
        }
        listEfectsDisplay.clear()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Liberar MediaPlayers
        if (::mediaPlayerMain.isInitialized) {
            try {
                mediaPlayerMain.release()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al liberar mediaPlayerMain: ${e.message}")
            }
        }

        if (::soundPlayer.isInitialized && soundPlayer != null) {
            try {
                soundPlayer!!.release()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al liberar soundPlayer: ${e.message}")
            }
        }

        // Liberar VideoViews
        if (::video_fondo.isInitialized) {
            try {
                video_fondo.suspend()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al suspender video_fondo: ${e.message}")
            }
        }

        if (::bg_download.isInitialized) {
            try {
                bg_download.suspend()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al suspender bg_download: ${e.message}")
            }
        }

        // Reciclar Bitmaps
        if (::arrayGrades.isInitialized) {
            try {
                arrayGrades.forEach { bitmap ->
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al reciclar arrayGrades: ${e.message}")
            }
        }

        if (::arrGradesDesc.isInitialized) {
            try {
                arrGradesDesc.forEach { bitmap ->
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al reciclar arrGradesDesc: ${e.message}")
            }
        }

        if (::arrGradesDescAbrev.isInitialized) {
            try {
                arrGradesDescAbrev.forEach { bitmap ->
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al reciclar arrGradesDescAbrev: ${e.message}")
            }
        }

        // Reciclar Bitmap del logo
        if (bmLogo != null && !bmLogo!!.isRecycled) {
            try {
                bmLogo!!.recycle()
                bmLogo = null
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al reciclar bmLogo: ${e.message}")
            }
        }

        // Limpiar ImageView
        if (::animLogo.isInitialized) {
            try {
                animLogo.setImageBitmap(null)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al limpiar animLogo: ${e.message}")
            }
        }

        // Cancelar Handlers y Coroutines
        try {
            Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al cancelar handlers: ${e.message}")
        }

        // Limpiar lista de efectos
        try {
            listEfectsDisplay.clear()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al limpiar listEfectsDisplay: ${e.message}")
        }
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

class ObjPuntaje(
    var cancion: String = "",
    var puntaje: String = "",
    var grade: String = "",
    var type: String,
    var player: String,
)

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

interface ItemClickListener {
    fun onItemClick(item: Pair<String, String>)
}
