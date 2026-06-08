package com.example.snakeladder

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.SystemClock
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
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
class SnakeLadderSoakTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun soak_rollSaveRotateLoop_threeMinutes_staysResponsive() {
        val context = composeRule.activity.applicationContext
        clearSavedGames(context)
        ensureBoardVisible()

        val soakSaveName = "Soak Save"
        val deadline = SystemClock.uptimeMillis() + 180_000L
        var iteration = 0

        while (SystemClock.uptimeMillis() < deadline) {
            composeRule.waitForIdle()

            if (nodeExistsByTag("winner_new_game_button")) {
                composeRule.onNodeWithTag("winner_new_game_button").performClick()
                ensureBoardVisible()
                continue
            }

            if (nodeExistsByText("Game Paused")) {
                composeRule.onNodeWithTag("pause_resume_button").performClick()
                waitForNodeByTag("dice_badge")
            }

            if (nodeExistsByTag("dice_badge")) {
                repeat(3) {
                    runCatching { composeRule.onNodeWithTag("dice_badge").performClick() }
                    Thread.sleep(35)
                }

                setOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

                if (iteration % 4 == 0) {
                    runCatching {
                        composeRule.onNodeWithTag("settings_button").performClick()
                        composeRule.onNodeWithTag("settings_save_game_button").performClick()
                        composeRule.onNodeWithTag("save_game_name_input").performTextInput(soakSaveName)
                        composeRule.onNodeWithTag("save_game_confirm_button").performClick()
                    }
                } else {
                    runCatching {
                        composeRule.onNodeWithTag("settings_button").performClick()
                        composeRule.onNodeWithTag("settings_pause_resume_button").performClick()
                        if (nodeExistsByText("Done")) {
                            composeRule.onNodeWithText("Done").performClick()
                        }
                        if (nodeExistsByText("Game Paused")) {
                            composeRule.onNodeWithTag("pause_resume_button").performClick()
                        }
                    }
                }
            }

            if (nodeExistsByTag("new_game_button")) {
                if (nodeExistsByTag("load_saved_game_button")) {
                    runCatching {
                        composeRule.onNodeWithTag("load_saved_game_button").performClick()
                        if (nodeExistsByText(soakSaveName)) {
                            composeRule.onNodeWithText(soakSaveName).performClick()
                        } else if (nodeExistsByText("Close")) {
                            composeRule.onNodeWithText("Close").performClick()
                            startNewGameFromLaunch()
                        } else {
                            startNewGameFromLaunch()
                        }
                    }
                } else {
                    startNewGameFromLaunch()
                }
            }

            ensureAnyPrimarySurfaceVisible()
            iteration++
        }

        ensureAnyPrimarySurfaceVisible()
        if (nodeExistsByTag("dice_badge")) {
            composeRule.onNodeWithTag("dice_badge").assertIsDisplayed()
        } else {
            composeRule.onNodeWithTag("new_game_button").assertIsEnabled()
        }
    }

    private fun ensureBoardVisible() {
        if (nodeExistsByTag("new_game_button")) {
            startNewGameFromLaunch()
        }
        waitForNodeByTag("dice_badge")
        composeRule.onNodeWithTag("dice_badge").assertIsDisplayed()
    }

    private fun startNewGameFromLaunch() {
        composeRule.onNodeWithTag("new_game_button").performClick()
        waitForNodeByTag("new_game_start_button")
        composeRule.onNodeWithTag("new_game_start_button").performClick()
    }

    private fun ensureAnyPrimarySurfaceVisible() {
        if (nodeExistsByTag("dice_badge") || nodeExistsByTag("new_game_button")) return
        waitForAnyPrimarySurface()
    }

    private fun setOrientation(orientation: Int) {
        composeRule.runOnUiThread {
            composeRule.activity.requestedOrientation = orientation
        }
        composeRule.waitForIdle()
    }

    private fun clearSavedGames(context: Context) {
        context.getSharedPreferences("snake_ladder_saved_games", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun nodeExistsByTag(tag: String): Boolean {
        return runCatching {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }.getOrDefault(false)
    }

    private fun nodeExistsByText(text: String): Boolean {
        return runCatching {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }.getOrDefault(false)
    }

    @OptIn(ExperimentalTestApi::class)
    private fun waitForNodeByTag(tag: String, timeoutMillis: Long = 10_000L) {
        composeRule.waitUntilAtLeastOneExists(
            hasTestTag(tag),
            timeoutMillis = timeoutMillis
        )
    }

    private fun waitForAnyPrimarySurface(timeoutMillis: Long = 10_000L) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (SystemClock.uptimeMillis() < deadline) {
            if (nodeExistsByTag("dice_badge") || nodeExistsByTag("new_game_button")) return
            Thread.sleep(60)
        }
        throw AssertionError("Neither board nor launch surface became visible within ${timeoutMillis}ms")
    }
}
