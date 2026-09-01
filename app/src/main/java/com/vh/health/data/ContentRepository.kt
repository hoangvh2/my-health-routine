package com.vh.health.data

import com.vh.health.core.content.ContentLoader
import com.vh.health.core.content.ExerciseLibrary
import com.vh.health.core.content.Program
import com.vh.health.core.content.Weekday
import java.time.LocalDate

/**
 * The bundled programme. Read once from the :core module's resources, which is the
 * same content `./gradlew :core:test` validates, so what ships is what was checked.
 */
class ContentRepository {

    val library: ExerciseLibrary by lazy { ContentLoader.loadLibrary() }

    val program: Program by lazy { ContentLoader.loadProgram() }

    fun weekdayToday(): Weekday = Weekday.entries[LocalDate.now().dayOfWeek.ordinal]

    fun workoutToday() = program.workoutFor(weekdayToday())
}
