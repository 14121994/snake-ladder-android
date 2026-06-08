package com.example.snakeladder

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameModelsTest {

    @Test
    fun enumsExposeExpectedValues() {
        assertEquals(listOf(GameMode.LOCAL_MULTIPLAYER, GameMode.VS_BOT), GameMode.entries.toList())
        assertEquals(listOf(GameDifficulty.EASY, GameDifficulty.MEDIUM, GameDifficulty.HARD), GameDifficulty.entries.toList())
        assertEquals(
            listOf(
                MoveType.NORMAL,
                MoveType.SNAKE,
                MoveType.LADDER,
                MoveType.SHORTCUT,
                MoveType.MYSTERY_TILE,
                MoveType.RISK_ROUTE,
                MoveType.BRANCH_PATH,
                MoveType.OVERSHOOT,
                MoveType.WIN,
                MoveType.POWER_UP,
                MoveType.TRAP,
                MoveType.TIMEOUT,
                MoveType.ROUND_WIN
            ),
            MoveType.entries.toList()
        )
        assertEquals(
            listOf(
                BoardThemeOption.VIBRANT,
                BoardThemeOption.PREMIUM_MUTED,
                BoardThemeOption.FESTIVAL,
                BoardThemeOption.MONSOON
            ),
            BoardThemeOption.entries.toList()
        )
        assertEquals(listOf(DiceSkinOption.CLASSIC_RED, DiceSkinOption.ROYAL_BLUE, DiceSkinOption.GOLD), DiceSkinOption.entries.toList())
        assertEquals(listOf(TokenTrailOption.NONE, TokenTrailOption.SPARK, TokenTrailOption.RIBBON), TokenTrailOption.entries.toList())
    }

    @Test
    fun dataModelsSupportCopyEqualityAndComponentAccess() {
        val player = PlayerState("Player 1", Color.Red, 12)
        val movedPlayer = player.copy(position = 20)
        val knockBackMove = KnockBackMove(playerIndex = 1, path = listOf(4, 3, 2, 1))
        val state = GameState(
            players = listOf(player, PlayerState("Player 2", Color.Blue, 4)),
            currentPlayerIndex = 0,
            lastDiceRoll = 3,
            statusMessage = "Player 1 rolled 3",
            bonusTurnGranted = false,
            winnerIndex = null,
            moveHistory = listOf("Player 1 rolled 3"),
            gameMode = GameMode.LOCAL_MULTIPLAYER,
            botPlayerIndex = null,
            lastMovePlayerIndex = 0,
            lastMovePath = listOf(10, 11, 12),
            lastMoveType = MoveType.NORMAL,
            moveSignal = 2,
            difficulty = GameDifficulty.HARD,
            knockBackMoves = listOf(knockBackMove)
        )
        val snapshot = SavedGameSnapshot(
            id = "save1",
            name = "Match",
            savedAt = 1234L,
            boardTheme = BoardThemeOption.PREMIUM_MUTED,
            state = state
        )
        val moveResult = MoveResult(
            position = 38,
            eventMessage = "Climbed ladder",
            moveType = MoveType.LADDER,
            path = listOf(2, 38)
        )

        assertEquals("Player 1", player.component1())
        assertEquals(Color.Red, player.component2())
        assertEquals(12, player.component3())
        assertEquals(null, player.teamId)
        assertEquals("classic_token", player.avatarId)
        assertEquals(20, movedPlayer.position)
        assertNotEquals(player, movedPlayer)
        assertEquals(1, knockBackMove.component1())
        assertEquals(listOf(4, 3, 2, 1), knockBackMove.component2())
        assertEquals(GameDifficulty.HARD, state.difficulty)
        assertEquals(listOf(knockBackMove), state.knockBackMoves)
        assertEquals(GameDifficulty.MEDIUM, state.copy(difficulty = GameDifficulty.MEDIUM).difficulty)
        assertEquals(BoardThemeOption.PREMIUM_MUTED, snapshot.component4())
        assertEquals(MoveType.LADDER, moveResult.component3())
        assertTrue(snapshot.toString().contains("Match"))
        assertTrue(moveResult.copy(moveType = MoveType.WIN).toString().contains("WIN"))
    }

    @Test
    fun zoomEffectModelsExposeAnimationValues() {
        val ladder = LadderZoomEffect(start = 2, end = 38, scale = 1.4f, alpha = 0.7f)
        val snake = SnakeZoomEffect(start = 99, end = 54, scale = 2.1f, alpha = 0.4f)

        assertEquals(2, ladder.component1())
        assertEquals(38, ladder.component2())
        assertEquals(1.4f, ladder.component3())
        assertEquals(0.7f, ladder.component4())
        assertEquals(LadderZoomEffect(2, 38, 1.4f, 0.7f), ladder.copy())

        assertEquals(99, snake.component1())
        assertEquals(54, snake.component2())
        assertEquals(2.1f, snake.component3())
        assertEquals(0.4f, snake.component4())
        assertEquals(SnakeZoomEffect(99, 54, 2.1f, 0.4f), snake.copy())
        assertTrue(snake.toString().contains("SnakeZoomEffect"))
    }
}
