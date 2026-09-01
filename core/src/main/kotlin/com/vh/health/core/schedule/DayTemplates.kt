package com.vh.health.core.schedule

import java.time.LocalTime

/**
 * The default shape of a weekday, as approved in docs/PLAN.md.
 *
 * Anchored at 04:30 this lays out exactly:
 *   04:30 vệ sinh · 04:40 khởi động · 04:52 buổi chính · 05:25 giãn cơ ·
 *   05:35 tắm · 05:55 bữa sáng · 06:15 xong.
 */
object DayTemplates {

    val DEFAULT_WAKE: LocalTime = LocalTime.of(4, 30)
    val DEFAULT_BEDTIME: LocalTime = LocalTime.of(20, 30)
    const val DEFAULT_SLEEP_TARGET_MINUTES: Int = 8 * 60

    /** Desk breaks hang off the working day, not off the wake time. */
    val DEFAULT_DESK_BREAKS: List<LocalTime> =
        listOf(LocalTime.of(9, 30), LocalTime.of(14, 0), LocalTime.of(16, 30))

    const val DESK_BREAK_MINUTES: Int = 3

    fun morning(mainMinutes: Int = 33): List<DayBlock> = listOf(
        DayBlock(
            id = "personal_care",
            title = "Dậy, vệ sinh cá nhân, uống 400 ml nước",
            kind = BlockKind.PERSONAL_CARE,
            minutes = 10,
            minMinutes = 5,
            priority = Priority.ESSENTIAL,
        ),
        DayBlock(
            id = "warm_up",
            title = "Khởi động & mobility",
            kind = BlockKind.WARM_UP,
            minutes = 12,
            minMinutes = 6,
            priority = Priority.ESSENTIAL,
            note = "Không bao giờ bỏ hẳn: khớp và đĩa đệm còn cứng sau 8 tiếng nằm.",
        ),
        DayBlock(
            id = "main",
            title = "Buổi tập chính",
            kind = BlockKind.MAIN,
            minutes = mainMinutes,
            minMinutes = 12,
            priority = Priority.HIGH,
        ),
        DayBlock(
            id = "cool_down",
            title = "Giãn cơ tĩnh & thở",
            kind = BlockKind.COOL_DOWN,
            minutes = 10,
            minMinutes = 5,
            priority = Priority.ESSENTIAL,
        ),
        DayBlock(
            id = "shower",
            title = "Tắm & vệ sinh sau tập",
            kind = BlockKind.PERSONAL_CARE,
            minutes = 20,
            minMinutes = 10,
            priority = Priority.HIGH,
        ),
        DayBlock(
            id = "breakfast",
            title = "Bữa sáng nhiều đạm",
            kind = BlockKind.MEAL,
            minutes = 20,
            minMinutes = 10,
            priority = Priority.NORMAL,
        ),
    )

    /**
     * Anchored with [Anchor.FinishBy] at bedtime, this ends the day at 20:30 having
     * started at 19:45 — the wind-down that makes an 20:30 bedtime actually work.
     */
    fun evening(): List<DayBlock> = listOf(
        DayBlock(
            id = "wind_down",
            title = "Hạ nhiệt: giãn cơ nhẹ & thở 4-7-8",
            kind = BlockKind.WIND_DOWN,
            minutes = 15,
            minMinutes = 8,
            priority = Priority.NORMAL,
        ),
        DayBlock(
            id = "evening_buffer",
            title = "Thời gian đệm",
            kind = BlockKind.BUFFER,
            minutes = 15,
            minMinutes = 5,
            priority = Priority.OPTIONAL,
        ),
        DayBlock(
            id = "prepare_sleep",
            title = "Soạn đồ tập, chuẩn bị đi ngủ",
            kind = BlockKind.PERSONAL_CARE,
            minutes = 15,
            minMinutes = 5,
            priority = Priority.NORMAL,
        ),
    )
}
