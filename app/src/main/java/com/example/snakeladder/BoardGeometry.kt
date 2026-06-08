package com.example.snakeladder

import androidx.compose.ui.geometry.Offset

internal fun rowColToPosition(row: Int, col: Int): Int {
    val rowFromBottom = 9 - row
    val base = rowFromBottom * 10
    return if (rowFromBottom % 2 == 0) {
        base + col + 1
    } else {
        base + (10 - col)
    }
}

internal fun positionToRowCol(position: Int): Pair<Int, Int> {
    val safe = position.coerceIn(1, 100)
    val rowFromBottom = (safe - 1) / 10
    val withinRow = (safe - 1) % 10
    val col = if (rowFromBottom % 2 == 0) withinRow else 9 - withinRow
    val row = 9 - rowFromBottom
    return row to col
}

internal fun cellCenter(position: Int, cellWidth: Float, cellHeight: Float): Offset {
    val (row, col) = positionToRowCol(position)
    return Offset(
        x = col * cellWidth + (cellWidth / 2f),
        y = row * cellHeight + (cellHeight / 2f)
    )
}

internal fun cellCenterNormalized(position: Int): Offset {
    val (row, col) = positionToRowCol(position)
    return Offset(
        x = (col + 0.5f) / 10f,
        y = (row + 0.5f) / 10f
    )
}

internal fun buildLadderAnimationPath(start: Int, end: Int): List<Offset> {
    return listOf(
        cellCenterNormalized(start),
        cellCenterNormalized(end)
    )
}

internal fun buildSnakeAnimationPath(head: Int, tail: Int): List<Offset> {
    val headCenter = cellCenterNormalized(head)
    val tailCenter = cellCenterNormalized(tail)
    val steps = 7
    val points = mutableListOf<Offset>()
    points.add(headCenter)
    for (i in 1 until steps) {
        val t = i / steps.toFloat()
        val x = headCenter.x + (tailCenter.x - headCenter.x) * t
        val yBase = headCenter.y + (tailCenter.y - headCenter.y) * t
        val sway = if (i % 2 == 0) 0.02f else -0.02f
        points.add(Offset(x + sway, yBase))
    }
    points.add(tailCenter)
    return points
}
