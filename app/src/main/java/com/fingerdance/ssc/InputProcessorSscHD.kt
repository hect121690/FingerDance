package com.fingerdance.ssc

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.fingerdance.*

private const val KEY_NONE = 0
private const val KEY_DOWN = 1
private const val KEY_PRESS = 2
private const val KEY_UP = 3

class InputProcessorSscHD : InputAdapter() {

    private val btnOffPress = Texture(
        Gdx.files.external("/FingerDance/Themes/$tema/GraphicsStatics/game_play/btn_off.png")
    )

    private val btnOnPress = Texture(
        Gdx.files.external("/FingerDance/Themes/$tema/GraphicsStatics/game_play/btn_on.png")
    )

    val getKeyBoard = IntArray(10) { KEY_NONE }

    // multitouch real
    private val pointerToPadMap = mutableMapOf<Int, Int>()
    private val padPointers = Array(10) { mutableSetOf<Int>() }

    private val wasPressed = BooleanArray(10)

    private val keyToPadMap = mapOf(
        Input.Keys.A to 2,
        Input.Keys.W to 3,
        Input.Keys.Z to 4,
        Input.Keys.X to 5,
        Input.Keys.E to 6,
        Input.Keys.D to 7
    )

    // ---------------------------------------------------
    // KEYBOARD
    // ---------------------------------------------------

    override fun keyDown(keycode: Int): Boolean {

        keyToPadMap[keycode]?.let { pad ->
            padPointers[pad].add(-keycode)
        }

        return keyToPadMap.containsKey(keycode)
    }

    override fun keyUp(keycode: Int): Boolean {

        keyToPadMap[keycode]?.let { pad ->
            padPointers[pad].remove(-keycode)
        }

        return keyToPadMap.containsKey(keycode)
    }

    // ---------------------------------------------------
    // TOUCH
    // ---------------------------------------------------

    override fun touchDown(
        screenX: Int,
        screenY: Int,
        pointer: Int,
        button: Int
    ): Boolean {

        val pad = getPadIndex(screenX.toFloat(), screenY.toFloat())
            ?: return false

        pointerToPadMap[pointer] = pad
        padPointers[pad].add(pointer)

        return true
    }

    override fun touchUp(
        screenX: Int,
        screenY: Int,
        pointer: Int,
        button: Int
    ): Boolean {

        pointerToPadMap.remove(pointer)

        for (i in padPointers.indices) {
            padPointers[i].remove(pointer)
        }

        return true
    }

    override fun touchDragged(
        screenX: Int,
        screenY: Int,
        pointer: Int
    ): Boolean {

        val newPad = getPadIndex(screenX.toFloat(), screenY.toFloat())
        val oldPad = pointerToPadMap[pointer]

        if (oldPad == newPad) return true

        if (oldPad != null) {
            padPointers[oldPad].remove(pointer)
        }

        if (newPad != null) {
            pointerToPadMap[pointer] = newPad
            padPointers[newPad].add(pointer)
        } else {
            pointerToPadMap.remove(pointer)
        }

        return true
    }

    override fun touchCancelled(
        screenX: Int,
        screenY: Int,
        pointer: Int,
        button: Int
    ): Boolean {

        return touchUp(screenX, screenY, pointer, button)
    }

    // ---------------------------------------------------
    // PAD DETECTION
    // ---------------------------------------------------

    private fun getPadIndex(x: Float, y: Float): Int? {

        val visiblePadIndex = padPositionsHD.indexOfFirst { pad ->

            !(pad[0] == 0f && pad[1] == 0f) &&
                    x in pad[0]..(pad[0] + colWidth) &&
                    y in pad[1]..(pad[1] + heightBtns)
        }

        return visiblePadIndex.takeIf { it >= 0 }
    }

    // ---------------------------------------------------
    // UPDATE
    // ---------------------------------------------------

    fun update() {

        for (i in 0 until 10) {

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

    // ---------------------------------------------------
    // RENDER
    // ---------------------------------------------------

    fun render(batch: SpriteBatch) {

        for (i in 2..7) {

            val (x, y) = padPositionsHD[i]

            val texture =
                if (getKeyBoard[i] == KEY_DOWN ||
                    getKeyBoard[i] == KEY_PRESS
                ) {
                    btnOnPress
                } else {
                    btnOffPress
                }

            batch.draw(texture, x, y, colWidth, heightBtns)
        }
    }

    // ---------------------------------------------------
    // RESET
    // ---------------------------------------------------

    fun resetState() {

        for (i in 0 until 10) {
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