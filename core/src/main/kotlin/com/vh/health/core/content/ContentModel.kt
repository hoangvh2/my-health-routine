package com.vh.health.core.content

import com.vh.health.core.program.ImpactLevel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MuscleGroup(val labelVi: String) {
    @SerialName("warmup") WARM_UP("Khởi động & mobility"),
    @SerialName("lower") LOWER("Thân dưới & mông"),
    @SerialName("knee") KNEE("Gối & cẳng chân"),
    @SerialName("core") CORE("Core & bụng"),
    @SerialName("upper") UPPER("Thân trên & lưng"),
    @SerialName("cardio") CARDIO("Cardio & tabata"),
}

@Serializable
enum class Equipment(val labelVi: String) {
    @SerialName("none") NONE("Tay không"),
    @SerialName("mat") MAT("Thảm"),
    @SerialName("band") BAND("Dây kháng lực"),
    @SerialName("dumbbell") DUMBBELL("Tạ đơn"),
    @SerialName("chair") CHAIR("Ghế hoặc bục"),
    @SerialName("wall") WALL("Tường"),
}

@Serializable
enum class Weekday(val labelVi: String) {
    @SerialName("mon") MONDAY("Thứ 2"),
    @SerialName("tue") TUESDAY("Thứ 3"),
    @SerialName("wed") WEDNESDAY("Thứ 4"),
    @SerialName("thu") THURSDAY("Thứ 5"),
    @SerialName("fri") FRIDAY("Thứ 6"),
    @SerialName("sat") SATURDAY("Thứ 7"),
    @SerialName("sun") SUNDAY("Chủ nhật"),
}

/**
 * One movement, with everything the player and the library page need.
 *
 * [animation] names a keyframe set drawn by the app's Compose animator (layer 1 of
 * the media plan); [videoUrl] is the optional online demo (layer 2). A file the user
 * attaches themselves (layer 3) is stored per-device and never lives in here.
 */
@Serializable
data class Exercise(
    val id: String,
    val nameVi: String,
    val nameEn: String,
    val group: MuscleGroup,
    val equipment: List<Equipment> = listOf(Equipment.NONE),
    val impact: ImpactLevel = ImpactLevel.NONE,
    val kneeFocus: Boolean = false,
    val tempo: String? = null,
    val cues: List<String> = emptyList(),
    val mistakes: List<String> = emptyList(),
    val easier: String? = null,
    val harder: String? = null,
    val animation: String? = null,
    val videoUrl: String? = null,
)

@Serializable
data class ExerciseLibrary(
    val version: Int,
    val exercises: List<Exercise>,
) {
    private val byId: Map<String, Exercise> by lazy { exercises.associateBy { it.id } }

    operator fun get(id: String): Exercise? = byId[id]

    fun inGroup(group: MuscleGroup): List<Exercise> = exercises.filter { it.group == group }

    fun kneeWork(): List<Exercise> = exercises.filter { it.kneeFocus }

    fun requiringOnly(available: Set<Equipment>): List<Exercise> =
        exercises.filter { exercise -> exercise.equipment.all { it == Equipment.NONE || it in available } }
}

@Serializable
data class WorkoutItem(
    val exerciseId: String,
    val workSeconds: Int = 0,
    val restSeconds: Int = 0,
    val reps: Int? = null,
    val perSide: Boolean = false,
    val note: String? = null,
)

@Serializable
data class WorkoutBlock(
    val titleVi: String,
    val rounds: Int = 1,
    val restBetweenRoundsSeconds: Int = 60,
    /** Recovery taken after the block finishes, before the next one starts. */
    val restAfterSeconds: Int = 0,
    val items: List<WorkoutItem>,
)

@Serializable
data class Workout(
    val id: String,
    val titleVi: String,
    val focusVi: String,
    val minutes: Int,
    val rpe: String,
    /**
     * True for the walking/running-heavy days — the ones the knee traffic light in
     * `KneeLoadPolicy` is actually about. The player only asks "did your knee ache
     * after this" on these; asking it after a strength or tabata day would be asking
     * about a signal that workout was never going to produce. See docs/DECISIONS.md.
     */
    val tracksKneeSignal: Boolean = false,
    val blocks: List<WorkoutBlock>,
) {
    /** Clock time the block list actually adds up to, rests included. */
    val estimatedSeconds: Int
        get() = blocks.sumOf { block ->
            val perRound = block.items.sumOf { it.workSeconds + it.restSeconds }
            perRound * block.rounds +
                block.restBetweenRoundsSeconds * (block.rounds - 1).coerceAtLeast(0) +
                block.restAfterSeconds
        }
}

@Serializable
data class ProgramDay(
    val day: Weekday,
    val workoutId: String,
)

@Serializable
data class Program(
    val id: String,
    val titleVi: String,
    val week: List<ProgramDay>,
    val workouts: List<Workout>,
) {
    private val byId: Map<String, Workout> by lazy { workouts.associateBy { it.id } }

    fun workout(id: String): Workout? = byId[id]

    fun workoutFor(day: Weekday): Workout? = week.firstOrNull { it.day == day }?.let { byId[it.workoutId] }
}
