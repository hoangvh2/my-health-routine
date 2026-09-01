package com.vh.health.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vh.health.AppContainer
import com.vh.health.core.content.Exercise
import com.vh.health.core.content.MuscleGroup

@Composable
fun LibraryScreen(container: AppContainer) {
    val library = remember { container.content.library }
    var group by remember { mutableStateOf<MuscleGroup?>(null) }
    var kneeOnly by remember { mutableStateOf(false) }
    var expandedId by remember { mutableStateOf<String?>(null) }

    val shown = library.exercises
        .filter { group == null || it.group == group }
        .filter { !kneeOnly || it.kneeFocus }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Spacer(Modifier.height(16.dp)) }

        item {
            Text("Thư viện động tác", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "${library.exercises.size} động tác · ${library.kneeWork().size} bài dành riêng cho gối",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = group == null && !kneeOnly,
                    onClick = { group = null; kneeOnly = false },
                    label = { Text("Tất cả") },
                )
                FilterChip(
                    selected = kneeOnly,
                    onClick = { kneeOnly = !kneeOnly },
                    label = { Text("Cho gối") },
                )
                MuscleGroup.entries.forEach { candidate ->
                    FilterChip(
                        selected = group == candidate,
                        onClick = { group = if (group == candidate) null else candidate },
                        label = { Text(candidate.labelVi) },
                    )
                }
            }
        }

        items(shown, key = { it.id }) { exercise ->
            ExerciseCard(
                exercise = exercise,
                expanded = expandedId == exercise.id,
                onToggle = { expandedId = if (expandedId == exercise.id) null else exercise.id },
            )
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ExerciseCard(exercise: Exercise, expanded: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(exercise.nameVi, style = MaterialTheme.typography.titleMedium)
            Text(
                text = buildString {
                    append(exercise.group.labelVi)
                    append(" · ")
                    append(exercise.equipment.joinToString { it.labelVi })
                    if (exercise.kneeFocus) append(" · bài cho gối")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    exercise.tempo?.let { tempo ->
                        Labelled("Nhịp", listOf(tempo))
                    }
                    Labelled("Kỹ thuật", exercise.cues)
                    Labelled("Lỗi thường gặp", exercise.mistakes)
                    exercise.easier?.let { Labelled("Dễ hơn", listOf(it)) }
                    exercise.harder?.let { Labelled("Khó hơn", listOf(it)) }
                    Text(
                        text = "Hoạt hình minh hoạ và video: mốc M3.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun Labelled(label: String, lines: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        lines.forEach { line ->
            Text("• $line", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
