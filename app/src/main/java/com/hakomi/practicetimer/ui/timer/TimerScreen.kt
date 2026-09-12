package com.hakomi.practicetimer.ui.timer

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hakomi.practicetimer.domain.Phase
import com.hakomi.practicetimer.domain.RunState
import com.hakomi.practicetimer.domain.Slot
import com.hakomi.practicetimer.domain.TimeFormat
import com.hakomi.practicetimer.domain.TimerEngine
import com.hakomi.practicetimer.domain.TimerState
import com.hakomi.practicetimer.ui.components.ConfirmDialog
import com.hakomi.practicetimer.ui.components.Eyebrow
import com.hakomi.practicetimer.ui.components.PrimaryAction
import com.hakomi.practicetimer.ui.components.SecondaryAction

@Composable
fun TimerScreen(
    state: TimerState,
    now: Long,
    onStart: () -> Unit,
    onTogglePause: () -> Unit,
    onSkip: () -> Unit,
    /** Marks the slot as done and leaves the screen. */
    onComplete: (Slot) -> Unit,
    /** Leaves without marking anything. */
    onAbandon: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val slot = state.target.slot
    var showExitConfirmation by remember { mutableStateOf(false) }

    // The screen stays lit for as long as a phase is running or paused: the point of a native timer.
    val view = LocalView.current
    val keepAwake = state.isRunning || state.isPaused
    DisposableEffect(view, keepAwake) {
        view.keepScreenOn = keepAwake
        onDispose { view.keepScreenOn = false }
    }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val startWithPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        onStart()
    }

    val requestExit = {
        when {
            state.isFinished -> onComplete(slot)
            state.hasStarted -> showExitConfirmation = true
            else -> onAbandon()
        }
    }
    BackHandler(onBack = requestExit)

    val phaseColor by animateColorAsState(
        targetValue = when {
            !state.isRunning -> colors.onSurface
            state.phase == Phase.PRACTICE -> colors.primary
            state.phase == Phase.FEEDBACK -> colors.tertiary
            else -> colors.secondary
        },
        label = "phaseColor",
    )
    val remainingSeconds = TimerEngine.remainingSeconds(state, now)
    val countdown = TimeFormat.countdown(remainingSeconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.safeDrawing.asPaddingValues())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = requestExit) {
                Icon(Icons.Rounded.Close, contentDescription = "Leave the timer", tint = colors.onSurfaceVariant)
            }
        }

        Spacer(Modifier.weight(1f))

        Eyebrow(phaseTitle(state), color = colors.secondary)
        Spacer(Modifier.height(12.dp))
        Text(
            text = countdown,
            style = if (countdown.length > 5) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displayLarge,
            color = phaseColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = phaseHint(state),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(1.2f))

        when {
            state.isFinished -> PrimaryAction(
                text = if (state.target.isBreak) "Finish break" else "Finish round",
                onClick = { onComplete(slot) },
            )

            state.runState == RunState.IDLE -> PrimaryAction(
                text = startLabel(state),
                onClick = startWithPermission,
                containerColor = when (state.phase) {
                    Phase.FEEDBACK -> colors.tertiary
                    Phase.BREAK -> colors.secondary
                    else -> colors.primary
                },
                contentColor = colors.onPrimary,
            )

            else -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryAction(
                    text = if (state.isPaused) "Resume" else "Pause",
                    onClick = onTogglePause,
                    modifier = Modifier.weight(1f),
                )
                SecondaryAction(
                    text = "Finish",
                    onClick = { if (state.phase == Phase.BREAK) onComplete(Slot.Break) else onSkip() },
                    contentColor = colors.error,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(40.dp))
    }

    if (showExitConfirmation) {
        ConfirmDialog(
            title = if (state.target.isBreak) "End the break?" else "Finish this round?",
            message = if (state.target.isBreak) {
                "The break will be marked as taken."
            } else {
                "The round will be marked as complete and the remaining time shared among the rounds still to come."
            },
            confirmLabel = "Finish",
            onConfirm = {
                showExitConfirmation = false
                onComplete(slot)
            },
            onDismiss = { showExitConfirmation = false },
        )
    }
}

private fun phaseTitle(state: TimerState): String {
    val slot = state.target.slot
    val prefix = if (slot is Slot.Round) "Round ${slot.number} · " else ""
    return when (state.phase) {
        Phase.PRACTICE -> "${prefix}Practice"
        Phase.FEEDBACK -> "${prefix}Feedback"
        Phase.BREAK -> "Break"
        Phase.FINISHED -> if (slot is Slot.Round) "Round ${slot.number} complete" else "Break complete"
    }
}

private fun phaseHint(state: TimerState): String = when {
    state.isPaused -> "Paused. Take the moment you need."
    state.isFinished -> "Thank you. Take a breath before moving on."
    state.isRunning -> when (state.phase) {
        Phase.PRACTICE -> "Stay with what is here."
        Phase.FEEDBACK -> "Notice, then share what you noticed."
        Phase.BREAK -> "Step away. The timer keeps the time."
        Phase.FINISHED -> ""
    }
    else -> when (state.phase) {
        Phase.PRACTICE -> "Settle in, then begin when the group is ready."
        Phase.FEEDBACK -> "Practice is complete. Begin feedback when everyone has landed."
        Phase.BREAK -> "The break begins as soon as you like."
        Phase.FINISHED -> ""
    }
}

private fun startLabel(state: TimerState): String {
    val minutes = TimeFormat.minutes((state.phaseDurationMillis / 60_000L).toInt())
    return when (state.phase) {
        Phase.PRACTICE -> "Start $minutes practice"
        Phase.FEEDBACK -> "Start $minutes feedback"
        Phase.BREAK -> "Start $minutes break"
        Phase.FINISHED -> "Finish"
    }
}
