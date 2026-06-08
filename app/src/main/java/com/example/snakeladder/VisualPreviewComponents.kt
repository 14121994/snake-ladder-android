package com.example.snakeladder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class PreviewThemePalette(
    val frameBackground: Color,
    val frameBorder: Color,
    val cellBorder: Color,
    val numberBadge: Color,
    val cellPalette: List<Color>
)

private fun previewThemePalette(option: BoardThemeOption): PreviewThemePalette {
    return when (option) {
        BoardThemeOption.VIBRANT -> PreviewThemePalette(
            frameBackground = Color(0xFFFFF9F1),
            frameBorder = Color(0xFFE7D2B5),
            cellBorder = Color(0xFFD6C2A5),
            numberBadge = Color(0xCCFFF8E8),
            cellPalette = listOf(Color(0xFFFFE0B2), Color(0xFFC8E6C9), Color(0xFFB3E5FC), Color(0xFFE1BEE7))
        )
        BoardThemeOption.PREMIUM_MUTED -> PreviewThemePalette(
            frameBackground = Color(0xFFF7F4EE),
            frameBorder = Color(0xFFD9D2C6),
            cellBorder = Color(0xFFCFC6B7),
            numberBadge = Color(0xCCFCFAF6),
            cellPalette = listOf(Color(0xFFF2E7DA), Color(0xFFDCE8DE), Color(0xFFDDE5EE), Color(0xFFE8DFEC))
        )
        BoardThemeOption.FESTIVAL -> PreviewThemePalette(
            frameBackground = Color(0xFFFFF7ED),
            frameBorder = Color(0xFFE5B85A),
            cellBorder = Color(0xFFD79B32),
            numberBadge = Color(0xCCFFF8E1),
            cellPalette = listOf(Color(0xFFFFCC80), Color(0xFFFFF59D), Color(0xFFFFAB91), Color(0xFFA5D6A7))
        )
        BoardThemeOption.MONSOON -> PreviewThemePalette(
            frameBackground = Color(0xFFE8F5F7),
            frameBorder = Color(0xFF8AB6BE),
            cellBorder = Color(0xFF83AEB8),
            numberBadge = Color(0xCCEAF7FA),
            cellPalette = listOf(Color(0xFFB2EBF2), Color(0xFFC8E6C9), Color(0xFFD7E7F7), Color(0xFFE0F2F1))
        )
    }
}

internal fun tileAccentColor(type: BoardTileType): Color {
    return when (type) {
        BoardTileType.TRAP -> Color(0xFFD81B60)
        BoardTileType.MYSTERY -> Color(0xFF7E57C2)
        BoardTileType.SHORTCUT -> Color(0xFF00897B)
        BoardTileType.RISK_ROUTE -> Color(0xFFF57C00)
        BoardTileType.BRANCH_PATH -> Color(0xFF1565C0)
    }
}

internal fun tileGlyph(type: BoardTileType): String {
    return when (type) {
        BoardTileType.TRAP -> "!"
        BoardTileType.MYSTERY -> "?"
        BoardTileType.SHORTCUT -> "+"
        BoardTileType.RISK_ROUTE -> "~"
        BoardTileType.BRANCH_PATH -> "Y"
    }
}

internal fun avatarTokenColors(avatarId: String, fallback: Color = Color(0xFF1976D2)): List<Color> {
    return when (avatarId) {
        "cobra_token" -> listOf(Color(0xFFA5D6A7), Color(0xFF2E7D32), Color(0xFF0B3D16))
        "ladder_king" -> listOf(Color(0xFFFFF59D), Color(0xFFFFB300), Color(0xFF6D4300))
        "gold_die" -> listOf(Color(0xFFFFE082), Color(0xFFFF8F00), Color(0xFF4A2B00))
        else -> listOf(fallback.copy(alpha = 0.92f), fallback, fallback.copy(alpha = 0.72f))
    }
}

internal fun avatarTokenBorder(avatarId: String): Color {
    return when (avatarId) {
        "cobra_token" -> Color(0xFFE8F5E9)
        "ladder_king", "gold_die" -> Color(0xFFFFF8E1)
        else -> Color.White
    }
}

private fun avatarGlyph(avatarId: String): String {
    return when (avatarId) {
        "cobra_token" -> "S"
        "ladder_king" -> "L"
        "gold_die" -> "D"
        else -> "P"
    }
}

internal fun suggestedPreviewTheme(boardLayoutId: String): BoardThemeOption {
    return when (boardLayoutId) {
        BoardLayouts.FESTIVAL_EVENT_ID -> BoardThemeOption.FESTIVAL
        BoardLayouts.MONSOON_EVENT_ID -> BoardThemeOption.MONSOON
        BoardLayouts.PRO_CHAOS_ID -> BoardThemeOption.PREMIUM_MUTED
        else -> BoardThemeOption.VIBRANT
    }
}

internal fun boardThemeDisplayLabel(option: BoardThemeOption): String {
    return when (option) {
        BoardThemeOption.VIBRANT -> "Vibrant"
        BoardThemeOption.PREMIUM_MUTED -> "Premium Muted"
        BoardThemeOption.FESTIVAL -> "Festival"
        BoardThemeOption.MONSOON -> "Monsoon"
    }
}

internal fun boardThemeSupportLabel(option: BoardThemeOption): String {
    return when (option) {
        BoardThemeOption.VIBRANT -> "Bright classic board"
        BoardThemeOption.PREMIUM_MUTED -> "Calmer tournament look"
        BoardThemeOption.FESTIVAL -> "Warm event palette"
        BoardThemeOption.MONSOON -> "Cool rainy palette"
    }
}

private fun boardLayoutSupportLabel(layout: BoardLayout): String {
    return when {
        layout.specialTiles.isNotEmpty() -> "${layout.specialTiles.size} special tiles"
        layout.snakes.size > layout.ladders.size -> "Snake pressure board"
        layout.ladders.size > layout.snakes.size -> "Ladder-heavy board"
        else -> "Balanced classic board"
    }
}

private fun boardLayoutStatsLabel(layout: BoardLayout): String {
    return buildList {
        add("${layout.ladders.size} ladders")
        add("${layout.snakes.size} snakes")
        if (layout.specialTiles.isNotEmpty()) add("${layout.specialTiles.size} specials")
    }.joinToString(" | ")
}

private fun boardLayoutCompactStatsLabel(layout: BoardLayout): String {
    return buildList {
        add("L${layout.ladders.size}")
        add("S${layout.snakes.size}")
        if (layout.specialTiles.isNotEmpty()) add("Sp${layout.specialTiles.size}")
    }.joinToString(" | ")
}

private fun dicePreviewGradientColors(skin: DiceSkinOption): List<Color> {
    return when (skin) {
        DiceSkinOption.CLASSIC_RED -> listOf(Color(0xFFF04A40), Color(0xFFC82024), Color(0xFF7A1117))
        DiceSkinOption.ROYAL_BLUE -> listOf(Color(0xFF42A5F5), Color(0xFF1565C0), Color(0xFF0D2B66))
        DiceSkinOption.GOLD -> listOf(Color(0xFFFFE082), Color(0xFFFFB300), Color(0xFF7A4F00))
    }
}

private fun dicePreviewEdgeColor(skin: DiceSkinOption): Color {
    return when (skin) {
        DiceSkinOption.CLASSIC_RED -> Color(0xFF4E090D)
        DiceSkinOption.ROYAL_BLUE -> Color(0xFF08224F)
        DiceSkinOption.GOLD -> Color(0xFF4B3000)
    }
}

private fun diceSkinSupportLabel(skin: DiceSkinOption): String {
    return when (skin) {
        DiceSkinOption.CLASSIC_RED -> "Tournament default"
        DiceSkinOption.ROYAL_BLUE -> "Cool high-contrast"
        DiceSkinOption.GOLD -> "Premium finish"
    }
}

private fun tokenTrailSupportLabel(option: TokenTrailOption): String {
    return when (option) {
        TokenTrailOption.NONE -> "No trail"
        TokenTrailOption.SPARK -> "Spark burst"
        TokenTrailOption.RIBBON -> "Long ribbon"
    }
}

@Composable
internal fun MiniBoardPreview(
    boardLayoutId: String,
    boardThemeOption: BoardThemeOption = suggestedPreviewTheme(boardLayoutId),
    modifier: Modifier = Modifier,
    layoutOverride: BoardLayout? = null,
    showSpecialTiles: Boolean = true
) {
    val layout = layoutOverride ?: BoardLayouts.byId(boardLayoutId)
    val theme = previewThemePalette(boardThemeOption)
    Canvas(modifier = modifier) {
        val outerPadding = size.minDimension * 0.06f
        val boardSize = size.minDimension - (outerPadding * 2f)
        val left = (size.width - boardSize) / 2f
        val top = (size.height - boardSize) / 2f
        val cellSize = boardSize / 10f

        drawRoundRect(
            color = theme.frameBackground,
            topLeft = Offset(left, top),
            size = Size(boardSize, boardSize),
            cornerRadius = CornerRadius(boardSize * 0.06f, boardSize * 0.06f)
        )
        drawRoundRect(
            color = theme.frameBorder,
            topLeft = Offset(left, top),
            size = Size(boardSize, boardSize),
            cornerRadius = CornerRadius(boardSize * 0.06f, boardSize * 0.06f),
            style = Stroke(width = boardSize * 0.015f)
        )

        for (row in 0 until 10) {
            for (col in 0 until 10) {
                val cellLeft = left + (col * cellSize)
                val cellTop = top + (row * cellSize)
                val cellColor = theme.cellPalette[(row + col) % theme.cellPalette.size]
                drawRoundRect(
                    color = cellColor,
                    topLeft = Offset(cellLeft, cellTop),
                    size = Size(cellSize, cellSize)
                )
                drawRoundRect(
                    color = theme.cellBorder,
                    topLeft = Offset(cellLeft, cellTop),
                    size = Size(cellSize, cellSize),
                    style = Stroke(width = boardSize * 0.003f)
                )
            }
        }

        layout.ladders.forEach { (start, end) ->
            val startCenter = previewCellCenter(start, left, top, cellSize)
            val endCenter = previewCellCenter(end, left, top, cellSize)
            drawLine(
                color = Color(0xFF4E342E),
                start = startCenter,
                end = endCenter,
                strokeWidth = boardSize * 0.018f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFFD7CCC8),
                start = startCenter,
                end = endCenter,
                strokeWidth = boardSize * 0.011f,
                cap = StrokeCap.Round
            )
        }

        layout.snakes.forEach { (head, tail) ->
            val headCenter = previewCellCenter(head, left, top, cellSize)
            val tailCenter = previewCellCenter(tail, left, top, cellSize)
            val path = Path().apply {
                moveTo(headCenter.x, headCenter.y)
                quadraticBezierTo(
                    (headCenter.x + tailCenter.x) / 2f,
                    ((headCenter.y + tailCenter.y) / 2f) + (cellSize * 0.7f),
                    tailCenter.x,
                    tailCenter.y
                )
            }
            drawPath(
                path = path,
                color = Color(0xFF5A1010),
                style = Stroke(width = boardSize * 0.018f, cap = StrokeCap.Round)
            )
            drawPath(
                path = path,
                color = Color(0xFFE53935),
                style = Stroke(width = boardSize * 0.011f, cap = StrokeCap.Round)
            )
        }

        if (showSpecialTiles) {
            layout.specialTiles.forEach { tile ->
                val badgeCenter = previewCellCornerBadgeCenter(tile.cell, left, top, cellSize)
                val accent = tileAccentColor(tile.type)
                val badgeRadius = cellSize * 0.16f
                drawCircle(
                    color = Color.White.copy(alpha = 0.94f),
                    radius = badgeRadius,
                    center = badgeCenter
                )
                drawCircle(
                    color = accent,
                    radius = badgeRadius,
                    center = badgeCenter,
                    style = Stroke(width = boardSize * 0.008f)
                )
                drawCircle(
                    color = accent.copy(alpha = 0.75f),
                    radius = badgeRadius * 0.34f,
                    center = badgeCenter
                )
            }
        }

        val badgeWidth = cellSize * 1.15f
        val badgeHeight = cellSize * 0.52f
        drawRoundRect(
            color = theme.numberBadge,
            topLeft = Offset(left + (cellSize * 0.12f), top + (cellSize * 0.12f)),
            size = Size(badgeWidth, badgeHeight),
            cornerRadius = CornerRadius(cellSize * 0.18f, cellSize * 0.18f)
        )
    }
}

private fun previewCellCenter(position: Int, left: Float, top: Float, cellSize: Float): Offset {
    val (row, col) = positionToRowCol(position)
    return Offset(
        x = left + (col * cellSize) + (cellSize / 2f),
        y = top + (row * cellSize) + (cellSize / 2f)
    )
}

private fun previewCellCornerBadgeCenter(position: Int, left: Float, top: Float, cellSize: Float): Offset {
    val (row, col) = positionToRowCol(position)
    return Offset(
        x = left + (col * cellSize) + (cellSize * 0.76f),
        y = top + (row * cellSize) + (cellSize * 0.26f)
    )
}

@Composable
internal fun AvatarTokenPreview(
    avatarId: String,
    modifier: Modifier = Modifier,
    fallbackColor: Color = Color(0xFF1976D2)
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Brush.radialGradient(colors = avatarTokenColors(avatarId, fallbackColor)))
            .border(1.8.dp, avatarTokenBorder(avatarId), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = avatarGlyph(avatarId),
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp
        )
    }
}

@Composable
internal fun TitleBadgePreview(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFFFF8E7))
            .border(1.dp, Color(0xFFE2C99F), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = Color(0xFF6D3E00)
        )
    }
}

@Composable
internal fun BoardThemeOptionCard(
    theme: BoardThemeOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val taggedModifier = if (testTag != null) modifier.testTag(testTag) else modifier
    Card(
        modifier = taggedModifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color(0xFF1565C0) else Color(0xFFD4C7B7),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MiniBoardPreview(
                boardLayoutId = BoardLayouts.CLASSIC_ID,
                boardThemeOption = theme,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp),
                showSpecialTiles = false
            )
            Text(
                text = boardThemeDisplayLabel(theme),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = boardThemeSupportLabel(theme),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6D6259),
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
internal fun BoardLayoutSelectionCard(
    board: BoardLayout,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color(0xFF1565C0) else Color(0xFFD4C7B7),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MiniBoardPreview(
                boardLayoutId = board.id,
                boardThemeOption = suggestedPreviewTheme(board.id),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp)
            )
            Text(
                text = board.label,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF4E342E),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = boardLayoutSupportLabel(board),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6D6259),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )
            Text(
                text = boardLayoutCompactStatsLabel(board),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8A3D00),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun DiceSkinOptionCard(
    skin: DiceSkinOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color(0xFF1565C0) else Color(0xFFD4C7B7),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FBFF))
                    .border(1.dp, Color(0xFFD4E0EE), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(dicePreviewGradientColors(skin)))
                        .border(2.dp, dicePreviewEdgeColor(skin), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        val pipRadius = size.minDimension * 0.09f
                        val positions = listOf(
                            Offset(size.width * 0.28f, size.height * 0.28f),
                            Offset(size.width * 0.72f, size.height * 0.28f),
                            Offset(size.width * 0.50f, size.height * 0.50f),
                            Offset(size.width * 0.28f, size.height * 0.72f),
                            Offset(size.width * 0.72f, size.height * 0.72f)
                        )
                        positions.forEach { center ->
                            drawCircle(
                                color = Color.White.copy(alpha = 0.96f),
                                radius = pipRadius,
                                center = center
                            )
                        }
                    }
                }
            }
            Text(
                text = skin.label,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF4E342E)
            )
            Text(
                text = diceSkinSupportLabel(skin),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6D6259)
            )
        }
    }
}

@Composable
internal fun TokenTrailOptionCard(
    option: TokenTrailOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color(0xFF1565C0) else Color(0xFFD4C7B7),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FBFF))
                    .border(1.dp, Color(0xFFD4E0EE), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerY = size.height / 2f
                    when (option) {
                        TokenTrailOption.NONE -> Unit
                        TokenTrailOption.SPARK -> {
                            listOf(0.18f, 0.33f, 0.48f, 0.62f).forEachIndexed { index, fraction ->
                                drawCircle(
                                    color = if (index % 2 == 0) Color(0xFFFFB300) else Color(0xFF42A5F5),
                                    radius = size.minDimension * (0.055f - (index * 0.007f)),
                                    center = Offset(size.width * fraction, centerY + if (index % 2 == 0) -8f else 8f)
                                )
                            }
                        }
                        TokenTrailOption.RIBBON -> {
                            val ribbon = Path().apply {
                                moveTo(size.width * 0.14f, centerY + 10f)
                                cubicTo(
                                    size.width * 0.28f, centerY - 12f,
                                    size.width * 0.52f, centerY + 18f,
                                    size.width * 0.74f, centerY - 6f
                                )
                            }
                            drawPath(
                                path = ribbon,
                                color = Color(0xFF7E57C2),
                                style = Stroke(width = size.minDimension * 0.14f, cap = StrokeCap.Round)
                            )
                            drawPath(
                                path = ribbon,
                                color = Color(0xFFD1C4E9),
                                style = Stroke(width = size.minDimension * 0.07f, cap = StrokeCap.Round)
                            )
                        }
                    }
                }
                AvatarTokenPreview(
                    avatarId = "classic_token",
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = option.label,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF4E342E)
            )
            Text(
                text = tokenTrailSupportLabel(option),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6D6259)
            )
        }
    }
}
