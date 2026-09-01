package com.vh.health.core.program

import kotlinx.serialization.Serializable

/**
 * What the knees reported after a long walk or run.
 *
 * The app asks about the signal that actually applies here — ache that follows
 * volume — rather than a daily pain score, which would mostly read zero and teach
 * nothing.
 *
 * `@Serializable` so `ProgressRepository` (:app) can store a check-in's signal
 * directly, without a parallel DTO enum to keep in sync.
 */
@Serializable
enum class KneeSignal(val labelVi: String) {
    /** No ache, or gone within the hour. */
    CLEAR("Không nhức, hoặc tan trong 1 giờ"),

    /** Still there the next day, but under 3/10. */
    LINGERING("Còn nhức sang hôm sau, dưới 3/10"),

    /** Over 3/10, or still there after 24 hours. */
    OVERLOADED("Nhức trên 3/10 hoặc quá 24 giờ"),
}

/**
 * How next week's load responds.
 *
 * The strength work is deliberately never cut back. Loading the quadriceps, glutes
 * and calves is the part that raises the knee's capacity; it is the impact volume
 * that caused the overload, so impact volume is what gives way.
 */
data class LoadDecision(
    val impactFactor: Double,
    val strengthFactor: Double,
    val explanationVi: String,
)

object KneeLoadPolicy {

    /** The ceiling the programme never crosses, even on a good week. */
    const val MAX_WEEKLY_IMPACT_INCREASE: Double = 0.10

    fun decide(signal: KneeSignal): LoadDecision = when (signal) {
        KneeSignal.CLEAR -> LoadDecision(
            impactFactor = 1.0 + MAX_WEEKLY_IMPACT_INCREASE,
            strengthFactor = 1.0,
            explanationVi = "Gối đang theo kịp. Tăng khối lượng đi bộ và chạy 10% theo kế hoạch.",
        )

        KneeSignal.LINGERING -> LoadDecision(
            impactFactor = 1.0,
            strengthFactor = 1.0,
            explanationVi = "Giữ nguyên khối lượng tuần này, không cộng thêm. Vẫn tập đủ bài.",
        )

        KneeSignal.OVERLOADED -> LoadDecision(
            impactFactor = 0.7,
            strengthFactor = 1.0,
            explanationVi = "Giảm 30% khối lượng đi bộ và chạy. Giữ nguyên phần tạ — " +
                "phần tạ là phần đang chữa, không phải phần gây quá tải.",
        )
    }

    /** True when the same red signal has come back often enough to be worth a doctor. */
    fun shouldSuggestClinician(recentSignals: List<KneeSignal>, window: Int = 2): Boolean {
        require(window >= 1) { "window must be at least 1 week, got $window" }
        return recentSignals.takeLast(window).count { it == KneeSignal.OVERLOADED } >= 2
    }

    /**
     * The factor [applyCardioLoadFactor] should scale a workout's cardio content by,
     * given the most recent knee check-in — the piece that turns a stored signal into
     * an actual load change on the next session. No check-in yet means no
     * adjustment: the programme runs exactly as authored until there is a real signal
     * to respond to.
     */
    fun impactFactorFor(mostRecentSignal: KneeSignal?): Double =
        mostRecentSignal?.let { decide(it).impactFactor } ?: 1.0
}
