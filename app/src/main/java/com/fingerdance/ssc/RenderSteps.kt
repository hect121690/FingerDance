package ssc

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.fingerdance.GameScreenKsf
import com.fingerdance.alphaPadB
import com.fingerdance.height
import com.fingerdance.heightBtns
import com.fingerdance.medidaFlechas
import com.fingerdance.padPositions
import com.fingerdance.playerSong
import com.fingerdance.ssc.GameNote
import com.fingerdance.ssc.Parser.NoteType
import com.fingerdance.ssc.ScrollEngine
import com.fingerdance.ssc.TimingEngine
import com.fingerdance.width
import com.fingerdance.widthBtns
import kotlin.math.max
import kotlin.math.min

class RenderSteps(
    private val batch: SpriteBatch,
    private val textures: TextureSet,
    private val showPadB: Int = 0,
    private val hideImagesPadA: Boolean = false,

    // Pads B/C/D (como KSF)
    private val spritePadB: com.badlogic.gdx.graphics.g2d.Sprite? = null,
    private val padCBg: TextureRegion? = null,
    private val arrPadsC: Array<Array<TextureRegion>>? = null,
    private val padPositionsC: List<GameScreenKsf.PadPositionC>? = null,
    private val arrayPad4Bg: Array<TextureRegion>? = null,
    private val arrayPad4: Array<TextureRegion>? = null,

    // Receptors expand frames
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
        val pads: Array<TextureRegion>? = null // PadA
    )

    // Judgment feedback data
    data class JudgmentFeedback(var startTime: Long = 0, var judgment: Int = 0)
    private var currentJudgment = JudgmentFeedback()

    // Pad expand feedback data
    data class PadExpandFeedback(var startTime: Long = 0, var column: Int = -1)
    private var currentPadExpand = PadExpandFeedback()

    private val columnX = FloatArray(5)

    // Lua offsets (como KSF)
    private var luaReceptOffsetX = 0f
    private var luaNoteOffsetX = 0f

    fun setLuaOffsets(receptorOffsetX: Float, noteOffsetX: Float) {
        luaReceptOffsetX = receptorOffsetX
        luaNoteOffsetX = noteOffsetX
    }

    // Judgment dimensions (como Player)
    private val widthJudges = Gdx.graphics.width / 2
    private val heightJudges = widthJudges / 6

    // Flare (como Player)
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

    // showExpand variables (como Player)
    private val sizeScale = medidaFlechas * 1.2f
    private val topPos = medidaFlechas * 0.9f
    private val posX = medidaFlechas * 0.1f

    private fun getFrame(currentBeat: Double): Int = ((currentBeat * 6.0) % 6).toInt()

    fun updateLayout(screenWidth: Float) {
        for (i in 0..4) {
            columnX[i] = medidaFlechas * (i + 1)
        }
    }

    fun setJudgment(judgment: Int, currentTimeMs: Long) {
        currentJudgment.judgment = judgment
        currentJudgment.startTime = currentTimeMs
    }

    fun setShowExpand(column: Int, currentTimeMs: Long) {
        currentPadExpand.column = column
        currentPadExpand.startTime = currentTimeMs
    }

    private fun drawJudgment(screenWidth: Float, screenHeight: Float, currentTimeMs: Long) {
        val judgments = textures.judgments ?: return
        if (currentJudgment.startTime == 0L) return
        if (currentJudgment.startTime + 2500 < currentTimeMs) {
            currentJudgment.startTime = 0
            return
        }

        val elapsedTime = currentTimeMs - currentJudgment.startTime
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

        val judgeSprite = Sprite(judgments[currentJudgment.judgment])
        val judgeW = widthJudges * ftX
        val judgeH = heightJudges * ftY
        judgeSprite.setSize(judgeW, judgeH)
        judgeSprite.setOriginCenter()
        judgeSprite.setCenter(screenWidth / 2f, screenHeight / 2f)
        judgeSprite.setColor(1f, 1f, 1f, alpha)
        judgeSprite.draw(batch)
    }

    private fun drawBgPads() {
        // Pad A (tema)
        if (showPadB == 0) {
            if (!hideImagesPadA) {
                val pads = textures.pads ?: return
                batch.draw(pads[0], padPositions[0][0], padPositions[0][1], widthBtns, heightBtns)
                batch.draw(pads[1], padPositions[1][0], padPositions[1][1], widthBtns, heightBtns)
                batch.draw(pads[2], padPositions[2][0], padPositions[2][1], widthBtns, heightBtns)
                batch.draw(pads[3], padPositions[3][0], padPositions[3][1], widthBtns, heightBtns)
                batch.draw(pads[4], padPositions[4][0], padPositions[4][1], widthBtns, heightBtns)
            }
            return
        }

        // Pad B
        if (showPadB == 1) {
            spritePadB?.setAlpha(alphaPadB)
            val posYpadB = height.toFloat() - (width.toFloat() * 1.1f)
            spritePadB?.setBounds(0f, posYpadB, width.toFloat(), width.toFloat() * 1.1f)
            spritePadB?.draw(batch)
            return
        }

        // Pad C
        if (showPadB == 2) {
            padCBg?.let {
                batch.draw(it, width.toFloat() * 0.05f, width.toFloat() * 1.1f, width.toFloat() * 0.9f, width.toFloat() * 0.9f)
            }
            return
        }

        // Pad D
        if (showPadB == 3) {
            val bg = arrayPad4Bg ?: return
            batch.draw(bg[0], padPositions[0][0], padPositions[0][1], widthBtns, heightBtns)
            batch.draw(bg[1], padPositions[1][0], padPositions[1][1], widthBtns, heightBtns)
            batch.draw(bg[2], padPositions[2][0], padPositions[2][1], widthBtns, heightBtns)
            batch.draw(bg[3], padPositions[3][0], padPositions[3][1], widthBtns, heightBtns)
            batch.draw(bg[4], padPositions[4][0], padPositions[4][1], widthBtns, heightBtns)
        }
    }

    private fun showExpand(position: Int, arrowFrame: Int, nowMs: Long) {
        if (currentPadExpand.column != position) return
        if (currentPadExpand.startTime == 0L) return

        if (currentPadExpand.startTime + 300 < nowMs) {
            currentPadExpand.startTime = 0
            currentPadExpand.column = -1
            return
        }

        batch.setColor(1f, 1f, 1f, 0.7f)

        fun drawPadC() {
            if (showPadB == 2 && arrPadsC != null && padPositionsC != null) {
                val padPos = padPositionsC[position]
                batch.setColor(1f, 1f, 1f, 1f)
                batch.draw(arrPadsC[position][arrowFrame], padPos.x, padPos.y, padPos.size, padPos.size)
            }
        }

        fun drawPadD() {
            if (showPadB == 3 && arrayPad4 != null) {
                batch.setColor(1f, 1f, 1f, 1f)
                batch.draw(arrayPad4[position], padPositions[position][0], padPositions[position][1], widthBtns, heightBtns)
            }
        }

        when (position) {
            0 -> { batch.draw(recept0Frames!![2], (medidaFlechas) - posX + luaReceptOffsetX, topPos, sizeScale, sizeScale); drawPadC(); drawPadD() }
            1 -> { batch.draw(recept1Frames!![2], (medidaFlechas * 2) - posX + luaReceptOffsetX, topPos, sizeScale, sizeScale); drawPadC(); drawPadD() }
            2 -> { batch.draw(recept2Frames!![2], (medidaFlechas * 3) - posX + luaReceptOffsetX, topPos, sizeScale, sizeScale); drawPadC(); drawPadD() }
            3 -> { batch.draw(recept3Frames!![2], (medidaFlechas * 4) - posX + luaReceptOffsetX, topPos, sizeScale, sizeScale); drawPadC(); drawPadD() }
            4 -> { batch.draw(recept4Frames!![2], (medidaFlechas * 5) - posX + luaReceptOffsetX, topPos, sizeScale, sizeScale); drawPadC(); drawPadD() }
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

        // ✅ igual a KSF
        val receptorY = medidaFlechas
        val pxPerBeat = basePixelsPerBeat * scrollSpeed

        fun y(beat: Double): Float {
            val vb = scroll.beatToVisual(beat)
            return receptorY + ((vb - currentVisualBeat) * pxPerBeat).toFloat()
        }

        // =========================================================
        // 1) HOLD BODY
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

            val cx = columnX[note.column] + luaNoteOffsetX
            val body = textures.arrowsBody[note.column][frame]

            // Body alineado a columna (sin cx - half)
            batch.draw(body, cx, top, medidaFlechas, height)

            // bottom cap
            if (tailY < receptorY) {
                val tail = textures.arrowsBottom[note.column][frame]
                batch.draw(tail, cx, tailY - 10f, medidaFlechas, medidaFlechas)
            }
        }

        // =========================================================
        // 2) NOTES
        // =========================================================
        for (note in notes) {
            if (note.missed) continue
            if (note.type == NoteType.TAP && note.hit) continue
            if (note.type == NoteType.HOLD && note.holdState == GameNote.HoldState.COMPLETED) continue

            val noteY = y(note.beat)
            val cx = columnX[note.column] + luaNoteOffsetX

            if (noteY > screenHeight + medidaFlechas) continue
            if (noteY < -medidaFlechas) continue

            when (note.type) {
                NoteType.TAP, NoteType.HOLD -> {
                    val region = textures.arrows[note.column][frame]
                    batch.draw(region, cx, noteY - 40f, medidaFlechas, medidaFlechas)
                }
                NoteType.MINE -> {
                    val region = textures.mines[frame]
                    batch.draw(region, cx - 40f, noteY - 40f, 80f, 80f)
                }
            }

            if (note.hit && note.type != NoteType.MINE) {
                val flare = textures.flare[frame]
                batch.draw(flare, arrPosXFlare[note.column] + luaNoteOffsetX, yFlare, widthFlare, widthFlare)
            }
        }

        // =========================================================
        // 3) RECEPTORS (con luaReceptOffsetX)
        // =========================================================
        for (i in 0..4) {
            batch.draw(
                textures.receptor[i][0],
                (medidaFlechas * (i + 1)) + luaReceptOffsetX,
                receptorY,
                medidaFlechas,
                medidaFlechas
            )
        }

        // =========================================================
        // 4) PADS
        // =========================================================
        drawBgPads()

        // =========================================================
        // 5) SHOW EXPAND
        // =========================================================
        for (i in 0..4) {
            showExpand(i, arrowFrame, currentTimeMs)
        }

        // =========================================================
        // 6) JUDGMENTS
        // =========================================================
        drawJudgment(screenWidth, screenHeight, currentTimeMs)
    }
}