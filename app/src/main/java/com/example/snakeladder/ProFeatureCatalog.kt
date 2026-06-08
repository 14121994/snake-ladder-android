package com.example.snakeladder

internal enum class ProFeatureCategory(val label: String) {
    SOCIAL("Social play"),
    COMPETITIVE("Competitive"),
    PROGRESSION("Progression"),
    BOT_AI("Bot AI"),
    MATCH_MODES("Match modes"),
    BOARD_RULES("Board rules"),
    CUSTOMIZATION("Customization"),
    LIVE_OPS("Events")
}

internal enum class ProFeatureStatus(val label: String) {
    IN_APP_FOUNDATION("Playable now"),
    OFFLINE_READY("Ready offline"),
    BACKEND_REQUIRED("Online idea")
}

internal data class ProFeature(
    val id: String,
    val title: String,
    val category: ProFeatureCategory,
    val status: ProFeatureStatus,
    val playerValue: String
)

internal object ProFeatureCatalog {
    val features: List<ProFeature> = listOf(
        ProFeature(
            id = "online_multiplayer",
            title = "Online Multiplayer",
            category = ProFeatureCategory.SOCIAL,
            status = ProFeatureStatus.BACKEND_REQUIRED,
            playerValue = "Play the familiar board remotely with family and friends."
        ),
        ProFeature(
            id = "friend_invite_matches",
            title = "Friend Invite Matches",
            category = ProFeatureCategory.SOCIAL,
            status = ProFeatureStatus.BACKEND_REQUIRED,
            playerValue = "Create low-friction private rooms from contacts or share links."
        ),
        ProFeature(
            id = "ranked_ladder",
            title = "Ranked Ladder",
            category = ProFeatureCategory.COMPETITIVE,
            status = ProFeatureStatus.BACKEND_REQUIRED,
            playerValue = "Give pro players a visible climb beyond casual wins."
        ),
        ProFeature(
            id = "elo_rating",
            title = "Elo Rating",
            category = ProFeatureCategory.COMPETITIVE,
            status = ProFeatureStatus.BACKEND_REQUIRED,
            playerValue = "Match serious players against similarly skilled opponents."
        ),
        ProFeature(
            id = "daily_challenges",
            title = "Daily Challenges",
            category = ProFeatureCategory.PROGRESSION,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Offer one quick objective each day without asking users to learn new rules."
        ),
        ProFeature(
            id = "weekly_tournaments",
            title = "Weekly Tournaments",
            category = ProFeatureCategory.COMPETITIVE,
            status = ProFeatureStatus.BACKEND_REQUIRED,
            playerValue = "Create appointment play and leaderboards for serious sessions."
        ),
        ProFeature(
            id = "seasonal_leagues",
            title = "Seasonal Leagues",
            category = ProFeatureCategory.LIVE_OPS,
            status = ProFeatureStatus.BACKEND_REQUIRED,
            playerValue = "Reset competitive goals often enough to keep players returning."
        ),
        ProFeature(
            id = "achievements",
            title = "Achievements",
            category = ProFeatureCategory.PROGRESSION,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Reward familiar moments like ladder streaks, exact finishes, and comebacks."
        ),
        ProFeature(
            id = "quest_campaign_map",
            title = "Quest Campaign Map",
            category = ProFeatureCategory.PROGRESSION,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Turn classic Snake & Ladder into a light single-player journey."
        ),
        ProFeature(
            id = "boss_bot_battles",
            title = "Boss Bot Battles",
            category = ProFeatureCategory.BOT_AI,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Give solo players memorable rivals with special table rules."
        ),
        ProFeature(
            id = "bot_personalities",
            title = "Bot Personalities",
            category = ProFeatureCategory.BOT_AI,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Make vs bot sessions feel less repetitive without complicating controls."
        ),
        ProFeature(
            id = "true_bot_difficulty_ai",
            title = "True Bot Difficulty AI",
            category = ProFeatureCategory.BOT_AI,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Tune bot pacing and tactical risk by difficulty instead of only changing penalties."
        ),
        ProFeature(
            id = "aggressive_defensive_bot_styles",
            title = "Aggressive And Defensive Bot Styles",
            category = ProFeatureCategory.BOT_AI,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Let expert players practice against different opponent behaviors."
        ),
        ProFeature(
            id = "post_match_analytics",
            title = "Post-Match Insights",
            category = ProFeatureCategory.PROGRESSION,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Explain wins through ladder gain, snake loss, knockbacks, and dice luck."
        ),
        ProFeature(
            id = "player_profile_stats",
            title = "Player Profile Stats",
            category = ProFeatureCategory.PROGRESSION,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Track wins, saves, best streaks, favorite mode, and completion pace locally."
        ),
        ProFeature(
            id = "win_streak",
            title = "Win Streak",
            category = ProFeatureCategory.COMPETITIVE,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Make short-session mastery visible even before full ranked play exists."
        ),
        ProFeature(
            id = "replay_system",
            title = "Replay System",
            category = ProFeatureCategory.PROGRESSION,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Review exact moves, snakes, ladders, knockbacks, and final turns."
        ),
        ProFeature(
            id = "shareable_match_highlights",
            title = "Shareable Match Highlights",
            category = ProFeatureCategory.SOCIAL,
            status = ProFeatureStatus.BACKEND_REQUIRED,
            playerValue = "Turn dramatic rolls and comebacks into clips players can send out."
        ),
        ProFeature(
            id = "time_attack",
            title = "Time Attack",
            category = ProFeatureCategory.MATCH_MODES,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Create a fast solo challenge for expert mobile players."
        ),
        ProFeature(
            id = "sudden_death",
            title = "Sudden Death",
            category = ProFeatureCategory.MATCH_MODES,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "End long matches decisively when players want short sessions."
        ),
        ProFeature(
            id = "best_of_three",
            title = "Best Of Three",
            category = ProFeatureCategory.MATCH_MODES,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Reduce dice variance for competitive pass-and-play matches."
        ),
        ProFeature(
            id = "missions_during_matches",
            title = "Missions During Matches",
            category = ProFeatureCategory.PROGRESSION,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Add lightweight objectives without replacing the familiar board game."
        ),
        ProFeature(
            id = "power_up_mode",
            title = "Power-Up Mode",
            category = ProFeatureCategory.BOARD_RULES,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Add optional tactical depth for players who already know the classic game."
        ),
        ProFeature(
            id = "card_tactical_mode",
            title = "Card Tactical Mode",
            category = ProFeatureCategory.BOARD_RULES,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Introduce simple one-tap cards such as reroll, shield, and swap."
        ),
        ProFeature(
            id = "revenge_mechanic",
            title = "Revenge Mechanic",
            category = ProFeatureCategory.BOARD_RULES,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Give knocked-back players a short comeback option."
        ),
        ProFeature(
            id = "shield_power_up",
            title = "Shield Power-Up",
            category = ProFeatureCategory.BOARD_RULES,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Protect players from one snake or one knockback in pro rules."
        ),
        ProFeature(
            id = "dice_modifiers_reroll_cards",
            title = "Dice Modifiers And Reroll Cards",
            category = ProFeatureCategory.BOARD_RULES,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Add controlled risk without changing the core roll-and-move loop."
        ),
        ProFeature(
            id = "trap_tiles",
            title = "Trap Tiles",
            category = ProFeatureCategory.BOARD_RULES,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Create board tension beyond fixed snakes."
        ),
        ProFeature(
            id = "mystery_tiles",
            title = "Mystery Tiles",
            category = ProFeatureCategory.BOARD_RULES,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Keep repeated boards fresh with small surprises."
        ),
        ProFeature(
            id = "shortcut_tiles",
            title = "Shortcut Tiles",
            category = ProFeatureCategory.BOARD_RULES,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Reward board awareness with alternate climbs."
        ),
        ProFeature(
            id = "risk_reward_routes",
            title = "Risk And Reward Routes",
            category = ProFeatureCategory.MATCH_MODES,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Let expert players choose safer or faster paths."
        ),
        ProFeature(
            id = "branching_board_paths",
            title = "Branching Board Paths",
            category = ProFeatureCategory.MATCH_MODES,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Add depth while preserving the familiar numbered-board structure."
        ),
        ProFeature(
            id = "custom_board_editor",
            title = "Custom Board Editor",
            category = ProFeatureCategory.CUSTOMIZATION,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Let players build family boards with their own snakes and ladders."
        ),
        ProFeature(
            id = "community_board_sharing",
            title = "Community Board Sharing",
            category = ProFeatureCategory.SOCIAL,
            status = ProFeatureStatus.BACKEND_REQUIRED,
            playerValue = "Turn custom boards into a social discovery loop."
        ),
        ProFeature(
            id = "themed_boards",
            title = "Themed Boards",
            category = ProFeatureCategory.CUSTOMIZATION,
            status = ProFeatureStatus.IN_APP_FOUNDATION,
            playerValue = "Extend the existing board theme system into richer regional and event looks."
        ),
        ProFeature(
            id = "dice_skins",
            title = "Dice Skins",
            category = ProFeatureCategory.CUSTOMIZATION,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Give players visible ownership with cosmetic dice."
        ),
        ProFeature(
            id = "token_skins",
            title = "Token Skins",
            category = ProFeatureCategory.CUSTOMIZATION,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Make each player easier to identify and more personal."
        ),
        ProFeature(
            id = "animated_token_trails",
            title = "Animated Token Trails",
            category = ProFeatureCategory.CUSTOMIZATION,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Make big ladders, snakes, and exact finishes feel premium."
        ),
        ProFeature(
            id = "emotes_reactions",
            title = "Emotes And Reactions",
            category = ProFeatureCategory.SOCIAL,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Add lightweight family-friendly expression during turns."
        ),
        ProFeature(
            id = "match_mvp_badges",
            title = "Match MVP Badges",
            category = ProFeatureCategory.PROGRESSION,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Reward the strongest climb, safest finish, and best comeback after every match."
        ),
        ProFeature(
            id = "spectator_mode",
            title = "Spectator Mode",
            category = ProFeatureCategory.SOCIAL,
            status = ProFeatureStatus.BACKEND_REQUIRED,
            playerValue = "Let family members watch private matches without taking a seat."
        ),
        ProFeature(
            id = "local_wifi_bluetooth_play",
            title = "Local Wi-Fi And Bluetooth Play",
            category = ProFeatureCategory.SOCIAL,
            status = ProFeatureStatus.BACKEND_REQUIRED,
            playerValue = "Serve unreliable-network environments with nearby-device play."
        ),
        ProFeature(
            id = "team_mode",
            title = "Team Mode",
            category = ProFeatureCategory.MATCH_MODES,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Make family sessions cooperative without requiring online accounts."
        ),
        ProFeature(
            id = "two_v_two_mode",
            title = "2v2 Mode",
            category = ProFeatureCategory.MATCH_MODES,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Create a clear pro format for four-player matches."
        ),
        ProFeature(
            id = "party_rules_presets",
            title = "Party Rules Presets",
            category = ProFeatureCategory.MATCH_MODES,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Offer classic, quick, pro, and family presets from the launch screen."
        ),
        ProFeature(
            id = "cloud_saves",
            title = "Cloud Saves",
            category = ProFeatureCategory.LIVE_OPS,
            status = ProFeatureStatus.BACKEND_REQUIRED,
            playerValue = "Keep saved games and profiles available across reinstalls."
        ),
        ProFeature(
            id = "cross_device_sync",
            title = "Cross-Device Sync",
            category = ProFeatureCategory.LIVE_OPS,
            status = ProFeatureStatus.BACKEND_REQUIRED,
            playerValue = "Let players continue from phone to tablet without friction."
        ),
        ProFeature(
            id = "haptic_themes",
            title = "Haptic Themes",
            category = ProFeatureCategory.CUSTOMIZATION,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Make snakes, ladders, shields, and wins feel distinct in hand."
        ),
        ProFeature(
            id = "dynamic_soundtracks",
            title = "Dynamic Soundtracks",
            category = ProFeatureCategory.CUSTOMIZATION,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Shift audio intensity during comebacks and final stretches."
        ),
        ProFeature(
            id = "limited_time_event_boards",
            title = "Limited-Time Event Boards",
            category = ProFeatureCategory.LIVE_OPS,
            status = ProFeatureStatus.OFFLINE_READY,
            playerValue = "Keep the familiar game fresh through festival and holiday boards."
        )
    )

    init {
        require(features.size == 50) { "Pro feature catalog must contain exactly 50 features." }
        require(features.map { it.id }.distinct().size == features.size) { "Pro feature ids must be unique." }
    }

    fun byCategory(): Map<ProFeatureCategory, List<ProFeature>> {
        return ProFeatureCategory.entries.associateWith { category ->
            features.filter { it.category == category }
        }
    }

    fun statusCounts(): Map<ProFeatureStatus, Int> {
        return ProFeatureStatus.entries.associateWith { status ->
            features.count { it.status == status }
        }
    }
}
