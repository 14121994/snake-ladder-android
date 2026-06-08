package com.example.snakeladder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
internal fun ReplayDialog(
    events: List<MatchEvent>,
    onDismiss: () -> Unit
) {
    var selectedIndex by remember(events.size) { mutableIntStateOf(0) }
    var isPlaying by remember(events.size) { mutableStateOf(false) }
    var speedIndex by remember(events.size) { mutableIntStateOf(1) }
    val safeEvents = events.ifEmpty {
        listOf(
            MatchEvent(
                turnNumber = 0,
                playerIndex = 0,
                playerName = "No replay",
                dice = 1,
                startPosition = 1,
                landedPosition = 1,
                finalPosition = 1,
                moveType = MoveType.NORMAL,
                path = emptyList()
            )
        )
    }
    val event = safeEvents[selectedIndex.coerceIn(0, safeEvents.lastIndex)]
    val speedOptions = listOf(900L, 550L, 260L)

    LaunchedEffect(isPlaying, selectedIndex, speedIndex, safeEvents.size) {
        if (!isPlaying || safeEvents.size <= 1) return@LaunchedEffect
        delay(speedOptions[speedIndex.coerceIn(speedOptions.indices)])
        if (selectedIndex >= safeEvents.lastIndex) {
            isPlaying = false
        } else {
            selectedIndex += 1
        }
    }

    AlertDialog(
        modifier = Modifier.testTag("replay_dialog"),
        onDismissRequest = onDismiss,
        title = { Text("Match Replay", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Move ${selectedIndex + 1} / ${safeEvents.size} | Turn ${event.turnNumber.coerceAtLeast(1)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6D6259),
                    modifier = Modifier.testTag("replay_move_counter")
                )
                ReplayTimeline(
                    selectedIndex = selectedIndex,
                    total = safeEvents.size,
                    event = event
                )
                if (safeEvents.size > 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "Scrub turns",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4E342E),
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            modifier = Modifier.testTag("replay_scrubber"),
                            value = selectedIndex.toFloat(),
                            onValueChange = { value ->
                                isPlaying = false
                                selectedIndex = value.roundToInt().coerceIn(0, safeEvents.lastIndex)
                            },
                            valueRange = 0f..safeEvents.lastIndex.toFloat(),
                            steps = (safeEvents.size - 2).coerceAtLeast(0)
                        )
                    }
                }
                Card(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("replay_event_card")
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(event.replayTitle(), fontWeight = FontWeight.Bold, color = Color(0xFF4E342E))
                        Text(event.replayMovement(), style = MaterialTheme.typography.bodySmall)
                        Text(event.replayOutcome(), style = MaterialTheme.typography.bodySmall, color = Color(0xFF5F5B55))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("replay_play_pause_button"),
                        enabled = safeEvents.size > 1,
                        onClick = { isPlaying = !isPlaying }
                    ) {
                        Text(if (isPlaying) "Pause" else "Play")
                    }
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = { speedIndex = (speedIndex + 1) % speedOptions.size }
                    ) {
                        Text("${speedIndex + 1}x")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        enabled = selectedIndex > 0,
                        onClick = {
                            isPlaying = false
                            selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                        }
                    ) {
                        Text("Previous")
                    }
                    TextButton(
                        modifier = Modifier.weight(1f),
                        enabled = selectedIndex < safeEvents.lastIndex,
                        onClick = {
                            isPlaying = false
                            selectedIndex = (selectedIndex + 1).coerceAtMost(safeEvents.lastIndex)
                        }
                    ) {
                        Text("Next")
                    }
                }
                safeEvents.forEachIndexed { index, item ->
                    Text(
                        text = "Move ${index + 1}: Turn ${item.turnNumber.coerceAtLeast(1)} | ${item.playerName} ${item.startPosition} -> ${item.finalPosition}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (index == selectedIndex) Color(0xFF1565C0) else Color(0xFF5F5B55),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                isPlaying = false
                                selectedIndex = index
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("replay_move_row_${index + 1}")
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun ReplayTimeline(
    selectedIndex: Int,
    total: Int,
    event: MatchEvent
) {
    val progress = ((selectedIndex + 1).toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.testTag("replay_timeline")) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFE3D4BB))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(10.dp)
                    .background(Color(0xFF1565C0))
            )
        }
        Text(
            text = "Cell ${event.startPosition} -> ${event.finalPosition}",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF4E342E),
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun MatchEvent.replayTitle(): String {
    return if (moveType == MoveType.POWER_UP && powerUpUsed != null) {
        "$playerName used ${powerUpUsed.label}"
    } else {
        "$playerName rolled $dice"
    }
}

private fun MatchEvent.replayMovement(): String {
    return "Start $startPosition | Landed $landedPosition | Final $finalPosition"
}

private fun MatchEvent.replayOutcome(): String {
    val base = when (moveType) {
        MoveType.NORMAL -> "Normal move"
        MoveType.SNAKE -> "Snake slide"
        MoveType.LADDER -> "Ladder climb"
        MoveType.SHORTCUT -> "Shortcut route"
        MoveType.MYSTERY_TILE -> "Mystery tile"
        MoveType.RISK_ROUTE -> "Risk route"
        MoveType.BRANCH_PATH -> "Branch path"
        MoveType.OVERSHOOT -> "Exact finish missed"
        MoveType.WIN -> "Winning move"
        MoveType.POWER_UP -> "Power-up action"
        MoveType.TRAP -> "Trap triggered"
        MoveType.TIMEOUT -> "Timer resolved the match"
        MoveType.ROUND_WIN -> "Round captured"
    }
    val extras = buildList {
        if (powerUpUsed != null) add(powerUpUsed.label)
        if (triggeredPowerUps.isNotEmpty()) add("triggered ${triggeredPowerUps.joinToString { it.label }}")
        if (awardedPowerUps.isNotEmpty()) add("earned ${awardedPowerUps.joinToString { it.label }}")
        if (tileLabel != null) add(tileLabel)
        if (bonusTurn) add("bonus turn")
        if (knockedBackPlayerIndices.isNotEmpty()) add("${knockedBackPlayerIndices.size} knockback")
        if (winner) add("winner")
    }
    return if (extras.isEmpty()) base else "$base | ${extras.joinToString()}"
}
