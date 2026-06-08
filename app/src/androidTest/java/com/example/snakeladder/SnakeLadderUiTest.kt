package com.example.snakeladder

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.example.snakeladder.ui.theme.SnakeLadderTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.unit.dp

class SnakeLadderUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setLaunchContent(
        orientation: Int = Configuration.ORIENTATION_PORTRAIT,
        screenWidthDp: Int? = null,
        screenHeightDp: Int? = null,
        playerProfile: PlayerProfile = PlayerProfile(),
        savedGames: List<SavedGameSnapshot> = emptyList(),
        onLoadSavedGame: (SavedGameSnapshot) -> Unit = {},
        onDeleteSavedGame: (SavedGameSnapshot) -> Unit = {},
        onEquipProfileItem: (String) -> String = { "Profile equipment unavailable." },
        onPurchaseStoreItem: (String) -> String = { "Store unavailable." },
        onExportProfile: () -> String = { "" },
        onShareProfile: (String) -> Boolean = { false },
        onStart: (GameDifficulty) -> Unit = {}
    ) {
        val selectedPlayers = mutableStateOf(2)
        val selectedMode = mutableStateOf(GameMode.LOCAL_MULTIPLAYER)
        val selectedBotPersonality = mutableStateOf(BotPersonality.STEADY)
        val selectedMatchMode = mutableStateOf(MatchModePreset.CLASSIC)
        val selectedBoardLayoutId = mutableStateOf(BoardLayouts.CLASSIC_ID)
        val playerSetups = mutableStateOf(defaultPlayerSetups())

        composeRule.setContent {
            SnakeLadderTheme {
                OrientationContainer(
                    orientation = orientation,
                    screenWidthDp = screenWidthDp,
                    screenHeightDp = screenHeightDp
                ) {
                    LaunchLayout(
                        selectedPlayers = selectedPlayers.value,
                        selectedMode = selectedMode.value,
                        selectedMatchMode = selectedMatchMode.value,
                        selectedBoardLayoutId = selectedBoardLayoutId.value,
                        selectedBotPersonality = selectedBotPersonality.value,
                        playerProfile = playerProfile,
                        playerSetups = playerSetups.value,
                        savedGames = savedGames,
                        onRefreshSavedGames = {},
                        onSelectPlayers = { selectedPlayers.value = it },
                        onSelectMode = { mode ->
                            selectedMode.value = mode
                            if (mode == GameMode.VS_BOT) selectedPlayers.value = 2
                        },
                        onSelectMatchMode = { mode ->
                            selectedMatchMode.value = mode
                            if (mode == MatchModePreset.TEAM_MODE || mode == MatchModePreset.TWO_V_TWO) {
                                selectedMode.value = GameMode.LOCAL_MULTIPLAYER
                                selectedPlayers.value = 4
                            }
                        },
                        onSelectBoardLayout = { selectedBoardLayoutId.value = it },
                        onSelectBotPersonality = { selectedBotPersonality.value = it },
                        onUpdatePlayerSetup = { index, setup ->
                            playerSetups.value = List(4) { draftIndex ->
                                if (draftIndex == index) {
                                    setup
                                } else {
                                    playerSetups.value.getOrNull(draftIndex)
                                        ?: PlayerSetup(
                                            defaultPlayerName(draftIndex + 1),
                                            defaultPlayerAvatarId(draftIndex + 1)
                                        )
                                }
                            }
                        },
                        onLoadSavedGame = onLoadSavedGame,
                        onDeleteSavedGame = onDeleteSavedGame,
                        onExportProfile = onExportProfile,
                        onShareProfile = onShareProfile,
                        onEquipProfileItem = onEquipProfileItem,
                        onPurchaseStoreItem = onPurchaseStoreItem,
                        onStart = onStart
                    )
                }
            }
        }
    }

    @Composable
    private fun OrientationContainer(
        orientation: Int,
        screenWidthDp: Int? = null,
        screenHeightDp: Int? = null,
        content: @Composable () -> Unit
    ) {
        val base = LocalConfiguration.current
        val frameWidthDp = screenWidthDp ?: if (orientation == Configuration.ORIENTATION_LANDSCAPE) 860 else 420
        val frameHeightDp = screenHeightDp ?: if (orientation == Configuration.ORIENTATION_LANDSCAPE) 420 else 860
        val overridden = Configuration(base).apply {
            this.orientation = orientation
            this.screenWidthDp = frameWidthDp
            this.screenHeightDp = frameHeightDp
        }
        val frameModifier = Modifier.size(frameWidthDp.dp, frameHeightDp.dp)
        CompositionLocalProvider(LocalConfiguration provides overridden) {
            Box(modifier = frameModifier) {
                content()
            }
        }
    }

    private fun sampleGameState(
        gameMode: GameMode = GameMode.LOCAL_MULTIPLAYER,
        currentPlayerIndex: Int = 0,
        botPlayerIndex: Int? = null,
        winnerIndex: Int? = null
    ): GameState {
        return GameState(
            players = listOf(
                PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 1),
                PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 1)
            ),
            currentPlayerIndex = currentPlayerIndex,
            lastDiceRoll = null,
            statusMessage = "Player 1 starts. Roll the dice.",
            bonusTurnGranted = false,
            winnerIndex = winnerIndex,
            moveHistory = emptyList(),
            gameMode = gameMode,
            botPlayerIndex = botPlayerIndex,
            lastMovePlayerIndex = null,
            lastMovePath = emptyList(),
            lastMoveType = null,
            moveSignal = 0
        )
    }

    private fun sampleSavedGame(
        id: String,
        name: String,
        savedAt: Long = 123456789L,
        gameMode: GameMode = GameMode.LOCAL_MULTIPLAYER,
        botPlayerIndex: Int? = null,
        boardLayoutId: String = BoardLayouts.CLASSIC_ID,
        matchMode: MatchModePreset = MatchModePreset.CLASSIC,
        difficulty: GameDifficulty = GameDifficulty.EASY
    ): SavedGameSnapshot {
        return SavedGameSnapshot(
            id = id,
            name = name,
            savedAt = savedAt,
            boardTheme = BoardThemeOption.VIBRANT,
            state = sampleGameState(gameMode = gameMode, botPlayerIndex = botPlayerIndex)
                .copy(
                    boardLayoutId = boardLayoutId,
                    matchMode = matchMode,
                    difficulty = difficulty
                )
        )
    }

    private fun resetBoardSettings() {
        InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("board_settings", 0)
            .edit()
            .clear()
            .commit()
    }

    private fun setBoardContent(
        orientation: Int,
        state: GameState = sampleGameState(),
        onStartNewGame: (GameDifficulty) -> Unit = {},
        onRollDice: () -> Unit = {},
        onUsePowerUp: (PowerUpType) -> Unit = {},
        onCancelArmedPowerUp: (PowerUpType) -> Unit = {},
        onRestart: () -> Unit = {},
        onSaveGame: (String) -> Boolean = { true },
        onExit: () -> Unit = {},
        playerProfile: PlayerProfile = PlayerProfile(),
        dailyChallenge: DailyChallenge = DailyChallengeCatalog.today()
    ) {
        val selectedBoardTheme = mutableStateOf(BoardThemeOption.VIBRANT)
        composeRule.setContent {
            SnakeLadderTheme {
                OrientationContainer(orientation = orientation) {
                    SnakeLadderScreen(
                        state = state,
                        selectedBoardTheme = selectedBoardTheme.value,
                        onSelectBoardTheme = { selectedBoardTheme.value = it },
                        onStartNewGame = onStartNewGame,
                        onRollDice = onRollDice,
                        onUsePowerUp = onUsePowerUp,
                        onCancelArmedPowerUp = onCancelArmedPowerUp,
                        onRestart = onRestart,
                        onSaveGame = onSaveGame,
                        onExit = onExit,
                        playerProfile = playerProfile,
                        dailyChallenge = dailyChallenge
                    )
                }
            }
        }
    }

    @Test
    fun launchScreen_showsMainActionsOnly() {
        setLaunchContent()
        composeRule.onNodeWithTag("new_game_button").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_challenge_start_button").assertIsDisplayed()
        composeRule.onNodeWithTag("campaign_button").assertIsDisplayed()
        composeRule.onNodeWithTag("launch_secondary_navigation").assertIsDisplayed()
        composeRule.onNodeWithTag("store_button").assertIsDisplayed()
        composeRule.onNodeWithTag("progression_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("pro_features_button").assertIsDisplayed()
        composeRule.onAllNodesWithTag("customize_match_button").assertCountEquals(0)
        composeRule.onAllNodesWithTag("match_setup_summary").assertCountEquals(0)
    }

    @Test
    fun launchScreen_localSaveStatusExplainsEmptyState() {
        setLaunchContent(savedGames = emptyList())

        composeRule.onNodeWithTag("local_save_status_card").assertIsDisplayed()
        composeRule.onNodeWithText("No saved match yet").assertIsDisplayed()
        composeRule.onNodeWithText("Start a match, then use Save Game in Settings.", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun launchScreen_localSaveStatusExplainsSavedState() {
        val save = sampleSavedGame(
            id = "local_status",
            name = "Local Weekend",
            savedAt = 123456789L
        )
        setLaunchContent(savedGames = listOf(save))

        composeRule.onNodeWithTag("local_save_status_card").assertIsDisplayed()
        composeRule.onNodeWithText("Local save ready").assertIsDisplayed()
        composeRule.onNodeWithText("1 saved match on this device. Latest: Local Weekend.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("load_saved_game_button").assertIsEnabled()
        composeRule.onNodeWithTag("resume_latest_saved_game_button").assertIsDisplayed()
    }

    @Test
    fun launchScreen_secondaryNavigationKeepsNonPlayFeaturesTogether() {
        setLaunchContent()

        composeRule.onNodeWithTag("launch_secondary_navigation").assertIsDisplayed()
        composeRule.onNodeWithTag("progression_button").assertIsDisplayed()
        composeRule.onNodeWithTag("store_button").assertIsDisplayed()
        composeRule.onNodeWithTag("pro_features_button").assertIsDisplayed()

        val storeDescription = composeRule.onNodeWithTag("store_button", useUnmergedTree = true)
            .fetchSemanticsNode("Store button missing")
            .config
            .getOrElseNullable(SemanticsProperties.ContentDescription) { null }
            .orEmpty()
            .joinToString(" ")
        assertTrue(storeDescription.contains("Unlocks"))
    }

    @Test
    fun newGameDialog_containsAllSetupOptionsAndStartsWithSelection() {
        var selectedDifficulty: GameDifficulty? = null
        setLaunchContent(onStart = { selectedDifficulty = it })

        composeRule.onNodeWithTag("new_game_button").performClick()

        composeRule.onNodeWithTag("new_game_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("new_game_mode_multiplayer_chip").assertIsDisplayed()
        composeRule.onNodeWithTag("new_game_players_2_chip").assertIsDisplayed()
        composeRule.onNodeWithTag("new_game_players_4_chip").performClick()
        composeRule.onNodeWithTag("new_game_difficulty_hard").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("new_game_difficulty_hard").performClick()
        composeRule.onNodeWithTag("match_mode_party_rules").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("match_mode_party_rules").performClick()
        composeRule.onNodeWithTag("board_layout_pro_chaos").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("board_layout_pro_chaos").performClick()
        composeRule.onNodeWithTag("advanced_setup_toggle").performScrollTo().performClick()
        composeRule.onNodeWithTag("rules_explanation_panel").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("match_setup_summary").assertIsDisplayed()
        composeRule.onNodeWithTag("new_game_start_button").performClick()

        assertEquals(GameDifficulty.HARD, selectedDifficulty)
    }

    @Test
    fun newGameDialog_stepStripJumpsBetweenSetupSections() {
        setLaunchContent()

        composeRule.onNodeWithTag("new_game_button").performClick()

        composeRule.onNodeWithTag("new_game_step_mode").assertIsDisplayed().performClick()
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("match_mode_picker").assertIsDisplayed()
        val modeStepState = composeRule.onNodeWithTag("new_game_step_mode", useUnmergedTree = true)
            .fetchSemanticsNode("Mode step missing")
            .config
            .getOrElseNullable(SemanticsProperties.StateDescription) { null }
        assertEquals("Current", modeStepState)

        composeRule.onNodeWithTag("new_game_step_board").performClick()
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("board_layout_picker").assertIsDisplayed()
        val boardStepState = composeRule.onNodeWithTag("new_game_step_board", useUnmergedTree = true)
            .fetchSemanticsNode("Board step missing")
            .config
            .getOrElseNullable(SemanticsProperties.StateDescription) { null }
        assertEquals("Current", boardStepState)

        composeRule.onNodeWithTag("new_game_step_review").performClick()
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("advanced_setup_toggle").assertIsDisplayed()
        val reviewStepState = composeRule.onNodeWithTag("new_game_step_review", useUnmergedTree = true)
            .fetchSemanticsNode("Review step missing")
            .config
            .getOrElseNullable(SemanticsProperties.StateDescription) { null }
        assertEquals("Current", reviewStepState)

        composeRule.onNodeWithTag("new_game_step_setup").performClick()
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("new_game_mode_multiplayer_chip").assertIsDisplayed()
        val setupStepState = composeRule.onNodeWithTag("new_game_step_setup", useUnmergedTree = true)
            .fetchSemanticsNode("Setup step missing")
            .config
            .getOrElseNullable(SemanticsProperties.StateDescription) { null }
        assertEquals("Current", setupStepState)
    }

    @Test
    fun newGameDialog_vsBotShowsBotPersonalitySelector() {
        setLaunchContent()

        composeRule.onNodeWithTag("new_game_button").performClick()
        composeRule.onNodeWithTag("new_game_mode_vs_bot_chip").performClick()

        composeRule.onNodeWithTag("new_game_bot_personality_steady").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("new_game_bot_personality_risky").assertIsDisplayed()
        composeRule.onNodeWithTag("new_game_bot_personality_defensive").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("new_game_bot_personality_pro").assertIsDisplayed()
        composeRule.onNodeWithTag("new_game_bot_personality_pro").performClick()
        composeRule.onNodeWithText("Pro").assertIsDisplayed()
        composeRule.onAllNodesWithTag("new_game_players_3_chip").assertCountEquals(0)
    }

    @Test
    fun newGameDialog_modeAndDifficultyExposeSegmentedSelectionState() {
        setLaunchContent()

        composeRule.onNodeWithTag("new_game_button").performClick()

        val localState = composeRule.onNodeWithTag("new_game_mode_multiplayer_chip", useUnmergedTree = true)
            .fetchSemanticsNode("Local mode segment missing")
            .config
            .getOrElseNullable(SemanticsProperties.StateDescription) { null }
        assertEquals("Selected", localState)

        composeRule.onNodeWithTag("new_game_mode_vs_bot_chip").performClick()
        val botState = composeRule.onNodeWithTag("new_game_mode_vs_bot_chip", useUnmergedTree = true)
            .fetchSemanticsNode("Bot mode segment missing")
            .config
            .getOrElseNullable(SemanticsProperties.StateDescription) { null }
        assertEquals("Selected", botState)

        composeRule.onNodeWithTag("new_game_difficulty_hard").performScrollTo().performClick()
        val hardState = composeRule.onNodeWithTag("new_game_difficulty_hard", useUnmergedTree = true)
            .fetchSemanticsNode("Hard difficulty segment missing")
            .config
            .getOrElseNullable(SemanticsProperties.StateDescription) { null }
        assertEquals("Selected", hardState)
    }

    @Test
    fun newGameDialog_allowsLocalPlayerNamesAndAvatarsBeforeStart() {
        setLaunchContent()

        composeRule.onNodeWithTag("new_game_button").performClick()

        composeRule.onNodeWithTag("player_setup_panel").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("player_setup_name_1").performTextReplacement("Asha")
        composeRule.onNodeWithTag("player_setup_avatar_1_cobra_token").performClick()
        composeRule.onNodeWithTag("match_setup_players_summary").assertTextEquals("Asha vs Player 2")

        composeRule.onNodeWithTag("new_game_players_3_chip").performScrollTo().performClick()
        composeRule.onNodeWithTag("player_setup_card_3").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("player_setup_name_3").performTextReplacement("Dev")
        composeRule.onNodeWithTag("player_setup_avatar_3_gold_die").performClick()

        composeRule.onNodeWithTag("match_setup_players_summary").assertTextEquals("Asha vs Player 2 vs Dev")
        val avatarState = composeRule.onNodeWithTag("player_setup_avatar_3_gold_die", useUnmergedTree = true)
            .fetchSemanticsNode("Player 3 Gold Die avatar option missing")
            .config
            .getOrElseNullable(SemanticsProperties.StateDescription) { null }
        assertEquals("Selected", avatarState)
    }

    @Test
    fun launchScreen_storeDialogShowsUnlockCatalog() {
        val fundedProfile = PlayerProfile(
            coins = 400,
            gems = 4,
            selectedAvatarId = "classic_token",
            unlockedAvatarIds = setOf("classic_token"),
            unlockedBoardIds = setOf(BoardLayouts.CLASSIC_ID)
        )
        setLaunchContent(playerProfile = fundedProfile)

        composeRule.onNodeWithTag("store_button").performScrollTo().performClick()

        composeRule.onNodeWithTag("store_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("store_balance_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("store_item_avatar_cobra_token").assertIsDisplayed()
        composeRule.onNodeWithTag("store_state_avatar_cobra_token").assertIsDisplayed()
        composeRule.onNodeWithTag("store_item_board_pro_chaos").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("store_hint_board_pro_chaos").assertIsDisplayed()
    }

    @Test
    fun storePurchaseConfirmationPanel_requiresConfirmBeforePurchase() {
        var confirmed = false
        var cancelled = false
        composeRule.setContent {
            SnakeLadderTheme {
                StorePurchaseConfirmationPanel(
                    item = StoreCatalog.byId("board_pro_chaos")!!,
                    onCancel = { cancelled = true },
                    onConfirm = { confirmed = true }
                )
            }
        }

        composeRule.onNodeWithTag("store_purchase_confirm_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Adds this board to the New Game board picker.").assertIsDisplayed()
        composeRule.onNodeWithTag("store_purchase_cancel_button").performClick()
        assertEquals(true, cancelled)
        assertEquals(false, confirmed)

        composeRule.onNodeWithTag("store_purchase_confirm_button").performClick()
        assertEquals(true, confirmed)
    }

    @Test
    fun storeDialog_highlightsEquippedStateImmediately() {
        val profile = PlayerProfile(
            coins = 280,
            gems = 4,
            selectedAvatarId = "cobra_token",
            unlockedAvatarIds = setOf("classic_token", "cobra_token"),
            unlockedBoardIds = setOf(BoardLayouts.CLASSIC_ID)
        )
        composeRule.setContent {
            SnakeLadderTheme {
                StoreDialog(
                    profile = profile,
                    onPurchaseItem = { "Store unavailable." },
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("280 coins | 4 gems").assertIsDisplayed()
        composeRule.onNodeWithTag("store_state_avatar_cobra_token").assertIsDisplayed()
        composeRule.onAllNodesWithText("Equipped").assertCountEquals(2)
        composeRule.onNodeWithText("Currently equipped").assertIsDisplayed()
    }

    @Test
    fun launchScreen_proFeatureHubListsFiftyNewFeatures() {
        setLaunchContent()

        composeRule.onNodeWithTag("pro_features_button").performScrollTo().performClick()

        composeRule.onNodeWithTag("pro_feature_hub_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("pro_feature_count").assertIsDisplayed()
        composeRule.onNodeWithTag("backend_features_hidden_notice").assertIsDisplayed()
        composeRule.onAllNodesWithTag("pro_feature_online_multiplayer").assertCountEquals(0)
        composeRule.onNodeWithTag("pro_feature_post_match_analytics").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Post-Match Insights", substring = true).assertIsDisplayed()
        composeRule.onAllNodesWithText("Post-Match Analytics", substring = true).assertCountEquals(0)
        composeRule.onNodeWithTag("pro_feature_daily_challenges").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun launchScreen_progressionDialogShowsProfileAndDailyChallenge() {
        setLaunchContent()

        composeRule.onNodeWithTag("progression_button").performScrollTo().performClick()

        composeRule.onNodeWithTag("progression_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_stats_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_challenge_card").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_tab_achievements").performClick()
        composeRule.onNodeWithTag("achievement_list").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_tab_rewards").performClick()
        composeRule.onNodeWithTag("profile_tools_panel").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun progressionStatsSeparateStartedAndCompletedMatches() {
        composeRule.setContent {
            SnakeLadderTheme {
                ProgressionDialog(
                    profile = PlayerProfile(matchesStarted = 5, matchesCompleted = 2, humanWins = 1),
                    dailyChallenge = DailyChallengeCatalog.today(),
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithTag("profile_stats_panel").assertIsDisplayed()
        composeRule.onNodeWithText("Started").assertIsDisplayed()
        composeRule.onNodeWithText("Completed").assertIsDisplayed()
    }

    @Test
    fun progressionRewardsTabPreviewsLockedAvatarsAndEquipsOwnedItems() {
        var equippedItemId = ""
        var sharedExport = ""
        val profile = PlayerProfile(
            coins = 240,
            gems = 2,
            selectedAvatarId = "classic_token",
            selectedTitle = "New Challenger",
            unlockedAvatarIds = setOf("classic_token", "cobra_token"),
            unlockedTitleIds = setOf("title_new_challenger", "title_snake_tamer")
        )
        composeRule.setContent {
            SnakeLadderTheme {
                ProgressionDialog(
                    profile = profile,
                    dailyChallenge = DailyChallengeCatalog.today(),
                    onExportProfile = { "profile-json" },
                    onShareProfile = { raw ->
                        sharedExport = raw
                        true
                    },
                    onEquipProfileItem = { itemId ->
                        equippedItemId = itemId
                        "Equipped from Progress."
                    },
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithTag("profile_tab_rewards").performClick()
        composeRule.onNodeWithTag("profile_customization_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_preview_avatar_ladder_king").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Locked preview - unlock in Store for 180 coins.").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_equip_avatar_cobra_token").performScrollTo().assertIsEnabled()
        composeRule.onNodeWithTag("profile_equip_avatar_cobra_token").performClick()
        assertEquals("avatar_cobra_token", equippedItemId)

        composeRule.onNodeWithTag("profile_equip_title_snake_tamer").performScrollTo().assertIsEnabled()
        composeRule.onNodeWithTag("profile_equip_title_snake_tamer").performClick()
        assertEquals("title_snake_tamer", equippedItemId)
        composeRule.onNodeWithTag("profile_equip_status").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("profile_share_export_button").performScrollTo().performClick()
        assertEquals("profile-json", sharedExport)
        composeRule.onNodeWithText("Profile export shared.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun launchProgressionRewardsForwardsEquipProfileItem() {
        var equippedItemId = ""
        var sharedExport = ""
        val profile = PlayerProfile(
            selectedAvatarId = "classic_token",
            unlockedAvatarIds = setOf("classic_token", "cobra_token")
        )
        setLaunchContent(
            playerProfile = profile,
            onEquipProfileItem = { itemId ->
                equippedItemId = itemId
                "Equipped from launcher."
            },
            onExportProfile = { "launch-profile-json" },
            onShareProfile = { raw ->
                sharedExport = raw
                true
            }
        )

        composeRule.onNodeWithTag("progression_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("profile_tab_rewards").performClick()
        composeRule.onNodeWithTag("profile_equip_avatar_cobra_token").performScrollTo().performClick()

        assertEquals("avatar_cobra_token", equippedItemId)
        composeRule.onNodeWithText("Equipped from launcher.").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("profile_share_export_button").performScrollTo().performClick()
        assertEquals("launch-profile-json", sharedExport)
        composeRule.onNodeWithText("Profile export shared.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun launchScreen_campaignDialogShowsQuestNodes() {
        setLaunchContent()

        composeRule.onNodeWithTag("campaign_button").performClick()

        composeRule.onNodeWithTag("campaign_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("campaign_map_progress").assertIsDisplayed()
        composeRule.onNodeWithTag("campaign_filter_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("campaign_node_classic_start").assertIsDisplayed()
        composeRule.onNodeWithText("Quest Map").assertIsDisplayed()
    }

    @Test
    fun campaignDialogFiltersAvailableAndBossNodes() {
        composeRule.setContent {
            SnakeLadderTheme {
                CampaignDialog(
                    profile = PlayerProfile(
                        humanWins = 2,
                        completedCampaignNodeIds = setOf("classic_start")
                    ),
                    onStartNode = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithTag("campaign_filter_available").performClick()

        composeRule.onNodeWithTag("campaign_filter_count").assertTextEquals("2 available")
        composeRule.onAllNodesWithTag("campaign_node_ladder_sprint").assertCountEquals(1)
        composeRule.onAllNodesWithTag("campaign_node_snake_survival").assertCountEquals(1)
        composeRule.onAllNodesWithTag("campaign_node_classic_start").assertCountEquals(0)
        composeRule.onAllNodesWithTag("campaign_node_party_trial").assertCountEquals(0)

        composeRule.onNodeWithTag("campaign_filter_bosses").performClick()

        composeRule.onNodeWithTag("campaign_filter_count").assertTextEquals("2 bosses | 2 available")
        composeRule.onAllNodesWithTag("campaign_node_boss_ladder_king").assertCountEquals(1)
        composeRule.onAllNodesWithTag("campaign_node_boss_cobra_guard").assertCountEquals(1)
        composeRule.onAllNodesWithTag("campaign_node_ladder_sprint").assertCountEquals(0)
    }

    @Test
    fun launchScreen_componentsVisibleInLandscape() {
        setLaunchContent(orientation = Configuration.ORIENTATION_LANDSCAPE)
        composeRule.onNodeWithTag("launch_wide_layout").assertIsDisplayed()
        composeRule.onNodeWithTag("launch_overview_rail").assertIsDisplayed()
        composeRule.onNodeWithTag("launch_overview_mode").assertIsDisplayed()
        composeRule.onNodeWithTag("new_game_button").assertIsDisplayed()
        composeRule.onNodeWithTag("daily_challenge_start_button").assertIsDisplayed()
        composeRule.onNodeWithTag("load_saved_game_button").assertIsDisplayed()
    }

    @Test
    fun launchScreen_tabletWidthUsesAdaptiveRailWithSavedState() {
        val save = sampleSavedGame(
            id = "tablet_save",
            name = "Tablet Save",
            savedAt = 123456789L
        )
        setLaunchContent(
            orientation = Configuration.ORIENTATION_PORTRAIT,
            screenWidthDp = 900,
            screenHeightDp = 1000,
            savedGames = listOf(save)
        )

        composeRule.onNodeWithTag("launch_wide_layout").assertIsDisplayed()
        composeRule.onNodeWithTag("launch_overview_rail").assertIsDisplayed()
        composeRule.onNodeWithTag("launch_overview_saved").assertIsDisplayed()
        composeRule.onNodeWithText("Local save ready").assertIsDisplayed()
        composeRule.onNodeWithTag("resume_latest_saved_game_button").assertIsDisplayed()
    }

    @Test
    fun gameSheetDialog_hasLandscapeSafeCloseButton() {
        var dismissed = false
        composeRule.setContent {
            SnakeLadderTheme {
                OrientationContainer(orientation = Configuration.ORIENTATION_LANDSCAPE) {
                    GameSheetDialog(
                        testTag = "sample_sheet_dialog",
                        title = "Sample Sheet",
                        subtitle = "Landscape shell check",
                        onDismiss = { dismissed = true }
                    ) {
                        Text("Sheet content")
                    }
                }
            }
        }

        composeRule.onNodeWithTag("sample_sheet_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("sample_sheet_dialog_close_button").assertIsDisplayed().performClick()
        assertEquals(true, dismissed)
    }

    @Test
    fun launchScreen_loadSavedGameDisabledWhenNoSaves() {
        setLaunchContent(savedGames = emptyList())
        composeRule.onNodeWithTag("load_saved_game_button").assertIsNotEnabled()

        val loadDescription = composeRule.onNodeWithTag("load_saved_game_button", useUnmergedTree = true)
            .fetchSemanticsNode("Load saved button missing")
            .config
            .getOrElseNullable(SemanticsProperties.ContentDescription) { null }
            .orEmpty()
            .joinToString(" ")
        assertTrue(loadDescription.contains("Save a match from the in-game settings menu"))
    }

    @Test
    fun launchScreen_loadSavedGameSelectInvokesCallback() {
        val save = SavedGameSnapshot(
            id = "s1",
            name = "My Saved Match",
            savedAt = 123456789L,
            boardTheme = BoardThemeOption.VIBRANT,
            state = sampleGameState()
        )
        var loadedId: String? = null
        setLaunchContent(
            savedGames = listOf(save),
            onLoadSavedGame = { loadedId = it.id }
        )

        composeRule.onNodeWithTag("load_saved_game_button").performScrollTo().assertIsEnabled()
        composeRule.onNodeWithTag("load_saved_game_button").performClick()
        composeRule.onNodeWithTag("saved_game_row_s1").performClick()
        assertEquals("s1", loadedId)
    }

    @Test
    fun launchScreen_savedGameSwipeDeleteInvokesCallback() {
        val save = SavedGameSnapshot(
            id = "swipe_delete",
            name = "Swipe Delete Match",
            savedAt = 123456789L,
            boardTheme = BoardThemeOption.VIBRANT,
            state = sampleGameState()
        )
        var deletedId: String? = null
        setLaunchContent(
            savedGames = listOf(save),
            onDeleteSavedGame = { deletedId = it.id }
        )

        composeRule.onNodeWithTag("load_saved_game_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("saved_game_row_swipe_delete").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("saved_game_delete_swipe_delete").performClick()
        composeRule.onNodeWithTag("saved_game_delete_confirm_button").performClick()

        assertEquals("swipe_delete", deletedId)
    }

    @Test
    fun launchScreen_tappingRevealedSavedGameHidesActionsBeforeLoading() {
        val save = SavedGameSnapshot(
            id = "swipe_load",
            name = "Swipe Load Match",
            savedAt = 123456789L,
            boardTheme = BoardThemeOption.VIBRANT,
            state = sampleGameState()
        )
        var loadedId: String? = null
        setLaunchContent(
            savedGames = listOf(save),
            onLoadSavedGame = { loadedId = it.id }
        )

        composeRule.onNodeWithTag("load_saved_game_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("saved_game_row_swipe_load").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("saved_game_resume_swipe_load").performClick()
        assertEquals("swipe_load", loadedId)
    }

    @Test
    fun launchScreen_loadSavedGameShowsAllSavedModes() {
        val multiplayerSave = SavedGameSnapshot(
            id = "multiplayer",
            name = "Multiplayer Save",
            savedAt = 123456789L,
            boardTheme = BoardThemeOption.VIBRANT,
            state = sampleGameState(gameMode = GameMode.LOCAL_MULTIPLAYER)
        )
        val botSave = SavedGameSnapshot(
            id = "bot",
            name = "Bot Save",
            savedAt = 123456799L,
            boardTheme = BoardThemeOption.VIBRANT,
            state = sampleGameState(gameMode = GameMode.VS_BOT, botPlayerIndex = 1)
        )
        setLaunchContent(savedGames = listOf(multiplayerSave, botSave))

        composeRule.onNodeWithTag("load_saved_game_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("saved_game_row_multiplayer").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("saved_game_row_bot").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun launchScreen_savedGameSearchOnlyAppearsForLargerLists() {
        setLaunchContent(
            savedGames = listOf(
                sampleSavedGame(id = "one", name = "One Save"),
                sampleSavedGame(id = "two", name = "Two Save"),
                sampleSavedGame(id = "three", name = "Three Save")
            )
        )

        composeRule.onNodeWithTag("load_saved_game_button").performScrollTo().performClick()
        composeRule.onAllNodesWithTag("saved_game_search_input").assertCountEquals(0)
    }

    @Test
    fun launchScreen_savedGameSearchFiltersLargeListsAndShowsEmptyState() {
        setLaunchContent(
            savedGames = listOf(
                sampleSavedGame(id = "family", name = "Family Night", savedAt = 4_000L),
                sampleSavedGame(
                    id = "bot",
                    name = "Bot Arena",
                    savedAt = 3_000L,
                    gameMode = GameMode.VS_BOT,
                    botPlayerIndex = 1,
                    boardLayoutId = BoardLayouts.TRAP_VALLEY_ID,
                    matchMode = MatchModePreset.PARTY_RULES,
                    difficulty = GameDifficulty.HARD
                ),
                sampleSavedGame(
                    id = "speed",
                    name = "Speed Run",
                    savedAt = 2_000L,
                    boardLayoutId = BoardLayouts.SPEED_RUN_ID,
                    matchMode = MatchModePreset.TIME_ATTACK
                ),
                sampleSavedGame(
                    id = "team",
                    name = "Team League",
                    savedAt = 1_000L,
                    boardLayoutId = BoardLayouts.LADDER_LEAGUE_ID,
                    matchMode = MatchModePreset.TWO_V_TWO
                )
            )
        )

        composeRule.onNodeWithTag("load_saved_game_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("saved_game_search_input").assertIsDisplayed()
        composeRule.onNodeWithTag("saved_game_search_input").performTextInput("bot")
        composeRule.onNodeWithTag("saved_game_row_bot").assertIsDisplayed()
        composeRule.onAllNodesWithTag("saved_game_row_family").assertCountEquals(0)
        composeRule.onAllNodesWithTag("saved_game_row_speed").assertCountEquals(0)

        composeRule.onNodeWithTag("saved_game_search_input").performTextClearance()
        composeRule.onNodeWithTag("saved_game_search_input").performTextInput("zzz")
        composeRule.onNodeWithTag("saved_game_search_empty").assertIsDisplayed()
    }

    @Test
    fun launchScreen_loadSavedGameEnabledWhenAnyModeHasSaves() {
        val botSave = SavedGameSnapshot(
            id = "bot",
            name = "Bot Save",
            savedAt = 123456799L,
            boardTheme = BoardThemeOption.VIBRANT,
            state = sampleGameState(gameMode = GameMode.VS_BOT, botPlayerIndex = 1)
        )
        setLaunchContent(savedGames = listOf(botSave))

        composeRule.onNodeWithTag("load_saved_game_button").assertIsEnabled()
    }

    @Test
    fun boardScreen_showsTurnModeDifficultyAndPlayerPositions() {
        setBoardContent(Configuration.ORIENTATION_PORTRAIT)
        composeRule.onNodeWithTag("settings_button").assertIsDisplayed()
        composeRule.onNodeWithTag("dice_badge").assertIsEnabled()
        composeRule.onNodeWithTag("turn_status_label").assertIsDisplayed()
        composeRule.onNodeWithTag("mode_difficulty_label").assertIsDisplayed()
        composeRule.onNodeWithTag("difficulty_rule_summary").assertIsDisplayed()
        composeRule.onNodeWithTag("player_position_strip").assertIsDisplayed()
        composeRule.onNodeWithTag("move_status_panel").assertIsDisplayed()
        composeRule.onAllNodesWithTag("quick_chat_toggle").assertCountEquals(0)
        composeRule.onAllNodesWithTag("quick_chat_panel").assertCountEquals(0)
    }

    @Test
    fun boardScreen_partyRulesShowsPowerUpPanel() {
        val state = sampleGameState().copy(
            matchMode = MatchModePreset.PARTY_RULES,
            ruleSetId = RuleSets.PARTY_ID,
            powerUpInventories = listOf(listOf(PowerUpType.TRAP), emptyList()),
            activeTraps = listOf(BoardTrap(cell = 12, ownerPlayerIndex = 0)),
            matchEvents = listOf(
                MatchEvent(
                    turnNumber = 1,
                    playerIndex = 0,
                    playerName = "Player 1",
                    dice = 0,
                    startPosition = 1,
                    landedPosition = 1,
                    finalPosition = 12,
                    moveType = MoveType.POWER_UP,
                    path = emptyList(),
                    powerUpUsed = PowerUpType.TRAP
                )
            )
        )

        setBoardContent(
            orientation = Configuration.ORIENTATION_PORTRAIT,
            state = state
        )

        composeRule.onNodeWithTag("power_up_inventory_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("power_up_trap").assertIsDisplayed()
        composeRule.onNodeWithTag("power_up_status").assertIsDisplayed()
        composeRule.onNodeWithTag("power_up_feedback_banner").assertIsDisplayed()
    }

    @Test
    fun boardScreen_armedPowerUpStaysVisibleAndCanBeCanceled() {
        var canceledPowerUp: PowerUpType? = null
        val state = sampleGameState().copy(
            matchMode = MatchModePreset.PARTY_RULES,
            ruleSetId = RuleSets.PARTY_ID,
            powerUpInventories = listOf(listOf(PowerUpType.REROLL, PowerUpType.TRAP), emptyList()),
            armedPowerUps = listOf(PlayerArmedPowerUp(0, PowerUpType.SHIELD))
        )

        setBoardContent(
            orientation = Configuration.ORIENTATION_PORTRAIT,
            state = state,
            onCancelArmedPowerUp = { canceledPowerUp = it }
        )

        composeRule.onNodeWithTag("power_up_inventory_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("power_up_shield").assertIsDisplayed()
        composeRule.onNodeWithTag("power_up_reroll").assertIsDisplayed()
        composeRule.onNodeWithText("Queued").assertIsDisplayed()
        composeRule.onNodeWithText("Shield armed. Tap to cancel and return it to hand.").assertIsDisplayed()
        val shieldBounds = composeRule.onNodeWithTag("power_up_shield").fetchSemanticsNode().boundsInRoot
        val rerollBounds = composeRule.onNodeWithTag("power_up_reroll").fetchSemanticsNode().boundsInRoot
        assertTrue("Queued power-up should be shown before remaining hand items.", shieldBounds.left < rerollBounds.left)
        composeRule.onNodeWithTag("power_up_shield").performClick()
        composeRule.runOnIdle {
            assertEquals(PowerUpType.SHIELD, canceledPowerUp)
        }
    }

    @Test
    fun boardScreen_powerUpsUseStableOrderAfterReturnedToHand() {
        val state = sampleGameState().copy(
            matchMode = MatchModePreset.PARTY_RULES,
            ruleSetId = RuleSets.PARTY_ID,
            powerUpInventories = listOf(listOf(PowerUpType.REROLL, PowerUpType.TRAP, PowerUpType.SHIELD), emptyList()),
            armedPowerUps = emptyList()
        )

        setBoardContent(
            orientation = Configuration.ORIENTATION_PORTRAIT,
            state = state
        )

        composeRule.onNodeWithTag("power_up_inventory_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("power_up_shield").assertIsDisplayed()
        composeRule.onNodeWithTag("power_up_reroll").assertIsDisplayed()
        val shieldBounds = composeRule.onNodeWithTag("power_up_shield").fetchSemanticsNode().boundsInRoot
        val rerollBounds = composeRule.onNodeWithTag("power_up_reroll").fetchSemanticsNode().boundsInRoot
        assertTrue("Shield should keep its canonical first slot after returning to hand.", shieldBounds.left < rerollBounds.left)
    }

    @Test
    fun boardScreen_tacticalCardsShowsInventoryAndTimingHint() {
        val state = sampleGameState().copy(
            matchMode = MatchModePreset.TACTICAL_CARDS,
            ruleSetId = RuleSets.TACTICAL_CARDS_ID,
            powerUpInventories = listOf(listOf(PowerUpType.REROLL, PowerUpType.SHIELD), emptyList()),
            matchEvents = listOf(
                MatchEvent(
                    turnNumber = 1,
                    playerIndex = 0,
                    playerName = "Player 1",
                    dice = 0,
                    startPosition = 1,
                    landedPosition = 1,
                    finalPosition = 1,
                    moveType = MoveType.POWER_UP,
                    path = emptyList(),
                    powerUpUsed = PowerUpType.REROLL
                )
            )
        )

        setBoardContent(
            orientation = Configuration.ORIENTATION_PORTRAIT,
            state = state
        )

        composeRule.onNodeWithTag("power_up_inventory_panel").assertIsDisplayed()
        composeRule.onNodeWithTag("power_up_panel_title").assertTextEquals("Cards")
        composeRule.onNodeWithTag("card_timing_hint").assertTextEquals(
            "Card timing: play before rolling; armed cards resolve on the next roll."
        )
        composeRule.onNodeWithText("Card: play before rolling to queue one extra roll.").assertIsDisplayed()
        composeRule.onNodeWithTag("power_up_recent_log_title").assertTextEquals("Recent cards")
    }

    @Test
    fun boardScreen_botPowerUpFeedbackExplainsChoiceReason() {
        val state = sampleGameState(
            gameMode = GameMode.VS_BOT,
            currentPlayerIndex = 0,
            botPlayerIndex = 1
        ).copy(
            matchMode = MatchModePreset.PARTY_RULES,
            ruleSetId = RuleSets.PARTY_ID,
            powerUpInventories = listOf(listOf(PowerUpType.SHIELD), emptyList()),
            matchEvents = listOf(
                MatchEvent(
                    turnNumber = 2,
                    playerIndex = 1,
                    playerName = "Rival Bot",
                    dice = 0,
                    startPosition = 44,
                    landedPosition = 44,
                    finalPosition = 44,
                    moveType = MoveType.POWER_UP,
                    path = emptyList(),
                    powerUpUsed = PowerUpType.DICE_BOOST
                )
            )
        )

        setBoardContent(
            orientation = Configuration.ORIENTATION_PORTRAIT,
            state = state
        )

        composeRule.onNodeWithTag("power_up_feedback_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("power_up_bot_reason").assertTextEquals(
            "Bot reason: pushing closer to finish or a ladder lane."
        )
        composeRule.onNodeWithTag("power_up_recent_log").assertIsDisplayed()
    }

    @Test
    fun boardScreen_dailyMatchShowsLiveDailyChallengeProgress() {
        val challenge = DailyChallenge(
            id = "daily_ladders",
            dateKey = "20260606",
            title = "Find The Ladders",
            description = "Climb two ladders across completed matches today.",
            target = 2,
            kind = DailyChallengeKind.CLIMB_LADDERS
        )
        val state = sampleGameState().copy(
            matchMode = MatchModePreset.DAILY_CHALLENGE,
            dailyChallengeId = challenge.id,
            matchEvents = listOf(
                MatchEvent(
                    turnNumber = 1,
                    playerIndex = 0,
                    playerName = "Player 1",
                    dice = 6,
                    startPosition = 1,
                    landedPosition = 7,
                    finalPosition = 14,
                    moveType = MoveType.LADDER,
                    path = listOf(2, 3, 4, 5, 6, 7, 14)
                )
            )
        )

        setBoardContent(
            orientation = Configuration.ORIENTATION_PORTRAIT,
            state = state,
            dailyChallenge = challenge,
            playerProfile = PlayerProfile(
                dailyChallengeId = challenge.id,
                dailyChallengeDateKey = challenge.dateKey
            )
        )

        composeRule.onNodeWithTag("in_match_daily_challenge").assertIsDisplayed()
        composeRule.onNodeWithTag("in_match_daily_title").assertIsDisplayed()
        composeRule.onNodeWithTag("in_match_daily_progress").assertTextEquals("1/2")
        composeRule.onNodeWithTag("in_match_daily_progress_bar").assertIsDisplayed()
    }

    @Test
    fun boardScreen_bonusTurnShowsRolledSixReason() {
        val state = sampleGameState().copy(
            bonusTurnGranted = true,
            lastDiceRoll = 6,
            statusMessage = "Player 1 rolled 6. Bonus turn (rolled 6)",
            matchEvents = listOf(
                MatchEvent(
                    turnNumber = 1,
                    playerIndex = 0,
                    playerName = "Player 1",
                    dice = 6,
                    startPosition = 1,
                    landedPosition = 7,
                    finalPosition = 7,
                    moveType = MoveType.NORMAL,
                    path = listOf(2, 3, 4, 5, 6, 7),
                    bonusTurn = true
                )
            )
        )

        setBoardContent(Configuration.ORIENTATION_PORTRAIT, state = state)

        composeRule.onNodeWithTag("bonus_turn_badge").assertTextEquals("Bonus: Six")
        composeRule.onNodeWithTag("dice_action_hint", useUnmergedTree = true).assertTextEquals(
            "Bonus turn: rolled a six. Tap the dice panel to roll again."
        )
    }

    @Test
    fun boardScreen_bonusTurnShowsLadderReason() {
        val state = sampleGameState().copy(
            bonusTurnGranted = true,
            lastDiceRoll = 1,
            statusMessage = "Player 1 rolled 1. Climbed ladder: 2 -> 38. Bonus turn (climbed ladder)",
            matchEvents = listOf(
                MatchEvent(
                    turnNumber = 1,
                    playerIndex = 0,
                    playerName = "Player 1",
                    dice = 1,
                    startPosition = 1,
                    landedPosition = 2,
                    finalPosition = 38,
                    moveType = MoveType.LADDER,
                    path = listOf(2, 38),
                    bonusTurn = true
                )
            )
        )

        setBoardContent(Configuration.ORIENTATION_PORTRAIT, state = state)

        composeRule.onNodeWithTag("bonus_turn_badge").assertTextEquals("Bonus: Ladder")
        composeRule.onNodeWithTag("dice_action_hint", useUnmergedTree = true).assertTextEquals(
            "Bonus turn: climbed a ladder. Tap the dice panel to roll again."
        )
    }

    @Test
    fun boardScreen_controlsVisibleInLandscape() {
        setBoardContent(Configuration.ORIENTATION_LANDSCAPE)
        composeRule.onNodeWithTag("settings_button").assertIsDisplayed()
        composeRule.onNodeWithTag("dice_badge").assertIsDisplayed()
    }

    @Test
    fun boardScreen_controlsVisibleInPortrait() {
        setBoardContent(Configuration.ORIENTATION_PORTRAIT)
        composeRule.onNodeWithTag("settings_button").assertIsDisplayed()
        composeRule.onNodeWithTag("dice_badge").assertIsDisplayed()
    }

    @Test
    fun boardScreen_boardViewportControlsZoomAndCenterCurrentTurn() {
        val state = sampleGameState(currentPlayerIndex = 1).copy(
            players = listOf(
                PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 1),
                PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 84)
            ),
            statusMessage = "Player 2 lines up a ladder route."
        )
        setBoardContent(Configuration.ORIENTATION_PORTRAIT, state = state)

        composeRule.onNodeWithTag("board_zoom_controls").assertIsDisplayed()
        composeRule.onNodeWithTag("board_zoom_status").assertTextEquals("1.0x")
        composeRule.onNodeWithTag("board_zoom_in_button").performClick()
        composeRule.onNodeWithTag("board_zoom_status").assertTextEquals("1.2x")
        composeRule.onNodeWithTag("board_center_turn_button").performClick()
        composeRule.onNodeWithTag("board_zoom_status").assertTextEquals("1.4x")
        composeRule.onNodeWithTag("board_focus_label", useUnmergedTree = true)
            .assertTextEquals("Player 2 centered on cell 84")
        composeRule.onNodeWithTag("board_zoom_out_button").performClick()
        composeRule.onNodeWithTag("board_zoom_status").assertTextEquals("1.2x")
    }

    @Test
    fun boardScreen_timeAttackTimerUsesDedicatedBadgeAwayFromDice() {
        val state = sampleGameState().copy(
            matchMode = MatchModePreset.TIME_ATTACK,
            turnsRemaining = 4,
            turnLimit = 12,
            statusMessage = "Beat the clock before the turn limit expires."
        )
        setBoardContent(Configuration.ORIENTATION_PORTRAIT, state = state)

        composeRule.onNodeWithTag("match_mode_state_strip").assertIsDisplayed()
        composeRule.onNodeWithTag("match_timer_badge").assertTextEquals("Turns left 4")
        composeRule.onNodeWithTag("dice_badge").assertIsDisplayed()
    }

    @Test
    fun boardScreen_primaryControlsHaveAccessibilityLabels() {
        setBoardContent(Configuration.ORIENTATION_PORTRAIT)
        composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Roll Dice").assertIsDisplayed()
    }

    @Test
    fun boardScreen_winnerOverlayHidesSettingsShortcut() {
        setBoardContent(
            orientation = Configuration.ORIENTATION_PORTRAIT,
            state = sampleGameState(winnerIndex = 0)
        )
        composeRule.onAllNodesWithTag("settings_button").assertCountEquals(0)
        composeRule.onNodeWithTag("post_match_analytics").assertIsDisplayed()
        composeRule.onNodeWithTag("post_match_momentum").assertIsDisplayed()
        composeRule.onNodeWithTag("post_match_next_objective").assertIsDisplayed()
        composeRule.onNodeWithTag("winner_replay_button").assertIsDisplayed()
        composeRule.onNodeWithTag("winner_replay_button").performClick()
        composeRule.onNodeWithTag("replay_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("replay_event_card").assertIsDisplayed()
    }

    @Test
    fun replayDialog_showsMoveNumbersAndScrubControls() {
        val events = listOf(
            MatchEvent(
                turnNumber = 1,
                playerIndex = 0,
                playerName = "Player 1",
                dice = 3,
                startPosition = 1,
                landedPosition = 4,
                finalPosition = 4,
                moveType = MoveType.NORMAL,
                path = listOf(2, 3, 4)
            ),
            MatchEvent(
                turnNumber = 2,
                playerIndex = 1,
                playerName = "Player 2",
                dice = 5,
                startPosition = 1,
                landedPosition = 6,
                finalPosition = 14,
                moveType = MoveType.LADDER,
                path = listOf(2, 3, 4, 5, 6, 14)
            )
        )

        composeRule.setContent {
            SnakeLadderTheme {
                ReplayDialog(events = events, onDismiss = {})
            }
        }

        composeRule.onNodeWithTag("replay_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("replay_move_counter").assertIsDisplayed()
        composeRule.onNodeWithText("Move 1 / 2 | Turn 1").assertIsDisplayed()
        composeRule.onNodeWithTag("replay_scrubber").assertIsDisplayed()
        composeRule.onNodeWithText("Move 1: Turn 1 | Player 1 1 -> 4").assertIsDisplayed()
        composeRule.onNodeWithTag("replay_move_row_2").performClick()
        composeRule.onNodeWithText("Move 2 / 2 | Turn 2").assertIsDisplayed()
        composeRule.onNodeWithText("Player 2 rolled 5").assertIsDisplayed()
    }

    @Test
    fun boardScreen_botTurnDisablesDiceInteraction() {
        resetBoardSettings()
        setBoardContent(
            orientation = Configuration.ORIENTATION_PORTRAIT,
            state = sampleGameState(
                gameMode = GameMode.VS_BOT,
                currentPlayerIndex = 1,
                botPlayerIndex = 1
            )
        )
        composeRule.onNodeWithTag("dice_badge").assertIsNotEnabled()
        composeRule.onNodeWithTag("dice_action_hint", useUnmergedTree = true).assertTextEquals(
            "Bot thinking: Steady rolls standard."
        )
    }

    @Test
    fun boardScreen_manualBotConfirmationEnablesBotRollSurface() {
        resetBoardSettings()
        setBoardContent(
            orientation = Configuration.ORIENTATION_PORTRAIT,
            state = sampleGameState(
                gameMode = GameMode.VS_BOT,
                currentPlayerIndex = 1,
                botPlayerIndex = 1
            )
        )

        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.onNodeWithTag("settings_tab_controls").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings_manual_bot_roll_switch").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings_done_button").performClick()

        composeRule.onNodeWithTag("turn_status_label").assertTextEquals("Bot ready")
        composeRule.onNodeWithTag("dice_badge").assertIsEnabled()
        composeRule.onNodeWithTag("dice_action_hint", useUnmergedTree = true).assertTextEquals(
            "Bot ready: tap Roll to confirm steady bot."
        )
    }

    @Test
    fun boardScreen_muteShortcutTogglesAudioState() {
        resetBoardSettings()
        setBoardContent(Configuration.ORIENTATION_PORTRAIT)

        composeRule.onNodeWithTag("mute_shortcut_button").assertTextEquals("Mute")
        composeRule.onNodeWithTag("mute_shortcut_button").performClick()
        composeRule.onNodeWithTag("mute_shortcut_button").assertTextEquals("Unmute")
    }

    @Test
    fun boardScreen_overshootShowsExactFinishWarning() {
        val state = sampleGameState(currentPlayerIndex = 1).copy(
            players = listOf(
                PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 97),
                PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 12)
            ),
            lastDiceRoll = 6,
            statusMessage = "Player 1 rolled 6. Exact finish missed: rolled 6, needed 3 to reach 100",
            moveHistory = listOf("Player 1 rolled 6. Exact finish missed: rolled 6, needed 3 to reach 100"),
            lastMovePlayerIndex = 0,
            lastMoveType = MoveType.OVERSHOOT,
            moveSignal = 1
        )

        setBoardContent(Configuration.ORIENTATION_PORTRAIT, state = state)

        composeRule.onNodeWithTag("overshoot_warning").assertTextEquals(
            "Overshot finish: Player 1 stays on 97; 3 still needed."
        )
    }

    @Test
    fun boardScreen_playerCardTapHighlightsTokenCell() {
        val state = sampleGameState().copy(
            players = listOf(
                PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 38),
                PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 12)
            )
        )

        setBoardContent(Configuration.ORIENTATION_PORTRAIT, state = state)

        composeRule.onNodeWithTag("player_position_1").performClick()
        composeRule.onNodeWithTag("board_focus_label").assertTextEquals("Player 1 is on cell 38")
    }

    @Test
    fun boardScreen_routeEndpointTapsShowSnakeAndLadderDetails() {
        setBoardContent(Configuration.ORIENTATION_PORTRAIT)

        composeRule.onNodeWithTag("ladder_endpoint_2_38_start").performClick()
        composeRule.onNodeWithTag("board_focus_label").assertTextEquals("Ladder 2 climbs to 38")

        composeRule.onNodeWithTag("snake_endpoint_99_54_head").performClick()
        composeRule.onNodeWithTag("board_focus_label").assertTextEquals("Snake 99 slides to 54")
    }

    @Test
    fun boardScreen_accessibilityHubStartsWithTurnAndOffersActions() {
        val state = sampleGameState().copy(
            matchMode = MatchModePreset.PARTY_RULES,
            ruleSetId = RuleSets.PARTY_ID,
            powerUpInventories = listOf(listOf(PowerUpType.TRAP), emptyList())
        )

        setBoardContent(
            orientation = Configuration.ORIENTATION_PORTRAIT,
            state = state
        )

        val hub = composeRule.onNodeWithTag("board_accessibility_hub", useUnmergedTree = true)
            .fetchSemanticsNode("Accessibility hub missing")
        val description = hub.config
            .getOrElseNullable(SemanticsProperties.ContentDescription) { null }
            .orEmpty()
            .joinToString(" ")
        assertTrue(description.startsWith("Current turn: Player 1's turn."))

        val actionLabels = hub.config
            .getOrElseNullable(SemanticsActions.CustomActions) { null }
            .orEmpty()
            .map { it.label }
        assertTrue(actionLabels.contains("Roll dice"))
        assertTrue(actionLabels.contains("Open settings"))
        assertTrue(actionLabels.contains("Use Trap power-up"))
    }

    @Test
    fun boardScreen_disabledDiceExplainsUnavailableReason() {
        resetBoardSettings()
        setBoardContent(
            orientation = Configuration.ORIENTATION_PORTRAIT,
            state = sampleGameState(
                gameMode = GameMode.VS_BOT,
                currentPlayerIndex = 1,
                botPlayerIndex = 1
            )
        )

        val diceDescription = composeRule.onNodeWithTag("dice_badge", useUnmergedTree = true)
            .fetchSemanticsNode("Dice badge missing")
            .config
            .getOrElseNullable(SemanticsProperties.ContentDescription) { null }
            .orEmpty()
            .joinToString(" ")
        assertTrue(diceDescription.contains("The bot rolls automatically."))
    }

    @Test
    fun boardScreen_clickingDiceRunsRollingAnimationBeforeCallback() {
        var rollClicks = 0
        composeRule.mainClock.autoAdvance = false
        try {
            setBoardContent(
                orientation = Configuration.ORIENTATION_PORTRAIT,
                onRollDice = { rollClicks += 1 }
            )

            composeRule.onNodeWithTag("dice_badge").performClick()
            assertEquals(0, rollClicks)
            composeRule.mainClock.advanceTimeBy(700L)
            composeRule.runOnIdle { assertEquals(1, rollClicks) }
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun boardScreen_rendersLadderMoveAnimationState() {
        composeRule.mainClock.autoAdvance = false
        try {
            val state = sampleGameState().copy(
                players = listOf(
                    PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 38),
                    PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 1)
                ),
                lastMovePlayerIndex = 0,
                lastMovePath = listOf(2, 38),
                lastMoveType = MoveType.LADDER,
                moveSignal = 1
            )

            setBoardContent(Configuration.ORIENTATION_PORTRAIT, state = state)
            composeRule.mainClock.advanceTimeBy(2_800L)

            composeRule.onNodeWithTag("dice_badge").assertIsDisplayed()
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun boardScreen_rendersSnakeMoveAnimationState() {
        composeRule.mainClock.autoAdvance = false
        try {
            val state = sampleGameState().copy(
                players = listOf(
                    PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 54),
                    PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 1)
                ),
                lastMovePlayerIndex = 0,
                lastMovePath = listOf(99, 54),
                lastMoveType = MoveType.SNAKE,
                moveSignal = 1
            )

            setBoardContent(Configuration.ORIENTATION_PORTRAIT, state = state)
            composeRule.mainClock.advanceTimeBy(2_300L)

            composeRule.onNodeWithTag("dice_badge").assertIsDisplayed()
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun boardScreen_rendersKnockBackAnimationState() {
        composeRule.mainClock.autoAdvance = false
        try {
            val state = sampleGameState().copy(
                players = listOf(
                    PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 3),
                    PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 1)
                ),
                lastMovePlayerIndex = 0,
                lastMovePath = listOf(2, 3),
                lastMoveType = MoveType.NORMAL,
                moveSignal = 1,
                knockBackMoves = listOf(KnockBackMove(playerIndex = 1, path = listOf(2, 1)))
            )

            setBoardContent(Configuration.ORIENTATION_PORTRAIT, state = state)
            composeRule.mainClock.advanceTimeBy(900L)

            composeRule.onNodeWithTag("dice_badge").assertIsDisplayed()
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun boardScreen_settingsPauseDisablesDiceAndResumesBack() {
        setBoardContent(Configuration.ORIENTATION_LANDSCAPE)
        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.onNodeWithTag("settings_pause_resume_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("dice_badge").assertIsNotEnabled()
        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.onNodeWithTag("settings_pause_resume_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("dice_badge").assertIsEnabled()
    }

    @Test
    fun settingsDialog_showsBoardThemeGameOptionsAndExit() {
        setBoardContent(Configuration.ORIENTATION_PORTRAIT)
        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.onNodeWithTag("settings_match_summary").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_tab_visual").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings_theme_vibrant_chip").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_theme_premium_muted_chip").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_tab_audio").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings_soundtrack_comeback").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings_soundtrack_preview_hint").performScrollTo().assertTextEquals(
            "Selecting a soundtrack previews its cue immediately."
        )
        composeRule.onNodeWithTag("settings_music_slider").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("SFX Volume").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_tab_controls").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings_reduced_motion_switch").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_reduced_motion_status").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_fast_animation_switch").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_compact_match_ui_switch").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_shake_to_roll_switch").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_shake_to_roll_availability").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_bot_pace_quick").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_bot_pace_quick").performClick()
        composeRule.onNodeWithTag("settings_bot_pace_summary").assertTextEquals("Speeds up repeated bot turns.")
        composeRule.onNodeWithTag("settings_manual_bot_roll_switch").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_vibration_switch").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_vibration_availability").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun settingsDialog_tabsResetScrollAndShowCategoryGuidance() {
        setBoardContent(Configuration.ORIENTATION_PORTRAIT)

        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.onNodeWithTag("settings_tab_guidance_text").assertTextEquals(
            "Save, restart, replay, pause, and exit stay with the current match rules."
        )

        composeRule.onNodeWithTag("settings_tab_controls").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings_tab_guidance_text").assertTextEquals(
            "Control choices tune speed, motion, bot turns, haptics, and one-hand match layout."
        )
        composeRule.onNodeWithTag("settings_vibration_availability").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("settings_tab_visual").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings_tab_guidance_text").assertTextEquals(
            "Visual choices preview board style, dice, trails, and contrast separately from match actions."
        )
    }

    @Test
    fun settingsDialog_compactMatchUiHidesAdvancedMatchExtras() {
        resetBoardSettings()
        setBoardContent(Configuration.ORIENTATION_PORTRAIT)

        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.onNodeWithTag("settings_tab_controls").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings_compact_match_ui_status").performScrollTo().assertTextEquals(
            "Compact match UI is off."
        )
        composeRule.onNodeWithTag("settings_compact_match_ui_switch").performScrollTo().performClick()
        composeRule.onNodeWithTag("settings_compact_match_ui_status").performScrollTo().assertTextEquals(
            "Compact match UI is on."
        )
    }

    @Test
    fun settingsDialog_matchTabShowsGameActionsAndExit() {
        setBoardContent(Configuration.ORIENTATION_PORTRAIT)
        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.onNodeWithTag("settings_match_summary").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_new_game_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_restart_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_pause_resume_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_save_game_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_replay_button").performScrollTo().assertIsDisplayed().assertIsNotEnabled()
        composeRule.onNodeWithTag("settings_rules_reference").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_exit_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun settingsExitConfirmationUsesSafeCloseShell() {
        setBoardContent(Configuration.ORIENTATION_PORTRAIT)

        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.onNodeWithTag("settings_exit_button").performScrollTo().performClick()

        composeRule.onNodeWithTag("exit_confirmation_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("exit_warning_message").assertIsDisplayed()
        composeRule.onNodeWithTag("exit_confirmation_dialog_close_button").assertIsDisplayed().performClick()
        composeRule.onAllNodesWithTag("exit_confirmation_dialog").assertCountEquals(0)
    }

    @Test
    fun settingsRestartConfirmationUsesSafeCloseShell() {
        setBoardContent(Configuration.ORIENTATION_PORTRAIT)

        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.onNodeWithTag("settings_restart_button").performScrollTo().performClick()

        composeRule.onNodeWithTag("restart_confirmation_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Restart Match?").assertIsDisplayed()
        composeRule.onNodeWithTag("restart_confirmation_dialog_close_button").assertIsDisplayed().performClick()
        composeRule.onAllNodesWithTag("restart_confirmation_dialog").assertCountEquals(0)
    }

    @Test
    fun settingsNewGame_showsDifficultyDialogAndStartsWithSelection() {
        var selectedDifficulty: GameDifficulty? = null
        setBoardContent(
            orientation = Configuration.ORIENTATION_PORTRAIT,
            onStartNewGame = { selectedDifficulty = it }
        )

        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.onNodeWithTag("settings_new_game_button").performScrollTo().performClick()
        composeRule.onNodeWithText("Select Difficulty").assertIsDisplayed()
        composeRule.onNodeWithTag("difficulty_hard_button").performClick()

        assertEquals(GameDifficulty.HARD, selectedDifficulty)
    }

    @Test
    fun settingsSaveGame_promptsForName_andSaves() {
        var savedName: String? = null
        setBoardContent(
            orientation = Configuration.ORIENTATION_PORTRAIT,
            onSaveGame = {
                savedName = it
                true
            }
        )
        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.onNodeWithTag("settings_save_game_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("save_game_name_input").assertIsDisplayed()
        composeRule.onNodeWithTag("save_game_name_input").performTextClearance()
        composeRule.onNodeWithTag("save_game_name_input").performTextInput("Weekend Match")
        composeRule.onNodeWithTag("save_game_confirm_button").performClick()
        assertEquals("Weekend Match", savedName)
    }

    @Test
    fun settingsSaveGame_failureKeepsDialogOpenAndShowsError() {
        var saveAttempts = 0
        setBoardContent(
            orientation = Configuration.ORIENTATION_PORTRAIT,
            onSaveGame = {
                saveAttempts += 1
                false
            }
        )

        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.onNodeWithTag("settings_save_game_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("save_game_name_input").performTextClearance()
        composeRule.onNodeWithTag("save_game_name_input").performTextInput("Weekend Match")
        composeRule.onNodeWithTag("save_game_confirm_button").performClick()

        assertEquals(1, saveAttempts)
        composeRule.onNodeWithTag("save_game_name_input").assertIsDisplayed()
        composeRule.onNodeWithTag("save_game_status").assertIsDisplayed()
    }

    @Test
    fun settingsSaveGame_saveDisabledForBlankName() {
        setBoardContent(Configuration.ORIENTATION_PORTRAIT)
        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.onNodeWithTag("settings_save_game_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("save_game_name_input").performTextClearance()
        composeRule.onNodeWithTag("save_game_name_input").performTextInput("   ")
        composeRule.onNodeWithTag("save_game_confirm_button").assertIsEnabled()
    }

    @Test
    fun settingsSaveGame_cancelDoesNotInvokeSave() {
        var savedName: String? = null
        setBoardContent(
            orientation = Configuration.ORIENTATION_PORTRAIT,
            onSaveGame = {
                savedName = it
                true
            }
        )
        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.onNodeWithTag("settings_save_game_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("save_game_name_input").performTextClearance()
        composeRule.onNodeWithTag("save_game_name_input").performTextInput("Weekend Match")
        composeRule.onNodeWithTag("save_game_cancel_button").performClick()
        assertEquals(null, savedName)
    }

    @Test
    fun settingsSaveGame_cancelClearsNameField() {
        setBoardContent(Configuration.ORIENTATION_PORTRAIT)
        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.onNodeWithTag("settings_save_game_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("save_game_name_input").performTextClearance()
        composeRule.onNodeWithTag("save_game_name_input").performTextInput("Temp Match")
        composeRule.onNodeWithTag("save_game_cancel_button").performClick()

        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.onNodeWithTag("settings_save_game_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("save_game_confirm_button").assertIsEnabled()
    }

    @Test
    fun boardScreen_pauseOverlayKeepsSettingsShortcutAccessible() {
        setBoardContent(Configuration.ORIENTATION_PORTRAIT)
        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.onNodeWithTag("settings_pause_resume_button").performScrollTo().performClick()
        composeRule.onNodeWithText("Done").performClick()
        composeRule.onNodeWithTag("settings_button").assertIsDisplayed()

        composeRule.onNodeWithText("Resume").performClick()
        composeRule.onNodeWithTag("settings_button").assertIsDisplayed()
    }

    @Test
    fun winnerOverlay_buttonsTriggerCallbacks() {
        var newGameClicks = 0
        var selectedDifficulty: GameDifficulty? = null
        var rematchClicks = 0
        var exitClicks = 0
        setBoardContent(
            orientation = Configuration.ORIENTATION_PORTRAIT,
            state = sampleGameState(winnerIndex = 0),
            onStartNewGame = {
                newGameClicks += 1
                selectedDifficulty = it
            },
            onRestart = { rematchClicks += 1 },
            onExit = { exitClicks += 1 }
        )

        composeRule.onNodeWithText("Winner!").assertIsDisplayed()
        composeRule.onNodeWithTag("winner_new_game_button").performClick()
        composeRule.onNodeWithText("Select Difficulty").assertIsDisplayed()
        composeRule.onNodeWithTag("difficulty_hard_button").performClick()
        composeRule.onNodeWithTag("winner_rematch_button").performClick()
        composeRule.onNodeWithTag("winner_exit_button").performClick()
        assertEquals(1, newGameClicks)
        assertEquals(GameDifficulty.HARD, selectedDifficulty)
        assertEquals(1, rematchClicks)
        assertEquals(1, exitClicks)
    }
}
