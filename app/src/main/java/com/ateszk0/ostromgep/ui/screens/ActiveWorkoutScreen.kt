package com.ateszk0.ostromgep.ui.screens

import android.media.RingtoneManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.ateszk0.ostromgep.viewmodel.WorkoutViewModel
import com.ateszk0.ostromgep.ui.theme.*
import com.ateszk0.ostromgep.ui.components.*
import com.ateszk0.ostromgep.model.Equipment
import androidx.compose.ui.res.stringResource
import com.ateszk0.ostromgep.R
import com.ateszk0.ostromgep.model.WorkoutSetData

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ActiveWorkoutScreen(viewModel: WorkoutViewModel, themeColor: Color, onFinishWorkout: () -> Unit, onMinimize: () -> Unit = {}) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val exercises by viewModel.activeExercises.collectAsState()
    val library by viewModel.exerciseLibrary.collectAsState()
    val prompts by viewModel.overloadPrompts.collectAsState()
    val latestBodyWeight by viewModel.latestBodyWeightKg.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var isSavingWorkout by remember { mutableStateOf(false) }
    var isSimpleMode by remember { mutableStateOf(false) }
    var showPlateCalculator by remember { mutableStateOf(false) }
    var exerciseToEditRepRange by remember { mutableStateOf<String?>(null) }
    var supersetSourceExercise by remember { mutableStateOf<com.ateszk0.ostromgep.model.ExerciseSessionData?>(null) }
    var rpeTarget by remember { mutableStateOf<Pair<Int, WorkoutSetData>?>(null) }
    
    androidx.activity.compose.BackHandler(enabled = true) {
        if (showBottomSheet || showCreateDialog || showPlateCalculator || rpeTarget != null) {
            showBottomSheet = false
            showCreateDialog = false
            showPlateCalculator = false
            rpeTarget = null
        } else {
            onMinimize()
        }
    }
    val totalVolume = exercises.sumOf { ex ->
        if (ex.name in com.ateszk0.ostromgep.viewmodel.WorkoutViewModel.BODYWEIGHT_EXERCISES) {
            ex.sets.sumOf { set ->
                if (set.isCompleted && !set.isWarmup) {
                    val extraKg = set.kg.toDoubleOrNull() ?: 0.0
                    (latestBodyWeight + extraKg) * (set.reps.toIntOrNull() ?: 0)
                } else 0.0
            }
        } else {
            ex.totalVolume()
        }
    }
    val completedSetsCount = exercises.sumOf { it.countCompletedSets() }

    // O(1) index lookup map to avoid O(n) indexOf per list item during scroll
    val exerciseIndexMap = remember(exercises) { exercises.mapIndexed { i, ex -> ex.id to i }.toMap() }
    // O(1) image lookup map to avoid O(n) library.find per list item during scroll
    val libraryImageMap = remember(library) { library.associate { it.name to it.imageUri } }

    val groupedExercises = remember(exercises) {
        val groups = mutableListOf<List<com.ateszk0.ostromgep.model.ExerciseSessionData>>()
        var currentGroup = mutableListOf<com.ateszk0.ostromgep.model.ExerciseSessionData>()
        for (ex in exercises) {
            if (currentGroup.isEmpty()) {
                currentGroup.add(ex)
            } else {
                if (ex.supersetId != null && ex.supersetId == currentGroup.last().supersetId) {
                    currentGroup.add(ex)
                } else {
                    groups.add(currentGroup)
                    currentGroup = mutableListOf(ex)
                }
            }
        }
        if (currentGroup.isNotEmpty()) groups.add(currentGroup)
        groups
    }

    if (isSavingWorkout) {
        SaveWorkoutScreen(
            viewModel = viewModel,
            themeColor = themeColor,
            onSave = { updateRoutine ->
                viewModel.finishWorkout(updateOriginalRoutine = updateRoutine)
                onFinishWorkout()
            },
            onDiscard = {
                viewModel.discardWorkout()
                onFinishWorkout()
            },
            onBack = { isSavingWorkout = false }
        )
    } else {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        topBar = { 
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onMinimize) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize", tint = Color.White)
                    }
                },
                title = { Text(stringResource(R.string.log_workout), color = Color.White, fontSize = 20.sp, maxLines = 1) }, 
                actions = { 
                    IconButton(onClick = { isSimpleMode = !isSimpleMode }) { 
                        Icon(if (isSimpleMode) Icons.Default.List else Icons.Default.Fullscreen, null, tint = themeColor) 
                    }
                    IconButton(onClick = { showPlateCalculator = true }) { Icon(Icons.Default.Calculate, null, tint = themeColor) }
                    Button(onClick = { isSavingWorkout = true }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text(stringResource(R.string.finish_btn), color = Color.White) } 
                }, 
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground),
                scrollBehavior = scrollBehavior
            ) 
        },
        bottomBar = {
            RestTimerBottomBar(viewModel, themeColor, isSimpleMode)
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().background(DarkBackground).padding(innerPadding)) {
            WorkoutStatsHeader(viewModel, totalVolume, completedSetsCount)

            if (isSimpleMode) {
                SimpleWorkoutView(viewModel, themeColor) { ex, set ->
                    rpeTarget = ex.id to set
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), state = androidx.compose.foundation.lazy.rememberLazyListState()) {
                    items(groupedExercises, key = { grp -> grp.first().id }) { group ->
                val isSuperset = group.size > 1 && group.first().supersetId != null
                Column(
                    modifier = Modifier.fillMaxWidth().run {
                        if (isSuperset) this.border(2.dp, Color(0xFF9C27B0), RoundedCornerShape(8.dp)).padding(2.dp) else this
                    }
                ) {
                    group.forEachIndexed { idxInGroup, exercise ->
                        val index = exerciseIndexMap[exercise.id] ?: 0
                                ExerciseBlock(
                                exercise, libraryImageMap[exercise.name], index, exercises.size, themeColor,
                                { viewModel.moveExerciseUp(index) },
                                { viewModel.moveExerciseDown(index) },
                                { s -> viewModel.updateSet(exercise.id, s) },
                                { s -> haptic.performHapticFeedback(HapticFeedbackType.LongPress); viewModel.toggleSetComplete(exercise.id, s) },
                                { viewModel.addSet(exercise.id) },
                                { id -> viewModel.deleteSet(exercise.id, id) },
                                { sec -> viewModel.updateExerciseRestTime(exercise.id, sec) },
                                { id -> viewModel.toggleWarmup(exercise.id, id) },
                                { note -> viewModel.updateExerciseNote(exercise.id, note) },
                                { viewModel.deleteExercise(exercise.id) },
                                { exerciseToEditRepRange = exercise.name },
                                { supersetSourceExercise = exercise },
                                { viewModel.removeSuperset(exercise.id) },
                                { set -> rpeTarget = exercise.id to set },
                                bodyweightKg = if (exercise.name in com.ateszk0.ostromgep.viewmodel.WorkoutViewModel.BODYWEIGHT_EXERCISES) latestBodyWeight else null
                            )
                        if (idxInGroup < group.size - 1 && !isSuperset) {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            item {
                Button(onClick = { showBottomSheet = true }, modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = themeColor)) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_exercise_btn), color = Color.White)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        }
        }

        if (prompts.isNotEmpty()) {
            ProgressiveOverloadDialog(prompts, themeColor, { p -> viewModel.applyOverloadPrompts(p) }, { viewModel.dismissOverloadPrompts() })
        }
        if (showPlateCalculator) PlateCalculatorDialog({ showPlateCalculator = false }, themeColor)



        exerciseToEditRepRange?.let { n ->
            val def = library.find { it.name == n } ?: com.ateszk0.ostromgep.model.ExerciseDef(n)
            ExerciseEditDialog(
                def, themeColor,
                { exerciseToEditRepRange = null },
                { name, min, max, imgUri, muscles, equip -> 
                    viewModel.updateExerciseDetails(name, min, max, imgUri, muscles, equip)
                    exerciseToEditRepRange = null 
                }
            )
        }
        
        rpeTarget?.let { (exerciseId, set) ->
            RpeSelectionSheet(
                themeColor = themeColor,
                currentRpe = set.rpe,
                onRpeSelected = { newRpe ->
                    viewModel.updateSet(exerciseId, set.copy(rpe = newRpe))
                    rpeTarget = null
                },
                onDismiss = { rpeTarget = null }
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showBottomSheet,
            enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }),
            exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.fillMaxSize().zIndex(10f)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                    AddExerciseContent(
                        library = library,
                        recentExercises = emptyList(),
                        onExerciseSelected = { exDef ->
                            viewModel.addNewExerciseBlock(exDef.name)
                            showBottomSheet = false
                        },
                        onClose = { showBottomSheet = false },
                        onCreateCustom = { showCreateDialog = true },
                        themeColor = themeColor
                    )
                }
            }
        }

        if (showCreateDialog) {
            com.ateszk0.ostromgep.ui.components.CreateExerciseDialog(
                themeColor = themeColor,
                onDismiss = { showCreateDialog = false },
                onSave = { name, min, max, imgUri, muscles, equip ->
                    viewModel.updateExerciseDetails(name, min, max, imgUri, muscles, equip)
                    viewModel.addNewExerciseBlock(name)
                    showCreateDialog = false
                    showBottomSheet = false
                }
            )
        }
        
        if (supersetSourceExercise != null) {
            var selectedTargets by remember { mutableStateOf(setOf<Int>()) }
            val sourceId = supersetSourceExercise!!.id
            val otherExercises = exercises.filter { it.id != sourceId }
            
            AlertDialog(
                onDismissRequest = { supersetSourceExercise = null },
                title = { Text("Create Superset", color = Color.White) },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(otherExercises) { ex ->
                            Row(modifier = Modifier.fillMaxWidth().clickable {
                                selectedTargets = if (selectedTargets.contains(ex.id)) selectedTargets - ex.id else selectedTargets + ex.id
                            }, verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selectedTargets.contains(ex.id), 
                                    onCheckedChange = { chk ->
                                        selectedTargets = if (chk) selectedTargets + ex.id else selectedTargets - ex.id
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = themeColor)
                                )
                                Text(ex.name, color = Color.White)
                            }
                        }
                        if (otherExercises.isEmpty()) {
                            item { Text("No other exercises in workout.", color = TextGray) }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { 
                        viewModel.pairSuperset(sourceId, selectedTargets.toList())
                        supersetSourceExercise = null
                    }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) {
                        Text(stringResource(R.string.save_btn), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { supersetSourceExercise = null }) { Text(stringResource(R.string.cancel_btn), color = themeColor) }
                }
            )
        }
    }
    }

}

@Composable
fun WorkoutStatsHeader(viewModel: WorkoutViewModel, totalVolume: Double, completedSetsCount: Int) {
    val totalSeconds by viewModel.totalSeconds.collectAsState()
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        StatItem(stringResource(R.string.duration_label), "%02d:%02d".format(totalSeconds/60, totalSeconds%60))
        StatItem(stringResource(R.string.volume_label), "${totalVolume.toInt()} kg")
        StatItem(stringResource(R.string.sets_label), "$completedSetsCount")
    }
}

@Composable
fun RestTimerBottomBar(viewModel: WorkoutViewModel, themeColor: Color, isSimpleMode: Boolean) {
    if (isSimpleMode) return
    val restTimerSeconds by viewModel.restTimerSeconds.collectAsState()
    if (restTimerSeconds > 0) {
        Row(
            modifier = Modifier.fillMaxWidth().background(themeColor).imePadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimerAdjustButton("-15") { viewModel.adjustRestTimer(-15) }
                Text(
                    "%02d:%02d".format(restTimerSeconds/60, restTimerSeconds%60),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                TimerAdjustButton("+15") { viewModel.adjustRestTimer(15) }
            }
            Button(
                onClick = { viewModel.skipRestTimer() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.2f))
            ) { Text(stringResource(R.string.skip_btn), color = Color.White) }
        }
    }
}
