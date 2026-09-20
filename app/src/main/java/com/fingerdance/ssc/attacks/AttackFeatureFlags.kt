package com.fingerdance.ssc.attacks

object AttackFeatureFlags {

    object Speed {
        var XMOD: Boolean = true
    }

    object AccelScroll {
        var BOOST: Boolean = true
        var BRAKE: Boolean = true
        var WAVE: Boolean = true
        var EXPAND: Boolean = true
        var EXPAND_INTENSITY: Float = 0.50f
        var BOOMERANG: Boolean = true
        var EARTHWORM: Boolean = true
    }

    object Position {
        var DRUNK: Boolean = true
        var TORNADO: Boolean = true
        var TIPSY: Boolean = true
        var BEAT: Boolean = true
        var MOVE_Z: Boolean = true
    }

    object Rotation3D {
        var DIZZY: Boolean = true
        var CONFUSION: Boolean = true
        var BUMPY: Boolean = true
        var TWIRL: Boolean = true
        var ROLL: Boolean = true
    }

    object Scale {
        var MINI: Boolean = true
        var TINY: Boolean = true
    }

    object DirectionColumn {
        var REVERSE: Boolean = true
        var SPLIT: Boolean = true
        var ALTERNATE: Boolean = true
        var CROSS: Boolean = true
        var CENTERED: Boolean = true
        var FLIP: Boolean = true
        var INVERT: Boolean = true
    }

    object Visibility {
        var DARK: Boolean = true
        var STEALTH: Boolean = true
        var HIDDEN: Boolean = true
        var SUDDEN: Boolean = true
        var BLINK: Boolean = true
        var RANDOM_VANISH: Boolean = true
        var BLIND: Boolean = true
    }

    object Perspective {
        var OVERHEAD: Boolean = true
        var HALLWAY: Boolean = true
        var DISTANT: Boolean = true
        var INCOMING: Boolean = true
        var SPACE: Boolean = true

        val ENABLED: Boolean
            get() = OVERHEAD || HALLWAY || DISTANT || INCOMING || SPACE
    }
}
