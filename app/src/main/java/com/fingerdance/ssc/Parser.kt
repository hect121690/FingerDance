package com.fingerdance.ssc

import com.fingerdance.KsfProccess.LuaVisualEvent
import com.fingerdance.VisualTarget
import java.io.File
import kotlin.math.max

class Parser {

    // =========================
    // DATA
    // =========================

    enum class NoteType { TAP, HOLD, MINE }

    data class Note(
        val column: Int,
        val beat: Double,
        val endBeat: Double? = null,
        val isFake: Boolean = false,
        val isVanish: Boolean = false,
        val isPhantom: Boolean = false,
        val type: NoteType
    )

    data class BpmSegment(val beat: Double, val bpm: Double)
    data class TickCountSegment(val beat: Double, val tickcount: Int)
    data class Stop(val beat: Double, val durationMs: Double)
    data class Delay(val beat: Double, val durationMs: Double)
    data class Warp(val beat: Double, val duration: Double)
    data class Fake(val beat: Double, val duration: Double)

    data class Speed(val beat: Double, val ratio: Double, val duration: Double, val mode: Int)
    data class Scroll(val beat: Double, val ratio: Double)

    private var luaFileName: String? = null
    private val luaEvents = mutableListOf<LuaVisualEvent>()

    data class Chart(
        val offset: Double = 0.0,
        val bpms: List<BpmSegment>,
        val tickcounts: List<TickCountSegment>,
        val stops: List<Stop>,
        val delays: List<Delay>,
        val warps: List<Warp>,
        val fakes: List<Fake>,
        val speeds: List<Speed>,
        val scrolls: List<Scroll>,
        var notes: List<Note>,
        val luaEvents: List<LuaVisualEvent> = emptyList()
    )

    // =========================
    // PUBLIC
    // =========================

    fun parseSSC(text: String, pathFile: String): Chart {
        val offset = extractTag(text, "OFFSET")?.toDoubleOrNull() ?: 0.0
        luaFileName = extractTag(text, "LUA")
        val bpms = parsePairs(text, "BPMS").map { BpmSegment(it.first, it.second) }
        val tickcounts = parsePairs(text, "TICKCOUNTS").map { TickCountSegment(it.first, it.second.toInt()) }
        val stops = parsePairs(text, "STOPS").map { Stop(it.first, it.second * 1000) }
        val delays = parsePairs(text, "DELAYS").map { Delay(it.first, it.second * 1000) }
        val warps = parsePairs(text, "WARPS").map { Warp(it.first, it.second) }
        val fakes = parsePairs(text, "FAKES").map { Fake(it.first, it.second) }

        val speeds = parseSpeeds(text)
        val scrolls = parseScrolls(text)

        val (notes, extendedNotes) = parseNotes(text, fakes)
        val allNotes = (notes + extendedNotes).sortedBy { it.beat }
        loadLuaEvents(pathFile)

        return Chart(
            offset = offset,
            bpms = bpms,
            tickcounts = tickcounts,
            stops = stops,
            delays = delays,
            warps = warps,
            fakes = fakes,
            speeds = speeds,
            scrolls = scrolls,
            notes = allNotes,
            luaEvents = luaEvents
        )
    }

    // =========================
    // NOTES (con soporte correcto de FAKE)
    // =========================

    private fun parseNotes(text: String, fakes: List<Fake>): Pair<List<Note>, List<Note>> {

        val notes = mutableListOf<Note>()          // jugables normales
        val extendedNotes = mutableListOf<Note>()  // de tokens extendidos

        val holds = mutableMapOf<Int, Double>()    // holds HEAD '2'
        val phantomHolds = mutableMapOf<Int, Double>() // HEAD '6' (fantom rolls)
        val extHolds = mutableMapOf<Int, Double>() // holds de tokens extendidos 2/3

        val block = extractNotesBlock(text) ?: return notes to extendedNotes
        val measures = block.split(",")

        var currentBeat = 0.0

        for (measure in measures) {

            val rows = measure.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("//") }

            val step = 4.0 / max(rows.size, 1)

            for ((i, rowRaw) in rows.withIndex()) {

                val (row, extTokens) = tokenize(rowRaw)
                val beat = currentBeat + i * step

                // 1) Procesar tokens extendidos ({...} excepto 108)
                for ((col, data) in extTokens) {
                    val (code, isVanish) = data
                    when (code) {
                        '1' -> {
                            extendedNotes.add(
                                Note(
                                    column = col,
                                    beat = beat,
                                    isFake = false,
                                    isVanish = isVanish,
                                    isPhantom = false,
                                    type = NoteType.TAP
                                )
                            )
                        }
                        '2' -> {
                            extHolds[col] = beat
                        }

                        '3' -> {
                            val startBeat = extHolds[col] ?: continue

                            extendedNotes.add(
                                Note(
                                    column = col,
                                    beat = startBeat,
                                    endBeat = beat,
                                    isFake = false,
                                    isVanish = isVanish,
                                    isPhantom = false,
                                    type = NoteType.HOLD
                                )
                            )

                            extHolds.remove(col)
                        }

                        'M', 'm' -> {
                            notes.add(
                                Note(
                                    column = col,
                                    beat = beat,
                                    isFake = false,
                                    isVanish = isVanish,
                                    isPhantom = false,
                                    type = NoteType.MINE
                                )
                            )
                        }
                    }
                }

                // 2) Procesar tokens planos (0,1,2,3,5,6,M,m, etc.)
                for (col in 0 until row.size) {
                    when (row[col]) {
                        'F', 'f' -> {
                            notes.add(
                                Note(
                                    column = col,
                                    beat = beat,
                                    isFake = true,
                                    isPhantom = false,
                                    type = NoteType.TAP
                                )
                            )
                        }
                        '1' -> {
                            val fake = isFake(beat, fakes)
                            notes.add(
                                Note(
                                    column = col,
                                    beat = beat,
                                    isFake = fake,
                                    isPhantom = false,
                                    type = NoteType.TAP
                                )
                            )
                        }
                        '2' -> {
                            holds[col] = beat
                        }
                        '3' -> {
                            // Normal: cierra hold iniciado con '2'
                            if (holds.contains(col)) {
                                val startBeat = holds[col]!!
                                val isStartFake = isFake(startBeat, fakes)
                                val isEndFake = isFake(beat, fakes)
                                val fake = isStartFake || isEndFake
                                notes.add(
                                    Note(
                                        column = col,
                                        beat = startBeat,
                                        endBeat = beat,
                                        isFake = fake,
                                        isPhantom = false,
                                        type = NoteType.HOLD
                                    )
                                )
                                holds.remove(col)
                            }
                            // PHANTOM: cierra hold iniciado con '6'
                            else if (phantomHolds.contains(col)) {
                                val startBeat = phantomHolds[col]!!
                                notes.add(
                                    Note(
                                        column = col,
                                        beat = startBeat,
                                        endBeat = beat,
                                        isFake = false,
                                        isPhantom = true,
                                        type = NoteType.HOLD
                                    )
                                )
                                phantomHolds.remove(col)
                            }
                        }
                        '5' -> {
                            // PHANTOM NOTE: tail sola, tap "fantasma"
                            notes.add(
                                Note(
                                    column = col,
                                    beat = beat,
                                    isFake = false,
                                    isPhantom = true,
                                    type = NoteType.TAP
                                )
                            )
                        }
                        '6' -> {
                            // PHANTOM: empieza phantom hold
                            phantomHolds[col] = beat
                        }
                        'M', 'm' -> {
                            val fake = isFake(beat, fakes)
                            notes.add(
                                Note(
                                    column = col,
                                    beat = beat,
                                    isFake = fake,
                                    isPhantom = false,
                                    type = NoteType.MINE
                                )
                            )
                        }
                        'X' -> {
                            // Ignora
                        }
                        else -> {
                            // '0' y cualquier otro carácter => nada
                        }
                    }
                }
            }
            currentBeat += 4.0
        }
        return notes to extendedNotes
    }

    // =========================
    // TOKENIZER
    // =========================

    /**
     * Devuelve:
     *  - tokens planos (List<Char>) para la lógica normal de notas
     *  - meta extendido por columna (Map<col, codeChar>) para tokens {code|...}
     */
    private fun tokenize(row: String): Pair<List<Char>, Map<Int, Pair<Char, Boolean>>> {

        val result = mutableListOf<Char>()

        // 👇 ahora guardamos: code + isVanish
        val extTokens = mutableMapOf<Int, Pair<Char, Boolean>>()

        var i = 0
        var colIndex = 0

        while (i < row.length) {

            val ch = row[i]

            if (ch == '{') {

                val end = row.indexOf('}', i)

                if (end != -1) {

                    val inside = row.substring(i + 1, end)
                    // ejemplo: "2|v|0|0"

                    val parts = inside.split("|")

                    if (parts.size == 1) {
                        // Caso {108} u otros códigos simples

                        val code = parts[0]

                        if (code == "108") {
                            // ignorado completamente
                        }

                    } else {
                        // Formato extendido: "2|v|0|0"

                        val codeChar = parts[0].firstOrNull() ?: ' '

                        // 👇 detectar vanish
                        val modifier = parts.getOrNull(1)
                        val isVanish = modifier == "v"

                        // 👇 guardamos ambos
                        extTokens[colIndex] = codeChar to isVanish

                        // consume columna
                        colIndex++
                    }

                    i = end + 1
                    continue
                }
            }

            // Carácter plano normal
            result.add(ch)
            colIndex++
            i++
        }

        return result to extTokens
    }

    // =========================
    // HELPERS
    // =========================

    private fun loadLuaEvents(ksfPath: String) {
        val luaName = luaFileName ?: return

        val luaFile = File(File(ksfPath).parentFile, luaName)
        if (!luaFile.exists()) return

        luaFile.forEachLine { line ->
            val clean = line.trim()
            if (clean.isEmpty() || clean.startsWith("--")) return@forEachLine

            // split: setRecept(...) , 52000
            val parts = clean.split("),")
            if (parts.size != 2) return@forEachLine

            val callPart = parts[0] + ")"
            val beat = parts[1].trim().toFloat()

            // 🔹 nombre de la función
            val funcName = callPart.substringBefore("(").trim()

            val target = when (funcName) {
                "setRecept" -> VisualTarget.RECEPTOR
                "setNotes"  -> VisualTarget.NOTES
                else -> return@forEachLine
            }

            // args
            val nameAndArgs = callPart.substringAfter("(").substringBefore(")")
            val args = nameAndArgs.split(",")

            val paramMap = mutableMapOf<String, Float>()
            var duration = 0F

            args.forEach {
                val pair = it.split("=")
                if (pair.size == 2) {
                    val key = pair[0].trim()
                    val value = pair[1].trim().toFloat()

                    if (key == "time") {
                        duration = value.toFloat()
                    } else {
                        paramMap[key] = value
                    }
                }
            }

            luaEvents.add(
                LuaVisualEvent(
                    startBeat = beat,
                    durationBeat = duration,
                    target = target,
                    params = paramMap
                )
            )
        }
    }

    private fun parseSpeeds(text: String): List<Speed> {
        val raw = extractTag(text, "SPEEDS") ?: return emptyList()

        return raw
            .replace(";", "") // quitar terminador
            .split(",")
            .mapNotNull { entry ->
                val clean = entry.trim()
                if (clean.isEmpty()) return@mapNotNull null

                val p = clean.split("=")
                if (p.size < 3) return@mapNotNull null

                try {
                    Speed(
                        p[0].trim().toDouble(),  // beat
                        p[1].trim().toDouble(),  // ratio
                        p[2].trim().toDouble(),  // duration
                        p.getOrNull(3)?.trim()?.toIntOrNull() ?: 0  // mode
                    )
                } catch (e: NumberFormatException) {
                    null
                }
            }
    }

    private fun parseScrolls(text: String): List<Scroll> {
        val raw = extractTag(text, "SCROLLS") ?: return emptyList()
        return raw.split(",").mapNotNull {
            val p = it.split("=")
            if (p.size == 2) Scroll(p[0].toDouble(), p[1].toDouble()) else null
        }
    }

    private fun parsePairs(text: String, tag: String): List<Pair<Double, Double>> {
        val raw = extractTag(text, tag) ?: return emptyList()
        return raw.split(",").mapNotNull {
            val p = it.split("=")
            if (p.size == 2) p[0].toDoubleOrNull()?.let { b ->
                p[1].toDoubleOrNull()?.let { v -> b to v }
            } else null
        }.sortedBy { it.first }
    }

    private fun extractTag(text: String, tag: String): String? {
        val regex = Regex("#$tag\\s*:(.*?);", RegexOption.DOT_MATCHES_ALL)
        return regex.find(text)?.groupValues?.get(1)?.trim()
    }

    private fun extractNotesBlock(text: String): String? {
        val regex = Regex("#NOTES\\s*:(.*?);", RegexOption.DOT_MATCHES_ALL)
        return regex.find(text)?.groupValues?.get(1)
    }

    private fun isFake(beat: Double, fakes: List<Fake>): Boolean {
        return fakes.any { beat >= it.beat && beat < it.beat + it.duration }
    }

    fun makeMirror(notes: List<Note>): List<Note> {
        val mirrorMap = intArrayOf(1, 0, 2, 4, 3)
        return remapColumns(notes, mirrorMap)
    }

    fun makeRandom(notes: List<Note>): List<Note> {
        val map = generateSafeRandomMap(5)
        return remapColumns(notes, map)
    }

    private fun remapColumns(notes: List<Note>, map: IntArray): List<Note> {

        fun transform(notes: List<Note>): List<Note> {
            return notes.map { note ->

                if (note.type == NoteType.MINE) {
                    note
                } else {
                    note.copy(
                        column = map.getOrElse(note.column) { note.column }
                    )
                }
            }
        }

        return transform(notes)

    }

    private fun generateSafeRandomMap(size: Int = 5): IntArray {

        val base = IntArray(size) { it }

        while (true) {

            val map = base.clone()
            map.shuffle()

            // centro no fijo (para 5K)
            if (size >= 3 && map[2] == 2) continue

            // evitar demasiados iguales
            var same = 0
            for (i in map.indices) {
                if (map[i] == i) same++
            }
            if (same > size / 2) continue

            // evitar colapso lateral (solo 5K)
            if (size == 5) {
                val leftSide = setOf(0, 1)
                val mapped0Left = map[0] in leftSide
                val mapped4Left = map[4] in leftSide

                if (mapped0Left && mapped4Left) continue
            }

            return map
        }
    }
}