package com.fingerdance.ssc

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.fingerdance.Channels
import com.fingerdance.Ksf
import com.fingerdance.Song
import com.fingerdance.generateCheckedValuesSsc
import com.fingerdance.readFileSsc
import com.fingerdance.tema
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.text.Normalizer

class LoadingSongs {

    fun getChannels(c: Context): ArrayList<Channels> {
        val dir = c.getExternalFilesDir("/FingerDance/Songs/Channels/")
        val listChannels = ArrayList<Channels>()
        val rutas = mutableListOf<String>()

        dir?.walkTopDown()?.forEach {
            if (it.isDirectory && it.name.equals("info", true)) {
                rutas.add(it.parentFile.absolutePath)
            }
        }

        rutas.sort()

        for (ruta in rutas) {
            val nombre = File(ruta).name
            val descripcion = readFileSsc("$ruta/info/text.ini")
            val banner = "$ruta/banner.png"
            val songs = getSongs(ruta, c)

            listChannels.add(Channels(nombre, descripcion, banner, songs))
        }

        return listChannels
    }

    // SOLO muestro getSongs que es donde estaba el problema

    private fun getSongs(rutaChannel: String, c: Context): ArrayList<Song> {

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

        for (ruta in listRutas) {

            val dir = File(ruta)
            val sscFiles = dir.listFiles { f -> f.absolutePath.endsWith("ssc", true) } ?: continue
            val imgs = dir.listFiles { i -> i.extension.equals("png", true) || i.extension.equals("jpg", true) }
            for (fileSSC in sscFiles) {

                // 🔹 VARIABLES LOCALES (FIX)
                var name = ""
                var artist = ""
                var displayBpm = ""
                var rutaDisc = ""
                var rutaBanner = ""
                var rutaPreview = ""
                var rutaCancion = ""
                var rutaBga = ""

                val ssc = readFileSsc(fileSSC.absolutePath)

                val seccions = ssc.split("#NOTEDATA:;")
                val arr = seccions[0].split(Regex("\\r?\\n"))

                for (lineRaw in arr) {

                    val line = lineRaw.trim()

                    when {
                        line.startsWith("#TITLE:") -> name = getValue(line)
                        line.startsWith("#ARTIST:") -> artist = getValue(line)

                        line.startsWith("#MUSIC:") -> {
                            val song = getValue(line)
                            rutaCancion = resolveRealFile(dir, song)
                            if(line.contains("/")){
                                val songSplit = line.split("/")
                                rutaCancion = resolveRealFile(dir, songSplit.last())
                            }

                            if (rutaCancion.endsWith(".mp3", true) || rutaCancion.endsWith(".ogg", true)) {
                                rutaBga = "$ruta/${rutaCancion.substringBeforeLast(".")}.mp4"
                            }
                        }

                        line.startsWith("#PREVIEW") -> {
                            rutaPreview = "$ruta/${getValue(line)}"
                            if(line.contains("/") || line.contains("..")){
                                val prevSplit = line.split("/")
                                rutaPreview = "$ruta/${prevSplit.last()}"
                            }
                        }

                        line.startsWith("#DISPLAYBPM:") -> {
                            if(name == "Switronic"){
                                name
                            }
                            displayBpm = getDisplayBpm(line)
                        }

                        line.startsWith("#BPMS:") -> {
                            if(displayBpm == "") {
                                if(line.contains("=", true)) {
                                    displayBpm = extractBpmsInHeader(line)
                                }else{
                                    displayBpm = getDisplayBpm(line)
                                }
                            }
                        }
                    }
                }

                if(rutaPreview.endsWith("mpg", true)){
                    when {
                        rutaCancion.endsWith(".mp3", true) -> {
                            rutaPreview = rutaCancion.replace(".mp3", "_p.mp4", ignoreCase = true)
                        }
                        rutaCancion.endsWith(".ogg", true) -> {
                            rutaPreview = rutaCancion.replace(".ogg", "_p.mp4", ignoreCase = true)
                        }
                    }
                }

                val listLevels = arrayListOf<Ksf>()

                // 🔹 NOTEDATA (INTACTO)
                for (index in 1 until seccions.size) {

                    var numberLevel = ""
                    var chartName = ""
                    var typePlayer = ""
                    var typeSteps = "NORMAL"
                    var checkedValues = ""
                    var credit = ""

                    val arr2 = seccions[index].split(Regex("\\r?\\n"))

                    for (lineRaw in arr2) {

                        val line = lineRaw.trim()

                        when {
                            line.startsWith("#STEPSTYPE:") -> typePlayer = getValue(line)
                            line.startsWith("#DESCRIPTION:") -> typeSteps = getValue(line)
                            line.startsWith("#METER:") -> numberLevel = getValue(line).padStart(2, '0')
                            line.startsWith("#CHARTNAME:") -> chartName = getValue(line)
                            line.startsWith("#CREDIT:") -> credit = getValue(line)
                            line.startsWith("#NOTES:") -> break
                        }
                    }

                    if (displayBpm == "") {
                        displayBpm = extractFromBPMS(arr2.toList())
                    }

                    if (displayBpm.toDoubleOrNull()!! < 10.0) {
                        displayBpm = extractFromBPMS(arr2.toList())
                    }

                    if (
                        typePlayer.contains("single", true) ||
                        typePlayer.contains("half", true)
                    ) {

                        val player = if (typePlayer.contains("half", true)) "B" else "A"
                        checkedValues = generateCheckedValuesSsc(seccions[index]) + "|${File(rutaCancion).length()}"
                        val icon = if (player == "B") rutaBitActiveHalfDouble else rutaBitActiveSingle

                        listLevels.add(
                            Ksf(
                                level = numberLevel,
                                rutaBitActive = icon,
                                steps = index,
                                typePlayer = player,
                                typeSteps = typeSteps,
                                checkedValues = checkedValues,
                                stepmaker = credit,
                                chartName = chartName,
                            )
                        )
                    }
                }

                // 🔹 SORT CORREGIDO
                listLevels.sortWith(
                    compareBy<Ksf>(
                        { it.typePlayer == "B" },
                        { it.level.toIntOrNull() ?: 0 },
                        { typeStepsOrder[it.typeSteps.uppercase()] ?: 99 }
                    )
                )

                if (rutaCancion.isEmpty()) continue

                if(imgs != null && imgs.size > 0){
                    if(imgs.size == 1){
                        rutaBanner = imgs[0].absolutePath
                        rutaDisc = imgs[0].absolutePath
                    }else{
                        var minPixels = Int.MAX_VALUE
                        var maxPixels = 0

                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }

                        for (file in imgs) {
                            BitmapFactory.decodeFile(file.absolutePath, options)
                            if (options.outWidth <= 0 || options.outHeight <= 0) continue
                            val pixels = options.outWidth * options.outHeight
                            if (pixels < minPixels) {
                                minPixels = pixels
                                rutaDisc = file.absolutePath
                            }
                            if (pixels > maxPixels) {
                                maxPixels = pixels
                                rutaBanner = file.absolutePath
                            }
                        }
                    }
                }

                listSongs.add(
                    Song(
                        title = name,
                        artist = artist,
                        displayBpm = displayBpm,
                        rutaDisc = rutaDisc,
                        rutaTitle = rutaBanner,
                        rutaSong = rutaCancion,
                        rutaPreview = rutaPreview,
                        rutaBGA = rutaBga,
                        listKsf = listLevels,
                        channel = dir.parentFile.name,
                        isSSC = true,
                        rutaSsc = fileSSC.absolutePath
                    )
                )
            }
        }

        return listSongs
    }

    private fun extractFromBPMS(header: List<String>): String {
        val line = header.find { it.startsWith("#BPMS:") } ?: return "0.00"
        val bpm = line.substringAfter("=").substringBefore(",").substringBefore(";")
        return "%.2f".format(bpm.toDoubleOrNull() ?: 0.0)
    }

    private fun extractBpmsInHeader(line: String): String {
        val bpm = line.substringAfter("=").substringBefore(",").substringBefore(";")
        return "%.2f".format(bpm.toDoubleOrNull() ?: 0.0)
    }

    private fun isFileExists(file: File): Boolean {
        return file.exists() && !file.isDirectory
    }

    private fun getDisplayBpm(line: String): String {
        val bpm = line.substringAfter(":").substringBefore(";")
        if(bpm.contains(":") || bpm.contains("-")){
            return "%.2f".format(bpm.substringAfter(":").toDoubleOrNull())
        }
        return "%.2f".format(bpm.toDoubleOrNull() ?: 0.0)
    }

    private fun getValue(line: String): String {
        return line.substringAfter(":")
            .substringBefore(";")
            .replace("\uFEFF", "")
            .trim()
    }

    fun resolveRealFile(dir: File, targetName: String): String {
        val files = dir.listFiles() ?: return ""

        files.firstOrNull { it.name == targetName }?.let { return it.absolutePath }

        val target = normalize(targetName)

        files.firstOrNull { normalize(it.name) == target }?.let { return it.absolutePath }
        files.firstOrNull { normalize(it.name).contains(target) }?.let { return it.absolutePath }

        return ""
    }

    fun normalize(input: String): String {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()
            .replace("\\s+".toRegex(), " ")
            .replace("[^a-z0-9._\\- ]".toRegex(), "")
            .trim()
    }

    private fun getRutasSongs(rutaChannel: String): MutableList<String> {
        val dir = File(rutaChannel)
        val list = mutableListOf<String>()

        dir.listFiles()?.forEach {
            if (it.isDirectory && !it.name.equals("info_ssc", true)) {
                list.add(it.absolutePath)
            }
        }

        return list
    }
}