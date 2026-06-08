package com.example.snakeladder

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal object PlayerProfileStore {
    private const val PREFS_NAME = "snake_ladder_player_profile"
    private const val KEY_PROFILE = "profile"

    fun load(context: Context): PlayerProfile {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PROFILE, null) ?: return PlayerProfile()
        return importProfile(raw) ?: PlayerProfile().also { recovered ->
            save(context, recovered)
        }
    }

    fun save(context: Context, profile: PlayerProfile) {
        val migrated = migrate(profile)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PROFILE, toJson(migrated).toString())
            .apply()
    }

    fun reset(context: Context): PlayerProfile {
        val profile = PlayerProfile()
        save(context, profile)
        return profile
    }

    fun exportProfile(profile: PlayerProfile): String {
        return toJson(migrate(profile)).toString(2)
    }

    fun importProfile(raw: String): PlayerProfile? {
        return try {
            migrate(fromJson(JSONObject(raw)))
        } catch (_: Exception) {
            null
        }
    }

    fun importAndSave(context: Context, raw: String): PlayerProfile? {
        val profile = importProfile(raw) ?: return null
        save(context, profile)
        return profile
    }

    fun maintenanceSummary(profile: PlayerProfile): String {
        val migrated = migrate(profile)
        return "Schema ${migrated.schemaVersion} | Level ${migrated.level} | " +
            "${migrated.unlockedAvatarIds.size} avatars | ${migrated.unlockedBoardIds.size} boards | " +
            "${migrated.matchesStarted} started | ${migrated.recentMatches.size} recent matches"
    }

    fun recordStartedMatch(context: Context): PlayerProfile {
        val profile = load(context).recordStartedMatch()
        save(context, profile)
        return profile
    }

    fun recordCompletedMatch(
        context: Context,
        state: GameState,
        challenge: DailyChallenge = DailyChallengeCatalog.today()
    ): ProfileUpdateResult {
        val result = load(context).recordCompletedMatch(state, challenge)
        save(context, result.profile)
        return result
    }

    private fun toJson(profile: PlayerProfile): JSONObject {
        return JSONObject().apply {
            put("schemaVersion", PLAYER_PROFILE_SCHEMA_VERSION)
            put("xp", profile.xp)
            put("coins", profile.coins)
            put("gems", profile.gems)
            put("selectedAvatarId", profile.selectedAvatarId)
            put("selectedTitle", profile.selectedTitle)
            put("unlockedTitleIds", JSONArray().apply {
                profile.unlockedTitleIds.sorted().forEach { put(it) }
            })
            put("unlockedAvatarIds", JSONArray().apply {
                profile.unlockedAvatarIds.sorted().forEach { put(it) }
            })
            put("unlockedBoardIds", JSONArray().apply {
                profile.unlockedBoardIds.sorted().forEach { put(it) }
            })
            put("matchesStarted", profile.matchesStarted)
            put("matchesCompleted", profile.matchesCompleted)
            put("humanWins", profile.humanWins)
            put("vsBotWins", profile.vsBotWins)
            put("multiplayerWins", profile.multiplayerWins)
            put("currentWinStreak", profile.currentWinStreak)
            put("bestWinStreak", profile.bestWinStreak)
            put("totalRolls", profile.totalRolls)
            put("totalLadders", profile.totalLadders)
            put("totalSnakes", profile.totalSnakes)
            put("totalSixes", profile.totalSixes)
            put("totalKnockBacks", profile.totalKnockBacks)
            put("totalOvershoots", profile.totalOvershoots)
            put("unlockedAchievementIds", JSONArray().apply {
                profile.unlockedAchievementIds.sorted().forEach { put(it) }
            })
            put("dailyChallengeId", profile.dailyChallengeId)
            put("dailyChallengeDateKey", profile.dailyChallengeDateKey)
            put("dailyChallengeProgress", profile.dailyChallengeProgress)
            put("dailyChallengeCompleted", profile.dailyChallengeCompleted)
            put("dailyStreak", profile.dailyStreak)
            put("bestDailyStreak", profile.bestDailyStreak)
            put("lastDailyChallengeDateKey", profile.lastDailyChallengeDateKey)
            put("completedDailyChallengeIds", JSONArray().apply {
                profile.completedDailyChallengeIds.sorted().forEach { put(it) }
            })
            put("completedCampaignNodeIds", JSONArray().apply {
                profile.completedCampaignNodeIds.sorted().forEach { put(it) }
            })
            put("recentMatches", JSONArray().apply {
                profile.recentMatches.forEach { summary ->
                    put(JSONObject().apply {
                        put("completedAt", summary.completedAt)
                        put("modeLabel", summary.modeLabel)
                        put("winnerName", summary.winnerName)
                        put("turns", summary.turns)
                        put("ladders", summary.ladders)
                        put("snakes", summary.snakes)
                        put("powerUps", summary.powerUps)
                        put("coinsEarned", summary.coinsEarned)
                        put("xpEarned", summary.xpEarned)
                        put("boardLayoutId", summary.boardLayoutId)
                        put("matchMode", summary.matchMode.name)
                    })
                }
            })
        }
    }

    private fun fromJson(json: JSONObject): PlayerProfile {
        return PlayerProfile(
            xp = json.optInt("xp", 0),
            coins = json.optInt("coins", 0),
            gems = json.optInt("gems", 0),
            selectedAvatarId = json.optString("selectedAvatarId", "classic_token"),
            selectedTitle = json.optString("selectedTitle", ""),
            unlockedTitleIds = json.optStringSet("unlockedTitleIds").ifEmpty { setOf("title_new_challenger") },
            unlockedAvatarIds = json.optStringSet("unlockedAvatarIds").ifEmpty { setOf("classic_token") },
            unlockedBoardIds = json.optStringSet("unlockedBoardIds").ifEmpty { setOf(BoardLayouts.CLASSIC_ID) },
            matchesStarted = json.optInt("matchesStarted", 0),
            matchesCompleted = json.optInt("matchesCompleted", 0),
            humanWins = json.optInt("humanWins", 0),
            vsBotWins = json.optInt("vsBotWins", 0),
            multiplayerWins = json.optInt("multiplayerWins", 0),
            currentWinStreak = json.optInt("currentWinStreak", 0),
            bestWinStreak = json.optInt("bestWinStreak", 0),
            totalRolls = json.optInt("totalRolls", 0),
            totalLadders = json.optInt("totalLadders", 0),
            totalSnakes = json.optInt("totalSnakes", 0),
            totalSixes = json.optInt("totalSixes", 0),
            totalKnockBacks = json.optInt("totalKnockBacks", 0),
            totalOvershoots = json.optInt("totalOvershoots", 0),
            unlockedAchievementIds = json.optStringSet("unlockedAchievementIds"),
            dailyChallengeId = json.optString("dailyChallengeId", ""),
            dailyChallengeDateKey = json.optString("dailyChallengeDateKey", ""),
            dailyChallengeProgress = json.optInt("dailyChallengeProgress", 0),
            dailyChallengeCompleted = json.optBoolean("dailyChallengeCompleted", false),
            dailyStreak = json.optInt("dailyStreak", 0),
            bestDailyStreak = json.optInt("bestDailyStreak", 0),
            lastDailyChallengeDateKey = json.optString("lastDailyChallengeDateKey", ""),
            recentMatches = json.optRecentMatches("recentMatches"),
            completedDailyChallengeIds = json.optStringSet("completedDailyChallengeIds"),
            completedCampaignNodeIds = json.optStringSet("completedCampaignNodeIds"),
            schemaVersion = json.optInt("schemaVersion", 1)
        )
    }

    private fun migrate(profile: PlayerProfile): PlayerProfile {
        val safeXp = profile.xp.coerceAtLeast(0)
        val safeCoins = profile.coins.coerceAtLeast(0)
        val safeGems = profile.gems.coerceAtLeast(0)
        val knownBoardIds = BoardLayouts.all.map { it.id }.toSet()
        val unlockedBoards = (profile.unlockedBoardIds.filter { it in knownBoardIds } + BoardLayouts.CLASSIC_ID).toSet()
        val unlockedAvatars = (profile.unlockedAvatarIds.filter { it.isNotBlank() } + "classic_token").toSet()
        val selectedAvatar = profile.selectedAvatarId.takeIf { it in unlockedAvatars } ?: "classic_token"
        val level = RewardEngine.levelForXp(safeXp)
        val title = profile.selectedTitle.ifBlank { RewardEngine.titleForLevel(level) }
        val unlockedTitles = (profile.unlockedTitleIds.filter { it.isNotBlank() } + titleUnlockId(title) + "title_new_challenger").toSet()
        return profile.copy(
            xp = safeXp,
            coins = safeCoins,
            gems = safeGems,
            selectedAvatarId = selectedAvatar,
            selectedTitle = title,
            unlockedTitleIds = unlockedTitles,
            unlockedAvatarIds = unlockedAvatars,
            unlockedBoardIds = unlockedBoards,
            matchesStarted = maxOf(profile.matchesStarted, profile.matchesCompleted).coerceAtLeast(0),
            matchesCompleted = profile.matchesCompleted.coerceAtLeast(0),
            humanWins = profile.humanWins.coerceAtLeast(0),
            vsBotWins = profile.vsBotWins.coerceAtLeast(0),
            multiplayerWins = profile.multiplayerWins.coerceAtLeast(0),
            currentWinStreak = profile.currentWinStreak.coerceAtLeast(0),
            bestWinStreak = profile.bestWinStreak.coerceAtLeast(0),
            totalRolls = profile.totalRolls.coerceAtLeast(0),
            totalLadders = profile.totalLadders.coerceAtLeast(0),
            totalSnakes = profile.totalSnakes.coerceAtLeast(0),
            totalSixes = profile.totalSixes.coerceAtLeast(0),
            totalKnockBacks = profile.totalKnockBacks.coerceAtLeast(0),
            totalOvershoots = profile.totalOvershoots.coerceAtLeast(0),
            dailyChallengeProgress = profile.dailyChallengeProgress.coerceAtLeast(0),
            dailyStreak = profile.dailyStreak.coerceAtLeast(0),
            bestDailyStreak = profile.bestDailyStreak.coerceAtLeast(0),
            recentMatches = profile.recentMatches.take(12),
            schemaVersion = PLAYER_PROFILE_SCHEMA_VERSION
        )
    }
}

private fun JSONObject.optStringSet(key: String): Set<String> {
    if (!has(key) || isNull(key)) return emptySet()
    val arr = optJSONArray(key) ?: return emptySet()
    return buildSet {
        for (i in 0 until arr.length()) {
            val value = arr.optString(i)
            if (value.isNotBlank()) add(value)
        }
    }
}

private fun JSONObject.optRecentMatches(key: String): List<ProfileMatchSummary> {
    if (!has(key) || isNull(key)) return emptyList()
    val arr = optJSONArray(key) ?: return emptyList()
    return buildList {
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            add(
                ProfileMatchSummary(
                    completedAt = item.optLong("completedAt", 0L),
                    modeLabel = item.optString("modeLabel", "Classic"),
                    winnerName = item.optString("winnerName", "No winner"),
                    turns = item.optInt("turns", 0),
                    ladders = item.optInt("ladders", 0),
                    snakes = item.optInt("snakes", 0),
                    powerUps = item.optInt("powerUps", 0),
                    coinsEarned = item.optInt("coinsEarned", 0),
                    xpEarned = item.optInt("xpEarned", 0),
                    boardLayoutId = item.optString("boardLayoutId", BoardLayouts.CLASSIC_ID),
                    matchMode = runCatching {
                        MatchModePreset.valueOf(item.optString("matchMode", MatchModePreset.CLASSIC.name))
                    }.getOrDefault(MatchModePreset.CLASSIC)
                )
            )
        }
    }
}
