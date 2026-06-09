package com.example.snakeladder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun ProFeatureHubDialog(
    onDismiss: () -> Unit
) {
    var selectedCategory by rememberSaveable { mutableStateOf<ProFeatureCategory?>(null) }
    var selectedStatus by rememberSaveable { mutableStateOf<ProFeatureStatus?>(null) }
    var expandedFeatureId by rememberSaveable { mutableStateOf<String?>(null) }
    val statusCounts = ProFeatureCatalog.statusCounts()
    val filteredFeatures = ProFeatureCatalog.features.filter { feature ->
        FeatureFlags.isFeatureVisible(feature) &&
            (selectedCategory == null || feature.category == selectedCategory) &&
            (selectedStatus == null || feature.status == selectedStatus)
    }

    AlertDialog(
        modifier = Modifier.testTag("pro_feature_hub_dialog"),
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Feature Guide", fontWeight = FontWeight.Bold)
                Text(
                    text = "Playable modes, rewards, and future ideas in plain language.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6D6259)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureSummaryRow(statusCounts = statusCounts)
                Text(
                    text = "Online-only ideas stay out of normal setup until they are playable on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6D6259),
                    modifier = Modifier.testTag("backend_features_hidden_notice")
                )
                StatusFilterRow(
                    selectedStatus = selectedStatus,
                    onSelectStatus = { selectedStatus = it }
                )
                CategoryFilter(
                    selectedCategory = selectedCategory,
                    onSelectCategory = { selectedCategory = it }
                )
                filteredFeatures.forEachIndexed { index, feature ->
                    ProFeatureRow(
                        index = index + 1,
                        feature = feature,
                        expanded = expandedFeatureId == feature.id,
                        onToggle = {
                            expandedFeatureId = if (expandedFeatureId == feature.id) null else feature.id
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun FeatureSummaryRow(statusCounts: Map<ProFeatureStatus, Int>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pro_feature_count"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "${ProFeatureCatalog.features.size} feature ideas",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = buildString {
                val playableNow = (statusCounts[ProFeatureStatus.IN_APP_FOUNDATION] ?: 0) +
                    (statusCounts[ProFeatureStatus.OFFLINE_READY] ?: 0)
                append("$playableNow playable now")
                append(" | ${statusCounts[ProFeatureStatus.BACKEND_REQUIRED] ?: 0} online ideas")
            },
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF5E5A55)
        )
    }
}

@Composable
private fun StatusFilterRow(
    selectedStatus: ProFeatureStatus?,
    onSelectStatus: (ProFeatureStatus?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Status",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag("pro_feature_status_chips"),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf<ProFeatureStatus?>(null, *ProFeatureStatus.entries.toTypedArray()).forEach { status ->
                val label = status?.label ?: "All"
                FilterChip(
                    selected = selectedStatus == status,
                    onClick = { onSelectStatus(status) },
                    colors = strongFilterChipColors(),
                    border = strongFilterChipBorder(selected = selectedStatus == status),
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryFilter(
    selectedCategory: ProFeatureCategory?,
    onSelectCategory: (ProFeatureCategory?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Feature groups",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
        val categories = listOf<ProFeatureCategory?>(
            null,
            ProFeatureCategory.SOCIAL,
            ProFeatureCategory.COMPETITIVE,
            ProFeatureCategory.PROGRESSION,
            ProFeatureCategory.BOT_AI,
            ProFeatureCategory.MATCH_MODES,
            ProFeatureCategory.BOARD_RULES,
            ProFeatureCategory.CUSTOMIZATION,
            ProFeatureCategory.LIVE_OPS
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag("pro_feature_category_chips"),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { category ->
                val label = category?.chipLabel() ?: "All"
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onSelectCategory(category) },
                    colors = strongFilterChipColors(),
                    border = strongFilterChipBorder(selected = selectedCategory == category),
                    label = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = category?.marker() ?: "*",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                            Text(
                                text = label,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryBadge(category: ProFeatureCategory) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(category.tint().copy(alpha = 0.12f))
            .border(1.dp, category.tint().copy(alpha = 0.32f), RoundedCornerShape(999.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(category.marker(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = category.tint())
        Text(category.chipLabel(), style = MaterialTheme.typography.labelSmall, color = Color(0xFF4E342E), maxLines = 1)
    }
}

@Composable
private fun StatusBadge(status: ProFeatureStatus) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(status.statusColor().copy(alpha = 0.12f))
            .border(1.dp, status.statusColor().copy(alpha = 0.32f), RoundedCornerShape(999.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(status.marker(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = status.statusColor())
        Text(status.label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF4E342E), maxLines = 1)
    }
}

@Composable
private fun DetailsHint(expanded: Boolean) {
    Text(
        text = if (expanded) "Hide details" else "Show details",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF0D47A1),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFE3F2FD))
            .border(1.dp, Color(0xFF90CAF9), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun FeatureGlyph(category: ProFeatureCategory) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(category.tint().copy(alpha = 0.14f))
            .border(1.dp, category.tint().copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = category.marker(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = category.tint(),
            maxLines = 1
        )
    }
}

private fun ProFeatureCategory.chipLabel(): String {
    return when (this) {
        ProFeatureCategory.SOCIAL -> "Social"
        ProFeatureCategory.COMPETITIVE -> "Ranked"
        ProFeatureCategory.PROGRESSION -> "Progress"
        ProFeatureCategory.BOT_AI -> "Bots"
        ProFeatureCategory.MATCH_MODES -> "Modes"
        ProFeatureCategory.BOARD_RULES -> "Rules"
        ProFeatureCategory.CUSTOMIZATION -> "Custom"
        ProFeatureCategory.LIVE_OPS -> "Events"
    }
}

private fun ProFeatureCategory.marker(): String {
    return when (this) {
        ProFeatureCategory.SOCIAL -> "SO"
        ProFeatureCategory.COMPETITIVE -> "VS"
        ProFeatureCategory.PROGRESSION -> "XP"
        ProFeatureCategory.BOT_AI -> "AI"
        ProFeatureCategory.MATCH_MODES -> "MD"
        ProFeatureCategory.BOARD_RULES -> "RL"
        ProFeatureCategory.CUSTOMIZATION -> "CU"
        ProFeatureCategory.LIVE_OPS -> "EV"
    }
}

private fun ProFeatureCategory.tint(): Color {
    return when (this) {
        ProFeatureCategory.SOCIAL -> Color(0xFF1565C0)
        ProFeatureCategory.COMPETITIVE -> Color(0xFF6A1B9A)
        ProFeatureCategory.PROGRESSION -> Color(0xFF2E7D32)
        ProFeatureCategory.BOT_AI -> Color(0xFF5D4037)
        ProFeatureCategory.MATCH_MODES -> Color(0xFF00838F)
        ProFeatureCategory.BOARD_RULES -> Color(0xFFC62828)
        ProFeatureCategory.CUSTOMIZATION -> Color(0xFF8A5A00)
        ProFeatureCategory.LIVE_OPS -> Color(0xFF455A64)
    }
}

private fun ProFeatureStatus.marker(): String {
    return when (this) {
        ProFeatureStatus.IN_APP_FOUNDATION -> "NOW"
        ProFeatureStatus.OFFLINE_READY -> "OFF"
        ProFeatureStatus.BACKEND_REQUIRED -> "NET"
    }
}

private fun ProFeatureStatus.statusColor(): Color {
    return when (this) {
        ProFeatureStatus.IN_APP_FOUNDATION -> Color(0xFF2E7D32)
        ProFeatureStatus.OFFLINE_READY -> Color(0xFF1565C0)
        ProFeatureStatus.BACKEND_REQUIRED -> Color(0xFF8A5A00)
    }
}

private fun ProFeature.statusColor(): Color = status.statusColor()

@Composable
private fun ProFeatureRow(
    index: Int,
    feature: ProFeature,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .testTag("pro_feature_${feature.id}")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                FeatureGlyph(feature.category)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "$index. ${feature.title}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        CategoryBadge(feature.category)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusBadge(feature.status)
                    }
                }
                DetailsHint(expanded = expanded)
            }
            if (expanded) {
                Text(
                    text = feature.playerValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5F5B55)
                )
                Text(
                    text = feature.actionHint(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4B5E20)
                )
            } else {
                Text(
                    text = feature.playerValue,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF5F5B55),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun ProFeature.actionHint(): String {
    if (status == ProFeatureStatus.BACKEND_REQUIRED) {
        return "Saved for later. Online play is not shown in normal setup yet."
    }
    return when (category) {
        ProFeatureCategory.PROGRESSION -> "Try it from Progress, Daily, or Campaign."
        ProFeatureCategory.MATCH_MODES,
        ProFeatureCategory.BOARD_RULES,
        ProFeatureCategory.BOT_AI -> "Try it from New Game setup."
        ProFeatureCategory.CUSTOMIZATION -> "Try it from Store or match settings."
        ProFeatureCategory.COMPETITIVE,
        ProFeatureCategory.SOCIAL,
        ProFeatureCategory.LIVE_OPS -> "Try it after matches or in Daily."
    }
}
