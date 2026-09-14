package com.fingerdance.ssc.attacks

data class AttackModifier(
    val type: AttackMod,
    val level: Float = 1f,
    val approachSpeed: Float = 1f,
    val raw: String,

    // Sólo se usa para mods por columna como MoveZ1/MoveZ2/MoveZ3.
    // Internamente es base 0: MoveZ1 -> 0, MoveZ2 -> 1, etc.
    val column: Int? = null
)
