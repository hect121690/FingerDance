package com.fingerdance

import java.io.File
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.round
import kotlin.random.Random

class KsfParser {
    companion object {
        // Constantes para tipos de notas
        const val NOTE_NONE: Byte = 0
        const val NOTE_NOTE: Byte = 1
        const val NOTE_LSTART: Byte = 6
        const val NOTE_LNOTE: Byte = 2
        const val NOTE_LEND: Byte = 10
        const val NOTE_LSTART_PRESS: Byte = 22
        const val NOTE_LEND_PRESS: Byte = 26
        const val NOTE_NOTE_MISS: Byte = 33
        const val NOTE_LSTART_MISS: Byte = 38
        const val NOTE_LEND_MISS: Byte = 42

        // Constantes para verificación de notas
        const val NOTE_NOTE_CHK: Byte = NOTE_NOTE
        const val NOTE_LONG_CHK: Byte = NOTE_LNOTE
        const val NOTE_START_CHK: Byte = 4
        const val NOTE_END_CHK: Byte = 8
        const val NOTE_PRESS_CHK: Byte = 16
        const val NOTE_MISS_CHK: Byte = 32
    }

    // Estructuras de datos
    data class LoadingInfo(val tag: String, val value: String)
    data class StepInfo(val step: String, val type: StepInfoType)
    data class LongNoteInfo(
        var isUsed: Boolean = false,
        var prevPtn: Int = -1,
        var prevPos: Int = -1
    )

    data class Line(val step: ByteArray = ByteArray(5)) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Line
            return step.contentEquals(other.step)
        }

        override fun hashCode(): Int = step.contentHashCode()
    }

    data class Pattern(
        var timePos: Long = 0,
        var timeLen: Long = 0,
        var timeDelay: Long = 0,
        var tick: Long = 0,
        var lastMissCheck: Int = 0,
        var bpm: Double = 0.0,
        var lines: MutableList<Line> = mutableListOf()
    )

    enum class StepInfoType {
        STEP, BPM, TICK, DELAY, DELAYBEAT, UNKNOWN
    }

    var patterns = mutableListOf<Pattern>()

    fun load(filename: String): Boolean {
        val file = File(filename)
        if (!file.exists()) return false

        val loadInfo = mutableListOf<LoadingInfo>()
        val vStepInfo = mutableListOf<StepInfo>()

        file.useLines { lines ->
            var readingSteps = false
            for (line in lines) {
                if (!readingSteps && line.startsWith("#")) {
                    val parts = line.split(":", limit = 2)
                    if (parts.size == 2) {
                        val tag = parts[0].substring(1)
                        var value = parts[1].trim()
                        if (value.endsWith(";")) {
                            value = value.substring(0, value.length - 1)
                        }
                        if (tag == "STEP") {
                            readingSteps = true
                        } else {
                            loadInfo.add(LoadingInfo(tag, value))
                        }
                    }
                } else if (readingSteps) {
                    when {
                        line.startsWith("|") -> {
                            val type = when (line[1]) {
                                'B' -> StepInfoType.BPM
                                'T' -> StepInfoType.TICK
                                'D' -> StepInfoType.DELAY
                                'E' -> StepInfoType.DELAYBEAT
                                else -> StepInfoType.UNKNOWN
                            }
                            vStepInfo.add(StepInfo(line.substring(2, line.length - 1), type))
                        }
                        line.startsWith("#") -> {
                            val parts = line.split(":", limit = 2)
                            if (parts.size == 2) {
                                val tag = parts[0].substring(1)
                                var value = parts[1].trim()
                                if (value.endsWith(";")) {
                                    value = value.substring(0, value.length - 1)
                                }
                                val type = when (tag) {
                                    "BPM" -> StepInfoType.BPM
                                    "TICKCOUNT" -> StepInfoType.TICK
                                    "DELAY" -> StepInfoType.DELAY
                                    "DELAYBEAT" -> StepInfoType.DELAYBEAT
                                    else -> StepInfoType.UNKNOWN
                                }
                                vStepInfo.add(StepInfo(value, type))
                            }
                        }
                        line.length == 13 -> {
                            vStepInfo.add(StepInfo(line, StepInfoType.STEP))
                        }
                    }
                }
            }
        }

        var tickCount = 4L
        var bpm = 0.0
        var bpm2 = 0.0
        var bpm3 = 0.0
        var bunki = 0
        var bunki2 = 0
        var startTime = 0L
        var startTime2 = 0L
        var startTime3 = 0L
        var useOldBpm = false

        for (info in loadInfo) {
            when (info.tag) {
                "TICKCOUNT" -> tickCount = info.value.toLong()
                "BPM" -> bpm = info.value.toDouble()
                "BPM2" -> bpm2 = info.value.toDouble()
                "BPM3" -> bpm3 = info.value.toDouble()
                "BUNKI" -> bunki = info.value.toInt() * 10
                "BUNKI2" -> bunki2 = info.value.toInt() * 10
                "STARTTIME" -> startTime = (round(info.value.toDouble()) * 10).toLong()
                "STARTTIME2" -> startTime2 = (round(info.value.toDouble()) * 10).toLong()
                "STARTTIME3" -> startTime3 = (round(info.value.toDouble()) * 10).toLong()
            }
        }

        if(bpm < 0) bpm = 0.0
        if(bpm2 < 0) bpm2 = 0.0
        if(bpm3 < 0) bpm3 = 0.0
        if(startTime < 0) startTime = 0L
        if(startTime2 < 0) startTime = 0L
        if(startTime3 < 0) startTime = 0L
        if(bunki < 0) bunki = 0
        if(bunki2 < 0) bunki2 = 0

        if (bpm2 != 0.0) {
            useOldBpm = true
            if (bunki == 0) {
                bpm = bpm2
                bpm2 = bpm3
                bpm3 = 0.0
                bunki = bunki2
                bunki2 = 0
                startTime = startTime2
                startTime2 = startTime3
                startTime3 = 0
            }
            if (bunki == bunki2) {
                bpm2 = bpm3
                bpm3 = 0.0
                bunki2 = 0
                startTime2 = startTime3
                startTime3 = 0
            }
            if (bpm2 == 0.0) {
                useOldBpm = false
            }
        }

        val curLongNote = Array(10) { LongNoteInfo() }
        var curTick = tickCount
        var curBpm = bpm
        val ButtonCount = 5

        var curPtn = Pattern(bpm = bpm, tick = tickCount)
        patterns.add(curPtn)

        while (vStepInfo.isNotEmpty()) {
            val stepInfo = vStepInfo.removeFirst()

            when (stepInfo.type) {
                StepInfoType.STEP -> {
                    val line = Line()
                    for (iStep in 0 until ButtonCount) {
                        when (stepInfo.step[iStep]) {
                            '1' -> {
                                line.step[iStep] = NOTE_NOTE
                                if (curLongNote[iStep].isUsed) {
                                    val step = patterns[curLongNote[iStep].prevPtn]
                                        .lines[curLongNote[iStep].prevPos]
                                        .step[iStep]
                                    curLongNote[iStep].isUsed = false
                                    line.step[iStep] = step or NOTE_END_CHK
                                    if ((step and (NOTE_START_CHK or NOTE_END_CHK)) == (NOTE_START_CHK or NOTE_END_CHK)) {
                                        line.step[iStep] = NOTE_NOTE
                                    }
                                }
                            }
                            '4' -> {
                                line.step[iStep] = NOTE_LNOTE
                                if (!curLongNote[iStep].isUsed) {
                                    line.step[iStep] = line.step[iStep] or NOTE_START_CHK
                                }
                                curLongNote[iStep].isUsed = true
                                curLongNote[iStep].prevPtn = patterns.size - 1
                                curLongNote[iStep].prevPos = patterns.last().lines.size
                            }
                            '0', '2' -> {
                                if (curLongNote[iStep].isUsed) {
                                    val step = patterns[curLongNote[iStep].prevPtn]
                                        .lines[curLongNote[iStep].prevPos]
                                        .step[iStep]
                                    curLongNote[iStep].isUsed = false
                                    line.step[iStep] = step or NOTE_END_CHK
                                    if ((step and (NOTE_START_CHK or NOTE_END_CHK)) == (NOTE_START_CHK or NOTE_END_CHK)) {
                                        line.step[iStep] = NOTE_NOTE
                                    }
                                }
                                line.step[iStep] = NOTE_NONE
                            }
                        }
                    }
                    patterns.last().lines.add(line)
                }

                StepInfoType.BPM -> {
                    if (patterns.last().lines.isNotEmpty()) {
                        curPtn = Pattern(bpm = stepInfo.step.toDouble(), tick = curTick)
                        curBpm = curPtn.bpm
                        patterns.add(curPtn)
                    } else {
                        curPtn.bpm = stepInfo.step.toDouble()
                        curBpm = curPtn.bpm
                    }
                }

                StepInfoType.TICK -> {
                    if (patterns.last().lines.isNotEmpty()) {
                        curPtn = Pattern(bpm = curBpm, tick = stepInfo.step.toLong())
                        curTick = curPtn.tick
                        patterns.add(curPtn)
                    } else {
                        curPtn.tick = stepInfo.step.toLong()
                        curTick = curPtn.tick
                    }
                }

                StepInfoType.DELAY, StepInfoType.DELAYBEAT -> {
                    val delayPtn = Pattern(bpm = curBpm, tick = curTick)
                    if (patterns.last().lines.isNotEmpty()) {
                        curPtn = Pattern(bpm = curBpm, tick = curTick)
                    } else {
                        patterns.removeLast()
                    }

                    delayPtn.timeDelay = if (stepInfo.type == StepInfoType.DELAY) {
                        stepInfo.step.toLong()
                    } else {
                        (60000 / curBpm * stepInfo.step.toFloat() / curTick).toLong()
                    }

                    patterns.add(delayPtn)
                    patterns.add(curPtn)
                }
                else -> {}
            }
        }

        /*
        // Procesar la información de los pasos
        for (si in stepInfo) {
            when (si.type) {
                StepInfoType.STEP -> {
                    val line = Line()
                    for (i in 0 until ButtonCount) {
                        when (si.step[i]) {
                            '1' -> {
                                line.step[i] = NOTE_NOTE
                                if (curLongNote[i].isUsed) {
                                    var step = patterns[curLongNote[i].prevPtn].lines[curLongNote[i].prevPos].step[i]
                                    curLongNote[i].isUsed = false
                                    //patterns[curLongNote[i].prevPtn].lines[curLongNote[i].prevPos].step[i] =
                                    step = step or NOTE_END_CHK
                                    if ((step and (NOTE_START_CHK or NOTE_END_CHK)) ==
                                        (NOTE_START_CHK or NOTE_END_CHK)
                                    ) {
                                        patterns[curLongNote[i].prevPtn].lines[curLongNote[i].prevPos].step[i] =
                                            NOTE_NOTE
                                    }
                                }
                            }
                            '4' -> {
                                line.step[i] = NOTE_LNOTE
                                if (!curLongNote[i].isUsed) {
                                    line.step[i] = NOTE_LSTART
                                }
                                curLongNote[i].isUsed = true
                                curLongNote[i].prevPtn = patterns.size - 1
                                curLongNote[i].prevPos = patterns.last().lines.size
                            }
                            else -> {
                                if (curLongNote[i].isUsed) {
                                    val prevStep = patterns[curLongNote[i].prevPtn].lines[curLongNote[i].prevPos].step[i]
                                    curLongNote[i].isUsed = false
                                    patterns[curLongNote[i].prevPtn].lines[curLongNote[i].prevPos].step[i] = (prevStep or NOTE_END_CHK).toByte()
                                    if ((prevStep and (NOTE_START_CHK or NOTE_END_CHK).toByte()) ==
                                        (NOTE_START_CHK or NOTE_END_CHK).toByte()
                                    ) {
                                        patterns[curLongNote[i].prevPtn].lines[curLongNote[i].prevPos].step[i] =
                                            NOTE_NOTE
                                    }
                                }
                                line.step[i] = NOTE_NONE
                            }
                        }
                    }
                    patterns.last().lines.add(line)
                }
                StepInfoType.BPM -> {
                    if (patterns.last().lines.isNotEmpty()) {
                        curPtn = Pattern(bpm = si.step.toDouble(), tick = curTick)
                        curBpm = curPtn.bpm
                        patterns.add(curPtn)
                    } else {
                        curPtn.bpm = si.step.toDouble()
                        curBpm = curPtn.bpm
                    }
                }
                StepInfoType.TICK -> {
                    if (patterns.last().lines.isNotEmpty()) {
                        curPtn = Pattern(bpm = curBpm, tick = si.step.toLong())
                        curTick = curPtn.tick
                        patterns.add(curPtn)
                    } else {
                        curPtn.tick = si.step.toLong()
                        curTick = curPtn.tick
                    }
                }
                StepInfoType.DELAY, StepInfoType.DELAYBEAT -> {
                    val delayPtn = Pattern(bpm = curBpm, tick = curTick)
                    if (patterns.last().lines.isNotEmpty()) {
                        curPtn = Pattern(bpm = curBpm, tick = curTick)
                    } else {
                        patterns.removeLast()
                    }
                    delayPtn.timeDelay = if (si.type == StepInfoType.DELAY) si.step.toLong() else
                        (60000 / curBpm * si.step.toFloat() / curTick).toLong()
                    patterns.add(delayPtn)
                    patterns.add(curPtn)
                }
                else -> {} // Ignorar tipos desconocidos
            }
        }
        */
        if (useOldBpm) {
            handleOldBpm(
                bpm,
                bpm2,
                bpm3,
                startTime,
                startTime2,
                startTime3,
                bunki,
                bunki2,
                tickCount
            )
        } else {
            var lastTick = startTime.toFloat()
            for (ptn in patterns) {
                ptn.timePos = lastTick.toLong()
                lastTick += if (ptn.timeDelay > 0) {
                    ptn.timeDelay.toFloat()
                } else if (ptn.bpm != 0.0) {
                    (60000 / ptn.bpm * ptn.lines.size / ptn.tick).toFloat()
                } else 0f
            }
        }

        for (i in patterns.indices) {
            val ptn = patterns[i]
            ptn.timeLen = if (ptn.timeDelay > 0) {
                ptn.timeDelay
            } else if (ptn.bpm != 0.0) {
                (60000 / ptn.bpm * ptn.lines.size / ptn.tick).toLong()
            } else 0
        }

        for (i in 0 until patterns.size - 1) {
            val mpos = patterns[i].timePos
            val mlen = patterns[i].timeLen
            val npos = patterns[i + 1].timePos
            if (mpos + mlen < npos) {
                patterns[i].timeLen = npos - mpos
            }
            if (useOldBpm) {
                patterns[i].timeLen = npos - mpos
            }
        }

        return true
    }

    private fun handleOldBpm(
        bpm: Double, bpm2: Double, bpm3: Double,
        startTime: Long, startTime2: Long, startTime3: Long,
        bunki: Int, bunki2: Int, tickCount: Long
    ) {
        val ptn0 = patterns.first()
        var ptn1: Pattern?
        var ptn2: Pattern? = null

        ptn0.timePos = startTime

        if (bunki != 0) {
            var cuttingPos = getCuttingPos(bpm, startTime, bunki.toLong(), ptn0.lines.size, tickCount)
            if (cuttingPos >= 0) {
                ptn1 = Pattern(bpm = bpm2, timePos = startTime2, tick = ptn0.tick)
                patterns.add(ptn1)

                while (ptn0.lines.size > cuttingPos) {
                    val newLine = ptn0.lines[cuttingPos]
                    ptn1.lines.add(newLine)
                    ptn0.lines.removeAt(cuttingPos)
                    //ptn1.lines.add(ptn0.lines.removeAt(cuttingPos))
                }

                if (bunki2 != 0) {
                    val cuttingPos2 = getCuttingPos(
                        bpm2, startTime2, bunki2.toLong(),
                        ptn0.lines.size + ptn1.lines.size, tickCount
                    )
                    if (cuttingPos < cuttingPos2) {
                        ptn2 = Pattern(bpm = bpm3, timePos = startTime3, tick = ptn1.tick)
                        patterns.add(ptn2)
                        val newCuttingPos = cuttingPos2 - cuttingPos
                        while (ptn1.lines.size > newCuttingPos) {
                            val newLine = ptn1.lines[cuttingPos]
                            ptn2.lines.add(newLine)
                            ptn1.lines.removeAt(cuttingPos)
                            //ptn2.lines.add(ptn1.lines.removeAt(newCuttingPos))
                        }
                    } else if (cuttingPos == cuttingPos2) {
                        ptn1.bpm = bpm3
                    } else {
                        ptn2 = Pattern(bpm = bpm3, timePos = startTime3, tick = ptn1.tick)
                        patterns.add(ptn2)
                        cuttingPos -= cuttingPos
                        cuttingPos = ptn0.lines.size - cuttingPos
                        //val newCuttingPos = ptn0.lines.size - (cuttingPos2 - cuttingPos)
                        while (ptn0.lines.size > cuttingPos) {
                            val newLine = ptn0.lines[cuttingPos]
                            ptn2.lines.add(newLine)
                            ptn0.lines.removeAt(cuttingPos)
                            //ptn2.lines.add(ptn0.lines.removeAt(newCuttingPos))
                        }
                        while (ptn1.lines.size > 0) {
                            val newLine = ptn0.lines[cuttingPos]
                            ptn2.lines.add(newLine)
                            ptn1.lines.removeAt(0)
                            //ptn2.lines.add(ptn0.lines.removeAt(newCuttingPos))
                        }
                        ptn1.lines.clear()
                        patterns.removeAt(1)
                        /*
                        ptn2.lines.addAll(ptn1.lines)
                        ptn1.lines.clear()
                        patterns.removeAt(1)
                        ptn1 = ptn2
                        */
                    }
                }

                ptn1.timePos = getPtnTimePos(bpm2, startTime2, bunki.toLong(), tickCount)
                val cutting1 = getCuttingPos(bpm, startTime, bunki.toLong(), Int.MAX_VALUE, tickCount)
                var cutting2 = getCuttingPos(bpm2, startTime2, bunki.toLong(), Int.MAX_VALUE, tickCount)

                while (cutting1 < cutting2) {
                    cutting2--
                    if (ptn1.lines.isNotEmpty()) ptn1.lines.removeAt(0)
                }
                if (cutting1 > cutting2) {
                    val temp = mutableListOf<Line>()
                    while (cutting1 > cutting2) {
                        cutting2++
                        temp.add(Line())
                    }
                    ptn1.lines.addAll(0, temp)
                }

                if (patterns.size == 3) {
                    ptn2!!.timePos = getPtnTimePos(bpm3, startTime3, bunki2.toLong(), tickCount)
                    val cutting3 = getCuttingPos(bpm2, startTime2, bunki2.toLong(), Int.MAX_VALUE, tickCount)
                    var cutting4 = getCuttingPos(bpm3, startTime3, bunki2.toLong(), Int.MAX_VALUE, tickCount)

                    while (cutting3 < cutting4) {
                        cutting4--
                        if (ptn2.lines.isNotEmpty()) ptn2.lines.removeAt(0)
                    }
                    if (cutting3 > cutting4) {
                        val temp = mutableListOf<Line>()
                        while (cutting3 > cutting4) {
                            cutting4++
                            temp.add(Line())
                        }
                        ptn2.lines.addAll(0, temp)
                    }
                }
            }
        }
    }

    private fun getPtnTimePos(bpm: Double, start: Long, bunki: Long, tick: Long): Long {
        val lastTick = start.toFloat()
        val ticks = 60000 / bpm
        val dest = bunki.toFloat()

        var num = 0
        while (true) {
            val now = lastTick + (ticks * num / tick)
            if (now >= dest) return now.toLong()
            num++
        }
    }

    private fun getCuttingPos(bpm: Double, start: Long, bunki: Long, max: Int, tick: Long): Int {
        val lastTick = start.toFloat()
        val ticks = 60000 / bpm
        val dest = bunki.toFloat()

        for (num in 0 until max) {
            val now = lastTick + (ticks * num / tick)
            if (now >= dest) return num
        }
        return -1
    }

    fun makeRandom() {
        val random = Random(System.currentTimeMillis())
        val buttonCount = 5
        for (ptn in patterns) {
            for (line in ptn.lines) {
                val newLine = ByteArray(buttonCount)
                val nowLong = ByteArray(buttonCount) { -1 }
                val noNote = ByteArray(buttonCount)

                for (i in 0 until buttonCount) {
                    when (line.step[i]) {
                        NOTE_LSTART -> {
                            var r: Int
                            do {
                                r = random.nextInt(buttonCount)
                            } while (newLine[r] != 0.toByte() || nowLong[i] != (-1).toByte() || noNote[r] != 0.toByte())
                            nowLong[i] = r.toByte()
                            newLine[r] = line.step[i]
                            line.step[i] = NOTE_NONE
                            noNote[r] = 1
                        }
                        NOTE_LNOTE -> {
                            newLine[nowLong[i].toInt()] = line.step[i]
                            line.step[i] = NOTE_NONE
                        }
                        NOTE_LEND -> {
                            newLine[nowLong[i].toInt()] = line.step[i]
                            noNote[nowLong[i].toInt()] = 2
                            nowLong[i] = (-1).toByte()
                            line.step[i] = NOTE_NONE
                        }
                        NOTE_NOTE -> {
                            var r: Int
                            do {
                                r = random.nextInt(buttonCount)
                            } while (newLine[r] != 0.toByte() || nowLong[i] != (-1).toByte() || noNote[r] != 0.toByte())
                            newLine[r] = line.step[i]
                        }
                    }
                }

                for (i in 0 until buttonCount) {
                    line.step[i] = newLine[i]
                    if (nowLong[i] == buttonCount.toByte()) {
                        nowLong[i] = -1
                    }
                }

                for (i in 0 until buttonCount) {
                    when (noNote[i]) {
                        2.toByte() -> noNote[i] = 3
                        3.toByte() -> noNote[i] = 0
                    }
                }
            }
        }
    }

    // Función equivalente al operador de asignación en C++
    fun copy(): KsfParser {
        val newParser = KsfParser()
        for (ptn in patterns) {
            val newPtn = Pattern(
                timePos = ptn.timePos,
                timeLen = ptn.timeLen,
                timeDelay = ptn.timeDelay,
                tick = ptn.tick,
                lastMissCheck = ptn.lastMissCheck,
                bpm = ptn.bpm
            )
            newPtn.lines.addAll(ptn.lines.map { it.copy() })
            newParser.patterns.add(newPtn)
        }
        return newParser
    }
}


