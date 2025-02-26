package com.fingerdance

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.mobileffmpeg.FFmpeg
import java.io.File
import java.io.FileInputStream
import java.lang.Double.parseDouble
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

private lateinit var channels: Channels
private lateinit var songs :Song


//var barLife1 : Drawable? = null
//var barLife : Drawable? = null

private lateinit var soundPoolSelectSong: SoundPool

private var selectSong_mov : Int = 0
private var selectSong_back : Int = 0
private var up_SelectSound : Int = 0
private var move_lvs : Int = 0

private var command_switch : Int = 0
private var command_back : Int = 0
private var command_move : Int = 0
private var command_mod : Int = 0

private var select : Int = 0
private var start : Int = 0
var listEfectsDisplay: ArrayList<CommandValues> = ArrayList()

class LoadingSongs() : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        getSupportActionBar()?.hide()
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_loading_songs)
        onWindowFocusChanged(true)

        songs = Song("","","","","","","","",
             "", "", "","", arrayListOf())

        //listChannels.clear()
        //listCommands.clear()
        listEfectsDisplay.clear()
        //listChannels = getChannels(this)
        //listCommands = getFilesCW(this)

        //loadSounds(this)
        loadImages(this)
        //val intent = Intent(this, SelectChannel()::class.java)
        //startActivity(intent)
    }

    fun loadImages(c: Context) {
        bgaOff = c.getExternalFilesDir("/FingerDance/Themes/$tema/Movies/BGA_OFF.mp4").toString()

        //barLife1 = Drawable.createFromPath(c.getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/game_play/barLife1.png").toString())
        //barLife = Drawable.createFromPath(c.getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/game_play/barLife.png").toString())

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

        val pathChannelmov = File(c.getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/sound_navegation.mp3").toString())
        val decriptorChannelMov = FileInputStream(pathChannelmov).fd
        selectSong_mov = soundPoolSelectSong.load(decriptorChannelMov, 0, pathChannelmov.length(), 1)

        val pathChannelBack = File(c.getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/sound_back.ogg").toString())
        val decriptorChannelBack = FileInputStream(pathChannelBack).fd
        selectSong_back = soundPoolSelectSong.load(decriptorChannelBack, 0, pathChannelBack.length(), 1)

        val pathUpSound = File(c.getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/up_sound.ogg").toString())
        val decriptorUpSound = FileInputStream(pathUpSound).fd
        up_SelectSound = soundPoolSelectSong.load(decriptorUpSound, 0, pathUpSound.length(), 1)

        val pathMoveLv = File(c.getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/move_lvs.mp3").toString())
        val decriptorMoveLv = FileInputStream(pathMoveLv).fd
        move_lvs = soundPoolSelectSong.load(decriptorMoveLv, 0, pathMoveLv.length(), 1)

        val pathSwitch = File(c.getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/command_switch.mp3").toString())
        val decriptorSwitsh = FileInputStream(pathSwitch).fd
        command_switch = soundPoolSelectSong.load(decriptorSwitsh, 0, pathSwitch.length(), 1)

        val pathBack = File(c.getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/command_back.mp3").toString())
        val decriptorBack = FileInputStream(pathBack).fd
        command_back = soundPoolSelectSong.load(decriptorBack,0 , pathBack.length(), 1)

        val pathMove = File(c.getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/command_move.mp3").toString())
        val decriptorMove = FileInputStream(pathMove).fd
        command_move = soundPoolSelectSong.load(decriptorMove, 0, pathMove.length(), 1)

        val pathMod = File(c.getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/command_mod.mp3").toString())
        val decriptorMod = FileInputStream(pathMod).fd
        command_mod = soundPoolSelectSong.load(decriptorMod, 0, pathMod.length(), 1)

        val pathSelect = File(c.getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/select.ogg").toString())
        val decriptorSelect = FileInputStream(pathSelect).fd
        select = soundPoolSelectSong.load(decriptorSelect, 0, pathSelect.length(), 1)

        val pathStart = File(c.getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/start.ogg").toString())
        val decriptorStart = FileInputStream(pathStart).fd
        start = soundPoolSelectSong.load(decriptorStart, 0, pathStart.length(), 1)
    }

    fun getChannels(c: Context): ArrayList<Channels> {
        val dir = c.getExternalFilesDir("/FingerDance/Songs/Channels/")
        val listChannels = ArrayList<Channels>()
        val listRutasChannels = mutableListOf<String>()
        if (dir != null){
            dir.walkTopDown().forEach {
                if(it.toString().endsWith("info", true)){
                    when {
                        it.isDirectory -> {
                            listRutasChannels.add(it.toString().replace("/info", "", ignoreCase = true))
                        }
                    }
                }
            }
            listRutasChannels.sortBy { it }
            var nombre: String
            var descripcion: String
            var banner: String
            var rutaChannel: String
            var listSongs: ArrayList<Song>
            for (index in 0 until listRutasChannels.size) {
                nombre = listRutasChannels[index].removeRange(0, 82)
                descripcion = readFile(listRutasChannels[index] + "/info/text.ini")
                banner = listRutasChannels[index] + "/banner.png"
                rutaChannel = listRutasChannels[index]
                listSongs = getSongs(rutaChannel, c)
                channels = Channels(nombre, descripcion, banner, listSongs) //, listCommands)

                listChannels.add(channels)
            }
        }
        return listChannels
    }

    private fun getSongs(rutaChannel: String, c: Context): ArrayList<Song> {
        var ssc = ""
        var nombre = ""
        var artist = ""
        var bpm = ""
        var banner = ""
        var rutaBanner = ""
        var rutaPrevVideo = ""
        var prevVideo = ""
        var rutaVideo = ""
        var song  = ""
        var rutaCancion = ""
        var textLvs  = ""
        var rutaSteps = ""
        val listSongs = ArrayList<Song>()
        val rutaBitActive = c.getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/img_lv.png").toString()

        val listRutas = getRutasSongs(rutaChannel)
        for(index in 0 until listRutas.size) {
            val dir = File(listRutas[index])
            if (dir != null) {
                dir.walkTopDown().forEach {
                    //if (it.toString().endsWith(".sma", true) || it.toString().endsWith(".ssc", true)){
                    if (it.toString().endsWith(".ssc", true)){
                        ssc = readFile(it.toString())
                        rutaSteps= it.toString()
                        val arrSecs = ssc.split("---------------").toTypedArray()
                        val listSecs = mutableListOf<String>()
                        listSecs.add(arrSecs[0])
                        for(index in 1 until arrSecs.size){
                            if(arrSecs[index].contains("pump-single")){
                                listSecs.add(arrSecs[index])
                            }
                        }
                        val arr  = listSecs[0].split("\r\n").toTypedArray()
                        for (i in 0..arr.size -1){
                            if (arr[i].contains("#BANNER:")){
                                banner = arr[i].replace("#BANNER:", "",true)
                                banner = banner.replace(";","")
                                break
                            }
                        }
                        rutaBanner = listRutas[index] + "/" + banner

                        for (c in 0..arr.size -1){
                            if (arr[c].contains("#ARTIST")){
                                artist = arr[c].replace("#ARTIST:", "",true)
                                artist = artist.replace(";","")
                                artist = "     $artist"
                                break
                            }
                        }
                        for (a in 0..arr.size -1){
                            if (arr[a].contains("#BPMS:")){
                                bpm = arr[a].replace("#BPMS:0.000=", "",true)
                                bpm = bpm.replace(";","")
                                val ar = bpm.split(".").toTypedArray()
                                bpm = ar[0]
                                bpm = "BPM " + (getNumericValues(bpm).toInt())
                                break
                            }
                        }
                        for (e in 0 until arr.size -1){
                            if (arr[e].contains("#TITLE:")){
                                nombre = arr[e].replace("#TITLE:", "",true)
                                nombre = nombre.replace(";","")
                                break
                            }
                        }
                        for (f in 0 until arr.size -1){
                            if (arr[f].contains("#PREVIEWVID:")){
                                prevVideo = arr[f].replace("#PREVIEWVID:", "",true)
                                prevVideo = prevVideo.replace(";","")
                                break
                            }
                            if (arr[f].contains("#PREVIEW")){
                                prevVideo = arr[f].replace("#PREVIEW:", "",true)
                                prevVideo = prevVideo.replace(";","")
                                break
                            }
                        }
                        if (isFileExists(File(listRutas[index] + "/" + prevVideo))) {
                            if (prevVideo.endsWith(".mpg", ignoreCase = true) || prevVideo.endsWith(".avi", ignoreCase = true)) {
                                val inputVideoPath = File(listRutas[index] + "/" + prevVideo)
                                var outputVideoPath = ""
                                if(inputVideoPath.absolutePath.endsWith(".mpg", ignoreCase = true)){
                                    outputVideoPath = (listRutas[index] + "/" + prevVideo).replace(".mpg", ".mp4", ignoreCase = true)
                                }
                                if(inputVideoPath.absolutePath.endsWith(".avi", ignoreCase = true)){
                                    outputVideoPath = (listRutas[index] + "/" + prevVideo).replace(".avi", ".mp4", ignoreCase = true)
                                }

                                val command = arrayOf("-y", "-i", inputVideoPath.absolutePath, outputVideoPath)
                                FFmpeg.execute(command)

                                rutaPrevVideo = outputVideoPath

                                if (isFileExists(File(outputVideoPath))) {
                                    inputVideoPath.delete()
                                } else {
                                    rutaPrevVideo = listRutas[index] + "/" + prevVideo
                                }
                            } else {
                                rutaPrevVideo = listRutas[index] + "/" + prevVideo
                            }
                        }else{
                            if (isFileExists(File(listRutas[index] + "/" + prevVideo.replace(".mpg", ".mp4", ignoreCase = true)))) {
                                rutaPrevVideo = listRutas[index] + "/" + prevVideo.replace(".mpg", ".mp4", ignoreCase = true)
                            }
                        }

                        for (index in 0 until arr.size -1){
                            if (arr[index].contains("#MUSIC:")){
                                song = arr[index].replace("#MUSIC:", "",true)
                                song = song.replace(";","")
                                break
                            }
                        }

                        rutaCancion = listRutas[index] + "/" + song
                        rutaVideo = if(rutaCancion.endsWith(".ogg")){
                            rutaCancion.replace(".ogg", ".mp4",true)
                        }else{
                            rutaCancion.replace(".mp3", ".mp4",true)
                        }


                        /*if(!isFileExists(File(rutaVideo))){
                            rutaVideo = ""
                        }
                        */

                        val listLvs = mutableListOf<Lvs>()
                        val listNumLv = mutableListOf<String>()

                        for(index in 1 until arrSecs.size){
                            if(arrSecs[index].contains("pump-single - ")) {
                                val listLvs = arrSecs[index + 1].split(";")
                                for(level in listLvs){
                                    if(level.contains("#METER:")){
                                        textLvs = getNumericValues(level)
                                        if(textLvs != ""){
                                            if (textLvs.length == 1) {
                                                textLvs = "0$textLvs"
                                            }
                                            listNumLv.add(textLvs)
                                        }else{
                                            textLvs = "??"
                                        }
                                        textLvs = ""
                                    }
                                }
                            }
                        }

                        listNumLv.sortBy { it }
                        for(i in 0 until listNumLv.size){
                            listLvs.add(Lvs(listNumLv[i] , rutaBitActive))
                        }
                        songs = Song(nombre, artist, bpm, "",prevVideo, rutaPrevVideo, "", song, rutaBanner, rutaCancion, rutaSteps,rutaVideo, listLvs)
                        listSongs.add(songs)
                    }
                }
            }
        }
        return listSongs
    }

    private fun getRutasSongs(rutaChannel: String): MutableList<String> {
        val dir = File(rutaChannel)
        val listRutas = mutableListOf<String>()
        dir.walkTopDown().forEach {
            when {
                it.isDirectory -> {
                    if (it.toString() != rutaChannel) {
                        if (it.toString() != "$rutaChannel/info") {
                            listRutas.add(it.toString())
                        }
                    }
                }
            }
        }
        return listRutas
    }

    fun getFilesCW(c: Context) : ArrayList<Command>{
        val rutaImgsCommands = c.getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/command_window/commands").toString()
        val listRutaImgs = ArrayList<String>()
        val listDescripciones = ArrayList<String>()
        val listCommands = ArrayList<Command>()
        var rutaImagenNS = ""

        val dirCommands = File(rutaImgsCommands)
        if (dirCommands != null) {
            val info = readFile(rutaImgsCommands + "/info.txt")
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
            var rutaCommandValue = ""

            for(index in 0 until listRutaImgs.size){
                val dirValues = File(listRutaImgs[index].replace(".png", "", true))
                val listCommandsValues = ArrayList<CommandValues>()
                var arrValues = emptyArray<String>()
                if (dirValues != null && !dirValues.toString().contains("noteskin", ignoreCase = true)){
                    var position = 0
                    dirValues.walkTopDown().forEach {
                        if(it.toString().endsWith(".png", true)){
                            if(isFileExists(File(dirValues.toString() + "/info.txt"))){
                                val infoValues = readFile(dirValues.toString() + "/info.txt")
                                arrValues = infoValues.split("\r\n").toTypedArray()
                            }
                            value = it.name.replace(".png", "", ignoreCase = true)
                            rutaCommandValue = it.toString()
                            var descripcion = ""
                            if(value.matches(Regex(".*[0-9].*"))){
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
                                if(isFileExists(File(dirValues.toString() + "/info.txt"))){
                                    descripcion = arrValues[position]
                                    position++
                                }
                            }
                            listCommandsValues.add(CommandValues(value,descripcion, rutaCommandValue))
                        }
                    }
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
                            if(it.toString().contains("_Icon", ignoreCase = true) && !it.toString().contains("default", ignoreCase = true)){
                                val descripcion = "Usar " + value + " NoteSkin"
                                rutaCommandValue = it.toString()
                                listCommandsValues.add(CommandValues(value,descripcion, rutaCommandValue))
                            }
                        }
                        listCommands.add(Command(
                            dirValues.toString(),
                            listDescripciones[index],
                            rutaImagenNS,
                            listCommandsValues))
                    }
                }
            }
        }
        return listCommands
    }

    private fun readFile(path: String): String {
        val encoded = Files.readAllBytes(Paths.get(path))
        return String(encoded, StandardCharsets.UTF_8)
    }

    private fun isFileExists(file: File): Boolean {
        return file.exists() && !file.isDirectory
    }

    fun deleteTrash(directorio: File): Boolean {
        if (!directorio.exists()) {
            return false
        }

        if (directorio.isDirectory) {
            val archivos = directorio.listFiles()

            if (archivos != null) {
                for (archivo in archivos) {
                    if (archivo.isDirectory) {
                        deleteTrash(archivo)
                    } else {
                        archivo.delete()
                    }
                }
            }
        }
        return directorio.delete()
    }

    fun getNumericValues(cadena: String): String {
        val sb = StringBuilder()
        for (i in cadena.indices) {
            var numeric = true
            try {
                val num = parseDouble(cadena[i].toString())
            } catch (e: NumberFormatException) {
                numeric = false
            }

            if (numeric) {
                sb.append(cadena[i].toString())
            } else {
                //no es valor numerico.
            }

        }

        return sb.toString();
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
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

}