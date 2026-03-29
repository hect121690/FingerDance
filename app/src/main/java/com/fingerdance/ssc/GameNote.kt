package com.fingerdance.ssc

import com.fingerdance.ssc.Parser.NoteType

class GameNote(
    val column: Int,
    val beat: Double,
    val endBeat: Double? = null,
    val isFake: Boolean,
    val type: NoteType
) {

    // =========================================================
    // TAP STATE
    // =========================================================

    var hit = false
    var missed = false

    // =========================================================
    // HOLD STATE (REDISEÑADO)
    // =========================================================

    enum class HoldState {
        NOT_STARTED,
        HIT,            // se presionó correctamente
        HOLDING,        // actualmente sostenido
        RELEASED_EARLY, // soltado antes de tiempo
        COMPLETED,      // completado correctamente
        FAILED          // falló completamente
    }

    var holdState = HoldState.NOT_STARTED

    // timestamps (importante para precisión)
    var holdStartTimeMs: Double = -1.0
    var holdEndTimeMs: Double = -1.0

    // =========================================================
    // HELPERS
    // =========================================================

    fun isHittable(): Boolean {
        return !hit && !missed && !isFake
    }

    fun isActiveHold(): Boolean {
        return type == NoteType.HOLD &&
                (holdState == HoldState.HIT || holdState == HoldState.HOLDING)
    }

    fun isFinished(): Boolean {
        return when (type) {
            NoteType.TAP, NoteType.MINE -> hit || missed
            NoteType.HOLD -> holdState == HoldState.COMPLETED ||
                    holdState == HoldState.FAILED
        }
    }
}