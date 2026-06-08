package com.example.snakeladder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.snakeladder.ui.theme.SnakeLadderTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BoardRenderingCoverageTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun diceBadge_drawsStaticFacesAndRollingCubeFrames() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            SnakeLadderTheme {
                Column(modifier = Modifier.size(width = 520.dp, height = 180.dp)) {
                    Row {
                        (1..6).forEach { face ->
                            DiceBadge(
                                value = face,
                                isRolling = false,
                                enabled = false,
                                boxSize = 42.dp,
                                outerHorizontalPadding = 2.dp,
                                outerVerticalPadding = 2.dp,
                                onClick = {}
                            )
                        }
                    }
                    DiceBadge(
                        value = 9,
                        isRolling = true,
                        enabled = true,
                        boxSize = 72.dp,
                        outerHorizontalPadding = 2.dp,
                        outerVerticalPadding = 2.dp,
                        onClick = {}
                    )
                }
            }
        }

        repeat(7) {
            composeRule.mainClock.advanceTimeBy(90L)
            val image = composeRule.onRoot().captureToImage()
            assertTrue(image.width > 0)
            assertTrue(image.height > 0)
        }
        composeRule.mainClock.autoAdvance = true
    }

    @Test
    fun board_drawsZoomEffectsThemesOverridesAndTokenLayouts() {
        val crowdedState = sampleState(
            players = listOf(
                PlayerState("Player 1", Color.Red, 5),
                PlayerState("Player 2", Color.Blue, 5),
                PlayerState("Player 3", Color.Green, 5),
                PlayerState("Player 4", Color(0xFFF57C00), 5)
            )
        )
        val winnerState = sampleState(
            players = listOf(
                PlayerState("Player 1", Color.Red, 10),
                PlayerState("Player 2", Color.Blue, 10),
                PlayerState("Player 3", Color.Green, 11),
                PlayerState("Player 4", Color(0xFFF57C00), 12)
            ),
            winnerIndex = 0
        )

        var showWinnerBoard by mutableStateOf(false)
        var composed = false
        composeRule.setContent {
            SnakeLadderTheme {
                SideEffect {
                    composed = true
                }
                Column(modifier = Modifier.width(320.dp)) {
                    if (showWinnerBoard) {
                        Board(
                            state = winnerState,
                            displayPositions = listOf(10f, 10.4f, 11f, 12f),
                            tokenVisualOverrides = emptyMap(),
                            ladderZoomEffect = null,
                            snakeZoomEffect = null,
                            boardThemeOption = BoardThemeOption.VIBRANT
                        )
                    } else {
                        Board(
                            state = crowdedState,
                            displayPositions = listOf(5f, 5f, 5f, 5f),
                            tokenVisualOverrides = mapOf(0 to Offset(-0.2f, 1.2f), 1 to null, 2 to null, 3 to null),
                            ladderZoomEffect = LadderZoomEffect(start = 2, end = 38, scale = 2.2f, alpha = 0.65f),
                            snakeZoomEffect = SnakeZoomEffect(start = 99, end = 54, scale = 1.8f, alpha = 0.7f),
                            boardThemeOption = BoardThemeOption.PREMIUM_MUTED
                        )
                    }
                }
            }
        }

        composeRule.runOnIdle {
            assertTrue(composed)
        }
        val image = composeRule.onRoot().captureToImage()
        assertTrue(image.width > 0)
        assertTrue(image.height > 0)

        composed = false
        composeRule.runOnIdle {
            showWinnerBoard = true
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertTrue(composed)
        }
        val winnerImage = composeRule.onRoot().captureToImage()
        assertTrue(winnerImage.width > 0)
        assertTrue(winnerImage.height > 0)
    }

    private fun sampleState(
        players: List<PlayerState>,
        winnerIndex: Int? = null
    ): GameState {
        return GameState(
            players = players,
            currentPlayerIndex = 0,
            lastDiceRoll = 4,
            statusMessage = "Rendering coverage",
            bonusTurnGranted = false,
            winnerIndex = winnerIndex,
            moveHistory = emptyList(),
            gameMode = GameMode.LOCAL_MULTIPLAYER,
            botPlayerIndex = null,
            lastMovePlayerIndex = null,
            lastMovePath = emptyList(),
            lastMoveType = null,
            moveSignal = 0
        )
    }
}
