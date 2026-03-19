package com.ateszk0.ostromgep.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ateszk0.ostromgep.model.ExerciseDef
import com.ateszk0.ostromgep.model.ExerciseSessionData
import com.ateszk0.ostromgep.model.OverloadPrompt
import com.ateszk0.ostromgep.model.WorkoutSetData
import com.ateszk0.ostromgep.model.WorkoutTemplate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WorkoutHistoryEntry(val timestamp: Long, val totalVolume: Double, val durationSeconds: Int, val exercises: List<ExerciseSessionData>)

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("ostromgep_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _appTheme = MutableStateFlow("Kék")
    val appTheme = _appTheme.asStateFlow()

    private val _totalSeconds = MutableStateFlow(0)
    val totalSeconds = _totalSeconds.asStateFlow()

    private val _restTimerSeconds = MutableStateFlow(0)
    val restTimerSeconds = _restTimerSeconds.asStateFlow()

    // ÚJ: Kibővített gyakorlatkönyvtár
    private val _exerciseLibrary = MutableStateFlow<List<ExerciseDef>>(emptyList())
    val exerciseLibrary = _exerciseLibrary.asStateFlow()

    private val _savedTemplates = MutableStateFlow<List<WorkoutTemplate>>(emptyList())
    val savedTemplates = _savedTemplates.asStateFlow()

    private val _workoutHistory = MutableStateFlow<List<WorkoutHistoryEntry>>(emptyList())
    val workoutHistory = _workoutHistory.asStateFlow()

    private val _activeExercises = MutableStateFlow<List<ExerciseSessionData>>(emptyList())
    val activeExercises = _activeExercises.asStateFlow()

    // ÚJ: Progressive Overload varázsló állapota
    private val _overloadPrompts = MutableStateFlow<List<OverloadPrompt>>(emptyList())
    val overloadPrompts = _overloadPrompts.asStateFlow()

    init {
        loadData()
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _totalSeconds.value += 1
                if (_restTimerSeconds.value > 0) _restTimerSeconds.value -= 1
            }
        }
    }

    private fun loadData() {
        _appTheme.value = prefs.getString("theme", "Kék") ?: "Kék"

        // Migráció: Ha van v2-es (okos) adatbázis, betöltjük, ha nincs, átalakítjuk a régit
        val libJsonV2 = prefs.getString("library_v2", null)
        if (libJsonV2 != null) {
            _exerciseLibrary.value = gson.fromJson(libJsonV2, object : TypeToken<List<ExerciseDef>>() {}.type)
        } else {
            val oldLibJson = prefs.getString("library", null)
            if (oldLibJson != null) {
                val oldList: List<String> = gson.fromJson(oldLibJson, object : TypeToken<List<String>>() {}.type)
                _exerciseLibrary.value = oldList.map { ExerciseDef(it) }
            } else {
                _exerciseLibrary.value = listOf(ExerciseDef("Fekvenyomás"), ExerciseDef("Guggolás"), ExerciseDef("Felhúzás"))
            }
        }

        val tempJson = prefs.getString("templates", null)
        val histJson = prefs.getString("history", null)
        if (tempJson != null) _savedTemplates.value = gson.fromJson(tempJson, object : TypeToken<List<WorkoutTemplate>>() {}.type)
        if (histJson != null) _workoutHistory.value = gson.fromJson(histJson, object : TypeToken<List<WorkoutHistoryEntry>>() {}.type)
    }

    private fun saveData() {
        prefs.edit().apply {
            putString("library_v2", gson.toJson(_exerciseLibrary.value))
            putString("templates", gson.toJson(_savedTemplates.value))
            putString("history", gson.toJson(_workoutHistory.value))
            putString("theme", _appTheme.value)
            apply()
        }
    }

    fun setTheme(colorName: String) { _appTheme.value = colorName; saveData() }
    fun deleteTemplate(templateId: Int) { _savedTemplates.value = _savedTemplates.value.filter { it.id != templateId }; saveData() }

    private fun getLastPerformedSets(exerciseName: String) = _workoutHistory.value.reversed().flatMap { it.exercises }.find { it.name == exerciseName }?.sets
    private fun getLastRestTimer(exerciseName: String) = _workoutHistory.value.reversed().flatMap { it.exercises }.find { it.name == exerciseName }?.restTimerDuration ?: 90

    // --- ÚJ: Gyakorlat Rep Range Módosítása ---
    fun updateExerciseRepRange(name: String, min: Int, max: Int) {
        val current = _exerciseLibrary.value.toMutableList()
        val index = current.indexOfFirst { it.name == name }
        if (index != -1) {
            current[index] = current[index].copy(minReps = min, maxReps = max)
        } else {
            current.add(ExerciseDef(name, min, max))
        }
        _exerciseLibrary.value = current
        saveData()
    }

    fun startEmptyWorkout() { _activeExercises.value = emptyList(); _totalSeconds.value = 0; _restTimerSeconds.value = 0 }

    // --- ÚJ: PROGRESSZÍV TÚLTERHELÉS VARÁZSLÓ INDÍTÁSA ---
    fun startWorkoutFromTemplate(template: WorkoutTemplate) {
        val newSession = template.exercises.map { templateEx ->
            val prevSets = getLastPerformedSets(templateEx.name)
            templateEx.copy(sets = templateEx.sets.mapIndexed { i, s ->
                val p = prevSets?.getOrNull(i)
                s.copy(isCompleted = false, kg = p?.kg ?: s.kg, reps = p?.reps ?: s.reps, rpe = "", previousText = if (p != null && p.kg.isNotBlank()) "${p.kg}kg x ${p.reps}" else "-")
            })
        }
        _activeExercises.value = newSession
        _totalSeconds.value = 0; _restTimerSeconds.value = 0

        // Legeneráljuk a javaslatokat
        val prompts = mutableListOf<OverloadPrompt>()
        newSession.forEach { ex ->
            val def = _exerciseLibrary.value.find { it.name == ex.name } ?: ExerciseDef(ex.name)
            // Az első éles (nem bemelegítő) széria alapján döntünk
            val firstWorkingSet = ex.sets.find { !it.isWarmup && it.reps.isNotBlank() && it.kg.isNotBlank() }
            if (firstWorkingSet != null) {
                val oldReps = firstWorkingSet.reps.toIntOrNull() ?: 0
                val oldWeight = firstWorkingSet.kg.toDoubleOrNull() ?: 0.0

                if (oldReps > 0) {
                    val prompt = OverloadPrompt(ex.id, ex.name, oldWeight, oldReps, def.maxReps, def.minReps)
                    if (prompt.requiresWeightIncrease) { prompt.newRepsStr = def.minReps.toString() }
                    prompts.add(prompt)
                }
            }
        }
        _overloadPrompts.value = prompts
    }

    // A Varázsló eredményének alkalmazása a mezőkre
    fun applyOverloadPrompts(prompts: List<OverloadPrompt>) {
        val currentExes = _activeExercises.value.toMutableList()
        prompts.filter { it.isSelected }.forEach { prompt ->
            val exIndex = currentExes.indexOfFirst { it.id == prompt.exerciseId }
            if (exIndex != -1) {
                val ex = currentExes[exIndex]
                val updatedSets = ex.sets.map { set ->
                    if (!set.isWarmup) set.copy(kg = prompt.newWeightStr, reps = prompt.newRepsStr) else set
                }
                currentExes[exIndex] = ex.copy(sets = updatedSets)
            }
        }
        _activeExercises.value = currentExes
        _overloadPrompts.value = emptyList() // Varázsló bezárása
    }

    fun dismissOverloadPrompts() { _overloadPrompts.value = emptyList() }

    fun finishWorkout(saveAsTemplateName: String?) {
        val vol = _activeExercises.value.flatMap { it.sets }.filter { it.isCompleted && !it.isWarmup }.sumOf { (it.kg.toDoubleOrNull() ?: 0.0) * (it.reps.toIntOrNull() ?: 0) }
        _workoutHistory.value = _workoutHistory.value + WorkoutHistoryEntry(System.currentTimeMillis(), vol, _totalSeconds.value, _activeExercises.value)
        if (!saveAsTemplateName.isNullOrBlank()) {
            val existing = _savedTemplates.value.find { it.templateName == saveAsTemplateName }
            if (existing != null) _savedTemplates.value = _savedTemplates.value.map { if (it.id == existing.id) it.copy(exercises = _activeExercises.value) else it }
            else _savedTemplates.value = _savedTemplates.value + WorkoutTemplate((_savedTemplates.value.maxOfOrNull { it.id } ?: 0) + 1, saveAsTemplateName, _activeExercises.value)
        }
        saveData(); _activeExercises.value = emptyList()
    }

    fun createCustomExercise(name: String) { if (name.isNotBlank() && _exerciseLibrary.value.none { it.name == name }) { _exerciseLibrary.value = _exerciseLibrary.value + ExerciseDef(name); saveData() } }

    fun addNewExerciseBlock(exerciseName: String) {
        val firstPrev = getLastPerformedSets(exerciseName)?.firstOrNull()
        val s = WorkoutSetData(1, "1", if (firstPrev != null && firstPrev.kg.isNotBlank()) "${firstPrev.kg}kg x ${firstPrev.reps}" else "-", firstPrev?.kg ?: "", firstPrev?.reps ?: "", "")
        _activeExercises.value = _activeExercises.value + ExerciseSessionData((_activeExercises.value.maxOfOrNull { it.id } ?: 0) + 1, exerciseName, restTimerDuration = getLastRestTimer(exerciseName), sets = listOf(s))
    }

    fun deleteExercise(exerciseId: Int) { _activeExercises.value = _activeExercises.value.filter { it.id != exerciseId } }
    fun updateExerciseNote(exerciseId: Int, newNote: String) { _activeExercises.value = _activeExercises.value.map { if (it.id == exerciseId) it.copy(note = newNote) else it } }
    fun swapExercises(from: Int, to: Int) { val c = _activeExercises.value.toMutableList(); val t = c[from]; c[from] = c[to]; c[to] = t; _activeExercises.value = c }
    fun deleteSet(eId: Int, sId: Int) { _activeExercises.value = _activeExercises.value.map { ex -> if (ex.id == eId) { var c = 1; ex.copy(sets = ex.sets.filter { it.id != sId }.map { if (it.isWarmup) it else it.copy(setLabel = (c++).toString()) }) } else ex } }
    fun updateSet(eId: Int, s: WorkoutSetData) { _activeExercises.value = _activeExercises.value.map { ex -> if (ex.id == eId) ex.copy(sets = ex.sets.map { if (it.id == s.id) s else it }) else ex } }
    fun toggleSetComplete(eId: Int, s: WorkoutSetData) { _activeExercises.value = _activeExercises.value.map { ex -> if (ex.id == eId) { if (!s.isCompleted) _restTimerSeconds.value = ex.restTimerDuration; ex.copy(sets = ex.sets.map { if (it.id == s.id) it.copy(isCompleted = !s.isCompleted) else it }) } else ex } }
    fun toggleWarmup(eId: Int, sId: Int) { _activeExercises.value = _activeExercises.value.map { ex -> if (ex.id == eId) { var c = 1; ex.copy(sets = ex.sets.map { if (it.id == sId) it.copy(isWarmup = !it.isWarmup) else it }.map { if (it.isWarmup) it.copy(setLabel = "W") else it.copy(setLabel = (c++).toString()) }) } else ex } }
    fun addSet(eId: Int) { _activeExercises.value = _activeExercises.value.map { ex -> if (ex.id == eId) { val p = getLastPerformedSets(ex.name)?.getOrNull(ex.sets.size); ex.copy(sets = ex.sets + WorkoutSetData((ex.sets.maxOfOrNull { it.id } ?: 0) + 1, (ex.sets.count { !it.isWarmup } + 1).toString(), if (p != null && p.kg.isNotBlank()) "${p.kg}kg x ${p.reps}" else ex.sets.lastOrNull()?.previousText ?: "-", p?.kg ?: ex.sets.lastOrNull()?.kg ?: "", p?.reps ?: ex.sets.lastOrNull()?.reps ?: "", "")) } else ex } }
    fun updateExerciseRestTime(eId: Int, s: Int) { _activeExercises.value = _activeExercises.value.map { if (it.id == eId) it.copy(restTimerDuration = s) else it } }
    fun adjustRestTimer(a: Int) { _restTimerSeconds.value = maxOf(0, _restTimerSeconds.value + a) }
    fun skipRestTimer() { _restTimerSeconds.value = 0 }

    fun getChartData(): List<Float> {
        val volumes = _workoutHistory.value.takeLast(7).map { it.totalVolume.toFloat() }
        if (volumes.isEmpty()) return listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f)
        val max = volumes.maxOrNull() ?: 1f
        return volumes.map { it / max }
    }
}