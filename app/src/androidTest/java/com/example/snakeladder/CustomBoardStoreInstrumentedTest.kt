package com.example.snakeladder

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomBoardStoreInstrumentedTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        BoardLayouts.updateCustom(defaultCustomBoard())
    }

    @Test
    fun saveLoadPersistsSanitizedCustomBoardPairs() {
        val saved = CustomBoardStore.save(
            context = context,
            snakesText = "97-61, junk, 70->42",
            laddersText = "4-29;18:46;99-98"
        ) ?: error("Expected custom board to save")

        val loaded = CustomBoardStore.load(context)

        assertEquals(61, saved.snakes[97])
        assertEquals(42, saved.snakes[70])
        assertEquals(29, loaded.ladders[4])
        assertEquals(46, loaded.ladders[18])
        assertFalse(99 in loaded.ladders)
        assertEquals("70-42,97-61", CustomBoardStore.formatPairs(loaded.snakes))
        assertEquals(saved.snakes, loaded.snakes)
        assertEquals(saved.ladders, loaded.ladders)
    }

    @Test
    fun saveRejectsBlankBoardWithoutChangingStoredValues() {
        val rejected = CustomBoardStore.save(
            context = context,
            snakesText = "not a pair",
            laddersText = "   "
        )

        assertNull(rejected)
        assertFalse(
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .contains(KEY_SNAKES)
        )
        assertTrue(CustomBoardStore.load(context).snakes.isNotEmpty())
    }

    private fun defaultCustomBoard(): BoardLayout {
        return BoardLayout(
            id = BoardLayouts.CUSTOM_ID,
            label = "Custom Lab",
            description = "Player-edited board saved on this device.",
            snakes = mapOf(97 to 61, 70 to 42, 54 to 23),
            ladders = mapOf(4 to 29, 18 to 46, 40 to 73, 66 to 88),
            specialTiles = listOf(
                BoardTile(22, BoardTileType.MYSTERY, rewardPowerUp = PowerUpType.REROLL),
                BoardTile(58, BoardTileType.SHORTCUT, targetCell = 78)
            )
        )
    }

    private companion object {
        const val PREFS_NAME = "snake_ladder_custom_board"
        const val KEY_SNAKES = "snakes"
    }
}
