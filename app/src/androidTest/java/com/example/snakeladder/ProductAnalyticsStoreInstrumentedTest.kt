package com.example.snakeladder

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductAnalyticsStoreInstrumentedTest {

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
    fun recordsFunnelCountersModePopularityChurnAndRecovery() {
        assertEquals(ProductAnalyticsSnapshot(), ProductAnalyticsStore.load(context))

        ProductAnalyticsStore.recordMatchStarted(context, GameMode.VS_BOT, MatchModePreset.PARTY_RULES)
        ProductAnalyticsStore.recordMatchStarted(context, GameMode.VS_BOT, MatchModePreset.PARTY_RULES)

        val started = ProductAnalyticsStore.load(context)
        assertEquals(2, started.matchStarts)
        assertEquals(2, started.modePopularity["VS_BOT/PARTY_RULES"])
        assertTrue(started.activeDays.isNotEmpty())

        val state = sampleState(
            events = (1..25).map { turn ->
                MatchEvent(
                    turnNumber = turn,
                    playerIndex = turn % 2,
                    playerName = "Player ${(turn % 2) + 1}",
                    dice = 3,
                    startPosition = turn,
                    landedPosition = turn + 3,
                    finalPosition = turn + 3,
                    moveType = MoveType.NORMAL,
                    path = listOf(turn + 1, turn + 2, turn + 3)
                )
            }
        )

        ProductAnalyticsStore.recordEarlyExit(context, state)
        ProductAnalyticsStore.recordMatchCompleted(context, state)

        val updated = ProductAnalyticsStore.load(context)
        assertEquals(1, updated.earlyExits)
        assertEquals(1, updated.matchCompletions)
        assertEquals(1, updated.churnPoints["VS_BOT/PARTY_RULES/turn_20"])

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ANALYTICS, "not-json")
            .commit()

        assertEquals(ProductAnalyticsSnapshot(), ProductAnalyticsStore.load(context))
    }

    @Test
    fun loadFiltersMalformedMapsAndBlankActiveDays() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(
                KEY_ANALYTICS,
                JSONObject()
                    .put("matchStarts", 3)
                    .put("modePopularity", JSONArray().put("bad"))
                    .put("churnPoints", JSONObject().put("CLASSIC/turn_1", 2))
                    .put("activeDays", JSONArray().put("").put("20260603"))
                    .toString()
            )
            .commit()

        val loaded = ProductAnalyticsStore.load(context)

        assertEquals(3, loaded.matchStarts)
        assertTrue(loaded.modePopularity.isEmpty())
        assertEquals(2, loaded.churnPoints["CLASSIC/turn_1"])
        assertEquals(setOf("20260603"), loaded.activeDays)
    }

    private fun sampleState(events: List<MatchEvent>): GameState {
        return GameState(
            players = listOf(
                PlayerState("Player 1", Color.Red, 30),
                PlayerState("Rival Bot", Color.Blue, 45)
            ),
            currentPlayerIndex = 0,
            lastDiceRoll = 3,
            statusMessage = "In progress",
            bonusTurnGranted = false,
            winnerIndex = null,
            moveHistory = emptyList(),
            gameMode = GameMode.VS_BOT,
            botPlayerIndex = 1,
            lastMovePlayerIndex = 0,
            lastMovePath = listOf(28, 29, 30),
            lastMoveType = MoveType.NORMAL,
            moveSignal = events.size,
            difficulty = GameDifficulty.MEDIUM,
            matchEvents = events,
            matchMode = MatchModePreset.PARTY_RULES,
            boardLayoutId = BoardLayouts.TRAP_VALLEY_ID,
            ruleSetId = RuleSets.PARTY_ID
        )
    }

    private companion object {
        const val PREFS_NAME = "snake_ladder_product_analytics"
        const val KEY_ANALYTICS = "analytics"
    }
}
