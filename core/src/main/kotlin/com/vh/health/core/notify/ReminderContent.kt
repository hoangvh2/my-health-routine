package com.vh.health.core.notify

import com.vh.health.core.content.Workout
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * What a reminder notification says. Deliberately front-loads the clock time and the
 * single most important fact — a smartwatch face shows the title and maybe one line
 * of text, nothing more, so anything past that is décor rather than information.
 */
data class NotificationCopy(
    val title: String,
    val text: String,
    /** Shown when the notification expands (BigTextStyle) — phone only in practice. */
    val bigText: String? = null,
)

private val HHMM = DateTimeFormatter.ofPattern("HH:mm")

object ReminderContent {

    /** The three desk-break cues rotate rather than repeat, so three buzzes a day
     *  teach three different small movements instead of nagging the same line. */
    private val deskBreakCues = listOf(
        "Đứng dậy, duỗi thẳng gối siết tứ đầu 10 lần.",
        "Mở hông: xoay hông 10 vòng mỗi bên.",
        "Nhón bắp chân 15 lần, giữ 2 giây ở đỉnh.",
    )

    fun forMorningStart(time: LocalTime, workout: Workout?): NotificationCopy {
        val clock = time.format(HHMM)
        if (workout == null) {
            return NotificationCopy(title = "$clock · Đến giờ dậy", text = "Hôm nay là ngày hồi phục.")
        }
        return NotificationCopy(
            title = "$clock · ${workout.titleVi}",
            text = "${workout.minutes}′ · RPE ${workout.rpe} · ${workout.focusVi}",
            bigText = "${workout.focusVi}. Khởi động trước, chạm để bắt đầu buổi tập.",
        )
    }

    fun forDeskBreak(index: Int, time: LocalTime): NotificationCopy = NotificationCopy(
        title = "${time.format(HHMM)} · Nghỉ bàn giấy · 3 phút",
        text = deskBreakCues.getOrElse(index) { deskBreakCues.first() },
    )

    fun forEveningWindDown(time: LocalTime, bedtime: LocalTime): NotificationCopy = NotificationCopy(
        title = "${time.format(HHMM)} · Hạ nhiệt trước khi ngủ",
        text = "Giãn cơ nhẹ, thở 4-7-8. Ngủ lúc ${bedtime.format(HHMM)}.",
    )
}
