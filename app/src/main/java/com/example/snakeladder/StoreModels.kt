package com.example.snakeladder

internal enum class StoreItemType {
    AVATAR,
    BOARD,
    TITLE
}

internal data class StoreItem(
    val id: String,
    val type: StoreItemType,
    val name: String,
    val description: String,
    val coinCost: Int,
    val gemCost: Int = 0,
    val targetId: String,
    val selectedTitle: String? = null
)

internal data class StorePurchaseResult(
    val profile: PlayerProfile,
    val purchased: Boolean,
    val message: String
)

internal object StoreCatalog {
    val items: List<StoreItem> = listOf(
        StoreItem(
            id = "avatar_cobra_token",
            type = StoreItemType.AVATAR,
            name = "Cobra Token",
            description = "A sharp token for players who survive snake-heavy boards.",
            coinCost = 120,
            targetId = "cobra_token"
        ),
        StoreItem(
            id = "avatar_ladder_king",
            type = StoreItemType.AVATAR,
            name = "Ladder King",
            description = "A climb-focused avatar for aggressive ladder hunters.",
            coinCost = 180,
            targetId = "ladder_king"
        ),
        StoreItem(
            id = "avatar_gold_die",
            type = StoreItemType.AVATAR,
            name = "Gold Die",
            description = "Premium avatar for high-streak players.",
            coinCost = 240,
            gemCost = 2,
            targetId = "gold_die"
        ),
        StoreItem(
            id = "board_speed_run",
            type = StoreItemType.BOARD,
            name = "Speed Run Board",
            description = "Unlock the quick-session board for short, intense races.",
            coinCost = 150,
            targetId = BoardLayouts.SPEED_RUN_ID
        ),
        StoreItem(
            id = "board_trap_valley",
            type = StoreItemType.BOARD,
            name = "Trap Valley Board",
            description = "Unlock a mid-board pressure layout for party rules.",
            coinCost = 220,
            targetId = BoardLayouts.TRAP_VALLEY_ID
        ),
        StoreItem(
            id = "board_pro_chaos",
            type = StoreItemType.BOARD,
            name = "Pro Chaos Board",
            description = "Unlock the volatile board tuned for expert play.",
            coinCost = 300,
            gemCost = 3,
            targetId = BoardLayouts.PRO_CHAOS_ID
        ),
        StoreItem(
            id = "title_snake_tamer",
            type = StoreItemType.TITLE,
            name = "Snake Tamer",
            description = "Equip a title that signals snake-board control.",
            coinCost = 130,
            targetId = "title_snake_tamer",
            selectedTitle = "Snake Tamer"
        ),
        StoreItem(
            id = "title_board_strategist",
            type = StoreItemType.TITLE,
            name = "Board Strategist",
            description = "Equip a title for tactical match-mode specialists.",
            coinCost = 260,
            gemCost = 1,
            targetId = "title_board_strategist",
            selectedTitle = "Board Strategist"
        )
    )

    fun byId(id: String): StoreItem? = items.firstOrNull { it.id == id }

    fun isOwned(profile: PlayerProfile, item: StoreItem): Boolean {
        return when (item.type) {
            StoreItemType.AVATAR -> item.targetId in profile.unlockedAvatarIds
            StoreItemType.BOARD -> item.targetId in profile.unlockedBoardIds
            StoreItemType.TITLE -> item.targetId in profile.unlockedTitleIds
        }
    }

    fun canAfford(profile: PlayerProfile, item: StoreItem): Boolean {
        return profile.coins >= item.coinCost && profile.gems >= item.gemCost
    }

    fun purchase(profile: PlayerProfile, itemId: String): StorePurchaseResult {
        val item = byId(itemId) ?: return StorePurchaseResult(
            profile = profile,
            purchased = false,
            message = "Store item unavailable."
        )
        if (isOwned(profile, item)) {
            return equip(profile, itemId).copy(message = "${item.name} is already unlocked.")
        }
        if (!canAfford(profile, item)) {
            return StorePurchaseResult(
                profile = profile,
                purchased = false,
                message = "Not enough coins or gems for ${item.name}."
            )
        }

        val debited = profile.copy(
            coins = profile.coins - item.coinCost,
            gems = profile.gems - item.gemCost
        )
        val updated = when (item.type) {
            StoreItemType.AVATAR -> debited.copy(
                selectedAvatarId = item.targetId,
                unlockedAvatarIds = debited.unlockedAvatarIds + item.targetId
            )
            StoreItemType.BOARD -> debited.copy(
                unlockedBoardIds = debited.unlockedBoardIds + item.targetId
            )
            StoreItemType.TITLE -> debited.copy(
                selectedTitle = item.selectedTitle ?: debited.selectedTitle,
                unlockedTitleIds = debited.unlockedTitleIds + item.targetId
            )
        }

        return StorePurchaseResult(
            profile = updated.copy(schemaVersion = PLAYER_PROFILE_SCHEMA_VERSION),
            purchased = true,
            message = "Unlocked ${item.name}."
        )
    }

    fun equip(profile: PlayerProfile, itemId: String): StorePurchaseResult {
        val item = byId(itemId) ?: return StorePurchaseResult(
            profile = profile,
            purchased = false,
            message = "Store item unavailable."
        )
        if (!isOwned(profile, item)) {
            return StorePurchaseResult(
                profile = profile,
                purchased = false,
                message = "${item.name} is locked. Unlock it from Store first."
            )
        }
        if (item.type == StoreItemType.BOARD) {
            return StorePurchaseResult(
                profile = profile,
                purchased = false,
                message = "${item.name} is available in New Game board selection."
            )
        }
        val updated = equipOwned(profile, item).copy(schemaVersion = PLAYER_PROFILE_SCHEMA_VERSION)
        return StorePurchaseResult(
            profile = updated,
            purchased = false,
            message = "Equipped ${item.name}."
        )
    }

    private fun equipOwned(profile: PlayerProfile, item: StoreItem): PlayerProfile {
        return when (item.type) {
            StoreItemType.AVATAR -> profile.copy(selectedAvatarId = item.targetId)
            StoreItemType.TITLE -> profile.copy(selectedTitle = item.selectedTitle ?: profile.selectedTitle)
            StoreItemType.BOARD -> profile
        }
    }
}
