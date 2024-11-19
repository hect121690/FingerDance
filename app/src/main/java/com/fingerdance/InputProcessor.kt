package com.fingerdance

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch

private val KEY_NONE = 0
private val KEY_DOWN = 1
private val KEY_PRESS = 2
private val KEY_UP = 3

class InputProcessor() : InputAdapter() {
    private val btnOffPress = Texture(Gdx.files.external("/FingerDance/Themes/$tema/GraphicsStatics/game_play/btn_off.png"))
    private val btnOnPress = Texture(Gdx.files.external("/FingerDance/Themes/$tema/GraphicsStatics/game_play/btn_on.png"))

    val getKeyBoard = IntArray(padPositions.size) { KEY_NONE }
    private val pointerToPadMap = mutableMapOf<Int, Int>()
    private var hasStateChanged = false

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
        return padPositions.indexOfFirst { pad ->
            x in pad[0]..(pad[0] + widthBtns) && y in pad[1]..(pad[1] + heightBtns)
        }.takeIf { it >= 0 }
    }

    fun update() {
        // Actualizar solo si ha habido cambios en los estados
        if (!hasStateChanged) return

        for (i in getKeyBoard.indices) {
            when (getKeyBoard[i]) {
                KEY_DOWN -> getKeyBoard[i] = KEY_PRESS
                KEY_UP -> getKeyBoard[i] = KEY_NONE
            }
        }
        hasStateChanged = false
    }

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

    fun dispose() {
        btnOffPress.dispose()
        btnOnPress.dispose()
    }
}
