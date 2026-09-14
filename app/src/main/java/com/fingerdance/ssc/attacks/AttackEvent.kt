package com.fingerdance.ssc.attacks

data class AttackEvent(
    val startSecond: Double,
    val durationSeconds: Double,
    val modifiers: List<String>
) {
    val endSecond: Double
        get() = startSecond + durationSeconds

    fun isActive(songTimeSeconds: Double): Boolean {
        return songTimeSeconds >= startSecond &&
                songTimeSeconds < endSecond
    }
}