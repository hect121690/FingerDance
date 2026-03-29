package com.fingerdance.ssc

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.fingerdance.heightBtns
import com.fingerdance.padPositions
import com.fingerdance.widthBtns
import kotlin.collections.set

class InputProcessorSsc(
    private val getTimeMs: () -> Double,
    private val callback: (column: Int, isDown: Boolean, timeMs: Double) -> Unit
) : InputAdapter() {

    private val pressed = BooleanArray(5)
    private val pointerToPadMap = mutableMapOf<Int, Int>()

    private val keyToPadMap = mapOf(
        Input.Keys.NUMPAD_1 to 0,
        Input.Keys.NUMPAD_7 to 1,
        Input.Keys.NUMPAD_5 to 2,
        Input.Keys.NUMPAD_9 to 3,
        Input.Keys.NUMPAD_3 to 4,
        Input.Keys.Z to 0,
        Input.Keys.Q to 1,
        Input.Keys.S to 2,
        Input.Keys.E to 3,
        Input.Keys.C to 4
    )

    // =========================
    // KEYBOARD
    // =========================

    override fun keyDown(keycode: Int): Boolean {
        keyToPadMap[keycode]?.let { col ->
            if (!pressed[col]) {
                pressed[col] = true
                callback(col, true, getTimeMs())
            }
        }
        return keyToPadMap.containsKey(keycode)
    }

    override fun keyUp(keycode: Int): Boolean {
        keyToPadMap[keycode]?.let { col ->
            pressed[col] = false
            callback(col, false, getTimeMs())
        }
        return keyToPadMap.containsKey(keycode)
    }

    // =========================
    // TOUCH (MULTITOUCH)
    // =========================

    override fun touchDown(x: Int, y: Int, pointer: Int, button: Int): Boolean {
        val col = getPadIndex(x.toFloat(), y.toFloat()) ?: return false

        if (!pressed[col]) {
            pressed[col] = true
            pointerToPadMap[pointer] = col
            callback(col, true, getTimeMs())
        }

        return true
    }

    override fun touchUp(x: Int, y: Int, pointer: Int, button: Int): Boolean {
        val col = pointerToPadMap[pointer] ?: return true

        pressed[col] = false
        pointerToPadMap.remove(pointer)
        callback(col, false, getTimeMs())

        return true
    }

    override fun touchDragged(x: Int, y: Int, pointer: Int): Boolean {
        val newCol = getPadIndex(x.toFloat(), y.toFloat())
        val oldCol = pointerToPadMap[pointer]

        if (newCol == oldCol) return true

        // release anterior
        oldCol?.let {
            pressed[it] = false
            callback(it, false, getTimeMs())
        }

        // presionar nuevo
        newCol?.let {
            pressed[it] = true
            pointerToPadMap[pointer] = it
            callback(it, true, getTimeMs())
        }

        return true
    }

    // =========================
    // HELPER
    // =========================

    private fun getPadIndex(x: Float, y: Float): Int? {
        val idx = padPositions.indexOfFirst { pad ->
            x in pad[0]..(pad[0] + widthBtns) && y in pad[1]..(pad[1] + heightBtns)
        }
        return if (idx >= 0) idx else null
    }

    fun isHeld(column: Int): Boolean = pressed[column]
}