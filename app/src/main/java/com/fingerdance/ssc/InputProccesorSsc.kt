package com.fingerdance.ssc

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.fingerdance.heightBtns
import com.fingerdance.padPositions
import com.fingerdance.tema
import com.fingerdance.touchAreas
import com.fingerdance.widthBtns

private const val KEY_NONE = 0
private const val KEY_DOWN = 1
private const val KEY_PRESS = 2
private const val KEY_UP = 3

class InputProcessorSsc : InputAdapter() {

    private val btnOffPress = Texture(Gdx.files.external("/FingerDance/Themes/$tema/GraphicsStatics/game_play/btn_off.png"))
    private val btnOnPress = Texture(Gdx.files.external("/FingerDance/Themes/$tema/GraphicsStatics/game_play/btn_on.png"))

    val getKeyBoard = IntArray(padPositions.size) { KEY_NONE }

    // pointer -> pad
    private val pointerToPadMap = mutableMapOf<Int, Int>()

    // pad -> set of pointers
    private val padPointers = Array(padPositions.size) { mutableSetOf<Int>() }

    private var hasStateChanged = false

    private val keyToPadMap = mapOf(
        Keys.NUMPAD_1 to 0,
        Keys.NUMPAD_7 to 1,
        Keys.NUMPAD_5 to 2,
        Keys.NUMPAD_9 to 3,
        Keys.NUMPAD_3 to 4,
        Keys.Z to 0,
        Keys.Q to 1,
        Keys.S to 2,
        Keys.E to 3,
        Keys.C to 4
    )

    // ---------------- KEYBOARD ----------------

    override fun keyDown(keycode: Int): Boolean {
        keyToPadMap[keycode]?.let { pad ->
            getKeyBoard[pad] = KEY_DOWN
            hasStateChanged = true
        }
        return keyToPadMap.containsKey(keycode)
    }

    override fun keyUp(keycode: Int): Boolean {
        keyToPadMap[keycode]?.let { pad ->
            getKeyBoard[pad] = KEY_UP
            hasStateChanged = true
        }
        return keyToPadMap.containsKey(keycode)
    }

    // ---------------- TOUCH ----------------

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val pad = getPadIndex(screenX.toFloat(), screenY.toFloat()) ?: return false

        pointerToPadMap[pointer] = pad
        padPointers[pad].add(pointer)

        // solo dispara DOWN si era el primer dedo
        if (padPointers[pad].size == 1) {
            getKeyBoard[pad] = KEY_DOWN
            hasStateChanged = true
        }

        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {

        val pad = pointerToPadMap[pointer]

        if (pad != null) {
            pointerToPadMap.remove(pointer)
            padPointers[pad].remove(pointer)

            if (padPointers[pad].isEmpty()) {
                getKeyBoard[pad] = KEY_UP
                hasStateChanged = true
            }
        } else {
            // 🔥 FIX: limpiar por seguridad
            for (i in padPointers.indices) {
                if (padPointers[i].remove(pointer)) {
                    if (padPointers[i].isEmpty()) {
                        getKeyBoard[i] = KEY_UP
                        hasStateChanged = true
                    }
                }
            }
        }

        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        val newPad = getPadIndex(screenX.toFloat(), screenY.toFloat())
        val oldPad = pointerToPadMap[pointer]

        if (oldPad == newPad) return true

        // salir del pad anterior
        if (oldPad != null) {
            padPointers[oldPad].remove(pointer)

            if (padPointers[oldPad].isEmpty()) {
                getKeyBoard[oldPad] = KEY_UP
                hasStateChanged = true
            }
        }

        // entrar al nuevo pad
        if (newPad != null) {
            pointerToPadMap[pointer] = newPad
            padPointers[newPad].add(pointer)

            if (padPointers[newPad].size == 1) {
                getKeyBoard[newPad] = KEY_DOWN
                hasStateChanged = true
            }
        } else {
            pointerToPadMap.remove(pointer)
        }

        return true
    }

    // ---------------- PAD DETECTION ----------------

    private fun getPadIndex(x: Float, y: Float): Int? {

        // 1. pads principales (rectángulos)
        val main = padPositions.indexOfFirst { pad ->
            x in pad[0]..(pad[0] + widthBtns) &&
                    y in pad[1]..(pad[1] + heightBtns)
        }
        if (main >= 0) return main

        // 2. áreas extendidas (tu rombo)
        val extra = touchAreas.indexOfFirst { area ->
            x in area[0]..(area[0] + (widthBtns / 2)) &&
                    y in area[1]..(area[1] + heightBtns)
        }

        return when (extra) {
            0 -> 0 // leftDown
            1 -> 4 // rightDown
            2 -> 1 // leftUp
            3 -> 3 // rightUp
            else -> null
        }
    }

    // ---------------- UPDATE ----------------

    fun update() {
        if (!hasStateChanged) return

        for (i in getKeyBoard.indices) {
            when (getKeyBoard[i]) {
                KEY_DOWN -> getKeyBoard[i] = KEY_PRESS
                KEY_UP -> getKeyBoard[i] = KEY_NONE
            }
        }

        hasStateChanged = false
    }

    // ---------------- RENDER ----------------

    fun render(batch: SpriteBatch) {
        for (i in padPositions.indices) {
            val (x, y) = padPositions[i]

            val texture = if (getKeyBoard[i] == KEY_DOWN || getKeyBoard[i] == KEY_PRESS) {
                btnOnPress
            } else {
                btnOffPress
            }

            batch.draw(texture, x, y, widthBtns, heightBtns)
        }
    }

    fun resetState() {
        for (i in getKeyBoard.indices) {
            getKeyBoard[i] = KEY_NONE
            padPointers[i].clear()
        }
        pointerToPadMap.clear()
        hasStateChanged = true
    }

    fun dispose() {
        btnOffPress.dispose()
        btnOnPress.dispose()
    }
}

