# Snake & Ladder UX Test Report

Date: 2026-06-03
Device tested: connected Android device, portrait orientation
Tester stance: pro mobile board-game player, with emphasis on repeat play, fast turn clarity, board readability, and feature discoverability.

## Overall Rating

Rating: 3.5 / 5

The game has a strong feature base for an offline Snake & Ladder app: multiple match modes, board layouts, vs-bot personalities, campaign, daily challenge, progression, store, custom board editing, audio/haptics, save flow, and party power-ups. The core board is readable and the dice loop works.

The main drag is presentation density. Many controls are packed into large modal sheets, several labels wrap or truncate, advanced flows are buried, and some persistence behavior did not feel reliable during testing. As a pro player, I enjoyed the ambition and the board variety, but the game needs stronger mobile ergonomics and clearer feedback to feel excellent.

## Feature Coverage Performed

1. Launch screen without the removed game preview and with the top game action card alignment.
2. New Game setup.
3. Local multiplayer setup.
4. Vs Bot setup.
5. Bot personality picker.
6. Easy, Medium, and Hard difficulty descriptions.
7. Match modes visible in setup: Classic, Time Attack, Sudden Death, Best Of Three, Party, Cards, and 2v2.
8. Board layout picker including Classic Board, Quick Climb, Snake Den, Speed Run, Ladder League, Trap Valley, Pro Chaos, Family Short, Festival Event, Monsoon Event, and Custom Lab.
9. Custom Lab editor reveal and raw snake/ladder pair input layout.
10. Classic local match loop with repeated dice rolls.
11. Board movement, player position cards, last-roll text, and turn handoff.
12. In-match settings.
13. Pause/resume state.
14. Board theme, dice skin, token trail, soundtrack, SFX, vibration, haptic theme, and fast animation settings.
15. Save Game dialog and exit confirmation.
16. Post-save launch state and Load Saved Game availability.
17. Progress screen with profile stats, tabs, and daily challenge card.
18. Store screen, economy display, disabled buy buttons, avatar/board item sections.
19. Campaign/Quest Map, locked/unlocked nodes, rewards, and Play affordance.
20. Daily play difficulty picker.
21. Pro Features catalog and category filters.
22. Party vs bot match and visible power-up inventory.
23. Shield power-up use and resulting feedback banner.
24. Source cross-check for progression, campaign, store, match modes, pro feature catalog, power-up rules, and board layouts.
25. New Game local player rename and avatar picker flow, including selected-avatar styling and pinned lineup summary.
26. Campaign map filters for All, Available, and Bosses.
27. Profile export sharing from Progress Rewards through the Android share sheet.
28. Board Controls settings for Compact match UI and Shake to roll with device sensor availability.
29. Tactical Cards match inventory, card timing hints, and bot power-up reason feedback.
30. Saved Games sheet with four seeded saves, threshold-based search, filtered result count, and one-result state.
31. Save Game failure path with inline retry feedback and store-level commit result handling.
32. Settings dialog category guidance, fixed tab picker, selected tab styling, content-only scrolling, and tab scroll reset.
33. Feature Guide wording for `Post-Match Insights` instead of the old analytics label.
34. Board viewport zoom controls, center-current-turn focus, Time Attack setup selection, and match timer badge placement.
35. Local Party match setup, Shield armed state, armed power-up cancellation, and stable returned-to-hand power-up ordering.
36. New Game setup step navigation for Setup, Mode, Board, and Review, including selected-step state and jump-to-section behavior.
37. Exit and Restart confirmation dialog shell, including consistent scrim, top-right close action, safe-area padding, and above-navigation action placement.
38. Launcher local-save status, saved-state copy, enabled Load Saved/Resume Latest state, and recreation-based resume regression.

## Evidence Captures

Screenshots and UI XML captures were saved under `/private/tmp/` during live testing:

- `/private/tmp/snake_launch.png`
- `/private/tmp/snake_newgame.png`
- `/private/tmp/snake_customlab_fields2.png`
- `/private/tmp/snake_match_start.png`
- `/private/tmp/snake_settings.png`
- `/private/tmp/snake_settings_audio.png`
- `/private/tmp/snake_midmatch.png`
- `/private/tmp/snake_save_dialog.png`
- `/private/tmp/snake_exit_dialog.png`
- `/private/tmp/snake_launch_saved.png`
- `/private/tmp/snake_progress.png`
- `/private/tmp/snake_store.png`
- `/private/tmp/snake_campaign.png`
- `/private/tmp/snake_daily.png`
- `/private/tmp/snake_profeatures.png`
- `/private/tmp/snake_party_start.png`
- `/private/tmp/snake_powerup_used.png`
- `/private/tmp/snake_batch_launch.png`
- `/private/tmp/snake_batch_new_game_mode_fixed.png`
- `/private/tmp/snake_batch_new_game_rules.png`
- `/private/tmp/snake_batch_new_game_team_chips.png`
- `/private/tmp/snake_final_launch.png`
- `/private/tmp/snake_final_campaign_map.png`
- `/private/tmp/snake_batch_settings_rules_replay.png`
- `/private/tmp/snake_batch_audio_comeback.png`
- `/private/tmp/snake_batch_replay_scrubber.png`
- `/private/tmp/snake_batch_store_purchase_confirm.png`
- `/private/tmp/snake_profile_rewards_final.png`
- `/private/tmp/snake_profile_started_stats.png`
- `/private/tmp/snake_daily_match_panel.png`
- `/private/tmp/snake_bonus_ladder_reason.png`
- `/private/tmp/snake_bot_turn_controls.png`
- `/private/tmp/snake_bot_turn_settings.png`
- `/private/tmp/snake_feedback_board.png`
- `/private/tmp/snake_feedback_audio.png`
- `/private/tmp/snake_feedback_settings.png`
- `/private/tmp/snake_board_focus_player.png`
- `/private/tmp/snake_board_route_focus.png`
- `/private/tmp/snake_accessibility_hub.png`
- `/private/tmp/snake_accessibility_hub.xml`
- `/private/tmp/snake_reduced_motion_settings.png`
- `/private/tmp/snake_reduced_motion_settings.xml`
- `/private/tmp/snake_launch_secondary.png`
- `/private/tmp/snake_launch_secondary.xml`
- `/private/tmp/snake_new_game_segmented.png`
- `/private/tmp/snake_new_game_segmented.xml`
- `/private/tmp/snake_new_game_difficulty_segmented_final.png`
- `/private/tmp/snake_new_game_difficulty_segmented_final.xml`
- `/private/tmp/snake_player_setup_launch.png`
- `/private/tmp/snake_player_setup_launch.xml`
- `/private/tmp/snake_player_setup_dialog.png`
- `/private/tmp/snake_player_setup_dialog.xml`
- `/private/tmp/snake_player_setup_panel.png`
- `/private/tmp/snake_player_setup_panel.xml`
- `/private/tmp/snake_player_setup_avatars.png`
- `/private/tmp/snake_player_setup_avatars.xml`
- `/private/tmp/snake_player_setup_cobra_selected.png`
- `/private/tmp/snake_campaign_filter_launch.xml`
- `/private/tmp/snake_campaign_filter_all.png`
- `/private/tmp/snake_campaign_filter_all.xml`
- `/private/tmp/snake_campaign_filter_available.png`
- `/private/tmp/snake_campaign_filter_available.xml`
- `/private/tmp/snake_profile_share_rewards_top.png`
- `/private/tmp/snake_profile_share_rewards_top.xml`
- `/private/tmp/snake_profile_share_tools.png`
- `/private/tmp/snake_profile_share_tools2.xml`
- `/private/tmp/snake_profile_share_button.png`
- `/private/tmp/snake_profile_share_button.xml`
- `/private/tmp/snake_profile_share_result.png`
- `/private/tmp/snake_profile_share_result.xml`
- `/private/tmp/snake_controls_compact_shake.png`
- `/private/tmp/snake_controls_compact_shake.xml`
- `/private/tmp/snake_controls_shake_status.png`
- `/private/tmp/snake_controls_shake_status.xml`
- `/private/tmp/snake_cards_inventory.png`
- `/private/tmp/snake_cards_inventory.xml`
- `/private/tmp/snake_saved_search.png`
- `/private/tmp/snake_saved_search.xml`
- `/private/tmp/snake_saved_search_bot.png`
- `/private/tmp/snake_saved_search_bot.xml`
- `/private/tmp/snake_saved_search_bot_result2.png`
- `/private/tmp/snake_saved_search_bot_result2.xml`
- `/private/tmp/snake_batch2_launch.png`
- `/private/tmp/snake_batch2_launch.xml`
- `/private/tmp/snake_batch2_newgame.png`
- `/private/tmp/snake_batch2_newgame.xml`
- `/private/tmp/snake_batch2_match.png`
- `/private/tmp/snake_batch2_match.xml`
- `/private/tmp/snake_batch2_settings_match.png`
- `/private/tmp/snake_batch2_settings_match.xml`
- `/private/tmp/snake_batch2_settings_visual.png`
- `/private/tmp/snake_batch2_settings_visual.xml`
- `/private/tmp/snake_batch2_settings_visual_scrolled.png`
- `/private/tmp/snake_batch2_settings_visual_scrolled.xml`
- `/private/tmp/snake_batch2_settings_match_reset.png`
- `/private/tmp/snake_batch2_settings_match_reset.xml`
- `/private/tmp/snake_batch2_profeatures_top.png`
- `/private/tmp/snake_batch2_profeatures_top.xml`
- `/private/tmp/snake_batch2_profeatures_insights.png`
- `/private/tmp/snake_batch2_profeatures_insights.xml`
- `/private/tmp/snake_batch3_launch.png`
- `/private/tmp/snake_batch3_launch.xml`
- `/private/tmp/snake_batch3_newgame.png`
- `/private/tmp/snake_batch3_newgame.xml`
- `/private/tmp/snake_batch3_board_default.png`
- `/private/tmp/snake_batch3_board_default.xml`
- `/private/tmp/snake_batch3_board_zoomed.png`
- `/private/tmp/snake_batch3_board_zoomed.xml`
- `/private/tmp/snake_batch3_setup_matchmode.png`
- `/private/tmp/snake_batch3_setup_matchmode.xml`
- `/private/tmp/snake_batch3_timeattack_setup.png`
- `/private/tmp/snake_batch3_timeattack_setup.xml`
- `/private/tmp/snake_batch3_timeattack_match.png`
- `/private/tmp/snake_batch3_timeattack_match.xml`
- `/private/tmp/snake_power_cancel_stable_party_live.png`
- `/private/tmp/snake_power_cancel_stable_party_live.xml`
- `/private/tmp/snake_power_cancel_stable_armed.png`
- `/private/tmp/snake_power_cancel_stable_armed.xml`
- `/private/tmp/snake_power_cancel_stable_returned.png`
- `/private/tmp/snake_power_cancel_stable_returned.xml`
- `/private/tmp/snake_step_launch.png`
- `/private/tmp/snake_step_launch.xml`
- `/private/tmp/snake_step_nav_setup.png`
- `/private/tmp/snake_step_nav_setup.xml`
- `/private/tmp/snake_step_nav_mode.png`
- `/private/tmp/snake_step_nav_mode.xml`
- `/private/tmp/snake_step_nav_board.png`
- `/private/tmp/snake_step_nav_board.xml`
- `/private/tmp/snake_step_nav_return_setup.png`
- `/private/tmp/snake_step_nav_return_setup.xml`
- `/private/tmp/snake_modal_launch.png`
- `/private/tmp/snake_modal_launch.xml`
- `/private/tmp/snake_modal_board.png`
- `/private/tmp/snake_modal_board.xml`
- `/private/tmp/snake_modal_settings.png`
- `/private/tmp/snake_modal_settings.xml`
- `/private/tmp/snake_modal_exit_confirm.png`
- `/private/tmp/snake_modal_exit_confirm.xml`
- `/private/tmp/snake_modal_restart_confirm.png`
- `/private/tmp/snake_modal_restart_confirm.xml`
- `/private/tmp/snake_save_status_launch.png`
- `/private/tmp/snake_save_status_launch.xml`
- `/private/tmp/snake_save_status_resumed_board.png`
- `/private/tmp/snake_save_status_resumed_board.xml`

## Latest Verification

2026-06-07 implementation pass:

- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin --console=plain` passed; the full coverage gate also ran `:app:testDebugUnitTest`.
- `./gradlew :app:connectedDebugAndroidTest --console=plain -Pandroid.testInstrumentationRunnerArguments.class=com.example.snakeladder.SnakeLadderUiTest#newGameDialog_allowsLocalPlayerNamesAndAvatarsBeforeStart` passed.
- `./gradlew :app:connectedDebugAndroidTest --console=plain -Pandroid.testInstrumentationRunnerArguments.class=com.example.snakeladder.SnakeLadderUiTest#newGameDialog_containsAllSetupOptionsAndStartsWithSelection` passed after removing the setup scroll hint that pushed player count chips below the first viewport.
- Manual device pass on RMX3998 captured the launch screen, New Game dialog, player setup panel, avatar choices, and selected Cobra avatar state.
- `./gradlew :app:connectedDebugAndroidTest --console=plain -Pandroid.testInstrumentationRunnerArguments.class=com.example.snakeladder.SnakeLadderUiTest#campaignDialogFiltersAvailableAndBossNodes` passed.
- Manual device pass on RMX3998 captured the Campaign filter panel in All state and the filtered Available state.
- `./gradlew :app:connectedDebugAndroidTest --console=plain -Pandroid.testInstrumentationRunnerArguments.class=com.example.snakeladder.SnakeLadderUiTest#progressionRewardsTabPreviewsLockedAvatarsAndEquipsOwnedItems,com.example.snakeladder.SnakeLadderUiTest#launchProgressionRewardsForwardsEquipProfileItem` passed.
- Manual device pass on RMX3998 captured the Rewards profile tools, visible Share Export action, and Android platform share sheet opened as `Share text`.
- `./gradlew :app:connectedDebugAndroidTest --console=plain -Pandroid.testInstrumentationRunnerArguments.class=com.example.snakeladder.SnakeLadderUiTest#settingsDialog_showsBoardThemeGameOptionsAndExit,com.example.snakeladder.SnakeLadderUiTest#settingsDialog_compactMatchUiHidesAdvancedMatchExtras,com.example.snakeladder.SnakeLadderUiTest#boardScreen_tacticalCardsShowsInventoryAndTimingHint,com.example.snakeladder.SnakeLadderUiTest#boardScreen_botPowerUpFeedbackExplainsChoiceReason,com.example.snakeladder.BoardSettingsStoreInstrumentedTest#saveLoadPersistsReducedMotionAndClampsVolume` passed.
- Manual device pass on RMX3998 captured Controls settings with Compact match UI and Shake to roll support, plus a Tactical Cards match showing the Cards inventory and card timing hint.
- `./gradlew :app:compileDebugAndroidTestKotlin --console=plain` passed after the save/load changes.
- `./gradlew :app:connectedDebugAndroidTest --console=plain -Pandroid.testInstrumentationRunnerArguments.class=com.example.snakeladder.SnakeLadderUiTest` passed all 62 UI tests, including saved-game search and save-failure regressions.
- `./gradlew :app:connectedDebugAndroidTest --console=plain -Pandroid.testInstrumentationRunnerArguments.class=com.example.snakeladder.SavedGameStoreInstrumentedTest` passed all 8 saved-game persistence tests.
- `./gradlew :app:testDebugUnitTest --console=plain` passed.
- Manual device pass on RMX3998 seeded four saved games, verified `Search saved games`, confirmed `4 of 4 saves shown`, filtered `bot` to `1 of 4 saves shown`, and captured the visible `Bot Arena` result row without overlap.
- `./gradlew :app:compileDebugAndroidTestKotlin --console=plain` passed after the Settings tab guidance and Feature Guide wording updates.
- Focused connected UI tests passed for settings tab guidance/reset, settings action visibility, Feature Guide wording, and Match tab settings visibility.
- `./gradlew :app:testDebugUnitTest --console=plain` passed after these changes.
- Manual device pass on RMX3998 captured launch, New Game, match start, Settings Match/Visual/scrolled/reset states, and the Feature Guide row showing `Post-Match Insights`. A system low-battery modal interrupted the first Guide capture and was dismissed before retesting the app state.
- `./gradlew :app:checkDebugCoverage --console=plain` passed with these metrics:
  - INSTRUCTION coverage: 97.19% (20856/21459)
  - LINE coverage: 99.00% (3071/3102)
  - BRANCH coverage: 90.69% (1189/1311)
  - METHOD coverage: 96.05% (559/582)
  - CLASS coverage: 98.70% (76/77)
- `./gradlew :app:compileDebugAndroidTestKotlin --console=plain` passed after adding board viewport and match timer regressions.
- `./gradlew :app:connectedDebugAndroidTest --console=plain -Pandroid.testInstrumentationRunnerArguments.class=com.example.snakeladder.SnakeLadderUiTest#boardScreen_boardViewportControlsZoomAndCenterCurrentTurn,com.example.snakeladder.SnakeLadderUiTest#boardScreen_timeAttackTimerUsesDedicatedBadgeAwayFromDice` passed.
- `./gradlew :app:testDebugUnitTest --console=plain` passed.
- `./gradlew :app:checkDebugCoverage --console=plain` passed with these metrics:
  - INSTRUCTION coverage: 97.19% (20856/21459)
  - LINE coverage: 99.00% (3071/3102)
  - BRANCH coverage: 90.69% (1189/1311)
  - METHOD coverage: 96.05% (559/582)
  - CLASS coverage: 98.70% (76/77)
- Manual device pass on RMX3998 captured launch, New Game, default board viewport controls, zoomed-and-centered board, Time Attack setup selection, and Time Attack match timer placement. The timer badge appeared in the match state strip above player cards while dice controls remained in their own lower panel.

2026-06-08 implementation pass:

- Implemented armed power-up cancellation for Party/Tactical power-ups. Tapping a queued Shield now cancels the armed state, returns it to hand, updates the last-move timeline, and keeps the power-up row in a stable order.
- Removed the fully implemented UX backlog item `Let players cancel armed power-up if possible`; remaining UX improvement points were renumbered.
- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin --console=plain` passed.
- Focused connected UI tests passed for `boardScreen_armedPowerUpStaysVisibleAndCanBeCanceled` and `boardScreen_powerUpsUseStableOrderAfterReturnedToHand`.
- `./gradlew :app:testDebugUnitTest --console=plain --tests com.example.snakeladder.RuleModelsTest` passed after adding targeted branch coverage for difficulty labels and bot power-up selection predicates.
- `./gradlew :app:testDebugUnitTest --console=plain` passed.
- `./gradlew :app:checkDebugCoverage --console=plain` passed with these metrics:
  - INSTRUCTION coverage: 97.19% (20970/21576)
  - LINE coverage: 99.04% (3088/3118)
  - BRANCH coverage: 90.40% (1196/1323)
  - METHOD coverage: 96.05% (560/583)
  - CLASS coverage: 98.70% (76/77)
- Manual device pass on RMX3998 captured a local Party match with Shield first in the inventory, the queued Shield state showing `Queued`, `Armed`, and `Shield armed. Tap to cancel and return it to hand.`, then the returned state with Shield first again as `x1` and last move `Player 1 canceled Shield`.
- Implemented New Game setup step navigation. The Setup, Mode, Board, and Review chips now jump to their section, expose `Current`/`Available` state for accessibility and tests, and avoid the previous passive progress-strip behavior.
- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin --console=plain` passed.
- `./gradlew :app:connectedDebugAndroidTest --console=plain -Pandroid.testInstrumentationRunnerArguments.class=com.example.snakeladder.SnakeLadderUiTest#newGameDialog_stepStripJumpsBetweenSetupSections` passed.
- Manual device pass on RMX3998 captured the New Game step-strip Setup state, jump to `2. Match Mode`, jump to `3. Board Layout`, and return to `1. Quick Setup`; the selected chip contrast, section landing, and sticky start footer were visually checked.
- `./gradlew :app:checkDebugCoverage --console=plain` passed with these metrics:
  - INSTRUCTION coverage: 97.19% (20970/21576)
  - LINE coverage: 99.04% (3088/3118)
  - BRANCH coverage: 90.40% (1196/1323)
  - METHOD coverage: 96.05% (560/583)
  - CLASS coverage: 98.70% (76/77)
- Implemented a shared in-match confirmation dialog shell for Exit and Restart. The shell uses the same scrim, top-right close affordance, system-bar padding, and constrained centered card for both destructive confirmations.
- Added focused connected UI tests for the Exit and Restart confirmation shells, including close-button dismissal and warning/content visibility.
- Added `LaunchSetupStoreInstrumentedTest` to cover invalid launch setup fallbacks and save-time player clamping, recovering the branch coverage margin after the modal changes.
- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin --console=plain` passed after the modal shell implementation.
- `./gradlew :app:connectedDebugAndroidTest --console=plain -Pandroid.testInstrumentationRunnerArguments.class=com.example.snakeladder.SnakeLadderUiTest#settingsExitConfirmationUsesSafeCloseShell,com.example.snakeladder.SnakeLadderUiTest#settingsRestartConfirmationUsesSafeCloseShell` passed.
- `./gradlew :app:connectedDebugAndroidTest --console=plain -Pandroid.testInstrumentationRunnerArguments.class=com.example.snakeladder.LaunchSetupStoreInstrumentedTest` passed.
- Manual device pass on RMX3998 captured launch, board, Settings, Exit confirmation, and Restart confirmation states; the confirmation cards were centered, the close action stayed in the top-right, and destructive actions remained above the navigation bar.
- `./gradlew :app:checkDebugCoverage --console=plain` passed with these metrics:
  - INSTRUCTION coverage: 97.32% (20997/21576)
  - LINE coverage: 99.04% (3088/3118)
  - BRANCH coverage: 90.63% (1199/1323)
  - METHOD coverage: 96.05% (560/583)
  - CLASS coverage: 98.70% (76/77)
- Added a launcher local-save status card. Empty launch now explains that Save Game in Settings enables local saves; saved launch now shows `Local save ready`, the saved count, and the latest save name.
- Added launcher UI tests for the empty and saved local-save status states.
- Added a MainActivity recreation regression that seeds a persisted save, recreates the activity, verifies Load Saved/Resume Latest are available, and resumes into the board.
- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin --console=plain` passed after the local-save status implementation.
- `./gradlew :app:connectedDebugAndroidTest --console=plain -Pandroid.testInstrumentationRunnerArguments.class=com.example.snakeladder.SnakeLadderUiTest#launchScreen_localSaveStatusExplainsEmptyState,com.example.snakeladder.SnakeLadderUiTest#launchScreen_localSaveStatusExplainsSavedState,com.example.snakeladder.SnakeLadderExploratoryUiTest#persistedSave_recreateShowsLoadAndResumeLatest` passed.
- Manual device pass on RMX3998 captured the launcher with a seeded `Visual Save`, confirmed Load Saved and Resume Latest were enabled, confirmed the `Local save ready` card was visible above the secondary navigation, then resumed into the board with Player 1 on cell 12.
- `./gradlew :app:checkDebugCoverage --console=plain` passed with these metrics:
  - INSTRUCTION coverage: 97.32% (20997/21576)
  - LINE coverage: 99.04% (3088/3118)
  - BRANCH coverage: 90.63% (1199/1323)
  - METHOD coverage: 96.05% (560/583)
  - CLASS coverage: 98.70% (76/77)

## Player Feedback

The board game itself feels familiar and approachable. Cell numbering is generally legible, snake and ladder paths are visible, and the token states are understandable. The party mode with power-ups is the most interesting advanced layer because it adds decisions without abandoning Snake & Ladder.

The launch screen feels polished at first glance, but it immediately shows truncation in important buttons. The new-game setup is feature-rich but modal-heavy, and a lot of expert-facing setup is below the fold. Store, Campaign, Progress, and Pro Features all work, but they read as dense panels rather than game-native experiences. The save/load path was concerning because after saving a named game, Load Saved Game still appeared disabled on the launcher.

## UI Improvement Points

Status note: when a UI pointer is reviewed during an implementation pass, it is tagged as `Fully implemented`, `Partially implemented`, or `Not implemented`. Untagged items in this section were not scanned in that pass.

1. [Partially implemented] Make selected chips more visually distinct across remaining chip groups. New Game step chips now use a strong active state; other chip families still need the same sweep.
2. [Partially implemented] Make unselected chips less heavy across remaining chip groups. New Game step chips now use lighter unselected styling; other chip families still need the same sweep.
3. [Partially implemented] Keep the board top visible above the fold.
4. [Partially implemented] Add cooldown/used styling to consumed power-ups.
5. [Partially implemented] Improve dialog scrim opacity consistency. Exit and Restart confirmations now share the same custom in-match scrim; remaining Material alert/dialog surfaces still need the same sweep.
6. [Partially implemented] Use snackbar or toast placement that is visible above nav bar.
7. [Partially implemented] Make Close button consistently positioned across all dialogs. Exit and Restart confirmations now use a top-right close affordance with content descriptions; remaining dialogs still need full alignment.
8. [Partially implemented] Use safe-area padding above the navigation bar. Exit and Restart confirmations now use system-bar padding and keep actions above the nav area; other modal surfaces still need review.
9. Improve landscape-specific layouts if supported.
10. Add tablet layout breakpoints for board and controls.
11. Avoid putting cards inside card-heavy modal stacks.
12. Improve contrast of gray disabled text on purple surfaces.
13. [Partially implemented] Make all tappable surfaces meet a consistent visual style. Exit and Restart confirmation actions now share one destructive-confirmation layout; broader button/chip styling still needs a full sweep.
14. [Partially implemented] Add accessibility labels for non-text icon states.
15. Verify dynamic font scaling at large accessibility sizes.
16. [Partially implemented] Add visual regression checks for truncation-prone labels. Exit/Restart confirmation shell tests and local-save status tests now assert key text/controls; broader screenshot-level truncation coverage is still pending.

## UX Improvement Points

Status note: when a UX pointer is reviewed during an implementation pass, it is tagged as `Fully implemented`, `Partially implemented`, or `Not implemented`. Untagged items in this section were not scanned in that pass.

1. [Partially implemented] Use a guided setup for casual players. New Game now has tappable Setup, Mode, Board, and Review section navigation; a full first-time tutorial is still pending.
2. [Partially implemented] Add a daily completion celebration.
3. [Partially implemented] Add story flavor to campaign nodes.
4. Offer starter unlocks early.
5. [Partially implemented] Make Progress more rewarding after partial matches.
6. Show achievements close to the actions that unlock them.
7. [Partially implemented] Add match analytics for dice luck and ladders.
8. [Partially implemented] Make settings changes preview instantly.
9. [Partially implemented] Animate the board to the active token after movement.
10. [Partially implemented] Make Time Attack urgency visible in setup and match.
11. [Partially implemented] Explain Sudden Death risk during play.
12. [Partially implemented] Show Best Of Three round score persistently.
13. [Partially implemented] Show 2v2 team colors and shared goal.
14. [Partially implemented] Use consistent terminology: match mode vs rules.
15. [Partially implemented] Reduce cognitive load in rule summaries.
16. Preserve replay after exit.
17. [Partially implemented] Provide non-audio equivalents for cues.
18. [Partially implemented] Ensure disabled buttons explain why to screen readers.
19. Support larger fonts without hiding actions.
20. Add localization readiness for long labels.
21. Avoid hardcoded English-heavy dense strings.
22. [Partially implemented] Make offline-first behavior explicit where useful. The launcher now labels saves as local/on-this-device and clarifies that saved matches are enabled through Save Game in Settings; other offline-only surfaces still need review.
23. [Partially implemented] Add bottom navigation if feature count grows.
24. [Partially implemented] Provide a clear home/back model across dialogs. New Game supports direct section return through the Setup chip, and Exit/Restart confirmations now share a predictable top-right close model; broader dialog navigation remains pending.
25. [Partially implemented] Avoid modal stacking for major app sections. Exit/Restart now use a single confirmation shell after the Settings action is selected; larger launch/progress/store/campaign section modal strategy remains pending.
26. [Partially implemented] Remember scroll positions only when useful across all long dialogs.
27. [Partially implemented] Reset dialog scroll on reopen for predictable starts across all long dialogs.
28. [Partially implemented] Test save/load after app process restart. Added a MainActivity recreation regression that verifies a persisted save is visible on relaunch and Resume Latest restores the board; full OS process-death coverage remains pending.
29. [Partially implemented] Keep all major actions reachable with one hand. Exit/Restart destructive and cancel actions now remain above the navigation bar; other long dialogs still need one-hand reach checks.
30. [Partially implemented] Reduce required vertical scrolling before playing. New Game setup can now jump directly to Mode, Board, and Review; other long dialogs still need similar shortcuts.
31. Avoid making player wait through nonessential transitions.
32. Make animations optional but polished by default.
33. Support short-session mode from launch.
34. [Partially implemented] Support family/pass-and-play mode with clearer handoff.
35. Add turn privacy option for pass-and-play devices.
36. Make current player announcement large but brief.
37. Add tutorial tips that disappear after first use.
38. Let experienced players disable tips.
39. [Partially implemented] Add UX tests for every top-level launch flow. Added New Game step-strip instrumentation coverage plus Exit/Restart confirmation shell coverage.
40. [Partially implemented] Add end-to-end tests for save, load, exit, and resume. Exit confirmation behavior has focused UI coverage, launch setup persistence has invalid/fallback coverage, and persisted Resume Latest after activity recreation is now covered; full save-exit-load-resume E2E remains pending.

## Highest Priority Fixes

1. Fix or clarify Save Game to Load Saved Game behavior.
2. Redesign launch button grid to eliminate truncation.
3. Split New Game setup into a clearer staged flow.
4. Move pause/save/exit out of the preferences-heavy settings dialog.
5. Add visual previews for boards, store items, and campaign nodes.
6. Make Daily show the actual daily challenge before difficulty selection.
7. Separate player-facing features from internal roadmap information.
8. Improve compact-phone layout for party/power-up matches.
9. Add explicit feedback labels for previous move versus current turn.
10. Add scroll affordances and truncation checks for every modal.
