package com.example.snakeladder

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object SavedGameStore {
    private const val PREFS_NAME = "snake_ladder_saved_games"
    private const val KEY_SAVES = "saves"

    fun loadAll(context: Context): List<SavedGameSnapshot> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SAVES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val parsed = buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    fromJson(item)?.let { add(it) }
                }
            }
            dedupeSavedSnapshotsByName(parsed)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(
        context: Context,
        name: String,
        boardTheme: BoardThemeOption,
        state: GameState,
        existingId: String? = null
    ): String? {
        val sanitizedName = name.trim()
        if (sanitizedName.isEmpty()) return null

        val current = loadAll(context)
        val nameMatchedId = current.firstOrNull {
            savedGameNameKey(it.name) == savedGameNameKey(sanitizedName)
        }?.id
        val targetId = when {
            !existingId.isNullOrBlank() && current.any { it.id == existingId } -> existingId
            !nameMatchedId.isNullOrBlank() -> nameMatchedId
            else -> "save_${System.currentTimeMillis()}_${(1000..9999).random()}"
        }

        val snapshot = SavedGameSnapshot(
            id = targetId,
            name = sanitizedName,
            savedAt = System.currentTimeMillis(),
            boardTheme = boardTheme,
            state = state
        )

        val merged = upsertSavedSnapshotsByName(current, snapshot)

        val out = JSONArray()
        merged.forEach { out.put(toJson(it)) }
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SAVES, out.toString())
            .commit()

        return if (saved) snapshot.id else null
    }

    fun deleteById(context: Context, id: String) {
        if (id.isBlank()) return
        val remaining = loadAll(context).filterNot { it.id == id }
        val out = JSONArray()
        remaining.forEach { out.put(toJson(it)) }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SAVES, out.toString())
            .commit()
    }

    private fun toJson(snapshot: SavedGameSnapshot): JSONObject {
        val state = snapshot.state
        return JSONObject().apply {
            put("schemaVersion", GAME_STATE_SCHEMA_VERSION)
            put("id", snapshot.id)
            put("name", snapshot.name)
            put("savedAt", snapshot.savedAt)
            put("boardTheme", snapshot.boardTheme.name)

            put("players", JSONArray().apply {
                state.players.forEach { player ->
                    put(JSONObject().apply {
                        put("name", player.name)
                        put("colorArgb", player.color.toArgb())
                        put("position", player.position)
                        put("teamId", player.teamId ?: JSONObject.NULL)
                        put("avatarId", player.avatarId)
                    })
                }
            })
            put("currentPlayerIndex", state.currentPlayerIndex)
            put("lastDiceRoll", state.lastDiceRoll ?: JSONObject.NULL)
            put("statusMessage", state.statusMessage)
            put("bonusTurnGranted", state.bonusTurnGranted)
            put("winnerIndex", state.winnerIndex ?: JSONObject.NULL)
            put("moveHistory", JSONArray().apply {
                state.moveHistory.forEach { put(it) }
            })
            put("gameMode", state.gameMode.name)
            put("difficulty", state.difficulty.name)
            put("botPersonality", state.botPersonality.name)
            put("matchMode", state.matchMode.name)
            put("boardLayoutId", state.boardLayoutId)
            put("ruleSetId", state.ruleSetId)
            put("powerUpInventories", JSONArray().apply {
                state.powerUpInventories.forEach { inventory ->
                    put(JSONArray().apply { inventory.forEach { put(it.name) } })
                }
            })
            put("armedPowerUps", JSONArray().apply {
                state.armedPowerUps.forEach { armed ->
                    put(JSONObject().apply {
                        put("playerIndex", armed.playerIndex)
                        put("type", armed.type.name)
                    })
                }
            })
            put("activeTraps", JSONArray().apply {
                state.activeTraps.forEach { trap ->
                    put(JSONObject().apply {
                        put("cell", trap.cell)
                        put("ownerPlayerIndex", trap.ownerPlayerIndex)
                    })
                }
            })
            put("roundNumber", state.roundNumber)
            put("roundWins", JSONArray().apply { state.roundWins.forEach { put(it) } })
            put("turnLimit", state.turnLimit ?: JSONObject.NULL)
            put("turnsRemaining", state.turnsRemaining ?: JSONObject.NULL)
            put("winningTeamId", state.winningTeamId ?: JSONObject.NULL)
            put("campaignNodeId", state.campaignNodeId ?: JSONObject.NULL)
            put("dailyChallengeId", state.dailyChallengeId ?: JSONObject.NULL)
            put("botPlayerIndex", state.botPlayerIndex ?: JSONObject.NULL)
            put("lastMovePlayerIndex", state.lastMovePlayerIndex ?: JSONObject.NULL)
            put("lastMovePath", JSONArray().apply {
                state.lastMovePath.forEach { put(it) }
            })
            put("lastMoveType", state.lastMoveType?.name ?: JSONObject.NULL)
            put("moveSignal", state.moveSignal)
            put("matchEvents", JSONArray().apply {
                state.matchEvents.forEach { event ->
                    put(JSONObject().apply {
                        put("turnNumber", event.turnNumber)
                        put("playerIndex", event.playerIndex)
                        put("playerName", event.playerName)
                        put("dice", event.dice)
                        put("startPosition", event.startPosition)
                        put("landedPosition", event.landedPosition)
                        put("finalPosition", event.finalPosition)
                        put("moveType", event.moveType.name)
                        put("path", JSONArray().apply { event.path.forEach { put(it) } })
                        put("knockedBackPlayerIndices", JSONArray().apply {
                            event.knockedBackPlayerIndices.forEach { put(it) }
                        })
                        put("bonusTurn", event.bonusTurn)
                        put("winner", event.winner)
                        put("powerUpUsed", event.powerUpUsed?.name ?: JSONObject.NULL)
                        put("triggeredPowerUps", JSONArray().apply {
                            event.triggeredPowerUps.forEach { put(it.name) }
                        })
                        put("awardedPowerUps", JSONArray().apply {
                            event.awardedPowerUps.forEach { put(it.name) }
                        })
                        put("tileLabel", event.tileLabel ?: JSONObject.NULL)
                        put("roundNumber", event.roundNumber)
                        put("winningTeamId", event.winningTeamId ?: JSONObject.NULL)
                    })
                }
            })
        }
    }

    private fun fromJson(json: JSONObject): SavedGameSnapshot? {
        return try {
            val players = mutableListOf<PlayerState>()
            val playersArray = json.getJSONArray("players")
            for (i in 0 until playersArray.length()) {
                val player = playersArray.getJSONObject(i)
                players.add(
                    PlayerState(
                        name = player.getString("name"),
                        color = Color(player.getInt("colorArgb")),
                        position = player.getInt("position"),
                        teamId = player.optNullableInt("teamId"),
                        avatarId = player.optString("avatarId", "classic_token")
                    )
                )
            }

            SavedGameSnapshot(
                id = json.getString("id"),
                name = json.getString("name"),
                savedAt = json.getLong("savedAt"),
                boardTheme = BoardThemeOption.valueOf(json.optString("boardTheme", BoardThemeOption.VIBRANT.name)),
                state = GameState(
                    players = players,
                    currentPlayerIndex = json.getInt("currentPlayerIndex"),
                    lastDiceRoll = json.optNullableInt("lastDiceRoll"),
                    statusMessage = json.optString("statusMessage", ""),
                    bonusTurnGranted = json.optBoolean("bonusTurnGranted", false),
                    winnerIndex = json.optNullableInt("winnerIndex"),
                    moveHistory = json.optStringList("moveHistory"),
                    gameMode = GameMode.valueOf(json.optString("gameMode", GameMode.LOCAL_MULTIPLAYER.name)),
                    botPlayerIndex = json.optNullableInt("botPlayerIndex"),
                    lastMovePlayerIndex = json.optNullableInt("lastMovePlayerIndex"),
                    lastMovePath = json.optIntList("lastMovePath"),
                    lastMoveType = json.optNullableString("lastMoveType")?.let { MoveType.valueOf(it) },
                    moveSignal = json.optInt("moveSignal", 0),
                    difficulty = GameDifficulty.valueOf(json.optString("difficulty", GameDifficulty.EASY.name)),
                    matchEvents = json.optMatchEvents("matchEvents"),
                    botPersonality = json.optEnum("botPersonality", BotPersonality.STEADY),
                    matchMode = json.optEnum("matchMode", MatchModePreset.CLASSIC),
                    boardLayoutId = json.optString("boardLayoutId", BoardLayouts.CLASSIC_ID),
                    ruleSetId = json.optString("ruleSetId", RuleSets.CLASSIC_ID),
                    powerUpInventories = json.optPowerUpInventories("powerUpInventories", players.size),
                    armedPowerUps = json.optArmedPowerUps("armedPowerUps"),
                    activeTraps = json.optBoardTraps("activeTraps"),
                    roundNumber = json.optInt("roundNumber", 1),
                    roundWins = json.optIntList("roundWins"),
                    turnLimit = json.optNullableInt("turnLimit"),
                    turnsRemaining = json.optNullableInt("turnsRemaining"),
                    winningTeamId = json.optNullableInt("winningTeamId"),
                    campaignNodeId = json.optNullableString("campaignNodeId"),
                    dailyChallengeId = json.optNullableString("dailyChallengeId"),
                    schemaVersion = json.optInt("schemaVersion", 1)
                )
            )
        } catch (_: Exception) {
            null
        }
    }
}

private fun savedGameNameKey(name: String): String = name.trim().lowercase(Locale.ROOT)

internal fun dedupeSavedSnapshotsByName(items: List<SavedGameSnapshot>): List<SavedGameSnapshot> {
    val sorted = items.sortedByDescending { it.savedAt }
    val deduped = LinkedHashMap<String, SavedGameSnapshot>()
    sorted.forEach { item ->
        val key = savedGameNameKey(item.name)
        if (key.isNotEmpty() && key !in deduped) {
            deduped[key] = item
        }
    }
    return deduped.values.take(20)
}

internal fun upsertSavedSnapshotsByName(
    existing: List<SavedGameSnapshot>,
    incoming: SavedGameSnapshot
): List<SavedGameSnapshot> {
    val key = savedGameNameKey(incoming.name)
    val filtered = existing.filterNot {
        it.id == incoming.id || savedGameNameKey(it.name) == key
    }
    return dedupeSavedSnapshotsByName(listOf(incoming) + filtered)
}

private fun JSONObject.optNullableInt(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return optInt(key)
}

private fun JSONObject.optNullableString(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key)
}

private fun JSONObject.optStringList(key: String): List<String> {
    if (!has(key) || isNull(key)) return emptyList()
    val arr = optJSONArray(key) ?: return emptyList()
    return List(arr.length()) { idx -> arr.optString(idx) }
}

private fun JSONObject.optIntList(key: String): List<Int> {
    if (!has(key) || isNull(key)) return emptyList()
    val arr = optJSONArray(key) ?: return emptyList()
    return List(arr.length()) { idx -> arr.optInt(idx) }
}

private fun JSONObject.optMatchEvents(key: String): List<MatchEvent> {
    if (!has(key) || isNull(key)) return emptyList()
    val arr = optJSONArray(key) ?: return emptyList()
    return buildList {
        for (i in 0 until arr.length()) {
            val event = arr.optJSONObject(i) ?: continue
            runCatching {
                add(
                    MatchEvent(
                        turnNumber = event.optInt("turnNumber", i + 1),
                        playerIndex = event.optInt("playerIndex", 0),
                        playerName = event.optString("playerName", "Player ${event.optInt("playerIndex", 0) + 1}"),
                        dice = event.optInt("dice", 1).coerceIn(0, 6),
                        startPosition = event.optInt("startPosition", 1),
                        landedPosition = event.optInt("landedPosition", 1),
                        finalPosition = event.optInt("finalPosition", 1),
                        moveType = event.optEnum("moveType", MoveType.NORMAL),
                        path = event.optIntList("path"),
                        knockedBackPlayerIndices = event.optIntList("knockedBackPlayerIndices"),
                        bonusTurn = event.optBoolean("bonusTurn", false),
                        winner = event.optBoolean("winner", false),
                        powerUpUsed = event.optNullableString("powerUpUsed")?.let {
                            runCatching { PowerUpType.valueOf(it) }.getOrNull()
                        },
                        triggeredPowerUps = event.optPowerUpList("triggeredPowerUps"),
                        awardedPowerUps = event.optPowerUpList("awardedPowerUps"),
                        tileLabel = event.optNullableString("tileLabel"),
                        roundNumber = event.optInt("roundNumber", 1),
                        winningTeamId = event.optNullableInt("winningTeamId")
                    )
                )
            }
        }
    }
}

private fun JSONObject.optPowerUpInventories(key: String, playerCount: Int): List<List<PowerUpType>> {
    val arr = optJSONArray(key) ?: return List(playerCount) { emptyList() }
    return List(playerCount) { index ->
        arr.optJSONArray(index)?.toPowerUpList().orEmpty()
    }
}

private fun JSONObject.optPowerUpList(key: String): List<PowerUpType> {
    return optJSONArray(key)?.toPowerUpList().orEmpty()
}

private fun JSONArray.toPowerUpList(): List<PowerUpType> {
    return buildList {
        for (i in 0 until length()) {
            val value = optString(i)
            runCatching { PowerUpType.valueOf(value) }.getOrNull()?.let { add(it) }
        }
    }
}

private fun JSONObject.optArmedPowerUps(key: String): List<PlayerArmedPowerUp> {
    val arr = optJSONArray(key) ?: return emptyList()
    return buildList {
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val type = runCatching {
                PowerUpType.valueOf(item.optString("type", ""))
            }.getOrNull() ?: continue
            add(PlayerArmedPowerUp(item.optInt("playerIndex", 0), type))
        }
    }
}

private fun JSONObject.optBoardTraps(key: String): List<BoardTrap> {
    val arr = optJSONArray(key) ?: return emptyList()
    return buildList {
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            add(
                BoardTrap(
                    cell = item.optInt("cell", 50).coerceIn(2, 99),
                    ownerPlayerIndex = item.optInt("ownerPlayerIndex", 0)
                )
            )
        }
    }
}

private inline fun <reified T : Enum<T>> JSONObject.optEnum(key: String, fallback: T): T {
    return runCatching {
        enumValueOf<T>(optString(key, fallback.name))
    }.getOrDefault(fallback)
}

internal fun formatSavedAt(ts: Long): String {
    val fmt = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return fmt.format(Date(ts))
}
