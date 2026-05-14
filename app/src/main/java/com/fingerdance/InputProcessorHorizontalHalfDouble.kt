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

class InputProcessorHorizontalHalfDouble : InputAdapter() {

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
    // ESTADO REAL FÍSICO (0..9)
    // ---------------------------------------------------

    val getKeyBoard = IntArray(
        padPositionsHorizontalHD.size
    ) { KEY_NONE }

    // pointer -> pad físico
    private val pointerToPadMap =
        mutableMapOf<Int, Int>()

    private var hasStateChanged = false

    // último pad presionado
    private var lastPressedPad: Int? = null

    // ---------------------------------------------------
    // KEYBOARD
    // ---------------------------------------------------

    private val keyToPadMap = mapOf(

        // izquierda
        Input.Keys.NUMPAD_1 to 0,
        Input.Keys.NUMPAD_7 to 1,
        Input.Keys.NUMPAD_5 to 2,
        Input.Keys.NUMPAD_9 to 3,
        Input.Keys.NUMPAD_3 to 4,

        // derecha
        Input.Keys.Z to 5,
        Input.Keys.Q to 6,
        Input.Keys.S to 7,
        Input.Keys.E to 8,
        Input.Keys.C to 9
    )

    // ---------------------------------------------------
    // KEY DOWN
    // ---------------------------------------------------

    override fun keyDown(keycode: Int): Boolean {

        keyToPadMap[keycode]?.let { padIndex ->

            getKeyBoard[padIndex] = KEY_DOWN

            lastPressedPad = padIndex

            hasStateChanged = true
        }

        return keyToPadMap.containsKey(keycode)
    }

    // ---------------------------------------------------
    // KEY UP
    // ---------------------------------------------------

    override fun keyUp(keycode: Int): Boolean {

        keyToPadMap[keycode]?.let { padIndex ->

            getKeyBoard[padIndex] = KEY_UP

            hasStateChanged = true
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

        val padIndex =
            getPadIndex(
                screenX.toFloat(),
                screenY.toFloat()
            )

        padIndex?.let {

            getKeyBoard[it] = KEY_DOWN

            lastPressedPad = it

            pointerToPadMap[pointer] = it

            hasStateChanged = true
        }

        return padIndex != null
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

        pointerToPadMap[pointer]?.let { oldPad ->

            getKeyBoard[oldPad] = KEY_UP

            hasStateChanged = true
        }

        pointerToPadMap.remove(pointer)

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

        // liberar pad anterior
        oldPad?.let {

            getKeyBoard[it] = KEY_UP
        }

        // nuevo pad
        newPad?.let {

            getKeyBoard[it] = KEY_DOWN

            lastPressedPad = it

            pointerToPadMap[pointer] = it
        } ?: run {

            pointerToPadMap.remove(pointer)
        }

        hasStateChanged = true

        return true
    }

    // ---------------------------------------------------
    // GET PAD INDEX
    // ---------------------------------------------------

    private fun getPadIndex(
        x: Float,
        y: Float
    ): Int? {

        // prioridad absoluta:
        // hitbox real
        val visible =
            padPositionsHorizontalHD.indexOfFirst { pad ->

                x in pad[0]..(pad[0] + widthBtnsHorizontal) &&
                        y in pad[1]..(pad[1] + heightBtnsHorizontal)
            }

        if (visible >= 0) {
            return normalizePadIndex(visible)
        }

        // fallback arcade:
        // nearest pad
        var minDist = Float.MAX_VALUE
        var nearestIndex = -1

        for (i in padPositionsHorizontalHD.indices) {

            val pad = padPositionsHorizontalHD[i]

            val centerX =
                pad[0] + (widthBtnsHorizontal / 2f)

            val centerY =
                pad[1] + (heightBtnsHorizontal / 2f)

            val dx = x - centerX
            val dy = y - centerY

            val dist =
                dx * dx + dy * dy

            if (dist < minDist) {

                minDist = dist
                nearestIndex = i
            }
        }

        return nearestIndex.takeIf { it >= 0 }
    }

    private fun normalizePadIndex(index: Int): Int {

        return when (index) {

            0 -> 5
            1 -> 6

            8 -> 3
            9 -> 4

            else -> index
        }
    }

    // ---------------------------------------------------
    // UPDATE
    // ---------------------------------------------------

    fun update() {

        if (!hasStateChanged) {
            return
        }

        for (i in getKeyBoard.indices) {

            when (getKeyBoard[i]) {

                KEY_DOWN -> {
                    getKeyBoard[i] = KEY_PRESS
                }

                KEY_UP -> {
                    getKeyBoard[i] = KEY_NONE
                }
            }
        }

        hasStateChanged = false
    }

    // ---------------------------------------------------
    // HELPERS
    // ---------------------------------------------------

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

    fun getLastPressedPad(): Int? {

        return lastPressedPad
    }

    fun isPadPressed(index: Int): Boolean {

        return getKeyBoard.getOrNull(index) == KEY_DOWN ||
                getKeyBoard.getOrNull(index) == KEY_PRESS
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

        for (i in getKeyBoard.indices) {

            getKeyBoard[i] = KEY_NONE
        }

        pointerToPadMap.clear()

        lastPressedPad = null

        hasStateChanged = true
    }

    // ---------------------------------------------------
    // DISPOSE
    // ---------------------------------------------------

    fun dispose() {

        btnOffPress.dispose()
        btnOnPress.dispose()
    }
}