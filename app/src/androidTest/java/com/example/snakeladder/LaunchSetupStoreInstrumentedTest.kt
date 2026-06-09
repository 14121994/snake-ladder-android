package com.example.snakeladder

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LaunchSetupStoreInstrumentedTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        clearLaunchSetup()
    }

    @After
    fun tearDown() {
        clearLaunchSetup()
    }

    private fun clearLaunchSetup() {
        context.getSharedPreferences("launch_setup", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun loadFallsBackWhenPersistedValuesAreInvalid() {
        context.getSharedPreferences("launch_setup", Context.MODE_PRIVATE)
            .edit()
            .putInt("players", 8)
            .putString("mode", "SIDEWAYS")
            .putString("match_mode", "MARATHON")
            .putString("board", null)
            .putString("bot_personality", "CHAOTIC")
            .commit()

        val loaded = LaunchSetupStore.load(context)

        assertEquals(4, loaded.players)
        assertEquals(GameMode.LOCAL_MULTIPLAYER, loaded.mode)
        assertEquals(MatchModePreset.CLASSIC, loaded.matchMode)
        assertEquals(BoardLayouts.CLASSIC_ID, loaded.boardLayoutId)
        assertEquals(BotPersonality.STEADY, loaded.botPersonality)
        assertEquals(false, loaded.newGameGuideDismissed)
    }

    @Test
    fun saveClampsPlayersAndPersistsSelectedSetup() {
        LaunchSetupStore.save(
            context,
            LaunchSetupSnapshot(
                players = 1,
                mode = GameMode.VS_BOT,
                matchMode = MatchModePreset.TIME_ATTACK,
                boardLayoutId = BoardLayouts.SPEED_RUN_ID,
                botPersonality = BotPersonality.RISKY,
                newGameGuideDismissed = true
            )
        )

        val loaded = LaunchSetupStore.load(context)

        assertEquals(2, loaded.players)
        assertEquals(GameMode.VS_BOT, loaded.mode)
        assertEquals(MatchModePreset.TIME_ATTACK, loaded.matchMode)
        assertEquals(BoardLayouts.SPEED_RUN_ID, loaded.boardLayoutId)
        assertEquals(BotPersonality.RISKY, loaded.botPersonality)
        assertEquals(true, loaded.newGameGuideDismissed)
    }
}
