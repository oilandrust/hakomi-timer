package com.hakomi.practicetimer.ui.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hakomi.practicetimer.domain.SessionPlan
import com.hakomi.practicetimer.domain.TimeFormat
import com.hakomi.practicetimer.domain.TimingMode
import com.hakomi.practicetimer.ui.components.ChoicePill
import com.hakomi.practicetimer.ui.components.Eyebrow
import com.hakomi.practicetimer.ui.components.NumberWheel
import com.hakomi.practicetimer.ui.components.PrimaryAction
import com.hakomi.practicetimer.ui.components.SegmentedToggle

private val durationHours = (0..PlanViewModel.MAX_DURATION / 60).toList()
private val minutesOfHour = (0..59).toList()
private val hoursOfDay = (0..23).toList()
private val twoDigits: (Int) -> String = { it.toString().padStart(2, '0') }
/** 0-23 shown the way a 12 hour clock face does, so passing noon or midnight flips AM and PM. */
private val onTheClock: (Int) -> String = { hour -> (hour % 12).let { if (it == 0) 12 else it }.toString() }

@Composable
fun PlanScreen(
    viewModel: PlanViewModel,
    onSessionStarted: () -> Unit,
) {
    val now by viewModel.now.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val use24Hour = remember(context) { android.text.format.DateFormat.is24HourFormat(context) }
    val plan = viewModel.plan(now)
    // Deliberately not the IME inset: the keypad may cover the footer, but the wheels must not move.
    val insets = WindowInsets.systemBars.union(WindowInsets.displayCutout).asPaddingValues()
    var showRoundsPicker by remember { mutableStateOf(false) }
    var showBreakPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(insets)
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        PlanModeHeader(viewModel)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                TimePickerSection(viewModel, now, use24Hour)
            }
            RoundsAndBreakSummaries(
                plan = plan,
                onRoundsClick = { showRoundsPicker = true },
                onBreakClick = { showBreakPicker = true },
            )
            Spacer(Modifier.weight(0.7f))
        }
        PlanFooter(
            plan = plan,
            now = now,
            use24Hour = use24Hour,
            onStart = { if (viewModel.launchSession()) onSessionStarted() },
        )
        Spacer(Modifier.height(20.dp))
    }

    if (showRoundsPicker) {
        OptionPickerDialog(
            title = "Rounds",
            onDismiss = { showRoundsPicker = false },
        ) {
            SessionPlan.roundPresets.forEach { count ->
                SmallPreset(
                    text = count.toString(),
                    selected = viewModel.rounds == count,
                    onClick = {
                        viewModel.chooseRounds(count)
                        showRoundsPicker = false
                    },
                )
            }
        }
    }

    if (showBreakPicker) {
        OptionPickerDialog(
            title = "Break",
            onDismiss = { showBreakPicker = false },
        ) {
            SessionPlan.breakPresets.forEach { minutes ->
                SmallPreset(
                    text = if (minutes == 0) "None" else "$minutes",
                    selected = viewModel.breakMinutes == minutes,
                    onClick = {
                        viewModel.chooseBreak(minutes)
                        showBreakPicker = false
                    },
                )
            }
        }
    }
}

@Composable
private fun PlanModeHeader(viewModel: PlanViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Plan Session",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.weight(1f))
        SegmentedToggle(
            options = listOf(TimingMode.DURING to "During", TimingMode.UNTIL to "Until"),
            selected = viewModel.timingMode,
            onSelect = viewModel::chooseTimingMode,
            modifier = Modifier.width(168.dp),
        )
    }
}

@Composable
private fun TimePickerSection(viewModel: PlanViewModel, now: Long, use24Hour: Boolean) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        when (viewModel.timingMode) {
            TimingMode.DURING -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NumberWheel(
                        value = viewModel.durationMinutes / 60,
                        values = durationHours,
                        onValueSelected = viewModel::chooseDurationHours,
                        unit = "hr",
                    )
                    NumberWheel(
                        value = viewModel.durationMinutes % 60,
                        values = minutesOfHour,
                        onValueSelected = viewModel::chooseDurationMinutes,
                        unit = "min",
                        format = twoDigits,
                        wrap = true,
                    )
                }
                Spacer(Modifier.height(14.dp))
                PresetRow {
                    SessionPlan.durationPresets.forEach { minutes ->
                        SmallPreset(
                            text = if (minutes == 120) "2 hours" else "$minutes min",
                            selected = viewModel.durationMinutes == minutes,
                            onClick = { viewModel.chooseDuration(minutes) },
                        )
                    }
                }
            }

            TimingMode.UNTIL -> {
                val endTime = viewModel.targetEndLocalTime()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NumberWheel(
                        value = endTime?.hour ?: 0,
                        values = hoursOfDay,
                        onValueSelected = viewModel::chooseEndHour,
                        format = if (use24Hour) twoDigits else onTheClock,
                        wrap = true,
                        onValueTyped = { viewModel.chooseEndHourOnClock(it, use24Hour) },
                    )
                    Text(
                        ":",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    NumberWheel(
                        value = endTime?.minute ?: 0,
                        values = minutesOfHour,
                        onValueSelected = viewModel::chooseEndMinute,
                        format = twoDigits,
                        wrap = true,
                    )
                    if (!use24Hour) {
                        Spacer(Modifier.width(12.dp))
                        MeridiemSelector(
                            isPm = (endTime?.hour ?: 0) >= 12,
                            onSelect = { viewModel.setMeridiem(it) },
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                PresetRow {
                    viewModel.endPresets(now).forEach { option ->
                        SmallPreset(
                            text = TimeFormat.timeOfDay(option.endMillis, use24Hour),
                            selected = viewModel.targetEndMillis == option.endMillis,
                            onClick = { viewModel.selectEndTime(option.endMillis) },
                        )
                    }
                }
            }
        }
    }
}

/** The quick suggestions that sit under the big value, sized to their text. */
@Composable
private fun PresetRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun SmallPreset(text: String, selected: Boolean, onClick: () -> Unit) {
    ChoicePill(
        text = text,
        selected = selected,
        onClick = onClick,
        minHeight = 32,
        textStyle = MaterialTheme.typography.labelMedium,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** AM and PM stacked beside the end time, tapped to switch. */
@Composable
private fun MeridiemSelector(isPm: Boolean, onSelect: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        listOf(false to "AM", true to "PM").forEach { (pm, label) ->
            val active = pm == isPm
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = if (active) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(role = Role.RadioButton) { onSelect(pm) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun RoundsAndBreakSummaries(
    plan: SessionPlan,
    onRoundsClick: () -> Unit,
    onBreakClick: () -> Unit,
) {
    val roundLabel = if (plan.rounds == 1) "1 Round" else "${plan.rounds} Rounds"
    val roundsSummary = "$roundLabel of ${TimeFormat.minutes(plan.perRoundMinutes)}"
    val breakSummary = if (plan.breakMinutes == 0) {
        "Add break"
    } else {
        "${TimeFormat.minutes(plan.breakMinutes)} break"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SummaryRow(
            text = roundsSummary,
            icon = Icons.Outlined.Autorenew,
            onClick = onRoundsClick,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SummaryRow(
            text = breakSummary,
            icon = Icons.Outlined.Spa,
            onClick = onBreakClick,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun SummaryRow(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun OptionPickerDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

@Composable
private fun PlanFooter(plan: SessionPlan, now: Long, use24Hour: Boolean, onStart: () -> Unit) {
    val error = plan.validationError
    Column(modifier = Modifier.fillMaxWidth()) {
        if (error != null) {
            Eyebrow("Not quite yet", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            Text(error, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(16.dp))
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlanStat(
                    icon = {
                        Icon(
                            Icons.Outlined.HourglassEmpty,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    text = "${TimeFormat.minutes(plan.totalMinutes)} total",
                )
                PlanStat(
                    icon = {
                        Icon(
                            Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    text = "until ${TimeFormat.timeOfDay(plan.endMillis(now), use24Hour)}",
                )
            }
            Spacer(Modifier.height(18.dp))
        }
        PrimaryAction(text = "Let's go", onClick = onStart, enabled = error == null)
    }
}

@Composable
private fun PlanStat(icon: @Composable () -> Unit, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        icon()
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
