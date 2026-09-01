package com.vh.health.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vh.health.AppContainer
import com.vh.health.core.schedule.BlockKind
import com.vh.health.core.schedule.BlockState
import com.vh.health.core.schedule.DayPhase
import com.vh.health.core.schedule.ScheduledBlock
import com.vh.health.core.schedule.Timeline
import com.vh.health.core.schedule.TimelinePosition
import com.vh.health.ui.hhmm
import com.vh.health.ui.minutesAsText
import com.vh.health.ui.theme.ClockStyle
import com.vh.health.ui.theme.MetricStyle
import com.vh.health.ui.theme.MicroLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM")

@Composable
fun TodayScreen(container: AppContainer, onStartWorkout: (String) -> Unit) {
    val viewModel: TodayViewModel = viewModel(factory = TodayViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()
    var expandedId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        item { DayHeader(state) }

        item {
            SessionCard(
                state = state,
                onStart = { state.workout?.let { workout -> onStartWorkout(workout.id) } },
            )
        }

        item {
            SectionBar(
                label = "Buổi sáng",
                range = state.morning.timeRange(),
                status = state.morning.statusText(state.morningPosition),
            )
        }

        item { DayBar(state.morning, state.morningStates, state.morningPosition) }

        items(state.morning.blocks, key = { it.block.id }) { scheduled ->
            TimelineRow(
                scheduled = scheduled,
                state = state.morningStates[scheduled.block.id] ?: BlockState.UPCOMING,
                expanded = expandedId == scheduled.block.id,
                onClick = { expandedId = if (expandedId == scheduled.block.id) null else scheduled.block.id },
            )
        }

        if (state.morning.dropped.isNotEmpty()) {
            item { Notice("Đã bỏ: " + state.morning.dropped.joinToString { it.title.lowercase() }) }
        }
        if (state.morning.overflowMinutes > 0) {
            item {
                Notice(
                    "Hẹp hơn phần bắt buộc ${state.morning.overflowMinutes} phút — khởi động và giãn cơ vẫn được giữ.",
                    error = true,
                )
            }
        }

        item { AnchorCard(state, viewModel) }

        item {
            SectionBar(
                label = "Buổi tối",
                range = state.evening.timeRange(),
                status = "Ngủ ${minutesAsText(state.sleepMinutes)}",
            )
        }

        items(state.evening.blocks, key = { "evening_" + it.block.id }) { scheduled ->
            TimelineRow(
                scheduled = scheduled,
                state = state.eveningStates[scheduled.block.id] ?: BlockState.UPCOMING,
                expanded = expandedId == "evening_" + scheduled.block.id,
                onClick = {
                    val key = "evening_" + scheduled.block.id
                    expandedId = if (expandedId == key) null else key
                },
            )
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

/* ---------------------------------------------------------------- header */

@Composable
private fun DayHeader(state: TodayUiState) {
    val today = remember { LocalDate.now().format(DayMonth) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "${state.weekday.labelVi} · $today".uppercase(),
            style = MicroLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(6.dp),
        ) {
            Text(
                text = "TUẦN ${state.week} · ${state.phase.labelVi}".uppercase(),
                style = MicroLabel,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun SessionCard(state: TodayUiState, onStart: () -> Unit) {
    val workout = state.workout
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = workout?.titleVi ?: "Chưa có buổi tập",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = workout?.focusVi.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("${workout?.minutes ?: 0}′", "Thời lượng", Modifier.weight(1f))
                StatTile(workout?.rpe ?: "—", "Cường độ", Modifier.weight(1f))
                StatTile("${workout?.blocks?.size ?: 0}", "Khối bài", Modifier.weight(1f))
            }
            Button(
                onClick = onStart,
                enabled = workout != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Bắt đầu", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(value, style = MetricStyle, maxLines = 1)
            Text(
                text = label.uppercase(),
                style = MicroLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/* ------------------------------------------------------------- timeline */

@Composable
private fun SectionBar(label: String, range: String, status: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 10.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MicroLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(10.dp))
        Text(text = range, style = ClockStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(
            text = status,
            style = MicroLabel,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** The whole morning as one proportional bar, with a marker at the current minute. */
@Composable
private fun DayBar(timeline: Timeline, states: Map<String, BlockState>, position: TimelinePosition) {
    if (timeline.blocks.isEmpty()) return
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            timeline.blocks.forEach { scheduled ->
                val past = states[scheduled.block.id] == BlockState.PAST
                Box(
                    modifier = Modifier
                        .weight(scheduled.minutes.toFloat())
                        .fillMaxHeight()
                        .background(kindColor(scheduled.block.kind).copy(alpha = if (past) 0.28f else 1f)),
                )
            }
        }
        if (position.phase == DayPhase.DURING && position.progress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(position.progress)
                    .height(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.onSurface),
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(
    scheduled: ScheduledBlock,
    state: BlockState,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val current = state == BlockState.CURRENT
    val dim = if (state == BlockState.PAST) 0.45f else 1f
    val accent = kindColor(scheduled.block.kind)

    Surface(
        color = if (current) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = scheduled.block.note != null, onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = scheduled.start.hhmm(),
                    style = ClockStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = dim),
                    modifier = Modifier.width(44.dp),
                )
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(if (current) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = dim)),
                )
                Text(
                    text = scheduled.block.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = dim),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (scheduled.block.note != null) {
                    Text(
                        text = if (expanded) "▴" else "▾",
                        style = MicroLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
                Text(
                    text = "${scheduled.minutes}′",
                    style = ClockStyle,
                    color = if (scheduled.isShortened) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = dim)
                    },
                )
            }
            AnimatedVisibility(visible = expanded) {
                Text(
                    text = scheduled.block.note.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 61.dp, top = 4.dp, end = 8.dp, bottom = 4.dp),
                )
            }
        }
    }
}

/* --------------------------------------------------------------- anchor */

@Composable
private fun AnchorCard(state: TodayUiState, viewModel: TodayViewModel) {
    val overridden = state.settings.todayStartOverride != null || state.settings.todayWindowMinutes != null
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Bắt đầu lúc".uppercase(),
                    style = MicroLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                NudgeButton("−15′") { viewModel.nudgeStart(-15) }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = state.settings.effectiveStart.hhmm(),
                    style = MetricStyle,
                )
                Spacer(Modifier.width(8.dp))
                NudgeButton("+15′") { viewModel.nudgeStart(15) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf<Int?>(null, 60, 40, 25).forEach { window ->
                    FilterChip(
                        selected = state.settings.todayWindowMinutes == window,
                        onClick = { viewModel.setWindow(window) },
                        label = {
                            Text(
                                text = window?.let { "$it′" } ?: "Đủ giờ",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                    )
                }
                if (overridden) {
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.resetToday() },
                        label = { Text("Đặt lại", style = MaterialTheme.typography.bodyMedium) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NudgeButton(label: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun Notice(text: String, error: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.padding(horizontal = 6.dp),
    )
}

/* ---------------------------------------------------------------- utils */

@Composable
private fun kindColor(kind: BlockKind): Color = when (kind) {
    BlockKind.MAIN -> MaterialTheme.colorScheme.tertiary
    BlockKind.WARM_UP, BlockKind.KNEE -> MaterialTheme.colorScheme.primary
    BlockKind.COOL_DOWN, BlockKind.WIND_DOWN -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.outline
}

private fun Timeline.timeRange(): String {
    val from = start ?: return ""
    val to = end ?: return ""
    return "${from.hhmm()} – ${to.hhmm()}"
}

private fun Timeline.statusText(position: TimelinePosition): String = when (position.phase) {
    DayPhase.BEFORE -> if (position.minutesUntilStart == 0) {
        "${totalMinutes} phút"
    } else {
        "Sau ${minutesAsText(position.minutesUntilStart)}"
    }
    DayPhase.DURING -> "Còn ${minutesAsText(position.minutesRemaining)}"
    DayPhase.AFTER -> "Đã xong"
}
