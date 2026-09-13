package com.hakomi.practicetimer.ui.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hakomi.practicetimer.domain.Distribution
import com.hakomi.practicetimer.domain.Pacing
import com.hakomi.practicetimer.domain.PracticeSession
import com.hakomi.practicetimer.domain.RoundSplit
import com.hakomi.practicetimer.domain.Slot
import com.hakomi.practicetimer.domain.TimeFormat
import com.hakomi.practicetimer.domain.TimerTarget
import com.hakomi.practicetimer.ui.components.ConfirmDialog
import com.hakomi.practicetimer.ui.components.EditableMinutes
import com.hakomi.practicetimer.ui.components.Eyebrow
import com.hakomi.practicetimer.ui.components.PrimaryAction
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun SessionScreen(
    session: PracticeSession,
    onFeedbackMinutesChanged: (Int) -> Unit,
    onStartTimer: (TimerTarget) -> Unit,
    onFinishSession: () -> Unit,
) {
    val context = LocalContext.current
    val use24Hour = remember(context) { android.text.format.DateFormat.is24HourFormat(context) }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    // Always follow the natural next slot; the progress bar is status only.
    val selectedSlot = session.nextSlot
    var showExitConfirmation by remember { mutableStateOf(false) }

    val distribution = session.distribution(now)
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val completedRounds = (1..session.plan.rounds).count { session.isCompleted(Slot.Round(it)) }

    val requestExit = {
        if (session.allDone) onFinishSession() else showExitConfirmation = true
    }
    BackHandler(onBack = requestExit)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(insets),
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(8.dp))
            SessionHeader(distribution, session, use24Hour, onBack = requestExit)
            Spacer(Modifier.height(20.dp))
        }

        RoundProgressBar(
            totalRounds = session.plan.rounds,
            completedRounds = completedRounds,
        )

        when (val slot = selectedSlot) {
            null -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AllDoneContent()
                }
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    PrimaryAction(text = "Finish session", onClick = onFinishSession)
                    Spacer(Modifier.height(20.dp))
                }
            }

            is Slot.Round -> {
                var roundMinutes by remember(slot.number, distribution.roundMinutes) {
                    mutableIntStateOf(distribution.roundMinutes)
                }
                val split = RoundSplit.of(roundMinutes, session.feedbackMinutes)
                Eyebrow(
                    text = "Round ${slot.number}",
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(top = 20.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    EditableMinutes(
                        minutes = split.practiceMinutes,
                        onMinutesChanged = { practice ->
                            roundMinutes = (practice + split.feedbackMinutes).coerceAtLeast(1)
                        },
                    )
                }
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    PracticeFeedbackSplit(split = split, onFeedbackChanged = onFeedbackMinutesChanged)
                    Spacer(Modifier.height(18.dp))
                    PrimaryAction(
                        text = "Start round ${slot.number}",
                        onClick = {
                            onStartTimer(
                                TimerTarget(
                                    slot = slot,
                                    practiceMinutes = split.practiceMinutes,
                                    feedbackMinutes = split.feedbackMinutes,
                                ),
                            )
                        },
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }

            Slot.Break -> {
                Eyebrow(
                    text = "Break",
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(top = 20.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BreakTimeDisplay(minutes = session.plan.breakMinutes)
                }
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    PrimaryAction(
                        text = "Start break",
                        onClick = {
                            onStartTimer(TimerTarget(slot = Slot.Break, breakMinutes = session.plan.breakMinutes))
                        },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }

    if (showExitConfirmation) {
        ConfirmDialog(
            title = "Finish the session?",
            message = "Rounds that have not been run yet will be let go.",
            confirmLabel = "Finish",
            onConfirm = {
                showExitConfirmation = false
                onFinishSession()
            },
            onDismiss = { showExitConfirmation = false },
        )
    }
}

/**
 * Edge-to-edge bar of round segments. Each is 1/N of the screen width, flush with its neighbours.
 * Completed rounds are forest green; the current one is sage; later rounds stay empty for now.
 */
@Composable
private fun RoundProgressBar(
    totalRounds: Int,
    completedRounds: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val done = completedRounds.coerceIn(0, totalRounds)
    val showActive = done < totalRounds
    if (totalRounds <= 0) return

    Row(modifier = modifier.fillMaxWidth().height(10.dp)) {
        repeat(done) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(colors.primary),
            )
        }
        if (showActive) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(colors.secondary),
            )
        }
        // Reserve the remaining width so each segment stays 1/N of the full screen.
        val remaining = totalRounds - done - if (showActive) 1 else 0
        if (remaining > 0) {
            Spacer(Modifier.weight(remaining.toFloat()))
        }
    }
}

@Composable
private fun SessionHeader(distribution: Distribution, session: PracticeSession, use24Hour: Boolean, onBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back to planning",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderStat(
                icon = { Icon(Icons.Outlined.HourglassEmpty, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp)) },
                text = "${TimeFormat.minutes(distribution.remainingMinutes)} left",
            )
            HeaderStat(
                icon = { Icon(Icons.Outlined.Schedule, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp)) },
                text = (if (session.plan.pacing == Pacing.FLEXIBLE) "about " else "until ") +
                    TimeFormat.timeOfDay(distribution.sessionEndMillis, use24Hour),
            )
        }
        Spacer(Modifier.width(48.dp))
    }
}

@Composable
private fun HeaderStat(icon: @Composable () -> Unit, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        icon()
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun BreakTimeDisplay(minutes: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = minutes.toString(),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = "min",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AllDoneContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("All done", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Every round has had its time. Thank you for practising.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Two-tone bar: practice on the left, feedback on the right. Dragging (or tapping) anywhere moves
 * the divider in whole minutes; the result is committed when the finger lifts.
 */
@Composable
private fun PracticeFeedbackSplit(split: RoundSplit, onFeedbackChanged: (Int) -> Unit) {
    val total = split.totalMinutes
    var dragging by remember { mutableStateOf(false) }
    var draftFeedback by remember { mutableIntStateOf(split.feedbackMinutes) }
    val feedback = if (dragging) draftFeedback else split.feedbackMinutes
    val practice = total - feedback
    val fraction = if (total > 0) practice / total.toFloat() else 1f
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.outline, shape)
            .pointerInput(total) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val width = size.width.toFloat()
                    fun feedbackAt(x: Float) = (((width - x) / width) * total).roundToInt().coerceIn(0, maxOf(0, total - 1))
                    dragging = true
                    draftFeedback = feedbackAt(down.position.x)
                    drag(down.id) { change ->
                        draftFeedback = feedbackAt(change.position.x)
                        change.consume()
                    }
                    dragging = false
                    onFeedbackChanged(draftFeedback)
                }
            },
    ) {
        val widthPx = constraints.maxWidth
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(maxWidth * (1f - fraction))
                .align(Alignment.CenterEnd)
                .background(colors.tertiaryContainer.copy(alpha = 0.55f)),
        )
        Box(
            modifier = Modifier
                .offset { IntOffset((widthPx * fraction).roundToInt() - 3, 0) }
                .fillMaxHeight()
                .width(6.dp)
                .background(colors.tertiary),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Practice", style = MaterialTheme.typography.titleLarge, color = colors.primary)
                Text(TimeFormat.minutes(practice), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("Feedback", style = MaterialTheme.typography.titleLarge, color = colors.tertiary)
                Text(TimeFormat.minutes(feedback), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}
