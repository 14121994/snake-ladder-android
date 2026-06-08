package com.example.snakeladder

internal fun GameDifficulty.iconLabel(): String {
    return when (this) {
        GameDifficulty.EASY -> "E"
        GameDifficulty.MEDIUM -> "M"
        GameDifficulty.HARD -> "H"
    }
}

internal fun GameDifficulty.shortLabel(): String {
    return when (this) {
        GameDifficulty.EASY -> "Easy"
        GameDifficulty.MEDIUM -> "Medium"
        GameDifficulty.HARD -> "Hard"
    }
}

internal fun GameDifficulty.compactLabel(): String {
    return when (this) {
        GameDifficulty.EASY -> "Easy"
        GameDifficulty.MEDIUM -> "Med"
        GameDifficulty.HARD -> "Hard"
    }
}

internal fun GameDifficulty.knockbackSymbolLabel(): String {
    return when (this) {
        GameDifficulty.EASY -> "KB --"
        GameDifficulty.MEDIUM -> "KB N"
        GameDifficulty.HARD -> "KB N+S+L"
    }
}

internal fun GameDifficulty.compactKnockbackLabel(): String {
    return when (this) {
        GameDifficulty.EASY -> "No KB"
        GameDifficulty.MEDIUM -> "Normal"
        GameDifficulty.HARD -> "N/S/L"
    }
}

internal fun GameDifficulty.knockbackRuleLabel(): String {
    return when (this) {
        GameDifficulty.EASY -> "No knockback"
        GameDifficulty.MEDIUM -> "Normal landings"
        GameDifficulty.HARD -> "Normal, snake, ladder"
    }
}
