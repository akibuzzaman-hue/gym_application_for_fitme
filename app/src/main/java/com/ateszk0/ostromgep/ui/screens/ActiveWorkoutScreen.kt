package com.ateszk0.ostromgep.ui.screens

import android.media.RingtoneManager
import androidx.compose.foundation.background
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
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var showPlateCalculator by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var exerciseToEditRepRange by remember { mutableStateOf<String?>(null) }
    
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

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Log Workout", color = Color.White) }, 
                actions = { 
                    IconButton(onClick = { showPlateCalculator = true }) { Icon(Icons.Default.Calculate, null, tint = themeColor) }
                    Button(onClick = { showSaveTemplateDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) { Text("Finish", color = Color.White) } 
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
                        Button(onClick = { viewModel.skipRestTimer() }, colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.2f))) { Text("Skip", color = Color.White) } 
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().background(DarkBackground).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(onClick = { showSettingsDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)) { Text("Settings", color = Color.White) }
                    Button(onClick = { showDiscardDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)) { Text("Discard", color = Color.Red) }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().background(DarkBackground).padding(innerPadding)) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem("Duration", "%02d:%02d".format(totalSeconds/60, totalSeconds%60))
                    StatItem("Volume", "${totalVolume.toInt()} kg")
                    StatItem("Sets", "$completedSetsCount")
                }
            }
            itemsIndexed(exercises, key = { _, exercise -> exercise.id }) { index, exercise ->
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
                        { exerciseToEditRepRange = exercise.name }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            item {
                Button(onClick = { showBottomSheet = true }, modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = themeColor)) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Exercise", color = Color.White)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (prompts.isNotEmpty()) {
            ProgressiveOverloadDialog(prompts, themeColor, { p -> viewModel.applyOverloadPrompts(p) }, { viewModel.dismissOverloadPrompts() })
        }
        if (showSettingsDialog) SettingsDialog(viewModel, themeColor) { showSettingsDialog = false }
        if (showPlateCalculator) PlateCalculatorDialog({ showPlateCalculator = false }, themeColor)

        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                title = { Text("Biztosan elveted az edzést?", color = Color.White) },
                text = { Text("Minden eddigi adatod elveszik.", color = TextGray) },
                confirmButton = {
                    Button(onClick = { 
                        viewModel.finishWorkout(null)
                        showDiscardDialog = false
                        onFinishWorkout() 
                    }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                        Text("Elvetés", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardDialog = false }) { Text("Mégse", color = themeColor) }
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

        if (showSaveTemplateDialog) {
            var n by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showSaveTemplateDialog = false },
                title = { Text("Befejezés", color = Color.White) },
                text = { OutlinedTextField(value = n, onValueChange = { n = it }, label = { Text("Sablon neve") }) },
                confirmButton = {
                    Button(onClick = { viewModel.finishWorkout(if (n.isNotBlank()) n else null); showSaveTemplateDialog = false; onFinishWorkout() }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) {
                        Text("Befejezés", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveTemplateDialog = false }) {
                        Text("Mégse", color = themeColor)
                    }
                }
            )
        }
    }

}
