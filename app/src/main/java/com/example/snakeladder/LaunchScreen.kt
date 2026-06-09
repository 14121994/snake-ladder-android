package com.example.snakeladder

import android.content.res.Configuration
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private fun GameMode.launchLabel(): String {
    return when (this) {
        GameMode.LOCAL_MULTIPLAYER -> "Multiplayer"
        GameMode.VS_BOT -> "Vs Bot"
    }
}

@Composable
internal fun LaunchLayout(
    selectedPlayers: Int,
    selectedMode: GameMode,
    selectedMatchMode: MatchModePreset = MatchModePreset.CLASSIC,
    selectedBoardLayoutId: String = BoardLayouts.CLASSIC_ID,
    selectedBotPersonality: BotPersonality = BotPersonality.STEADY,
    playerProfile: PlayerProfile = PlayerProfile(),
    playerSetups: List<PlayerSetup> = defaultPlayerSetups(),
    dailyChallenge: DailyChallenge = DailyChallengeCatalog.today(),
    savedGames: List<SavedGameSnapshot>,
    showNewGameGuide: Boolean = true,
    onRefreshSavedGames: () -> Unit,
    onSelectPlayers: (Int) -> Unit,
    onSelectMode: (GameMode) -> Unit,
    onSelectMatchMode: (MatchModePreset) -> Unit = {},
    onSelectBoardLayout: (String) -> Unit = {},
    onSaveCustomBoard: (String, String) -> Boolean = { _, _ -> false },
    onSelectBotPersonality: (BotPersonality) -> Unit = {},
    onUpdatePlayerSetup: (Int, PlayerSetup) -> Unit = { _, _ -> },
    onLoadSavedGame: (SavedGameSnapshot) -> Unit,
    onDeleteSavedGame: (SavedGameSnapshot) -> Unit,
    onStartDailyChallenge: (GameDifficulty) -> Unit = {},
    onQuickStart: () -> Unit = {},
    onStartCampaignNode: (CampaignNode) -> Unit = {},
    onExportProfile: () -> String = { "" },
    onShareProfile: (String) -> Boolean = { false },
    onImportProfile: (String) -> Boolean = { false },
    onResetProfile: () -> Unit = {},
    onEquipProfileItem: (String) -> String = { "Profile equipment unavailable." },
    onPurchaseStoreItem: (String) -> String = { "Store unavailable." },
    onDismissNewGameGuide: () -> Unit = {},
    onStart: (GameDifficulty) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTabletWidth = configuration.screenWidthDp >= 700
    val useWideLaunchLayout = isLandscape || isTabletWidth
    val compactLaunchUi = isLandscape || configuration.screenHeightDp <= 760
    var showSavedGamesDialog by rememberSaveable { mutableStateOf(false) }
    var showDifficultyDialog by rememberSaveable { mutableStateOf(false) }
    var showProFeatureHub by rememberSaveable { mutableStateOf(false) }
    var showProgressionDialog by rememberSaveable { mutableStateOf(false) }
    var showCampaignDialog by rememberSaveable { mutableStateOf(false) }
    var showNewGameDialog by rememberSaveable { mutableStateOf(false) }
    var showStoreDialog by rememberSaveable { mutableStateOf(false) }
    var newGameGuideHiddenInSession by rememberSaveable { mutableStateOf(false) }
    val nextCampaignNode = CampaignCatalog.nextPlayableNode(playerProfile)
    val backgroundPulse by rememberInfiniteTransition(label = "launch_bg_pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "launch_bg_pulse_value"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFDF3D8),
                        Color(0xFFE7F2FF),
                        Color(0xFFEFF9EC)
                    ),
                    start = Offset.Zero,
                    end = Offset(1000f, 1900f)
                )
            )
            .padding(24.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val wobble = sin(backgroundPulse * PI * 2f).toFloat()

            drawCircle(
                color = Color(0x1AFFB74D),
                radius = size.minDimension * 0.30f,
                center = Offset(size.width * 0.14f, size.height * (0.15f + 0.008f * wobble))
            )
            drawCircle(
                color = Color(0x1A2196F3),
                radius = size.minDimension * 0.32f,
                center = Offset(size.width * 0.92f, size.height * (0.32f - 0.006f * wobble))
            )
            drawCircle(
                color = Color(0x1A4CAF50),
                radius = size.minDimension * 0.26f,
                center = Offset(size.width * 0.72f, size.height * (0.94f + 0.005f * wobble))
            )

            val ladderLeft = Path().apply {
                moveTo(size.width * 0.08f, size.height * 0.52f)
                lineTo(size.width * 0.22f, size.height * 0.90f)
            }
            val ladderRight = Path().apply {
                moveTo(size.width * 0.14f, size.height * 0.50f)
                lineTo(size.width * 0.28f, size.height * 0.88f)
            }
            drawPath(ladderLeft, color = Color(0x20A1703D), style = Stroke(width = 8f, cap = StrokeCap.Round))
            drawPath(ladderRight, color = Color(0x20A1703D), style = Stroke(width = 8f, cap = StrokeCap.Round))
            for (i in 0..6) {
                val t = i / 6f
                val x1 = size.width * (0.08f + (0.22f - 0.08f) * t)
                val y1 = size.height * (0.52f + (0.90f - 0.52f) * t)
                val x2 = size.width * (0.14f + (0.28f - 0.14f) * t)
                val y2 = size.height * (0.50f + (0.88f - 0.50f) * t)
                drawLine(
                    color = Color(0x20C89E5C),
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = 7f,
                    cap = StrokeCap.Round
                )
            }

            val snakePath = Path().apply {
                moveTo(size.width * 0.84f, size.height * 0.08f)
                cubicTo(
                    size.width * 0.98f, size.height * 0.18f,
                    size.width * 0.70f, size.height * 0.28f,
                    size.width * 0.90f, size.height * 0.42f
                )
                cubicTo(
                    size.width * 1.00f, size.height * 0.50f,
                    size.width * 0.66f, size.height * 0.63f,
                    size.width * 0.83f, size.height * 0.77f
                )
            }
            drawPath(snakePath, color = Color(0x1AB71C1C), style = Stroke(width = 14f, cap = StrokeCap.Round))
            drawPath(snakePath, color = Color(0x12EF5350), style = Stroke(width = 8f, cap = StrokeCap.Round))
        }

        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .systemBarsPadding()
                .fillMaxSize()
                .testTag(if (useWideLaunchLayout) "launch_wide_layout" else "launch_compact_layout"),
            contentAlignment = Alignment.TopCenter
        ) {
            val launchCard: @Composable (Modifier) -> Unit = { cardModifier ->
                LaunchSettingsCard(
                    savedGames = savedGames,
                    onOpenSavedGames = {
                        onRefreshSavedGames()
                        showSavedGamesDialog = true
                    },
                    onResumeLatestSavedGame = {
                        savedGames.firstOrNull()?.let(onLoadSavedGame)
                    },
                    onOpenProgression = { showProgressionDialog = true },
                    onOpenProFeatures = { showProFeatureHub = true },
                    onOpenCampaign = { showCampaignDialog = true },
                    onOpenNewGame = { showNewGameDialog = true },
                    onOpenStore = { showStoreDialog = true },
                    onStartDailyChallenge = {
                        showDifficultyDialog = true
                    },
                    onQuickStart = onQuickStart,
                    onStartNextCampaignNode = {
                        nextCampaignNode?.let(onStartCampaignNode)
                    },
                    nextCampaignNode = nextCampaignNode,
                    playerProfile = playerProfile,
                    dailyChallenge = dailyChallenge,
                    compact = compactLaunchUi,
                    wideActions = useWideLaunchLayout,
                    modifier = cardModifier
                )
            }

            if (useWideLaunchLayout) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    LaunchOverviewRail(
                        selectedPlayers = selectedPlayers,
                        selectedMode = selectedMode,
                        selectedMatchMode = selectedMatchMode,
                        selectedBoardLayoutId = selectedBoardLayoutId,
                        selectedBotPersonality = selectedBotPersonality,
                        savedGameCount = savedGames.size,
                        nextCampaignNode = nextCampaignNode,
                        dailyChallenge = dailyChallenge,
                        playerProfile = playerProfile,
                        compact = compactLaunchUi,
                        modifier = Modifier.weight(0.86f)
                    )
                    launchCard(Modifier.weight(1.14f))
                }
            } else {
                launchCard(
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = 620.dp)
                )
            }
        }

        if (showSavedGamesDialog) {
            SavedGamesDialog(
                savedGames = savedGames,
                onLoad = {
                    showSavedGamesDialog = false
                    onLoadSavedGame(it)
                },
                onDelete = onDeleteSavedGame,
                onDismiss = { showSavedGamesDialog = false }
            )
        }

        if (showDifficultyDialog) {
            DifficultyDialog(
                title = "Daily Challenge",
                supportingText = "Choose how punishing knockbacks should be for today's run.",
                badgeText = "Today",
                contextPreview = {
                    DailyChallengeStartCard(
                        challenge = dailyChallenge,
                        profile = playerProfile
                    )
                },
                onSelectDifficulty = { difficulty ->
                    showDifficultyDialog = false
                    onStartDailyChallenge(difficulty)
                },
                onDismiss = { showDifficultyDialog = false }
            )
        }

        if (showProFeatureHub) {
            ProFeatureHubDialog(
                onDismiss = { showProFeatureHub = false }
            )
        }

        if (showProgressionDialog) {
            ProgressionDialog(
                profile = playerProfile,
                dailyChallenge = dailyChallenge,
                onExportProfile = onExportProfile,
                onShareProfile = onShareProfile,
                onImportProfile = onImportProfile,
                onResetProfile = onResetProfile,
                onEquipProfileItem = onEquipProfileItem,
                onDismiss = { showProgressionDialog = false }
            )
        }

        if (showCampaignDialog) {
            CampaignDialog(
                profile = playerProfile,
                onStartNode = { node ->
                    showCampaignDialog = false
                    onStartCampaignNode(node)
                },
                onDismiss = { showCampaignDialog = false }
            )
        }

        if (showNewGameDialog) {
            NewGameDialog(
                selectedPlayers = selectedPlayers,
                selectedMode = selectedMode,
                selectedMatchMode = selectedMatchMode,
                selectedBoardLayoutId = selectedBoardLayoutId,
                selectedBotPersonality = selectedBotPersonality,
                playerProfile = playerProfile,
                playerSetups = playerSetups,
                showSetupGuide = showNewGameGuide && !newGameGuideHiddenInSession,
                onSelectPlayers = onSelectPlayers,
                onSelectMode = onSelectMode,
                onSelectMatchMode = onSelectMatchMode,
                onSelectBoardLayout = onSelectBoardLayout,
                onSaveCustomBoard = onSaveCustomBoard,
                onSelectBotPersonality = onSelectBotPersonality,
                onUpdatePlayerSetup = onUpdatePlayerSetup,
                onDismissSetupGuide = {
                    newGameGuideHiddenInSession = true
                    onDismissNewGameGuide()
                },
                onStart = { difficulty ->
                    showNewGameDialog = false
                    onStart(difficulty)
                },
                onDismiss = { showNewGameDialog = false }
            )
        }

        if (showStoreDialog) {
            StoreDialog(
                profile = playerProfile,
                onPurchaseItem = onPurchaseStoreItem,
                onDismiss = { showStoreDialog = false }
            )
        }
    }
}

@Composable
private fun LaunchOverviewRail(
    selectedPlayers: Int,
    selectedMode: GameMode,
    selectedMatchMode: MatchModePreset,
    selectedBoardLayoutId: String,
    selectedBotPersonality: BotPersonality,
    savedGameCount: Int,
    nextCampaignNode: CampaignNode?,
    dailyChallenge: DailyChallenge,
    playerProfile: PlayerProfile,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val board = BoardLayouts.byId(selectedBoardLayoutId)
    val dailyProgress = playerProfile.progressFor(dailyChallenge)
    val titleStyle = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge
    val bodyStyle = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
    val railPadding = if (compact) 8.dp else 12.dp
    val railSpacing = if (compact) 5.dp else 9.dp

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF8EA)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2C99F)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("launch_overview_rail")
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = railPadding, vertical = railPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(railSpacing)
        ) {
            Text(
                text = "Current setup",
                modifier = Modifier.testTag("launch_overview_title"),
                style = titleStyle,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4E342E)
            )
            Text(
                text = "Quick Start uses these choices.",
                style = bodyStyle,
                color = Color(0xFF6B5A4D)
            )
            LaunchOverviewRow(
                label = "Mode",
                value = "${selectedMode.launchLabel()} | ${selectedMatchMode.label}",
                testTag = "launch_overview_mode",
                compact = compact
            )
            LaunchOverviewRow(
                label = "Players",
                value = "$selectedPlayers player${if (selectedPlayers == 1) "" else "s"}",
                testTag = "launch_overview_players",
                compact = compact
            )
            LaunchOverviewRow(
                label = "Board",
                value = board.label,
                testTag = "launch_overview_board",
                compact = compact
            )
            if (selectedMode == GameMode.VS_BOT) {
                LaunchOverviewRow(
                    label = "Bot",
                    value = selectedBotPersonality.styleName,
                    testTag = "launch_overview_bot",
                    compact = compact
                )
            }
            LaunchOverviewRow(
                label = "Daily",
                value = "${dailyChallenge.title} $dailyProgress/${dailyChallenge.target}",
                testTag = "launch_overview_daily",
                compact = compact
            )
            LaunchOverviewRow(
                label = "Saves",
                value = if (savedGameCount == 0) "No local saves" else "$savedGameCount local save${if (savedGameCount == 1) "" else "s"}",
                testTag = "launch_overview_saved",
                compact = compact
            )
            LaunchOverviewRow(
                label = "Campaign",
                value = nextCampaignNode?.let { "${it.title} next" } ?: "Open the quest map",
                testTag = "launch_overview_campaign",
                compact = compact
            )
        }
    }
}

@Composable
private fun LaunchOverviewRow(
    label: String,
    value: String,
    testTag: String,
    compact: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.62f))
            .border(1.dp, Color(0xFFE8D8B9), RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp, vertical = if (compact) 5.dp else 7.dp)
            .testTag(testTag)
    ) {
        if (compact) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    modifier = Modifier.weight(0.34f),
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7A4A00),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    modifier = Modifier.weight(0.66f),
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    color = Color(0xFF4E4239),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7A4A00)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4E4239),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LaunchSettingsCard(
    savedGames: List<SavedGameSnapshot>,
    onOpenSavedGames: () -> Unit,
    onResumeLatestSavedGame: () -> Unit,
    onOpenProgression: () -> Unit,
    onOpenProFeatures: () -> Unit,
    onOpenCampaign: () -> Unit,
    onOpenNewGame: () -> Unit,
    onOpenStore: () -> Unit,
    onStartDailyChallenge: () -> Unit,
    onQuickStart: () -> Unit,
    onStartNextCampaignNode: () -> Unit,
    nextCampaignNode: CampaignNode?,
    playerProfile: PlayerProfile,
    dailyChallenge: DailyChallenge,
    compact: Boolean,
    wideActions: Boolean = false,
    modifier: Modifier = Modifier
) {
    val hasSavedGames = savedGames.isNotEmpty()
    val latestSavedGame = savedGames.firstOrNull()
    val contentPadding = if (compact) 10.dp else 12.dp
    val contentSpacing = if (compact) 7.dp else 9.dp
    val titleStyle = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall
    val subtitleStyle = MaterialTheme.typography.bodyMedium
    val dailyProgress = playerProfile.progressFor(dailyChallenge)
    val dailySubtitle = if (playerProfile.dailyCompletedFor(dailyChallenge)) {
        "Completed today"
    } else {
        "Progress $dailyProgress/${dailyChallenge.target}"
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FBFF)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = contentPadding, vertical = contentPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Snake & Ladder",
                style = titleStyle,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3E2723)
            )
            Text(
                text = "Local play, bot matches, campaign, and daily goals.",
                style = subtitleStyle,
                color = Color(0xFF5D5D5D)
            )

            if (wideActions) {
                LaunchActionGridRow {
                    LaunchActionButton(
                        marker = "1",
                        title = "Quick Start",
                        subtitle = "Use last setup",
                        testTag = "quick_start_button",
                        dense = true,
                        onClick = onQuickStart,
                        modifier = Modifier.weight(1f)
                    )
                    LaunchActionButton(
                        marker = "SET",
                        title = "New Game",
                        subtitle = "Mode, board, rules",
                        testTag = "new_game_button",
                        dense = true,
                        onClick = onOpenNewGame,
                        modifier = Modifier.weight(1f)
                    )
                    LaunchActionButton(
                        marker = "DAY",
                        title = "Play Daily",
                        subtitle = dailySubtitle,
                        testTag = "daily_challenge_start_button",
                        dense = true,
                        onClick = onStartDailyChallenge,
                        modifier = Modifier.weight(1f)
                    )
                }
                LaunchActionGridRow {
                    LaunchActionButton(
                        marker = "MAP",
                        title = "Campaign",
                        subtitle = nextCampaignNode?.let { "${it.title} next" } ?: "Quest map and rewards",
                        testTag = "campaign_button",
                        dense = true,
                        onClick = onOpenCampaign,
                        modifier = Modifier.weight(1f)
                    )
                    LaunchActionButton(
                        marker = "SAV",
                        title = "Load Saved",
                        subtitle = if (hasSavedGames) {
                            "${savedGames.size} saved match${if (savedGames.size == 1) "" else "es"}"
                        } else {
                            "Save required first"
                        },
                        testTag = "load_saved_game_button",
                        enabled = hasSavedGames,
                        disabledReason = "Save a match from the in-game settings menu to enable loading.",
                        dense = true,
                        onClick = onOpenSavedGames,
                        modifier = Modifier.weight(1f)
                    )
                    if (latestSavedGame != null) {
                        LaunchActionButton(
                            marker = "GO",
                            title = "Resume",
                            subtitle = latestSavedGame.name,
                            testTag = "resume_latest_saved_game_button",
                            dense = true,
                            onClick = onResumeLatestSavedGame,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            } else {
                LaunchActionGridRow {
                    LaunchActionButton(
                        marker = "1",
                        title = "Quick Start",
                        subtitle = "Use last setup",
                        testTag = "quick_start_button",
                        onClick = onQuickStart,
                        modifier = Modifier.weight(1f)
                    )
                    LaunchActionButton(
                        marker = "SET",
                        title = "New Game",
                        subtitle = "Mode, board, rules",
                        testTag = "new_game_button",
                        onClick = onOpenNewGame,
                        modifier = Modifier.weight(1f)
                    )
                }

                LaunchActionGridRow {
                    LaunchActionButton(
                        marker = "DAY",
                        title = "Play Daily",
                        subtitle = dailySubtitle,
                        testTag = "daily_challenge_start_button",
                        onClick = onStartDailyChallenge,
                        modifier = Modifier.weight(1f)
                    )
                    LaunchActionButton(
                        marker = "MAP",
                        title = "Campaign",
                        subtitle = nextCampaignNode?.let { "${it.title} next" } ?: "Quest map and rewards",
                        testTag = "campaign_button",
                        onClick = onOpenCampaign,
                        modifier = Modifier.weight(1f)
                    )
                }

                LaunchActionGridRow {
                    LaunchActionButton(
                        marker = "SAV",
                        title = "Load Saved",
                        subtitle = if (hasSavedGames) {
                            "${savedGames.size} saved match${if (savedGames.size == 1) "" else "es"}"
                        } else {
                            "Save required first"
                        },
                        testTag = "load_saved_game_button",
                        enabled = hasSavedGames,
                        disabledReason = "Save a match from the in-game settings menu to enable loading.",
                        onClick = onOpenSavedGames,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (latestSavedGame != null) {
                    LaunchActionButton(
                        marker = "GO",
                        title = "Resume Latest",
                        subtitle = "${latestSavedGame.name} | Saved ${formatSavedAt(latestSavedGame.savedAt)}",
                        testTag = "resume_latest_saved_game_button",
                        onClick = onResumeLatestSavedGame,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            LocalSaveStatusCard(
                savedGameCount = savedGames.size,
                latestSavedGame = latestSavedGame,
                dailyProgress = dailyProgress,
                dailyTarget = dailyChallenge.target
            )

            LaunchSecondaryNavigation(
                onOpenProgression = onOpenProgression,
                onOpenStore = onOpenStore,
                onOpenProFeatures = onOpenProFeatures
            )

            if (!compact) {
                Text(
                    text = "Use New Game for the full rules setup. Daily, Campaign, and Store keep rewards local to this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7A6E65)
                )
            }
        }
    }
}

@Composable
private fun LocalSaveStatusCard(
    savedGameCount: Int,
    latestSavedGame: SavedGameSnapshot?,
    dailyProgress: Int,
    dailyTarget: Int
) {
    val hasSavedGames = savedGameCount > 0
    val title = if (hasSavedGames) "Local save ready" else "No saved match yet"
    val detail = if (hasSavedGames) {
        val latestName = latestSavedGame?.name ?: "Resume Latest"
        "$savedGameCount saved match${if (savedGameCount == 1) "" else "es"} on this device. Latest: $latestName."
    } else {
        "Start a match, then use Save Game in Settings. Daily progress stays local: $dailyProgress/$dailyTarget."
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (hasSavedGames) Color(0xFFEFF8F1) else Color(0xFFFFF8E7))
            .border(
                1.dp,
                if (hasSavedGames) Color(0xFFB8D7BE) else Color(0xFFE7D2B1),
                RoundedCornerShape(12.dp)
            )
            .semantics { contentDescription = "$title. $detail" }
            .testTag("local_save_status_card")
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                modifier = Modifier.testTag("local_save_status_title"),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (hasSavedGames) Color(0xFF245A31) else Color(0xFF6B4B24)
            )
            Text(
                text = detail,
                modifier = Modifier.testTag("local_save_status_detail"),
                style = MaterialTheme.typography.bodySmall,
                color = if (hasSavedGames) Color(0xFF315C3A) else Color(0xFF6B5A4D)
            )
        }
    }
}

@Composable
private fun LaunchActionGridRow(
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
private fun LaunchActionButton(
    marker: String,
    title: String,
    subtitle: String,
    testTag: String,
    enabled: Boolean = true,
    disabledReason: String? = null,
    dense: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val unavailableReason = disabledReason ?: subtitle
    val buttonMinHeight = 66.dp
    val markerSize = if (dense) 30.dp else 34.dp
    val markerShape = if (dense) 11.dp else 12.dp
    val titleFontSize = if (dense) 12.sp else 14.sp
    val titleLineHeight = if (dense) 14.sp else 16.sp
    val subtitleFontSize = if (dense) 11.sp else 12.sp
    val subtitleLineHeight = if (dense) 13.sp else 14.sp
    Button(
        modifier = modifier
            .heightIn(min = buttonMinHeight)
            .semantics {
                contentDescription = if (enabled) {
                    "$title. $subtitle"
                } else {
                    "$title. Disabled. $unavailableReason"
                }
                stateDescription = if (enabled) {
                    "Enabled"
                } else {
                    "Disabled: $unavailableReason"
                }
            }
            .testTag(testTag),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = if (dense) 6.dp else 8.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = Color(0xFFE9EDF2),
            disabledContentColor = Color(0xFF69717A)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(markerSize)
                    .clip(RoundedCornerShape(markerShape))
                    .background(if (enabled) Color(0x22FFFFFF) else Color(0xFFFFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = marker,
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    fontSize = titleFontSize,
                    lineHeight = titleLineHeight,
                    fontWeight = FontWeight.Bold,
                    maxLines = if (dense) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = subtitleFontSize,
                    lineHeight = subtitleLineHeight,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LaunchSecondaryNavigation(
    onOpenProgression: () -> Unit,
    onOpenStore: () -> Unit,
    onOpenProFeatures: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF7FBFF))
            .border(1.dp, Color(0xFFD4E0EE), RoundedCornerShape(12.dp))
            .padding(8.dp)
            .testTag("launch_secondary_navigation"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "More",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF24435F)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LaunchSecondaryButton(
                label = "Progress",
                description = "Stats and rewards",
                testTag = "progression_button",
                onClick = onOpenProgression,
                modifier = Modifier.weight(1f)
            )
            LaunchSecondaryButton(
                label = "Store",
                description = "Unlocks",
                testTag = "store_button",
                onClick = onOpenStore,
                modifier = Modifier.weight(1f)
            )
            LaunchSecondaryButton(
                label = "Guide",
                description = "Feature guide",
                testTag = "pro_features_button",
                onClick = onOpenProFeatures,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LaunchSecondaryButton(
    label: String,
    description: String,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics { contentDescription = "$label. $description" }
            .testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 7.dp),
        onClick = onClick
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DailyChallengeStartCard(
    challenge: DailyChallenge,
    profile: PlayerProfile
) {
    val board = BoardLayouts.byId(challenge.boardLayoutId)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF8E7))
            .border(1.dp, Color(0xFFE2C99F), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            MiniBoardPreview(
                boardLayoutId = challenge.boardLayoutId,
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = challenge.title,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4E342E)
                )
                Text(
                    text = challenge.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5F5B55)
                )
                Text(
                    text = "${board.label} | ${challenge.matchMode.label} | ${challenge.botPersonality.styleName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6D6259)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DailyInfoPill(text = "Target ${challenge.target}", modifier = Modifier.weight(1f))
                    DailyInfoPill(text = "Streak ${profile.dailyStreak}", modifier = Modifier.weight(1f))
                }
                DailyInfoPill(
                    text = "Reward ${challenge.reward.summary()}",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DailyInfoPill(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFF7FBFF))
            .border(1.dp, Color(0xFFBFD4EA), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF24435F),
            maxLines = 2
        )
    }
}

@Composable
private fun MatchSetupSummary(
    selectedMode: GameMode,
    selectedDifficulty: GameDifficulty,
    selectedMatchMode: MatchModePreset,
    selectedBoardLayoutId: String,
    selectedBotPersonality: BotPersonality,
    selectedPlayers: Int,
    playerSetups: List<PlayerSetup>
) {
    val board = BoardLayouts.byId(selectedBoardLayoutId)
    val ruleSet = RuleSets.forMatchMode(selectedMatchMode)
    val visiblePlayerCount = visiblePlayerSetupCount(
        selectedMode = selectedMode,
        selectedMatchMode = selectedMatchMode,
        selectedPlayers = selectedPlayers
    )
    val playerNames = (0 until visiblePlayerCount).map { index ->
        playerSetupAt(playerSetups, index).name.trim().ifBlank { defaultPlayerName(index + 1) }
    }
    val lineupText = if (selectedMode == GameMode.VS_BOT) {
        "${playerNames.first()} vs ${selectedBotPersonality.displayName}"
    } else {
        playerNames.joinToString(" vs ")
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFFF8E7))
            .border(1.dp, Color(0xFFE6D3B4), RoundedCornerShape(14.dp))
            .padding(12.dp)
            .testTag("match_setup_summary")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Ready to start",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4E342E)
            )
            Text(
                text = "${selectedMatchMode.label} | ${board.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF4E342E)
            )
            Text(
                text = "${selectedMode.launchLabel()} | ${selectedDifficulty.name.lowercase().replaceFirstChar { it.uppercase() }} | ${ruleSet.label}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF675A50)
            )
            Text(
                text = lineupText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF24435F),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("match_setup_players_summary")
            )
            if (selectedMode == GameMode.VS_BOT) {
                Text(
                    text = "${selectedBotPersonality.displayName}: ${selectedBotPersonality.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF675A50)
                )
            }
        }
    }
}

@Composable
private fun NewGameDialog(
    selectedPlayers: Int,
    selectedMode: GameMode,
    selectedMatchMode: MatchModePreset,
    selectedBoardLayoutId: String,
    selectedBotPersonality: BotPersonality,
    playerProfile: PlayerProfile,
    playerSetups: List<PlayerSetup>,
    showSetupGuide: Boolean,
    onSelectPlayers: (Int) -> Unit,
    onSelectMode: (GameMode) -> Unit,
    onSelectMatchMode: (MatchModePreset) -> Unit,
    onSelectBoardLayout: (String) -> Unit,
    onSaveCustomBoard: (String, String) -> Boolean,
    onSelectBotPersonality: (BotPersonality) -> Unit,
    onUpdatePlayerSetup: (Int, PlayerSetup) -> Unit,
    onDismissSetupGuide: () -> Unit,
    onStart: (GameDifficulty) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDifficulty by rememberSaveable { mutableStateOf(GameDifficulty.EASY) }
    var showAdvancedSetup by rememberSaveable { mutableStateOf(false) }
    var guidedSetupStep by rememberSaveable { mutableStateOf(NewGameSetupStep.SETUP) }
    val selectedBoard = BoardLayouts.byId(selectedBoardLayoutId)
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val sectionOffsets = remember { mutableStateMapOf<NewGameSetupStep, Int>() }
    val activeStep by remember {
        derivedStateOf {
            val scrollPosition = scrollState.value + 24
            val positionedStep = sectionOffsets.entries
                .filter { it.value <= scrollPosition }
                .maxByOrNull { it.value }
                ?.key
            when {
                scrollState.maxValue > 0 && scrollState.value >= scrollState.maxValue - 24 -> NewGameSetupStep.REVIEW
                positionedStep != null -> positionedStep
                else -> NewGameSetupStep.SETUP
            }
        }
    }

    fun Modifier.setupStepAnchor(step: NewGameSetupStep): Modifier {
        return onGloballyPositioned { coordinates ->
            sectionOffsets[step] = coordinates.positionInParent().y
                .toInt()
                .coerceAtLeast(0)
        }
    }

    fun scrollToStep(step: NewGameSetupStep) {
        val target = if (step == NewGameSetupStep.REVIEW) {
            scrollState.maxValue
        } else {
            sectionOffsets[step] ?: 0
        }
        coroutineScope.launch {
            scrollState.animateScrollTo((target - 12).coerceIn(0, scrollState.maxValue))
        }
    }

    fun selectGuidedStep(step: NewGameSetupStep) {
        guidedSetupStep = step
        scrollToStep(step)
    }

    GameSheetDialog(
        testTag = "new_game_dialog",
        title = "New Game",
        subtitle = "${selectedMatchMode.label} | ${selectedBoard.label}",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SetupJourneyStrip(
                activeStep = activeStep,
                onSelectStep = ::selectGuidedStep
            )
            if (showSetupGuide) {
                GuidedSetupTutorialCard(
                    currentStep = guidedSetupStep,
                    onBack = {
                        guidedSetupStep.previous()?.let(::selectGuidedStep)
                    },
                    onNext = {
                        val nextStep = guidedSetupStep.next()
                        if (nextStep == null) {
                            onDismissSetupGuide()
                        } else {
                            selectGuidedStep(nextStep)
                        }
                    },
                    onStartSetup = {
                        selectGuidedStep(NewGameSetupStep.SETUP)
                    },
                    onHideTips = onDismissSetupGuide
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SetupSectionPanel(
                    modifier = Modifier.setupStepAnchor(NewGameSetupStep.SETUP),
                    title = "1. Quick Setup",
                    subtitle = "Choose the core match choices first. Advanced tools stay optional."
                ) {
                    GameModePicker(
                        selectedMode = selectedMode,
                        onSelectMode = { mode ->
                            onSelectMode(mode)
                            if (mode == GameMode.VS_BOT && selectedMatchMode == MatchModePreset.TWO_V_TWO) {
                                onSelectMatchMode(MatchModePreset.CLASSIC)
                            }
                        }
                    )
                    if (selectedMode == GameMode.VS_BOT) {
                        BotPersonalityPicker(
                            selectedBotPersonality = selectedBotPersonality,
                            onSelectBotPersonality = onSelectBotPersonality
                        )
                    } else {
                        PlayerCountPicker(
                            selectedPlayers = selectedPlayers,
                            onSelectPlayers = { count ->
                                onSelectPlayers(count)
                                if (count != 4 && selectedMatchMode == MatchModePreset.TWO_V_TWO) {
                                    onSelectMatchMode(MatchModePreset.CLASSIC)
                                }
                            }
                        )
                    }
                    PlayerSetupPanel(
                        selectedMode = selectedMode,
                        selectedMatchMode = selectedMatchMode,
                        selectedPlayers = selectedPlayers,
                        playerProfile = playerProfile,
                        playerSetups = playerSetups,
                        onUpdatePlayerSetup = onUpdatePlayerSetup
                    )
                    DifficultyPicker(
                        selectedDifficulty = selectedDifficulty,
                        onSelectDifficulty = { selectedDifficulty = it }
                    )
                }
                SetupSectionPanel(
                    modifier = Modifier.setupStepAnchor(NewGameSetupStep.MODE),
                    title = "2. Match Mode",
                    subtitle = "Pick how this match is scored. Board layout stays separate below."
                ) {
                    MatchModePicker(
                        selectedMatchMode = selectedMatchMode,
                        selectedPlayers = selectedPlayers,
                        selectedMode = selectedMode,
                        onSelectMatchMode = onSelectMatchMode,
                        compact = false
                    )
                    MatchModeInsightPanel(selectedMatchMode = selectedMatchMode)
                    if (selectedMatchMode == MatchModePreset.TWO_V_TWO) {
                        TeamPreviewPanel()
                    }
                }
                SetupSectionPanel(
                    modifier = Modifier.setupStepAnchor(NewGameSetupStep.BOARD),
                    title = "3. Board Layout",
                    subtitle = if (selectedBoardLayoutId == BoardLayouts.CUSTOM_ID) {
                        "Custom Lab is selected. Edit its snakes and ladders here before you start."
                    } else {
                        "Preview each board first. Selecting Custom Lab opens its editor here."
                    }
                ) {
                    BoardLayoutPicker(
                        selectedBoardLayoutId = selectedBoardLayoutId,
                        onSelectBoardLayout = onSelectBoardLayout,
                        compact = false
                    )
                    if (selectedBoardLayoutId == BoardLayouts.CUSTOM_ID) {
                        CustomBoardEditorPanel(
                            onSaveCustomBoard = { snakes, ladders ->
                                if (onSaveCustomBoard(snakes, ladders)) {
                                    onSelectBoardLayout(BoardLayouts.CUSTOM_ID)
                                    true
                                } else {
                                    false
                                }
                            }
                        )
                    }
                }
                Box(modifier = Modifier.setupStepAnchor(NewGameSetupStep.REVIEW)) {
                    AdvancedSetupPanel(
                        expanded = showAdvancedSetup,
                        onToggle = { showAdvancedSetup = !showAdvancedSetup }
                    ) {
                        RulesExplanationPanel(
                            selectedMode = selectedMode,
                            selectedMatchMode = selectedMatchMode,
                            selectedDifficulty = selectedDifficulty,
                            selectedBoardLayoutId = selectedBoardLayoutId,
                            selectedBotPersonality = selectedBotPersonality
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF8FBFF))
                    .border(1.dp, Color(0xFFD4E0EE), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MatchSetupSummary(
                        selectedMode = selectedMode,
                        selectedDifficulty = selectedDifficulty,
                        selectedMatchMode = selectedMatchMode,
                        selectedBoardLayoutId = selectedBoardLayoutId,
                        selectedBotPersonality = selectedBotPersonality,
                        selectedPlayers = selectedPlayers,
                        playerSetups = playerSetups
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            modifier = Modifier.weight(1f),
                            onClick = onDismiss
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("new_game_start_button"),
                            onClick = {
                                onDismissSetupGuide()
                                onStart(selectedDifficulty)
                            }
                        ) {
                            Text("Start Match")
                        }
                    }
                }
            }
        }
    }
}

private data class CustomBoardPairDraft(
    val id: Int,
    val fromText: String,
    val toText: String
)

private data class CustomBoardEditorSnapshot(
    val snakeRows: List<CustomBoardPairDraft>,
    val ladderRows: List<CustomBoardPairDraft>,
    val nextSnakeRowId: Int,
    val nextLadderRowId: Int
)

private enum class NewGameSetupStep(
    val label: String,
    val testTag: String,
    val contentDescription: String,
    val guideTitle: String,
    val guideText: String
) {
    SETUP(
        "1 Setup",
        "new_game_step_setup",
        "Jump to Quick Setup",
        "Quick setup",
        "Choose who is playing, name local players, pick a bot if needed, then set the difficulty."
    ),
    MODE(
        "2 Mode",
        "new_game_step_mode",
        "Jump to Match Mode",
        "Match mode",
        "Pick the rule style: Classic is easiest, while Timed, Party, Cards, and 2v2 add more decisions."
    ),
    BOARD(
        "3 Board",
        "new_game_step_board",
        "Jump to Board Layout",
        "Board layout",
        "Preview the board before starting. Short boards are better for quick sessions; chaotic boards suit repeat play."
    ),
    REVIEW(
        "4 Review",
        "new_game_step_review",
        "Jump to Review",
        "Review and start",
        "Check the ready summary at the bottom, open advanced rules only if you want details, then start the match."
    )
}

private fun NewGameSetupStep.next(): NewGameSetupStep? {
    return NewGameSetupStep.entries.getOrNull(ordinal + 1)
}

private fun NewGameSetupStep.previous(): NewGameSetupStep? {
    return NewGameSetupStep.entries.getOrNull(ordinal - 1)
}

private data class CustomBoardValidatedRow(
    val id: Int,
    val from: Int,
    val to: Int
)

private data class CustomBoardRowReview(
    val validRows: List<CustomBoardValidatedRow>,
    val feedback: Map<Int, CustomBoardRowFeedback>
)

private data class CustomBoardEditorValidation(
    val validSnakes: List<Pair<Int, Int>>,
    val validLadders: List<Pair<Int, Int>>,
    val snakeFeedback: Map<Int, CustomBoardRowFeedback>,
    val ladderFeedback: Map<Int, CustomBoardRowFeedback>,
    val issueCount: Int,
    val canSave: Boolean,
    val summary: String,
    val detail: String,
    val previewLayout: BoardLayout
)

private data class CustomBoardRowFeedback(
    val tone: CustomBoardRowTone,
    val message: String
)

private enum class CustomBoardPairType {
    SNAKE,
    LADDER
}

private enum class CustomBoardRowTone {
    NEUTRAL,
    VALID,
    ERROR
}

private enum class CustomBoardMessageTone {
    INFO,
    SUCCESS,
    ERROR
}

@Composable
private fun SetupJourneyStrip(
    activeStep: NewGameSetupStep,
    onSelectStep: (NewGameSetupStep) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFF7FBFF))
            .border(1.dp, Color(0xFFD4E0EE), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NewGameSetupStep.entries.forEach { step ->
            val selected = step == activeStep
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selected) strongSelectedChipContainer else neutralChipContainer)
                    .border(
                        width = 1.dp,
                        color = if (selected) strongSelectedChipBorder else neutralChipBorder,
                        shape = RoundedCornerShape(999.dp)
                    )
                    .heightIn(min = 36.dp)
                    .clickable { onSelectStep(step) }
                    .testTag(step.testTag)
                    .semantics {
                        contentDescription = step.contentDescription
                        stateDescription = if (selected) "Current" else "Available"
                    }
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = step.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else neutralChipLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GuidedSetupTutorialCard(
    currentStep: NewGameSetupStep,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onStartSetup: () -> Unit,
    onHideTips: () -> Unit
) {
    val stepNumber = currentStep.ordinal + 1
    val totalSteps = NewGameSetupStep.entries.size
    val nextLabel = if (currentStep.next() == null) "Finish" else "Next"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFEAF7F0))
            .border(1.dp, Color(0xFFB7D8C4), RoundedCornerShape(14.dp))
            .testTag("new_game_setup_guide")
            .semantics {
                contentDescription = "New Game setup guide. Step $stepNumber of $totalSteps. ${currentStep.guideTitle}."
                stateDescription = "Step $stepNumber of $totalSteps"
            }
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "First-time setup guide",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E5A38)
                    )
                    Text(
                        text = "$stepNumber of $totalSteps: ${currentStep.guideTitle}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF173F2A),
                        modifier = Modifier.testTag("new_game_setup_guide_step_label")
                    )
                }
                TextButton(
                    modifier = Modifier.testTag("new_game_setup_guide_hide_button"),
                    onClick = onHideTips,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Hide tips")
                }
            }
            Text(
                text = currentStep.guideText,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF365545),
                modifier = Modifier.testTag("new_game_setup_guide_text")
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("new_game_setup_guide_back_button"),
                    enabled = currentStep.previous() != null,
                    onClick = onBack,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("Back", maxLines = 1)
                }
                OutlinedButton(
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("new_game_setup_guide_start_button"),
                    onClick = onStartSetup,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("Setup", maxLines = 1)
                }
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("new_game_setup_guide_next_button"),
                    onClick = onNext,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(nextLabel, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SetupSectionPanel(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF8FBFF))
            .border(1.dp, Color(0xFFD4E0EE), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF24435F)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5A6470)
            )
        }
        content()
    }
}

@Composable
private fun AdvancedSetupPanel(
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFFF8E7))
            .border(1.dp, Color(0xFFE3D0AA), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Advanced Setup",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8A3D00)
                )
                Text(
                    text = "Detailed rule notes stay tucked away until you need them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6D6259)
                )
            }
            TextButton(
                modifier = Modifier.testTag("advanced_setup_toggle"),
                onClick = onToggle
            ) {
                Text(if (expanded) "Hide" else "Show")
            }
        }
        if (expanded) {
            content()
        }
    }
}

@Composable
private fun CustomBoardEditorPanel(
    onSaveCustomBoard: (String, String) -> Boolean
) {
    var snakeRows by remember { mutableStateOf(customBoardRowsFromPairs(BoardLayouts.custom.snakes)) }
    var ladderRows by remember { mutableStateOf(customBoardRowsFromPairs(BoardLayouts.custom.ladders)) }
    var nextSnakeRowId by remember {
        mutableStateOf((snakeRows.maxOfOrNull { it.id } ?: 0) + 1)
    }
    var nextLadderRowId by remember {
        mutableStateOf((ladderRows.maxOfOrNull { it.id } ?: 0) + 1)
    }
    var history by remember { mutableStateOf(emptyList<CustomBoardEditorSnapshot>()) }
    var statusText by remember {
        mutableStateOf("Select cells directly. Save stays locked until the board is valid.")
    }
    var statusTone by remember { mutableStateOf(CustomBoardMessageTone.INFO) }
    val validation = remember(snakeRows, ladderRows) {
        buildCustomBoardEditorValidation(
            snakeRows = snakeRows,
            ladderRows = ladderRows
        )
    }

    fun pushHistory() {
        history = (history + CustomBoardEditorSnapshot(
            snakeRows = snakeRows,
            ladderRows = ladderRows,
            nextSnakeRowId = nextSnakeRowId,
            nextLadderRowId = nextLadderRowId
        )).takeLast(24)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("custom_board_editor"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFFFF8E7))
                .border(1.dp, Color(0xFFE3D0AA), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Custom Lab Editor",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8A3D00)
                )
                Text(
                    text = "Edit pairs directly, validate the board, then save it for this device. Snakes go from a higher cell down. Ladders go from a lower cell up.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5F5B55)
                )
                CustomBoardEditorOverview(validation = validation)
            }
        }

        CustomBoardPairSection(
            title = "Snakes",
            subtitle = "Set a higher head and a lower tail. Example: 97 to 61.",
            pairType = CustomBoardPairType.SNAKE,
            rows = snakeRows,
            feedback = validation.snakeFeedback,
            onAddRow = {
                pushHistory()
                snakeRows = snakeRows + CustomBoardPairDraft(
                    id = nextSnakeRowId,
                    fromText = "",
                    toText = ""
                )
                nextSnakeRowId += 1
            },
            onUpdateRow = { rowId, updateFrom, value ->
                pushHistory()
                snakeRows = snakeRows.map { row ->
                    if (row.id != rowId) {
                        row
                    } else if (updateFrom) {
                        row.copy(fromText = sanitizeBoardCellInput(value))
                    } else {
                        row.copy(toText = sanitizeBoardCellInput(value))
                    }
                }
            },
            onRemoveRow = { rowId ->
                pushHistory()
                snakeRows = snakeRows.filterNot { it.id == rowId }
                statusText = "Removed one snake pair."
                statusTone = CustomBoardMessageTone.INFO
            }
        )

        CustomBoardPairSection(
            title = "Ladders",
            subtitle = "Set a lower start and a higher finish. Example: 4 to 29.",
            pairType = CustomBoardPairType.LADDER,
            rows = ladderRows,
            feedback = validation.ladderFeedback,
            onAddRow = {
                pushHistory()
                ladderRows = ladderRows + CustomBoardPairDraft(
                    id = nextLadderRowId,
                    fromText = "",
                    toText = ""
                )
                nextLadderRowId += 1
            },
            onUpdateRow = { rowId, updateFrom, value ->
                pushHistory()
                ladderRows = ladderRows.map { row ->
                    if (row.id != rowId) {
                        row
                    } else if (updateFrom) {
                        row.copy(fromText = sanitizeBoardCellInput(value))
                    } else {
                        row.copy(toText = sanitizeBoardCellInput(value))
                    }
                }
            },
            onRemoveRow = { rowId ->
                pushHistory()
                ladderRows = ladderRows.filterNot { it.id == rowId }
                statusText = "Removed one ladder pair."
                statusTone = CustomBoardMessageTone.INFO
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                modifier = Modifier
                    .weight(1f)
                    .testTag("custom_board_check_button"),
                onClick = {
                    statusText = if (validation.canSave) {
                        "Board ready: ${validation.validSnakes.size} snakes and ${validation.validLadders.size} ladders."
                    } else {
                        validation.summary
                    }
                    statusTone = if (validation.canSave) {
                        CustomBoardMessageTone.SUCCESS
                    } else {
                        CustomBoardMessageTone.ERROR
                    }
                }
            ) {
                Text("Check Layout")
            }
            TextButton(
                modifier = Modifier
                    .weight(1f)
                    .testTag("custom_board_undo_button"),
                enabled = history.isNotEmpty(),
                onClick = {
                    val previous = history.lastOrNull() ?: return@TextButton
                    history = history.dropLast(1)
                    snakeRows = previous.snakeRows
                    ladderRows = previous.ladderRows
                    nextSnakeRowId = previous.nextSnakeRowId
                    nextLadderRowId = previous.nextLadderRowId
                    statusText = "Undid the last Custom Lab change."
                    statusTone = CustomBoardMessageTone.INFO
                }
            ) {
                Text("Undo")
            }
            TextButton(
                modifier = Modifier
                    .weight(1f)
                    .testTag("custom_board_reset_button"),
                onClick = {
                    pushHistory()
                    val defaultSnakeRows = customBoardRowsFromPairs(CustomBoardStore.defaultSnakePairs())
                    val defaultLadderRows = customBoardRowsFromPairs(CustomBoardStore.defaultLadderPairs())
                    snakeRows = defaultSnakeRows
                    ladderRows = defaultLadderRows
                    nextSnakeRowId = (defaultSnakeRows.maxOfOrNull { it.id } ?: 0) + 1
                    nextLadderRowId = (defaultLadderRows.maxOfOrNull { it.id } ?: 0) + 1
                    statusText = "Reset the draft to the default Custom Lab board."
                    statusTone = CustomBoardMessageTone.INFO
                }
            ) {
                Text("Reset")
            }
        }

        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_board_save_button"),
            enabled = validation.canSave,
            onClick = {
                val snakeText = CustomBoardStore.formatPairs(validation.previewLayout.snakes)
                val ladderText = CustomBoardStore.formatPairs(validation.previewLayout.ladders)
                if (onSaveCustomBoard(snakeText, ladderText)) {
                    statusText = "Custom Lab saved and selected."
                    statusTone = CustomBoardMessageTone.SUCCESS
                } else {
                    statusText = "Add at least one valid snake or ladder pair."
                    statusTone = CustomBoardMessageTone.ERROR
                }
            }
        ) {
            Text("Save Custom Lab")
        }

        CustomBoardStatusCard(
            text = statusText,
            tone = statusTone
        )
    }
}

@Composable
private fun CustomBoardPairSection(
    title: String,
    subtitle: String,
    pairType: CustomBoardPairType,
    rows: List<CustomBoardPairDraft>,
    feedback: Map<Int, CustomBoardRowFeedback>,
    onAddRow: () -> Unit,
    onUpdateRow: (rowId: Int, updateFrom: Boolean, value: String) -> Unit,
    onRemoveRow: (rowId: Int) -> Unit
) {
    val filledRows = rows.count { it.fromText.isNotBlank() || it.toText.isNotBlank() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF8FBFF))
            .border(1.dp, Color(0xFFD4E0EE), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4E342E)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6D6259)
                )
            }
            Text(
                text = "$filledRows / ${CustomBoardStore.MAX_PAIRS}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8A3D00)
            )
        }

        if (rows.isEmpty()) {
            Text(
                text = if (pairType == CustomBoardPairType.SNAKE) {
                    "No snake pairs yet. Add one if you want slides on this board."
                } else {
                    "No ladder pairs yet. Add one if you want climbs on this board."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6D6259)
            )
        } else {
            rows.forEachIndexed { index, row ->
                val rowFeedback = feedback[row.id] ?: CustomBoardRowFeedback(
                    tone = CustomBoardRowTone.NEUTRAL,
                    message = "Fill both cells to validate this pair."
                )
                val (borderColor, backgroundColor, messageColor) = when (rowFeedback.tone) {
                    CustomBoardRowTone.VALID -> Triple(Color(0xFF9FD6A8), Color(0xFFF2FBF3), Color(0xFF1B6B3A))
                    CustomBoardRowTone.ERROR -> Triple(Color(0xFFE6A39A), Color(0xFFFFF3F1), Color(0xFF9A2C23))
                    CustomBoardRowTone.NEUTRAL -> Triple(Color(0xFFD9D1C5), Color(0xFFFFFCF5), Color(0xFF6D6259))
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(backgroundColor)
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${if (pairType == CustomBoardPairType.SNAKE) "Snake" else "Ladder"} ${index + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4E342E)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = row.fromText,
                            onValueChange = { onUpdateRow(row.id, true, it) },
                            singleLine = true,
                            label = {
                                Text(
                                    if (pairType == CustomBoardPairType.SNAKE) "Head" else "Start"
                                )
                            },
                            isError = rowFeedback.tone == CustomBoardRowTone.ERROR,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_board_${title.lowercase()}_${index}_from")
                        )
                        OutlinedTextField(
                            value = row.toText,
                            onValueChange = { onUpdateRow(row.id, false, it) },
                            singleLine = true,
                            label = {
                                Text(
                                    if (pairType == CustomBoardPairType.SNAKE) "Tail" else "Finish"
                                )
                            },
                            isError = rowFeedback.tone == CustomBoardRowTone.ERROR,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_board_${title.lowercase()}_${index}_to")
                        )
                        TextButton(onClick = { onRemoveRow(row.id) }) {
                            Text("Remove")
                        }
                    }
                    Text(
                        text = rowFeedback.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = messageColor
                    )
                }
            }
        }

        OutlinedButton(
            onClick = onAddRow,
            enabled = rows.size < CustomBoardStore.MAX_PAIRS
        ) {
            Text(if (pairType == CustomBoardPairType.SNAKE) "Add Snake" else "Add Ladder")
        }
    }
}

@Composable
private fun CustomBoardMetricPill(
    label: String,
    value: String,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .heightIn(min = 40.dp)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CustomBoardEditorOverview(validation: CustomBoardEditorValidation) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stackContent = maxWidth < 420.dp
        if (stackContent) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MiniBoardPreview(
                    boardLayoutId = BoardLayouts.CUSTOM_ID,
                    layoutOverride = validation.previewLayout,
                    modifier = Modifier
                        .size(156.dp)
                        .testTag("custom_board_preview")
                )
                CustomBoardValidationSummary(
                    validation = validation,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniBoardPreview(
                    boardLayoutId = BoardLayouts.CUSTOM_ID,
                    layoutOverride = validation.previewLayout,
                    modifier = Modifier
                        .size(128.dp)
                        .testTag("custom_board_preview")
                )
                CustomBoardValidationSummary(
                    validation = validation,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CustomBoardValidationSummary(
    validation: CustomBoardEditorValidation,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CustomBoardMetricPill(
                label = "Snakes",
                value = validation.validSnakes.size.toString(),
                background = Color(0xFFFFE0E0),
                contentColor = Color(0xFF8C1D18),
                modifier = Modifier.weight(1f)
            )
            CustomBoardMetricPill(
                label = "Ladders",
                value = validation.validLadders.size.toString(),
                background = Color(0xFFE3F7E8),
                contentColor = Color(0xFF1B6B3A),
                modifier = Modifier.weight(1f)
            )
            CustomBoardMetricPill(
                label = "Issues",
                value = validation.issueCount.toString(),
                background = if (validation.issueCount == 0) Color(0xFFE9F5FF) else Color(0xFFFFECEB),
                contentColor = if (validation.issueCount == 0) Color(0xFF1A5B92) else Color(0xFF9A2C23),
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = validation.summary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF4E342E)
        )
        Text(
            text = validation.detail,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6D6259)
        )
    }
}

@Composable
private fun CustomBoardStatusCard(
    text: String,
    tone: CustomBoardMessageTone
) {
    val background = when (tone) {
        CustomBoardMessageTone.SUCCESS -> Color(0xFFE6F6EA)
        CustomBoardMessageTone.ERROR -> Color(0xFFFFECEB)
        CustomBoardMessageTone.INFO -> Color(0xFFF7FBFF)
    }
    val border = when (tone) {
        CustomBoardMessageTone.SUCCESS -> Color(0xFF9FD6A8)
        CustomBoardMessageTone.ERROR -> Color(0xFFE6A39A)
        CustomBoardMessageTone.INFO -> Color(0xFFBFD4EA)
    }
    val textColor = when (tone) {
        CustomBoardMessageTone.SUCCESS -> Color(0xFF1B6B3A)
        CustomBoardMessageTone.ERROR -> Color(0xFF9A2C23)
        CustomBoardMessageTone.INFO -> Color(0xFF24435F)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = textColor
        )
    }
}

private fun customBoardRowsFromPairs(pairs: Map<Int, Int>): List<CustomBoardPairDraft> {
    return customBoardRowsFromPairs(
        pairs.entries
            .sortedBy { it.key }
            .map { it.key to it.value }
    )
}

private fun customBoardRowsFromPairs(pairs: List<Pair<Int, Int>>): List<CustomBoardPairDraft> {
    val rows = pairs.sortedBy { it.first }.mapIndexed { index, (from, to) ->
        CustomBoardPairDraft(
            id = index + 1,
            fromText = from.toString(),
            toText = to.toString()
        )
    }
    return if (rows.isEmpty()) {
        listOf(CustomBoardPairDraft(id = 1, fromText = "", toText = ""))
    } else {
        rows
    }
}

private fun sanitizeBoardCellInput(raw: String): String = raw.filter { it.isDigit() }.take(3)

private fun buildCustomBoardEditorValidation(
    snakeRows: List<CustomBoardPairDraft>,
    ladderRows: List<CustomBoardPairDraft>
): CustomBoardEditorValidation {
    val snakeReview = reviewCustomBoardRows(snakeRows, CustomBoardPairType.SNAKE)
    val ladderReview = reviewCustomBoardRows(ladderRows, CustomBoardPairType.LADDER)
    val snakeFeedback = snakeReview.feedback.toMutableMap()
    val ladderFeedback = ladderReview.feedback.toMutableMap()
    val conflictingStarts = snakeReview.validRows.map { it.from }
        .intersect(ladderReview.validRows.map { it.from }.toSet())

    if (conflictingStarts.isNotEmpty()) {
        snakeReview.validRows.filter { it.from in conflictingStarts }.forEach { row ->
            snakeFeedback[row.id] = CustomBoardRowFeedback(
                tone = CustomBoardRowTone.ERROR,
                message = "Cell ${row.from} already starts a ladder. Use a unique start cell."
            )
        }
        ladderReview.validRows.filter { it.from in conflictingStarts }.forEach { row ->
            ladderFeedback[row.id] = CustomBoardRowFeedback(
                tone = CustomBoardRowTone.ERROR,
                message = "Cell ${row.from} already starts a snake. Use a unique start cell."
            )
        }
    }

    val validSnakes = snakeReview.validRows
        .filterNot { it.from in conflictingStarts }
        .map { it.from to it.to }
    val validLadders = ladderReview.validRows
        .filterNot { it.from in conflictingStarts }
        .map { it.from to it.to }
    val issueCount = snakeFeedback.values.count { it.tone == CustomBoardRowTone.ERROR } +
        ladderFeedback.values.count { it.tone == CustomBoardRowTone.ERROR }
    val hasAnyValidPairs = validSnakes.isNotEmpty() || validLadders.isNotEmpty()
    val canSave = hasAnyValidPairs && issueCount == 0
    val summary = when {
        canSave -> "Ready to save."
        !hasAnyValidPairs && issueCount == 0 -> "Add at least one snake or ladder."
        else -> "Resolve $issueCount issue${if (issueCount == 1) "" else "s"} before saving."
    }
    val detail = when {
        canSave -> "${validSnakes.size} snakes and ${validLadders.size} ladders will be saved to this device."
        !hasAnyValidPairs && issueCount == 0 -> "Use the Add buttons to build a board. Empty rows are ignored."
        else -> "Reversed directions, incomplete pairs, and duplicate start cells are blocked."
    }

    return CustomBoardEditorValidation(
        validSnakes = validSnakes,
        validLadders = validLadders,
        snakeFeedback = snakeFeedback,
        ladderFeedback = ladderFeedback,
        issueCount = issueCount,
        canSave = canSave,
        summary = summary,
        detail = detail,
        previewLayout = BoardLayouts.customFromPairs(
            snakePairs = validSnakes,
            ladderPairs = validLadders,
            preserveCurrentWhenEmpty = false
        )
    )
}

private fun reviewCustomBoardRows(
    rows: List<CustomBoardPairDraft>,
    pairType: CustomBoardPairType
): CustomBoardRowReview {
    val feedback = mutableMapOf<Int, CustomBoardRowFeedback>()
    val validRows = mutableListOf<CustomBoardValidatedRow>()

    rows.forEach { row ->
        val fromText = row.fromText.trim()
        val toText = row.toText.trim()
        if (fromText.isBlank() && toText.isBlank()) {
            feedback[row.id] = CustomBoardRowFeedback(
                tone = CustomBoardRowTone.NEUTRAL,
                message = if (pairType == CustomBoardPairType.SNAKE) {
                    "Empty slot. Add a head and a tail when you need another snake."
                } else {
                    "Empty slot. Add a start and a finish when you need another ladder."
                }
            )
            return@forEach
        }
        if (fromText.isBlank() || toText.isBlank()) {
            feedback[row.id] = CustomBoardRowFeedback(
                tone = CustomBoardRowTone.ERROR,
                message = "Fill both cells for this pair."
            )
            return@forEach
        }
        val from = fromText.toIntOrNull()
        val to = toText.toIntOrNull()
        if (from == null || to == null) {
            feedback[row.id] = CustomBoardRowFeedback(
                tone = CustomBoardRowTone.ERROR,
                message = "Use numbers only."
            )
            return@forEach
        }
        val isValid = when (pairType) {
            CustomBoardPairType.SNAKE -> from in 2..99 && to in 1 until from
            CustomBoardPairType.LADDER -> from in 2..99 && to in (from + 1)..100
        }
        if (!isValid) {
            feedback[row.id] = CustomBoardRowFeedback(
                tone = CustomBoardRowTone.ERROR,
                message = if (pairType == CustomBoardPairType.SNAKE) {
                    "Snakes need a head between 2 and 99, then a lower tail."
                } else {
                    "Ladders need a start between 2 and 99, then a higher finish."
                }
            )
            return@forEach
        }
        validRows += CustomBoardValidatedRow(row.id, from, to)
        feedback[row.id] = CustomBoardRowFeedback(
            tone = CustomBoardRowTone.VALID,
            message = if (pairType == CustomBoardPairType.SNAKE) {
                "Slides from $from down to $to."
            } else {
                "Climbs from $from up to $to."
            }
        )
    }

    val duplicateStarts = validRows.groupBy { it.from }
        .filterValues { it.size > 1 }
        .keys
    if (duplicateStarts.isNotEmpty()) {
        validRows.filter { it.from in duplicateStarts }.forEach { row ->
            feedback[row.id] = CustomBoardRowFeedback(
                tone = CustomBoardRowTone.ERROR,
                message = "Cell ${row.from} is already used in this section."
            )
        }
    }

    return CustomBoardRowReview(
        validRows = validRows.filterNot { it.from in duplicateStarts },
        feedback = feedback
    )
}

@Composable
private fun GameModePicker(
    selectedMode: GameMode,
    onSelectMode: (GameMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag("new_game_mode_picker")) {
        Text("Game Mode", fontWeight = FontWeight.SemiBold)
        SetupSegmentedRow(
            options = listOf(
                SetupSegmentOption(
                    title = "Local",
                    subtitle = "2-4 players",
                    marker = "P",
                    selected = selectedMode == GameMode.LOCAL_MULTIPLAYER,
                    testTag = "new_game_mode_multiplayer_chip",
                    onClick = { onSelectMode(GameMode.LOCAL_MULTIPLAYER) }
                ),
                SetupSegmentOption(
                    title = "Vs Bot",
                    subtitle = "Solo rival",
                    marker = "AI",
                    selected = selectedMode == GameMode.VS_BOT,
                    testTag = "new_game_mode_vs_bot_chip",
                    onClick = { onSelectMode(GameMode.VS_BOT) }
                )
            )
        )
    }
}

@Composable
private fun PlayerCountPicker(
    selectedPlayers: Int,
    onSelectPlayers: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag("new_game_player_picker")) {
        Text("Players", fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(2, 3, 4).forEach { count ->
                FilterChip(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("new_game_players_${count}_chip"),
                    selected = selectedPlayers == count,
                    onClick = { onSelectPlayers(count) },
                    colors = strongFilterChipColors(),
                    border = strongFilterChipBorder(selected = selectedPlayers == count),
                    label = { ChipLabel(count.toString()) }
                )
            }
        }
    }
}

private data class LaunchAvatarChoice(
    val id: String,
    val label: String
)

private val baseLaunchAvatarChoices = listOf(
    LaunchAvatarChoice("classic_token", "Classic"),
    LaunchAvatarChoice("cobra_token", "Cobra"),
    LaunchAvatarChoice("ladder_king", "Ladder"),
    LaunchAvatarChoice("gold_die", "Gold Die")
)

@Composable
private fun PlayerSetupPanel(
    selectedMode: GameMode,
    selectedMatchMode: MatchModePreset,
    selectedPlayers: Int,
    playerProfile: PlayerProfile,
    playerSetups: List<PlayerSetup>,
    onUpdatePlayerSetup: (Int, PlayerSetup) -> Unit
) {
    val visiblePlayerCount = visiblePlayerSetupCount(
        selectedMode = selectedMode,
        selectedMatchMode = selectedMatchMode,
        selectedPlayers = selectedPlayers
    )
    val avatarChoices = launchAvatarChoices(playerProfile)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("player_setup_panel"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Players & avatars", fontWeight = FontWeight.SemiBold)
            Text(
                text = if (selectedMode == GameMode.VS_BOT) {
                    "Customize your player. The rival name follows the selected bot personality."
                } else {
                    "Rename local players and choose match tokens before starting."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5A6470)
            )
        }
        repeat(visiblePlayerCount) { index ->
            PlayerSetupRow(
                playerNumber = index + 1,
                setup = playerSetupAt(playerSetups, index),
                avatarChoices = avatarChoices,
                onUpdate = { onUpdatePlayerSetup(index, it) }
            )
        }
    }
}

@Composable
private fun PlayerSetupRow(
    playerNumber: Int,
    setup: PlayerSetup,
    avatarChoices: List<LaunchAvatarChoice>,
    onUpdate: (PlayerSetup) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFD4E0EE), RoundedCornerShape(12.dp))
            .padding(10.dp)
            .testTag("player_setup_card_$playerNumber"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarTokenPreview(
                avatarId = setup.avatarId,
                fallbackColor = playerPreviewColor(playerNumber),
                modifier = Modifier
                    .size(40.dp)
                    .testTag("player_setup_token_$playerNumber")
            )
            OutlinedTextField(
                value = setup.name,
                onValueChange = { value ->
                    onUpdate(setup.copy(name = value.take(MAX_PLAYER_SETUP_NAME_LENGTH)))
                },
                singleLine = true,
                label = { Text("Player $playerNumber name") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("player_setup_name_$playerNumber")
            )
        }
        avatarChoices.chunked(2).forEach { rowChoices ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowChoices.forEach { choice ->
                    PlayerAvatarChoiceTile(
                        playerNumber = playerNumber,
                        choice = choice,
                        selected = setup.avatarId == choice.id,
                        fallbackColor = playerPreviewColor(playerNumber),
                        onClick = { onUpdate(setup.copy(avatarId = choice.id)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(2 - rowChoices.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RowScope.PlayerAvatarChoiceTile(
    playerNumber: Int,
    choice: LaunchAvatarChoice,
    selected: Boolean,
    fallbackColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (selected) Color(0xFFE1F6E8) else lightUnselectedCardContainer
    val border = if (selected) Color(0xFF0E6B32) else lightUnselectedCardBorder
    Row(
        modifier = modifier
            .heightIn(min = 50.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .border(if (selected) 3.dp else 1.dp, border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "Player $playerNumber ${choice.label} avatar"
                stateDescription = if (selected) "Selected" else "Not selected"
            }
            .padding(horizontal = 8.dp, vertical = 7.dp)
            .testTag("player_setup_avatar_${playerNumber}_${choice.id}"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarTokenPreview(
            avatarId = choice.id,
            fallbackColor = fallbackColor,
            modifier = Modifier.size(26.dp)
        )
        Text(
            text = choice.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color(0xFF0E4E24) else lightUnselectedCardLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun visiblePlayerSetupCount(
    selectedMode: GameMode,
    selectedMatchMode: MatchModePreset,
    selectedPlayers: Int
): Int {
    return when {
        selectedMode == GameMode.VS_BOT -> 1
        selectedMatchMode == MatchModePreset.TEAM_MODE || selectedMatchMode == MatchModePreset.TWO_V_TWO -> 4
        else -> selectedPlayers.coerceIn(2, 4)
    }
}

private fun playerSetupAt(playerSetups: List<PlayerSetup>, index: Int): PlayerSetup {
    return playerSetups.getOrNull(index)?.let { setup ->
        PlayerSetup(
            name = setup.name.take(MAX_PLAYER_SETUP_NAME_LENGTH).ifBlank { defaultPlayerName(index + 1) },
            avatarId = setup.avatarId.ifBlank { defaultPlayerAvatarId(index + 1) }
        )
    } ?: PlayerSetup(
        name = defaultPlayerName(index + 1),
        avatarId = defaultPlayerAvatarId(index + 1)
    )
}

private fun launchAvatarChoices(playerProfile: PlayerProfile): List<LaunchAvatarChoice> {
    val unlockedChoices = playerProfile.unlockedAvatarIds.map { avatarId ->
        LaunchAvatarChoice(id = avatarId, label = avatarDisplayLabel(avatarId))
    }
    return (baseLaunchAvatarChoices + unlockedChoices).distinctBy { it.id }
}

private fun avatarDisplayLabel(avatarId: String): String {
    return when (avatarId) {
        "cobra_token" -> "Cobra"
        "ladder_king" -> "Ladder"
        "gold_die" -> "Gold Die"
        else -> "Classic"
    }
}

private fun playerPreviewColor(playerNumber: Int): Color {
    return when (playerNumber) {
        2 -> Color(0xFF1E88E5)
        3 -> Color(0xFF43A047)
        4 -> Color(0xFFF57C00)
        else -> Color(0xFFE53935)
    }
}

@Composable
private fun BotPersonalityPicker(
    selectedBotPersonality: BotPersonality,
    onSelectBotPersonality: (BotPersonality) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("new_game_bot_picker")) {
        Text("Bot Personality", fontWeight = FontWeight.SemiBold)
        BotPersonality.entries.chunked(2).forEach { personalities ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                personalities.forEach { personality ->
                    BotPersonalityCard(
                        personality = personality,
                        selected = selectedBotPersonality == personality,
                        onClick = { onSelectBotPersonality(personality) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(2 - personalities.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        BotPersonalityImpactPanel(selectedBotPersonality)
    }
}

@Composable
private fun BotPersonalityCard(
    personality: BotPersonality,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SetupChoiceCard(
        modifier = modifier,
        title = botPersonalityTitle(personality),
        subtitle = botPersonalityShortLabel(personality),
        marker = botPersonalityMarker(personality),
        selected = selected,
        onClick = onClick,
        testTag = "new_game_bot_personality_${personality.name.lowercase()}"
    )
}

@Composable
private fun BotPersonalityImpactPanel(personality: BotPersonality) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF8E7))
            .border(1.dp, Color(0xFFE6D3B4), RoundedCornerShape(12.dp))
            .padding(10.dp)
            .testTag("bot_personality_impact_panel")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BotImpactMetric("Speed", botPersonalitySpeed(personality), Modifier.weight(1f))
                BotImpactMetric("Risk", botPersonalityRisk(personality), Modifier.weight(1f))
                BotImpactMetric("Defense", botPersonalityDefense(personality), Modifier.weight(1f))
            }
            Text(
                text = if (personality == BotPersonality.STEADY) {
                    "Recommended for new players. ${personality.description}"
                } else {
                    personality.description
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF675A50)
            )
        }
    }
}

@Composable
private fun BotImpactMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF6D6259),
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4E342E),
            maxLines = 1
        )
    }
}

@Composable
private fun DifficultyPicker(
    selectedDifficulty: GameDifficulty,
    onSelectDifficulty: (GameDifficulty) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag("new_game_difficulty_picker")) {
        Text("Difficulty", fontWeight = FontWeight.SemiBold)
        SetupSegmentedRow(
            options = GameDifficulty.entries.map { difficulty ->
                SetupSegmentOption(
                    title = difficulty.compactLabel(),
                    subtitle = difficulty.compactKnockbackLabel(),
                    marker = difficulty.iconLabel(),
                    selected = selectedDifficulty == difficulty,
                    testTag = "new_game_difficulty_${difficulty.name.lowercase()}",
                    onClick = { onSelectDifficulty(difficulty) }
                )
            }
        )
        Text(
            text = when (selectedDifficulty) {
                GameDifficulty.EASY -> "Easy: ${selectedDifficulty.knockbackRuleLabel()}."
                GameDifficulty.MEDIUM -> "Tactical: ${selectedDifficulty.knockbackRuleLabel()} knock rivals back to start."
                GameDifficulty.HARD -> "Pro: ${selectedDifficulty.knockbackRuleLabel()} landings can knock rivals back."
            },
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF675A50)
        )
    }
}

private data class SetupSegmentOption(
    val title: String,
    val subtitle: String,
    val marker: String,
    val selected: Boolean,
    val testTag: String,
    val onClick: () -> Unit
)

@Composable
private fun SetupSegmentedRow(
    options: List<SetupSegmentOption>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF7FBFF))
            .border(1.dp, Color(0xFFD4E0EE), RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->
            val selectedBackground = strongSelectedChipContainer
            val unselectedBackground = Color.Transparent
            val selectedBorder = strongSelectedChipBorder
            val unselectedBorder = Color.Transparent
            val textColor = if (option.selected) Color.White else lightUnselectedCardLabel
            val supportingColor = if (option.selected) Color(0xFFD8E7FF) else lightUnselectedCardSupport
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 64.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (option.selected) selectedBackground else unselectedBackground)
                    .border(
                        width = if (option.selected) 3.dp else 1.dp,
                        color = if (option.selected) selectedBorder else unselectedBorder,
                        shape = RoundedCornerShape(11.dp)
                    )
                    .clickable(onClick = option.onClick)
                    .semantics {
                        contentDescription = "${option.title}. ${option.subtitle}"
                        stateDescription = if (option.selected) "Selected" else "Not selected"
                    }
                    .padding(horizontal = 5.dp, vertical = 5.dp)
                    .testTag(option.testTag),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (option.selected) Color.White else lightUnselectedMarkerContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.marker,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (option.selected) FontWeight.ExtraBold else FontWeight.Bold,
                        color = if (option.selected) strongSelectedChipContainer else neutralChipLabel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    text = option.title,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (option.selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = option.subtitle,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    color = supportingColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RulesExplanationPanel(
    selectedMode: GameMode,
    selectedMatchMode: MatchModePreset,
    selectedDifficulty: GameDifficulty,
    selectedBoardLayoutId: String,
    selectedBotPersonality: BotPersonality
) {
    val board = BoardLayouts.byId(selectedBoardLayoutId)
    val ruleSet = RuleSets.forMatchMode(selectedMatchMode)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF7FBFF))
            .padding(10.dp)
            .testTag("rules_explanation_panel")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Rule Reference", fontWeight = FontWeight.SemiBold, color = Color(0xFF24435F))
            RuleInsightRow {
                RuleInsightChip(label = "Match mode", value = selectedMatchMode.label)
                RuleInsightChip(label = "Board layout", value = board.label)
            }
            RuleInsightRow {
                RuleInsightChip(label = "Difficulty", value = selectedDifficulty.shortLabel())
                RuleInsightChip(label = "Knockback", value = selectedDifficulty.knockbackSymbolLabel())
            }
            RuleInsightRow {
                RuleInsightChip(label = "Finish", value = "Exact")
                ruleSet.turnLimit?.let { RuleInsightChip(label = "Limit", value = "$it turns") }
                if (ruleSet.roundTarget > 1) {
                    RuleInsightChip(label = "Rounds", value = "First to ${ruleSet.roundTarget}")
                }
            }
            Text(
                text = modeInsightText(selectedMatchMode),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF24435F)
            )
            Text(ruleSet.description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF4F5963))
            Text(board.description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF4F5963))
            Text(PowerUpRuleEngine.describe(ruleSet), style = MaterialTheme.typography.bodySmall, color = Color(0xFF4F5963))
            if (selectedMode == GameMode.VS_BOT) {
                Text(
                    text = "Bot style: ${selectedBotPersonality.description}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF24435F)
                )
            }
        }
    }
}

@Composable
private fun MatchModeInsightPanel(selectedMatchMode: MatchModePreset) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFFFF8E7))
            .border(1.dp, Color(0xFFE3D0AA), RoundedCornerShape(10.dp))
            .padding(10.dp)
            .testTag("match_mode_insight_panel")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = modeInsightTitle(selectedMatchMode),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8A3D00)
            )
            Text(
                text = modeInsightText(selectedMatchMode),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5F5B55)
            )
        }
    }
}

@Composable
private fun TeamPreviewPanel() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF7FBFF))
            .border(1.dp, Color(0xFFD4E0EE), RoundedCornerShape(10.dp))
            .padding(10.dp)
            .testTag("team_preview_panel")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Team preview",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF24435F)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TeamPreviewChip(
                    label = "Team A",
                    players = "P1 + P3",
                    color = Color(0xFFB3261E),
                    modifier = Modifier.weight(1f)
                )
                TeamPreviewChip(
                    label = "Team B",
                    players = "P2 + P4",
                    color = Color(0xFF1565C0),
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = "Either teammate reaching cell 100 wins the round for the whole team.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF4F5963)
            )
        }
    }
}

@Composable
private fun TeamPreviewChip(
    label: String,
    players: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1
            )
            Text(
                text = players,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF4F5963),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RuleInsightRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun RowScope.RuleInsightChip(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFD4E0EE), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF6D6259),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF24435F),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MatchModePicker(
    selectedMatchMode: MatchModePreset,
    selectedPlayers: Int,
    selectedMode: GameMode,
    onSelectMatchMode: (MatchModePreset) -> Unit,
    compact: Boolean
) {
    val baseModes = listOf(
        MatchModePreset.CLASSIC,
        MatchModePreset.TIME_ATTACK,
        MatchModePreset.SUDDEN_DEATH,
        MatchModePreset.BEST_OF_THREE,
        MatchModePreset.PARTY_RULES,
        MatchModePreset.TACTICAL_CARDS
    )
    val showTwoVTwo = selectedMode == GameMode.LOCAL_MULTIPLAYER && selectedPlayers == 4
    val modes = if (showTwoVTwo) baseModes + MatchModePreset.TWO_V_TWO else baseModes
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag("match_mode_picker")) {
        Text("Match Mode", fontWeight = FontWeight.SemiBold)
        modes.chunked(2).forEach { rowModes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowModes.forEach { mode ->
                    MatchModeChoiceCard(
                        mode = mode,
                        selected = selectedMatchMode == mode,
                        onClick = { onSelectMatchMode(mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(if (rowModes.size > 1) 2 - rowModes.size else 0) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        if (!showTwoVTwo) {
            RequirementHint(
                text = if (selectedMode == GameMode.VS_BOT) {
                    "2v2 Teams is a four-player local mode."
                } else {
                    "Select 4 players to unlock 2v2 Teams."
                }
            )
        }
        if (!compact || selectedMatchMode != MatchModePreset.CLASSIC) {
            Text(
                text = selectedMatchMode.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF675A50)
            )
        }
    }
}

@Composable
private fun MatchModeChoiceCard(
    mode: MatchModePreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SetupChoiceCard(
        modifier = modifier,
        title = matchModePickerLabel(mode),
        subtitle = matchModeShortLabel(mode),
        marker = matchModeMarker(mode),
        selected = selected,
        onClick = onClick,
        testTag = "match_mode_${mode.name.lowercase()}"
    )
}

private fun matchModePickerLabel(mode: MatchModePreset): String {
    return when (mode) {
        MatchModePreset.TIME_ATTACK -> "Timed"
        MatchModePreset.SUDDEN_DEATH -> "Sudden"
        MatchModePreset.BEST_OF_THREE -> "Best of 3"
        MatchModePreset.TWO_V_TWO -> "2v2 Teams"
        else -> mode.label
    }
}

private fun matchModeMarker(mode: MatchModePreset): String {
    return when (mode) {
        MatchModePreset.CLASSIC -> "C"
        MatchModePreset.TIME_ATTACK -> "20"
        MatchModePreset.SUDDEN_DEATH -> "SD"
        MatchModePreset.BEST_OF_THREE -> "3"
        MatchModePreset.PARTY_RULES -> "P"
        MatchModePreset.TACTICAL_CARDS -> "CARD"
        MatchModePreset.TWO_V_TWO -> "2v2"
        MatchModePreset.DAILY_CHALLENGE -> "D"
        MatchModePreset.QUEST_NODE -> "Q"
        MatchModePreset.TEAM_MODE -> "T"
    }
}

private fun matchModeShortLabel(mode: MatchModePreset): String {
    return when (mode) {
        MatchModePreset.CLASSIC -> "Standard race"
        MatchModePreset.TIME_ATTACK -> "Turn clock"
        MatchModePreset.SUDDEN_DEATH -> "Furthest wins"
        MatchModePreset.BEST_OF_THREE -> "Two rounds"
        MatchModePreset.PARTY_RULES -> "Power-ups"
        MatchModePreset.TACTICAL_CARDS -> "Card timing"
        MatchModePreset.TWO_V_TWO -> "Shared win"
        MatchModePreset.DAILY_CHALLENGE -> "Daily goal"
        MatchModePreset.QUEST_NODE -> "Campaign"
        MatchModePreset.TEAM_MODE -> "Teams"
    }
}

private fun modeInsightTitle(mode: MatchModePreset): String {
    return when (mode) {
        MatchModePreset.CLASSIC -> "Standard race"
        MatchModePreset.TIME_ATTACK -> "Timed pressure"
        MatchModePreset.SUDDEN_DEATH -> "Risk-heavy finish"
        MatchModePreset.BEST_OF_THREE -> "Round score matters"
        MatchModePreset.PARTY_RULES -> "Power-up timing"
        MatchModePreset.TACTICAL_CARDS -> "Card timing"
        MatchModePreset.TWO_V_TWO -> "Shared team finish"
        MatchModePreset.DAILY_CHALLENGE -> "Daily objective"
        MatchModePreset.QUEST_NODE -> "Campaign objective"
        MatchModePreset.TEAM_MODE -> "Team objective"
    }
}

private fun modeInsightText(mode: MatchModePreset): String {
    return when (mode) {
        MatchModePreset.CLASSIC -> "Race to cell 100 with exact finish rules."
        MatchModePreset.TIME_ATTACK -> "The turn limit makes every ladder, snake, and missed roll matter."
        MatchModePreset.SUDDEN_DEATH -> "When the limit ends, the farthest player wins, so late snakes are costly."
        MatchModePreset.BEST_OF_THREE -> "Win two rounds; the setup keeps the round target visible before start."
        MatchModePreset.PARTY_RULES -> "Power-ups can change a turn before the final race settles."
        MatchModePreset.TACTICAL_CARDS -> "Cards reward planning because timing can matter as much as dice luck."
        MatchModePreset.TWO_V_TWO -> "Four local players split into two teams with a shared finish goal."
        MatchModePreset.DAILY_CHALLENGE -> "The daily challenge locks a board, bot, and rule preset for today's goal."
        MatchModePreset.QUEST_NODE -> "Campaign nodes combine fixed boards, modes, and rewards."
        MatchModePreset.TEAM_MODE -> "Team mode uses shared progress pressure across paired players."
    }
}

@Composable
private fun RequirementHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF7FBFF))
            .border(1.dp, Color(0xFFD4E0EE), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF24435F)
        )
    }
}

@Composable
private fun SetupChoiceCard(
    title: String,
    subtitle: String,
    marker: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val shape = RoundedCornerShape(12.dp)
    val background = when {
        !enabled -> Color(0xFFF0F0F0)
        selected -> strongSelectedCardContainer
        else -> lightUnselectedCardContainer
    }
    val border = when {
        !enabled -> Color(0xFFCCCCCC)
        selected -> strongSelectedCardBorder
        else -> lightUnselectedCardBorder
    }
    val content = when {
        !enabled -> Color(0xFF8A8A8A)
        selected -> Color(0xFF133F73)
        else -> lightUnselectedCardLabel
    }
    val markerText = if (selected) Color.White else neutralChipLabel
    val taggedModifier = if (testTag != null) modifier.testTag(testTag) else modifier

    Box(
        modifier = taggedModifier
            .heightIn(min = 74.dp)
            .clip(shape)
            .background(background)
            .border(if (selected) 3.dp else 1.dp, border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                stateDescription = if (selected) "Selected" else "Not selected"
            }
            .padding(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) strongSelectedChipContainer else lightUnselectedMarkerContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = marker,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = markerText,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        !enabled -> Color(0xFF8A8A8A)
                        selected -> Color(0xFF24435F)
                        else -> lightUnselectedCardSupport
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

private fun botPersonalityTitle(personality: BotPersonality): String {
    return when (personality) {
        BotPersonality.DEFENSIVE -> "Guard"
        else -> personality.styleName
    }
}

private fun botPersonalityMarker(personality: BotPersonality): String {
    return when (personality) {
        BotPersonality.STEADY -> "S"
        BotPersonality.RISKY -> "R"
        BotPersonality.DEFENSIVE -> "D"
        BotPersonality.PRO -> "PRO"
    }
}

private fun botPersonalityShortLabel(personality: BotPersonality): String {
    return when (personality) {
        BotPersonality.STEADY -> "Beginner pick"
        BotPersonality.RISKY -> "Fast swings"
        BotPersonality.DEFENSIVE -> "Safer play"
        BotPersonality.PRO -> "Expert timing"
    }
}

private fun botPersonalitySpeed(personality: BotPersonality): String {
    return when (personality) {
        BotPersonality.STEADY -> "Med"
        BotPersonality.RISKY -> "High"
        BotPersonality.DEFENSIVE -> "Low"
        BotPersonality.PRO -> "High"
    }
}

private fun botPersonalityRisk(personality: BotPersonality): String {
    return when (personality) {
        BotPersonality.STEADY -> "Med"
        BotPersonality.RISKY -> "High"
        BotPersonality.DEFENSIVE -> "Low"
        BotPersonality.PRO -> "Med"
    }
}

private fun botPersonalityDefense(personality: BotPersonality): String {
    return when (personality) {
        BotPersonality.STEADY -> "Med"
        BotPersonality.RISKY -> "Low"
        BotPersonality.DEFENSIVE -> "High"
        BotPersonality.PRO -> "High"
    }
}

@Composable
private fun BoardLayoutPicker(
    selectedBoardLayoutId: String,
    onSelectBoardLayout: (String) -> Unit,
    compact: Boolean
) {
    val boards = BoardLayouts.all
    val selected = BoardLayouts.byId(selectedBoardLayoutId)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag("board_layout_picker")) {
        Text("Board", fontWeight = FontWeight.SemiBold)
        boards.chunked(2).forEach { rowBoards ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowBoards.forEach { board ->
                    BoardLayoutSelectionCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("board_layout_${board.id}"),
                        board = board,
                        selected = selectedBoardLayoutId == board.id,
                        onClick = { onSelectBoardLayout(board.id) }
                    )
                }
                repeat(2 - rowBoards.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        if (!compact || selected.id != BoardLayouts.CLASSIC_ID) {
            Text(
                text = "${selected.description} ${selected.ladders.size} ladders, ${selected.snakes.size} snakes.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF675A50)
            )
        }
    }
}

@Composable
private fun SavedGamesDialog(
    savedGames: List<SavedGameSnapshot>,
    onLoad: (SavedGameSnapshot) -> Unit,
    onDelete: (SavedGameSnapshot) -> Unit,
    onDismiss: () -> Unit
) {
    var pendingDelete by remember { mutableStateOf<SavedGameSnapshot?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val orderedSaves = remember(savedGames) { savedGames.sortedByDescending { it.savedAt } }
    val latestSavedGame = orderedSaves.firstOrNull()
    val searchEnabled = orderedSaves.size >= 4
    val filteredSaves = remember(orderedSaves, searchEnabled, searchQuery) {
        if (!searchEnabled || searchQuery.isBlank()) {
            orderedSaves
        } else {
            orderedSaves.filter { it.matchesSavedGameQuery(searchQuery) }
        }
    }

    GameSheetDialog(
        testTag = "saved_games_dialog",
        title = "Saved Games",
        subtitle = "Resume recent matches or remove old saves from this device.",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (latestSavedGame == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFF8E7))
                        .border(1.dp, Color(0xFFE2C99F), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "No saved games yet. Save a match from the in-game menu to see it here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B5A4D)
                    )
                }
            } else {
                SavedGamesSummaryCard(
                    latestSavedGame = latestSavedGame,
                    savedGameCount = orderedSaves.size,
                    onResumeLatest = { onLoad(latestSavedGame) }
                )
                if (searchEnabled) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("saved_game_search_input"),
                        label = { Text("Search saved games") },
                        placeholder = { Text("Name, board, mode") },
                        supportingText = {
                            Text(
                                text = "${filteredSaves.size} of ${orderedSaves.size} saves shown",
                                modifier = Modifier.testTag("saved_game_search_summary"),
                                color = Color(0xFF6D6259)
                            )
                        }
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredSaves.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFF8E7))
                                .border(1.dp, Color(0xFFE2C99F), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                                .testTag("saved_game_search_empty")
                        ) {
                            Text(
                                text = "No saved games match \"$searchQuery\".",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6B5A4D)
                            )
                        }
                    } else {
                        filteredSaves.forEach { item ->
                            SavedGameListRow(
                                item = item,
                                onLoad = { onLoad(item) },
                                onRequestDelete = { pendingDelete = item }
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { item ->
        SavedGameDeleteConfirmationDialog(
            name = item.name,
            onConfirm = {
                onDelete(item)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null }
        )
    }
}

private fun SavedGameSnapshot.matchesSavedGameQuery(query: String): Boolean {
    val board = BoardLayouts.byId(state.boardLayoutId)
    val gameModeLabel = if (state.gameMode == GameMode.VS_BOT) "Vs Bot" else "Multiplayer"
    val searchable = listOf(
        name,
        board.label,
        state.matchMode.label,
        gameModeLabel,
        state.difficulty.name,
        "${state.players.size} players"
    )
    return searchable.any { it.contains(query.trim(), ignoreCase = true) }
}

@Composable
private fun SavedGamesSummaryCard(
    latestSavedGame: SavedGameSnapshot,
    savedGameCount: Int,
    onResumeLatest: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF7FBFF))
            .border(1.dp, Color(0xFFBFD4EA), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Most recent save",
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF24435F)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniBoardPreview(
                    boardLayoutId = latestSavedGame.state.boardLayoutId,
                    boardThemeOption = latestSavedGame.boardTheme,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(latestSavedGame.name, fontWeight = FontWeight.Bold, color = Color(0xFF4E342E))
                    Text(
                        text = "${BoardLayouts.byId(latestSavedGame.state.boardLayoutId).label} | ${latestSavedGame.state.matchMode.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF5F5B55)
                    )
                    Text(
                        text = "Saved ${formatSavedAt(latestSavedGame.savedAt)} | $savedGameCount total save${if (savedGameCount == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6D6259)
                    )
                }
            }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("resume_latest_saved_game_sheet_button"),
                onClick = onResumeLatest
            ) {
                Text("Resume Latest")
            }
        }
    }
}

@Composable
private fun SavedGameListRow(
    item: SavedGameSnapshot,
    onLoad: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val board = BoardLayouts.byId(item.state.boardLayoutId)
    val cell = item.state.players.maxOfOrNull { player -> player.position } ?: 0
    val gameModeLabel = if (item.state.gameMode == GameMode.VS_BOT) "Vs Bot" else "Multiplayer"
    val difficulty = item.state.difficulty.name.lowercase().replaceFirstChar { char -> char.uppercase() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLoad)
            .semantics { contentDescription = "Load saved game ${item.name}" }
            .testTag("saved_game_row_${item.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniBoardPreview(
                    boardLayoutId = item.state.boardLayoutId,
                    boardThemeOption = item.boardTheme,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = item.name,
                        modifier = Modifier.testTag("saved_game_title_${item.id}"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF4E342E)
                    )
                    Text(
                        text = "${board.label} | ${item.state.matchMode.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF5E5E5E),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$gameModeLabel | ${item.state.players.size} players | $difficulty",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6C625A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Saved ${formatSavedAt(item.savedAt)} | Cell $cell",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7A7A7A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    modifier = Modifier.testTag("saved_game_delete_${item.id}"),
                    onClick = onRequestDelete
                ) {
                    DeleteGlyph()
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "Delete",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFB3261E)
                    )
                }
                Button(
                    modifier = Modifier
                        .heightIn(min = 34.dp)
                        .testTag("saved_game_resume_${item.id}"),
                    onClick = onLoad
                ) {
                    Text("Resume", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun DeleteGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(12.dp)) {
        val stroke = size.minDimension * 0.10f
        val lidTop = size.height * 0.24f
        val bodyTop = size.height * 0.34f
        drawLine(
            color = Color(0xFFB3261E),
            start = Offset(size.width * 0.20f, lidTop),
            end = Offset(size.width * 0.80f, lidTop),
            strokeWidth = stroke
        )
        drawLine(
            color = Color(0xFFB3261E),
            start = Offset(size.width * 0.40f, size.height * 0.16f),
            end = Offset(size.width * 0.60f, size.height * 0.16f),
            strokeWidth = stroke
        )
        drawRect(
            color = Color(0xFFB3261E),
            topLeft = Offset(size.width * 0.28f, bodyTop),
            size = Size(size.width * 0.44f, size.height * 0.44f),
            style = Stroke(width = stroke)
        )
        drawLine(
            color = Color(0xFFB3261E),
            start = Offset(size.width * 0.42f, size.height * 0.40f),
            end = Offset(size.width * 0.42f, size.height * 0.68f),
            strokeWidth = stroke
        )
        drawLine(
            color = Color(0xFFB3261E),
            start = Offset(size.width * 0.58f, size.height * 0.40f),
            end = Offset(size.width * 0.58f, size.height * 0.68f),
            strokeWidth = stroke
        )
    }
}

@Composable
private fun SavedGameDeleteConfirmationDialog(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete saved game?", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = "\"$name\" will be removed from this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5F5B55)
            )
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag("saved_game_delete_confirm_button"),
                onClick = onConfirm
            ) {
                Text("Delete", color = Color(0xFFB3261E), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.testTag("saved_game_delete_cancel_button"),
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ChipLabel(text: String) {
    val singleWord = text.none { it.isWhitespace() }
    Text(
        text = text,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        maxLines = if (singleWord) 1 else 2,
        softWrap = !singleWord,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ButtonLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        lineHeight = 15.sp,
        maxLines = 2,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
