package com.fingerdance.ssc.attacks

enum class AttackMod {
    BOOST, BRAKE, WAVE, EXPAND, BOOMERANG,
    DRUNK, DIZZY, CONFUSION, TORNADO, TIPSY, BUMPY, BEAT,
    TWIRL, ROLL,
    MINI,
    OVERHEAD, HALLWAY, DISTANT, INCOMING, SPACE,
    REVERSE, FLIP, INVERT,
    DARK, STEALTH, HIDDEN, SUDDEN, BLINK, BLIND,

    // MoveZ1, MoveZ2, MoveZ3... usan este mismo tipo y guardan
    // la columna concreta en AttackModifier.column (base 0).
    MOVE_Z,

    NORMAL, UNKNOWN
}
