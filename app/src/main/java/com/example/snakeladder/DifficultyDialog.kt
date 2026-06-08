package com.example.snakeladder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun DifficultyDialog(
    title: String = "Select Difficulty",
    supportingText: String = "Pick how aggressive knockbacks should be. The board layout stays classic.",
    contextTitle: String? = null,
    contextBody: String? = null,
    badgeText: String? = null,
    contextPreview: (@Composable () -> Unit)? = null,
    onSelectDifficulty: (GameDifficulty) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold
                )
                badgeText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8A5A00)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5B4A42)
                )
                if (!contextTitle.isNullOrBlank() || !contextBody.isNullOrBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        contextTitle?.let {
                            Text(
                                text = it,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4E342E)
                            )
                        }
                        contextBody?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6B5A4D)
                            )
                        }
                    }
                }
                contextPreview?.invoke()
                DifficultyButton(
                    difficulty = GameDifficulty.EASY,
                    description = "Easy: no knockbacks when players share a cell.",
                    tag = "difficulty_easy_button",
                    onClick = { onSelectDifficulty(GameDifficulty.EASY) }
                )
                DifficultyButton(
                    difficulty = GameDifficulty.MEDIUM,
                    description = "Tactical: normal landings knock rivals back to start.",
                    tag = "difficulty_medium_button",
                    onClick = { onSelectDifficulty(GameDifficulty.MEDIUM) }
                )
                DifficultyButton(
                    difficulty = GameDifficulty.HARD,
                    description = "Pro: normal, snake, and ladder landings can knock rivals back.",
                    tag = "difficulty_hard_button",
                    onClick = { onSelectDifficulty(GameDifficulty.HARD) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DifficultyButton(
    difficulty: GameDifficulty,
    description: String,
    tag: String,
    onClick: () -> Unit
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DifficultyIconBadge(difficulty)
                Text(
                    text = difficulty.shortLabel(),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                text = difficulty.knockbackSymbolLabel(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DifficultyIconBadge(difficulty: GameDifficulty) {
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .size(26.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.22f))
            .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = difficulty.iconLabel(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}
