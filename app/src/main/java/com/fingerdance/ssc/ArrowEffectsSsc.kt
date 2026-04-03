package com.fingerdance.ssc

class ArrowEffectsSsc(
    private val stepSize: Float, // tamaño base por beat (medidaFlechas)
    private val speedX: Float,   // X-Mod del jugador (playerSong.speed “X”)
    private val targetY: Float   // Y donde está la línea objetivo (receptores)
) {
    /**
     * Versión simple: beat de la nota vs beat actual de la canción.
     * (si quieres aprovechar TimmingData.getYOffsetForBeat, puedes delegar desde aquí).
     */
    fun yForBeat(noteBeat: Double, currentBeat: Double): Float {
        val beatDiff = noteBeat - currentBeat
        val gapPerBeat = stepSize * speedX
        return targetY + (beatDiff * gapPerBeat).toFloat()
    }

    fun isOnScreen(y: Float, height: Int): Boolean {
        return y > -stepSize && y < height + stepSize
    }
}