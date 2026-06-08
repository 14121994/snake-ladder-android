package com.example.snakeladder

import android.content.Context

internal object CustomBoardStore {
    private const val PREFS_NAME = "snake_ladder_custom_board"
    private const val KEY_SNAKES = "snakes"
    private const val KEY_LADDERS = "ladders"
    const val MAX_PAIRS = 12

    fun load(context: Context): BoardLayout {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val snakes = parsePairs(prefs.getString(KEY_SNAKES, "") ?: "")
        val ladders = parsePairs(prefs.getString(KEY_LADDERS, "") ?: "")
        val layout = BoardLayouts.customFromPairs(snakes, ladders)
        BoardLayouts.updateCustom(layout)
        return layout
    }

    fun save(context: Context, snakesText: String, laddersText: String): BoardLayout? {
        val snakes = parsePairs(snakesText)
        val ladders = parsePairs(laddersText)
        val validSnakes = snakes.filter { (head, tail) -> head in 2..99 && tail in 1 until head }
        val validLadders = ladders.filter { (start, end) -> start in 2..99 && end in (start + 1)..100 }
        if (validSnakes.isEmpty() && validLadders.isEmpty()) return null
        val layout = BoardLayouts.customFromPairs(
            snakePairs = validSnakes,
            ladderPairs = validLadders,
            preserveCurrentWhenEmpty = false
        )
        BoardLayouts.updateCustom(layout)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SNAKES, formatPairs(layout.snakes))
            .putString(KEY_LADDERS, formatPairs(layout.ladders))
            .apply()
        return layout
    }

    fun defaultSnakePairs(): List<Pair<Int, Int>> = BoardLayouts.defaultCustomLayout().snakes.entries
        .sortedBy { it.key }
        .map { it.key to it.value }

    fun defaultLadderPairs(): List<Pair<Int, Int>> = BoardLayouts.defaultCustomLayout().ladders.entries
        .sortedBy { it.key }
        .map { it.key to it.value }

    fun formatPairs(pairs: Map<Int, Int>): String {
        return pairs.entries.sortedBy { it.key }.joinToString(",") { "${it.key}-${it.value}" }
    }

    fun parsePairs(raw: String): List<Pair<Int, Int>> {
        return raw.split(',', ';', '\n')
            .mapNotNull { token ->
                val parts = token.trim().split('-', '>', ':').map { it.trim() }.filter { it.isNotBlank() }
                if (parts.size != 2) return@mapNotNull null
                val first = parts[0].toIntOrNull() ?: return@mapNotNull null
                val second = parts[1].toIntOrNull() ?: return@mapNotNull null
                first to second
            }
    }
}
