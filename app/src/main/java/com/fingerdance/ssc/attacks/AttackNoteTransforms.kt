package com.fingerdance.ssc.attacks

import com.fingerdance.ssc.Parser
import com.fingerdance.ssc.TimmingData
import kotlin.math.abs
import kotlin.math.roundToInt

object AttackNoteTransforms {

    private const val ROWS_PER_BEAT = 48
    private const val BIG_WINDOW_ROWS = ROWS_PER_BEAT
    private const val BIG_INSERT_OFFSET_ROWS = ROWS_PER_BEAT / 2
    private const val BIG_STRIDE_ROWS = ROWS_PER_BEAT

    fun apply(
        sourceNotes: List<Parser.Note>,
        attacks: List<AttackEvent>,
        timingData: TimmingData,
        columnCount: Int
    ): List<Parser.Note> {
        if (sourceNotes.isEmpty() || attacks.isEmpty()) {
            return sourceNotes
        }

        val notes = sourceNotes.toMutableList()

        attacks.sortedBy { it.startSecond }
            .forEach { attack ->
                val hasBig = attack.modifiers.any { raw ->
                    val modifier = AttackModifierParser.parse(raw)
                    modifier.type == AttackMod.BIG && modifier.level != 0f
                }

                if (!hasBig) {
                    return@forEach
                }

                val startBeat = timingData.timeToBeat(attack.startSecond * 1000.0)
                val endBeat = timingData.timeToBeat(attack.endSecond * 1000.0)

                val startRow = beatToNoteRow(startBeat)
                val endRow = beatToNoteRow(endBeat)

                applyBig(
                    notes = notes,
                    startRow = startRow,
                    endRow = endRow,
                    columnCount = columnCount
                )
            }

        return notes.sortedWith(
            compareBy<Parser.Note>(
                { it.beat },
                { it.column }
            )
        )
    }

    private fun applyBig(
        notes: MutableList<Parser.Note>,
        startRow: Int,
        endRow: Int,
        columnCount: Int
    ) {
        if (columnCount <= 0 || endRow <= startRow) {
            return
        }
        val quantizedStart = quantize(startRow, BIG_STRIDE_ROWS)

        val candidateRows = notes
                .asSequence()
                .map { beatToNoteRow(it.beat) }
                .filter {
                    it >= quantizedStart && it < endRow
                }
                .distinct()
                .sorted()
                .toList()

        for (rowEarlier in candidateRows) {
            if (rowEarlier % BIG_STRIDE_ROWS != 0) {
                continue
            }

            val rowLater = rowEarlier + BIG_WINDOW_ROWS
            val rowToAdd = rowEarlier + BIG_INSERT_OFFSET_ROWS

            if (getNumTapNonEmptyTracks(notes, rowEarlier) != 1 ||
                getNumTracksWithTapOrHoldHead(notes, rowEarlier) != 1) {
                continue
            }
            if (getNumTapNonEmptyTracks(notes, rowLater) != 1 ||
                getNumTracksWithTapOrHoldHead(notes, rowLater) != 1) {
                continue
            }

            var noteInMiddle = false

            for (column in 0 until columnCount) {
                if (isHoldNoteAtRow(notes = notes, column = column, row = rowEarlier + 1)) {
                    noteInMiddle = true
                    break
                }
            }

            if (noteInMiddle) {
                continue
            }
            noteInMiddle = notes.any { note ->
                    val row = beatToNoteRow(note.beat)
                    row > rowEarlier && row < rowLater
                }

            if (noteInMiddle) {
                continue
            }

            val trackEarlier = getTapFirstNonEmptyTrack(notes, rowEarlier)
            val trackLater = getTapFirstNonEmptyTrack(notes, rowLater)

            if (trackEarlier < 0 || trackLater < 0) {
                continue
            }

            val trackToAdd =
                when {
                    abs(trackEarlier - trackLater) >= 2 -> {
                        minOf(trackEarlier, trackLater) + 1
                    }

                    minOf(trackEarlier, trackLater) - 1 >= 0 -> {
                        minOf(trackEarlier, trackLater) - 1
                    }

                    maxOf(trackEarlier, trackLater) + 1 < columnCount -> {
                        maxOf(trackEarlier, trackLater) + 1
                    }

                    else -> {
                        continue
                    }
                }

            val alreadyExists = notes.any { note ->
                    note.column == trackToAdd && beatToNoteRow(note.beat) == rowToAdd
                }

            if (alreadyExists) {
                continue
            }
            println("BIG INSERT -> beat=${noteRowToBeat(rowToAdd)} col=$trackToAdd")
            notes.add(
                Parser.Note(
                    column = trackToAdd,
                    beat = noteRowToBeat(rowToAdd),
                    endBeat = null,
                    isFake = false,
                    isVanish = false,
                    isHide = false,
                    isPhantom = false,
                    isMine = false,
                    isPressed = false,
                    type = Parser.NoteType.TAP
                )
            )
        }
    }

    private fun getNumTapNonEmptyTracks(notes: List<Parser.Note>, row: Int): Int {
        return notes.asSequence().filter { beatToNoteRow(it.beat) == row }.map { it.column }.distinct().count()
    }

    private fun getNumTracksWithTapOrHoldHead(notes: List<Parser.Note>, row: Int): Int {
        return notes.asSequence().filter { note ->
                beatToNoteRow(note.beat) == row &&
                        !note.isMine &&
                        (note.type == Parser.NoteType.TAP
                                || note.type == Parser.NoteType.HOLD)
            }
            .map { it.column }
            .distinct()
            .count()
    }

    private fun getTapFirstNonEmptyTrack(notes: List<Parser.Note>, row: Int): Int {
        return notes.asSequence().filter { beatToNoteRow(it.beat) == row }.map { it.column }.minOrNull() ?: -1
    }

    private fun isHoldNoteAtRow(notes: List<Parser.Note>, column: Int, row: Int): Boolean {
        return notes.any { note ->
            if (note.column != column || note.type != Parser.NoteType.HOLD) {
                return@any false
            }

            val headRow = beatToNoteRow(note.beat)
            val tailRow = beatToNoteRow(note.endBeat ?: return@any false)
            row > headRow && row <= tailRow
        }
    }

    private fun beatToNoteRow(beat: Double): Int {
        return (beat * ROWS_PER_BEAT).roundToInt()
    }

    private fun noteRowToBeat(row: Int): Double {
        return row.toDouble() / ROWS_PER_BEAT.toDouble()
    }

    private fun quantize(value: Int, interval: Int): Int {
        return ((value + interval / 2) / interval) * interval
    }
}