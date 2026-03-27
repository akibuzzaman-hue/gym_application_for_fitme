package com.ateszk0.ostromgep.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import com.ateszk0.ostromgep.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.ateszk0.ostromgep.model.ExerciseSessionData
import com.ateszk0.ostromgep.model.WorkoutSetData
import com.ateszk0.ostromgep.model.Equipment
import com.ateszk0.ostromgep.viewmodel.WorkoutViewModel
import com.ateszk0.ostromgep.ui.theme.*
import com.ateszk0.ostromgep.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorScreen(viewModel: WorkoutViewModel, themeColor: Color, onBack: () -> Unit) {
    val library by viewModel.exerciseLibrary.collectAsState()
    
    var routineName by remember { mutableStateOf("") }
    var draftExercises by remember { mutableStateOf(emptyList<ExerciseSessionData>()) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = true) {
        if (showBottomSheet || showCreateDialog) {
            showBottomSheet = false
            showCreateDialog = false
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text(stringResource(R.string.routine_editor_title), color = Color.White) }, 
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.Close, null, tint = Color.White) }
                },
                actions = { 
                    Button(
                        onClick = { 
                            if (draftExercises.isNotEmpty() && routineName.isNotBlank()) {
                                viewModel.saveNewTemplate(routineName, draftExercises)
                                onBack()
                            }
                        }, 
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                    ) { 
                        Icon(Icons.Default.Save, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.save_btn), color = Color.White) 
                    } 
                }, 
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            ) 
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().background(DarkBackground).padding(innerPadding)) {
            item {
                OutlinedTextField(
                    value = routineName,
                    onValueChange = { routineName = it },
                    label = { Text(stringResource(R.string.routine_name_label), color = TextGray) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.default_workout_name), color = TextGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColor,
                        unfocusedBorderColor = SurfaceDark
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            itemsIndexed(draftExercises) { index, exercise ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val def = library.find { it.name == exercise.name }
                                if (def?.imageUri != null) {
                                    coil.compose.AsyncImage(
                                        model = def.imageUri,
                                        contentDescription = null,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp))
                                    )
                                } else {
                                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(exercise.name, color = themeColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            IconButton(onClick = { draftExercises = draftExercises.filterIndexed { i, _ -> i != index } }) {
                                Icon(Icons.Default.Delete, null, tint = Color.Red)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.set_label_short), color = TextGray, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text(stringResource(R.string.target_reps_label), color = TextGray, fontSize = 12.sp, modifier = Modifier.weight(2f), textAlign = TextAlign.Center)
                            Text(stringResource(R.string.del_label_short), color = TextGray, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }
                        
                        exercise.sets.forEachIndexed { sIndex, set ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${sIndex + 1}", color = Color.White, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                
                                Box(modifier = Modifier.weight(2f), contentAlignment = Alignment.Center) {
                                    OutlinedTextField(
                                        value = set.reps,
                                        onValueChange = { newReps -> 
                                            draftExercises = draftExercises.mapIndexed { i, ex -> 
                                                if (i == index) {
                                                    ex.copy(sets = ex.sets.mapIndexed { j, s -> if (j == sIndex) s.copy(reps = newReps) else s })
                                                } else ex 
                                            }
                                        },
                                        modifier = Modifier.width(80.dp).height(48.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, textAlign = TextAlign.Center),
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                    )
                                }
                                
                                IconButton(
                                    onClick = { 
                                        draftExercises = draftExercises.mapIndexed { i, ex -> 
                                            if (i == index) ex.copy(sets = ex.sets.filterIndexed { j, _ -> j != sIndex }) else ex 
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Close, null, tint = TextGray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        
                        Text(
                            "+ " + stringResource(R.string.add_set_label), 
                            color = themeColor, 
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBackground)
                                .clickable { 
                                    draftExercises = draftExercises.mapIndexed { i, ex -> 
                                        if (i == index) {
                                            val newSet = WorkoutSetData(id = (ex.sets.maxOfOrNull { it.id } ?: 0) + 1, setLabel = "1", previousText = "-", kg = "", reps = "10", rpe = "")
                                            ex.copy(sets = ex.sets + newSet)
                                        } else ex 
                                    }
                                }
                                .padding(vertical = 12.dp), 
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            item {
                Button(
                    onClick = { showBottomSheet = true }, 
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp), 
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = themeColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_exercise_to_routine), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
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
                            val newSession = ExerciseSessionData(
                                id = (draftExercises.maxOfOrNull { it.id } ?: 0) + 1,
                                name = exDef.name,
                                restTimerDuration = 90,
                                sets = listOf(WorkoutSetData(id = 1, setLabel = "1", previousText = "-", kg = "", reps = "10", rpe = ""))
                            )
                            draftExercises = draftExercises + newSession
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
                    val newSession = ExerciseSessionData(
                        id = (draftExercises.maxOfOrNull { it.id } ?: 0) + 1,
                        name = name,
                        restTimerDuration = 90,
                        sets = listOf(WorkoutSetData(id = 1, setLabel = "1", previousText = "-", kg = "", reps = "10", rpe = ""))
                    )
                    draftExercises = draftExercises + newSession
                    showCreateDialog = false
                    showBottomSheet = false
                }
            )
        }
    }
}
