package com.ateszk0.ostromgep.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ateszk0.ostromgep.NotificationHelper
import com.ateszk0.ostromgep.WorkoutAction
import com.ateszk0.ostromgep.WorkoutEventBus
import com.ateszk0.ostromgep.data.WorkoutRepository
import com.ateszk0.ostromgep.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {


    private val repository = WorkoutRepository(application)

    private val _appTheme = MutableStateFlow(repository.getTheme())
    val appTheme = _appTheme.asStateFlow()

    private val _totalSeconds = MutableStateFlow(0)
    val totalSeconds = _totalSeconds.asStateFlow()

    private val _restTimerSeconds = MutableStateFlow(0)
    val restTimerSeconds = _restTimerSeconds.asStateFlow()

    private val _exerciseLibrary = MutableStateFlow(repository.getExerciseLibrary())
    val exerciseLibrary = _exerciseLibrary.asStateFlow()

    private val _savedTemplates = MutableStateFlow(repository.getSavedTemplates())
    val savedTemplates = _savedTemplates.asStateFlow()

    private val _workoutHistory = MutableStateFlow(repository.getWorkoutHistory())
    val workoutHistory = _workoutHistory.asStateFlow()

    private val _activeExercises = MutableStateFlow<List<ExerciseSessionData>>(emptyList())
    val activeExercises = _activeExercises.asStateFlow()

    private val _overloadPrompts = MutableStateFlow<List<OverloadPrompt>>(emptyList())
    val overloadPrompts = _overloadPrompts.asStateFlow()

    private val _bodyWeightHistory = MutableStateFlow(repository.getBodyWeightHistory())
    val bodyWeightHistory = _bodyWeightHistory.asStateFlow()

    private val _username = MutableStateFlow(repository.getUsername())
    val username = _username.asStateFlow()

    private val _profilePictureUri = MutableStateFlow(repository.getProfilePictureUri())
    val profilePictureUri = _profilePictureUri.asStateFlow()

    init {
        viewModelScope.launch {
            WorkoutEventBus.events.collect { action ->
                when(action) {
                    WorkoutAction.SKIP_REST -> skipRestTimer()
                    WorkoutAction.ADD_15S -> adjustRestTimer(15)
                    WorkoutAction.SUB_15S -> adjustRestTimer(-15)
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_activeExercises.value.isNotEmpty()) {
                    _totalSeconds.value += 1
                    if (_restTimerSeconds.value > 0) _restTimerSeconds.value -= 1
                    NotificationHelper.updateNotification(application, _activeExercises.value, _restTimerSeconds.value)
                }
            }
        }
    }

    fun setTheme(colorName: String) { 
        _appTheme.value = colorName
        repository.saveTheme(colorName)
    }

    fun updateUsername(name: String) {
        _username.value = name
        repository.saveUsername(name)
    }

    fun updateProfilePictureUri(uri: String) {
        _profilePictureUri.value = uri
        repository.saveProfilePictureUri(uri)
    }

    fun deleteTemplate(templateId: Int) { 
        val newTemplates = _savedTemplates.value.filter { it.id != templateId }
        _savedTemplates.value = newTemplates
        repository.saveSavedTemplates(newTemplates)
    }

    private fun getLastPerformedSets(exerciseName: String) = _workoutHistory.value.reversed().flatMap { it.exercises }.find { it.name == exerciseName }?.sets
    private fun getLastRestTimer(exerciseName: String) = _workoutHistory.value.reversed().flatMap { it.exercises }.find { it.name == exerciseName }?.restTimerDuration ?: 90

    fun updateExerciseDetails(name: String, min: Int, max: Int, imageUri: String?, muscleGroups: List<MuscleGroup>) {
        val current = _exerciseLibrary.value.toMutableList()
        val index = current.indexOfFirst { it.name == name }
        if (index != -1) {
            current[index] = current[index].copy(minReps = min, maxReps = max, imageUri = imageUri, muscleGroups = muscleGroups)
        } else {
            current.add(ExerciseDef(name, min, max, imageUri, muscleGroups))
        }
        _exerciseLibrary.value = current
        repository.saveExerciseLibrary(current)
    }

    fun startEmptyWorkout() {
        _activeExercises.value = emptyList()
        _totalSeconds.value = 0
        _restTimerSeconds.value = 0
    }

    fun startWorkoutFromTemplate(template: WorkoutTemplate) {
        val newSession = template.exercises.map { templateEx ->
            val prevSets = getLastPerformedSets(templateEx.name)
            templateEx.copy(sets = templateEx.sets.mapIndexed { i, s ->
                val p = prevSets?.getOrNull(i)
                s.copy(
                    isCompleted = false, 
                    kg = p?.kg ?: s.kg, 
                    reps = p?.reps ?: s.reps, 
                    rpe = "", 
                    previousText = if (p != null && p.kg.isNotBlank()) "${p.kg}kg x ${p.reps}" else "-"
                )
            })
        }
        _activeExercises.value = newSession
        _totalSeconds.value = 0
        _restTimerSeconds.value = 0

        val prompts = mutableListOf<OverloadPrompt>()
        newSession.forEach { ex ->
            val def = _exerciseLibrary.value.find { it.name == ex.name } ?: ExerciseDef(ex.name)
            val firstWorkingSet = ex.sets.find { it.isWorkingSet() }
            if (firstWorkingSet != null) {
                val oldReps = firstWorkingSet.reps.toIntOrNull() ?: 0
                val oldWeight = firstWorkingSet.kg.toDoubleOrNull() ?: 0.0
                if (oldReps > 0) {
                    val prompt = OverloadPrompt(ex.id, ex.name, oldWeight, oldReps, def.maxReps, def.minReps)
                    if (prompt.requiresWeightIncrease) prompt.newRepsStr = def.minReps.toString()
                    prompts.add(prompt)
                }
            }
        }
        _overloadPrompts.value = prompts
    }

    fun applyOverloadPrompts(prompts: List<OverloadPrompt>) {
        val currentExes = _activeExercises.value.toMutableList()
        prompts.filter { it.isSelected }.forEach { prompt ->
            val exIndex = currentExes.indexOfFirst { it.id == prompt.exerciseId }
            if (exIndex != -1) {
                val ex = currentExes[exIndex]
                currentExes[exIndex] = ex.copy(sets = ex.sets.map { 
                    if (!it.isWarmup) it.copy(kg = prompt.newWeightStr, reps = prompt.newRepsStr) else it 
                })
            }
        }
        _activeExercises.value = currentExes
        _overloadPrompts.value = emptyList()
    }

    fun dismissOverloadPrompts() { _overloadPrompts.value = emptyList() }

    fun finishWorkout(saveAsTemplateName: String?) {
        val vol = _activeExercises.value.sumOf { it.totalVolume() }
        val newHistory = _workoutHistory.value + WorkoutHistoryEntry(System.currentTimeMillis(), vol, _totalSeconds.value, _activeExercises.value)
        _workoutHistory.value = newHistory
        repository.saveWorkoutHistory(newHistory)

        if (!saveAsTemplateName.isNullOrBlank()) {
            val existing = _savedTemplates.value.find { it.templateName == saveAsTemplateName }
            if (existing != null) {
                val updatedTemplates = _savedTemplates.value.map { 
                    if (it.id == existing.id) it.copy(exercises = _activeExercises.value) else it 
                }
                _savedTemplates.value = updatedTemplates
                repository.saveSavedTemplates(updatedTemplates)
            } else {
                val newTemplates = _savedTemplates.value + WorkoutTemplate(
                    id = (_savedTemplates.value.maxOfOrNull { it.id } ?: 0) + 1, 
                    templateName = saveAsTemplateName, 
                    exercises = _activeExercises.value
                )
                _savedTemplates.value = newTemplates
                repository.saveSavedTemplates(newTemplates)
            }
        }
        _activeExercises.value = emptyList()
    }

    fun saveNewTemplate(name: String, exercises: List<ExerciseSessionData>) {
        val id = (_savedTemplates.value.maxOfOrNull { it.id } ?: 0) + 1
        val updated = _savedTemplates.value + WorkoutTemplate(id, name, exercises)
        _savedTemplates.value = updated
        repository.saveSavedTemplates(updated)
    }

    fun createCustomExercise(name: String) { 
        if (name.isNotBlank() && _exerciseLibrary.value.none { it.name == name }) { 
            val newLib = _exerciseLibrary.value + ExerciseDef(name)
            _exerciseLibrary.value = newLib
            repository.saveExerciseLibrary(newLib)
        } 
    }

    fun addNewExerciseBlock(exerciseName: String) {
        val firstPrev = getLastPerformedSets(exerciseName)?.firstOrNull()
        val s = WorkoutSetData(
            id = 1, 
            setLabel = "1", 
            previousText = if (firstPrev != null && firstPrev.kg.isNotBlank()) "${firstPrev.kg}kg x ${firstPrev.reps}" else "-", 
            kg = firstPrev?.kg ?: "", 
            reps = firstPrev?.reps ?: "", 
            rpe = ""
        )
        val newSession = ExerciseSessionData(
            id = (_activeExercises.value.maxOfOrNull { it.id } ?: 0) + 1, 
            name = exerciseName, 
            restTimerDuration = getLastRestTimer(exerciseName), 
            sets = listOf(s)
        )
        _activeExercises.value = _activeExercises.value + newSession
    }

    fun deleteExercise(exerciseId: Int) { 
        _activeExercises.value = _activeExercises.value.filter { it.id != exerciseId } 
    }
    
    fun updateExerciseNote(exerciseId: Int, newNote: String) { 
        updateExercise(exerciseId) { it.updateNote(newNote) }
    }
    
    fun moveExerciseUp(index: Int) { 
        if (index > 0) { 
            val list = _activeExercises.value.toMutableList()
            val item = list.removeAt(index)
            list.add(index - 1, item)
            _activeExercises.value = list 
        } 
    }
    
    fun moveExerciseDown(index: Int) { 
        if (index < _activeExercises.value.size - 1) { 
            val list = _activeExercises.value.toMutableList()
            val item = list.removeAt(index)
            list.add(index + 1, item)
            _activeExercises.value = list 
        } 
    }
    
    fun deleteSet(eId: Int, sId: Int) { 
        updateExercise(eId) { it.deleteSet(sId) }
    }
    
    fun updateSet(eId: Int, s: WorkoutSetData) { 
        updateExercise(eId) { it.updateSet(s.id, s) }
    }
    
    fun toggleSetComplete(eId: Int, s: WorkoutSetData) { 
        updateExercise(eId) { 
            if (!s.isCompleted) {
                _restTimerSeconds.value = it.restTimerDuration
            }
            it.toggleSetComplete(s.id) 
        }
    }
    
    fun toggleWarmup(eId: Int, sId: Int) { 
        updateExercise(eId) { it.toggleSetWarmup(sId) }
    }
    
    fun addSet(eId: Int) { 
        updateExercise(eId) { ex ->
            val p = getLastPerformedSets(ex.name)?.getOrNull(ex.sets.size)
            val prevText = if (p != null && p.kg.isNotBlank()) "${p.kg}kg x ${p.reps}" else ex.sets.lastOrNull()?.previousText ?: "-"
            val kg = p?.kg ?: ex.sets.lastOrNull()?.kg ?: ""
            val reps = p?.reps ?: ex.sets.lastOrNull()?.reps ?: ""
            val label = (ex.sets.count { !it.isWarmup } + 1).toString()
            ex.addSet(label, prevText, kg, reps)
        }
    }
    
    fun updateExerciseRestTime(eId: Int, durationSeconds: Int) { 
        updateExercise(eId) { it.updateRestTimer(durationSeconds) }
    }
    
    private fun updateExercise(eId: Int, updater: (ExerciseSessionData) -> ExerciseSessionData) {
        _activeExercises.value = _activeExercises.value.map { if (it.id == eId) updater(it) else it }
    }
    
    fun adjustRestTimer(a: Int) { _restTimerSeconds.value = maxOf(0, _restTimerSeconds.value + a) }
    fun skipRestTimer() { _restTimerSeconds.value = 0 }

    fun getChartData(): List<Float> {
        val volumes = _workoutHistory.value.takeLast(7).map { it.totalVolume.toFloat() }
        if (volumes.isEmpty()) return listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f)
        val max = volumes.maxOrNull() ?: 1f
        return volumes.map { it / max }
    }

    fun addBodyWeight(weightKg: Double) {
        val newEntry = BodyWeightEntry(System.currentTimeMillis(), weightKg)
        val updated = _bodyWeightHistory.value + newEntry
        _bodyWeightHistory.value = updated
        repository.saveBodyWeightHistory(updated)
    }

    fun getMuscleRecoveryStatus(): Map<MuscleGroup, String> {
        val now = System.currentTimeMillis()
        val recoveryMap = mutableMapOf<MuscleGroup, String>()
        
        MuscleGroup.values().forEach { recoveryMap[it] = "Green" }
        
        val oneDayMs = 24 * 60 * 60 * 1000L
        val twoDaysMs = 48 * 60 * 60 * 1000L

        _workoutHistory.value.forEach { entry ->
            val timeDiff = now - entry.timestamp
            entry.exercises.forEach { exSession ->
                val def = _exerciseLibrary.value.find { it.name == exSession.name }
                def?.muscleGroups?.forEach { mg ->
                    if (timeDiff < oneDayMs) {
                        recoveryMap[mg] = "Red"
                    } else if (timeDiff < twoDaysMs && recoveryMap[mg] != "Red") {
                        recoveryMap[mg] = "Yellow"
                    }
                }
            }
        }
        return recoveryMap
    }

    fun suggestNextMission(): WorkoutTemplate? {
        val templates = _savedTemplates.value
        if (templates.isEmpty()) return null
        
        val templateLastUsed = templates.associateWith { t ->
            val latestEntry = _workoutHistory.value.filter {
                it.exercises.map { e -> e.name }.containsAll(t.exercises.map { e -> e.name })
            }.maxByOrNull { it.timestamp }
            latestEntry?.timestamp ?: 0L
        }
        
        return templateLastUsed.minByOrNull { it.value }?.key
    }

    fun getWeeklyBattleLog(): List<Boolean> {
        val now = java.util.Calendar.getInstance()
        var currentDayOfWeek = now.get(java.util.Calendar.DAY_OF_WEEK) - 1
        if (currentDayOfWeek == 0) currentDayOfWeek = 7 
        
        val mondayCal = now.clone() as java.util.Calendar
        mondayCal.add(java.util.Calendar.DAY_OF_YEAR, -(currentDayOfWeek - 1))
        mondayCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        mondayCal.set(java.util.Calendar.MINUTE, 0)
        mondayCal.set(java.util.Calendar.SECOND, 0)
        mondayCal.set(java.util.Calendar.MILLISECOND, 0)
        
        val startOfWeekMs = mondayCal.timeInMillis
        val dayMs = 24 * 60 * 60 * 1000L
        
        val booleanList = mutableListOf<Boolean>()
        for (i in 0..6) {
            val startOfDay = startOfWeekMs + (i * dayMs)
            val endOfDay = startOfDay + dayMs
            val hasWorkout = _workoutHistory.value.any { it.timestamp in startOfDay until endOfDay }
            booleanList.add(hasWorkout)
        }
        return booleanList
    }

    fun getPersonalRecords(): List<String> {
        val records = mutableListOf<String>()
        val exerciseMaxWeight = mutableMapOf<String, Double>()
        val exerciseMaxVolume = mutableMapOf<String, Double>()
        
        _workoutHistory.value.sortedBy { it.timestamp }.forEach { entry ->
            entry.exercises.forEach { exSession ->
                var sessionMaxWeight = 0.0
                var sessionTotalVolume = 0.0
                
                exSession.sets.filter { it.isCompleted && !it.isWarmup }.forEach { set ->
                    val weight = set.kg.toDoubleOrNull() ?: 0.0
                    if (weight > sessionMaxWeight) sessionMaxWeight = weight
                    sessionTotalVolume += set.volume()
                }
                
                if (sessionMaxWeight > 0) {
                    val prevMaxWeight = exerciseMaxWeight[exSession.name] ?: 0.0
                    if (sessionMaxWeight > prevMaxWeight) {
                        if (prevMaxWeight > 0.0) { 
                            records.add("🔥 ${exSession.name}: Új súlyrekord ${OverloadPrompt.formatWeight(sessionMaxWeight)}kg!")
                        }
                        exerciseMaxWeight[exSession.name] = sessionMaxWeight
                    }
                }
                
                if (sessionTotalVolume > 0) {
                    val prevMaxVol = exerciseMaxVolume[exSession.name] ?: 0.0
                    if (sessionTotalVolume > prevMaxVol && prevMaxVol > 0.0) {
                        records.add("📈 ${exSession.name}: Új volumenrekord ${sessionTotalVolume.toInt()}kg!")
                        exerciseMaxVolume[exSession.name] = sessionTotalVolume
                    }
                }
            }
        }
        return records.takeLast(10).reversed()
    }
}