package com.example.snakeladder

internal data class CampaignNode(
    val id: String,
    val chapter: Int,
    val title: String,
    val description: String,
    val requiredWins: Int,
    val gameMode: GameMode,
    val difficulty: GameDifficulty,
    val botPersonality: BotPersonality = BotPersonality.STEADY,
    val boardLayoutId: String = BoardLayouts.CLASSIC_ID,
    val matchMode: MatchModePreset = MatchModePreset.QUEST_NODE,
    val reward: RewardBundle = RewardBundle(coins = 60, xp = 80),
    val isBoss: Boolean = false
)

internal object CampaignCatalog {
    val nodes: List<CampaignNode> = listOf(
        CampaignNode(
            id = "classic_start",
            chapter = 1,
            title = "Classic Start",
            description = "Win a familiar local match on the classic board.",
            requiredWins = 0,
            gameMode = GameMode.LOCAL_MULTIPLAYER,
            difficulty = GameDifficulty.EASY,
            reward = RewardBundle(coins = 50, xp = 70)
        ),
        CampaignNode(
            id = "ladder_sprint",
            chapter = 1,
            title = "Ladder Sprint",
            description = "Use the Quick Climb board to practice high-tempo finishes.",
            requiredWins = 1,
            gameMode = GameMode.VS_BOT,
            difficulty = GameDifficulty.MEDIUM,
            botPersonality = BotPersonality.RISKY,
            boardLayoutId = BoardLayouts.QUICK_CLIMB_ID,
            reward = RewardBundle(coins = 70, xp = 90)
        ),
        CampaignNode(
            id = "snake_survival",
            chapter = 1,
            title = "Snake Survival",
            description = "Survive the Snake Den against a careful defensive bot.",
            requiredWins = 2,
            gameMode = GameMode.VS_BOT,
            difficulty = GameDifficulty.HARD,
            botPersonality = BotPersonality.DEFENSIVE,
            boardLayoutId = BoardLayouts.SNAKE_DEN_ID,
            reward = RewardBundle(coins = 90, gems = 1, xp = 120)
        ),
        CampaignNode(
            id = "party_trial",
            chapter = 2,
            title = "Party Trial",
            description = "Preview the power-up rule framework in a party-rules node.",
            requiredWins = 3,
            gameMode = GameMode.LOCAL_MULTIPLAYER,
            difficulty = GameDifficulty.MEDIUM,
            boardLayoutId = BoardLayouts.CLASSIC_ID,
            matchMode = MatchModePreset.PARTY_RULES,
            reward = RewardBundle(coins = 110, gems = 1, xp = 140)
        ),
        CampaignNode(
            id = "speed_run_trial",
            chapter = 2,
            title = "Speed Run Trial",
            description = "Beat the turn clock on a fast board.",
            requiredWins = 4,
            gameMode = GameMode.VS_BOT,
            difficulty = GameDifficulty.MEDIUM,
            botPersonality = BotPersonality.RISKY,
            boardLayoutId = BoardLayouts.SPEED_RUN_ID,
            matchMode = MatchModePreset.TIME_ATTACK,
            reward = RewardBundle(coins = 120, gems = 1, xp = 160)
        ),
        CampaignNode(
            id = "best_of_three_arena",
            chapter = 2,
            title = "Best Of Three Arena",
            description = "Win two rounds before the rival adapts.",
            requiredWins = 5,
            gameMode = GameMode.VS_BOT,
            difficulty = GameDifficulty.HARD,
            botPersonality = BotPersonality.STEADY,
            boardLayoutId = BoardLayouts.LADDER_LEAGUE_ID,
            matchMode = MatchModePreset.BEST_OF_THREE,
            reward = RewardBundle(coins = 150, gems = 1, xp = 190)
        ),
        CampaignNode(
            id = "trap_valley",
            chapter = 3,
            title = "Trap Valley",
            description = "Use traps and shields on the party board without losing tempo.",
            requiredWins = 6,
            gameMode = GameMode.VS_BOT,
            difficulty = GameDifficulty.HARD,
            botPersonality = BotPersonality.DEFENSIVE,
            boardLayoutId = BoardLayouts.TRAP_VALLEY_ID,
            matchMode = MatchModePreset.PARTY_RULES,
            reward = RewardBundle(coins = 175, gems = 2, xp = 220)
        ),
        CampaignNode(
            id = "boss_cobra_guard",
            chapter = 3,
            title = "Boss: Cobra Guard",
            description = "Beat a defensive boss with bonus shields on Trap Valley.",
            requiredWins = 8,
            gameMode = GameMode.VS_BOT,
            difficulty = GameDifficulty.HARD,
            botPersonality = BotPersonality.DEFENSIVE,
            boardLayoutId = BoardLayouts.TRAP_VALLEY_ID,
            matchMode = MatchModePreset.PARTY_RULES,
            reward = RewardBundle(coins = 260, gems = 3, xp = 320),
            isBoss = true
        ),
        CampaignNode(
            id = "boss_ladder_king",
            chapter = 4,
            title = "Boss: Ladder King",
            description = "Defeat the pro boss on the most volatile board.",
            requiredWins = 10,
            gameMode = GameMode.VS_BOT,
            difficulty = GameDifficulty.HARD,
            botPersonality = BotPersonality.PRO,
            boardLayoutId = BoardLayouts.PRO_CHAOS_ID,
            matchMode = MatchModePreset.BEST_OF_THREE,
            reward = RewardBundle(coins = 360, gems = 5, xp = 460),
            isBoss = true
        ),
        CampaignNode(
            id = "team_climb",
            chapter = 3,
            title = "Team Climb",
            description = "Four-player 2v2 pressure with shared victory.",
            requiredWins = 7,
            gameMode = GameMode.LOCAL_MULTIPLAYER,
            difficulty = GameDifficulty.HARD,
            boardLayoutId = BoardLayouts.PRO_CHAOS_ID,
            matchMode = MatchModePreset.TWO_V_TWO,
            reward = RewardBundle(coins = 210, gems = 2, xp = 260)
        )
    )

    fun unlockedNodes(profile: PlayerProfile): List<CampaignNode> {
        return nodes.filter { node ->
            profile.humanWins >= node.requiredWins ||
                node.id in profile.completedCampaignNodeIds ||
                node.requiredWins == 0
        }
    }

    fun nextPlayableNode(profile: PlayerProfile): CampaignNode? {
        val unlocked = unlockedNodes(profile)
        return unlocked.firstOrNull { it.id !in profile.completedCampaignNodeIds }
            ?: unlocked.firstOrNull()
    }

    fun byId(id: String): CampaignNode? = nodes.firstOrNull { it.id == id }
}
