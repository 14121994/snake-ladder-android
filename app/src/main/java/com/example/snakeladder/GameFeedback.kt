package com.example.snakeladder

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.roundToInt

internal class GameFeedback(context: Context) {
    private val appContext = context.applicationContext
    private var masterVolume = 1f
    private var vibrationEnabled = true
    private var hapticTheme = HapticThemeOption.CLASSIC
    private var soundtrack = SoundtrackOption.CLASSIC
    private var tone = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
    private var isReleased = false
    private val handler = Handler(Looper.getMainLooper())
    private val diceRollPlayer: MediaPlayer? = MediaPlayer.create(appContext, R.raw.dice_roll_sfx)?.apply {
        isLooping = false
        setVolume(masterVolume, masterVolume)
    }
    private val snakeHissPlayer: MediaPlayer? = MediaPlayer.create(appContext, R.raw.snake_hiss_sfx)?.apply {
        isLooping = false
        setVolume(masterVolume, masterVolume)
    }

    fun setMasterVolume(volume: Float) {
        masterVolume = volume.coerceIn(0f, 1f)
        if (isReleased) return
        diceRollPlayer?.setVolume(masterVolume, masterVolume)
        snakeHissPlayer?.setVolume(masterVolume, masterVolume)
        handler.removeCallbacksAndMessages(null)
        tone.release()
        val volumePercent = (masterVolume * 100f).roundToInt().coerceIn(0, 100)
        tone = ToneGenerator(AudioManager.STREAM_MUSIC, volumePercent)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        vibrationEnabled = enabled
    }

    fun setHapticTheme(theme: HapticThemeOption) {
        hapticTheme = theme
    }

    fun setSoundtrack(option: SoundtrackOption) {
        soundtrack = option
    }

    fun previewSoundtrack(option: SoundtrackOption) {
        setSoundtrack(option)
        val toneType = when (option) {
            SoundtrackOption.CLASSIC -> ToneGenerator.TONE_PROP_BEEP
            SoundtrackOption.COMEBACK -> ToneGenerator.TONE_PROP_ACK
            SoundtrackOption.FESTIVAL -> ToneGenerator.TONE_DTMF_8
        }
        startToneSafely(toneType, 110)
    }

    fun play(moveType: MoveType?) {
        if (masterVolume > 0.01f) {
            startToneSafely(openingTone(moveType), 90)
        }

        when (moveType) {
            MoveType.SNAKE -> {
                playSnakeSlideSfx()
                vibrate(180)
            }
            MoveType.LADDER -> {
                playLadderClimbJingle()
                vibrate(60)
            }
            MoveType.SHORTCUT,
            MoveType.BRANCH_PATH -> {
                playRouteJingle()
                vibrate(70)
            }
            MoveType.MYSTERY_TILE,
            MoveType.RISK_ROUTE,
            MoveType.TRAP -> {
                playTensionCue()
                vibrate(95)
            }
            MoveType.OVERSHOOT -> vibrate(40)
            MoveType.WIN -> {
                playWinJingle()
                vibrate(120)
            }
            else -> Unit
        }
    }

    private fun openingTone(moveType: MoveType?): Int {
        return when (soundtrack) {
            SoundtrackOption.CLASSIC -> ToneGenerator.TONE_PROP_BEEP
            SoundtrackOption.COMEBACK -> if (moveType == MoveType.SNAKE || moveType == MoveType.TRAP) {
                ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
            } else {
                ToneGenerator.TONE_PROP_ACK
            }
            SoundtrackOption.FESTIVAL -> ToneGenerator.TONE_DTMF_8
        }
    }

    fun startDiceRollingSfx() {
        if (masterVolume <= 0.01f) return
        diceRollPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.pause()
                }
                player.seekTo(0)
                player.start()
            } catch (_: IllegalStateException) {
            }
        }
    }

    fun stopDiceRollingSfx() {
        diceRollPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.pause()
                }
                player.seekTo(0)
            } catch (_: IllegalStateException) {
            }
        }
    }

    fun confirmRoll() {
        startToneSafely(ToneGenerator.TONE_PROP_ACK, 70)
        vibrate(28)
    }

    fun release() {
        isReleased = true
        handler.removeCallbacksAndMessages(null)
        diceRollPlayer?.release()
        snakeHissPlayer?.release()
        tone.release()
    }

    private fun startToneSafely(toneType: Int, durationMs: Int) {
        if (isReleased || masterVolume <= 0.01f) return
        try {
            tone.startTone(toneType, durationMs)
        } catch (_: RuntimeException) {
        }
    }

    private fun scheduleTone(toneType: Int, durationMs: Int, delayMs: Long) {
        if (isReleased || masterVolume <= 0.01f) return
        handler.postDelayed(
            { startToneSafely(toneType, durationMs) },
            delayMs
        )
    }

    private fun playWinJingle() {
        if (masterVolume <= 0.01f) return
        val sequence = listOf(
            ToneGenerator.TONE_DTMF_5 to 120,
            ToneGenerator.TONE_DTMF_7 to 120,
            ToneGenerator.TONE_DTMF_9 to 140,
            ToneGenerator.TONE_PROP_ACK to 220
        )
        var delayMs = 0L
        sequence.forEach { (toneType, durationMs) ->
            scheduleTone(toneType, durationMs, delayMs)
            delayMs += (durationMs + 30L)
        }
    }

    private fun playLadderClimbJingle() {
        if (masterVolume <= 0.01f) return
        val sequence = when (soundtrack) {
            SoundtrackOption.CLASSIC -> listOf(ToneGenerator.TONE_DTMF_3 to 70, ToneGenerator.TONE_DTMF_5 to 70, ToneGenerator.TONE_DTMF_7 to 90)
            SoundtrackOption.COMEBACK -> listOf(ToneGenerator.TONE_DTMF_2 to 80, ToneGenerator.TONE_DTMF_6 to 90, ToneGenerator.TONE_PROP_ACK to 100)
            SoundtrackOption.FESTIVAL -> listOf(ToneGenerator.TONE_DTMF_5 to 70, ToneGenerator.TONE_DTMF_8 to 70, ToneGenerator.TONE_DTMF_9 to 100)
        }
        var delayMs = 0L
        sequence.forEach { (toneType, durationMs) ->
            scheduleTone(toneType, durationMs, delayMs)
            delayMs += (durationMs + 20L)
        }
    }

    private fun playRouteJingle() {
        if (masterVolume <= 0.01f) return
        val sequence = listOf(
            ToneGenerator.TONE_DTMF_4 to 70,
            ToneGenerator.TONE_DTMF_8 to 90
        )
        var delayMs = 0L
        sequence.forEach { (toneType, durationMs) ->
            scheduleTone(toneType, durationMs, delayMs)
            delayMs += durationMs + 20L
        }
    }

    private fun playTensionCue() {
        if (masterVolume <= 0.01f) return
        val toneType = when (soundtrack) {
            SoundtrackOption.CLASSIC -> ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE
            SoundtrackOption.COMEBACK -> ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
            SoundtrackOption.FESTIVAL -> ToneGenerator.TONE_DTMF_0
        }
        startToneSafely(toneType, 120)
    }

    private fun playSnakeSlideSfx() {
        snakeHissPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.pause()
                }
                player.seekTo(0)
                player.start()
                return
            } catch (_: IllegalStateException) {
            }
        }
        if (masterVolume > 0.01f) {
            startToneSafely(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 170)
        }
    }

    private fun vibrate(durationMs: Long) {
        if (!vibrationEnabled) return
        val themedDuration = (durationMs * hapticTheme.multiplier).roundToInt().coerceAtLeast(15).toLong()
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(themedDuration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(themedDuration)
        }
    }
}
