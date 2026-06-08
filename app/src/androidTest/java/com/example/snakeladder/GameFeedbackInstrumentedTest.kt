package com.example.snakeladder

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameFeedbackInstrumentedTest {

    @Test
    fun feedbackHandlesVolumeVibrationDiceAndMoveTypeBranches() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val feedback = GameFeedback(context)

        try {
            feedback.setVibrationEnabled(false)
            feedback.setMasterVolume(-1f)
            feedback.play(null)
            feedback.play(MoveType.NORMAL)
            feedback.startDiceRollingSfx()
            feedback.stopDiceRollingSfx()

            feedback.setMasterVolume(2f)
            feedback.play(MoveType.SNAKE)
            feedback.play(MoveType.LADDER)
            feedback.play(MoveType.OVERSHOOT)
            feedback.play(MoveType.WIN)
            feedback.setVibrationEnabled(true)
            feedback.setMasterVolume(0f)
            feedback.play(MoveType.NORMAL)
        } finally {
            feedback.release()
        }
    }
}
