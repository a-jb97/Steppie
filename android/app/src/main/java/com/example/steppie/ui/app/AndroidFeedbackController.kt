package com.example.steppie.ui.app

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import com.example.steppie.core.environment.LocaleProvider
import com.example.steppie.ui.child.ChildRoutineFeedbackEvent
import java.util.Locale

internal class AndroidFeedbackController(
    private val context: Context,
    localeProvider: LocaleProvider,
) {
    private var ttsReady = false
    private var tts: TextToSpeech? = null
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 40)

    init {
        tts = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                tts?.language = Locale.forLanguageTag(localeProvider.languageTag())
                tts?.setSpeechRate(1.0f)
            }
        }
    }

    fun play(event: ChildRoutineFeedbackEvent) {
        if (event.vibrate) vibrate()
        if (event.sound) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 120)
        }
        val spokenText = event.spokenText
        if (ttsReady && spokenText != null) {
            tts?.stop()
            tts?.setSpeechRate(event.ttsRate)
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, event.ttsVolume)
            }
            tts?.speak(spokenText, TextToSpeech.QUEUE_FLUSH, params, "routine-complete")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        toneGenerator.release()
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
