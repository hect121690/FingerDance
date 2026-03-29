package com.fingerdance.ssc

class GameState {
    var score = 0
    var combo = 0
    var maxCombo = 0
    var life = 1.0

    var failed = false

    var lastJudgment = JudgmentEngine.Judgment.PERFECT
    var lastJudgmentTime = 0.0

    val judgments = mutableMapOf<JudgmentEngine.Judgment, Int>()
}