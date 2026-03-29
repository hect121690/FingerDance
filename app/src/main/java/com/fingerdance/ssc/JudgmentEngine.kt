package com.fingerdance.ssc

import com.fingerdance.ssc.Parser.NoteType
import kotlin.math.abs

class JudgmentEngine(
    private val timing: TimingEngine
) {

    enum class Judgment {
        PERFECT, GREAT, GOOD, BAD, MISS
    }

    data class TimingWindows(
        val perfect: Double = 22.5,
        val great: Double = 45.0,
        val good: Double = 90.0,
        val bad: Double = 135.0
    )

    private val windows = TimingWindows()

    // =========================================================
    // COLUMN INDEXING (🔥 clave)
    // =========================================================

    private val columnNotes = Array(5) { mutableListOf<GameNote>() }
    private val columnIndex = IntArray(5)

    fun loadNotes(notes: List<GameNote>) {
        for (i in columnNotes.indices) {
            columnNotes[i].clear()
            columnIndex[i] = 0
        }

        notes.forEach {
            columnNotes[it.column].add(it)
        }

        columnNotes.forEach { it.sortBy { n -> n.beat } }
    }

    // =========================================================
    // JUDGMENT
    // =========================================================

    private fun getJudgment(deltaMs: Double): Judgment? {
        val a = abs(deltaMs)

        return when {
            a <= windows.perfect -> Judgment.PERFECT
            a <= windows.great -> Judgment.GREAT
            a <= windows.good -> Judgment.GOOD
            a <= windows.bad -> Judgment.BAD
            else -> null
        }
    }

    // =========================================================
    // INPUT HIT (TAP + HOLD START)
    // =========================================================

    fun onPress(
        column: Int,
        nowMs: Double,
        state: GameState
    ) {

        val list = columnNotes[column]
        var idx = columnIndex[column]

        while (idx < list.size) {

            val note = list[idx]

            if (!note.isHittable()) {
                idx++
                continue
            }

            val noteTime = timing.beatToTime(note.beat)
            val delta = nowMs - noteTime

            // demasiado temprano → no consumir
            if (delta < -windows.bad) break

            // dentro de ventana
            val judgment = getJudgment(delta)

            if (judgment != null) {

                note.hit = true

                if (note.type == NoteType.HOLD) {
                    note.holdState = GameNote.HoldState.HIT
                    note.holdStartTimeMs = nowMs
                }

                applyJudgment(state, judgment, nowMs)

                columnIndex[column] = idx + 1
                return
            }

            // demasiado tarde → skip como miss
            if (delta > windows.bad) {
                note.missed = true
                applyJudgment(state, Judgment.MISS, nowMs)
                idx++
                columnIndex[column] = idx
                continue
            }

            break
        }
    }

    // =========================================================
    // MISSES (AUTO)
    // =========================================================

    fun updateMisses(nowMs: Double, state: GameState) {

        for (col in columnNotes.indices) {

            val list = columnNotes[col]
            var idx = columnIndex[col]

            while (idx < list.size) {

                val note = list[idx]

                if (!note.isHittable()) {
                    idx++
                    continue
                }

                val noteTime = timing.beatToTime(note.beat)
                val delta = nowMs - noteTime

                if (delta > windows.bad) {
                    note.missed = true
                    applyJudgment(state, Judgment.MISS, nowMs)
                    idx++
                    columnIndex[col] = idx
                } else break
            }
        }
    }

    // =========================================================
    // HOLDS (CORRECTO)
    // =========================================================

    fun updateHolds(
        nowMs: Double,
        isHeld: (Int) -> Boolean,
        state: GameState
    ) {

        for (col in columnNotes.indices) {

            for (note in columnNotes[col]) {

                if (note.type != NoteType.HOLD) continue

                val endBeat = note.endBeat ?: continue
                val endTime = timing.beatToTime(endBeat)

                when (note.holdState) {

                    GameNote.HoldState.HIT,
                    GameNote.HoldState.HOLDING -> {

                        val held = isHeld(note.column)

                        if (!held) {
                            note.holdState = GameNote.HoldState.RELEASED_EARLY
                            applyJudgment(state, Judgment.MISS, nowMs)
                        } else if (nowMs >= endTime) {
                            note.holdState = GameNote.HoldState.COMPLETED
                            state.score += 100 // bonus hold
                        } else {
                            note.holdState = GameNote.HoldState.HOLDING
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    // =========================================================
    // APPLY
    // =========================================================

    private fun applyJudgment(
        state: GameState,
        judgment: Judgment,
        nowMs: Double
    ) {

        state.judgments[judgment] =
            state.judgments.getOrDefault(judgment, 0) + 1

        val score = when (judgment) {
            Judgment.PERFECT -> 1000
            Judgment.GREAT -> 500
            Judgment.GOOD -> 200
            Judgment.BAD -> -100
            Judgment.MISS -> -200
        }

        if (score > 0) {
            val comboMult = 1 + minOf(state.combo, 100) / 100.0
            state.score += (score * comboMult).toInt()
            state.combo++
            state.maxCombo = maxOf(state.maxCombo, state.combo)
        } else {
            state.score = maxOf(0, state.score + score)
            state.combo = 0
        }

        val lifeDelta = when (judgment) {
            Judgment.PERFECT -> 0.02
            Judgment.GREAT -> 0.015
            Judgment.GOOD -> 0.005
            Judgment.BAD -> -0.05
            Judgment.MISS -> -0.1
        }

        state.life = (state.life + lifeDelta).coerceIn(0.0, 1.0)
        state.lastJudgment = judgment
        state.lastJudgmentTime = nowMs

        if (state.life <= 0) state.failed = true
    }
}