package com.ateszk0.ostromgep.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ateszk0.ostromgep.R
import com.ateszk0.ostromgep.model.RoutineChanges
import com.ateszk0.ostromgep.ui.theme.DarkBackground
import com.ateszk0.ostromgep.ui.theme.SurfaceDark
import com.ateszk0.ostromgep.ui.theme.TextGray
import com.ateszk0.ostromgep.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveWorkoutScreen(
    viewModel: WorkoutViewModel,
    themeColor: Color,
    onSave: (String?, Boolean) -> Unit,
    onDiscard: () -> Unit,
    onBack: () -> Unit
) {
    val totalSeconds by viewModel.totalSeconds.collectAsState()
    val exercises by viewModel.activeExercises.collectAsState()

    val totalVolume = exercises.sumOf { it.totalVolume() }
    val completedSetsCount = exercises.sumOf { it.countCompletedSets() }

    var workoutTitle by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val sdf = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val currentTimeStr = remember { sdf.format(Date()) }

    var showChangesSheet by remember { mutableStateOf(false) }
    var routineChanges by remember { mutableStateOf<RoutineChanges?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.save_workout_title), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
                },
                actions = {
                    Button(
                        onClick = {
                            val changes = viewModel.getRoutineChanges()
                            if (changes != null && changes.hasChanges()) {
                                routineChanges = changes
                                showChangesSheet = true
                            } else {
                                onSave(workoutTitle.takeIf { it.isNotBlank() }, false)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(stringResource(R.string.save_btn), color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = workoutTitle,
                onValueChange = { workoutTitle = it },
                placeholder = { Text(stringResource(R.string.workout_title_label), color = TextGray, fontSize = 24.sp, fontWeight = FontWeight.Bold) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatDisplay(stringResource(R.string.duration_label), "%dmin".format(totalSeconds / 60), themeColor)
                StatDisplay(stringResource(R.string.volume_label), "${totalVolume.toInt()} kg", themeColor)
                StatDisplay(stringResource(R.string.sets_label), "$completedSetsCount", themeColor)
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = SurfaceDark)
            Spacer(modifier = Modifier.height(16.dp))

            Text(stringResource(R.string.when_label), color = TextGray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(currentTimeStr, color = themeColor, fontSize = 16.sp)

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = SurfaceDark)
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .border(1.dp, SurfaceDark, RoundedCornerShape(8.dp))
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = TextGray, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.add_photo_video), color = TextGray, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = SurfaceDark)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.description_label), color = TextGray) },
                placeholder = { Text(stringResource(R.string.how_did_it_go), color = TextGray) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(
                onClick = { showDiscardDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.discard_workout_btn), color = Color.Red)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                title = { Text(stringResource(R.string.discard_dialog_title), color = Color.White) },
                text = { Text(stringResource(R.string.discard_dialog_text), color = TextGray) },
                confirmButton = {
                    Button(
                        onClick = {
                            showDiscardDialog = false
                            onDiscard()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text(stringResource(R.string.discard_confirm), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardDialog = false }) {
                        Text(stringResource(R.string.cancel_btn), color = themeColor)
                    }
                }
            )
        }

        if (showChangesSheet && routineChanges != null) {
            ModalBottomSheet(
                onDismissRequest = { showChangesSheet = false },
                sheetState = sheetState,
                containerColor = SurfaceDark
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(48.dp).background(DarkBackground, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Text("📋", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.update_routine_title, routineChanges!!.templateName),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val parts = mutableListOf<String>()
                    if (routineChanges!!.addedExercises > 0) {
                        parts.add(stringResource(R.string.changes_added_exercises, routineChanges!!.addedExercises))
                    }
                    if (routineChanges!!.removedExercises > 0) {
                        parts.add(stringResource(R.string.changes_removed_exercises, routineChanges!!.removedExercises))
                    }
                    if (routineChanges!!.addedSets > 0) {
                        parts.add(stringResource(R.string.changes_added_sets, routineChanges!!.addedSets))
                    }
                    if (routineChanges!!.removedSets > 0) {
                        parts.add(stringResource(R.string.changes_removed_sets, routineChanges!!.removedSets))
                    }
                    
                    val andStr = stringResource(R.string.changes_and)
                    val combinedStr = if (parts.size > 1) {
                        parts.dropLast(1).joinToString(", ") + " $andStr " + parts.last()
                    } else {
                        parts.firstOrNull() ?: ""
                    }
                    val finalDesc = stringResource(R.string.changes_you, combinedStr)

                    Text(
                        text = finalDesc,
                        color = Color.White,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { 
                            showChangesSheet = false
                            onSave(workoutTitle.takeIf { it.isNotBlank() }, true)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                    ) {
                        Text(stringResource(R.string.update_routine_btn), color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { 
                            showChangesSheet = false
                            onSave(workoutTitle.takeIf { it.isNotBlank() }, false)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBackground)
                    ) {
                        Text(stringResource(R.string.keep_original_btn), color = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun StatDisplay(label: String, value: String, color: Color) {
    Column {
        Text(label, color = TextGray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = color, fontSize = 16.sp)
    }
}
