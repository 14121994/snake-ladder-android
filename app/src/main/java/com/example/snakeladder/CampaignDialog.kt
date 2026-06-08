package com.example.snakeladder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun CampaignDialog(
    profile: PlayerProfile,
    onStartNode: (CampaignNode) -> Unit,
    onDismiss: () -> Unit
) {
    val unlockedIds = CampaignCatalog.unlockedNodes(profile).map { it.id }.toSet()
    val completedIds = profile.completedCampaignNodeIds
    var selectedFilter by rememberSaveable { mutableStateOf(CampaignNodeFilter.ALL) }
    val availableCount = CampaignCatalog.nodes.count { it.id in unlockedIds && it.id !in completedIds }
    val bossCount = CampaignCatalog.nodes.count { it.isBoss }
    val filteredNodes = CampaignCatalog.nodes.filter { node ->
        selectedFilter.includes(node, unlockedIds, completedIds)
    }

    GameSheetDialog(
        testTag = "campaign_dialog",
        title = "Quest Map",
        subtitle = "Local campaign nodes built on board layouts and match modes.",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CampaignMapHeader(
                completedCount = completedIds.size,
                totalCount = CampaignCatalog.nodes.size
            )
            CampaignFilterRow(
                selectedFilter = selectedFilter,
                availableCount = availableCount,
                bossCount = bossCount,
                visibleCount = filteredNodes.size,
                onSelectFilter = { selectedFilter = it }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredNodes.isEmpty()) {
                    CampaignEmptyFilterState(selectedFilter = selectedFilter)
                }
                filteredNodes.groupBy { it.chapter }.forEach { (chapter, nodes) ->
                    val chapterCompleted = nodes.count { it.id in completedIds }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF7FBFF))
                            .border(1.dp, Color(0xFFD4E0EE), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Chapter $chapter",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF24435F)
                            )
                            CampaignStatusChip(
                                text = "$chapterCompleted/${nodes.size} cleared",
                                background = Color(0xFFE8F2FF),
                                foreground = Color(0xFF1557A8)
                            )
                        }
                        nodes.forEachIndexed { index, node ->
                            val unlocked = node.id in unlockedIds
                            CampaignNodePathRow(
                                node = node,
                                unlocked = unlocked,
                                completed = node.id in completedIds,
                                showConnector = index < nodes.lastIndex,
                                onStartNode = onStartNode
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class CampaignNodeFilter(
    val label: String,
    val countLabel: String,
    val testTag: String
) {
    ALL("All", "nodes", "campaign_filter_all"),
    AVAILABLE("Available", "available", "campaign_filter_available"),
    BOSSES("Bosses", "bosses", "campaign_filter_bosses");

    fun includes(
        node: CampaignNode,
        unlockedIds: Set<String>,
        completedIds: Set<String>
    ): Boolean {
        return when (this) {
            ALL -> true
            AVAILABLE -> node.id in unlockedIds && node.id !in completedIds
            BOSSES -> node.isBoss
        }
    }
}

@Composable
private fun CampaignMapHeader(
    completedCount: Int,
    totalCount: Int
) {
    val progress = if (totalCount == 0) 0f else completedCount / totalCount.toFloat()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFF8E7))
            .border(1.dp, Color(0xFFE2C99F), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Campaign progress $completedCount/$totalCount",
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF4E342E)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFE9D8B9))
                .testTag("campaign_map_progress")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(8.dp)
                    .background(Color(0xFF2E7D32))
            )
        }
    }
}

@Composable
private fun CampaignFilterRow(
    selectedFilter: CampaignNodeFilter,
    availableCount: Int,
    bossCount: Int,
    visibleCount: Int,
    onSelectFilter: (CampaignNodeFilter) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF7FBFF))
            .border(1.dp, Color(0xFFD4E0EE), RoundedCornerShape(10.dp))
            .padding(8.dp)
            .testTag("campaign_filter_panel"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CampaignNodeFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onSelectFilter(filter) },
                    label = {
                        Text(
                            text = filter.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag(filter.testTag)
                )
            }
        }
        Text(
            text = when (selectedFilter) {
                CampaignNodeFilter.ALL -> "$visibleCount nodes | $availableCount available"
                CampaignNodeFilter.AVAILABLE -> "$visibleCount available"
                CampaignNodeFilter.BOSSES -> "$visibleCount bosses | $availableCount available"
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF24435F),
            modifier = Modifier.testTag("campaign_filter_count")
        )
        if (bossCount > 0 && selectedFilter != CampaignNodeFilter.BOSSES) {
            Text(
                text = "$bossCount boss nodes stay visible from the Bosses filter.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6D6259)
            )
        }
    }
}

@Composable
private fun CampaignEmptyFilterState(
    selectedFilter: CampaignNodeFilter
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF8E7))
            .border(1.dp, Color(0xFFE2C99F), RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("campaign_filter_empty"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (selectedFilter) {
                CampaignNodeFilter.AVAILABLE -> "No available campaign nodes. Replay completed nodes from All."
                CampaignNodeFilter.BOSSES -> "No boss nodes found."
                CampaignNodeFilter.ALL -> "No campaign nodes found."
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF4E342E),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CampaignNodePathRow(
    node: CampaignNode,
    unlocked: Boolean,
    completed: Boolean,
    showConnector: Boolean,
    onStartNode: (CampaignNode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .width(34.dp)
                .padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        when {
                            node.isBoss && unlocked && !completed -> Color(0xFFB3261E)
                            completed -> Color(0xFF2E7D32)
                            unlocked -> Color(0xFF1565C0)
                            else -> Color(0xFFB8A896)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    completed -> Text(
                        text = "OK",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    node.isBoss && unlocked -> Text(
                        text = "!",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    unlocked -> Text(
                        text = node.chapter.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    else -> LockNodeGlyph()
                }
            }
            if (showConnector) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (completed) Color(0xFF2E7D32) else Color(0xFFD4C2AB))
                )
            }
        }
        CampaignNodeCard(
            node = node,
            unlocked = unlocked,
            completed = completed,
            onStartNode = onStartNode
        )
    }
}

@Composable
private fun CampaignNodeCard(
    node: CampaignNode,
    unlocked: Boolean,
    completed: Boolean,
    onStartNode: (CampaignNode) -> Unit
) {
    val board = BoardLayouts.byId(node.boardLayoutId)
    val borderColor = when {
        node.isBoss -> Color(0xFFB3261E)
        completed -> Color(0xFF2E7D32)
        unlocked -> Color(0xFFB7CDE8)
        else -> Color(0xFFD2C6B8)
    }
    Card(
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(if (node.isBoss) 1.6.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = when {
                node.isBoss -> Color(0xFFFFF7ED)
                completed -> Color(0xFFF2FFF5)
                unlocked -> Color(0xFFFDFEFF)
                else -> Color(0xFFF5F0EA)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("campaign_node_${node.id}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            MiniBoardPreview(
                boardLayoutId = node.boardLayoutId,
                modifier = Modifier
                    .width(82.dp)
                    .height(82.dp)
                    .clip(RoundedCornerShape(12.dp)),
                boardThemeOption = suggestedPreviewTheme(node.boardLayoutId)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (node.isBoss) {
                        CampaignStatusChip(
                            text = "BOSS",
                            background = Color(0xFFB3261E),
                            foreground = Color.White
                        )
                    }
                    Text(
                        text = "Chapter ${node.chapter} | ${node.title}",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4E342E)
                    )
                }
                Text(node.description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF5F5B55))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        CampaignNodeMetaChip(node.matchMode.label)
                        CampaignNodeMetaChip("${node.difficulty.iconLabel()} ${node.difficulty.shortLabel()}")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        CampaignNodeMetaChip(board.label)
                        if (node.gameMode == GameMode.VS_BOT) {
                            CampaignNodeMetaChip(node.botPersonality.styleName)
                        }
                    }
                }
                Text(
                    text = "${node.difficulty.knockbackSymbolLabel()} | ${node.matchMode.description}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF455A64)
                )
                RewardBundleBadges(
                    reward = node.reward,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusText = when {
                        completed -> "Completed"
                        unlocked -> "Unlocked"
                        else -> "Needs ${node.requiredWins} wins"
                    }
                    CampaignStatusChip(
                        text = statusText,
                        background = when {
                            completed -> Color(0xFFE8F5E9)
                            unlocked -> Color(0xFFE8F2FF)
                            else -> Color(0xFFFFF4D6)
                        },
                        foreground = when {
                            completed -> Color(0xFF2E7D32)
                            unlocked -> Color(0xFF1557A8)
                            else -> Color(0xFF8A5A00)
                        }
                    )
                    if (unlocked) {
                        Button(
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (node.isBoss) Color(0xFFB3261E) else Color(0xFF1565C0),
                                contentColor = Color.White
                            ),
                            onClick = { onStartNode(node) }
                        ) {
                            Text(if (completed) "Replay" else "Play")
                        }
                    } else {
                        CampaignStatusChip(
                            text = "Locked",
                            background = Color(0xFFE8E2DB),
                            foreground = Color(0xFF6B5D50)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LockNodeGlyph() {
    Canvas(modifier = Modifier.size(12.dp)) {
        val bodyWidth = size.width * 0.56f
        val bodyHeight = size.height * 0.44f
        val bodyLeft = (size.width - bodyWidth) / 2f
        val bodyTop = size.height * 0.44f
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(2f, 2f)
        )
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.34f, size.height * 0.44f),
            end = Offset(size.width * 0.34f, size.height * 0.28f)
        )
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.66f, size.height * 0.44f),
            end = Offset(size.width * 0.66f, size.height * 0.28f)
        )
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.34f, size.height * 0.28f),
            end = Offset(size.width * 0.50f, size.height * 0.16f)
        )
        drawLine(
            color = Color.White,
            start = Offset(size.width * 0.66f, size.height * 0.28f),
            end = Offset(size.width * 0.50f, size.height * 0.16f)
        )
    }
}

@Composable
private fun RewardBundleBadges(
    reward: RewardBundle,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (reward.coins > 0) RewardBadge(icon = "C", label = "Coins", value = reward.coins.toString(), tint = Color(0xFF8A5A00))
        if (reward.gems > 0) RewardBadge(icon = "G", label = "Gems", value = reward.gems.toString(), tint = Color(0xFF6A1B9A))
        if (reward.xp > 0) RewardBadge(icon = "XP", label = "XP", value = reward.xp.toString(), tint = Color(0xFF1565C0))
    }
}

@Composable
private fun RewardBadge(
    icon: String,
    label: String,
    value: String,
    tint: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(tint.copy(alpha = 0.16f), tint.copy(alpha = 0.08f))
                )
            )
            .border(1.dp, tint.copy(alpha = 0.30f), RoundedCornerShape(999.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(tint),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = "$label $value",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = tint
        )
    }
}

@Composable
private fun CampaignNodeMetaChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFEFF5FF))
            .border(1.dp, Color(0xFFD4E0EE), RoundedCornerShape(999.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF3E5266),
            maxLines = 1
        )
    }
}

@Composable
private fun CampaignStatusChip(
    text: String,
    background: Color,
    foreground: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(1.dp, foreground.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = foreground,
            maxLines = 1
        )
    }
}
