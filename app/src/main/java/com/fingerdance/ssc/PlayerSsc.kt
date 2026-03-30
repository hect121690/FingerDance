package com.fingerdance

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils.sin
import com.fingerdance.KsfProccess.LuaVisualEvent
import com.fingerdance.KsfProccess.Pattern
import com.fingerdance.KsfProccess.TypeNote
import java.io.File
import java.lang.Math.abs
import kotlin.experimental.and
import kotlin.math.sqrt

class PlayerSsc(private val batch: SpriteBatch, activity: GameScreenActivity) : GameScreenKsf(activity) {

    private val chkPtnNum = IntArray(5)
    private val chkLineNum = IntArray(5)

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

        const val NOTE_NONE: Byte = 0
        const val NOTE_NOTE: Byte = 1
        const val NOTE_LSTART: Byte = 6
        const val NOTE_LNOTE: Byte = 2
        const val NOTE_LEND: Byte = 10
        const val NOTE_LSTART_PRESS: Byte = 22
        const val NOTE_LEND_PRESS: Byte = 26
        const val NOTE_NOTE_MISS: Byte =33
        const val NOTE_LSTART_MISS: Byte = 38
        const val NOTE_LEND_MISS: Byte = 42

        const val NOTE_NOTE_CHK = NOTE_NOTE
        const val NOTE_LONG_CHK = NOTE_LNOTE
        const val NOTE_START_CHK: Byte = 4
        const val NOTE_END_CHK: Byte = 8
        const val NOTE_PRESS_CHK: Byte = 16
        const val NOTE_MISS_CHK: Byte = 32

        private var ZONE_PERFECT: Long = if(playerSong.hj) 18 else 40
        private var ZONE_GREAT: Long = if(playerSong.hj) 40 else 80
        private var ZONE_GOOD: Long = if(playerSong.hj) 80 else 100
        private var ZONE_BAD: Long = if(playerSong.hj) 100 else 130

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

        private lateinit var arrMines : Array<TextureRegion>

        private lateinit var arrArrows : Array<Array<TextureRegion>>
        private lateinit var arrArrowsBody : Array<Array<TextureRegion>>
        private lateinit var arrArrowsBottom : Array<Array<TextureRegion>>

        private lateinit var sprFlare: Texture

        private lateinit var flareArrowFrame : Array<TextureRegion>

        private val LONGNOTE = Array(5) { LongNotePress() }

        private lateinit var whiteTex: Texture

    }

    data class PlayerFlare(var startTime: Long = 0)
    data class PlayerJudge(var startTime: Long = 0, var judge: Int = 0)
    enum class SetSpeedType { SET, ADD, SUB }
    data class JudgePos(val x: Int = widthJudges - (widthJudges / 2), val y: Int = Gdx.graphics.height / 2 - heightJudges * 6)

    private var m_fGauge = 0.35f
    var m_fCurBPM  = 0F
    private var arrowFrame = 0
    private var mCurPtnNum = 0
    private var m_iStepWidth = 5
    var curCombo = 0
    var curComboMiss = 0
    private var bBpmFound = false
    private val flare = Array(5) { PlayerFlare() }
    private var m_judge = PlayerJudge()
    private var noEffects = false

    // Mine flash variables
    private var mineFlashStartTime: Long = 0L
    private val MINE_FLASH_DURATION: Long = 100L

    private val widthFlare = medidaFlechas * 5f
    private var yFlare = medidaFlechas - (medidaFlechas * 2f)

    private val inputProcessor = InputProcessor()

    private var speed = playerSong.speed.replace("X", "").toFloat() + 1f

    var baseSpeed = speed

    private var currentTimeToExpands = 0L

    private val judgePos = JudgePos()
    private val x = judgePos.x.toFloat()
    private val y = judgePos.y.toFloat()
    private var digitWidth = medidaFlechas * 0.7f
    private var digitHeight = heightJudges * 1.3f

    private var stepWidth = 5
    private val GaugeInc = if(playerSong.hj) gaugeIncHJ else gaugeIncNormal
    val tipWidth = (medidaFlechas / 4f)
    val tipHeight = medidaFlechas / 1.5f
    val tipY = 0f - (medidaFlechas * 0.05f)

    init {
        currentTimeToExpands = timeGetTime()
        initCommonInfo()
        setSpeed(SetSpeedType.SET, speed)
        Gdx.input.inputProcessor = inputProcessor

        for(x in 0 until 5){
            LONGNOTE[x].pressed = false
            chkPtnNum[x] = 0
            chkLineNum[x] = 0
        }
        if(playerSong.ap || playerSong.vanish){
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
        var typeNote: TypeNote = TypeNote.NORMAL
    )

    private var isSpeedChanging = false
    private var speedStart = 0f
    private var speedTarget = 0f
    private var speedElapsed = 0f
    private var speedDuration = 0f
    private var lastSpeedPattern = -1

    var luaReceptOffsetX = 0f
    private var luaNoteOffsetX = 0f

    private var activeLuaEvents = mutableListOf<LuaVisualEvent>()

    fun render(delta: Long) {
        var time = delta
        val timeCom = timeGetTime()
        val iLongTop = LongArray(5){0}

        var iPtnNoGauge: Int

        if(showPadB == 0){
            inputProcessor.render(batch)
        }
        val msPorBeat = (MINUTE / m_fCurBPM.coerceIn(1f, ksf.MAX_BPM))
        val msPorFrame = msPorBeat / 5
        arrowFrame = ((timeCom % msPorBeat) / msPorFrame).toInt()

        val currentBeat = getGlobalBeat(time)
        updateLuaEvents(currentBeat)



        if(m_fGauge > 1.0f){
            m_fGauge = 1.0f
        }
        if(m_fGauge < -0.5f){
            m_fGauge = 0.0f
        }

        var gaugeFind = (MINUTE / m_fCurBPM.coerceIn(1f, ksf.MAX_BPM))
        iPtnNoGauge = abs(ksf.patterns[iPtnNowNo].timePos - time).toInt()

        while(iPtnNoGauge >= gaugeFind){
            iPtnNoGauge = (iPtnNoGauge - gaugeFind).toInt()
        }
        if(time < ptnFirst.timePos){
            iPtnNoGauge = (gaugeFind - iPtnNoGauge).toInt()
        }
        gaugeFind = iPtnNoGauge / gaugeFind * 0.1f

        if(m_fGauge == 1.0f){
            gaugeFind *= 3.0f
            gaugeFind += 0.7f
            drawGauge(2.0f + gaugeFind)
        }else{
            gaugeFind += m_fGauge
            if (gaugeFind > 1.0f){
                gaugeFind = 1.0f
            }
            drawGauge(gaugeFind)
        }

        for (iStepNo in 0 until m_iStepWidth) {

            if (flare[iStepNo].startTime == 0L) {
                continue
            }
            iLongTop[iStepNo] = ((timeCom - flare[iStepNo].startTime) shr 6)
            if (iLongTop[iStepNo] >= 6) {
                flare[iStepNo].startTime = 0
                continue
            }
            drawFlare(iStepNo, iLongTop[iStepNo].toInt())
        }

        // Draw mine flash
        drawMineFlash(timeCom)

        if(m_judge.startTime == 0L){
            return
        }
        if(m_judge.startTime + 2500 < timeCom){
            m_judge.startTime = 0
            return
        }
        drawJudge(timeCom - m_judge.startTime)
    }

    val BASE_MS = 16f

    fun updateStepData(time: Long) {
        var iptn: Int
        val iptnc = ksf.patterns.size
        var line_num_s: Int
        var line_num_c: Int
        var judge_time: Long
        var line_mpos: Long
        var line_num: Int
        var judge: Int
        val key = IntArray(5)

        var ptn_now: Pattern

        // 1) Leer estado actual ANTES de convertir DOWN->PRESS
        val keyBoard = inputProcessor.getKeyBoard
        for (x in 0 until m_iStepWidth) {
            key[x] = keyBoard[x]
        }

        for (x in 0 until stepWidth) {
            if (key[x] == KEY_DOWN) {
                showExpand(x)
            }
            else if (key[x] == KEY_PRESS) {
                showExpand(x)

            }
            else if (key[x] == KEY_UP) {

            }
        }

        inputProcessor.update()

        if (m_fGauge > 1.0f) {
            m_fGauge = 1.0f
        } else if (m_fGauge < -0.5f) {
            if (!isOnline) {
                if (breakSong) {
                    a.breakDance()
                }
            }
        }
    }

    private fun updateLuaEvents(currentBeat: Float) {

        luaReceptOffsetX = 0f
        luaNoteOffsetX = 0f

        for (event in ksf.luaEvents) {
            if (!event.started && currentBeat >= event.startBeat) {
                event.started = true
                event.runtimeStartBeat = event.startBeat
                activeLuaEvents.add(event)
            }
        }

        val iterator = activeLuaEvents.iterator()

        while (iterator.hasNext()) {

            val event = iterator.next()
            val elapsed = currentBeat - event.runtimeStartBeat

            if (elapsed >= event.durationBeat) {

                val xPercent = event.params["x"] ?: 0f
                val finalOffset = gdxWidth * xPercent

                when (event.target) {
                    KsfProccess.VisualTarget.RECEPTOR -> luaReceptOffsetX += finalOffset
                    KsfProccess.VisualTarget.NOTES -> luaNoteOffsetX += finalOffset
                }

                iterator.remove()
                continue
            }

            val duration = maxOf(event.durationBeat, 0.0001f)
            val t = (elapsed / duration).coerceIn(0f, 1f)

            val xPercent = event.params["x"] ?: 0f
            val targetOffset = gdxWidth * xPercent
            val offset = targetOffset * t

            when (event.target) {
                KsfProccess.VisualTarget.RECEPTOR -> luaReceptOffsetX += offset
                KsfProccess.VisualTarget.NOTES -> luaNoteOffsetX += offset
            }
        }
    }

    private fun findPatternByTime(time: Long): Pattern {
        for (i in ksf.patterns.size - 1 downTo 0) {
            if (time >= ksf.patterns[i].timePos) return ksf.patterns[i]
        }
        return ksf.patterns.first()
    }

    fun getGlobalBeat(songTimeMs: Long): Float {

        val ptn = findPatternByTime(songTimeMs)

        if (ptn.fBPM <= 0f) return ptn.beatStart

        val localMs = songTimeMs - ptn.timePos
        val msPerBeat = MINUTE / ptn.fBPM
        val localBeat = localMs / msPerBeat

        return ptn.beatStart + localBeat
    }

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
        if(playerSong.snake) {
            offsetX = (sin(y * frequency) * amplitude)
            if (y <= medidaFlechas + fadeDistance) {
                val factor = (y - medidaFlechas) / fadeDistance
                offsetX *= factor.coerceIn(0f, 1f)
            }
        }

        val left = baseX + offsetX + luaNoteOffsetX

        if(!noEffects){
            if(isMidLine){
                if(y < initArrow){
                    batch.setColor(1f, 1f, 1f, getAlpha(y.toFloat(), initArrow))
                    batch.draw(arrMines[arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                    batch.setColor(1f, 1f, 1f, 1f)
                }
            }else{
                batch.draw(arrMines[arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
            }
        }else{
            if(playerSong.vanish){
                if(y > MEASUREVANISH){
                    batch.setColor(1f, 1f, 1f, getVanishAlpha(y.toFloat()))
                    batch.draw(arrMines[arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                    batch.setColor(1f, 1f, 1f, 1f)
                }
            }
            if(playerSong.ap){
                if(y < MEASURE){
                    batch.setColor(1f, 1f, 1f, getAlpha(y.toFloat(), MEASURE))
                    batch.draw(arrMines[arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                    batch.setColor(1f, 1f, 1f, 1f)
                }
            }
        }
    }

    private fun drawNote(x: Int, y: Int) {
        val baseX = medidaFlechas * (x + 1)
        if(playerSong.snake) {
            offsetX = (sin(y * frequency) * amplitude)
            if (y <= medidaFlechas + fadeDistance) {
                val factor = (y - medidaFlechas) / fadeDistance
                offsetX *= factor.coerceIn(0f, 1f)
            }
        }

        val left = baseX + offsetX + luaNoteOffsetX
        //val left = medidaFlechas * (x + 1)

        if(!noEffects){
            if(isMidLine){
                if(y < initArrow){
                    batch.setColor(1f, 1f, 1f, getAlpha(y.toFloat(), initArrow))
                    batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                    batch.setColor(1f, 1f, 1f, 1f)
                }
            }else{
                batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
            }
        }else{
            if(playerSong.vanish){
                if(y > MEASUREVANISH){
                    batch.setColor(1f, 1f, 1f, getVanishAlpha(y.toFloat()))
                    batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                    batch.setColor(1f, 1f, 1f, 1f)
                }
            }
            if(playerSong.ap){
                if(y < MEASURE){
                    batch.setColor(1f, 1f, 1f, getAlpha(y.toFloat(), MEASURE))
                    batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                    batch.setColor(1f, 1f, 1f, 1f)
                }
            }
        }
    }

    private fun drawLongNote(x: Int, y: Int, y2: Int) {
        val baseX = medidaFlechas * (x + 1)
        if(playerSong.snake) {
            offsetX = (sin(y * frequency) * amplitude)
            if (y <= medidaFlechas + fadeDistance) {
                val factor = (y - medidaFlechas) / fadeDistance
                offsetX *= factor.coerceIn(0f, 1f)
            }
        }
        val left = baseX + offsetX + luaNoteOffsetX

        //val left = medidaFlechas * (x + 1)
        val posY = y.toFloat() + (medidaFlechas)
        var heightBody = (y2 - y).toFloat() - (medidaFlechas)

        if(!noEffects){
            if(isMidLine){
                if(posY < initArrow){
                    if(posY + heightBody > initArrow){
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
                        batch.draw(arrArrowsBody[x][arrowFrame], left, y + heightBodyHead, medidaFlechas, heightBodyHead)
                        batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                    }

                    batch.setColor(1f, 1f, 1f, 1f)
                }
            }else{
                batch.draw(arrArrowsBody[x][arrowFrame], left, posY, medidaFlechas, heightBody)
                batch.draw(arrArrowsBottom[x][arrowFrame],left, y2.toFloat(), medidaFlechas, medidaFlechas)

                if(y > 0){
                    batch.draw(arrArrowsBody[x][arrowFrame], left, y + heightBodyHead, medidaFlechas, heightBodyHead)
                    batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                }
            }
        }else{
            if(playerSong.vanish){
                var currentY = posY
                while (currentY < posY + heightBody) {
                    val drawHeight = minOf(segmentHeight, posY + heightBody - currentY)

                    val alphaSegment = when {
                        currentY > MEASUREVANISH + (medidaFlechas * 2) -> 1f
                        currentY >= MEASUREVANISH + (medidaFlechas * 2) - rangeAlpha -> {
                            ((currentY - (MEASUREVANISH + (medidaFlechas * 2) - rangeAlpha)) / rangeAlpha).toFloat().coerceIn(0f, 1f)
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

                if(posY + heightBody > MEASUREVANISH){
                    batch.setColor(1f, 1f, 1f, getVanishAlpha(y2.toFloat()))
                    batch.draw(arrArrowsBottom[x][arrowFrame],left, y2.toFloat(), medidaFlechas, medidaFlechas)
                }
                if(posY > MEASUREVANISH){
                    if(y > 0){
                        batch.setColor(1f, 1f, 1f, getVanishAlpha(y.toFloat()))
                        batch.draw(arrArrowsBody[x][arrowFrame], left, y + heightBodyHead, medidaFlechas, heightBodyHead)
                        batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                    }

                    batch.setColor(1f, 1f, 1f, 1f)
                }

            }
            if(playerSong.ap){
                if(posY < MEASURE){
                    if(posY + heightBody > MEASURE){
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
                        batch.draw(arrArrowsBody[x][arrowFrame], left, y + heightBodyHead, medidaFlechas, heightBodyHead)
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

    private fun drawLongNotePress(x: Int) {
        val frame = (timeGetTime() % 299 * 6 / 300).toInt()
        aBatch = batch.blendSrcFunc
        bBatch = batch.blendDstFunc
        var left = 0f
        when(x){
            0->{left = medidaFlechas * (x + 1) - xFlare1}
            1->{left = medidaFlechas * (x + 1) - xFlare2}
            2->{left = medidaFlechas * (x + 1) - xFlare3}
            3->{left = medidaFlechas * (x + 1) - xFlare4}
            4->{left = medidaFlechas * (x + 1) - xFlare5}
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        batch.draw(flareArrowFrame[frame], left, yFlare, widthFlare, widthFlare)
        batch.setBlendFunction(aBatch, bBatch)

        batch.color.a = 1f
        batch.setBlendFunction(aBatch, bBatch)
    }

    private fun drawFlare(x: Int, frame: Int) {
        var left = 0f
        when(x){
            0->{left = medidaFlechas * (x + 1) - xFlare1}
            1->{left = medidaFlechas * (x + 1) - xFlare2}
            2->{left = medidaFlechas * (x + 1) - xFlare3}
            3->{left = medidaFlechas * (x + 1) - xFlare4}
            4->{left = medidaFlechas * (x + 1) - xFlare5}
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
        batch.color.a = alpha // Establecer transparencia (alpha)

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
                if(showPadB == 2){
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(arrPadsC[position][arrowFrame], padPositionsC[position].x, padPositionsC[position].y, padPositionsC[position].size, padPositionsC[position].size)
                }
                if(showPadB == 3){
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(arrayPad4[position], padPositions[0][0], padPositions[0][1], widthBtns, heightBtns)
                }
            }
            1 -> {
                batch.draw(recept1Frames[2], (medidaFlechas * 2) - posX, topPos, sizeScale, sizeScale)
                if(showPadB == 2){
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(arrPadsC[position][arrowFrame], padPositionsC[position].x, padPositionsC[position].y, padPositionsC[position].size, padPositionsC[position].size)
                }
                if(showPadB == 3){
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(arrayPad4[position], padPositions[1][0], padPositions[1][1], widthBtns, heightBtns)
                }
            }
            2 -> {
                batch.draw(recept2Frames[2], (medidaFlechas * 3) - posX, topPos, sizeScale, sizeScale)
                if(showPadB == 2){
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(arrPadsC[position][arrowFrame], padPositionsC[position].x, padPositionsC[position].y, padPositionsC[position].size, padPositionsC[position].size)
                }
                if(showPadB == 3){
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(arrayPad4[position], padPositions[2][0], padPositions[2][1], widthBtns, heightBtns)
                }
            }
            3 -> {
                batch.draw(recept3Frames[2], (medidaFlechas * 4) - posX, topPos, sizeScale, sizeScale)
                if(showPadB == 2){
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(arrPadsC[position][arrowFrame], padPositionsC[position].x, padPositionsC[position].y, padPositionsC[position].size, padPositionsC[position].size)
                }
                if(showPadB == 3){
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(arrayPad4[position], padPositions[3][0], padPositions[3][1], widthBtns, heightBtns)
                }
            }
            4 -> {
                batch.draw(recept4Frames[2], (medidaFlechas * 5) - posX, topPos, sizeScale, sizeScale)
                if(showPadB == 2){
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(arrPadsC[position][arrowFrame], padPositionsC[position].x, padPositionsC[position].y, padPositionsC[position].size, padPositionsC[position].size)
                }
                if(showPadB == 3){
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(arrayPad4[position], padPositions[4][0], padPositions[4][1], widthBtns, heightBtns)
                }
            }
        }
        batch.setColor(1f, 1f, 1f, 1f)
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
                batch.setBlendFunction(previousSrcFunc, previousDstFunc )
            } else {
                barColors.setColor(1f, 1f, 1f, 1f)
                barColors.draw(batch)
            }
        }else{
            barColors.setColor(1f, 1f, 1f, 1f)
        }
        barFrame.setSize(maxWidth, maxlHeight)
        barFrame.setPosition(medidaFlechas, 0f)
        barFrame.draw(batch)

        val tipX: Float

        if(gauge <= 0.99f && gauge > 0f){
            tipX = visibleWidth + medidaFlechas
            barTip.setSize(tipWidth, tipHeight)
            barTip.setPosition(tipX, tipY)
            barTip.draw(batch)
        }

    }

    private fun getJudge(judgeType: Int) {
        when (judgeType) {
            0 -> {
                resultSong.perfect++
                curCombo++
                curComboMiss = 0
            }
            1 -> {
                resultSong.great++
                curCombo++
                curComboMiss = 0
            }
            2 -> {
                resultSong.good++
                curComboMiss = 0
            }
            3 -> {
                curCombo = 0
                curComboMiss = 0
                resultSong.bad++
            }
            4 -> {
                curCombo = 0
                curComboMiss++
                resultSong.miss++
            }
        }

        if (resultSong.maxCombo < curCombo) {
            resultSong.maxCombo = curCombo
        }
    }

    fun onMineHit(timeCom: Long) {
        mineFlashStartTime = timeCom
        soundPoolSelectSongKsf.play(sound_mine, 1f, 1f, 1, 0, 1f)
    }

    private fun newJudge(judgeType: Int, time: Long){
        m_judge.judge = judgeType
        m_judge.startTime = time
    }

    private fun drawJudge(time: Long) {
        val ftX: Float
        val ftY: Float
        val alpha: Float

        if (time < 100) {
            ftX = (time / 300f) + 1.0f
            ftY = ftX
            alpha = 1f
        }
        else if (time > 1200) {
            val progress = ((time - 1200) / 300f).coerceIn(0f, 1f)
            ftX = 1.0f + 0.6f * progress
            ftY = 1.0f - 0.8f * progress
            alpha = (1.0f - progress).coerceIn(0f, 1f)
        }
        else {
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

    private fun getJudgement(judgeTime: Long): Int {
        return if(judgeTime <= ZONE_PERFECT){
            JUDGE_PERFECT
        }else if(judgeTime <= ZONE_GREAT){
            JUDGE_GREAT
        }else if(judgeTime <= ZONE_GOOD){
            JUDGE_GOOD
        }else {
            JUDGE_BAD
        }
    }

    private fun adaptValue(valorOriginal: Long): Long {
        return (valorOriginal * STEPSIZE) / 60
    }

    private fun setSpeed(type: SetSpeedType, spd: Float) {
        when (type) {
            SetSpeedType.SET -> speed = spd
            SetSpeedType.ADD -> speed += spd
            SetSpeedType.SUB -> speed -= spd
        }
        speed = speed.coerceIn(0.5f, 8f)
    }

    private fun newFlare(x: Int, time: Long) {
        flare[x].startTime = time
    }

    private fun getArrows3x2(arrow: Texture, isMirror: Boolean = false) : Array<TextureRegion> {
        val tmp = TextureRegion.split(arrow, arrow.width / 3, arrow.height / 2)

        return if(!isMirror) {
            val frames = arrayOf(
                tmp[0][0], tmp[0][1], tmp[0][2],
                tmp[1][0], tmp[1][1], tmp[1][2]
            )
            frames[0].flip(false, true)
            frames[1].flip(false, true)
            frames[2].flip(false, true)
            frames[3].flip(false, true)
            frames[4].flip(false, true)
            frames[5].flip(false, true)
            frames
        }else{
            val frames = arrayOf(
                tmp[0][0], tmp[0][1], tmp[0][2],
                tmp[1][0], tmp[1][1], tmp[1][2]
            )
            frames[0].flip(true, true)
            frames[1].flip(true, true)
            frames[2].flip(true, true)
            frames[3].flip(true, true)
            frames[4].flip(true, true)
            frames[5].flip(true, true)
            frames
        }
    }

    private fun getArrows6x1(arrow: Texture, isMirror: Boolean = false) : Array<TextureRegion> {
        arrow.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        val tmp = TextureRegion.split(arrow, arrow.width / 6, arrow.height)

        return if(!isMirror) {
            val frames = arrayOf(
                tmp[0][0], tmp[0][1], tmp[0][2],
                tmp[0][3], tmp[0][4], tmp[0][5]
            )
            frames[0].flip(false, true)
            frames[1].flip(false, true)
            frames[2].flip(false, true)
            frames[3].flip(false, true)
            frames[4].flip(false, true)
            frames[5].flip(false, true)
            frames
        }else{
            val frames = arrayOf(
                tmp[0][0], tmp[0][1], tmp[0][2],
                tmp[0][3], tmp[0][4], tmp[0][5]
            )
            frames[0].flip(true, true)
            frames[1].flip(true, true)
            frames[2].flip(true, true)
            frames[3].flip(true, true)
            frames[4].flip(true, true)
            frames[5].flip(true, true)
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

        /*
        arrArrowsHead.forEach { frameArray ->
            frameArray.forEach { it.texture.dispose() }
        }
        */

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
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
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
        val alpha = 1f - (t * t) // caída más agresiva

        batch.setColor(1f, 1f, 1f, alpha)
        batch.draw(whiteTex, 0f, 0f, gdxWidth.toFloat(), gdxHeight.toFloat())
        batch.setColor(Color.WHITE)
    }

}




