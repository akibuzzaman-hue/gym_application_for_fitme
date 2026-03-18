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
    val note: String = "Add notes here...",
    val restTimerDuration: Int = 90, // Másodpercben, egyedi időzítő
    val sets: List<WorkoutSetData> = listOf(WorkoutSetData())
)

data class WorkoutTemplate(
    val id: Int = 0,
    val templateName: String,
    val exercises: List<ExerciseSessionData>
)