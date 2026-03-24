package com.ateszk0.ostromgep.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ateszk0.ostromgep.model.MuscleGroup
import com.ateszk0.ostromgep.viewmodel.WorkoutViewModel
import com.ateszk0.ostromgep.ui.theme.*
import com.ateszk0.ostromgep.R
import androidx.compose.ui.res.stringResource

@Composable
fun HomeScreen(viewModel: WorkoutViewModel, themeColor: Color, onNavigateToWorkout: () -> Unit) {
    val history by viewModel.workoutHistory.collectAsState()
    val bodyWeightHistory by viewModel.bodyWeightHistory.collectAsState()
    val nextMission = viewModel.suggestNextMission()
    val weeklyLog = viewModel.getWeeklyBattleLog()
    val recoveryMap = viewModel.getMuscleRecoveryStatus()
    val records = viewModel.getPersonalRecords()
    
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        val activeExercises by viewModel.activeExercises.collectAsState()
        var showOverrideConfirm by remember { mutableStateOf<(() -> Unit)?>(null) }
    
        if (showOverrideConfirm != null) {
            AlertDialog(
                onDismissRequest = { showOverrideConfirm = null },
                title = { Text(stringResource(R.string.discard_dialog_title), color = Color.White) }, 
                text = { Text("Biztosan új edzést kezdesz? A jelenlegi progressz elveszik.", color = TextGray) },
                confirmButton = {
                    Button(
                        onClick = { 
                            showOverrideConfirm?.invoke()
                            showOverrideConfirm = null 
                        }, 
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Folytatás", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showOverrideConfirm = null }) { 
                        Text(stringResource(R.string.cancel_btn), color = themeColor) 
                    }
                }
            )
        }
    
        val handleStartWorkout: (() -> Unit) -> Unit = { startAction ->
            if (activeExercises.isNotEmpty()) {
                showOverrideConfirm = startAction
            } else {
                startAction()
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Ostromgép", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
        
        NextMissionCard(nextMission = nextMission, themeColor = themeColor, onStart = {
            if (nextMission != null) {
                handleStartWorkout {
                    viewModel.startWorkoutFromTemplate(nextMission)
                    onNavigateToWorkout()
                }
            }
        })

        WeeklyBattleLog(log = weeklyLog, themeColor = themeColor)

        ReadyToSiegeHeatmap(recoveryMap = recoveryMap)

        WallOfFameTicker(records = records, themeColor = themeColor)

        QuickMetricsChart(viewModel = viewModel, themeColor = themeColor)
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun NextMissionCard(nextMission: com.ateszk0.ostromgep.model.WorkoutTemplate?, themeColor: Color, onStart: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.next_mission), color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            if (nextMission != null) {
                Text(nextMission.templateName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.start_mission), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(stringResource(R.string.next_mission_empty), color = TextGray, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun WeeklyBattleLog(log: List<Boolean>, themeColor: Color) {
    Column {
        Text(stringResource(R.string.weekly_battle_log), color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val days = listOf(
                stringResource(R.string.day_mon), 
                stringResource(R.string.day_tue), 
                stringResource(R.string.day_wed), 
                stringResource(R.string.day_thu), 
                stringResource(R.string.day_fri), 
                stringResource(R.string.day_sat), 
                stringResource(R.string.day_sun)
            )
            days.forEachIndexed { index, day ->
                val isCompleted = log.getOrNull(index) ?: false
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isCompleted) themeColor else Color.Transparent)
                            .border(
                                width = if (isCompleted) 0.dp else 1.dp,
                                color = if (isCompleted) Color.Transparent else TextGray,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(day, color = if (isCompleted) Color.White else TextGray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ReadyToSiegeHeatmap(recoveryMap: Map<MuscleGroup, String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.ready_to_siege), color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            val columns = 4
            val items = MuscleGroup.values().toList()
            val rows = items.chunked(columns)
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (i in 0 until columns) {
                            if (i < rowItems.size) {
                                val mg = rowItems[i]
                                val status = recoveryMap[mg] ?: "Green"
                                val color = when(status) {
                                    "Red" -> Color(0xFFFF453A)
                                    "Yellow" -> Color(0xFFFFD60A)
                                    else -> Color(0xFF32D74B)
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkBackground)
                                        .border(2.dp, color, RoundedCornerShape(8.dp))
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mg.name.lowercase().replaceFirstChar { it.uppercase() },
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        fontWeight = FontWeight.Bold,
                                        softWrap = false
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WallOfFameTicker(records: List<String>, themeColor: Color) {
    if (records.isEmpty()) return
    
    Column {
        Text(stringResource(R.string.wall_of_fame), color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        val scrollState = rememberScrollState()
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            records.forEach { record ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .border(1.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(record, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun QuickMetricsChart(viewModel: WorkoutViewModel, themeColor: Color) {
    val history by viewModel.bodyWeightHistory.collectAsState()
    var weightInput by remember { mutableStateOf("") }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.quick_metrics), color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        modifier = Modifier.width(80.dp).height(53.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                        placeholder = { Text("0.0", color = TextGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColor,
                            unfocusedBorderColor = DarkBackground
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val w = weightInput.toDoubleOrNull()
                            if (w != null) {
                                viewModel.addBodyWeight(w)
                                weightInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.White)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val recentWeights = history.sortedBy { it.timestamp }.takeLast(7).map { it.weightKg.toFloat() }
            if (recentWeights.size > 1) {
                val maxW = recentWeights.maxOrNull() ?: 100f
                val minW = recentWeights.minOrNull() ?: 0f
                val diff = if (maxW == minW) 1f else maxW - minW
                
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                    val width = size.width
                    val height = size.height
                    val stepX = width / (recentWeights.size - 1)
                    
                    val path = androidx.compose.ui.graphics.Path()
                    recentWeights.forEachIndexed { index, weight ->
                        val x = index * stepX
                        val normalizedY = 1f - ((weight - minW) / diff)
                        val y = normalizedY * height
                        
                        if (index == 0) path.moveTo(x, y)
                        else path.lineTo(x, y)
                    }
                    
                    val gradient = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(themeColor.copy(alpha = 0.5f), Color.Transparent)
                    )
                    
                    val fillPath = androidx.compose.ui.graphics.Path().apply {
                        addPath(path)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }
                    
                    drawPath(fillPath, brush = gradient)
                    drawPath(path, color = themeColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()))
                    
                    recentWeights.forEachIndexed { index, weight ->
                        val x = index * stepX
                        val normalizedY = 1f - ((weight - minW) / diff)
                        val y = normalizedY * height
                        drawCircle(color = themeColor, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                    }
                }
            } else {
                Text(stringResource(R.string.metrics_empty_chart), color = TextGray, fontSize = 12.sp)
            }
        }
    }
}
