package com.fingerdance.ssc

import kotlin.collections.sortedBy
import kotlin.math.max

class Parser {

    // =========================
    // DATA
    // =========================

    enum class NoteType { TAP, HOLD }

    data class Note(
        val column: Int,
        val beat: Double,
        val endBeat: Double? = null,
        val isFake: Boolean = false,
        val isVanish: Boolean = false,
        val isPhantom: Boolean = false,
        val isMine: Boolean = false,
        val isPressed: Boolean = false,
        val type: NoteType
    )

    data class FGChange(
        val beat: Double,
        val script: String,
        val duration: Float = 0f,
        var executed: Boolean = false
    )

    data class BpmSegment(val beat: Double, val bpm: Double)
    data class TickCountSegment(val beat: Double, val tickcount: Int)
    data class Stop(val beat: Double, val durationMs: Double)
    data class Delay(val beat: Double, val durationMs: Double)
    data class Warp(val beat: Double, val duration: Double)

    data class Fake(
        val beat: Double,
        val duration: Double,
        val isPressed: Boolean = false
    )

    data class Speed(val beat: Double, val ratio: Double, val duration: Double, val mode: Int)
    data class Scroll(val beat: Double, val ratio: Double)
    data class Combo(val beat: Double, val number: Int)

    data class ExtendedToken(
        val code: Char,
        val isVanish: Boolean,
        val isFake: Boolean,
        val isPressed: Boolean
    )

    data class HoldData(
        val beat: Double,
        val isFake: Boolean,
        val isPressed: Boolean
    )

    data class Chart(
        val chartPath: String = "",
        val offset: Double = 0.0,
        val bpms: List<BpmSegment>,
        val tickcounts: List<TickCountSegment>,
        val stops: List<Stop>,
        val delays: List<Delay>,
        val warps: List<Warp>,
        val fakes: List<Fake>,
        val speeds: List<Speed>,
        val scrolls: List<Scroll>,
        val combos: List<Combo>,
        var notes: List<Note>,
        val fgChanges: MutableList<FGChange> = mutableListOf()
    )

    // =========================
    // PUBLIC
    // =========================

    fun parseSSC(textHeader: String, textChart: String, pathFile: String): Chart {

        val offset = extractTag(textChart, "OFFSET")?.toDoubleOrNull() ?: 0.0
        val fgChanges = parseFGChanges(textHeader)

        val bpms = parsePairs(textChart, "BPMS").map { BpmSegment(it.first, it.second) }
        val tickcounts = parsePairs(textChart, "TICKCOUNTS", true).map { TickCountSegment(it.first, it.second.toInt()) }
        val stops = parsePairs(textChart, "STOPS").map { Stop(it.first, it.second * 1000) }
        val delays = parsePairs(textChart, "DELAYS").map { Delay(it.first, it.second * 1000) }
        val warps = parsePairs(textChart, "WARPS").map { Warp(it.first, it.second) }
        val combos = parsePairs(textChart, "COMBOS").map { Combo(it.first, it.second.toInt()) }
        val baseFakes = parsePairs(textChart, "FAKES").map {
            Fake(it.first, it.second)
        }

        val speeds = parseSpeeds(textChart)
        val scrolls = parseScrolls(textChart)

        val (notes, extendedNotes, tokenFakes) =
            parseNotes(textChart, baseFakes)

        val fakes =
            (baseFakes + tokenFakes)
                .sortedBy { it.beat }

        val allNotes =
            (notes + extendedNotes)
                .sortedBy { it.beat }

        return Chart(
            chartPath = pathFile,
            offset = offset,
            bpms = bpms,
            tickcounts = tickcounts,
            stops = stops,
            delays = delays,
            warps = warps,
            fakes = fakes,
            speeds = speeds, //.sortedBy { it.beat },
            scrolls = scrolls, //.sortedBy { it.beat },
            combos = combos,
            notes = allNotes,
            fgChanges = fgChanges
        )
    }

    // =========================
    // NOTES
    // =========================

    private fun parseNotes(
        text: String,
        fakes: List<Fake>
    ): Triple<List<Note>, List<Note>, List<Fake>> {

        val notes = mutableListOf<Note>()
        val extendedNotes = mutableListOf<Note>()
        val tokenFakes = mutableListOf<Fake>()

        val holds = mutableMapOf<Int, Double>()
        val phantomHolds = mutableMapOf<Int, Double>()

        val extHolds = mutableMapOf<Int, HoldData>()

        val block = extractNotesBlock(text) ?: return Triple(notes, extendedNotes, tokenFakes)

        val measures = block.split(",")

        var currentBeat = 0.0

        for (measure in measures) {

            val rows = measure.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("//") }

            val step = 4.0 / max(rows.size, 1)

            for ((i, rowRaw) in rows.withIndex()) {

                val normalizedRow = normalizeRowForHD(rowRaw)

                val (rowMap, extTokens) = tokenize(normalizedRow)

                val beat = currentBeat + i * step

                val parsedCols = mutableSetOf<Int>()

                // =========================
                // TOKENS EXTENDIDOS
                // =========================

                for ((col, data) in extTokens) {

                    val code = data.code
                    val isVanish = data.isVanish
                    val isFake = data.isFake
                    val isPressed = data.isPressed

                    when (code) {

                        '1' -> {

                            extendedNotes.add(
                                Note(
                                    column = col,
                                    beat = beat,
                                    isFake = isFake,
                                    isVanish = isVanish,
                                    isPhantom = false,
                                    isPressed = isPressed,
                                    type = NoteType.TAP
                                )
                            )

                            if (isFake) {
                                tokenFakes.add(
                                    Fake(
                                        beat = beat,
                                        duration = 0.0,
                                        isPressed = isPressed
                                    )
                                )
                            }
                        }

                        '2' -> {

                            extHolds[col] =
                                HoldData(
                                    beat = beat,
                                    isFake = isFake,
                                    isPressed = isPressed
                                )
                        }

                        '3' -> {

                            val holdData = extHolds[col] ?: continue

                            val fake = holdData.isFake || isFake

                            extendedNotes.add(
                                Note(
                                    column = col,
                                    beat = holdData.beat,
                                    endBeat = beat,
                                    isFake = fake,
                                    isVanish = isVanish,
                                    isPhantom = false,
                                    isPressed = holdData.isPressed,
                                    type = NoteType.HOLD
                                )
                            )

                            if (fake) {
                                tokenFakes.add(
                                    Fake(
                                        beat = holdData.beat,
                                        duration = beat - holdData.beat,
                                        isPressed = holdData.isPressed
                                    )
                                )
                            }

                            extHolds.remove(col)
                        }

                        'M', 'm' -> {

                            extendedNotes.add(
                                Note(
                                    column = col,
                                    beat = beat,
                                    isFake = isFake,
                                    isVanish = isVanish,
                                    isPhantom = false,
                                    isMine = true,
                                    type = NoteType.TAP
                                )
                            )
                        }
                    }

                    parsedCols.add(col)
                }

                // =========================
                // TOKENS NORMALES
                // =========================

                for ((col, value) in rowMap) {

                    if (parsedCols.contains(col)) continue

                    when (value) {

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

                            } else if (phantomHolds.contains(col)) {

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
                                    isMine = true,
                                    type = NoteType.TAP
                                )
                            )
                        }

                        'X' -> {
                            // ignore
                        }
                    }
                }
            }

            currentBeat += 4.0
        }

        return Triple(notes, extendedNotes, tokenFakes)
    }

    private fun normalizeRowForHD(row: String): String {

        val (parsed, ext) = tokenize(row)

        val totalCols = parsed.size + ext.size

        return if (totalCols == 6) {
            "00$row" + "00"
        } else {
            row
        }
    }

    // =========================
    // TOKENIZER
    // =========================

    private fun tokenize(
        row: String
    ): Pair<Map<Int, Char>, Map<Int, ExtendedToken>> {

        val result = mutableMapOf<Int, Char>()

        val extTokens = mutableMapOf<Int, ExtendedToken>()

        var i = 0
        var colIndex = 0

        while (i < row.length) {

            val ch = row[i]

            if (ch == '{') {

                val end = row.indexOf('}', i)

                if (end != -1) {

                    val inside = row.substring(i + 1, end)

                    val parts = inside.split("|")

                    if (parts.size > 1) {

                        val codeChar = parts[0].firstOrNull() ?: ' '

                        val modifier = parts.getOrNull(1)

                        if (codeChar == 'h') {
                            colIndex++
                            i = end + 1
                            continue
                        }

                        val isVanish = modifier == "v"

                        val fakeFlag = parts.getOrNull(2) == "1"

                        val isFake =
                            fakeFlag &&
                                    (codeChar == '1'
                                            || codeChar == '2'
                                            || codeChar == '3')

                        val isPressed =
                            modifier == "n" &&
                                    parts.getOrNull(3) == "1"

                        extTokens[colIndex] =
                            ExtendedToken(
                                code = codeChar,
                                isVanish = isVanish,
                                isFake = isFake,
                                isPressed = isPressed
                            )

                        colIndex++
                    }

                    i = end + 1
                    continue
                }
            }

            result[colIndex] = ch

            colIndex++
            i++
        }

        return result to extTokens
    }

    // =========================
    // HELPERS
    // =========================

    private fun parseFGChanges(text: String): MutableList<FGChange> {

        val raw = extractTag(text, "FGCHANGES")
            ?: return mutableListOf()

        val result = mutableListOf<FGChange>()

        raw.split(",").forEach { line ->

            val clean = line.trim()

            if (clean.isEmpty()) return@forEach

            val parts = clean.split("=")

            if (parts.size < 2) return@forEach

            try {

                val beat = parts[0].trim().toDouble()
                val script = parts[1].trim()

                val duration =
                    parts.getOrNull(2)
                        ?.trim()
                        ?.toFloatOrNull()
                        ?: 0f

                result.add(
                    FGChange(
                        beat = beat,
                        script = script,
                        duration = duration
                    )
                )

            } catch (_: Exception) {
            }
        }

        return result
    }

    private fun parseSpeeds(text: String): List<Speed> {

        val raw = extractTag(text, "SPEEDS") ?: return emptyList()

        return raw
            .replace(";", "")
            .split(",")
            .mapNotNull { entry ->

                val clean = entry.trim()

                if (clean.isEmpty()) return@mapNotNull null

                val p = clean.split("=")

                if (p.size < 3) return@mapNotNull null

                try {

                    Speed(
                        p[0].trim().toDouble(),
                        p[1].trim().toDouble(),
                        p[2].trim().toDouble(),
                        p.getOrNull(3)?.trim()?.toIntOrNull() ?: 0
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

            if (p.size == 2)
                Scroll(p[0].toDouble(), p[1].toDouble())
            else
                null
        }
    }

    private fun parsePairs(
        text: String,
        tag: String,
        isTickcount: Boolean = false
    ): List<Pair<Double, Double>> {

        val raw = extractTag(text, tag) ?: return emptyList()

        return raw.split(",").mapNotNull {

            val p = it.split("=")

            if (p.size == 2) {

                p[0].toDoubleOrNull()?.let { b ->

                    p[1].toDoubleOrNull()?.let { originalValue ->

                        val value =
                            if (isTickcount && originalValue == 0.0)
                                1.0
                            else
                                originalValue

                        b to value
                    }
                }

            } else null

        }.sortedBy { it.first }
    }

    private fun extractTag(text: String, tag: String): String? {

        val regex =
            Regex("#$tag\\s*:(.*?);", RegexOption.DOT_MATCHES_ALL)

        return regex.find(text)
            ?.groupValues
            ?.get(1)
            ?.trim()
    }

    private fun extractNotesBlock(text: String): String? {

        val regex =
            Regex("#NOTES\\s*:(.*?);", RegexOption.DOT_MATCHES_ALL)

        return regex.find(text)
            ?.groupValues
            ?.get(1)
    }

    private fun isFake(
        beat: Double,
        fakes: List<Fake>
    ): Boolean {

        return fakes.any {
            beat >= it.beat &&
                    beat < it.beat + it.duration
        }
    }

    fun makeMirror(notes: List<Note>): List<Note> {
        val mirrorMap = intArrayOf(1, 0, 2, 4, 3)
        return remapColumns(notes, mirrorMap)
    }

    fun makeMirrorHD(notes: List<Note>): List<Note> {
        val mirrorMap = intArrayOf(0, 1, 7, 6, 5, 4, 3, 2, 8, 9)
        return remapColumns(notes, mirrorMap)
    }

    fun makeRandom(notes: List<Note>): List<Note> {
        val map = generatePumpRandomMap()
        return remapColumns(notes, map)
    }

    fun makeRandomHD(notes: List<Note>): List<Note> {
        val map = generatePumpRandomMapHD()
        return remapColumns(notes, map)
    }

    private fun remapColumns(notes: List<Note>, map: IntArray): List<Note> {
        return notes.map { note ->
            if (note.isMine && note.isFake) {
                note
            } else {
                note.copy(column = map.getOrElse(note.column) { note.column })
            }
        }
    }

    // =====================================================
    // SINGLE RANDOM
    // =====================================================

    private fun generatePumpRandomMap(): IntArray {

        return singleRandomMaps.random()
    }

    private val singleRandomMaps = listOf(

        // mirror
        intArrayOf(4,3,2,1,0),

        // swap esquinas
        intArrayOf(1,0,2,4,3),

        // cruzado suave
        intArrayOf(3,4,2,0,1),

        // diagonales
        intArrayOf(1,3,2,0,4),

        // shuffle arcade
        intArrayOf(4,0,2,3,1),

        // crossover ligero
        intArrayOf(3,1,2,4,0),

        // variante rara
        intArrayOf(0,4,2,1,3)
    )

    // =====================================================
    // HALF DOUBLE RANDOM
    // =====================================================

    private fun generatePumpRandomMapHD(): IntArray {

        return hdRandomMaps.random()
    }

    private val hdRandomMaps = listOf(

        // mirror interno
        intArrayOf(0,1,7,6,5, 4,3,2,8,9),

        // swap centros
        intArrayOf(0,1,3,2,5, 4,7,6,8,9),

        // crossover suave
        intArrayOf(0,1,6,3,4, 5,2,7,8,9),

        // expandido
        intArrayOf(0,1,5,4,3, 2,6,7,8,9),

        // diagonales
        intArrayOf(0,1,7,4,5, 2,3,6,8,9),

        // raro arcade
        intArrayOf(0,1,4,5,2, 7,3,6,8,9),

        // shuffle fuerte
        intArrayOf(0,1,6,5,4, 3,2,7,8,9)
    )
}