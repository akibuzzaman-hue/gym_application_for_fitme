package com.ateszk0.ostromgep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ateszk0.ostromgep.model.ExerciseSessionData
import com.ateszk0.ostromgep.model.WorkoutSetData
import com.ateszk0.ostromgep.viewmodel.WorkoutViewModel

val DarkBackground = Color(0xFF000000)
val SurfaceDark = Color(0xFF1C1C1E)
val PrimaryBlue = Color(0xFF0A84FF)
val CompletedGreen = Color(0xFF2E7D32)
val TextGray = Color(0xFF8E8E93)
val InputBackground = Color(0xFF2C2C2E)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = DarkBackground, surface = SurfaceDark)) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    OstromgepApp()
                }
            }
        }
    }
}

@Composable
fun OstromgepApp() {
    var isWorkoutActive by remember { mutableStateOf(false) }
    val workoutViewModel: WorkoutViewModel = viewModel()

    if (isWorkoutActive) {
        ActiveWorkoutScreen(
            viewModel = workoutViewModel,
            onFinishWorkout = { isWorkoutActive = false }
        )
    } else {
        HomeScreen(
            viewModel = workoutViewModel,
            onStartEmptyWorkout = {
                workoutViewModel.startEmptyWorkout()
                isWorkoutActive = true
            },
            onStartTemplate = { template ->
                workoutViewModel.startWorkoutFromTemplate(template)
                isWorkoutActive = true
            }
        )
    }
}

@Composable
fun HomeScreen(
    viewModel: WorkoutViewModel,
    onStartEmptyWorkout: () -> Unit,
    onStartTemplate: (com.ateszk0.ostromgep.model.WorkoutTemplate) -> Unit
) {
    val templates by viewModel.savedTemplates.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(modifier = Modifier.height(32.dp))
        Text("OSTROMGÉP", fontSize = 32.sp, fontWeight = FontWeight.Black, color = PrimaryBlue)
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStartEmptyWorkout,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Text("QUICK START (ÜRES EDZÉS)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Saját Sablonok", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        if (templates.isEmpty()) {
            Text("Még nincs mentett sablonod. Indíts egy edzést, és a végén elmentheted!", color = TextGray)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(templates) { template ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onStartTemplate(template) },
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Text(template.templateName, modifier = Modifier.padding(16.dp), fontSize = 18.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    viewModel: WorkoutViewModel,
    onFinishWorkout: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val exercises by viewModel.activeExercises.collectAsState()
    val totalSeconds by viewModel.totalSeconds.collectAsState()
    val restTimerSeconds by viewModel.restTimerSeconds.collectAsState()
    val library by viewModel.exerciseLibrary.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var showNewExerciseDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val totalVolume = exercises.flatMap { it.sets }.filter { it.isCompleted }.sumOf { (it.kg.toDoubleOrNull() ?: 0.0) * (it.reps.toIntOrNull() ?: 0) }
    val completedSetsCount = exercises.flatMap { it.sets }.count { it.isCompleted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Workout", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                actions = {
                    Text(text = "⏱️", fontSize = 20.sp, modifier = Modifier.padding(end = 16.dp))
                    Button(
                        onClick = { showSaveTemplateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier.padding(end = 8.dp).height(36.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) {
                        Text("Finish", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        bottomBar = {
            Column {
                if (restTimerSeconds > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(PrimaryBlue).padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TimerAdjustButton("-15", onClick = { viewModel.adjustRestTimer(-15) })
                            Text(
                                text = "%02d:%02d".format(restTimerSeconds / 60, restTimerSeconds % 60),
                                color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            TimerAdjustButton("+15", onClick = { viewModel.adjustRestTimer(15) })
                        }
                        Button(
                            onClick = { viewModel.skipRestTimer() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Skip", color = Color.White)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().background(DarkBackground).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = { /* TODO: Settings */ }, colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)) {
                        Text("Settings", color = Color.White)
                    }
                    Button(
                        onClick = {
                            viewModel.finishWorkout(null)
                            onFinishWorkout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)
                    ) {
                        Text("Discard Workout", color = Color.Red)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(DarkBackground).padding(innerPadding)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem("Duration", "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60))
                    StatItem("Volume", "${totalVolume.toInt()} kg")
                    StatItem("Sets", "$completedSetsCount")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(exercises, key = { _, exercise -> exercise.id }) { index, exercise ->
                ExerciseBlock(
                    exercise = exercise,
                    index = index,
                    totalExercises = exercises.size,
                    onMoveExercise = { from, to -> viewModel.swapExercises(from, to) },
                    onSetUpdate = { updatedSet -> viewModel.updateSet(exercise.id, updatedSet) },
                    onSetCompleteToggle = { set ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleSetComplete(exercise.id, set)
                    },
                    onAddSet = { viewModel.addSet(exercise.id) },
                    onDeleteSet = { setId -> viewModel.deleteSet(exercise.id, setId) },
                    onUpdateRestTime = { seconds -> viewModel.updateExerciseRestTime(exercise.id, seconds) }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Button(
                    onClick = { showBottomSheet = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Exercise", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = SurfaceDark
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Válassz gyakorlatot", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showNewExerciseDialog = true }) {
                        Text("+ Új saját", color = PrimaryBlue)
                    }
                }
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(library) { exName ->
                        ListItem(
                            headlineContent = { Text(exName, color = Color.White, fontSize = 18.sp) },
                            modifier = Modifier.clickable {
                                viewModel.addNewExerciseBlock(exName)
                                showBottomSheet = false
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showSaveTemplateDialog) {
            var templateName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showSaveTemplateDialog = false },
                title = { Text("Edzés befejezése") },
                text = {
                    Column {
                        Text("Szeretnéd elmenteni az edzést sablonként is?")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = templateName,
                            onValueChange = { templateName = it },
                            label = { Text("Sablon neve (opcionális)") }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.finishWorkout(if (templateName.isNotBlank()) templateName else null)
                        showSaveTemplateDialog = false
                        onFinishWorkout()
                    }) { Text("Mentés és Befejezés") }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveTemplateDialog = false }) { Text("Mégse") }
                }
            )
        }

        if (showNewExerciseDialog) {
            var customExName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showNewExerciseDialog = false },
                title = { Text("Saját gyakorlat") },
                text = {
                    OutlinedTextField(
                        value = customExName,
                        onValueChange = { customExName = it },
                        label = { Text("Gyakorlat neve") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.createCustomExercise(customExName)
                        showNewExerciseDialog = false
                    }) { Text("Hozzáadás") }
                },
                dismissButton = {
                    TextButton(onClick = { showNewExerciseDialog = false }) { Text("Mégse") }
                }
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column {
        Text(text = label, color = TextGray, fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TimerAdjustButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.2f)).clickable { onClick() }.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CustomTextField(value: String, onValueChange: (String) -> Unit, isCompleted: Boolean) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(PrimaryBlue),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (isCompleted) Color.Transparent else InputBackground).padding(vertical = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseBlock(
    exercise: ExerciseSessionData,
    index: Int,
    totalExercises: Int,
    onMoveExercise: (Int, Int) -> Unit,
    onSetUpdate: (WorkoutSetData) -> Unit,
    onSetCompleteToggle: (WorkoutSetData) -> Unit,
    onAddSet: () -> Unit,
    onDeleteSet: (Int) -> Unit,
    onUpdateRestTime: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var showRestPickerDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount.y
                            if (dragOffset > 150f && index < totalExercises - 1) {
                                onMoveExercise(index, index + 1)
                                dragOffset = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } else if (dragOffset < -150f && index > 0) {
                                onMoveExercise(index, index - 1)
                                dragOffset = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onDragEnd = { dragOffset = 0f }
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = exercise.name, color = PrimaryBlue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .clickable { showRestPickerDialog = true }
                        .background(SurfaceDark, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Timer, contentDescription = "Timer", tint = TextGray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${exercise.restTimerDuration}s", color = TextGray, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextGray)
            }
        }

        Text(text = exercise.note, color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("SET", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.1f), textAlign = TextAlign.Center)
            Text("PREVIOUS", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.3f), textAlign = TextAlign.Center)
            Text("KG", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center)
            Text("REPS", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center)
            Text("RPE", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.15f), textAlign = TextAlign.Center)
            Text("✔", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.1f), textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(4.dp))

        exercise.sets.forEach { set ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = {
                    if (it == SwipeToDismissBoxValue.EndToStart) {
                        onDeleteSet(set.id)
                        true
                    } else false
                }
            )

            key(set.id) {
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    enableDismissFromEndToStart = exercise.sets.size > 1,
                    backgroundContent = {
                        val color by animateColorAsState(if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) Color.Red else Color.Transparent, label = "swipe")
                        Box(Modifier.fillMaxSize().background(color).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterEnd) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                        }
                    },
                    content = {
                        val rowBg = if (set.isCompleted) CompletedGreen.copy(alpha = 0.2f) else DarkBackground
                        Row(modifier = Modifier.fillMaxWidth().background(rowBg).padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = set.setLabel, color = if (set.isWarmup) Color(0xFFE6C229) else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.1f), textAlign = TextAlign.Center)
                            Text(text = set.previousText, color = TextGray, fontSize = 12.sp, modifier = Modifier.weight(0.3f), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Box(modifier = Modifier.weight(0.2f).padding(horizontal = 4.dp)) { CustomTextField(value = set.kg, onValueChange = { onSetUpdate(set.copy(kg = it)) }, isCompleted = set.isCompleted) }
                            Box(modifier = Modifier.weight(0.2f).padding(horizontal = 4.dp)) { CustomTextField(value = set.reps, onValueChange = { onSetUpdate(set.copy(reps = it)) }, isCompleted = set.isCompleted) }
                            Box(modifier = Modifier.weight(0.15f).padding(horizontal = 4.dp)) { CustomTextField(value = set.rpe, onValueChange = { onSetUpdate(set.copy(rpe = it)) }, isCompleted = set.isCompleted) }
                            Box(modifier = Modifier.weight(0.1f).aspectRatio(1f).clip(RoundedCornerShape(4.dp)).background(if (set.isCompleted) CompletedGreen else InputBackground).clickable { onSetCompleteToggle(set) }, contentAlignment = Alignment.Center) {
                                if (set.isCompleted) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "+ Add Set",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceDark).clickable { onAddSet() }.padding(vertical = 12.dp),
            textAlign = TextAlign.Center
        )
    }

    if (showRestPickerDialog) {
        var tempTime by remember { mutableStateOf(exercise.restTimerDuration.toString()) }
        AlertDialog(
            onDismissRequest = { showRestPickerDialog = false },
            title = { Text("Pihenőidő (másodperc)") },
            text = {
                OutlinedTextField(
                    value = tempTime,
                    onValueChange = { tempTime = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateRestTime(tempTime.toIntOrNull() ?: 90)
                    showRestPickerDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showRestPickerDialog = false }) { Text("Mégse") }
            }
        )
    }
}