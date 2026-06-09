package com.example.snakeladder

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.snakeladder.ui.theme.SnakeLadderTheme

private const val AUTO_SAVE_NAME = "Auto Save"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = AndroidColor.rgb(255, 249, 241)
        window.navigationBarColor = AndroidColor.rgb(17, 17, 22)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        setContent {
            SnakeLadderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val gameController = remember { SnakeLadderController() }
                    val appContext = this@MainActivity
                    var showLaunchLayout by rememberSaveable { mutableStateOf(true) }
                    var selectedPlayers by rememberSaveable { mutableIntStateOf(2) }
                    var selectedMode by rememberSaveable { mutableStateOf(GameMode.LOCAL_MULTIPLAYER) }
                    var selectedMatchMode by rememberSaveable { mutableStateOf(MatchModePreset.CLASSIC) }
                    var selectedBoardLayoutId by rememberSaveable { mutableStateOf(BoardLayouts.CLASSIC_ID) }
                    var selectedBotPersonality by rememberSaveable { mutableStateOf(BotPersonality.STEADY) }
                    var selectedBoardTheme by rememberSaveable { mutableStateOf(BoardThemeOption.VIBRANT) }
                    var activeSaveId by rememberSaveable { mutableStateOf<String?>(null) }
                    var autoSaveId by rememberSaveable { mutableStateOf<String?>(null) }
                    var lastRecordedWinSignal by rememberSaveable { mutableIntStateOf(-1) }
                    var newGameGuideDismissed by rememberSaveable { mutableStateOf(false) }
                    var savedGames by remember { mutableStateOf(emptyList<SavedGameSnapshot>()) }
                    var playerProfile by remember { mutableStateOf(PlayerProfile()) }
                    var playerSetupNames by rememberSaveable {
                        mutableStateOf(playerSetupNameDrafts(defaultPlayerSetups()))
                    }
                    var playerSetupAvatarIds by rememberSaveable {
                        mutableStateOf(playerSetupAvatarDrafts(defaultPlayerSetups()))
                    }
                    var progressAlerts by remember { mutableStateOf(emptyList<String>()) }
                    var launchSetupLoaded by rememberSaveable { mutableStateOf(false) }
                    val dailyChallenge = remember { DailyChallengeCatalog.today() }
                    val playerSetups = remember(playerSetupNames, playerSetupAvatarIds) {
                        buildPlayerSetupDrafts(playerSetupNames, playerSetupAvatarIds)
                    }

                    fun updatePlayerSetup(index: Int, setup: PlayerSetup) {
                        playerSetupNames = updatePlayerSetupDraftValue(
                            values = playerSetupNames,
                            index = index,
                            value = setup.name.take(MAX_PLAYER_SETUP_NAME_LENGTH),
                            defaultValueFor = ::defaultPlayerName
                        )
                        playerSetupAvatarIds = updatePlayerSetupDraftValue(
                            values = playerSetupAvatarIds,
                            index = index,
                            value = setup.avatarId.ifBlank { defaultPlayerAvatarId(index + 1) },
                            defaultValueFor = ::defaultPlayerAvatarId
                        )
                    }

                    fun syncPrimaryAvatar(avatarId: String) {
                        playerSetupAvatarIds = updatePlayerSetupDraftValue(
                            values = playerSetupAvatarIds,
                            index = 0,
                            value = avatarId.ifBlank { defaultPlayerAvatarId(1) },
                            defaultValueFor = ::defaultPlayerAvatarId
                        )
                    }

                    LaunchedEffect(Unit) {
                        CustomBoardStore.load(appContext)
                        val launchSetup = LaunchSetupStore.load(appContext)
                        selectedPlayers = launchSetup.players
                        selectedMode = launchSetup.mode
                        selectedMatchMode = launchSetup.matchMode
                        selectedBoardLayoutId = launchSetup.boardLayoutId
                        selectedBotPersonality = launchSetup.botPersonality
                        newGameGuideDismissed = launchSetup.newGameGuideDismissed
                        launchSetupLoaded = true
                        savedGames = SavedGameStore.loadAll(appContext)
                        val loadedProfile = PlayerProfileStore.load(appContext)
                        playerProfile = loadedProfile
                        syncPrimaryAvatar(loadedProfile.selectedAvatarId)
                    }

                    LaunchedEffect(
                        launchSetupLoaded,
                        selectedPlayers,
                        selectedMode,
                        selectedMatchMode,
                        selectedBoardLayoutId,
                        selectedBotPersonality,
                        newGameGuideDismissed
                    ) {
                        if (launchSetupLoaded) {
                            LaunchSetupStore.save(
                                appContext,
                                LaunchSetupSnapshot(
                                    players = selectedPlayers,
                                    mode = selectedMode,
                                    matchMode = selectedMatchMode,
                                    boardLayoutId = selectedBoardLayoutId,
                                    botPersonality = selectedBotPersonality,
                                    newGameGuideDismissed = newGameGuideDismissed
                                )
                            )
                        }
                    }

                    LaunchedEffect(
                        showLaunchLayout,
                        gameController.state.winnerIndex,
                        gameController.state.moveSignal
                    ) {
                        val state = gameController.state
                        if (!showLaunchLayout &&
                            state.winnerIndex != null &&
                            state.moveSignal != lastRecordedWinSignal
                        ) {
                            val result = PlayerProfileStore.recordCompletedMatch(
                                context = appContext,
                                state = state,
                                challenge = dailyChallenge
                            )
                            ProductAnalyticsStore.recordMatchCompleted(appContext, state)
                            playerProfile = result.profile
                            progressAlerts = buildList {
                                result.newlyUnlockedAchievements.forEach { achievement ->
                                    add("Achievement unlocked: ${achievement.title}")
                                }
                                if (result.dailyChallengeCompletedNow) {
                                    add("Daily challenge completed: ${dailyChallenge.title}")
                                }
                                if (!result.reward.isEmpty()) {
                                    add("Rewards earned: ${result.reward.summary()}")
                                }
                            }
                            lastRecordedWinSignal = state.moveSignal
                        }
                    }

                    if (showLaunchLayout) {
                        LaunchLayout(
                            selectedPlayers = selectedPlayers,
                            selectedMode = selectedMode,
                            selectedMatchMode = selectedMatchMode,
                            selectedBoardLayoutId = selectedBoardLayoutId,
                            selectedBotPersonality = selectedBotPersonality,
                            playerProfile = playerProfile,
                            playerSetups = playerSetups,
                            dailyChallenge = dailyChallenge,
                            savedGames = savedGames,
                            showNewGameGuide = !newGameGuideDismissed,
                            onRefreshSavedGames = {
                                savedGames = SavedGameStore.loadAll(appContext)
                            },
                            onSelectPlayers = { selectedPlayers = it },
                            onSelectMode = { mode ->
                                selectedMode = mode
                                if (mode == GameMode.VS_BOT) {
                                    selectedPlayers = 2
                                }
                            },
                            onSelectMatchMode = { mode ->
                                selectedMatchMode = mode
                                if (mode == MatchModePreset.TEAM_MODE || mode == MatchModePreset.TWO_V_TWO) {
                                    selectedMode = GameMode.LOCAL_MULTIPLAYER
                                    selectedPlayers = 4
                                }
                            },
                            onSelectBoardLayout = { selectedBoardLayoutId = it },
                            onSaveCustomBoard = { snakes, ladders ->
                                CustomBoardStore.save(appContext, snakes, ladders)?.also {
                                    selectedBoardLayoutId = BoardLayouts.CUSTOM_ID
                                } != null
                            },
                            onSelectBotPersonality = { selectedBotPersonality = it },
                            onUpdatePlayerSetup = ::updatePlayerSetup,
                            onLoadSavedGame = { saved ->
                                selectedMode = saved.state.gameMode
                                selectedPlayers = if (saved.state.gameMode == GameMode.VS_BOT) {
                                    2
                                } else {
                                    saved.state.players.size.coerceIn(2, 4)
                                }
                                selectedBotPersonality = saved.state.botPersonality
                                selectedMatchMode = saved.state.matchMode
                                selectedBoardLayoutId = saved.state.boardLayoutId
                                selectedBoardTheme = saved.boardTheme
                                playerSetupNames = playerSetupNameDrafts(
                                    saved.state.players.map { PlayerSetup(it.name, it.avatarId) }
                                )
                                playerSetupAvatarIds = playerSetupAvatarDrafts(
                                    saved.state.players.map { PlayerSetup(it.name, it.avatarId) }
                                )
                                activeSaveId = saved.id
                                autoSaveId = if (saved.name == AUTO_SAVE_NAME) saved.id else null
                                lastRecordedWinSignal = if (saved.state.winnerIndex != null) saved.state.moveSignal else -1
                                gameController.loadState(saved.state)
                                showLaunchLayout = false
                            },
                            onDeleteSavedGame = { saved ->
                                SavedGameStore.deleteById(appContext, saved.id)
                                savedGames = SavedGameStore.loadAll(appContext)
                            },
                            onStartDailyChallenge = { difficulty ->
                                activeSaveId = null
                                autoSaveId = null
                                lastRecordedWinSignal = -1
                                progressAlerts = emptyList()
                                selectedMode = GameMode.VS_BOT
                                selectedPlayers = 2
                                selectedBotPersonality = dailyChallenge.botPersonality
                                selectedMatchMode = dailyChallenge.matchMode
                                selectedBoardLayoutId = dailyChallenge.boardLayoutId
                                showLaunchLayout = false
                                playerProfile = PlayerProfileStore.recordStartedMatch(appContext)
                                ProductAnalyticsStore.recordMatchStarted(appContext, GameMode.VS_BOT, dailyChallenge.matchMode)
                                gameController.startGame(
                                    players = 2,
                                    mode = GameMode.VS_BOT,
                                    difficulty = difficulty,
                                    botPersonality = dailyChallenge.botPersonality,
                                    matchMode = dailyChallenge.matchMode,
                                    boardLayoutId = dailyChallenge.boardLayoutId,
                                    humanAvatarId = playerProfile.selectedAvatarId,
                                    playerSetups = playerSetups,
                                    dailyChallengeId = dailyChallenge.id
                                )
                            },
                            onQuickStart = {
                                activeSaveId = null
                                autoSaveId = null
                                lastRecordedWinSignal = -1
                                progressAlerts = emptyList()
                                showLaunchLayout = false
                                playerProfile = PlayerProfileStore.recordStartedMatch(appContext)
                                ProductAnalyticsStore.recordMatchStarted(appContext, selectedMode, selectedMatchMode)
                                gameController.startGame(
                                    players = selectedPlayers,
                                    mode = selectedMode,
                                    difficulty = GameDifficulty.EASY,
                                    botPersonality = selectedBotPersonality,
                                    matchMode = selectedMatchMode,
                                    boardLayoutId = selectedBoardLayoutId,
                                    humanAvatarId = playerProfile.selectedAvatarId,
                                    playerSetups = playerSetups
                                )
                            },
                            onStartCampaignNode = { node ->
                                activeSaveId = null
                                autoSaveId = null
                                lastRecordedWinSignal = -1
                                progressAlerts = emptyList()
                                val campaignPlayers = when {
                                    node.matchMode == MatchModePreset.TEAM_MODE || node.matchMode == MatchModePreset.TWO_V_TWO -> 4
                                    node.gameMode == GameMode.VS_BOT -> 2
                                    else -> selectedPlayers.coerceIn(2, 4)
                                }
                                selectedMode = node.gameMode
                                selectedPlayers = campaignPlayers
                                selectedBotPersonality = node.botPersonality
                                selectedMatchMode = node.matchMode
                                selectedBoardLayoutId = node.boardLayoutId
                                showLaunchLayout = false
                                playerProfile = PlayerProfileStore.recordStartedMatch(appContext)
                                ProductAnalyticsStore.recordMatchStarted(appContext, node.gameMode, node.matchMode)
                                gameController.startGame(
                                    players = campaignPlayers,
                                    mode = node.gameMode,
                                    difficulty = node.difficulty,
                                    botPersonality = node.botPersonality,
                                    matchMode = node.matchMode,
                                    boardLayoutId = node.boardLayoutId,
                                    humanAvatarId = playerProfile.selectedAvatarId,
                                    playerSetups = playerSetups,
                                    campaignNodeId = node.id
                                )
                            },
                            onExportProfile = {
                                PlayerProfileStore.exportProfile(playerProfile)
                            },
                            onShareProfile = { raw ->
                                shareProfileExport(raw)
                            },
                            onImportProfile = { raw ->
                                val imported = PlayerProfileStore.importAndSave(appContext, raw)
                                if (imported != null) {
                                    playerProfile = imported
                                    syncPrimaryAvatar(imported.selectedAvatarId)
                                    true
                                } else {
                                    false
                                }
                            },
                            onResetProfile = {
                                playerProfile = PlayerProfileStore.reset(appContext).also {
                                    syncPrimaryAvatar(it.selectedAvatarId)
                                }
                            },
                            onEquipProfileItem = { itemId ->
                                val result = StoreCatalog.equip(playerProfile, itemId)
                                if (result.profile != playerProfile) {
                                    playerProfile = result.profile
                                    syncPrimaryAvatar(result.profile.selectedAvatarId)
                                    PlayerProfileStore.save(appContext, result.profile)
                                }
                                result.message
                            },
                            onPurchaseStoreItem = { itemId ->
                                val result = StoreCatalog.purchase(playerProfile, itemId)
                                if (result.profile != playerProfile) {
                                    playerProfile = result.profile
                                    syncPrimaryAvatar(result.profile.selectedAvatarId)
                                    PlayerProfileStore.save(appContext, result.profile)
                                }
                                result.message
                            },
                            onDismissNewGameGuide = {
                                newGameGuideDismissed = true
                            },
                            onStart = { difficulty ->
                                activeSaveId = null
                                autoSaveId = null
                                lastRecordedWinSignal = -1
                                progressAlerts = emptyList()
                                showLaunchLayout = false
                                playerProfile = PlayerProfileStore.recordStartedMatch(appContext)
                                ProductAnalyticsStore.recordMatchStarted(appContext, selectedMode, selectedMatchMode)
                                gameController.startGame(
                                    players = selectedPlayers,
                                    mode = selectedMode,
                                    difficulty = difficulty,
                                    botPersonality = selectedBotPersonality,
                                    matchMode = selectedMatchMode,
                                    boardLayoutId = selectedBoardLayoutId,
                                    humanAvatarId = playerProfile.selectedAvatarId,
                                    playerSetups = playerSetups
                                )
                            }
                        )
                    } else {
                        SnakeLadderScreen(
                            state = gameController.state,
                            selectedBoardTheme = selectedBoardTheme,
                            onSelectBoardTheme = { selectedBoardTheme = it },
                            onStartNewGame = { difficulty ->
                                activeSaveId = null
                                autoSaveId = null
                                lastRecordedWinSignal = -1
                                playerProfile = PlayerProfileStore.recordStartedMatch(appContext)
                                ProductAnalyticsStore.recordMatchStarted(appContext, selectedMode, selectedMatchMode)
                                gameController.startGame(
                                    players = selectedPlayers,
                                    mode = selectedMode,
                                    difficulty = difficulty,
                                    botPersonality = selectedBotPersonality,
                                    matchMode = selectedMatchMode,
                                    boardLayoutId = selectedBoardLayoutId,
                                    humanAvatarId = playerProfile.selectedAvatarId,
                                    playerSetups = playerSetups
                                )
                            },
                            onRollDice = { gameController.rollDice() },
                            onUsePowerUp = { gameController.usePowerUp(it) },
                            onCancelArmedPowerUp = { gameController.cancelArmedPowerUp(it) },
                            onRestart = {
                                lastRecordedWinSignal = -1
                                autoSaveId = null
                                gameController.reset()
                            },
                            onSaveGame = { saveName ->
                                val savedId = SavedGameStore.save(
                                    context = appContext,
                                    name = saveName,
                                    boardTheme = selectedBoardTheme,
                                    state = gameController.state,
                                    existingId = activeSaveId
                                )
                                if (savedId != null) {
                                    activeSaveId = savedId
                                    savedGames = SavedGameStore.loadAll(appContext)
                                    showLaunchLayout = true
                                    true
                                } else {
                                    false
                                }
                            },
                            onExit = {
                                if (gameController.state.winnerIndex == null) {
                                    autoSaveId = SavedGameStore.save(
                                        context = appContext,
                                        name = AUTO_SAVE_NAME,
                                        boardTheme = selectedBoardTheme,
                                        state = gameController.state,
                                        existingId = autoSaveId
                                    )
                                    savedGames = SavedGameStore.loadAll(appContext)
                                    ProductAnalyticsStore.recordEarlyExit(appContext, gameController.state)
                                }
                                showLaunchLayout = true
                            },
                            progressAlerts = progressAlerts,
                            playerProfile = playerProfile,
                            dailyChallenge = dailyChallenge,
                            onDismissProgressAlert = {
                                progressAlerts = progressAlerts.drop(1)
                            }
                        )
                    }

                    LaunchedEffect(
                        showLaunchLayout,
                        gameController.state.moveSignal,
                        gameController.state.winnerIndex
                    ) {
                        val state = gameController.state
                        if (!showLaunchLayout && state.moveSignal > 0 && state.winnerIndex == null) {
                            autoSaveId = SavedGameStore.save(
                                context = appContext,
                                name = AUTO_SAVE_NAME,
                                boardTheme = selectedBoardTheme,
                                state = state,
                                existingId = autoSaveId
                            )
                            savedGames = SavedGameStore.loadAll(appContext)
                        }
                    }
                }
            }
        }
    }

    private fun shareProfileExport(raw: String): Boolean {
        if (raw.isBlank()) return false
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Snake & Ladder profile backup")
            putExtra(Intent.EXTRA_TEXT, raw)
        }
        return runCatching {
            startActivity(Intent.createChooser(sendIntent, "Share profile backup"))
        }.isSuccess
    }
}

private fun buildPlayerSetupDrafts(
    names: List<String>,
    avatarIds: List<String>
): List<PlayerSetup> {
    return List(4) { index ->
        PlayerSetup(
            name = names.getOrNull(index)
                ?.take(MAX_PLAYER_SETUP_NAME_LENGTH)
                ?.ifBlank { defaultPlayerName(index + 1) }
                ?: defaultPlayerName(index + 1),
            avatarId = avatarIds.getOrNull(index)
                ?.ifBlank { defaultPlayerAvatarId(index + 1) }
                ?: defaultPlayerAvatarId(index + 1)
        )
    }
}

private fun playerSetupNameDrafts(setups: List<PlayerSetup>): List<String> {
    return List(4) { index ->
        setups.getOrNull(index)
            ?.name
            ?.take(MAX_PLAYER_SETUP_NAME_LENGTH)
            ?.ifBlank { defaultPlayerName(index + 1) }
            ?: defaultPlayerName(index + 1)
    }
}

private fun playerSetupAvatarDrafts(setups: List<PlayerSetup>): List<String> {
    return List(4) { index ->
        setups.getOrNull(index)
            ?.avatarId
            ?.ifBlank { defaultPlayerAvatarId(index + 1) }
            ?: defaultPlayerAvatarId(index + 1)
    }
}

private fun updatePlayerSetupDraftValue(
    values: List<String>,
    index: Int,
    value: String,
    defaultValueFor: (Int) -> String
): List<String> {
    val targetIndex = index.coerceIn(0, 3)
    return List(4) { draftIndex ->
        if (draftIndex == targetIndex) {
            value
        } else {
            values.getOrNull(draftIndex) ?: defaultValueFor(draftIndex + 1)
        }
    }
}
