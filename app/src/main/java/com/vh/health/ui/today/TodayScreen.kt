package com.vh.health.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vh.health.AppContainer
import com.vh.health.core.schedule.BlockKind
import com.vh.health.core.schedule.ScheduledBlock
import com.vh.health.ui.hhmm
import com.vh.health.ui.minutesAsText
import com.vh.health.ui.theme.ClockStyle

@Composable
fun TodayScreen(container: AppContainer) {
    val viewModel: TodayViewModel = viewModel(factory = TodayViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Spacer(Modifier.height(16.dp)) }

        item {
            Text(
                text = state.weekday.labelVi.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = state.workout?.titleVi ?: "Chưa có buổi tập cho hôm nay",
                style = MaterialTheme.typography.headlineMedium,
            )
            state.workout?.let { workout ->
                Text(
                    text = "${workout.focusVi} · ${workout.minutes} phút · RPE ${workout.rpe}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item { AnchorControls(state, viewModel) }

        item {
            SectionHeading(
                title = "Buổi sáng",
                trailing = state.morning.start?.let { start ->
                    "${start.hhmm()} – ${state.morning.end?.hhmm().orEmpty()}"
                }.orEmpty(),
            )
        }

        items(state.morning.blocks, key = { it.block.id }) { scheduled ->
            BlockRow(scheduled)
        }

        if (state.morning.dropped.isNotEmpty()) {
            item {
                Text(
                    text = "Đã bỏ để vừa khung giờ: " +
                        state.morning.dropped.joinToString { it.title.lowercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        if (state.morning.overflowMinutes > 0) {
            item {
                Text(
                    text = "Khung giờ hẹp hơn cả phần bắt buộc ${state.morning.overflowMinutes} phút. " +
                        "Khởi động và giãn cơ vẫn được giữ vì đó là phần bảo vệ gối.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        item {
            SectionHeading(
                title = "Buổi tối",
                trailing = state.evening.start?.let { start ->
                    "${start.hhmm()} – ${state.evening.end?.hhmm().orEmpty()}"
                }.orEmpty(),
            )
        }

        items(state.evening.blocks, key = { "evening_" + it.block.id }) { scheduled ->
            BlockRow(scheduled)
        }

        item {
            Text(
                text = "Ngủ ${minutesAsText(state.sleepMinutes)} " +
                    "(${state.settings.bedtime.hhmm()} → ${state.settings.wakeTime.hhmm()})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun AnchorControls(state: TodayUiState, viewModel: TodayViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Bắt đầu lúc",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(text = state.settings.effectiveStart.hhmm(), style = ClockStyle)
            }

            Text(
                text = "Đổi giờ này là cả buổi sáng dịch theo. Không mốc giờ nào được ghi cứng.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.nudgeStart(-15) }) { Text("−15′") }
                OutlinedButton(onClick = { viewModel.nudgeStart(15) }) { Text("+15′") }
                if (state.settings.todayStartOverride != null || state.settings.todayWindowMinutes != null) {
                    OutlinedButton(onClick = { viewModel.resetToday() }) { Text("Về mặc định") }
                }
            }

            Text(
                text = "Sáng nay có bao nhiêu phút?",
                style = MaterialTheme.typography.titleMedium,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null, 60, 40, 25).forEach { window ->
                    FilterChip(
                        selected = state.settings.todayWindowMinutes == window,
                        onClick = { viewModel.setWindow(window) },
                        label = { Text(window?.let { "$it′" } ?: "Đủ giờ") },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, trailing: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        if (trailing.isNotBlank()) {
            Text(text = trailing, style = ClockStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BlockRow(scheduled: ScheduledBlock) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = scheduled.start.hhmm(),
            style = ClockStyle,
            modifier = Modifier.width(52.dp),
        )
        Box(
            modifier = Modifier
                .padding(top = 6.dp, end = 12.dp)
                .size(10.dp)
                .background(colorFor(scheduled.block.kind), CircleShape),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = scheduled.block.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (scheduled.block.kind == BlockKind.MAIN) FontWeight.SemiBold else FontWeight.Normal,
            )
            Text(
                text = buildString {
                    append("${scheduled.minutes} phút")
                    if (scheduled.isShortened) append(" · đã rút gọn từ ${scheduled.block.minutes}")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            scheduled.block.note?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun colorFor(kind: BlockKind): Color = when (kind) {
    BlockKind.MAIN -> MaterialTheme.colorScheme.tertiary
    BlockKind.WARM_UP, BlockKind.KNEE -> MaterialTheme.colorScheme.primary
    BlockKind.COOL_DOWN, BlockKind.WIND_DOWN -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
