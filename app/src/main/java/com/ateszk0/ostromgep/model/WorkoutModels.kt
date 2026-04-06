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
) {
    fun normalize(): WorkoutSetData {
        @Suppress("SENSELESS_COMPARISON")
        return copy(
            setLabel = if (setLabel != null) setLabel else "1",
            previousText = if (previousText != null) previousText else "-",
            kg = if (kg != null) kg else "",
            reps = if (reps != null) reps else "",
            rpe = if (rpe != null) rpe else "",
            isCompleted = if (isCompleted != null) isCompleted else false,
            isWarmup = if (isWarmup != null) isWarmup else false
        )
    }

    fun toggleComplete(): WorkoutSetData = copy(isCompleted = !isCompleted)
    fun toggleWarmup(): WorkoutSetData = copy(isWarmup = !isWarmup)
    fun isWorkingSet() = !isWarmup && kg.isNotBlank() && reps.isNotBlank()
    fun volume(): Double = if (isCompleted && !isWarmup) (kg.toDoubleOrNull() ?: 0.0) * (reps.toIntOrNull() ?: 0) else 0.0
}

data class ExerciseSessionData(
    val id: Int = 0,
    val name: String,
    val note: String = "",
    val restTimerDuration: Int = 90,
    val supersetId: String? = null,
    val sets: List<WorkoutSetData> = listOf(WorkoutSetData())
) {
    fun normalize(): ExerciseSessionData {
        @Suppress("SENSELESS_COMPARISON")
        return copy(
            name = if (name != null) name else "Unknown Exercise",
            note = if (note != null) note else "",
            restTimerDuration = if (restTimerDuration != null && restTimerDuration != 0) restTimerDuration else 90,
            supersetId = if (supersetId != null) supersetId else null,
            sets = if (sets != null) sets.map { it.normalize() } else emptyList()
        )
    }
    // OOP Methods to manipulate the session
    fun addSet(suggestedLabel: String, previousText: String, kg: String, reps: String): ExerciseSessionData {
        val newId = (sets.maxOfOrNull { it.id } ?: 0) + 1
        val newSet = WorkoutSetData(
            id = newId,
            setLabel = suggestedLabel,
            previousText = previousText,
            kg = kg,
            reps = reps,
            rpe = ""
        )
        return copy(sets = sets + newSet)
    }

    fun deleteSet(setId: Int): ExerciseSessionData {
        val updatedSets = sets.filter { it.id != setId }
        return copy(sets = recalculateSetLabels(updatedSets))
    }

    fun updateSet(setId: Int, newSet: WorkoutSetData): ExerciseSessionData {
        return copy(sets = sets.map { if (it.id == setId) newSet else it })
    }

    fun toggleSetComplete(setId: Int): ExerciseSessionData {
        return copy(sets = sets.map { if (it.id == setId) it.toggleComplete() else it })
    }

    fun toggleSetWarmup(setId: Int): ExerciseSessionData {
        val updatedSets = sets.map { if (it.id == setId) it.toggleWarmup() else it }
        return copy(sets = recalculateSetLabels(updatedSets))
    }

    private fun recalculateSetLabels(list: List<WorkoutSetData>): List<WorkoutSetData> {
        var c = 1
        return list.map { set ->
            if (set.isWarmup) {
                set.copy(setLabel = "W")
            } else {
                set.copy(setLabel = (c++).toString())
            }
        }
    }

    fun updateNote(newNote: String): ExerciseSessionData = copy(note = newNote)
    fun updateRestTimer(duration: Int): ExerciseSessionData = copy(restTimerDuration = duration)

    fun totalVolume(): Double = sets.sumOf { it.volume() }
    fun countCompletedSets(): Int = sets.count { it.isCompleted }
}

data class WorkoutTemplate(
    val id: Int = 0,
    val templateName: String,
    val exercises: List<ExerciseSessionData>
) {
    fun normalize(): WorkoutTemplate {
        @Suppress("SENSELESS_COMPARISON")
        return copy(
            templateName = if (templateName != null) templateName else "Unnamed Template",
            exercises = if (exercises != null) exercises.map { it.normalize() } else emptyList()
        )
    }
}

data class WorkoutHistoryEntry(
    val timestamp: Long,
    val totalVolume: Double,
    val durationSeconds: Int,
    val exercises: List<ExerciseSessionData>,
    val name: String? = null
) {
    fun normalize(): WorkoutHistoryEntry {
        @Suppress("SENSELESS_COMPARISON")
        return copy(
            name = if (name != null) name else null,
            exercises = if (exercises != null) exercises.map { it.normalize() } else emptyList()
        )
    }
}

data class BodyWeightEntry(
    val timestamp: Long,
    val weightKg: Double
)

enum class ExerciseType {
    REPS_WEIGHT,
    REPS_ONLY,
    TIME,
    DISTANCE_TIME
}

enum class Equipment {
    NONE, BARBELL, DUMBBELL, KETTLEBELL, MACHINE, PLATE, RESISTANCE_BAND, SUSPENSION_BAND, OTHER
}

enum class MuscleGroup {
    ABDOMINALS, ABDUCTORS, ADDUCTORS, BICEPS, CALVES, CARDIO, CHEST, FOREARMS, 
    GLUTES, HAMSTRINGS, LATS, LOWER_BACK, NECK, QUADRICEPS, SHOULDERS, TRAPS, TRICEPS, UPPER_BACK, OTHER
}

data class ExerciseDef(
    val name: String,
    val minReps: Int = 8,
    val maxReps: Int = 12,
    val imageUri: String? = null,
    val videoUrl: String? = null,
    val muscleGroups: List<MuscleGroup> = emptyList(),
    val equipment: Equipment = Equipment.NONE,
    val type: ExerciseType = ExerciseType.REPS_WEIGHT,
    val isCustom: Boolean = false
) {
    fun normalize(): ExerciseDef {
        @Suppress("SENSELESS_COMPARISON")
        return copy(
            name = if (name != null) name else "Unknown Exercise",
            minReps = if (minReps != null && minReps > 0) minReps else 8,
            maxReps = if (maxReps != null && maxReps > 0) maxReps else 12,
            muscleGroups = if (muscleGroups != null) muscleGroups.filterNotNull() else emptyList(),
            equipment = if (equipment != null) equipment else Equipment.NONE,
            type = if (type != null) type else ExerciseType.REPS_WEIGHT,
            isCustom = if (isCustom != null) isCustom else false
        )
    }
}

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

data class RoutineChanges(
    val templateName: String,
    val addedExercises: Int,
    val removedExercises: Int,
    val addedSets: Int,
    val removedSets: Int
) {
    fun hasChanges(): Boolean = addedExercises > 0 || removedExercises > 0 || addedSets > 0 || removedSets > 0
}

data class WorkoutSummaryData(
    val durationSeconds: Int,
    val totalVolumeKg: Double,
    val totalSets: Int,
    val totalReps: Int,
    val muscleGroups: List<String>,
    val newPersonalRecords: List<String>       // exercise names where new max was set
)

data class OverallStats(
    val totalWorkouts: Int,
    val totalVolumeKg: Double,
    val totalDurationMin: Int,
    val avgDurationMin: Int,
    val longestStreakDays: Int,
    val thisMonthWorkouts: Int,
    val thisMonthVolumeKg: Double
)