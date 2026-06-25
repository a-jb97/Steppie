package com.example.steppie

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.steppie.data.sample.RoutineSampleData
import com.example.steppie.ui.child.ChildRoutineScreen
import com.example.steppie.ui.child.ChildRoutineViewModel
import com.example.steppie.ui.theme.SteppieTheme

class MainActivity : ComponentActivity() {
    private val routineRepository by lazy { RoutineSampleData.inMemoryRepository() }

    override fun onCreate(savedInstanceState: Bundle?) {
        enforceSupportedOrientation()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SteppieTheme {
                val childViewModel: ChildRoutineViewModel = viewModel(
                    factory = ChildRoutineViewModel.factory(routineRepository),
                )
                val state by childViewModel.uiState.collectAsStateWithLifecycle()
                ChildRoutineScreen(
                    state = state,
                    onShowList = childViewModel::showList,
                    onShowFocus = childViewModel::showFocus,
                    onSelectRoutine = childViewModel::selectRoutine,
                )
            }
        }
    }

    private fun enforceSupportedOrientation() {
        requestedOrientation = if (resources.configuration.smallestScreenWidthDp < 600) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}
