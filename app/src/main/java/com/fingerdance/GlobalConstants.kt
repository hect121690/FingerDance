package com.fingerdance

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.fingerdance.ssc.Parser.Chart
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.File
import java.io.FileInputStream
import java.nio.IntBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

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

var chart = Chart(
    offset = 0.0,
    bpms = emptyList(),
    tickcounts = emptyList(),
    stops = emptyList(),
    warps = emptyList(),
    fakes = emptyList(),
    speeds = emptyList(),
    scrolls = emptyList(),
    notes = emptyList(),
    extendedNotes = emptyList()
)

var indexCanalSsc = -1
var indexCancionSsc = -1
var indexNivelSsc = -1

var listChannelsSsc: ArrayList<Channels> = arrayListOf()
var listCancionesSsc: ArrayList<Song> = arrayListOf()

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


lateinit var soundPoolSelectSong: SoundPool
var selectSong_movKsf : Int = 0
var selectSong_backKsf : Int = 0
var up_SelectSoundKsf : Int = 0
var move_lvsKsf : Int = 0

var rank_sound : Int = 0
var new_record : Int = 0
var sound_mine : Int = 0
var sss_rank : Int = 0
var ss_rank : Int = 0
var s_rank : Int = 0
var a_rank : Int = 0
var b_rank : Int = 0
var c_rank : Int = 0
var d_rank : Int = 0
var f_rank : Int = 0

var sss_rankB : Int = 0
var ss_rankB : Int = 0
var s_rankB : Int = 0
var a_rankB : Int = 0
var b_rankB : Int = 0
var c_rankB : Int = 0
var d_rankB : Int = 0
var f_rankB : Int = 0

var perfect_game : Int = 0
var full_combo : Int = 0
var no_miss : Int = 0

var st_zero : Int = 0
var nx_nxAbs: Int = 0
var fiesta_fiesta2: Int = 0
var prime: Int = 0
var prime2: Int = 0
var aniversary_xx: Int = 0
var phoenix: Int = 0


var command_switchKsf : Int = 0
var command_backKsf : Int = 0
var command_moveKsf : Int = 0
var command_modKsf : Int = 0

var selectKsf : Int = 0
var startKsf : Int = 0

var tick : Int = 0

var listChannels : ArrayList<Channels> = ArrayList()
var listChannelsOnline : ArrayList<Channels> = ArrayList()
var listCommands : ArrayList<Command> = ArrayList()

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


fun getFilesCW(c: Context) : ArrayList<Command>{
    val rutaImgsCommands = c.getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/command_window/commands").toString()
    val listRutaImgs = ArrayList<String>()
    val listDescripciones = ArrayList<String>()
    val listCommands = ArrayList<Command>()
    var rutaImagenNS = ""

    val dirCommands = File(rutaImgsCommands)
    val info = readFile("$rutaImgsCommands/info.txt")
    val arr = info.split("\r\n").toTypedArray()
    dirCommands.walkTopDown().forEach {
        if (it.isDirectory) {
            if (it.toString() != c.getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/command_window/commands").toString()) {
                if(it.toString().contains("noteskin", ignoreCase = true)){
                    rutaImagenNS = it.toString()
                    listRutaImgs.add(c.getExternalFilesDir("/FingerDance/NoteSkins").toString())
                }else {
                    listRutaImgs.add(it.toString() + ".png")
                }
            }
        }
        listRutaImgs.sortBy { it }
    }
    listDescripciones.addAll(arr)

    var value = ""
    var rutaCommandValue: String

    for(index in 0 until listRutaImgs.size){
        val dirValues = File(listRutaImgs[index].replace(".png", "", true))
        val listCommandsValues = ArrayList<CommandValues>()
        var arrValues = emptyArray<String>()
        if (!dirValues.toString().contains("noteskin", ignoreCase = true)){
            var position = 0
            dirValues.walkTopDown().forEach {
                if(it.toString().endsWith(".png", true)){
                    if(isFileExists(File("$dirValues/info.txt"))){
                        val infoValues = readFile("$dirValues/info.txt")
                        arrValues = infoValues.split("\r\n").toTypedArray()
                    }
                    value = it.name.replace(".png", "", ignoreCase = true)
                    rutaCommandValue = it.toString()
                    var descripcion = ""
                    if(value.matches(Regex(".*[0-9].*")) && !dirValues.toString().contains("offset")){
                        if(value.contains("-")){
                            descripcion = "Disminuir velocidad "
                        }
                        if(!value.contains("-") && value != "0"){
                            descripcion = "Aumentar velocidad "
                        }
                        if(value == "0"){
                            descripcion = "Reiniciar velocidad "
                        }
                    }else{
                        if(isFileExists(File("$dirValues/info.txt"))){
                            descripcion = arrValues[position]
                            position++
                        }
                        if(dirValues.toString().contains("offset")){
                            if(value.contains("-")){
                                descripcion = "Disminuir STARTTIME "
                            }
                            if(!value.contains("-") && value != "0"){
                                descripcion = "Aumentar STARTTIME "
                            }
                            if(value == "0"){
                                descripcion = "Reiniciar a original "
                            }
                        }
                    }
                    listCommandsValues.add(CommandValues(value,descripcion, rutaCommandValue))
                }
            }
            reorderDescriptions(listCommandsValues)
            if(value.matches(Regex(".*[0-9].*"))){
                for (i in 1 until listCommandsValues.size) {
                    for (j in 0 until listCommandsValues.size - i) {
                        val lineaActual = listCommandsValues[j].value
                        val lineaPosterior = listCommandsValues[j + 1].value
                        if (lineaActual.toDouble() > lineaPosterior.toDouble()) {
                            val aux = listCommandsValues[j]
                            listCommandsValues.set(j, listCommandsValues[j + 1])
                            listCommandsValues.set(j + 1, aux)
                        }
                    }
                }
            }

            listCommands.add(Command(
                listRutaImgs[index].removeRange(0, 117).replace(".png", ""),
                listDescripciones[index],
                listRutaImgs[index].replace(".png", ""), listCommandsValues)
            )
        }else{
            if (dirValues.toString().contains("noteskin", ignoreCase = true)){
                dirValues.walkTopDown().forEach {
                    if(it.isDirectory){
                        if(it.toString()!= dirValues.toString()){
                            value = it.toString().removeRange(0, dirValues.toString().length + 1)
                        }
                    }
                    if(it.toString().contains("_Icon", ignoreCase = true)){
                        //val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        //BitmapFactory.decodeFile(it.path, options)
                        val descripcion = "Usar $value NoteSkin"
                        rutaCommandValue = it.toString()
                        //if (options.outWidth == 128 || options.outWidth == 64) {
                        listCommandsValues.add(CommandValues(value, descripcion, rutaCommandValue))
                        //}else{
                        //    listNoteSkinAdditionals.add(CommandValues(value, descripcion, rutaCommandValue))
                        //}
                    }
                }
                listCommands.add(Command(dirValues.toString(), listDescripciones[index], rutaImagenNS, listCommandsValues))
            }
        }
    }
    //themes.edit().putString("listNoteSkinAdditionals", gson.toJson(listNoteSkinAdditionals)).apply()
    return listCommands
}

fun loadSounds(c: Context) {
    val audioAttributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_GAME)
        .build()

    soundPoolSelectSong = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(audioAttributes)
        .build()


    val pathSounds = c.getExternalFilesDir("/FingerDance/Themes/$tema/Sounds").toString()

    val pathChannelmov = File("$pathSounds/sound_navegation.mp3")
    val decriptorChannelMov = FileInputStream(pathChannelmov).fd
    selectSong_movKsf = soundPoolSelectSong.load(decriptorChannelMov, 0, pathChannelmov.length(), 1)

    val pathChannelBack = File("$pathSounds/sound_back.ogg")
    val decriptorChannelBack = FileInputStream(pathChannelBack).fd
    selectSong_backKsf = soundPoolSelectSong.load(decriptorChannelBack, 0, pathChannelBack.length(), 1)

    val pathUpSound = File("$pathSounds/up_sound.ogg")
    val decriptorUpSound = FileInputStream(pathUpSound).fd
    up_SelectSoundKsf = soundPoolSelectSong.load(decriptorUpSound, 0, pathUpSound.length(), 1)

    val pathMoveLv = File("$pathSounds/move_lvs.mp3")
    val decriptorMoveLv = FileInputStream(pathMoveLv).fd
    move_lvsKsf = soundPoolSelectSong.load(decriptorMoveLv, 0, pathMoveLv.length(), 1)

    val pathSwitch = File("$pathSounds/command_switch.mp3")
    val decriptorSwitsh = FileInputStream(pathSwitch).fd
    command_switchKsf = soundPoolSelectSong.load(decriptorSwitsh, 0, pathSwitch.length(), 1)

    val pathBack = File("$pathSounds/command_back.mp3")
    val decriptorBack = FileInputStream(pathBack).fd
    command_backKsf = soundPoolSelectSong.load(decriptorBack,0 , pathBack.length(), 1)

    val pathMove = File("$pathSounds/command_move.mp3")
    val decriptorMove = FileInputStream(pathMove).fd
    command_moveKsf = soundPoolSelectSong.load(decriptorMove, 0, pathMove.length(), 1)

    val pathMod = File("$pathSounds/command_mod.mp3")
    val decriptorMod = FileInputStream(pathMod).fd
    command_modKsf = soundPoolSelectSong.load(decriptorMod, 0, pathMod.length(), 1)

    val pathSelect = File("$pathSounds/select.ogg")
    val decriptorSelect = FileInputStream(pathSelect).fd
    selectKsf = soundPoolSelectSong.load(decriptorSelect, 0, pathSelect.length(), 1)

    val pathStart = File("$pathSounds/start.ogg")
    val decriptorStart = FileInputStream(pathStart).fd
    startKsf = soundPoolSelectSong.load(decriptorStart, 0, pathStart.length(), 1)

    val pathTick = File("$pathSounds/TICK.ogg")
    val decriptorTick = FileInputStream(pathTick).fd
    tick = soundPoolSelectSong.load(decriptorTick, 0, pathTick.length(), 1)

    val pathRankSound = File("$pathSounds/Evaluation/rank_sound.ogg")
    val decriptorRankSound = FileInputStream(pathRankSound).fd
    rank_sound = soundPoolSelectSong.load(decriptorRankSound, 0, pathRankSound.length(), 1)

    val pathNewRecord = File("$pathSounds/Evaluation/new_record.ogg")
    val decriptorNewRecord = FileInputStream(pathNewRecord).fd
    new_record = soundPoolSelectSong.load(decriptorNewRecord, 0, pathNewRecord.length(), 1)

    val pathMine = File(c.getExternalFilesDir("/FingerDance/NoteSkins/Player mine.ogg").toString())
    val decriptorMine = FileInputStream(pathMine).fd
    sound_mine = soundPoolSelectSong.load(decriptorMine, 0, pathMine.length(), 1)

    getRank(pathSounds)
    getRankB(pathSounds)
    getNavigationSounds(pathSounds)
    getSoundEndSong(pathSounds)
}

private fun readFile(path: String): String {
    val encoded = Files.readAllBytes(Paths.get(path))
    return String(encoded, StandardCharsets.UTF_8)
}

private fun isFileExists(file: File): Boolean {
    return file.exists() && !file.isDirectory
}

private fun reorderDescriptions(list: List<CommandValues>) {
    val descEspejo = list.find { it.descripcion.equals("Las notas cambian su posicion a modo espejo.", ignoreCase = true) }?.descripcion
    val descAleatorio = list.find { it.descripcion.equals("Las notas cambian aleatoriamente.", ignoreCase = true)}?.descripcion

    val descAparecen = list.find { it.descripcion.equals("Las notas aparecen desde la mitad de la pantalla.", ignoreCase = true)}?.descripcion
    val descBGAOFF = list.find { it.descripcion.contains("Jugar sin BGA.", ignoreCase = true)}?.descripcion
    val descBGADark = list.find { it.descripcion.contains("Obscurecer BGA", ignoreCase = true)}?.descripcion
    val descSecuencia = list.find { it.descripcion.contains("Zona de secuencia invisible.", ignoreCase = true)}?.descripcion
    val descDesaparecen  = list.find { it.descripcion.contains("Las notas desaparecen a la mitad de la pantalla.", ignoreCase = true)}?.descripcion

    list.forEach {
        when (it.value) {
            "M" -> if (descEspejo != null) it.descripcion = descEspejo
            "RS" -> if (descAleatorio != null) it.descripcion = descAleatorio
            "AP" -> if (descAparecen != null) it.descripcion = descAparecen
            "BGAOFF" -> if (descBGAOFF != null) it.descripcion = descBGAOFF
            "BGADARK" -> if (descBGADark != null) it.descripcion = descBGADark
            "FD" -> if (descSecuencia != null) it.descripcion = descSecuencia
            "V" -> if (descDesaparecen != null) it.descripcion = descDesaparecen
        }
    }
}

private fun getNavigationSounds(pathSounds: String){
    val pathStZero = File("$pathSounds/st_zero.mp3")
    val decriptorStZero = FileInputStream(pathStZero).fd
    st_zero = soundPoolSelectSong.load(decriptorStZero, 0, pathStZero.length(), 1)

    val pathNxNxAbs = File("$pathSounds/nx_nx_abs.mp3")
    val decriptorNxNxAbs = FileInputStream(pathNxNxAbs).fd
    nx_nxAbs = soundPoolSelectSong.load(decriptorNxNxAbs, 0, pathNxNxAbs.length(), 1)

    val pathFiestaFiesta2 = File("$pathSounds/fiesta_fiesta2.mp3")
    val decriptorFiestaFiesta2 = FileInputStream(pathFiestaFiesta2).fd
    fiesta_fiesta2 = soundPoolSelectSong.load(decriptorFiestaFiesta2, 0, pathFiestaFiesta2.length(), 1)

    val pathPrime = File("$pathSounds/prime.mp3")
    val decriptorPrime = FileInputStream(pathPrime).fd
    prime = soundPoolSelectSong.load(decriptorPrime, 0, pathPrime.length(), 1)

    val pathPrime2 = File("$pathSounds/prime2.mp3")
    val decriptorPrime2 = FileInputStream(pathPrime2).fd
    prime2 = soundPoolSelectSong.load(decriptorPrime2, 0, pathPrime2.length(), 1)

    val pathAniversaryXX = File("$pathSounds/aniversary_xx.mp3")
    val decriptorAniversaryXX = FileInputStream(pathAniversaryXX).fd
    aniversary_xx = soundPoolSelectSong.load(decriptorAniversaryXX, 0, pathAniversaryXX.length(), 1)

    val pathPhoenix = File("$pathSounds/phoenix.mp3")
    val decriptorPhoenix = FileInputStream(pathPhoenix).fd
    phoenix = soundPoolSelectSong.load(decriptorPhoenix, 0, pathPhoenix.length(), 1)
}

private fun getRank(pathSounds: String) {
    val pathRankSSS = File("$pathSounds/Evaluation/sss.ogg")
    val decriptorRankSSS = FileInputStream(pathRankSSS).fd
    sss_rank = soundPoolSelectSong.load(decriptorRankSSS, 0, pathRankSSS.length(), 1)

    val pathRankSS = File("$pathSounds/Evaluation/ss.ogg")
    val decriptorRankSS = FileInputStream(pathRankSS).fd
    ss_rank = soundPoolSelectSong.load(decriptorRankSS, 0, pathRankSS.length(), 1)

    val pathRankS = File("$pathSounds/Evaluation/s.ogg")
    val decriptorRankS = FileInputStream(pathRankS).fd
    s_rank = soundPoolSelectSong.load(decriptorRankS, 0, pathRankSSS.length(), 1)

    val pathRanka = File("$pathSounds/Evaluation/a.ogg")
    val decriptorRanka = FileInputStream(pathRanka).fd
    a_rank = soundPoolSelectSong.load(decriptorRanka, 0, pathRanka.length(), 1)

    val pathRankb = File("$pathSounds/Evaluation/b.ogg")
    val decriptorRankb = FileInputStream(pathRankb).fd
    b_rank = soundPoolSelectSong.load(decriptorRankb, 0, pathRankb.length(), 1)

    val pathRankc = File("$pathSounds/Evaluation/c.ogg")
    val decriptorRankc = FileInputStream(pathRankc).fd
    c_rank = soundPoolSelectSong.load(decriptorRankc, 0, pathRankc.length(), 1)

    val pathRankd = File("$pathSounds/Evaluation/d.ogg")
    val decriptorRankd = FileInputStream(pathRankd).fd
    d_rank = soundPoolSelectSong.load(decriptorRankd, 0, pathRankd.length(), 1)

    val pathRankf = File("$pathSounds/Evaluation/f.ogg")
    val decriptorRankf = FileInputStream(pathRankf).fd
    f_rank = soundPoolSelectSong.load(decriptorRankf, 0, pathRankf.length(), 1)
}

private fun getRankB(pathSounds: String) {
    val pathRankSSS = File("$pathSounds/Evaluation/sssB.ogg")
    val decriptorRankSSS = FileInputStream(pathRankSSS).fd
    sss_rankB = soundPoolSelectSong.load(decriptorRankSSS, 0, pathRankSSS.length(), 1)

    val pathRankSS = File("$pathSounds/Evaluation/ssB.ogg")
    val decriptorRankSS = FileInputStream(pathRankSS).fd
    ss_rankB = soundPoolSelectSong.load(decriptorRankSS, 0, pathRankSS.length(), 1)

    val pathRankS = File("$pathSounds/Evaluation/sB.ogg")
    val decriptorRankS = FileInputStream(pathRankS).fd
    s_rankB = soundPoolSelectSong.load(decriptorRankS, 0, pathRankSSS.length(), 1)

    val pathRanka = File("$pathSounds/Evaluation/aB.ogg")
    val decriptorRanka = FileInputStream(pathRanka).fd
    a_rankB = soundPoolSelectSong.load(decriptorRanka, 0, pathRanka.length(), 1)

    val pathRankb = File("$pathSounds/Evaluation/bB.ogg")
    val decriptorRankb = FileInputStream(pathRankb).fd
    b_rankB = soundPoolSelectSong.load(decriptorRankb, 0, pathRankb.length(), 1)

    val pathRankc = File("$pathSounds/Evaluation/cB.ogg")
    val decriptorRankc = FileInputStream(pathRankc).fd
    c_rankB = soundPoolSelectSong.load(decriptorRankc, 0, pathRankc.length(), 1)

    val pathRankd = File("$pathSounds/Evaluation/dB.ogg")
    val decriptorRankd = FileInputStream(pathRankd).fd
    d_rankB = soundPoolSelectSong.load(decriptorRankd, 0, pathRankd.length(), 1)

    val pathRankf = File("$pathSounds/Evaluation/fB.ogg")
    val decriptorRankf = FileInputStream(pathRankf).fd
    f_rankB = soundPoolSelectSong.load(decriptorRankf, 0, pathRankf.length(), 1)
}

private fun getSoundEndSong(pathSounds: String){
    val pathPerfectGame = File("$pathSounds/perfect_game.mp3")
    val decriptorPerfectGame = FileInputStream(pathPerfectGame).fd
    perfect_game = soundPoolSelectSong.load(decriptorPerfectGame, 0, pathPerfectGame.length(), 1)

    val pathFullcombo = File("$pathSounds/full_combo.mp3")
    val decriptorFullcombo = FileInputStream(pathFullcombo).fd
    full_combo = soundPoolSelectSong.load(decriptorFullcombo, 0, pathFullcombo.length(), 1)

    val pathNoMiss = File("$pathSounds/no_miss.mp3")
    val decriptorNoMiss = FileInputStream(pathNoMiss).fd
    no_miss = soundPoolSelectSong.load(decriptorNoMiss, 0, pathNoMiss.length(), 1)
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
