package com.ateszk0.ostromgep.model

data class WorkoutSetData(
    val id: Int = 0,
    val setLabel: String = "1",
    val previousText: String = "-",
    val kg: String = "",
    val reps: String = "",
    val rpe: String = "",
    val isCompleted: Boolean = false,
    val isWarmup: Boolean = false
)

data class ExerciseSessionData(
    val id: Int = 0,
    val name: String,
    val note: String = "",
    val restTimerDuration: Int = 90,
    val sets: List<WorkoutSetData> = listOf(WorkoutSetData())
)

data class WorkoutTemplate(
    val id: Int = 0,
    val templateName: String,
    val exercises: List<ExerciseSessionData>
)

// ÚJ: Okos gyakorlat definíció (Rep Range-dzsel)
data class ExerciseDef(
    val name: String,
    val minReps: Int = 8,
    val maxReps: Int = 12
)

// ÚJ: A Varázsló ideiglenes adatmodellje
data class OverloadPrompt(
    val exerciseId: Int,
    val name: String,
    val oldWeight: Double,
    val oldReps: Int,
    val maxReps: Int,
    val minReps: Int,
    var isSelected: Boolean = true,
    var newWeightStr: String = formatWeight(oldWeight),
    var newRepsStr: String = (oldReps + 1).toString()
) {
    val requiresWeightIncrease: Boolean get() = (oldReps + 1) > maxReps

    companion object {
        fun formatWeight(w: Double): String = if (w % 1.0 == 0.0) w.toInt().toString() else w.toString()
    }
}