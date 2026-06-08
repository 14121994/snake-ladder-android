package com.example.snakeladder

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionModelsTest {

    private fun sampleWinningState(
        events: List<MatchEvent>,
        mode: GameMode = GameMode.LOCAL_MULTIPLAYER,
        difficulty: GameDifficulty = GameDifficulty.EASY
    ): GameState {
        return GameState(
            players = listOf(
                PlayerState("Player 1", Color.Red, 100),
                PlayerState(if (mode == GameMode.VS_BOT) "Rival Bot" else "Player 2", Color.Blue, 60)
            ),
            currentPlayerIndex = 0,
            lastDiceRoll = events.lastOrNull()?.dice,
            statusMessage = "Player 1 wins!",
            bonusTurnGranted = false,
            winnerIndex = 0,
            moveHistory = emptyList(),
            gameMode = mode,
            botPlayerIndex = if (mode == GameMode.VS_BOT) 1 else null,
            lastMovePlayerIndex = 0,
            lastMovePath = events.lastOrNull()?.path ?: emptyList(),
            lastMoveType = MoveType.WIN,
            moveSignal = events.size,
            difficulty = difficulty,
            matchEvents = events
        )
    }

    @Test
    fun matchAnalytics_countsStructuredEvents() {
        val state = sampleWinningState(
            events = listOf(
                MatchEvent(1, 0, "Player 1", 6, 1, 7, 14, MoveType.LADDER, listOf(2, 3, 4, 5, 6, 7, 14), bonusTurn = true),
                MatchEvent(2, 0, "Player 1", 6, 14, 20, 20, MoveType.NORMAL, listOf(15, 16, 17, 18, 19, 20), bonusTurn = true),
                MatchEvent(3, 1, "Player 2", 3, 95, 98, 54, MoveType.SNAKE, listOf(96, 97, 98, 54)),
                MatchEvent(4, 0, "Player 1", 4, 96, 100, 100, MoveType.WIN, listOf(97, 98, 99, 100), winner = true)
            )
        )

        val analytics = MatchAnalytics.from(state)

        assertEquals(4, analytics.totalTurns)
        assertEquals(1, analytics.ladderCount)
        assertEquals(1, analytics.snakeCount)
        assertEquals(2, analytics.sixCount)
        assertEquals(7, analytics.biggestLadderGain)
        assertEquals(44, analytics.worstSnakeDrop)
    }

    @Test
    fun profileRecordCompletedMatch_updatesStatsAchievementsAndDailyChallenge() {
        val challenge = DailyChallenge(
            id = "20260601_roll_sixes",
            dateKey = "20260601",
            title = "Roll With Power",
            description = "Roll two sixes.",
            target = 2,
            kind = DailyChallengeKind.ROLL_SIXES
        )
        val state = sampleWinningState(
            mode = GameMode.VS_BOT,
            difficulty = GameDifficulty.HARD,
            events = listOf(
                MatchEvent(1, 0, "Player 1", 6, 1, 7, 14, MoveType.LADDER, listOf(2, 3, 4, 5, 6, 7, 14), bonusTurn = true),
                MatchEvent(2, 0, "Player 1", 6, 14, 20, 20, MoveType.NORMAL, listOf(15, 16, 17, 18, 19, 20), bonusTurn = true),
                MatchEvent(3, 0, "Player 1", 4, 96, 100, 100, MoveType.WIN, listOf(97, 98, 99, 100), winner = true)
            )
        )

        val result = PlayerProfile().recordCompletedMatch(state, challenge)

        assertEquals(1, result.profile.matchesStarted)
        assertEquals(1, result.profile.matchesCompleted)
        assertEquals(1, result.profile.humanWins)
        assertEquals(1, result.profile.vsBotWins)
        assertEquals(2, result.profile.totalSixes)
        assertEquals(2, result.profile.dailyChallengeProgress)
        assertTrue(result.profile.dailyChallengeCompleted)
        assertTrue(result.dailyChallengeCompletedNow)
        assertTrue(result.reward.coins > 0)
        assertTrue(result.profile.xp > 0)
        assertTrue(result.profile.coins > 0)
        assertTrue("first_win" in result.profile.unlockedAchievementIds)
        assertTrue("bot_slayer" in result.profile.unlockedAchievementIds)
        assertTrue("pro_rules_winner" in result.profile.unlockedAchievementIds)
    }

    @Test
    fun profileRecordStartedMatch_tracksStartsWithoutCompletingMatch() {
        val profile = PlayerProfile(matchesStarted = 2, matchesCompleted = 1)

        val started = profile.recordStartedMatch()

        assertEquals(3, started.matchesStarted)
        assertEquals(1, started.matchesCompleted)
        assertEquals(PLAYER_PROFILE_SCHEMA_VERSION, started.schemaVersion)
    }

    @Test
    fun profileRecordCompletedCampaignMatch_addsCampaignRewardsAndCompletion() {
        val state = sampleWinningState(
            events = listOf(
                MatchEvent(1, 0, "Player 1", 3, 97, 100, 100, MoveType.WIN, listOf(98, 99, 100), winner = true)
            )
        ).copy(campaignNodeId = "classic_start")

        val result = PlayerProfile().recordCompletedMatch(state)

        assertTrue("classic_start" in result.profile.completedCampaignNodeIds)
        assertTrue(result.reward.xp >= CampaignCatalog.byId("classic_start")!!.reward.xp)
        assertTrue("campaign_climber" in result.profile.unlockedAchievementIds)
    }

    @Test
    fun rewardBundleAndTitleThresholdsCoverDisplayBranches() {
        assertTrue(RewardBundle().isEmpty())
        assertEquals("No reward", RewardBundle().summary())
        assertEquals("12 coins", RewardBundle(coins = 12).summary())
        assertEquals("2 gems", RewardBundle(gems = 2).summary())
        assertEquals("40 XP", RewardBundle(xp = 40).summary())
        assertEquals("12 coins, 2 gems, 40 XP", RewardBundle(coins = 12, gems = 2, xp = 40).summary())
        assertFalse(RewardBundle(coins = 1).isEmpty())
        assertFalse(RewardBundle(gems = 1).isEmpty())
        assertFalse(RewardBundle(xp = 1).isEmpty())

        assertEquals("New Challenger", RewardEngine.titleForLevel(1))
        assertEquals("Climb Specialist", RewardEngine.titleForLevel(3))
        assertEquals("Snake Tamer", RewardEngine.titleForLevel(7))
        assertEquals("Board Strategist", RewardEngine.titleForLevel(12))
        assertEquals("Ladder Legend", RewardEngine.titleForLevel(20))
        assertEquals("Climb Specialist", PlayerProfile(xp = 500, selectedTitle = "").title)
    }

    @Test
    fun achievementCatalogEvaluatesWinLossAndHighSignalMatchBranches() {
        val events = buildList {
            add(MatchEvent(1, 0, "Player 1", 6, 1, 7, 38, MoveType.LADDER, listOf(2, 3, 4, 5, 6, 7, 38), bonusTurn = true))
            add(MatchEvent(2, 0, "Player 1", 6, 38, 44, 84, MoveType.LADDER, listOf(39, 40, 41, 42, 43, 44, 84), bonusTurn = true))
            add(MatchEvent(3, 0, "Player 1", 6, 84, 90, 90, MoveType.NORMAL, listOf(85, 86, 87, 88, 89, 90), triggeredPowerUps = listOf(PowerUpType.DICE_BOOST, PowerUpType.SHIELD)))
            add(MatchEvent(4, 1, "Rival Bot", 3, 95, 98, 54, MoveType.SNAKE, listOf(96, 97, 98, 54)))
            add(MatchEvent(5, 0, "Player 1", 4, 90, 94, 94, MoveType.TRAP, listOf(91, 92, 93, 94), knockedBackPlayerIndices = listOf(1), triggeredPowerUps = listOf(PowerUpType.TRAP)))
            repeat(24) { index ->
                add(MatchEvent(6 + index, index % 2, "Player ${(index % 2) + 1}", 2, 10, 12, 12, MoveType.NORMAL, listOf(11, 12)))
            }
            add(MatchEvent(30, 0, "Player 1", 3, 97, 100, 100, MoveType.WIN, listOf(98, 99, 100), winner = true))
        }
        val state = sampleWinningState(events, mode = GameMode.VS_BOT, difficulty = GameDifficulty.HARD).copy(
            players = listOf(
                PlayerState("Player 1", Color.Red, 100, teamId = 0),
                PlayerState("Rival Bot", Color.Blue, 54, teamId = 1)
            ),
            matchMode = MatchModePreset.TIME_ATTACK,
            winningTeamId = 0,
            campaignNodeId = "classic_start"
        )

        val unlocked = AchievementCatalog.evaluate(state)
        val noWinner = AchievementCatalog.evaluate(state.copy(winnerIndex = null, winningTeamId = null, campaignNodeId = null))

        assertTrue(
            setOf(
                "first_win",
                "bot_slayer",
                "ladder_rider",
                "snake_survivor",
                "six_machine",
                "knockout_artist",
                "marathoner",
                "comeback_climber",
                "exact_finisher",
                "pro_rules_winner",
                "power_player",
                "trap_master",
                "speed_winner",
                "team_captain",
                "campaign_climber"
            ).all { it in unlocked }
        )
        assertFalse("first_win" in noWinner)
        assertFalse("snake_survivor" in noWinner)
        assertFalse("exact_finisher" in AchievementCatalog.evaluate(state.copy(matchEvents = state.matchEvents.dropLast(1))))
    }

    @Test
    fun dailyChallengeProgressCoversEveryObjectiveAndCapsTarget() {
        val state = sampleWinningState(
            events = listOf(
                MatchEvent(1, 0, "Player 1", 6, 1, 7, 14, MoveType.LADDER, listOf(2, 3, 4, 5, 6, 7, 14), bonusTurn = true),
                MatchEvent(2, 0, "Player 1", 6, 14, 20, 20, MoveType.LADDER, listOf(15, 16, 17, 18, 19, 20), bonusTurn = true),
                MatchEvent(3, 0, "Player 1", 6, 20, 26, 26, MoveType.NORMAL, listOf(21, 22, 23, 24, 25, 26), knockedBackPlayerIndices = listOf(1)),
                MatchEvent(4, 0, "Player 1", 3, 97, 100, 100, MoveType.WIN, listOf(98, 99, 100), winner = true)
            )
        )
        fun challenge(kind: DailyChallengeKind, target: Int = 1): DailyChallenge {
            return DailyChallenge("id_$kind", "20260603", kind.name, kind.name, target, kind)
        }

        assertEquals(2, DailyChallengeCatalog.progressFromMatch(state, challenge(DailyChallengeKind.ROLL_SIXES, target = 2)))
        assertEquals(1, DailyChallengeCatalog.progressFromMatch(state, challenge(DailyChallengeKind.CLIMB_LADDERS, target = 1)))
        assertEquals(1, DailyChallengeCatalog.progressFromMatch(state, challenge(DailyChallengeKind.WIN_MATCH)))
        assertEquals(1, DailyChallengeCatalog.progressFromMatch(state, challenge(DailyChallengeKind.KNOCK_BACK_RIVAL)))
        assertEquals(0, DailyChallengeCatalog.progressFromMatch(state.copy(winnerIndex = null), challenge(DailyChallengeKind.WIN_MATCH)))
        assertEquals(7, DailyChallengeCatalog.weeklyCalendar(nowMillis = 1_780_294_400_000L).map { it.id }.distinct().size)
    }

    @Test
    fun matchAnalyticsAndMissionsCoverFallbackAndTileBranches() {
        val normalOnly = sampleWinningState(
            events = listOf(
                MatchEvent(1, 0, "Player 1", 2, 1, 3, 3, MoveType.NORMAL, listOf(2, 3))
            )
        ).copy(winnerIndex = null)
        val analytics = MatchAnalytics.from(normalOnly)

        assertEquals(0, analytics.biggestLadderGain)
        assertEquals(0, analytics.worstSnakeDrop)
        assertEquals(null, analytics.winnerName)
        assertTrue(analytics.summaryLine.contains("1 turns"))
        assertTrue(analytics.momentumLine.contains("Worst slide -0"))

        val turnLimitOnly = normalOnly.copy(matchMode = MatchModePreset.CLASSIC, turnLimit = 10, turnsRemaining = null)
        val timeModeNoLimit = normalOnly.copy(matchMode = MatchModePreset.TIME_ATTACK, turnLimit = null, turnsRemaining = null)
        assertTrue(InMatchMissionCatalog.activeFor(turnLimitOnly).any { it.id == "pace" && it.progress == 5 })
        assertTrue(InMatchMissionCatalog.activeFor(timeModeNoLimit).any { it.id == "pace" && it.progress == 0 })

        listOf(
            MoveType.MYSTERY_TILE to null,
            MoveType.RISK_ROUTE to null,
            MoveType.BRANCH_PATH to null,
            MoveType.NORMAL to "Manual tile"
        ).forEach { (moveType, label) ->
            val state = normalOnly.copy(
                boardLayoutId = BoardLayouts.PRO_CHAOS_ID,
                matchEvents = listOf(
                    MatchEvent(1, 0, "Player 1", 2, 10, 12, 12, moveType, listOf(11, 12), tileLabel = label)
                )
            )
            assertTrue(InMatchMissionCatalog.activeFor(state).any { it.id == "tile_master" && it.completed })
        }
        assertFalse(InMatchMissionCatalog.activeFor(normalOnly.copy(boardLayoutId = BoardLayouts.CLASSIC_ID)).any { it.id == "tile_master" })
    }

    @Test
    fun baseRewardsAndProfileLevelUnlocksCoverTeamAndHighLevelBranches() {
        val winEvent = MatchEvent(1, 0, "Player 1", 3, 97, 100, 100, MoveType.WIN, listOf(98, 99, 100), winner = true)
        val teamState = sampleWinningState(listOf(winEvent)).copy(
            players = listOf(
                PlayerState("Player 1", Color.Red, 80, teamId = 0),
                PlayerState("Player 2", Color.Blue, 100, teamId = 1)
            ),
            winnerIndex = 1,
            winningTeamId = 0,
            ruleSetId = RuleSets.PARTY_ID
        )
        val lossState = teamState.copy(winningTeamId = null)
        val analytics = MatchAnalytics.from(teamState)

        assertTrue(RewardEngine.baseMatchReward(teamState, analytics).coins >= 60)
        assertEquals(20, RewardEngine.baseMatchReward(lossState, analytics).coins)

        val challenge = DailyChallenge(
            id = "finish",
            dateKey = "20260603",
            title = "Finish",
            description = "Finish once.",
            target = 1,
            kind = DailyChallengeKind.WIN_MATCH,
            reward = RewardBundle()
        )
        val result = PlayerProfile(xp = 1_740).recordCompletedMatch(sampleWinningState(listOf(winEvent)), challenge)

        assertTrue(BoardLayouts.all.map { it.id }.all { it in result.profile.unlockedBoardIds })
        assertTrue(setOf("classic_token", "cobra_token", "ladder_king", "gold_die").all { it in result.profile.unlockedAvatarIds })
        assertEquals("Snake Tamer", result.profile.selectedTitle)
    }

    @Test
    fun profileRecordCompletedMatchCoversHumanWinCombinationsAndCampaignFallbacks() {
        val event = MatchEvent(1, 0, "Player 1", 3, 97, 100, 100, MoveType.WIN, listOf(98, 99, 100), winner = true)
        val base = sampleWinningState(listOf(event)).copy(
            players = listOf(
                PlayerState("Player 1", Color.Red, 100, teamId = 0),
                PlayerState("Player 2", Color.Blue, 30, teamId = 1)
            )
        )
        val challenge = DailyChallenge("roll", "20260603", "Roll", "Roll sixes", 3, DailyChallengeKind.ROLL_SIXES)

        val directWin = PlayerProfile().recordCompletedMatch(base.copy(winnerIndex = 0), challenge)
        val teamWin = PlayerProfile().recordCompletedMatch(base.copy(winnerIndex = 1, winningTeamId = 0), challenge)
        val noHumanWin = PlayerProfile().recordCompletedMatch(base.copy(winnerIndex = 1, winningTeamId = 1), challenge)
        val incomplete = PlayerProfile().recordCompletedMatch(
            base.copy(winnerIndex = null, campaignNodeId = "classic_start", matchEvents = emptyList()),
            challenge
        )

        assertEquals(1, directWin.profile.humanWins)
        assertEquals(1, teamWin.profile.humanWins)
        assertEquals(0, noHumanWin.profile.humanWins)
        assertEquals(0, incomplete.profile.matchesCompleted)
        assertTrue(incomplete.reward.isEmpty())
        assertEquals("No winner", incomplete.profile.recentMatches.first().winnerName)
    }

    @Test
    fun profileDailyCompletionDoesNotDoubleCountExistingStreak() {
        val challenge = DailyChallenge(
            id = "finish",
            dateKey = "20260603",
            title = "Finish",
            description = "Finish once.",
            target = 1,
            kind = DailyChallengeKind.WIN_MATCH,
            reward = RewardBundle(coins = 5)
        )
        val state = sampleWinningState(
            listOf(MatchEvent(1, 0, "Player 1", 3, 97, 100, 100, MoveType.WIN, listOf(98, 99, 100), winner = true))
        ).copy(campaignNodeId = "missing_node")
        val profile = PlayerProfile(
            dailyChallengeId = challenge.id,
            dailyChallengeDateKey = challenge.dateKey,
            dailyChallengeProgress = 1,
            dailyChallengeCompleted = true,
            dailyStreak = 3,
            bestDailyStreak = 5,
            lastDailyChallengeDateKey = challenge.dateKey
        )

        val result = profile.recordCompletedMatch(state, challenge)

        assertFalse(result.dailyChallengeCompletedNow)
        assertEquals(3, result.profile.dailyStreak)
        assertEquals(5, result.profile.bestDailyStreak)
        assertFalse("missing_node" in result.profile.completedCampaignNodeIds)
        assertEquals(0, PlayerProfile(dailyChallengeId = "other").progressFor(challenge))
        assertFalse(PlayerProfile(dailyChallengeId = challenge.id, dailyChallengeDateKey = challenge.dateKey).dailyCompletedFor(challenge))
    }

    @Test
    fun dailyChallengeWinWithoutSnakesOnlyChecksWinnerSnakeHistory() {
        val challenge = DailyChallenge(
            id = "clean_escape",
            dateKey = "20260603",
            title = "Clean Escape",
            description = "Win without hitting a snake.",
            target = 1,
            kind = DailyChallengeKind.WIN_WITHOUT_SNAKES
        )
        val rivalSnakeState = sampleWinningState(
            events = listOf(
                MatchEvent(1, 1, "Player 2", 3, 95, 98, 54, MoveType.SNAKE, listOf(96, 97, 98, 54)),
                MatchEvent(2, 0, "Player 1", 3, 97, 100, 100, MoveType.WIN, listOf(98, 99, 100), winner = true)
            )
        )
        val winnerSnakeState = sampleWinningState(
            events = listOf(
                MatchEvent(1, 0, "Player 1", 3, 95, 98, 54, MoveType.SNAKE, listOf(96, 97, 98, 54)),
                MatchEvent(2, 0, "Player 1", 3, 97, 100, 100, MoveType.WIN, listOf(98, 99, 100), winner = true)
            )
        )

        assertEquals(1, DailyChallengeCatalog.progressFromMatch(rivalSnakeState, challenge))
        assertEquals(0, DailyChallengeCatalog.progressFromMatch(winnerSnakeState, challenge))
    }

    @Test
    fun inMatchMissionsAdaptToPowerUpsTimePressureAndSpecialTiles() {
        val timeState = sampleWinningState(events = emptyList()).copy(
            winnerIndex = null,
            matchMode = MatchModePreset.TIME_ATTACK,
            ruleSetId = RuleSets.TIME_ATTACK_ID,
            turnLimit = 20,
            turnsRemaining = 8
        )
        val specialTileState = sampleWinningState(
            events = listOf(
                MatchEvent(1, 0, "Player 1", 3, 10, 13, 37, MoveType.SHORTCUT, listOf(11, 12, 13, 37), tileLabel = "Shortcut")
            )
        ).copy(boardLayoutId = BoardLayouts.PRO_CHAOS_ID)
        val powerState = sampleWinningState(
            events = listOf(
                MatchEvent(1, 0, "Player 1", 0, 1, 1, 1, MoveType.POWER_UP, emptyList(), powerUpUsed = PowerUpType.TRAP)
            )
        ).copy(ruleSetId = RuleSets.PARTY_ID)

        assertTrue(InMatchMissionCatalog.activeFor(timeState).any { it.id == "pace" && it.progress == 8 })
        assertTrue(InMatchMissionCatalog.activeFor(specialTileState).any { it.id == "tile_master" && it.completed })
        assertTrue(InMatchMissionCatalog.activeFor(powerState).any { it.id == "use_power" && it.progress == 1 })
    }

    @Test
    fun profileRecordCompletedMatchForHumanLossResetsStreakWithoutHumanWin() {
        val state = sampleWinningState(
            events = listOf(
                MatchEvent(1, 1, "Player 2", 3, 97, 100, 100, MoveType.WIN, listOf(98, 99, 100), winner = true)
            )
        ).copy(
            players = listOf(
                PlayerState("Player 1", Color.Red, 54),
                PlayerState("Player 2", Color.Blue, 100)
            ),
            winnerIndex = 1
        )

        val challenge = DailyChallenge(
            id = "knockback_check",
            dateKey = "20260603",
            title = "Send Them Back",
            description = "Knock one rival back.",
            target = 1,
            kind = DailyChallengeKind.KNOCK_BACK_RIVAL
        )
        val result = PlayerProfile(
            humanWins = 4,
            currentWinStreak = 3,
            bestWinStreak = 5
        ).recordCompletedMatch(state, challenge)

        assertEquals(1, result.profile.matchesCompleted)
        assertEquals(4, result.profile.humanWins)
        assertEquals(0, result.profile.currentWinStreak)
        assertEquals(5, result.profile.bestWinStreak)
        assertFalse(result.dailyChallengeCompletedNow)
    }

    @Test
    fun analyticsRewardsAndAchievementsCoverEdgeFallbackBranches() {
        val oddEvents = listOf(
            MatchEvent(1, 0, "Player 1", 0, 10, 10, 10, MoveType.POWER_UP, emptyList(), powerUpUsed = PowerUpType.SHIELD),
            MatchEvent(2, 0, "Player 1", 7, 10, 17, 17, MoveType.NORMAL, listOf(11, 12, 13, 14, 15, 16, 17)),
            MatchEvent(3, 0, "Player 1", 3, 97, 100, 97, MoveType.OVERSHOOT, emptyList())
        )
        val outOfRangeWinner = sampleWinningState(oddEvents).copy(
            players = listOf(PlayerState("Solo", Color.Red, 97)),
            winnerIndex = 3,
            winningTeamId = 9
        )
        val analytics = MatchAnalytics.from(outOfRangeWinner)

        assertEquals(1, analytics.totalTurns)
        assertEquals(1, analytics.overshootCount)
        assertEquals(1, analytics.powerUpCount)
        assertEquals(null, analytics.winnerName)
        assertEquals(9, analytics.winningTeamId)
        assertFalse(InMatchMission("manual", "Manual", target = 2, progress = 1).completed)
        assertEquals("Manual 1/2", InMatchMission("manual", "Manual", target = 2, progress = 1).summary)

        val noPlayersTeamReward = RewardEngine.baseMatchReward(
            outOfRangeWinner.copy(players = emptyList(), winnerIndex = null, winningTeamId = 0),
            analytics
        )
        val humanTeamReward = RewardEngine.baseMatchReward(
            outOfRangeWinner.copy(
                players = listOf(PlayerState("Player 1", Color.Red, 80, teamId = 0)),
                winnerIndex = 1,
                winningTeamId = 0
            ),
            analytics
        )

        assertTrue(noPlayersTeamReward.coins >= 10)
        assertTrue(humanTeamReward.coins > noPlayersTeamReward.coins)
        assertFalse("snake_survivor" in AchievementCatalog.evaluate(outOfRangeWinner.copy(winnerIndex = null)))
        assertFalse("exact_finisher" in AchievementCatalog.evaluate(outOfRangeWinner.copy(matchEvents = oddEvents.dropLast(1))))
    }

    @Test
    fun profileDailyProgressAndCampaignRewardsCoverRemainingBranches() {
        val challenge = DailyChallenge(
            id = "finish",
            dateKey = "20260604",
            title = "Finish",
            description = "Finish once.",
            target = 1,
            kind = DailyChallengeKind.WIN_MATCH,
            reward = RewardBundle(coins = 5)
        )
        val winEvent = MatchEvent(1, 0, "Player 1", 3, 97, 100, 100, MoveType.WIN, listOf(98, 99, 100), winner = true)
        val state = sampleWinningState(listOf(winEvent)).copy(campaignNodeId = "classic_start")

        val freshDaily = PlayerProfile(lastDailyChallengeDateKey = "20260603")
            .recordCompletedMatch(state, challenge)
        val alreadyCompletedCampaign = PlayerProfile(
            completedCampaignNodeIds = setOf("classic_start"),
            dailyChallengeId = challenge.id,
            dailyChallengeDateKey = challenge.dateKey,
            dailyChallengeProgress = 0,
            dailyChallengeCompleted = false
        ).recordCompletedMatch(state, challenge)

        assertTrue(freshDaily.dailyChallengeCompletedNow)
        assertEquals(1, freshDaily.profile.dailyStreak)
        assertEquals(1, freshDaily.profile.bestDailyStreak)
        assertTrue("classic_start" in freshDaily.profile.completedCampaignNodeIds)
        assertTrue(alreadyCompletedCampaign.reward.coins < freshDaily.reward.coins)
        assertEquals(1, alreadyCompletedCampaign.profile.progressFor(challenge))
        assertTrue(alreadyCompletedCampaign.profile.dailyCompletedFor(challenge))
    }

    @Test
    fun progressionModelsCoverSnakeSpecialTileAndMidLevelUnlockBranches() {
        val snakeAndLabelState = sampleWinningState(
            events = listOf(
                MatchEvent(1, 1, "Player 2", 4, 95, 99, 54, MoveType.SNAKE, listOf(96, 97, 98, 99, 54)),
                MatchEvent(2, 0, "Player 1", 2, 20, 22, 22, MoveType.NORMAL, listOf(21, 22), tileLabel = "Festival gift"),
                MatchEvent(3, 0, "Player 1", 3, 97, 100, 100, MoveType.NORMAL, listOf(98, 99, 100), winner = true)
            )
        ).copy(
            boardLayoutId = BoardLayouts.FESTIVAL_EVENT_ID,
            campaignNodeId = ""
        )

        val analytics = MatchAnalytics.from(snakeAndLabelState)
        val missions = InMatchMissionCatalog.activeFor(snakeAndLabelState)
        val achievements = AchievementCatalog.evaluate(snakeAndLabelState, analytics)
        val midLevelProfile = PlayerProfile(xp = 760).recordCompletedMatch(
            sampleWinningState(
                listOf(MatchEvent(1, 0, "Player 1", 3, 97, 100, 100, MoveType.WIN, listOf(98, 99, 100), winner = true))
            )
        ).profile

        assertEquals(45, analytics.worstSnakeDrop)
        assertTrue(missions.any { it.id == "tile_master" && it.completed })
        assertTrue("first_win" in achievements)
        assertTrue("exact_finisher" !in achievements)
        assertTrue("campaign_climber" !in achievements)
        assertTrue("cobra_token" in midLevelProfile.unlockedAvatarIds)
        assertTrue("ladder_king" in midLevelProfile.unlockedAvatarIds)
        assertFalse("gold_die" in midLevelProfile.unlockedAvatarIds)
    }

    @Test
    fun achievementsAndDailyCleanWinCoverWinnerSnakeBranches() {
        val challenge = DailyChallenge(
            id = "clean_escape",
            dateKey = "20260605",
            title = "Clean Escape",
            description = "Win without a snake.",
            target = 1,
            kind = DailyChallengeKind.WIN_WITHOUT_SNAKES
        )
        val winnerHitSnake = sampleWinningState(
            mode = GameMode.VS_BOT,
            events = listOf(
                MatchEvent(1, 0, "Player 1", 3, 95, 98, 54, MoveType.SNAKE, listOf(96, 97, 98, 54)),
                MatchEvent(2, 1, "Rival Bot", 2, 10, 12, 12, MoveType.NORMAL, listOf(11, 12)),
                MatchEvent(3, 0, "Player 1", 3, 97, 100, 100, MoveType.WIN, listOf(98, 99, 100), winner = true)
            )
        )
        val botWins = winnerHitSnake.copy(winnerIndex = 1)

        val humanAchievements = AchievementCatalog.evaluate(winnerHitSnake)
        val botAchievements = AchievementCatalog.evaluate(botWins)

        assertTrue("bot_slayer" in humanAchievements)
        assertFalse("snake_survivor" in humanAchievements)
        assertFalse("bot_slayer" in botAchievements)
        assertEquals(0, DailyChallengeCatalog.progressFromMatch(winnerHitSnake, challenge))
        assertEquals(0, DailyChallengeCatalog.progressFromMatch(winnerHitSnake.copy(winnerIndex = null), challenge))
    }
}
