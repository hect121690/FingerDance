package com.fingerdance

import kotlin.math.max
import kotlin.math.min

class Parser {

    // =========================================================
    // ENUMS
    // =========================================================

    enum class NoteType {
        TAP,
        HOLD,
        MINE,
        FAKE
    }

    // =========================================================
    // TIMING SEGMENTS
    // =========================================================

    data class BPMChange(val beat: Double, val bpm: Double)
    data class StopSegment(val beat: Double, val duration: Double)
    data class DelaySegment(val beat: Double, val duration: Double)
    data class ComboSegment(val beat: Double, val combo: Int)
    data class WarpSegment(val beat: Double, val length: Double)
    data class TimeSignatureSegment(val beat: Double, val num: Int, val den: Int)
    data class TickCountSegment(val beat: Double, val ticks: Int)
    data class SpeedSegment(val beat: Double, val ratio: Double, val duration: Double, val mode: Int)
    data class ScrollSegment(val beat: Double, val factor: Double)
    data class FakeSegment(val beat: Double, val length: Double)

    // =========================================================
    // NOTES
    // =========================================================

    data class Note(
        val column: Int,
        val beatStart: Double,
        val beatEnd: Double?,
        val timeStart: Double,
        val timeEnd: Double?,
        val type: NoteType
    )

    // =========================================================
    // CHART
    // =========================================================

    data class ChartData(
        val bpms: MutableList<BPMChange> = mutableListOf(),
        val stops: MutableList<StopSegment> = mutableListOf(),
        val delays: MutableList<DelaySegment> = mutableListOf(),
        val combos: MutableList<ComboSegment> = mutableListOf(),
        val warps: MutableList<WarpSegment> = mutableListOf(),
        val timeSignatures: MutableList<TimeSignatureSegment> = mutableListOf(),
        val tickCounts: MutableList<TickCountSegment> = mutableListOf(),
        val speeds: MutableList<SpeedSegment> = mutableListOf(),
        val scrolls: MutableList<ScrollSegment> = mutableListOf(),
        val fakes: MutableList<FakeSegment> = mutableListOf(),
        val notes: MutableList<Note> = mutableListOf()
    )

    // =========================================================
    // PUBLIC ENTRY
    // =========================================================

    fun parseSSC(data: String): ChartData {

        val lines = data.lines()
        val chart = ChartData()

        var readingNotes = false
        val noteBuffer = mutableListOf<String>()
        var songOffset = 0.0

        for (i in lines.indices) {

            val raw = lines[i]
            val line = raw.trim()

            if (line.startsWith("#OFFSET:"))
                songOffset = extractValue(line).toDouble()

            if (line.startsWith("#BPMS:"))
                parseBPMS(chart, collectBlock(lines, i))

            if (line.startsWith("#STOPS:"))
                parseStops(chart, collectBlock(lines, i))

            if (line.startsWith("#DELAYS:"))
                parseDelays(chart, collectBlock(lines, i))

            if (line.startsWith("#COMBOS:"))
                parseCombos(chart, collectBlock(lines, i))

            if (line.startsWith("#WARPS:"))
                parseWarps(chart, collectBlock(lines, i))

            if (line.startsWith("#TIMESIGNATURES:"))
                parseTimeSignatures(chart, collectBlock(lines, i))

            if (line.startsWith("#TICKCOUNTS:"))
                parseTickCounts(chart, collectBlock(lines, i))

            if (line.startsWith("#SPEEDS:"))
                parseSpeeds(chart, collectBlock(lines, i))

            if (line.startsWith("#SCROLLS:"))
                parseScrolls(chart, collectBlock(lines, i))

            if (line.startsWith("#FAKES:"))
                parseFakes(chart, collectBlock(lines, i))

            if (line.startsWith("#NOTES:")) {
                readingNotes = true
                noteBuffer.clear()
                continue
            }

            if (readingNotes) {
                if (line == ";") {
                    readingNotes = false
                    parseNotes(chart, noteBuffer, songOffset)
                } else {
                    noteBuffer.add(raw)
                }
            }
        }

        // Si el chart no cerró con ';'
        if (readingNotes && noteBuffer.isNotEmpty()) {
            parseNotes(chart, noteBuffer, songOffset)
        }

        if (chart.bpms.isEmpty()) {
            chart.bpms.add(BPMChange(0.0, 120.0))
        }

        chart.bpms.sortBy { it.beat }
        chart.warps.sortBy { it.beat }

        return chart
    }

    // =========================================================
    // PARSE NOTES
    // =========================================================

    private fun parseNotes(chart: ChartData, lines: List<String>, offset: Double) {

        var currentBeat = 0.0
        val holdBuffer = mutableMapOf<Int, Double>()
        val measure = mutableListOf<String>()

        for (raw in lines) {

            val line = raw.trim()

            if (line.startsWith(",")) {
                processMeasure(chart, measure, holdBuffer, offset, currentBeat)
                currentBeat += 4.0
                measure.clear()
                continue
            }

            if (line.startsWith("//") || line.isBlank()) continue
            measure.add(line)
        }

        if (measure.isNotEmpty())
            processMeasure(chart, measure, holdBuffer, offset, currentBeat)
    }

    private fun processMeasure(
        chart: ChartData,
        rows: List<String>,
        holdBuffer: MutableMap<Int, Double>,
        offset: Double,
        measureStartBeat: Double
    ) {

        val linesInMeasure = rows.size
        val beatPerRow = 4.0 / max(linesInMeasure, 1)

        for ((index, row) in rows.withIndex()) {

            val beat = measureStartBeat + (index * beatPerRow)

            val tokens = tokenizeRow(row)

            for (col in tokens.indices) {

                val token = tokens[col]

                when {
                    token == "1" -> addTap(chart, col, beat, offset)

                    token == "2" -> holdBuffer[col] = beat

                    token == "3" -> {
                        val startBeat = holdBuffer[col] ?: continue
                        addHold(chart, col, startBeat, beat, offset)
                        holdBuffer.remove(col)
                    }

                    token == "M" -> addMine(chart, col, beat, offset)

                    // token == "0" o cualquier otro caracter (incluyendo {xxx} convertidos a 0)
                    // simplemente se ignora, es una nota vacía
                }
            }
        }
    }

    // =========================================================
    // TOKENIZER PARA {xxx}
    // =========================================================

    private fun tokenizeRow(row: String): List<String> {

        val result = mutableListOf<String>()
        var i = 0

        while (i < row.length) {

            if (row[i] == '{') {

                val end = row.indexOf('}', i)
                if (end != -1) {
                    // Tratar {xxx} como 0 (nota vacía)
                    result.add("0")
                    i = end + 1
                } else {
                    result.add(row[i].toString())
                    i++
                }

            } else {
                result.add(row[i].toString())
                i++
            }
        }

        return result
    }

    // =========================================================
    // NOTE ADDERS
    // =========================================================

    private fun addTap(chart: ChartData, col: Int, beat: Double, offset: Double) {
        if (isFake(chart, beat)) return
        val time = beatToSeconds(chart, beat) - offset
        chart.notes.add(Note(col, beat, null, time, null, NoteType.TAP))
    }

    private fun addHold(chart: ChartData, col: Int, start: Double, end: Double, offset: Double) {
        if (isFake(chart, start)) return
        val timeStart = beatToSeconds(chart, start) - offset
        val timeEnd = beatToSeconds(chart, end) - offset
        chart.notes.add(Note(col, start, end, timeStart, timeEnd, NoteType.HOLD))
    }

    private fun addMine(chart: ChartData, col: Int, beat: Double, offset: Double) {
        val time = beatToSeconds(chart, beat) - offset
        chart.notes.add(Note(col, beat, null, time, null, NoteType.MINE))
    }

    // =========================================================
    // TIMING ENGINE
    // =========================================================

    private fun beatToSeconds(chart: ChartData, targetBeat: Double): Double {

        var time = 0.0
        var lastBeat = 0.0
        var currentBpm = chart.bpms.first().bpm

        for (bpmChange in chart.bpms) {

            if (bpmChange.beat >= targetBeat) break

            val deltaBeat = subtractWarpedBeats(chart, lastBeat, bpmChange.beat)
            time += (60.0 / currentBpm) * deltaBeat

            currentBpm = bpmChange.bpm
            lastBeat = bpmChange.beat
        }

        val remaining = subtractWarpedBeats(chart, lastBeat, targetBeat)
        time += (60.0 / currentBpm) * remaining

        chart.stops.forEach {
            if (it.beat < targetBeat)
                time += it.duration
        }

        return time
    }

    private fun subtractWarpedBeats(chart: ChartData, start: Double, end: Double): Double {

        var effective = end - start

        for (warp in chart.warps) {

            val warpStart = warp.beat
            val warpEnd = warp.beat + warp.length

            val overlapStart = max(start, warpStart)
            val overlapEnd = min(end, warpEnd)

            if (overlapEnd > overlapStart)
                effective -= (overlapEnd - overlapStart)
        }

        return effective
    }

    private fun isFake(chart: ChartData, beat: Double): Boolean {
        return chart.fakes.any {
            beat >= it.beat && beat < it.beat + it.length
        }
    }

    // =========================================================
    // SEGMENT PARSERS
    // =========================================================

    private fun parseBPMS(chart: ChartData, block: String) {
        parsePairBlock(block, "#BPMS:") {
            chart.bpms.add(BPMChange(it.first, it.second))
        }
    }

    private fun parseStops(chart: ChartData, block: String) {
        parsePairBlock(block, "#STOPS:") {
            chart.stops.add(StopSegment(it.first, it.second))
        }
    }

    private fun parseDelays(chart: ChartData, block: String) {
        parsePairBlock(block, "#DELAYS:") {
            chart.delays.add(DelaySegment(it.first, it.second))
        }
    }

    private fun parseCombos(chart: ChartData, block: String) {
        val cleaned = cleanBlock(block, "#COMBOS:")
        cleaned.split(",").forEach {
            val p = it.split("=")
            if (p.size == 2)
                chart.combos.add(ComboSegment(p[0].toDouble(), p[1].toInt()))
        }
    }

    private fun parseWarps(chart: ChartData, block: String) {
        parsePairBlock(block, "#WARPS:") {
            chart.warps.add(WarpSegment(it.first, it.second))
        }
    }

    private fun parseScrolls(chart: ChartData, block: String) {
        parsePairBlock(block, "#SCROLLS:") {
            chart.scrolls.add(ScrollSegment(it.first, it.second))
        }
    }

    private fun parseFakes(chart: ChartData, block: String) {
        parsePairBlock(block, "#FAKES:") {
            chart.fakes.add(FakeSegment(it.first, it.second))
        }
    }

    private fun parseTickCounts(chart: ChartData, block: String) {
        val cleaned = cleanBlock(block, "#TICKCOUNTS:")
        cleaned.split(",").forEach {
            val p = it.split("=")
            if (p.size == 2)
                chart.tickCounts.add(TickCountSegment(p[0].toDouble(), p[1].toInt()))
        }
    }

    private fun parseTimeSignatures(chart: ChartData, block: String) {
        val cleaned = cleanBlock(block, "#TIMESIGNATURES:")
        cleaned.split(",").forEach {
            val p = it.split("=")
            if (p.size == 3)
                chart.timeSignatures.add(
                    TimeSignatureSegment(
                        p[0].toDouble(),
                        p[1].toInt(),
                        p[2].toInt()
                    )
                )
        }
    }

    private fun parseSpeeds(chart: ChartData, block: String) {
        val cleaned = cleanBlock(block, "#SPEEDS:")
        cleaned.split(",").forEach {
            val p = it.split("=")
            if (p.size == 4)
                chart.speeds.add(
                    SpeedSegment(
                        p[0].toDouble(),
                        p[1].toDouble(),
                        p[2].toDouble(),
                        p[3].toInt()
                    )
                )
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private fun parsePairBlock(block: String, header: String, action: (Pair<Double, Double>) -> Unit) {
        val cleaned = cleanBlock(block, header)
        if (cleaned.isBlank()) return

        cleaned.split(",").forEach {
            val p = it.split("=")
            if (p.size == 2)
                action(Pair(p[0].toDouble(), p[1].toDouble()))
        }
    }

    private fun cleanBlock(block: String, header: String): String {
        return block
            .replace(header, "")
            .replace(";", "")
            .replace("\n", "")
            .trim()
    }

    private fun extractValue(line: String): String {
        return line.substringAfter(":").replace(";", "").trim()
    }

    private fun collectBlock(lines: List<String>, startIndex: Int): String {
        val sb = StringBuilder()
        var i = startIndex
        while (i < lines.size) {
            sb.append(lines[i])
            if (lines[i].contains(";")) break
            i++
        }
        return sb.toString()
    }
}
