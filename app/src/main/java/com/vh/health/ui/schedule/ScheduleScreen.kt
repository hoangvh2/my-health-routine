package com.vh.health.ui.schedule

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vh.health.AppContainer
import com.vh.health.core.content.Weekday
import com.vh.health.core.content.Workout
import com.vh.health.core.program.Phase
import com.vh.health.core.program.Progression
import com.vh.health.data.AppSettings
import com.vh.health.ui.theme.ClockStyle
import com.vh.health.ui.theme.MicroLabel
import java.time.LocalDate

@Composable
fun ScheduleScreen(container: AppContainer) {
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val program = remember { container.content.program }
    val todayEpochDay = remember { LocalDate.now().toEpochDay() }
    val todayWeekday = remember { Weekday.entries[LocalDate.now().dayOfWeek.ordinal] }
    val week = settings.programStartEpochDay?.let { Progression.weekNumber(it, todayEpochDay) } ?: 1
    val phase = Progression.phaseOf(week)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        item {
            Text("Lịch tuần", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Tuần $week · khối ${Progression.blockOf(week)}, ngày ${Progression.weekWithinBlock(week)}/4",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item { BlockStrip(currentPhase = phase) }

        items(program.week, key = { it.day.name }) { day ->
            DayRow(
                weekday = day.day,
                workout = program.workout(day.workoutId),
                isToday = day.day == todayWeekday,
            )
        }

        item { Spacer(Modifier.height(4.dp)) }
        item {
            Text(
                text = "Kéo thả đổi ngày và đánh dấu ngày nghỉ: mốc M5 tiếp theo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun BlockStrip(currentPhase: Phase) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Phase.entries.forEach { phase ->
                val current = phase == currentPhase
                Surface(
                    color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = phase.labelVi,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (current) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayRow(weekday: Weekday, workout: Workout?, isToday: Boolean) {
    Surface(
        color = if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.width(56.dp)) {
                Text(
                    text = weekday.labelVi.uppercase(),
                    style = MicroLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isToday) {
                    Text("Hôm nay", style = MicroLabel, color = MaterialTheme.colorScheme.primary)
                }
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(rpeAccent(workout?.rpe)),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout?.titleVi ?: "Nghỉ",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
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
            if (workout != null) {
                Text(
                    text = "${workout.minutes}′",
                    style = ClockStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Colour by RPE tier so a heavy day reads red-ish and an easy day reads calm at a glance. */
@Composable
private fun rpeAccent(rpe: String?): Color {
    val lead = rpe?.takeWhile { it.isDigit() }?.toIntOrNull() ?: return MaterialTheme.colorScheme.outline
    return when {
        lead >= 8 -> MaterialTheme.colorScheme.tertiary
        lead >= 6 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }
}
