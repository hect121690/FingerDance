package com.fingerdance

import android.app.Dialog
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import android.text.method.LinkMovementMethod
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.fingerdance.ssc.LoadingSongs
import com.fingerdance.ssc.Parser
import com.google.common.primitives.Ints.min
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.Serializable
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.system.exitProcess
import androidx.core.graphics.drawable.toDrawable
import androidx.core.text.HtmlCompat
import com.google.firebase.database.ChildEventListener

private var descargando = true
var height: Int = 0
var width: Int = 0
var bgaOff : String = ""
val gson = Gson()

lateinit var mediPlayer : MediaPlayer
lateinit var playerSong: PlayerSong
var initGameScreen = false
var positionActualLvs: Int = 0
var displayBPM = 0f
var sizeLvs = 0

var isPlayer1 = true
var isOnline = false

lateinit var mediaPlayer : MediaPlayer
var ruta = ""

var rutaBase = ""

lateinit var salaRef: DatabaseReference
lateinit var activeSala : Sala

lateinit var db : DataBasePlayer

var decimoHeigtn = 0
var decimoWidth = 0
var isOffline = false

var listEfectsDisplay: ArrayList<CommandValues> = ArrayList()

var bgaPathSelectChannel = ""
var bgaPathSelectSong = ""

lateinit var arrayGrades : ArrayList<Bitmap>
lateinit var arrGradesDesc : ArrayList<Bitmap>
lateinit var arrGradesDescAbrev : ArrayList<Bitmap>
lateinit var channelFavorites : Channels
lateinit var listFavorites : ArrayList<Song>
lateinit var mockListChannels : ArrayList<Nivel>

lateinit var bitNR1 : Bitmap
lateinit var bitNR2 : Bitmap
lateinit var bitNR3 : Bitmap

private var bmLogo : Bitmap? = null

lateinit var channel : String

object OnlineRoomExitOverlay {
    private const val OVERLAY_TAG = "online_room_exit_overlay"

    fun show(
        activity: Activity,
        playerName: String,
        onExit: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        val root = activity.window.decorView as? ViewGroup ?: return
        if (root.findViewWithTag<View>(OVERLAY_TAG) != null) return

        val overlay = FrameLayout(activity).apply {
            tag = OVERLAY_TAG
            setBackgroundColor(Color.BLACK)
            isClickable = true
            isFocusable = true
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        val message = TextView(activity).apply {
            text = "El jugador ${playerName.ifBlank { "rival" }} abandonó la sala"
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(Color.WHITE)
            textSize = 22f
        }

        val button = Button(activity).apply {
            text = "SALIR"
            isAllCaps = true
            setOnClickListener {
                isEnabled = false
                onExit()
            }
        }

        content.addView(
            message,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 48 }
        )
        content.addView(
            button,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        overlay.addView(
            content,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        root.addView(overlay)
        overlay.bringToFront()
    }
}

class MainActivity : AppCompatActivity(), Serializable {
    private lateinit var video_fondo : VideoView
    private lateinit var image_fondo : ImageView
    private lateinit var bg_download : VideoView
    private lateinit var mediaPlayerMain : MediaPlayer
    private var soundPlayer : MediaPlayer? = null
    private var currentVideoPosition : Int = 0

    // Fondo principal: usa video si existe fondo.mp4; de lo contrario usa fondo.png con parallax.
    private var usingVideoBackground = false
    private var parallaxEnabled = false

    private lateinit var sensorManager: SensorManager
    private var parallaxSensor: Sensor? = null

    // La primera inclinación detectada se toma como el centro del efecto.
    private var parallaxBaseHorizontalTilt: Float? = null
    private var parallaxBaseForwardTilt: Float? = null

    private var parallaxCurrentX = 0f
    private var parallaxCurrentY = 0f

    // Ajustes del efecto.
    private val parallaxSmoothing = 0.10f
    private val parallaxMaxTiltRadians = Math.toRadians(15.0).toFloat()
    private val parallaxMaxXDp = 20f
    private val parallaxMaxYDp = 24f
    private val parallaxScaleSafety = 0.045f

    private val parallaxSensorListener = object : SensorEventListener {

        override fun onSensorChanged(event: SensorEvent) {
            if (!parallaxEnabled) return

            // TYPE_GRAVITY es la opción principal. Si el teléfono no lo tiene,
            // usamos TYPE_ACCELEROMETER como fallback.
            if (event.sensor.type != Sensor.TYPE_GRAVITY &&
                event.sensor.type != Sensor.TYPE_ACCELEROMETER
            ) {
                return
            }

            val rawX = event.values[0]
            val rawY = event.values[1]
            val rawZ = event.values[2]

            // Convertimos los ejes del sensor a los ejes visuales de la pantalla.
            // Así funciona correctamente tanto en portrait como en landscape.
            val displayRotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display?.rotation ?: Surface.ROTATION_0
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.rotation
            }

            val screenX: Float
            val screenY: Float

            when (displayRotation) {
                Surface.ROTATION_90 -> {
                    screenX = -rawY
                    screenY = rawX
                }

                Surface.ROTATION_180 -> {
                    screenX = -rawX
                    screenY = -rawY
                }

                Surface.ROTATION_270 -> {
                    screenX = rawY
                    screenY = -rawX
                }

                else -> {
                    screenX = rawX
                    screenY = rawY
                }
            }

            /*
             * Horizontal: inclinación izquierda/derecha.
             *
             * Forward: inclinación de la parte superior del teléfono
             * hacia adelante/atrás. Usar atan2(Z, Y) da una respuesta
             * mucho más natural que tomar solamente pitch de getOrientation().
             */
            val horizontalTilt = atan2(
                screenX,
                sqrt((screenY * screenY) + (rawZ * rawZ))
            )

            val forwardTilt = atan2(rawZ, screenY)

            if (parallaxBaseHorizontalTilt == null || parallaxBaseForwardTilt == null) {
                parallaxBaseHorizontalTilt = horizontalTilt
                parallaxBaseForwardTilt = forwardTilt
                return
            }

            val horizontalDelta = angleDelta(
                horizontalTilt,
                parallaxBaseHorizontalTilt!!
            )

            val forwardDelta = angleDelta(
                forwardTilt,
                parallaxBaseForwardTilt!!
            )

            val normalizedX =
                (horizontalDelta / parallaxMaxTiltRadians).coerceIn(-1f, 1f)

            val normalizedY =
                (forwardDelta / parallaxMaxTiltRadians).coerceIn(-1f, 1f)

            val maxX = dpToPx(parallaxMaxXDp)
            val maxY = dpToPx(parallaxMaxYDp)

            // El fondo se mueve en sentido contrario a la inclinación.
            val targetX = -normalizedX * maxX
            val targetY = -normalizedY * maxY

            // Suavizado para evitar vibraciones y movimientos bruscos.
            parallaxCurrentX +=
                (targetX - parallaxCurrentX) * parallaxSmoothing

            parallaxCurrentY +=
                (targetY - parallaxCurrentY) * parallaxSmoothing

            image_fondo.translationX = parallaxCurrentX
            image_fondo.translationY = parallaxCurrentY
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private lateinit var animLogo : ImageView
    private lateinit var btnPlay : Button
    private lateinit var btnPlayOnline : Button
    private lateinit var btnOptions : Button
    private lateinit var btnExit : Button
    private lateinit var loadingLayout: LinearLayout

    private lateinit var linearDownload : ConstraintLayout
    private lateinit var lbDescargando : TextView
    private lateinit var progressBar : ProgressBar

    private var showUpdateView = true

    private lateinit var txtLoadingMessage: TextView
    private lateinit var txtLoadingChannel: TextView
    private lateinit var txtLoadingSong: TextView
    private var waitingPlayer2Listener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        if(isHorizontalMode){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        }else{
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        }
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))
        setContentView(R.layout.activity_main)
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        db = DataBasePlayer(this)
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)

        width = min(metrics.widthPixels, metrics.heightPixels)
        height = max(metrics.widthPixels, metrics.heightPixels)

        decimoWidth = width / 10
        decimoHeigtn = height / 10

        if(tema == ""){
            tema ="default"
        }

        rutaBase = getExternalFilesDir(null)!!.absolutePath

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val json = assets.open("mockChannels.json")
                    .bufferedReader().use { it.readText() }

                val listChannelsJson: ArrayList<Nivel> = Gson().fromJson(
                    json,
                    object : TypeToken<ArrayList<Nivel>>() {}.type
                )

                withContext(Dispatchers.Main) {
                    mockListChannels = listChannelsJson
                }

            } catch (e: Exception) {
                Log.e("MOCK", "Error cargando JSON: ${e.message}")
            }
        }
        playerSong = PlayerSong("","", "",0.0,"", 0.0, "","",false, false,"", "", "")
        mediPlayer = MediaPlayer()
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
            arrayOf(widthBtns, heightLayoutBtns + heightBtns + (heightBtns / 2)), //leftDown
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

        medidaFlechasHorizontal = width / 8f
        //height = 2436 width = 1080
        heightBtnsHorizontal = width / 3.35f               //323
        widthBtnsHorizontal = height * 0.115f               //248
        val initPadB = height - (widthBtnsHorizontal * 3f)

        padPositionsHorizontal = listOf(
            arrayOf(0f, width - heightBtnsHorizontal), // leftDown
            arrayOf(0f, (medidaFlechasHorizontal * 2)), // leftUp
            arrayOf(widthBtnsHorizontal, width - (heightBtnsHorizontal * 1.75f)), // center
            arrayOf((widthBtnsHorizontal * 2f), (medidaFlechasHorizontal * 2)), // rightUp
            arrayOf((widthBtnsHorizontal * 2f), width - heightBtnsHorizontal),  // rightDown

            arrayOf(initPadB, width - heightBtnsHorizontal), // leftDown
            arrayOf(initPadB, (medidaFlechasHorizontal * 2)), // leftUp
            arrayOf(initPadB + widthBtnsHorizontal, width - (heightBtnsHorizontal * 1.75f)), // center
            arrayOf(initPadB + (widthBtnsHorizontal * 2f), (medidaFlechasHorizontal * 2)), // rightUp
            arrayOf(initPadB + (widthBtnsHorizontal * 2f), width - heightBtnsHorizontal)  // rightDown
        )

        val verticalSpace = getVerticalGap(
            padPositionsHorizontal[1], // LU
            padPositionsHorizontal[0], // LD
            heightBtnsHorizontal
        )

        val leftAreas = listOf(
            arrayOf(0f, ((medidaFlechasHorizontal * 2) + heightBtnsHorizontal) + (verticalSpace / 2)),                      //leftDown-UP
            arrayOf(0f, (medidaFlechasHorizontal * 2) + heightBtnsHorizontal),                                              //LefUp-DOWN
            arrayOf(widthBtnsHorizontal, (medidaFlechasHorizontal * 2)),                                                    //LefttUp-Right
            arrayOf(widthBtnsHorizontal + (widthBtnsHorizontal / 2), (medidaFlechasHorizontal * 2)),                        //rightUp-Left
            arrayOf(widthBtnsHorizontal * 2f, (medidaFlechasHorizontal * 2) + heightBtnsHorizontal),                        //rightUp-Down
            arrayOf(widthBtnsHorizontal * 2f, (medidaFlechasHorizontal * 2) + heightBtnsHorizontal) + (verticalSpace / 2),  //rightDown-Up
            arrayOf((widthBtnsHorizontal) * 1.5f, padPositionsHorizontal[2][1] + heightBtnsHorizontal),                     //rightDown-Right
            arrayOf(widthBtnsHorizontal, padPositionsHorizontal[2][1] + heightBtnsHorizontal)                               //leftDown-Right

        )
        val rightAreas = leftAreas.map { area ->
            arrayOf(
                area[0] + initPadB,
                area[1]
            )
        }

        touchAreasHorizontal = leftAreas + rightAreas

        val leftMap = listOf(
            0,
            1,
            1,
            3,
            3,
            4,
            4,
            0
        )

        val rightMap = leftMap.map { it + 5 }
        areaToPadMap = leftMap + rightMap

        padPositionsHorizontalHD = listOf(
            arrayOf((widthBtnsHorizontal * 3f) - widthBtnsHorizontal, width - heightBtnsHorizontal), // leftDown
            arrayOf((widthBtnsHorizontal * 3f) - widthBtnsHorizontal, (medidaFlechasHorizontal * 2)), // leftUp
            arrayOf(widthBtnsHorizontal - widthBtnsHorizontal, width - (heightBtnsHorizontal * 1.75f)), // center
            arrayOf((widthBtnsHorizontal * 2f) - widthBtnsHorizontal, (medidaFlechasHorizontal * 2)), // rightUp
            arrayOf((widthBtnsHorizontal * 2f) - widthBtnsHorizontal, width - heightBtnsHorizontal),  // rightDown

            arrayOf(initPadB + widthBtnsHorizontal, width - heightBtnsHorizontal), // leftDown
            arrayOf(initPadB + widthBtnsHorizontal, (medidaFlechasHorizontal * 2)), // leftUp
            arrayOf(initPadB + widthBtnsHorizontal * 2, width - (heightBtnsHorizontal * 1.75f)), // center
            arrayOf(initPadB , (medidaFlechasHorizontal * 2)), // rightUp
            arrayOf(initPadB , width - heightBtnsHorizontal)  // rightDown
        )

        val verticalSpaceHD = getVerticalGap(
            padPositionsHorizontalHD[1], // LU
            padPositionsHorizontalHD[0], // LD
            heightBtnsHorizontal
        )

        val leftAreasHD = listOf(

            // LD <-> LU
            arrayOf(
                padPositionsHorizontalHD[1][0],
                padPositionsHorizontalHD[1][1] + heightBtnsHorizontal
            ),

            // LU -> CE
            arrayOf(
                padPositionsHorizontalHD[2][0],
                padPositionsHorizontalHD[1][1]
            ),

            // CE -> RU
            arrayOf(
                padPositionsHorizontalHD[2][0] + (widthBtnsHorizontal / 2f),
                padPositionsHorizontalHD[1][1]
            ),

            // RU <-> RD
            arrayOf(
                padPositionsHorizontalHD[3][0],
                padPositionsHorizontalHD[3][1] + heightBtnsHorizontal
            ),

            // CE -> LD
            arrayOf(
                padPositionsHorizontalHD[2][0],
                padPositionsHorizontalHD[2][1] + heightBtnsHorizontal
            ),

            // CE -> RD
            arrayOf(
                padPositionsHorizontalHD[2][0] + (widthBtnsHorizontal / 2f),
                padPositionsHorizontalHD[2][1] + heightBtnsHorizontal
            )
        )

        val rightAreasHD = leftAreasHD.map { area ->

            arrayOf(
                area[0] + initPadB + widthBtnsHorizontal,
                area[1]
            )
        }
        touchAreasHorizontalHD = leftAreasHD + rightAreasHD

        val leftMapHD = listOf(

            0, // LD-LU
            1, // LU-CE left
            3, // CE-RU
            4, // RU-RD
            0, // CE-LD
            4  // CE-RD
        )
        val rightMapHD = leftMapHD.map { it + 5 }

        areaToPadMapHD = leftMapHD + rightMapHD

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
        image_fondo = findViewById(R.id.image_fondo)
        bg_download = findViewById(R.id.bg_download)
        loadingLayout = findViewById(R.id.loadingLayout)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        parallaxSensor =
            sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
                ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        image_fondo.scaleType = ImageView.ScaleType.CENTER_CROP
        image_fondo.visibility = View.GONE
        image_fondo.setBackgroundColor("#020713".toColorInt())

        txtLoadingMessage = findViewById(R.id.txtLoadingMessage)
        txtLoadingChannel = findViewById(R.id.txtLoadingChannel)
        txtLoadingSong = findViewById(R.id.txtLoadingSong)

        txtLoadingChannel.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            16f * resources.displayMetrics.density
        )

        txtLoadingSong.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            15f * resources.displayMetrics.density
        )

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

            /*
            val text = TextView(this@MainActivity).apply {
                text = ""
                setTextColor(Color.WHITE)
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(0, 30, 0, 0)
            }
            */

            addView(progressBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            //addView(text, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }

        val folder = File(getExternalFilesDir(null), "FingerDance")
        if (folder.exists()) {
            lifecycleScope.launch {
                startOnline = true
                creaMain()
            }
        } else {
            creaDescarga()
        }
        if(!isOnline || !isOffline) {
            getEventListenerFirebase()
            firebaseDatabase.getReference("rankings").addChildEventListener(rankingsListener)
        }

        /*
        if (!canWritePublicStorage()) {
            requestManageStoragePermission()
            val acept = true
        }
        */

    }

    private fun canWritePublicStorage(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    private fun requestManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = "package:$packageName".toUri()
            startActivity(intent)
        }
    }


    private fun getEventListenerFirebase() {
        rankingsListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val rankingId = snapshot.key ?: return
                val rankings = arrayListOf<FirstRank>()
                for (rankSnapshot in snapshot.child("firstRank").children) {
                    val nombre = rankSnapshot.child("nombre").getValue(String::class.java) ?: ""
                    val puntaje = rankSnapshot.child("puntaje").getValue(String::class.java) ?: "0"
                    val grade = rankSnapshot.child("grade").getValue(String::class.java) ?: ""
                    rankings.add(FirstRank(nombre, puntaje, grade))
                }

                listGlobalRanking[rankingId] = rankings
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val rankingId = snapshot.key ?: return
                val rankings = arrayListOf<FirstRank>()
                for (rankSnapshot in snapshot.child("firstRank").children) {
                    val nombre = rankSnapshot.child("nombre").getValue(String::class.java) ?: ""
                    val puntaje = rankSnapshot.child("puntaje").getValue(String::class.java) ?: "0"
                    val grade = rankSnapshot.child("grade").getValue(String::class.java) ?: ""

                    rankings.add(FirstRank(nombre, puntaje, grade))
                }
                listGlobalRanking[rankingId] = rankings
                Log.d("FirebaseRanking", "Ranking actualizado: $rankingId")
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val rankingId = snapshot.key ?: return
                listGlobalRanking.remove(rankingId)
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al obtener datos: ${error.message}")
            }
        }
    }

    fun getVerticalGap(topPad: Array<Float>, bottomPad: Array<Float>, height: Float): Float {
        return bottomPad[1] - (topPad[1] + height)
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
                    //themes.edit().putString("versionUpdate", numberUpdate).apply()
                    themes.edit().putString("efects", "").apply()
                    //versionUpdate = numberUpdate
                    Toast.makeText(this@MainActivity, "Pack cargado correctamente", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Error cargando pack: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun creaDescarga() {
        linearDownload.isVisible = true
        bg_download.setVideoURI("android.resource://${packageName}/${R.raw.bg_download}".toUri())
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

        withContext(Dispatchers.Main) {
            lbDescargando.isVisible = true
            progressBar.isVisible = true
        }

        return withContext(Dispatchers.IO) {
            try {
                val url = "https://www.googleapis.com/drive/v3/files/$idDownload?alt=media&key=$API_KEY"

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {

                    withContext(Dispatchers.Main) {
                        showAlertFail(isUpdate)
                    }

                    return@withContext null
                }

                val localFile = File(getExternalFilesDir(null), "FingerDance.$typeFile")

                val totalSize = connection.contentLength
                val buffer = ByteArray(64 * 1024)

                var totalBytes = 0
                var lastProgress = 0

                BufferedInputStream(connection.inputStream).use { input ->
                    FileOutputStream(localFile).use { output ->

                        var bytesRead: Int

                        while (input.read(buffer).also { bytesRead = it } != -1) {

                            output.write(buffer, 0, bytesRead)

                            totalBytes += bytesRead

                            if (totalSize > 0) {

                                val progress = (100.0 * totalBytes / totalSize).toInt()

                                if (progress != lastProgress) {

                                    lastProgress = progress

                                    withContext(Dispatchers.Main) {
                                        progressCallback(progress)
                                    }
                                }
                            }
                        }

                        output.flush()
                    }
                }

                connection.disconnect()

                return@withContext localFile

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
        val songs: ArrayList<SongsDrive>,
    )

    data class SongsDrive(
        val name: String,
        val id: String,
        val videos: ArrayList<VideosDrive>,
    )

    data class VideosDrive(
        val name: String,
        val id: String,
        val size: String,
    )

    private suspend fun creaMain() {
        showLoadingOverlay("Espere por favor...")
        loadingLayout.visibility = View.INVISIBLE

        checkAppVersion()
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionApp = packageInfo.versionName ?: ""
        showUpdateView = themes.getBoolean("showUpdateView", true)
        //themes.edit().putString("efects", "").apply()
        if(versionApp == "3.1.7"){
            deleteOldNoteSkins()
        }
        if(versionApp == "3.1.8" && showUpdateView){
            showUpdateDialog(this)
        }
    }

    private fun deleteOldNoteSkins() {
        val baseDir = getExternalFilesDir(null)

        val pathChannels = "FingerDance/NoteSkins"
        val channels = listOf(
            pathChannels
        )

        channels.forEach { path ->
            val dir = File(baseDir, path)
            if (dir.exists() && dir.isDirectory) {
                val deleted = dir.deleteRecursively()

                if (!deleted) {
                    Log.e("DELETE_DIR", "No se pudo borrar: ${dir.absolutePath}")
                }
            }
        }

    }

    private fun showUpdateDialog(context: Context) {
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_update_notes)
        dialog.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
        val txUpdateMessage = dialog.findViewById<TextView>(R.id.txUpdateMessage)

        txUpdateMessage.text = HtmlCompat.fromHtml(
            context.getString(R.string.message_update),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        txUpdateMessage.movementMethod = LinkMovementMethod.getInstance()
        txUpdateMessage.setLinkTextColor(Color.rgb(0, 191, 255))

        val btnClose = dialog.findViewById<Button>(R.id.btnClose)
        btnClose.setOnClickListener {
            showUpdateView = false
            themes.edit().putBoolean("showUpdateView", showUpdateView).apply()
            dialog.dismiss()
        }

        dialog.show()
    }

    private suspend fun checkAppVersion() {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionApp = packageInfo.versionName ?: ""
        if (versionUpdate == versionApp) {
            checkContentUpdate()
        } else {
            showForceUpdateDialog()
        }
    }

    private suspend fun checkContentUpdate() {
        if (numberUpdateLocal != numberUpdateFirebase) {
            val downloadDialog = WebDownloadDialog(this)
            downloadDialog.show("FingerDance-Update.zip")
            val downloadedFile = iniciarDescargaUpdate { progress ->
                runOnUiThread { downloadDialog.updateProgress(progress) }
            }
            downloadDialog.dismiss()

            if (downloadedFile != null) {
                unzipContent()
            } else {
                Toast.makeText(this, "Error en la descarga", Toast.LENGTH_LONG).show()
                createMain(startOnline)
            }
        } else {
            createMain(startOnline)
        }
    }

    private suspend fun unzipContent() {
        withContext(Dispatchers.IO) {
            val unzip = Unzip(this@MainActivity)
            val rutaZip = getExternalFilesDir("FingerDance.zip").toString()
            unzip.performUnzip(rutaZip, "FingerDance.zip", false) // false = no reiniciar MainActivity
            themes.edit().putString("numberUpdateLocal", numberUpdateFirebase).apply()
            numberUpdateLocal = numberUpdateFirebase
        }

        createMain(startOnline)
    }

    private fun showForceUpdateDialog() {
        linearDownload.isVisible = true
        linearDownload.bringToFront()

        bg_download.visibility = View.VISIBLE
        bg_download.setVideoURI(Uri.parse("android.resource://${packageName}/${R.raw.bg_download}"))
        bg_download.setOnPreparedListener { mp ->
            mp.isLooping = true
            mp.start()
        }
        bg_download.start()
        bg_download.setOnCompletionListener {
            bg_download.start()
        }

        val builder = AlertDialog.Builder(this, R.style.TransparentDialog)
        builder.setTitle("Actualizar Aplicación")
        builder.setMessage("Se requiere una nueva versión de la aplicación.\n\nSe descargará e instalará automáticamente.")
        builder.setCancelable(false)
        builder.setPositiveButton("Descargar") { dialog, which ->
            dialog.dismiss()
            lifecycleScope.launch {
                downloadAndInstallAPK()
            }
        }
        builder.setNegativeButton("Salir") { dialog, which ->
            cerrarApp()
        }
        builder.show()
    }

    private suspend fun downloadAndInstallAPK() {
        val downloadDialog = WebDownloadDialog(this)
        downloadDialog.show("FingerDance.apk")

        val packageApp = iniciarDescargaDrive(
            APK_ID_FILE,
            "apk",
            true
        ) { progress ->
            runOnUiThread { downloadDialog.updateProgress(progress) }
        }

        downloadDialog.dismiss()

        if (packageApp != null) {
            instalarAPK(File(getExternalFilesDir(null), "FingerDance.apk").absolutePath)
        } else {
            linearDownload.isVisible = false
            lbDescargando.isVisible = false
            progressBar.isVisible = false
            Toast.makeText(this, "Error en la descarga del APK", Toast.LENGTH_LONG).show()
        }
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

    private fun createMain(startOnline: Boolean){
        linearDownload.isVisible = false
        lbDescargando.isVisible = false
        progressBar.isVisible = false
        val them = File(getExternalFilesDir(null), "FingerDance/Themes/$tema")
        if (!them.exists()) {
            tema ="default"
        }

        if(AppResources.arrayGrades.isNullOrEmpty()){
            AppResources.arrayGrades = getGrades(rutaGrades)
            AppResources.arrGradesDesc = getGradesDescription(gradeDescription)
            AppResources.arrGradesDescAbrev = getGradesDescription(gradeDescriptionAbrev)
        }

        bitNR1 = BitmapFactory.decodeFile("$rutaBase/FingerDance/Themes/$tema/GraphicsStatics/dance_grade/new_record_1.png")
        bitNR2 = BitmapFactory.decodeFile("$rutaBase/FingerDance/Themes/$tema/GraphicsStatics/dance_grade/new_record_2.png")
        bitNR3 = BitmapFactory.decodeFile("$rutaBase/FingerDance/Themes/$tema/GraphicsStatics/dance_grade/new_record_3.png")

        bgaPathSelectChannel = "$rutaBase/FingerDance/Themes/$tema/BGAs/BgaSelectChannel.mp4"
        bgaPathSelectSong = "$rutaBase/FingerDance/Themes/$tema/BGAs/BgaSelectSong.mp4"

        if(File(bgaPathSelectChannel).isDirectory){
            File(bgaPathSelectChannel).delete()
        }

        btnPlay.foreground = Drawable.createFromPath("$rutaBase/FingerDance/Themes/$tema/GraphicsStatics/play.png")
        btnPlayOnline.foreground = Drawable.createFromPath("$rutaBase/FingerDance/Themes/$tema/GraphicsStatics/play_online.png")

        arrayGrades = AppResources.arrayGrades ?: arrayListOf()
        arrGradesDesc = AppResources.arrGradesDesc ?: arrayListOf()
        arrGradesDescAbrev = AppResources.arrGradesDescAbrev ?: arrayListOf()

        val lpPlay = btnPlay.layoutParams as ConstraintLayout.LayoutParams
        val lpPlayOnline = btnPlayOnline.layoutParams as ConstraintLayout.LayoutParams
        val lpOptions = btnOptions.layoutParams as ConstraintLayout.LayoutParams
        val lpExit = btnExit.layoutParams as ConstraintLayout.LayoutParams

        if (isHorizontalMode) {
            lpPlay.verticalBias = 0.20f
            lpPlayOnline.verticalBias = 0.45f
            lpOptions.verticalBias = 0.70f
            lpExit.verticalBias = 0.95f
        } else {
            lpPlay.verticalBias = 0.40f
            lpPlayOnline.verticalBias = 0.55f
            lpOptions.verticalBias = 0.70f
            lpExit.verticalBias = 0.85f
        }

        if (!startOnline) {
            btnPlayOnline.visibility = View.GONE
            if (isHorizontalMode) {
                lpPlay.verticalBias = 0.25f
                lpOptions.verticalBias = 0.60f
                lpExit.verticalBias = 0.95f
            } else {
                lpPlay.verticalBias = 0.40f
                lpOptions.verticalBias = 0.55f
                lpExit.verticalBias = 0.70f
            }
        }

        btnPlay.layoutParams = lpPlay
        btnPlayOnline.layoutParams = lpPlayOnline
        btnOptions.layoutParams = lpOptions
        btnExit.layoutParams = lpExit

        btnOptions.foreground = Drawable.createFromPath("$rutaBase/FingerDance/Themes/$tema/GraphicsStatics/options.png")
        btnExit.foreground = Drawable.createFromPath("$rutaBase/FingerDance/Themes/$tema/GraphicsStatics/exit.png")

        val sound = MediaPlayer.create(this@MainActivity, Uri.fromFile(File("$rutaBase/FingerDance/Themes/$tema/Sounds/screen_title_music.ogg")))
        soundPlayer = sound
        soundPlayer!!.isLooping = true
        soundPlayer!!.start()

        animLogo = findViewById(R.id.imgLogo)
        //animLogo.layoutParams.height = decimoHeigtn * 2
        animLogo.layoutParams.width = width / 2
        if(isHorizontalMode){
            animLogo.visibility = View.INVISIBLE
        }else{
            animar()
        }

        bmLogo = BitmapFactory.decodeFile("$rutaBase/FingerDance/Themes/$tema/GraphicsStatics/logo.png")
        animLogo.setImageBitmap(bmLogo)
        bgaOff = "$rutaBase/FingerDance/Themes/$tema/Movies/BGA_OFF.mp4"
        mediaPlayerMain = MediaPlayer()
        configureMainBackground()

        val goSound = MediaPlayer.create(this@MainActivity, Uri.fromFile(File("$rutaBase/FingerDance/Themes/$tema/Sounds/hitme.mp3")))
        val animation = AnimationUtils.loadAnimation(this@MainActivity, R.anim.press_button)

        animLogo.setOnLongClickListener {
            val intent = Intent(this@MainActivity, BluetoothLatencyProfilesActivity::class.java)
            startActivity(intent)
            true
        }
        btnPlay.setOnClickListener {
            lifecycleScope.launch {
                goPlay(goSound, animation)
            }
        }

        btnPlayOnline.setOnClickListener {
            showOnlineMode(animation, goSound)
        }

        val goOptionMP = MediaPlayer.create(
            this@MainActivity,
            Uri.fromFile(File(getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/option_sound.mp3").toString()))
        )

        btnOptions.setOnClickListener {
            lifecycleScope.launch {
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

        if(userName == ""){
            ingresaNameUser()
        }else{
            Toast.makeText(this@MainActivity, "Bienvenido $userName", Toast.LENGTH_SHORT).show()
        }
    }

    private fun configureMainBackground() {
        val videoFile = File(
            "$rutaBase/FingerDance/Themes/$tema/BGAs/fondo.mp4"
        )

        val imageFile = File(
            "$rutaBase/FingerDance/Themes/$tema/BGAs/fondo.png"
        )

        when {
            videoFile.exists() && videoFile.isFile -> {
                usingVideoBackground = true
                stopParallax()

                image_fondo.visibility = View.GONE
                image_fondo.setImageDrawable(null)
                image_fondo.translationX = 0f
                image_fondo.translationY = 0f

                video_fondo.visibility = View.VISIBLE
                video_fondo.setVideoPath(videoFile.absolutePath)

                video_fondo.setOnPreparedListener { mp ->
                    mediaPlayerMain = mp
                    mediaPlayerMain.isLooping = true

                    if (currentVideoPosition > 0) {
                        mp.seekTo(currentVideoPosition)
                    }

                    mp.start()
                }

                video_fondo.start()
            }

            imageFile.exists() && imageFile.isFile -> {
                usingVideoBackground = false

                try {
                    video_fondo.stopPlayback()
                } catch (_: Exception) {
                }

                video_fondo.visibility = View.GONE

                image_fondo.visibility = View.VISIBLE
                image_fondo.setBackgroundColor("#020713".toColorInt())
                image_fondo.scaleType = ImageView.ScaleType.CENTER_CROP
                image_fondo.setImageDrawable(
                    Drawable.createFromPath(imageFile.absolutePath)
                )

                resetParallaxCenter()
                configureParallaxScale()
                startParallax()
            }

            else -> {
                usingVideoBackground = false
                stopParallax()

                try {
                    video_fondo.stopPlayback()
                } catch (_: Exception) {
                }

                video_fondo.visibility = View.GONE

                image_fondo.visibility = View.VISIBLE
                image_fondo.setImageDrawable(null)
                image_fondo.setBackgroundColor("#020713".toColorInt())
                image_fondo.scaleX = 1f
                image_fondo.scaleY = 1f
                image_fondo.translationX = 0f
                image_fondo.translationY = 0f

                Log.w(
                    "MainActivity",
                    "No existe fondo.mp4 ni fondo.png para el tema $tema"
                )
            }
        }
    }

    private fun configureParallaxScale() {
        image_fondo.post {
            if (image_fondo.width <= 0 || image_fondo.height <= 0) {
                return@post
            }

            val maxX = dpToPx(parallaxMaxXDp)
            val maxY = dpToPx(parallaxMaxYDp)

            val viewWidth = image_fondo.width.toFloat()
            val viewHeight = image_fondo.height.toFloat()

            val scaleNeededX =
                1f + ((maxX * 2f) / viewWidth)

            val scaleNeededY =
                1f + ((maxY * 2f) / viewHeight)

            val finalScale =
                maxOf(scaleNeededX, scaleNeededY) + parallaxScaleSafety

            image_fondo.scaleX = finalScale
            image_fondo.scaleY = finalScale

            Log.d(
                "MainActivity",
                "Parallax scale: $finalScale | maxX=$maxX maxY=$maxY"
            )
        }
    }

    private fun startParallax() {
        if (usingVideoBackground) return
        if (!::image_fondo.isInitialized) return
        if (image_fondo.visibility != View.VISIBLE) return

        val sensor = parallaxSensor

        if (sensor == null) {
            parallaxEnabled = false
            Log.w(
                "MainActivity",
                "El dispositivo no tiene TYPE_GRAVITY ni TYPE_ACCELEROMETER"
            )
            return
        }

        if (parallaxEnabled) return

        resetParallaxCenter()
        parallaxEnabled = true

        sensorManager.registerListener(
            parallaxSensorListener,
            sensor,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    private fun stopParallax() {
        if (::sensorManager.isInitialized) {
            sensorManager.unregisterListener(parallaxSensorListener)
        }

        parallaxEnabled = false
    }

    private fun resetParallaxCenter() {
        parallaxBaseHorizontalTilt = null
        parallaxBaseForwardTilt = null

        parallaxCurrentX = 0f
        parallaxCurrentY = 0f

        if (::image_fondo.isInitialized) {
            image_fondo.translationX = 0f
            image_fondo.translationY = 0f
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }

    private fun angleDelta(current: Float, base: Float): Float {
        var delta = current - base

        val twoPi = (2.0 * PI).toFloat()
        val pi = PI.toFloat()

        while (delta > pi) delta -= twoPi
        while (delta < -pi) delta += twoPi

        return delta
    }

    private fun goPlay(goSound: MediaPlayer, animation: Animation) {
        isOnline = false

        btnPlay.isEnabled = false
        goSound.start()
        btnPlay.startAnimation(animation)

        showLoadingOverlay("Espere por favor...")

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (themes.getString("allTunes", "").orEmpty().isNotEmpty()) {
                        val jsonListChannels = themes.getString("allTunes", "")
                        listChannels = gson.fromJson(
                            jsonListChannels,
                            object : TypeToken<ArrayList<Channels>>() {}.type
                        )
                    } else {
                        listCommands = loadCommandsForMain()

                        val progress: (String, String) -> Unit = { channel, song ->
                            updateLoadingSongs(
                                channel = channel,
                                song = song
                            )
                        }

                        val listSongsKsf = LoadSongsKsf().getChannels(
                            context = this@MainActivity,
                            onProgress = progress
                        )

                        val listSongsSsc = LoadingSongs().getChannels(
                            c = this@MainActivity,
                            onProgress = progress
                        )

                        listChannels = ArrayList(listSongsKsf + listSongsSsc)

                        themes.edit()
                            .putString("allTunes", gson.toJson(listChannels))
                            .putString("efects", gson.toJson(listCommands))
                            .apply()
                    }

                    if (themes.getString("efects", "").orEmpty().isEmpty()) {
                        listCommands = loadCommandsForMain()

                        themes.edit()
                            .putString("efects", gson.toJson(listCommands))
                            .apply()
                    } else {
                        val jsonListCommands = themes.getString("efects", "")
                        listCommands = gson.fromJson(
                            jsonListCommands,
                            object : TypeToken<ArrayList<Command>>() {}.type
                        )
                    }

                    if (themes.getString("favorites", "").orEmpty().isNotEmpty()) {
                        val jsonListFavorites = themes.getString("favorites", "")

                        listFavorites = gson.fromJson(
                            jsonListFavorites,
                            object : TypeToken<ArrayList<Song>>() {}.type
                        )

                        val pathBannerFavorites = getExternalFilesDir(
                            "/FingerDance/Themes/favorites_banner.png"
                        )!!.absolutePath

                        channelFavorites = Channels(
                            "06-FAVORITES",
                            getString(R.string.favorites_description),
                            pathBannerFavorites,
                            listCanciones = listFavorites
                        )

                        if (listFavorites.isNotEmpty()) {
                            val alreadyExists = listChannels.any { it.nombre == "06-FAVORITES" }

                            if (!alreadyExists) {
                                listChannels.add(channelFavorites)
                            }

                            listChannels.sortBy {
                                it.nombre.substringBefore("-").trim()
                            }
                        }
                    } else {
                        listFavorites = arrayListOf()

                        val pathBannerFavorites = getExternalFilesDir(
                            "/FingerDance/Themes/favorites_banner.png"
                        )!!.absolutePath

                        channelFavorites = Channels(
                            "06-FAVORITES",
                            getString(R.string.favorites_description),
                            pathBannerFavorites,
                            listCanciones = listFavorites
                        )
                    }

                    loadSounds(this@MainActivity)
                }

                mediaPlayerMain.pause()
                soundPlayer?.pause()

                val intent = Intent(this@MainActivity, LoadResourcesActivity::class.java)
                startActivity(intent)
                loadingLayout.visibility = View.INVISIBLE
                btnPlay.isEnabled = true

            } catch (e: Exception) {
                loadingLayout.visibility = View.INVISIBLE
                btnPlay.isEnabled = true

                Toast.makeText(
                    this@MainActivity,
                    "Error al cargar recursos: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun goOption(goOption: MediaPlayer, animation: Animation) {
        btnOptions.isEnabled = false

        btnOptions.startAnimation(animation)
        goOption.start()
        soundPlayer?.pause()

        showLoadingOverlay("Espere por favor...")

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (themes.getString("allTunes", "").orEmpty().isNotEmpty()) {
                        val jsonListChannels = themes.getString("allTunes", "")
                        listChannels = gson.fromJson(
                            jsonListChannels,
                            object : TypeToken<ArrayList<Channels>>() {}.type
                        )
                    } else {
                        listCommands = loadCommandsForMain()

                        val progress: (String, String) -> Unit = { channel, song ->
                            updateLoadingSongs(
                                channel = channel,
                                song = song
                            )
                        }

                        val listSongsKsf = LoadSongsKsf().getChannels(
                            context = this@MainActivity,
                            onProgress = progress
                        )

                        val listSongsSsc = LoadingSongs().getChannels(
                            c = this@MainActivity,
                            onProgress = progress
                        )


                        listChannels = ArrayList(listSongsKsf + listSongsSsc)

                        themes.edit()
                            .putString("allTunes", gson.toJson(listChannels))
                            .putString("efects", gson.toJson(listCommands))
                            .apply()
                    }

                    if (themes.getString("efects", "").orEmpty().isEmpty()) {
                        listCommands = loadCommandsForMain()

                        themes.edit()
                            .putString("efects", gson.toJson(listCommands))
                            .apply()
                    } else {
                        val jsonListCommands = themes.getString("efects", "")
                        listCommands = gson.fromJson(
                            jsonListCommands,
                            object : TypeToken<ArrayList<Command>>() {}.type
                        )
                    }
                }

                val intent = Intent(this@MainActivity, OptionsActivity::class.java)
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                loadingLayout.visibility = View.INVISIBLE
                btnOptions.isEnabled = true

                Toast.makeText(
                    this@MainActivity,
                    "Error al cargar opciones: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadCommandsForMain(): ArrayList<Command> {
        val commands = getFilesCW(this@MainActivity)
        val ordenEspecifico = listOf("-.05", "-.1", "-.5", "-1", "0", "1", ".5", ".1", ".05")
        val ordenMap = ordenEspecifico.withIndex().associate { it.value to it.index }
        commands
            .find { it.descripcion == "Cambiar la velocidad de la nota." }
            ?.listCommandValues
            ?.sortBy { ordenMap[it.value] ?: Int.MAX_VALUE }
        return commands
    }

    private fun showLoadingOverlay(message: String, channel: String = "", song: String = "") {
        loadingLayout.visibility = View.VISIBLE
        txtLoadingMessage.text = message
        txtLoadingChannel.text = channel
        txtLoadingSong.text = song
        txtLoadingChannel.visibility = if (channel.isBlank()) View.INVISIBLE else View.VISIBLE
        txtLoadingSong.visibility = if (song.isBlank()) View.INVISIBLE else View.VISIBLE
    }

    private fun updateLoadingSongs(channel: String, song: String) {
        loadingLayout.post {
            txtLoadingChannel.text = channel
            txtLoadingSong.text = song
            txtLoadingChannel.visibility = if (channel.isBlank()) View.INVISIBLE else View.VISIBLE
            txtLoadingSong.visibility = if (song.isBlank()) View.INVISIBLE else View.VISIBLE
        }
    }

    private fun showOnlineMode(animation: Animation, goSound: MediaPlayer) {
        btnPlayOnline.isEnabled = false
        btnPlayOnline.startAnimation(animation)

        val view = layoutInflater.inflate(R.layout.dialog_online, null)

        val btnCreateRoom = view.findViewById<Button>(R.id.btnCreateRoom)
        val btnJoinRoom = view.findViewById<Button>(R.id.btnJoinRoom)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        val dialog = AlertDialog.Builder(this@MainActivity)
            .setView(view)
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            dialog.window?.apply {
                setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                setLayout(
                    (resources.displayMetrics.widthPixels * 0.92f).toInt(),
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
            }
        }

        dialog.show()

        btnCancel.setOnClickListener {
            dialog.dismiss()
            btnPlayOnline.isEnabled = true
        }

        btnJoinRoom.setOnClickListener {
            dialog.dismiss()
            mostrarJoinSala(goSound)
        }

        btnCreateRoom.setOnClickListener {
            btnCreateRoom.isEnabled = false
            goSound.start()

            isPlayer1 = true
            isOnline = true
            idSala = UUID.randomUUID().toString().substring(0, 8)
            salaRef = firebaseDatabase.getReference("rooms/$idSala")

            val jugador1 = Jugador(
                id = userName,
                conectado = true,
                listo = false
            )

            activeSala = Sala(
                jugador1 = jugador1,
                turno = jugador1.id,
                estado = RoomState.WAITING.name
            )

            salaRef.setValue(activeSala)
                .addOnSuccessListener {
                    // Los resultados deben NO existir hasta que cada DanceGrade
                    // publique el resultado real de la ronda.
                    val cleanInitialResults = hashMapOf<String, Any?>(
                        "jugador1/result" to null,
                        "jugador2/result" to null
                    )

                    salaRef.updateChildren(cleanInitialResults)
                        .addOnSuccessListener {
                            salaRef
                                .child("jugador1/conectado")
                                .onDisconnect()
                                .setValue(false)
                                .addOnCompleteListener {
                                    dialog.dismiss()
                                    mostrarCodigoSala()
                                }
                        }
                        .addOnFailureListener { error ->
                            btnCreateRoom.isEnabled = true
                            isOnline = false
                            btnPlayOnline.isEnabled = true
                            mostrarErrorSala("No se pudo inicializar la sala: ${error.message.orEmpty()}")
                        }
                }
                .addOnFailureListener { error ->
                    btnCreateRoom.isEnabled = true
                    isOnline = false
                    btnPlayOnline.isEnabled = true
                    mostrarErrorSala("No se pudo crear la sala:${error.message.orEmpty()}")
                }
        }
    }

    private fun mostrarJoinSala(goSound: MediaPlayer) {
        val view = layoutInflater.inflate(R.layout.dialog_join_sala, null)

        val editRoomCode = view.findViewById<EditText>(R.id.editRoomCode)
        val btnEnter = view.findViewById<View>(R.id.btnEnterOverlay)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        val dialog = AlertDialog.Builder(this@MainActivity)
            .setView(view)
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            dialog.window?.apply {
                setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                setLayout(
                    (resources.displayMetrics.widthPixels * 0.94f).toInt(),
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
            }
        }

        dialog.show()

        editRoomCode.requestFocus()

        dialog.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        )

        btnCancel.setOnClickListener {
            dialog.dismiss()
            btnPlayOnline.isEnabled = true
        }

        btnEnter.setOnClickListener {
            val roomCode = editRoomCode.text.toString().trim()

            if (roomCode.isEmpty()) {
                mostrarErrorSala("Debes ingresar la clave de la sala")
                return@setOnClickListener
            }

            btnEnter.isEnabled = false
            editRoomCode.isEnabled = false
            goSound.start()

            idSala = roomCode
            salaRef = firebaseDatabase.getReference("rooms/$idSala")
            val jugador2 = Jugador(
                id = userName,
                conectado = true,
                listo = false
            )

            attemptJoinRoom(
                dialog = dialog,
                editRoomCode = editRoomCode,
                btnEnter = btnEnter,
                jugador2 = jugador2,
                retryIfMissing = true
            )
        }
    }

    private fun attemptJoinRoom(
        dialog: AlertDialog,
        editRoomCode: EditText,
        btnEnter: View,
        jugador2: Jugador,
        retryIfMissing: Boolean,
    ) {
        var abortReason = "La sala ya fue ocupada o ya no está disponible"

        salaRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val sala = currentData.getValue(Sala::class.java)

                if (sala == null) {
                    abortReason = "La sala no existe"
                    return Transaction.abort()
                }

                if (sala.estado != RoomState.WAITING.name) {
                    abortReason = "La sala ya no está esperando jugadores"
                    return Transaction.abort()
                }

                if (sala.jugador1.id.isBlank() || !sala.jugador1.conectado) {
                    abortReason = "El jugador 1 ya no está conectado"
                    return Transaction.abort()
                }

                // Si ya hay un ID de Player 2, la posición está ocupada.
                if (sala.jugador2.id.isNotBlank()) {
                    abortReason = "La sala ya tiene un jugador 2"
                    return Transaction.abort()
                }

                // Insertamos solamente los tres campos de Player 2.
                // No tocamos jugador1, turno, canción ni resultados.
                currentData.child("jugador2/id").value = jugador2.id
                currentData.child("jugador2/conectado").value = true
                currentData.child("jugador2/listo").value = false
                currentData.child("estado").value = RoomState.SELECTING.name

                return Transaction.success(currentData)
            }

            override fun onComplete(
                databaseError: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (databaseError != null) {
                    btnEnter.isEnabled = true
                    editRoomCode.isEnabled = true
                    mostrarErrorSala("Error al unir a la sala: ${databaseError.message.orEmpty()}")
                    return
                }

                if (!committed) {
                    btnEnter.isEnabled = true
                    editRoomCode.isEnabled = true
                    mostrarErrorSala(abortReason)
                    return
                }

                val sala = currentData?.getValue(Sala::class.java)
                if (sala == null) {
                    btnEnter.isEnabled = true
                    editRoomCode.isEnabled = true
                    mostrarErrorSala("No se pudo leer la sala después de entrar")
                    return
                }

                isPlayer1 = false
                isOnline = true
                activeSala = sala

                // Player 2 queda conectado en la transacción. Registramos
                // onDisconnect antes de abrir el flujo de juego.
                salaRef.child("jugador2/conectado")
                    .onDisconnect()
                    .setValue(false)
                    .addOnCompleteListener {
                        dialog.dismiss()
                        prepareOnlineAndOpenSelectChannel()
                    }
            }
        })
    }

    private fun mostrarErrorSala(message: String) {
        val view = layoutInflater.inflate(R.layout.dialog_sala_not_found, null)

        val txtMessage = view.findViewById<TextView>(R.id.txtMessage)
        val btnAccept = view.findViewById<Button>(R.id.btnAccept)

        txtMessage.text = message

        val dialog = AlertDialog.Builder(this@MainActivity)
            .setView(view)
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            dialog.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(
                    (resources.displayMetrics.widthPixels * 0.90f).toInt(),
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
            }
        }

        dialog.show()

        btnAccept.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun prepareOnlineAndOpenSelectChannel() {
        getSelectChannel = false
        showLoadingOverlay("Espere por favor...")
        try {
            val jsonListChannels = themes.getString("allTunes", "")
            listChannels = gson.fromJson(
                jsonListChannels,
                object : TypeToken<ArrayList<Channels>>() {}.type
            )
            listChannelsOnline = listChannels.filter { it.nombre in validFolders } as ArrayList<Channels>

            if (themes.getString("efects", "").orEmpty().isEmpty()) {
                listCommands = getFilesCW(this@MainActivity)
                val ordenEspecifico = listOf("-.05", "-.1", "-.5", "-1", "0", "1",".5", ".1", ".05")
                val ordenMap = ordenEspecifico.withIndex().associate { it.value to it.index }

                listCommands.find { it.descripcion == "Cambiar la velocidad de la nota." }
                    ?.listCommandValues
                    ?.sortBy {
                        ordenMap[it.value] ?: Int.MAX_VALUE
                    }

                themes.edit().putString("efects", gson.toJson(listCommands)).apply()
            } else {
                val jsonListCommands = themes.getString("efects", "")
                listCommands = gson.fromJson(
                    jsonListCommands,
                    object : TypeToken<ArrayList<Command>>() {}.type
                )
            }

            loadSounds(this@MainActivity)

            loadingLayout.visibility = View.INVISIBLE

            val intent = Intent(this@MainActivity, LoadResourcesActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            mediaPlayerMain.pause()
            soundPlayer?.pause()
            btnPlayOnline.isEnabled = true

        } catch (e: Exception) {
            loadingLayout.visibility = View.INVISIBLE
            btnPlayOnline.isEnabled = true
            Toast.makeText(this@MainActivity, "Error al cargar modo online: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun instalarAPK(filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(this, "Archivo APK no encontrado", Toast.LENGTH_LONG).show()
                return
            }

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                androidx.core.content.FileProvider.getUriForFile(
                    this,
                    "$packageName.provider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            startActivity(intent)


        } catch (e: Exception) {
            Toast.makeText(this, "Error al instalar APK: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun cerrarApp() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        exitProcess(0)
    }

    private fun mostrarCodigoSala() {
        val view = layoutInflater.inflate(R.layout.dialog_create_sala, null)

        val txtRoomCode = view.findViewById<TextView>(R.id.txtRoomCode)
        val btnShare = view.findViewById<View>(R.id.btnShareOverlay)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)

        txtRoomCode.text = idSala

        val dialog = AlertDialog.Builder(this@MainActivity)
            .setView(view)
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            dialog.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(
                    (resources.displayMetrics.widthPixels * 0.94f).toInt(),
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
            }
        }

        dialog.show()

        btnShare.setOnClickListener {
            dialog.dismiss()

            // El host entra de inmediato al flujo Online. SelectChannelOnline
            // mostrará "Esperando al jugador 2" mientras la sala siga WAITING.
            prepareOnlineAndOpenSelectChannel()
            mostrarDialogoCompartir(this)
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
            cancelCreatedRoom()
        }
    }

    private fun waitForPlayer2() {
        stopWaitingPlayer2Listener()

        val view = layoutInflater.inflate(R.layout.dialog_wait_player, null)

        val txtWaitingMessage = view.findViewById<TextView>(R.id.txtWaitingMessage)
        val txtWaitingRoomCode = view.findViewById<TextView>(R.id.txtWaitingRoomCode)
        val btnCancelRoom = view.findViewById<Button>(R.id.btnCancelRoom)

        txtWaitingRoomCode.text = idSala

        val waitingDialog = AlertDialog.Builder(this@MainActivity)
            .setView(view)
            .setCancelable(false)
            .create()

        waitingDialog.setOnShowListener {
            waitingDialog.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(
                    (resources.displayMetrics.widthPixels * 0.90f).toInt(),
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
            }
        }

        waitingDialog.show()

        btnCancelRoom.setOnClickListener {
            stopWaitingPlayer2Listener()
            waitingDialog.dismiss()
            cancelCreatedRoom()
        }

        waitingPlayer2Listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sala = snapshot.getValue(Sala::class.java)

                if (sala == null) {
                    stopWaitingPlayer2Listener()
                    waitingDialog.dismiss()

                    isOnline = false
                    btnPlayOnline.isEnabled = true
                    Toast.makeText(this@MainActivity, "La sala ya no existe", Toast.LENGTH_SHORT).show()
                    return
                }

                activeSala = sala

                val player2Connected = sala.jugador2.id.isNotBlank() && sala.jugador2.conectado

                if (!player2Connected) {
                    txtWaitingMessage.text = "Esperando a que el jugador 2 entre a la sala..."
                    return
                }
                txtWaitingMessage.text = "${sala.jugador2.id} se ha unido a la sala"
                // Si Player 2 ya está conectado pero la sala sigue WAITING,
                // el host la cambia a SELECTING.
                if (sala.estado == RoomState.WAITING.name) {
                    salaRef.child("estado").setValue(RoomState.SELECTING.name)
                    return
                }

                if (sala.estado != RoomState.SELECTING.name) {
                    return
                }

                stopWaitingPlayer2Listener()
                waitingDialog.dismiss()

                prepareOnlineAndOpenSelectChannel()
            }

            override fun onCancelled(error: DatabaseError) {
                stopWaitingPlayer2Listener()
                waitingDialog.dismiss()

                isOnline = false
                btnPlayOnline.isEnabled = true
                mostrarErrorSala("Error esperando al jugador 2:\n${error.message}")
            }
        }
        salaRef.addValueEventListener(waitingPlayer2Listener!!)
    }

    private fun stopWaitingPlayer2Listener() {
        val listener = waitingPlayer2Listener ?: return

        if (::salaRef.isInitialized) {
            salaRef.removeEventListener(listener)
        }

        waitingPlayer2Listener = null
    }

    private fun cancelCreatedRoom() {
        stopWaitingPlayer2Listener()

        if (::salaRef.isInitialized) {
            salaRef.child("jugador1/conectado").onDisconnect().cancel()
            salaRef.removeValue()
        }

        isOnline = false
        btnPlayOnline.isEnabled = true
    }

    private fun mostrarDialogoCompartir(context: Context) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, idSala)
        }

        context.startActivity(
            Intent.createChooser(
                intent,
                "Compartir con"
            )
        )
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

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

        stopParallax()

        if (usingVideoBackground && ::mediaPlayerMain.isInitialized) {
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
        } else {
            soundPlayer?.let {
                try {
                    if (it.isPlaying) it.pause()
                } catch (_: Exception) {
                }
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

        if (usingVideoBackground && ::mediaPlayerMain.isInitialized) {
            try {
                if (!video_fondo.isPlaying) {
                    video_fondo.start()
                }
                if (soundPlayer != null && !soundPlayer!!.isPlaying) {
                    soundPlayer!!.start()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error en onResume al iniciar video: ${e.message}")
            }
        } else if (!usingVideoBackground && ::image_fondo.isInitialized && image_fondo.visibility == View.VISIBLE) {
            resetParallaxCenter()
            startParallax()

            try {
                if (soundPlayer != null && !soundPlayer!!.isPlaying) {
                    soundPlayer!!.start()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error en onResume al iniciar audio: ${e.message}")
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
        stopWaitingPlayer2Listener()
        stopParallax()
        super.onDestroy()

        // Liberar MediaPlayers
        if (::mediaPlayerMain.isInitialized) {
            try {
                mediaPlayerMain.release()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al liberar mediaPlayerMain: ${e.message}")
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
        firebaseDatabase.getReference("rankings").removeEventListener(rankingsListener)
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
    var checkedValues: String,
    var puntaje: String = "",
    var grade: String = "",
)

enum class RoomState {
    WAITING,
    SELECTING,
    PLAYING,
    RESULTS,
    CLOSED
}

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
    var conectado: Boolean = false,
    var listo: Boolean = false,
    var live: LiveResult = LiveResult(),
    var result: Resultado = Resultado(),
)

data class LiveResult(
    var score: Int = 0,
)

data class Sala(
    var cancion: CancionOnline = CancionOnline(),
    var jugador1: Jugador = Jugador(),
    var jugador2: Jugador = Jugador(),
    var turno: String = "",
    var date: String = "",
    var estado: String = RoomState.WAITING.name,
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

