package com.vh.health.ui.progress

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vh.health.AppContainer
import com.vh.health.core.program.KneeLoadPolicy
import com.vh.health.core.program.KneeSignal
import com.vh.health.core.progress.BodyMetric
import com.vh.health.ui.ddmm
import java.time.LocalDate

@Composable
fun ProgressScreen(container: AppContainer) {
    val viewModel: ProgressViewModel = viewModel(factory = ProgressViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Spacer(Modifier.height(16.dp)) }
        item { Text("Tiến trình", style = MaterialTheme.typography.headlineMedium) }

        item { StreakCard(state.streak) }
        item { KneeStatusCard(state.latestKneeSignal, state.suggestClinician) }
        item { BodyMetricCard(state.bodyMetrics, onSave = viewModel::saveBodyMetric) }

        item {
            Text(
                "Buổi tập gần đây",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (state.recentSessions.isEmpty()) {
            item {
                InfoCard(
                    if (state.isLoading) "Đang tải…" else "Chưa có buổi tập nào được ghi nhận. Hoàn thành một buổi ở tab Hôm nay để bắt đầu.",
                )
            }
        } else {
            items(state.recentSessions) { session -> SessionRow(session) }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun StreakCard(streak: Int) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Chuỗi ngày tập",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = if (streak == 0) "Bắt đầu hôm nay" else "Đang giữ chuỗi — đừng để đứt",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                text = "$streak",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun KneeStatusCard(signal: KneeSignal?, suggestClinician: Boolean) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tín hiệu gối", style = MaterialTheme.typography.titleMedium)
            if (signal == null) {
                Text(
                    "Chưa có dữ liệu. Trả lời câu hỏi sau buổi đi bộ/chạy để bắt đầu theo dõi.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(signal.labelVi, style = MaterialTheme.typography.bodyLarge)
                Text(
                    KneeLoadPolicy.decide(signal).explanationVi,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (suggestClinician) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        "Gối nhức nặng hai lần liên tiếp gần đây. Nếu tình trạng này tiếp tục, nên hẹn khám.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BodyMetricCard(metrics: List<BodyMetric>, onSave: (Double?, Double?) -> Unit) {
    var weightInput by remember { mutableStateOf("") }
    var waistInput by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Cân nặng & vòng eo", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = waistInput,
                    onValueChange = { waistInput = it },
                    label = { Text("vòng eo, cm") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Button(
                onClick = {
                    onSave(weightInput.toDoubleOrNull(), waistInput.toDoubleOrNull())
                    weightInput = ""
                    waistInput = ""
                },
                modifier = Modifier.align(Alignment.End),
            ) { Text("Lưu") }

            if (metrics.isNotEmpty()) {
                HorizontalDivider()
                metrics.take(5).forEach { metric -> BodyMetricRow(metric) }
            }
        }
    }
}

@Composable
private fun BodyMetricRow(metric: BodyMetric) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = LocalDate.ofEpochDay(metric.epochDay).ddmm(),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = listOfNotNull(metric.weightKg?.let { "${it.trimmed()} kg" }, metric.waistCm?.let { "${it.trimmed()} cm" })
                .joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SessionRow(session: SessionSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(session.workoutTitleVi, style = MaterialTheme.typography.bodyLarge)
        Text(
            session.date.ddmm(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InfoCard(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}

/** "78.0" reads like a rounding artefact to a person who typed "78" — drop the ".0". */
private fun Double.trimmed(): String = if (this == this.toLong().toDouble()) "${this.toLong()}" else toString()
