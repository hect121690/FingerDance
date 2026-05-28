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

    // 🔥 fuente de verdad
    private val pointerToPadMap = mutableMapOf<Int, Int>()
    private val padPointers = Array(padPositions.size) { mutableSetOf<Int>() }

    // 🔥 estado anterior (para transiciones)
    private val wasPressed = BooleanArray(padPositions.size)

    // ---------------- KEYBOARD ----------------

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

    override fun keyDown(keycode: Int): Boolean {
        keyToPadMap[keycode]?.let { pad ->
            padPointers[pad].add(-keycode) // IDs negativos = teclado
        }
        return keyToPadMap.containsKey(keycode)
    }

    override fun keyUp(keycode: Int): Boolean {
        keyToPadMap[keycode]?.let { pad ->
            padPointers[pad].remove(-keycode)
        }
        return keyToPadMap.containsKey(keycode)
    }

    // ---------------- TOUCH ----------------

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val pad = getPadIndex(screenX.toFloat(), screenY.toFloat()) ?: return false

        pointerToPadMap[pointer] = pad
        padPointers[pad].add(pointer)

        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        pointerToPadMap.remove(pointer)
        for (i in padPointers.indices) {
            padPointers[i].remove(pointer)
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
        }

        // entrar al nuevo
        if (newPad != null) {
            pointerToPadMap[pointer] = newPad
            padPointers[newPad].add(pointer)
        } else {
            pointerToPadMap.remove(pointer)
        }

        return true
    }

    override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return touchUp(screenX, screenY, pointer, button)
    }

    // ---------------- PAD DETECTION ----------------

    private fun getPadIndex(x: Float, y: Float): Int? {

        val main = padPositions.indexOfFirst { pad ->
            x in pad[0]..(pad[0] + widthBtns) &&
                    y in pad[1]..(pad[1] + heightBtns)
        }
        if (main >= 0) return main

        val extra = touchAreas.indexOfFirst { area ->
            x in area[0]..(area[0] + (widthBtns / 2)) &&
                    y in area[1]..(area[1] + heightBtns)
        }

        return when (extra) {
            0 -> 0
            1 -> 4
            2 -> 1
            3 -> 3
            else -> null
        }
    }

    // ---------------- UPDATE (🔥 CLAVE) ----------------

    fun update() {
        val activePointers = mutableSetOf<Int>()

        for (i in 0 until 20) {

            if (Gdx.input.isTouched(i)) {
                activePointers.add(i)
            }
        }
        pointerToPadMap.keys.toList().forEach { pointer ->

            if (
                pointer >= 0 &&
                pointer !in activePointers
            ) {

                pointerToPadMap.remove(pointer)

                for (i in padPointers.indices) {
                    padPointers[i].remove(pointer)
                }
            }
        }

        for (i in padPositions.indices) {

            val pressedNow = padPointers[i].isNotEmpty()

            getKeyBoard[i] = when {
                pressedNow && !wasPressed[i] -> KEY_DOWN
                pressedNow && wasPressed[i] -> KEY_PRESS
                !pressedNow && wasPressed[i] -> KEY_UP
                else -> KEY_NONE
            }

            wasPressed[i] = pressedNow
        }
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

    // ---------------- RESET ----------------

    fun resetState() {
        for (i in padPointers.indices) {
            padPointers[i].clear()
            wasPressed[i] = false
            getKeyBoard[i] = KEY_NONE
        }
        pointerToPadMap.clear()
    }

    fun dispose() {
        btnOffPress.dispose()
        btnOnPress.dispose()
    }
}

