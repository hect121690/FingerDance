package com.fingerdance

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

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
            var listSongs: ArrayList<Song>

            for (index in 0 until listRutasChannels.size) {
                nombre = listRutasChannels[index].removeRange(0, 82)
                descripcion = readFile(listRutasChannels[index] + "/info/text.ini")
                banner = listRutasChannels[index] + "/banner.png"
                rutaChannel = listRutasChannels[index]
                listSongs = getSongs(rutaChannel, context)
                channel = Channels(nombre, descripcion, banner, listSongs)

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

                val channel = Channels(nombre, descripcion, banner, listSongs)
                listChannels.add(channel)
            }
        }
        return listChannels
    }

    private fun getSongs(rutaChannel: String, c: Context): ArrayList<Song> {

        val listSongs = ArrayList<Song>()
        val rutaBitActive = c.getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/img_lv.png").toString()
        val rutaBitActiveHalfDouble = c.getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/img_lv_hd.png").toString()
        val typeStepsOrder = mapOf(
            "NORMAL" to 0,
            "UCS" to 1,
            "ANOTHER" to 2,
            "QUEST" to 3,
            "NEW" to 4,
            "RISE" to 5
        )
        val listRutas = getRutasSongs(rutaChannel)
        val listOthers = arrayListOf("03-SHORT CUT - V2", "04-REMIX - V2", "05-FULLSONGS - V2")

        for(index in 0 until listRutas.size){
            val listKsf = arrayListOf<Ksf>()
            val songKsf = Song()
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
                            val ksf = Ksf(steps = "")
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
                                        line.startsWith("#SONGFILE:") ->{
                                            ksf.songFile = getValue(line)
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

    private fun readFile(path: String): String {
        val encoded = Files.readAllBytes(Paths.get(path))
        return String(encoded, StandardCharsets.UTF_8)
    }

    private fun isFileExists(file: File): Boolean {
        return file.exists() && !file.isDirectory
    }

}


