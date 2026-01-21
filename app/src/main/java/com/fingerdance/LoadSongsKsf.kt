package com.fingerdance

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
//import com.arthenica.mobileffmpeg.FFmpeg
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest

lateinit var soundPoolSelectSongKsf: SoundPool
var selectSong_movKsf : Int = 0
var selectSong_backKsf : Int = 0
var up_SelectSoundKsf : Int = 0
var move_lvsKsf : Int = 0

var rank_sound : Int = 0
var new_record : Int = 0
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

lateinit var soundPoolSelectSongSound: SoundPool
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

class LoadSongsKsf {

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
                channel = Channels(nombre, descripcion, banner, arrayListOf(), listSongs)

                listChannels.add(channel)
            }
        }
        return listChannels
    }

    fun getChannelsOnline(context: Context): ArrayList<Channels> {
        val baseDir = context.getExternalFilesDir("/FingerDance/Songs/Channels/")
        val listChannels = ArrayList<Channels>()

        if (baseDir?.exists() == true && baseDir.isDirectory) {
            val listRutasChannels = mutableListOf<String>()

            // Filtrar solo las carpetas que coincidan con la lista
            baseDir.listFiles()?.forEach { file ->
                if (file.isDirectory && validFolders.contains(file.name)) {
                    val infoDir = File(file, "info")
                    if (infoDir.exists() && infoDir.isDirectory) {
                        listRutasChannels.add(file.absolutePath)
                    }
                }
            }

            listRutasChannels.sort()

            for (rutaChannel in listRutasChannels) {
                val nombre = rutaChannel.substringAfterLast("/") // Nombre de la carpeta
                val descripcion = readFile("$rutaChannel/info/text.ini")
                val banner = "$rutaChannel/banner.png"
                val listSongs = getSongs(rutaChannel, context)

                val channel = Channels(nombre, descripcion, banner, arrayListOf(), listSongs)
                listChannels.add(channel)
            }
        }
        return listChannels
    }

    private fun getSongs(rutaChannel: String, c: Context): ArrayList<SongKsf> {

        val listSongs = ArrayList<SongKsf>()
        val rutaBitActive = c.getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/img_lv.png").toString()
        val rutaBitActiveHalfDouble = c.getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/img_lv_hd.png").toString()
        val typeStepsOrder = mapOf(
            "NORMAL" to 0,
            "UCS" to 1,
            "ANOTHER" to 2,
            "QUEST" to 3
        )
        val listRutas = getRutasSongs(rutaChannel)
        val listOthers = arrayListOf("03-SHORT CUT - V2", "04-REMIX - V2", "05-FULLSONGS - V2")

        for(index in 0 until listRutas.size){
            val listKsf = arrayListOf<Ksf>()
            val songKsf = SongKsf("","","","","","","","", arrayListOf())
            val dir = File(listRutas[index])
            val channel = rutaChannel.split("/")
            val i = channel.size - 1
            songKsf.channel = channel[i]
            dir.walkTopDown().forEach { it ->
                when {
                    it.isFile ->{
                        if(it.toString().endsWith("Disc.png", ignoreCase = true)){
                            songKsf.rutaDisc = it.absolutePath
                        }
                        if(it.toString().endsWith("Title.png", ignoreCase = true)){
                            songKsf.rutaTitle = it.absolutePath
                        }
                        if(it.toString().endsWith(".mp3", ignoreCase = true) || it.toString().endsWith(".ogg", ignoreCase = true)){
                            songKsf.rutaSong = it.absolutePath
                            songKsf.rutaPreview = if(songKsf.rutaSong.endsWith(".mp3", ignoreCase = true)){
                                songKsf.rutaSong.replace(".mp3", "_p.mpg", ignoreCase = true)
                            }else{
                                songKsf.rutaSong.replace(".ogg", "_p.mpg", ignoreCase = true)
                            }
                            songKsf.rutaBGA = if(songKsf.rutaSong.endsWith(".mp3", ignoreCase = true)){
                                songKsf.rutaSong.replace(".mp3", ".mp4", ignoreCase = true)
                            }else{
                                songKsf.rutaSong.replace(".ogg", ".mp4", ignoreCase = true)
                            }

                            if (isFileExists(File(songKsf.rutaPreview))) {
                                val inputVideoPath = File(songKsf.rutaPreview)
                                val outputVideoPath = (songKsf.rutaPreview).replace(".mpg", ".mp4", ignoreCase = true)

                                //val command = arrayOf("-y", "-i", inputVideoPath.absolutePath, outputVideoPath)
                                //FFmpeg.execute(command)

                                if (isFileExists(File(outputVideoPath))) {
                                    inputVideoPath.delete()
                                    songKsf.rutaPreview = outputVideoPath
                                }
                            }else if (isFileExists(File(songKsf.rutaPreview.replace(".mpg", ".mp4")))) {
                                songKsf.rutaPreview = songKsf.rutaPreview.replace(".mpg", ".mp4")
                            }else {
                                songKsf.rutaPreview = songKsf.rutaPreview.replace(".mpg", ".mp4")
                            }
                        }

                        if(it.toString().endsWith(".ksf", ignoreCase = true)){
                            val ksf = Ksf()
                            ksf.typePlayer = "A"
                            ksf.typeSteps = "NORMAL"
                            val file = File(it.toString())

                            /*
                            ksf.checkedValues = generateCheckedValues(file)
                            */

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
                                        line.startsWith("#STARTTIME:") ->{
                                            songKsf.offset = if(getValue(line) != "") getValue(line).trim().toDouble().toLong() else 0
                                        }
                                        line.startsWith("#TYPE:") ->{
                                            ksf.typeSteps = getValue(line)
                                        }
                                        line.startsWith("#STEP:") && line.trim().length > 6 ->{
                                            ksf.stepmaker = getValue(line)
                                        }
                                        line.startsWith("#STEPMAKER:") ||
                                        line.startsWith("#CREATOR") ||
                                        line.startsWith("#RECREATOR:") ->{
                                            ksf.stepmaker = getValue(line)
                                        }
                                        line.startsWith("#PLAYER:") ->{
                                            if(getValue(line) == "HALFDOUBLE"){
                                                ksf.typePlayer = "B"
                                            }
                                        }
                                        line.startsWith("#DIFFICULTY:") ->{
                                            var level = getValue(line)
                                            if (level.length == 1) {
                                                level = "0$level"
                                            }
                                            ksf.level = level
                                        }
                                        line.startsWith("00000") || line.startsWith("|")->{
                                            ksf.rutaKsf = it.toString()
                                            ksf.rutaBitActive = rutaBitActive
                                            if(ksf.typePlayer == "B"){
                                                ksf.rutaBitActive = rutaBitActiveHalfDouble
                                            }
                                            listKsf.add(ksf)
                                            break
                                        }
                                    }
                                }
                            }
                        }

                        listKsf.sortWith(
                            compareBy<Ksf>(
                                { it.typePlayer == "B" },
                                { it.level },
                                { typeStepsOrder[it.typeSteps] ?: 99 }
                            )
                        )
                        songKsf.listKsf = listKsf
                    }
                }
            }
            /*
            for(i in 0 until songKsf.listKsf.size){
                songKsf.listKsf[i].checkedValues = "${songKsf.listKsf[i].checkedValues}|${File(songKsf.rutaSong).length()}"
            }
            */
            listSongs.add(songKsf)
        }
        listSongs.sortBy { song ->
            when (song.channel) {
                in listOthers -> File(song.rutaSong).parentFile!!.name
                else -> song.title
            }
        }

        return listSongs
    }

    private fun generateCheckedValues(file: File): String {
        var inStepBlock = false
        var count1 = 0
        var count4 = 0

        file.forEachLine { line ->
            if (!inStepBlock && line.startsWith("#STEP:")) {
                inStepBlock = true
                return@forEachLine
            }

            if (inStepBlock) {
                if (line.startsWith("22222")) return@forEachLine
                if (line.startsWith("|")) return@forEachLine
                line.forEach { char ->
                    when (char) {
                        '1' -> count1++
                        '4' -> count4++
                    }
                }
            }
        }

        return "$count1|$count4"
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

        soundPoolSelectSongSound = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        val pathSounds = c.getExternalFilesDir("/FingerDance/Themes/$tema/Sounds").toString()

        val pathChannelmov = File("$pathSounds/sound_navegation.mp3")
        val decriptorChannelMov = FileInputStream(pathChannelmov).fd
        selectSong_movKsf = soundPoolSelectSongKsf.load(decriptorChannelMov, 0, pathChannelmov.length(), 1)

        val pathChannelBack = File("$pathSounds/sound_back.ogg")
        val decriptorChannelBack = FileInputStream(pathChannelBack).fd
        selectSong_backKsf = soundPoolSelectSongKsf.load(decriptorChannelBack, 0, pathChannelBack.length(), 1)

        val pathUpSound = File("$pathSounds/up_sound.ogg")
        val decriptorUpSound = FileInputStream(pathUpSound).fd
        up_SelectSoundKsf = soundPoolSelectSongKsf.load(decriptorUpSound, 0, pathUpSound.length(), 1)

        val pathMoveLv = File("$pathSounds/move_lvs.mp3")
        val decriptorMoveLv = FileInputStream(pathMoveLv).fd
        move_lvsKsf = soundPoolSelectSongKsf.load(decriptorMoveLv, 0, pathMoveLv.length(), 1)

        val pathSwitch = File("$pathSounds/command_switch.mp3")
        val decriptorSwitsh = FileInputStream(pathSwitch).fd
        command_switchKsf = soundPoolSelectSongKsf.load(decriptorSwitsh, 0, pathSwitch.length(), 1)

        val pathBack = File("$pathSounds/command_back.mp3")
        val decriptorBack = FileInputStream(pathBack).fd
        command_backKsf = soundPoolSelectSongKsf.load(decriptorBack,0 , pathBack.length(), 1)

        val pathMove = File("$pathSounds/command_move.mp3")
        val decriptorMove = FileInputStream(pathMove).fd
        command_moveKsf = soundPoolSelectSongKsf.load(decriptorMove, 0, pathMove.length(), 1)

        val pathMod = File("$pathSounds/command_mod.mp3")
        val decriptorMod = FileInputStream(pathMod).fd
        command_modKsf = soundPoolSelectSongKsf.load(decriptorMod, 0, pathMod.length(), 1)

        val pathSelect = File("$pathSounds/select.ogg")
        val decriptorSelect = FileInputStream(pathSelect).fd
        selectKsf = soundPoolSelectSongKsf.load(decriptorSelect, 0, pathSelect.length(), 1)

        val pathStart = File("$pathSounds/start.ogg")
        val decriptorStart = FileInputStream(pathStart).fd
        startKsf = soundPoolSelectSongKsf.load(decriptorStart, 0, pathStart.length(), 1)

        val pathTick = File("$pathSounds/TICK.ogg")
        val decriptorTick = FileInputStream(pathTick).fd
        tick = soundPoolSelectSongKsf.load(decriptorTick, 0, pathTick.length(), 1)

        val pathRankSound = File("$pathSounds/Evaluation/rank_sound.ogg")
        val decriptorRankSound = FileInputStream(pathRankSound).fd
        rank_sound = soundPoolSelectSongKsf.load(decriptorRankSound, 0, pathRankSound.length(), 1)

        val pathNewRecord = File("$pathSounds/Evaluation/new_record.ogg")
        val decriptorNewRecord = FileInputStream(pathNewRecord).fd
        new_record = soundPoolSelectSongKsf.load(decriptorNewRecord, 0, pathNewRecord.length(), 1)

        getRank(pathSounds)
        getRankB(pathSounds)
        getNavigationSounds(pathSounds)
        getSoundEndSong(pathSounds)
    }

    private fun getNavigationSounds(pathSounds: String){
        val pathStZero = File("$pathSounds/st_zero.mp3")
        val decriptorStZero = FileInputStream(pathStZero).fd
        st_zero = soundPoolSelectSongSound.load(decriptorStZero, 0, pathStZero.length(), 1)

        val pathNxNxAbs = File("$pathSounds/nx_nx_abs.mp3")
        val decriptorNxNxAbs = FileInputStream(pathNxNxAbs).fd
        nx_nxAbs = soundPoolSelectSongSound.load(decriptorNxNxAbs, 0, pathNxNxAbs.length(), 1)

        val pathFiestaFiesta2 = File("$pathSounds/fiesta_fiesta2.mp3")
        val decriptorFiestaFiesta2 = FileInputStream(pathFiestaFiesta2).fd
        fiesta_fiesta2 = soundPoolSelectSongSound.load(decriptorFiestaFiesta2, 0, pathFiestaFiesta2.length(), 1)

        val pathPrime = File("$pathSounds/prime.mp3")
        val decriptorPrime = FileInputStream(pathPrime).fd
        prime = soundPoolSelectSongSound.load(decriptorPrime, 0, pathPrime.length(), 1)

        val pathPrime2 = File("$pathSounds/prime2.mp3")
        val decriptorPrime2 = FileInputStream(pathPrime2).fd
        prime2 = soundPoolSelectSongSound.load(decriptorPrime2, 0, pathPrime2.length(), 1)

        val pathAniversaryXX = File("$pathSounds/aniversary_xx.mp3")
        val decriptorAniversaryXX = FileInputStream(pathAniversaryXX).fd
        aniversary_xx = soundPoolSelectSongSound.load(decriptorAniversaryXX, 0, pathAniversaryXX.length(), 1)

        val pathPhoenix = File("$pathSounds/phoenix.mp3")
        val decriptorPhoenix = FileInputStream(pathPhoenix).fd
        phoenix = soundPoolSelectSongSound.load(decriptorPhoenix, 0, pathPhoenix.length(), 1)
    }

    private fun getRank(pathSounds: String) {
        val pathRankSSS = File("$pathSounds/Evaluation/sss.ogg")
        val decriptorRankSSS = FileInputStream(pathRankSSS).fd
        sss_rank = soundPoolSelectSongKsf.load(decriptorRankSSS, 0, pathRankSSS.length(), 1)

        val pathRankSS = File("$pathSounds/Evaluation/ss.ogg")
        val decriptorRankSS = FileInputStream(pathRankSS).fd
        ss_rank = soundPoolSelectSongKsf.load(decriptorRankSS, 0, pathRankSS.length(), 1)

        val pathRankS = File("$pathSounds/Evaluation/s.ogg")
        val decriptorRankS = FileInputStream(pathRankS).fd
        s_rank = soundPoolSelectSongKsf.load(decriptorRankS, 0, pathRankSSS.length(), 1)

        val pathRanka = File("$pathSounds/Evaluation/a.ogg")
        val decriptorRanka = FileInputStream(pathRanka).fd
        a_rank = soundPoolSelectSongKsf.load(decriptorRanka, 0, pathRanka.length(), 1)

        val pathRankb = File("$pathSounds/Evaluation/b.ogg")
        val decriptorRankb = FileInputStream(pathRankb).fd
        b_rank = soundPoolSelectSongKsf.load(decriptorRankb, 0, pathRankb.length(), 1)

        val pathRankc = File("$pathSounds/Evaluation/c.ogg")
        val decriptorRankc = FileInputStream(pathRankc).fd
        c_rank = soundPoolSelectSongKsf.load(decriptorRankc, 0, pathRankc.length(), 1)

        val pathRankd = File("$pathSounds/Evaluation/d.ogg")
        val decriptorRankd = FileInputStream(pathRankd).fd
        d_rank = soundPoolSelectSongKsf.load(decriptorRankd, 0, pathRankd.length(), 1)

        val pathRankf = File("$pathSounds/Evaluation/f.ogg")
        val decriptorRankf = FileInputStream(pathRankf).fd
        f_rank = soundPoolSelectSongKsf.load(decriptorRankf, 0, pathRankf.length(), 1)
    }

    private fun getRankB(pathSounds: String) {
        val pathRankSSS = File("$pathSounds/Evaluation/sssB.ogg")
        val decriptorRankSSS = FileInputStream(pathRankSSS).fd
        sss_rankB = soundPoolSelectSongKsf.load(decriptorRankSSS, 0, pathRankSSS.length(), 1)

        val pathRankSS = File("$pathSounds/Evaluation/ssB.ogg")
        val decriptorRankSS = FileInputStream(pathRankSS).fd
        ss_rankB = soundPoolSelectSongKsf.load(decriptorRankSS, 0, pathRankSS.length(), 1)

        val pathRankS = File("$pathSounds/Evaluation/sB.ogg")
        val decriptorRankS = FileInputStream(pathRankS).fd
        s_rankB = soundPoolSelectSongKsf.load(decriptorRankS, 0, pathRankSSS.length(), 1)

        val pathRanka = File("$pathSounds/Evaluation/aB.ogg")
        val decriptorRanka = FileInputStream(pathRanka).fd
        a_rankB = soundPoolSelectSongKsf.load(decriptorRanka, 0, pathRanka.length(), 1)

        val pathRankb = File("$pathSounds/Evaluation/bB.ogg")
        val decriptorRankb = FileInputStream(pathRankb).fd
        b_rankB = soundPoolSelectSongKsf.load(decriptorRankb, 0, pathRankb.length(), 1)

        val pathRankc = File("$pathSounds/Evaluation/cB.ogg")
        val decriptorRankc = FileInputStream(pathRankc).fd
        c_rankB = soundPoolSelectSongKsf.load(decriptorRankc, 0, pathRankc.length(), 1)

        val pathRankd = File("$pathSounds/Evaluation/dB.ogg")
        val decriptorRankd = FileInputStream(pathRankd).fd
        d_rankB = soundPoolSelectSongKsf.load(decriptorRankd, 0, pathRankd.length(), 1)

        val pathRankf = File("$pathSounds/Evaluation/fB.ogg")
        val decriptorRankf = FileInputStream(pathRankf).fd
        f_rankB = soundPoolSelectSongKsf.load(decriptorRankf, 0, pathRankf.length(), 1)
    }

    private fun getSoundEndSong(pathSounds: String){
        val pathPerfectGame = File("$pathSounds/perfect_game.mp3")
        val decriptorPerfectGame = FileInputStream(pathPerfectGame).fd
        perfect_game = soundPoolSelectSongKsf.load(decriptorPerfectGame, 0, pathPerfectGame.length(), 1)

        val pathFullcombo = File("$pathSounds/full_combo.mp3")
        val decriptorFullcombo = FileInputStream(pathFullcombo).fd
        full_combo = soundPoolSelectSongKsf.load(decriptorFullcombo, 0, pathFullcombo.length(), 1)

        val pathNoMiss = File("$pathSounds/no_miss.mp3")
        val decriptorNoMiss = FileInputStream(pathNoMiss).fd
        no_miss = soundPoolSelectSongKsf.load(decriptorNoMiss, 0, pathNoMiss.length(), 1)
    }
}

data class Ksf(var rutaKsf: String = "",
               var level: String = "",
               var rutaBitActive: String = "",
               var stepmaker: String = "",
               var typePlayer: String = "",
               var checkedValues: String = "",
               var typeSteps: String = "")

data class SongKsf(
    var title: String,
    var artist: String,
    var displayBpm: String,
    var rutaDisc: String,
    var rutaTitle: String,
    var rutaSong: String,
    var rutaPreview: String,
    var rutaBGA: String,
    var listKsf: ArrayList<Ksf>,
    var offset: Long = 0L,
    var channel: String = "",
    var isFavorite: Boolean = false)