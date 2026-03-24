package com.ateszk0.ostromgep.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ateszk0.ostromgep.R
import com.ateszk0.ostromgep.model.WorkoutHistoryEntry
import com.ateszk0.ostromgep.ui.components.ExerciseBlock
import com.ateszk0.ostromgep.ui.theme.DarkBackground
import com.ateszk0.ostromgep.ui.theme.SurfaceDark
import com.ateszk0.ostromgep.ui.theme.TextGray
import com.ateszk0.ostromgep.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutLogScreen(
    viewModel: WorkoutViewModel,
    themeColor: Color,
    onBack: () -> Unit,
    onNavigateToActiveWorkout: () -> Unit,
    initialEntryTimestamp: Long? = null
) {
    val history by viewModel.workoutHistory.collectAsState()
    val library by viewModel.exerciseLibrary.collectAsState()

    var selectedEntry by remember { mutableStateOf<WorkoutHistoryEntry?>(null) }
    var entryOptionsToShow by remember { mutableStateOf<WorkoutHistoryEntry?>(null) }
    var saveRoutineDialogEntry by remember { mutableStateOf<WorkoutHistoryEntry?>(null) }

    LaunchedEffect(history, initialEntryTimestamp) {
        if (initialEntryTimestamp != null && selectedEntry == null) {
            selectedEntry = history.find { it.timestamp == initialEntryTimestamp }
        } else if (selectedEntry != null) {
            val updated = history.find { it.timestamp == selectedEntry?.timestamp }
            if (updated == null) { // Deleted
                selectedEntry = null
            } else {
                selectedEntry = updated
            }
        }
    }

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd · EEEE", Locale.getDefault()) }

    if (saveRoutineDialogEntry != null) {
        var routineName by remember { mutableStateOf(saveRoutineDialogEntry?.exercises?.firstOrNull()?.name ?: "Routine") }
        AlertDialog(
            onDismissRequest = { saveRoutineDialogEntry = null },
            title = { Text(stringResource(R.string.save_as_routine), color = Color.White) },
            text = {
                OutlinedTextField(
                    value = routineName,
                    onValueChange = { routineName = it },
                    label = { Text(stringResource(R.string.routine_name_label)) },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (routineName.isNotBlank()) {
                            viewModel.saveHistoryEntryAsRoutine(saveRoutineDialogEntry!!, routineName)
                            saveRoutineDialogEntry = null
                            if (entryOptionsToShow == saveRoutineDialogEntry) entryOptionsToShow = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                ) {
                    Text(stringResource(R.string.save_btn), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { saveRoutineDialogEntry = null }) {
                    Text(stringResource(R.string.cancel_btn), color = themeColor)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedEntry != null) stringResource(R.string.workout_detail_title)
                        else stringResource(R.string.workout_log_screen_title),
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedEntry != null && initialEntryTimestamp == null) {
                            selectedEntry = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (selectedEntry != null) {
                        IconButton(onClick = { entryOptionsToShow = selectedEntry }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        if (entryOptionsToShow != null) {
            ModalBottomSheet(
                onDismissRequest = { entryOptionsToShow = null },
                containerColor = SurfaceDark
            ) {
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.copy_workout), color = Color.White) },
                        modifier = Modifier.clickable {
                            viewModel.copyWorkoutAsNewActive(entryOptionsToShow!!)
                            entryOptionsToShow = null
                            onNavigateToActiveWorkout()
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.save_as_routine), color = Color.White) },
                        modifier = Modifier.clickable {
                            saveRoutineDialogEntry = entryOptionsToShow
                            entryOptionsToShow = null
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.delete_workout), color = Color.Red) },
                        modifier = Modifier.clickable {
                            viewModel.deleteWorkoutHistory(entryOptionsToShow!!.timestamp)
                            entryOptionsToShow = null
                            if (selectedEntry != null && initialEntryTimestamp != null) {
                                onBack() // Exit detail if deep linked and deleted
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }

        if (selectedEntry != null) {
            val entry = selectedEntry!!
            Column(modifier = Modifier.fillMaxSize().background(DarkBackground).padding(padding)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(dateFormatter.format(Date(entry.timestamp)), color = TextGray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column {
                            Text(stringResource(R.string.time_label), color = TextGray, fontSize = 12.sp)
                            Text("${entry.durationSeconds / 60}m ${entry.durationSeconds % 60}s", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(stringResource(R.string.volume_label), color = TextGray, fontSize = 12.sp)
                            Text("${entry.totalVolume.toInt()} kg", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(stringResource(R.string.sets_label), color = TextGray, fontSize = 12.sp)
                            Text("${entry.exercises.sumOf { it.countCompletedSets() }}", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    val groupedExercises = entry.exercises.groupBy { it.supersetId }
                    var displayIndex = 0
                    groupedExercises.forEach { (supersetId, exList) ->
                        val isSuperset = supersetId != null
                        item {
                            if (isSuperset) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.width(4.dp).height(24.dp).background(Color(0xFF9C27B0), RoundedCornerShape(2.dp)))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Superset", color = Color(0xFF9C27B0), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                exList.forEachIndexed { i, ex ->
                                    val def = library.find { it.name == ex.name }
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        ExerciseBlock(
                                            exercise = ex,
                                            imageUri = def?.imageUri,
                                            index = displayIndex++,
                                            total = entry.exercises.size,
                                            themeColor = themeColor,
                                            onMoveUp = { },
                                            onMoveDown = { },
                                            onSetUpdate = { },
                                            onSetCompleteToggle = { },
                                            onAddSet = { },
                                            onDeleteSet = { },
                                            onUpdateRestTime = { },
                                            onToggleWarmup = { },
                                            onNoteUpdate = { },
                                            onDeleteExercise = { },
                                            onEditRepRange = { },
                                            onSuperset = { },
                                            onRemoveSuperset = { },
                                            onRpeClick = { }
                                        )
                                        // Invisible overlay to intercept clicks and make it read-only
                                        Box(modifier = Modifier.matchParentSize().clickable(enabled = false) { }.background(Color.Transparent))
                                    }
                                    if (i < exList.size - 1) Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                viewModel.copyWorkoutAsNewActive(entry)
                                onNavigateToActiveWorkout()
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.copy_workout), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_workouts_yet), color = TextGray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(DarkBackground).padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(history.sortedByDescending { it.timestamp }) { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedEntry = entry },
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(stringResource(R.string.workout_log_title), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
                                        Text(dateFormatter.format(Date(entry.timestamp)), color = TextGray, fontSize = 12.sp)
                                    }
                                    IconButton(
                                        onClick = { entryOptionsToShow = entry },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.MoreVert, null, tint = TextGray)
                                    }
                                }
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)) {
                                    Column {
                                        Text(stringResource(R.string.time_label), color = TextGray, fontSize = 12.sp)
                                        Text("${entry.durationSeconds / 60}m", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text(stringResource(R.string.volume_label), color = TextGray, fontSize = 12.sp)
                                        Text("${entry.totalVolume.toInt()} kg", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }

                                entry.exercises.take(3).forEach { ex ->
                                    val count = ex.countCompletedSets()
                                    Text(stringResource(R.string.sets_format, count, ex.name), color = TextGray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                                }
                                if (entry.exercises.size > 3) {
                                    Text(
                                        stringResource(R.string.more_exercises_format, entry.exercises.size - 3),
                                        color = themeColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
