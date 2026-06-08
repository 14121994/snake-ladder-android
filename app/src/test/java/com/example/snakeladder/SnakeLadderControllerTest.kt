package com.example.snakeladder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnakeLadderControllerTest {

    private fun sampleStateAt(
        p1: Int,
        p2: Int,
        currentPlayerIndex: Int = 0,
        difficulty: GameDifficulty = GameDifficulty.EASY
    ): GameState {
        return GameState(
            players = listOf(
                PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, p1),
                PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, p2)
            ),
            currentPlayerIndex = currentPlayerIndex,
            lastDiceRoll = null,
            statusMessage = "",
            bonusTurnGranted = false,
            winnerIndex = null,
            moveHistory = emptyList(),
            gameMode = GameMode.LOCAL_MULTIPLAYER,
            botPlayerIndex = null,
            lastMovePlayerIndex = null,
            lastMovePath = emptyList(),
            lastMoveType = null,
            moveSignal = 0,
            difficulty = difficulty
        )
    }

    @Test
    fun resolvePosition_climbsLadder() {
        val controller = SnakeLadderController()

        val move = controller.resolvePosition(currentPosition = 1, dice = 1)

        assertEquals(38, move.position)
        assertTrue(move.eventMessage?.contains("Climbed ladder") == true)
    }

    @Test
    fun resolvePosition_bittenBySnake() {
        val controller = SnakeLadderController()

        val move = controller.resolvePosition(currentPosition = 98, dice = 1)

        assertEquals(54, move.position)
        assertTrue(move.eventMessage?.contains("Bitten by snake") == true)
    }

    @Test
    fun resolvePosition_preventsOvershoot() {
        val controller = SnakeLadderController()

        val move = controller.resolvePosition(currentPosition = 97, dice = 6)

        assertEquals(97, move.position)
        assertTrue(move.eventMessage?.contains("needed 3") == true)
    }

    @Test
    fun applyRoll_whenSix_grantsBonusTurn() {
        val controller = SnakeLadderController()

        controller.applyRoll(6)

        assertEquals(0, controller.state.currentPlayerIndex)
        assertTrue(controller.state.statusMessage.contains("bonus turn", ignoreCase = true))
    }

    @Test
    fun applyRoll_whenLadderClimbed_grantsBonusTurn() {
        val controller = SnakeLadderController()

        controller.applyRoll(1) // 1 -> 2, then ladder 2 -> 38

        assertEquals(38, controller.state.players[0].position)
        assertEquals(0, controller.state.currentPlayerIndex)
        assertTrue(controller.state.statusMessage.contains("Bonus turn"))
    }

    @Test
    fun applyRoll_recordsStructuredMatchEvent() {
        val controller = SnakeLadderController()

        controller.applyRoll(1) // 1 -> 2, then ladder 2 -> 38

        val event = controller.state.matchEvents.single()
        assertEquals(1, event.turnNumber)
        assertEquals(0, event.playerIndex)
        assertEquals("Player 1", event.playerName)
        assertEquals(1, event.dice)
        assertEquals(1, event.startPosition)
        assertEquals(2, event.landedPosition)
        assertEquals(38, event.finalPosition)
        assertEquals(MoveType.LADDER, event.moveType)
        assertEquals(listOf(2, 38), event.path)
        assertTrue(event.bonusTurn)
    }

    @Test
    fun applyRoll_clampsOutOfRangeDiceValue() {
        val controller = SnakeLadderController()

        controller.applyRoll(99)

        assertEquals(14, controller.state.players[0].position)
        assertEquals(0, controller.state.currentPlayerIndex)
    }

    @Test
    fun startGame_vsBot_forcesTwoPlayersAndBotIndex() {
        val controller = SnakeLadderController()

        controller.startGame(players = 4, mode = GameMode.VS_BOT)

        assertEquals(2, controller.state.players.size)
        assertEquals("Rival Bot", controller.state.players[1].name)
        assertEquals(1, controller.state.botPlayerIndex)
    }

    @Test
    fun startGame_appliesCustomLocalPlayerNamesAndAvatars() {
        val controller = SnakeLadderController()

        controller.startGame(
            players = 3,
            mode = GameMode.LOCAL_MULTIPLAYER,
            playerSetups = listOf(
                PlayerSetup(name = " Asha ", avatarId = "cobra_token"),
                PlayerSetup(name = "   ", avatarId = "gold_die"),
                PlayerSetup(name = "Dev", avatarId = "ladder_king")
            )
        )

        assertEquals(listOf("Asha", "Player 2", "Dev"), controller.state.players.map { it.name })
        assertEquals(listOf("cobra_token", "gold_die", "ladder_king"), controller.state.players.map { it.avatarId })
        assertEquals("Asha starts. Roll the dice.", controller.state.statusMessage)

        controller.reset()

        assertEquals(listOf("Asha", "Player 2", "Dev"), controller.state.players.map { it.name })
    }

    @Test
    fun startGame_vsBotKeepsBotIdentityWhileCustomizingHumanPlayer() {
        val controller = SnakeLadderController()

        controller.startGame(
            players = 2,
            mode = GameMode.VS_BOT,
            botPersonality = BotPersonality.RISKY,
            playerSetups = listOf(
                PlayerSetup(name = "Asha", avatarId = "gold_die"),
                PlayerSetup(name = "Local Rival", avatarId = "classic_token")
            )
        )

        assertEquals("Asha", controller.state.players[0].name)
        assertEquals("gold_die", controller.state.players[0].avatarId)
        assertEquals("Rocket Bot", controller.state.players[1].name)
        assertEquals("cobra_token", controller.state.players[1].avatarId)
    }

    @Test
    fun startGame_vsBot_appliesSelectedBotPersonality() {
        val controller = SnakeLadderController()

        controller.startGame(
            players = 2,
            mode = GameMode.VS_BOT,
            botPersonality = BotPersonality.RISKY
        )
        controller.applyRoll(2) // Player 1: 1 -> 3, bot turn
        controller.applyRoll(5) // Bot: risky normal move

        assertEquals("Rocket Bot", controller.state.players[1].name)
        assertEquals(BotPersonality.RISKY, controller.state.botPersonality)
        assertTrue(controller.state.statusMessage.contains("Risky bot pushes the pace"))
    }

    @Test
    fun applyRoll_usesSelectedBoardLayoutAndMatchMode() {
        val controller = SnakeLadderController()
        controller.startGame(
            players = 2,
            mode = GameMode.LOCAL_MULTIPLAYER,
            matchMode = MatchModePreset.DAILY_CHALLENGE,
            boardLayoutId = BoardLayouts.QUICK_CLIMB_ID
        )
        controller.loadState(
            controller.state.copy(
                players = listOf(
                    PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 11),
                    PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 1)
                ),
                currentPlayerIndex = 0
            )
        )

        controller.applyRoll(1)

        assertEquals(34, controller.state.players[0].position)
        assertEquals(MatchModePreset.DAILY_CHALLENGE, controller.state.matchMode)
        assertEquals(BoardLayouts.QUICK_CLIMB_ID, controller.state.boardLayoutId)
        assertEquals(RuleSets.DAILY_ID, controller.state.ruleSetId)
    }

    @Test
    fun applyRoll_resolvesSpecialShortcutTileAndRecordsEvent() {
        val controller = SnakeLadderController()
        controller.startGame(
            players = 2,
            mode = GameMode.LOCAL_MULTIPLAYER,
            boardLayoutId = BoardLayouts.PRO_CHAOS_ID
        )
        controller.loadState(
            controller.state.copy(
                players = listOf(
                    PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 10),
                    PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 1)
                ),
                currentPlayerIndex = 0
            )
        )

        controller.applyRoll(3)

        assertEquals(37, controller.state.players[0].position)
        assertEquals(MoveType.SHORTCUT, controller.state.lastMoveType)
        assertEquals("Shortcut", controller.state.matchEvents.last().tileLabel)
    }

    @Test
    fun applyRoll_resolvesRiskRouteSuccessAndFailure() {
        val successController = SnakeLadderController()
        successController.startGame(
            players = 2,
            mode = GameMode.LOCAL_MULTIPLAYER,
            boardLayoutId = BoardLayouts.PRO_CHAOS_ID
        )
        successController.loadState(
            successController.state.copy(
                players = listOf(
                    PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 27),
                    PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 1)
                )
            )
        )

        successController.applyRoll(4)

        assertEquals(65, successController.state.players[0].position)
        assertEquals(MoveType.RISK_ROUTE, successController.state.lastMoveType)
        assertTrue(PowerUpType.DICE_BOOST in successController.state.matchEvents.last().awardedPowerUps)

        val failureController = SnakeLadderController()
        failureController.startGame(
            players = 2,
            mode = GameMode.LOCAL_MULTIPLAYER,
            boardLayoutId = BoardLayouts.PRO_CHAOS_ID
        )
        failureController.loadState(
            failureController.state.copy(
                players = listOf(
                    PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 29),
                    PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 1)
                )
            )
        )

        failureController.applyRoll(2)

        assertEquals(17, failureController.state.players[0].position)
        assertEquals(MoveType.RISK_ROUTE, failureController.state.lastMoveType)
        assertTrue(failureController.state.matchEvents.last().awardedPowerUps.isEmpty())
    }

    @Test
    fun applyRoll_resolvesMysteryTileBranchesDeterministically() {
        fun controllerAtMystery(boardLayoutId: String, startPosition: Int): SnakeLadderController {
            val controller = SnakeLadderController()
            controller.startGame(
                players = 2,
                mode = GameMode.LOCAL_MULTIPLAYER,
                boardLayoutId = boardLayoutId
            )
            controller.loadState(
                controller.state.copy(
                    players = listOf(
                        PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, startPosition),
                        PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 1)
                    )
                )
            )
            return controller
        }

        val diceBoost = controllerAtMystery(BoardLayouts.PRO_CHAOS_ID, startPosition = 66)
        diceBoost.applyRoll(2)
        assertEquals(68, diceBoost.state.players[0].position)
        assertEquals(listOf(PowerUpType.DICE_BOOST), diceBoost.state.matchEvents.last().awardedPowerUps)

        val tileReward = controllerAtMystery(BoardLayouts.TRAP_VALLEY_ID, startPosition = 74)
        tileReward.applyRoll(2)
        assertEquals(76, tileReward.state.players[0].position)
        assertEquals(listOf(PowerUpType.REROLL), tileReward.state.matchEvents.last().awardedPowerUps)

        val advance = controllerAtMystery(BoardLayouts.TRAP_VALLEY_ID, startPosition = 37)
        advance.applyRoll(2)
        assertEquals(42, advance.state.players[0].position)
        assertTrue(advance.state.matchEvents.last().awardedPowerUps.isEmpty())
    }

    @Test
    fun applyRoll_defensiveBotTakesSaferBranchPath() {
        val controller = SnakeLadderController()
        controller.startGame(
            players = 2,
            mode = GameMode.VS_BOT,
            botPersonality = BotPersonality.DEFENSIVE,
            boardLayoutId = BoardLayouts.PRO_CHAOS_ID
        )
        controller.loadState(
            controller.state.copy(
                players = listOf(
                    PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 1),
                    PlayerState("Guard Bot", androidx.compose.ui.graphics.Color.Blue, 44)
                ),
                currentPlayerIndex = 1
            )
        )

        controller.applyRoll(3)

        assertEquals(52, controller.state.players[1].position)
        assertEquals(MoveType.BRANCH_PATH, controller.state.lastMoveType)
        assertEquals("Branch", controller.state.matchEvents.last().tileLabel)
    }

    @Test
    fun startGame_tacticalCardsDealsCardInventory() {
        val controller = SnakeLadderController()

        controller.startGame(
            players = 2,
            mode = GameMode.LOCAL_MULTIPLAYER,
            matchMode = MatchModePreset.TACTICAL_CARDS
        )

        assertEquals(RuleSets.TACTICAL_CARDS_ID, controller.state.ruleSetId)
        assertTrue(PowerUpType.REROLL in controller.state.powerUpInventories[0])
        assertTrue(PowerUpType.SHIELD in controller.state.powerUpInventories[0])
    }

    @Test
    fun startGame_bossCampaignGivesBotExtraResources() {
        val controller = SnakeLadderController()

        controller.startGame(
            players = 2,
            mode = GameMode.VS_BOT,
            matchMode = MatchModePreset.PARTY_RULES,
            campaignNodeId = "boss_cobra_guard"
        )

        assertTrue(PowerUpType.SHIELD in controller.state.powerUpInventories[1])
        assertTrue(PowerUpType.REVENGE in controller.state.powerUpInventories[1])
    }

    @Test
    fun startGame_multiplayer_hasNoBot() {
        val controller = SnakeLadderController()

        controller.startGame(players = 3, mode = GameMode.LOCAL_MULTIPLAYER)

        assertEquals(3, controller.state.players.size)
        assertNull(controller.state.botPlayerIndex)
    }

    @Test
    fun applyRoll_normalMove_passesTurnToNextPlayer() {
        val controller = SnakeLadderController()

        controller.applyRoll(2) // Player 1: 1 -> 3 (no ladder/snake, no bonus)

        assertEquals(3, controller.state.players[0].position)
        assertEquals(1, controller.state.currentPlayerIndex)
        assertNull(controller.state.winnerIndex)
    }

    @Test
    fun applyRoll_capsMoveHistoryTo20Entries() {
        val controller = SnakeLadderController()

        repeat(25) { controller.applyRoll(1) }

        assertEquals(20, controller.state.moveHistory.size)
    }

    @Test
    fun resolvePosition_pathIncludesLadderDestination() {
        val controller = SnakeLadderController()

        val move = controller.resolvePosition(currentPosition = 1, dice = 1)

        assertEquals(listOf(2, 38), move.path)
        assertEquals(MoveType.LADDER, move.moveType)
    }

    @Test
    fun resolvePosition_pathIncludesSnakeTail() {
        val controller = SnakeLadderController()

        val move = controller.resolvePosition(currentPosition = 98, dice = 1)

        assertEquals(listOf(99, 54), move.path)
        assertEquals(MoveType.SNAKE, move.moveType)
    }

    @Test
    fun startGame_multiplayer_clampsPlayerCountToFour() {
        val controller = SnakeLadderController()

        controller.startGame(players = 99, mode = GameMode.LOCAL_MULTIPLAYER)

        assertEquals(4, controller.state.players.size)
    }

    @Test
    fun reset_restoresInitialPositionsForCurrentConfiguration() {
        val controller = SnakeLadderController()
        controller.startGame(players = 3, mode = GameMode.LOCAL_MULTIPLAYER)
        controller.applyRoll(6)
        controller.applyRoll(4)

        controller.reset()

        assertEquals(3, controller.state.players.size)
        assertEquals(listOf(1, 1, 1), controller.state.players.map { it.position })
        assertEquals(0, controller.state.currentPlayerIndex)
        assertNull(controller.state.winnerIndex)
    }

    @Test
    fun applyRoll_vsBot_botGetsBonusTurnOnSix() {
        val controller = SnakeLadderController()
        controller.startGame(players = 2, mode = GameMode.VS_BOT)

        controller.applyRoll(2) // Player 1 -> Player 2(Bot)
        assertEquals(1, controller.state.currentPlayerIndex)

        controller.applyRoll(6) // Bot should retain turn

        assertEquals(1, controller.state.currentPlayerIndex)
        assertTrue(controller.state.statusMessage.contains("bonus turn", ignoreCase = true))
    }

    @Test
    fun applyRoll_vsBot_botGetsBonusTurnOnLadder() {
        val controller = SnakeLadderController()
        controller.startGame(players = 2, mode = GameMode.VS_BOT)

        controller.applyRoll(2) // Player 1: 1 -> 3, now Bot turn at 1
        assertEquals(1, controller.state.currentPlayerIndex)

        controller.applyRoll(1) // Bot: 1 -> 2, ladder 2 -> 38

        assertEquals(38, controller.state.players[1].position)
        assertEquals(1, controller.state.currentPlayerIndex)
        assertTrue(controller.state.statusMessage.contains("bonus turn", ignoreCase = true))
    }

    @Test
    fun applyRoll_easyDifficulty_doesNotKnockBackOnSharedCell() {
        val controller = SnakeLadderController()
        controller.loadState(sampleStateAt(p1 = 1, p2 = 3, difficulty = GameDifficulty.EASY))

        controller.applyRoll(2)

        assertEquals(3, controller.state.players[0].position)
        assertEquals(3, controller.state.players[1].position)
        assertTrue(controller.state.knockBackMoves.isEmpty())
    }

    @Test
    fun applyRoll_mediumDifficulty_normalLandingKnocksBackOccupant() {
        val controller = SnakeLadderController()
        controller.loadState(sampleStateAt(p1 = 1, p2 = 3, difficulty = GameDifficulty.MEDIUM))

        controller.applyRoll(2)

        assertEquals(3, controller.state.players[0].position)
        assertEquals(1, controller.state.players[1].position)
        assertEquals(listOf(KnockBackMove(playerIndex = 1, path = listOf(2, 1))), controller.state.knockBackMoves)
        assertTrue(controller.state.statusMessage.contains("knocked Player 2 back to start"))
    }

    @Test
    fun applyRoll_mediumDifficulty_ladderLandingDoesNotKnockBackOccupant() {
        val controller = SnakeLadderController()
        controller.loadState(sampleStateAt(p1 = 1, p2 = 38, difficulty = GameDifficulty.MEDIUM))

        controller.applyRoll(1)

        assertEquals(38, controller.state.players[0].position)
        assertEquals(38, controller.state.players[1].position)
        assertTrue(controller.state.knockBackMoves.isEmpty())
    }

    @Test
    fun applyRoll_hardDifficulty_ladderLandingKnocksBackOccupant() {
        val controller = SnakeLadderController()
        controller.loadState(sampleStateAt(p1 = 1, p2 = 38, difficulty = GameDifficulty.HARD))

        controller.applyRoll(1)

        assertEquals(38, controller.state.players[0].position)
        assertEquals(1, controller.state.players[1].position)
        assertEquals(1, controller.state.knockBackMoves.size)
        assertEquals(1, controller.state.knockBackMoves.first().playerIndex)
        assertEquals(37, controller.state.knockBackMoves.first().path.first())
        assertEquals(1, controller.state.knockBackMoves.first().path.last())
    }

    @Test
    fun applyRoll_hardDifficulty_snakeLandingKnocksBackOccupant() {
        val controller = SnakeLadderController()
        controller.loadState(sampleStateAt(p1 = 98, p2 = 54, difficulty = GameDifficulty.HARD))

        controller.applyRoll(1)

        assertEquals(54, controller.state.players[0].position)
        assertEquals(1, controller.state.players[1].position)
        assertEquals(1, controller.state.knockBackMoves.size)
        assertEquals(53, controller.state.knockBackMoves.first().path.first())
        assertEquals(1, controller.state.knockBackMoves.first().path.last())
    }

    @Test
    fun loadState_restoresSavedGameForVsBot() {
        val controller = SnakeLadderController()
        val savedState = GameState(
            players = listOf(
                PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 44),
                PlayerState("Bot", androidx.compose.ui.graphics.Color.Blue, 52)
            ),
            currentPlayerIndex = 1,
            lastDiceRoll = 6,
            statusMessage = "Bot rolled 6. Bonus turn",
            bonusTurnGranted = true,
            winnerIndex = null,
            moveHistory = listOf("Bot rolled 6"),
            gameMode = GameMode.VS_BOT,
            botPlayerIndex = 1,
            lastMovePlayerIndex = 1,
            lastMovePath = listOf(46, 52),
            lastMoveType = MoveType.LADDER,
            moveSignal = 9
        )

        controller.loadState(savedState)

        assertEquals(GameMode.VS_BOT, controller.state.gameMode)
        assertEquals(2, controller.state.players.size)
        assertEquals(44, controller.state.players[0].position)
        assertEquals(52, controller.state.players[1].position)
        assertEquals(1, controller.state.currentPlayerIndex)
        assertEquals(0, controller.state.moveSignal)
        assertTrue(controller.state.lastMovePath.isEmpty())
        assertNull(controller.state.lastMoveType)
    }

    @Test
    fun upsertSavedSnapshotsByName_overwritesExistingEntryForSameName() {
        val older = SavedGameSnapshot(
            id = "s1",
            name = "Weekend Match",
            savedAt = 1000L,
            boardTheme = BoardThemeOption.VIBRANT,
            state = sampleStateAt(p1 = 12, p2 = 18)
        )
        val newer = SavedGameSnapshot(
            id = "s1",
            name = "Weekend Match",
            savedAt = 2000L,
            boardTheme = BoardThemeOption.PREMIUM_MUTED,
            state = sampleStateAt(p1 = 44, p2 = 52)
        )

        val merged = upsertSavedSnapshotsByName(listOf(older), newer)

        assertEquals(1, merged.size)
        assertEquals("Weekend Match", merged.first().name)
        assertEquals(44, merged.first().state.players[0].position)
        assertEquals(52, merged.first().state.players[1].position)
        assertEquals(BoardThemeOption.PREMIUM_MUTED, merged.first().boardTheme)
    }

    @Test
    fun upsertSavedSnapshotsByName_treatsNameCaseInsensitively() {
        val older = SavedGameSnapshot(
            id = "s1",
            name = "Weekend Match",
            savedAt = 1000L,
            boardTheme = BoardThemeOption.VIBRANT,
            state = sampleStateAt(p1 = 8, p2 = 9)
        )
        val newer = SavedGameSnapshot(
            id = "s1",
            name = " weekend match ",
            savedAt = 2000L,
            boardTheme = BoardThemeOption.VIBRANT,
            state = sampleStateAt(p1 = 33, p2 = 34)
        )

        val merged = upsertSavedSnapshotsByName(listOf(older), newer)

        assertEquals(1, merged.size)
        assertEquals(33, merged.first().state.players[0].position)
        assertEquals(34, merged.first().state.players[1].position)
    }

    @Test
    fun upsertSavedSnapshotsByName_preservesOtherGamesAndPlacesUpdatedFirst() {
        val existingA = SavedGameSnapshot(
            id = "a1",
            name = "Alpha",
            savedAt = 1000L,
            boardTheme = BoardThemeOption.VIBRANT,
            state = sampleStateAt(p1 = 3, p2 = 4)
        )
        val existingB = SavedGameSnapshot(
            id = "b1",
            name = "Beta",
            savedAt = 1500L,
            boardTheme = BoardThemeOption.VIBRANT,
            state = sampleStateAt(p1 = 5, p2 = 6)
        )
        val updatedA = SavedGameSnapshot(
            id = "a1",
            name = "Alpha",
            savedAt = 3000L,
            boardTheme = BoardThemeOption.PREMIUM_MUTED,
            state = sampleStateAt(p1 = 30, p2 = 40)
        )

        val merged = upsertSavedSnapshotsByName(listOf(existingA, existingB), updatedA)

        assertEquals(2, merged.size)
        assertEquals("Alpha", merged[0].name)
        assertEquals(30, merged[0].state.players[0].position)
        assertEquals("Beta", merged[1].name)
    }

    @Test
    fun dedupeSavedSnapshotsByName_keepsMostRecentPerName() {
        val oldAlpha = SavedGameSnapshot(
            id = "a1",
            name = "Alpha",
            savedAt = 1000L,
            boardTheme = BoardThemeOption.VIBRANT,
            state = sampleStateAt(p1 = 1, p2 = 2)
        )
        val newAlpha = SavedGameSnapshot(
            id = "a2",
            name = " alpha ",
            savedAt = 2500L,
            boardTheme = BoardThemeOption.PREMIUM_MUTED,
            state = sampleStateAt(p1 = 10, p2 = 20)
        )
        val beta = SavedGameSnapshot(
            id = "b1",
            name = "Beta",
            savedAt = 2000L,
            boardTheme = BoardThemeOption.VIBRANT,
            state = sampleStateAt(p1 = 7, p2 = 8)
        )

        val deduped = dedupeSavedSnapshotsByName(listOf(oldAlpha, beta, newAlpha))

        assertEquals(2, deduped.size)
        assertEquals("alpha", deduped[0].name.trim().lowercase())
        assertEquals(10, deduped[0].state.players[0].position)
        assertEquals("Beta", deduped[1].name)
    }

    @Test
    fun dedupeSavedSnapshotsByName_capsAtTwentyUniqueGames() {
        val many = (1..25).map { idx ->
            SavedGameSnapshot(
                id = "id$idx",
                name = "Game $idx",
                savedAt = idx.toLong(),
                boardTheme = BoardThemeOption.VIBRANT,
                state = sampleStateAt(p1 = idx, p2 = idx)
            )
        }

        val deduped = dedupeSavedSnapshotsByName(many)

        assertEquals(20, deduped.size)
        assertEquals("Game 25", deduped.first().name)
        assertEquals("Game 6", deduped.last().name)
    }

    @Test
    fun dedupeSavedSnapshotsByName_ignoresBlankNames() {
        val blank = SavedGameSnapshot(
            id = "blank",
            name = "   ",
            savedAt = 3000L,
            boardTheme = BoardThemeOption.VIBRANT,
            state = sampleStateAt(p1 = 1, p2 = 1)
        )
        val named = SavedGameSnapshot(
            id = "named",
            name = "Named",
            savedAt = 1000L,
            boardTheme = BoardThemeOption.VIBRANT,
            state = sampleStateAt(p1 = 2, p2 = 2)
        )

        val deduped = dedupeSavedSnapshotsByName(listOf(blank, named))

        assertEquals(1, deduped.size)
        assertEquals("Named", deduped.single().name)
    }

    @Test
    fun upsertSavedSnapshotsByName_overwritesByIdEvenWhenNameChanges() {
        val loadedSave = SavedGameSnapshot(
            id = "loaded_1",
            name = "Old Name",
            savedAt = 1000L,
            boardTheme = BoardThemeOption.VIBRANT,
            state = sampleStateAt(p1 = 10, p2 = 20)
        )
        val renamedAndUpdated = SavedGameSnapshot(
            id = "loaded_1",
            name = "New Name",
            savedAt = 3000L,
            boardTheme = BoardThemeOption.PREMIUM_MUTED,
            state = sampleStateAt(p1 = 45, p2 = 55)
        )

        val merged = upsertSavedSnapshotsByName(listOf(loadedSave), renamedAndUpdated)

        assertEquals(1, merged.size)
        assertEquals("loaded_1", merged.first().id)
        assertEquals("New Name", merged.first().name)
        assertEquals(45, merged.first().state.players[0].position)
        assertEquals(55, merged.first().state.players[1].position)
    }

    @Test
    fun rollDice_usesInjectedDiceRoller() {
        val controller = SnakeLadderController(diceRoller = { 4 })

        controller.rollDice()

        assertEquals(5, controller.state.players[0].position)
        assertEquals(4, controller.state.lastDiceRoll)
    }

    @Test
    fun applyRoll_whenWinnerAlreadyExists_doesNothing() {
        val controller = SnakeLadderController()
        val winningState = sampleStateAt(p1 = 100, p2 = 44).copy(
            currentPlayerIndex = 0,
            winnerIndex = 0,
            statusMessage = "Player 1 wins!",
            lastDiceRoll = 6,
            lastMoveType = MoveType.WIN
        )
        controller.loadState(winningState)

        controller.applyRoll(1)

        assertEquals(100, controller.state.players[0].position)
        assertEquals(44, controller.state.players[1].position)
        assertEquals(0, controller.state.winnerIndex)
        assertEquals("Player 1 wins!", controller.state.statusMessage)
    }

    @Test
    fun applyRoll_exactFinishSetsWinnerAndWinMoveType() {
        val controller = SnakeLadderController()
        controller.loadState(sampleStateAt(p1 = 97, p2 = 44))

        controller.applyRoll(3)

        assertEquals(100, controller.state.players[0].position)
        assertEquals(0, controller.state.currentPlayerIndex)
        assertEquals(0, controller.state.winnerIndex)
        assertEquals(MoveType.WIN, controller.state.lastMoveType)
        assertTrue(controller.state.statusMessage.contains("wins"))
    }

    @Test
    fun applyRoll_hardDifficulty_overshootDoesNotKnockBackOccupant() {
        val controller = SnakeLadderController()
        controller.loadState(sampleStateAt(p1 = 97, p2 = 97, difficulty = GameDifficulty.HARD))

        controller.applyRoll(6)

        assertEquals(97, controller.state.players[0].position)
        assertEquals(97, controller.state.players[1].position)
        assertEquals(MoveType.OVERSHOOT, controller.state.lastMoveType)
        assertTrue(controller.state.knockBackMoves.isEmpty())
    }

    @Test
    fun partyRules_startWithPlayablePowerUps() {
        val controller = SnakeLadderController()

        controller.startGame(
            players = 2,
            mode = GameMode.LOCAL_MULTIPLAYER,
            matchMode = MatchModePreset.PARTY_RULES,
            boardLayoutId = BoardLayouts.TRAP_VALLEY_ID
        )

        assertEquals(RuleSets.PARTY_ID, controller.state.ruleSetId)
        assertTrue(PowerUpType.SHIELD in controller.state.powerUpInventories[0])
        assertTrue(PowerUpType.REROLL in controller.state.powerUpInventories[0])
        assertTrue(PowerUpType.TRAP in controller.state.powerUpInventories[0])
    }

    @Test
    fun usePowerUp_shieldBlocksSnake() {
        val controller = SnakeLadderController()
        controller.startGame(players = 2, mode = GameMode.LOCAL_MULTIPLAYER, matchMode = MatchModePreset.PARTY_RULES)
        controller.loadState(
            controller.state.copy(
                players = listOf(
                    PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 98),
                    PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 1)
                ),
                currentPlayerIndex = 0
            )
        )

        controller.usePowerUp(PowerUpType.SHIELD)
        controller.applyRoll(1)

        assertEquals(99, controller.state.players[0].position)
        assertEquals(MoveType.NORMAL, controller.state.lastMoveType)
        assertTrue(PowerUpType.SHIELD in controller.state.matchEvents.last().triggeredPowerUps)
        assertFalse(PowerUpType.SHIELD in controller.state.powerUpInventories[0])
    }

    @Test
    fun usePowerUp_trapSendsRivalBackToStart() {
        val controller = SnakeLadderController()
        controller.startGame(players = 2, mode = GameMode.LOCAL_MULTIPLAYER, matchMode = MatchModePreset.PARTY_RULES)
        controller.usePowerUp(PowerUpType.TRAP)
        val trap = controller.state.activeTraps.single()
        controller.loadState(
            controller.state.copy(
                players = listOf(
                    PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 1),
                    PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, trap.cell - 1)
                ),
                currentPlayerIndex = 1,
                powerUpInventories = listOf(controller.state.powerUpInventories[0], emptyList())
            )
        )

        controller.applyRoll(1)

        assertEquals(1, controller.state.players[1].position)
        assertEquals(MoveType.TRAP, controller.state.lastMoveType)
        assertTrue(controller.state.activeTraps.isEmpty())
    }

    @Test
    fun usePowerUp_diceBoostImprovesNextRoll() {
        val controller = SnakeLadderController()
        controller.startGame(players = 2, mode = GameMode.LOCAL_MULTIPLAYER, matchMode = MatchModePreset.PARTY_RULES)
        controller.loadState(
            controller.state.copy(
                powerUpInventories = listOf(listOf(PowerUpType.DICE_BOOST), emptyList())
            )
        )

        controller.usePowerUp(PowerUpType.DICE_BOOST)
        controller.applyRoll(1)

        assertEquals(3, controller.state.players[0].position)
        assertEquals(2, controller.state.lastDiceRoll)
        assertTrue(PowerUpType.DICE_BOOST in controller.state.matchEvents.last().triggeredPowerUps)
    }

    @Test
    fun rollDice_botUsesPersonalityPowerUpBeforeRoll() {
        val controller = SnakeLadderController(diceRoller = { 2 })
        controller.startGame(
            players = 2,
            mode = GameMode.VS_BOT,
            botPersonality = BotPersonality.RISKY,
            matchMode = MatchModePreset.PARTY_RULES
        )

        controller.applyRoll(2)
        controller.rollDice()

        assertTrue(controller.state.matchEvents.any { it.powerUpUsed == PowerUpType.TRAP })
        assertTrue(controller.state.statusMessage.contains("Rocket Bot rolled"))
    }

    @Test
    fun usePowerUp_ignoresUnavailableDisabledAndFinishedStates() {
        val classic = SnakeLadderController()
        classic.usePowerUp(PowerUpType.SHIELD)
        assertTrue(classic.state.matchEvents.isEmpty())

        val missingInventory = SnakeLadderController()
        missingInventory.startGame(players = 2, mode = GameMode.LOCAL_MULTIPLAYER, matchMode = MatchModePreset.PARTY_RULES)
        missingInventory.loadState(
            missingInventory.state.copy(powerUpInventories = listOf(emptyList(), emptyList()))
        )
        missingInventory.usePowerUp(PowerUpType.SHIELD)
        assertTrue(missingInventory.state.matchEvents.isEmpty())

        val finished = SnakeLadderController()
        finished.startGame(players = 2, mode = GameMode.LOCAL_MULTIPLAYER, matchMode = MatchModePreset.PARTY_RULES)
        finished.loadState(finished.state.copy(winnerIndex = 0))
        finished.usePowerUp(PowerUpType.SHIELD)
        assertTrue(finished.state.matchEvents.isEmpty())
    }

    @Test
    fun usePowerUp_revengeTargetsLeaderAndHandlesNoRivalFallback() {
        val controller = SnakeLadderController()
        controller.startGame(players = 2, mode = GameMode.LOCAL_MULTIPLAYER, matchMode = MatchModePreset.PARTY_RULES)
        controller.loadState(
            controller.state.copy(
                players = listOf(
                    PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 20),
                    PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 70)
                ),
                powerUpInventories = listOf(listOf(PowerUpType.REVENGE), emptyList())
            )
        )

        controller.usePowerUp(PowerUpType.REVENGE)

        assertEquals(64, controller.state.players[1].position)
        assertTrue(controller.state.statusMessage.contains("Player 2"))

        val solo = SnakeLadderController()
        solo.startGame(players = 2, mode = GameMode.LOCAL_MULTIPLAYER, matchMode = MatchModePreset.PARTY_RULES)
        solo.loadState(
            controller.state.copy(
                players = listOf(PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 20)),
                currentPlayerIndex = 0,
                powerUpInventories = listOf(listOf(PowerUpType.REVENGE))
            )
        )
        solo.usePowerUp(PowerUpType.REVENGE)

        assertEquals(20, solo.state.players[0].position)
        assertTrue(solo.state.statusMessage.contains("a rival"))
    }

    @Test
    fun usePowerUp_mysteryCoversAdvanceDiceBoostAndShieldOutcomes() {
        fun controllerWithPriorEvents(priorEvents: Int): SnakeLadderController {
            val controller = SnakeLadderController()
            controller.startGame(players = 2, mode = GameMode.LOCAL_MULTIPLAYER, matchMode = MatchModePreset.PARTY_RULES)
            val events = (1..priorEvents).map { turn ->
                MatchEvent(
                    turnNumber = turn,
                    playerIndex = 0,
                    playerName = "Player 1",
                    dice = 1,
                    startPosition = 1,
                    landedPosition = 2,
                    finalPosition = 2,
                    moveType = MoveType.NORMAL,
                    path = listOf(2)
                )
            }
            controller.loadState(
                controller.state.copy(
                    players = listOf(
                        PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 98),
                        PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 1)
                    ),
                    matchEvents = events,
                    powerUpInventories = listOf(listOf(PowerUpType.MYSTERY), emptyList())
                )
            )
            return controller
        }

        val advance = controllerWithPriorEvents(0)
        advance.usePowerUp(PowerUpType.MYSTERY)
        assertEquals(99, advance.state.players[0].position)

        val diceBoost = controllerWithPriorEvents(1)
        diceBoost.usePowerUp(PowerUpType.MYSTERY)
        assertTrue(PowerUpType.DICE_BOOST in diceBoost.state.powerUpInventories[0])

        val shield = controllerWithPriorEvents(2)
        shield.usePowerUp(PowerUpType.MYSTERY)
        assertTrue(PowerUpType.SHIELD in shield.state.powerUpInventories[0])
    }

    @Test
    fun usePowerUp_handlesMissingPlayerAndRearmingExistingPowerUp() {
        val missingPlayer = SnakeLadderController()
        missingPlayer.startGame(players = 2, mode = GameMode.LOCAL_MULTIPLAYER, matchMode = MatchModePreset.PARTY_RULES)
        missingPlayer.loadState(
            missingPlayer.state.copy(
                players = emptyList(),
                powerUpInventories = listOf(listOf(PowerUpType.SHIELD))
            )
        )
        missingPlayer.usePowerUp(PowerUpType.SHIELD)
        assertTrue(missingPlayer.state.matchEvents.isEmpty())

        val rearm = SnakeLadderController()
        rearm.startGame(players = 2, mode = GameMode.LOCAL_MULTIPLAYER, matchMode = MatchModePreset.PARTY_RULES)
        rearm.loadState(
            rearm.state.copy(
                powerUpInventories = listOf(listOf(PowerUpType.SHIELD, PowerUpType.SHIELD), emptyList()),
                armedPowerUps = listOf(PlayerArmedPowerUp(0, PowerUpType.SHIELD))
            )
        )
        rearm.usePowerUp(PowerUpType.SHIELD)
        assertEquals(1, rearm.state.armedPowerUps.count { it.playerIndex == 0 && it.type == PowerUpType.SHIELD })
        assertEquals(1, rearm.state.powerUpInventories[0].count { it == PowerUpType.SHIELD })
    }

    @Test
    fun cancelArmedPowerUp_returnsQueuedPowerUpWithoutAddingUseEvent() {
        val controller = SnakeLadderController()
        controller.startGame(players = 2, mode = GameMode.LOCAL_MULTIPLAYER, matchMode = MatchModePreset.PARTY_RULES)

        controller.usePowerUp(PowerUpType.SHIELD)
        val eventCountAfterArm = controller.state.matchEvents.size
        assertEquals(listOf(PlayerArmedPowerUp(0, PowerUpType.SHIELD)), controller.state.armedPowerUps)
        assertFalse(PowerUpType.SHIELD in controller.state.powerUpInventories[0])

        controller.cancelArmedPowerUp(PowerUpType.SHIELD)

        assertTrue(controller.state.armedPowerUps.isEmpty())
        assertTrue(PowerUpType.SHIELD in controller.state.powerUpInventories[0])
        assertEquals(eventCountAfterArm, controller.state.matchEvents.size)
        assertTrue(controller.state.statusMessage.contains("canceled Shield"))
    }

    @Test
    fun applyRoll_customSpecialTilesCoverFallbackTargetsAndRewards() {
        val originalCustom = BoardLayouts.custom

        fun runCustomTile(
            tile: BoardTile,
            startPosition: Int,
            dice: Int,
            mode: GameMode = GameMode.LOCAL_MULTIPLAYER,
            botPersonality: BotPersonality = BotPersonality.STEADY,
            currentPlayerIndex: Int = 0
        ): SnakeLadderController {
            BoardLayouts.updateCustom(
                BoardLayout(
                    id = BoardLayouts.CUSTOM_ID,
                    label = "Branch Test Board",
                    description = "Test-only custom branch board.",
                    snakes = emptyMap(),
                    ladders = emptyMap(),
                    specialTiles = listOf(tile)
                )
            )
            val controller = SnakeLadderController()
            controller.startGame(
                players = 2,
                mode = mode,
                botPersonality = botPersonality,
                boardLayoutId = BoardLayouts.CUSTOM_ID
            )
            controller.loadState(
                controller.state.copy(
                    players = listOf(
                        PlayerState(
                            "Player 1",
                            androidx.compose.ui.graphics.Color.Red,
                            if (currentPlayerIndex == 0) startPosition else 1
                        ),
                        PlayerState(
                            botPersonality.displayName,
                            androidx.compose.ui.graphics.Color.Blue,
                            if (currentPlayerIndex == 1) startPosition else 1
                        )
                    ),
                    currentPlayerIndex = currentPlayerIndex
                )
            )
            controller.applyRoll(dice)
            return controller
        }

        try {
            val mystery = runCustomTile(BoardTile(19, BoardTileType.MYSTERY), startPosition = 17, dice = 2)
            assertEquals(listOf(PowerUpType.SHIELD), mystery.state.matchEvents.last().awardedPowerUps)

            val shortcut = runCustomTile(BoardTile(20, BoardTileType.SHORTCUT), startPosition = 18, dice = 2)
            assertEquals(32, shortcut.state.players[0].position)

            val riskSuccess = runCustomTile(BoardTile(20, BoardTileType.RISK_ROUTE), startPosition = 16, dice = 4)
            assertEquals(36, riskSuccess.state.players[0].position)

            val riskFailure = runCustomTile(BoardTile(20, BoardTileType.RISK_ROUTE), startPosition = 18, dice = 2)
            assertEquals(10, riskFailure.state.players[0].position)

            val branch = runCustomTile(BoardTile(20, BoardTileType.BRANCH_PATH), startPosition = 18, dice = 2)
            assertEquals(28, branch.state.players[0].position)

            val defensiveBotBranch = runCustomTile(
                tile = BoardTile(20, BoardTileType.BRANCH_PATH),
                startPosition = 18,
                dice = 2,
                mode = GameMode.VS_BOT,
                botPersonality = BotPersonality.DEFENSIVE,
                currentPlayerIndex = 1
            )
            assertEquals(25, defensiveBotBranch.state.players[1].position)
        } finally {
            BoardLayouts.updateCustom(originalCustom)
        }
    }

    @Test
    fun applyRoll_shieldsAgainstBoardTrapActiveTrapAndKnockback() {
        val boardTrap = SnakeLadderController()
        boardTrap.startGame(
            players = 2,
            mode = GameMode.LOCAL_MULTIPLAYER,
            matchMode = MatchModePreset.PARTY_RULES,
            boardLayoutId = BoardLayouts.TRAP_VALLEY_ID
        )
        boardTrap.loadState(
            boardTrap.state.copy(
                players = listOf(
                    PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 15),
                    PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 1)
                ),
                powerUpInventories = listOf(listOf(PowerUpType.SHIELD), emptyList())
            )
        )
        boardTrap.applyRoll(3)
        assertEquals(18, boardTrap.state.players[0].position)
        assertEquals(MoveType.NORMAL, boardTrap.state.lastMoveType)
        assertTrue(PowerUpType.SHIELD in boardTrap.state.matchEvents.last().triggeredPowerUps)

        val activeTrap = SnakeLadderController()
        activeTrap.startGame(players = 2, mode = GameMode.LOCAL_MULTIPLAYER, matchMode = MatchModePreset.PARTY_RULES)
        activeTrap.loadState(
            activeTrap.state.copy(
                players = listOf(
                    PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 4),
                    PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 1)
                ),
                activeTraps = listOf(BoardTrap(cell = 6, ownerPlayerIndex = 1)),
                powerUpInventories = listOf(listOf(PowerUpType.SHIELD), emptyList())
            )
        )
        activeTrap.applyRoll(2)
        assertEquals(6, activeTrap.state.players[0].position)
        assertEquals(MoveType.NORMAL, activeTrap.state.lastMoveType)
        assertTrue(activeTrap.state.activeTraps.isEmpty())

        val knockback = SnakeLadderController()
        knockback.startGame(players = 2, mode = GameMode.LOCAL_MULTIPLAYER, difficulty = GameDifficulty.HARD)
        knockback.loadState(
            knockback.state.copy(
                players = listOf(
                    PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 1),
                    PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 3)
                ),
                powerUpInventories = listOf(emptyList(), listOf(PowerUpType.SHIELD))
            )
        )
        knockback.applyRoll(2)
        assertEquals(3, knockback.state.players[1].position)
        assertTrue(knockback.state.knockBackMoves.isEmpty())
        assertTrue(knockback.state.statusMessage.contains("blocked knockback"))
    }

    @Test
    fun applyRoll_activeTrapIgnoresOwnOrFinishTraps() {
        val ownTrap = SnakeLadderController()
        ownTrap.startGame(players = 2, mode = GameMode.LOCAL_MULTIPLAYER, matchMode = MatchModePreset.PARTY_RULES)
        ownTrap.loadState(
            ownTrap.state.copy(
                players = listOf(
                    PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 4),
                    PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 1)
                ),
                activeTraps = listOf(BoardTrap(cell = 6, ownerPlayerIndex = 0))
            )
        )
        ownTrap.applyRoll(2)
        assertEquals(6, ownTrap.state.players[0].position)
        assertEquals(MoveType.NORMAL, ownTrap.state.lastMoveType)
        assertEquals(1, ownTrap.state.activeTraps.size)

        val finishTrap = SnakeLadderController()
        finishTrap.startGame(players = 2, mode = GameMode.LOCAL_MULTIPLAYER, matchMode = MatchModePreset.PARTY_RULES)
        finishTrap.loadState(
            finishTrap.state.copy(
                players = listOf(
                    PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 97),
                    PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 1)
                ),
                activeTraps = listOf(BoardTrap(cell = 100, ownerPlayerIndex = 1))
            )
        )
        finishTrap.applyRoll(3)
        assertEquals(100, finishTrap.state.players[0].position)
        assertEquals(MoveType.WIN, finishTrap.state.lastMoveType)
    }

    @Test
    fun applyRoll_teamWinAndTimeoutPopulateWinningTeamBranches() {
        val teamWinFromRuleSet = SnakeLadderController()
        teamWinFromRuleSet.startGame(players = 2, mode = GameMode.LOCAL_MULTIPLAYER, matchMode = MatchModePreset.TEAM_MODE)
        teamWinFromRuleSet.loadState(
            teamWinFromRuleSet.state.copy(
                players = listOf(
                    PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 97, teamId = 0),
                    PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 10, teamId = 1),
                    PlayerState("Player 3", androidx.compose.ui.graphics.Color.Green, 20, teamId = 0),
                    PlayerState("Player 4", androidx.compose.ui.graphics.Color.Yellow, 30, teamId = 1)
                ),
                currentPlayerIndex = 0
            )
        )
        teamWinFromRuleSet.applyRoll(3)
        assertEquals(0, teamWinFromRuleSet.state.winningTeamId)

        val teamWinFromMatchMode = SnakeLadderController()
        teamWinFromMatchMode.startGame(players = 2, mode = GameMode.LOCAL_MULTIPLAYER)
        teamWinFromMatchMode.loadState(
            teamWinFromMatchMode.state.copy(
                players = listOf(
                    PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 97, teamId = 0),
                    PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 10, teamId = 1)
                ),
                matchMode = MatchModePreset.TWO_V_TWO,
                ruleSetId = RuleSets.CLASSIC_ID,
                currentPlayerIndex = 0
            )
        )
        teamWinFromMatchMode.applyRoll(3)
        assertEquals(0, teamWinFromMatchMode.state.winningTeamId)

        val timeoutTeam = SnakeLadderController()
        timeoutTeam.startGame(players = 2, mode = GameMode.LOCAL_MULTIPLAYER)
        timeoutTeam.loadState(
            timeoutTeam.state.copy(
                players = listOf(
                    PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 10, teamId = 0),
                    PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 20, teamId = 1)
                ),
                matchMode = MatchModePreset.TIME_ATTACK,
                ruleSetId = RuleSets.TEAM_ID,
                turnLimit = 1,
                turnsRemaining = 1
            )
        )
        timeoutTeam.applyRoll(1)
        assertEquals(1, timeoutTeam.state.winnerIndex)
        assertEquals(1, timeoutTeam.state.winningTeamId)
    }

    @Test
    fun rollDice_handlesMissingBotStateWhenMatchAlreadyFinished() {
        val controller = SnakeLadderController(diceRoller = { 1 })
        controller.loadState(
            sampleStateAt(p1 = 100, p2 = 1).copy(
                currentPlayerIndex = 5,
                botPlayerIndex = 5,
                winnerIndex = 0
            )
        )

        controller.rollDice()

        assertEquals(100, controller.state.players[0].position)
        assertEquals(0, controller.state.winnerIndex)
    }

    @Test
    fun botStatusCommentsCoverPersonalitiesAndMoveTypes() {
        fun botMove(
            personality: BotPersonality,
            botPosition: Int,
            dice: Int,
            boardLayoutId: String = BoardLayouts.CLASSIC_ID
        ): String {
            val controller = SnakeLadderController()
            controller.startGame(
                players = 2,
                mode = GameMode.VS_BOT,
                botPersonality = personality,
                boardLayoutId = boardLayoutId
            )
            controller.loadState(
                controller.state.copy(
                    players = listOf(
                        PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 1),
                        PlayerState(personality.displayName, androidx.compose.ui.graphics.Color.Blue, botPosition)
                    ),
                    currentPlayerIndex = 1
                )
            )
            controller.applyRoll(dice)
            return controller.state.statusMessage
        }

        assertTrue(botMove(BotPersonality.STEADY, botPosition = 1, dice = 1).contains("keeps the climb controlled"))
        assertTrue(botMove(BotPersonality.STEADY, botPosition = 98, dice = 1).contains("resets and stays"))
        assertTrue(botMove(BotPersonality.STEADY, botPosition = 3, dice = 6).contains("bonus calmly"))
        assertTrue(botMove(BotPersonality.RISKY, botPosition = 1, dice = 1).contains("surges forward"))
        assertTrue(botMove(BotPersonality.RISKY, botPosition = 98, dice = 1).contains("pays for the gamble"))
        assertTrue(botMove(BotPersonality.RISKY, botPosition = 4, dice = 5).contains("pushes the pace"))
        assertTrue(botMove(BotPersonality.DEFENSIVE, botPosition = 1, dice = 1).contains("banks a safe lead"))
        assertTrue(botMove(BotPersonality.DEFENSIVE, botPosition = 98, dice = 1).contains("absorbs the setback"))
        assertTrue(botMove(BotPersonality.DEFENSIVE, botPosition = 3, dice = 2).contains("advances carefully"))
        assertTrue(botMove(BotPersonality.PRO, botPosition = 1, dice = 1).contains("converts the climb"))
        assertTrue(botMove(BotPersonality.PRO, botPosition = 98, dice = 1).contains("recalculates"))
        assertTrue(botMove(BotPersonality.PRO, botPosition = 15, dice = 3, boardLayoutId = BoardLayouts.TRAP_VALLEY_ID).contains("forced a board reset"))
        assertTrue(botMove(BotPersonality.PRO, botPosition = 4, dice = 5).contains("pressures the endgame"))
    }

    @Test
    fun timeAttack_turnLimitExpiresAndLeaderWins() {
        val controller = SnakeLadderController()
        controller.loadState(
            sampleStateAt(p1 = 10, p2 = 20).copy(
                matchMode = MatchModePreset.TIME_ATTACK,
                ruleSetId = RuleSets.TIME_ATTACK_ID,
                turnLimit = 1,
                turnsRemaining = 1
            )
        )

        controller.applyRoll(1)

        assertEquals(1, controller.state.winnerIndex)
        assertEquals(MoveType.TIMEOUT, controller.state.lastMoveType)
        assertTrue(controller.state.statusMessage.contains("Time expired"))
    }

    @Test
    fun bestOfThree_tracksRoundsBeforeMatchWin() {
        val controller = SnakeLadderController()
        controller.startGame(players = 2, mode = GameMode.LOCAL_MULTIPLAYER, matchMode = MatchModePreset.BEST_OF_THREE)
        controller.loadState(
            controller.state.copy(
                players = listOf(
                    PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 97),
                    PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 1)
                ),
                currentPlayerIndex = 0
            )
        )

        controller.applyRoll(3)

        assertNull(controller.state.winnerIndex)
        assertEquals(1, controller.state.roundWins[0])
        assertEquals(2, controller.state.roundNumber)
        assertEquals(listOf(1, 1), controller.state.players.map { it.position })

        controller.loadState(
            controller.state.copy(
                players = listOf(
                    PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 97),
                    PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 1)
                ),
                currentPlayerIndex = 0
            )
        )
        controller.applyRoll(3)

        assertEquals(0, controller.state.winnerIndex)
        assertEquals(2, controller.state.roundWins[0])
    }

    @Test
    fun twoVsTwo_forcesFourPlayersAndAssignsTeams() {
        val controller = SnakeLadderController()

        controller.startGame(players = 2, mode = GameMode.LOCAL_MULTIPLAYER, matchMode = MatchModePreset.TWO_V_TWO)

        assertEquals(4, controller.state.players.size)
        assertEquals(listOf(0, 1, 0, 1), controller.state.players.map { it.teamId })
        assertEquals(RuleSets.TEAM_ID, controller.state.ruleSetId)
    }

    @Test
    fun simulation_thousandBotAndRuleMatchesStayWithinValidState() {
        val boards = listOf(
            BoardLayouts.CLASSIC_ID,
            BoardLayouts.TRAP_VALLEY_ID,
            BoardLayouts.PRO_CHAOS_ID,
            BoardLayouts.FESTIVAL_EVENT_ID,
            BoardLayouts.MONSOON_EVENT_ID
        )
        val modes = listOf(
            MatchModePreset.CLASSIC,
            MatchModePreset.TIME_ATTACK,
            MatchModePreset.SUDDEN_DEATH,
            MatchModePreset.PARTY_RULES,
            MatchModePreset.TACTICAL_CARDS
        )

        repeat(1_000) { index ->
            var rollSeed = index
            val controller = SnakeLadderController(
                diceRoller = {
                    rollSeed = (rollSeed * 31 + 7) % 6
                    rollSeed + 1
                }
            )
            controller.startGame(
                players = 2,
                mode = GameMode.VS_BOT,
                difficulty = GameDifficulty.entries[index % GameDifficulty.entries.size],
                botPersonality = BotPersonality.entries[index % BotPersonality.entries.size],
                matchMode = modes[index % modes.size],
                boardLayoutId = boards[index % boards.size]
            )
            repeat(120) {
                if (controller.state.winnerIndex == null) controller.rollDice()
            }

            assertTrue(controller.state.players.all { it.position in 1..100 })
            assertTrue(controller.state.currentPlayerIndex in controller.state.players.indices)
            assertTrue(controller.state.matchEvents.size <= 260)
        }
    }

    @Test
    fun replayDeterminism_sameDiceSequenceProducesSameEvents() {
        fun runMatch(): List<MatchEvent> {
            val rolls = listOf(3, 5, 2, 6, 1, 4, 6, 2, 5, 3)
            var index = 0
            val controller = SnakeLadderController(
                diceRoller = {
                    val value = rolls[index % rolls.size]
                    index += 1
                    value
                }
            )
            controller.startGame(
                players = 2,
                mode = GameMode.LOCAL_MULTIPLAYER,
                difficulty = GameDifficulty.HARD,
                matchMode = MatchModePreset.TACTICAL_CARDS,
                boardLayoutId = BoardLayouts.PRO_CHAOS_ID
            )
            repeat(60) {
                if (controller.state.winnerIndex == null) controller.rollDice()
            }
            return controller.state.matchEvents
        }

        val first = runMatch()
        val second = runMatch()

        assertEquals(
            first.map { listOf(it.playerIndex, it.dice, it.startPosition, it.landedPosition, it.finalPosition, it.moveType, it.tileLabel) },
            second.map { listOf(it.playerIndex, it.dice, it.startPosition, it.landedPosition, it.finalPosition, it.moveType, it.tileLabel) }
        )
    }
}
