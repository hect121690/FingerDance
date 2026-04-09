package com.fingerdance.ssc

import android.content.Context
import android.util.Log
import com.fingerdance.Channels
import com.fingerdance.Ksf
import com.fingerdance.Song
import com.fingerdance.tema
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.text.Normalizer
import kotlin.collections.sortWith

class LoadingSongs () {

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
        var credit = ""
        var listLevels = arrayListOf<Ksf>()
        val listSongs = ArrayList<Song>()
        val rutaBitActiveSingle = c.getExternalFilesDir("/FingerDance/Themes/${tema}/GraphicsStatics/img_lv.png").toString()
        val rutaBitActiveHalfDouble = c.getExternalFilesDir("/FingerDance/Themes/${tema}/GraphicsStatics/img_lv_hd.png").toString()

        val typeStepsOrder = mapOf(
            "NORMAL" to 0,
            "UCS" to 1,
            "ANOTHER" to 2,
            "QUEST" to 3,
            "NEW" to 4,
            "RISE" to 5
        )

        val listRutas = getRutasSongs(rutaChannel)
        for(index in 0 until listRutas.size) {
            val dir = File(listRutas[index])
            if (dir != null) {
                dir.walkTopDown().forEach {
                    if (it.toString().endsWith(".ssc", true)){
                        ssc = readFile(it.toString())
                        val seccions = ssc.split("#NOTEDATA:;").toTypedArray()
                        val arr = seccions[0].split(Regex("\\r?\\n"))
                        val file = File(listRutas[index])

                        for (e in 0 until arr.size) {
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
                                    rutaCancion = resolveRealFile(File(listRutas[index]), song)
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
                                arr[e].startsWith("#CREDIT:") -> {
                                    credit = getValue(arr[e])
                                }
                            }
                        }
                        if(rutaDisc.split("/").last() == "" || !isFileExists(File(rutaDisc))){
                            rutaDisc = rutaBanner
                        }
                        listLevels = arrayListOf<Ksf>()
                        for(index in 1 until seccions.size){
                            var numberLevel = ""
                            var rutaBitlevel = rutaBitActiveSingle
                            var noteString = seccions[index]
                            var typePlayer = "A"
                            var typeSteps = "NORMAL"
                            val arr  = seccions[0].split(Regex("\\r?\\n"))
                            for(i in 0 until arr.size -1){
                                when {
                                    arr[i].startsWith("#STEPSTYPE:") -> {
                                        typePlayer = getValue(arr[i])
                                    }
                                    arr[i].startsWith("#DESCRIPTION:") ->{
                                        typeSteps = getValue(arr[i])
                                    }
                                    arr[i].startsWith("#METER:") -> {
                                        numberLevel = getValue(arr[i]).padStart(2, '0')
                                    }
                                    arr[i].startsWith("#NOTES:") -> {
                                        break
                                    }
                                }
                            }
                            if(typePlayer.equals("pump-single", true) || typePlayer.equals("pump-half-double", true) || typePlayer.equals("pump-halfdouble", true)){
                                if(typePlayer.contains("double", true)){
                                    typePlayer = "B"
                                    rutaBitlevel = rutaBitActiveHalfDouble
                                }else{
                                    typePlayer = "A"
                                    rutaBitlevel = rutaBitActiveSingle
                                }
                                listLevels.add(
                                    Ksf(
                                        level = numberLevel,
                                        rutaBitActive = rutaBitlevel,
                                        steps = noteString,
                                        typePlayer = typePlayer,
                                        typeSteps = typeSteps,
                                        stepmaker = credit
                                    )
                                )
                            }
                            listLevels.sortWith(
                                compareBy<Ksf>(
                                    { it.typePlayer == "B" },
                                    { it.level },
                                    { typeStepsOrder[it.typeSteps] ?: 99 }
                                )
                            )
                        }
                        listSongs.add(
                            Song(
                                title = name,
                                artist = artist,
                                displayBpm = displayBpm,
                                rutaDisc = rutaDisc,
                                rutaTitle = rutaBanner,
                                rutaSong = rutaCancion,
                                rutaPreview = rutaPrevVideo,
                                rutaBGA = rutaBga,
                                listKsf = listLevels,
                                channel = file.parentFile.name,
                                isSSC = true

                            )
                        )
                    }
                }
            }
        }
        return listSongs
    }

    private fun isFileExists(file: File): Boolean {
        return file.exists() && !file.isDirectory
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
        return line
            .substringAfter(":")
            .substringBefore(";")
            .replace("\uFEFF", "") // BOM
            .replace("\r", "")
            .replace("\n", "")
            .trim()
    }

    fun resolveRealFile(dir: File, targetName: String): String {
        val files = dir.listFiles()
        Log.d("FILE:","[$targetName] (${targetName.length})")
        // 1. Intento exacto primero (rápido)
        files?.forEach {
            if (it.name == targetName) {
                return it.absolutePath
            }
        }

        // 2. Intento normalizado (acentos, ñ, etc.)
        val normalizedTarget = normalize(targetName)

        files?.forEach {
            if (normalize(it.name) == normalizedTarget) {
                return it.absolutePath
            }
        }

        // 3. Intento contains (fallback tolerante)
        files?.forEach {
            if (normalize(it.name).contains(normalizedTarget)) {
                return it.absolutePath
            }
        }
        return ""
    }

    fun normalize(input: String): String {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace("ñ", "n")
            .replace("Ñ", "N")
            .lowercase()
            .trim()
            .replace("\\s+".toRegex(), " ")
            .replace("[^a-z0-9. ]".toRegex(), "")
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
        val bytes = Files.readAllBytes(Paths.get(path))

        // Intento 1: UTF-8
        val utf8 = String(bytes, Charsets.UTF_8)

        // Si hay caracteres corruptos, fallback
        return if (utf8.contains("�")) {
            String(bytes, Charsets.ISO_8859_1)
        } else {
            utf8
        }
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