package com.fingerdance

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.arthenica.mobileffmpeg.FFmpeg
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

lateinit var soundPoolSelectSongKsf: SoundPool
var selectSong_movKsf : Int = 0
var selectSong_backKsf : Int = 0
var up_SelectSoundKsf : Int = 0
var move_lvsKsf : Int = 0

var command_switchKsf : Int = 0
var command_backKsf : Int = 0
var command_moveKsf : Int = 0
var command_modKsf : Int = 0

var selectKsf : Int = 0
var startKsf : Int = 0

var tick : Int = 0

class LoadSongsKsf (context : Context){
    //private var context = context

    private lateinit var channel: Channels
    fun getChannels(context: Context): ArrayList<Channels> {
        val dir = context.getExternalFilesDir("/FingerDance/Songs/Channels/")
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
            var listSongs: ArrayList<SongKsf>

            for (index in 0 until listRutasChannels.size) {
                nombre = listRutasChannels[index].removeRange(0, 82)
                descripcion = readFile(listRutasChannels[index] + "/info/text.ini")
                banner = listRutasChannels[index] + "/banner.png"
                rutaChannel = listRutasChannels[index]
                listSongs = getSongs(rutaChannel, context)
                channel = Channels(nombre, descripcion, banner, rutaChannel, arrayListOf() , listSongs)

                listChannels.add(channel)
            }

        }
        return listChannels
    }

    private fun getSongs(rutaChannel: String, c: Context): ArrayList<SongKsf> {

        val listSongs = ArrayList<SongKsf>()
        val rutaBitActive = c.getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/img_lv.png").toString()

        val listRutas = getRutasSongs(rutaChannel)

        for(index in 0 until listRutas.size){
            val listKsf = arrayListOf<Ksf>()
            val songKsf = SongKsf("","","","","","","","", arrayListOf())
            val dir = File(listRutas[index])
            dir.walkTopDown().forEach { it ->
                when {
                    it.isFile ->{
                        if(it.toString().endsWith("Disc.png", ignoreCase = true)){
                            songKsf.rutaDisc = it.absolutePath
                        }
                        if(it.toString().endsWith("Title.png", ignoreCase = true)){
                            songKsf.rutaTitle = it.absolutePath
                        }
                        if(it.toString().endsWith("Song.mp3", ignoreCase = true)){
                            songKsf.rutaSong = it.absolutePath
                            songKsf.rutaPreview = songKsf.rutaSong.replace(".mp3", "_p.mpg", ignoreCase = true)
                            songKsf.rutaBGA = songKsf.rutaSong.replace(".mp3", ".mp4", ignoreCase = true)

                            if (isFileExists(File(songKsf.rutaPreview))) {
                                val inputVideoPath = File(songKsf.rutaPreview)
                                val outputVideoPath = (songKsf.rutaPreview).replace(".mpg", ".mp4", ignoreCase = true)

                                val command = arrayOf("-y", "-i", inputVideoPath.absolutePath, outputVideoPath)
                                FFmpeg.execute(command)

                                if (isFileExists(File(outputVideoPath))) {
                                    inputVideoPath.delete()
                                    songKsf.rutaPreview = outputVideoPath
                                }
                            }else if (isFileExists(File(songKsf.rutaPreview.replace(".mpg", ".mp4")))) {
                                songKsf.rutaPreview = songKsf.rutaPreview.replace(".mpg", ".mp4")
                            }
                        }

                        if(it.toString().endsWith(".ksf", ignoreCase = true)){
                            val ksf = Ksf("", "", "")
                            val file = File(it.toString())
                            file.useLines { lines ->
                                for (line in lines) {
                                    when {
                                        line.startsWith("#TITLE:") ->{
                                            songKsf.title = getValue(line)
                                        }
                                        line.startsWith("#ARTIST:") ->{
                                            songKsf.artist = getValue(line)
                                        }
                                        line.startsWith("#BPM:") ->{
                                            songKsf.displayBpm = getValue(line)
                                        }
                                        line.startsWith("#DIFFICULTY:") ->{
                                            var level = getValue(line)
                                            if (level.length == 1) {
                                                level = "0$level"
                                            }
                                            ksf.level = level
                                            ksf.rutaKsf = it.toString()
                                            ksf.rutaBitActive = rutaBitActive
                                            listKsf.add(ksf)
                                        }
                                        line.startsWith("00000") ->{
                                            break
                                        }
                                    }
                                }
                                listKsf.sortBy { it.level }
                                songKsf.listKsf = listKsf
                            }
                        }

                    }
                }
            }
            listSongs.add(songKsf)
        }
        return listSongs
    }

    private fun getValue(line: String): String {
        return line.split(":")[1].replace(";", "")
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
        var rutaCommandValue = ""

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
                            if(isFileExists(File("$dirValues/info.txt"))){
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
                            val descripcion = "Usar $value NoteSkin"
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
        return listCommands
    }

    private fun readFile(path: String): String {
        val encoded = Files.readAllBytes(Paths.get(path))
        return String(encoded, StandardCharsets.UTF_8)
    }

    private fun isFileExists(file: File): Boolean {
        return file.exists() && !file.isDirectory
    }

    fun loadSounds(c: Context) {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_GAME)
            .build()

        soundPoolSelectSongKsf = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        val pathChannelmov = File(c.getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/sound_navegation.mp3").toString())
        val decriptorChannelMov = FileInputStream(pathChannelmov).fd
        selectSong_movKsf = soundPoolSelectSongKsf.load(decriptorChannelMov, 0, pathChannelmov.length(), 1)

        val pathChannelBack = File(c.getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/sound_back.ogg").toString())
        val decriptorChannelBack = FileInputStream(pathChannelBack).fd
        selectSong_backKsf = soundPoolSelectSongKsf.load(decriptorChannelBack, 0, pathChannelBack.length(), 1)

        val pathUpSound = File(c.getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/up_sound.ogg").toString())
        val decriptorUpSound = FileInputStream(pathUpSound).fd
        up_SelectSoundKsf = soundPoolSelectSongKsf.load(decriptorUpSound, 0, pathUpSound.length(), 1)

        val pathMoveLv = File(c.getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/move_lvs.mp3").toString())
        val decriptorMoveLv = FileInputStream(pathMoveLv).fd
        move_lvsKsf = soundPoolSelectSongKsf.load(decriptorMoveLv, 0, pathMoveLv.length(), 1)

        val pathSwitch = File(c.getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/command_switch.mp3").toString())
        val decriptorSwitsh = FileInputStream(pathSwitch).fd
        command_switchKsf = soundPoolSelectSongKsf.load(decriptorSwitsh, 0, pathSwitch.length(), 1)

        val pathBack = File(c.getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/command_back.mp3").toString())
        val decriptorBack = FileInputStream(pathBack).fd
        command_backKsf = soundPoolSelectSongKsf.load(decriptorBack,0 , pathBack.length(), 1)

        val pathMove = File(c.getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/command_move.mp3").toString())
        val decriptorMove = FileInputStream(pathMove).fd
        command_moveKsf = soundPoolSelectSongKsf.load(decriptorMove, 0, pathMove.length(), 1)

        val pathMod = File(c.getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/command_mod.mp3").toString())
        val decriptorMod = FileInputStream(pathMod).fd
        command_modKsf = soundPoolSelectSongKsf.load(decriptorMod, 0, pathMod.length(), 1)

        val pathSelect = File(c.getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/select.ogg").toString())
        val decriptorSelect = FileInputStream(pathSelect).fd
        selectKsf = soundPoolSelectSongKsf.load(decriptorSelect, 0, pathSelect.length(), 1)

        val pathStart = File(c.getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/start.ogg").toString())
        val decriptorStart = FileInputStream(pathStart).fd
        startKsf = soundPoolSelectSongKsf.load(decriptorStart, 0, pathStart.length(), 1)

        val pathTick = File(c.getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/TICK.ogg").toString())
        val decriptorTick = FileInputStream(pathTick).fd
        tick = soundPoolSelectSongKsf.load(decriptorTick, 0, pathTick.length(), 1)
    }
}

data class Ksf(var rutaKsf: String, var level: String, var rutaBitActive: String)

data class SongKsf(
    var title: String,
    var artist: String,
    var displayBpm: String,
    var rutaDisc: String,
    var rutaTitle: String,
    var rutaSong: String,
    var rutaPreview: String,
    var rutaBGA: String,
    var listKsf: ArrayList<Ksf>)