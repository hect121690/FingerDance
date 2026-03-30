package com.fingerdance.ssc

import kotlin.math.max
import kotlin.math.min

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
        val type: NoteType
    )

    data class BpmSegment(val beat: Double, val bpm: Double)
    data class Stop(val beat: Double, val durationMs: Double)
    data class Warp(val beat: Double, val duration: Double)
    data class Fake(val beat: Double, val duration: Double)

    data class Speed(val beat: Double, val ratio: Double, val duration: Double, val mode: Int)
    data class Scroll(val beat: Double, val ratio: Double)

    data class Chart(
        val bpms: List<BpmSegment>,
        val stops: List<Stop>,
        val warps: List<Warp>,
        val fakes: List<Fake>,
        val speeds: List<Speed>,
        val scrolls: List<Scroll>,
        val notes: List<Note>
    )

    // =========================
    // PUBLIC
    // =========================

    fun parseSSC(text: String): Chart {

        val bpms = parsePairs(text, "BPMS").map { BpmSegment(it.first, it.second) }
        val stops = parsePairs(text, "STOPS").map { Stop(it.first, it.second * 1000) }
        val delays = parsePairs(text, "DELAYS").map { Stop(it.first, it.second * 1000) }
        val warps = parsePairs(text, "WARPS").map { Warp(it.first, it.second) }
        val fakes = parsePairs(text, "FAKES").map { Fake(it.first, it.second) }

        val speeds = parseSpeeds(text)
        val scrolls = parseScrolls(text)

        val allStops = (stops + delays).sortedBy { it.beat }

        val notes = parseNotes(text, fakes)

        return Chart(
            bpms = bpms,
            stops = allStops,
            warps = warps,
            fakes = fakes,
            speeds = speeds,
            scrolls = scrolls,
            notes = notes
        )
    }

    // =========================
    // NOTES (con soporte correcto de FAKE)
    // =========================

    private fun parseNotes(
        text: String,
        fakes: List<Fake>
    ): List<Note> {

        val notes = mutableListOf<Note>()
        val holds = mutableMapOf<Int, Double>() // column -> startBeat

        val block = extractNotesBlock(text) ?: return notes
        val measures = block.split(",")

        var currentBeat = 0.0

        for (measure in measures) {

            val rows = measure.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("//") }

            val step = 4.0 / max(rows.size, 1)

            for ((i, rowRaw) in rows.withIndex()) {

                val row = tokenize(rowRaw)
                val beat = currentBeat + i * step

                for (col in 0 until row.size) {

                    when (row[col]) {

                        // TAP
                        '1' -> {
                            val fake = isFake(beat, fakes)
                            notes.add(
                                Note(
                                    column = col,
                                    beat = beat,
                                    isFake = fake,
                                    type = NoteType.TAP
                                )
                            )
                        }

                        // HOLD START
                        '2' -> {
                            // guardamos sólo el beat de inicio; el flag fake se decide con el rango completo
                            holds[col] = beat
                        }

                        // HOLD END
                        '3' -> {
                            val startBeat = holds[col] ?: continue
                            // fake si cualquier parte del rango está en FAKE
                            val isStartFake = isFake(startBeat, fakes)
                            val isEndFake = isFake(beat, fakes)
                            val fake = isStartFake || isEndFake

                            notes.add(
                                Note(
                                    column = col,
                                    beat = startBeat,
                                    endBeat = beat,
                                    isFake = fake,
                                    type = NoteType.HOLD
                                )
                            )

                            holds.remove(col)
                        }

                        // MINE
                        'M', 'm' -> {
                            val fake = isFake(beat, fakes)
                            notes.add(
                                Note(
                                    column = col,
                                    beat = beat,
                                    isFake = fake,
                                    type = NoteType.MINE
                                )
                            )
                        }
                    }
                }
            }

            // IMPORTANTE: NO limpiamos holds aquí para no romper holds que cruzan medidas
            currentBeat += 4.0
        }

        return notes.sortedBy { it.beat }
    }

    // =========================
    // TOKENIZER
    // =========================

    private fun tokenize(row: String): List<Char> {
        val result = mutableListOf<Char>()
        var i = 0

        while (i < row.length) {
            if (row[i] == '{') {
                val end = row.indexOf('}', i)
                if (end != -1) {
                    i = end + 1
                    continue
                }
            }
            result.add(row[i])
            i++
        }

        return result
    }

    // =========================
    // HELPERS
    // =========================

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

    // Un beat está en FAKE si cae dentro de alguno de los rangos beat..beat+duration
    private fun isFake(beat: Double, fakes: List<Fake>): Boolean {
        return fakes.any { beat >= it.beat && beat < it.beat + it.duration }
    }
}