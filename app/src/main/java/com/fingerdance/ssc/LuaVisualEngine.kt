package com.fingerdance.ssc

import com.badlogic.gdx.Gdx


class LuaVisualEngine {

    enum class VisualTarget { RECEPTOR, NOTES }

    data class LuaVisualEvent(
        val target: VisualTarget,
        val startBeat: Float,
        val durationBeat: Float,
        val params: Map<String, Float>,
        var started: Boolean = false,
        var runtimeStartBeat: Float = 0f
    )

    var luaReceptOffsetX = 0f
        private set

    var luaNoteOffsetX = 0f
        private set

    private val active = mutableListOf<LuaVisualEvent>()

    fun reset() {
        luaReceptOffsetX = 0f
        luaNoteOffsetX = 0f
        active.clear()
    }

    fun update(currentBeat: Float, allEvents: List<LuaVisualEvent>) {

        luaReceptOffsetX = 0f
        luaNoteOffsetX = 0f

        // activar eventos
        for (event in allEvents) {
            if (!event.started && currentBeat >= event.startBeat) {
                event.started = true
                event.runtimeStartBeat = event.startBeat
                active.add(event)
            }
        }

        val it = active.iterator()
        while (it.hasNext()) {
            val event = it.next()
            val elapsed = currentBeat - event.runtimeStartBeat

            // si terminó, aplicar offset final y remover
            if (elapsed >= event.durationBeat) {
                val xPercent = event.params["x"] ?: 0f
                val finalOffset = Gdx.graphics.width * xPercent
                when (event.target) {
                    VisualTarget.RECEPTOR -> luaReceptOffsetX += finalOffset
                    VisualTarget.NOTES -> luaNoteOffsetX += finalOffset
                }
                it.remove()
                continue
            }

            val duration = maxOf(event.durationBeat, 0.0001f)
            val t = (elapsed / duration).coerceIn(0f, 1f)

            val xPercent = event.params["x"] ?: 0f
            val targetOffset = Gdx.graphics.width * xPercent
            val offset = targetOffset * t

            when (event.target) {
                VisualTarget.RECEPTOR -> luaReceptOffsetX += offset
                VisualTarget.NOTES -> luaNoteOffsetX += offset
            }
        }
    }
}