package com.example.snakeladder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ProgressionDialog(
    profile: PlayerProfile,
    dailyChallenge: DailyChallenge,
    onExportProfile: () -> String = { "" },
    onShareProfile: (String) -> Boolean = { false },
    onImportProfile: (String) -> Boolean = { false },
    onResetProfile: () -> Unit = {},
    onEquipProfileItem: (String) -> String = { "" },
    onDismiss: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(ProfileTab.OVERVIEW) }
    val dailyProgress = profile.progressFor(dailyChallenge)
    val unlockedAchievements = AchievementCatalog.all.filter { achievement ->
        achievement.id in profile.unlockedAchievementIds
    }

    GameSheetDialog(
        testTag = "progression_dialog",
        title = "Player Progress",
        subtitle = "Local profile, achievements, and today's challenge.",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileTabs(selectedTab = selectedTab, onSelectTab = { selectedTab = it })
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (selectedTab) {
                    ProfileTab.OVERVIEW -> {
                        ProfileStatGrid(profile = profile)
                        DailyChallengeCard(
                            challenge = dailyChallenge,
                            progress = dailyProgress,
                            completed = profile.dailyCompletedFor(dailyChallenge),
                            profile = profile
                        )
                        DailyCalendarPreview(profile = profile)
                    }
                    ProfileTab.ACHIEVEMENTS -> AchievementList(unlockedAchievements = unlockedAchievements)
                    ProfileTab.HISTORY -> MatchHistoryList(profile.recentMatches)
                    ProfileTab.BOTS -> BotRecordPanel(profile)
                    ProfileTab.REWARDS -> RewardInventoryPanel(
                        profile = profile,
                        onExportProfile = onExportProfile,
                        onShareProfile = onShareProfile,
                        onImportProfile = onImportProfile,
                        onResetProfile = onResetProfile,
                        onEquipProfileItem = onEquipProfileItem
                    )
                }
            }
        }
    }
}

private enum class ProfileTab(val label: String, val marker: String) {
    OVERVIEW("Stats", "OV"),
    ACHIEVEMENTS("Badges", "AW"),
    HISTORY("Games", "HI"),
    BOTS("Bots", "AI"),
    REWARDS("Rewards", "RW")
}

@Composable
private fun ProfileTabs(
    selectedTab: ProfileTab,
    onSelectTab: (ProfileTab) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_tabs"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val tabsPerRow = 2
        ProfileTab.entries.chunked(tabsPerRow).forEach { rowTabs ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowTabs.forEach { tab ->
                    FilterChip(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_tab_${tab.name.lowercase()}"),
                        selected = selectedTab == tab,
                        onClick = { onSelectTab(tab) },
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MiniBadge(
                                    text = tab.marker,
                                    tint = if (selectedTab == tab) Color(0xFF0D47A1) else Color(0xFF758196)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    lineHeight = 12.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    )
                }
                repeat(tabsPerRow - rowTabs.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProfileStatGrid(profile: PlayerProfile) {
    val xpIntoLevel = profile.xp.mod(250)
    val xpFraction = xpIntoLevel / 250f
    val stats = listOf(
        "Level" to profile.level.toString(),
        "Coins" to profile.coins.toString(),
        "Gems" to profile.gems.toString(),
        "Started" to profile.matchesStarted.toString(),
        "Completed" to profile.matchesCompleted.toString(),
        "Wins" to profile.humanWins.toString(),
        "Streak" to profile.bestWinStreak.toString(),
        "Sixes" to profile.totalSixes.toString(),
        "Ladders" to profile.totalLadders.toString(),
        "Snakes" to profile.totalSnakes.toString()
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_stats_panel"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Profile Stats", fontWeight = FontWeight.SemiBold)
        ProgressMeter(
            label = "Level ${profile.level} progress",
            valueText = "$xpIntoLevel/250 XP to level ${profile.level + 1}",
            fraction = xpFraction,
            tint = Color(0xFF1565C0),
            modifier = Modifier.testTag("profile_xp_progress")
        )
        val statsPerRow = 3
        stats.chunked(statsPerRow).forEach { rowStats ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowStats.forEach { (label, value) ->
                    StatTile(
                        label = label,
                        value = value,
                        modifier = Modifier.weight(1f),
                        compact = true
                    )
                }
                repeat(statsPerRow - rowStats.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MatchHistoryList(recentMatches: List<ProfileMatchSummary>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("match_history_panel"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Recent Matches", fontWeight = FontWeight.SemiBold)
        if (recentMatches.isEmpty()) {
            EmptyStatePanel(
                marker = "GO",
                title = "No completed matches yet",
                body = "Finish any match to build history, rewards, and replay context.",
                modifier = Modifier.testTag("match_history_empty_state")
            )
        } else {
            recentMatches.forEach { match ->
                val board = BoardLayouts.byId(match.boardLayoutId)
                Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(match.winnerName, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${match.matchMode.label} | ${board.label} | ${match.turns} turns | ${match.powerUps} powers",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF5F5B55)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ResourceBadge("LAD", match.ladders.toString(), Color(0xFF5D4037))
                            ResourceBadge("SNK", match.snakes.toString(), Color(0xFFC62828))
                            ResourceBadge("COIN", "+${match.coinsEarned}", Color(0xFF8A5A00))
                            ResourceBadge("XP", "+${match.xpEarned}", Color(0xFF1565C0))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniBadge(
    text: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(tint.copy(alpha = 0.14f))
            .border(1.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = tint,
            maxLines = 1
        )
    }
}

@Composable
private fun ResourceBadge(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tint.copy(alpha = 0.10f))
            .border(1.dp, tint.copy(alpha = 0.32f), RoundedCornerShape(999.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = tint)
        Text(value, style = MaterialTheme.typography.labelSmall, color = Color(0xFF4E342E))
    }
}

@Composable
private fun BotRecordPanel(profile: PlayerProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bot_record_panel"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Bot Records", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Vs Bot Wins", profile.vsBotWins.toString(), Modifier.weight(1f))
            StatTile("Current Streak", profile.currentWinStreak.toString(), Modifier.weight(1f))
        }
        BotPersonality.entries.forEach { personality ->
            Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    MiniBadge(
                        text = personality.displayName.take(2).uppercase(),
                        tint = Color(0xFF1565C0),
                        modifier = Modifier.size(34.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                        Text(personality.displayName, fontWeight = FontWeight.Bold)
                        Text(personality.description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF5F5B55))
                        Text(
                            text = "Auto-roll delay: ${personality.autoRollDelayMs}ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6D6259)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFF8E7))
            .border(1.dp, Color(0xFFE2C99F), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = if (compact) 8.dp else 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = value,
                style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF4E342E)
            )
            Text(
                text = label,
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                color = Color(0xFF66564E),
                maxLines = if (compact) 1 else 2
            )
        }
    }
}

@Composable
private fun DailyChallengeCard(
    challenge: DailyChallenge,
    progress: Int,
    completed: Boolean,
    profile: PlayerProfile
) {
    val board = BoardLayouts.byId(challenge.boardLayoutId)
    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_challenge_card")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Daily Challenge", fontWeight = FontWeight.SemiBold)
            Text(challenge.title, fontWeight = FontWeight.Bold, color = Color(0xFF4E342E))
            Text(
                text = challenge.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5F5B55)
            )
            Text(
                text = if (completed) "Completed: $progress/${challenge.target}" else "Progress: $progress/${challenge.target}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (completed) Color(0xFF2E7D32) else Color(0xFF1565C0)
            )
            ProgressMeter(
                label = "Daily challenge progress",
                valueText = "$progress/${challenge.target}",
                fraction = (progress / challenge.target.toFloat()).coerceIn(0f, 1f),
                tint = if (completed) Color(0xFF2E7D32) else Color(0xFF1565C0),
                modifier = Modifier.testTag("daily_challenge_progress_bar")
            )
            Text(
                text = "${board.label} | ${challenge.matchMode.label}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5F5B55)
            )
            RewardBadgeRow(reward = challenge.reward)
            Text(
                text = "Daily streak ${profile.dailyStreak} | Best ${profile.bestDailyStreak}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6D6259)
            )
        }
    }
}

@Composable
private fun DailyCalendarPreview(profile: PlayerProfile) {
    val week = DailyChallengeCatalog.weeklyCalendar()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_calendar_preview"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Challenge Calendar", fontWeight = FontWeight.SemiBold)
        week.take(4).forEach { challenge ->
            val done = challenge.id in profile.completedDailyChallengeIds
            Text(
                text = "${challenge.dateKey}: ${challenge.title} (${if (done) "done" else BoardLayouts.byId(challenge.boardLayoutId).label})",
                style = MaterialTheme.typography.bodySmall,
                color = if (done) Color(0xFF2E7D32) else Color(0xFF5F5B55)
            )
        }
    }
}

@Composable
private fun RewardInventoryPanel(
    profile: PlayerProfile,
    onExportProfile: () -> String,
    onShareProfile: (String) -> Boolean,
    onImportProfile: (String) -> Boolean,
    onResetProfile: () -> Unit,
    onEquipProfileItem: (String) -> String
) {
    var equipStatus by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reward_inventory_panel"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Profile Detail", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("XP", profile.xp.toString(), Modifier.weight(1f))
            StatTile("Gems", profile.gems.toString(), Modifier.weight(1f))
        }
        ProgressMeter(
            label = "Next title progress",
            valueText = "Level ${profile.level} to ${profile.level + 1}",
            fraction = profile.xp.mod(250) / 250f,
            tint = Color(0xFF6A1B9A),
            modifier = Modifier.testTag("reward_xp_progress")
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ResourceBadge("COIN", profile.coins.toString(), Color(0xFF8A5A00))
            ResourceBadge("GEM", profile.gems.toString(), Color(0xFF6A1B9A))
            ResourceBadge("XP", profile.xp.toString(), Color(0xFF1565C0))
        }
        Text("Title: ${profile.title}", fontWeight = FontWeight.Bold, color = Color(0xFF4E342E))
        ProfileCustomizationPanel(
            profile = profile,
            equipStatus = equipStatus,
            onEquipItem = { itemId ->
                equipStatus = onEquipProfileItem(itemId)
            }
        )
        Text(
            text = "Boards: ${profile.unlockedBoardIds.map { BoardLayouts.byId(it).label }.sorted().joinToString()}",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF5F5B55)
        )
        Text(
            text = "Campaign nodes cleared: ${profile.completedCampaignNodeIds.size}/${CampaignCatalog.nodes.size}",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF5F5B55)
        )
        ProfileToolsPanel(
            profile = profile,
            onExportProfile = onExportProfile,
            onShareProfile = onShareProfile,
            onImportProfile = onImportProfile,
            onResetProfile = onResetProfile
        )
    }
}

@Composable
private fun ProfileCustomizationPanel(
    profile: PlayerProfile,
    equipStatus: String,
    onEquipItem: (String) -> Unit
) {
    val avatarItems = StoreCatalog.items.filter { it.type == StoreItemType.AVATAR }
    val titleItems = StoreCatalog.items.filter { it.type == StoreItemType.TITLE }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFFBF1))
            .border(1.dp, Color(0xFFE2C99F), RoundedCornerShape(8.dp))
            .padding(10.dp)
            .testTag("profile_customization_panel"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Customize Profile", fontWeight = FontWeight.SemiBold)
        Text(
            text = "Preview locked avatars here. Equip owned avatars and titles without leaving Progress.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF5F5B55)
        )
        Text("Avatars", fontWeight = FontWeight.Bold, color = Color(0xFF4E342E))
        avatarItems.forEach { item ->
            ProfileStoreItemRow(
                profile = profile,
                item = item,
                onEquipItem = onEquipItem
            )
        }
        Text("Titles", fontWeight = FontWeight.Bold, color = Color(0xFF4E342E))
        titleItems.forEach { item ->
            ProfileStoreItemRow(
                profile = profile,
                item = item,
                onEquipItem = onEquipItem
            )
        }
        if (equipStatus.isNotBlank()) {
            Text(
                text = equipStatus,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1565C0),
                modifier = Modifier.testTag("profile_equip_status")
            )
        }
    }
}

@Composable
private fun ProfileStoreItemRow(
    profile: PlayerProfile,
    item: StoreItem,
    onEquipItem: (String) -> Unit
) {
    val owned = StoreCatalog.isOwned(profile, item)
    val equipped = when (item.type) {
        StoreItemType.AVATAR -> profile.selectedAvatarId == item.targetId
        StoreItemType.TITLE -> profile.selectedTitle == (item.selectedTitle ?: "")
        StoreItemType.BOARD -> false
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (equipped) Color(0xFFE8F5E9) else Color.White)
            .border(
                width = if (equipped) 2.dp else 1.dp,
                color = if (equipped) Color(0xFF2E7D32) else Color(0xFFE7DCCB),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
            .testTag("profile_preview_${item.id}"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (item.type) {
            StoreItemType.AVATAR -> AvatarTokenPreview(
                avatarId = item.targetId,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("profile_avatar_token_${item.targetId}")
            )
            StoreItemType.TITLE -> MiniBadge(
                text = item.name.initials(),
                tint = Color(0xFF8A5A00),
                modifier = Modifier
                    .size(42.dp)
                    .testTag("profile_title_badge_${item.targetId}")
            )
            StoreItemType.BOARD -> MiniBadge(item.name.initials(), Color(0xFF1565C0), Modifier.size(42.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(item.name, fontWeight = FontWeight.Bold, color = Color(0xFF4E342E))
            Text(
                text = when {
                    equipped -> "Equipped"
                    owned -> "Owned"
                    else -> "Locked preview - unlock in Store for ${item.priceLabelForProfile()}."
                },
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    equipped -> Color(0xFF2E7D32)
                    owned -> Color(0xFF1565C0)
                    else -> Color(0xFF6D6259)
                }
            )
        }
        Button(
            enabled = owned && !equipped,
            modifier = Modifier.testTag("profile_equip_${item.id}"),
            onClick = { onEquipItem(item.id) }
        ) {
            Text(if (equipped) "Equipped" else "Equip")
        }
    }
}

@Composable
private fun ProfileToolsPanel(
    profile: PlayerProfile,
    onExportProfile: () -> String,
    onShareProfile: (String) -> Boolean,
    onImportProfile: (String) -> Boolean,
    onResetProfile: () -> Unit
) {
    var exportText by rememberSaveable { mutableStateOf("") }
    var importText by rememberSaveable { mutableStateOf("") }
    var statusText by rememberSaveable { mutableStateOf("") }
    var showResetConfirm by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF7FBFF))
            .border(1.dp, Color(0xFFBFD4EA), RoundedCornerShape(8.dp))
            .padding(10.dp)
            .testTag("profile_tools_panel"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Profile Tools", fontWeight = FontWeight.SemiBold)
        Text(
            text = "Export creates a local backup code. Import replaces this profile with a backup code from this device.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF35506B),
            modifier = Modifier.testTag("profile_tools_plain_language")
        )
        Text(
            text = PlayerProfileStore.maintenanceSummary(profile),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF5F5B55),
            modifier = Modifier.testTag("profile_maintenance_summary")
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier
                    .weight(1f)
                    .testTag("profile_export_button"),
                onClick = {
                    exportText = onExportProfile()
                    statusText = "Profile exported."
                }
            ) {
                Text("Export")
            }
            Button(
                modifier = Modifier
                    .weight(1f)
                    .testTag("profile_import_button"),
                enabled = importText.trim().isNotEmpty(),
                onClick = {
                    val imported = onImportProfile(importText.trim())
                    statusText = if (imported) "Profile imported." else "Import failed."
                    if (imported) importText = ""
                }
            ) {
                Text("Import")
            }
            TextButton(
                modifier = Modifier
                    .weight(1f)
                    .testTag("profile_reset_button"),
                onClick = { showResetConfirm = true }
            ) {
                Text("Reset")
            }
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_share_export_button"),
            onClick = {
                val backup = exportText.ifBlank {
                    onExportProfile().also { exportText = it }
                }
                statusText = if (backup.isBlank()) {
                    "Export failed."
                } else if (onShareProfile(backup)) {
                    "Profile export shared."
                } else {
                    "Share unavailable."
                }
            }
        ) {
            Text("Share Export")
        }
        OutlinedTextField(
            value = exportText,
            onValueChange = { exportText = it },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 84.dp)
                .testTag("profile_export_text"),
            label = { Text("Export JSON") },
            maxLines = 4
        )
        OutlinedTextField(
            value = importText,
            onValueChange = { importText = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 84.dp)
                .testTag("profile_import_text"),
            label = { Text("Import JSON") },
            maxLines = 4
        )
        if (statusText.isNotBlank()) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4E342E),
                modifier = Modifier.testTag("profile_tools_status")
            )
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            modifier = Modifier.testTag("profile_reset_confirmation_dialog"),
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset profile?") },
            text = {
                Text("This clears local rewards, achievements, history, and unlocks. Export first if you want a backup.")
            },
            confirmButton = {
                Button(
                    modifier = Modifier.testTag("profile_reset_confirm_button"),
                    onClick = {
                        onResetProfile()
                        exportText = ""
                        importText = ""
                        statusText = "Profile reset."
                        showResetConfirm = false
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AchievementList(unlockedAchievements: List<Achievement>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("achievement_list"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Achievements ${unlockedAchievements.size}/${AchievementCatalog.all.size}",
            fontWeight = FontWeight.SemiBold
        )
        if (unlockedAchievements.isEmpty()) {
            EmptyStatePanel(
                marker = "AW",
                title = "No achievements unlocked yet",
                body = "Win, climb, avoid snakes, and clear campaign nodes to earn badges.",
                modifier = Modifier.testTag("achievement_empty_state")
            )
        } else {
            unlockedAchievements.forEach { achievement ->
                Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        MiniBadge(
                            text = achievement.title.initials(),
                            tint = Color(0xFF8A5A00),
                            modifier = Modifier.size(34.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                            Text(achievement.title, fontWeight = FontWeight.Bold)
                            Text(
                                text = achievement.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF5F5B55)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressMeter(
    label: String,
    valueText: String,
    fraction: Float,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF4E342E))
            Text(valueText, style = MaterialTheme.typography.labelSmall, color = Color(0xFF5F5B55))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFE2EAF4))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(8.dp)
                    .background(tint)
            )
        }
    }
}

@Composable
private fun RewardBadgeRow(reward: RewardBundle) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (reward.coins > 0) ResourceBadge("COIN", reward.coins.toString(), Color(0xFF8A5A00))
        if (reward.gems > 0) ResourceBadge("GEM", reward.gems.toString(), Color(0xFF6A1B9A))
        if (reward.xp > 0) ResourceBadge("XP", reward.xp.toString(), Color(0xFF1565C0))
        if (reward.isEmpty()) ResourceBadge("REWARD", "None", Color(0xFF5F5B55))
    }
}

@Composable
private fun EmptyStatePanel(
    marker: String,
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF7FBFF))
            .border(1.dp, Color(0xFFBFD4EA), RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiniBadge(text = marker, tint = Color(0xFF1565C0), modifier = Modifier.size(40.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF4E342E))
            Text(body, style = MaterialTheme.typography.bodySmall, color = Color(0xFF5F5B55))
        }
    }
}

private fun String.initials(): String {
    return split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { take(2).uppercase() }
}

private fun StoreItem.priceLabelForProfile(): String {
    return buildList {
        if (coinCost > 0) add("$coinCost coins")
        if (gemCost > 0) add("$gemCost gems")
    }.joinToString(" + ").ifBlank { "free" }
}
