package com.fingerdance

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.fingerdance.ssc.Parser.Chart
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.nio.IntBuffer

// ========== CONSTANTES DE API ==========
const val API_KEY = "AIzaSyCL1ukVSzaKtIZZo3PFqfHXdlWIAxD1hGM"
const val FOLDER_ID = "19cM-WcAJyzo7w-7sbrPUzufMu_-gi9bS"
const val FOLDER_ID_THEMES = "1mqNKLVyhcQ8I7rXOD4z9Sg1Glrv8YVDX"
const val FOLDER_ID_CHANNELS_BGA = "1YAAWzSJw6o2h8G4cj80fle6G45wf937p"
const val KEY_CHANNELS_CACHE = "channels_cache"
const val APK_ID_FILE = "199Y0lsIdAHmLdRWb3ZQJmUPLMl8GjqYM"

// ========== VARIABLES GLOBALES - DRIVE ==========
var listFilesDrive = arrayListOf<Pair<String, String>>()
var listThemesDrive = arrayListOf<Pair<String, String>>()
var listChannelsDrive = mutableListOf<MainActivity.ChannelsDrive>()

var sscSong = Song()
var isSsc = false
var chart = Chart(0.0, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())



var aBatch = 0
var bBatch = 0
// ========== VARIABLES GLOBALES - USUARIO ==========
var userName = ""
lateinit var firebaseDatabase : FirebaseDatabase
var listGlobalRanking = arrayListOf<Cancion>()
var listAllowDevices = arrayListOf<String>()
var deviceIdFind = ""
var idSala = ""

// ========== VARIABLES GLOBALES - UI ==========
var medidaFlechas = 0f
var medidaFlechasHorizontal = 0f
var spaceInitHorizontal = 0f
var heightLayoutBtns = 0f
var heightBtns  = 0f
var widthBtns = 0f
var padPositions = listOf<Array<Float>>()
var padPositionsHD = listOf<Array<Float>>()
var padPositionsHorizontal = listOf<Array<Float>>()
var touchAreasHorizontal = listOf<Array<Float>>()
var areaToPadMap = listOf<Int>()
var heightBtnsHorizontal  = 0f
var widthBtnsHorizontal = 0f
var touchAreas = listOf<Array<Float>>()
var colWidth = 0f

var currentChannel = ""
var currentSong = ""
var currentLevel = ""

var positionCurrentChannel = 0
var isVideo = false

var channelIndex = 0
var songIndex = 0
var levelIndex = 0
var oldValue: Int = 0
var oldValueCommand: Int = 0
var oldValueCommandValues: Int = 0

var currentPathSong: String = ""

var countMiss = 0
var halfDouble = false

lateinit var listSongScores: Array<ObjPuntaje>

// ========== VARIABLES GLOBALES - VERSION ==========
var flagActiveAllows = false
var numberUpdateFirebase = ""
var mpOn : Boolean = false
var resetRegister = false
var paypalOn = false
var startOnline = false
var TIME_ADJUST = 0L
var timeToPresiscionHD = 0L
var timeToPresiscion = 0L
var versionUpdate = ""
lateinit var validFolders : List<String>

// ========== VARIABLES GLOBALES - SETTINGS ==========
lateinit var themes : SharedPreferences
var isMidLine = false
var isCounter = false
var breakSong = true
var isHorizontalMode = false
var skinSelected : String = ""
var speedSelected : String = ""
var valueOffset = 0L
var showPadB : Int = 0
var typePadD : Int = 0
var hideImagesPadA : Boolean = false
var skinPad = ""
var alphaPadB = 1f
var numberUpdateLocal = ""

var rutaGrades = ""
var gradeDescription = ""
var gradeDescriptionAbrev = ""

var ready = 0

// ========== FUNCIONES HELPER ==========

fun listenScoreChannel(canalNombre: String, callback: (ArrayList<Cancion>) -> Unit) {
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

fun isConnectedToInternet(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    } else {
        @Suppress("DEPRECATION")
        val networkInfo = connectivityManager.activeNetworkInfo
        networkInfo != null && networkInfo.isConnected
    }
}

fun getGrades(rutaGrades: String): ArrayList<Bitmap> {
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

fun getGradesDescription(rutaGrades: String): ArrayList<Bitmap> {
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

fun trimTransparentEdges(source: Bitmap): Bitmap {

    val width = source.width
    val height = source.height

    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)

    var top = height
    var left = width
    var right = 0
    var bottom = 0

    for (y in 0 until height) {
        val offset = y * width

        for (x in 0 until width) {
            val alpha = pixels[offset + x] ushr 24

            if (alpha != 0) {
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
    }

    if (right <= left || bottom <= top) return source

    return Bitmap.createBitmap(
        source,
        left,
        top,
        right - left + 1,
        bottom - top + 1
    )
}

data class FirstRank(
    val nombre: String = "---------",
    val puntaje: String = "0",
    val grade: String = "?"
)

data class Nivel(
    val nivel: String = "??",
    val checkedValues: String = "",
    val type: String = "",
    val player: String = "",
    val fisrtRank: ArrayList<FirstRank> = arrayListOf()
)

data class Cancion(
    val cancion: String,
    val niveles: ArrayList<Nivel>
)

data class Canal(
    val canal: String,
    val canciones: ArrayList<Cancion>
)

enum class SaveResult {
    NONE,
    LOCAL,
    LOCAL_AND_FIREBASE,
    INVALID_LEVEL
}
