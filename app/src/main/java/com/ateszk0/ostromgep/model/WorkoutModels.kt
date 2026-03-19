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
    val sets: List<WorkoutSetData> = listOf(WorkoutSetData())
) {
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
)

data class WorkoutHistoryEntry(
    val timestamp: Long,
    val totalVolume: Double,
    val durationSeconds: Int,
    val exercises: List<ExerciseSessionData>
)

enum class MuscleGroup {
    CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, LEGS, CORE, CARDIO
}

data class ExerciseDef(
    val name: String,
    val minReps: Int = 8,
    val maxReps: Int = 12,
    val imageUri: String? = null,
    val muscleGroups: List<MuscleGroup> = emptyList()
)

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