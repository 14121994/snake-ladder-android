package com.example.snakeladder

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BoardSettingsStoreInstrumentedTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        clearSettings()
    }

    @After
    fun tearDown() {
        clearSettings()
    }

    private fun clearSettings() {
        context.getSharedPreferences("board_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun saveLoadPersistsReducedMotionAndClampsVolume() {
        BoardSettingsStore.save(
            context,
            BoardSettingsSnapshot(
                sfxVolume = 1.5f,
                vibrationEnabled = false,
                fastAnimations = true,
                reducedMotionEnabled = true,
                diceSkin = DiceSkinOption.ROYAL_BLUE,
                tokenTrail = TokenTrailOption.SPARK,
                hapticTheme = HapticThemeOption.SOFT,
                soundtrack = SoundtrackOption.FESTIVAL,
                highContrastBoard = true,
                botTurnPace = BotTurnPaceOption.QUICK,
                manualBotRollConfirmation = true,
                shakeToRollEnabled = true,
                compactMatchUiEnabled = true
            )
        )

        val loaded = BoardSettingsStore.load(context)

        assertEquals(1f, loaded.sfxVolume)
        assertEquals(false, loaded.vibrationEnabled)
        assertTrue(loaded.fastAnimations)
        assertTrue(loaded.reducedMotionEnabled)
        assertEquals(DiceSkinOption.ROYAL_BLUE, loaded.diceSkin)
        assertEquals(TokenTrailOption.SPARK, loaded.tokenTrail)
        assertEquals(HapticThemeOption.SOFT, loaded.hapticTheme)
        assertEquals(SoundtrackOption.FESTIVAL, loaded.soundtrack)
        assertTrue(loaded.highContrastBoard)
        assertEquals(BotTurnPaceOption.QUICK, loaded.botTurnPace)
        assertTrue(loaded.manualBotRollConfirmation)
        assertTrue(loaded.shakeToRollEnabled)
        assertTrue(loaded.compactMatchUiEnabled)
    }
}
