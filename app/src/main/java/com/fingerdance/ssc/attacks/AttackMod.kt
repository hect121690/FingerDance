package com.fingerdance.ssc.attacks

enum class AttackMod {
    XMOD,
    BOOST, BRAKE, WAVE, EXPAND, BOOMERANG,
    DRUNK, DIZZY, CONFUSION, TORNADO, TIPSY, BUMPY, BEAT,
    TWIRL, ROLL,
    MINI, TINY,
    OVERHEAD, HALLWAY, DISTANT, INCOMING, SPACE,
    REVERSE, SPLIT, ALTERNATE, CROSS, CENTERED, FLIP, INVERT,
    DARK, STEALTH, HIDDEN, SUDDEN, BLINK, RANDOM_VANISH, BLIND,
    EARTHWORM,

    // MoveZ1, MoveZ2, MoveZ3... usan este mismo tipo y guardan
    // la columna concreta en AttackModifier.column (base 0).
    MOVE_Z,

    NORMAL, UNKNOWN
}
