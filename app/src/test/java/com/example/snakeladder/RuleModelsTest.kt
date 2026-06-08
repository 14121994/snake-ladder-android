package com.example.snakeladder

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleModelsTest {

    @Test
    fun ruleSets_mapMatchModesToExpectedRules() {
        assertEquals(RuleSets.daily, RuleSets.forMatchMode(MatchModePreset.DAILY_CHALLENGE))
        assertEquals(RuleSets.quest, RuleSets.forMatchMode(MatchModePreset.QUEST_NODE))
        assertEquals(RuleSets.timeAttack, RuleSets.forMatchMode(MatchModePreset.TIME_ATTACK))
        assertEquals(RuleSets.suddenDeath, RuleSets.forMatchMode(MatchModePreset.SUDDEN_DEATH))
        assertEquals(RuleSets.bestOfThree, RuleSets.forMatchMode(MatchModePreset.BEST_OF_THREE))
        assertEquals(RuleSets.partyPower, RuleSets.forMatchMode(MatchModePreset.PARTY_RULES))
        assertEquals(RuleSets.tacticalCards, RuleSets.forMatchMode(MatchModePreset.TACTICAL_CARDS))
        assertEquals(RuleSets.teamPower, RuleSets.forMatchMode(MatchModePreset.TWO_V_TWO))
    }

    @Test
    fun powerUpRuleEngine_exposesEnabledPowerUpsForPartyRules() {
        val powerUps = PowerUpRuleEngine.availablePowerUps(RuleSets.partyPower)

        assertTrue(PowerUpType.SHIELD in powerUps)
        assertTrue(PowerUpType.REROLL in powerUps)
        assertTrue(PowerUpType.MYSTERY in powerUps)
        assertTrue(PowerUpRuleEngine.describe(RuleSets.partyPower).contains("Power-ups"))
    }

    @Test
    fun boardLayouts_resolveUnknownIdToClassic() {
        assertEquals(BoardLayouts.classic, BoardLayouts.byId("missing"))
        assertEquals(34, BoardLayouts.quickClimb.ladders[12])
        assertEquals(24, BoardLayouts.snakeDen.snakes[88])
        assertEquals(97, BoardLayouts.speedRun.ladders[79])
        assertEquals(13, BoardLayouts.trapValley.snakes[41])
        assertTrue(BoardLayouts.proChaos.specialTiles.any { it.type == BoardTileType.RISK_ROUTE })
        assertTrue(BoardLayouts.festivalEvent.specialTiles.any { it.type == BoardTileType.MYSTERY })
    }

    @Test
    fun customBoardParsesAndBuildsPlayableLayout() {
        val snakes = CustomBoardStore.parsePairs("97-61, 70->42")
        val ladders = CustomBoardStore.parsePairs("4-29;18:46")
        val layout = BoardLayouts.customFromPairs(snakes, ladders)

        assertEquals(61, layout.snakes[97])
        assertEquals(42, layout.snakes[70])
        assertEquals(29, layout.ladders[4])
        assertEquals("4-29,18-46", CustomBoardStore.formatPairs(layout.ladders))
    }

    @Test
    fun customBoardFiltersInvalidPairsCapsEntriesAndRemovesTileCollisions() {
        val snakePairs = listOf(100 to 1, 30 to 30, 20 to 25) + (10..25).map { it to 1 }
        val ladderPairs = listOf(1 to 20, 99 to 99, 40 to 10) + (2..18).map { it to it + 10 }
        val layout = BoardLayouts.customFromPairs(
            snakePairs = snakePairs,
            ladderPairs = ladderPairs,
            specialTiles = listOf(
                BoardTile(10, BoardTileType.MYSTERY, rewardPowerUp = PowerUpType.REROLL),
                BoardTile(60, BoardTileType.SHORTCUT, targetCell = 78)
            )
        )

        assertEquals(12, layout.snakes.size)
        assertEquals(12, layout.ladders.size)
        assertFalse(100 in layout.snakes)
        assertFalse(30 in layout.snakes)
        assertFalse(layout.specialTiles.any { it.cell == 10 })
        assertTrue(layout.specialTiles.any { it.cell == 60 })
    }

    @Test
    fun customBoardFilteringCoversBoundaryAndPreserveBranches() {
        val current = BoardLayouts.custom
        val layout = BoardLayouts.customFromPairs(
            snakePairs = listOf(2 to 1, 99 to 98, 100 to 2, 4 to 4, 5 to 6, 8 to 0),
            ladderPairs = listOf(2 to 3, 99 to 100, 1 to 3, 4 to 4, 5 to 4, 10 to 101),
            specialTiles = listOf(
                BoardTile(2, BoardTileType.MYSTERY),
                BoardTile(99, BoardTileType.SHORTCUT, targetCell = 100),
                BoardTile(60, BoardTileType.TRAP)
            ),
            preserveCurrentWhenEmpty = true
        )
        val emptyPreview = BoardLayouts.customFromPairs(
            snakePairs = emptyList(),
            ladderPairs = emptyList(),
            specialTiles = emptyList(),
            preserveCurrentWhenEmpty = false
        )

        assertEquals(1, layout.snakes[2])
        assertEquals(98, layout.snakes[99])
        assertFalse(100 in layout.snakes)
        assertEquals(3, layout.ladders[2])
        assertEquals(100, layout.ladders[99])
        assertFalse(layout.specialTiles.any { it.cell == 2 || it.cell == 99 })
        assertTrue(layout.specialTiles.any { it.cell == 60 })
        assertEquals(current.snakes, BoardLayouts.customFromPairs(emptyList(), emptyList()).snakes)
        assertTrue(emptyPreview.snakes.isEmpty())
        assertTrue(emptyPreview.ladders.isEmpty())
    }

    @Test
    fun customBoardPreviewCanClearOneSectionWithoutFallingBackToSavedPairs() {
        val preview = BoardLayouts.customFromPairs(
            snakePairs = emptyList(),
            ladderPairs = listOf(4 to 29, 18 to 46),
            preserveCurrentWhenEmpty = false
        )

        assertTrue(preview.snakes.isEmpty())
        assertEquals(29, preview.ladders[4])
        assertEquals(46, preview.ladders[18])
    }

    @Test
    fun customBoardParserSkipsMalformedNumbersAndIncompletePairs() {
        val parsed = CustomBoardStore.parsePairs("10-x, y-2, 30-20, 40, 50->")

        assertEquals(listOf(30 to 20), parsed)
    }

    @Test
    fun boardTileAndCustomBoardRejectInvalidConfiguration() {
        assertThrows(IllegalArgumentException::class.java) {
            BoardTile(1, BoardTileType.MYSTERY)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BoardTile(20, BoardTileType.SHORTCUT, targetCell = 101)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BoardLayouts.updateCustom(BoardLayouts.classic)
        }
    }

    @Test
    fun boardTilesAndRuleLookupsCoverValidOptionalBranches() {
        val mystery = BoardTile(22, BoardTileType.MYSTERY)
        val shortcut = BoardTile(58, BoardTileType.SHORTCUT, targetCell = 100)

        assertEquals("Mystery", mystery.label)
        assertNull(mystery.targetCell)
        assertEquals(100, shortcut.targetCell)
        assertEquals(RuleSets.partyPower, RuleSets.byId(RuleSets.PARTY_ID))
        assertEquals(RuleSets.classic, RuleSets.byId("not_real"))
    }

    @Test
    fun difficultyVisualLabelsCoverEveryDifficulty() {
        assertEquals(listOf("E", "M", "H"), GameDifficulty.entries.map { it.iconLabel() })
        assertEquals(listOf("Easy", "Medium", "Hard"), GameDifficulty.entries.map { it.shortLabel() })
        assertEquals(listOf("Easy", "Med", "Hard"), GameDifficulty.entries.map { it.compactLabel() })
        assertEquals(listOf("KB --", "KB N", "KB N+S+L"), GameDifficulty.entries.map { it.knockbackSymbolLabel() })
        assertEquals(listOf("No KB", "Normal", "N/S/L"), GameDifficulty.entries.map { it.compactKnockbackLabel() })
        assertEquals(
            listOf("No knockback", "Normal landings", "Normal, snake, ladder"),
            GameDifficulty.entries.map { it.knockbackRuleLabel() }
        )
    }

    @Test
    fun campaignCatalog_unlocksNodesFromProfileWins() {
        val profile = PlayerProfile(humanWins = 2)
        val unlocked = CampaignCatalog.unlockedNodes(profile).map { it.id }

        assertTrue("classic_start" in unlocked)
        assertTrue("ladder_sprint" in unlocked)
        assertTrue("snake_survival" in unlocked)
        assertTrue("party_trial" !in unlocked)
    }

    @Test
    fun campaignCatalog_keepsCompletedNodesUnlockedRegardlessOfWinRequirement() {
        val unlocked = CampaignCatalog.unlockedNodes(
            PlayerProfile(humanWins = 0, completedCampaignNodeIds = setOf("boss_ladder_king"))
        ).map { it.id }

        assertTrue("classic_start" in unlocked)
        assertTrue("boss_ladder_king" in unlocked)
        assertFalse("ladder_sprint" in unlocked)
    }

    @Test
    fun campaignCatalogFallsBackToStarterNodeWhenWinsAreMissingOrUnlockedNodesAreCompleted() {
        val starterOnly = CampaignCatalog.unlockedNodes(PlayerProfile(humanWins = -1))
        val completedStarter = CampaignCatalog.nextPlayableNode(
            PlayerProfile(humanWins = -1, completedCampaignNodeIds = setOf("classic_start"))
        )

        assertEquals(listOf("classic_start"), starterOnly.map { it.id })
        assertEquals("classic_start", completedStarter?.id)
    }

    @Test
    fun dailyChallengeCarriesPlayableBoardModeAndReward() {
        val challenge = DailyChallengeCatalog.today(nowMillis = 1_780_294_400_000L)

        assertTrue(BoardLayouts.all.any { it.id == challenge.boardLayoutId })
        assertTrue(challenge.reward.coins > 0 || challenge.reward.xp > 0)
        assertTrue(challenge.matchMode == MatchModePreset.DAILY_CHALLENGE || challenge.matchMode == MatchModePreset.PARTY_RULES)
    }

    @Test
    fun powerUpRuleEngineAwardsAndCapsInventory() {
        val event = MatchEvent(
            turnNumber = 5,
            playerIndex = 0,
            playerName = "Player 1",
            dice = 6,
            startPosition = 1,
            landedPosition = 7,
            finalPosition = 14,
            moveType = MoveType.LADDER,
            path = listOf(2, 3, 4, 5, 6, 7, 14)
        )

        val awards = PowerUpRuleEngine.awardsForMove(RuleSets.partyPower, event)
        val capped = PowerUpRuleEngine.addPowerUps(
            listOf(PowerUpType.SHIELD, PowerUpType.TRAP, PowerUpType.REROLL),
            awards
        )

        assertTrue(PowerUpType.DICE_BOOST in awards)
        assertEquals(4, capped.size)
    }

    @Test
    fun powerUpRuleEngineCoversAwardTrapFallbackAndBotNoChoiceBranches() {
        fun event(moveType: MoveType, turn: Int = 1, dice: Int = 3, knockedBack: Boolean = false): MatchEvent {
            return MatchEvent(
                turnNumber = turn,
                playerIndex = 0,
                playerName = "Player 1",
                dice = dice,
                startPosition = 10,
                landedPosition = 13,
                finalPosition = 13,
                moveType = moveType,
                path = listOf(11, 12, 13),
                knockedBackPlayerIndices = if (knockedBack) listOf(1) else emptyList()
            )
        }

        assertEquals(listOf(PowerUpType.SHIELD), PowerUpRuleEngine.awardsForMove(RuleSets.partyPower, event(MoveType.SNAKE)))
        assertEquals(listOf(PowerUpType.REVENGE), PowerUpRuleEngine.awardsForMove(RuleSets.partyPower, event(MoveType.TRAP)))
        assertEquals(listOf(PowerUpType.REVENGE), PowerUpRuleEngine.awardsForMove(RuleSets.partyPower, event(MoveType.NORMAL, knockedBack = true)))
        assertEquals(listOf(PowerUpType.REROLL), PowerUpRuleEngine.awardsForMove(RuleSets.partyPower, event(MoveType.NORMAL, dice = 6)))
        assertEquals(listOf(PowerUpType.MYSTERY), PowerUpRuleEngine.awardsForMove(RuleSets.partyPower, event(MoveType.NORMAL, turn = 5)))
        assertTrue(PowerUpRuleEngine.awardsForMove(RuleSets.classic, event(MoveType.LADDER)).isEmpty())
        assertTrue(PowerUpRuleEngine.awardsForMove(RuleSets.partyPower, event(MoveType.POWER_UP)).isEmpty())
        assertEquals(listOf(PowerUpType.SHIELD), PowerUpRuleEngine.addPowerUps(listOf(PowerUpType.SHIELD), emptyList()))
        assertEquals(50, PowerUpRuleEngine.trapCellFor(position = 98, occupiedCells = (2..99).toSet()))
        assertNull(PowerUpRuleEngine.chooseBotPowerUp(BotPersonality.STEADY, emptyList(), botPosition = 20, leaderGap = 0))
        assertNull(PowerUpRuleEngine.chooseBotPowerUp(BotPersonality.DEFENSIVE, listOf(PowerUpType.REVENGE), botPosition = 20, leaderGap = 2))
    }

    @Test
    fun proBotPrioritizesComebackAndEndgamePowerUps() {
        val comebackChoice = PowerUpRuleEngine.chooseBotPowerUp(
            personality = BotPersonality.PRO,
            inventory = listOf(PowerUpType.DICE_BOOST, PowerUpType.REVENGE, PowerUpType.TRAP),
            botPosition = 58,
            leaderGap = 5
        )
        val endgameChoice = PowerUpRuleEngine.chooseBotPowerUp(
            personality = BotPersonality.PRO,
            inventory = listOf(PowerUpType.DICE_BOOST, PowerUpType.TRAP),
            botPosition = 72,
            leaderGap = 0
        )
        val hardChoice = PowerUpRuleEngine.chooseBotPowerUp(
            personality = BotPersonality.STEADY,
            inventory = listOf(PowerUpType.SHIELD, PowerUpType.DICE_BOOST),
            botPosition = 30,
            leaderGap = 0,
            difficulty = GameDifficulty.HARD
        )

        assertEquals(PowerUpType.REVENGE, comebackChoice)
        assertEquals(PowerUpType.DICE_BOOST, endgameChoice)
        assertEquals(PowerUpType.DICE_BOOST, hardChoice)
    }

    @Test
    fun botPowerUpSelectionCoversPersonalityAndDifficultyPriorities() {
        assertEquals(
            PowerUpType.TRAP,
            PowerUpRuleEngine.chooseBotPowerUp(
                personality = BotPersonality.RISKY,
                inventory = listOf(PowerUpType.TRAP, PowerUpType.DICE_BOOST),
                botPosition = 20,
                leaderGap = 0
            )
        )
        assertEquals(
            PowerUpType.SHIELD,
            PowerUpRuleEngine.chooseBotPowerUp(
                personality = BotPersonality.DEFENSIVE,
                inventory = listOf(PowerUpType.SHIELD, PowerUpType.TRAP),
                botPosition = 20,
                leaderGap = 0
            )
        )
        assertEquals(
            PowerUpType.TRAP,
            PowerUpRuleEngine.chooseBotPowerUp(
                personality = BotPersonality.STEADY,
                inventory = listOf(PowerUpType.TRAP, PowerUpType.MYSTERY),
                botPosition = 20,
                leaderGap = 0,
                difficulty = GameDifficulty.MEDIUM
            )
        )
        assertEquals(
            PowerUpType.SHIELD,
            PowerUpRuleEngine.chooseBotPowerUp(
                personality = BotPersonality.PRO,
                inventory = listOf(PowerUpType.SHIELD, PowerUpType.MYSTERY),
                botPosition = 44,
                leaderGap = 1
            )
        )
    }

    @Test
    fun botPowerUpSelectionCoversRejectedPriorityPredicates() {
        val lowPositionSteady = PowerUpRuleEngine.chooseBotPowerUp(
            personality = BotPersonality.STEADY,
            inventory = listOf(PowerUpType.DICE_BOOST, PowerUpType.TRAP),
            botPosition = 20,
            leaderGap = 0
        )
        val proWithoutEnoughGap = PowerUpRuleEngine.chooseBotPowerUp(
            personality = BotPersonality.PRO,
            inventory = listOf(PowerUpType.REVENGE, PowerUpType.SHIELD),
            botPosition = 39,
            leaderGap = 4
        )
        val defensiveShield = PowerUpRuleEngine.chooseBotPowerUp(
            personality = BotPersonality.DEFENSIVE,
            inventory = listOf(PowerUpType.SHIELD),
            botPosition = 10,
            leaderGap = 0
        )
        val hardShield = PowerUpRuleEngine.chooseBotPowerUp(
            personality = BotPersonality.STEADY,
            inventory = listOf(PowerUpType.SHIELD),
            botPosition = 10,
            leaderGap = 0,
            difficulty = GameDifficulty.HARD
        )
        val proLateDiceBoost = PowerUpRuleEngine.chooseBotPowerUp(
            personality = BotPersonality.PRO,
            inventory = listOf(PowerUpType.DICE_BOOST),
            botPosition = 55,
            leaderGap = 0
        )
        val steadyLateDiceBoost = PowerUpRuleEngine.chooseBotPowerUp(
            personality = BotPersonality.STEADY,
            inventory = listOf(PowerUpType.DICE_BOOST),
            botPosition = 75,
            leaderGap = 0
        )
        val steadyEarlyDiceBoost = PowerUpRuleEngine.chooseBotPowerUp(
            personality = BotPersonality.STEADY,
            inventory = listOf(PowerUpType.DICE_BOOST),
            botPosition = 40,
            leaderGap = 0
        )
        val proShieldAtThreshold = PowerUpRuleEngine.chooseBotPowerUp(
            personality = BotPersonality.PRO,
            inventory = listOf(PowerUpType.SHIELD),
            botPosition = 40,
            leaderGap = 0
        )

        assertEquals(PowerUpType.TRAP, lowPositionSteady)
        assertNull(proWithoutEnoughGap)
        assertEquals(PowerUpType.SHIELD, defensiveShield)
        assertEquals(PowerUpType.SHIELD, hardShield)
        assertEquals(PowerUpType.DICE_BOOST, proLateDiceBoost)
        assertEquals(PowerUpType.DICE_BOOST, steadyLateDiceBoost)
        assertNull(steadyEarlyDiceBoost)
        assertEquals(PowerUpType.SHIELD, proShieldAtThreshold)
    }

    @Test
    fun storeCatalogPurchasesAndUnlocksItems() {
        val result = StoreCatalog.purchase(
            profile = PlayerProfile(coins = 500, gems = 5),
            itemId = "board_pro_chaos"
        )

        assertTrue(result.purchased)
        assertEquals(200, result.profile.coins)
        assertEquals(2, result.profile.gems)
        assertTrue(BoardLayouts.PRO_CHAOS_ID in result.profile.unlockedBoardIds)
    }

    @Test
    fun storeCatalogHandlesUnavailableInsufficientFundsAndOwnedEquipPaths() {
        val poorProfile = PlayerProfile(coins = 10, gems = 0)

        val unavailable = StoreCatalog.purchase(poorProfile, "missing_item")
        val unaffordable = StoreCatalog.purchase(poorProfile, "avatar_gold_die")
        val equippedAvatar = StoreCatalog.purchase(
            profile = PlayerProfile(
                selectedAvatarId = "classic_token",
                unlockedAvatarIds = setOf("classic_token", "cobra_token")
            ),
            itemId = "avatar_cobra_token"
        )
        val equippedTitle = StoreCatalog.purchase(
            profile = PlayerProfile(
                selectedTitle = "New Challenger",
                unlockedTitleIds = setOf("title_new_challenger", "title_snake_tamer")
            ),
            itemId = "title_snake_tamer"
        )

        assertFalse(unavailable.purchased)
        assertEquals(poorProfile, unavailable.profile)
        assertFalse(unaffordable.purchased)
        assertEquals(poorProfile, unaffordable.profile)
        assertFalse(equippedAvatar.purchased)
        assertEquals("cobra_token", equippedAvatar.profile.selectedAvatarId)
        assertFalse(equippedTitle.purchased)
        assertEquals("Snake Tamer", equippedTitle.profile.selectedTitle)
    }

    @Test
    fun storeCatalogPurchasesAvatarAndTitleItems() {
        val avatar = StoreCatalog.purchase(PlayerProfile(coins = 200, gems = 0), "avatar_cobra_token")
        val title = StoreCatalog.purchase(PlayerProfile(coins = 300, gems = 1), "title_board_strategist")

        assertTrue(avatar.purchased)
        assertEquals("cobra_token", avatar.profile.selectedAvatarId)
        assertTrue("cobra_token" in avatar.profile.unlockedAvatarIds)
        assertTrue(title.purchased)
        assertEquals("Board Strategist", title.profile.selectedTitle)
        assertTrue("title_board_strategist" in title.profile.unlockedTitleIds)
    }

    @Test
    fun storeCatalogChecksOwnershipAndAffordabilityAcrossItemTypes() {
        val boardItem = StoreCatalog.byId("board_speed_run") ?: error("Missing board item")
        val titleItem = StoreCatalog.byId("title_board_strategist") ?: error("Missing title item")

        val ownedBoard = StoreCatalog.purchase(
            PlayerProfile(unlockedBoardIds = setOf(BoardLayouts.CLASSIC_ID, BoardLayouts.SPEED_RUN_ID)),
            boardItem.id
        )

        assertTrue(StoreCatalog.isOwned(PlayerProfile(unlockedBoardIds = setOf(BoardLayouts.SPEED_RUN_ID)), boardItem))
        assertFalse(StoreCatalog.canAfford(PlayerProfile(coins = 1_000, gems = 0), titleItem))
        assertFalse(ownedBoard.purchased)
        assertEquals(BoardLayouts.SPEED_RUN_ID, boardItem.targetId)
    }

    @Test
    fun storeCatalogEquipCoversUnavailableLockedBoardAndOwnedItems() {
        val unavailable = StoreCatalog.equip(PlayerProfile(), "missing_item")
        val locked = StoreCatalog.equip(PlayerProfile(), "avatar_cobra_token")
        val board = StoreCatalog.equip(
            PlayerProfile(unlockedBoardIds = setOf(BoardLayouts.CLASSIC_ID, BoardLayouts.SPEED_RUN_ID)),
            "board_speed_run"
        )
        val avatar = StoreCatalog.equip(
            PlayerProfile(unlockedAvatarIds = setOf("classic_token", "cobra_token")),
            "avatar_cobra_token"
        )
        val title = StoreCatalog.equip(
            PlayerProfile(unlockedTitleIds = setOf("title_new_challenger", "title_snake_tamer")),
            "title_snake_tamer"
        )

        assertFalse(unavailable.purchased)
        assertTrue(unavailable.message.contains("unavailable"))
        assertFalse(locked.purchased)
        assertTrue(locked.message.contains("locked"))
        assertFalse(board.purchased)
        assertTrue(board.message.contains("New Game"))
        assertEquals("cobra_token", avatar.profile.selectedAvatarId)
        assertEquals("Snake Tamer", title.profile.selectedTitle)
        assertEquals(PLAYER_PROFILE_SCHEMA_VERSION, title.profile.schemaVersion)
    }

    @Test
    fun matchSummaryCarriesSchemaAndRecentMatchMetadata() {
        val state = GameState(
            players = listOf(
                PlayerState("Player 1", Color.Red, 100),
                PlayerState("Player 2", Color.Blue, 1)
            ),
            currentPlayerIndex = 0,
            lastDiceRoll = 3,
            statusMessage = "Player 1 wins!",
            bonusTurnGranted = false,
            winnerIndex = 0,
            moveHistory = emptyList(),
            gameMode = GameMode.LOCAL_MULTIPLAYER,
            botPlayerIndex = null,
            lastMovePlayerIndex = 0,
            lastMovePath = listOf(98, 99, 100),
            lastMoveType = MoveType.WIN,
            moveSignal = 1,
            matchEvents = listOf(
                MatchEvent(1, 0, "Player 1", 3, 97, 100, 100, MoveType.WIN, listOf(98, 99, 100), winner = true)
            ),
            matchMode = MatchModePreset.PARTY_RULES,
            boardLayoutId = BoardLayouts.SNAKE_DEN_ID,
            ruleSetId = RuleSets.PARTY_ID
        )

        val result = PlayerProfile().recordCompletedMatch(state)

        assertEquals(PLAYER_PROFILE_SCHEMA_VERSION, result.profile.schemaVersion)
        assertEquals(1, result.profile.recentMatches.size)
        assertEquals(MatchModePreset.PARTY_RULES, result.profile.recentMatches.first().matchMode)
        assertEquals(BoardLayouts.SNAKE_DEN_ID, result.profile.recentMatches.first().boardLayoutId)
    }
}
