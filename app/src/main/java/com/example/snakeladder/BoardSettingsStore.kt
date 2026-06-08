package com.example.snakeladder

import android.content.Context

internal data class BoardSettingsSnapshot(
    val sfxVolume: Float = 1f,
    val vibrationEnabled: Boolean = true,
    val fastAnimations: Boolean = false,
    val diceSkin: DiceSkinOption = DiceSkinOption.CLASSIC_RED,
    val tokenTrail: TokenTrailOption = TokenTrailOption.NONE,
    val hapticTheme: HapticThemeOption = HapticThemeOption.CLASSIC,
    val soundtrack: SoundtrackOption = SoundtrackOption.CLASSIC,
    val highContrastBoard: Boolean = false,
    val reducedMotionEnabled: Boolean = false,
    val botTurnPace: BotTurnPaceOption = BotTurnPaceOption.STANDARD,
    val manualBotRollConfirmation: Boolean = false,
    val shakeToRollEnabled: Boolean = false,
    val compactMatchUiEnabled: Boolean = false
)

internal object BoardSettingsStore {
    private const val PREFS = "board_settings"
    private const val KEY_VOLUME = "sfx_volume"
    private const val KEY_VIBRATION = "vibration"
    private const val KEY_FAST = "fast_animations"
    private const val KEY_DICE = "dice_skin"
    private const val KEY_TRAIL = "token_trail"
    private const val KEY_HAPTIC = "haptic_theme"
    private const val KEY_SOUNDTRACK = "soundtrack"
    private const val KEY_HIGH_CONTRAST_BOARD = "high_contrast_board"
    private const val KEY_REDUCED_MOTION = "reduced_motion"
    private const val KEY_BOT_TURN_PACE = "bot_turn_pace"
    private const val KEY_MANUAL_BOT_ROLL = "manual_bot_roll"
    private const val KEY_SHAKE_TO_ROLL = "shake_to_roll"
    private const val KEY_COMPACT_MATCH_UI = "compact_match_ui"

    fun load(context: Context): BoardSettingsSnapshot {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return BoardSettingsSnapshot(
            sfxVolume = prefs.getFloat(KEY_VOLUME, 1f).coerceIn(0f, 1f),
            vibrationEnabled = prefs.getBoolean(KEY_VIBRATION, true),
            fastAnimations = prefs.getBoolean(KEY_FAST, false),
            diceSkin = enumValue(prefs.getString(KEY_DICE, null), DiceSkinOption.CLASSIC_RED),
            tokenTrail = enumValue(prefs.getString(KEY_TRAIL, null), TokenTrailOption.NONE),
            hapticTheme = enumValue(prefs.getString(KEY_HAPTIC, null), HapticThemeOption.CLASSIC),
            soundtrack = enumValue(prefs.getString(KEY_SOUNDTRACK, null), SoundtrackOption.CLASSIC),
            highContrastBoard = prefs.getBoolean(KEY_HIGH_CONTRAST_BOARD, false),
            reducedMotionEnabled = prefs.getBoolean(KEY_REDUCED_MOTION, false),
            botTurnPace = enumValue(prefs.getString(KEY_BOT_TURN_PACE, null), BotTurnPaceOption.STANDARD),
            manualBotRollConfirmation = prefs.getBoolean(KEY_MANUAL_BOT_ROLL, false),
            shakeToRollEnabled = prefs.getBoolean(KEY_SHAKE_TO_ROLL, false),
            compactMatchUiEnabled = prefs.getBoolean(KEY_COMPACT_MATCH_UI, false)
        )
    }

    fun save(context: Context, snapshot: BoardSettingsSnapshot) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_VOLUME, snapshot.sfxVolume.coerceIn(0f, 1f))
            .putBoolean(KEY_VIBRATION, snapshot.vibrationEnabled)
            .putBoolean(KEY_FAST, snapshot.fastAnimations)
            .putString(KEY_DICE, snapshot.diceSkin.name)
            .putString(KEY_TRAIL, snapshot.tokenTrail.name)
            .putString(KEY_HAPTIC, snapshot.hapticTheme.name)
            .putString(KEY_SOUNDTRACK, snapshot.soundtrack.name)
            .putBoolean(KEY_HIGH_CONTRAST_BOARD, snapshot.highContrastBoard)
            .putBoolean(KEY_REDUCED_MOTION, snapshot.reducedMotionEnabled)
            .putString(KEY_BOT_TURN_PACE, snapshot.botTurnPace.name)
            .putBoolean(KEY_MANUAL_BOT_ROLL, snapshot.manualBotRollConfirmation)
            .putBoolean(KEY_SHAKE_TO_ROLL, snapshot.shakeToRollEnabled)
            .putBoolean(KEY_COMPACT_MATCH_UI, snapshot.compactMatchUiEnabled)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String?, fallback: T): T {
        return value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback
    }
}
