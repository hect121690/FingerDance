package com.fingerdance

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

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

class LoadingSongs (context: Context) {

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
                if(it.toString().endsWith("info_ssc", true)){
                    when {
                        it.isDirectory -> {
                            listRutasChannels.add(it.toString().replace("/info_ssc", "", ignoreCase = true))
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
                nombre = File(listRutasChannels[index]).name
                descripcion = readFile(listRutasChannels[index] + "/info_ssc/text.ini")
                banner = listRutasChannels[index] + "/banner_ssc.png"
                rutaChannel = listRutasChannels[index]
                listSongs = getSongs(rutaChannel, c)
                listChannels.add(Channels(nombre, descripcion, banner, listSongs))
            }
        }
        return listChannels
    }

    private fun getSongs(rutaChannel: String, c: Context): ArrayList<Song> {
        var ssc = ""
        var name = ""
        var artist = ""
        var displayBpm = ""
        var banner = ""
        var rutaDisc = ""
        var rutaBanner = ""
        var rutaPrevVideo = ""
        var rutaCancion = ""
        var rutaBga = ""
        var listLevels = mutableListOf<Lvs>()
        val listSongs = ArrayList<Song>()
        val rutaBitActiveSingle = c.getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/img_lv.png").toString()
        val rutaBitActiveHalfDouble = c.getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/img_lv_hd.png").toString()

        val listRutas = getRutasSongs(rutaChannel)
        for(index in 0 until listRutas.size) {
            val dir = File(listRutas[index])
            if (dir != null) {
                dir.walkTopDown().forEach {
                    if (it.toString().endsWith(".ssc", true)){
                        ssc = readFile(it.toString())
                        val seccions = ssc.split("#NOTEDATA:;").toTypedArray()
                        val arr  = seccions[0].split("\r\n").toTypedArray()
                        for (e in 0 until arr.size -1) {
                            when {
                                arr[e].startsWith("#TITLE:") -> {
                                    name = getValue(arr[e])
                                }
                                arr[e].startsWith("#ARTIST:") -> {
                                    artist = getValue(arr[e])
                                }
                                arr[e].startsWith("#BANNER:") || arr[e].startsWith("#BACKGROUND")-> {
                                    banner = getValue(arr[e])
                                    rutaBanner = listRutas[index] + "/" + banner
                                }
                                arr[e].startsWith("#CDIMAGE:") || arr[e].startsWith("#DISCIMAGE") -> {
                                    val disc = getValue(arr[e])
                                    rutaDisc = listRutas[index] + "/" + disc
                                }
                                arr[e].startsWith("#MUSIC:") -> {
                                    val song = getValue(arr[e])
                                    rutaCancion = listRutas[index] + "/" + song
                                    if(song.endsWith(".mp3", true)){
                                        rutaBga = listRutas[index] + "/" + song.replace(".mp3", ".mp4", true)
                                    }
                                    if(song.endsWith(".ogg", true)){
                                        rutaBga = listRutas[index] + "/" + song.replace(".ogg", ".mp4", true)
                                    }
                                }
                                arr[e].startsWith("#PREVIEW:") || arr[e].startsWith("#PREVIEWVID") -> {
                                    val prevVideo = getValue(arr[e])
                                    rutaPrevVideo = listRutas[index] + "/" + prevVideo
                                }
                                arr[e].startsWith("#DISPLAYBPM:") -> {
                                    displayBpm = getDisplayBpm(arr[e])
                                }
                            }
                        }
                        if(rutaDisc == ""){
                            rutaDisc = banner + "_B"
                        }
                        listLevels = mutableListOf<Lvs>()
                        for(index in 1 until seccions.size){
                            var numberLevel = ""
                            var rutaBitlevel = ""
                            var noteString = seccions[index]
                            var stepType = ""
                            val arr  = seccions[index].split("\r\n").toTypedArray()
                            for(i in 0 until arr.size -1){
                                when {
                                    arr[i].startsWith("#STEPSTYPE:") -> {
                                        stepType = getValue(arr[i])
                                        if(stepType.equals("pump-single", true)){
                                            rutaBitlevel = rutaBitActiveSingle
                                        }
                                        if(stepType.equals("pump-halfdouble", true)){
                                            rutaBitlevel = rutaBitActiveHalfDouble
                                        }

                                    }
                                    arr[i].startsWith("#METER:") -> {
                                        numberLevel = getValue(arr[i]).padStart(2, '0')
                                    }
                                    arr[i].startsWith("#NOTES:") -> {
                                        break
                                    }
                                    arr[i].startsWith("#BPMS:") ->{
                                        if(displayBpm == "" || displayBpm.toDouble() < 0){
                                            displayBpm = getDisplayBpmEmpty(arr[i])
                                        }
                                    }
                                }
                            }
                            if(stepType.equals("pump-single", true) || stepType.equals("pump-half-double", true) || stepType.equals("pump-halfdouble", true)){
                                listLevels.add(Lvs(lvl = numberLevel, rutaLvImg = rutaBitlevel, steps = noteString, typePlayer = stepType))
                            }
                            listLevels.sortWith(compareBy<Lvs> {
                                when (it.typePlayer) {
                                    "pump-single" -> 1
                                    "pump-half-double" -> 2
                                    else -> 3
                                }
                            }.thenBy {
                                it.lvl
                            })
                        }
                        listSongs.add(
                            Song(
                                name = name,
                                artist = artist,
                                rutaCancion = rutaCancion,
                                rutaPrevVideo = rutaPrevVideo,
                                rutaBga = rutaBga,
                                displayBpm = displayBpm,
                                rutaDisc = rutaDisc,
                                rutaBanner = rutaBanner,
                                listLvs = listLevels
                            )
                        )
                    }
                }
            }
        }
        return listSongs
    }

    private fun getDisplayBpmEmpty(line: String): String {
        val bpm = line.substringAfter("=").substringBefore(";").substringBefore(",")
        return "%.2f".format(bpm.toDouble())

    }

    private fun getDisplayBpm(line: String): String {
        val bpm = line.substringAfter(":").substringBefore(";")
        if(bpm.contains(":")){
            val bpmSplit = bpm.split(":").toTypedArray()
            return "%.2f".format(bpmSplit[0].toDouble())

        }
        return "%.2f".format(bpm.toDouble())

    }

    private fun getValue(line: String): String {
        return line.substringAfter(":").substringBefore(";")
    }

    private fun getRutasSongs(rutaChannel: String): MutableList<String> {
        val dir = File(rutaChannel)
        val listRutas = mutableListOf<String>()
        dir.walkTopDown().forEach {
            when {
                it.isDirectory -> {
                    if (it.toString() != rutaChannel) {
                        if (it.toString() != "$rutaChannel/info_ssc") {
                            listRutas.add(it.toString())
                        }
                    }
                }
            }
        }
        return listRutas
    }

    private fun readFile(path: String): String {
        val encoded = Files.readAllBytes(Paths.get(path))
        return String(encoded, StandardCharsets.UTF_8)
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
}





