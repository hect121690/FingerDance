package com.fingerdance.ssc

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.fingerdance.*

private const val KEY_NONE = 0
private const val KEY_DOWN = 1
private const val KEY_PRESS = 2
private const val KEY_UP = 3

class InputProcessorSscHorizontalHD : InputAdapter() {

    private val btnOffPress = Texture(
        Gdx.files.external(
            "/FingerDance/Themes/$tema/GraphicsStatics/game_play/btn_off.png"
        )
    )

    private val btnOnPress = Texture(
        Gdx.files.external(
            "/FingerDance/Themes/$tema/GraphicsStatics/game_play/btn_on.png"
        )
    )

    // ---------------------------------------------------
    // 10 pads físicos
    // ---------------------------------------------------

    val getKeyBoard = IntArray(
        padPositionsHorizontalHD.size
    ) { KEY_NONE }

    // ---------------------------------------------------
    // estado lógico SSC
    // ---------------------------------------------------

    val logicalState = IntArray(10) { KEY_NONE }

    // ---------------------------------------------------
    // multitouch real
    // ---------------------------------------------------

    private val pointerToPadMap =
        mutableMapOf<Int, Int>()

    private val padPointers =
        Array(padPositionsHorizontalHD.size) {
            mutableSetOf<Int>()
        }

    private val wasPressed =
        BooleanArray(padPositionsHorizontalHD.size)

    // ---------------------------------------------------
    // KEYBOARD
    // ---------------------------------------------------

    private val keyToPadMap = mapOf(

        // izquierda
        Keys.NUMPAD_1 to 0,
        Keys.NUMPAD_7 to 1,
        Keys.NUMPAD_5 to 2,
        Keys.NUMPAD_9 to 3,
        Keys.NUMPAD_3 to 4,

        // derecha
        Keys.Z to 5,
        Keys.Q to 6,
        Keys.S to 7,
        Keys.E to 8,
        Keys.C to 9
    )

    // ---------------------------------------------------
    // KEY DOWN
    // ---------------------------------------------------

    override fun keyDown(keycode: Int): Boolean {

        keyToPadMap[keycode]?.let { pad ->

            padPointers[pad].add(-keycode)
        }

        return keyToPadMap.containsKey(keycode)
    }

    // ---------------------------------------------------
    // KEY UP
    // ---------------------------------------------------

    override fun keyUp(keycode: Int): Boolean {

        keyToPadMap[keycode]?.let { pad ->

            padPointers[pad].remove(-keycode)
        }

        return keyToPadMap.containsKey(keycode)
    }

    // ---------------------------------------------------
    // TOUCH DOWN
    // ---------------------------------------------------

    override fun touchDown(
        screenX: Int,
        screenY: Int,
        pointer: Int,
        button: Int
    ): Boolean {

        val pad = getPadIndex(screenX.toFloat(), screenY.toFloat()) ?: return false

        pointerToPadMap[pointer] = pad

        padPointers[pad].add(pointer)

        return true
    }

    // ---------------------------------------------------
    // TOUCH UP
    // ---------------------------------------------------

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

    // ---------------------------------------------------
    // TOUCH DRAGGED
    // ---------------------------------------------------

    override fun touchDragged(
        screenX: Int,
        screenY: Int,
        pointer: Int
    ): Boolean {

        val newPad =
            getPadIndex(
                screenX.toFloat(),
                screenY.toFloat()
            )

        val oldPad =
            pointerToPadMap[pointer]

        if (oldPad == newPad) {
            return true
        }

        oldPad?.let {

            padPointers[it].remove(pointer)
        }

        newPad?.let {

            pointerToPadMap[pointer] = it

            padPointers[it].add(pointer)

        } ?: run {

            pointerToPadMap.remove(pointer)
        }

        return true
    }

    // ---------------------------------------------------
    // TOUCH CANCEL
    // ---------------------------------------------------

    override fun touchCancelled(
        screenX: Int,
        screenY: Int,
        pointer: Int,
        button: Int
    ): Boolean {

        return touchUp(
            screenX,
            screenY,
            pointer,
            button
        )
    }

    // ---------------------------------------------------
    // PAD DETECTION
    // ---------------------------------------------------

    private fun getPadIndex(x: Float, y: Float): Int? {

        // ---------------------------------------------------
        // pads visibles reales
        // ---------------------------------------------------

        val visible = padPositionsHorizontalHD.indexOfFirst { pad ->
                x in pad[0]..(pad[0] + widthBtnsHorizontal) &&
                        y in pad[1]..(pad[1] + heightBtnsHorizontal)
            }

        if (visible >= 0) {
            return normalizePadIndex(visible)
        }

        // ---------------------------------------------------
        // touch areas diagonales HD
        // ---------------------------------------------------

        val extra = touchAreasHorizontalHD.indexOfFirst { area ->

                x in area[0]..(area[0] + (widthBtnsHorizontal / 2f)) &&
                        y in area[1]..(area[1] + heightBtnsHorizontal)
            }

        if (extra >= 0) {
            return areaToPadMapHD[extra]
        }

        // ---------------------------------------------------
        // fallback nearest
        // ---------------------------------------------------

        var minDist = Float.MAX_VALUE
        var nearest = -1

        for (i in padPositionsHorizontalHD.indices) {

            val pad =
                padPositionsHorizontalHD[i]

            val cx =
                pad[0] + widthBtnsHorizontal / 2f

            val cy =
                pad[1] + heightBtnsHorizontal / 2f

            val dx = x - cx
            val dy = y - cy

            val dist =
                dx * dx + dy * dy

            if (dist < minDist) {

                minDist = dist
                nearest = i
            }
        }

        return nearest.takeIf { it >= 0 }
    }

    // ---------------------------------------------------
    // UPDATE
    // ---------------------------------------------------

    private fun normalizePadIndex(index: Int): Int {

        return when (index) {

            0 -> 5
            1 -> 6

            8 -> 3
            9 -> 4

            else -> index
        }
    }

    fun update() {

        for (i in logicalState.indices) {

            logicalState[i] = KEY_NONE
        }

        for (i in padPositionsHorizontalHD.indices) {

            val pressedNow =
                padPointers[i].isNotEmpty()

            val state = when {

                pressedNow && !wasPressed[i] ->
                    KEY_DOWN

                pressedNow && wasPressed[i] ->
                    KEY_PRESS

                !pressedNow && wasPressed[i] ->
                    KEY_UP

                else ->
                    KEY_NONE
            }

            getKeyBoard[i] = state

            logicalState[i] = state

            wasPressed[i] = pressedNow
        }
    }

    // ---------------------------------------------------
    // HELPERS
    // ---------------------------------------------------

    fun isPadPressed(index: Int): Boolean {

        return getKeyBoard.getOrNull(index) == KEY_DOWN ||
                getKeyBoard.getOrNull(index) == KEY_PRESS
    }

    fun getPressedPads(): List<Int> {

        val result = mutableListOf<Int>()

        for (i in getKeyBoard.indices) {

            if (
                getKeyBoard[i] == KEY_DOWN ||
                getKeyBoard[i] == KEY_PRESS
            ) {

                result.add(i)
            }
        }

        return result
    }

    // ---------------------------------------------------
    // RENDER
    // ---------------------------------------------------

    fun render(batch: SpriteBatch) {

        for (i in padPositionsHorizontalHD.indices) {

            val (x, y) =
                padPositionsHorizontalHD[i]

            val texture =
                if (
                    getKeyBoard[i] == KEY_DOWN ||
                    getKeyBoard[i] == KEY_PRESS
                ) {
                    btnOnPress
                } else {
                    btnOffPress
                }

            batch.draw(
                texture,
                x,
                y,
                widthBtnsHorizontal,
                heightBtnsHorizontal
            )
        }
    }

    // ---------------------------------------------------
    // RESET
    // ---------------------------------------------------

    fun resetState() {

        for (i in padPointers.indices) {

            padPointers[i].clear()

            wasPressed[i] = false

            getKeyBoard[i] = KEY_NONE
        }

        for (i in logicalState.indices) {

            logicalState[i] = KEY_NONE
        }

        pointerToPadMap.clear()
    }

    // ---------------------------------------------------
    // DISPOSE
    // ---------------------------------------------------

    fun dispose() {

        btnOffPress.dispose()
        btnOnPress.dispose()
    }
}