package com.fingerdance.ssc.attacks

object AttackFeatureFlags {

    // ACCEL / SCROLL
    const val BOOST = true
    const val BRAKE = true
    const val WAVE = true
    const val EXPAND = true
    const val BOOMERANG = true

    // POSITION
    const val DRUNK = true
    const val TORNADO = true
    const val TIPSY = true
    const val BEAT = true
    const val MOVE_Z = true

    // ROTATION / 3D
    const val DIZZY = true
    const val CONFUSION = true

    // Dejamos éstos apagados por defecto.
    const val BUMPY = false
    const val TWIRL = false
    const val ROLL = false

    // SCALE
    const val MINI = true

    // DIRECTION / COLUMN MAPPING
    const val REVERSE = true
    const val FLIP = true
    const val INVERT = true

    // VISIBILITY
    const val DARK = true
    const val STEALTH = true

    // PERSPECTIVE
    const val OVERHEAD = true
    const val HALLWAY = true
    const val DISTANT = true
    const val INCOMING = true
    const val SPACE = true

    const val PERSPECTIVE =
        OVERHEAD || HALLWAY || DISTANT || INCOMING || SPACE
}
