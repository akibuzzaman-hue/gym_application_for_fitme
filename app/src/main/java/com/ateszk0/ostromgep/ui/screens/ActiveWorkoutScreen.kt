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
import androidx.compose.material.icons.filled.Calculate
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
import androidx.compose.ui.zIndex
import com.ateszk0.ostromgep.viewmodel.WorkoutViewModel
import com.ateszk0.ostromgep.ui.theme.*
import com.ateszk0.ostromgep.ui.components.*
import androidx.compose.ui.res.stringResource
import com.ateszk0.ostromgep.R

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ActiveWorkoutScreen(viewModel: WorkoutViewModel, themeColor: Color, onFinishWorkout: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val exercises by viewModel.activeExercises.collectAsState()
    val totalSeconds by viewModel.totalSeconds.collectAsState()
    val restTimerSeconds by viewModel.restTimerSeconds.collectAsState()
    val library by viewModel.exerciseLibrary.collectAsState()
    val prompts by viewModel.overloadPrompts.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
    var isSavingWorkout by remember { mutableStateOf(false) }
    var showPlateCalculator by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var exerciseToEditRepRange by remember { mutableStateOf<String?>(null) }
    var supersetSourceExercise by remember { mutableStateOf<com.ateszk0.ostromgep.model.ExerciseSessionData?>(null) }
    
    androidx.activity.compose.BackHandler(enabled = true) {
        showDiscardDialog = true
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val totalVolume = exercises.sumOf { it.totalVolume() }
    val completedSetsCount = exercises.sumOf { it.countCompletedSets() }

    var lastTimerValue by remember { mutableIntStateOf(0) }
    LaunchedEffect(restTimerSeconds) { 
        if (lastTimerValue > 0 && restTimerSeconds == 0) { 
            try { 
                RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))?.play() 
            } catch (e: Exception) {} 
        }
        lastTimerValue = restTimerSeconds 
    }

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

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
    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text(stringResource(R.string.log_workout), color = Color.White) }, 
                actions = { 
                    IconButton(onClick = { showPlateCalculator = true }) { Icon(Icons.Default.Calculate, null, tint = themeColor) }
                    Button(onClick = { isSavingWorkout = true }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text(stringResource(R.string.finish_btn), color = Color.White) } 
                }, 
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            ) 
        },
        bottomBar = {
            Column {
                if (restTimerSeconds > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(themeColor).padding(16.dp), 
                        verticalAlignment = Alignment.CenterVertically, 
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) { 
                        Row { 
                            TimerAdjustButton("-15") { viewModel.adjustRestTimer(-15) }
                            Text("%02d:%02d".format(restTimerSeconds/60, restTimerSeconds%60), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                            TimerAdjustButton("+15") { viewModel.adjustRestTimer(15) } 
                        }
                        Button(onClick = { viewModel.skipRestTimer() }, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.2f))) { Text(stringResource(R.string.skip_btn), color = Color.White) } 
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().background(DarkBackground).padding(16.dp), horizontalArrangement = Arrangement.End) {
                    Button(onClick = { showDiscardDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)) { Text(stringResource(R.string.discard_btn), color = Color.Red) }
                }
            }
        }
    ) { innerPadding ->
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

        LazyColumn(modifier = Modifier.fillMaxSize().background(DarkBackground).padding(innerPadding)) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem(stringResource(R.string.duration_label), "%02d:%02d".format(totalSeconds/60, totalSeconds%60))
                    StatItem(stringResource(R.string.volume_label), "${totalVolume.toInt()} kg")
                    StatItem(stringResource(R.string.sets_label), "$completedSetsCount")
                }
            }

            items(groupedExercises, key = { grp -> grp.first().id }) { group ->
                val isSuperset = group.size > 1 && group.first().supersetId != null
                Column(
                    modifier = Modifier.fillMaxWidth().run {
                        if (isSuperset) this.border(2.dp, Color(0xFF9C27B0), RoundedCornerShape(8.dp)).padding(2.dp) else this
                    }
                ) {
                    group.forEachIndexed { idxInGroup, exercise ->
                        val index = exercises.indexOf(exercise)
                        val isDragged = draggedIndex == index
                        val zIndex = if (isDragged) 1f else 0f
                        val scale = if (isDragged) 1.02f else 1f
                        val alpha = if (isDragged) 0.8f else 1f
                        val translationY = if (isDragged) dragOffset else 0f
                        val elevation = if (isDragged) 8.dp else 0.dp
                        Box(
                            modifier = Modifier.fillMaxWidth().zIndex(zIndex).graphicsLayer { this.translationY = translationY; this.scaleX = scale; this.scaleY = scale; this.alpha = alpha }
                            .shadow(elevation, RoundedCornerShape(8.dp))
                            .background(if (isDragged) SurfaceDark.copy(0.5f) else Color.Transparent)
                            .animateItemPlacement()
                        ) {
                            val def = library.find { it.name == exercise.name }
                            ExerciseBlock(
                                exercise, def?.imageUri, index, exercises.size, themeColor,
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
                                { viewModel.removeSuperset(exercise.id) }
                            )
                        }
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

        if (prompts.isNotEmpty()) {
            ProgressiveOverloadDialog(prompts, themeColor, { p -> viewModel.applyOverloadPrompts(p) }, { viewModel.dismissOverloadPrompts() })
        }
        if (showPlateCalculator) PlateCalculatorDialog({ showPlateCalculator = false }, themeColor)

        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                title = { Text(stringResource(R.string.discard_dialog_title), color = Color.White) },
                text = { Text(stringResource(R.string.discard_dialog_text), color = TextGray) },
                confirmButton = {
                    Button(onClick = { 
                        viewModel.discardWorkout()
                        showDiscardDialog = false
                        onFinishWorkout() 
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                        Text(stringResource(R.string.discard_confirm), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardDialog = false }) { Text(stringResource(R.string.cancel_btn), color = themeColor) }
                }
            )
        }

        exerciseToEditRepRange?.let { n ->
            val def = library.find { it.name == n } ?: com.ateszk0.ostromgep.model.ExerciseDef(n)
            ExerciseEditDialog(
                def, themeColor,
                { exerciseToEditRepRange = null },
                { name, min, max, imgUri, muscles -> 
                    viewModel.updateExerciseDetails(name, min, max, imgUri, muscles)
                    exerciseToEditRepRange = null 
                }
            )
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = Color.Black,
                modifier = Modifier.fillMaxHeight(0.95f)
            ) {
                AddExerciseContent(
                    library = library,
                    recentExercises = emptyList(),
                    onExerciseSelected = { exDef ->
                        viewModel.addNewExerciseBlock(exDef.name)
                        showBottomSheet = false
                    },
                    onClose = { showBottomSheet = false },
                    themeColor = themeColor
                )
            }
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
