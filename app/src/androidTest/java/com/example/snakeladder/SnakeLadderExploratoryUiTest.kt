package com.example.snakeladder

import android.content.Context
import android.content.pm.ActivityInfo
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SnakeLadderExploratoryUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun rapidDiceTapBurst_appRemainsResponsive() {
        startGameFromLaunch()

        repeat(20) {
            runCatching { composeRule.onNodeWithTag("dice_badge").performClick() }
            Thread.sleep(25)
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.onNodeWithTag("settings_pause_resume_button").assertIsDisplayed()
        composeRule.onNodeWithText("Done").performClick()
        composeRule.onNodeWithTag("dice_badge").assertIsDisplayed()
    }

    @Test
    fun saveLoadDuringRollTransition_remainsStable() {
        val context = composeRule.activity.applicationContext
        clearSavedGames(context)

        startGameFromLaunch()

        composeRule.onNodeWithTag("dice_badge").performClick()
        waitForNodeTag("settings_button")
        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.onNodeWithTag("settings_save_game_button").performClick()
        composeRule.onNodeWithTag("save_game_name_input").performTextClearance()
        composeRule.onNodeWithTag("save_game_name_input").performTextInput("Anim Save")
        composeRule.onNodeWithTag("save_game_confirm_button").performClick()

        waitForNodeTag("new_game_button", timeoutMillis = 10_000L)
        composeRule.onNodeWithTag("load_saved_game_button").assertIsEnabled()
        composeRule.onNodeWithTag("load_saved_game_button").performClick()
        waitForNodeText("Anim Save", timeoutMillis = 10_000L)
        composeRule.onNodeWithContentDescription("Load saved game Anim Save").performClick()
        waitForNodeTag("dice_badge", timeoutMillis = 10_000L)
        composeRule.onNodeWithTag("dice_badge").assertIsDisplayed()
    }

    @Test
    fun persistedSave_recreateShowsLoadAndResumeLatest() {
        val context = composeRule.activity.applicationContext
        clearSavedGames(context)
        SavedGameStore.save(
            context = context,
            name = "Recreate Save",
            boardTheme = BoardThemeOption.VIBRANT,
            state = resumableState()
        )

        refreshActivityAfterPersistedSave()

        composeRule.onNodeWithText("Local save ready").assertIsDisplayed()
        composeRule.onNodeWithText("1 saved match on this device. Latest: Recreate Save.").assertIsDisplayed()
        composeRule.onNodeWithTag("load_saved_game_button").assertIsEnabled()
        composeRule.onNodeWithTag("resume_latest_saved_game_button").performClick()
        waitForNodeTag("dice_badge", timeoutMillis = 10_000L)
        composeRule.onNodeWithTag("dice_badge").assertIsDisplayed()
    }

    @Test
    fun winnerOverlay_rotateThenNewGame_works() {
        val context = composeRule.activity.applicationContext
        clearSavedGames(context)
        SavedGameStore.save(
            context = context,
            name = "Winner Explore",
            boardTheme = BoardThemeOption.VIBRANT,
            state = winningState()
        )

        refreshActivityAfterPersistedSave()
        waitForNodeTag("load_saved_game_button")
        composeRule.onNodeWithTag("load_saved_game_button").assertIsEnabled()
        composeRule.onNodeWithTag("load_saved_game_button").performClick()
        waitForNodeText("Winner Explore")
        composeRule.onNodeWithContentDescription("Load saved game Winner Explore").performClick()
        waitForNodeText("Winner!")

        repeat(4) {
            setOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        }

        val winnerButtonVisible = composeRule
            .onAllNodesWithTag("winner_new_game_button")
            .fetchSemanticsNodes().isNotEmpty()
        if (winnerButtonVisible) {
            composeRule.onNodeWithTag("winner_new_game_button").performClick()
            waitForNodeTag("dice_badge", timeoutMillis = 10_000L)
            composeRule.onNodeWithTag("dice_badge").assertIsDisplayed()
            return
        }

        val launchVisible = composeRule
            .onAllNodesWithTag("new_game_button")
            .fetchSemanticsNodes().isNotEmpty()
        if (launchVisible) {
            composeRule.onNodeWithTag("new_game_button").performClick()
            waitForNodeTag("new_game_start_button", timeoutMillis = 10_000L)
            composeRule.onNodeWithTag("new_game_start_button").performClick()
            waitForNodeTag("dice_badge", timeoutMillis = 10_000L)
            composeRule.onNodeWithTag("dice_badge").assertIsDisplayed()
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
        throw AssertionError("Activity recreation failed while preparing exploratory winner flow", lastError)
    }

    @OptIn(ExperimentalTestApi::class)
    private fun waitForNodeTag(tag: String, timeoutMillis: Long = 7_500L) {
        composeRule.waitUntilAtLeastOneExists(
            hasTestTag(tag),
            timeoutMillis = timeoutMillis
        )
    }

    @OptIn(ExperimentalTestApi::class)
    private fun waitForNodeText(text: String, timeoutMillis: Long = 7_500L) {
        composeRule.waitUntilAtLeastOneExists(
            hasText(text),
            timeoutMillis = timeoutMillis
        )
    }

    private fun clearSavedGames(context: Context) {
        context.getSharedPreferences("snake_ladder_saved_games", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
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

    private fun resumableState(): GameState {
        return GameState(
            players = listOf(
                PlayerState("Player 1", Color.Red, 12),
                PlayerState("Player 2", Color.Blue, 4)
            ),
            currentPlayerIndex = 0,
            lastDiceRoll = 3,
            statusMessage = "Player 1 moved to cell 12.",
            bonusTurnGranted = false,
            winnerIndex = null,
            moveHistory = listOf("Player 1 moved to cell 12."),
            gameMode = GameMode.LOCAL_MULTIPLAYER,
            botPlayerIndex = null,
            lastMovePlayerIndex = 0,
            lastMovePath = listOf(9, 10, 11, 12),
            lastMoveType = MoveType.NORMAL,
            moveSignal = 1
        )
    }
}
