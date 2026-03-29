package com.fingerdance.ssc

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.fingerdance.ssc.Parser.NoteType
import ssc.RenderSteps

class GameLoop(
    private val chart: Parser.Chart,
    private val audio: AudioEngine,
    private val timing: TimingEngine,
    private val scroll: ScrollEngine,
    private val renderer: RenderSteps,
    private val input: InputProcessorSsc,
    private val batch: SpriteBatch,
    private val onJudgment: ((Int, Long) -> Unit)? = null,
    private val timingOffsetMs: Double = 360.0,
    private val onFrameBeat: ((Double) -> Unit)? = null

) {

    private val judgment = JudgmentEngine(timing)

    private val notes = chart.notes.map {
        GameNote(
            column = it.column,
            beat = it.beat,
            endBeat = it.endBeat,
            isFake = it.isFake,
            type = it.type
        )
    }.toMutableList()

    private val state = GameState()

    enum class Phase { COUNTDOWN, PLAYING, FINISHED }
    private var phase = Phase.COUNTDOWN

    private var countdownStart = System.nanoTime()

    fun start() {
        countdownStart = System.nanoTime()
        phase = Phase.COUNTDOWN

        // 🔥 CRÍTICO: inicializar JudgmentEngine
        judgment.loadNotes(notes)
    }

    // =========================================================
    // INPUT
    // =========================================================

    fun onInput(column: Int, isDown: Boolean) {

        if (phase != Phase.PLAYING) return

        val now = audio.currentTimeMs()
        val lastJudgmentBefore = state.lastJudgment

        if (isDown) {
            judgment.onPress(
                column = column,
                nowMs = now,
                state = state
            )

            // Si el judgment cambió, notificar al renderer
            if (state.lastJudgment != lastJudgmentBefore) {
                val judgmentIndex = when (state.lastJudgment) {
                    JudgmentEngine.Judgment.PERFECT -> 0
                    JudgmentEngine.Judgment.GREAT -> 1
                    JudgmentEngine.Judgment.GOOD -> 2
                    JudgmentEngine.Judgment.BAD -> 3
                    JudgmentEngine.Judgment.MISS -> 4
                }
                onJudgment?.invoke(judgmentIndex, now.toLong())
                renderer.setJudgment(judgmentIndex, now.toLong())
                renderer.setShowExpand(column, now.toLong())
            }
        }
    }

    // =========================================================
    // UPDATE
    // =========================================================

    fun update(screenHeight: Float) {
        when (phase) {
            Phase.COUNTDOWN -> {
                val elapsed = (System.nanoTime() - countdownStart) / 1_000_000
                if (elapsed >= 2000) {
                    phase = Phase.PLAYING
                    audio.play()
                }
                renderEmpty()
            }

            Phase.PLAYING -> {
                val now = audio.currentTimeMs()

                // =========================
                // JUDGMENT (NUEVO)
                // =========================

                judgment.updateMisses(now, state)

                judgment.updateHolds(
                    nowMs = now,
                    isHeld = { col -> input.isHeld(col) },
                    state = state
                )

                // =========================
                // TIMING
                // =========================

                val currentBeat = timing.timeToBeat(now)
                onFrameBeat?.invoke(currentBeat)   // ✅ aquí
                val currentVisual = scroll.beatToVisual(currentBeat)

                // =========================
                // RENDER
                // =========================

                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

                batch.begin()

                renderer.draw(
                    notes = notes,
                    currentVisualBeat = currentVisual,
                    currentBeat = currentBeat,
                    screenHeight = screenHeight,
                    screenWidth = Gdx.graphics.width.toFloat(),
                    timing = timing,
                    scroll = scroll,
                    currentTimeMs = now.toLong(),
                    arrowFrame = ((currentBeat * 6.0) % 6).toInt()
                )

                batch.end()

                // =========================
                // FIN
                // =========================

                if (isFinished()) {
                    phase = Phase.FINISHED
                }
            }

            Phase.FINISHED -> {
                renderEmpty()
            }
        }
    }

    // =========================================================
    // FIN
    // =========================================================

    private fun isFinished(): Boolean {

        val allDone = notes.all { note ->
            when (note.type) {
                NoteType.MINE -> true
                NoteType.TAP -> note.hit || note.missed
                NoteType.HOLD -> note.isFinished()
            }
        }

        return allDone && !audio.isPlaying()
    }

    // =========================================================
    // RENDER AUX
    // =========================================================

    private fun renderEmpty() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        batch.begin()
        batch.end()
    }

    fun getState(): GameState = state
}