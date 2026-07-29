package com.fingerdance.ssc

import LuaEngine
import android.util.Log
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils.sin
import com.fingerdance.GameScreenActivityHorizontal
import com.fingerdance.aBatch
import com.fingerdance.bBatch
import com.fingerdance.breakSong
import com.fingerdance.chart
import com.fingerdance.decimoHeigtn
import com.fingerdance.displayBPM
import com.fingerdance.heightBtnsHorizontal
import com.fingerdance.heightJudges
import com.fingerdance.hjBad
import com.fingerdance.hjGood
import com.fingerdance.hjGreat
import com.fingerdance.hjPerfect
import com.fingerdance.isMidLine
import com.fingerdance.isOnline
import com.fingerdance.luaFlare
import com.fingerdance.luaNotes
import com.fingerdance.luaRecepts
import com.fingerdance.medidaFlechasHorizontal
import com.fingerdance.nBad
import com.fingerdance.nGood
import com.fingerdance.nGreat
import com.fingerdance.nPerfect
import com.fingerdance.padPositionsHorizontalHD
import com.fingerdance.playerSong
import com.fingerdance.resultSong
import com.fingerdance.ruta
import com.fingerdance.showPadB
import com.fingerdance.soundPoolSelectSong
import com.fingerdance.sound_mine
import com.fingerdance.valueOffset
import com.fingerdance.widthBtnsHorizontal
import com.fingerdance.widthJudges
import java.io.File

class PlayerSscHorizontalHD (
    val screen : GameScreenSscHorizontalHD,
    private val batch: SpriteBatch,
    private val activity: GameScreenActivityHorizontal
) : SscGameplayEngine.Renderer, SscGameplayEngine.Listener {

    private val bpms = chart.bpms
    private val tickcounts = chart.tickcounts
    private val stops = chart.stops
    private val delays = chart.delays
    private val warps = chart.warps
    private val notes = chart.notes
    private val speeds = chart.speeds
    private val scrolls = chart.scrolls
    private val combos = chart.combos

    private val sizeScale = medidaFlechasHorizontal * 1.2f
    private val topPos = medidaFlechasHorizontal *  0.9f
    private val posX = medidaFlechasHorizontal * 0.1f
    private val animationDuration: Long = 300L
    private var multiplierCombo = 1
    val STEPSIZE = medidaFlechasHorizontal.toInt()

    companion object {
        const val MINUTE = 60000f

        private var ZONE_PERFECT: Long = if (playerSong.hj) hjPerfect else nPerfect
        private var ZONE_GREAT: Long = if (playerSong.hj) hjGreat else nGreat
        private var ZONE_GOOD: Long = if (playerSong.hj) hjGood else nGood
        private var ZONE_BAD: Long = if (playerSong.hj) hjBad else nBad

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

        private val LONGNOTE = Array(10) { LongNotePress() }

        private lateinit var whiteTex: Texture
    }

    data class PlayerFlare(var startTime: Long = 0)
    data class PlayerJudge(var startTime: Long = 0, var judge: Int = 0)
    data class JudgePos(
        val x: Float = Gdx.graphics.width / 2f,
        val y: Float = Gdx.graphics.height / 2f
    )

    private val baseSpeed = playerSong.speed.replace("X", "").toFloat() + 1f

    private val timingData = TimmingData(
        bpms = bpms,
        stops = stops,
        delays = delays,
        warps = warps,
        speeds = speeds,
        scrolls = scrolls,
        offsetMs = chart.offset * 1000.0,
        userOffsetMs = valueOffset * 10.0
    )

    private val gameplayEngineConfig =
        SscGameplayEngine.Config(
            columnCount = 10,
            activeColumns = 2..7,
            stepSize = medidaFlechasHorizontal,
            screenHeight = screen.gdxHeight.toFloat(),
            baseSpeed = baseSpeed,
            isEW = playerSong.isEw,
            zonePerfectMs = ZONE_PERFECT,
            zoneGreatMs = ZONE_GREAT,
            zoneGoodMs = ZONE_GOOD,
            zoneBadMs = ZONE_BAD,
        )

    private val gameplayEngine =
        SscGameplayEngine(
            notes = notes,
            timingData = timingData,
            tickSegments = tickcounts.map {
                SscGameplayEngine.TickSegment(
                    beat = it.beat,
                    tickCount = it.tickcount.toDouble()
                )
            },
            comboSegments = combos.map {
                SscGameplayEngine.ComboSegment(
                    beat = it.beat,
                    multiplier = it.number
                )
            },
            config = gameplayEngineConfig,
            listener = this
        )

    private var m_fGauge = 0.35f
    var m_fCurBPM = displayBPM
    private var arrowFrame = 0
    private var m_iStepWidth = 10
    var curCombo = 0
    var curComboMiss = 0
    private val flare = Array(10) { PlayerFlare() }
    private var m_judge = PlayerJudge()
    private var noEffects = false

    private var mineFlashStartTime: Long = 0L
    private val MINE_FLASH_DURATION: Long = 100L

    private val widthFlare = medidaFlechasHorizontal * 5f
    private var yFlare = medidaFlechasHorizontal - (medidaFlechasHorizontal * 2f)

    private val inputProcessor = InputProcessorSscHorizontalHD()

    private var currentTimeToExpands = 0L

    private val judgePos = JudgePos()
    private val x = judgePos.x.toFloat()
    private val y = judgePos.y.toFloat()
    private var digitWidth = medidaFlechasHorizontal * 0.7f
    private var digitHeight = heightJudges * 1.3f

    private val gaugeInc = if (playerSong.hj) screen.gaugeIncHJ else screen.gaugeIncNormal
    val tipWidth = (medidaFlechasHorizontal / 4f)
    val tipHeight = medidaFlechasHorizontal / 1.5f
    val tipY = 0f - (medidaFlechasHorizontal * 0.05f) + (screen.posYGauje)

    private val columnNotes = Array(10) { mutableListOf<Parser.Note>() }
    private val columnIndex = IntArray(10) { 0 }

    private var isVanish = playerSong.vanish
    private var isAp = playerSong.ap

    private var luaFlashStartTime = 0L
    private var luaFlashDuration = 0L
    lateinit var luaEngine: LuaEngine
    init {
        currentTimeToExpands = screen.timeGetTime()
        initCommonInfo()

        Gdx.input.inputProcessor = inputProcessor

        for (x in 0 until 5) {
            LONGNOTE[x].pressed = false
        }
        if (isAp || isVanish) {
            noEffects = true
        }
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

        arrArrows = arrayOf(
            ldArrowFrame,
            luArrowFrame,
            ceArrowFrame,
            ruArrowFrame,
            rdArrowFrame,
            ldArrowFrame,
            luArrowFrame,
            ceArrowFrame,
            ruArrowFrame,
            rdArrowFrame)

        val ldBodyArrowFrame = getArrows6x1(downLeftBody)
        val luBodyArrowFrame = getArrows6x1(upLeftBody)
        val ceBodyArrowFrame = getArrows6x1(centerBody)
        val ruBodyArrowFrame = getArrows6x1(upLeftBody, true)
        val rdBodyArrowFrame = getArrows6x1(downLeftBody, true)

        arrArrowsBody = arrayOf(
            ldBodyArrowFrame,
            luBodyArrowFrame,
            ceBodyArrowFrame,
            ruBodyArrowFrame,
            rdBodyArrowFrame,
            ldBodyArrowFrame,
            luBodyArrowFrame,
            ceBodyArrowFrame,
            ruBodyArrowFrame,
            rdBodyArrowFrame)

        val ldBottomArrowFrame = getArrows6x1(downLeftBottom)
        val luBottomArrowFrame = getArrows6x1(upLeftBottom)
        val ceBottomArrowFrame = getArrows6x1(centerBottom)
        val ruBottomArrowFrame = getArrows6x1(upLeftBottom, true)
        val rdBottomArrowFrame = getArrows6x1(downLeftBottom, true)

        arrArrowsBottom = arrayOf(
            ldBottomArrowFrame,
            luBottomArrowFrame,
            ceBottomArrowFrame,
            ruBottomArrowFrame,
            rdBottomArrowFrame,
            ldBottomArrowFrame,
            luBottomArrowFrame,
            ceBottomArrowFrame,
            ruBottomArrowFrame,
            rdBottomArrowFrame)

        sprFlare = Texture(Gdx.files.absolute("$ruta/Flare 6x1.png"))
        flareArrowFrame = getArrows6x1(sprFlare)

        arrMines = getArrows3x2(mine)
        createWhiteTexture()
        initColumnNotes()
        inputProcessor.resetState()
        luaEngine = LuaEngine(playerSscHorizontalHD = this, widthNotes = medidaFlechasHorizontal * 6f)
    }

    private data class LongNotePress(
        var pressed: Boolean = false,
        var lastTickBeat: Double = 0.0,
        var nextTickBeat: Double = 0.0,
        var note: Parser.Note? = null,
        var timeStarted: Long = 0L,
    )

    private fun initColumnNotes() {
        for (col in 0 until columnNotes.size) columnNotes[col].clear()
        for (n in notes) {
            if (n.column in 0 until columnNotes.size) {
                columnNotes[n.column].add(n)
            }
        }
        for (c in 0 until 10) {
            columnNotes[c].sortBy { it.beat }
            columnIndex[c] = 0
        }
    }

    private val iLongTop = LongArray(10)

    fun render(songTimeMs: Double) {
        val timeCom = screen.timeGetTime()
        val nowMs = songTimeMs.toDouble()
        val currentBeat = timeToBeat(nowMs)

        if (showPadB == 0) {
            inputProcessor.render(batch)
        }

        val currentBpm =
            bpms.lastOrNull { it.beat <= currentBeat }?.bpm
                ?: bpms.firstOrNull()?.bpm
                ?: 120.0

        m_fCurBPM = currentBpm.toFloat()

        val msPorBeat = MINUTE / m_fCurBPM.coerceIn(1f, 999f)
        val msPorFrame = msPorBeat / 5f
        arrowFrame = ((timeCom % msPorBeat.toLong()) / msPorFrame.toLong()).toInt()

        updateFGChanges(currentBeat)

        gameplayEngine.render(
            songTimeMs = songTimeMs,
            renderer = this
        )

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
        drawLuaFlash(timeCom)
        if (m_judge.startTime == 0L) return
        if (m_judge.startTime + 2500 < timeCom) {
            m_judge.startTime = 0
            return
        }
        drawJudge(timeCom - m_judge.startTime)
    }

    fun updateStepData(songTimeMs: Double) {
        gameplayEngine.updateStepData(
            songTimeMs = songTimeMs,
            input = inputProcessor.getKeyBoard
        )

        inputProcessor.update()

        if (m_fGauge > 1.0f) {
            m_fGauge = 1.0f
        } else if (m_fGauge < -0.5f) {
            if (!isOnline && breakSong) {
                activity.breakDance()
            }
        }
    }

    private fun updateFGChanges(currentBeat: Double) {
        for (event in chart.fgChanges) {
            if (event.executed) continue
            if (currentBeat >= event.beat) {
                event.executed = true
                val stepsFolder = File(chart.chartPath).parentFile
                val target = File(stepsFolder, event.script)
                val luaFile = if (target.isDirectory) {
                    target.listFiles()?.firstOrNull { it.isFile && it.extension.equals("lua", true) }
                } else {
                    target
                }
                if (luaFile != null && luaFile.exists()) {
                    luaEngine.executeLua(luaFile.absolutePath)
                } else {
                    Log.d("LUA_DEBUG", "Lua no encontrado: $target")
                }
            }
        }
    }

    private fun applyJudge(col: Int, judge: Int, isBodyLongNote: Boolean = false, isFromInput: Boolean, isMine: Boolean = false, note: Parser.Note? = null) {
        if (note != null && note.isFake) return
        when (judge) {
            JUDGE_PERFECT -> {
                resultSong.perfect++
                curCombo += multiplierCombo
                curComboMiss = 0
            }

            JUDGE_GREAT -> {
                resultSong.great++
                curCombo += multiplierCombo
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
                curComboMiss += multiplierCombo
            }
        }

        if (resultSong.maxCombo < curCombo) {
            resultSong.maxCombo = curCombo
        }

        when (judge) {
            JUDGE_PERFECT -> {
                m_fGauge += if(isBodyLongNote){
                    (gaugeInc[judge] * 0.002f) // los ticks de hold dan muy poco
                }else {
                    gaugeInc[judge]
                }
            }

            JUDGE_GREAT, JUDGE_GOOD -> {
                m_fGauge += gaugeInc[judge]
            }
            JUDGE_BAD -> {
                m_fGauge += gaugeInc[judge]
                if (m_fGauge < 0f) m_fGauge = 0f
            }
            JUDGE_MISS -> {
                m_fGauge += if(isMine){
                    (gaugeInc[judge] + MINE_PENALTY)
                }else{
                    gaugeInc[judge]
                }
                if (m_fGauge < 0f) m_fGauge = 0f
            }
        }

        val now = screen.timeGetTime()
        m_judge.judge = judge
        m_judge.startTime = now

        // Flare: siempre que no sea MISS. (incluye ticks)
        if (isFromInput && judge != JUDGE_MISS) {
            flare[col].startTime = now
            //showExpand(col)
        }
    }

    private fun timeToBeat(timeMs: Double): Double = timingData.timeToBeat(timeMs)

    private val MEASURE = (decimoHeigtn * 2.5).toDouble()
    private val MEASUREVANISH = if(isMidLine) (decimoHeigtn * 2.375).toDouble() else (decimoHeigtn * 3.5).toDouble()
    private val initArrow = (screen.gdxHeight * 0.575)
    private var rangeAlpha = (screen.gdxHeight * 0.1)
    private val segmentHeight = screen.gdxHeight * 0.001f
    private var heightBodyHead = (medidaFlechasHorizontal * 0.3f)
    private var middleSizeFlechas = medidaFlechasHorizontal * 0.5f
    private val amplitude = medidaFlechasHorizontal / 3f
    private val frequency = 0.01f
    private val fadeDistance = medidaFlechasHorizontal
    private var offsetX = 0f

    private fun drawLongNote(x: Int, y: Int, y2: Int, note: Parser.Note) {
        when {
            isAp -> drawLongNoteAp(x, y, y2)
            isVanish || note.isVanish -> drawLongNoteVanish(x, y, y2)
            else -> drawLongNoteNormal(x, y, y2)
        }

    }

    private fun drawMines(x: Int, y: Int, note: Parser.Note) {
        when {
            isAp -> drawNoteMineAp(x, y)
            isVanish || note.isVanish -> drawNoteMineVanish(x, y)
            else -> drawNoteMine(x, y)
        }
    }

    private fun drawNote(x: Int, y: Int, note: Parser.Note) {
        when {
            isAp -> drawNoteAp(x, y)
            isVanish || note.isVanish -> drawNoteVanish(x, y)
            else -> drawNoteNormal(x, y)
        }
    }

    private fun drawLongNoteNormal(x: Int, y: Int, y2: Int) {
        val left = computeLeft(x, y)
        val posY = y.toFloat() + middleSizeFlechas
        val remainingLength = (y2 - y).toFloat()
        val heightBody = remainingLength - middleSizeFlechas

        if (heightBody > 0f) {
            batch.draw(arrArrowsBody[x][arrowFrame], left, posY, medidaFlechasHorizontal, heightBody)
        }

        if (remainingLength > heightBodyHead) {
            batch.draw(arrArrowsBottom[x][arrowFrame], left, y2.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
        }

        if (y > 0) {
            batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
        }
    }

    private fun drawLongNoteVanish(x: Int, y: Int, y2: Int){
        val left = computeLeft(x, y)
        val posY = y.toFloat() + middleSizeFlechas
        var heightBody = (y2 - y).toFloat() - middleSizeFlechas
        if (isMidLine) {
            if (posY + heightBody > MEASUREVANISH && posY < initArrow){
                var currentY = posY
                while (currentY < posY + heightBody) {
                    val drawHeight = minOf(segmentHeight, posY + heightBody - currentY)

                    val alphaSegment = when {
                        currentY > MEASUREVANISH + (medidaFlechasHorizontal * 2) -> 1f
                        currentY >= MEASUREVANISH + (medidaFlechasHorizontal * 2) - rangeAlpha -> {
                            ((currentY - (MEASUREVANISH + (medidaFlechasHorizontal * 2) - rangeAlpha)) / rangeAlpha)
                                .toFloat()
                                .coerceIn(0f, 1f)
                        }
                        else -> 0f
                    }

                    if (alphaSegment > 0f) {
                        batch.setColor(1f, 1f, 1f, alphaSegment)
                        batch.draw(arrArrowsBody[x][arrowFrame], left, currentY, medidaFlechasHorizontal, drawHeight)
                    }

                    currentY += drawHeight
                }
                batch.setColor(1f, 1f, 1f, 1f)

                if (posY + heightBody > MEASUREVANISH) {
                    batch.setColor(1f, 1f, 1f, getVanishAlpha(y2.toFloat()))

                    val shouldDrawBottom = (y2 - y) > (heightBodyHead)
                    if (shouldDrawBottom) {
                        batch.draw(arrArrowsBottom[x][arrowFrame], left, y2.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
                    }
                }
                if (posY > MEASUREVANISH) {
                    if (y > 0) {
                        batch.setColor(1f, 1f, 1f, getVanishAlpha(y.toFloat()))
                        batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
                    }

                    batch.setColor(1f, 1f, 1f, 1f)
                }
            }
        } else {
            var currentY = posY
            while (currentY < posY + heightBody) {
                val drawHeight = minOf(segmentHeight, posY + heightBody - currentY)

                val alphaSegment = when {
                    currentY > MEASUREVANISH + (medidaFlechasHorizontal * 2) -> 1f
                    currentY >= MEASUREVANISH + (medidaFlechasHorizontal * 2) - rangeAlpha -> {
                        ((currentY - (MEASUREVANISH + (medidaFlechasHorizontal * 2) - rangeAlpha)) / rangeAlpha)
                            .toFloat()
                            .coerceIn(0f, 1f)
                    }
                    else -> 0f
                }

                if (alphaSegment > 0f) {
                    batch.setColor(1f, 1f, 1f, alphaSegment)
                    batch.draw(arrArrowsBody[x][arrowFrame], left, currentY, medidaFlechasHorizontal, drawHeight)
                }

                currentY += drawHeight
            }
            batch.setColor(1f, 1f, 1f, 1f)

            if (posY + heightBody > MEASUREVANISH) {
                batch.setColor(1f, 1f, 1f, getVanishAlpha(y2.toFloat()))

                val shouldDrawBottom = (y2 - y) > (heightBodyHead)
                if (shouldDrawBottom) {
                    batch.draw(arrArrowsBottom[x][arrowFrame], left, y2.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
                }
            }
            if (posY > MEASUREVANISH) {
                if (y > 0) {
                    batch.setColor(1f, 1f, 1f, getVanishAlpha(y.toFloat()))
                    batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
                }

                batch.setColor(1f, 1f, 1f, 1f)
            }
        }
    }

    private fun drawLongNoteAp(x: Int, y: Int, y2: Int){
        val left = computeLeft(x, y)
        val posY = y.toFloat() + middleSizeFlechas
        var heightBody = (y2 - y).toFloat() - middleSizeFlechas
        if (posY < MEASURE) {
            if (posY + heightBody > MEASURE) {
                heightBody = (MEASURE - posY).toFloat()
            }
            var currentY = posY
            while (currentY < posY + heightBody) {
                val alphaSegment = getAlpha(currentY, MEASURE)
                val drawHeight = minOf(segmentHeight, posY + heightBody - currentY)
                batch.setColor(1f, 1f, 1f, alphaSegment)
                batch.draw(arrArrowsBody[x][arrowFrame], left, currentY, medidaFlechasHorizontal, drawHeight)
                currentY += drawHeight
            }

            if (y2 < MEASURE) {
                batch.setColor(1f, 1f, 1f, getAlpha(y2.toFloat(), MEASURE))
                val shouldDrawBottom = (y2 - y) > (heightBodyHead)
                if (shouldDrawBottom) {
                    batch.draw(arrArrowsBottom[x][arrowFrame], left, y2.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
                }
            }

            if (y > 0) {
                batch.setColor(1f, 1f, 1f, getAlpha(y.toFloat(), MEASURE))
                batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
            }
            batch.setColor(1f, 1f, 1f, 1f)
        }
    }

    private fun drawNoteNormal(x: Int, y: Int){
        val left = computeLeft(x, y)
        batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
    }

    private fun drawNoteVanish(x: Int, y: Int){
        val left = computeLeft(x, y)
        if(isMidLine){
            if (y < initArrow && y > MEASUREVANISH) {
                batch.setColor(1f, 1f, 1f, getVanishAlpha(y.toFloat()))
                batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
                batch.setColor(1f, 1f, 1f, 1f)
            }
        }else {
            if (y > MEASUREVANISH) {
                batch.setColor(1f, 1f, 1f, getVanishAlpha(y.toFloat()))
                batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
                batch.setColor(1f, 1f, 1f, 1f)
            }
        }
    }

    private fun drawNoteAp(x: Int, y: Int){
        val left = computeLeft(x, y)
        if (y < MEASURE) {
            batch.setColor(1f, 1f, 1f, getAlpha(y.toFloat(), MEASURE))
            batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
            batch.setColor(1f, 1f, 1f, 1f)
        }
    }

    private fun drawNoteMine(x: Int, y: Int){
        val left = computeLeft(x, y)
        if (isMidLine) {
            if (y < initArrow) {
                batch.setColor(1f, 1f, 1f, getAlpha(y.toFloat(), initArrow))
                batch.draw(arrMines[arrowFrame], left, y.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
                batch.setColor(1f, 1f, 1f, 1f)
            }
        } else {
            batch.draw(arrMines[arrowFrame], left, y.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
        }
    }

    private fun drawNoteMineVanish(x: Int, y: Int){
        val left = computeLeft(x, y)
        if(isMidLine){
            if (y < initArrow && y > MEASUREVANISH) {
                batch.setColor(1f, 1f, 1f, getVanishAlpha(y.toFloat()))
                batch.draw(arrMines[arrowFrame], left, y.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
                batch.setColor(1f, 1f, 1f, 1f)
            }
        }else {
            if (y > MEASUREVANISH) {
                batch.setColor(1f, 1f, 1f, getVanishAlpha(y.toFloat()))
                batch.draw(arrMines[arrowFrame], left, y.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
                batch.setColor(1f, 1f, 1f, 1f)
            }
        }
    }

    private fun drawNoteMineAp(x: Int, y: Int){
        val left = computeLeft(x, y)
        if (y < MEASURE) {
            batch.setColor(1f, 1f, 1f, getAlpha(y.toFloat(), MEASURE))
            batch.draw(arrMines[arrowFrame], left, y.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
            batch.setColor(1f, 1f, 1f, 1f)
        }
    }

    private fun computeLeft(x: Int, y: Int): Float {
        val baseX = getLaneX(x)
        if (playerSong.snake) {
            offsetX = (sin(y * frequency) * amplitude)
            if (y <= medidaFlechasHorizontal + fadeDistance) {
                val factor = (y - medidaFlechasHorizontal) / fadeDistance
                offsetX *= factor.coerceIn(0f, 1f)
            }
        }

        return baseX + offsetX + luaNotes.screenX
    }

    private fun getAlpha(y: Float, init: Double): Float {
        return ((init - y) / rangeAlpha).toFloat().coerceIn(0f, 1f)
    }

    private fun getVanishAlpha(y: Float): Float {
        return ((y - MEASUREVANISH) / rangeAlpha).toFloat().coerceIn(0f, 1f)
    }

    private fun drawFlare(x: Int, frame: Int) {
        val left = getLaneX(x) - ((widthFlare - medidaFlechasHorizontal) / 2f) + offsetX + luaFlare.screenX
        val flareSprite = flareSprites[frame]
        flareSprite.setBounds(left, yFlare, widthFlare, widthFlare)
        aBatch = batch.blendSrcFunc
        bBatch = batch.blendDstFunc
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        flareSprite.draw(batch)
        batch.setBlendFunction(aBatch, bBatch)

        val elapsed = (screen.timeGetTime() - currentTimeToExpands) % animationDuration

        val (alpha, zoom) = calculateAlphaAndZoom(elapsed)

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        batch.color.a = alpha // Establecer transparencia (alpha)

        batch.draw(
            arrArrows[x][arrowFrame],
            luaFlare.screenX + getLaneX(x) - ((medidaFlechasHorizontal * zoom) - medidaFlechasHorizontal) / 2f,
            STEPSIZE.toFloat() - ((medidaFlechasHorizontal * zoom) - medidaFlechasHorizontal) / 2,
            medidaFlechasHorizontal * zoom,
            medidaFlechasHorizontal * zoom
        )

        batch.color.a = 1f
        batch.setBlendFunction(aBatch, bBatch)

    }

    private fun getLaneX(x: Int): Float {
        return (
                medidaFlechasHorizontal *
                        (if (x > 2) x - 2 else 0)
                ) + screen.spaceInitHorizontalHD
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

    private fun showExpand(x: Int) {
        batch.setColor(1f, 1f, 1f, 0.7f)

        when (x) {
            2 -> batch.draw(screen.receptCE[2], luaRecepts.screenX + screen.spaceInitHorizontalHD - posX, topPos, sizeScale, sizeScale)
            3 -> batch.draw(screen.receptRU[2], luaRecepts.screenX + (medidaFlechasHorizontal) - posX + screen.spaceInitHorizontalHD, topPos, sizeScale, sizeScale)
            4 -> batch.draw(screen.receptRD[2], luaRecepts.screenX + (medidaFlechasHorizontal * 2) - posX + screen.spaceInitHorizontalHD, topPos, sizeScale, sizeScale)
            5 -> batch.draw(screen.receptLD[2], luaRecepts.screenX + (medidaFlechasHorizontal * 3) - posX + screen.spaceInitHorizontalHD, topPos, sizeScale, sizeScale)
            6 -> batch.draw(screen.receptLU[2], luaRecepts.screenX + (medidaFlechasHorizontal * 4) - posX + screen.spaceInitHorizontalHD, topPos, sizeScale, sizeScale)
            7 -> batch.draw(screen.receptCE[2], luaRecepts.screenX + (medidaFlechasHorizontal * 5) - posX + screen.spaceInitHorizontalHD, topPos, sizeScale, sizeScale)
        }

        if (showPadB == 3) {

            batch.setColor(1f, 1f, 1f, 1f)

            batch.draw(
                screen.arrayPad4[x % 5],
                padPositionsHorizontalHD[x][0],
                padPositionsHorizontalHD[x][1],
                widthBtnsHorizontal,
                heightBtnsHorizontal
            )
        }

        batch.setColor(1f, 1f, 1f, 1f)
    }

    private val judgeSprite = Sprite()
    private val comboSprite = Sprite()
    private val digitSprite = Sprite()
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

        judgeSprite.setRegion(screen.imgsJudge[m_judge.judge])
        val judgeW = widthJudges * ftX
        val judgeH = heightJudges * ftY

        judgeSprite.setSize(judgeW, judgeH)
        judgeSprite.setOriginCenter()
        judgeSprite.setCenter(x, y)
        judgeSprite.setColor(1f, 1f, 1f, alpha)
        judgeSprite.draw(batch)

        val comboWidth = (widthJudges * 0.5f) * ftX
        val comboHeight = (heightJudges * 0.5f) * ftY
        val comboY = y + heightJudges + 5f
        val comboX = (Gdx.graphics.width / 2f) - (comboWidth / 2f)

        val digitW = digitWidth * ftX
        val digitH = digitHeight * ftY

        if (curCombo >= 4 || curComboMiss >= 4) {
            val isMiss = curComboMiss >= 4
            comboSprite.setRegion(if (isMiss) screen.imgsTypeCombo[1] else (screen.imgsTypeCombo[0]))
            val count = if (isMiss) curComboMiss else curCombo
            val numberList = if (isMiss) screen.listNumbersMiss else screen.listNumbers

            comboSprite.setSize(comboWidth, comboHeight)
            comboSprite.setOriginCenter()
            comboSprite.setCenter(comboX + comboWidth / 2f, comboY + comboHeight / 2f)
            comboSprite.setColor(1f, 1f, 1f, alpha)
            comboSprite.draw(batch)

            val numStr = if (count < 100) count.toString().padStart(3, '0') else count.toString()

            val totalWidth = numStr.length * digitW
            var startX = (Gdx.graphics.width / 2f) - (totalWidth / 2f)
            val digitY = comboY + comboHeight - 10f

            //digitSprite = Sprite()
            digitSprite.setColor(1f, 1f, 1f, alpha)
            for (char in numStr) {
                val digit = char.digitToInt()
                digitSprite.setRegion(numberList[digit])
                digitSprite.setBounds(startX, digitY, digitW, digitH)
                digitSprite.draw(batch)
                startX += digitW
            }
        }
    }

    private val initGaujePosition = screen.spaceInitHorizontalHD - (medidaFlechasHorizontal * 0.5f)
    private fun drawGauge(gauge: Float) {
        val previousSrcFunc = batch.blendSrcFunc
        val previousDstFunc = batch.blendDstFunc

        val barToDraw = if (gauge <= 0.2f) screen.barRed else screen.barBlack
        barToDraw.setSize(screen.maxWidth, screen.maxlHeight)
        barToDraw.setPosition(initGaujePosition, screen.posYGauje)
        barToDraw.draw(batch)

        val visibleWidth = screen.maxWidth * gauge
        val regionWidth = (screen.barColors.texture.width * gauge).toInt()

        if (regionWidth > 0.1 && visibleWidth > 0.1f) {
            screen.barColors.setRegion(0, 0, regionWidth, screen.barColors.texture.height)
            screen.barColors.setSize(visibleWidth, screen.maxlHeight)
            screen.barColors.setPosition(initGaujePosition, screen.posYGauje)
            screen.barColors.draw(batch)
        }

        if (gauge >= 1.0f) {
            val currentTime = (screen.timeGetTime() / 100L) % 2 == 0L

            if (currentTime) {
                val time = (screen.timeGetTime() % 200L) / 200f
                val shine = 1f + 0.5f * Math.sin(time * Math.PI).toFloat()
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
                screen.barColors.setColor(shine, shine, shine, 1f)
                screen.barColors.draw(batch)
                batch.setBlendFunction(previousSrcFunc, previousDstFunc )
            } else {
                screen.barColors.setColor(1f, 1f, 1f, 1f)
                screen.barColors.draw(batch)
            }
        }else{
            screen.barColors.setColor(1f, 1f, 1f, 1f)
        }
        screen.barFrame.setSize(screen.maxWidth, screen.maxlHeight)
        screen.barFrame.setPosition(initGaujePosition, screen.posYGauje)
        screen.barFrame.draw(batch)

        val tipX: Float

        if(gauge <= 0.99f && gauge > 0f){
            tipX = visibleWidth + initGaujePosition
            screen.barTip.setSize(tipWidth, tipHeight)
            screen.barTip.setPosition(tipX, tipY)
            screen.barTip.draw(batch)
        }

    }

    private fun onMineHit(timeCom: Long) {
        mineFlashStartTime = timeCom
        soundPoolSelectSong.play(sound_mine, 1f, 1f, 1, 0, 1f)
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
        batch.draw(whiteTex, 0f, 0f, screen.gdxWidth.toFloat(), screen.gdxHeight.toFloat())
        batch.setColor(Color.WHITE)
    }

    fun triggerLuaFlash(duration: Long) {
        luaFlashDuration = duration
        luaFlashStartTime = screen.timeGetTime()
    }

    private fun drawLuaFlash(timeCom: Long) {
        if (luaFlashStartTime == 0L) return
        val elapsed = timeCom - luaFlashStartTime
        if (elapsed >= luaFlashDuration) {
            luaFlashStartTime = 0L
            return
        }
        val t = elapsed.toFloat() / luaFlashDuration
        val alpha = 1f - (t * t)
        batch.setColor(1f, 1f, 1f, alpha)
        batch.draw(whiteTex, 0f, 0f, screen.gdxWidth.toFloat(), screen.gdxHeight.toFloat())
        batch.setColor(Color.WHITE)
    }

    override fun drawTap(note: Parser.Note, column: Int, y: Int) {
        drawNote(column, y, note)
    }

    override fun drawMine(note: Parser.Note, column: Int, y: Int) {
        drawMines(column, y, note)
    }

    override fun drawHold(note: Parser.Note, column: Int, yHead: Int, yTail: Int) {
        drawLongNote(column, yHead, yTail, note)
    }

    override fun onColumnActive(column: Int) {
        showExpand(column)
    }

    override fun onJudge(
        column: Int,
        judge: Int,
        comboMultiplier: Int,
        isBodyLongNote: Boolean,
        isFromInput: Boolean,
        isMine: Boolean,
        note: Parser.Note?
    ) {
        this.multiplierCombo = comboMultiplier

        applyJudge(
            col = column,
            judge = judge,
            isBodyLongNote = isBodyLongNote,
            isFromInput = isFromInput,
            isMine = isMine,
            note = note
        )
    }

    override fun onMineHit(column: Int, note: Parser.Note) {
        onMineHit(screen.timeGetTime())
    }

}
