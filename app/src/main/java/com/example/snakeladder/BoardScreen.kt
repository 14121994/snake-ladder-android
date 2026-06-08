package com.example.snakeladder

import android.content.Context
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private data class BoardBackdropStyle(
    val gradientColors: List<Color>,
    val accentA: Color,
    val accentB: Color,
    val accentC: Color,
    val pathColor: Color
)

private const val CELL_MOVE_FRAMES = 14
private const val CELL_MOVE_FRAME_DELAY_MS = 16L
private const val KNOCK_BACK_MOVE_FRAMES = CELL_MOVE_FRAMES / 2
private const val KNOCK_BACK_MOVE_FRAME_DELAY_MS = CELL_MOVE_FRAME_DELAY_MS / 4
private val inMatchDialogScrimColor = Color(0x990F1720)

private val vibrantBackdrop = BoardBackdropStyle(
    gradientColors = listOf(
        Color(0xFFFFF3DD),
        Color(0xFFEAF4FF),
        Color(0xFFF1FCEA)
    ),
    accentA = Color(0x33FFB74D),
    accentB = Color(0x332196F3),
    accentC = Color(0x334CAF50),
    pathColor = Color(0x22A1887F)
)

private val premiumMutedBackdrop = BoardBackdropStyle(
    gradientColors = listOf(
        Color(0xFFF6F2EB),
        Color(0xFFE9EEF4),
        Color(0xFFEFEDE7)
    ),
    accentA = Color(0x2FB39D82),
    accentB = Color(0x2F7F96AF),
    accentC = Color(0x2F8FA58A),
    pathColor = Color(0x22A49A8E)
)

private val festivalBackdrop = BoardBackdropStyle(
    gradientColors = listOf(
        Color(0xFFFFF4D6),
        Color(0xFFFFE6C7),
        Color(0xFFEAF7DB)
    ),
    accentA = Color(0x40FFB300),
    accentB = Color(0x33F4511E),
    accentC = Color(0x334CAF50),
    pathColor = Color(0x24C27A00)
)

private val monsoonBackdrop = BoardBackdropStyle(
    gradientColors = listOf(
        Color(0xFFE0F7FA),
        Color(0xFFE8F5E9),
        Color(0xFFDDEBF7)
    ),
    accentA = Color(0x3374B9C7),
    accentB = Color(0x334DB6AC),
    accentC = Color(0x335B8FC1),
    pathColor = Color(0x22557788)
)

private fun GameMode.displayName(): String {
    return when (this) {
        GameMode.LOCAL_MULTIPLAYER -> "Multiplayer"
        GameMode.VS_BOT -> "Vs Bot"
    }
}

private fun GameDifficulty.displayName(): String {
    return when (this) {
        GameDifficulty.EASY -> "Easy"
        GameDifficulty.MEDIUM -> "Medium"
        GameDifficulty.HARD -> "Hard"
    }
}

internal fun GameDifficulty.ruleSummary(): String {
    return when (this) {
        GameDifficulty.EASY -> "Classic rules. Landings never knock other players back."
        GameDifficulty.MEDIUM -> "Tactical rules. Normal landings knock rivals back to start."
        GameDifficulty.HARD -> "Pro rules. Normal, snake, and ladder landings can knock rivals back."
    }
}

private fun scaledDelayMillis(
    baseMillis: Long,
    fastAnimations: Boolean,
    reducedMotion: Boolean = false
): Long {
    val multiplier = when {
        reducedMotion -> 0.12f
        fastAnimations -> 0.45f
        else -> 1f
    }
    return (baseMillis * multiplier).roundToInt().coerceAtLeast(1).toLong()
}

private fun botAutoRollDelayMillis(personality: BotPersonality, pace: BotTurnPaceOption): Long {
    return (personality.autoRollDelayMs * pace.multiplier).roundToInt().coerceAtLeast(150).toLong()
}

private fun deviceHasVibrator(context: Context): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator.hasVibrator()
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.hasVibrator()
        }
    } catch (_: RuntimeException) {
        false
    }
}

private fun deviceHasAccelerometer(context: Context): Boolean {
    return try {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
    } catch (_: RuntimeException) {
        false
    }
}

private fun systemReducedMotionEnabled(context: Context): Boolean {
    return try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    } catch (_: RuntimeException) {
        false
    }
}

private fun suggestedSaveGameName(state: GameState, now: Long = System.currentTimeMillis()): String {
    val boardLabel = BoardLayouts.byId(state.boardLayoutId).label
    val timestamp = SimpleDateFormat("dd MMM hh:mm a", Locale.getDefault()).format(Date(now))
    return "$boardLabel ${state.matchMode.label} $timestamp"
}

private fun GameState.accessibilityTurnSummary(
    isPaused: Boolean,
    isRolling: Boolean,
    isMoveAnimating: Boolean
): String {
    val currentPlayer = players.getOrNull(currentPlayerIndex)
    val turnText = when {
        winnerIndex != null -> "${players.getOrNull(winnerIndex)?.name ?: "Player"} wins."
        isPaused -> "Paused."
        isRolling -> "Rolling dice."
        isMoveAnimating -> "Resolving move."
        currentPlayer != null -> "Current turn: ${currentPlayer.name}'s turn."
        else -> "Current turn pending."
    }
    val positionText = currentPlayer?.let { "${it.name} is on cell ${it.position}." }.orEmpty()
    val lastMoveText = lastMovePlayerIndex
        ?.let { players.getOrNull(it) }
        ?.let { "Last move: ${it.name} is now on cell ${it.position}." }
        .orEmpty()
    return listOf(turnText, positionText, lastMoveText)
        .filter { it.isNotBlank() }
        .joinToString(" ")
}

@Composable
internal fun SnakeLadderScreen(
    state: GameState,
    selectedBoardTheme: BoardThemeOption,
    onSelectBoardTheme: (BoardThemeOption) -> Unit,
    onStartNewGame: (GameDifficulty) -> Unit,
    onRollDice: () -> Unit,
    onUsePowerUp: (PowerUpType) -> Unit = {},
    onCancelArmedPowerUp: (PowerUpType) -> Unit = {},
    onRestart: () -> Unit,
    onSaveGame: (String) -> Boolean,
    onExit: () -> Unit,
    playerProfile: PlayerProfile = PlayerProfile(),
    dailyChallenge: DailyChallenge = DailyChallengeCatalog.today(),
    progressAlerts: List<String> = emptyList(),
    onDismissProgressAlert: () -> Unit = {}
) {
    var isRolling by rememberSaveable { mutableStateOf(false) }
    var isPaused by rememberSaveable { mutableStateOf(false) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showDifficultyDialog by rememberSaveable { mutableStateOf(false) }
    var showReplayDialog by rememberSaveable { mutableStateOf(false) }
    var showSaveGameDialog by rememberSaveable { mutableStateOf(false) }
    var saveGameName by rememberSaveable { mutableStateOf("") }
    var suggestedSaveName by rememberSaveable { mutableStateOf("") }
    var saveGameError by rememberSaveable { mutableStateOf<String?>(null) }
    var sfxVolume by rememberSaveable { mutableStateOf(1f) }
    var sfxVolumeBeforeMute by rememberSaveable { mutableStateOf(1f) }
    var vibrationEnabled by rememberSaveable { mutableStateOf(true) }
    var fastAnimations by rememberSaveable { mutableStateOf(false) }
    var reducedMotionEnabledByUser by rememberSaveable { mutableStateOf(false) }
    var highContrastBoard by rememberSaveable { mutableStateOf(false) }
    var selectedDiceSkin by rememberSaveable { mutableStateOf(DiceSkinOption.CLASSIC_RED) }
    var selectedTokenTrail by rememberSaveable { mutableStateOf(TokenTrailOption.NONE) }
    var selectedHapticTheme by rememberSaveable { mutableStateOf(HapticThemeOption.CLASSIC) }
    var selectedSoundtrack by rememberSaveable { mutableStateOf(SoundtrackOption.CLASSIC) }
    var selectedBotTurnPace by rememberSaveable { mutableStateOf(BotTurnPaceOption.STANDARD) }
    var manualBotRollConfirmation by rememberSaveable { mutableStateOf(false) }
    var shakeToRollEnabled by rememberSaveable { mutableStateOf(false) }
    var compactMatchUiEnabled by rememberSaveable { mutableStateOf(false) }
    var lastReaction by rememberSaveable { mutableStateOf<String?>(null) }
    var boardSettingsLoaded by rememberSaveable { mutableStateOf(false) }
    var isMoveAnimating by remember { mutableStateOf(false) }
    var lastBotAutoRollSignal by remember { mutableIntStateOf(-1) }
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    var showRestartDialog by rememberSaveable { mutableStateOf(false) }
    var previewDice by rememberSaveable { mutableIntStateOf(state.lastDiceRoll ?: 1) }
    var diceResultOverlay by remember { mutableStateOf<Int?>(null) }
    var ladderZoomStart by remember { mutableStateOf<Int?>(null) }
    var ladderZoomEnd by remember { mutableStateOf<Int?>(null) }
    var ladderZoomScale by remember { mutableStateOf(1f) }
    var ladderZoomAlpha by remember { mutableStateOf(1f) }
    var snakeZoomStart by remember { mutableStateOf<Int?>(null) }
    var snakeZoomEnd by remember { mutableStateOf<Int?>(null) }
    var snakeZoomScale by remember { mutableStateOf(1f) }
    var snakeZoomAlpha by remember { mutableStateOf(1f) }
    var boardFocusHighlight by remember { mutableStateOf<BoardFocusHighlight?>(null) }
    var boardViewportSize by remember { mutableStateOf(IntSize.Zero) }
    var boardViewportZoom by rememberSaveable { mutableStateOf(1f) }
    var boardViewportPanX by rememberSaveable { mutableStateOf(0f) }
    var boardViewportPanY by rememberSaveable { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val compactBoardUi = isLandscape || configuration.screenHeightDp <= 760
    val effectiveCompactUi = compactBoardUi || compactMatchUiEnabled
    val feedback = remember(context) { GameFeedback(context) }
    val deviceHasVibrator = remember(context) { deviceHasVibrator(context) }
    val deviceHasAccelerometer = remember(context) { deviceHasAccelerometer(context) }
    val systemReducedMotionEnabled = remember(context) { systemReducedMotionEnabled(context) }
    val reducedMotionEnabled = reducedMotionEnabledByUser || systemReducedMotionEnabled
    val displayPositions = remember { mutableStateListOf<Float>() }
    val tokenVisualOverrides = remember { mutableStateMapOf<Int, Offset?>() }
    val winnerName = state.winnerIndex?.let { winner ->
        state.players.getOrNull(winner)?.name
    }
    val activeDailyChallenge = dailyChallenge.takeIf { state.dailyChallengeId == it.id }
    val activeDailyBaseProgress = activeDailyChallenge?.let { playerProfile.progressFor(it) } ?: 0
    fun updateBoardZoom(nextZoom: Float) {
        boardViewportZoom = nextZoom.coerceIn(1f, 1.8f)
        if (boardViewportZoom <= 1.01f) {
            boardViewportPanX = 0f
            boardViewportPanY = 0f
        }
    }

    fun centerBoardOnCurrentTurn() {
        val currentCell = state.players.getOrNull(state.currentPlayerIndex)?.position ?: return
        val center = cellCenterNormalized(currentCell)
        if (boardViewportZoom < 1.4f) {
            boardViewportZoom = 1.4f
        }
        boardViewportPanX = ((0.5f - center.x) * 2f).coerceIn(-1f, 1f)
        boardViewportPanY = ((0.5f - center.y) * 2f).coerceIn(-1f, 1f)
        boardFocusHighlight = BoardFocusHighlight(
            cell = currentCell,
            label = "${state.players[state.currentPlayerIndex].name} centered on cell $currentCell"
        )
    }

    DisposableEffect(Unit) {
        onDispose { feedback.release() }
    }

    LaunchedEffect(Unit) {
        val savedSettings = BoardSettingsStore.load(context)
        sfxVolume = savedSettings.sfxVolume
        if (savedSettings.sfxVolume > 0.01f) {
            sfxVolumeBeforeMute = savedSettings.sfxVolume
        }
        vibrationEnabled = savedSettings.vibrationEnabled
        fastAnimations = savedSettings.fastAnimations
        reducedMotionEnabledByUser = savedSettings.reducedMotionEnabled
        selectedDiceSkin = savedSettings.diceSkin
        selectedTokenTrail = savedSettings.tokenTrail
        selectedHapticTheme = savedSettings.hapticTheme
        selectedSoundtrack = savedSettings.soundtrack
        highContrastBoard = savedSettings.highContrastBoard
        selectedBotTurnPace = savedSettings.botTurnPace
        manualBotRollConfirmation = savedSettings.manualBotRollConfirmation
        shakeToRollEnabled = savedSettings.shakeToRollEnabled
        compactMatchUiEnabled = savedSettings.compactMatchUiEnabled
        boardSettingsLoaded = true
    }

    LaunchedEffect(
        boardSettingsLoaded,
        sfxVolume,
        vibrationEnabled,
        fastAnimations,
        reducedMotionEnabledByUser,
        selectedDiceSkin,
        selectedTokenTrail,
        selectedHapticTheme,
        selectedSoundtrack,
        highContrastBoard,
        selectedBotTurnPace,
        manualBotRollConfirmation,
        shakeToRollEnabled,
        compactMatchUiEnabled
    ) {
        if (boardSettingsLoaded) {
            BoardSettingsStore.save(
                context,
                BoardSettingsSnapshot(
                    sfxVolume = sfxVolume,
                    vibrationEnabled = vibrationEnabled,
                    fastAnimations = fastAnimations,
                    reducedMotionEnabled = reducedMotionEnabledByUser,
                    diceSkin = selectedDiceSkin,
                    tokenTrail = selectedTokenTrail,
                    hapticTheme = selectedHapticTheme,
                    soundtrack = selectedSoundtrack,
                    highContrastBoard = highContrastBoard,
                    botTurnPace = selectedBotTurnPace,
                    manualBotRollConfirmation = manualBotRollConfirmation,
                    shakeToRollEnabled = shakeToRollEnabled,
                    compactMatchUiEnabled = compactMatchUiEnabled
                )
            )
        }
    }

    LaunchedEffect(sfxVolume, vibrationEnabled, selectedHapticTheme, selectedSoundtrack) {
        feedback.setMasterVolume(sfxVolume)
        feedback.setVibrationEnabled(vibrationEnabled)
        feedback.setHapticTheme(selectedHapticTheme)
        feedback.setSoundtrack(selectedSoundtrack)
    }

    LaunchedEffect(state.players.size) {
        displayPositions.clear()
        displayPositions.addAll(state.players.map { it.position.toFloat() })
        tokenVisualOverrides.clear()
        ladderZoomStart = null
        ladderZoomEnd = null
        ladderZoomScale = 1f
        ladderZoomAlpha = 1f
        snakeZoomStart = null
        snakeZoomEnd = null
        snakeZoomScale = 1f
        snakeZoomAlpha = 1f
        repeat(state.players.size) { index ->
            tokenVisualOverrides[index] = null
        }
    }

    LaunchedEffect(state.moveSignal, state.players.size) {
        if (state.moveSignal == 0) {
            displayPositions.clear()
            displayPositions.addAll(state.players.map { it.position.toFloat() })
            tokenVisualOverrides.clear()
            ladderZoomStart = null
            ladderZoomEnd = null
            ladderZoomScale = 1f
            ladderZoomAlpha = 1f
            snakeZoomStart = null
            snakeZoomEnd = null
            snakeZoomScale = 1f
            snakeZoomAlpha = 1f
            repeat(state.players.size) { index ->
                tokenVisualOverrides[index] = null
            }
            previewDice = 1
            boardFocusHighlight = null
        }
    }

    LaunchedEffect(state.lastDiceRoll, isRolling) {
        if (!isRolling && state.lastDiceRoll != null) {
            previewDice = state.lastDiceRoll
        }
    }

    LaunchedEffect(state.moveSignal, state.lastDiceRoll) {
        val roll = state.lastDiceRoll
        if (state.moveSignal != 0 && roll != null) {
            diceResultOverlay = roll
            delay(scaledDelayMillis(1500, fastAnimations, reducedMotionEnabled))
            diceResultOverlay = null
        }
    }

    LaunchedEffect(state.moveSignal) {
        if (state.moveSignal == 0) return@LaunchedEffect

        isMoveAnimating = true
        try {
            if (state.lastMoveType != MoveType.SNAKE && state.lastMoveType != MoveType.LADDER) {
                feedback.play(state.lastMoveType)
            }

            val moved = state.lastMovePlayerIndex ?: return@LaunchedEffect
            if (moved !in displayPositions.indices) return@LaunchedEffect

            if (reducedMotionEnabled) {
                if (state.lastMoveType == MoveType.SNAKE || state.lastMoveType == MoveType.LADDER) {
                    feedback.play(state.lastMoveType)
                }
                ladderZoomStart = null
                ladderZoomEnd = null
                ladderZoomScale = 1f
                ladderZoomAlpha = 1f
                snakeZoomStart = null
                snakeZoomEnd = null
                snakeZoomScale = 1f
                snakeZoomAlpha = 1f
                state.players.forEachIndexed { index, player ->
                    if (index in displayPositions.indices) {
                        displayPositions[index] = player.position.toFloat()
                        tokenVisualOverrides[index] = null
                    }
                }
                state.lastMovePlayerIndex
                    ?.let { movedIndex -> state.players.getOrNull(movedIndex) }
                    ?.let { player ->
                        boardFocusHighlight = BoardFocusHighlight(
                            cell = player.position,
                            label = "${player.name} landed on cell ${player.position}"
                        )
                    }
                return@LaunchedEffect
            }

            val path = state.lastMovePath
            if (path.isNotEmpty()) {
                val hasSpecialSegment = (state.lastMoveType == MoveType.LADDER || state.lastMoveType == MoveType.SNAKE) &&
                    path.size >= 2
                val walkTargets = if (hasSpecialSegment) path.dropLast(1) else path

                walkTargets.forEach { cell ->
                    val start = displayPositions[moved]
                    val end = cell.toFloat()
                    for (frame in 1..CELL_MOVE_FRAMES) {
                        val t = frame / CELL_MOVE_FRAMES.toFloat()
                        displayPositions[moved] = start + ((end - start) * t)
                        delay(scaledDelayMillis(CELL_MOVE_FRAME_DELAY_MS, fastAnimations, reducedMotionEnabled))
                    }
                }

                if (hasSpecialSegment) {
                    val startCell = walkTargets.last()
                    val endCell = path.last()
                    val geometryPath = if (state.lastMoveType == MoveType.LADDER) {
                        buildLadderAnimationPath(startCell, endCell)
                    } else {
                        buildSnakeAnimationPath(startCell, endCell)
                    }

                    if (state.lastMoveType == MoveType.LADDER) {
                        ladderZoomStart = startCell
                        ladderZoomEnd = endCell
                        val zoomFrames = 40
                        val zoomDelayMs = 30L
                        for (frame in 0..zoomFrames) {
                            val phase = frame / zoomFrames.toFloat()
                            ladderZoomScale = 1f + (phase * 3f) // 1x -> 4x
                            // While zooming, gradually reduce opacity before the final fade-out.
                            ladderZoomAlpha = 1f - (phase * 0.45f)
                            delay(scaledDelayMillis(zoomDelayMs, fastAnimations, reducedMotionEnabled))
                        }
                        val fadeFrames = 28
                        val fadeDelayMs = 28L
                        for (frame in 0..fadeFrames) {
                            val phase = frame / fadeFrames.toFloat()
                            ladderZoomScale = 4f
                            val startAlpha = 0.55f
                            ladderZoomAlpha = (startAlpha * (1f - phase)).coerceIn(0f, 1f)
                            delay(scaledDelayMillis(fadeDelayMs, fastAnimations, reducedMotionEnabled))
                        }
                        ladderZoomScale = 1f
                        ladderZoomAlpha = 1f
                        ladderZoomStart = null
                        ladderZoomEnd = null
                        // Trigger ladder feedback exactly when climb starts.
                        feedback.play(MoveType.LADDER)
                    }
                    if (state.lastMoveType == MoveType.SNAKE) {
                        snakeZoomStart = startCell
                        snakeZoomEnd = endCell
                        val zoomFrames = 30
                        val zoomDelayMs = 24L
                        for (frame in 0..zoomFrames) {
                            val phase = frame / zoomFrames.toFloat()
                            snakeZoomScale = 1f + (phase * 1.8f) // 1x -> 2.8x
                            snakeZoomAlpha = 1f - (phase * 0.45f)
                            delay(scaledDelayMillis(zoomDelayMs, fastAnimations, reducedMotionEnabled))
                        }
                        val fadeFrames = 24
                        val fadeDelayMs = 24L
                        for (frame in 0..fadeFrames) {
                            val phase = frame / fadeFrames.toFloat()
                            snakeZoomScale = 2.8f
                            val startAlpha = 0.55f
                            snakeZoomAlpha = (startAlpha * (1f - phase)).coerceIn(0f, 1f)
                            delay(scaledDelayMillis(fadeDelayMs, fastAnimations, reducedMotionEnabled))
                        }
                        snakeZoomScale = 1f
                        snakeZoomAlpha = 1f
                        snakeZoomStart = null
                        snakeZoomEnd = null
                        // Trigger hiss exactly when slide starts.
                        feedback.play(MoveType.SNAKE)
                    }

                    tokenVisualOverrides[moved] = cellCenterNormalized(startCell)
                    for (segment in 0 until geometryPath.lastIndex) {
                        val from = geometryPath[segment]
                        val to = geometryPath[segment + 1]
                        val frames = 14
                        val frameDelayMs = 22L
                        for (frame in 1..frames) {
                            val t = frame / frames.toFloat()
                            tokenVisualOverrides[moved] = Offset(
                                x = from.x + ((to.x - from.x) * t),
                                y = from.y + ((to.y - from.y) * t)
                            )
                            delay(scaledDelayMillis(frameDelayMs, fastAnimations, reducedMotionEnabled))
                        }
                    }
                    tokenVisualOverrides[moved] = null
                    displayPositions[moved] = endCell.toFloat()
                }
            } else {
                displayPositions[moved] = state.players[moved].position.toFloat()
            }

            val knockBackJobs = state.knockBackMoves
                .filter { move -> move.playerIndex in displayPositions.indices && move.path.isNotEmpty() }
                .map { move ->
                    launch {
                        move.path.forEach { cell ->
                            val start = displayPositions[move.playerIndex]
                            val end = cell.toFloat()
                            for (frame in 1..KNOCK_BACK_MOVE_FRAMES) {
                                val t = frame / KNOCK_BACK_MOVE_FRAMES.toFloat()
                                displayPositions[move.playerIndex] = start + ((end - start) * t)
                                delay(scaledDelayMillis(KNOCK_BACK_MOVE_FRAME_DELAY_MS, fastAnimations, reducedMotionEnabled))
                            }
                        }
                        displayPositions[move.playerIndex] = state.players[move.playerIndex].position.toFloat()
                        tokenVisualOverrides[move.playerIndex] = null
                    }
                }
            knockBackJobs.forEach { it.join() }

            state.players.forEachIndexed { index, player ->
                if (index in displayPositions.indices) {
                    displayPositions[index] = player.position.toFloat()
                    tokenVisualOverrides[index] = null
                }
            }
            state.lastMovePlayerIndex
                ?.let { movedIndex -> state.players.getOrNull(movedIndex) }
                ?.let { player ->
                    boardFocusHighlight = BoardFocusHighlight(
                        cell = player.position,
                        label = "${player.name} landed on cell ${player.position}"
                    )
                }
        } finally {
            isMoveAnimating = false
        }
    }

    suspend fun triggerAnimatedRoll() {
        if (isRolling || isMoveAnimating || isPaused || state.winnerIndex != null) return
        isRolling = true
        try {
            feedback.startDiceRollingSfx()
            val tickIntervalMs = 90L
            val rollDurationMs = 600L
            val steps = if (reducedMotionEnabled) {
                1
            } else {
                (rollDurationMs / tickIntervalMs).toInt().coerceAtLeast(1)
            }
            repeat(steps) {
                delay(scaledDelayMillis(tickIntervalMs, fastAnimations, reducedMotionEnabled))
            }
            feedback.stopDiceRollingSfx()
            feedback.confirmRoll()
            onRollDice()
            previewDice = state.lastDiceRoll ?: previewDice
        } finally {
            feedback.stopDiceRollingSfx()
            isRolling = false
        }
    }

    LaunchedEffect(
        state.currentPlayerIndex,
        state.winnerIndex,
        state.gameMode,
        state.bonusTurnGranted,
        selectedBotTurnPace,
        manualBotRollConfirmation,
        isRolling,
        isMoveAnimating,
        reducedMotionEnabled,
        showSettingsDialog,
        showExitDialog,
        showSaveGameDialog,
        showRestartDialog,
        state.moveSignal
    ) {
        val isBotTurn = state.botPlayerIndex == state.currentPlayerIndex && state.winnerIndex == null
        val currentSignal = state.moveSignal
        val shouldAutoRoll = state.gameMode == GameMode.VS_BOT &&
            isBotTurn &&
            !manualBotRollConfirmation &&
            !isPaused &&
            !isRolling &&
            !isMoveAnimating &&
            !showSettingsDialog &&
            !showExitDialog &&
            !showSaveGameDialog &&
            !showRestartDialog &&
            lastBotAutoRollSignal != currentSignal

        if (shouldAutoRoll) {
            lastBotAutoRollSignal = currentSignal
            delay(
                scaledDelayMillis(
                    botAutoRollDelayMillis(state.botPersonality, selectedBotTurnPace),
                    fastAnimations,
                    reducedMotionEnabled
                )
            )
            if (!isRolling && !isMoveAnimating && state.moveSignal == currentSignal) {
                scope.launch { triggerAnimatedRoll() }
            }
        }
    }

    val controlSection: @Composable () -> Unit = {
        ControlCard(
            state = state,
            previewDice = previewDice,
            isRolling = isRolling,
            isMoveAnimating = isMoveAnimating,
            compact = effectiveCompactUi,
            isPaused = isPaused,
            diceSkin = selectedDiceSkin,
            dailyChallenge = activeDailyChallenge,
            dailyChallengeBaseProgress = activeDailyBaseProgress,
            manualBotRollConfirmation = manualBotRollConfirmation,
            botTurnPace = selectedBotTurnPace,
            shakeToRollEnabled = shakeToRollEnabled && deviceHasAccelerometer,
            isMuted = sfxVolume <= 0.01f,
            onToggleMute = {
                if (sfxVolume <= 0.01f) {
                    sfxVolume = sfxVolumeBeforeMute.coerceIn(0.25f, 1f)
                } else {
                    sfxVolumeBeforeMute = sfxVolume.coerceIn(0.25f, 1f)
                    sfxVolume = 0f
                }
            },
            onUsePowerUp = onUsePowerUp,
            onCancelArmedPowerUp = onCancelArmedPowerUp,
            onLocatePlayer = { index ->
                state.players.getOrNull(index)?.let { player ->
                    boardFocusHighlight = BoardFocusHighlight(
                        cell = player.position,
                        label = "${player.name} is on cell ${player.position}"
                    )
                }
            },
            lastReaction = lastReaction,
            onReaction = { reaction ->
                val playerName = state.players.getOrNull(state.currentPlayerIndex)?.name ?: "Player"
                lastReaction = "$playerName: ${reaction.label}"
            },
            onRollDice = {
                if (isPaused) return@ControlCard
                if (state.botPlayerIndex == state.currentPlayerIndex && !manualBotRollConfirmation) return@ControlCard
                scope.launch { triggerAnimatedRoll() }
            }
        )
    }

    val boardSection: @Composable (Modifier) -> Unit = { modifier ->
        Box(
            modifier = modifier
                .clipToBounds()
                .onSizeChanged { boardViewportSize = it }
                .testTag("board_viewport"),
            contentAlignment = Alignment.TopCenter
        ) {
            val maxPanX = (boardViewportSize.width * (boardViewportZoom - 1f)) / 2f
            val maxPanY = (boardViewportSize.height * (boardViewportZoom - 1f)) / 2f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = boardViewportZoom
                        scaleY = boardViewportZoom
                        translationX = boardViewportPanX * maxPanX
                        translationY = boardViewportPanY * maxPanY
                    }
                    .testTag("board_zoom_layer")
            ) {
                Board(
                    state = state,
                    displayPositions = displayPositions,
                    tokenVisualOverrides = tokenVisualOverrides,
                    ladderZoomEffect = if (ladderZoomStart != null && ladderZoomEnd != null) {
                        LadderZoomEffect(
                            start = ladderZoomStart ?: 1,
                            end = ladderZoomEnd ?: 1,
                            scale = ladderZoomScale,
                            alpha = ladderZoomAlpha
                        )
                    } else {
                        null
                    },
                    snakeZoomEffect = if (snakeZoomStart != null && snakeZoomEnd != null) {
                        SnakeZoomEffect(
                            start = snakeZoomStart ?: 1,
                            end = snakeZoomEnd ?: 1,
                            scale = snakeZoomScale,
                            alpha = snakeZoomAlpha
                        )
                    } else {
                        null
                    },
                    boardThemeOption = selectedBoardTheme,
                    tokenTrail = selectedTokenTrail,
                    highContrastBoard = highContrastBoard,
                    focusHighlight = boardFocusHighlight,
                    onInspectRoute = { isLadder, start, end ->
                        boardFocusHighlight = BoardFocusHighlight(
                            cell = start,
                            secondaryCell = end,
                            label = if (isLadder) {
                                "Ladder $start climbs to $end"
                            } else {
                                "Snake $start slides to $end"
                            }
                        )
                    }
                )
            }
            BoardViewportControls(
                zoom = boardViewportZoom,
                onZoomOut = { updateBoardZoom(boardViewportZoom - 0.2f) },
                onCenterTurn = { centerBoardOnCurrentTurn() },
                onZoomIn = { updateBoardZoom(boardViewportZoom + 0.2f) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            )
        }
    }

    val showTopActionButtons = winnerName == null &&
        !showExitDialog &&
        !showSaveGameDialog

    val floatingRollEnabled = winnerName == null &&
        !isPaused &&
        !isRolling &&
        !isMoveAnimating &&
        (state.botPlayerIndex != state.currentPlayerIndex || manualBotRollConfirmation) &&
        !showSettingsDialog &&
        !showExitDialog &&
        !showSaveGameDialog &&
        !showRestartDialog

    val accessibilityPowerUp = state.powerUpInventories.getOrNull(state.currentPlayerIndex).orEmpty().firstOrNull()
    val accessibilityCanUsePowerUp = accessibilityPowerUp != null &&
        RuleSets.byId(state.ruleSetId).usesPowerUps &&
        winnerName == null &&
        !isPaused &&
        !isRolling &&
        !isMoveAnimating &&
        state.botPlayerIndex != state.currentPlayerIndex &&
        !showSettingsDialog &&
        !showExitDialog &&
        !showSaveGameDialog &&
        !showRestartDialog

    val accessibilityActions = buildList {
        if (floatingRollEnabled) {
            add(
                CustomAccessibilityAction("Roll dice") {
                    scope.launch { triggerAnimatedRoll() }
                    true
                }
            )
        }
        if (showTopActionButtons && !showSettingsDialog && !showRestartDialog) {
            add(
                CustomAccessibilityAction("Open settings") {
                    showSettingsDialog = true
                    true
                }
            )
        }
        accessibilityPowerUp?.takeIf { accessibilityCanUsePowerUp }?.let { powerUp ->
            add(
                CustomAccessibilityAction("Use ${powerUp.label} power-up") {
                    onUsePowerUp(powerUp)
                    true
                }
            )
        }
    }

    DisposableEffect(shakeToRollEnabled, floatingRollEnabled, deviceHasAccelerometer, context) {
        if (!shakeToRollEnabled || !floatingRollEnabled || !deviceHasAccelerometer) {
            return@DisposableEffect onDispose {}
        }
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: return@DisposableEffect onDispose {}
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?: return@DisposableEffect onDispose {}
        var lastShakeAt = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.values.size < 3) return
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val acceleration = sqrt((x * x) + (y * y) + (z * z))
                val now = System.currentTimeMillis()
                if (acceleration >= 18f && now - lastShakeAt >= 900L) {
                    lastShakeAt = now
                    scope.launch { triggerAnimatedRoll() }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription = state.accessibilityTurnSummary(
                    isPaused = isPaused,
                    isRolling = isRolling,
                    isMoveAnimating = isMoveAnimating
                )
                customActions = accessibilityActions
            }
            .testTag("board_accessibility_hub")
    ) {
        BoardScreenBackground(
            themeOption = selectedBoardTheme,
            reducedMotion = reducedMotionEnabled
        )

        if (isLandscape) {
            val controlWeight = if (effectiveCompactUi || configuration.screenHeightDp <= 430) 0.45f else 0.40f
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (compactBoardUi) 10.dp else 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(controlWeight)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    controlSection()
                }
                Column(
                    modifier = Modifier
                        .weight(1f - controlWeight)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Top
                ) {
                    boardSection(Modifier.fillMaxSize())
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (effectiveCompactUi) 10.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(if (effectiveCompactUi) 7.dp else 8.dp)
            ) {
                controlSection()
                boardSection(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
        }

        diceResultOverlay?.let { roll ->
            DiceResultOverlay(
                roll = roll,
                modifier = Modifier
                    .align(Alignment.Center)
                    .testTag("dice_result_overlay")
            )
        }

        if (winnerName != null) {
            WinnerCelebrationOverlay(
                winnerName = winnerName,
                analytics = MatchAnalytics.from(state),
                onNewGame = {
                    isRolling = false
                    isPaused = false
                    showDifficultyDialog = true
                },
                onExit = {
                    isRolling = false
                    isPaused = false
                    onExit()
                },
                onReplay = {
                    showReplayDialog = true
                },
                onRematch = {
                    isRolling = false
                    isPaused = false
                    onRestart()
                },
                reducedMotion = reducedMotionEnabled,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isPaused && winnerName == null) {
            PauseOverlay(
                onResume = { isPaused = false },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (showExitDialog) {
            ExitConfirmationDialog(
                onCancel = { showExitDialog = false },
                onSaveFirst = {
                    showExitDialog = false
                    suggestedSaveName = suggestedSaveGameName(state)
                    saveGameName = suggestedSaveName
                    saveGameError = null
                    showSaveGameDialog = true
                },
                onConfirmExit = {
                    showExitDialog = false
                    isRolling = false
                    isPaused = false
                    onExit()
                }
            )
        }

        if (showRestartDialog) {
            RestartConfirmationDialog(
                onCancel = { showRestartDialog = false },
                onConfirmRestart = {
                    showRestartDialog = false
                    isRolling = false
                    isPaused = false
                    onRestart()
                }
            )
        }

        if (showTopActionButtons) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = if (isLandscape) 12.dp else 18.dp, end = if (isLandscape) 12.dp else 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TopOverlayActionButton(
                    icon = if (isPaused) TopOverlayIcon.RESUME else TopOverlayIcon.PAUSE,
                    contentDescription = if (isPaused) "Resume" else "Pause",
                    testTag = "pause_button",
                    onClick = { isPaused = !isPaused }
                )
                TopOverlayActionButton(
                    icon = TopOverlayIcon.SETTINGS,
                    contentDescription = "Settings",
                    testTag = "settings_button",
                    onClick = { showSettingsDialog = true }
                )
            }
        }

        if (floatingRollEnabled) {
            Button(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 18.dp, bottom = 22.dp)
                    .heightIn(min = 48.dp)
                    .testTag("floating_roll_button"),
                shape = RoundedCornerShape(999.dp),
                onClick = { scope.launch { triggerAnimatedRoll() } }
            ) {
                Text("Roll", fontWeight = FontWeight.Bold)
            }
        }

        if (showSettingsDialog) {
            SettingsDialog(
                state = state,
                selectedBoardTheme = selectedBoardTheme,
                onSelectBoardTheme = onSelectBoardTheme,
                selectedDiceSkin = selectedDiceSkin,
                onSelectDiceSkin = { selectedDiceSkin = it },
                selectedTokenTrail = selectedTokenTrail,
                onSelectTokenTrail = { selectedTokenTrail = it },
                musicVolume = sfxVolume,
                onMusicVolumeChanged = { sfxVolume = it },
                vibrationEnabled = vibrationEnabled,
                onVibrationChanged = { vibrationEnabled = it },
                deviceHasVibrator = deviceHasVibrator,
                selectedHapticTheme = selectedHapticTheme,
                onSelectHapticTheme = { selectedHapticTheme = it },
                selectedSoundtrack = selectedSoundtrack,
                onSelectSoundtrack = {
                    selectedSoundtrack = it
                    feedback.previewSoundtrack(it)
                },
                fastAnimations = fastAnimations,
                onFastAnimationsChanged = { fastAnimations = it },
                reducedMotionEnabled = reducedMotionEnabled,
                systemReducedMotionEnabled = systemReducedMotionEnabled,
                onReducedMotionChanged = { reducedMotionEnabledByUser = it },
                selectedBotTurnPace = selectedBotTurnPace,
                onSelectBotTurnPace = { selectedBotTurnPace = it },
                manualBotRollConfirmation = manualBotRollConfirmation,
                onManualBotRollConfirmationChanged = { manualBotRollConfirmation = it },
                shakeToRollEnabled = shakeToRollEnabled,
                onShakeToRollChanged = { shakeToRollEnabled = it },
                deviceHasAccelerometer = deviceHasAccelerometer,
                compactMatchUiEnabled = compactMatchUiEnabled,
                onCompactMatchUiChanged = { compactMatchUiEnabled = it },
                highContrastBoard = highContrastBoard,
                onHighContrastBoardChanged = { highContrastBoard = it },
                onNewGame = {
                    isRolling = false
                    isPaused = false
                    showDifficultyDialog = true
                    showSettingsDialog = false
                },
                onRestart = {
                    isRolling = false
                    isPaused = false
                    showRestartDialog = true
                    showSettingsDialog = false
                },
                onSaveGame = {
                    showSettingsDialog = false
                    suggestedSaveName = suggestedSaveGameName(state)
                    saveGameName = suggestedSaveName
                    saveGameError = null
                    showSaveGameDialog = true
                },
                onTogglePause = { isPaused = !isPaused },
                isPaused = isPaused,
                onShowReplay = {
                    showSettingsDialog = false
                    showReplayDialog = true
                },
                replayEnabled = state.matchEvents.isNotEmpty(),
                onExit = {
                    showSettingsDialog = false
                    showExitDialog = true
                },
                onDismiss = { showSettingsDialog = false }
            )
        }

        if (showDifficultyDialog) {
            DifficultyDialog(
                onSelectDifficulty = { difficulty ->
                    showDifficultyDialog = false
                    isRolling = false
                    isPaused = false
                    onStartNewGame(difficulty)
                },
                onDismiss = { showDifficultyDialog = false }
            )
        }

        if (showReplayDialog) {
            ReplayDialog(
                events = state.matchEvents,
                onDismiss = { showReplayDialog = false }
            )
        }

        if (progressAlerts.isNotEmpty()) {
            ProgressAlertDialog(
                message = progressAlerts.first(),
                onDismiss = onDismissProgressAlert
            )
        }

        if (showSaveGameDialog) {
            val typedSaveName = saveGameName.trim()
            val resolvedSaveName = typedSaveName.ifBlank { suggestedSaveName }
            AlertDialog(
                onDismissRequest = {
                    saveGameName = ""
                    suggestedSaveName = ""
                    saveGameError = null
                    showSaveGameDialog = false
                },
                title = { Text("Save Game", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Save now with the suggested name or rename it before confirming.")
                        OutlinedTextField(
                            value = saveGameName,
                            onValueChange = {
                                saveGameName = it
                                saveGameError = null
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_game_name_input"),
                            placeholder = { Text(if (suggestedSaveName.isBlank()) "e.g. Weekend Match" else suggestedSaveName) },
                            supportingText = {
                                Text(
                                    text = if (typedSaveName.isBlank()) {
                                        "Using suggested name: $resolvedSaveName"
                                    } else {
                                        "Reusing the same name updates that save."
                                    },
                                    color = if (typedSaveName.isBlank()) Color(0xFF1565C0) else Color(0xFF6D6259)
                                )
                            }
                        )
                        saveGameError?.let { error ->
                            Text(
                                text = error,
                                modifier = Modifier.testTag("save_game_status"),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB3261E)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        modifier = Modifier.testTag("save_game_confirm_button"),
                        onClick = {
                            if (onSaveGame(resolvedSaveName)) {
                                Toast.makeText(context, "Saved \"$resolvedSaveName\"", Toast.LENGTH_SHORT).show()
                                saveGameName = ""
                                suggestedSaveName = ""
                                saveGameError = null
                                showSaveGameDialog = false
                            } else {
                                saveGameError = "Save failed. Check device storage and try again."
                            }
                        },
                        enabled = resolvedSaveName.isNotBlank()
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(
                        modifier = Modifier.testTag("save_game_cancel_button"),
                        onClick = {
                            saveGameName = ""
                            suggestedSaveName = ""
                            saveGameError = null
                            showSaveGameDialog = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsDialog(
    state: GameState,
    selectedBoardTheme: BoardThemeOption,
    onSelectBoardTheme: (BoardThemeOption) -> Unit,
    selectedDiceSkin: DiceSkinOption,
    onSelectDiceSkin: (DiceSkinOption) -> Unit,
    selectedTokenTrail: TokenTrailOption,
    onSelectTokenTrail: (TokenTrailOption) -> Unit,
    musicVolume: Float,
    onMusicVolumeChanged: (Float) -> Unit,
    vibrationEnabled: Boolean,
    onVibrationChanged: (Boolean) -> Unit,
    deviceHasVibrator: Boolean,
    selectedHapticTheme: HapticThemeOption,
    onSelectHapticTheme: (HapticThemeOption) -> Unit,
    selectedSoundtrack: SoundtrackOption,
    onSelectSoundtrack: (SoundtrackOption) -> Unit,
    fastAnimations: Boolean,
    onFastAnimationsChanged: (Boolean) -> Unit,
    reducedMotionEnabled: Boolean,
    systemReducedMotionEnabled: Boolean,
    onReducedMotionChanged: (Boolean) -> Unit,
    selectedBotTurnPace: BotTurnPaceOption,
    onSelectBotTurnPace: (BotTurnPaceOption) -> Unit,
    manualBotRollConfirmation: Boolean,
    onManualBotRollConfirmationChanged: (Boolean) -> Unit,
    shakeToRollEnabled: Boolean,
    onShakeToRollChanged: (Boolean) -> Unit,
    deviceHasAccelerometer: Boolean,
    compactMatchUiEnabled: Boolean,
    onCompactMatchUiChanged: (Boolean) -> Unit,
    highContrastBoard: Boolean,
    onHighContrastBoardChanged: (Boolean) -> Unit,
    onNewGame: () -> Unit,
    onRestart: () -> Unit,
    onSaveGame: () -> Unit,
    onTogglePause: () -> Unit,
    isPaused: Boolean,
    onShowReplay: () -> Unit,
    replayEnabled: Boolean,
    onExit: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(SettingsDialogTab.MATCH) }
    val settingsScrollState = rememberScrollState()

    LaunchedEffect(selectedTab) {
        settingsScrollState.scrollTo(0)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4E342E)
                )
                Text(
                    text = selectedTab.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6D6259)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 580.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SettingsTabPicker(
                        selectedTab = selectedTab,
                        onSelectTab = { selectedTab = it }
                    )
                    SettingsTabGuidanceCard(selectedTab = selectedTab)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                    ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(settingsScrollState)
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (selectedTab) {
                            SettingsDialogTab.MATCH -> {
                                SettingsSectionPanel(
                                    title = "Match Controls",
                                    subtitle = "Save and exit stay separated from cosmetic settings."
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("settings_new_game_button"),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                            onClick = onNewGame
                                        ) { SettingsOptionButtonLabel("New Game") }
                                        Button(
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("settings_save_game_button"),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                            onClick = onSaveGame
                                        ) { SettingsOptionButtonLabel("Save Game") }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("settings_restart_button"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFE7E0EC),
                                                contentColor = Color(0xFF1D1B20)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                            onClick = onRestart
                                        ) { SettingsOptionButtonLabel("Restart") }
                                        Button(
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("settings_pause_resume_button"),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                            onClick = onTogglePause
                                        ) { SettingsOptionButtonLabel(if (isPaused) "Resume" else "Pause") }
                                    }
                                    Button(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("settings_replay_button"),
                                        enabled = replayEnabled,
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                        onClick = onShowReplay
                                    ) { SettingsOptionButtonLabel("Replay") }
                                    Button(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("settings_exit_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFB3261E),
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                        onClick = onExit
                                    ) { SettingsOptionButtonLabel("Exit To Launch") }
                                }

                                SettingsSectionPanel(
                                    title = "Current Match",
                                    subtitle = "${state.gameMode.displayName()} | ${state.matchMode.label} | ${state.difficulty.displayName()}"
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFFFF8E7))
                                            .border(1.dp, Color(0xFFE2C99F), RoundedCornerShape(12.dp))
                                            .padding(10.dp)
                                            .testTag("settings_match_summary")
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = state.difficulty.ruleSummary(),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF66564E)
                                            )
                                            Text(
                                                text = "${BoardLayouts.byId(state.boardLayoutId).label} | ${RuleSets.byId(state.ruleSetId).label}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF66564E)
                                            )
                                            Text(
                                                text = PowerUpRuleEngine.describe(RuleSets.byId(state.ruleSetId)),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF66564E)
                                            )
                                            RulesReferencePanel(state = state)
                                            if (state.turnsRemaining != null) {
                                                Text(
                                                    text = "Turns left: ${state.turnsRemaining}/${state.turnLimit ?: state.turnsRemaining}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF8A3D00)
                                                )
                                            }
                                            if (state.activeTraps.isNotEmpty()) {
                                                Text(
                                                    text = "Active traps: ${state.activeTraps.joinToString { it.cell.toString() }}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF8B1E3F)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            SettingsDialogTab.VISUAL -> {
                                SettingsSectionPanel(
                                    title = "Board Theme",
                                    subtitle = "Preview every board theme before applying it."
                                ) {
                                    BoardThemeOption.entries.chunked(2).forEach { row ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            row.forEach { theme ->
                                                BoardThemeOptionCard(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("settings_theme_${theme.name.lowercase()}_chip"),
                                                    theme = theme,
                                                    selected = selectedBoardTheme == theme,
                                                    onClick = { onSelectBoardTheme(theme) }
                                                )
                                            }
                                            if (row.size == 1) {
                                                Box(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                                SettingsSectionPanel(
                                    title = "Dice And Trails",
                                    subtitle = "Check every die and token trail before locking it in."
                                ) {
                                    SettingsToggleRow(
                                        iconLabel = "HC",
                                        title = "High contrast board",
                                        subtitle = "Boosts board number, token, snake, ladder, and trap contrast.",
                                        checked = highContrastBoard,
                                        testTag = "settings_high_contrast_board_switch",
                                        onCheckedChange = onHighContrastBoardChanged
                                    )
                                    Text("Dice Skin", fontWeight = FontWeight.SemiBold)
                                    DiceSkinOption.entries.chunked(2).forEach { row ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            row.forEach { skin ->
                                                DiceSkinOptionCard(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("settings_dice_${skin.label.lowercase().replace(' ', '_')}"),
                                                    skin = skin,
                                                    selected = selectedDiceSkin == skin,
                                                    onClick = { onSelectDiceSkin(skin) }
                                                )
                                            }
                                            if (row.size == 1) {
                                                Box(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                    Text("Token Trail", fontWeight = FontWeight.SemiBold)
                                    TokenTrailOption.entries.chunked(3).forEach { row ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            row.forEach { option ->
                                                TokenTrailOptionCard(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("settings_trail_${option.label.lowercase().replace(' ', '_')}"),
                                                    option = option,
                                                    selected = selectedTokenTrail == option,
                                                    onClick = { onSelectTokenTrail(option) }
                                                )
                                            }
                                            repeat(3 - row.size) {
                                                Box(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }

                            SettingsDialogTab.AUDIO -> {
                                SettingsSectionPanel(
                                    title = "Audio",
                                    subtitle = "Keep soundtrack and effects separate from match actions."
                                ) {
                                    Text("Soundtrack", fontWeight = FontWeight.SemiBold)
                                    SettingsEnumChips(
                                        values = SoundtrackOption.entries,
                                        selected = selectedSoundtrack,
                                        label = { it.label },
                                        tagPrefix = "settings_soundtrack",
                                        columns = 3,
                                        onSelect = onSelectSoundtrack
                                    )
                                    Text(
                                        text = "Selecting a soundtrack previews its cue immediately.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF6D6259),
                                        modifier = Modifier.testTag("settings_soundtrack_preview_hint")
                                    )
                                    Text("SFX Volume", fontWeight = FontWeight.SemiBold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Slider(
                                            value = musicVolume,
                                            onValueChange = onMusicVolumeChanged,
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 56.dp)
                                                .testTag("settings_music_slider"),
                                            valueRange = 0f..1f
                                        )
                                        Text("${(musicVolume * 100).roundToInt()}%")
                                    }
                                }
                            }

                            SettingsDialogTab.CONTROLS -> {
                                SettingsSectionPanel(
                                    title = "Controls",
                                    subtitle = "Tune motion and physical feedback without changing audio."
                                ) {
                                    SettingsToggleRow(
                                        iconLabel = "RM",
                                        title = "Reduced motion",
                                        subtitle = if (systemReducedMotionEnabled) {
                                            "Enabled by Android animation settings. Skips route zooms and shortens dice, move, and bot waits."
                                        } else {
                                            "Skips route zooms and shortens dice, move, and bot waits."
                                        },
                                        checked = reducedMotionEnabled,
                                        testTag = "settings_reduced_motion_switch",
                                        enabled = !systemReducedMotionEnabled,
                                        onCheckedChange = onReducedMotionChanged
                                    )
                                    Text(
                                        text = if (systemReducedMotionEnabled) {
                                            "Android animation scale is off; reduced motion stays active."
                                        } else if (reducedMotionEnabled) {
                                            "Reduced motion is on for this game."
                                        } else {
                                            "Reduced motion follows this switch unless Android animations are disabled."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (reducedMotionEnabled) Color(0xFF2E6B3D) else Color(0xFF6D6259),
                                        modifier = Modifier.testTag("settings_reduced_motion_status")
                                    )
                                    SettingsToggleRow(
                                        iconLabel = "FF",
                                        title = "Fast animations",
                                        subtitle = "Shortens movement and bot-turn waits for quicker repeat play.",
                                        checked = fastAnimations,
                                        testTag = "settings_fast_animation_switch",
                                        onCheckedChange = onFastAnimationsChanged
                                    )
                                    SettingsToggleRow(
                                        iconLabel = "CM",
                                        title = "Compact match UI",
                                        subtitle = "Keeps the control card shorter by hiding reactions and mission extras.",
                                        checked = compactMatchUiEnabled,
                                        testTag = "settings_compact_match_ui_switch",
                                        onCheckedChange = onCompactMatchUiChanged
                                    )
                                    Text(
                                        text = if (compactMatchUiEnabled) {
                                            "Compact match UI is on."
                                        } else {
                                            "Compact match UI is off."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (compactMatchUiEnabled) Color(0xFF2E6B3D) else Color(0xFF6D6259),
                                        modifier = Modifier.testTag("settings_compact_match_ui_status")
                                    )
                                    SettingsToggleRow(
                                        iconLabel = "SH",
                                        title = "Shake to roll",
                                        subtitle = if (deviceHasAccelerometer) {
                                            "Shake the device on your turn to trigger the same roll as tapping dice."
                                        } else {
                                            "This device does not report a motion sensor."
                                        },
                                        checked = shakeToRollEnabled && deviceHasAccelerometer,
                                        testTag = "settings_shake_to_roll_switch",
                                        enabled = deviceHasAccelerometer,
                                        onCheckedChange = { if (deviceHasAccelerometer) onShakeToRollChanged(it) }
                                    )
                                    Text(
                                        text = "Shake support: ${if (deviceHasAccelerometer) "Available" else "Unavailable"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (deviceHasAccelerometer) Color(0xFF2E6B3D) else Color(0xFF8A3D00),
                                        modifier = Modifier.testTag("settings_shake_to_roll_availability")
                                    )
                                    Text("Bot Turn Pace", fontWeight = FontWeight.SemiBold)
                                    SettingsEnumChips(
                                        values = BotTurnPaceOption.entries,
                                        selected = selectedBotTurnPace,
                                        label = { it.label },
                                        tagPrefix = "settings_bot_pace",
                                        columns = 2,
                                        onSelect = onSelectBotTurnPace
                                    )
                                    Text(
                                        text = selectedBotTurnPace.supportText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF6D6259),
                                        modifier = Modifier.testTag("settings_bot_pace_summary")
                                    )
                                    SettingsToggleRow(
                                        iconLabel = "BT",
                                        title = "Confirm bot rolls",
                                        subtitle = "Stops automatic bot rolls and lets you tap Roll when the bot is ready.",
                                        checked = manualBotRollConfirmation,
                                        testTag = "settings_manual_bot_roll_switch",
                                        onCheckedChange = onManualBotRollConfirmationChanged
                                    )
                                    SettingsToggleRow(
                                        iconLabel = "VB",
                                        title = if (deviceHasVibrator) "Vibration" else "Vibration unavailable",
                                        subtitle = if (deviceHasVibrator) {
                                            "Uses short haptic taps for rolls, ladders, snakes, and wins."
                                        } else {
                                            "This device does not report a vibration motor."
                                        },
                                        checked = vibrationEnabled && deviceHasVibrator,
                                        testTag = "settings_vibration_switch",
                                        enabled = deviceHasVibrator,
                                        onCheckedChange = { if (deviceHasVibrator) onVibrationChanged(it) }
                                    )
                                    Text(
                                        text = "Device vibration: ${if (deviceHasVibrator) "Available" else "Unavailable"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (deviceHasVibrator) Color(0xFF2E6B3D) else Color(0xFF8A3D00),
                                        modifier = Modifier.testTag("settings_vibration_availability")
                                    )
                                    Text("Haptic Theme", fontWeight = FontWeight.SemiBold)
                                    SettingsEnumChips(
                                        values = HapticThemeOption.entries,
                                        selected = selectedHapticTheme,
                                        label = { it.label },
                                        tagPrefix = "settings_haptic",
                                        columns = 3,
                                        onSelect = onSelectHapticTheme
                                    )
                                }
                            }
                        }
                    }
                    if (settingsScrollState.maxValue > 0 && settingsScrollState.value < settingsScrollState.maxValue) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(34.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color(0xFFFDF8F1))
                                    )
                                )
                                .testTag("settings_scroll_fade")
                        )
                    }
                    if (settingsScrollState.maxValue > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(4.dp)
                                .height(72.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFFB7A99A))
                                .testTag("settings_scroll_indicator")
                        )
                    }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFDF8F1))
                        .padding(top = 10.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TextButton(
                        modifier = Modifier.testTag("settings_done_button"),
                        onClick = onDismiss
                    ) {
                        Text("Done")
                    }
                }
            }
        },
        confirmButton = {}
    )
}

private enum class SettingsDialogTab(
    val label: String,
    val summary: String,
    val guidance: String
) {
    MATCH(
        label = "Match",
        summary = "Match actions, current rules, and exit choices.",
        guidance = "Save, restart, replay, pause, and exit stay with the current match rules."
    ),
    VISUAL(
        label = "Visual",
        summary = "Board theme, dice skin, token trails, and contrast.",
        guidance = "Visual choices preview board style, dice, trails, and contrast separately from match actions."
    ),
    AUDIO(
        label = "Audio",
        summary = "Soundtrack, effect volume, and cue preview.",
        guidance = "Audio choices keep soundtrack previews and effect volume in one place."
    ),
    CONTROLS(
        label = "Controls",
        summary = "Motion, haptics, bot pace, and compact match layout.",
        guidance = "Control choices tune speed, motion, bot turns, haptics, and one-hand match layout."
    )
}

private fun rulesReferenceLines(state: GameState): List<String> {
    val ruleSet = RuleSets.byId(state.ruleSetId)
    return buildList {
        add("Exact finish ${if (ruleSet.exactFinishRequired) "required" else "not required"}; sixes and ladders can grant bonus turns.")
        when (state.matchMode) {
            MatchModePreset.TIME_ATTACK -> add("Time Attack: leader wins if the turn clock expires before cell 100.")
            MatchModePreset.SUDDEN_DEATH -> add("Sudden Death: after the limit, the furthest player wins immediately.")
            MatchModePreset.BEST_OF_THREE -> add("Best Of Three: first to ${ruleSet.roundTarget} round wins takes the match.")
            MatchModePreset.PARTY_RULES,
            MatchModePreset.TACTICAL_CARDS -> add("Power-ups arm before a roll or resolve instantly depending on the card.")
            MatchModePreset.TEAM_MODE,
            MatchModePreset.TWO_V_TWO -> add("Team mode: teammates share the win target and team colors.")
            MatchModePreset.DAILY_CHALLENGE -> add("Daily: match progress also counts toward today's local objective.")
            MatchModePreset.QUEST_NODE -> add("Quest: campaign node rules use the displayed board, bot, and reward setup.")
            MatchModePreset.CLASSIC -> add("Classic: race to 100 with snakes, ladders, and exact finish.")
        }
        if (ruleSet.usesPowerUps) {
            add(PowerUpRuleEngine.describe(ruleSet))
        }
    }.take(4)
}

@Composable
private fun RulesReferencePanel(state: GameState) {
    val rules = rulesReferenceLines(state)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFFFFFFF))
            .border(1.dp, Color(0xFFE2C99F), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp)
            .testTag("settings_rules_reference"),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = "Rules reference",
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF6B3A00)
        )
        rules.forEachIndexed { index, rule ->
            Text(
                text = rule,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                color = Color(0xFF5B4A42),
                modifier = Modifier.testTag("settings_rule_${index + 1}")
            )
        }
    }
}

@Composable
private fun SettingsTabPicker(
    selectedTab: SettingsDialogTab,
    onSelectTab: (SettingsDialogTab) -> Unit
) {
    val tabScrollState = rememberScrollState()
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(tabScrollState)
                .testTag("settings_tab_scroller"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsDialogTab.entries.forEach { tab ->
                FilterChip(
                    modifier = Modifier
                        .width(108.dp)
                        .heightIn(min = 48.dp)
                        .testTag("settings_tab_${tab.name.lowercase()}"),
                    selected = selectedTab == tab,
                    onClick = { onSelectTab(tab) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFFF4F7FA),
                        labelColor = Color(0xFF4F6071),
                        selectedContainerColor = Color(0xFF1557A8),
                        selectedLabelColor = Color.White
                    ),
                    label = {
                        Text(
                            text = tab.label,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }
        if (tabScrollState.maxValue > 0 && tabScrollState.value > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(28.dp)
                    .height(52.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFFFDF8F1), Color.Transparent)
                        )
                    )
                    .testTag("settings_tab_left_fade")
            )
        }
        if (tabScrollState.maxValue > 0 && tabScrollState.value < tabScrollState.maxValue) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(28.dp)
                    .height(52.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color(0xFFFDF8F1))
                        )
                    )
                    .testTag("settings_tab_right_fade")
            )
        }
    }
}

@Composable
private fun SettingsTabGuidanceCard(selectedTab: SettingsDialogTab) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFFBF1))
            .border(1.dp, Color(0xFFE2C99F), RoundedCornerShape(12.dp))
            .padding(10.dp)
            .testTag("settings_tab_guidance"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        MiniSettingMarker(text = selectedTab.label.take(2).uppercase())
        Text(
            text = selectedTab.guidance,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF5A463A),
            modifier = Modifier
                .weight(1f)
                .testTag("settings_tab_guidance_text")
        )
    }
}

@Composable
private fun MiniSettingMarker(text: String) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE8F0FA))
            .border(1.dp, Color(0xFFBFD4EA), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1557A8)
        )
    }
}

@Composable
private fun SettingsSectionPanel(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
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
private fun SettingsToggleRow(
    iconLabel: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val titleColor = if (enabled) Color.Unspecified else Color(0xFF6B6076)
    val subtitleColor = if (enabled) Color(0xFF6D6259) else Color(0xFF7A7283)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (checked) Color(0xFF1557A8) else Color(0xFFE7EEF7))
                .border(1.dp, if (checked) Color(0xFF0B3F7C) else Color(0xFFC7D4E3), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = iconLabel,
                fontSize = 11.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (checked) Color.White else Color(0xFF33506A)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, fontWeight = FontWeight.SemiBold, color = titleColor)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor
            )
        }
        Switch(
            modifier = Modifier
                .semantics { contentDescription = title }
                .testTag(testTag),
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun <T> SettingsEnumChips(
    values: List<T>,
    selected: T,
    label: (T) -> String,
    tagPrefix: String,
    columns: Int = 3,
    onSelect: (T) -> Unit
) {
    values.chunked(columns).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row.forEach { value ->
                FilterChip(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("${tagPrefix}_${label(value).lowercase().replace(' ', '_')}"),
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { ChipLabel(label(value)) }
                )
            }
            repeat(columns - row.size) {
                Box(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GameConfirmationDialog(
    testTag: String,
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(inMatchDialogScrimColor)
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.4.dp, Color(0xFFE5D5B8)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .testTag(testTag)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFFFF6E3), Color(0xFFFFEDD2))
                            )
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF4E342E)
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF5D5243)
                            )
                        }
                        TextButton(
                            modifier = Modifier
                                .semantics { contentDescription = "Close $title" }
                                .testTag("${testTag}_close_button"),
                            onClick = onDismiss
                        ) {
                            Text("Close")
                        }
                    }
                    content()
                }
            }
        }
    }
}

@Composable
private fun ExitConfirmationDialog(
    onCancel: () -> Unit,
    onSaveFirst: () -> Unit,
    onConfirmExit: () -> Unit
) {
    GameConfirmationDialog(
        testTag = "exit_confirmation_dialog",
        title = "Exit Game?",
        subtitle = "Save first to keep the current turn available from Load Saved or Resume Latest.",
        onDismiss = onCancel
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFF8E7))
                .border(1.dp, Color(0xFFE5D5B8), RoundedCornerShape(12.dp))
                .padding(10.dp)
                .testTag("exit_warning_message")
        ) {
            Text(
                text = "Warning: exiting without saving discards unsaved progress.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF7A3F00)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 58.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                onClick = onSaveFirst
            ) {
                ExitActionLabel(iconLabel = "SAVE", text = "Save First")
            }
            Button(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 58.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE7E0EC),
                    contentColor = Color(0xFF1D1B20)
                ),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                onClick = onCancel
            ) {
                Text(
                    text = "Cancel",
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 58.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB3261E),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                onClick = onConfirmExit
            ) {
                Text(
                    text = "Exit",
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    lineHeight = 14.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ExitActionLabel(
    iconLabel: String,
    text: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(Color.White.copy(alpha = 0.22f))
                .border(1.dp, Color.White.copy(alpha = 0.48f), RoundedCornerShape(5.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = iconLabel,
                fontSize = 8.sp,
                lineHeight = 8.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Text(
            text = text,
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RestartConfirmationDialog(
    onCancel: () -> Unit,
    onConfirmRestart: () -> Unit
) {
    GameConfirmationDialog(
        testTag = "restart_confirmation_dialog",
        title = "Restart Match?",
        subtitle = "The current match will restart from cell 1. Auto-save keeps your latest turn available from launch.",
        onDismiss = onCancel
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE7E0EC),
                    contentColor = Color(0xFF1D1B20)
                ),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                onClick = onCancel
            ) {
                Text("Cancel", textAlign = TextAlign.Center, maxLines = 1)
            }
            Button(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                onClick = onConfirmRestart
            ) {
                Text("Restart", textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

private enum class TopOverlayIcon {
    PAUSE,
    RESUME,
    SETTINGS
}

@Composable
private fun TopOverlayGlyph(icon: TopOverlayIcon) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val stroke = size.minDimension * 0.12f
        when (icon) {
            TopOverlayIcon.PAUSE -> {
                drawLine(
                    color = Color(0xFF4E342E),
                    start = Offset(size.width * 0.36f, size.height * 0.22f),
                    end = Offset(size.width * 0.36f, size.height * 0.78f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFF4E342E),
                    start = Offset(size.width * 0.64f, size.height * 0.22f),
                    end = Offset(size.width * 0.64f, size.height * 0.78f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
            TopOverlayIcon.RESUME -> {
                val playPath = Path().apply {
                    moveTo(size.width * 0.34f, size.height * 0.22f)
                    lineTo(size.width * 0.76f, size.height * 0.50f)
                    lineTo(size.width * 0.34f, size.height * 0.78f)
                    close()
                }
                drawPath(playPath, color = Color(0xFF4E342E))
            }
            TopOverlayIcon.SETTINGS -> {
                drawCircle(
                    color = Color(0xFF4E342E),
                    radius = size.minDimension * 0.16f,
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    style = Stroke(width = stroke)
                )
                repeat(8) { index ->
                    val angle = (PI * 2.0 * index / 8.0).toFloat()
                    val inner = size.minDimension * 0.30f
                    val outer = size.minDimension * 0.44f
                    val center = Offset(size.width * 0.5f, size.height * 0.5f)
                    drawLine(
                        color = Color(0xFF4E342E),
                        start = Offset(
                            x = center.x + cos(angle) * inner,
                            y = center.y + sin(angle) * inner
                        ),
                        end = Offset(
                            x = center.x + cos(angle) * outer,
                            y = center.y + sin(angle) * outer
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardScreenBackground(
    themeOption: BoardThemeOption,
    reducedMotion: Boolean
) {
    val style = when (themeOption) {
        BoardThemeOption.VIBRANT -> vibrantBackdrop
        BoardThemeOption.PREMIUM_MUTED -> premiumMutedBackdrop
        BoardThemeOption.FESTIVAL -> festivalBackdrop
        BoardThemeOption.MONSOON -> monsoonBackdrop
    }

    val pulse = if (reducedMotion) {
        0f
    } else {
        val animatedPulse by rememberInfiniteTransition(label = "board_bg_pulse").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 5200),
                repeatMode = RepeatMode.Reverse
            ),
            label = "board_bg_pulse_value"
        )
        animatedPulse
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = style.gradientColors,
                    start = Offset.Zero,
                    end = Offset(900f, 1900f)
                )
            )
    ) {
        val wobble = sin(pulse * PI * 2f).toFloat()

        drawCircle(
            color = style.accentA,
            radius = size.minDimension * 0.34f,
            center = Offset(size.width * 0.12f, size.height * (0.22f + 0.02f * wobble))
        )
        drawCircle(
            color = style.accentB,
            radius = size.minDimension * 0.38f,
            center = Offset(size.width * 0.94f, size.height * (0.34f - 0.015f * wobble))
        )
        drawCircle(
            color = style.accentC,
            radius = size.minDimension * 0.30f,
            center = Offset(size.width * 0.78f, size.height * (0.92f + 0.012f * wobble))
        )

        val decorativePath = Path().apply {
            moveTo(size.width * 0.03f, size.height * 0.74f)
            cubicTo(
                size.width * 0.24f, size.height * 0.56f,
                size.width * 0.33f, size.height * 0.94f,
                size.width * 0.58f, size.height * 0.70f
            )
            cubicTo(
                size.width * 0.76f, size.height * 0.54f,
                size.width * 0.87f, size.height * 0.86f,
                size.width * 0.98f, size.height * 0.64f
            )
        }
        drawPath(decorativePath, color = style.pathColor, style = Stroke(width = 16f, cap = StrokeCap.Round))

        val watermarkColor = style.pathColor.copy(alpha = 0.45f)

        // Dice watermark (rounded square + pips)
        val diceSize = size.minDimension * 0.16f
        val diceTopLeft = Offset(size.width * 0.08f, size.height * 0.10f)
        drawRoundRect(
            color = watermarkColor,
            topLeft = diceTopLeft,
            size = androidx.compose.ui.geometry.Size(diceSize, diceSize),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(diceSize * 0.18f, diceSize * 0.18f),
            style = Stroke(width = 6f)
        )
        val pipOffset = diceSize * 0.22f
        val pipCenter = Offset(diceTopLeft.x + diceSize / 2f, diceTopLeft.y + diceSize / 2f)
        listOf(
            Offset(-pipOffset, -pipOffset),
            Offset(pipOffset, -pipOffset),
            Offset(0f, 0f),
            Offset(-pipOffset, pipOffset),
            Offset(pipOffset, pipOffset)
        ).forEach { offset ->
            drawCircle(
                color = watermarkColor,
                radius = diceSize * 0.05f,
                center = pipCenter + offset
            )
        }

        // Ladder watermark
        val ladderStartLeft = Offset(size.width * 0.82f, size.height * 0.14f)
        val ladderEndLeft = Offset(size.width * 0.70f, size.height * 0.42f)
        val ladderStartRight = Offset(size.width * 0.88f, size.height * 0.16f)
        val ladderEndRight = Offset(size.width * 0.76f, size.height * 0.44f)
        drawLine(watermarkColor, ladderStartLeft, ladderEndLeft, strokeWidth = 8f, cap = StrokeCap.Round)
        drawLine(watermarkColor, ladderStartRight, ladderEndRight, strokeWidth = 8f, cap = StrokeCap.Round)
        for (i in 1..5) {
            val t = i / 6f
            val start = Offset(
                x = ladderStartLeft.x + (ladderEndLeft.x - ladderStartLeft.x) * t,
                y = ladderStartLeft.y + (ladderEndLeft.y - ladderStartLeft.y) * t
            )
            val end = Offset(
                x = ladderStartRight.x + (ladderEndRight.x - ladderStartRight.x) * t,
                y = ladderStartRight.y + (ladderEndRight.y - ladderStartRight.y) * t
            )
            drawLine(watermarkColor, start, end, strokeWidth = 5f, cap = StrokeCap.Round)
        }

        // Snake watermark
        val snakePath = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.88f)
            cubicTo(
                size.width * 0.28f, size.height * 0.80f,
                size.width * 0.20f, size.height * 0.64f,
                size.width * 0.36f, size.height * 0.60f
            )
            cubicTo(
                size.width * 0.50f, size.height * 0.56f,
                size.width * 0.42f, size.height * 0.40f,
                size.width * 0.58f, size.height * 0.36f
            )
        }
        drawPath(snakePath, color = watermarkColor, style = Stroke(width = 11f, cap = StrokeCap.Round))
    }
}

@Composable
private fun DiceResultOverlay(
    roll: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xE6FFFFFF))
            .border(2.dp, Color(0xFF7B4EA3), RoundedCornerShape(18.dp))
            .padding(horizontal = 22.dp, vertical = 14.dp)
            .semantics { contentDescription = "Dice rolled $roll" },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = roll.toString(),
                fontSize = 42.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF3B2557),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Roll",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6C4A85)
            )
        }
    }
}

@Composable
private fun WinnerCelebrationOverlay(
    winnerName: String,
    analytics: MatchAnalytics,
    onNewGame: () -> Unit,
    onExit: () -> Unit,
    onReplay: () -> Unit,
    onRematch: () -> Unit,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = if (reducedMotion) null else rememberInfiniteTransition(label = "winnerCelebration")
    val fireworkProgress = if (transition == null) {
        0.35f
    } else {
        val animatedProgress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1700),
                repeatMode = RepeatMode.Restart
            ),
            label = "fireworkProgress"
        )
        animatedProgress
    }
    val bannerScale = if (transition == null) {
        1f
    } else {
        val animatedScale by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 650),
                repeatMode = RepeatMode.Reverse
            ),
            label = "winnerBannerScale"
        )
        animatedScale
    }

    Box(
        modifier = modifier.background(Color(0xA6141720)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bursts = listOf(
                Offset(size.width * 0.2f, size.height * 0.22f),
                Offset(size.width * 0.82f, size.height * 0.18f),
                Offset(size.width * 0.22f, size.height * 0.7f),
                Offset(size.width * 0.8f, size.height * 0.66f)
            )
            val burstColors = listOf(
                Color(0xFFFFC107),
                Color(0xFF80DEEA),
                Color(0xFFEF9A9A),
                Color(0xFFA5D6A7),
                Color(0xFFCE93D8)
            )

            bursts.forEachIndexed { burstIndex, center ->
                val localProgress = (fireworkProgress + burstIndex * 0.23f) % 1f
                val radius = (0.08f + localProgress) * size.minDimension * 0.36f
                val alpha = (1f - localProgress).coerceIn(0f, 1f)
                val sparkCount = 18

                for (spark in 0 until sparkCount) {
                    val angle = (2.0 * PI * spark / sparkCount) + (localProgress * PI)
                    val px = center.x + cos(angle).toFloat() * radius
                    val py = center.y + sin(angle).toFloat() * radius
                    drawCircle(
                        color = burstColors[(spark + burstIndex) % burstColors.size].copy(alpha = alpha),
                        radius = 5.6f * (1f - localProgress * 0.45f),
                        center = Offset(px, py)
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.graphicsLayer {
                scaleX = bannerScale
                scaleY = bannerScale
            }
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFFFF8E1), Color(0xFFFFECB3))
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Winner!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF5D4037)
                )
                Text(
                    text = winnerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4E342E)
                )
                Text(
                    text = analytics.summaryLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5D5243),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("post_match_analytics")
                )
                Text(
                    text = analytics.momentumLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5D5243),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("post_match_momentum")
                )
                Text(
                    text = analytics.nextObjectiveLine(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B4F1D),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("post_match_next_objective")
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("winner_new_game_button"),
                        onClick = onNewGame
                    ) {
                        Text("New Game")
                    }
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("winner_rematch_button"),
                        onClick = onRematch
                    ) {
                        Text("Rematch")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("winner_replay_button"),
                        onClick = onReplay
                    ) {
                        Text("Replay")
                    }
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("winner_exit_button"),
                        onClick = onExit
                    ) {
                        Text("Exit")
                    }
                }
            }
        }
    }
}

private fun MatchAnalytics.nextObjectiveLine(): String {
    return when {
        totalTurns == 0 -> "Next target: start a rematch and build a replay timeline."
        snakeCount > ladderCount -> "Next target: replay the slides, then rematch for a cleaner climb."
        ladderCount == 0 -> "Next target: chase a ladder climb in the rematch."
        else -> "Next target: review the replay or rematch this setup."
    }
}

@Composable
private fun ProgressAlertDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        modifier = Modifier.testTag("progress_alert_dialog"),
        onDismissRequest = onDismiss,
        title = { Text("Progress Updated", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF4E342E)
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
private fun ControlCard(
    state: GameState,
    previewDice: Int,
    isRolling: Boolean,
    isMoveAnimating: Boolean,
    compact: Boolean,
    isPaused: Boolean,
    diceSkin: DiceSkinOption,
    dailyChallenge: DailyChallenge?,
    dailyChallengeBaseProgress: Int,
    manualBotRollConfirmation: Boolean,
    botTurnPace: BotTurnPaceOption,
    shakeToRollEnabled: Boolean,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onUsePowerUp: (PowerUpType) -> Unit,
    onCancelArmedPowerUp: (PowerUpType) -> Unit,
    onLocatePlayer: (Int) -> Unit,
    lastReaction: String?,
    onReaction: (QuickReaction) -> Unit,
    onRollDice: () -> Unit
) {
    val cardPadding = if (compact) 7.dp else 8.dp
    val sectionSpacing = if (compact) 4.dp else 6.dp
    val diceSize = if (compact) 62.dp else 72.dp
    val diceOuterHorizontalPadding = if (compact) 8.dp else 10.dp
    val diceOuterVerticalPadding = if (compact) 4.dp else 5.dp
    val currentPlayer = state.players.getOrNull(state.currentPlayerIndex)
    val winnerName = state.winnerIndex?.let { state.players.getOrNull(it)?.name }
    val isBotTurn = state.botPlayerIndex == state.currentPlayerIndex && state.winnerIndex == null
    val botRollNeedsConfirmation = isBotTurn && manualBotRollConfirmation
    val playerCanRoll = state.winnerIndex == null && !isRolling && !isMoveAnimating && (!isBotTurn || manualBotRollConfirmation)
    val playerCanUsePowerUp = playerCanRoll && !isPaused && !isBotTurn
    val currentInventory = state.powerUpInventories.getOrNull(state.currentPlayerIndex).orEmpty()
    val bonusReason = state.bonusTurnReason()
    val isCardMode = state.matchMode == MatchModePreset.TACTICAL_CARDS
    val turnLabel = when {
        winnerName != null -> "$winnerName wins"
        isPaused -> "Paused"
        isRolling && isBotTurn -> "Bot rolling"
        isRolling -> "Rolling dice"
        isMoveAnimating -> "Resolving move"
        botRollNeedsConfirmation -> "Bot ready"
        isBotTurn -> "Bot turn"
        currentPlayer != null -> "${currentPlayer.name}'s turn"
        else -> "Turn pending"
    }
    val actionHint = when {
        winnerName != null -> "Start a new match or exit from the winner panel."
        isPaused -> "Resume to continue the match."
        isRolling -> "Dice is rolling."
        isMoveAnimating -> "Move animation is resolving."
        botRollNeedsConfirmation -> "Bot ready: tap Roll to confirm ${state.botPersonality.styleName.lowercase()} bot."
        isBotTurn -> "Bot thinking: ${state.botPersonality.styleName} rolls ${botTurnPace.label.lowercase()}."
        state.bonusTurnGranted && bonusReason != null -> {
            val rollPrompt = if (shakeToRollEnabled) "Tap dice or shake to roll again." else "Tap the dice panel to roll again."
            "Bonus turn: ${bonusReason.actionText}. $rollPrompt"
        }
        state.bonusTurnGranted -> {
            if (shakeToRollEnabled) {
                "Bonus turn active. Tap dice or shake to roll again."
            } else {
                "Bonus turn active. Tap the dice panel to roll again."
            }
        }
        shakeToRollEnabled -> "Tap dice or shake device to roll."
        else -> "Tap the dice panel to roll."
    }
    val rollUnavailableReason = when {
        winnerName != null -> "The match is complete."
        isPaused -> "Resume the match first."
        isRolling -> "The dice is already rolling."
        isMoveAnimating -> "Wait for the move animation to finish."
        isBotTurn && !manualBotRollConfirmation -> "The bot rolls automatically."
        else -> "Roll is not available right now."
    }
    val exactFinishHint = currentPlayer?.position
        ?.takeIf { it in 95..99 }
        ?.let { "Exact finish: ${100 - it} needed to win." }
    val overshootWarning = state.lastMovePlayerIndex
        ?.takeIf { state.lastMoveType == MoveType.OVERSHOOT }
        ?.let { index ->
            val player = state.players.getOrNull(index)
            val position = player?.position ?: return@let null
            "Overshot finish: ${player.name} stays on $position; ${100 - position} still needed."
        }
    val hasRecentMove = state.moveSignal > 0 && state.lastMovePlayerIndex != null
    val statusLabel = if (hasRecentMove) "Last move" else "Current turn"
    val statusBackground = if (hasRecentMove) Color(0xFFEAF3FF) else Color(0xFFFFF8E7)
    val statusBorder = if (hasRecentMove) Color(0xFFB5CAE8) else Color(0xFFE2C99F)
    val statusLabelColor = if (hasRecentMove) Color(0xFF164E80) else Color(0xFF8A4B00)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(sectionSpacing)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = if (compact) 64.dp else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = turnLabel,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (compact) 15.sp else 17.sp,
                        color = Color(0xFF3E2723),
                        modifier = Modifier.testTag("turn_status_label")
                    )
                    Text(
                        text = "${state.gameMode.displayName()} • ${state.matchMode.label} • ${state.difficulty.displayName()}",
                        fontSize = 12.sp,
                        color = Color(0xFF6D5C52),
                        modifier = Modifier.testTag("mode_difficulty_label")
                    )
                }
                if (state.bonusTurnGranted && state.winnerIndex == null) {
                    Text(
                        text = bonusReason?.badgeText ?: "Bonus",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5D2B00),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFFFD180))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("bonus_turn_badge")
                    )
                }
            }

            Text(
                text = state.difficulty.ruleSummary(),
                fontSize = if (compact) 10.sp else 11.sp,
                lineHeight = if (compact) 12.sp else 13.sp,
                color = Color(0xFF5B4A42),
                maxLines = if (compact) 2 else 3,
                modifier = Modifier
                    .padding(end = if (compact) 132.dp else 0.dp)
                    .testTag("difficulty_rule_summary")
            )

            MatchModeStrip(state = state, compact = compact)

            PlayerPositionStrip(
                players = state.players,
                currentPlayerIndex = state.currentPlayerIndex,
                botPlayerIndex = state.botPlayerIndex,
                compact = compact,
                onLocatePlayer = onLocatePlayer
            )

            if (dailyChallenge != null) {
                InMatchDailyChallengePanel(
                    challenge = dailyChallenge,
                    state = state,
                    baseProgress = dailyChallengeBaseProgress,
                    compact = compact
                )
            }

            if (!compact) {
                ReactionStrip(
                    lastReaction = lastReaction,
                    compact = compact,
                    enabled = state.winnerIndex == null && !isPaused,
                    onReaction = onReaction
                )

                InMatchMissionPanel(
                    missions = InMatchMissionCatalog.activeFor(state),
                    compact = compact
                )
            }

            if (RuleSets.byId(state.ruleSetId).usesPowerUps) {
                val occupiedCells = state.players.map { it.position }.toSet()
                PowerUpInventoryPanel(
                    inventory = currentInventory,
                    armedPowerUps = state.armedPowerUps.filter { it.playerIndex == state.currentPlayerIndex }.map { it.type },
                    activeTraps = state.activeTraps,
                    trapPreviewCell = PowerUpRuleEngine.trapCellFor(
                        state.players[state.currentPlayerIndex].position,
                        occupiedCells
                    ),
                    recentPowerUpEvents = state.matchEvents
                        .asReversed()
                        .filter {
                            it.powerUpUsed != null ||
                                it.triggeredPowerUps.isNotEmpty() ||
                                it.awardedPowerUps.isNotEmpty()
                        }
                        .take(2),
                    isCardMode = isCardMode,
                    botPlayerIndex = state.botPlayerIndex,
                    enabled = playerCanUsePowerUp,
                    compact = compact,
                    onUsePowerUp = onUsePowerUp,
                    onCancelArmedPowerUp = onCancelArmedPowerUp
                )
            }
            PowerUpFeedbackBanner(
                event = state.matchEvents.lastOrNull(),
                currentPlayerIndex = state.currentPlayerIndex,
                botPlayerIndex = state.botPlayerIndex,
                compact = compact
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusBackground)
                    .border(1.dp, statusBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("move_status_panel")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = statusLabel,
                        fontSize = if (compact) 9.sp else 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusLabelColor,
                        modifier = Modifier.testTag("move_status_label")
                    )
                    Text(
                        text = state.statusMessage,
                        fontSize = if (compact) 11.sp else 12.sp,
                        lineHeight = if (compact) 13.sp else 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4E342E)
                    )
                    exactFinishHint?.let {
                        Text(
                            text = it,
                            fontSize = 11.sp,
                            color = Color(0xFF8A3D00),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("exact_finish_hint")
                        )
                    }
                    overshootWarning?.let {
                        Text(
                            text = it,
                            fontSize = 11.sp,
                            color = Color(0xFF8A3D00),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("overshoot_warning")
                        )
                    }
                    if (state.moveHistory.isNotEmpty()) {
                        Text(
                            text = "Timeline: ${state.moveHistory.take(if (compact) 2 else 3).joinToString("  |  ")}",
                            fontSize = if (compact) 9.sp else 10.sp,
                            lineHeight = 12.sp,
                            color = if (hasRecentMove) Color(0xFF33556F) else Color(0xFF6A5A52),
                            maxLines = 2,
                            modifier = Modifier.testTag("move_history_preview")
                        )
                    }
                }
            }

            val rollPanelEnabled = playerCanRoll && !isPaused
            val rollPanelDescription = when {
                rollPanelEnabled && botRollNeedsConfirmation -> "Confirm bot roll panel"
                rollPanelEnabled -> "Roll dice panel"
                else -> "Dice panel unavailable. $rollUnavailableReason"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (rollPanelEnabled) Color(0xFFFFFFFF) else Color(0xFFECEFF1))
                    .border(
                        1.dp,
                        if (rollPanelEnabled) Color(0xFFE1D2BB) else Color(0xFFCDD3D8),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = rollPanelEnabled, onClick = onRollDice)
                    .padding(4.dp)
                    .semantics {
                        contentDescription = rollPanelDescription
                    },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DiceBadge(
                    value = previewDice,
                    isRolling = isRolling,
                    enabled = playerCanRoll && !isPaused,
                    contentDescription = if (rollPanelEnabled) {
                        "Roll Dice"
                    } else {
                        "Roll Dice unavailable. $rollUnavailableReason"
                    },
                    diceSkin = diceSkin,
                    boxSize = diceSize,
                    outerHorizontalPadding = diceOuterHorizontalPadding,
                    outerVerticalPadding = diceOuterVerticalPadding,
                    onClick = onRollDice
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = actionHint,
                        fontSize = if (compact) 11.sp else 12.sp,
                        lineHeight = if (compact) 13.sp else 15.sp,
                        color = Color(0xFF4E342E),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("dice_action_hint")
                    )
                    Text(
                        text = "Last roll: ${state.lastDiceRoll?.toString() ?: "-"}",
                        fontSize = 11.sp,
                        color = Color(0xFF75665E),
                        modifier = Modifier.testTag("last_roll_label")
                    )
                    TextButton(
                        modifier = Modifier
                            .heightIn(min = 32.dp)
                            .testTag("mute_shortcut_button"),
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                        onClick = onToggleMute
                    ) {
                        Text(
                            text = if (isMuted) "Unmute" else "Mute",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private data class BonusTurnReason(
    val badgeText: String,
    val actionText: String
)

private fun GameState.bonusTurnReason(): BonusTurnReason? {
    if (!bonusTurnGranted || winnerIndex != null) return null
    val event = matchEvents.lastOrNull { it.bonusTurn }
    return when {
        event?.moveType == MoveType.LADDER && event.dice == 6 -> BonusTurnReason(
            badgeText = "Bonus: Six + Ladder",
            actionText = "rolled a six and climbed a ladder"
        )
        event?.moveType == MoveType.LADDER -> BonusTurnReason(
            badgeText = "Bonus: Ladder",
            actionText = "climbed a ladder"
        )
        event?.dice == 6 -> BonusTurnReason(
            badgeText = "Bonus: Six",
            actionText = "rolled a six"
        )
        else -> BonusTurnReason(
            badgeText = "Bonus",
            actionText = "extra turn earned"
        )
    }
}

@Composable
private fun InMatchDailyChallengePanel(
    challenge: DailyChallenge,
    state: GameState,
    baseProgress: Int,
    compact: Boolean
) {
    val liveProgress = DailyChallengeCatalog.progressFromMatch(state, challenge)
    val progress = (baseProgress + liveProgress).coerceAtMost(challenge.target)
    val fraction = if (challenge.target <= 0) 1f else (progress / challenge.target.toFloat()).coerceIn(0f, 1f)
    val progressText = if (progress >= challenge.target) {
        "Complete"
    } else {
        "$progress/${challenge.target}"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF7E0))
            .border(1.dp, Color(0xFFE4C478), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = if (compact) 6.dp else 8.dp)
            .semantics {
                contentDescription = "Daily challenge ${challenge.title}, progress $progressText"
            }
            .testTag("in_match_daily_challenge")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily",
                    fontSize = if (compact) 10.sp else 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6D4300),
                    modifier = Modifier.testTag("in_match_daily_label")
                )
                Text(
                    text = progressText,
                    fontSize = if (compact) 10.sp else 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (progress >= challenge.target) Color(0xFF1B5E20) else Color(0xFF6D4300),
                    modifier = Modifier.testTag("in_match_daily_progress")
                )
            }
            Text(
                text = challenge.title,
                fontSize = if (compact) 10.sp else 11.sp,
                lineHeight = if (compact) 12.sp else 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4E342E),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("in_match_daily_title")
            )
            Text(
                text = challenge.description,
                fontSize = if (compact) 9.sp else 10.sp,
                lineHeight = if (compact) 11.sp else 12.sp,
                color = Color(0xFF6B5B4E),
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 5.dp else 6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFE9D9B6))
                    .testTag("in_match_daily_progress_bar")
            ) {
                if (fraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .background(if (progress >= challenge.target) Color(0xFF2E7D32) else Color(0xFFB87500))
                    )
                }
            }
        }
    }
}

@Composable
private fun InMatchMissionPanel(
    missions: List<InMatchMission>,
    compact: Boolean
) {
    if (missions.isEmpty()) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFEFF5FF))
            .border(1.dp, Color(0xFFB8CAE6), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = if (compact) 6.dp else 8.dp)
            .testTag("in_match_missions")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = "Missions",
                fontSize = if (compact) 10.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF173B63)
            )
            missions.forEach { mission ->
                Text(
                    text = if (mission.completed) "${mission.summary} done" else mission.summary,
                    fontSize = if (compact) 9.sp else 10.sp,
                    color = if (mission.completed) Color(0xFF1B5E20) else Color(0xFF40546C),
                    modifier = Modifier.testTag("mission_${mission.id}")
                )
            }
        }
    }
}

@Composable
private fun ReactionStrip(
    lastReaction: String?,
    compact: Boolean,
    enabled: Boolean,
    onReaction: (QuickReaction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reaction_strip"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QuickReaction.entries.forEach { reaction ->
                TextButton(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("reaction_${reaction.name.lowercase()}"),
                    enabled = enabled,
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = if (compact) 2.dp else 4.dp),
                    onClick = { onReaction(reaction) }
                ) {
                    Text(
                        text = reaction.label,
                        fontSize = if (compact) 9.sp else 10.sp,
                        maxLines = 1
                    )
                }
            }
        }
        lastReaction?.let {
            Text(
                text = it,
                fontSize = if (compact) 10.sp else 11.sp,
                color = Color(0xFF5D4037),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("last_reaction_label")
            )
        }
    }
}

@Composable
private fun PowerUpFeedbackBanner(
    event: MatchEvent?,
    currentPlayerIndex: Int,
    botPlayerIndex: Int?,
    compact: Boolean
) {
    val used = event?.powerUpUsed
    val triggered = event?.triggeredPowerUps.orEmpty()
    if (event == null || (used == null && triggered.isEmpty())) return

    val label = if (event.playerIndex != currentPlayerIndex && !event.winner) "Previous move" else "Power-up"
    val title = when {
        used != null -> "${event.playerName} used ${used.label}"
        triggered.isNotEmpty() -> "${event.playerName} triggered ${triggered.joinToString { it.label }}"
        else -> return
    }
    val detail = when {
        used != null -> used.description
        triggered.isNotEmpty() -> triggered.joinToString { it.description }
        else -> ""
    }
    val botReason = used
        ?.takeIf { event.playerIndex == botPlayerIndex }
        ?.botPowerUpReason()

    if (compact) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFEFF8F1))
                .border(1.dp, Color(0xFFB7D6BE), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .testTag("power_up_feedback_banner"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFDCEFE1))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    maxLines = 1
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20),
                    maxLines = 1
                )
                Text(
                    text = detail,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    color = Color(0xFF3E5F43),
                    maxLines = 1
                )
                botReason?.let {
                    Text(
                        text = it,
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                        color = Color(0xFF3E5F43),
                        maxLines = 1,
                        modifier = Modifier.testTag("power_up_bot_reason")
                    )
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFEFF8F1))
            .border(1.dp, Color(0xFFB7D6BE), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = if (compact) 7.dp else 9.dp)
            .testTag("power_up_feedback_banner")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                fontSize = if (compact) 10.sp else 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2E7D32)
            )
            Text(
                text = title,
                fontSize = if (compact) 10.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )
            Text(
                text = detail,
                fontSize = if (compact) 10.sp else 11.sp,
                lineHeight = if (compact) 12.sp else 13.sp,
                color = Color(0xFF3E5F43)
            )
            botReason?.let {
                Text(
                    text = it,
                    fontSize = if (compact) 10.sp else 11.sp,
                    lineHeight = if (compact) 12.sp else 13.sp,
                    color = Color(0xFF2F5738),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("power_up_bot_reason")
                )
            }
        }
    }
}

@Composable
private fun MatchModeStrip(
    state: GameState,
    compact: Boolean
) {
    val roundText = if (state.roundWins.isNotEmpty() && state.matchMode == MatchModePreset.BEST_OF_THREE) {
        val scores = state.roundWins.mapIndexed { index, wins -> "P${index + 1}:$wins" }.joinToString(" ")
        "Round ${state.roundNumber} | $scores"
    } else {
        null
    }
    val timerText = state.turnsRemaining?.let { remaining ->
        "Turns left $remaining"
    }
    val teamText = state.players
        .mapIndexedNotNull { index, player -> player.teamId?.let { "P${index + 1}:T${it + 1}" } }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" ")

    val items = listOfNotNull(
        timerText?.let { MatchStateChipSpec(text = it, testTag = "match_timer_badge", urgent = (state.turnsRemaining ?: Int.MAX_VALUE) <= 5) },
        roundText?.let { MatchStateChipSpec(text = it, testTag = "match_round_badge") },
        teamText?.let { MatchStateChipSpec(text = it, testTag = "match_team_badge") }
    )
    if (items.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("match_mode_state_strip"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            MatchStateChip(item = item, compact = compact)
        }
    }
}

private data class MatchStateChipSpec(
    val text: String,
    val testTag: String,
    val urgent: Boolean = false
)

@Composable
private fun MatchStateChip(
    item: MatchStateChipSpec,
    compact: Boolean
) {
    val background = if (item.urgent) Color(0xFFFFE0E0) else Color(0xFFFFF3D4)
    val border = if (item.urgent) Color(0xFFC62828) else Color(0xFFE2B65D)
    val foreground = if (item.urgent) Color(0xFF8B0000) else Color(0xFF7B3F00)
    Text(
        text = item.text,
        fontSize = if (compact) 10.sp else 11.sp,
        lineHeight = if (compact) 12.sp else 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = foreground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .padding(horizontal = if (compact) 7.dp else 8.dp, vertical = if (compact) 3.dp else 4.dp)
            .testTag(item.testTag)
    )
}

@Composable
private fun BoardViewportControls(
    zoom: Float,
    onZoomOut: () -> Unit,
    onCenterTurn: () -> Unit,
    onZoomIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xEFFFFFFF))
            .border(1.dp, Color(0xFFD7C6A7), RoundedCornerShape(999.dp))
            .padding(horizontal = 4.dp, vertical = 3.dp)
            .semantics {
                contentDescription = "Board zoom controls, ${"%.1f".format(Locale.US, zoom)}x"
            }
            .testTag("board_zoom_controls"),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            modifier = Modifier
                .heightIn(min = 34.dp)
                .widthIn(min = 38.dp)
                .testTag("board_zoom_out_button"),
            enabled = zoom > 1.01f,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
            onClick = onZoomOut
        ) {
            Text("-", fontWeight = FontWeight.ExtraBold)
        }
        Text(
            text = "${"%.1f".format(Locale.US, zoom)}x",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4E342E),
            modifier = Modifier
                .padding(horizontal = 3.dp)
                .testTag("board_zoom_status")
        )
        TextButton(
            modifier = Modifier
                .heightIn(min = 34.dp)
                .testTag("board_center_turn_button"),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            onClick = onCenterTurn
        ) {
            Text("Center", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        TextButton(
            modifier = Modifier
                .heightIn(min = 34.dp)
                .widthIn(min = 38.dp)
                .testTag("board_zoom_in_button"),
            enabled = zoom < 1.79f,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
            onClick = onZoomIn
        ) {
            Text("+", fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun PowerUpInventoryPanel(
    inventory: List<PowerUpType>,
    armedPowerUps: List<PowerUpType>,
    activeTraps: List<BoardTrap>,
    trapPreviewCell: Int,
    recentPowerUpEvents: List<MatchEvent>,
    isCardMode: Boolean,
    botPlayerIndex: Int?,
    enabled: Boolean,
    compact: Boolean,
    onUsePowerUp: (PowerUpType) -> Unit,
    onCancelArmedPowerUp: (PowerUpType) -> Unit
) {
    val grouped = inventory.groupingBy { it }.eachCount()
    val visiblePowerUps = (armedPowerUps + grouped.keys)
        .distinct()
        .sortedWith(
            compareBy<PowerUpType> { if (it in armedPowerUps) 0 else 1 }
                .thenBy { it.ordinal }
        )
    var collapsed by rememberSaveable { mutableStateOf(false) }
    val disabledReason = if (enabled) null else "Available on your turn before rolling."
    val horizontalScrollState = rememberScrollState()
    val panelTitle = if (isCardMode) "Cards" else "Power-ups"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF6F2FF))
            .border(1.dp, Color(0xFFD2C3EA), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = if (compact) 6.dp else 8.dp)
            .testTag("power_up_inventory_panel"),
        verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = panelTitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E255F),
                    modifier = Modifier.testTag("power_up_panel_title")
                )
                Text(
                    text = powerUpPanelSummary(grouped.size, armedPowerUps.size, activeTraps.size),
                    fontSize = 9.sp,
                    color = Color(0xFF6C5B7E),
                    maxLines = 1,
                    modifier = Modifier.testTag("power_up_panel_summary")
                )
            }
            TextButton(
                modifier = Modifier.testTag("power_up_panel_toggle"),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                onClick = { collapsed = !collapsed }
            ) {
                Text(
                    text = if (collapsed) "Show" else "Hide",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (collapsed) return@Column

        if (isCardMode) {
            Text(
                text = "Card timing: play before rolling; armed cards resolve on the next roll.",
                fontSize = 9.sp,
                lineHeight = 11.sp,
                color = Color(0xFF5B357A),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("card_timing_hint")
            )
        }

        if (visiblePowerUps.isEmpty()) {
            Text(
                text = if (isCardMode) "No cards in hand." else "No power-ups in hand.",
                fontSize = 10.sp,
                color = Color(0xFF6D6259)
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScrollState)
                    .testTag("power_up_button_row"),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                visiblePowerUps.forEach { powerUp ->
                    PowerUpActionButton(
                        powerUp = powerUp,
                        count = grouped[powerUp] ?: 0,
                        armed = powerUp in armedPowerUps,
                        enabled = enabled,
                        compact = compact,
                        isCardMode = isCardMode,
                        trapPreviewCell = trapPreviewCell,
                        onUsePowerUp = onUsePowerUp,
                        onCancelArmedPowerUp = onCancelArmedPowerUp
                    )
                }
            }
            if (grouped.size > 2) {
                Text(
                    text = "Swipe sideways for every power-up.",
                    fontSize = 9.sp,
                    color = Color(0xFF746185),
                    modifier = Modifier.testTag("power_up_scroll_hint")
                )
            }
            disabledReason?.let {
                Text(
                    text = it,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    color = Color(0xFF746185),
                    modifier = Modifier.testTag("power_up_disabled_reason")
                )
            }
        }

        val statusItems = buildList {
            armedPowerUps.forEach { add(it.armedStatusLabel()) }
            activeTraps.forEach { add("Trap at ${it.cell}") }
        }
        if (statusItems.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("power_up_status"),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                statusItems.forEach { item ->
                    PowerUpStatusChip(text = item)
                }
            }
        }

        if (recentPowerUpEvents.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFFFFFFFF))
                    .border(1.dp, Color(0xFFE0D6ED), RoundedCornerShape(9.dp))
                    .padding(horizontal = 7.dp, vertical = 5.dp)
                    .testTag("power_up_recent_log"),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (isCardMode) "Recent cards" else "Recent powers",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5B357A),
                    modifier = Modifier.testTag("power_up_recent_log_title")
                )
                recentPowerUpEvents.forEach { event ->
                    Text(
                        text = event.powerUpLogLabel(botPlayerIndex),
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        color = Color(0xFF5E5066),
                        maxLines = 1,
                        modifier = Modifier.testTag("power_up_recent_event")
                    )
                }
            }
        }
    }
}

@Composable
private fun PowerUpActionButton(
    powerUp: PowerUpType,
    count: Int,
    armed: Boolean,
    enabled: Boolean,
    compact: Boolean,
    isCardMode: Boolean,
    trapPreviewCell: Int,
    onUsePowerUp: (PowerUpType) -> Unit,
    onCancelArmedPowerUp: (PowerUpType) -> Unit
) {
    val description = if (armed) {
        "${powerUp.armedStatusLabel()}. Tap to cancel and return it to hand."
    } else if (isCardMode) {
        powerUp.cardPlayHint(trapPreviewCell)
    } else {
        powerUp.shortPlayHint(trapPreviewCell)
    }
    val activeContainerColor = if (armed) Color(0xFF2E7D60) else Color(0xFF6F4DA0)
    Button(
        modifier = Modifier
            .widthIn(min = if (compact) 116.dp else 132.dp)
            .testTag("power_up_${powerUp.name.lowercase()}")
            .semantics {
                contentDescription = if (enabled) {
                    val countText = if (armed && count == 0) "queued" else "$count available"
                    "${powerUp.label}, $countText. $description"
                } else {
                    "${powerUp.label}, $count available. Disabled. Available on your turn before rolling."
                }
            },
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = activeContainerColor,
            disabledContainerColor = Color(0xFFE8E0F1),
            contentColor = Color.White,
            disabledContentColor = Color(0xFF6B6076)
        ),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = if (compact) 5.dp else 7.dp),
        onClick = {
            if (armed) {
                onCancelArmedPowerUp(powerUp)
            } else {
                onUsePowerUp(powerUp)
            }
        }
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                PowerUpIconBadge(powerUp = powerUp, enabled = enabled)
                Text(
                    text = powerUp.label,
                    fontSize = if (compact) 9.sp else 10.sp,
                    lineHeight = if (compact) 10.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                CountPill(count = count, enabled = enabled, armed = armed)
                if (armed) {
                    ArmedPill()
                }
            }
            Text(
                text = description,
                fontSize = if (compact) 8.sp else 9.sp,
                lineHeight = if (compact) 9.sp else 10.sp,
                color = if (enabled) Color(0xFFEFE5FF) else Color(0xFF6B6076),
                maxLines = 2
            )
        }
    }
}

@Composable
private fun PowerUpIconBadge(
    powerUp: PowerUpType,
    enabled: Boolean
) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (enabled) powerUp.badgeColor() else Color(0xFFD7D0DF))
            .border(
                1.dp,
                if (enabled) Color(0xFFFFFFFF) else Color(0xFFB8AFBF),
                RoundedCornerShape(6.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = powerUp.iconText(),
            fontSize = 8.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (enabled) Color.White else Color(0xFF51495C),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CountPill(
    count: Int,
    enabled: Boolean,
    armed: Boolean
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (enabled) Color(0x33FFFFFF) else Color(0xFFEDE8F4))
            .border(
                1.dp,
                if (enabled) Color(0x66FFFFFF) else Color(0xFFC9BED7),
                RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 5.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (armed && count == 0) "Queued" else "x$count",
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) Color.White else Color(0xFF6B6076)
        )
    }
}

@Composable
private fun ArmedPill() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0x33FFFFFF))
            .border(1.dp, Color(0x77FFFFFF), RoundedCornerShape(999.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Armed",
            fontSize = 8.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}

@Composable
private fun PowerUpStatusChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFE8F4EC))
            .border(1.dp, Color(0xFFB8D9C4), RoundedCornerShape(999.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF245337),
            maxLines = 1
        )
    }
}

private fun powerUpPanelSummary(
    inventoryTypes: Int,
    armedCount: Int,
    trapCount: Int
): String {
    return buildList {
        add("$inventoryTypes types")
        if (armedCount > 0) add("$armedCount armed")
        if (trapCount > 0) add("$trapCount traps")
    }.joinToString(" | ")
}

private fun PowerUpType.iconText(): String {
    return when (this) {
        PowerUpType.SHIELD -> "SH"
        PowerUpType.REROLL -> "RR"
        PowerUpType.REVENGE -> "RV"
        PowerUpType.DICE_BOOST -> "+1"
        PowerUpType.TRAP -> "TP"
        PowerUpType.MYSTERY -> "?"
    }
}

private fun PowerUpType.badgeColor(): Color {
    return when (this) {
        PowerUpType.SHIELD -> Color(0xFF2E7D60)
        PowerUpType.REROLL -> Color(0xFF2D5D9F)
        PowerUpType.REVENGE -> Color(0xFF9C3D3D)
        PowerUpType.DICE_BOOST -> Color(0xFF8A5A00)
        PowerUpType.TRAP -> Color(0xFF6C3E91)
        PowerUpType.MYSTERY -> Color(0xFF4D5B66)
    }
}

private fun PowerUpType.shortPlayHint(trapPreviewCell: Int): String {
    return when (this) {
        PowerUpType.SHIELD -> "Arms for next snake, trap, or knockback."
        PowerUpType.REROLL -> "Arms one extra roll after your next roll."
        PowerUpType.REVENGE -> "Pushes the leader back right away."
        PowerUpType.DICE_BOOST -> "Adds one cell to your next roll."
        PowerUpType.TRAP -> "Places a trap preview at cell $trapPreviewCell."
        PowerUpType.MYSTERY -> "Triggers a small board effect now."
    }
}

private fun PowerUpType.cardPlayHint(trapPreviewCell: Int): String {
    return when (this) {
        PowerUpType.SHIELD -> "Card: play before rolling to block the next setback."
        PowerUpType.REROLL -> "Card: play before rolling to queue one extra roll."
        PowerUpType.REVENGE -> "Card: play before rolling when a leader needs pressure."
        PowerUpType.DICE_BOOST -> "Card: play before rolling to add one cell."
        PowerUpType.TRAP -> "Card: play before rolling to mark cell $trapPreviewCell."
        PowerUpType.MYSTERY -> "Card: play before rolling for a random board effect."
    }
}

private fun PowerUpType.botPowerUpReason(): String {
    return when (this) {
        PowerUpType.SHIELD -> "Bot reason: protecting against snakes, traps, or knockback."
        PowerUpType.REROLL -> "Bot reason: chasing an extra roll before the board changes."
        PowerUpType.REVENGE -> "Bot reason: pressuring the leader after falling behind."
        PowerUpType.DICE_BOOST -> "Bot reason: pushing closer to finish or a ladder lane."
        PowerUpType.TRAP -> "Bot reason: setting a future setback before rivals advance."
        PowerUpType.MYSTERY -> "Bot reason: taking a swing when a random effect can help."
    }
}

private fun PowerUpType.armedStatusLabel(): String {
    return when (this) {
        PowerUpType.SHIELD -> "Shield armed"
        PowerUpType.REROLL -> "Reroll next"
        PowerUpType.DICE_BOOST -> "+1 next roll"
        PowerUpType.TRAP -> "Trap ready"
        PowerUpType.REVENGE -> "Revenge armed"
        PowerUpType.MYSTERY -> "Mystery armed"
    }
}

private fun MatchEvent.powerUpLogLabel(botPlayerIndex: Int?): String {
    val parts = buildList {
        powerUpUsed?.let { add("used ${it.label}") }
        if (triggeredPowerUps.isNotEmpty()) {
            add("triggered ${triggeredPowerUps.joinToString { it.label }}")
        }
        if (awardedPowerUps.isNotEmpty()) {
            add("earned ${awardedPowerUps.joinToString { it.label }}")
        }
    }
    val reason = powerUpUsed
        ?.takeIf { playerIndex == botPlayerIndex }
        ?.botPowerUpReason()
        ?.removePrefix("Bot reason: ")
        ?.replaceFirstChar { it.lowercase() }
    return buildString {
        append("$playerName ${parts.joinToString(", ")}")
        if (reason != null) {
            append(" | bot: ")
            append(reason)
        }
    }
}

@Composable
private fun PlayerPositionStrip(
    players: List<PlayerState>,
    currentPlayerIndex: Int,
    botPlayerIndex: Int?,
    compact: Boolean,
    onLocatePlayer: (Int) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("player_position_strip"),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        players.forEachIndexed { index, player ->
            val isCurrent = index == currentPlayerIndex
            val isBot = index == botPlayerIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isCurrent) Color(0xFFFFF0C2) else Color(0xFFF9F4EA))
                    .border(
                        width = if (isCurrent) 1.4.dp else 0.8.dp,
                        color = if (isCurrent) Color(0xFFD18B00) else Color(0xFFD6C7B0),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onLocatePlayer(index) }
                    .semantics {
                        contentDescription = "${player.name}, cell ${player.position}. Tap to locate token."
                    }
                    .padding(horizontal = 7.dp, vertical = if (compact) 5.dp else 6.dp)
                    .testTag("player_position_${index + 1}")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (compact) 18.dp else 20.dp)
                                    .clip(CircleShape)
                                    .background(player.color)
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = player.avatarGlyph(index),
                                    fontSize = if (compact) 8.sp else 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = if (isBot) "Bot" else "P${index + 1}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3E2723),
                                maxLines = 1
                            )
                        }
                        Text(
                            text = player.position.toString(),
                            fontSize = if (compact) 12.sp else 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF4E342E),
                            maxLines = 1
                        )
                    }
                    if (!compact) {
                        Text(
                            text = if (isCurrent) "${player.name} to move" else player.name,
                            fontSize = 9.sp,
                            color = Color(0xFF5A4A42),
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private fun PlayerState.avatarGlyph(index: Int): String {
    return when (avatarId) {
        "cobra_token" -> "C"
        "ladder_king" -> "L"
        "gold_die" -> "G"
        else -> (index + 1).toString()
    }
}

@Composable
private fun PauseOverlay(
    onResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color(0xA0141720)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFDDC7A0))
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFFFF4DE), Color(0xFFFFE9C3))
                        )
                    )
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Game Paused",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4E342E)
                )
                Button(
                    modifier = Modifier.testTag("pause_resume_button"),
                    onClick = onResume
                ) {
                    Text("Resume")
                }
            }
        }
    }
}

@Composable
private fun TopOverlayActionButton(
    icon: TopOverlayIcon,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0xE6FFF3E0))
            .border(1.dp, Color(0xFFD9C6A6), CircleShape)
            .semantics { this.contentDescription = contentDescription }
            .testTag(testTag)
    ) {
        TopOverlayGlyph(icon = icon)
    }
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
private fun SettingsOptionButtonLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        lineHeight = 13.sp,
        maxLines = 2,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
