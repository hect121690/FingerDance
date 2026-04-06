package com.fingerdance.ssc

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils.sin
import com.fingerdance.GameScreenActivity
import com.fingerdance.KsfProccess.TypeNote
import com.fingerdance.aBatch
import com.fingerdance.bBatch
import com.fingerdance.breakSong
import com.fingerdance.chart
import com.fingerdance.height
import com.fingerdance.heightBtns
import com.fingerdance.heightJudges
import com.fingerdance.isMidLine
import com.fingerdance.isOnline
import com.fingerdance.medidaFlechas
import com.fingerdance.padPositions
import com.fingerdance.playerSong
import com.fingerdance.resultSong
import com.fingerdance.ruta
import com.fingerdance.showPadB
import com.fingerdance.soundPoolSelectSongKsf
import com.fingerdance.sound_mine
import com.fingerdance.valueOffset
import com.fingerdance.widthBtns
import com.fingerdance.widthJudges
import java.io.File
import kotlin.math.min

class PlayerSsc(private val batch: SpriteBatch, activity: GameScreenActivity) : GameScreenSsc(activity) {

    private val bpms = chart.bpms
    private val tickcounts = chart.tickcounts
    private val stops = chart.stops
    private val warps = chart.warps
    private val notes = chart.notes
    private val extendedNotes = chart.extendedNotes
    private val speeds = chart.speeds
    private val scrolls = chart.scrolls

    private val sizeScale = medidaFlechas * 1.2f
    private val topPos = medidaFlechas * 0.9f
    private val posX = medidaFlechas * 0.1f

    private val xFlare1 = medidaFlechas * 2.1f
    private val xFlare2 = medidaFlechas * 2.15f
    private val xFlare3 = medidaFlechas * 2.1f
    private val xFlare4 = medidaFlechas * 2.05f
    private val xFlare5 = medidaFlechas * 2.05f
    private val animationDuration: Long = 300L
    private var elapsedTimeToExpands = 0L

    companion object {
        val STEPSIZE = medidaFlechas.toInt()
        val MEASURE = height * 0.25
        val MEASUREVANISH = medidaFlechas * 3

        const val MINUTE = 60000f

        private var ZONE_PERFECT: Long = if (playerSong.hj) 18 else 40
        private var ZONE_GREAT: Long = if (playerSong.hj) 40 else 80
        private var ZONE_GOOD: Long = if (playerSong.hj) 80 else 100
        private var ZONE_BAD: Long = if (playerSong.hj) 100 else 130

        const val JUDGE_PERFECT = 0
        const val JUDGE_GREAT = 1
        const val JUDGE_GOOD = 2
        const val JUDGE_BAD = 3
        const val JUDGE_MISS = 4

        const val KEY_NONE = 0
        const val KEY_DOWN = 1
        const val KEY_PRESS = 2
        const val KEY_UP = 3

        const val MINE_PENALTY = 0.025f

        private lateinit var mine: Texture
        private lateinit var downLeftTap: Texture
        private lateinit var upLeftTap: Texture
        private lateinit var centerTap: Texture
        private lateinit var upRightTap: Texture
        private lateinit var downRightTap: Texture

        private lateinit var downLeftBody: Texture
        private lateinit var upLeftBody: Texture
        private lateinit var centerBody: Texture
        private lateinit var upRightBody: Texture
        private lateinit var downRightBody: Texture

        private lateinit var downLeftBottom: Texture
        private lateinit var upLeftBottom: Texture
        private lateinit var centerBottom: Texture
        private lateinit var upRightBottom: Texture
        private lateinit var downRightBottom: Texture

        private lateinit var arrMines: Array<TextureRegion>

        private lateinit var arrArrows: Array<Array<TextureRegion>>
        private lateinit var arrArrowsBody: Array<Array<TextureRegion>>
        private lateinit var arrArrowsBottom: Array<Array<TextureRegion>>

        private lateinit var sprFlare: Texture

        private lateinit var flareArrowFrame: Array<TextureRegion>

        private val LONGNOTE = Array(5) { LongNotePress() }

        private lateinit var whiteTex: Texture
    }

    data class PlayerFlare(var startTime: Long = 0)
    data class PlayerJudge(var startTime: Long = 0, var judge: Int = 0)
    data class JudgePos(
        val x: Int = widthJudges - (widthJudges / 2),
        val y: Int = Gdx.graphics.height / 2 - heightJudges * 6
    )

    private val baseSpeed = playerSong.speed.replace("X", "").toFloat()

    private val timingData = TimmingData(
        bpms = bpms,
        stops = stops,
        warps = warps,
        speeds = speeds,
        scrolls = scrolls,
        offsetMs = chart.offset * 1000.0,
        userOffsetMs = valueOffset * 10.0
    )

    private val hitNotes = mutableSetOf<Parser.Note>()
    private val finishedHolds = mutableSetOf<Parser.Note>()

    private val columnNotes = Array(5) { mutableListOf<Parser.Note>() }
    private val columnIndex = IntArray(5)

    private fun initColumnNotes() {
        for (n in notes) {
            if (n.column in 0..4) {
                columnNotes[n.column].add(n)
            }
        }
        for (c in 0 until 5) {
            columnNotes[c].sortBy { it.beat }
            columnIndex[c] = 0
        }
    }

    private var m_fGauge = 0.35f
    var m_fCurBPM = 0f
    private var arrowFrame = 0
    private var m_iStepWidth = 5
    var curCombo = 0
    var curComboMiss = 0
    private val flare = Array(5) { PlayerFlare() }
    private var m_judge = PlayerJudge()
    private var noEffects = false

    private var mineFlashStartTime: Long = 0L
    private val MINE_FLASH_DURATION: Long = 100L

    private val widthFlare = medidaFlechas * 5f
    private var yFlare = medidaFlechas - (medidaFlechas * 2f)

    private val inputProcessor = InputProcessorSsc()

    private var currentTimeToExpands = 0L

    private val judgePos = JudgePos()
    private val x = judgePos.x.toFloat()
    private val y = judgePos.y.toFloat()
    private var digitWidth = medidaFlechas * 0.7f
    private var digitHeight = heightJudges * 1.3f

    private val GaugeInc = if (playerSong.hj) gaugeIncHJ else gaugeIncNormal
    val tipWidth = (medidaFlechas / 4f)
    val tipHeight = medidaFlechas / 1.5f
    val tipY = 0f - (medidaFlechas * 0.05f)

    private val userOffsetUnits: Long = valueOffset
    private val userOffsetMs: Double
        get() = userOffsetUnits * 10.0

    init {
        currentTimeToExpands = timeGetTime()
        initCommonInfo()
        Gdx.input.inputProcessor = inputProcessor

        for (x in 0 until 5) {
            LONGNOTE[x].pressed = false
        }
        if (playerSong.ap || playerSong.vanish) {
            noEffects = true
        }
        initColumnNotes()
    }

    private fun initCommonInfo() {
        mine = Texture(Gdx.files.absolute("${File(ruta).parent}/Tap Mine 3x2.png"))
        downLeftTap = Texture(Gdx.files.absolute("$ruta/DownLeft Tap Note 3x2.png"))
        upLeftTap = Texture(Gdx.files.absolute("$ruta/UpLeft Tap Note 3x2.png"))
        centerTap = Texture(Gdx.files.absolute("$ruta/Center Tap Note 3x2.png"))
        upRightTap = upLeftTap
        downRightTap = downLeftTap

        downLeftBody = Texture(Gdx.files.absolute("$ruta/DownLeft Hold Body Active 6x1.png"))
        upLeftBody = Texture(Gdx.files.absolute("$ruta/UpLeft Hold Body Active 6x1.png"))
        centerBody = Texture(Gdx.files.absolute("$ruta/Center Hold Body Active 6x1.png"))
        upRightBody = upLeftBody
        downRightBody = downLeftBody

        downLeftBottom = Texture(Gdx.files.absolute("$ruta/DownLeft Hold BottomCap Active 6x1.png"))
        upLeftBottom = Texture(Gdx.files.absolute("$ruta/UpLeft Hold BottomCap Active 6x1.png"))
        centerBottom = Texture(Gdx.files.absolute("$ruta/Center Hold BottomCap Active 6x1.png"))
        upRightBottom = upLeftBody
        downRightBottom = downLeftBottom

        val ldArrowFrame = getArrows3x2(downLeftTap)
        val luArrowFrame = getArrows3x2(upLeftTap)
        val ceArrowFrame = getArrows3x2(centerTap)
        val ruArrowFrame = getArrows3x2(upLeftTap, true)
        val rdArrowFrame = getArrows3x2(downLeftTap, true)

        arrArrows = arrayOf(ldArrowFrame, luArrowFrame, ceArrowFrame, ruArrowFrame, rdArrowFrame)

        val ldBodyArrowFrame = getArrows6x1(downLeftBody)
        val luBodyArrowFrame = getArrows6x1(upLeftBody)
        val ceBodyArrowFrame = getArrows6x1(centerBody)
        val ruBodyArrowFrame = getArrows6x1(upLeftBody, true)
        val rdBodyArrowFrame = getArrows6x1(downLeftBody, true)

        arrArrowsBody = arrayOf(ldBodyArrowFrame, luBodyArrowFrame, ceBodyArrowFrame, ruBodyArrowFrame, rdBodyArrowFrame)

        val ldBottomArrowFrame = getArrows6x1(downLeftBottom)
        val luBottomArrowFrame = getArrows6x1(upLeftBottom)
        val ceBottomArrowFrame = getArrows6x1(centerBottom)
        val ruBottomArrowFrame = getArrows6x1(upLeftBottom, true)
        val rdBottomArrowFrame = getArrows6x1(downLeftBottom, true)

        arrArrowsBottom = arrayOf(ldBottomArrowFrame, luBottomArrowFrame, ceBottomArrowFrame, ruBottomArrowFrame, rdBottomArrowFrame)

        sprFlare = Texture(Gdx.files.absolute("$ruta/Flare 6x1.png"))
        flareArrowFrame = getArrows6x1(sprFlare)

        arrMines = getArrows3x2(mine)
        createWhiteTexture()

        inputProcessor.resetState()
    }

    private data class LongNotePress(
        var pressed: Boolean = false,
        var time: Long = 0,
        var ptn: Int = 0,
        var line: Int = 0,
        var typeNote: TypeNote = TypeNote.NORMAL,
        var dropped: Boolean = false,
        var lastTickIndex: Int = -1,
        var lastTickBeat: Double = Double.NaN,
        var droppedAtTimeMs: Long = 0L,
        var droppedHoldPtn: Int = 0,
        var dropJudged: Boolean = false,
        var releasedEarly: Boolean = false,
        var releasedAtTimeMs: Long = 0L
    )

    data class RenderHold(
        val column: Int,
        val startBeat: Double,
        val endBeat: Double
    )

    private var holdTickBeats = 0.25
    private val HOLD_CATCH_MIN_REMAINING_BEATS = 0.10
    private val HOLD_EARLY_RELEASE_GRACE_BEATS = 0.12
    private val HOLD_PREPRESS_GRACE_BEATS = 0.08

    private fun clearLongNote(col: Int) {
        LONGNOTE[col].pressed = false
        LONGNOTE[col].dropped = false
        LONGNOTE[col].time = 0
        LONGNOTE[col].ptn = 0
        LONGNOTE[col].line = 0
        LONGNOTE[col].lastTickBeat = Double.NaN
        LONGNOTE[col].lastTickIndex = -1
        LONGNOTE[col].typeNote = TypeNote.NORMAL
        LONGNOTE[col].droppedAtTimeMs = 0L
        LONGNOTE[col].droppedHoldPtn = 0
        LONGNOTE[col].dropJudged = false
        LONGNOTE[col].releasedEarly = false
        LONGNOTE[col].releasedAtTimeMs = 0L
    }

    var luaReceptOffsetX = 0f
    private var luaNoteOffsetX = 0f

    val beatWindowBack = 16.0
    val beatWindowForward = 16.0
    var activeExtendedNOtes = false
    fun render(songTimeMs: Long) {
        val timeCom = timeGetTime()
        val iLongTop = LongArray(5) { 0 }
        val nowMs = songTimeMs.toDouble()
        val currentBeat = timeToBeat(nowMs)

        val minBeat = currentBeat - beatWindowBack
        val maxBeat = currentBeat + beatWindowForward

        if (showPadB == 0) {
            inputProcessor.render(batch)
        }

        val currentBpm = bpms.lastOrNull { it.beat <= currentBeat }?.bpm ?: bpms.firstOrNull()?.bpm ?: 120.0
        m_fCurBPM = currentBpm.toFloat()
        val msPorBeat = MINUTE / m_fCurBPM.coerceIn(1f, 999f)
        val msPorFrame = msPorBeat / 5f
        arrowFrame = ((timeCom % msPorBeat.toLong()) / msPorFrame.toLong()).toInt()
        val overlap = 8
        if(!activeExtendedNOtes){
            for (n in notes) {
                if (n.beat < minBeat || n.beat > maxBeat) continue
                when (n.type) {
                    Parser.NoteType.MINE -> {
                        val y = yForBeat(n.beat, currentBeat, nowMs)
                        if (y > -STEPSIZE && y < gdxHeight + STEPSIZE) {
                            drawMines(n.column, y)
                        }
                    }
                    Parser.NoteType.TAP -> {
                        if (hitNotes.contains(n)) continue
                        val y = yForBeat(n.beat, currentBeat, nowMs)
                        if (y > -STEPSIZE && y < gdxHeight + STEPSIZE) {
                            drawNote(n.column, y)
                        }
                    }
                    Parser.NoteType.HOLD -> {
                        if (finishedHolds.contains(n)) continue
                        val endBeat = n.endBeat ?: continue
                        var yHead = yForBeat(n.beat, currentBeat, nowMs)
                        val yTail = yForBeat(endBeat, currentBeat, nowMs)

                        if (yHead < gdxHeight + STEPSIZE && yTail > -STEPSIZE) {
                            val col = n.column
                            val startBeat = n.beat
                            val nowBeat = currentBeat

                            val locked = (LONGNOTE[col].pressed) &&
                                    nowBeat in startBeat..endBeat
                            if (locked) {
                                yHead = medidaFlechas.toInt()
                            }

                            drawLongNote(col, yHead, yTail)
                        }
                    }
                }
            }
        }

        /*
        if(extendedNotes.isNotEmpty()){
            if(currentBeat > extendedNotes.first().beat && currentBeat < extendedNotes.last().beat){
                activeExtendedNOtes = true
                for (n in extendedNotes) {
                    //if (n.beat < minBeat || n.beat > maxBeat) continue
                    when (n.type) {
                        Parser.NoteType.TAP -> {
                            if (hitNotes.contains(n)) continue
                            val y = yForBeat(n.beat, currentBeat, nowMs)
                            if (y > -STEPSIZE && y < gdxHeight + STEPSIZE) {
                                drawNote(n.column, y)
                            }
                        }
                        Parser.NoteType.HOLD -> {
                            if (finishedHolds.contains(n)) continue
                            val endBeat = n.endBeat ?: continue
                            var yHead = yForBeat(n.beat, currentBeat, nowMs)
                            val yTail = yForBeat(endBeat, currentBeat, nowMs)

                            if (yHead < gdxHeight + STEPSIZE && yTail > -STEPSIZE) {
                                drawLongNote(n.column, yHead, yTail)
                            }
                        }
                        else -> {}
                    }
                }
            }else{
                activeExtendedNOtes = false
            }
        }
        */

        if (m_fGauge > 1.0f) m_fGauge = 1.0f
        if (m_fGauge < -0.5f) m_fGauge = 0.0f

        var gaugeFind = (MINUTE / m_fCurBPM.coerceIn(1f, 999f))
        val phase = (nowMs % gaugeFind).toFloat()
        gaugeFind = (phase / gaugeFind) * 0.1f

        if (m_fGauge >= 1.0f) m_fGauge = 1.0f
        if (m_fGauge < -0.5f) m_fGauge = 0.0f

        val gaugeVisual = if (m_fGauge == 1.0f) {
            var v = gaugeFind * 3.0f + 0.7f
            2.0f + v
        } else {
            var v = gaugeFind + m_fGauge
            if (v > 1.0f) v = 1.0f
            v
        }

        drawGauge(gaugeVisual)

        for (iStepNo in 0 until m_iStepWidth) {
            if (flare[iStepNo].startTime == 0L) continue

            iLongTop[iStepNo] = ((timeCom - flare[iStepNo].startTime) shr 6)
            if (iLongTop[iStepNo] >= 6) {
                flare[iStepNo].startTime = 0
                continue
            }

            drawFlare(iStepNo, iLongTop[iStepNo].toInt())
        }

        drawMineFlash(timeCom)

        if (m_judge.startTime == 0L) return
        if (m_judge.startTime + 2500 < timeCom) {
            m_judge.startTime = 0
            return
        }
        drawJudge(timeCom - m_judge.startTime)
    }

    fun updateStepData(songTimeMs: Long) {
        val keyBoard = inputProcessor.getKeyBoard
        val key = IntArray(m_iStepWidth)
        for (x in 0 until m_iStepWidth) {
            key[x] = keyBoard[x]
        }

        for (col in 0 until m_iStepWidth) {
            when (key[col]) {
                KEY_DOWN -> {
                    showExpand(col)
                    processTapAndHeadOnColumn(col, songTimeMs)
                    if (!LONGNOTE[col].pressed) {
                        tryAutoStartHoldOnPress(col, songTimeMs)
                    }
                    if (LONGNOTE[col].pressed) {
                        processLongNoteTick(col, songTimeMs)
                    }
                }

                KEY_PRESS -> {
                    showExpand(col)

                    // Si estamos sosteniendo una hold y ya pasamos su tail, completar y encadenar a la siguiente sin soltar.
                    if (LONGNOTE[col].pressed) {
                        val endBeat = LONGNOTE[col].line / 1000.0
                        val nowBeat = timeToBeat(songTimeMs.toDouble())
                        if (nowBeat >= endBeat) {
                            val list = columnNotes[col]
                            val holdNote = list.firstOrNull { isHold(it) && it.beat * 1000.0 == LONGNOTE[col].ptn.toDouble() }
                            if (holdNote != null) finishedHolds.add(holdNote)
                            clearLongNote(col)
                        }
                    }

                    if (!LONGNOTE[col].pressed) {
                        tryAutoStartHoldOnPress(col, songTimeMs)
                    }
                    if (LONGNOTE[col].pressed) {
                        processLongNoteTick(col, songTimeMs)
                    }
                }

                KEY_UP -> {
                    if (LONGNOTE[col].pressed) {
                        endLongNote(col, songTimeMs)
                    }
                }
            }
        }

        updateAutoMisses(songTimeMs)
        inputProcessor.update()

        if (m_fGauge > 1.0f) {
            m_fGauge = 1.0f
        } else if (m_fGauge < -0.5f) {
            if (!isOnline && breakSong) {
                a.breakDance()
            }
        }
    }

    private fun isHold(note: Parser.Note) =
        note.type == Parser.NoteType.HOLD && note.endBeat != null

    private fun tryAutoStartHoldOnPress(col: Int, timeMs: Long) {
        if (LONGNOTE[col].pressed) return

        val list = columnNotes[col]
        if (list.isEmpty()) return

        val nowBeat = timeToBeat(timeMs.toDouble())

        // Si venimos de un drop, necesitamos poder re-catch sin depender de columnIndex (puede haber avanzado).
        // En dropped, buscamos explícitamente el HOLD que estamos siguiendo.
        val startIdx = if (LONGNOTE[col].dropped && LONGNOTE[col].droppedHoldPtn != 0) {
            list.indexOfFirst { isHold(it) && (it.beat * 1000).toInt() == LONGNOTE[col].droppedHoldPtn }.let { if (it >= 0) it else 0 }
        } else {
            columnIndex[col]
        }
        if (startIdx >= list.size) return

        // Reglas estilo PIU simplificadas:
        // - En PRESS (y también DOWN cuando se llame aquí), permite enganchar:
        //   a) HEAD por prepress alrededor del head => PERFECT forzado
        //   b) BODY catch / re-catch dentro del cuerpo => PERFECT forzado
        // - Si venimos de soltar temprano, permitir re-catch del mismo hold.

        val scanLimit = min(list.size, startIdx + 10)
        for (i in startIdx until scanLimit) {
            val n = list[i]
            if (n.isFake) continue
            if (!isHold(n)) continue
            val endBeat = n.endBeat ?: continue

            // Si estamos en dropped, solo re-catch del mismo hold.
            if (LONGNOTE[col].dropped && LONGNOTE[col].droppedHoldPtn != 0 && LONGNOTE[col].droppedHoldPtn != (n.beat * 1000).toInt()) {
                continue
            }

            // (A) HEAD prepress alrededor del head
            if (!LONGNOTE[col].dropped && nowBeat >= n.beat - HOLD_PREPRESS_GRACE_BEATS && nowBeat <= n.beat + HOLD_PREPRESS_GRACE_BEATS) {
                applyJudge(col, JUDGE_PERFECT, timeMs, isFromInput = true)
                startLongNote(col, n, timeMs)
                LONGNOTE[col].lastTickBeat = nowBeat
                columnIndex[col] = i + 1
                return
            }

            // (B) BODY catch / re-catch dentro del cuerpo
            if (nowBeat >= n.beat && nowBeat <= endBeat) {
                val remaining = endBeat - nowBeat
                if (remaining >= HOLD_CATCH_MIN_REMAINING_BEATS) {
                    applyJudge(col, JUDGE_PERFECT, timeMs, isFromInput = true)
                    startLongNote(col, n, timeMs)
                    LONGNOTE[col].lastTickBeat = nowBeat
                    if (columnIndex[col] <= i) {
                        columnIndex[col] = i + 1
                    }
                    return
                }
            }

            if (n.beat - nowBeat > 2.0) break
        }
    }

    private fun processTapAndHeadOnColumn(col: Int, timeMs: Long) {
        val list = columnNotes[col]
        val startIdx = columnIndex[col]
        if (startIdx >= list.size) return

        val scanLimit = min(list.size, startIdx + 6)
        var bestIdx = -1
        var bestJudge = -1
        var bestAbsDelta = Long.MAX_VALUE

        var i = startIdx
        while (i < scanLimit) {
            val n = list[i]

            if (n.isFake) {
                i++
                continue
            }

            if (n.type == Parser.NoteType.MINE) {
                val deltaMsMine = getDeltaMsForNote(n.beat, timeMs)
                if (kotlin.math.abs(deltaMsMine) <= ZONE_BAD) {
                    m_fGauge -= MINE_PENALTY
                    if (m_fGauge < 0f) m_fGauge = 0f
                    applyJudge(col, JUDGE_MISS, timeMs, isFromInput = false)
                    onMineHit(timeGetTime())
                    columnIndex[col] = i + 1
                    return
                }
                i++
                continue
            }

            // Para HEAD_HOLD en KEY_DOWN: igual que TAP (delta real)
            val deltaMs = getDeltaMsForNote(n.beat, timeMs)
            if (deltaMs < -ZONE_BAD) break

            val judge = getJudgeFromDelta(deltaMs)
            if (judge != -1) {
                val absDelta = kotlin.math.abs(deltaMs)
                if (absDelta < bestAbsDelta) {
                    bestAbsDelta = absDelta
                    bestIdx = i
                    bestJudge = judge
                }
            }
            i++
        }

        if (bestIdx == -1) return

        val n = list[bestIdx]
        if (!isHold(n)) {
            applyJudge(col, bestJudge, timeMs, isFromInput = true)
            hitNotes.add(n)
            columnIndex[col] = bestIdx + 1
            return
        }

        // HOLD HEAD en DOWN: como TAP
        applyJudge(col, bestJudge, timeMs, isFromInput = true)
        startLongNote(col, n, timeMs)
        LONGNOTE[col].lastTickBeat = timeToBeat(timeMs.toDouble())
        columnIndex[col] = bestIdx + 1
    }

    private fun startLongNote(col: Int, note: Parser.Note, timeMs: Long) {
        LONGNOTE[col].pressed = true
        LONGNOTE[col].dropped = false
        LONGNOTE[col].dropJudged = false
        LONGNOTE[col].droppedAtTimeMs = 0L
        LONGNOTE[col].droppedHoldPtn = 0
        LONGNOTE[col].releasedEarly = false
        LONGNOTE[col].releasedAtTimeMs = 0L
        LONGNOTE[col].time = timeMs
        LONGNOTE[col].ptn = (note.beat * 1000).toInt()
        LONGNOTE[col].line = ((note.endBeat ?: note.beat) * 1000).toInt()
        LONGNOTE[col].typeNote = TypeNote.NORMAL
        LONGNOTE[col].lastTickIndex = -1
        LONGNOTE[col].lastTickBeat = Double.NaN
    }

    private fun processLongNoteTick(col: Int, timeMs: Long) {
        val startBeat = LONGNOTE[col].ptn / 1000.0
        val endBeat = LONGNOTE[col].line / 1000.0

        val nowBeat = timeToBeat(timeMs.toDouble())

        if (nowBeat > endBeat) {
            val list = columnNotes[col]
            val holdNote = list.firstOrNull { isHold(it) && it.beat * 1000.0 == LONGNOTE[col].ptn.toDouble() }
            if (holdNote != null) finishedHolds.add(holdNote)
            clearLongNote(col)
            return
        }

        if (nowBeat < startBeat || nowBeat > endBeat) return

        updateHoldTickBeats(nowBeat)

        val tickBeats = holdTickBeats.coerceAtLeast(1e-6)
        val localBeat = (nowBeat - startBeat).coerceAtLeast(0.0)
        val currentTickIndex = kotlin.math.floor(localBeat / tickBeats).toInt()

        if (LONGNOTE[col].lastTickIndex < 0) {
            LONGNOTE[col].lastTickIndex = currentTickIndex
            LONGNOTE[col].lastTickBeat = nowBeat
            LONGNOTE[col].time = timeMs
            return
        }

        if (currentTickIndex > LONGNOTE[col].lastTickIndex) {
            // Regla PIU: tick siempre PERFECT y suma combo
            applyJudge(col, JUDGE_PERFECT, timeMs, isFromInput = true)
            LONGNOTE[col].lastTickIndex += 1
            LONGNOTE[col].lastTickBeat = nowBeat
            LONGNOTE[col].time = timeMs
        }
    }

    private fun endLongNote(col: Int, timeMs: Long) {
        val endBeat = LONGNOTE[col].line / 1000.0
        val startBeat = LONGNOTE[col].ptn / 1000.0
        val nowBeat = timeToBeat(timeMs.toDouble())

        val list = columnNotes[col]
        val holdNote = list.firstOrNull { isHold(it) && it.beat * 1000.0 == LONGNOTE[col].ptn.toDouble() }

        val totalBeats = (endBeat - startBeat).coerceAtLeast(1e-6)
        val remainingBeats = endBeat - nowBeat
        val tailTolBeats = (totalBeats * 0.10).coerceAtLeast(HOLD_EARLY_RELEASE_GRACE_BEATS)

        if (remainingBeats <= tailTolBeats) {
            val judge = if (remainingBeats <= HOLD_EARLY_RELEASE_GRACE_BEATS) JUDGE_PERFECT else JUDGE_BAD
            applyJudge(col, judge, timeMs, isFromInput = true)
            if (holdNote != null) finishedHolds.add(holdNote)
            clearLongNote(col)
            return
        }

        applyJudge(col, JUDGE_MISS, timeMs, isFromInput = true)

        // Mantener ptn/line para que el render y limpieza por tail funcionen, pero marcar dropped.
        LONGNOTE[col].pressed = false
        LONGNOTE[col].dropped = true
        LONGNOTE[col].dropJudged = true
        LONGNOTE[col].releasedEarly = true
        LONGNOTE[col].releasedAtTimeMs = timeMs
        LONGNOTE[col].droppedAtTimeMs = timeMs
        LONGNOTE[col].droppedHoldPtn = LONGNOTE[col].ptn
    }

    private fun getTickcountAt(beat: Double): Int {
        val tc = tickcounts.lastOrNull { it.beat <= beat } ?: return 2
        return tc.tickcount
    }

    private fun updateHoldTickBeats(nowBeat: Double) {
        val tickcount = getTickcountAt(nowBeat).coerceAtLeast(1)
        holdTickBeats = 1.0 / tickcount
    }

    private fun updateAutoMisses(timeMs: Long) {
        for (col in 0 until m_iStepWidth) {
            val list = columnNotes[col]
            var idx = columnIndex[col]

            val nowBeat = timeToBeat(timeMs.toDouble())

            while (idx < list.size) {
                val n = list[idx]
                if (n.isFake) {
                    idx++
                    columnIndex[col] = idx
                    continue
                }



                val deltaMs = getDeltaMsForNote(n.beat, timeMs)

                if (n.type == Parser.NoteType.MINE) {
                    if (deltaMs > ZONE_BAD) {
                        idx++
                        columnIndex[col] = idx
                        continue
                    }
                    break
                }

                // Si hay un hold dropeado en este column, no consumas notas por el lock; deja que el jugador recachee.
                if (LONGNOTE[col].dropped) {
                    break
                }

                // HOLD pending: si está activo (pressed) y estamos dentro del cuerpo, NO missear el head.
                if (isHold(n)) {
                    val endBeat = n.endBeat ?: n.beat
                    val insideBody = nowBeat in n.beat..endBeat
                    val lockedByThisHold = (LONGNOTE[col].pressed) && (LONGNOTE[col].ptn == (n.beat * 1000).toInt())

                    if (insideBody || lockedByThisHold) {
                        if (deltaMs > ZONE_BAD) {
                            idx++
                            columnIndex[col] = idx
                        }
                        break
                    }
                }

                if (deltaMs > ZONE_BAD) {
                    applyJudge(col, JUDGE_MISS, timeMs, isFromInput = false)
                    idx++
                    columnIndex[col] = idx
                    continue
                }
                break
            }

            // Cuando ya pasó el tail, limpia el lock (aunque haya sido dropeado)
            if (LONGNOTE[col].dropped) {
                val endBeat = LONGNOTE[col].line / 1000.0
                val nowBeat2 = timeToBeat(timeMs.toDouble())
                if (nowBeat2 > endBeat) {
                    val holdNote = list.firstOrNull { isHold(it) && it.beat * 1000.0 == LONGNOTE[col].ptn.toDouble() }
                    if (holdNote != null) finishedHolds.add(holdNote)
                    clearLongNote(col)
                }
            }
        }
    }

    private fun getDeltaMsForNote(noteBeat: Double, timeMs: Long): Long {
        val noteTimeMs = beatToTime(noteBeat)
        return (timeMs - noteTimeMs).toLong()
    }

    private fun getJudgeFromDelta(deltaMs: Long): Int {
        val a = kotlin.math.abs(deltaMs)
        return when {
            a <= ZONE_PERFECT -> JUDGE_PERFECT
            a <= ZONE_GREAT -> JUDGE_GREAT
            a <= ZONE_GOOD -> JUDGE_GOOD
            a <= ZONE_BAD -> JUDGE_BAD
            else -> -1
        }
    }

    private fun applyJudge(col: Int, judge: Int, timeMs: Long, isFromInput: Boolean) {
        when (judge) {
            JUDGE_PERFECT -> {
                resultSong.perfect++
                curCombo++
                curComboMiss = 0
            }

            JUDGE_GREAT -> {
                resultSong.great++
                curCombo++
                curComboMiss = 0
            }

            JUDGE_GOOD -> {
                resultSong.good++
                curComboMiss = 0
            }

            JUDGE_BAD -> {
                resultSong.bad++
                curCombo = 0
                curComboMiss = 0
            }

            JUDGE_MISS -> {
                resultSong.miss++
                curCombo = 0
                curComboMiss++
            }
        }

        if (resultSong.maxCombo < curCombo) {
            resultSong.maxCombo = curCombo
        }

        when (judge) {
            JUDGE_PERFECT, JUDGE_GREAT, JUDGE_GOOD -> {
                m_fGauge += GaugeInc[judge]
            }

            JUDGE_BAD, JUDGE_MISS -> {
                m_fGauge += GaugeInc[judge]
                if (m_fGauge < 0f) m_fGauge = 0f
            }
        }

        val now = timeGetTime()
        m_judge.judge = judge
        m_judge.startTime = now

        // Flare: siempre que no sea MISS. (incluye ticks)
        if (isFromInput && judge != JUDGE_MISS) {
            flare[col].startTime = now
            showExpand(col)
        }
    }

    private fun yForBeat(beat: Double, currentBeat: Double, songTimeMs: Double): Int {
        val baseOffset = timingData.getYOffsetForBeat(
            noteBeat = beat,
            songVisibleBeat = currentBeat,
            songVisibleTimeMs = songTimeMs,
            stepSize = medidaFlechas
        )

        val totalOffset = baseOffset * baseSpeed
        val y = medidaFlechas + totalOffset
        return y.toInt()
    }

    private fun timeToBeat(timeMs: Double): Double = timingData.timeToBeat(timeMs)

    private fun beatToTime(beat: Double): Double = timingData.beatToTime(beat)

    private val initArrow = (gdxHeight * 0.55)
    private var rangeAlpha = (gdxHeight * 0.1)
    private val segmentHeight = gdxHeight * 0.001f
    private val heightBodyHead = medidaFlechas / 2
    private val amplitude = medidaFlechas / 3f
    private val frequency = 0.01f
    private val fadeDistance = medidaFlechas
    private var offsetX = 0f

    private fun drawMines(x: Int, y: Int) {
        val baseX = medidaFlechas * (x + 1)
        if (playerSong.snake) {
            offsetX = (sin(y * frequency) * amplitude)
            if (y <= medidaFlechas + fadeDistance) {
                val factor = (y - medidaFlechas) / fadeDistance
                offsetX *= factor.coerceIn(0f, 1f)
            }
        }

        val left = baseX + offsetX + luaNoteOffsetX

        if (!noEffects) {
            if (isMidLine) {
                if (y < initArrow) {
                    batch.setColor(1f, 1f, 1f, getAlpha(y.toFloat(), initArrow))
                    batch.draw(arrMines[arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                    batch.setColor(1f, 1f, 1f, 1f)
                }
            } else {
                batch.draw(arrMines[arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
            }
        } else {
            if (playerSong.vanish) {
                if (y > MEASUREVANISH) {
                    batch.setColor(1f, 1f, 1f, getVanishAlpha(y.toFloat()))
                    batch.draw(arrMines[arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                    batch.setColor(1f, 1f, 1f, 1f)
                }
            }
            if (playerSong.ap) {
                if (y < MEASURE) {
                    batch.setColor(1f, 1f, 1f, getAlpha(y.toFloat(), MEASURE))
                    batch.draw(arrMines[arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                    batch.setColor(1f, 1f, 1f, 1f)
                }
            }
        }
    }

    private fun drawNote(x: Int, y: Int) {
        val baseX = medidaFlechas * (x + 1)
        if (playerSong.snake) {
            offsetX = (sin(y * frequency) * amplitude)
            if (y <= medidaFlechas + fadeDistance) {
                val factor = (y - medidaFlechas) / fadeDistance
                offsetX *= factor.coerceIn(0f, 1f)
            }
        }

        val left = baseX + offsetX + luaNoteOffsetX

        if (!noEffects) {
            if (isMidLine) {
                if (y < initArrow) {
                    batch.setColor(1f, 1f, 1f, getAlpha(y.toFloat(), initArrow))
                    batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                    batch.setColor(1f, 1f, 1f, 1f)
                }
            } else {
                batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
            }
        } else {
            if (playerSong.vanish) {
                if (y > MEASUREVANISH) {
                    batch.setColor(1f, 1f, 1f, getVanishAlpha(y.toFloat()))
                    batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                    batch.setColor(1f, 1f, 1f, 1f)
                }
            }
            if (playerSong.ap) {
                if (y < MEASURE) {
                    batch.setColor(1f, 1f, 1f, getAlpha(y.toFloat(), MEASURE))
                    batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                    batch.setColor(1f, 1f, 1f, 1f)
                }
            }
        }
    }

    private fun drawLongNote(x: Int, y: Int, y2: Int) {
        val baseX = medidaFlechas * (x + 1)

        if (playerSong.snake) {
            offsetX = (sin(y * frequency) * amplitude)
            if (y <= medidaFlechas + fadeDistance) {
                val factor = (y - medidaFlechas) / fadeDistance
                offsetX *= factor.coerceIn(0f, 1f)
            }
        }
        val left = baseX + offsetX + luaNoteOffsetX

        val posY = y.toFloat() + medidaFlechas
        var heightBody = (y2 - y).toFloat() - medidaFlechas

        if (!noEffects) {
            if (isMidLine) {
                if (posY < initArrow) {
                    if (posY + heightBody > initArrow) {
                        heightBody = (initArrow - posY).toFloat()
                    }

                    var currentY = posY
                    while (currentY < posY + heightBody) {
                        val alphaSegment = getAlpha(currentY, initArrow)
                        val drawHeight = minOf(segmentHeight, posY + heightBody - currentY)
                        batch.setColor(1f, 1f, 1f, alphaSegment)
                        batch.draw(arrArrowsBody[x][arrowFrame], left, currentY, medidaFlechas, drawHeight)
                        currentY += drawHeight
                    }

                    if (y2 < initArrow) {
                        batch.setColor(1f, 1f, 1f, getAlpha(y2.toFloat(), initArrow))
                        batch.draw(arrArrowsBottom[x][arrowFrame], left, y2.toFloat(), medidaFlechas, medidaFlechas)
                    }

                    if (y > 0) {
                        batch.setColor(1f, 1f, 1f, getAlpha(y.toFloat(), initArrow))
                        batch.draw(
                            arrArrowsBody[x][arrowFrame],
                            left,
                            y + heightBodyHead,
                            medidaFlechas,
                            heightBodyHead
                        )
                        batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                    }

                    batch.setColor(1f, 1f, 1f, 1f)
                }
            } else {
                batch.draw(arrArrowsBody[x][arrowFrame], left, posY, medidaFlechas, heightBody)
                batch.draw(arrArrowsBottom[x][arrowFrame], left, y2.toFloat(), medidaFlechas, medidaFlechas)

                if (y > 0) {
                    batch.draw(
                        arrArrowsBody[x][arrowFrame],
                        left,
                        y + heightBodyHead,
                        medidaFlechas,
                        heightBodyHead
                    )
                    batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                }
            }
        } else {
            if (playerSong.vanish) {
                var currentY = posY
                while (currentY < posY + heightBody) {
                    val drawHeight = minOf(segmentHeight, posY + heightBody - currentY)

                    val alphaSegment = when {
                        currentY > MEASUREVANISH + (medidaFlechas * 2) -> 1f
                        currentY >= MEASUREVANISH + (medidaFlechas * 2) - rangeAlpha -> {
                            ((currentY - (MEASUREVANISH + (medidaFlechas * 2) - rangeAlpha)) / rangeAlpha)
                                .toFloat()
                                .coerceIn(0f, 1f)
                        }
                        else -> 0f
                    }

                    if (alphaSegment > 0f) {
                        batch.setColor(1f, 1f, 1f, alphaSegment)
                        batch.draw(arrArrowsBody[x][arrowFrame], left, currentY, medidaFlechas, drawHeight)
                    }

                    currentY += drawHeight
                }
                batch.setColor(1f, 1f, 1f, 1f)

                if (posY + heightBody > MEASUREVANISH) {
                    batch.setColor(1f, 1f, 1f, getVanishAlpha(y2.toFloat()))
                    batch.draw(arrArrowsBottom[x][arrowFrame], left, y2.toFloat(), medidaFlechas, medidaFlechas)
                }
                if (posY > MEASUREVANISH) {
                    if (y > 0) {
                        batch.setColor(1f, 1f, 1f, getVanishAlpha(y.toFloat()))
                        batch.draw(
                            arrArrowsBody[x][arrowFrame],
                            left,
                            y + heightBodyHead,
                            medidaFlechas,
                            heightBodyHead
                        )
                        batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                    }

                    batch.setColor(1f, 1f, 1f, 1f)
                }
            }
            if (playerSong.ap) {
                if (posY < MEASURE) {
                    if (posY + heightBody > MEASURE) {
                        heightBody = (MEASURE - posY).toFloat()
                    }
                    var currentY = posY
                    while (currentY < posY + heightBody) {
                        val alphaSegment = getAlpha(currentY, MEASURE)
                        val drawHeight = minOf(segmentHeight, posY + heightBody - currentY)
                        batch.setColor(1f, 1f, 1f, alphaSegment)
                        batch.draw(arrArrowsBody[x][arrowFrame], left, currentY, medidaFlechas, drawHeight)
                        currentY += drawHeight
                    }

                    if (y2 < MEASURE) {
                        batch.setColor(1f, 1f, 1f, getAlpha(y2.toFloat(), MEASURE))
                        batch.draw(arrArrowsBottom[x][arrowFrame], left, y2.toFloat(), medidaFlechas, medidaFlechas)
                    }

                    if (y > 0) {
                        batch.setColor(1f, 1f, 1f, getAlpha(y.toFloat(), MEASURE))
                        batch.draw(
                            arrArrowsBody[x][arrowFrame],
                            left,
                            y + heightBodyHead,
                            medidaFlechas,
                            heightBodyHead
                        )
                        batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                    }
                    batch.setColor(1f, 1f, 1f, 1f)
                }
            }
        }
    }

    private fun getAlpha(y: Float, init: Double): Float {
        return ((init - y) / rangeAlpha).toFloat().coerceIn(0f, 1f)
    }

    private fun getVanishAlpha(y: Float): Float {
        return ((y - MEASUREVANISH) / rangeAlpha).toFloat().coerceIn(0f, 1f)
    }

    private fun drawFlare(x: Int, frame: Int) {
        var left = 0f
        when (x) {
            0 -> left = medidaFlechas * (x + 1) - xFlare1
            1 -> left = medidaFlechas * (x + 1) - xFlare2
            2 -> left = medidaFlechas * (x + 1) - xFlare3
            3 -> left = medidaFlechas * (x + 1) - xFlare4
            4 -> left = medidaFlechas * (x + 1) - xFlare5
        }
        val flareSprite = flareSprites[frame]
        flareSprite.setBounds(left, yFlare, widthFlare, widthFlare)
        aBatch = batch.blendSrcFunc
        bBatch = batch.blendDstFunc
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        flareSprite.draw(batch)
        batch.setBlendFunction(aBatch, bBatch)

        elapsedTimeToExpands = (timeGetTime() - currentTimeToExpands) % animationDuration

        val (alpha, zoom) = calculateAlphaAndZoom(elapsedTimeToExpands)

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        batch.color.a = alpha

        batch.draw(
            arrArrows[x][arrowFrame],
            (medidaFlechas * (x + 1)) - ((medidaFlechas * zoom) - medidaFlechas) / 2,
            STEPSIZE.toFloat() - ((medidaFlechas * zoom) - medidaFlechas) / 2,
            medidaFlechas * zoom,
            medidaFlechas * zoom
        )

        batch.color.a = 1f
        batch.setBlendFunction(aBatch, bBatch)
    }

    private val flareSprites = Array(flareArrowFrame.size) { i ->
        Sprite(flareArrowFrame[i])
    }

    private fun calculateAlphaAndZoom(elapsedTime: Long): Pair<Float, Float> {
        val phaseDuration = 360 / 3
        return when {
            elapsedTime < phaseDuration -> {
                val progress = elapsedTime / phaseDuration.toFloat()
                1.0f - 0.4f * progress to 1.0f - 0.2f * progress
            }
            elapsedTime < 2 * phaseDuration -> {
                val progress = (elapsedTime - phaseDuration) / phaseDuration.toFloat()
                0.6f - 0.3f * progress to 0.8f + 0.2f * progress
            }
            else -> {
                val progress = (elapsedTime - 2 * phaseDuration) / phaseDuration.toFloat()
                0.3f - 0.3f * progress to 1.0f - 0.2f * progress
            }
        }
    }

    private fun showExpand(position: Int) {
        batch.setColor(1f, 1f, 1f, 0.7f)
        when (position) {
            0 -> {
                batch.draw(recept0Frames[2], medidaFlechas - posX, topPos, sizeScale, sizeScale)
                if (showPadB == 2) {
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(
                        arrPadsC[position][arrowFrame],
                        padPositionsC[position].x,
                        padPositionsC[position].y,
                        padPositionsC[position].size,
                        padPositionsC[position].size
                    )
                }
                if (showPadB == 3) {
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(
                        arrayPad4[position],
                        padPositions[0][0],
                        padPositions[0][1],
                        widthBtns,
                        heightBtns
                    )
                }
            }
            1 -> {
                batch.draw(recept1Frames[2], (medidaFlechas * 2) - posX, topPos, sizeScale, sizeScale)
                if (showPadB == 2) {
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(
                        arrPadsC[position][arrowFrame],
                        padPositionsC[position].x,
                        padPositionsC[position].y,
                        padPositionsC[position].size,
                        padPositionsC[position].size
                    )
                }
                if (showPadB == 3) {
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(
                        arrayPad4[position],
                        padPositions[1][0],
                        padPositions[1][1],
                        widthBtns,
                        heightBtns
                    )
                }
            }
            2 -> {
                batch.draw(recept2Frames[2], (medidaFlechas * 3) - posX, topPos, sizeScale, sizeScale)
                if (showPadB == 2) {
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(
                        arrPadsC[position][arrowFrame],
                        padPositionsC[position].x,
                        padPositionsC[position].y,
                        padPositionsC[position].size,
                        padPositionsC[position].size
                    )
                }
                if (showPadB == 3) {
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(
                        arrayPad4[position],
                        padPositions[2][0],
                        padPositions[2][1],
                        widthBtns,
                        heightBtns
                    )
                }
            }
            3 -> {
                batch.draw(recept3Frames[2], (medidaFlechas * 4) - posX, topPos, sizeScale, sizeScale)
                if (showPadB == 2) {
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(
                        arrPadsC[position][arrowFrame],
                        padPositionsC[position].x,
                        padPositionsC[position].y,
                        padPositionsC[position].size,
                        padPositionsC[position].size
                    )
                }
                if (showPadB == 3) {
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(
                        arrayPad4[position],
                        padPositions[3][0],
                        padPositions[3][1],
                        widthBtns,
                        heightBtns
                    )
                }
            }
            4 -> {
                batch.draw(recept4Frames[2], (medidaFlechas * 5) - posX, topPos, sizeScale, sizeScale)
                if (showPadB == 2) {
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(
                        arrPadsC[position][arrowFrame],
                        padPositionsC[position].x,
                        padPositionsC[position].y,
                        padPositionsC[position].size,
                        padPositionsC[position].size
                    )
                }
                if (showPadB == 3) {
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(
                        arrayPad4[position],
                        padPositions[4][0],
                        padPositions[4][1],
                        widthBtns,
                        heightBtns
                    )
                }
            }
        }
        batch.setColor(1f, 1f, 1f, 1f)
    }

    private fun drawJudge(time: Long) {
        val ftX: Float
        val ftY: Float
        val alpha: Float

        if (time < 100) {
            ftX = (time / 300f) + 1.0f
            ftY = ftX
            alpha = 1f
        } else if (time > 1200) {
            val progress = ((time - 1200) / 300f).coerceIn(0f, 1f)
            ftX = 1.0f + 0.6f * progress
            ftY = 1.0f - 0.8f * progress
            alpha = (1.0f - progress).coerceIn(0f, 1f)
        } else {
            ftX = 1.0f
            ftY = 1.0f
            alpha = 1.0f
        }

        val judgeSprite = Sprite(imgsJudge[m_judge.judge])
        val judgeW = widthJudges * ftX
        val judgeH = heightJudges * ftY

        judgeSprite.setSize(judgeW, judgeH)
        judgeSprite.setOriginCenter()
        judgeSprite.setCenter(x + widthJudges / 2f, y + heightJudges / 2f)
        judgeSprite.setColor(1f, 1f, 1f, alpha)
        judgeSprite.draw(batch)

        val comboWidth = (widthJudges * 0.5f) * ftX
        val comboHeight = (heightJudges * 0.5f) * ftY
        val comboY = y + heightJudges + 5f
        val comboX = widthJudges - comboWidth / 2

        val digitW = digitWidth * ftX
        val digitH = digitHeight * ftY

        if (curCombo >= 4 || curComboMiss >= 4) {
            val isMiss = curComboMiss >= 4
            val comboSprite = if (isMiss) Sprite(imgsTypeCombo[1]) else Sprite(imgsTypeCombo[0])
            val count = if (isMiss) curComboMiss else curCombo
            val numberList = if (isMiss) listNumbersMiss else listNumbers

            comboSprite.setSize(comboWidth, comboHeight)
            comboSprite.setOriginCenter()
            comboSprite.setCenter(comboX + comboWidth / 2f, comboY + comboHeight / 2f)
            comboSprite.setColor(1f, 1f, 1f, alpha)
            comboSprite.draw(batch)

            val numStr = if (count < 100) count.toString().padStart(3, '0') else count.toString()

            val totalWidth = numStr.length * digitW
            var startX = widthJudges - (totalWidth / 2)
            val digitY = comboY + comboHeight - 10f

            val digitSprite = Sprite()
            for (char in numStr) {
                val digit = char.digitToInt()
                digitSprite.setRegion(numberList[digit])
                digitSprite.setBounds(startX, digitY, digitW, digitH)
                digitSprite.setColor(1f, 1f, 1f, alpha)
                digitSprite.draw(batch)
                startX += digitW
            }
        }
    }

    private fun drawGauge(gauge: Float) {
        val previousSrcFunc = batch.blendSrcFunc
        val previousDstFunc = batch.blendDstFunc

        val barToDraw = if (gauge <= 0.2f) barRed else barBlack
        barToDraw.setSize(maxWidth, maxlHeight)
        barToDraw.setPosition(medidaFlechas, 0f)
        barToDraw.draw(batch)

        val visibleWidth = maxWidth * gauge
        val regionWidth = (barColors.texture.width * gauge).toInt()

        if (regionWidth > 0.1 && visibleWidth > 0.1f) {
            barColors.setRegion(0, 0, regionWidth, barColors.texture.height)
            barColors.setSize(visibleWidth, maxlHeight)
            barColors.setPosition(medidaFlechas, 0f)
            barColors.draw(batch)
        }

        if (gauge >= 1.0f) {
            val currentTime = (timeGetTime() / 100L) % 2 == 0L

            if (currentTime) {
                val time = (timeGetTime() % 200L) / 200f
                val shine = 1f + 0.5f * Math.sin(time * Math.PI).toFloat()
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)

                barColors.setColor(shine, shine, shine, 1f)
                barColors.draw(batch)
                batch.setBlendFunction(previousSrcFunc, previousDstFunc)
            } else {
                barColors.setColor(1f, 1f, 1f, 1f)
                barColors.draw(batch)
            }
        } else {
            barColors.setColor(1f, 1f, 1f, 1f)
        }
        barFrame.setSize(maxWidth, maxlHeight)
        barFrame.setPosition(medidaFlechas, 0f)
        barFrame.draw(batch)

        if (gauge <= 0.99f && gauge > 0f) {
            val tipX = visibleWidth + medidaFlechas
            barTip.setSize(tipWidth, tipHeight)
            barTip.setPosition(tipX, tipY)
            barTip.draw(batch)
        }
    }

    fun onMineHit(timeCom: Long) {
        mineFlashStartTime = timeCom
        soundPoolSelectSongKsf.play(sound_mine, 1f, 1f, 1, 0, 1f)
    }

    private fun getArrows3x2(arrow: Texture, isMirror: Boolean = false): Array<TextureRegion> {
        val tmp = TextureRegion.split(arrow, arrow.width / 3, arrow.height / 2)

        return if (!isMirror) {
            val frames = arrayOf(
                tmp[0][0], tmp[0][1], tmp[0][2],
                tmp[1][0], tmp[1][1], tmp[1][2]
            )
            frames.forEach { it.flip(false, true) }
            frames
        } else {
            val frames = arrayOf(
                tmp[0][0], tmp[0][1], tmp[0][2],
                tmp[1][0], tmp[1][1], tmp[1][2]
            )
            frames.forEach { it.flip(true, true) }
            frames
        }
    }

    private fun getArrows6x1(arrow: Texture, isMirror: Boolean = false): Array<TextureRegion> {
        arrow.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        val tmp = TextureRegion.split(arrow, arrow.width / 6, arrow.height)

        return if (!isMirror) {
            val frames = arrayOf(
                tmp[0][0], tmp[0][1], tmp[0][2],
                tmp[0][3], tmp[0][4], tmp[0][5]
            )
            frames.forEach { it.flip(false, true) }
            frames
        } else {
            val frames = arrayOf(
                tmp[0][0], tmp[0][1], tmp[0][2],
                tmp[0][3], tmp[0][4], tmp[0][5]
            )
            frames.forEach { it.flip(true, true) }
            frames
        }
    }

    fun disposePlayer() {
        downLeftTap.dispose()
        upLeftTap.dispose()
        centerTap.dispose()
        downLeftBody.dispose()
        upLeftBody.dispose()
        centerBody.dispose()
        downLeftBottom.dispose()
        upLeftBottom.dispose()
        centerBottom.dispose()
        sprFlare.dispose()

        arrMines.forEach { it.texture.dispose() }

        arrArrows.forEach { frameArray ->
            frameArray.forEach { it.texture.dispose() }
        }

        arrArrowsBody.forEach { frameArray ->
            frameArray.forEach { it.texture.dispose() }
        }

        arrArrowsBottom.forEach { frameArray ->
            frameArray.forEach { it.texture.dispose() }
        }
        sprFlare.dispose()
        curCombo = 0
        inputProcessor.dispose()
    }

    fun createWhiteTexture() {
        val pixmap =
            Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)
        pixmap.fill()
        whiteTex = Texture(pixmap)
        pixmap.dispose()
    }

    private fun drawMineFlash(timeCom: Long) {
        if (mineFlashStartTime == 0L) return

        val elapsed = timeCom - mineFlashStartTime
        if (elapsed >= MINE_FLASH_DURATION) {
            mineFlashStartTime = 0L
            return
        }
        val t = elapsed.toFloat() / MINE_FLASH_DURATION
        val alpha = 1f - (t * t)

        batch.setColor(1f, 1f, 1f, alpha)
        batch.draw(whiteTex, 0f, 0f, gdxWidth.toFloat(), gdxHeight.toFloat())
        batch.setColor(Color.WHITE)
    }
}
