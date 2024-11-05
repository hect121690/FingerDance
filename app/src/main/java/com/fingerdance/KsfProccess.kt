package com.fingerdance

import java.io.File
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.round
import kotlin.random.Random

class KsfProccess {
    companion object {
        const val STEPINFO_STEP = 0
        const val STEPINFO_BPM = 1
        const val STEPINFO_TICK = 2
        const val STEPINFO_DELAY = 3
        const val STEPINFO_DELAYBEAT = 4
        const val STEPINFO_UNKNOWN = 5

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

        const val NOTE_NOTE_CHK: Byte = NOTE_NOTE
        const val NOTE_LONG_CHK: Byte = NOTE_LNOTE
        const val NOTE_START_CHK: Byte = 4
        const val NOTE_END_CHK: Byte = 8
        const val NOTE_PRESS_CHK: Byte = 16
        const val NOTE_MISS_CHK: Byte = 32
    }

    data class Line(val step: ByteArray = ByteArray(10))

    data class Pattern(
        var timePos: Long = 0,
        var timeLen: Long = 0,
        var timeDelay: Long = 0,
        var tick: Int = 0,
        var iLastMissCheck: Int = 0,
        var bpm: Double = 0.0,
        val lines: MutableList<Line> = mutableListOf()
    )

    data class LoadingInfo(val tag: String, val value: String)
    data class StepInfo(val step: String, val type: Int)
    data class LongNoteInfo(var bUsed: Boolean = false, var iPrevPtn: Int = 0, var iPrevPos: Int = 0)

    val patterns = mutableListOf<Pattern>()

    fun load(filePath: String): Boolean {
        val loadInfo = mutableListOf<LoadingInfo>()
        val stepInfo = mutableListOf<StepInfo>()

        try {
            File(filePath).bufferedReader().use { reader ->
                var line: String?
                var readingSteps = false
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.startsWith("#") && !readingSteps) {
                        val parts = line!!.split(":", limit = 2)
                        if (parts.size == 2) {
                            val tag = parts[0].substring(1)
                            var value = parts[1]
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
                            line!!.startsWith("|") && line!!.endsWith("|") -> {
                                val type = when (line!![1]) {
                                    'B' -> STEPINFO_BPM
                                    'T' -> STEPINFO_TICK
                                    'D' -> STEPINFO_DELAY
                                    'E' -> STEPINFO_DELAYBEAT
                                    else -> STEPINFO_UNKNOWN
                                }
                                stepInfo.add(StepInfo(line!!.substring(2, line!!.length - 1), type))
                            }
                            line!!.startsWith("#") -> {
                                val parts = line!!.split(":", limit = 2)
                                if (parts.size == 2) {
                                    val tag = parts[0].substring(1)
                                    var value = parts[1]
                                    if (value.endsWith(";")) {
                                        value = value.substring(0, value.length - 1)
                                    }
                                    val type = when (tag) {
                                        "BPM" -> STEPINFO_BPM
                                        "TICKCOUNT" -> STEPINFO_TICK
                                        "DELAY" -> STEPINFO_DELAY
                                        "DELAYBEAT" -> STEPINFO_DELAYBEAT
                                        else -> STEPINFO_UNKNOWN
                                    }
                                    stepInfo.add(StepInfo(value, type))
                                }
                            }
                            line!!.length == 13 -> {
                                stepInfo.add(StepInfo(line!!, STEPINFO_STEP))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        var tickCount = 4
        var bpm = 0.0
        var bpm2 = 0.0
        var bpm3 = 0.0
        var bunki = 0L
        var bunki2 = 0L
        var startTime = 0L
        var startTime2 = 0L
        var startTime3 = 0L
        var bUseOldBPM = false

        loadInfo.forEach { info ->
            when (info.tag) {
                "TICKCOUNT" -> tickCount = info.value.toInt()
                "BPM" -> bpm = info.value.toDouble()
                "BPM2" -> bpm2 = info.value.toDouble()
                "BPM3" -> bpm3 = info.value.toDouble()
                "BUNKI" -> bunki = info.value.toLong() * 10
                "BUNKI2" -> bunki2 = info.value.toLong() * 10
                "STARTTIME" -> startTime = (round(info.value.toDouble()) * 10).toLong()
                "STARTTIME2" -> startTime2 = (round(info.value.toDouble()) * 10).toLong()
                "STARTTIME3" -> startTime3 = (round(info.value.toDouble()) * 10).toLong()
            }
        }

        if (bpm < 0) bpm = 0.0
        if (bpm2 < 0) bpm2 = 0.0
        if (bpm3 < 0) bpm3 = 0.0
        if (startTime < 0) startTime = 0
        if (startTime2 < 0) startTime2 = 0
        if (startTime3 < 0) startTime3 = 0
        if (bunki < 0) bunki = 0
        if (bunki2 < 0) bunki2 = 0

        if (bpm2 != 0.0) {
            bUseOldBPM = true
            if (bunki == 0L) {
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
                bUseOldBPM = false
            }
        }

        val curLongNote = Array(10) { LongNoteInfo() }
        var curTick = tickCount
        var curBPM = bpm
        val buttonCount = 5

        var curPtn = Pattern(bpm = bpm, tick = tickCount)
        patterns.add(curPtn)

        stepInfo.forEach { info ->
            when (info.type) {
                STEPINFO_STEP -> {
                    val line = Line()
                    for (iStep in 0 until buttonCount) {
                        when (info.step[iStep]) {
                            '1' -> {
                                line.step[iStep] = NOTE_NOTE
                                if (curLongNote[iStep].bUsed) {
                                    val prevStep = patterns[curLongNote[iStep].iPrevPtn].lines[curLongNote[iStep].iPrevPos].step[iStep]
                                    curLongNote[iStep].bUsed = false
                                    patterns[curLongNote[iStep].iPrevPtn].lines[curLongNote[iStep].iPrevPos].step[iStep] = (prevStep or NOTE_END_CHK)
                                    if ((prevStep and (NOTE_START_CHK or NOTE_END_CHK)) == (NOTE_START_CHK or NOTE_END_CHK)) {
                                        patterns[curLongNote[iStep].iPrevPtn].lines[curLongNote[iStep].iPrevPos].step[iStep] = NOTE_NOTE
                                    }
                                }
                            }
                            '4' -> {
                                line.step[iStep] = NOTE_LNOTE
                                if (!curLongNote[iStep].bUsed) {
                                    line.step[iStep] = (line.step[iStep] or NOTE_START_CHK)
                                }
                                curLongNote[iStep].bUsed = true
                                curLongNote[iStep].iPrevPtn = patterns.size - 1
                                curLongNote[iStep].iPrevPos = patterns.last().lines.size
                            }
                            else -> {
                                if (curLongNote[iStep].bUsed) {
                                    val prevStep = patterns[curLongNote[iStep].iPrevPtn].lines[curLongNote[iStep].iPrevPos].step[iStep]
                                    curLongNote[iStep].bUsed = false
                                    patterns[curLongNote[iStep].iPrevPtn].lines[curLongNote[iStep].iPrevPos].step[iStep] = (prevStep or NOTE_END_CHK)
                                    if ((prevStep and (NOTE_START_CHK or NOTE_END_CHK)) == (NOTE_START_CHK or NOTE_END_CHK)) {
                                        patterns[curLongNote[iStep].iPrevPtn].lines[curLongNote[iStep].iPrevPos].step[iStep] = NOTE_NOTE
                                    }
                                }
                                line.step[iStep] = NOTE_NONE
                            }
                        }
                    }
                    patterns.last().lines.add(line)
                }
                STEPINFO_BPM -> {
                    if (patterns.last().lines.isNotEmpty()) {
                        curPtn = Pattern(bpm = info.step.toDouble(), tick = curTick)
                        patterns.add(curPtn)
                    } else {
                        curPtn.bpm = info.step.toDouble()
                    }
                    curBPM = curPtn.bpm
                }
                STEPINFO_TICK -> {
                    if (patterns.last().lines.isNotEmpty()) {
                        curPtn = Pattern(bpm = curBPM, tick = info.step.toInt())
                        patterns.add(curPtn)
                    } else {
                        curPtn.tick = info.step.toInt()
                    }
                    curTick = curPtn.tick
                }
                STEPINFO_DELAY -> {
                    val delayPtn = Pattern(bpm = curBPM, tick = curTick, timeDelay = info.step.toLong())
                    if (patterns.last().lines.isNotEmpty()) {
                        curPtn = Pattern(bpm = curBPM, tick = curTick)
                    } else {
                        patterns.removeLast()
                    }
                    patterns.add(delayPtn)
                    patterns.add(curPtn)
                }
                STEPINFO_DELAYBEAT -> {
                    val delayPtn = Pattern(bpm = curBPM, tick = curTick)
                    if (patterns.last().lines.isNotEmpty()) {
                        curPtn = Pattern(bpm = curBPM, tick = curTick)
                    } else {
                        patterns.removeLast()
                    }
                    delayPtn.timeDelay = (60000 / curBPM * info.step.toFloat() / curTick).toLong()
                    patterns.add(delayPtn)
                    patterns.add(curPtn)
                }
            }
        }

        if (bUseOldBPM) {
            val ptn0 = patterns.first()
            val ptn1: Pattern?
            var ptn2: Pattern? = null
            ptn0.timePos = startTime

            if (bunki != 0L) {
                val cuttingPos = getCuttingPos(bpm, startTime, bunki, ptn0.lines.size.toLong(), tickCount)
                if (cuttingPos >= 0) {
                    ptn1 = Pattern(bpm = bpm2, timePos = startTime2, tick = ptn0.tick)
                    patterns.add(ptn1)
                    while (ptn0.lines.size > cuttingPos) {
                        val newLine = ptn0.lines[cuttingPos]
                        ptn1.lines.add(newLine)
                        ptn0.lines.removeAt(cuttingPos)
                    }
                    if (bunki2 != 0L) {
                        val cuttingPos2 = getCuttingPos(bpm2, startTime2, bunki2, (ptn0.lines.size + ptn1.lines.size).toLong(), tickCount)
                        if (cuttingPos < cuttingPos2) {
                            ptn2 = Pattern(bpm = bpm3, timePos = startTime3, tick = ptn1.tick)
                            patterns.add(ptn2)
                            val newCuttingPos = cuttingPos2 - cuttingPos
                            while  (ptn1.lines.size > newCuttingPos) {
                                val newLine = ptn1.lines[newCuttingPos]
                                ptn2.lines.add(newLine)
                                ptn1.lines.removeAt(newCuttingPos)
                            }
                        } else if (cuttingPos == cuttingPos2) {
                            ptn1.bpm = bpm3
                        } else {
                            ptn2 = Pattern(bpm = bpm3, timePos = startTime3, tick = ptn1.tick)
                            patterns.add(ptn2)
                            val newCuttingPos = ptn0.lines.size - (cuttingPos2 - cuttingPos)
                            while (ptn0.lines.size > newCuttingPos) {
                                val newLine = ptn0.lines[newCuttingPos]
                                ptn2.lines.add(newLine)
                                ptn0.lines.removeAt(newCuttingPos)
                            }
                            while (ptn1.lines.isNotEmpty()) {
                                val newLine = ptn1.lines[0]
                                ptn2.lines.add(newLine)
                                ptn1.lines.removeAt(0)
                            }
                            patterns.removeAt(1)
                        }
                    }
                    ptn1.timePos = getPtnTimePos(bpm2, startTime2, bunki, tickCount)
                    val cuttingPos1 = getCuttingPos(bpm, startTime, bunki, Long.MAX_VALUE, tickCount)
                    var tcuttingPos = getCuttingPos(bpm2, startTime2, bunki, Long.MAX_VALUE, tickCount)
                    while (cuttingPos1 < tcuttingPos) {
                        tcuttingPos--
                        ptn1.lines.removeAt(0)
                    }
                    if (cuttingPos1 > tcuttingPos) {
                        val temp = mutableListOf<Line>()
                        while (cuttingPos1 > tcuttingPos) {
                            val newLine = Line()
                            tcuttingPos++
                            temp.add(newLine)
                        }
                        ptn1.lines.addAll(0, temp)
                    }
                    if (patterns.size == 3) {
                        ptn2!!.timePos = getPtnTimePos(bpm3, startTime3, bunki2, tickCount)
                        val cuttingPos2 = getCuttingPos(bpm2, startTime2, bunki2, Long.MAX_VALUE, tickCount)
                        var tcuttingPos2 = getCuttingPos(bpm3, startTime3, bunki2, Long.MAX_VALUE, tickCount)
                        while (cuttingPos2 < tcuttingPos2) {
                            tcuttingPos2--
                            ptn2.lines.removeAt(0)
                        }
                        if (cuttingPos2 > tcuttingPos2) {
                            val temp = mutableListOf<Line>()
                            while (cuttingPos2 > tcuttingPos2) {
                                val newLine = Line()
                                tcuttingPos2++
                                temp.add(newLine)
                            }
                            ptn2.lines.addAll(0, temp)
                        }
                    }
                }
            }
        } else {
            var lastTick = startTime.toFloat()
            patterns.forEachIndexed { i, ptn ->
                ptn.timePos = lastTick.toLong()
                if (ptn.timeDelay > 0) {
                    lastTick += ptn.timeDelay
                } else {
                    if (ptn.bpm != 0.0) {
                        lastTick += (60000 / ptn.bpm * ptn.lines.size / ptn.tick).toFloat()
                    }
                }
            }
        }

        patterns.forEach { ptn ->
            ptn.timeLen = if (ptn.timeDelay > 0) {
                ptn.timeDelay
            } else {
                if (ptn.bpm != 0.0) {
                    (60000 / ptn.bpm * ptn.lines.size / ptn.tick).toLong()
                } else {
                    0
                }
            }
        }

        for (i in 0 until patterns.size - 1) {
            val mpos = patterns[i].timePos
            val mlen = patterns[i].timeLen
            val npos = patterns[i + 1].timePos
            if (mpos + mlen < npos) {
                patterns[i].timeLen = npos - mpos
            }
            if (bUseOldBPM) {
                patterns[i].timeLen = npos - mpos
            }
        }

        return true
    }

    private fun getPtnTimePos(bpm: Double, start: Long, bunki: Long, tick: Int): Long {
        val lastTick = start.toFloat()
        val ticks = 60000 / bpm
        val dest = bunki.toFloat()

        var num = 0
        while (true) {
            val now = lastTick + (ticks * num / tick)
            if (now >= dest) {
                return now.toLong()
            }
            num++
        }
    }

    private fun getCuttingPos(bpm: Double, start: Long, bunki: Long, max: Long, tick: Int): Int {
        val lastTick = start.toFloat()
        val ticks = 60000 / bpm
        val dest = bunki.toFloat()

        for (num in 0 until max.toInt()) {
            val now = lastTick + (ticks * num / tick)
            if (now >= dest) {
                return num
            }
        }
        return -1
    }

    fun makeRandom() {
        val newLine = ByteArray(10)
        val nowLong = ByteArray(10) { 255.toByte() }
        val noNote = ByteArray(10)
        val stepWidth = 5

        patterns.forEach { ptn ->
            ptn.lines.forEachIndexed { l, line ->
                for (a in 0 until stepWidth) {
                    newLine[a] = 0
                    if (noNote[a] == 2.toByte()) {
                        noNote[a] = 3
                    } else if (noNote[a] == 3.toByte()) {
                        noNote[a] = 0
                    }
                }

                for (a in 0 until stepWidth) {
                    when (line.step[a]) {
                        NOTE_LSTART -> {
                            while (true) {
                                val r = Random.nextInt(stepWidth)
                                if (newLine[r] == 0.toByte() && nowLong[a] == 255.toByte() && noNote[r] == 0.toByte()) {
                                    nowLong[a] = r.toByte()
                                    newLine[r] = line.step[a]
                                    line.step[a] = NOTE_NONE
                                    noNote[r] = 1
                                    break
                                }
                            }
                        }
                        NOTE_LNOTE -> {
                            newLine[nowLong[a].toInt()] = line.step[a]
                            line.step[a] = NOTE_NONE
                        }
                        NOTE_LEND -> {
                            newLine[nowLong[a].toInt()] = line.step[a]
                            noNote[nowLong[a].toInt()] = 2
                            nowLong[a] = stepWidth.toByte()
                            line.step[a] = NOTE_NONE
                        }
                    }
                }

                for (a in 0 until stepWidth) {
                    if (line.step[a] == NOTE_NOTE) {
                        while (true) {
                            val r = Random.nextInt(stepWidth)
                            if (newLine[r] == 0.toByte() && nowLong[a] == 255.toByte() && noNote[r] == 0.toByte()) {
                                newLine[r] = line.step[a]
                                break
                            }
                        }
                    }
                }

                for (a in 0 until stepWidth) {
                    line.step[a] = newLine[a]
                }

                for (a in 0 until stepWidth) {
                    if (nowLong[a].toInt() == stepWidth) {
                        nowLong[a] = 255.toByte()
                    }
                }
            }
        }
    }
}
