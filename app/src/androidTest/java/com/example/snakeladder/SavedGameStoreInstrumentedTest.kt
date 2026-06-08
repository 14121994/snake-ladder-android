package com.example.snakeladder

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SavedGameStoreInstrumentedTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun loadAll_missingAndInvalidJsonReturnEmptyList() {
        assertTrue(SavedGameStore.loadAll(context).isEmpty())

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SAVES, "not-json")
            .commit()

        assertTrue(SavedGameStore.loadAll(context).isEmpty())
    }

    @Test
    fun loadAll_skipsBadItemsAndAppliesLegacyDefaults() {
        val saves = JSONArray()
            .put(JSONObject.NULL)
            .put(JSONObject().put("id", "broken"))
            .put(legacySnapshotJson(id = "old", name = "Alpha", savedAt = 1000L, firstPlayerPosition = 7))
            .put(legacySnapshotJson(id = "new", name = " alpha ", savedAt = 2000L, firstPlayerPosition = 18))

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SAVES, saves.toString())
            .commit()

        val loaded = SavedGameStore.loadAll(context)

        assertEquals(1, loaded.size)
        assertEquals("new", loaded.first().id)
        assertEquals(" alpha ", loaded.first().name)
        assertEquals(BoardThemeOption.VIBRANT, loaded.first().boardTheme)
        assertEquals(18, loaded.first().state.players.first().position)
        assertEquals(GameMode.LOCAL_MULTIPLAYER, loaded.first().state.gameMode)
        assertEquals(GameDifficulty.EASY, loaded.first().state.difficulty)
        assertNull(loaded.first().state.lastDiceRoll)
        assertNull(loaded.first().state.winnerIndex)
        assertTrue(loaded.first().state.moveHistory.isEmpty())
        assertTrue(loaded.first().state.lastMovePath.isEmpty())
    }

    @Test
    fun save_blankNameDoesNotPersist() {
        val id = SavedGameStore.save(
            context = context,
            name = "   ",
            boardTheme = BoardThemeOption.VIBRANT,
            state = sampleState()
        )

        assertNull(id)
        assertTrue(SavedGameStore.loadAll(context).isEmpty())
    }

    @Test
    fun save_reusesExistingIdNameMatchOrCreatesNewId() {
        val firstId = requireNotNull(SavedGameStore.save(
            context = context,
            name = "Alpha",
            boardTheme = BoardThemeOption.VIBRANT,
            state = sampleState(firstPlayerPosition = 9)
        ))
        val renamedId = requireNotNull(SavedGameStore.save(
            context = context,
            name = "Renamed",
            boardTheme = BoardThemeOption.PREMIUM_MUTED,
            state = sampleState(firstPlayerPosition = 22),
            existingId = firstId
        ))
        val matchedByNameId = requireNotNull(SavedGameStore.save(
            context = context,
            name = " renamed ",
            boardTheme = BoardThemeOption.VIBRANT,
            state = sampleState(firstPlayerPosition = 33)
        ))
        val betaId = requireNotNull(SavedGameStore.save(
            context = context,
            name = "Beta",
            boardTheme = BoardThemeOption.VIBRANT,
            state = sampleState(firstPlayerPosition = 44),
            existingId = "missing-id"
        ))

        val loaded = SavedGameStore.loadAll(context)

        assertTrue(firstId.isNotBlank())
        assertEquals(firstId, renamedId)
        assertEquals(firstId, matchedByNameId)
        assertNotEquals(firstId, betaId)
        assertEquals(2, loaded.size)
        assertEquals("Beta", loaded[0].name)
        assertEquals("renamed", loaded[1].name.trim().lowercase())
        assertEquals(33, loaded[1].state.players[0].position)
    }

    @Test
    fun saveAndLoad_roundTripsModernMatchMetadataAndPowerUps() {
        val state = sampleState(firstPlayerPosition = 72).copy(
            gameMode = GameMode.VS_BOT,
            botPlayerIndex = 1,
            difficulty = GameDifficulty.HARD,
            botPersonality = BotPersonality.PRO,
            matchMode = MatchModePreset.PARTY_RULES,
            boardLayoutId = BoardLayouts.TRAP_VALLEY_ID,
            ruleSetId = RuleSets.PARTY_ID,
            powerUpInventories = listOf(
                listOf(PowerUpType.SHIELD, PowerUpType.REROLL),
                listOf(PowerUpType.TRAP)
            ),
            armedPowerUps = listOf(PlayerArmedPowerUp(0, PowerUpType.SHIELD)),
            activeTraps = listOf(BoardTrap(cell = 40, ownerPlayerIndex = 1)),
            roundNumber = 2,
            roundWins = listOf(1, 0),
            turnLimit = 20,
            turnsRemaining = 12,
            campaignNodeId = "trap_valley",
            dailyChallengeId = "20260603_knock_back_rival",
            matchEvents = listOf(
                MatchEvent(
                    turnNumber = 4,
                    playerIndex = 0,
                    playerName = "Player 1",
                    dice = 5,
                    startPosition = 67,
                    landedPosition = 72,
                    finalPosition = 72,
                    moveType = MoveType.TRAP,
                    path = listOf(68, 69, 70, 71, 72),
                    knockedBackPlayerIndices = listOf(1),
                    triggeredPowerUps = listOf(PowerUpType.TRAP),
                    awardedPowerUps = listOf(PowerUpType.REVENGE),
                    tileLabel = "Deep trap",
                    roundNumber = 2
                )
            )
        )

        val id = requireNotNull(SavedGameStore.save(
            context = context,
            name = "Modern",
            boardTheme = BoardThemeOption.FESTIVAL,
            state = state
        ))

        val loaded = SavedGameStore.loadAll(context).single()
        val loadedState = loaded.state

        assertEquals(id, loaded.id)
        assertEquals(BoardThemeOption.FESTIVAL, loaded.boardTheme)
        assertEquals(GameMode.VS_BOT, loadedState.gameMode)
        assertEquals(BotPersonality.PRO, loadedState.botPersonality)
        assertEquals(MatchModePreset.PARTY_RULES, loadedState.matchMode)
        assertEquals(BoardLayouts.TRAP_VALLEY_ID, loadedState.boardLayoutId)
        assertEquals(listOf(PowerUpType.SHIELD, PowerUpType.REROLL), loadedState.powerUpInventories[0])
        assertEquals(listOf(PlayerArmedPowerUp(0, PowerUpType.SHIELD)), loadedState.armedPowerUps)
        assertEquals(listOf(BoardTrap(cell = 40, ownerPlayerIndex = 1)), loadedState.activeTraps)
        assertEquals(2, loadedState.roundNumber)
        assertEquals(listOf(1, 0), loadedState.roundWins)
        assertEquals(20, loadedState.turnLimit)
        assertEquals(12, loadedState.turnsRemaining)
        assertEquals("trap_valley", loadedState.campaignNodeId)
        assertEquals("20260603_knock_back_rival", loadedState.dailyChallengeId)
        assertEquals(MoveType.TRAP, loadedState.matchEvents.single().moveType)
        assertEquals(listOf(PowerUpType.REVENGE), loadedState.matchEvents.single().awardedPowerUps)
    }

    @Test
    fun loadAll_parsesSparseModernJsonWithFallbackEnumsAndMalformedOptionalArrays() {
        val sparse = legacySnapshotJson(id = "sparse", name = "Sparse", savedAt = 3000L, firstPlayerPosition = 12)
            .put("boardTheme", BoardThemeOption.MONSOON.name)
            .put("botPersonality", "BROKEN")
            .put("matchMode", "BROKEN")
            .put("moveHistory", JSONObject())
            .put("lastMovePath", JSONObject())
            .put("lastMoveType", JSONObject.NULL)
            .put("powerUpInventories", JSONArray().put(JSONArray().put("SHIELD").put("NOPE")).put("bad"))
            .put(
                "armedPowerUps",
                JSONArray()
                    .put(JSONObject.NULL)
                    .put(JSONObject().put("playerIndex", 0).put("type", "NOPE"))
                    .put(JSONObject().put("playerIndex", 1).put("type", PowerUpType.TRAP.name))
            )
            .put(
                "activeTraps",
                JSONArray()
                    .put(JSONObject.NULL)
                    .put(JSONObject().put("cell", 1).put("ownerPlayerIndex", 0))
                    .put(JSONObject().put("cell", 120).put("ownerPlayerIndex", 1))
            )
            .put(
                "matchEvents",
                JSONArray()
                    .put(JSONObject.NULL)
                    .put(
                        JSONObject()
                            .put("playerIndex", 1)
                            .put("dice", 99)
                            .put("moveType", "BROKEN")
                            .put("powerUpUsed", "NOPE")
                            .put("triggeredPowerUps", JSONArray().put("SHIELD").put("NOPE"))
                            .put("awardedPowerUps", JSONArray().put("REROLL"))
                    )
                    .put(
                        JSONObject()
                            .put("turnNumber", 2)
                            .put("playerIndex", 0)
                            .put("dice", 0)
                            .put("moveType", MoveType.POWER_UP.name)
                    )
            )

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SAVES, JSONArray().put(sparse).toString())
            .commit()

        val loaded = SavedGameStore.loadAll(context).single()
        val state = loaded.state

        assertEquals(BoardThemeOption.MONSOON, loaded.boardTheme)
        assertEquals(BotPersonality.STEADY, state.botPersonality)
        assertEquals(MatchModePreset.CLASSIC, state.matchMode)
        assertTrue(state.moveHistory.isEmpty())
        assertTrue(state.lastMovePath.isEmpty())
        assertEquals(listOf(PowerUpType.SHIELD), state.powerUpInventories[0])
        assertTrue(state.powerUpInventories[1].isEmpty())
        assertEquals(listOf(PlayerArmedPowerUp(1, PowerUpType.TRAP)), state.armedPowerUps)
        assertEquals(listOf(BoardTrap(2, 0), BoardTrap(99, 1)), state.activeTraps)
        assertEquals(2, state.matchEvents.size)
        assertEquals(6, state.matchEvents.first().dice)
        assertEquals(MoveType.NORMAL, state.matchEvents.first().moveType)
        assertNull(state.matchEvents.first().powerUpUsed)
        assertEquals(listOf(PowerUpType.SHIELD), state.matchEvents.first().triggeredPowerUps)
        assertEquals(listOf(PowerUpType.REROLL), state.matchEvents.first().awardedPowerUps)
        assertTrue(state.matchEvents[1].triggeredPowerUps.isEmpty())
        assertTrue(state.matchEvents[1].awardedPowerUps.isEmpty())
        assertTrue(formatSavedAt(loaded.savedAt).isNotBlank())
    }

    @Test
    fun saveAndLoad_roundTripsTeamWinnerAndPowerUpUsedBranches() {
        val state = sampleState(firstPlayerPosition = 100).copy(
            players = listOf(
                PlayerState("Player 1", Color.Red, 100, teamId = 0),
                PlayerState("Player 2", Color.Blue, 48, teamId = 1)
            ),
            winnerIndex = 0,
            winningTeamId = 0,
            matchMode = MatchModePreset.TWO_V_TWO,
            ruleSetId = RuleSets.TEAM_ID,
            matchEvents = listOf(
                MatchEvent(
                    turnNumber = 1,
                    playerIndex = 0,
                    playerName = "Player 1",
                    dice = 0,
                    startPosition = 100,
                    landedPosition = 100,
                    finalPosition = 100,
                    moveType = MoveType.POWER_UP,
                    path = emptyList(),
                    powerUpUsed = PowerUpType.REVENGE,
                    winningTeamId = 0
                )
            )
        )

        SavedGameStore.save(
            context = context,
            name = "Team Win",
            boardTheme = BoardThemeOption.PREMIUM_MUTED,
            state = state
        )

        val loaded = SavedGameStore.loadAll(context).single().state

        assertEquals(0, loaded.players.first().teamId)
        assertEquals(0, loaded.winningTeamId)
        assertEquals(PowerUpType.REVENGE, loaded.matchEvents.single().powerUpUsed)
        assertEquals(0, loaded.matchEvents.single().winningTeamId)
    }

    @Test
    fun deleteById_ignoresBlankIdAndRemovesMatchingSave() {
        val keepId = requireNotNull(SavedGameStore.save(
            context = context,
            name = "Keep",
            boardTheme = BoardThemeOption.VIBRANT,
            state = sampleState(firstPlayerPosition = 12)
        ))
        val deleteId = requireNotNull(SavedGameStore.save(
            context = context,
            name = "Delete",
            boardTheme = BoardThemeOption.VIBRANT,
            state = sampleState(firstPlayerPosition = 24)
        ))

        SavedGameStore.deleteById(context, " ")
        assertEquals(2, SavedGameStore.loadAll(context).size)

        SavedGameStore.deleteById(context, deleteId)
        val remaining = SavedGameStore.loadAll(context)

        assertEquals(1, remaining.size)
        assertEquals(keepId, remaining.first().id)
        assertEquals("Keep", remaining.first().name)
    }

    private fun legacySnapshotJson(
        id: String,
        name: String,
        savedAt: Long,
        firstPlayerPosition: Int
    ): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("name", name)
            .put("savedAt", savedAt)
            .put(
                "players",
                JSONArray()
                    .put(playerJson("Player 1", Color.Red, firstPlayerPosition))
                    .put(playerJson("Player 2", Color.Blue, 1))
            )
            .put("currentPlayerIndex", 0)
            .put("statusMessage", "Loaded legacy save")
            .put("bonusTurnGranted", false)
    }

    private fun playerJson(name: String, color: Color, position: Int): JSONObject {
        return JSONObject()
            .put("name", name)
            .put("colorArgb", color.toArgb())
            .put("position", position)
    }

    private fun sampleState(firstPlayerPosition: Int = 1): GameState {
        return GameState(
            players = listOf(
                PlayerState("Player 1", Color.Red, firstPlayerPosition),
                PlayerState("Player 2", Color.Blue, 4)
            ),
            currentPlayerIndex = 0,
            lastDiceRoll = null,
            statusMessage = "Player 1 starts. Roll the dice.",
            bonusTurnGranted = false,
            winnerIndex = null,
            moveHistory = emptyList(),
            gameMode = GameMode.LOCAL_MULTIPLAYER,
            botPlayerIndex = null,
            lastMovePlayerIndex = null,
            lastMovePath = emptyList(),
            lastMoveType = null,
            moveSignal = 0,
            difficulty = GameDifficulty.EASY
        )
    }

    private companion object {
        const val PREFS_NAME = "snake_ladder_saved_games"
        const val KEY_SAVES = "saves"
    }
}
