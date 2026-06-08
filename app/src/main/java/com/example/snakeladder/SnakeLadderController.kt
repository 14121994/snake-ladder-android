package com.example.snakeladder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

class SnakeLadderController(
    private var playerCount: Int = 2,
    private var gameMode: GameMode = GameMode.LOCAL_MULTIPLAYER,
    private var difficulty: GameDifficulty = GameDifficulty.EASY,
    private var botPersonality: BotPersonality = BotPersonality.STEADY,
    private var matchMode: MatchModePreset = MatchModePreset.CLASSIC,
    private var boardLayout: BoardLayout = BoardLayouts.classic,
    private var ruleSet: RuleSet = RuleSets.classic,
    private var humanAvatarId: String = "classic_token",
    private var playerSetups: List<PlayerSetup> = emptyList(),
    private var campaignNodeId: String? = null,
    private var dailyChallengeId: String? = null,
    private val diceRoller: () -> Int = { Random.nextInt(1, 7) }
) {
    private val playerColors = listOf(
        Color(0xFFE53935),
        Color(0xFF1E88E5),
        Color(0xFF43A047),
        Color(0xFFF57C00)
    )

    var state by mutableStateOf(initialState(playerCount, gameMode, difficulty, botPersonality, matchMode, boardLayout, ruleSet, humanAvatarId, playerSetups, campaignNodeId, dailyChallengeId))
        private set

    fun startGame(
        players: Int,
        mode: GameMode,
        difficulty: GameDifficulty = GameDifficulty.EASY,
        botPersonality: BotPersonality = this.botPersonality,
        matchMode: MatchModePreset = MatchModePreset.CLASSIC,
        boardLayoutId: String = BoardLayouts.CLASSIC_ID,
        humanAvatarId: String = this.humanAvatarId,
        playerSetups: List<PlayerSetup> = emptyList(),
        campaignNodeId: String? = null,
        dailyChallengeId: String? = null
    ) {
        gameMode = mode
        this.difficulty = difficulty
        this.botPersonality = botPersonality
        this.matchMode = matchMode
        this.boardLayout = BoardLayouts.byId(boardLayoutId)
        this.ruleSet = RuleSets.forMatchMode(matchMode)
        this.humanAvatarId = humanAvatarId
        this.playerSetups = playerSetups
        this.campaignNodeId = campaignNodeId
        this.dailyChallengeId = dailyChallengeId
        playerCount = when {
            matchMode == MatchModePreset.TEAM_MODE || matchMode == MatchModePreset.TWO_V_TWO -> 4
            mode == GameMode.VS_BOT -> 2
            else -> players.coerceIn(2, 4)
        }
        state = initialState(playerCount, gameMode, difficulty, botPersonality, matchMode, boardLayout, ruleSet, humanAvatarId, playerSetups, campaignNodeId, dailyChallengeId)
    }

    fun reset() {
        state = initialState(playerCount, gameMode, difficulty, botPersonality, matchMode, boardLayout, ruleSet, humanAvatarId, playerSetups, campaignNodeId, dailyChallengeId)
    }

    fun loadState(savedState: GameState) {
        gameMode = savedState.gameMode
        difficulty = savedState.difficulty
        botPersonality = savedState.botPersonality
        matchMode = savedState.matchMode
        boardLayout = BoardLayouts.byId(savedState.boardLayoutId)
        ruleSet = RuleSets.byId(savedState.ruleSetId)
        humanAvatarId = savedState.players.firstOrNull()?.avatarId ?: humanAvatarId
        playerSetups = savedState.players.map { PlayerSetup(name = it.name, avatarId = it.avatarId) }
        campaignNodeId = savedState.campaignNodeId
        dailyChallengeId = savedState.dailyChallengeId
        playerCount = if (savedState.gameMode == GameMode.VS_BOT) 2 else savedState.players.size.coerceIn(2, 4)
        state = savedState.copy(
            lastMovePlayerIndex = null,
            lastMovePath = emptyList(),
            lastMoveType = null,
            moveSignal = 0,
            knockBackMoves = emptyList()
        )
    }

    fun rollDice() {
        if (state.botPlayerIndex == state.currentPlayerIndex) {
            useBotPowerUpIfUseful()
        }
        applyRoll(diceRoller())
    }

    internal fun resolvePosition(currentPosition: Int, dice: Int): MoveResult {
        val tentative = currentPosition + dice
        if (tentative > 100) {
            val needed = 100 - currentPosition
            return MoveResult(
                position = currentPosition,
                eventMessage = "Exact finish missed: rolled $dice, needed $needed to reach 100",
                moveType = MoveType.OVERSHOOT,
                path = emptyList()
            )
        }

        val walkPath = (currentPosition + 1..tentative).toList()

        boardLayout.snakes[tentative]?.let { tail ->
            return MoveResult(
                position = tail,
                eventMessage = "Bitten by snake: $tentative -> $tail",
                moveType = MoveType.SNAKE,
                path = walkPath + tail
            )
        }

        boardLayout.ladders[tentative]?.let { top ->
            return MoveResult(
                position = top,
                eventMessage = "Climbed ladder: $tentative -> $top",
                moveType = MoveType.LADDER,
                path = walkPath + top
            )
        }

        boardLayout.specialTiles.firstOrNull { it.cell == tentative }?.let { tile ->
            return resolveSpecialTile(tile = tile, currentPosition = currentPosition, tentative = tentative, dice = dice, walkPath = walkPath)
        }

        return MoveResult(
            position = tentative,
            eventMessage = null,
            moveType = MoveType.NORMAL,
            path = walkPath
        )
    }

    private fun resolveSpecialTile(
        tile: BoardTile,
        currentPosition: Int,
        tentative: Int,
        dice: Int,
        walkPath: List<Int>
    ): MoveResult {
        return when (tile.type) {
            BoardTileType.TRAP -> {
                val target = (tentative - tile.penaltyCells.coerceAtLeast(7)).coerceAtLeast(1)
                MoveResult(
                    position = target,
                    eventMessage = "${tile.label}: $tentative -> $target",
                    moveType = MoveType.TRAP,
                    path = walkPath + target,
                    tileLabel = tile.label
                )
            }
            BoardTileType.MYSTERY -> {
                val mysteryChoice = (state.moveSignal + currentPosition + dice + tentative).mod(3)
                when (mysteryChoice) {
                    0 -> {
                        val target = (tentative + 3).coerceAtMost(99)
                        MoveResult(
                            position = target,
                            eventMessage = "${tile.label}: advanced to $target",
                            moveType = MoveType.MYSTERY_TILE,
                            path = walkPath + target,
                            tileLabel = tile.label
                        )
                    }
                    1 -> MoveResult(
                        position = tentative,
                        eventMessage = "${tile.label}: found Dice Boost",
                        moveType = MoveType.MYSTERY_TILE,
                        path = walkPath,
                        awardedPowerUps = listOf(PowerUpType.DICE_BOOST),
                        tileLabel = tile.label
                    )
                    else -> MoveResult(
                        position = tentative,
                        eventMessage = "${tile.label}: found Shield",
                        moveType = MoveType.MYSTERY_TILE,
                        path = walkPath,
                        awardedPowerUps = listOf(tile.rewardPowerUp ?: PowerUpType.SHIELD),
                        tileLabel = tile.label
                    )
                }
            }
            BoardTileType.SHORTCUT -> {
                val target = (tile.targetCell ?: (tentative + 12)).coerceIn(1, 100)
                MoveResult(
                    position = target,
                    eventMessage = "${tile.label}: $tentative -> $target",
                    moveType = MoveType.SHORTCUT,
                    path = walkPath + target,
                    awardedPowerUps = listOfNotNull(tile.rewardPowerUp),
                    tileLabel = tile.label
                )
            }
            BoardTileType.RISK_ROUTE -> {
                val success = dice >= 4
                val target = if (success) {
                    (tile.targetCell ?: (tentative + 16)).coerceIn(1, 100)
                } else {
                    (tentative - tile.penaltyCells.coerceAtLeast(10)).coerceAtLeast(1)
                }
                MoveResult(
                    position = target,
                    eventMessage = if (success) {
                        "${tile.label}: risk paid off to $target"
                    } else {
                        "${tile.label}: risk failed to $target"
                    },
                    moveType = MoveType.RISK_ROUTE,
                    path = walkPath + target,
                    awardedPowerUps = if (success) listOfNotNull(tile.rewardPowerUp) else emptyList(),
                    tileLabel = tile.label
                )
            }
            BoardTileType.BRANCH_PATH -> {
                val target = (tile.targetCell ?: (tentative + 8)).coerceIn(1, 100)
                val selectedTarget = if (state.botPlayerIndex == state.currentPlayerIndex && botPersonality == BotPersonality.DEFENSIVE) {
                    minOf(target, tentative + 5)
                } else {
                    target
                }
                MoveResult(
                    position = selectedTarget,
                    eventMessage = "${tile.label}: routed to $selectedTarget",
                    moveType = MoveType.BRANCH_PATH,
                    path = walkPath + selectedTarget,
                    awardedPowerUps = listOfNotNull(tile.rewardPowerUp),
                    tileLabel = tile.label
                )
            }
        }
    }

    fun usePowerUp(powerUpType: PowerUpType) {
        if (state.winnerIndex != null || !ruleSet.usesPowerUps) return

        val current = state.currentPlayerIndex
        val inventory = inventoryFor(current)
        if (powerUpType !in inventory) return

        val player = state.players.getOrNull(current) ?: return
        val updatedInventories = removePowerUp(state.powerUpInventories, current, powerUpType)
        val powerEvent = MatchEvent(
            turnNumber = nextEventNumber(),
            playerIndex = current,
            playerName = player.name,
            dice = 0,
            startPosition = player.position,
            landedPosition = player.position,
            finalPosition = player.position,
            moveType = MoveType.POWER_UP,
            path = emptyList(),
            powerUpUsed = powerUpType,
            roundNumber = state.roundNumber,
            winningTeamId = state.winningTeamId
        )

        when (powerUpType) {
            PowerUpType.SHIELD,
            PowerUpType.REROLL,
            PowerUpType.DICE_BOOST -> {
                val armed = (state.armedPowerUps.filterNot {
                    it.playerIndex == current && it.type == powerUpType
                } + PlayerArmedPowerUp(current, powerUpType)).distinct()
                updateAfterPowerUp(
                    inventories = updatedInventories,
                    armed = armed,
                    traps = state.activeTraps,
                    players = state.players,
                    event = powerEvent,
                    message = "${player.name} armed ${powerUpType.label}"
                )
            }
            PowerUpType.TRAP -> {
                val occupied = state.players.map { it.position }.toSet() + state.activeTraps.map { it.cell }
                val trapCell = PowerUpRuleEngine.trapCellFor(player.position, occupied)
                updateAfterPowerUp(
                    inventories = updatedInventories,
                    armed = state.armedPowerUps,
                    traps = state.activeTraps + BoardTrap(trapCell, current),
                    players = state.players,
                    event = powerEvent.copy(finalPosition = trapCell),
                    message = "${player.name} placed a trap on cell $trapCell"
                )
            }
            PowerUpType.REVENGE -> {
                val targetIndex = state.players.withIndex()
                    .filter { it.index != current }
                    .maxByOrNull { it.value.position }
                    ?.index
                val updatedPlayers = if (targetIndex != null) {
                    state.players.mapIndexed { index, item ->
                        if (index == targetIndex) {
                            item.copy(position = (item.position - 6).coerceAtLeast(1))
                        } else {
                            item
                        }
                    }
                } else {
                    state.players
                }
                val targetName = targetIndex?.let { state.players[it].name } ?: "a rival"
                updateAfterPowerUp(
                    inventories = updatedInventories,
                    armed = state.armedPowerUps,
                    traps = state.activeTraps,
                    players = updatedPlayers,
                    event = powerEvent,
                    message = "${player.name} used Revenge on $targetName"
                )
            }
            PowerUpType.MYSTERY -> {
                val mysteryRoll = (state.moveSignal + current + state.matchEvents.size).mod(3)
                val (mysteryPlayers, mysteryInventory, mysteryMessage) = when (mysteryRoll) {
                    0 -> Triple(
                        state.players.mapIndexed { index, item ->
                            if (index == current) item.copy(position = (item.position + 3).coerceAtMost(99)) else item
                        },
                        updatedInventories,
                        "${player.name} opened Mystery and advanced 3 cells"
                    )
                    1 -> Triple(
                        state.players,
                        addPowerUps(updatedInventories, current, listOf(PowerUpType.DICE_BOOST)),
                        "${player.name} opened Mystery and found Dice Boost"
                    )
                    else -> Triple(
                        state.players,
                        addPowerUps(updatedInventories, current, listOf(PowerUpType.SHIELD)),
                        "${player.name} opened Mystery and found Shield"
                    )
                }
                updateAfterPowerUp(
                    inventories = mysteryInventory,
                    armed = state.armedPowerUps,
                    traps = state.activeTraps,
                    players = mysteryPlayers,
                    event = powerEvent,
                    message = mysteryMessage
                )
            }
        }
    }

    fun cancelArmedPowerUp(powerUpType: PowerUpType) {
        if (state.winnerIndex != null || !ruleSet.usesPowerUps) return

        val current = state.currentPlayerIndex
        val player = state.players.getOrNull(current) ?: return
        val armed = state.armedPowerUps.toMutableList()
        val armedIndex = armed.indexOfFirst { it.playerIndex == current && it.type == powerUpType }
        if (armedIndex < 0) return

        armed.removeAt(armedIndex)
        val inventories = addPowerUps(state.powerUpInventories, current, listOf(powerUpType))
        val message = "${player.name} canceled ${powerUpType.label}"
        state = state.copy(
            statusMessage = message,
            moveHistory = (listOf(message) + state.moveHistory).take(20),
            powerUpInventories = inventories,
            armedPowerUps = armed,
            schemaVersion = GAME_STATE_SCHEMA_VERSION
        )
    }

    internal fun applyRoll(diceInput: Int) {
        if (state.winnerIndex != null) return

        var dice = diceInput.coerceIn(1, 6)
        val current = state.currentPlayerIndex
        val currentPlayer = state.players[current]
        val triggeredPowerUps = mutableListOf<PowerUpType>()
        val mutableInventories = mutableInventories()
        val mutableArmed = state.armedPowerUps.toMutableList()
        val mutableTraps = state.activeTraps.toMutableList()

        if (consumeArmed(mutableArmed, current, PowerUpType.REROLL)) {
            if (dice < 4) dice = 4
            triggeredPowerUps.add(PowerUpType.REROLL)
        }
        if (consumeArmed(mutableArmed, current, PowerUpType.DICE_BOOST)) {
            dice = (dice + 1).coerceAtMost(6)
            triggeredPowerUps.add(PowerUpType.DICE_BOOST)
        }

        val moveResult = resolvePosition(currentPlayer.position, dice)
        val landedPosition = if (moveResult.moveType == MoveType.OVERSHOOT) {
            currentPlayer.position
        } else {
            currentPlayer.position + dice
        }
        var resolvedMove = moveResult

        if (moveResult.moveType == MoveType.SNAKE && consumeShield(mutableInventories, mutableArmed, current)) {
            triggeredPowerUps.add(PowerUpType.SHIELD)
            resolvedMove = MoveResult(
                position = landedPosition,
                eventMessage = "Shield blocked snake at $landedPosition",
                moveType = MoveType.NORMAL,
                path = (currentPlayer.position + 1..landedPosition).toList()
            )
        }
        if (moveResult.moveType == MoveType.TRAP && consumeShield(mutableInventories, mutableArmed, current)) {
            triggeredPowerUps.add(PowerUpType.SHIELD)
            resolvedMove = MoveResult(
                position = landedPosition,
                eventMessage = "Shield blocked ${moveResult.tileLabel ?: "trap"} at $landedPosition",
                moveType = MoveType.NORMAL,
                path = (currentPlayer.position + 1..landedPosition).toList(),
                awardedPowerUps = moveResult.awardedPowerUps,
                tileLabel = moveResult.tileLabel
            )
        }

        val trapIndex = mutableTraps.indexOfFirst { trap ->
            trap.ownerPlayerIndex != current && trap.cell == resolvedMove.position && resolvedMove.position !in listOf(1, 100)
        }
        if (trapIndex >= 0) {
            val trap = mutableTraps.removeAt(trapIndex)
            triggeredPowerUps.add(PowerUpType.TRAP)
            resolvedMove = if (consumeShield(mutableInventories, mutableArmed, current)) {
                triggeredPowerUps.add(PowerUpType.SHIELD)
                resolvedMove.copy(
                    eventMessage = listOfNotNull(resolvedMove.eventMessage, "Shield blocked trap on ${trap.cell}")
                        .joinToString(". ")
                )
            } else {
                MoveResult(
                    position = 1,
                    eventMessage = "Triggered trap on ${trap.cell}: back to start",
                    moveType = MoveType.TRAP,
                    path = resolvedMove.path.ifEmpty { listOf(trap.cell) } + 1
                )
            }
        }

        var winner = if (resolvedMove.position == 100) current else null
        var winningTeamId = winner?.let { state.players.getOrNull(it)?.teamId }
            ?.takeIf { ruleSet.teamMode || matchMode == MatchModePreset.TEAM_MODE || matchMode == MatchModePreset.TWO_V_TWO }

        val shieldedKnockBacks = mutableListOf<String>()
        val knockBackMoves = if (winner == null && shouldKnockBack(resolvedMove.moveType)) {
            state.players.withIndex()
                .filter { (index, player) -> index != current && player.position == resolvedMove.position && resolvedMove.position != 1 }
                .mapNotNull { (index, player) ->
                    if (consumeShield(mutableInventories, mutableArmed, index)) {
                        triggeredPowerUps.add(PowerUpType.SHIELD)
                        shieldedKnockBacks.add(player.name)
                        null
                    } else {
                    KnockBackMove(
                        playerIndex = index,
                        path = ((player.position - 1) downTo 1).toList()
                    )
                    }
                }
        } else {
            emptyList()
        }
        val killedPlayerIndices = knockBackMoves.map { it.playerIndex }

        val updatedPlayers = state.players.mapIndexed { index, player ->
            if (index == current) player.copy(position = resolvedMove.position) else player
        }.mapIndexed { index, player ->
            if (index in killedPlayerIndices) player.copy(position = 1) else player
        }

        var extraTurn = winner == null &&
            (dice == 6 || resolvedMove.moveType == MoveType.LADDER)
        var finalMoveType = if (winner != null) MoveType.WIN else resolvedMove.moveType
        var finalPlayers = updatedPlayers
        var roundNumber = state.roundNumber
        var roundWins = normalizedRoundWins()
        var turnsRemaining = state.turnsRemaining?.let { (it - 1).coerceAtLeast(0) }
        var timeoutWinner = false

        val message = buildString {
            append("${currentPlayer.name} rolled $dice")
            if (triggeredPowerUps.isNotEmpty()) {
                append(" using ${triggeredPowerUps.distinct().joinToString { it.label }}")
            }
            resolvedMove.eventMessage?.let { append(". $it") }
            if (killedPlayerIndices.isNotEmpty()) {
                val knockedNames = killedPlayerIndices.joinToString { state.players[it].name }
                append(". ${currentPlayer.name} knocked $knockedNames back to start")
            }
            if (shieldedKnockBacks.isNotEmpty()) {
                append(". ${shieldedKnockBacks.joinToString()} blocked knockback with Shield")
            }
            if (extraTurn) {
                val reason = if (resolvedMove.moveType == MoveType.LADDER && dice != 6) {
                    "climbed ladder"
                } else {
                    "rolled 6"
                }
                append(". Bonus turn ($reason)")
            }
            botMoveComment(
                playerIndex = current,
                dice = dice,
                moveType = resolvedMove.moveType
            )?.let { append(". $it") }
            if (winner != null) append(". ${currentPlayer.name} wins!")
        }

        val turnNumber = nextEventNumber()
        var nextPlayer = when {
            winner != null -> current
            extraTurn -> current
            else -> (current + 1) % state.players.size
        }

        var statusMessage = message
        var newEvent = MatchEvent(
            turnNumber = turnNumber,
            playerIndex = current,
            playerName = currentPlayer.name,
            dice = dice,
            startPosition = currentPlayer.position,
            landedPosition = landedPosition,
            finalPosition = resolvedMove.position,
            moveType = finalMoveType,
            path = resolvedMove.path,
            knockedBackPlayerIndices = killedPlayerIndices,
            bonusTurn = extraTurn,
            winner = winner != null,
            triggeredPowerUps = triggeredPowerUps.distinct(),
            awardedPowerUps = resolvedMove.awardedPowerUps.distinct(),
            tileLabel = resolvedMove.tileLabel,
            roundNumber = state.roundNumber,
            winningTeamId = winningTeamId
        )

        val awards = (PowerUpRuleEngine.awardsForMove(ruleSet, newEvent) + resolvedMove.awardedPowerUps).distinct()
        if (awards.isNotEmpty()) {
            mutableInventories[current] = PowerUpRuleEngine.addPowerUps(mutableInventories[current], awards).toMutableList()
            statusMessage += ". Earned ${awards.joinToString { it.label }}"
        }

        if (winner != null && ruleSet.roundTarget > 1) {
            roundWins = roundWins.mapIndexed { index, wins ->
                if (index == current) wins + 1 else wins
            }
            val currentRoundWins = roundWins[current]
            if (currentRoundWins >= ruleSet.roundTarget) {
                finalMoveType = MoveType.WIN
                statusMessage += ". ${currentPlayer.name} wins best of ${ruleSet.roundTarget * 2 - 1}"
            } else {
                winner = null
                winningTeamId = null
                extraTurn = false
                finalMoveType = MoveType.ROUND_WIN
                roundNumber += 1
                finalPlayers = resetRoundPositions(updatedPlayers)
                nextPlayer = (current + 1) % state.players.size
                turnsRemaining = ruleSet.turnLimit
                statusMessage += ". Round ${roundNumber - 1} to ${currentPlayer.name}. Round $roundNumber starts."
            }
            newEvent = newEvent.copy(
                moveType = finalMoveType,
                winner = winner != null,
                bonusTurn = extraTurn,
                winningTeamId = winningTeamId
            )
        }

        if (winner == null && turnsRemaining == 0 && (matchMode == MatchModePreset.TIME_ATTACK || matchMode == MatchModePreset.SUDDEN_DEATH)) {
            val leader = finalPlayers.withIndex().maxWithOrNull(
                compareBy<IndexedValue<PlayerState>> { it.value.position }.thenByDescending { it.index }
            )
            winner = leader?.index
            winningTeamId = winner?.let { finalPlayers.getOrNull(it)?.teamId }
                ?.takeIf { ruleSet.teamMode || matchMode == MatchModePreset.TEAM_MODE || matchMode == MatchModePreset.TWO_V_TWO }
            timeoutWinner = winner != null
            extraTurn = false
            nextPlayer = winner ?: nextPlayer
            finalMoveType = MoveType.TIMEOUT
            statusMessage += ". Time expired: ${leader?.value?.name ?: "Leader"} wins by position."
            newEvent = newEvent.copy(
                moveType = MoveType.TIMEOUT,
                winner = timeoutWinner,
                bonusTurn = false,
                winningTeamId = winningTeamId
            )
        }

        val newEvents = (state.matchEvents + newEvent).takeLast(260)
        val newHistory = (listOf(statusMessage) + state.moveHistory).take(20)

        state = state.copy(
            players = finalPlayers,
            currentPlayerIndex = nextPlayer,
            lastDiceRoll = dice,
            statusMessage = statusMessage,
            bonusTurnGranted = extraTurn,
            winnerIndex = winner,
            moveHistory = newHistory,
            lastMovePlayerIndex = current,
            lastMovePath = resolvedMove.path,
            lastMoveType = finalMoveType,
            moveSignal = state.moveSignal + 1,
            difficulty = difficulty,
            knockBackMoves = knockBackMoves,
            matchEvents = newEvents,
            botPersonality = botPersonality,
            matchMode = matchMode,
            boardLayoutId = boardLayout.id,
            ruleSetId = ruleSet.id,
            powerUpInventories = mutableInventories.map { it.toList() },
            armedPowerUps = mutableArmed,
            activeTraps = mutableTraps,
            roundNumber = roundNumber,
            roundWins = roundWins,
            turnLimit = ruleSet.turnLimit,
            turnsRemaining = turnsRemaining,
            winningTeamId = winningTeamId,
            campaignNodeId = campaignNodeId,
            dailyChallengeId = dailyChallengeId,
            schemaVersion = GAME_STATE_SCHEMA_VERSION
        )
    }

    private fun shouldKnockBack(moveType: MoveType): Boolean {
        return when (difficulty) {
            GameDifficulty.EASY -> false
            GameDifficulty.MEDIUM -> moveType == MoveType.NORMAL
            GameDifficulty.HARD -> moveType == MoveType.NORMAL || moveType == MoveType.LADDER || moveType == MoveType.SNAKE
        }
    }

    private fun initialState(
        players: Int,
        mode: GameMode,
        difficulty: GameDifficulty,
        botPersonality: BotPersonality,
        matchMode: MatchModePreset,
        boardLayout: BoardLayout,
        ruleSet: RuleSet,
        humanAvatarId: String,
        playerSetups: List<PlayerSetup>,
        campaignNodeId: String?,
        dailyChallengeId: String?
    ): GameState {
        val safePlayers = when {
            matchMode == MatchModePreset.TEAM_MODE || matchMode == MatchModePreset.TWO_V_TWO -> 4
            mode == GameMode.VS_BOT -> 2
            else -> players.coerceIn(2, 4)
        }
        val startingInventory = PowerUpRuleEngine.startingInventory(ruleSet)
        val isBossBattle = campaignNodeId?.startsWith("boss_") == true
        val initialPlayers = (1..safePlayers).map { index ->
            val isBot = mode == GameMode.VS_BOT && index == 2
            val defaultName = if (isBot) botPersonality.displayName else defaultPlayerName(index)
            val setup = playerSetups.getOrNull(index - 1)
            val setupName = setup?.name
                ?.trim()
                ?.take(MAX_PLAYER_SETUP_NAME_LENGTH)
                ?.takeIf { it.isNotBlank() }
            val setupAvatarId = setup?.avatarId?.takeIf { it.isNotBlank() }
            PlayerState(
                name = if (isBot) defaultName else setupName ?: defaultName,
                color = playerColors[index - 1],
                position = 1,
                teamId = if (ruleSet.teamMode || matchMode == MatchModePreset.TEAM_MODE || matchMode == MatchModePreset.TWO_V_TWO) {
                    (index - 1) % 2
                } else {
                    null
                },
                avatarId = when {
                    isBot -> defaultPlayerAvatarId(index)
                    setupAvatarId != null -> setupAvatarId
                    index == 1 -> humanAvatarId
                    else -> defaultPlayerAvatarId(index)
                }
            )
        }
        return GameState(
            players = initialPlayers,
            currentPlayerIndex = 0,
            lastDiceRoll = null,
            statusMessage = "${initialPlayers.first().name} starts. Roll the dice.",
            bonusTurnGranted = false,
            winnerIndex = null,
            moveHistory = emptyList(),
            gameMode = mode,
            botPlayerIndex = if (mode == GameMode.VS_BOT) 1 else null,
            lastMovePlayerIndex = null,
            lastMovePath = emptyList(),
            lastMoveType = null,
            moveSignal = 0,
            difficulty = difficulty,
            knockBackMoves = emptyList(),
            matchEvents = emptyList(),
            botPersonality = botPersonality,
            matchMode = matchMode,
            boardLayoutId = boardLayout.id,
            ruleSetId = ruleSet.id,
            powerUpInventories = List(safePlayers) { index ->
                if (isBossBattle && mode == GameMode.VS_BOT && index == 1) {
                    listOf(PowerUpType.SHIELD, PowerUpType.REVENGE, PowerUpType.DICE_BOOST, PowerUpType.TRAP)
                } else {
                    startingInventory
                }
            },
            armedPowerUps = emptyList(),
            activeTraps = emptyList(),
            roundNumber = 1,
            roundWins = List(safePlayers) { 0 },
            turnLimit = ruleSet.turnLimit,
            turnsRemaining = ruleSet.turnLimit,
            winningTeamId = null,
            campaignNodeId = campaignNodeId,
            dailyChallengeId = dailyChallengeId,
            schemaVersion = GAME_STATE_SCHEMA_VERSION
        )
    }

    private fun nextEventNumber(): Int = state.matchEvents.size + 1

    private fun inventoryFor(playerIndex: Int): List<PowerUpType> {
        return state.powerUpInventories.getOrNull(playerIndex).orEmpty()
    }

    private fun mutableInventories(): MutableList<MutableList<PowerUpType>> {
        return MutableList(state.players.size) { index ->
            state.powerUpInventories.getOrNull(index).orEmpty().toMutableList()
        }
    }

    private fun removePowerUp(
        inventories: List<List<PowerUpType>>,
        playerIndex: Int,
        powerUpType: PowerUpType
    ): List<List<PowerUpType>> {
        val out = MutableList(state.players.size) { index ->
            inventories.getOrNull(index).orEmpty().toMutableList()
        }
        out.getOrNull(playerIndex)?.remove(powerUpType)
        return out.map { it.toList() }
    }

    private fun addPowerUps(
        inventories: List<List<PowerUpType>>,
        playerIndex: Int,
        additions: List<PowerUpType>
    ): List<List<PowerUpType>> {
        val out = MutableList(state.players.size) { index ->
            inventories.getOrNull(index).orEmpty().toMutableList()
        }
        out[playerIndex] = PowerUpRuleEngine.addPowerUps(out[playerIndex], additions).toMutableList()
        return out.map { it.toList() }
    }

    private fun consumeArmed(
        armed: MutableList<PlayerArmedPowerUp>,
        playerIndex: Int,
        powerUpType: PowerUpType
    ): Boolean {
        val index = armed.indexOfFirst { it.playerIndex == playerIndex && it.type == powerUpType }
        if (index < 0) return false
        armed.removeAt(index)
        return true
    }

    private fun consumeShield(
        inventories: MutableList<MutableList<PowerUpType>>,
        armed: MutableList<PlayerArmedPowerUp>,
        playerIndex: Int
    ): Boolean {
        if (consumeArmed(armed, playerIndex, PowerUpType.SHIELD)) return true
        return inventories.getOrNull(playerIndex)?.remove(PowerUpType.SHIELD) == true
    }

    private fun normalizedRoundWins(): List<Int> {
        return List(state.players.size) { index -> state.roundWins.getOrElse(index) { 0 } }
    }

    private fun resetRoundPositions(players: List<PlayerState>): List<PlayerState> {
        return players.map { it.copy(position = 1) }
    }

    private fun updateAfterPowerUp(
        inventories: List<List<PowerUpType>>,
        armed: List<PlayerArmedPowerUp>,
        traps: List<BoardTrap>,
        players: List<PlayerState>,
        event: MatchEvent,
        message: String
    ) {
        state = state.copy(
            players = players,
            statusMessage = message,
            moveHistory = (listOf(message) + state.moveHistory).take(20),
            matchEvents = (state.matchEvents + event).takeLast(260),
            powerUpInventories = inventories,
            armedPowerUps = armed,
            activeTraps = traps,
            lastMovePlayerIndex = event.playerIndex,
            lastMovePath = event.path,
            lastMoveType = MoveType.POWER_UP,
            moveSignal = state.moveSignal + 1,
            schemaVersion = GAME_STATE_SCHEMA_VERSION
        )
    }

    private fun useBotPowerUpIfUseful() {
        val botIndex = state.botPlayerIndex ?: return
        val bot = state.players.getOrNull(botIndex) ?: return
        val leaderPosition = state.players.maxOfOrNull { it.position } ?: bot.position
        val leaderGap = leaderPosition - bot.position
        val choice = PowerUpRuleEngine.chooseBotPowerUp(
            personality = botPersonality,
            inventory = inventoryFor(botIndex),
            botPosition = bot.position,
            leaderGap = leaderGap,
            difficulty = difficulty
        )
        if (choice != null) {
            usePowerUp(choice)
        }
    }

    private fun botMoveComment(
        playerIndex: Int,
        dice: Int,
        moveType: MoveType
    ): String? {
        if (gameMode != GameMode.VS_BOT || playerIndex != state.botPlayerIndex) return null
        return when (botPersonality) {
            BotPersonality.STEADY -> when (moveType) {
                MoveType.LADDER -> "Steady bot keeps the climb controlled"
                MoveType.SNAKE -> "Steady bot resets and stays in the race"
                else -> if (dice == 6) "Steady bot takes the bonus calmly" else null
            }
            BotPersonality.RISKY -> when (moveType) {
                MoveType.LADDER -> "Risky bot surges forward"
                MoveType.SNAKE -> "Risky bot pays for the gamble"
                else -> if (dice >= 5) "Risky bot pushes the pace" else null
            }
            BotPersonality.DEFENSIVE -> when (moveType) {
                MoveType.LADDER -> "Defensive bot banks a safe lead"
                MoveType.SNAKE -> "Defensive bot absorbs the setback"
                else -> if (dice <= 2) "Defensive bot advances carefully" else null
            }
            BotPersonality.PRO -> when (moveType) {
                MoveType.LADDER -> "Pro bot converts the climb into tempo"
                MoveType.SNAKE -> "Pro bot recalculates after the slide"
                MoveType.TRAP -> "Pro bot forced a board reset"
                else -> if (dice >= 5) "Pro bot pressures the endgame" else null
            }
        }
    }
}
