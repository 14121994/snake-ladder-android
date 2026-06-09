package com.example.snakeladder

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal val strongSelectedChipContainer = Color(0xFF1557A8)
internal val strongSelectedChipBorder = Color(0xFF0B3F7C)
internal val strongSelectedCardContainer = Color(0xFFE8F1FF)
internal val strongSelectedCardBorder = Color(0xFF0B3F7C)
internal val neutralChipContainer = Color.Transparent
internal val neutralChipBorder = Color(0xFFD6E3F1)
internal val neutralChipLabel = Color(0xFF5D6E7F)
internal val lightUnselectedCardContainer = Color.Transparent
internal val lightUnselectedCardBorder = Color(0xFFE0E8F1)
internal val lightUnselectedCardLabel = Color(0xFF566879)
internal val lightUnselectedCardSupport = Color(0xFF748191)
internal val lightUnselectedMarkerContainer = Color(0xFFF7FAFD)

@Composable
internal fun strongFilterChipColors(): SelectableChipColors {
    return FilterChipDefaults.filterChipColors(
        containerColor = neutralChipContainer,
        labelColor = neutralChipLabel,
        iconColor = neutralChipLabel,
        selectedContainerColor = strongSelectedChipContainer,
        selectedLabelColor = Color.White,
        selectedLeadingIconColor = Color.White,
        selectedTrailingIconColor = Color.White
    )
}

@Composable
internal fun strongFilterChipBorder(
    selected: Boolean,
    enabled: Boolean = true
): BorderStroke {
    return FilterChipDefaults.filterChipBorder(
        enabled = enabled,
        selected = selected,
        borderColor = neutralChipBorder,
        selectedBorderColor = strongSelectedChipBorder,
        borderWidth = 1.dp,
        selectedBorderWidth = 2.dp
    )
}
