package com.ateszk0.ostromgep.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ateszk0.ostromgep.NotificationHelper
import com.ateszk0.ostromgep.WorkoutAction
import com.ateszk0.ostromgep.WorkoutEventBus
import com.ateszk0.ostromgep.data.WorkoutRepository
import com.ateszk0.ostromgep.model.*
import com.ateszk0.ostromgep.utils.FileHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    enum class GeneratorStatus { IDLE, LOADING, SUCCESS, ERROR_KEY, ERROR_GENERIC }

    private val repository = WorkoutRepository(application)

    private val _totalSeconds = MutableStateFlow(0)
    val totalSeconds = _totalSeconds.asStateFlow()

    private val _restTimerSeconds = MutableStateFlow(0)
    val restTimerSeconds = _restTimerSeconds.asStateFlow()

    private val _exerciseLibrary = MutableStateFlow<List<ExerciseDef>>(emptyList())
    val exerciseLibrary = _exerciseLibrary.asStateFlow()

    private val _savedTemplates = MutableStateFlow(repository.getSavedTemplates())
    val savedTemplates = _savedTemplates.asStateFlow()

    private val _routineFolders = MutableStateFlow(repository.getRoutineFolders())
    val routineFolders = _routineFolders.asStateFlow()

    private val _activeFolderId = MutableStateFlow(repository.getActiveFolderId())
    val activeFolderId = _activeFolderId.asStateFlow()

    private val _workoutHistory = MutableStateFlow(repository.getWorkoutHistory())
    val workoutHistory = _workoutHistory.asStateFlow()

    private val _activeExercises = MutableStateFlow<List<ExerciseSessionData>>(emptyList())
    val activeExercises = _activeExercises.asStateFlow()

    private val _activeTemplateId = MutableStateFlow<Int?>(null)
    val activeTemplateId = _activeTemplateId.asStateFlow()

    private var lastInteractedExerciseId: Int? = null

    private val _overloadPrompts = MutableStateFlow<List<OverloadPrompt>>(emptyList())
    val overloadPrompts = _overloadPrompts.asStateFlow()

    private val _bodyWeightHistory = MutableStateFlow(repository.getBodyWeightHistory())
    val bodyWeightHistory = _bodyWeightHistory.asStateFlow()

    private val _username = MutableStateFlow(repository.getUsername())
    val username: StateFlow<String> = _username.asStateFlow()

    private val _appTheme = MutableStateFlow(repository.getTheme())
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    private val _appLanguage = MutableStateFlow(repository.getLanguage())
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _profilePictureUri = MutableStateFlow(repository.getProfilePictureUri())
    val profilePictureUri = _profilePictureUri.asStateFlow()

    private val _latestBodyWeightKg = MutableStateFlow(repository.getBodyWeightHistory().maxByOrNull { it.timestamp }?.weightKg ?: 0.0)
    val latestBodyWeightKg: StateFlow<Double> = _latestBodyWeightKg.asStateFlow()

    // UI state for workout session (ViewModel-level so it survives config changes)
    private val _isWorkoutActive = MutableStateFlow(false)
    val isWorkoutActive: StateFlow<Boolean> = _isWorkoutActive.asStateFlow()

    private val _isWorkoutMinimized = MutableStateFlow(false)
    val isWorkoutMinimized: StateFlow<Boolean> = _isWorkoutMinimized.asStateFlow()

    // --- AI Generator State ---
    private val _geminiApiKey = MutableStateFlow(repository.getGeminiApiKey())
    val geminiApiKey: StateFlow<String?> = _geminiApiKey.asStateFlow()

    private val _generatorStatus = MutableStateFlow(GeneratorStatus.IDLE)
    val generatorStatus: StateFlow<GeneratorStatus> = _generatorStatus.asStateFlow()

    companion object {
        val BODYWEIGHT_EXERCISES = setOf(
            "Pull Up", "Pull-up", 
            "Triceps Dip", "Tricep Dip",
            "Chest Dip", "Dips (Chest)", "Dips",
            "Push Up", "Push-up"
        )
    }

    init {
        _exerciseLibrary.value = repository.getExerciseLibrary().sortedBy { it.name }
        
        viewModelScope.launch {
            // Update latestBodyWeight whenever bodyWeightHistory changes
            _bodyWeightHistory.collect { history ->
                _latestBodyWeightKg.value = history.maxByOrNull { it.timestamp }?.weightKg ?: 0.0
            }
        }

        // Restore active workout from persistent storage (survives process death)
        val savedState = repository.getActiveWorkoutState()
        if (savedState != null) {
            _activeExercises.value = savedState.exercises
            _activeTemplateId.value = savedState.templateId
            _totalSeconds.value = savedState.totalSeconds
            _restTimerSeconds.value = 0 // Don't restore rest timer
            _isWorkoutActive.value = true
        }

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
                    if (_restTimerSeconds.value > 0) {
                        _restTimerSeconds.value -= 1
                        if (_restTimerSeconds.value == 0) {
                            try {
                                android.media.RingtoneManager.getRingtone(application, android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))?.play()
                            } catch (e: Exception) {}
                        }
                    }
                    val targetEx = targetExerciseForNotification()
                    NotificationHelper.updateNotification(application, targetEx, _restTimerSeconds.value)
                    // Persist every 5 seconds to avoid excessive I/O
                    if (_totalSeconds.value % 5 == 0) {
                        persistActiveWorkout()
                    }
                }
            }
        }
    }

    fun setTheme(theme: String) { _appTheme.value = theme; repository.saveTheme(theme) }

    private val _generatorErrorMessage = MutableStateFlow<String?>(null)
    val generatorErrorMessage: StateFlow<String?> = _generatorErrorMessage.asStateFlow()

    fun saveGeminiApiKey(key: String) {
        repository.saveGeminiApiKey(key)
        _geminiApiKey.value = key
        _generatorStatus.value = GeneratorStatus.IDLE
    }

    fun generateWorkoutPlan(trainingDays: Int, customPrompt: String, availableEquipments: Set<Equipment> = Equipment.entries.toSet()) {
        val key = _geminiApiKey.value ?: return
        viewModelScope.launch {
            _generatorStatus.value = GeneratorStatus.LOADING
            try {
                val routines = WorkoutGenerator(getApplication()).generateRoutines(key, trainingDays, customPrompt, availableEquipments.toList())
                routines.forEach { saveNewTemplate(it.templateName, it.exercises) }
                _generatorStatus.value = GeneratorStatus.SUCCESS
            } catch (e: InvalidApiKeyException) {
                _generatorErrorMessage.value = "Invalid API Key."
                _generatorStatus.value = GeneratorStatus.ERROR_KEY
            } catch (e: Exception) {
                e.printStackTrace()
                _generatorErrorMessage.value = e.message ?: "Unknown error"
                _generatorStatus.value = GeneratorStatus.ERROR_GENERIC
            }
        }
    }

    fun resetGeneratorStatus() { 
        _generatorStatus.value = GeneratorStatus.IDLE 
        _generatorErrorMessage.value = null
    }

    fun setWorkoutActive(active: Boolean) {
        _isWorkoutActive.value = active
        if (!active) {
            _isWorkoutMinimized.value = false
        }
    }

    fun setWorkoutMinimized(minimized: Boolean) {
        _isWorkoutMinimized.value = minimized
    }

    private fun persistActiveWorkout() {
        repository.saveActiveWorkoutState(
            com.ateszk0.ostromgep.data.WorkoutRepository.ActiveWorkoutState(
                exercises = _activeExercises.value,
                templateId = _activeTemplateId.value,
                totalSeconds = _totalSeconds.value,
                restTimerSeconds = _restTimerSeconds.value
            )
        )
    }

    fun updateUsername(name: String) {
        _username.value = name
        repository.saveUsername(name)
    }

    fun updateProfilePictureUri(uri: String) {
        val app = getApplication<android.app.Application>()
        val localUri = if (uri.startsWith("content://")) {
            com.ateszk0.ostromgep.utils.FileHelper.copyImageToInternalStorage(app, android.net.Uri.parse(uri)) ?: uri
        } else uri
        _profilePictureUri.value = localUri
        repository.saveProfilePictureUri(localUri)
    }

    fun setLanguage(lang: String) { 
        _appLanguage.value = lang
        repository.saveLanguage(lang)
    }

    fun deleteTemplate(templateId: Int) { 
        val newTemplates = _savedTemplates.value.filter { it.id != templateId }
        _savedTemplates.value = newTemplates
        repository.saveSavedTemplates(newTemplates)
    }

    private fun getLastPerformedSets(exerciseName: String) = _workoutHistory.value.flatMap { it.exercises }.find { it.name == exerciseName }?.sets
    private fun getLastRestTimer(exerciseName: String) = _workoutHistory.value.flatMap { it.exercises }.find { it.name == exerciseName }?.restTimerDuration ?: 90

    fun updateExerciseDetails(name: String, min: Int, max: Int, imageUri: String?, muscleGroups: List<MuscleGroup>, equipment: Equipment) {
        val app = getApplication<Application>()
        val localImgUri = if (imageUri?.startsWith("content://") == true) {
            FileHelper.copyImageToInternalStorage(app, android.net.Uri.parse(imageUri)) ?: imageUri
        } else imageUri

        val current = _exerciseLibrary.value.toMutableList()
        val index = current.indexOfFirst { it.name == name }
        if (index != -1) {
            current[index] = current[index].copy(minReps = min, maxReps = max, imageUri = localImgUri, muscleGroups = muscleGroups, equipment = equipment)
        } else {
            current.add(ExerciseDef(name = name, minReps = min, maxReps = max, imageUri = localImgUri, muscleGroups = muscleGroups, equipment = equipment, isCustom = true))
        }
        _exerciseLibrary.value = current
        repository.saveExerciseLibrary(current)
    }

    fun startEmptyWorkout() {
        _activeTemplateId.value = null
        _activeExercises.value = emptyList()
        _totalSeconds.value = 0
        _restTimerSeconds.value = 0
        _isWorkoutActive.value = true
        persistActiveWorkout()
    }

    fun startWorkoutFromTemplate(template: WorkoutTemplate) {
        _activeTemplateId.value = template.id
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
        _isWorkoutActive.value = true
        persistActiveWorkout()

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

    fun finishWorkout(customName: String? = null, updateOriginalRoutine: Boolean = false) {
        val bw = _latestBodyWeightKg.value
        val vol = _activeExercises.value.sumOf { ex ->
            if (ex.name in BODYWEIGHT_EXERCISES) {
                ex.sets.sumOf { set ->
                    if (set.isCompleted && !set.isWarmup) {
                        val extraKg = set.kg.toDoubleOrNull() ?: 0.0
                        (bw + extraKg) * (set.reps.toIntOrNull() ?: 0)
                    } else 0.0
                }
            } else {
                ex.totalVolume()
            }
        }
        var finalName = customName
        if (finalName.isNullOrBlank()) {
            val activeId = _activeTemplateId.value
            if (activeId != null) {
                val template = _savedTemplates.value.find { it.id == activeId }
                if (template != null) finalName = template.templateName
            }
        }
        val newHistory = (_workoutHistory.value + WorkoutHistoryEntry(System.currentTimeMillis(), vol, _totalSeconds.value, _activeExercises.value, finalName))
            .sortedByDescending { it.timestamp }
        _workoutHistory.value = newHistory
        repository.saveWorkoutHistory(newHistory)

        if (updateOriginalRoutine && _activeTemplateId.value != null) {
            val updatedTemplates = _savedTemplates.value.map {
                if (it.id == _activeTemplateId.value) it.copy(exercises = _activeExercises.value) else it
            }
            _savedTemplates.value = updatedTemplates
            repository.saveSavedTemplates(updatedTemplates)
        }
        
        _activeExercises.value = emptyList()
        _activeTemplateId.value = null
        repository.clearActiveWorkoutState()
        _isWorkoutActive.value = false
        _isWorkoutMinimized.value = false
    }

    fun discardWorkout() {
        _activeExercises.value = emptyList()
        _activeTemplateId.value = null
        _totalSeconds.value = 0
        _restTimerSeconds.value = 0
        _isWorkoutActive.value = false
        _isWorkoutMinimized.value = false
        repository.clearActiveWorkoutState()
    }

    fun getRoutineChanges(): RoutineChanges? {
        val activeId = _activeTemplateId.value ?: return null
        val originalTemplate = _savedTemplates.value.find { it.id == activeId } ?: return null
        
        var addedExercises = 0
        var removedExercises = 0
        var addedSets = 0
        var removedSets = 0
        
        val originalNames = originalTemplate.exercises.map { it.name }.toSet()
        val currentExercises = _activeExercises.value
        val currentNames = currentExercises.map { it.name }.toSet()
        
        addedExercises += (currentNames - originalNames).size
        removedExercises += (originalNames - currentNames).size
        
        val commonNames = originalNames.intersect(currentNames)
        for (name in commonNames) {
            val origSetCount = originalTemplate.exercises.find { it.name == name }?.sets?.count { !it.isWarmup } ?: 0
            val currSetCount = currentExercises.find { it.name == name }?.sets?.count { !it.isWarmup } ?: 0
            if (currSetCount > origSetCount) {
                addedSets += (currSetCount - origSetCount)
            } else if (origSetCount > currSetCount) {
                removedSets += (origSetCount - currSetCount)
            }
        }
        
        return RoutineChanges(
            templateName = originalTemplate.templateName,
            addedExercises = addedExercises,
            removedExercises = removedExercises,
            addedSets = addedSets,
            removedSets = removedSets
        )
    }

    fun saveNewTemplate(name: String, exercises: List<ExerciseSessionData>) {
        val id = (_savedTemplates.value.maxOfOrNull { it.id } ?: 0) + 1
        // 1. normalize() fixes any nulls that Gson may inject into non-null Kotlin fields
        //    (e.g. `sets`, `note`, `rpe`) when fields are missing in the JSON.
        // 2. Renumber IDs so that explore-page templates (which all use IDs 1, 2, 3)
        //    get unique IDs and don't cause duplicate-key crashes in the Compose LazyColumn.
        val normalizedExercises = exercises.mapIndexed { exIdx, ex ->
            val safe = ex.normalize()
            val renumberedSets = safe.sets.mapIndexed { setIdx, set ->
                set.copy(id = setIdx + 1)
            }
            safe.copy(id = exIdx + 1, sets = renumberedSets)
        }
        val updated = _savedTemplates.value + WorkoutTemplate(id, name, normalizedExercises)
        _savedTemplates.value = updated
        repository.saveSavedTemplates(updated)
    }

    fun createFolder(name: String, templateIds: List<Int>) {
        val newFolder = RoutineFolder(name = name, templateIds = templateIds)
        val updated = _routineFolders.value + newFolder
        _routineFolders.value = updated
        repository.saveRoutineFolders(updated)
    }

    fun updateFolder(id: String, name: String, templateIds: List<Int>) {
        val updated = _routineFolders.value.map { if (it.id == id) it.copy(name = name, templateIds = templateIds) else it }
        _routineFolders.value = updated
        repository.saveRoutineFolders(updated)
    }

    fun deleteFolder(id: String) {
        val updated = _routineFolders.value.filter { it.id != id }
        _routineFolders.value = updated
        repository.saveRoutineFolders(updated)
        if (_activeFolderId.value == id) {
            setActiveFolder(null)
        }
    }

    fun setActiveFolder(id: String?) {
        _activeFolderId.value = id
        repository.saveActiveFolderId(id)
    }

    fun createCustomExercise(name: String) { 
        if (name.isNotBlank() && _exerciseLibrary.value.none { it.name == name }) { 
            val newLib = _exerciseLibrary.value + ExerciseDef(name, isCustom = true)
            _exerciseLibrary.value = newLib
            repository.saveExerciseLibrary(newLib)
        } 
    }

    fun deleteCustomExercise(name: String) {
        val newLib = _exerciseLibrary.value.filter { it.name != name }
        _exerciseLibrary.value = newLib
        repository.saveExerciseLibrary(newLib)
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

    fun replaceExercise(exerciseId: Int, newName: String) {
        val prevSets = getLastPerformedSets(newName)
        _activeExercises.value = _activeExercises.value.map { ex ->
            if (ex.id != exerciseId) return@map ex
            val sets = ex.sets.mapIndexed { i, s ->
                val p = prevSets?.getOrNull(i)
                s.copy(
                    isCompleted = false,
                    kg = p?.kg ?: "",
                    reps = p?.reps ?: s.reps,
                    rpe = "",
                    previousText = if (p != null && p.kg.isNotBlank()) "${p.kg}kg x ${p.reps}" else "-"
                )
            }
            ex.copy(name = newName, sets = sets, restTimerDuration = getLastRestTimer(newName))
        }
        persistActiveWorkout()
    }
    
    fun pairSuperset(sourceId: Int, targetIds: List<Int>) {
        if (targetIds.isEmpty()) return
        val allIdsToPair = listOf(sourceId) + targetIds
        val currentList = _activeExercises.value.toMutableList()
        val existingSupersetId = allIdsToPair.mapNotNull { id -> currentList.find { it.id == id }?.supersetId }.firstOrNull()
        val newSupersetId = existingSupersetId ?: java.util.UUID.randomUUID().toString()
        
        val itemsToGroup = currentList.filter { it.id in allIdsToPair }.map { it.copy(supersetId = newSupersetId) }
        val newList = mutableListOf<ExerciseSessionData>()
        var inserted = false
        
        for (item in currentList) {
            if (item.id in allIdsToPair) {
                if (!inserted) {
                    newList.addAll(itemsToGroup)
                    inserted = true
                }
            } else {
                newList.add(item)
            }
        }
        _activeExercises.value = newList
    }

    fun removeSuperset(exerciseId: Int) {
        _activeExercises.value = _activeExercises.value.map { 
            if (it.id == exerciseId) it.copy(supersetId = null) else it 
        }
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

    fun importCsvHistory(context: android.content.Context, uri: android.net.Uri): Boolean {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return false
            val reader = java.io.BufferedReader(java.io.InputStreamReader(inputStream))
            val lines = reader.readLines()
            if (lines.isEmpty()) return false
            
            // Expected headers: title,"start_time","end_time","description","exercise_title","superset_id","exercise_notes","set_index","set_type","weight_kg","reps","distance_km","duration_seconds","rpe"
            val workoutsMap = mutableMapOf<String, MutableList<String>>() // Key: start_time, Value: List of rows
            
            // Skip header (index 0)
            for (i in 1 until lines.size) {
                val row = lines[i]
                if (row.isBlank()) continue
                // Very basic split by comma ignoring quotes, usually standard parsers are better
                // For simplicity, assuming no commas inside the values except maybe dates which are quoted.
                // A regex to split by comma outside quotes:
                val tokens = row.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.replace("\"", "") }
                if (tokens.size >= 11) {
                    val startTime = tokens[1]
                    workoutsMap.getOrPut(startTime) { mutableListOf() }.add(row)
                }
            }
            
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.ENGLISH)
            val newEntries = mutableListOf<WorkoutHistoryEntry>()
            
            for ((startTimeStr, rows) in workoutsMap) {
                val timestamp = try { sdf.parse(startTimeStr)?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
                
                // Group by exercise_title and superset_id
                // Row format assumed: title [0], start_time [1], end_time [2], description [3], exercise_title [4], superset_id [5], 
                // exercise_notes [6], set_index [7], set_type [8], weight_kg [9], reps [10], distance_km [11], duration_seconds [12], rpe [13]
                
                val exercisesMap = mutableMapOf<Pair<String, String>, MutableList<List<String>>>()
                for (row in rows) {
                    val tokens = row.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.replace("\"", "") }
                    if (tokens.size < 11) continue
                    val exTitle = tokens[4]
                    val supersetId = tokens[5].takeIf { it.isNotBlank() } ?: "none"
                    exercisesMap.getOrPut(exTitle to supersetId) { mutableListOf() }.add(tokens)
                }
                
                val finalExercises = mutableListOf<ExerciseSessionData>()
                var exIdCounter = 1
                for ((key, setTokensList) in exercisesMap) {
                    val sets = mutableListOf<WorkoutSetData>()
                    var setIdCounter = 1
                    for (t in setTokensList.sortedBy { it[7].toIntOrNull() ?: 0 }) {
                        val isWarmup = t[8].equals("warmup", ignoreCase = true) || t[8].equals("warm up", ignoreCase = true)
                        sets.add(
                            WorkoutSetData(
                                id = setIdCounter++,
                                setLabel = if (isWarmup) "W" else (setIdCounter - 1).toString(),
                                kg = t[9],
                                reps = t[10],
                                rpe = if (t.size > 13) t[13] else "",
                                isCompleted = true,
                                isWarmup = isWarmup
                            )
                        )
                    }
                    val trueSupersetId = if (key.second == "none") null else key.second
                    finalExercises.add(
                        ExerciseSessionData(
                            id = exIdCounter++,
                            name = key.first,
                            supersetId = trueSupersetId,
                            sets = sets
                        )
                    )
                }
                
                val totalVol = finalExercises.sumOf { it.totalVolume() }
                val startMs = timestamp
                var endMs = startMs + 3600000 // default 1 hr
                var csvTitle: String? = null
                if (rows.isNotEmpty()) {
                    val firstRowTokens = rows[0].split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.replace("\"", "") }
                    if (firstRowTokens.isNotEmpty()) csvTitle = firstRowTokens[0]
                    if (firstRowTokens.size > 2) {
                        try { endMs = sdf.parse(firstRowTokens[2])?.time ?: endMs } catch (e: Exception) {}
                    }
                }
                val durationSec = ((endMs - startMs) / 1000).coerceAtLeast(0).toInt()
                
                newEntries.add(WorkoutHistoryEntry(timestamp, totalVol, durationSec, finalExercises, csvTitle))
            }
            
            if (newEntries.isNotEmpty()) {
                val combined = (_workoutHistory.value + newEntries).sortedByDescending { it.timestamp }
                _workoutHistory.value = combined
                repository.saveWorkoutHistory(combined)
                return true
            }
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun deleteWorkoutHistory(timestamp: Long) {
        val updated = _workoutHistory.value.filter { it.timestamp != timestamp }
        _workoutHistory.value = updated
        repository.saveWorkoutHistory(updated)
    }

    fun copyWorkoutAsNewActive(entry: WorkoutHistoryEntry) {
        _activeTemplateId.value = null
        val newExercises = entry.exercises.mapIndexed { exIdx, ex ->
            val renumberedSets = ex.sets.mapIndexed { setIdx, set ->
                set.copy(
                    id = setIdx + 1,
                    isCompleted = false,
                    rpe = "",
                    previousText = if (set.kg.isNotBlank()) "${set.kg}kg x ${set.reps}" else "-"
                )
            }
            ex.copy(id = exIdx + 1, sets = renumberedSets)
        }
        _activeExercises.value = newExercises
        _totalSeconds.value = 0
        _restTimerSeconds.value = 0
        _isWorkoutActive.value = true
        persistActiveWorkout()
    }

    fun saveHistoryEntryAsRoutine(entry: WorkoutHistoryEntry, name: String) {
        saveNewTemplate(name, entry.exercises)
    }

    fun exportWorkoutsAsCsv(context: android.content.Context, uri: android.net.Uri): Boolean {
        return try {
            val outputStream = context.contentResolver.openOutputStream(uri) ?: return false
            val writer = java.io.BufferedWriter(java.io.OutputStreamWriter(outputStream))
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.ENGLISH)
            // Write header
            writer.write("\"title\",\"start_time\",\"end_time\",\"description\",\"exercise_title\",\"superset_id\",\"exercise_notes\",\"set_index\",\"set_type\",\"weight_kg\",\"reps\",\"distance_km\",\"duration_seconds\",\"rpe\"")
            writer.newLine()
            _workoutHistory.value.sortedByDescending { it.timestamp }.forEach { entry ->
                val startTime = sdf.format(java.util.Date(entry.timestamp))
                val endTime = sdf.format(java.util.Date(entry.timestamp + entry.durationSeconds * 1000L))
                
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = entry.timestamp }
                val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                val defaultTitle = when (hour) {
                    in 5..11 -> "Morning workout \uD83C\uDFCB\uFE0F"
                    in 12..16 -> "Afternoon workout \uD83C\uDFCB\uFE0F"
                    in 17..21 -> "Evening workout \uD83C\uDFCB\uFE0F"
                    else -> "Night workout \uD83C\uDFCB\uFE0F"
                }
                val title = entry.name ?: defaultTitle
                
                entry.exercises.forEachIndexed { exIdx, ex ->
                    ex.sets.forEachIndexed { setIdx, set ->
                        val setType = if (set.isWarmup) "warmup" else "normal"
                        val supersetStr = if (ex.supersetId.isNullOrBlank()) "" else "\"${ex.supersetId}\""
                        val weightStr = set.kg.takeIf { it.isNotBlank() } ?: ""
                        val repsStr = set.reps.takeIf { it.isNotBlank() } ?: ""
                        val rpeStr = set.rpe.takeIf { it.isNotBlank() } ?: ""
                        
                        writer.write("\"$title\",\"$startTime\",\"$endTime\",\"\",\"${ex.name}\",$supersetStr,\"${ex.note}\",$setIdx,\"$setType\",$weightStr,$repsStr,,,$rpeStr")
                        writer.newLine()
                    }
                }
            }
            writer.flush()
            writer.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun targetExerciseForNotification(): ExerciseSessionData? {
        val exercises = _activeExercises.value
        if (exercises.isEmpty()) return null

        // Priority 1: Check the last interacted exercise and its superset group
        if (lastInteractedExerciseId != null) {
            val lastEx = exercises.find { it.id == lastInteractedExerciseId }
            if (lastEx != null) {
                val supersetId = lastEx.supersetId
                val group = if (supersetId != null) exercises.filter { it.supersetId == supersetId } else listOf(lastEx)
                val uncompletedInGroup = group.find { it.sets.any { s -> !s.isCompleted } }
                if (uncompletedInGroup != null) return uncompletedInGroup
            }
        }
        
        // Priority 2: Return first exercise in order that has empty sets
        val firstUncompleted = exercises.find { it.sets.any { s -> !s.isCompleted } }
        if (firstUncompleted != null) return firstUncompleted
        
        // Priority 3: Return last interacted or last in list
        return exercises.find { it.id == lastInteractedExerciseId } ?: exercises.lastOrNull()
    }

    private fun updateExercise(eId: Int, updater: (ExerciseSessionData) -> ExerciseSessionData) {
        lastInteractedExerciseId = eId
        _activeExercises.value = _activeExercises.value.map { if (it.id == eId) updater(it) else it }
    }
    
    fun adjustRestTimer(a: Int) { _restTimerSeconds.value = maxOf(0, _restTimerSeconds.value + a) }
    fun skipRestTimer() { _restTimerSeconds.value = 0 }

    fun getVolumeChartData(): List<Pair<String, Float>> {
        val sdf = java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault())
        // Aggregate volume per calendar day
        val dailyVolume = mutableMapOf<String, Float>()
        val dateKeyFormat = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
        _workoutHistory.value.forEach { entry ->
            val dayKey = dateKeyFormat.format(java.util.Date(entry.timestamp))
            val label = sdf.format(java.util.Date(entry.timestamp))
            val vol = entry.totalVolume.toFloat()
            dailyVolume[dayKey] = (dailyVolume[dayKey] ?: 0f) + vol
            // Store label alongside (use dayKey -> Pair)
            // We'll handle label separately below
        }
        // Sort by date and take last 7 unique days
        val sortedEntries = _workoutHistory.value
            .groupBy { dateKeyFormat.format(java.util.Date(it.timestamp)) }
            .map { (dayKey, entries) ->
                val label = sdf.format(java.util.Date(entries.first().timestamp))
                val totalVol = entries.sumOf { it.totalVolume }.toFloat()
                Triple(dayKey, label, totalVol)
            }
            .sortedBy { it.first }
            .takeLast(7)
        return sortedEntries.map { it.second to it.third }
    }

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
        val activeFolderId = _activeFolderId.value
        val validTemplates = if (activeFolderId != null) {
            val folder = _routineFolders.value.find { it.id == activeFolderId }
            val folderTemplates = folder?.templateIds?.mapNotNull { id -> _savedTemplates.value.find { it.id == id } }
            if (folderTemplates.isNullOrEmpty()) _savedTemplates.value else folderTemplates
        } else {
            _savedTemplates.value
        }
        
        if (validTemplates.isEmpty()) return null
        
        val templateLastUsed = validTemplates.associateWith { t ->
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

    fun getWorkedMusclesLast7Days(): Set<MuscleGroup> {
        val now = System.currentTimeMillis()
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
        val workedMuscles = mutableSetOf<MuscleGroup>()

        _workoutHistory.value.filter { now - it.timestamp <= sevenDaysMs }.forEach { entry ->
            entry.exercises.forEach { exSession ->
                val def = _exerciseLibrary.value.find { it.name == exSession.name }
                def?.muscleGroups?.forEach { mg ->
                    workedMuscles.add(mg)
                }
            }
        }
        return workedMuscles
    }

    fun getWorkedDaysLast7Days(): List<Boolean> {
        val nowMs = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        val booleanList = mutableListOf<Boolean>()
        
        // We want the last 7 days including today (index 6 is today, 0 is 6 days ago)
        val todayStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        for (i in 6 downTo 0) {
            val startOfDay = todayStart - (i * dayMs)
            val endOfDay = startOfDay + dayMs
            val hasWorkout = _workoutHistory.value.any { it.timestamp in startOfDay until endOfDay }
            booleanList.add(hasWorkout)
        }
        return booleanList
    }
}