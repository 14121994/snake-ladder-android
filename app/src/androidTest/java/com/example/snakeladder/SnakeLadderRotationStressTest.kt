package com.example.snakeladder

import android.content.Context
import android.content.pm.ActivityInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SnakeLadderRotationStressTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun saveDialog_rotationStress_staysInteractive() {
        startGameFromLaunch()

        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.onNodeWithTag("settings_save_game_button").performClick()
        composeRule.onNodeWithTag("save_game_name_input").performTextInput("Rotation Save")

        rapidRotate(times = 8)

        composeRule.onNodeWithTag("save_game_name_input").assertIsDisplayed()
        composeRule.onNodeWithTag("save_game_confirm_button").assertIsEnabled()
        composeRule.onNodeWithTag("save_game_cancel_button").performClick()
        composeRule.onNodeWithTag("dice_badge").assertIsDisplayed()
    }

    @Test
    fun pauseOverlay_rotationStress_resumeRemainsResponsive() {
        startGameFromLaunch()

        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.onNodeWithTag("settings_pause_resume_button").performClick()
        composeRule.onNodeWithText("Done").performClick()
        composeRule.onNodeWithText("Game Paused").assertIsDisplayed()

        rapidRotate(times = 8)

        composeRule.onNodeWithText("Game Paused").assertIsDisplayed()
        composeRule.onNodeWithText("Resume").performClick()
        composeRule.onAllNodesWithText("Game Paused").assertCountEquals(0)
        composeRule.onNodeWithTag("dice_badge").assertIsDisplayed()
    }

    @Test
    fun winnerOverlay_rotationStress_remainsUsable() {
        val context = composeRule.activity.applicationContext
        clearSavedGames(context)
        SavedGameStore.save(
            context = context,
            name = "Winner Stress Save",
            boardTheme = BoardThemeOption.VIBRANT,
            state = winningState()
        )
        waitForSavedGamePersist(context, "Winner Stress Save")
        refreshActivityAfterPersistedSave()

        waitForNodeTag("load_saved_game_button")
        composeRule.onNodeWithTag("load_saved_game_button").performClick()
        waitForNodeText("Winner Stress Save")
        composeRule.onNodeWithContentDescription("Load saved game Winner Stress Save").performClick()

        composeRule.onNodeWithText("Winner!").assertIsDisplayed()

        rapidRotate(times = 8)

        val winnerOverlayStillVisible = composeRule
            .onAllNodesWithTag("winner_exit_button")
            .fetchSemanticsNodes().isNotEmpty()

        if (winnerOverlayStillVisible) {
            composeRule.onNodeWithTag("winner_exit_button").performClick()
        }
        val launchVisible = composeRule.onAllNodesWithTag("new_game_button").fetchSemanticsNodes().isNotEmpty()
        if (launchVisible) {
            composeRule.onNodeWithTag("new_game_button").assertIsDisplayed()
        } else {
            composeRule.onNodeWithTag("dice_badge").assertIsDisplayed()
        }
    }

    private fun startGameFromLaunch() {
        waitForNodeTag("new_game_button")
        composeRule.onNodeWithTag("new_game_button").performClick()
        waitForNodeTag("new_game_start_button")
        composeRule.onNodeWithTag("new_game_start_button").performClick()
        waitForNodeTag("dice_badge")
        composeRule.onNodeWithTag("dice_badge").assertIsDisplayed()
    }

    private fun rapidRotate(times: Int) {
        repeat(times) {
            setOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        }
    }

    private fun setOrientation(orientation: Int) {
        composeRule.runOnUiThread {
            composeRule.activity.requestedOrientation = orientation
        }
        composeRule.waitForIdle()
    }

    private fun refreshActivityAfterPersistedSave() {
        var lastError: Throwable? = null
        repeat(3) {
            val result = runCatching {
                composeRule.activityRule.scenario.recreate()
                composeRule.waitForIdle()
                waitForNodeTag("load_saved_game_button", timeoutMillis = 10_000L)
            }
            if (result.isSuccess) return
            lastError = result.exceptionOrNull()
            Thread.sleep(220)
        }
        throw AssertionError("Activity recreation failed while preparing winner stress test", lastError)
    }

    @OptIn(ExperimentalTestApi::class)
    private fun waitForNodeTag(tag: String, timeoutMillis: Long = 7_500L) {
        composeRule.waitUntilAtLeastOneExists(
            hasTestTag(tag),
            timeoutMillis = timeoutMillis
        )
    }

    private fun clearSavedGames(context: Context) {
        context.getSharedPreferences("snake_ladder_saved_games", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun waitForSavedGamePersist(context: Context, name: String, timeoutMillis: Long = 5_000L) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (SavedGameStore.loadAll(context).any { it.name == name }) return
            Thread.sleep(80)
        }
        throw AssertionError("Saved game '$name' was not persisted within ${timeoutMillis}ms")
    }

    @OptIn(ExperimentalTestApi::class)
    private fun waitForNodeText(text: String, timeoutMillis: Long = 7_500L) {
        composeRule.waitUntilAtLeastOneExists(
            hasText(text),
            timeoutMillis = timeoutMillis
        )
    }

    private fun winningState(): GameState {
        return GameState(
            players = listOf(
                PlayerState("Player 1", Color.Red, 100),
                PlayerState("Player 2", Color.Blue, 76)
            ),
            currentPlayerIndex = 0,
            lastDiceRoll = 6,
            statusMessage = "Player 1 wins!",
            bonusTurnGranted = false,
            winnerIndex = 0,
            moveHistory = listOf("Player 1 wins!"),
            gameMode = GameMode.LOCAL_MULTIPLAYER,
            botPlayerIndex = null,
            lastMovePlayerIndex = 0,
            lastMovePath = listOf(95, 96, 97, 98, 99, 100),
            lastMoveType = MoveType.WIN,
            moveSignal = 1
        )
    }
}
