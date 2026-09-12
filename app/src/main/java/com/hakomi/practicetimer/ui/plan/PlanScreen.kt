package com.hakomi.practicetimer.ui.plan

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hakomi.practicetimer.domain.Pacing
import com.hakomi.practicetimer.domain.SessionPlan
import com.hakomi.practicetimer.domain.TimeFormat
import com.hakomi.practicetimer.domain.TimingMode
import com.hakomi.practicetimer.ui.components.ChoicePill
import com.hakomi.practicetimer.ui.components.Eyebrow
import com.hakomi.practicetimer.ui.components.PrimaryAction
import com.hakomi.practicetimer.ui.components.RoundBubble
import com.hakomi.practicetimer.ui.components.SectionCard
import com.hakomi.practicetimer.ui.components.SegmentedToggle
import com.hakomi.practicetimer.ui.components.Stepper

@Composable
fun PlanScreen(
    viewModel: PlanViewModel,
    onSessionStarted: () -> Unit,
) {
    val now by viewModel.now.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val use24Hour = remember(context) { android.text.format.DateFormat.is24HourFormat(context) }
    val plan = viewModel.plan(now)
    val insets = WindowInsets.safeDrawing.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(insets)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(28.dp))
        Eyebrow("Hakomi practice", color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(6.dp))
        Text("Plan the session", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Set the container. The rounds will find their length.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))

        TotalSessionCard(viewModel, now, use24Hour)
        Spacer(Modifier.height(16.dp))
        RoundsCard(viewModel)
        Spacer(Modifier.height(16.dp))
        BreakCard(viewModel)
        Spacer(Modifier.height(16.dp))
        SummaryCard(plan, now, use24Hour, onStart = { if (viewModel.launchSession()) onSessionStarted() })
        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun TotalSessionCard(viewModel: PlanViewModel, now: Long, use24Hour: Boolean) {
    var showPicker by remember { mutableStateOf(false) }

    SectionCard(
        title = "Total session",
        trailing = {
            SegmentedToggle(
                options = listOf(TimingMode.DURING to "During", TimingMode.UNTIL to "Until"),
                selected = viewModel.timingMode,
                onSelect = viewModel::chooseTimingMode,
                modifier = Modifier.width(168.dp),
            )
        },
    ) {
        when (viewModel.timingMode) {
            TimingMode.DURING -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SessionPlan.durationPresets.forEach { minutes ->
                        ChoicePill(
                            text = if (minutes == 120) "2 hours" else "$minutes min",
                            selected = viewModel.durationMinutes == minutes,
                            onClick = { viewModel.chooseDuration(minutes) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Stepper(
                        value = (viewModel.durationMinutes / 60).toString(),
                        unit = "hr",
                        onDecrement = { viewModel.nudgeDuration(-60) },
                        onIncrement = { viewModel.nudgeDuration(60) },
                        decrementEnabled = viewModel.durationMinutes >= 60 + PlanViewModel.MIN_DURATION,
                        incrementEnabled = viewModel.durationMinutes + 60 <= PlanViewModel.MAX_DURATION,
                        modifier = Modifier.weight(1f),
                    )
                    Stepper(
                        value = (viewModel.durationMinutes % 60).toString(),
                        unit = "min",
                        onDecrement = { viewModel.nudgeDuration(-5) },
                        onIncrement = { viewModel.nudgeDuration(5) },
                        decrementEnabled = viewModel.durationMinutes > PlanViewModel.MIN_DURATION,
                        incrementEnabled = viewModel.durationMinutes < PlanViewModel.MAX_DURATION,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            TimingMode.UNTIL -> {
                val presets = viewModel.endPresets(now)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    presets.forEach { option ->
                        ChoicePill(
                            text = TimeFormat.timeOfDay(option.endMillis, use24Hour),
                            selected = viewModel.targetEndMillis == option.endMillis,
                            onClick = { viewModel.selectEndTime(option.endMillis) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Stepper(
                        value = viewModel.targetEndMillis?.let { TimeFormat.timeOfDay(it, use24Hour) } ?: "--:--",
                        onDecrement = { viewModel.nudgeEndTime(-5) },
                        onIncrement = { viewModel.nudgeEndTime(5) },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { showPicker = true }) {
                        Text("Pick a time", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "That is ${TimeFormat.minutes(viewModel.totalMinutes(now))} from now.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(18.dp))
        Eyebrow("If the group runs late")
        Spacer(Modifier.height(10.dp))
        SegmentedToggle(
            options = listOf(Pacing.FINISH_ON_TIME to "Finish on time", Pacing.FLEXIBLE to "Flexible"),
            selected = viewModel.pacing,
            onSelect = viewModel::choosePacing,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = when (viewModel.pacing) {
                Pacing.FINISH_ON_TIME -> "Remaining rounds shrink so the session still ends when planned."
                Pacing.FLEXIBLE -> "Every round keeps its full length; the end time moves instead."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showPicker) {
        EndTimePickerDialog(
            initial = viewModel.targetEndLocalTime(),
            use24Hour = use24Hour,
            onDismiss = { showPicker = false },
            onConfirm = { hour, minute ->
                viewModel.pickEndTime(hour, minute)
                showPicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EndTimePickerDialog(
    initial: java.time.LocalTime?,
    use24Hour: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initial?.hour ?: 12,
        initialMinute = initial?.minute ?: 0,
        is24Hour = use24Hour,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Finish by", style = MaterialTheme.typography.headlineSmall) },
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    selectorColor = MaterialTheme.colorScheme.primary,
                    timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    periodSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("Set") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun RoundsCard(viewModel: PlanViewModel) {
    SectionCard(title = "Rounds") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        ) {
            SessionPlan.roundPresets.forEach { count ->
                RoundBubble(
                    label = count.toString(),
                    selected = viewModel.rounds == count,
                    completed = false,
                    onClick = { viewModel.chooseRounds(count) },
                    size = 62,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Each round is one person's turn as client, with practice then feedback.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BreakCard(viewModel: PlanViewModel) {
    SectionCard(title = "Break & landing") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SessionPlan.breakPresets.forEach { minutes ->
                ChoicePill(
                    text = if (minutes == 0) "None" else "$minutes min",
                    selected = viewModel.breakMinutes == minutes,
                    onClick = { viewModel.chooseBreak(minutes) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(
                checked = viewModel.includeLanding,
                onCheckedChange = viewModel::chooseIncludeLanding,
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
            )
            Spacer(Modifier.width(4.dp))
            Stepper(
                value = viewModel.landingMinutes.toString(),
                unit = "min",
                enabled = viewModel.includeLanding,
                onDecrement = { viewModel.nudgeLanding(-1) },
                onIncrement = { viewModel.nudgeLanding(1) },
                modifier = Modifier.widthIn(min = 150.dp),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                "to land and wrap up",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryCard(plan: SessionPlan, now: Long, use24Hour: Boolean, onStart: () -> Unit) {
    val error = plan.validationError
    SectionCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp)) {
        if (error != null) {
            Eyebrow("Not quite yet", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            Text(error, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        } else {
            Eyebrow("Practice session", color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(8.dp))
            Text(
                "${TimeFormat.minutes(plan.totalMinutes)}, until ${TimeFormat.timeOfDay(plan.endMillis(now), use24Hour)}",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "${plan.rounds} ${if (plan.rounds == 1) "round" else "rounds"} of ${TimeFormat.minutes(plan.perRoundMinutes)}",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (plan.breakMinutes > 0) {
                Text(
                    "${TimeFormat.minutes(plan.breakMinutes)} break",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (plan.landingTime > 0) {
                Text(
                    "${TimeFormat.minutes(plan.landingTime)} to land and wrap up",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(22.dp))
            PrimaryAction(text = "Let's go", onClick = onStart)
        }
    }
}
