package com.example.snakeladder

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerProfileStoreInstrumentedTest {

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
    fun exportImportSaveAndLoadMigratesProfile() {
        val raw = PlayerProfileStore.exportProfile(
            PlayerProfile(
                xp = -20,
                coins = -10,
                gems = -1,
                matchesStarted = 1,
                matchesCompleted = 3,
                selectedAvatarId = "missing",
                selectedTitle = "",
                unlockedAvatarIds = emptySet(),
                unlockedBoardIds = setOf("missing_board"),
                schemaVersion = 1
            )
        )

        val imported = PlayerProfileStore.importAndSave(context, raw) ?: error("Profile import failed")
        val loaded = PlayerProfileStore.load(context)

        assertEquals(PLAYER_PROFILE_SCHEMA_VERSION, imported.schemaVersion)
        assertEquals(0, loaded.xp)
        assertEquals(0, loaded.coins)
        assertEquals(0, loaded.gems)
        assertEquals(3, loaded.matchesStarted)
        assertEquals("classic_token", loaded.selectedAvatarId)
        assertTrue(BoardLayouts.CLASSIC_ID in loaded.unlockedBoardIds)
        assertTrue("title_new_challenger" in loaded.unlockedTitleIds)
    }

    @Test
    fun rejectsCorruptedImportAndRecoversCorruptedStoredProfile() {
        assertNull(PlayerProfileStore.importProfile("{not valid json"))

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PROFILE, "{not valid json")
            .commit()

        val recovered = PlayerProfileStore.load(context)

        assertEquals(PLAYER_PROFILE_SCHEMA_VERSION, recovered.schemaVersion)
        assertEquals("classic_token", recovered.selectedAvatarId)
        assertTrue(BoardLayouts.CLASSIC_ID in recovered.unlockedBoardIds)
    }

    @Test
    fun resetReplacesProfileWithDefault() {
        PlayerProfileStore.save(context, PlayerProfile(coins = 250, gems = 3))

        val reset = PlayerProfileStore.reset(context)

        assertEquals(0, reset.coins)
        assertEquals(0, reset.gems)
        assertEquals(reset, PlayerProfileStore.load(context))
    }

    @Test
    fun loadMaintenanceImportAndRecordCompletedMatchCoverFallbackBranches() {
        val defaultProfile = PlayerProfileStore.load(context)
        assertEquals(PlayerProfile(), defaultProfile)
        assertTrue(PlayerProfileStore.maintenanceSummary(defaultProfile).contains("Schema $PLAYER_PROFILE_SCHEMA_VERSION"))

        val raw = JSONObject()
            .put("xp", 500)
            .put("coins", 50)
            .put("selectedAvatarId", "missing")
            .put("unlockedTitleIds", JSONObject())
            .put("unlockedAvatarIds", JSONArray().put("").put("cobra_token"))
            .put("unlockedBoardIds", JSONArray().put("missing_board"))
            .put(
                "recentMatches",
                JSONArray()
                    .put(JSONObject.NULL)
                    .put(
                        JSONObject()
                            .put("completedAt", 10L)
                            .put("modeLabel", "legacy")
                            .put("winnerName", "Player 1")
                            .put("turns", 4)
                            .put("ladders", 1)
                            .put("snakes", 0)
                            .put("matchMode", "BROKEN")
                    )
            )

        val imported = PlayerProfileStore.importProfile(raw.toString()) ?: error("Profile import failed")
        val result = PlayerProfileStore.recordCompletedMatch(
            context = context,
            state = sampleWinningState(),
            challenge = DailyChallenge(
                id = "finish_once",
                dateKey = "20260603",
                title = "Finish",
                description = "Finish once.",
                target = 1,
                kind = DailyChallengeKind.WIN_MATCH
            )
        )

        assertEquals("classic_token", imported.selectedAvatarId)
        assertTrue(BoardLayouts.CLASSIC_ID in imported.unlockedBoardIds)
        assertEquals(MatchModePreset.CLASSIC, imported.recentMatches.single().matchMode)
        assertEquals(1, result.profile.matchesCompleted)
        assertEquals(result.profile, PlayerProfileStore.load(context))

        val started = PlayerProfileStore.recordStartedMatch(context)

        assertEquals(1, started.matchesCompleted)
        assertEquals(2, started.matchesStarted)
        assertEquals(started, PlayerProfileStore.load(context))
    }

    @Test
    fun importProfileHandlesNullAndNonArrayCollections() {
        val imported = PlayerProfileStore.importProfile(
            JSONObject()
                .put("unlockedTitleIds", JSONObject.NULL)
                .put("unlockedAvatarIds", JSONObject.NULL)
                .put("unlockedBoardIds", JSONObject.NULL)
                .put("recentMatches", JSONObject())
                .toString()
        ) ?: error("Profile import failed")

        assertTrue("title_new_challenger" in imported.unlockedTitleIds)
        assertTrue("classic_token" in imported.unlockedAvatarIds)
        assertTrue(BoardLayouts.CLASSIC_ID in imported.unlockedBoardIds)
        assertTrue(imported.recentMatches.isEmpty())
    }

    @Test
    fun importAndSaveRejectsInvalidAndKeepsValidMigratedSelections() {
        assertNull(PlayerProfileStore.importAndSave(context, "broken json"))

        val imported = PlayerProfileStore.importProfile(
            JSONObject()
                .put("xp", 1_250)
                .put("selectedAvatarId", "cobra_token")
                .put("selectedTitle", "Snake Tamer")
                .put("unlockedAvatarIds", JSONArray().put("cobra_token").put("ladder_king"))
                .put("unlockedTitleIds", JSONArray().put("title_snake_tamer"))
                .put("unlockedBoardIds", JSONArray().put(BoardLayouts.SPEED_RUN_ID))
                .toString()
        ) ?: error("Profile import failed")

        assertEquals("cobra_token", imported.selectedAvatarId)
        assertEquals("Snake Tamer", imported.selectedTitle)
        assertTrue("ladder_king" in imported.unlockedAvatarIds)
        assertTrue("title_snake_tamer" in imported.unlockedTitleIds)
        assertTrue(BoardLayouts.SPEED_RUN_ID in imported.unlockedBoardIds)
    }

    private fun sampleWinningState(): GameState {
        return GameState(
            players = listOf(
                PlayerState("Player 1", androidx.compose.ui.graphics.Color.Red, 100),
                PlayerState("Player 2", androidx.compose.ui.graphics.Color.Blue, 44)
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
            difficulty = GameDifficulty.EASY,
            matchEvents = listOf(
                MatchEvent(1, 0, "Player 1", 3, 97, 100, 100, MoveType.WIN, listOf(98, 99, 100), winner = true)
            )
        )
    }

    private companion object {
        const val PREFS_NAME = "snake_ladder_player_profile"
        const val KEY_PROFILE = "profile"
    }
}
