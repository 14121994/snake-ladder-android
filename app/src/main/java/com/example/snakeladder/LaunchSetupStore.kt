package com.example.snakeladder

import android.content.Context

internal data class LaunchSetupSnapshot(
    val players: Int = 2,
    val mode: GameMode = GameMode.LOCAL_MULTIPLAYER,
    val matchMode: MatchModePreset = MatchModePreset.CLASSIC,
    val boardLayoutId: String = BoardLayouts.CLASSIC_ID,
    val botPersonality: BotPersonality = BotPersonality.STEADY,
    val newGameGuideDismissed: Boolean = false
)

internal object LaunchSetupStore {
    private const val PREFS = "launch_setup"
    private const val KEY_PLAYERS = "players"
    private const val KEY_MODE = "mode"
    private const val KEY_MATCH_MODE = "match_mode"
    private const val KEY_BOARD = "board"
    private const val KEY_BOT = "bot_personality"
    private const val KEY_NEW_GAME_GUIDE_DISMISSED = "new_game_guide_dismissed"

    fun load(context: Context): LaunchSetupSnapshot {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val mode = prefs.getString(KEY_MODE, null)?.let { value ->
            runCatching { GameMode.valueOf(value) }.getOrNull()
        } ?: GameMode.LOCAL_MULTIPLAYER
        val matchMode = prefs.getString(KEY_MATCH_MODE, null)?.let { value ->
            runCatching { MatchModePreset.valueOf(value) }.getOrNull()
        } ?: MatchModePreset.CLASSIC
        val botPersonality = prefs.getString(KEY_BOT, null)?.let { value ->
            runCatching { BotPersonality.valueOf(value) }.getOrNull()
        } ?: BotPersonality.STEADY

        return LaunchSetupSnapshot(
            players = prefs.getInt(KEY_PLAYERS, 2).coerceIn(2, 4),
            mode = mode,
            matchMode = matchMode,
            boardLayoutId = prefs.getString(KEY_BOARD, BoardLayouts.CLASSIC_ID) ?: BoardLayouts.CLASSIC_ID,
            botPersonality = botPersonality,
            newGameGuideDismissed = prefs.getBoolean(KEY_NEW_GAME_GUIDE_DISMISSED, false)
        )
    }

    fun save(context: Context, snapshot: LaunchSetupSnapshot) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_PLAYERS, snapshot.players.coerceIn(2, 4))
            .putString(KEY_MODE, snapshot.mode.name)
            .putString(KEY_MATCH_MODE, snapshot.matchMode.name)
            .putString(KEY_BOARD, snapshot.boardLayoutId)
            .putString(KEY_BOT, snapshot.botPersonality.name)
            .putBoolean(KEY_NEW_GAME_GUIDE_DISMISSED, snapshot.newGameGuideDismissed)
            .apply()
    }
}
