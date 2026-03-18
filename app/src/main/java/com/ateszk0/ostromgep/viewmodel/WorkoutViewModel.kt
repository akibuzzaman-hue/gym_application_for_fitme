package com.ateszk0.ostromgep.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ateszk0.ostromgep.model.ExerciseSessionData
import com.ateszk0.ostromgep.model.WorkoutSetData
import com.ateszk0.ostromgep.model.WorkoutTemplate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Belső adatmodell az elvégzett edzések tárolására
data class WorkoutHistoryEntry(val timestamp: Long, val exercises: List<ExerciseSessionData>)

class WorkoutViewModel : ViewModel() {

    private val _totalSeconds = MutableStateFlow(0)
    val totalSeconds: StateFlow<Int> = _totalSeconds.asStateFlow()

    private val _restTimerSeconds = MutableStateFlow(0)
    val restTimerSeconds: StateFlow<Int> = _restTimerSeconds.asStateFlow()

    private val _exerciseLibrary = MutableStateFlow(
        listOf("Fekvenyomás (Rúd)", "Guggolás", "Felhúzás", "Mellből nyomás", "Húzódzkodás", "Oldalemelés", "Bicepsz állva")
    )
    val exerciseLibrary: StateFlow<List<String>> = _exerciseLibrary.asStateFlow()

    private val _savedTemplates = MutableStateFlow<List<WorkoutTemplate>>(emptyList())
    val savedTemplates: StateFlow<List<WorkoutTemplate>> = _savedTemplates.asStateFlow()

    private val _workoutHistory = MutableStateFlow<List<WorkoutHistoryEntry>>(emptyList())

    private val _activeExercises = MutableStateFlow<List<ExerciseSessionData>>(emptyList())
    val activeExercises: StateFlow<List<ExerciseSessionData>> = _activeExercises.asStateFlow()

    init {
        // A háttérben futó időzítők
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _totalSeconds.value += 1
                if (_restTimerSeconds.value > 0) {
                    _restTimerSeconds.value -= 1
                }
            }
        }
    }

    // --- MÚLTBELI ADATOK VISSZAKERESÉSE ---
    private fun getLastPerformedSets(exerciseName: String): List<WorkoutSetData>? {
        val lastWorkout = _workoutHistory.value.reversed().flatMap { it.exercises }.find { it.name == exerciseName }
        return lastWorkout?.sets
    }

    private fun getLastRestTimer(exerciseName: String): Int {
        val lastWorkout = _workoutHistory.value.reversed().flatMap { it.exercises }.find { it.name == exerciseName }
        return lastWorkout?.restTimerDuration ?: 90
    }

    // --- EDZÉS INDÍTÁSA / BEFEJEZÉSE ---
    fun startEmptyWorkout() {
        _activeExercises.value = emptyList()
        _totalSeconds.value = 0
        _restTimerSeconds.value = 0
    }

    fun startWorkoutFromTemplate(template: WorkoutTemplate) {
        val newSession = template.exercises.map { templateEx ->
            val previousSets = getLastPerformedSets(templateEx.name)

            val updatedSets = templateEx.sets.mapIndexed { index, templateSet ->
                val prevSet = previousSets?.getOrNull(index)
                templateSet.copy(
                    isCompleted = false,
                    kg = prevSet?.kg ?: templateSet.kg,
                    reps = prevSet?.reps ?: templateSet.reps,
                    rpe = "",
                    previousText = if (prevSet != null && prevSet.kg.isNotBlank()) "${prevSet.kg}kg x ${prevSet.reps}" else "-"
                )
            }
            templateEx.copy(sets = updatedSets)
        }
        _activeExercises.value = newSession
        _totalSeconds.value = 0
        _restTimerSeconds.value = 0
    }

    fun finishWorkout(saveAsTemplateName: String?) {
        _workoutHistory.value = _workoutHistory.value + WorkoutHistoryEntry(System.currentTimeMillis(), _activeExercises.value)

        if (!saveAsTemplateName.isNullOrBlank()) {
            val newId = (_savedTemplates.value.maxOfOrNull { it.id } ?: 0) + 1
            val newTemplate = WorkoutTemplate(id = newId, templateName = saveAsTemplateName, exercises = _activeExercises.value)
            _savedTemplates.value = _savedTemplates.value + newTemplate
        }

        _activeExercises.value = emptyList()
    }

    // --- GYAKORLATOK ÉS SZÉRIÁK KEZELÉSE ---
    fun createCustomExercise(name: String) {
        if (name.isNotBlank() && !_exerciseLibrary.value.contains(name)) {
            _exerciseLibrary.value = _exerciseLibrary.value + listOf(name)
        }
    }

    fun addNewExerciseBlock(exerciseName: String) {
        val currentList = _activeExercises.value
        val newId = (currentList.maxOfOrNull { it.id } ?: 0) + 1

        val previousSets = getLastPerformedSets(exerciseName)
        val firstPrev = previousSets?.firstOrNull()
        val prevText = if (firstPrev != null && firstPrev.kg.isNotBlank()) "${firstPrev.kg}kg x ${firstPrev.reps}" else "-"

        val initialSet = WorkoutSetData(
            id = 1,
            setLabel = "1",
            previousText = prevText,
            kg = firstPrev?.kg ?: "",
            reps = firstPrev?.reps ?: "",
            rpe = ""
        )
        val newExercise = ExerciseSessionData(
            id = newId,
            name = exerciseName,
            restTimerDuration = getLastRestTimer(exerciseName),
            sets = listOf(initialSet)
        )
        _activeExercises.value = currentList + listOf(newExercise)
    }

    fun swapExercises(fromIndex: Int, toIndex: Int) {
        val current = _activeExercises.value.toMutableList()
        val temp = current[fromIndex]
        current[fromIndex] = current[toIndex]
        current[toIndex] = temp
        _activeExercises.value = current
    }

    fun deleteSet(exerciseId: Int, setId: Int) {
        _activeExercises.value = _activeExercises.value.map { ex ->
            if (ex.id == exerciseId) {
                val filteredSets = ex.sets.filter { it.id != setId }
                var normalSetCount = 1
                val renumberedSets = filteredSets.map { set ->
                    if (set.isWarmup) set else set.copy(setLabel = (normalSetCount++).toString())
                }
                ex.copy(sets = renumberedSets)
            } else ex
        }
    }

    fun updateSet(exerciseId: Int, updatedSet: WorkoutSetData) {
        _activeExercises.value = _activeExercises.value.map { ex ->
            if (ex.id == exerciseId) {
                ex.copy(sets = ex.sets.map { if (it.id == updatedSet.id) updatedSet else it })
            } else ex
        }
    }

    fun toggleSetComplete(exerciseId: Int, set: WorkoutSetData) {
        val isNowCompleted = !set.isCompleted
        _activeExercises.value = _activeExercises.value.map { ex ->
            if (ex.id == exerciseId) {
                if (isNowCompleted) {
                    _restTimerSeconds.value = ex.restTimerDuration
                }
                ex.copy(sets = ex.sets.map { if (it.id == set.id) it.copy(isCompleted = isNowCompleted) else it })
            } else ex
        }
    }

    fun addSet(exerciseId: Int) {
        _activeExercises.value = _activeExercises.value.map { ex ->
            if (ex.id == exerciseId) {
                val newId = (ex.sets.maxOfOrNull { it.id } ?: 0) + 1
                val lastSet = ex.sets.lastOrNull()

                val previousSets = getLastPerformedSets(ex.name)
                val prevSetMatched = previousSets?.getOrNull(ex.sets.size)
                val prevText = if (prevSetMatched != null && prevSetMatched.kg.isNotBlank()) "${prevSetMatched.kg}kg x ${prevSetMatched.reps}" else lastSet?.previousText ?: "-"

                val newSetNumber = ex.sets.count { !it.isWarmup } + 1
                ex.copy(sets = ex.sets + listOf(WorkoutSetData(
                    id = newId,
                    setLabel = newSetNumber.toString(),
                    previousText = prevText,
                    kg = prevSetMatched?.kg ?: lastSet?.kg ?: "",
                    reps = prevSetMatched?.reps ?: lastSet?.reps ?: "",
                    rpe = ""
                )))
            } else ex
        }
    }

    fun updateExerciseRestTime(exerciseId: Int, newSeconds: Int) {
        _activeExercises.value = _activeExercises.value.map { ex ->
            if (ex.id == exerciseId) ex.copy(restTimerDuration = newSeconds) else ex
        }
    }

    fun adjustRestTimer(amount: Int) {
        _restTimerSeconds.value = maxOf(0, _restTimerSeconds.value + amount)
    }

    fun skipRestTimer() {
        _restTimerSeconds.value = 0
    }
}