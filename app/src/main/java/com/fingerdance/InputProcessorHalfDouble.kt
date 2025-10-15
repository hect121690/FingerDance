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

class InputProcessorHalfDouble : InputAdapter() {
    private val btnOffPress = Texture(Gdx.files.external("/FingerDance/Themes/$tema/GraphicsStatics/game_play/btn_off.png"))
    private val btnOnPress = Texture(Gdx.files.external("/FingerDance/Themes/$tema/GraphicsStatics/game_play/btn_on.png"))

    val getKeyBoard = IntArray(10) { KEY_NONE }
    private var pointerToPadMap = mutableMapOf<Int, Int>()
    private var hasStateChanged = false

    // Map physical keys to pad positions
    private val keyToPadMap = mapOf(
        Input.Keys.A to 2,
        Input.Keys.W to 3,
        Input.Keys.Z to 4,
        Input.Keys.X to 5,
        Input.Keys.E to 6,
        Input.Keys.D to 7
    )

    override fun keyDown(keycode: Int): Boolean {
        keyToPadMap[keycode]?.let { padIndex ->
            getKeyBoard[padIndex] = KEY_DOWN
            hasStateChanged = true
        }
        return keyToPadMap.containsKey(keycode)
    }

    override fun keyUp(keycode: Int): Boolean {
        keyToPadMap[keycode]?.let { padIndex ->
            getKeyBoard[padIndex] = KEY_UP
            hasStateChanged = true
        }
        return keyToPadMap.containsKey(keycode)
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val padIndex = getPadIndex(screenX.toFloat(), screenY.toFloat())
        padIndex?.let {
            getKeyBoard[it] = KEY_DOWN
            pointerToPadMap[pointer] = it
            hasStateChanged = true
        }
        return padIndex != null
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val padIndex = getPadIndex(screenX.toFloat(), screenY.toFloat())
        padIndex?.let {
            getKeyBoard[it] = KEY_UP
            pointerToPadMap.remove(pointer)
            hasStateChanged = true
        }
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        val padIndex = getPadIndex(screenX.toFloat(), screenY.toFloat())

        if (pointerToPadMap[pointer] == padIndex) return true

        pointerToPadMap[pointer]?.let { previousPad ->
            getKeyBoard[previousPad] = KEY_NONE
        }

        padIndex?.let {
            getKeyBoard[it] = KEY_DOWN
            pointerToPadMap[pointer] = it
            hasStateChanged = true
        }

        return true
    }

    private fun getPadIndex(x: Float, y: Float): Int? {
        val visiblePadIndex = padPositionsHD.indexOfFirst { pad ->
            // ignorar pads invisibles (0,0)
            !(pad[0] == 0f && pad[1] == 0f) &&
                    x in pad[0]..(pad[0] + colWidth) &&
                    y in pad[1]..(pad[1] + heightBtns)
        }.takeIf { it >= 0 }

        return visiblePadIndex
    }

    fun update() {
        if (!hasStateChanged) return
        for (i in 2..7) {
            when (getKeyBoard[i]) {
                KEY_DOWN -> getKeyBoard[i] = KEY_PRESS
                KEY_UP -> getKeyBoard[i] = KEY_NONE
            }
        }
        hasStateChanged = false
    }

    fun render(batch: SpriteBatch) {
        for (i in 2..7) {
            val (x, y) = padPositionsHD[i]
            val texture = if (getKeyBoard[i] == KEY_DOWN || getKeyBoard[i] == KEY_PRESS) {
                btnOnPress
            } else {
                btnOffPress
            }
            batch.draw(texture, x, y, colWidth, heightBtns)
        }
    }

    fun resetState() {
        for (i in 2..7) {
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