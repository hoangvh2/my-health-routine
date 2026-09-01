package com.vh.health.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vh.health.AppContainer
import com.vh.health.core.schedule.SleepLink
import com.vh.health.data.AppSettings
import com.vh.health.notify.ReminderScheduler
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
                subtitle = "Cả chuỗi buổi sáng tính ra từ giờ này.",
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
                    "Tự tính từ giờ dậy trừ ${minutesAsText(settings.sleepTargetMinutes)}."
                } else {
                    "Đặt tay. Hiện ngủ ${minutesAsText(SleepLink.sleepMinutes(settings.bedtime, settings.wakeTime))}."
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
                subtitle = "Khởi động và giãn cơ không đổi theo.",
            ) {
                Stepper(
                    value = "${settings.mainSessionMinutes} phút",
                    onDown = { scope.launch { container.settings.setMainSessionMinutes(settings.mainSessionMinutes - 3) } },
                    onUp = { scope.launch { container.settings.setMainSessionMinutes(settings.mainSessionMinutes + 3) } },
                )
            }
        }

        item { RemindersCard(container, settings) }

        item {
            SettingCard(
                title = "Còn thiếu",
                subtitle = "Sao lưu JSON · M6\nBản tiếng Anh · M7",
                content = {},
            )
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

/**
 * Five daily notifications, never a ringing "báo thức" — see notify/ReminderScheduler.
 * Two permissions can gate this, handled separately because Android treats them very
 * differently: POST_NOTIFICATIONS is an in-app runtime prompt (API 33+, default-granted
 * below that); the exact-alarm permission has no in-app dialog at all, only a system
 * settings screen, and only matters on API 31+.
 */
@Composable
private fun RemindersCard(container: AppContainer, settings: AppSettings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var exactAlarmGranted by remember { mutableStateOf(ReminderScheduler.hasExactAlarmPermission(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        // The only way back from the exact-alarm settings screen is onResume — no
        // callback exists for it the way there is for the notification permission.
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                exactAlarmGranted = ReminderScheduler.hasExactAlarmPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        // A denial turns the switch back off rather than leaving it on with nothing
        // actually scheduled — an enabled switch that silently does nothing is worse
        // than an honest one.
        scope.launch { container.settings.setRemindersEnabled(granted) }
        if (granted) ReminderScheduler.scheduleAll(context, settings.wakeTime, settings.bedtime)
    }

    fun setEnabled(enabled: Boolean) {
        if (!enabled) {
            scope.launch { container.settings.setRemindersEnabled(false) }
            ReminderScheduler.cancelAll(context)
            return
        }
        val needsRuntimePrompt = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsRuntimePrompt) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            scope.launch { container.settings.setRemindersEnabled(true) }
            ReminderScheduler.scheduleAll(context, settings.wakeTime, settings.bedtime)
        }
    }

    SettingCard(
        title = "Nhắc nhở",
        subtitle = "5 lần/ngày: bắt đầu buổi sáng, 3 lần nghỉ bàn giấy, hạ nhiệt buổi tối. " +
            "Thông báo thường, không phải báo thức — không chuông riêng, không toàn màn hình.",
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Bật nhắc nhở",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = settings.remindersEnabled, onCheckedChange = ::setEnabled)
        }

        if (settings.remindersEnabled && !exactAlarmGranted) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Chưa cấp quyền báo đúng giờ — nhắc nhở vẫn hoạt động nhưng có thể trễ vài phút.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(onClick = { ReminderScheduler.openExactAlarmSettings(context) }) {
                    Text("Cấp quyền báo đúng giờ")
                }
            }
        }
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
