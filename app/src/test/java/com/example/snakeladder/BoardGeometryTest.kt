package com.example.snakeladder

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardGeometryTest {

    @Test
    fun rowColToPosition_mapsSerpentineRows() {
        assertEquals(1, rowColToPosition(row = 9, col = 0))
        assertEquals(10, rowColToPosition(row = 9, col = 9))
        assertEquals(20, rowColToPosition(row = 8, col = 0))
        assertEquals(11, rowColToPosition(row = 8, col = 9))
        assertEquals(100, rowColToPosition(row = 0, col = 0))
        assertEquals(91, rowColToPosition(row = 0, col = 9))
    }

    @Test
    fun positionToRowCol_clampsAndMapsSerpentineRows() {
        assertEquals(9 to 0, positionToRowCol(0))
        assertEquals(9 to 0, positionToRowCol(1))
        assertEquals(9 to 9, positionToRowCol(10))
        assertEquals(8 to 9, positionToRowCol(11))
        assertEquals(8 to 0, positionToRowCol(20))
        assertEquals(0 to 0, positionToRowCol(100))
        assertEquals(0 to 0, positionToRowCol(101))
    }

    @Test
    fun cellCenter_usesMappedRowAndColumn() {
        assertEquals(Offset(5f, 95f), cellCenter(position = 1, cellWidth = 10f, cellHeight = 10f))
        assertEquals(Offset(95f, 95f), cellCenter(position = 10, cellWidth = 10f, cellHeight = 10f))
        assertEquals(Offset(95f, 85f), cellCenter(position = 11, cellWidth = 10f, cellHeight = 10f))
        assertEquals(Offset(5f, 5f), cellCenter(position = 100, cellWidth = 10f, cellHeight = 10f))
    }

    @Test
    fun cellCenterNormalized_returnsFractionalBoardCenter() {
        assertEquals(Offset(0.05f, 0.95f), cellCenterNormalized(1))
        assertEquals(Offset(0.95f, 0.85f), cellCenterNormalized(11))
        assertEquals(Offset(0.05f, 0.05f), cellCenterNormalized(100))
    }

    @Test
    fun buildLadderAnimationPath_connectsStartAndEndCenters() {
        val path = buildLadderAnimationPath(start = 2, end = 38)

        assertEquals(listOf(cellCenterNormalized(2), cellCenterNormalized(38)), path)
    }

    @Test
    fun buildSnakeAnimationPath_addsAlternatingSwayBetweenHeadAndTail() {
        val head = cellCenterNormalized(99)
        val tail = cellCenterNormalized(54)
        val path = buildSnakeAnimationPath(head = 99, tail = 54)

        assertEquals(8, path.size)
        assertEquals(head, path.first())
        assertEquals(tail, path.last())

        val firstSegmentT = 1f / 7f
        val firstLinearX = head.x + ((tail.x - head.x) * firstSegmentT)
        val secondSegmentT = 2f / 7f
        val secondLinearX = head.x + ((tail.x - head.x) * secondSegmentT)

        assertTrue(path[1].x < firstLinearX)
        assertTrue(path[2].x > secondLinearX)
    }
}
