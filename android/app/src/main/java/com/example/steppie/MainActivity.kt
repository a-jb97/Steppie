package com.example.steppie

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.steppie.data.sample.RoutineSampleData
import com.example.steppie.ui.child.ChildRoutineFeedbackEvent
import com.example.steppie.ui.child.ChildRoutineScreen
import com.example.steppie.ui.child.ChildRoutineViewModel
import com.example.steppie.ui.theme.SteppieTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val routineRepository by lazy { RoutineSampleData.inMemoryRepository() }
    private lateinit var feedbackController: AndroidFeedbackController

    override fun onCreate(savedInstanceState: Bundle?) {
        enforceSupportedOrientation()
        super.onCreate(savedInstanceState)
        feedbackController = AndroidFeedbackController(this)
        enableEdgeToEdge()
        setContent {
            SteppieTheme {
                val childViewModel: ChildRoutineViewModel = viewModel(
                    factory = ChildRoutineViewModel.factory(routineRepository),
                )
                val state by childViewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(childViewModel) {
                    childViewModel.feedbackEvents.collect(feedbackController::play)
                }
                ChildRoutineScreen(
                    state = state,
                    onShowList = childViewModel::showList,
                    onShowFocus = childViewModel::showFocus,
                    onSelectRoutine = childViewModel::selectRoutine,
                    onCompleteRoutine = childViewModel::completeSelectedRoutine,
                    onAdvanceFromFeedback = childViewModel::advanceFromFeedback,
                    onUndoRoutine = childViewModel::undoLastCompletion,
                )
            }
        }
    }

    override fun onDestroy() {
        feedbackController.shutdown()
        super.onDestroy()
    }

    private fun enforceSupportedOrientation() {
        requestedOrientation = if (resources.configuration.smallestScreenWidthDp < 600) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}

private class AndroidFeedbackController(
    private val activity: ComponentActivity,
) {
    private var ttsReady = false
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(activity) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                tts?.language = Locale.getDefault()
                tts?.setSpeechRate(1.0f)
            }
        }
    }

    fun play(event: ChildRoutineFeedbackEvent) {
        if (event.vibrate) vibrate()
        if (ttsReady) {
            tts?.stop()
            tts?.speak(event.spokenText, TextToSpeech.QUEUE_FLUSH, null, "routine-complete")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            activity.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            activity.getSystemService(Vibrator::class.java)
        }
        vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
