package com.hakomi.practicetimer.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Small letter-spaced heading used above each group of controls. */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier,
        textAlign = textAlign,
    )
}

/** A group of controls under a heading, held together by spacing rather than a card. */
@Composable
fun Section(
    modifier: Modifier = Modifier,
    title: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null || trailing != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.weight(1f))
                trailing?.invoke(this)
            }
            Spacer(Modifier.height(18.dp))
        }
        content()
    }
}

/** Hairline rule with room around it, used between sections. */
@Composable
fun SectionSeparator(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(18.dp))
    }
}

/** Selectable pill; filled when chosen, outlined otherwise. */
@Composable
fun ChoicePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minHeight: Int = 46,
    textStyle: TextStyle = MaterialTheme.typography.titleSmall,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
) {
    val colors = MaterialTheme.colorScheme
    val selectedFill = colors.primaryContainer
    val background by animateColorAsState(
        if (selected) selectedFill else selectedFill.copy(alpha = 0f),
        label = "pillBackground",
    )
    val content by animateColorAsState(
        when {
            !enabled -> colors.onSurfaceVariant.copy(alpha = 0.5f)
            selected -> colors.onPrimaryContainer
            else -> colors.onSurface
        },
        label = "pillContent",
    )
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, if (selected) selectedFill else colors.outline, shape)
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = textStyle,
            color = content,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** Two or three options in one rounded track. */
@Composable
fun <T> SegmentedToggle(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.surfaceVariant)
            .padding(4.dp),
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            val background by animateColorAsState(if (isSelected) colors.surface else Color.Transparent, label = "segment")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(background)
                    .clickable(role = Role.Tab) { onSelect(value) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) colors.primary else colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** How many times a wrapping wheel repeats its values; enough that no one scrolls off the end. */
private const val WheelLoops = 401

/**
 * A column of numbers that scrolls under the finger and snaps to the middle, its neighbours fading
 * out above and below. Tapping the middle number opens the keypad to type a value instead.
 *
 * [values] must be ascending. With [wrap] the list runs on past both ends, so scrolling off the top
 * of the range comes back at the bottom. The wheel only commits a value once it has come to rest,
 * so that clamping in the model cannot fight the finger mid-scroll.
 */
@Composable
fun NumberWheel(
    value: Int,
    values: List<Int>,
    onValueSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    unit: String? = null,
    format: (Int) -> String = { it.toString() },
    wrap: Boolean = false,
    onValueTyped: (Int) -> Unit = onValueSelected,
    itemHeight: Dp = 76.dp,
    visibleItems: Int = 3,
) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val currentValue by rememberUpdatedState(value)
    val commitValue by rememberUpdatedState(onValueSelected)
    val commitTyped by rememberUpdatedState(onValueTyped)
    val loops = if (wrap) WheelLoops else 1
    val itemCount = values.size * loops
    val startIndex = values.size * (loops / 2) + values.indexOf(value).coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)

    val centeredIndex by remember {
        derivedStateOf {
            val layout = state.layoutInfo
            val middle = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
            layout.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2f - middle) }?.index ?: startIndex
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) {
                values.getOrNull(centeredIndex % values.size)?.let { if (it != currentValue) commitValue(it) }
            }
        }
    }
    // A wheel that turns should be felt, one tick per number passing the middle.
    LaunchedEffect(state) {
        snapshotFlow { centeredIndex }.drop(1).collect {
            if (state.isScrollInProgress) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }
    LaunchedEffect(value) {
        val place = values.indexOf(value)
        val settled = centeredIndex
        if (place < 0 || state.isScrollInProgress || settled % values.size == place) return@LaunchedEffect
        val nearest = (-1..1)
            .map { settled - settled % values.size + place + it * values.size }
            .filter { it in 0 until itemCount }
            .minBy { abs(it - settled) }
        state.scrollToItem(nearest)
    }

    var typing by remember { mutableStateOf(false) }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.height(itemHeight * visibleItems),
            contentAlignment = Alignment.Center,
        ) {
            if (!typing) {
                LazyColumn(
                    state = state,
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
                    contentPadding = PaddingValues(vertical = itemHeight * ((visibleItems - 1) / 2)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(itemCount) { index ->
                        Box(
                            modifier = Modifier
                                .height(itemHeight)
                                .defaultMinSize(minWidth = 96.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    if (index == centeredIndex) typing = true
                                    else scope.launch { state.animateScrollToItem(index) }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = format(values[index % values.size]),
                                style = MaterialTheme.typography.displayMedium,
                                color = colors.onSurface,
                                maxLines = 1,
                                modifier = Modifier.graphicsLayer {
                                    val layout = state.layoutInfo
                                    val middle = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
                                    val info = layout.visibleItemsInfo.firstOrNull { it.index == index }
                                    val steps = info?.let { abs(it.offset + it.size / 2f - middle) / it.size } ?: 2f
                                    alpha = (1f - 0.6f * steps).coerceIn(0.1f, 1f)
                                    val shrink = (1f - 0.2f * steps).coerceIn(0.62f, 1f)
                                    scaleX = shrink
                                    scaleY = shrink
                                },
                            )
                        }
                    }
                }
            } else {
                TypedNumberField(
                    placeholder = format(value),
                    onCommit = { typed ->
                        typing = false
                        typed?.coerceIn(values.first(), values.last())?.let(commitTyped)
                    },
                    modifier = Modifier
                        .height(itemHeight)
                        .defaultMinSize(minWidth = 96.dp)
                        .background(colors.background),
                )
            }
        }
        if (unit != null) {
            Text(
                text = " $unit",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/** The keypad entry that takes the wheel's place while a value is being typed. */
@Composable
private fun TypedNumberField(
    placeholder: String,
    onCommit: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displayMedium,
    maxDigits: Int = 2,
) {
    val colors = MaterialTheme.colorScheme
    var text by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    var committed by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val commit = {
        if (!committed) {
            committed = true
            keyboard?.hide()
            onCommit(text.toIntOrNull())
        }
    }

    BasicTextField(
        value = text,
        onValueChange = { entry -> text = entry.filter(Char::isDigit).take(maxDigits) },
        singleLine = true,
        textStyle = style.copy(color = colors.primary, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { commit() }),
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) focused = true else if (focused) commit()
            },
        decorationBox = { field ->
            Box(contentAlignment = Alignment.Center) {
                if (text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = style,
                        color = colors.onSurfaceVariant.copy(alpha = 0.4f),
                        maxLines = 1,
                    )
                }
                field()
            }
        },
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

/**
 * A duration shown as "X hr Y min". Tapping the hour or minute opens the number keypad so the
 * value can be typed instead of scrolled.
 */
@Composable
fun EditableDuration(
    totalMinutes: Int,
    onTotalMinutesChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minMinutes: Int = 1,
    maxMinutes: Int = 12 * 60,
) {
    val colors = MaterialTheme.colorScheme
    val clamped = totalMinutes.coerceIn(minMinutes, maxMinutes)
    val hours = clamped / 60
    val minutes = clamped % 60
    var editing by remember { mutableStateOf<DurationPart?>(null) }

    fun commit(part: DurationPart, typed: Int?) {
        editing = null
        if (typed == null) return
        val next = when (part) {
            DurationPart.HOURS -> typed.coerceIn(0, maxMinutes / 60) * 60 + minutes
            DurationPart.MINUTES -> hours * 60 + typed.coerceIn(0, 59)
        }.coerceIn(minMinutes, maxMinutes)
        if (next != clamped) onTotalMinutesChanged(next)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.Center,
    ) {
        DurationPartField(
            value = hours,
            unit = "hr",
            editing = editing == DurationPart.HOURS,
            onStartEdit = { editing = DurationPart.HOURS },
            onCommit = { commit(DurationPart.HOURS, it) },
            color = colors.primary,
        )
        Spacer(Modifier.width(18.dp))
        DurationPartField(
            value = minutes,
            unit = "min",
            editing = editing == DurationPart.MINUTES,
            onStartEdit = { editing = DurationPart.MINUTES },
            onCommit = { commit(DurationPart.MINUTES, it) },
            color = colors.primary,
            format = { it.toString().padStart(2, '0') },
        )
    }
}

@Composable
fun EditableMinutes(
    minutes: Int,
    onMinutesChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minMinutes: Int = 1,
    maxMinutes: Int = 12 * 60,
) {
    val colors = MaterialTheme.colorScheme
    val clamped = minutes.coerceIn(minMinutes, maxMinutes)
    var editing by remember { mutableStateOf(false) }

    DurationPartField(
        value = clamped,
        unit = "min",
        editing = editing,
        onStartEdit = { editing = true },
        onCommit = { typed ->
            editing = false
            if (typed == null) return@DurationPartField
            val next = typed.coerceIn(minMinutes, maxMinutes)
            if (next != clamped) onMinutesChanged(next)
        },
        color = colors.primary,
        modifier = modifier,
        maxDigits = 3,
    )
}

private enum class DurationPart { HOURS, MINUTES }

@Composable
private fun DurationPartField(
    value: Int,
    unit: String,
    editing: Boolean,
    onStartEdit: () -> Unit,
    onCommit: (Int?) -> Unit,
    color: Color,
    modifier: Modifier = Modifier,
    maxDigits: Int = 2,
    format: (Int) -> String = { it.toString() },
) {
    val style = MaterialTheme.typography.displayMedium
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Box(
            modifier = Modifier.defaultMinSize(minWidth = 72.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (editing) {
                TypedNumberField(
                    placeholder = format(value),
                    onCommit = onCommit,
                    style = style,
                    maxDigits = maxDigits,
                )
            } else {
                Text(
                    text = format(value),
                    style = style,
                    color = color,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onStartEdit,
                        ),
                )
            }
        }
        Text(
            text = " $unit",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 10.dp),
        )
    }
}

/** Round chip for rounds and the break. */
@Composable
fun RoundBubble(
    label: String,
    selected: Boolean,
    completed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 76,
    sublabel: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        when {
            completed -> colors.surfaceVariant
            selected -> colors.primary
            else -> Color.Transparent
        },
        label = "bubbleBackground",
    )
    val content = when {
        completed -> colors.onSurfaceVariant
        selected -> colors.onPrimary
        else -> colors.onSurface
    }
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, if (selected || completed) Color.Transparent else colors.outline, CircleShape)
            .clickable(enabled = !completed, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (completed) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = "Completed",
                    tint = colors.secondary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                color = content,
                textAlign = TextAlign.Center,
            )
            if (sublabel != null && !completed) {
                Text(
                    text = sublabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = content.copy(alpha = 0.85f),
                )
            }
        }
    }
}

/** Full-width primary action. */
@Composable
fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        elevation = null,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

/** Quiet outlined action, sized to sit beside another. */
@Composable
fun SecondaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.6f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "Not yet",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel, style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}
