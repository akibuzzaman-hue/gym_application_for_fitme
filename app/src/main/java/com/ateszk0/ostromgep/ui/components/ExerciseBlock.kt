package com.ateszk0.ostromgep.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ateszk0.ostromgep.R
import com.ateszk0.ostromgep.model.ExerciseSessionData
import com.ateszk0.ostromgep.model.WorkoutSetData
import com.ateszk0.ostromgep.ui.theme.*
import androidx.compose.ui.platform.LocalFocusManager
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseBlock(
    exercise: ExerciseSessionData, 
    imageUri: String?,
    videoUrl: String?,
    index: Int, 
    total: Int, 
    themeColor: Color,
    onMoveUp: () -> Unit, 
    onMoveDown: () -> Unit,
    onSetUpdate: (WorkoutSetData) -> Unit, 
    onSetCompleteToggle: (WorkoutSetData) -> Unit, 
    onAddSet: () -> Unit, 
    onDeleteSet: (Int) -> Unit, 
    onUpdateRestTime: (Int) -> Unit, 
    onToggleWarmup: (Int) -> Unit, 
    onNoteUpdate: (String) -> Unit, 
    onDeleteExercise: () -> Unit, 
    onEditRepRange: () -> Unit,
    onSuperset: () -> Unit,
    onRemoveSuperset: () -> Unit,
    onRpeClick: (WorkoutSetData) -> Unit,
    onReplaceExercise: () -> Unit = {},
    onApplyWarmups: (baseKg: Double, baseReps: String, percentages: List<Int>) -> Unit = { _, _, _ -> },
    bodyweightKg: Double? = null,
    exerciseType: com.ateszk0.ostromgep.model.ExerciseType = com.ateszk0.ostromgep.model.ExerciseType.REPS_WEIGHT
) {
    val showKgColumn = exerciseType == com.ateszk0.ostromgep.model.ExerciseType.REPS_WEIGHT
    val kgColLabel = if (exerciseType == com.ateszk0.ostromgep.model.ExerciseType.TIME) "Sec" else if (exerciseType == com.ateszk0.ostromgep.model.ExerciseType.DISTANCE_TIME) "Km" else if (bodyweightKg != null) "BW (${bodyweightKg.toInt()}kg) + KG" else stringResource(R.string.kg_label_short)
    val focusManager = LocalFocusManager.current
    var showRest by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showWarmupCalc by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf<String?>(null) }
    var showVideoDialog by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), 
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                if (!imageUri.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (!videoUrl.isNullOrEmpty()) showVideoDialog = videoUrl
                                else showImageDialog = imageUri
                            }
                    ) {
                        coil.compose.AsyncImage(
                            model = imageUri,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceDark)
                            .clickable { if (!videoUrl.isNullOrEmpty()) showVideoDialog = videoUrl },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FitnessCenter, null, tint = TextGray, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = exercise.name, 
                    color = themeColor, 
                    fontSize = 16.sp, 
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.clickable { showRest = true }.background(SurfaceDark, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp), 
                    verticalAlignment = Alignment.CenterVertically
                ) { 
                    Icon(Icons.Default.Timer, null, tint = TextGray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${exercise.restTimerDuration}s", color = TextGray, fontSize = 12.sp) 
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box {
                    Icon(Icons.Default.MoreVert, null, tint = TextGray, modifier = Modifier.clickable { showMenu = true }.padding(4.dp))
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(SurfaceDark)) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.move_up) + " ↑", color = Color.White) }, onClick = { showMenu = false; onMoveUp() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.move_down) + " ↓", color = Color.White) }, onClick = { showMenu = false; onMoveDown() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.edit_label), color = Color.White) }, onClick = { showMenu = false; onEditRepRange() })
                        DropdownMenuItem(text = { Text("Replace Exercise", color = Color.White) }, onClick = { showMenu = false; onReplaceExercise() })
                        if (showKgColumn) {
                            DropdownMenuItem(text = { Text("Warmup Calculator", color = Color.White) }, onClick = { showMenu = false; showWarmupCalc = true })
                        }
                        DropdownMenuItem(text = { Text(if (exercise.supersetId == null) stringResource(R.string.superset_label) else stringResource(R.string.remove_superset), color = Color.White) }, onClick = { showMenu = false; if (exercise.supersetId == null) onSuperset() else onRemoveSuperset() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.delete_btn), color = Color.Red) }, onClick = { showMenu = false; onDeleteExercise() })
                    }
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            if (exercise.note.isEmpty()) { 
                Text(stringResource(R.string.add_notes_placeholder), color = TextGray.copy(alpha = 0.5f), fontSize = 12.sp) 
            }
            BasicTextField(
                value = exercise.note, 
                onValueChange = onNoteUpdate, 
                textStyle = TextStyle(color = TextGray, fontSize = 12.sp), 
                modifier = Modifier.fillMaxWidth(), 
                cursorBrush = SolidColor(themeColor)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) { 
            Text(stringResource(R.string.set_label_short), color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.1f), textAlign = TextAlign.Center)
            Text(stringResource(R.string.previous_label_short), color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.3f), textAlign = TextAlign.Center)
            if (showKgColumn) {
                Text(kgColLabel, color = if (bodyweightKg != null) WarmupYellow else TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center)
            } else if (exerciseType == com.ateszk0.ostromgep.model.ExerciseType.DISTANCE_TIME) {
                Text("Km", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.2f), textAlign = TextAlign.Center)
            }
            Text(if (exerciseType == com.ateszk0.ostromgep.model.ExerciseType.TIME || exerciseType == com.ateszk0.ostromgep.model.ExerciseType.DISTANCE_TIME) "Time (mm:ss)" else stringResource(R.string.reps_label_short), color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(if (showKgColumn || exerciseType == com.ateszk0.ostromgep.model.ExerciseType.DISTANCE_TIME) 0.2f else 0.4f), textAlign = TextAlign.Center)
            Text(stringResource(R.string.rir_label_short), color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.15f), textAlign = TextAlign.Center)
            Text("✔", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(0.1f), textAlign = TextAlign.Center) 
        }

        exercise.sets.forEach { set ->
            key(set.id) {
                val coroutineScope = rememberCoroutineScope()
                val maxSwipe = -150f
                val swipeOffset = remember { androidx.compose.animation.core.Animatable(0f) }

                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Red)
                            .clickable { onDeleteSet(set.id) },
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.padding(end = 24.dp))
                    }

                    val rowBg = if (set.isCompleted) CompletedGreen.copy(alpha = 0.2f) else Color.Transparent
                    Row(
                        modifier = Modifier
                            .offset { IntOffset(swipeOffset.value.roundToInt(), 0) }
                            .fillMaxWidth()
                            .background(DarkBackground)
                            .background(rowBg)
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        coroutineScope.launch {
                                            swipeOffset.snapTo((swipeOffset.value + dragAmount).coerceIn(maxSwipe, 0f))
                                        }
                                    },
                                    onDragEnd = {
                                        coroutineScope.launch {
                                            if (swipeOffset.value < maxSwipe / 2) {
                                                swipeOffset.animateTo(maxSwipe)
                                            } else {
                                                swipeOffset.animateTo(0f)
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        coroutineScope.launch { swipeOffset.animateTo(0f) }
                                    }
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = set.setLabel, 
                            color = if (set.isWarmup) WarmupYellow else Color.White, 
                            fontSize = 14.sp, 
                            fontWeight = FontWeight.Bold, 
                            modifier = Modifier.weight(0.1f).clickable { onToggleWarmup(set.id) }, 
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = set.previousText, 
                            color = TextGray, 
                            fontSize = 12.sp, 
                            modifier = Modifier.weight(0.3f), 
                            textAlign = TextAlign.Center, 
                            maxLines = 1
                        )
                        Box(modifier = Modifier.weight(0.2f).padding(horizontal = 4.dp)) { 
                            if (showKgColumn) {
                                CustomTextField(set.kg, { onSetUpdate(set.copy(kg = it)) }, set.isCompleted, themeColor)
                            } else if (exerciseType == com.ateszk0.ostromgep.model.ExerciseType.DISTANCE_TIME) {
                                CustomTextField(set.kg, { onSetUpdate(set.copy(kg = it)) }, set.isCompleted, themeColor, KeyboardType.Decimal)
                            }
                        }
                        val isTimeInput = exerciseType == com.ateszk0.ostromgep.model.ExerciseType.TIME || exerciseType == com.ateszk0.ostromgep.model.ExerciseType.DISTANCE_TIME
                        Box(modifier = Modifier.weight(if (showKgColumn || exerciseType == com.ateszk0.ostromgep.model.ExerciseType.DISTANCE_TIME) 0.2f else 0.4f).padding(horizontal = 4.dp)) { 
                            CustomTextField(set.reps, { onSetUpdate(set.copy(reps = it)) }, set.isCompleted, themeColor, if (isTimeInput) KeyboardType.Text else KeyboardType.Number) 
                        }
                        Box(
                            modifier = Modifier
                                .weight(0.15f)
                                .padding(horizontal = 4.dp)
                                .height(36.dp)
                                .background(if (set.isCompleted) Color.Transparent else SurfaceDark, RoundedCornerShape(4.dp))
                                .border(1.dp, if (set.isCompleted) Color.Transparent else Color.DarkGray, RoundedCornerShape(4.dp))
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { focusManager.clearFocus(); onRpeClick(set) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (set.rir.isBlank()) "-" else set.rir,
                                color = if (set.isCompleted) TextGray else Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(0.1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (set.isCompleted) CompletedGreen else InputBackground)
                                .clickable { focusManager.clearFocus(); onSetCompleteToggle(set) }, 
                            contentAlignment = Alignment.Center
                        ) { 
                            if (set.isCompleted) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) 
                        }
                    }
                }
            }
        }
        Text(
            "+ Add Set", 
            color = Color.White, 
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
                .clickable { onAddSet() }
                .padding(vertical = 12.dp), 
            textAlign = TextAlign.Center
        )
    }
    
    if (showRest) { 
        var t by remember { mutableStateOf(exercise.restTimerDuration.toString()) }
        AlertDialog(
            onDismissRequest = { showRest = false }, 
            title = { Text("Pihenőidő (s)", color = Color.White) }, 
            text = { 
                OutlinedTextField(
                    value = t, 
                    onValueChange = { t = it }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                ) 
            }, 
            confirmButton = { 
                Button(
                    onClick = { onUpdateRestTime(t.toIntOrNull() ?: 90); showRest = false },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                ) { Text("OK", color = Color.White) } 
            }
        ) 
    }
    
    if (showImageDialog != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showImageDialog = null }) {
            Box(modifier = Modifier.fillMaxSize().clickable { showImageDialog = null }, contentAlignment = Alignment.Center) {
                coil.compose.AsyncImage(
                    model = showImageDialog,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                )
            }
        }
    }
    
    if (showVideoDialog != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showVideoDialog = null }) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp))) {
                VideoPlayer(videoUrl = showVideoDialog!!)
            }
        }
    }
    
    if (showWarmupCalc) {
        WarmupCalculatorDialog(
            exercise = exercise,
            themeColor = themeColor,
            onDismiss = { showWarmupCalc = false },
            onApply = onApplyWarmups
        )
    }
}

@Composable
fun CustomTextField(v: String, onV: (String) -> Unit, comp: Boolean, theme: Color, keyboardType: KeyboardType = KeyboardType.Number) {
    BasicTextField(
        value = v, 
        onValueChange = onV, 
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType), 
        textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center), 
        cursorBrush = SolidColor(theme), 
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (comp) Color.Transparent else InputBackground)
            .padding(vertical = 8.dp)
    )
}

@Composable
fun StatItem(l: String, v: String) { 
    Column { 
        Text(l, color = TextGray, fontSize = 12.sp)
        Text(v, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) 
    } 
}

@Composable
fun TimerAdjustButton(t: String, onClick: () -> Unit) { 
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(0.2f))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) { 
        Text(t, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) 
    } 
}

@Composable
fun WarmupCalculatorDialog(
    exercise: ExerciseSessionData,
    themeColor: Color,
    onDismiss: () -> Unit,
    onApply: (baseKg: Double, baseReps: String, percentages: List<Int>) -> Unit
) {
    var warmupSetsCount by remember { mutableIntStateOf(1) }
    var percentages by remember { mutableStateOf(listOf(50)) }
    
    // First non-warmup set for default value
    val workingSet = exercise.sets.firstOrNull { it.isWorkingSet() } ?: exercise.sets.firstOrNull { !it.isWarmup }
    val baseWeightStr = workingSet?.kg ?: "0"
    val baseReps = workingSet?.reps ?: "10"
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkBackground),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Warmup Calculator", 
                    color = Color.White, 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text("Base Working Weight: $baseWeightStr kg", color = TextGray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Number of Warmup Sets", color = Color.White, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { 
                            if (warmupSetsCount > 1) {
                                warmupSetsCount--
                                percentages = percentages.dropLast(1)
                            }
                        }) {
                            Text("-", color = themeColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("$warmupSetsCount", color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp))
                        IconButton(onClick = { 
                            if (warmupSetsCount < 5) {
                                warmupSetsCount++
                                val p = if (percentages.isNotEmpty()) percentages.last() else 50
                                percentages = percentages + (p + 10).coerceAtMost(90)
                            }
                        }) {
                            Text("+", color = themeColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                percentages.forEachIndexed { index, pct ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Set ${index + 1}", color = Color.White, fontSize = 12.sp)
                            Text("$pct%", color = themeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = pct.toFloat(),
                            onValueChange = { newValue ->
                                val newPct = ((newValue / 10).roundToInt() * 10).coerceIn(10, 90)
                                percentages = percentages.toMutableList().apply { set(index, newPct) }
                            },
                            valueRange = 10f..90f,
                            steps = 7,
                            colors = SliderDefaults.colors(thumbColor = themeColor, activeTrackColor = themeColor)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel_btn), color = TextGray)
                    }
                    Button(
                        onClick = { 
                            val baseWeightDb = baseWeightStr.toDoubleOrNull() ?: 0.0
                            if (baseWeightDb > 0) {
                                onApply(baseWeightDb, baseReps, percentages)
                            }
                            onDismiss() 
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                    ) {
                        Text(stringResource(R.string.apply_btn), color = Color.White)
                    }
                }
            }
        }
    }
}

