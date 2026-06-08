package com.example.snakeladder

import androidx.compose.ui.graphics.Color

enum class GameMode {
    LOCAL_MULTIPLAYER,
    VS_BOT
}

enum class GameDifficulty {
    EASY,
    MEDIUM,
    HARD
}

enum class MoveType {
    NORMAL,
    SNAKE,
    LADDER,
    SHORTCUT,
    MYSTERY_TILE,
    RISK_ROUTE,
    BRANCH_PATH,
    OVERSHOOT,
    WIN,
    POWER_UP,
    TRAP,
    TIMEOUT,
    ROUND_WIN
}

const val MAX_PLAYER_SETUP_NAME_LENGTH = 18

data class PlayerSetup(
    val name: String,
    val avatarId: String
)

internal fun defaultPlayerName(playerNumber: Int): String = "Player $playerNumber"

internal fun defaultPlayerAvatarId(playerNumber: Int): String {
    return when (playerNumber) {
        2 -> "cobra_token"
        3 -> "ladder_king"
        4 -> "gold_die"
        else -> "classic_token"
    }
}

internal fun defaultPlayerSetups(): List<PlayerSetup> {
    return List(4) { index ->
        PlayerSetup(
            name = defaultPlayerName(index + 1),
            avatarId = defaultPlayerAvatarId(index + 1)
        )
    }
}

enum class BoardThemeOption {
    VIBRANT,
    PREMIUM_MUTED,
    FESTIVAL,
    MONSOON
}

enum class DiceSkinOption(val label: String) {
    CLASSIC_RED("Classic Red"),
    ROYAL_BLUE("Royal Blue"),
    GOLD("Gold")
}

enum class TokenTrailOption(val label: String) {
    NONE("None"),
    SPARK("Spark"),
    RIBBON("Ribbon")
}

enum class HapticThemeOption(val label: String, val multiplier: Float) {
    SOFT("Soft", 0.55f),
    CLASSIC("Classic", 1f),
    INTENSE("Intense", 1.45f)
}

enum class SoundtrackOption(val label: String) {
    CLASSIC("Classic"),
    COMEBACK("Comeback"),
    FESTIVAL("Festival")
}

enum class QuickReaction(val label: String) {
    NICE("Nice"),
    CLOSE("Close"),
    OOPS("Oops"),
    WOW("Wow")
}

enum class BotPersonality(
    val displayName: String,
    val styleName: String,
    val description: String,
    val autoRollDelayMs: Long
) {
    STEADY(
        displayName = "Rival Bot",
        styleName = "Steady",
        description = "Balanced rival with calm pressure.",
        autoRollDelayMs = 700L
    ),
    RISKY(
        displayName = "Rocket Bot",
        styleName = "Risky",
        description = "Aggressive rival that celebrates fast swings.",
        autoRollDelayMs = 420L
    ),
    DEFENSIVE(
        displayName = "Guard Bot",
        styleName = "Defensive",
        description = "Patient rival that plays like every cell matters.",
        autoRollDelayMs = 950L
    ),
    PRO(
        displayName = "Pro Bot",
        styleName = "Pro",
        description = "Advanced rival that times shields, traps, boosts, and revenge by board state.",
        autoRollDelayMs = 520L
    )
}

enum class BotTurnPaceOption(
    val label: String,
    val multiplier: Float,
    val supportText: String
) {
    RELAXED("Relaxed", 1.45f, "Longer pause before bot rolls."),
    STANDARD("Standard", 1f, "Uses the bot personality pace."),
    QUICK("Quick", 0.45f, "Speeds up repeated bot turns.")
}

data class PlayerState(
    val name: String,
    val color: Color,
    val position: Int,
    val teamId: Int? = null,
    val avatarId: String = "classic_token"
)

data class GameState(
    val players: List<PlayerState>,
    val currentPlayerIndex: Int,
    val lastDiceRoll: Int?,
    val statusMessage: String,
    val bonusTurnGranted: Boolean,
    val winnerIndex: Int?,
    val moveHistory: List<String>,
    val gameMode: GameMode,
    val botPlayerIndex: Int?,
    val lastMovePlayerIndex: Int?,
    val lastMovePath: List<Int>,
    val lastMoveType: MoveType?,
    val moveSignal: Int,
    val difficulty: GameDifficulty = GameDifficulty.EASY,
    val knockBackMoves: List<KnockBackMove> = emptyList(),
    val matchEvents: List<MatchEvent> = emptyList(),
    val botPersonality: BotPersonality = BotPersonality.STEADY,
    val matchMode: MatchModePreset = MatchModePreset.CLASSIC,
    val boardLayoutId: String = BoardLayouts.CLASSIC_ID,
    val ruleSetId: String = RuleSets.CLASSIC_ID,
    val powerUpInventories: List<List<PowerUpType>> = emptyList(),
    val armedPowerUps: List<PlayerArmedPowerUp> = emptyList(),
    val activeTraps: List<BoardTrap> = emptyList(),
    val roundNumber: Int = 1,
    val roundWins: List<Int> = emptyList(),
    val turnLimit: Int? = null,
    val turnsRemaining: Int? = null,
    val winningTeamId: Int? = null,
    val campaignNodeId: String? = null,
    val dailyChallengeId: String? = null,
    val schemaVersion: Int = GAME_STATE_SCHEMA_VERSION
)

data class KnockBackMove(
    val playerIndex: Int,
    val path: List<Int>
)

data class MatchEvent(
    val turnNumber: Int,
    val playerIndex: Int,
    val playerName: String,
    val dice: Int,
    val startPosition: Int,
    val landedPosition: Int,
    val finalPosition: Int,
    val moveType: MoveType,
    val path: List<Int>,
    val knockedBackPlayerIndices: List<Int> = emptyList(),
    val bonusTurn: Boolean = false,
    val winner: Boolean = false,
    val powerUpUsed: PowerUpType? = null,
    val triggeredPowerUps: List<PowerUpType> = emptyList(),
    val awardedPowerUps: List<PowerUpType> = emptyList(),
    val tileLabel: String? = null,
    val roundNumber: Int = 1,
    val winningTeamId: Int? = null
)

data class PlayerArmedPowerUp(
    val playerIndex: Int,
    val type: PowerUpType
)

data class BoardTrap(
    val cell: Int,
    val ownerPlayerIndex: Int
)

data class SavedGameSnapshot(
    val id: String,
    val name: String,
    val savedAt: Long,
    val boardTheme: BoardThemeOption,
    val state: GameState
)

data class MoveResult(
    val position: Int,
    val eventMessage: String?,
    val moveType: MoveType = MoveType.NORMAL,
    val path: List<Int> = emptyList(),
    val awardedPowerUps: List<PowerUpType> = emptyList(),
    val tileLabel: String? = null
)

internal data class LadderZoomEffect(
    val start: Int,
    val end: Int,
    val scale: Float,
    val alpha: Float
)

internal data class SnakeZoomEffect(
    val start: Int,
    val end: Int,
    val scale: Float,
    val alpha: Float
)

internal data class BoardFocusHighlight(
    val cell: Int,
    val label: String,
    val secondaryCell: Int? = null
)
