package com.vh.health.core.session

enum class StepPhase { PREPARE, WORK, REST }

/**
 * One tick-driven segment of a running session: "do this exercise for this many
 * seconds", or "rest, here's what's next". The player is a pure function of a
 * position inside a `List<SessionStep>` — nothing about wall-clock time here.
 */
data class SessionStep(
    val phase: StepPhase,
    /** The exercise this segment is about. On REST this is the *upcoming* one, so the
     *  player can preview it — null only when nothing follows. */
    val exerciseId: String?,
    val seconds: Int,
    val blockTitle: String,
    val round: Int,
    val totalRounds: Int,
    /** True on the first step of a block, so the UI can announce a new section. */
    val isNewBlock: Boolean,
) {
    init {
        require(seconds > 0) { "a session step must last at least a second" }
    }
}

/** Where a running session sits right now. */
data class SessionCursor(
    val stepIndex: Int,
    val remainingInStep: Int,
    val elapsedInStep: Int,
    val finished: Boolean,
) {
    /** The last three seconds of a segment — the cue to start a 3-2-1 countdown. */
    val isCountIn: Boolean get() = !finished && remainingInStep in 1..3
}
