package com.fingerdance.ssc.attacks

data class AttackState(
    var boost: Float = 0f,
    var brake: Float = 0f,
    var wave: Float = 0f,
    var expand: Float = 0f,
    var boomerang: Float = 0f,

    var drunk: Float = 0f,
    var dizzy: Float = 0f,
    var confusion: Float = 0f,
    var tornado: Float = 0f,
    var tipsy: Float = 0f,
    var bumpy: Float = 0f,
    var beat: Float = 0f,

    var twirl: Float = 0f,
    var roll: Float = 0f,

    var mini: Float = 0f,

    var reverse: Float = 0f,
    var flip: Float = 0f,
    var invert: Float = 0f,

    var dark: Float = 0f,
    var stealth: Float = 0f,
    var hidden: Float = 0f,
    var sudden: Float = 0f,
    var blink: Float = 0f,
    var blind: Float = 0f,

    // MoveZ por columna, base 0. Ejemplo: key 2 == MoveZ3.
    val moveZ: MutableMap<Int, Float> = mutableMapOf(),
    val moveZApproachSpeeds: MutableMap<Int, Float> = mutableMapOf(),

    var skew: Float = 0f,
    var perspectiveTilt: Float = 0f,

    val approachSpeeds: MutableMap<AttackMod, Float> = mutableMapOf(),
    var skewApproachSpeed: Float = 1f,
    var perspectiveTiltApproachSpeed: Float = 1f
) {
    fun apply(modifier: AttackModifier) {
        val level = modifier.level
        val speed = modifier.approachSpeed

        if (modifier.type != AttackMod.MOVE_Z) {
            approachSpeeds[modifier.type] = speed
        }

        when (modifier.type) {
            AttackMod.BOOST -> boost = level
            AttackMod.BRAKE -> brake = level
            AttackMod.WAVE -> wave = level
            AttackMod.EXPAND -> expand = level
            AttackMod.BOOMERANG -> boomerang = level

            AttackMod.DRUNK -> drunk = level
            AttackMod.DIZZY -> dizzy = level
            AttackMod.CONFUSION -> confusion = level
            AttackMod.TORNADO -> tornado = level
            AttackMod.TIPSY -> tipsy = level
            AttackMod.BUMPY -> bumpy = level
            AttackMod.BEAT -> beat = level

            AttackMod.TWIRL -> twirl = level
            AttackMod.ROLL -> roll = level

            AttackMod.MINI -> mini = level

            AttackMod.REVERSE -> reverse = level
            AttackMod.FLIP -> flip = level
            AttackMod.INVERT -> invert = level

            AttackMod.DARK -> dark = level
            AttackMod.STEALTH -> stealth = level
            AttackMod.HIDDEN -> hidden = level
            AttackMod.SUDDEN -> sudden = level
            AttackMod.BLINK -> blink = level
            AttackMod.BLIND -> blind = level

            AttackMod.MOVE_Z -> {
                val column = modifier.column ?: return
                moveZ[column] = level
                moveZApproachSpeeds[column] = speed
            }

            AttackMod.OVERHEAD -> {
                skew = 0f
                perspectiveTilt = 0f
                skewApproachSpeed = speed
                perspectiveTiltApproachSpeed = speed
            }

            AttackMod.INCOMING -> {
                skew = level
                perspectiveTilt = -level
                skewApproachSpeed = speed
                perspectiveTiltApproachSpeed = speed
            }

            AttackMod.SPACE -> {
                skew = level
                perspectiveTilt = level
                skewApproachSpeed = speed
                perspectiveTiltApproachSpeed = speed
            }

            AttackMod.HALLWAY -> {
                skew = 0f
                perspectiveTilt = -level
                skewApproachSpeed = speed
                perspectiveTiltApproachSpeed = speed
            }

            AttackMod.DISTANT -> {
                skew = 0f
                perspectiveTilt = level
                skewApproachSpeed = speed
                perspectiveTiltApproachSpeed = speed
            }

            AttackMod.NORMAL,
            AttackMod.UNKNOWN -> Unit
        }
    }

    fun getApproachSpeed(mod: AttackMod): Float = approachSpeeds[mod] ?: 1f
    fun getMoveZ(column: Int): Float = moveZ[column] ?: 0f
    fun getMoveZApproachSpeed(column: Int): Float = moveZApproachSpeeds[column] ?: 1f

    fun isDefault(): Boolean {
        return boost == 0f &&
                brake == 0f &&
                wave == 0f &&
                expand == 0f &&
                boomerang == 0f &&
                drunk == 0f &&
                dizzy == 0f &&
                confusion == 0f &&
                tornado == 0f &&
                tipsy == 0f &&
                bumpy == 0f &&
                beat == 0f &&
                twirl == 0f &&
                roll == 0f &&
                mini == 0f &&
                reverse == 0f &&
                flip == 0f &&
                invert == 0f &&
                dark == 0f &&
                stealth == 0f &&
                hidden == 0f &&
                sudden == 0f &&
                blink == 0f &&
                blind == 0f &&
                moveZ.values.all { it == 0f } &&
                skew == 0f &&
                perspectiveTilt == 0f
    }
}
