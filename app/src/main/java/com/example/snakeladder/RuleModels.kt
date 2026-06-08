package com.example.snakeladder

internal const val GAME_STATE_SCHEMA_VERSION = 3
internal const val PLAYER_PROFILE_SCHEMA_VERSION = 4

enum class MatchModePreset(
    val label: String,
    val description: String
) {
    CLASSIC(
        label = "Classic",
        description = "Standard Snake & Ladder rules."
    ),
    DAILY_CHALLENGE(
        label = "Daily",
        description = "Today's objective tracked against your local profile."
    ),
    QUEST_NODE(
        label = "Quest",
        description = "Campaign node with a focused board or bot setup."
    ),
    TIME_ATTACK(
        label = "Time Attack",
        description = "Win before the turn clock expires."
    ),
    SUDDEN_DEATH(
        label = "Sudden Death",
        description = "After the turn limit, the furthest player wins."
    ),
    BEST_OF_THREE(
        label = "Best Of Three",
        description = "First player to win two rounds wins the match."
    ),
    PARTY_RULES(
        label = "Party",
        description = "Power-ups, traps, and comeback rules."
    ),
    TACTICAL_CARDS(
        label = "Cards",
        description = "One-tap tactical cards with rerolls, shields, boosts, and revenge."
    ),
    TEAM_MODE(
        label = "Team",
        description = "Two teams share match pressure and rewards."
    ),
    TWO_V_TWO(
        label = "2v2",
        description = "Four-player team match with shared victory."
    )
}

enum class PowerUpType(val label: String, val description: String) {
    SHIELD("Shield", "Block one snake bite or knockback."),
    REROLL("Reroll", "Roll the die again once."),
    REVENGE("Revenge", "Return pressure after being knocked back."),
    DICE_BOOST("Dice Boost", "Add one cell to a roll when available."),
    TRAP("Trap", "Mark a tile that sends a rival back."),
    MYSTERY("Mystery", "Trigger a small random board effect.")
}

enum class BoardTileType(val label: String) {
    TRAP("Trap"),
    MYSTERY("Mystery"),
    SHORTCUT("Shortcut"),
    RISK_ROUTE("Risk route"),
    BRANCH_PATH("Branch")
}

data class BoardTile(
    val cell: Int,
    val type: BoardTileType,
    val targetCell: Int? = null,
    val penaltyCells: Int = 0,
    val rewardPowerUp: PowerUpType? = null,
    val label: String = type.label
) {
    init {
        require(cell in 2..99) { "Special tile cell must be between 2 and 99." }
        targetCell?.let { require(it in 1..100) { "Special tile target must be between 1 and 100." } }
    }
}

data class RuleSet(
    val id: String,
    val label: String,
    val description: String,
    val enabledPowerUps: List<PowerUpType> = emptyList(),
    val exactFinishRequired: Boolean = true,
    val suddenDeathAfterTurns: Int? = null,
    val turnLimit: Int? = null,
    val roundTarget: Int = 1,
    val startingPowerUps: List<PowerUpType> = emptyList(),
    val rewardMultiplier: Int = 1,
    val teamMode: Boolean = false
) {
    val usesPowerUps: Boolean
        get() = enabledPowerUps.isNotEmpty()
}

object RuleSets {
    const val CLASSIC_ID = "classic"
    const val DAILY_ID = "daily"
    const val QUEST_ID = "quest"
    const val PARTY_ID = "party_power"
    const val SUDDEN_DEATH_ID = "sudden_death"
    const val BEST_OF_THREE_ID = "best_of_three"
    const val TIME_ATTACK_ID = "time_attack"
    const val TEAM_ID = "team_power"
    const val TACTICAL_CARDS_ID = "tactical_cards"

    val classic = RuleSet(
        id = CLASSIC_ID,
        label = "Classic Rules",
        description = "Exact finish, sixes grant bonus turns, ladders grant bonus turns."
    )

    val daily = RuleSet(
        id = DAILY_ID,
        label = "Daily Challenge Rules",
        description = "Classic rules with daily objective tracking."
    )

    val quest = RuleSet(
        id = QUEST_ID,
        label = "Quest Rules",
        description = "Classic base rules used by campaign nodes."
    )

    val partyPower = RuleSet(
        id = PARTY_ID,
        label = "Party Power Rules",
        description = "Power-up framework for shields, rerolls, revenge, traps, and mystery tiles.",
        enabledPowerUps = listOf(
            PowerUpType.SHIELD,
            PowerUpType.REROLL,
            PowerUpType.REVENGE,
            PowerUpType.DICE_BOOST,
            PowerUpType.TRAP,
            PowerUpType.MYSTERY
        ),
        startingPowerUps = listOf(PowerUpType.SHIELD, PowerUpType.REROLL, PowerUpType.TRAP),
        rewardMultiplier = 2
    )

    val suddenDeath = RuleSet(
        id = SUDDEN_DEATH_ID,
        label = "Sudden Death Rules",
        description = "Short matches resolve to the furthest player after the turn limit.",
        suddenDeathAfterTurns = 24,
        turnLimit = 24,
        rewardMultiplier = 2
    )

    val bestOfThree = RuleSet(
        id = BEST_OF_THREE_ID,
        label = "Best Of Three Rules",
        description = "First player to capture two rounds wins the match.",
        roundTarget = 2,
        rewardMultiplier = 2
    )

    val timeAttack = RuleSet(
        id = TIME_ATTACK_ID,
        label = "Time Attack Rules",
        description = "Twenty turns to finish. If time expires, the leader wins.",
        turnLimit = 20,
        rewardMultiplier = 2
    )

    val teamPower = RuleSet(
        id = TEAM_ID,
        label = "Team Power Rules",
        description = "Four-player team mode with shared power-up pressure.",
        enabledPowerUps = partyPower.enabledPowerUps,
        startingPowerUps = listOf(PowerUpType.SHIELD, PowerUpType.DICE_BOOST, PowerUpType.TRAP),
        rewardMultiplier = 2,
        teamMode = true
    )

    val tacticalCards = RuleSet(
        id = TACTICAL_CARDS_ID,
        label = "Tactical Card Rules",
        description = "Cards are dealt as power-ups. Use reroll, shield, boost, revenge, and mystery at the right moment.",
        enabledPowerUps = listOf(
            PowerUpType.REROLL,
            PowerUpType.SHIELD,
            PowerUpType.DICE_BOOST,
            PowerUpType.REVENGE,
            PowerUpType.MYSTERY
        ),
        startingPowerUps = listOf(PowerUpType.REROLL, PowerUpType.SHIELD, PowerUpType.DICE_BOOST),
        rewardMultiplier = 2
    )

    val all = listOf(classic, daily, quest, partyPower, suddenDeath, bestOfThree, timeAttack, teamPower, tacticalCards)

    fun byId(id: String): RuleSet = all.firstOrNull { it.id == id } ?: classic

    fun forMatchMode(matchMode: MatchModePreset): RuleSet {
        return when (matchMode) {
            MatchModePreset.CLASSIC -> classic
            MatchModePreset.DAILY_CHALLENGE -> daily
            MatchModePreset.QUEST_NODE -> quest
            MatchModePreset.TIME_ATTACK -> timeAttack
            MatchModePreset.SUDDEN_DEATH -> suddenDeath
            MatchModePreset.BEST_OF_THREE -> bestOfThree
            MatchModePreset.PARTY_RULES -> partyPower
            MatchModePreset.TACTICAL_CARDS -> tacticalCards
            MatchModePreset.TEAM_MODE,
            MatchModePreset.TWO_V_TWO -> teamPower
        }
    }
}

data class BoardLayout(
    val id: String,
    val label: String,
    val description: String,
    val snakes: Map<Int, Int>,
    val ladders: Map<Int, Int>,
    val specialTiles: List<BoardTile> = emptyList()
)

object BoardLayouts {
    const val CLASSIC_ID = "classic"
    const val QUICK_CLIMB_ID = "quick_climb"
    const val SNAKE_DEN_ID = "snake_den"
    const val SPEED_RUN_ID = "speed_run"
    const val LADDER_LEAGUE_ID = "ladder_league"
    const val TRAP_VALLEY_ID = "trap_valley"
    const val PRO_CHAOS_ID = "pro_chaos"
    const val FAMILY_SHORT_ID = "family_short"
    const val FESTIVAL_EVENT_ID = "festival_event"
    const val MONSOON_EVENT_ID = "monsoon_event"
    const val CUSTOM_ID = "custom_lab"
    private val defaultCustomSnakes = mapOf(97 to 61, 70 to 42, 54 to 23)
    private val defaultCustomLadders = mapOf(4 to 29, 18 to 46, 40 to 73, 66 to 88)
    private val defaultCustomSpecialTiles = listOf(
        BoardTile(22, BoardTileType.MYSTERY, rewardPowerUp = PowerUpType.REROLL),
        BoardTile(58, BoardTileType.SHORTCUT, targetCell = 78)
    )

    val classic = BoardLayout(
        id = CLASSIC_ID,
        label = "Classic Board",
        description = "Traditional fixed snakes and ladders.",
        snakes = mapOf(
            99 to 54,
            95 to 72,
            92 to 36,
            83 to 41,
            73 to 52,
            64 to 60,
            59 to 17,
            49 to 11,
            46 to 25,
            16 to 6
        ),
        ladders = mapOf(
            2 to 38,
            7 to 14,
            8 to 31,
            15 to 26,
            21 to 42,
            28 to 84,
            36 to 44,
            51 to 67,
            71 to 91,
            78 to 98,
            87 to 94
        )
    )

    val quickClimb = BoardLayout(
        id = QUICK_CLIMB_ID,
        label = "Quick Climb",
        description = "More early ladders for daily and campaign pacing.",
        snakes = mapOf(
            99 to 54,
            92 to 48,
            73 to 52,
            59 to 17,
            46 to 25,
            16 to 6
        ),
        ladders = classic.ladders + mapOf(
            12 to 34,
            32 to 55,
            62 to 81
        )
    )

    val snakeDen = BoardLayout(
        id = SNAKE_DEN_ID,
        label = "Snake Den",
        description = "Risk-heavy board for survival-style quest nodes.",
        snakes = classic.snakes + mapOf(
            88 to 24,
            69 to 33,
            57 to 19
        ),
        ladders = mapOf(
            2 to 38,
            8 to 31,
            21 to 42,
            51 to 67,
            78 to 98
        )
    )

    val speedRun = BoardLayout(
        id = SPEED_RUN_ID,
        label = "Speed Run",
        description = "Short-session board for time attack and quick rematches.",
        snakes = mapOf(
            94 to 73,
            68 to 44,
            47 to 21,
            29 to 9
        ),
        ladders = mapOf(
            3 to 24,
            11 to 37,
            22 to 58,
            45 to 76,
            63 to 88,
            79 to 97
        )
    )

    val ladderLeague = BoardLayout(
        id = LADDER_LEAGUE_ID,
        label = "Ladder League",
        description = "Aggressive climbs with enough snakes to punish greedy play.",
        snakes = mapOf(
            96 to 61,
            84 to 57,
            66 to 30,
            52 to 34,
            39 to 18
        ),
        ladders = classic.ladders + mapOf(
            4 to 25,
            18 to 45,
            43 to 77,
            56 to 89
        )
    )

    val trapValley = BoardLayout(
        id = TRAP_VALLEY_ID,
        label = "Trap Valley",
        description = "Mid-board pressure built for party rules and trap timing.",
        snakes = mapOf(
            97 to 66,
            85 to 53,
            74 to 47,
            58 to 28,
            41 to 13
        ),
        ladders = mapOf(
            5 to 22,
            14 to 35,
            27 to 49,
            48 to 70,
            60 to 82,
            72 to 93
        ),
        specialTiles = listOf(
            BoardTile(18, BoardTileType.TRAP, penaltyCells = 9, label = "Snare"),
            BoardTile(39, BoardTileType.MYSTERY, rewardPowerUp = PowerUpType.SHIELD),
            BoardTile(62, BoardTileType.TRAP, penaltyCells = 12, label = "Deep trap"),
            BoardTile(76, BoardTileType.MYSTERY, rewardPowerUp = PowerUpType.REROLL)
        )
    )

    val proChaos = BoardLayout(
        id = PRO_CHAOS_ID,
        label = "Pro Chaos",
        description = "High-swing board for experienced players who want volatility.",
        snakes = classic.snakes + mapOf(
            90 to 50,
            80 to 43,
            67 to 22,
            55 to 10
        ),
        ladders = classic.ladders + mapOf(
            10 to 40,
            24 to 63,
            44 to 86
        ),
        specialTiles = listOf(
            BoardTile(13, BoardTileType.SHORTCUT, targetCell = 37, label = "Shortcut"),
            BoardTile(31, BoardTileType.RISK_ROUTE, targetCell = 65, penaltyCells = 14, rewardPowerUp = PowerUpType.DICE_BOOST),
            BoardTile(47, BoardTileType.BRANCH_PATH, targetCell = 72, rewardPowerUp = PowerUpType.SHIELD),
            BoardTile(68, BoardTileType.MYSTERY, rewardPowerUp = PowerUpType.MYSTERY),
            BoardTile(82, BoardTileType.TRAP, penaltyCells = 18, label = "Chaos trap")
        )
    )

    val familyShort = BoardLayout(
        id = FAMILY_SHORT_ID,
        label = "Family Short",
        description = "Gentler board with fewer brutal drops for shared-device play.",
        snakes = mapOf(
            92 to 70,
            73 to 52,
            49 to 31,
            16 to 6
        ),
        ladders = mapOf(
            2 to 23,
            8 to 31,
            15 to 36,
            28 to 60,
            51 to 75,
            78 to 96
        ),
        specialTiles = listOf(
            BoardTile(24, BoardTileType.SHORTCUT, targetCell = 41),
            BoardTile(44, BoardTileType.MYSTERY, rewardPowerUp = PowerUpType.SHIELD),
            BoardTile(67, BoardTileType.BRANCH_PATH, targetCell = 83)
        )
    )

    val festivalEvent = BoardLayout(
        id = FESTIVAL_EVENT_ID,
        label = "Festival Event",
        description = "Limited-time style board with shortcuts and bonus mystery tiles.",
        snakes = mapOf(
            91 to 57,
            76 to 50,
            63 to 32,
            38 to 19
        ),
        ladders = mapOf(
            6 to 27,
            17 to 48,
            35 to 68,
            52 to 86,
            74 to 96
        ),
        specialTiles = listOf(
            BoardTile(12, BoardTileType.MYSTERY, rewardPowerUp = PowerUpType.DICE_BOOST, label = "Festival gift"),
            BoardTile(44, BoardTileType.SHORTCUT, targetCell = 61, label = "Parade shortcut"),
            BoardTile(69, BoardTileType.MYSTERY, rewardPowerUp = PowerUpType.REROLL, label = "Firework gift")
        )
    )

    val monsoonEvent = BoardLayout(
        id = MONSOON_EVENT_ID,
        label = "Monsoon Event",
        description = "Limited-time style board with slippery risk routes.",
        snakes = mapOf(
            98 to 64,
            87 to 53,
            71 to 34,
            45 to 18
        ),
        ladders = mapOf(
            3 to 25,
            13 to 39,
            29 to 62,
            56 to 81,
            77 to 94
        ),
        specialTiles = listOf(
            BoardTile(21, BoardTileType.RISK_ROUTE, targetCell = 49, penaltyCells = 11, rewardPowerUp = PowerUpType.SHIELD, label = "Flood crossing"),
            BoardTile(58, BoardTileType.BRANCH_PATH, targetCell = 79, rewardPowerUp = PowerUpType.REROLL, label = "River branch"),
            BoardTile(83, BoardTileType.TRAP, penaltyCells = 15, label = "Slippery tile")
        )
    )

    fun defaultCustomLayout(): BoardLayout {
        return BoardLayout(
            id = CUSTOM_ID,
            label = "Custom Lab",
            description = "Player-edited board saved on this device.",
            snakes = defaultCustomSnakes,
            ladders = defaultCustomLadders,
            specialTiles = defaultCustomSpecialTiles
        )
    }

    var custom = defaultCustomLayout()
        private set

    val all: List<BoardLayout>
        get() = listOf(classic, quickClimb, snakeDen, speedRun, ladderLeague, trapValley, proChaos, familyShort, festivalEvent, monsoonEvent, custom)

    fun byId(id: String): BoardLayout = all.firstOrNull { it.id == id } ?: classic

    fun updateCustom(layout: BoardLayout) {
        require(layout.id == CUSTOM_ID) { "Only the custom board can be replaced." }
        custom = layout
    }

    fun customFromPairs(
        snakePairs: List<Pair<Int, Int>>,
        ladderPairs: List<Pair<Int, Int>>,
        specialTiles: List<BoardTile> = custom.specialTiles,
        preserveCurrentWhenEmpty: Boolean = true
    ): BoardLayout {
        val validSnakes = snakePairs
            .filter { (head, tail) -> head in 2..99 && tail in 1 until head }
            .take(12)
            .toMap()
        val validLadders = ladderPairs
            .filter { (start, end) -> start in 2..99 && end in (start + 1)..100 }
            .take(12)
            .toMap()
        return custom.copy(
            snakes = if (preserveCurrentWhenEmpty) validSnakes.ifEmpty { custom.snakes } else validSnakes,
            ladders = if (preserveCurrentWhenEmpty) validLadders.ifEmpty { custom.ladders } else validLadders,
            specialTiles = specialTiles.filter { tile ->
                tile.cell !in validSnakes.keys && tile.cell !in validLadders.keys
            }.take(12)
        )
    }
}

object PowerUpRuleEngine {
    private const val MAX_INVENTORY_SIZE = 4

    fun availablePowerUps(ruleSet: RuleSet): List<PowerUpType> = ruleSet.enabledPowerUps

    fun startingInventory(ruleSet: RuleSet): List<PowerUpType> {
        return ruleSet.startingPowerUps.take(MAX_INVENTORY_SIZE)
    }

    fun addPowerUps(
        inventory: List<PowerUpType>,
        additions: List<PowerUpType>
    ): List<PowerUpType> {
        if (additions.isEmpty()) return inventory.take(MAX_INVENTORY_SIZE)
        return (inventory + additions).take(MAX_INVENTORY_SIZE)
    }

    fun awardsForMove(ruleSet: RuleSet, event: MatchEvent): List<PowerUpType> {
        if (!ruleSet.usesPowerUps || event.moveType == MoveType.POWER_UP) return emptyList()
        return buildList {
            when {
                event.moveType == MoveType.LADDER -> add(PowerUpType.DICE_BOOST)
                event.moveType == MoveType.SNAKE -> add(PowerUpType.SHIELD)
                event.moveType == MoveType.TRAP -> add(PowerUpType.REVENGE)
                event.knockedBackPlayerIndices.isNotEmpty() -> add(PowerUpType.REVENGE)
                event.dice == 6 -> add(PowerUpType.REROLL)
            }
            if (event.turnNumber % 5 == 0) add(PowerUpType.MYSTERY)
        }.distinct().take(2)
    }

    fun trapCellFor(position: Int, occupiedCells: Set<Int>): Int {
        val candidates = (position + 3..position + 8).map { it.coerceIn(2, 99) } +
            (position + 1..99)
        return candidates.firstOrNull { cell -> cell !in occupiedCells } ?: 50
    }

    fun chooseBotPowerUp(
        personality: BotPersonality,
        inventory: List<PowerUpType>,
        botPosition: Int,
        leaderGap: Int,
        difficulty: GameDifficulty = GameDifficulty.EASY
    ): PowerUpType? {
        if (inventory.isEmpty()) return null
        val baseOrder = when (personality) {
            BotPersonality.RISKY -> listOf(PowerUpType.TRAP, PowerUpType.DICE_BOOST, PowerUpType.MYSTERY, PowerUpType.REROLL)
            BotPersonality.DEFENSIVE -> listOf(PowerUpType.SHIELD, PowerUpType.REROLL, PowerUpType.TRAP, PowerUpType.REVENGE)
            BotPersonality.STEADY -> listOf(PowerUpType.DICE_BOOST, PowerUpType.TRAP, PowerUpType.REROLL, PowerUpType.MYSTERY)
            BotPersonality.PRO -> listOf(PowerUpType.REVENGE, PowerUpType.DICE_BOOST, PowerUpType.TRAP, PowerUpType.SHIELD, PowerUpType.REROLL, PowerUpType.MYSTERY)
        }
        return when (difficulty) {
            GameDifficulty.EASY -> baseOrder
            GameDifficulty.MEDIUM -> (listOf(PowerUpType.DICE_BOOST, PowerUpType.TRAP) + baseOrder).distinct()
            GameDifficulty.HARD -> (listOf(PowerUpType.REVENGE, PowerUpType.DICE_BOOST, PowerUpType.TRAP, PowerUpType.SHIELD) + baseOrder).distinct()
        }.firstOrNull { candidate ->
            candidate in inventory &&
                when (candidate) {
                    PowerUpType.REVENGE -> leaderGap >= if (personality == BotPersonality.PRO) 5 else 8
                    PowerUpType.DICE_BOOST -> botPosition in 70..99 || difficulty == GameDifficulty.HARD || personality == BotPersonality.RISKY || (personality == BotPersonality.PRO && botPosition >= 55)
                    PowerUpType.SHIELD -> difficulty == GameDifficulty.HARD || personality == BotPersonality.DEFENSIVE || (personality == BotPersonality.PRO && botPosition >= 40)
                    else -> true
                }
        }
    }

    fun describe(ruleSet: RuleSet): String {
        return if (ruleSet.enabledPowerUps.isEmpty()) {
            "Power-ups disabled"
        } else {
            "Power-ups: ${ruleSet.enabledPowerUps.joinToString { it.label }}"
        }
    }
}
