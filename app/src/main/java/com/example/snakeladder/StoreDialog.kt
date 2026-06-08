package com.example.snakeladder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun StoreDialog(
    profile: PlayerProfile,
    onPurchaseItem: (String) -> String,
    onDismiss: () -> Unit
) {
    var statusMessage by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf<StoreItemType?>(null) }
    var pendingPurchaseId by rememberSaveable { mutableStateOf<String?>(null) }
    val groupedItems = StoreCatalog.items.groupBy { it.type }
    val pendingPurchase = pendingPurchaseId?.let(StoreCatalog::byId)

    GameSheetDialog(
        testTag = "store_dialog",
        title = "Store & Unlocks",
        subtitle = "Spend match rewards on avatars, boards, and titles.",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            pendingPurchase?.let { item ->
                StorePurchaseConfirmationPanel(
                    item = item,
                    onCancel = { pendingPurchaseId = null },
                    onConfirm = {
                        pendingPurchaseId = null
                        statusMessage = onPurchaseItem(item.id)
                    }
                )
                return@Column
            }
            StoreBalancePanel(profile)
            StoreEarnCoinsPanel()
            StoreTypeTabs(
                selectedType = selectedType,
                onSelectType = { selectedType = it }
            )
            if (statusMessage.isNotBlank()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4E342E),
                    modifier = Modifier.testTag("store_status_message")
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StoreItemType.entries
                    .filter { selectedType == null || it == selectedType }
                    .forEach { type ->
                    val items = groupedItems[type].orEmpty()
                    if (items.isNotEmpty()) {
                        Text(type.sectionLabel(), fontWeight = FontWeight.SemiBold)
                        items.forEach { item ->
                            StoreItemCard(
                                profile = profile,
                                item = item,
                                onClick = {
                                    if (!StoreCatalog.isOwned(profile, item)) {
                                        pendingPurchaseId = item.id
                                    } else {
                                        statusMessage = onPurchaseItem(item.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreTypeTabs(
    selectedType: StoreItemType?,
    onSelectType: (StoreItemType?) -> Unit
) {
    val rows = listOf(
        listOf<StoreItemType?>(null, StoreItemType.AVATAR),
        listOf(StoreItemType.BOARD, StoreItemType.TITLE)
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { type ->
                    val label = type?.sectionLabel() ?: "All"
                    FilterChip(
                        modifier = Modifier.weight(1f),
                        selected = selectedType == type,
                        onClick = { onSelectType(type) },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StoreEarnCoinsPanel() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF7FBFF))
            .border(1.dp, Color(0xFFBFD4EA), RoundedCornerShape(8.dp))
            .padding(10.dp)
            .testTag("store_earn_coins_panel")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Earn coins fast",
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF24435F)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StoreSourceBadge("Daily", Modifier.weight(1f))
                StoreSourceBadge("Campaign", Modifier.weight(1f))
                StoreSourceBadge("Match Wins", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StoreSourceBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFD4E0EE), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF24435F),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StoreBalancePanel(profile: PlayerProfile) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFF8E7))
            .border(1.dp, Color(0xFFE2C99F), RoundedCornerShape(8.dp))
            .padding(10.dp)
            .testTag("store_balance_panel")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = "${profile.coins} coins | ${profile.gems} gems",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4E342E)
            )
            Text(
                text = "Earn more from Daily, Campaign, and completed match wins.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF66564E)
            )
            Text(
                text = "Equipped: ${profile.title} | ${avatarDisplayName(profile.selectedAvatarId)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF66564E)
            )
        }
    }
}

@Composable
private fun StoreItemCard(
    profile: PlayerProfile,
    item: StoreItem,
    onClick: () -> Unit
) {
    val owned = StoreCatalog.isOwned(profile, item)
    val canAfford = StoreCatalog.canAfford(profile, item)
    val equipped = when (item.type) {
        StoreItemType.AVATAR -> profile.selectedAvatarId == item.targetId
        StoreItemType.TITLE -> profile.selectedTitle == (item.selectedTitle ?: "")
        StoreItemType.BOARD -> false
    }
    val canEquipOwned = owned && item.type != StoreItemType.BOARD && !equipped
    val buttonText = when {
        !owned -> "Buy"
        equipped -> "Equipped"
        canEquipOwned -> "Equip"
        else -> "Owned"
    }
    val enabled = (!owned && canAfford) || canEquipOwned
    val actionHint = when {
        !owned && !canAfford -> buildString {
            val coinGap = (item.coinCost - profile.coins).coerceAtLeast(0)
            val gemGap = (item.gemCost - profile.gems).coerceAtLeast(0)
            if (coinGap > 0) append("Need $coinGap more coins")
            if (coinGap > 0 && gemGap > 0) append(" and ")
            if (gemGap > 0) append("$gemGap more gems")
        }
        equipped -> "Currently equipped"
        owned && item.type == StoreItemType.BOARD -> "Unlocked for board selection"
        canEquipOwned -> "Ready to equip"
        !owned && item.type == StoreItemType.BOARD -> "Unlocks in New Game board selection after purchase"
        else -> "Tap to unlock"
    }
    val stateLabel = when {
        equipped -> "Equipped"
        owned -> "Owned"
        else -> "Locked"
    }
    val stateColor = when {
        equipped -> Color(0xFF155724)
        owned -> Color(0xFF1E5C8A)
        else -> Color(0xFF7A4B00)
    }
    val stateBackground = when {
        equipped -> Color(0xFFE4F6E8)
        owned -> Color(0xFFE8F2FF)
        else -> Color(0xFFFFF4D8)
    }
    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("store_item_${item.id}")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (item.type) {
                    StoreItemType.AVATAR -> AvatarTokenPreview(
                        avatarId = item.targetId,
                        modifier = Modifier.size(50.dp)
                    )
                    StoreItemType.BOARD -> MiniBoardPreview(
                        boardLayoutId = item.targetId,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    StoreItemType.TITLE -> TitleBadgePreview(
                        title = item.selectedTitle ?: item.name,
                        modifier = Modifier
                            .height(34.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(item.name, fontWeight = FontWeight.Bold, color = Color(0xFF4E342E))
                    StoreStatePill(
                        text = stateLabel,
                        background = stateBackground,
                        contentColor = stateColor,
                        modifier = Modifier.testTag("store_state_${item.id}")
                    )
                    Text(
                        text = if (owned) "Unlocked" else item.priceLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (owned) Color(0xFF2E7D32) else Color(0xFF6D3E00)
                    )
                }
            }
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5F5B55)
            )
            Text(
                text = actionHint,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) Color(0xFF2E7D32) else Color(0xFF8A5A00),
                modifier = Modifier.testTag("store_hint_${item.id}")
            )
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("store_buy_${item.id}"),
                enabled = enabled,
                onClick = onClick
            ) {
                Text(buttonText)
            }
        }
    }
}

@Composable
private fun StoreStatePill(
    text: String,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(1.dp, contentColor.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1
        )
    }
}

@Composable
internal fun StorePurchaseConfirmationPanel(
    item: StoreItem,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFF8E7))
            .border(1.dp, Color(0xFFE2C99F), RoundedCornerShape(8.dp))
            .padding(10.dp)
            .testTag("store_purchase_confirm_dialog")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Confirm Purchase", fontWeight = FontWeight.Bold, color = Color(0xFF4E342E))
            Text("Spend ${item.priceLabel()} to unlock ${item.name}?")
            Text(
                text = item.purchaseEffectLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5F5B55)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("store_purchase_cancel_button"),
                    onClick = onCancel
                ) {
                    Text("Cancel")
                }
                Button(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("store_purchase_confirm_button"),
                    onClick = onConfirm
                ) {
                    Text("Confirm")
                }
            }
        }
    }
}

private fun StoreItem.priceLabel(): String {
    val parts = buildList {
        if (coinCost > 0) add("$coinCost coins")
        if (gemCost > 0) add("$gemCost gems")
    }
    return parts.joinToString(" | ")
}

private fun StoreItem.purchaseEffectLabel(): String {
    return when (type) {
        StoreItemType.AVATAR -> "Equips this avatar immediately for new matches."
        StoreItemType.BOARD -> "Adds this board to the New Game board picker."
        StoreItemType.TITLE -> "Equips this title immediately on your profile."
    }
}

private fun StoreItemType.sectionLabel(): String {
    return when (this) {
        StoreItemType.AVATAR -> "Avatars"
        StoreItemType.BOARD -> "Boards"
        StoreItemType.TITLE -> "Titles"
    }
}

private fun avatarDisplayName(avatarId: String): String {
    return when (avatarId) {
        "cobra_token" -> "Cobra Token"
        "ladder_king" -> "Ladder King"
        "gold_die" -> "Gold Die"
        else -> "Classic Token"
    }
}
