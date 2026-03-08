package com.fingerdance

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.google.firebase.database.FirebaseDatabase
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

// ========== VARIABLES GLOBALES - USUARIO ==========
var userName = ""
lateinit var firebaseDatabase : FirebaseDatabase
var listGlobalRanking = arrayListOf<Cancion>()
var listAllowDevices = arrayListOf<String>()
var deviceIdFind = ""
var idSala = ""

// ========== VARIABLES GLOBALES - UI ==========
var medidaFlechas = 0f
var heightLayoutBtns = 0f
var heightBtns  = 0f
var widthBtns = 0f
var padPositions = listOf<Array<Float>>()
var padPositionsHD = listOf<Array<Float>>()
var touchAreas = listOf<Array<Float>>()
var colWidth = 0f

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

// ========== FUNCIONES HELPER ==========
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
    source.copyPixelsToBuffer(IntBuffer.wrap(pixels))

    var top = height
    var left = width
    var right = 0
    var bottom = 0

    for (y in 0 until height) {
        for (x in 0 until width) {
            val alpha = pixels[y * width + x] ushr 24
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
