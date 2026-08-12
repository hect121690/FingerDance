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

private val TOUCH_RADIUS = widthBtnsHorizontal * MULTIPLER_TOUCH_RADIUS

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

    val getKeyBoard = IntArray(padPositionsHorizontalHD.size) { KEY_NONE }
    val logicalState = IntArray(10) { KEY_NONE }

    private val pointerToPadsMap = mutableMapOf<Int, Set<Int>>()
    private val padPointers = Array(padPositionsHorizontalHD.size) { mutableSetOf<Int>() }
    private val wasPressed = BooleanArray(padPositionsHorizontalHD.size)

    private val keyToPadMap = mapOf(
        Keys.NUMPAD_1 to 0,
        Keys.NUMPAD_7 to 1,
        Keys.NUMPAD_5 to 2,
        Keys.NUMPAD_9 to 3,
        Keys.NUMPAD_3 to 4,
        Keys.Z to 5,
        Keys.Q to 6,
        Keys.S to 7,
        Keys.E to 8,
        Keys.C to 9
    )

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

    override fun touchDown(
        screenX: Int,
        screenY: Int,
        pointer: Int,
        button: Int
    ): Boolean {
        val pads = getPadIndices(screenX.toFloat(), screenY.toFloat())
        if (pads.isEmpty()) return false

        pointerToPadsMap[pointer] = pads

        pads.forEach { pad ->
            padPointers[pad].add(pointer)
        }

        return true
    }

    override fun touchUp(
        screenX: Int,
        screenY: Int,
        pointer: Int,
        button: Int
    ): Boolean {
        pointerToPadsMap.remove(pointer)

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
        val newPads = getPadIndices(screenX.toFloat(), screenY.toFloat())
        if (newPads.isEmpty()) return true

        val oldPads = pointerToPadsMap[pointer].orEmpty()

        if (oldPads == newPads) return true

        oldPads.forEach { oldPad ->
            padPointers[oldPad].remove(pointer)
        }

        newPads.forEach { newPad ->
            padPointers[newPad].add(pointer)
        }

        pointerToPadsMap[pointer] = newPads

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

    private fun getPadIndices(x: Float, y: Float): Set<Int> {
        val result = mutableSetOf<Int>()
        val radiusSq = TOUCH_RADIUS * TOUCH_RADIUS

        for (i in padPositionsHorizontalHD.indices) {
            val pad = padPositionsHorizontalHD[i]

            val left = pad[0]
            val top = pad[1]
            val right = left + widthBtnsHorizontal
            val bottom = top + heightBtnsHorizontal

            val closestX = x.coerceIn(left, right)
            val closestY = y.coerceIn(top, bottom)

            val dx = x - closestX
            val dy = y - closestY
            val distanceSq = dx * dx + dy * dy

            if (distanceSq <= radiusSq) {
                result.add(normalizePadIndex(i))
            }
        }

        for (i in touchAreasHorizontalHD.indices) {
            val area = touchAreasHorizontalHD[i]

            val left = area[0]
            val top = area[1]
            val right = left + (widthBtnsHorizontal / 2f)
            val bottom = top + heightBtnsHorizontal

            val closestX = x.coerceIn(left, right)
            val closestY = y.coerceIn(top, bottom)

            val dx = x - closestX
            val dy = y - closestY
            val distanceSq = dx * dx + dy * dy

            if (distanceSq <= radiusSq) {
                areaToPadMapHD[i]?.let { mappedPad ->
                    result.add(mappedPad)
                }
            }
        }

        return result
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

    fun update() {
        val activePointers = mutableSetOf<Int>()

        for (i in 0 until 20) {
            if (Gdx.input.isTouched(i)) {
                activePointers.add(i)
            }
        }

        pointerToPadsMap.keys.toList().forEach { pointer ->
            if (pointer >= 0 && pointer !in activePointers) {
                pointerToPadsMap.remove(pointer)

                for (i in padPointers.indices) {
                    padPointers[i].remove(pointer)
                }
            }
        }

        for (i in logicalState.indices) {
            logicalState[i] = KEY_NONE
        }

        for (i in padPositionsHorizontalHD.indices) {
            val pressedNow = padPointers[i].isNotEmpty()

            val state = when {
                pressedNow && !wasPressed[i] -> KEY_DOWN
                pressedNow && wasPressed[i] -> KEY_PRESS
                !pressedNow && wasPressed[i] -> KEY_UP
                else -> KEY_NONE
            }

            getKeyBoard[i] = state
            logicalState[i] = state
            wasPressed[i] = pressedNow
        }
    }

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

    fun render(batch: SpriteBatch) {
        for (i in padPositionsHorizontalHD.indices) {
            val (x, y) = padPositionsHorizontalHD[i]

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

    fun resetState() {
        for (i in padPointers.indices) {
            padPointers[i].clear()
            wasPressed[i] = false
            getKeyBoard[i] = KEY_NONE
        }

        for (i in logicalState.indices) {
            logicalState[i] = KEY_NONE
        }

        pointerToPadsMap.clear()
    }

    fun dispose() {
        btnOffPress.dispose()
        btnOnPress.dispose()
    }
}