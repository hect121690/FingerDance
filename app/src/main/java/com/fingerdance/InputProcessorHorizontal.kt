package com.fingerdance

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch

private const val KEY_NONE = 0
private const val KEY_DOWN = 1
private const val KEY_PRESS = 2
private const val KEY_UP = 3

class InputProcessorHorizontal : InputAdapter() {

    private val btnOffPress = Texture(Gdx.files.external("/FingerDance/Themes/$tema/GraphicsStatics/game_play/btn_off.png"))
    private val btnOnPress = Texture(Gdx.files.external("/FingerDance/Themes/$tema/GraphicsStatics/game_play/btn_on.png"))

    val getKeyBoard = IntArray(padPositionsHorizontal.size) { KEY_NONE }

    // 🔥 NUEVO: estado lógico (solo 5)
    val logicalState = IntArray(5) { KEY_NONE }

    private var pointerToPadMap = mutableMapOf<Int, Int>()
    private var hasStateChanged = false

    private val keyToPadMap = mapOf(
        Input.Keys.NUMPAD_1 to 0,
        Input.Keys.NUMPAD_7 to 1,
        Input.Keys.NUMPAD_5 to 2,
        Input.Keys.NUMPAD_9 to 3,
        Input.Keys.NUMPAD_3 to 4,
        Input.Keys.Z to 5,
        Input.Keys.Q to 6,
        Input.Keys.S to 7,
        Input.Keys.E to 8,
        Input.Keys.C to 9
    )

    // 🔥 UTILIDADES CLAVE
    fun getLogicalPad(padIndex: Int): Int = padIndex % 5
    fun isRightSide(padIndex: Int): Boolean = padIndex >= 5

    override fun keyDown(keycode: Int): Boolean {
        keyToPadMap[keycode]?.let { padIndex ->
            getKeyBoard[padIndex] = KEY_DOWN

            val logical = getLogicalPad(padIndex)
            logicalState[logical] = KEY_DOWN

            hasStateChanged = true
        }
        return keyToPadMap.containsKey(keycode)
    }

    override fun keyUp(keycode: Int): Boolean {
        keyToPadMap[keycode]?.let { padIndex ->
            getKeyBoard[padIndex] = KEY_UP

            val logical = getLogicalPad(padIndex)
            logicalState[logical] = KEY_UP

            hasStateChanged = true
        }
        return keyToPadMap.containsKey(keycode)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val padIndex = getPadIndex(screenX.toFloat(), screenY.toFloat())
        padIndex?.let {
            getKeyBoard[it] = KEY_DOWN

            val logical = getLogicalPad(it)
            logicalState[logical] = KEY_DOWN

            pointerToPadMap[pointer] = it
            hasStateChanged = true
        }
        return padIndex != null
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        pointerToPadMap[pointer]?.let { previousPad ->
            getKeyBoard[previousPad] = KEY_UP

            val logical = getLogicalPad(previousPad)
            logicalState[logical] = KEY_UP

            hasStateChanged = true
        }

        pointerToPadMap.remove(pointer)
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        val newPad = getPadIndex(screenX.toFloat(), screenY.toFloat())
        val oldPad = pointerToPadMap[pointer]

        if (oldPad == newPad) return true

        oldPad?.let {
            getKeyBoard[it] = KEY_UP
            logicalState[getLogicalPad(it)] = KEY_UP
        }

        newPad?.let {
            getKeyBoard[it] = KEY_DOWN
            logicalState[getLogicalPad(it)] = KEY_DOWN
            pointerToPadMap[pointer] = it
        }

        hasStateChanged = true
        return true
    }

    fun getPhysicalPadsForLogical(logical: Int): List<Int> {
        val result = mutableListOf<Int>()

        for (i in getKeyBoard.indices) { // 0–9
            if ((getKeyBoard[i] == KEY_DOWN || getKeyBoard[i] == KEY_PRESS) &&
                i % 5 == logical) {

                result.add(i)
            }
        }

        return result
    }

    private fun getPadIndex(x: Float, y: Float): Int? {
        val index = padPositionsHorizontal.indexOfFirst { pad ->
            x in pad[0]..(pad[0] + widthBtnsHorizontal) &&
                    y in pad[1]..(pad[1] + heightBtnsHorizontal)
        }
        return index.takeIf { it >= 0 }
    }

    fun update() {
        if (!hasStateChanged) return

        // 🔹 físicos
        for (i in getKeyBoard.indices) {
            when (getKeyBoard[i]) {
                KEY_DOWN -> getKeyBoard[i] = KEY_PRESS
                KEY_UP -> getKeyBoard[i] = KEY_NONE
            }
        }

        // 🔹 lógicos
        for (i in logicalState.indices) {
            when (logicalState[i]) {
                KEY_DOWN -> logicalState[i] = KEY_PRESS
                KEY_UP -> logicalState[i] = KEY_NONE
            }
        }

        hasStateChanged = false
    }

    fun render(batch: SpriteBatch) {
        for (i in padPositionsHorizontal.indices) {
            val (x, y) = padPositionsHorizontal[i]
            val texture = if (getKeyBoard[i] == KEY_DOWN || getKeyBoard[i] == KEY_PRESS) {
                btnOnPress
            } else {
                btnOffPress
            }
            batch.draw(texture, x, y, widthBtnsHorizontal, heightBtnsHorizontal)
        }
    }

    fun resetState() {
        for (i in getKeyBoard.indices) {
            getKeyBoard[i] = KEY_NONE
        }
        pointerToPadMap.clear()
        hasStateChanged = true
    }

    fun dispose() {
        btnOffPress.dispose()
        btnOnPress.dispose()
    }
}