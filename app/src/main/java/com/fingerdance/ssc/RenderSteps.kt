package com.fingerdance.ssc

import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.fingerdance.medidaFlechas
import com.fingerdance.playerSong
import com.fingerdance.ssc.Parser.NoteType
import kotlin.math.max
import kotlin.math.min

class RenderSteps(
    private val batch: SpriteBatch,
    private val textures: TextureSet,
    private val showPadB: Int = 0,
    private val hideImagesPadA: Boolean = false,
    private val arrPadsC: Array<Array<TextureRegion>>? = null,
    private val padPositionsC: List<*>? = null,
    private val arrayPad4: Array<TextureRegion>? = null,
    private val recept0Frames: Array<TextureRegion>? = null,
    private val recept1Frames: Array<TextureRegion>? = null,
    private val recept2Frames: Array<TextureRegion>? = null,
    private val recept3Frames: Array<TextureRegion>? = null,
    private val recept4Frames: Array<TextureRegion>? = null
) {

    data class TextureSet(
        val arrows: Array<Array<TextureRegion>>,
        val arrowsBody: Array<Array<TextureRegion>>,
        val arrowsBottom: Array<Array<TextureRegion>>,
        val mines: Array<TextureRegion>,
        val flare: Array<TextureRegion>,
        val receptor: Array<Array<TextureRegion>>,
        val judgments: Array<TextureRegion>? = null,
        val pads: Array<TextureRegion>? = null
    )

    // Judgment feedback data
    data class JudgmentFeedback(var startTime: Long = 0, var judgment: Int = 0)
    private var currentJudgment = JudgmentFeedback()

    // Pad expand feedback data
    data class PadExpandFeedback(var startTime: Long = 0, var column: Int = -1)
    private var currentPadExpand = PadExpandFeedback()

    private val columnX = FloatArray(5)

    // Judgment dimensions (from Player)
    private val widthJudges = com.badlogic.gdx.Gdx.graphics.width / 2
    private val heightJudges = widthJudges / 6

    private var yFlare = medidaFlechas - (medidaFlechas * 2f)
    private val xFlare1 = medidaFlechas * 2.1f
    private val xFlare2 = medidaFlechas * 2.15f
    private val xFlare3 = medidaFlechas * 2.1f
    private val xFlare4 = medidaFlechas * 2.05f
    private val xFlare5 = medidaFlechas * 2.05f
    private val widthFlare = medidaFlechas * 5f
    private val arrPosXFlare = arrayOf(xFlare1, xFlare2, xFlare3, xFlare4, xFlare5)

    var scrollSpeed = (1f * playerSong.speed.replace("X", "").toFloat()) + 1f
    var basePixelsPerBeat = 150f

    // showExpand variables (from Player)
    private val sizeScale = medidaFlechas * 1.2f
    private val topPos = medidaFlechas * 0.9f
    private val posX = medidaFlechas * 0.1f

    // =========================================================
    // FRAME (SIN DRIFT 🔥)
    // =========================================================
    private fun getFrame(currentBeat: Double): Int {
        return ((currentBeat * 6.0) % 6).toInt()
    }

    fun updateLayout(screenWidth: Float) {
        val noteWidth = medidaFlechas
        val total = 5 * noteWidth
        val start = medidaFlechas

        for (i in 0..4) {
            columnX[i] = start + i * (noteWidth)
        }
    }

    // =========================================================
    // JUDGMENT FEEDBACK
    // =========================================================

    fun setJudgment(judgment: Int, currentTimeMs: Long) {
        currentJudgment.judgment = judgment
        currentJudgment.startTime = currentTimeMs
    }

    fun setShowExpand(column: Int, currentTimeMs: Long) {
        currentPadExpand.column = column
        currentPadExpand.startTime = currentTimeMs
    }

    private fun drawJudgment(screenWidth: Float, screenHeight: Float, currentTimeMs: Long) {
        if (currentJudgment.startTime == 0L) return
        if (currentJudgment.startTime + 2500 < currentTimeMs) {
            currentJudgment.startTime = 0
            return
        }

        if (textures.judgments == null) return

        val elapsedTime = currentTimeMs - currentJudgment.startTime

        // Animación exacta de Player
        val (ftX, ftY, alpha) = when {
            elapsedTime < 100 -> {
                val progress = elapsedTime / 300f
                Triple(1.0f + progress, 1.0f + progress, 1f)
            }
            elapsedTime > 1200 -> {
                val progress = ((elapsedTime - 1200) / 300f).coerceIn(0f, 1f)
                Triple(
                    1.0f + 0.6f * progress,
                    1.0f - 0.8f * progress,
                    (1.0f - progress).coerceIn(0f, 1f)
                )
            }
            else -> Triple(1.0f, 1.0f, 1f)
        }

        val judgeSprite = Sprite(textures.judgments[currentJudgment.judgment])
        val judgeW = widthJudges * ftX
        val judgeH = heightJudges * ftY

        judgeSprite.setSize(judgeW, judgeH)
        judgeSprite.setOriginCenter()
        judgeSprite.setCenter(screenWidth / 2f, screenHeight / 2f)
        judgeSprite.setColor(1f, 1f, 1f, alpha)
        judgeSprite.draw(batch)
    }

    // =========================================================
    // PADS DISPLAY (COMO GAMESCREENKSF)
    // =========================================================

    private fun drawBgPads() {
        if(showPadB == 0){
            if(!hideImagesPadA){
                batch.draw(textures.pads!![0], com.fingerdance.padPositions[0][0], com.fingerdance.padPositions[0][1], com.fingerdance.widthBtns, com.fingerdance.heightBtns)
                batch.draw(textures.pads[1], com.fingerdance.padPositions[1][0], com.fingerdance.padPositions[1][1], com.fingerdance.widthBtns, com.fingerdance.heightBtns)
                batch.draw(textures.pads[2], com.fingerdance.padPositions[2][0], com.fingerdance.padPositions[2][1], com.fingerdance.widthBtns, com.fingerdance.heightBtns)
                batch.draw(textures.pads[3], com.fingerdance.padPositions[3][0], com.fingerdance.padPositions[3][1], com.fingerdance.widthBtns, com.fingerdance.heightBtns)
                batch.draw(textures.pads[4], com.fingerdance.padPositions[4][0], com.fingerdance.padPositions[4][1], com.fingerdance.widthBtns, com.fingerdance.heightBtns)
            }
        }
    }

    // =========================================================
    // SHOW EXPAND (COMO PLAYER)
    // =========================================================

    private fun showExpand(position: Int, arrowFrame: Int) {
        if (currentPadExpand.column != position) return
        if (currentPadExpand.startTime == 0L) return
        if (currentPadExpand.startTime + 300 < System.currentTimeMillis()) {
            currentPadExpand.startTime = 0
            currentPadExpand.column = -1
            return
        }

        batch.setColor(1f, 1f, 1f, 0.7f)
        when (position) {
            0 -> {
                batch.draw(recept0Frames!![2], medidaFlechas - posX, topPos, sizeScale, sizeScale)
                if(showPadB == 2 && arrPadsC != null && padPositionsC != null){
                    batch.setColor(1f, 1f, 1f, 1f)
                    @Suppress("UNCHECKED_CAST")
                    val padPos = (padPositionsC as List<com.fingerdance.GameScreenKsf.PadPositionC>)[position]
                    batch.draw(arrPadsC[position][arrowFrame], padPos.x, padPos.y, padPos.size, padPos.size)
                }
                if(showPadB == 3 && arrayPad4 != null){
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(arrayPad4[position], com.fingerdance.padPositions[0][0], com.fingerdance.padPositions[0][1], com.fingerdance.widthBtns, com.fingerdance.heightBtns)
                }
            }
            1 -> {
                batch.draw(recept1Frames!![2], (medidaFlechas * 2) - posX, topPos, sizeScale, sizeScale)
                if(showPadB == 2 && arrPadsC != null && padPositionsC != null){
                    batch.setColor(1f, 1f, 1f, 1f)
                    @Suppress("UNCHECKED_CAST")
                    val padPos = (padPositionsC as List<com.fingerdance.GameScreenKsf.PadPositionC>)[position]
                    batch.draw(arrPadsC[position][arrowFrame], padPos.x, padPos.y, padPos.size, padPos.size)
                }
                if(showPadB == 3 && arrayPad4 != null){
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(arrayPad4[position], com.fingerdance.padPositions[1][0], com.fingerdance.padPositions[1][1], com.fingerdance.widthBtns, com.fingerdance.heightBtns)
                }
            }
            2 -> {
                batch.draw(recept2Frames!![2], (medidaFlechas * 3) - posX, topPos, sizeScale, sizeScale)
                if(showPadB == 2 && arrPadsC != null && padPositionsC != null){
                    batch.setColor(1f, 1f, 1f, 1f)
                    @Suppress("UNCHECKED_CAST")
                    val padPos = (padPositionsC as List<com.fingerdance.GameScreenKsf.PadPositionC>)[position]
                    batch.draw(arrPadsC[position][arrowFrame], padPos.x, padPos.y, padPos.size, padPos.size)
                }
                if(showPadB == 3 && arrayPad4 != null){
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(arrayPad4[position], com.fingerdance.padPositions[2][0], com.fingerdance.padPositions[2][1], com.fingerdance.widthBtns, com.fingerdance.heightBtns)
                }
            }
            3 -> {
                batch.draw(recept3Frames!![2], (medidaFlechas * 4) - posX, topPos, sizeScale, sizeScale)
                if(showPadB == 2 && arrPadsC != null && padPositionsC != null){
                    batch.setColor(1f, 1f, 1f, 1f)
                    @Suppress("UNCHECKED_CAST")
                    val padPos = (padPositionsC as List<com.fingerdance.GameScreenKsf.PadPositionC>)[position]
                    batch.draw(arrPadsC[position][arrowFrame], padPos.x, padPos.y, padPos.size, padPos.size)
                }
                if(showPadB == 3 && arrayPad4 != null){
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(arrayPad4[position], com.fingerdance.padPositions[3][0], com.fingerdance.padPositions[3][1], com.fingerdance.widthBtns, com.fingerdance.heightBtns)
                }
            }
            4 -> {
                batch.draw(recept4Frames!![2], (medidaFlechas * 5) - posX, topPos, sizeScale, sizeScale)
                if(showPadB == 2 && arrPadsC != null && padPositionsC != null){
                    batch.setColor(1f, 1f, 1f, 1f)
                    @Suppress("UNCHECKED_CAST")
                    val padPos = (padPositionsC as List<com.fingerdance.GameScreenKsf.PadPositionC>)[position]
                    batch.draw(arrPadsC[position][arrowFrame], padPos.x, padPos.y, padPos.size, padPos.size)
                }
                if(showPadB == 3 && arrayPad4 != null){
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(arrayPad4[position], com.fingerdance.padPositions[4][0], com.fingerdance.padPositions[4][1], com.fingerdance.widthBtns, com.fingerdance.heightBtns)
                }
            }
        }
        batch.setColor(1f, 1f, 1f, 1f)
    }

    fun draw(
        notes: List<GameNote>,
        currentVisualBeat: Double,
        currentBeat: Double,
        screenHeight: Float,
        screenWidth: Float,
        timing: TimingEngine,
        scroll: ScrollEngine,
        currentTimeMs: Long = 0,
        arrowFrame: Int = 0
    ) {

        val frame = getFrame(currentBeat)

        val receptorY = screenHeight * 0.2f
        val pxPerBeat = basePixelsPerBeat * scrollSpeed

        fun y(beat: Double): Float {
            val vb = scroll.beatToVisual(beat)
            return receptorY + ((vb - currentVisualBeat) * pxPerBeat).toFloat()
        }

        // =========================================================
        // 1. HOLD BODY
        // =========================================================
        for (note in notes) {

            if (note.type != NoteType.HOLD) continue
            if (note.missed) continue
            if (note.holdState == GameNote.HoldState.COMPLETED) continue

            val endBeat = note.endBeat ?: continue

            val headY = y(note.beat)
            val tailY = y(endBeat)

            if (headY < -medidaFlechas && tailY < -medidaFlechas) continue
            if (headY > screenHeight + medidaFlechas && tailY > screenHeight + medidaFlechas) continue

            val clippedHead = max(headY, receptorY)
            val clippedTail = min(tailY, receptorY)

            val top = min(clippedHead, clippedTail)
            val bottom = max(clippedHead, clippedTail)

            val height = bottom - top
            if (height <= 0) continue

            val cx = (columnX[note.column])

            val body = textures.arrowsBody[note.column][frame]

            batch.draw(body, cx , top, medidaFlechas, height)

            // tail
            if (tailY < receptorY) {
                val tail = textures.arrowsBottom[note.column][frame]
                batch.draw(tail, cx - (medidaFlechas / 2f), tailY - 10f, medidaFlechas, medidaFlechas)
            }
        }

        // =========================================================
        // 2. NOTES
        // =========================================================
        for (note in notes) {

            if (note.missed) continue
            if (note.type == NoteType.TAP && note.hit) continue
            if (note.type == NoteType.HOLD &&
                note.holdState == GameNote.HoldState.COMPLETED
            ) continue

            val noteY = y(note.beat)
            val cx = (columnX[note.column])

            if (noteY > screenHeight + medidaFlechas) continue
            if (noteY < -medidaFlechas) continue

            when (note.type) {

                NoteType.TAP,
                NoteType.HOLD -> {

                    val region = textures.arrows[note.column][frame]

                    batch.draw(region, cx, noteY - 40f, medidaFlechas, medidaFlechas)
                }

                NoteType.MINE -> {

                    val region = textures.mines[frame]

                    batch.draw(region, cx - 40f, noteY - 40f, 80f, 80f)
                }
            }

            // =========================================================
            // FLARE (simple)
            // =========================================================
            if (note.hit && note.type != NoteType.MINE) {
                val flare = textures.flare[frame]
                batch.draw(flare, arrPosXFlare[note.column], yFlare, widthFlare, widthFlare)
            }
        }

        // =========================================================
        // 3. RECEPTORS
        // =========================================================
        for (i in 0..4) {
            batch.draw(
                textures.receptor[i][0],
                medidaFlechas * (i + 1),
                medidaFlechas,
                medidaFlechas,
                medidaFlechas
            )
        }

        // =========================================================
        // 4. PADS (COMO GAMESCREENKSF showBgPads)
        // =========================================================
        drawBgPads()

        // =========================================================
        // 5. SHOW EXPAND (COMO PLAYER)
        // =========================================================
        for (i in 0..4) {
            showExpand(i, arrowFrame)
        }

        // =========================================================
        // 6. JUDGMENTS
        // =========================================================
        drawJudgment(screenWidth, screenHeight, currentTimeMs)
    }
}
