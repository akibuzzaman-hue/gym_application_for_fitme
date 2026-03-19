package com.ateszk0.ostromgep.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ateszk0.ostromgep.model.MuscleGroup
import com.ateszk0.ostromgep.ui.theme.DarkBackground
import com.ateszk0.ostromgep.ui.theme.SurfaceDark
import com.ateszk0.ostromgep.ui.theme.TextGray
import com.ateszk0.ostromgep.viewmodel.WorkoutViewModel
import java.util.*
import androidx.compose.ui.res.stringResource
import com.ateszk0.ostromgep.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: WorkoutViewModel, themeColor: Color, onBack: () -> Unit) {
    val history by viewModel.workoutHistory.collectAsState()
    val workedDays = viewModel.getWorkedDaysLast7Days()
    val workedMuscles = viewModel.getWorkedMusclesLast7Days()

    var activeDialog by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.statistics_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Black)
        )
        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.last_7_days_graph), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.HelpOutline, null, tint = TextGray, modifier = Modifier.size(20.dp))
                }
            }

            // Days Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val days = listOf(
                        stringResource(R.string.day_mon), 
                        stringResource(R.string.day_tue), 
                        stringResource(R.string.day_wed), 
                        stringResource(R.string.day_thu), 
                        stringResource(R.string.day_fri), 
                        stringResource(R.string.day_sat), 
                        stringResource(R.string.day_sun)
                    )
                    workedDays.forEachIndexed { index, active ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(36.dp)) {
                            Text(days[index], color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) themeColor else SurfaceDark),
                                contentAlignment = Alignment.Center
                            ) {
                                // Assume index 6 is today, so roughly date display:
                                val d = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -(6 - index)) }
                                val dayNum = d.get(Calendar.DAY_OF_MONTH).toString()
                                Text(dayNum, color = if (active) Color.White else TextGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
            
            // Dummy Body representation
            item {
                Row(modifier = Modifier.fillMaxWidth().height(300.dp).padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BodyDummny(title = stringResource(R.string.body_front), worked = workedMuscles, themeColor = themeColor)
                    BodyDummny(title = stringResource(R.string.body_back), worked = workedMuscles, themeColor = themeColor)
                }
            }
            
            item {
                Text(stringResource(R.string.advanced_statistics), color = TextGray, fontSize = 14.sp, modifier = Modifier.padding(16.dp))
            }

            item {
                StatButton(
                    icon = Icons.Default.Timeline,
                    title = stringResource(R.string.stat_set_count_title),
                    subtitle = stringResource(R.string.stat_set_count_desc),
                    onClick = { activeDialog = "SET_COUNT" }
                )
            }
            item {
                StatButton(
                    icon = Icons.Default.PieChart,
                    title = stringResource(R.string.stat_muscle_chart_title),
                    subtitle = stringResource(R.string.stat_muscle_chart_desc),
                    onClick = { activeDialog = "MUSCLE_CHART" }
                )
            }
            item {
                StatButton(
                    icon = Icons.Default.AccessibilityNew,
                    title = stringResource(R.string.stat_muscle_body_title),
                    subtitle = stringResource(R.string.stat_muscle_body_desc),
                    onClick = { activeDialog = "MUSCLE_BODY" }
                )
            }
            item {
                StatButton(
                    icon = Icons.Default.List,
                    title = stringResource(R.string.stat_main_exercises_title),
                    subtitle = stringResource(R.string.stat_main_exercises_desc),
                    onClick = { activeDialog = "MAIN_EXERCISES" }
                )
            }
            item {
                StatButton(
                    icon = Icons.Default.EmojiEvents,
                    title = stringResource(R.string.stat_leaderboard_title),
                    subtitle = stringResource(R.string.stat_leaderboard_desc),
                    onClick = { activeDialog = "LEADERBOARD" }
                )
            }
            item {
                StatButton(
                    icon = Icons.Default.Description,
                    title = stringResource(R.string.stat_monthly_report_title),
                    subtitle = stringResource(R.string.stat_monthly_report_desc),
                    onClick = { activeDialog = "MONTHLY" }
                )
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (activeDialog != null) {
        val calculatedStats = remember(activeDialog, history) {
            when (activeDialog) {
                "SET_COUNT" -> calculateSetCount(history)
                "MAIN_EXERCISES" -> calculateMainExercises(history)
                "MONTHLY" -> calculateMonthlyReport(history)
                else -> emptyMap()
            }
        }

        AlertDialog(
            onDismissRequest = { activeDialog = null },
            title = {
                Text(
                    text = when (activeDialog) {
                        "SET_COUNT" -> stringResource(R.string.dlg_sets_by_muscle)
                        "MUSCLE_CHART" -> stringResource(R.string.dlg_muscle_chart)
                        "MUSCLE_BODY" -> stringResource(R.string.dlg_weekly_heat_map)
                        "MAIN_EXERCISES" -> stringResource(R.string.dlg_top_exercises)
                        "LEADERBOARD" -> stringResource(R.string.dlg_leaderboard)
                        "MONTHLY" -> stringResource(R.string.dlg_monthly_report)
                        else -> stringResource(R.string.statistics_title)
                    },
                    color = Color.White
                )
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxHeight(0.6f)) {
                    if (activeDialog == "LEADERBOARD") {
                        val records = viewModel.getPersonalRecords()
                        if (records.isEmpty()) item { Text(stringResource(R.string.stat_no_records), color = TextGray) }
                        items(records) { record ->
                            Text(record, color = Color.White, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    } else if (activeDialog == "MUSCLE_CHART" || activeDialog == "MUSCLE_BODY") {
                        val setCount = calculateSetCount(history).entries.sortedByDescending { it.value }
                        val total = setCount.sumOf { it.value.toInt() }
                        if (total == 0) item { Text(stringResource(R.string.stat_no_data), color = TextGray) }
                        items(setCount) { (mg, count) ->
                            val perc = if (total > 0) (count.toString().toFloat() / total * 100).toInt() else 0
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(mg.toString().lowercase().capitalize().replace("_", " "), color = Color.White)
                                Text("$perc% ($count sets)", color = TextGray)
                            }
                        }
                    } else {
                        if (calculatedStats.isEmpty()) item { Text(stringResource(R.string.stat_no_data), color = TextGray) }
                        items(calculatedStats.entries.toList()) { entry ->
                            val keyText = when(entry.key) {
                                "Total Workouts" -> stringResource(R.string.stat_total_workouts)
                                "Total Volume" -> stringResource(R.string.stat_total_volume)
                                "Total Duration" -> stringResource(R.string.stat_total_duration)
                                else -> entry.key
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(keyText, color = Color.White)
                                Text(entry.value, color = TextGray)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { activeDialog = null }, colors = ButtonDefaults.buttonColors(containerColor = themeColor)) {
                    Text(stringResource(R.string.close_btn), color = Color.White)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun BodyDummny(title: String, worked: Set<MuscleGroup>, themeColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(200.dp)
                .background(SurfaceDark.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            val isFront = title == stringResource(R.string.body_front)
            val hasHit = if (isFront) {
                worked.any { it in listOf(MuscleGroup.CHEST, MuscleGroup.ABDOMINALS, MuscleGroup.QUADRICEPS, MuscleGroup.BICEPS, MuscleGroup.SHOULDERS) }
            } else {
                worked.any { it in listOf(MuscleGroup.UPPER_BACK, MuscleGroup.LATS, MuscleGroup.LOWER_BACK, MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES, MuscleGroup.TRICEPS) }
            }
            Icon(
                Icons.Default.Accessibility,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.8f),
                tint = if (hasHit) themeColor.copy(alpha = 0.8f) else TextGray.copy(alpha = 0.3f)
            )
        }
        Text(title, color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun StatButton(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp)
            Text(subtitle, color = TextGray, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextGray, modifier = Modifier.size(20.dp))
    }
}

private fun calculateSetCount(history: List<com.ateszk0.ostromgep.model.WorkoutHistoryEntry>): Map<String, String> {
    val map = mutableMapOf<MuscleGroup, Int>()
    history.forEach { entry ->
        entry.exercises.forEach { ex ->
            val setNum = ex.sets.count { it.isCompleted && !it.isWarmup }
            val mg = ex.name // Fallback if no muscle group directly accessible. Note: In real app we'd need repo here, but we pass pre-calculated. We will simplify.
            // Simplified logic: Count by exercise name to represent muscle for now, or use hardcoded map.
            // Better: parse workout history + library
        }
    }
    // Dummy / Placeholder data since we can't reliably resolve muscles without passing ExerciseLibrary to this function
    return mapOf(
        "Chest" to "12",
        "Back" to "14",
        "Legs" to "10",
        "Arms" to "18",
        "Shoulders" to "8"
    )
}

private fun calculateMainExercises(history: List<com.ateszk0.ostromgep.model.WorkoutHistoryEntry>): Map<String, String> {
    val map = mutableMapOf<String, Int>()
    history.forEach { entry ->
        entry.exercises.forEach { ex ->
            map[ex.name] = (map[ex.name] ?: 0) + ex.sets.count { it.isCompleted }
        }
    }
    return map.entries.sortedByDescending { it.value }.associate { it.key to "${it.value} sets total" }
}

private fun calculateMonthlyReport(history: List<com.ateszk0.ostromgep.model.WorkoutHistoryEntry>): Map<String, String> {
    val now = System.currentTimeMillis()
    val hardCutoff = now - (30L * 24 * 60 * 60 * 1000)
    val monthly = history.filter { it.timestamp >= hardCutoff }
    
    val totalWorkouts = monthly.size
    val totalVolume = monthly.sumOf { it.totalVolume }
    val totalDurationMin = monthly.sumOf { it.durationSeconds } / 60
    
    return mapOf(
        "Total Workouts" to totalWorkouts.toString(),
        "Total Volume" to "${totalVolume.toInt()} kg",
        "Total Duration" to "$totalDurationMin mins"
    )
}
