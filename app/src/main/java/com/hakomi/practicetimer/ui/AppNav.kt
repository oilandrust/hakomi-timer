package com.hakomi.practicetimer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hakomi.practicetimer.HakomiApp
import com.hakomi.practicetimer.domain.Slot
import com.hakomi.practicetimer.ui.plan.PlanScreen
import com.hakomi.practicetimer.ui.plan.PlanViewModel
import com.hakomi.practicetimer.ui.session.SessionScreen
import com.hakomi.practicetimer.ui.timer.TimerScreen

object Routes {
    const val PLAN = "plan"
    const val SESSION = "session"
    const val TIMER = "timer"
}

@Composable
fun AppNav() {
    val app = HakomiApp.from(LocalContext.current)
    val repository = app.repository
    val controller = app.timerController
    val navController = rememberNavController()

    val session by repository.session.collectAsStateWithLifecycle()
    val timerState by controller.state.collectAsStateWithLifecycle()
    val now by controller.now.collectAsStateWithLifecycle()

    // Cold start lands wherever the user left off: an active timer, an open session, or the planner.
    val startDestination = remember {
        when {
            repository.session.value != null && controller.state.value != null -> Routes.TIMER
            repository.session.value != null -> Routes.SESSION
            else -> Routes.PLAN
        }
    }

    fun resetTo(route: String) = navController.navigate(route) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }

    fun backToSession() {
        if (!navController.popBackStack(Routes.SESSION, inclusive = false)) resetTo(Routes.SESSION)
    }

    fun finishSession() {
        controller.clear()
        repository.clearSession()
        resetTo(Routes.PLAN)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        NavHost(navController = navController, startDestination = startDestination) {
            composable(Routes.PLAN) {
                val viewModel: PlanViewModel = viewModel(factory = PlanViewModel.factory(repository))
                PlanScreen(viewModel = viewModel, onSessionStarted = { resetTo(Routes.SESSION) })
            }

            composable(Routes.SESSION) {
                val current = session
                if (current == null) {
                    LaunchedEffect(Unit) { resetTo(Routes.PLAN) }
                    Box(Modifier.fillMaxSize())
                } else {
                    SessionScreen(
                        session = current,
                        onFeedbackMinutesChanged = { minutes ->
                            repository.updateSession { it.copy(feedbackMinutes = minutes) }
                        },
                        onStartTimer = { target ->
                            controller.begin(target)
                            navController.navigate(Routes.TIMER) { launchSingleTop = true }
                        },
                        onFinishSession = ::finishSession,
                    )
                }
            }

            composable(Routes.TIMER) {
                val state = timerState
                if (state == null || session == null) {
                    LaunchedEffect(Unit) { if (session == null) resetTo(Routes.PLAN) else backToSession() }
                    Box(Modifier.fillMaxSize())
                } else {
                    TimerScreen(
                        state = state,
                        now = now,
                        onStart = controller::start,
                        onTogglePause = controller::togglePause,
                        onSkip = controller::skip,
                        onComplete = { slot: Slot ->
                            repository.updateSession { it.markCompleted(slot) }
                            controller.clear()
                            backToSession()
                        },
                        onAbandon = {
                            controller.clear()
                            backToSession()
                        },
                    )
                }
            }
        }
    }
}