package com.fingerdance.ssc

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.fingerdance.MULTIPLER_TOUCH_RADIUS
import com.fingerdance.heightBtns
import com.fingerdance.padPositions
import com.fingerdance.tema
import com.fingerdance.widthBtns

private const val KEY_NONE = 0
private const val KEY_DOWN = 1
private const val KEY_PRESS = 2
private const val KEY_UP = 3

private val TOUCH_RADIUS = widthBtns * MULTIPLER_TOUCH_RADIUS

class InputProcessorSsc : InputAdapter() {

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

    val getKeyBoard = IntArray(padPositions.size) { KEY_NONE }

    // Fuente de verdad.
    private val pointerToPadsMap = mutableMapOf<Int, Set<Int>>()
    private val padPointers = Array(padPositions.size) { mutableSetOf<Int>() }

    // Estado del frame anterior para generar DOWN / PRESS / UP.
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
        val pad = keyToPadMap[keycode] ?: return false
        padPointers[pad].add(-keycode) // IDs negativos = teclado
        return true
    }

    override fun keyUp(keycode: Int): Boolean {
        val pad = keyToPadMap[keycode] ?: return false
        padPointers[pad].remove(-keycode)
        return true
    }

    // ---------------- TOUCH ----------------

    override fun touchDown(
        screenX: Int,
        screenY: Int,
        pointer: Int,
        button: Int
    ): Boolean {
        val pads = getPadIndices(
            screenX.toFloat(),
            screenY.toFloat()
        )

        if (pads.isEmpty()) return false

        // Por seguridad, limpia cualquier asociación anterior del mismo pointer.
        pointerToPadsMap[pointer]?.forEach { oldPad ->
            padPointers[oldPad].remove(pointer)
        }

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
        removePointer(pointer)
        return true
    }

    override fun touchDragged(
        screenX: Int,
        screenY: Int,
        pointer: Int
    ): Boolean {
        val newPads = getPadIndices(
            screenX.toFloat(),
            screenY.toFloat()
        )

        val oldPads = pointerToPadsMap[pointer].orEmpty()

        if (oldPads == newPads) return true

        oldPads.forEach { oldPad ->
            padPointers[oldPad].remove(pointer)
        }

        // IMPORTANTE:
        // si el dedo salió de todos los pads, también debemos borrar
        // la asociación anterior. De lo contrario el pad puede quedarse
        // artificialmente en KEY_PRESS.
        if (newPads.isEmpty()) {
            pointerToPadsMap.remove(pointer)
            return true
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
        removePointer(pointer)
        return true
    }

    // ---------------- PAD DETECTION ----------------

    private fun getPadIndices(x: Float, y: Float): Set<Int> {
        val result = mutableSetOf<Int>()
        val radiusSq = TOUCH_RADIUS * TOUCH_RADIUS

        for (i in padPositions.indices) {
            val pad = padPositions[i]

            val left = pad[0].toFloat()
            val top = pad[1].toFloat()
            val right = left + widthBtns
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

    // ---------------- UPDATE ----------------

    fun update() {
        val activePointers = mutableSetOf<Int>()

        for (pointer in 0 until 20) {
            if (Gdx.input.isTouched(pointer)) {
                activePointers.add(pointer)
            }
        }

        // Limpieza defensiva: si Android/libGDX perdió un touchUp,
        // eliminamos pointers físicos que ya no están realmente activos.
        pointerToPadsMap.keys.toList().forEach { pointer ->
            if (pointer >= 0 && pointer !in activePointers) {
                removePointer(pointer)
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
                widthBtns,
                heightBtns
            )
        }
    }

    // ---------------- RESET ----------------

    fun resetState() {
        for (i in padPointers.indices) {
            padPointers[i].clear()
            wasPressed[i] = false
            getKeyBoard[i] = KEY_NONE
        }

        pointerToPadsMap.clear()
    }

    private fun removePointer(pointer: Int) {
        val pads = pointerToPadsMap.remove(pointer)

        if (pads != null) {
            pads.forEach { pad ->
                padPointers[pad].remove(pointer)
            }
            return
        }

        // Fallback defensivo por si el mapa perdió sincronía.
        for (i in padPointers.indices) {
            padPointers[i].remove(pointer)
        }
    }

    fun dispose() {
        btnOffPress.dispose()
        btnOnPress.dispose()
    }
}
