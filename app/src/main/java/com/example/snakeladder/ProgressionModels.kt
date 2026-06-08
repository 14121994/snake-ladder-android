package com.example.snakeladder

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class MatchAnalytics(
    val totalTurns: Int,
    val ladderCount: Int,
    val snakeCount: Int,
    val sixCount: Int,
    val overshootCount: Int,
    val knockBackCount: Int,
    val powerUpCount: Int,
    val trapCount: Int,
    val biggestLadderGain: Int,
    val worstSnakeDrop: Int,
    val winnerName: String?,
    val winningTeamId: Int?
) {
    val summaryLine: String
        get() = "$totalTurns turns | $ladderCount ladders | $snakeCount snakes | $knockBackCount knockbacks | $powerUpCount powers"

    val momentumLine: String
        get() = "Best climb +$biggestLadderGain | Worst slide -$worstSnakeDrop | Sixes $sixCount | Traps $trapCount | Exact misses $overshootCount"

    companion object {
        fun from(state: GameState): MatchAnalytics {
            val events = state.matchEvents
            val turnEvents = events.filter { it.dice in 1..6 && it.moveType != MoveType.POWER_UP }
            val ladderEvents = turnEvents.filter { it.moveType == MoveType.LADDER }
            val snakeEvents = turnEvents.filter { it.moveType == MoveType.SNAKE }
            return MatchAnalytics(
                totalTurns = turnEvents.size,
                ladderCount = ladderEvents.size,
                snakeCount = snakeEvents.size,
                sixCount = turnEvents.count { it.dice == 6 },
                overshootCount = turnEvents.count { it.moveType == MoveType.OVERSHOOT },
                knockBackCount = turnEvents.sumOf { it.knockedBackPlayerIndices.size },
                powerUpCount = events.count { it.powerUpUsed != null } + events.sumOf { it.triggeredPowerUps.size },
                trapCount = turnEvents.count { it.moveType == MoveType.TRAP },
                biggestLadderGain = ladderEvents.maxOfOrNull { it.finalPosition - it.landedPosition } ?: 0,
                worstSnakeDrop = snakeEvents.maxOfOrNull { it.landedPosition - it.finalPosition } ?: 0,
                winnerName = state.winnerIndex?.let { state.players.getOrNull(it)?.name },
                winningTeamId = state.winningTeamId
            )
        }
    }
}

internal data class InMatchMission(
    val id: String,
    val title: String,
    val target: Int,
    val progress: Int
) {
    val completed: Boolean
        get() = progress >= target

    val summary: String
        get() = "$title $progress/$target"
}

internal object InMatchMissionCatalog {
    fun activeFor(state: GameState): List<InMatchMission> {
        val analytics = MatchAnalytics.from(state)
        return buildList {
            add(InMatchMission("roll_six", "Roll a six", 1, minOf(analytics.sixCount, 1)))
            add(InMatchMission("climb_ladder", "Climb ladders", 2, minOf(analytics.ladderCount, 2)))
            if (RuleSets.byId(state.ruleSetId).usesPowerUps) {
                add(InMatchMission("use_power", "Use powers", 2, minOf(analytics.powerUpCount, 2)))
            }
            if (state.matchMode == MatchModePreset.TIME_ATTACK || state.turnLimit != null) {
                val remaining = state.turnsRemaining ?: state.turnLimit ?: 0
                val target = (state.turnLimit ?: 20) / 2
                add(InMatchMission("pace", "Keep tempo", target, minOf(remaining, target)))
            }
            if (state.boardLayoutId == BoardLayouts.PRO_CHAOS_ID || BoardLayouts.byId(state.boardLayoutId).specialTiles.isNotEmpty()) {
                val tileMoves = state.matchEvents.count {
                    it.moveType == MoveType.SHORTCUT ||
                        it.moveType == MoveType.MYSTERY_TILE ||
                        it.moveType == MoveType.RISK_ROUTE ||
                        it.moveType == MoveType.BRANCH_PATH ||
                        it.tileLabel != null
                }
                add(InMatchMission("tile_master", "Use board tiles", 1, minOf(tileMoves, 1)))
            }
        }.take(3)
    }
}

internal data class RewardBundle(
    val coins: Int = 0,
    val gems: Int = 0,
    val xp: Int = 0
) {
    operator fun plus(other: RewardBundle): RewardBundle {
        return RewardBundle(
            coins = coins + other.coins,
            gems = gems + other.gems,
            xp = xp + other.xp
        )
    }

    fun isEmpty(): Boolean = coins == 0 && gems == 0 && xp == 0

    fun summary(): String {
        val parts = buildList {
            if (coins > 0) add("$coins coins")
            if (gems > 0) add("$gems gems")
            if (xp > 0) add("$xp XP")
        }
        return if (parts.isEmpty()) "No reward" else parts.joinToString()
    }
}

internal object RewardEngine {
    fun levelForXp(xp: Int): Int = (xp / 250) + 1

    fun titleForLevel(level: Int): String {
        return when {
            level >= 20 -> "Ladder Legend"
            level >= 12 -> "Board Strategist"
            level >= 7 -> "Snake Tamer"
            level >= 3 -> "Climb Specialist"
            else -> "New Challenger"
        }
    }

    fun baseMatchReward(state: GameState, analytics: MatchAnalytics): RewardBundle {
        val ruleSet = RuleSets.byId(state.ruleSetId)
        val humanWon = state.winnerIndex == 0 ||
            (state.winningTeamId != null && state.players.firstOrNull()?.teamId == state.winningTeamId)
        val winBonus = if (humanWon) 30 else 10
        return RewardBundle(
            coins = (winBonus + analytics.ladderCount * 2 + analytics.powerUpCount).coerceAtLeast(10) * ruleSet.rewardMultiplier,
            xp = (35 + analytics.totalTurns + analytics.knockBackCount * 4) * ruleSet.rewardMultiplier
        )
    }
}

internal data class Achievement(
    val id: String,
    val title: String,
    val description: String
)

internal object AchievementCatalog {
    val all: List<Achievement> = listOf(
        Achievement("first_win", "First Win", "Win any completed match."),
        Achievement("bot_slayer", "Bot Slayer", "Beat the bot in Vs Bot mode."),
        Achievement("ladder_rider", "Ladder Rider", "Climb at least two ladders in one match."),
        Achievement("snake_survivor", "Snake Survivor", "Win a match without being bitten by a snake."),
        Achievement("six_machine", "Six Machine", "Roll three or more sixes in one match."),
        Achievement("knockout_artist", "Knockout Artist", "Knock a rival back to start."),
        Achievement("marathoner", "Marathoner", "Complete a match with at least thirty recorded turns."),
        Achievement("comeback_climber", "Comeback Climber", "Gain at least thirty cells from one ladder."),
        Achievement("exact_finisher", "Exact Finisher", "Win with an exact final roll."),
        Achievement("pro_rules_winner", "Pro Rules Winner", "Win a Hard difficulty match."),
        Achievement("power_player", "Power Player", "Use or trigger at least three power-ups in one match."),
        Achievement("trap_master", "Trap Master", "Win a match where a trap is triggered."),
        Achievement("speed_winner", "Speed Winner", "Win a Time Attack match."),
        Achievement("team_captain", "Team Captain", "Win a team match."),
        Achievement("campaign_climber", "Campaign Climber", "Clear a campaign node.")
    )

    fun evaluate(state: GameState, analytics: MatchAnalytics = MatchAnalytics.from(state)): Set<String> {
        val winnerIndex = state.winnerIndex
        val humanWonVsBot = state.gameMode == GameMode.VS_BOT && winnerIndex == 0
        val winnerEvents = if (winnerIndex == null) {
            emptyList()
        } else {
            state.matchEvents.filter { it.playerIndex == winnerIndex }
        }
        val winnerSnakeCount = winnerEvents.count { it.moveType == MoveType.SNAKE }
        val lastEvent = state.matchEvents.lastOrNull()

        return buildSet {
            if (winnerIndex != null) add("first_win")
            if (humanWonVsBot) add("bot_slayer")
            if (analytics.ladderCount >= 2) add("ladder_rider")
            if (winnerIndex != null && winnerSnakeCount == 0) add("snake_survivor")
            if (analytics.sixCount >= 3) add("six_machine")
            if (analytics.knockBackCount > 0) add("knockout_artist")
            if (analytics.totalTurns >= 30) add("marathoner")
            if (analytics.biggestLadderGain >= 30) add("comeback_climber")
            if (lastEvent?.winner == true && lastEvent.moveType == MoveType.WIN) add("exact_finisher")
            if (winnerIndex != null && state.difficulty == GameDifficulty.HARD) add("pro_rules_winner")
            if (analytics.powerUpCount >= 3) add("power_player")
            if (winnerIndex != null && analytics.trapCount > 0) add("trap_master")
            if (winnerIndex != null && state.matchMode == MatchModePreset.TIME_ATTACK) add("speed_winner")
            if (winnerIndex != null && state.winningTeamId != null) add("team_captain")
            if (winnerIndex != null && !state.campaignNodeId.isNullOrBlank()) add("campaign_climber")
        }
    }

    fun byId(id: String): Achievement? = all.firstOrNull { it.id == id }
}

internal enum class DailyChallengeKind {
    ROLL_SIXES,
    CLIMB_LADDERS,
    WIN_MATCH,
    WIN_WITHOUT_SNAKES,
    KNOCK_BACK_RIVAL
}

internal data class DailyChallenge(
    val id: String,
    val dateKey: String,
    val title: String,
    val description: String,
    val target: Int,
    val kind: DailyChallengeKind,
    val boardLayoutId: String = BoardLayouts.QUICK_CLIMB_ID,
    val matchMode: MatchModePreset = MatchModePreset.DAILY_CHALLENGE,
    val botPersonality: BotPersonality = BotPersonality.STEADY,
    val reward: RewardBundle = RewardBundle(coins = 45, xp = 60)
)

internal object DailyChallengeCatalog {
    private val templates = listOf(
        DailyChallengeTemplate(
            title = "Roll With Power",
            description = "Roll two sixes across completed matches today.",
            target = 2,
            kind = DailyChallengeKind.ROLL_SIXES,
            boardLayoutId = BoardLayouts.SPEED_RUN_ID,
            botPersonality = BotPersonality.RISKY
        ),
        DailyChallengeTemplate(
            title = "Find The Ladders",
            description = "Climb two ladders across completed matches today.",
            target = 2,
            kind = DailyChallengeKind.CLIMB_LADDERS,
            boardLayoutId = BoardLayouts.LADDER_LEAGUE_ID,
            botPersonality = BotPersonality.STEADY
        ),
        DailyChallengeTemplate(
            title = "Finish A Match",
            description = "Complete one match today.",
            target = 1,
            kind = DailyChallengeKind.WIN_MATCH,
            boardLayoutId = BoardLayouts.FAMILY_SHORT_ID,
            botPersonality = BotPersonality.STEADY
        ),
        DailyChallengeTemplate(
            title = "Clean Escape",
            description = "Win one match without your winning player hitting a snake.",
            target = 1,
            kind = DailyChallengeKind.WIN_WITHOUT_SNAKES,
            boardLayoutId = BoardLayouts.SNAKE_DEN_ID,
            botPersonality = BotPersonality.DEFENSIVE
        ),
        DailyChallengeTemplate(
            title = "Send Them Back",
            description = "Knock one rival back to start today.",
            target = 1,
            kind = DailyChallengeKind.KNOCK_BACK_RIVAL,
            boardLayoutId = BoardLayouts.TRAP_VALLEY_ID,
            matchMode = MatchModePreset.PARTY_RULES,
            botPersonality = BotPersonality.RISKY,
            reward = RewardBundle(coins = 70, gems = 1, xp = 85)
        )
    )

    fun today(nowMillis: Long = System.currentTimeMillis()): DailyChallenge {
        val dateKey = todayKey(nowMillis)
        val index = (dateKey.toIntOrNull() ?: 0).mod(templates.size)
        val template = templates[index]
        return DailyChallenge(
            id = "${dateKey}_${template.kind.name.lowercase()}",
            dateKey = dateKey,
            title = template.title,
            description = template.description,
            target = template.target,
            kind = template.kind,
            boardLayoutId = template.boardLayoutId,
            matchMode = template.matchMode,
            botPersonality = template.botPersonality,
            reward = template.reward
        )
    }

    fun weeklyCalendar(nowMillis: Long = System.currentTimeMillis()): List<DailyChallenge> {
        val dayMillis = 86_400_000L
        return (0..6).map { offset -> today(nowMillis + (offset * dayMillis)) }
    }

    fun progressFromMatch(state: GameState, challenge: DailyChallenge): Int {
        val analytics = MatchAnalytics.from(state)
        return when (challenge.kind) {
            DailyChallengeKind.ROLL_SIXES -> analytics.sixCount
            DailyChallengeKind.CLIMB_LADDERS -> analytics.ladderCount
            DailyChallengeKind.WIN_MATCH -> if (state.winnerIndex != null) 1 else 0
            DailyChallengeKind.WIN_WITHOUT_SNAKES -> if (wonWithoutSnake(state)) 1 else 0
            DailyChallengeKind.KNOCK_BACK_RIVAL -> analytics.knockBackCount
        }.coerceAtMost(challenge.target)
    }

    private fun todayKey(nowMillis: Long): String {
        return SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(nowMillis))
    }

    private fun wonWithoutSnake(state: GameState): Boolean {
        val winnerIndex = state.winnerIndex ?: return false
        return state.matchEvents.none { event ->
            event.playerIndex == winnerIndex && event.moveType == MoveType.SNAKE
        }
    }
}

private data class DailyChallengeTemplate(
    val title: String,
    val description: String,
    val target: Int,
    val kind: DailyChallengeKind,
    val boardLayoutId: String,
    val matchMode: MatchModePreset = MatchModePreset.DAILY_CHALLENGE,
    val botPersonality: BotPersonality,
    val reward: RewardBundle = RewardBundle(coins = 45, xp = 60)
)

internal data class PlayerProfile(
    val xp: Int = 0,
    val coins: Int = 0,
    val gems: Int = 0,
    val selectedAvatarId: String = "classic_token",
    val selectedTitle: String = "New Challenger",
    val unlockedTitleIds: Set<String> = setOf("title_new_challenger"),
    val unlockedAvatarIds: Set<String> = setOf("classic_token"),
    val unlockedBoardIds: Set<String> = setOf(BoardLayouts.CLASSIC_ID),
    val matchesStarted: Int = 0,
    val matchesCompleted: Int = 0,
    val humanWins: Int = 0,
    val vsBotWins: Int = 0,
    val multiplayerWins: Int = 0,
    val currentWinStreak: Int = 0,
    val bestWinStreak: Int = 0,
    val totalRolls: Int = 0,
    val totalLadders: Int = 0,
    val totalSnakes: Int = 0,
    val totalSixes: Int = 0,
    val totalKnockBacks: Int = 0,
    val totalOvershoots: Int = 0,
    val unlockedAchievementIds: Set<String> = emptySet(),
    val dailyChallengeId: String = "",
    val dailyChallengeDateKey: String = "",
    val dailyChallengeProgress: Int = 0,
    val dailyChallengeCompleted: Boolean = false,
    val dailyStreak: Int = 0,
    val bestDailyStreak: Int = 0,
    val lastDailyChallengeDateKey: String = "",
    val recentMatches: List<ProfileMatchSummary> = emptyList(),
    val completedDailyChallengeIds: Set<String> = emptySet(),
    val completedCampaignNodeIds: Set<String> = emptySet(),
    val schemaVersion: Int = PLAYER_PROFILE_SCHEMA_VERSION
) {
    val level: Int
        get() = RewardEngine.levelForXp(xp)

    val title: String
        get() = selectedTitle.ifBlank { RewardEngine.titleForLevel(level) }

    fun progressFor(challenge: DailyChallenge): Int {
        return if (dailyChallengeId == challenge.id && dailyChallengeDateKey == challenge.dateKey) {
            dailyChallengeProgress.coerceAtMost(challenge.target)
        } else {
            0
        }
    }

    fun dailyCompletedFor(challenge: DailyChallenge): Boolean {
        return dailyChallengeId == challenge.id &&
            dailyChallengeDateKey == challenge.dateKey &&
            dailyChallengeCompleted
    }
}

internal fun PlayerProfile.recordStartedMatch(): PlayerProfile {
    return copy(
        matchesStarted = matchesStarted + 1,
        schemaVersion = PLAYER_PROFILE_SCHEMA_VERSION
    )
}

internal data class ProfileMatchSummary(
    val completedAt: Long,
    val modeLabel: String,
    val winnerName: String,
    val turns: Int,
    val ladders: Int,
    val snakes: Int,
    val powerUps: Int = 0,
    val coinsEarned: Int = 0,
    val xpEarned: Int = 0,
    val boardLayoutId: String,
    val matchMode: MatchModePreset
)

internal data class ProfileUpdateResult(
    val profile: PlayerProfile,
    val analytics: MatchAnalytics,
    val newlyUnlockedAchievements: List<Achievement>,
    val dailyChallengeCompletedNow: Boolean,
    val reward: RewardBundle
)

internal fun PlayerProfile.recordCompletedMatch(
    state: GameState,
    challenge: DailyChallenge = DailyChallengeCatalog.today()
): ProfileUpdateResult {
    val analytics = MatchAnalytics.from(state)
    val winnerIndex = state.winnerIndex
    val humanTeamId = state.players.firstOrNull()?.teamId
    val humanWon = winnerIndex == 0 || (state.winningTeamId != null && state.winningTeamId == humanTeamId)
    val wonVsBot = humanWon && state.gameMode == GameMode.VS_BOT
    val wonMultiplayer = humanWon && state.gameMode == GameMode.LOCAL_MULTIPLAYER
    val evaluatedAchievementIds = AchievementCatalog.evaluate(state, analytics)
    val unlockedIds = unlockedAchievementIds + evaluatedAchievementIds
    val newlyUnlocked = (evaluatedAchievementIds - unlockedAchievementIds)
        .mapNotNull { AchievementCatalog.byId(it) }

    val previousDailyProgress = progressFor(challenge)
    val matchDailyProgress = DailyChallengeCatalog.progressFromMatch(state, challenge)
    val newDailyProgress = (previousDailyProgress + matchDailyProgress).coerceAtMost(challenge.target)
    val dailyCompleted = newDailyProgress >= challenge.target
    val dailyCompletedNow = dailyCompleted && !dailyCompletedFor(challenge)
    val campaignNode = state.campaignNodeId?.let { CampaignCatalog.byId(it) }
    val campaignCompletedNow = humanWon &&
        campaignNode != null &&
        campaignNode.id !in completedCampaignNodeIds
    val completedCampaignNodeId = campaignNode?.id?.takeIf { campaignCompletedNow }
    val baseReward = if (winnerIndex != null) RewardEngine.baseMatchReward(state, analytics) else RewardBundle()
    val achievementReward = RewardBundle(coins = newlyUnlocked.size * 15, gems = newlyUnlocked.size / 4, xp = newlyUnlocked.size * 25)
    val dailyReward = if (dailyCompletedNow) challenge.reward else RewardBundle()
    val campaignReward = if (campaignCompletedNow) campaignNode?.reward ?: RewardBundle() else RewardBundle()
    val totalReward = baseReward + achievementReward + dailyReward + campaignReward
    val nextXp = xp + totalReward.xp
    val nextLevel = RewardEngine.levelForXp(nextXp)
    val nextTitle = RewardEngine.titleForLevel(nextLevel)
    val newlyUnlockedBoards = if (nextLevel >= 4) {
        unlockedBoardIds + BoardLayouts.all.map { it.id }
    } else {
        unlockedBoardIds + state.boardLayoutId
    }
    val newlyUnlockedAvatars = unlockedAvatarIds + when {
        nextLevel >= 8 -> setOf("classic_token", "cobra_token", "ladder_king", "gold_die")
        nextLevel >= 4 -> setOf("classic_token", "cobra_token", "ladder_king")
        else -> setOf("classic_token")
    }
    val newlyUnlockedTitles = unlockedTitleIds + titleUnlockId(nextTitle)
    val nextDailyStreak = when {
        !dailyCompletedNow -> dailyStreak
        lastDailyChallengeDateKey == challenge.dateKey -> dailyStreak
        else -> dailyStreak + 1
    }
    val matchSummary = ProfileMatchSummary(
        completedAt = System.currentTimeMillis(),
        modeLabel = "${state.gameMode.name.lowercase().replace('_', ' ')} / ${state.matchMode.label}",
        winnerName = analytics.winnerName ?: "No winner",
        turns = analytics.totalTurns,
        ladders = analytics.ladderCount,
        snakes = analytics.snakeCount,
        powerUps = analytics.powerUpCount,
        coinsEarned = totalReward.coins,
        xpEarned = totalReward.xp,
        boardLayoutId = state.boardLayoutId,
        matchMode = state.matchMode
    )

    val completedIncrement = if (winnerIndex != null) 1 else 0
    val updatedProfile = copy(
        xp = nextXp,
        coins = coins + totalReward.coins,
        gems = gems + totalReward.gems,
        selectedTitle = nextTitle,
        unlockedTitleIds = newlyUnlockedTitles,
        unlockedAvatarIds = newlyUnlockedAvatars,
        unlockedBoardIds = newlyUnlockedBoards,
        matchesStarted = maxOf(matchesStarted, matchesCompleted + completedIncrement),
        matchesCompleted = matchesCompleted + completedIncrement,
        humanWins = humanWins + if (humanWon) 1 else 0,
        vsBotWins = vsBotWins + if (wonVsBot) 1 else 0,
        multiplayerWins = multiplayerWins + if (wonMultiplayer) 1 else 0,
        currentWinStreak = if (humanWon) currentWinStreak + 1 else 0,
        bestWinStreak = if (humanWon) maxOf(bestWinStreak, currentWinStreak + 1) else bestWinStreak,
        totalRolls = totalRolls + analytics.totalTurns,
        totalLadders = totalLadders + analytics.ladderCount,
        totalSnakes = totalSnakes + analytics.snakeCount,
        totalSixes = totalSixes + analytics.sixCount,
        totalKnockBacks = totalKnockBacks + analytics.knockBackCount,
        totalOvershoots = totalOvershoots + analytics.overshootCount,
        unlockedAchievementIds = unlockedIds,
        dailyChallengeId = challenge.id,
        dailyChallengeDateKey = challenge.dateKey,
        dailyChallengeProgress = newDailyProgress,
        dailyChallengeCompleted = dailyCompleted,
        dailyStreak = nextDailyStreak,
        bestDailyStreak = maxOf(bestDailyStreak, nextDailyStreak),
        lastDailyChallengeDateKey = if (dailyCompletedNow) challenge.dateKey else lastDailyChallengeDateKey,
        recentMatches = (listOf(matchSummary) + recentMatches).take(12),
        completedDailyChallengeIds = if (dailyCompleted) completedDailyChallengeIds + challenge.id else completedDailyChallengeIds,
        completedCampaignNodeIds = completedCampaignNodeId?.let { completedCampaignNodeIds + it } ?: completedCampaignNodeIds,
        schemaVersion = PLAYER_PROFILE_SCHEMA_VERSION
    )

    return ProfileUpdateResult(
        profile = updatedProfile,
        analytics = analytics,
        newlyUnlockedAchievements = newlyUnlocked,
        dailyChallengeCompletedNow = dailyCompletedNow,
        reward = totalReward
    )
}

internal fun titleUnlockId(title: String): String {
    return "title_${title.trim().lowercase().replace(" ", "_")}"
}
