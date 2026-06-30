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

private val TOUCH_RADIUS = colWidth * 0.15f

class InputProcessorSscHD : InputAdapter() {

    private val btnOffPress = Texture(
        Gdx.files.external("/FingerDance/Themes/$tema/GraphicsStatics/game_play/btn_off.png")
    )

    private val btnOnPress = Texture(
        Gdx.files.external("/FingerDance/Themes/$tema/GraphicsStatics/game_play/btn_on.png")
    )

    val getKeyBoard = IntArray(10) { KEY_NONE }

    private val pointerToPadsMap = mutableMapOf<Int, Set<Int>>()
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

        for (i in 2..7) {
            val pad = padPositionsHD[i]

            if (pad[0] == 0f && pad[1] == 0f) continue

            val left = pad[0]
            val top = pad[1]
            val right = left + colWidth
            val bottom = top + heightBtns

            val closestX = x.coerceIn(left, right)
            val closestY = y.coerceIn(top, bottom)

            val dx = x - closestX
            val dy = y - closestY
            val distanceSq = dx * dx + dy * dy

            if (distanceSq <= radiusSq) {
                result.add(i)
            }
        }

        return result
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

    fun resetState() {
        for (i in 0 until 10) {
            padPointers[i].clear()
            wasPressed[i] = false
            getKeyBoard[i] = KEY_NONE
        }

        pointerToPadsMap.clear()
    }

    fun dispose() {
        btnOffPress.dispose()
        btnOnPress.dispose()
    }
}