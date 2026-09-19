package com.fingerdance.ssc

import android.content.Context
import android.graphics.BitmapFactory
import com.fingerdance.Channels
import com.fingerdance.Ksf
import com.fingerdance.Song
import com.fingerdance.generateCheckedValuesSsc
import com.fingerdance.generateId
import com.fingerdance.readFileSsc
import com.fingerdance.tema
import java.io.File
import java.text.Normalizer

class LoadingSongs {

    fun getChannels(c: Context, onProgress: ((channel: String, song: String) -> Unit)? = null): ArrayList<Channels> {
        val dir = c.getExternalFilesDir("/FingerDance/Songs/Channels/")
        val listChannels = ArrayList<Channels>()

        if (dir == null) {
            return listChannels
        }

        if (!dir.exists()) {
            dir.mkdirs()
        }

        val rutas = dir.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name.lowercase() } ?: emptyList()

        for (channelDir in rutas) {
            val hasInfoKsf = channelDir.listFiles()?.any {
                it.isDirectory && it.name.equals("info_ksf", ignoreCase = true)
            } == true

            if (hasInfoKsf) {
                continue
            }

            val ruta = channelDir.absolutePath
            val nombre = channelDir.name

            onProgress?.invoke(nombre, "")

            var infoDir = channelDir.listFiles()?.firstOrNull {
                it.isDirectory && it.name.equals("info", ignoreCase = true)
            }

            if (infoDir == null) {
                infoDir = File(channelDir, "info")

                if (!infoDir.mkdirs() && !infoDir.exists()) {
                    android.util.Log.e("LoadingSongs", "No se pudo crear info en: ${channelDir.absolutePath}")
                    continue
                }
            }

            var textIni = infoDir.listFiles()?.firstOrNull {
                it.isFile && it.name.equals("text.ini", ignoreCase = true)
            }

            val defaultDescription = "En este canal encontraras canciones de $nombre"

            if (textIni == null) {
                textIni = File(infoDir, "text.ini")

                try {
                    textIni.writeText(defaultDescription)
                } catch (e: Exception) {
                    android.util.Log.e("LoadingSongs", "No se pudo crear text.ini para $nombre", e)
                }
            } else {
                val contenidoActual = try {
                    textIni.readText().trim()
                } catch (e: Exception) {
                    ""
                }

                if (contenidoActual.isBlank()) {
                    try {
                        textIni.writeText(defaultDescription)
                    } catch (e: Exception) {
                        android.util.Log.e("LoadingSongs", "No se pudo completar text.ini para $nombre", e)
                    }
                }
            }

            val descripcion = if (textIni.exists()) {
                readFileSsc(textIni.absolutePath).ifBlank { defaultDescription }
            } else {
                defaultDescription
            }

            val banner = "$ruta/banner.png"
            val songs = getSongs(rutaChannel = ruta, c = c, channelName = nombre, onProgress = onProgress)

            listChannels.add(Channels(nombre, descripcion, banner, songs))
        }

        return listChannels
    }

    private fun getSongs(
        rutaChannel: String,
        c: Context,
        channelName: String,
        onProgress: ((channel: String, song: String) -> Unit)?
    ): ArrayList<Song> {

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
        val listOrderBy = arrayListOf<String>()

        val sortFile = File(rutaChannel).listFiles()?.firstOrNull {
            it.isFile && it.name.equals("Sort.txt", ignoreCase = true)
        }

        sortFile?.readLines()?.forEach {
            if (it.isNotBlank() && !it.startsWith("-")) {
                listOrderBy.add(it.trim())
            }
        }

        for (ruta in listRutas) {
            val dir = File(ruta)
            val sscFiles = dir.listFiles { f -> f.absolutePath.endsWith("ssc", true) } ?: continue
            val imgs = dir.listFiles { i -> i.extension.equals("png", true) || i.extension.equals("jpg", true) }

            for (fileSSC in sscFiles) {
                var name = ""
                var artist = ""
                var displayBpm = ""
                var rutaDisc = ""
                var rutaBanner = ""
                var rutaPreview = ""
                var rutaCancion = ""
                var rutaBga = ""
                var bpmsLine = ""

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

                            if (line.contains("/")) {
                                val songSplit = line.split("/")
                                rutaCancion = resolveRealFile(dir, songSplit.last())
                            }
                        }

                        line.startsWith("#DISPLAYBPM:", true) -> {
                            displayBpm = parseDisplayBpm(line) ?: ""
                        }

                        line.startsWith("#BPMS:", true) -> {
                            bpmsLine = line
                        }
                    }
                }

                onProgress?.invoke(channelName, name.ifBlank { dir.name })

                rutaBga = "$ruta/song.mp4"
                rutaPreview = "$ruta/song_p.mp4"

                val listLevels = arrayListOf<Ksf>()

                for (index in 1 until seccions.size) {
                    var numberLevel = ""
                    var chartName = ""
                    var typePlayer = ""
                    var typeSteps = "NORMAL"
                    var checkedValues = ""
                    var credit = ""
                    var difficulty = ""
                    var songFile = ""

                    val arr2 = seccions[index].split(Regex("\\r?\\n"))

                    for (lineRaw in arr2) {
                        val line = lineRaw.trim()

                        when {
                            line.startsWith("#STEPSTYPE:") -> typePlayer = getValue(line)
                            line.startsWith("#DESCRIPTION:") -> typeSteps = getValue(line)
                            line.startsWith("#METER:") -> numberLevel = getValue(line).padStart(2, '0')
                            line.startsWith("#CHARTNAME:") -> chartName = getValue(line)
                            line.startsWith("#CREDIT:") -> credit = getValue(line)
                            line.startsWith("#DIFFICULTY:") -> difficulty = getValue(line)
                            line.startsWith("#DISPLAYBPM:") -> displayBpm = parseDisplayBpm(line) ?: ""
                            line.startsWith("#MUSIC:") -> songFile = getValue(line)
                            line.startsWith("#NOTES:") -> break
                        }
                    }

                    if (numberLevel == "") {
                        numberLevel = difficulty.toIntOrNull()?.toString() ?: "0"
                    }

                    if (displayBpm.isBlank() && bpmsLine.isNotBlank()) {
                        displayBpm = extractDisplayBpmFromBpms(listOf(bpmsLine))
                    }

                    if (typePlayer.contains("single", true) || typePlayer.contains("half", true)) {
                        val player = if (typePlayer.contains("half", true)) "B" else "A"
                        val uniqueId = generateId("${numberLevel}|${typeSteps}|${player}|${chartName}|${credit}|${difficulty}")
                        checkedValues = generateCheckedValuesSsc(seccions[index]) + "|${File(rutaCancion).length()}-$uniqueId"
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
                                difficulty = difficulty,
                                songFile = songFile
                            )
                        )
                    }
                }

                listLevels.sortWith(
                    compareBy<Ksf>(
                        { it.typePlayer == "B" },
                        { it.level.toIntOrNull() ?: 0 },
                        { typeStepsOrder[it.typeSteps.uppercase()] ?: 99 }
                    )
                )

                val isValidAudio = rutaCancion.endsWith(".mp3", true) || rutaCancion.endsWith(".ogg", true)

                if (rutaCancion.isEmpty() || !isValidAudio) {
                    val audioFile = dir.listFiles()?.firstOrNull { file ->
                        file.isFile && (file.extension.equals("mp3", ignoreCase = true) || file.extension.equals("ogg", ignoreCase = true))
                    }

                    if (audioFile != null) {
                        rutaCancion = audioFile.absolutePath
                    }
                }

                if (rutaCancion.isEmpty()) continue

                if (imgs != null && imgs.isNotEmpty()) {
                    if (imgs.size == 1) {
                        rutaBanner = imgs[0].absolutePath
                        rutaDisc = imgs[0].absolutePath
                    } else {
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

        if (listOrderBy.isNotEmpty()) {
            val orderMap = listOrderBy.withIndex().associate { (index, name) -> name.lowercase() to index }
            val folderNameBySong = listSongs.associateWith { song ->
                File(song.rutaSong).parentFile?.name.orEmpty()
            }

            listSongs.sortWith(
                compareBy<Song>(
                    { song ->
                        val folderName = folderNameBySong[song].orEmpty()
                        orderMap[folderName.lowercase()] ?: Int.MAX_VALUE
                    },
                    { song ->
                        folderNameBySong[song].orEmpty().lowercase()
                    }
                )
            )
        } else {
            listSongs.sortWith(
                compareBy { song ->
                    song.title.lowercase()
                }
            )
        }

        return listSongs
    }

    private fun formatBpm(value: Double): String {
        val rounded = String.format(java.util.Locale.US, "%.2f", value)

        return if (rounded.endsWith(".00")) {
            rounded.dropLast(3)
        } else {
            rounded.trimEnd('0').trimEnd('.')
        }
    }

    private fun parseDisplayBpm(line: String): String? {
        val value = getValue(line)

        if (value.isBlank()) return null
        if (value == "*") return null

        val separator = when {
            value.contains(":") -> ":"
            value.contains("-") -> "-"
            else -> null
        }

        if (separator != null) {
            val parts = value.split(separator).map { it.trim() }

            if (parts.size >= 2) {
                val min = parts[0].toDoubleOrNull()
                val max = parts[1].toDoubleOrNull()

                if (min != null && max != null) {
                    return "${formatBpm(min)}-${formatBpm(max)}"
                }
            }

            return null
        }

        return value.toDoubleOrNull()?.let {
            formatBpm(it)
        }
    }

    private fun extractDisplayBpmFromBpms(lines: List<String>): String {
        val bpms = mutableListOf<Double>()

        lines.forEach { line ->
            if (!line.startsWith("#BPMS:", true)) return@forEach

            val values = line.substringAfter(":").substringBefore(";").split(",")

            values.forEach { entry ->
                val bpm = entry.substringAfter("=").trim().toDoubleOrNull()

                if (bpm != null) {
                    bpms.add(bpm)
                }
            }
        }

        if (bpms.isEmpty()) {
            return "0"
        }

        val min = bpms.minOrNull() ?: return "0"
        val max = bpms.maxOrNull() ?: return "0"

        return if (min == max) {
            formatBpm(min)
        } else {
            "${formatBpm(min)}-${formatBpm(max)}"
        }
    }

    private fun getValue(line: String): String {
        return line.substringAfter(":")
            .substringBefore(";")
            .replace("\uFEFF", "")
            .trim()
    }

    fun resolveRealFile(dir: File, targetName: String): String {
        val files = dir.listFiles() ?: return ""

        files.firstOrNull { it.name == targetName }?.let {
            return it.absolutePath
        }

        val target = normalize(targetName)

        files.firstOrNull { normalize(it.name) == target }?.let {
            return it.absolutePath
        }

        files.firstOrNull { normalize(it.name).contains(target) }?.let {
            return it.absolutePath
        }

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
            if (
                it.isDirectory &&
                !it.name.equals("info", true) &&
                !it.name.equals("info_ksf", true)
            ) {
                list.add(it.absolutePath)
            }
        }

        return list
    }
}