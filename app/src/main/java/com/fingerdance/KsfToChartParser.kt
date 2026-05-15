package com.fingerdance

import com.fingerdance.ssc.Parser
import kotlin.experimental.or
import java.io.File

class KsfToChartParser(
    private val columns: Int = 5
) {

    companion object {

        const val STEPINFO_STEP = 0
        const val STEPINFO_BPM = 1
        const val STEPINFO_TICK = 2
        const val STEPINFO_DELAY = 3

        const val NOTE_NONE: Byte = 0
        const val NOTE_NOTE: Byte = 1
        const val NOTE_LNOTE: Byte = 2

        const val NOTE_START_CHK: Byte = 4
        const val NOTE_END_CHK: Byte = 8
    }

    enum class TypeNote {
        NORMAL,
        FAKE,
        PHANTOM,
        MINE
    }

    data class Note(
        var step: Byte = NOTE_NONE,
        var type: TypeNote = TypeNote.NORMAL
    )

    data class Line(
        val note: Array<Note>
    )

    data class Pattern(

        var fBPM: Float = 120f,
        var iTick: Int = 4,
        var timeDelay: Long = 0,

        val vLine: MutableList<Line> =
            mutableListOf(),

        var beatStart: Double = 0.0,
        var beatLen: Double = 0.0
    )

    data class StepInfo(
        val value: String,
        val type: Int
    )

    data class HoldInfo(

        var active: Boolean = false,
        var startBeat: Double = 0.0,
        var type: TypeNote = TypeNote.NORMAL
    )

    private val patterns =
        mutableListOf<Pattern>()

    // =========================
    // REAL KSF COLUMN MAPS
    // =========================

    private val colMap =
        if (columns == 5) {

            intArrayOf(
                4, 5, 6, 7, 8
            )

        } else {

            intArrayOf(
                0, 1, 2, 3, 4,
                8, 9, 10, 11, 12
            )
        }

    // =========================
    // PARSE
    // =========================

    fun parse(path: String): Parser.Chart {

        val file = File(path)

        val stepInfo =
            mutableListOf<StepInfo>()

        var tickCount = 4
        var bpm = 120f

        // =========================
        // READ FILE
        // =========================

        file.forEachLine { raw ->

            val line = raw.trim()

            when {

                line.startsWith("#BPM:", true) -> {

                    bpm =
                        getValue(line)
                            .toFloatOrNull()
                            ?: 120f
                }

                line.startsWith("#TICKCOUNT:", true) -> {

                    tickCount =
                        getValue(line)
                            .toIntOrNull()
                            ?: 4
                }

                line.startsWith("|B") &&
                        line.endsWith("|") -> {

                    stepInfo.add(
                        StepInfo(
                            line.substring(
                                2,
                                line.length - 1
                            ),
                            STEPINFO_BPM
                        )
                    )
                }

                line.startsWith("|T") &&
                        line.endsWith("|") -> {

                    stepInfo.add(
                        StepInfo(
                            line.substring(
                                2,
                                line.length - 1
                            ),
                            STEPINFO_TICK
                        )
                    )
                }

                line.startsWith("|D") &&
                        line.endsWith("|") -> {

                    stepInfo.add(
                        StepInfo(
                            line.substring(
                                2,
                                line.length - 1
                            ),
                            STEPINFO_DELAY
                        )
                    )
                }

                // =========================
                // REAL KSF STEP ROW
                // =========================

                line.length == 13 -> {

                    stepInfo.add(
                        StepInfo(
                            line,
                            STEPINFO_STEP
                        )
                    )
                }
            }
        }

        // =========================
        // BUILD PATTERNS
        // =========================

        var curPattern =
            Pattern(
                fBPM = bpm,
                iTick = tickCount
            )

        patterns.add(curPattern)

        stepInfo.forEach { info ->

            when (info.type) {

                STEPINFO_BPM -> {

                    val newBpm =
                        info.value
                            .toFloatOrNull()
                            ?: curPattern.fBPM

                    if (curPattern.vLine.isNotEmpty()) {

                        curPattern =
                            Pattern(
                                fBPM = newBpm,
                                iTick = curPattern.iTick
                            )

                        patterns.add(curPattern)

                    } else {

                        curPattern.fBPM =
                            newBpm
                    }
                }

                STEPINFO_TICK -> {

                    val newTick =
                        info.value
                            .toIntOrNull()
                            ?: curPattern.iTick

                    if (curPattern.vLine.isNotEmpty()) {

                        curPattern =
                            Pattern(
                                fBPM = curPattern.fBPM,
                                iTick = newTick
                            )

                        patterns.add(curPattern)

                    } else {

                        curPattern.iTick =
                            newTick
                    }
                }

                STEPINFO_DELAY -> {

                    val delay =
                        info.value
                            .toLongOrNull()
                            ?: 0L

                    val delayPattern =
                        Pattern(
                            fBPM = curPattern.fBPM,
                            iTick = curPattern.iTick,
                            timeDelay = delay
                        )

                    patterns.add(delayPattern)

                    curPattern =
                        Pattern(
                            fBPM = curPattern.fBPM,
                            iTick = curPattern.iTick
                        )

                    patterns.add(curPattern)
                }

                // =========================
                // STEP ROW
                // =========================

                STEPINFO_STEP -> {

                    val line =
                        Line(
                            Array(columns) {
                                Note()
                            }
                        )

                    for (i in 0 until columns) {

                        val realIndex =
                            colMap[i]

                        val char =
                            info.value[realIndex]

                        when (char) {

                            '1' -> {

                                line.note[i].step =
                                    NOTE_NOTE
                            }

                            'M',
                            'm' -> {

                                line.note[i].step =
                                    NOTE_NOTE

                                line.note[i].type =
                                    TypeNote.MINE
                            }

                            'F',
                            'f' -> {

                                line.note[i].step =
                                    NOTE_NOTE

                                line.note[i].type =
                                    TypeNote.FAKE
                            }

                            'P',
                            'p' -> {

                                line.note[i].step =
                                    NOTE_NOTE

                                line.note[i].type =
                                    TypeNote.PHANTOM
                            }

                            // =========================
                            // HOLD BODY
                            // =========================

                            '4' -> {

                                line.note[i].step =
                                    (NOTE_LNOTE or NOTE_START_CHK)
                            }

                            else -> {

                                line.note[i].step =
                                    NOTE_NONE
                            }
                        }
                    }

                    curPattern.vLine.add(line)
                }
            }
        }

        // =========================
        // COMPUTE BEATS
        // =========================

        computePatternBeats()

        // =========================
        // BUILD NOTES
        // =========================

        val notes =
            mutableListOf<Parser.Note>()

        val holds =
            Array(columns) {
                HoldInfo()
            }

        patterns.forEach { ptn ->

            val beatStep =
                1.0 / ptn.iTick

            ptn.vLine.forEachIndexed { rowIndex, line ->

                val beat =
                    ptn.beatStart +
                            (rowIndex * beatStep)

                for (col in 0 until columns) {

                    val note =
                        line.note[col]

                    // =========================
                    // TAPS
                    // =========================

                    if (note.step == NOTE_NOTE) {

                        notes.add(
                            Parser.Note(
                                column = col,
                                beat = beat,

                                type =
                                    when (note.type) {

                                        TypeNote.MINE ->
                                            Parser.NoteType.MINE

                                        else ->
                                            Parser.NoteType.TAP
                                    },

                                isFake =
                                    note.type ==
                                            TypeNote.FAKE,

                                isPhantom =
                                    note.type ==
                                            TypeNote.PHANTOM
                            )
                        )
                    }

                    // =========================
                    // HOLDS
                    // =========================

                    else if (
                        note.step.toInt() and
                        NOTE_LNOTE.toInt()
                        != 0
                    ) {

                        // START

                        if (!holds[col].active) {

                            holds[col].active = true

                            holds[col].startBeat =
                                beat

                            holds[col].type =
                                note.type
                        }
                    }

                    // =========================
                    // HOLD END
                    // =========================

                    else {

                        if (holds[col].active) {

                            notes.add(
                                Parser.Note(
                                    column = col,

                                    beat =
                                        holds[col].startBeat,

                                    endBeat = beat,

                                    type =
                                        Parser.NoteType.HOLD,

                                    isFake =
                                        holds[col].type ==
                                                TypeNote.FAKE,

                                    isPhantom =
                                        holds[col].type ==
                                                TypeNote.PHANTOM
                                )
                            )

                            holds[col].active =
                                false
                        }
                    }
                }
            }
        }

        // =========================
        // BPM SEGMENTS
        // =========================

        val bpms =
            mutableListOf<Parser.BpmSegment>()

        val tickCounts =
            mutableListOf<Parser.TickCountSegment>()

        val delays =
            mutableListOf<Parser.Delay>()

        patterns.forEach { ptn ->

            bpms.add(
                Parser.BpmSegment(
                    beat = ptn.beatStart,
                    bpm = ptn.fBPM.toDouble()
                )
            )

            tickCounts.add(
                Parser.TickCountSegment(
                    beat = ptn.beatStart,
                    tickcount = ptn.iTick
                )
            )

            if (ptn.timeDelay > 0) {

                delays.add(
                    Parser.Delay(
                        beat = ptn.beatStart,
                        durationMs =
                            ptn.timeDelay.toDouble()
                    )
                )
            }
        }

        return Parser.Chart(

            offset = 0.0,

            bpms =
                bpms.distinctBy {
                    it.beat
                },

            tickcounts =
                tickCounts.distinctBy {
                    it.beat
                },

            stops = emptyList(),

            delays = delays,

            warps = emptyList(),

            fakes = emptyList(),

            speeds = emptyList(),

            scrolls = emptyList(),

            notes =
                notes.sortedBy {
                    it.beat
                },

            luaEvents = emptyList()
        )
    }

    // =========================
    // COMPUTE PATTERN BEATS
    // =========================

    private fun computePatternBeats() {

        var beatAccum = 0.0

        patterns.forEach { ptn ->

            ptn.beatStart =
                beatAccum

            val rows =
                if (ptn.vLine.isEmpty()) {
                    1
                } else {
                    ptn.vLine.size
                }

            ptn.beatLen =
                rows.toDouble() /
                        ptn.iTick

            beatAccum +=
                ptn.beatLen
        }
    }

    // =========================
    // HELPERS
    // =========================

    private fun getValue(
        line: String
    ): String {

        return line
            .substringAfter(":")
            .substringBefore(";")
            .trim()
    }
}