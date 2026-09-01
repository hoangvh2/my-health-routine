package com.vh.health.core.content

import kotlinx.serialization.json.Json

/**
 * Reads the bundled programme content.
 *
 * Content lives in this module's resources rather than in the app's assets so that
 * [validate] can run over the real files in `./gradlew :core:test` — a typo in an
 * exercise id fails CI instead of crashing the session player on a Tuesday morning.
 */
object ContentLoader {

    const val LIBRARY_PATH: String = "/content/exercises.json"
    const val PROGRAM_PATH: String = "/content/program.json"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    fun parseLibrary(text: String): ExerciseLibrary = json.decodeFromString(text)

    fun parseProgram(text: String): Program = json.decodeFromString(text)

    fun loadLibrary(): ExerciseLibrary = parseLibrary(readResource(LIBRARY_PATH))

    fun loadProgram(): Program = parseProgram(readResource(PROGRAM_PATH))

    private fun readResource(path: String): String =
        ContentLoader::class.java.getResourceAsStream(path)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: error("bundled content missing from the classpath: $path")

    /**
     * Every way the content can be internally inconsistent, as readable lines.
     * An empty list means the content is sound.
     */
    fun validate(library: ExerciseLibrary, program: Program): List<String> {
        val problems = mutableListOf<String>()

        library.exercises
            .groupBy { it.id }
            .filterValues { it.size > 1 }
            .keys
            .forEach { problems += "động tác trùng id: $it" }

        program.workouts
            .groupBy { it.id }
            .filterValues { it.size > 1 }
            .keys
            .forEach { problems += "buổi tập trùng id: $it" }

        val knownExercises = library.exercises.map { it.id }.toSet()
        program.workouts.forEach { workout ->
            if (workout.blocks.isEmpty()) problems += "buổi '${workout.id}' không có khối nào"
            workout.blocks.forEach { block ->
                if (block.items.isEmpty()) problems += "khối '${block.titleVi}' của '${workout.id}' rỗng"
                if (block.rounds < 1) problems += "khối '${block.titleVi}' của '${workout.id}' có số vòng < 1"
                block.items.forEach { item ->
                    if (item.exerciseId !in knownExercises) {
                        problems += "buổi '${workout.id}' gọi động tác không tồn tại: ${item.exerciseId}"
                    }
                    if (item.workSeconds <= 0 && item.reps == null) {
                        problems += "buổi '${workout.id}' có mục '${item.exerciseId}' không đặt thời gian lẫn số lần"
                    }
                }
            }
        }

        val knownWorkouts = program.workouts.map { it.id }.toSet()
        program.week.forEach { day ->
            if (day.workoutId !in knownWorkouts) {
                problems += "${day.day.labelVi} trỏ tới buổi tập không tồn tại: ${day.workoutId}"
            }
        }

        val days = program.week.map { it.day }
        Weekday.entries.forEach { weekday ->
            if (weekday !in days) problems += "tuần thiếu ${weekday.labelVi}"
        }
        days.groupBy { it }.filterValues { it.size > 1 }.keys.forEach {
            problems += "tuần có ${it.labelVi} hai lần"
        }

        return problems
    }

    /** Exercises the programme never uses — not an error, but worth knowing about. */
    fun unusedExercises(library: ExerciseLibrary, program: Program): List<String> {
        val used = program.workouts
            .flatMap { it.blocks }
            .flatMap { it.items }
            .map { it.exerciseId }
            .toSet()
        return library.exercises.map { it.id }.filterNot { it in used }
    }
}
