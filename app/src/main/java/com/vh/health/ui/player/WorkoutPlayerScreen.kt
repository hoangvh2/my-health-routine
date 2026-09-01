package com.vh.health.ui.player

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vh.health.AppContainer
import com.vh.health.core.content.Exercise
import com.vh.health.core.session.StepPhase
import com.vh.health.ui.minutesAsText
import com.vh.health.ui.theme.ClockStyle
import com.vh.health.ui.theme.MicroLabel

@Composable
fun WorkoutPlayerScreen(container: AppContainer, workoutId: String, onFinish: () -> Unit) {
    val viewModel: WorkoutPlayerViewModel = viewModel(factory = WorkoutPlayerViewModel.factory(container, workoutId))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    // "Màn hình luôn sáng trong lúc tập" — docs/PLAN.md.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(state.stepIndex) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    if (state.workout == null || state.steps.isEmpty()) {
        EmptySessionNotice(onFinish)
        return
    }

    val step = state.currentStep
    val phase = step?.phase ?: StepPhase.PREPARE
    val accent = phaseColor(phase)
    val exercise = step?.exerciseId?.let { container.content.library[it] }
    val nextExercise = state.nextStep?.exerciseId?.let { container.content.library[it] }
    val ringFraction = state.remainingInStep.toFloat() / (step?.seconds?.toFloat() ?: 1f)

    Column(modifier = Modifier.fillMaxSize()) {
        PlayerTopBar(
            blockTitle = step?.blockTitle.orEmpty(),
            round = step?.round ?: 1,
            totalRounds = step?.totalRounds ?: 1,
            elapsedSeconds = state.elapsedSeconds,
            totalSeconds = state.totalSeconds,
            onClose = onFinish,
        )

        Column(
            // weight(1f), not fillMaxSize(): inside a Column, fillMaxSize() on a
            // middle child claims the WHOLE column's height (matching its parent),
            // not "whatever is left after the top bar" — that pushed PlayerControls
            // off the bottom of the screen entirely, so Bắt đầu was unreachable and
            // nothing ever played. weight(1f) is what actually means "the remainder".
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (state.isFinished) {
                FinishedContent(onFinish)
            } else {
                Text(
                    text = phaseLabel(phase).uppercase(),
                    style = MicroLabel,
                    color = accent,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = exercise?.nameVi ?: (if (phase == StepPhase.PREPARE) "Chuẩn bị" else "Nghỉ"),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(28.dp))

                CountdownRing(fraction = ringFraction, accent = accent, isCountIn = state.isCountIn) {
                    Text(
                        text = "${state.remainingInStep}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 56.sp),
                        color = if (state.isCountIn) accent else MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(Modifier.height(28.dp))
                CueText(phase, exercise, nextExercise)
                Spacer(Modifier.height(20.dp))
                exercise?.videoUrl?.let { url -> WatchVideoButton(url) }
            }
        }

        if (!state.isFinished) {
            PlayerControls(
                isRunning = state.isRunning,
                onToggle = viewModel::toggle,
                onPrev = viewModel::skipToPrevious,
                onNext = viewModel::skipToNext,
            )
        }
    }
}

/* ------------------------------------------------------------------ parts */

@Composable
private fun PlayerTopBar(
    blockTitle: String,
    round: Int,
    totalRounds: Int,
    elapsedSeconds: Int,
    totalSeconds: Int,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = "Đóng")
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(blockTitle, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            if (totalRounds > 1) {
                Text(
                    "Vòng $round/$totalRounds",
                    style = MicroLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = "${minutesAsText(elapsedSeconds / 60)} / ${minutesAsText(totalSeconds / 60)}",
            style = ClockStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp),
        )
    }
}

@Composable
private fun CountdownRing(
    fraction: Float,
    accent: Color,
    isCountIn: Boolean,
    content: @Composable () -> Unit,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(224.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = if (isCountIn) 18.dp.toPx() else 14.dp.toPx()
            val inset = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * fraction.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        content()
    }
}

@Composable
private fun CueText(phase: StepPhase, exercise: Exercise?, nextExercise: Exercise?) {
    when {
        phase == StepPhase.REST && nextExercise != null -> Text(
            text = "Tiếp theo: ${nextExercise.nameVi}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        phase == StepPhase.WORK && exercise != null && exercise.cues.isNotEmpty() -> Text(
            text = exercise.cues.first(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        else -> {}
    }
}

@Composable
private fun WatchVideoButton(url: String) {
    val context = LocalContext.current
    Button(
        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Xem video hướng dẫn", color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PlayerControls(isRunning: Boolean, onToggle: () -> Unit, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Bài trước", modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.width(24.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(76.dp),
        ) {
            IconButton(onClick = onToggle, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isRunning) "Tạm dừng" else "Bắt đầu",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        Spacer(Modifier.width(24.dp))
        IconButton(onClick = onNext, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Bài kế", modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun FinishedContent(onFinish: () -> Unit) {
    Text("Hoàn thành!", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(8.dp))
    Text(
        "Làm tốt lắm. Nhớ uống nước.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(24.dp))
    Button(onClick = onFinish) { Text("Xong") }
}

@Composable
private fun EmptySessionNotice(onFinish: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Không tìm thấy buổi tập", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onFinish) { Text("Quay lại") }
    }
}

private fun phaseLabel(phase: StepPhase): String = when (phase) {
    StepPhase.PREPARE -> "Chuẩn bị"
    StepPhase.WORK -> "Tập"
    StepPhase.REST -> "Nghỉ"
}

@Composable
private fun phaseColor(phase: StepPhase): Color = when (phase) {
    StepPhase.WORK -> MaterialTheme.colorScheme.tertiary
    StepPhase.REST -> MaterialTheme.colorScheme.secondary
    StepPhase.PREPARE -> MaterialTheme.colorScheme.primary
}
