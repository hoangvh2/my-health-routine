package com.vh.health.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vh.health.AppContainer
import com.vh.health.core.schedule.SleepLink
import com.vh.health.data.AppSettings
import com.vh.health.ui.hhmm
import com.vh.health.ui.minutesAsText
import com.vh.health.ui.theme.ClockStyle
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val settings by container.settings.settings
        .collectAsStateWithLifecycle(initialValue = AppSettings())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Spacer(Modifier.height(16.dp)) }
        item { Text("Cài đặt", style = MaterialTheme.typography.headlineMedium) }

        item {
            SettingCard(
                title = "Điểm neo buổi sáng",
                subtitle = "Giờ bắt đầu mặc định. Cả chuỗi buổi sáng tính ra từ đây.",
            ) {
                Stepper(
                    value = settings.wakeTime.hhmm(),
                    onDown = { scope.launch { container.settings.setWakeTime(settings.wakeTime.minusMinutes(15)) } },
                    onUp = { scope.launch { container.settings.setWakeTime(settings.wakeTime.plusMinutes(15)) } },
                )
            }
        }

        item {
            SettingCard(
                title = "Điểm neo buổi tối",
                subtitle = if (settings.bedtimeFollowsWake) {
                    "Đang tự tính từ giờ dậy trừ ${minutesAsText(settings.sleepTargetMinutes)}."
                } else {
                    "Bạn đang đặt tay. Hiện ngủ ${minutesAsText(SleepLink.sleepMinutes(settings.bedtime, settings.wakeTime))}."
                },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Stepper(
                        value = settings.bedtime.hhmm(),
                        onDown = { scope.launch { container.settings.setBedtime(settings.bedtime.minusMinutes(15)) } },
                        onUp = { scope.launch { container.settings.setBedtime(settings.bedtime.plusMinutes(15)) } },
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Tự đi theo giờ dậy",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = settings.bedtimeFollowsWake,
                            onCheckedChange = { follows ->
                                scope.launch { container.settings.setBedtimeFollowsWake(follows) }
                            },
                        )
                    }
                }
            }
        }

        item {
            SettingCard(
                title = "Độ dài buổi tập chính",
                subtitle = "Phần giữa của buổi sáng. Khởi động và giãn cơ không đổi.",
            ) {
                Stepper(
                    value = "${settings.mainSessionMinutes} phút",
                    onDown = { scope.launch { container.settings.setMainSessionMinutes(settings.mainSessionMinutes - 3) } },
                    onUp = { scope.launch { container.settings.setMainSessionMinutes(settings.mainSessionMinutes + 3) } },
                )
            }
        }

        item {
            SettingCard(
                title = "Còn thiếu",
                subtitle = "Nhắc nhở và báo thức (M5), âm thanh tabata và giọng đếm (M4), " +
                    "sao lưu JSON (M6), bản tiếng Anh (M7).",
                content = {},
            )
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SettingCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

@Composable
private fun Stepper(value: String, onDown: () -> Unit, onUp: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(onClick = onDown) { Text("−") }
        Text(text = value, style = ClockStyle, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onUp) { Text("+") }
    }
}
