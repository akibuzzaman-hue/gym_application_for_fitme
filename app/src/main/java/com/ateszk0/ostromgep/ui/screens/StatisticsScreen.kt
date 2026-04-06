package com.ateszk0.ostromgep.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ateszk0.ostromgep.R
import com.ateszk0.ostromgep.model.MuscleGroup
import com.ateszk0.ostromgep.ui.theme.SurfaceDark
import com.ateszk0.ostromgep.ui.theme.TextGray
import com.ateszk0.ostromgep.viewmodel.WorkoutViewModel
import java.util.*
import kotlin.math.min

// ── Palette for muscle groups ──────────────────────────────────────────────
private val MUSCLE_COLORS = listOf(
    Color(0xFF5E81F4), Color(0xFFFF6B6B), Color(0xFF4ECDC4), Color(0xFFFFA500),
    Color(0xFFAD7BE9), Color(0xFF56CFE1), Color(0xFFFF9F89), Color(0xFF2CB67D),
    Color(0xFFFFBD3E), Color(0xFFFF477E), Color(0xFF7EB8F7), Color(0xFFB8F77E),
    Color(0xFFF77EB8), Color(0xFF7EF7D4), Color(0xFFFF6347), Color(0xFF9B59B6),
    Color(0xFF27AE60), Color(0xFFF39C12), Color(0xFF1ABC9C)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: WorkoutViewModel, themeColor: Color, onBack: () -> Unit) {
    val history by viewModel.workoutHistory.collectAsState()
    val exerciseLibrary by viewModel.exerciseLibrary.collectAsState()

    val overallStats = remember(history, exerciseLibrary) { viewModel.getOverallStats() }
    val muscleSetCounts = remember(history, exerciseLibrary) {
        viewModel.getMuscleGroupSetCounts()
            .filter { it.key != MuscleGroup.OTHER && it.value > 0 }
            .entries.sortedByDescending { it.value }
    }
    val topExercises = remember(history) { viewModel.getTopExercises(10) }
    val personalRecords = remember(history) { viewModel.getPersonalRecords() }
    val workedDays = remember(history) { viewModel.getWorkedDaysLast7Days() }

    val totalMuscleSets = muscleSetCounts.sumOf { it.value }
    val maxMuscleSets = muscleSetCounts.maxOfOrNull { it.value } ?: 1

    // Localised strings needed in non-composable helpers below
    val setsUnit = stringResource(R.string.stat_sets_unit)
    val daysUnit = stringResource(R.string.stat_label_days_unit)
    val workoutsUnit = stringResource(R.string.stat_label_workouts_unit)

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    stringResource(R.string.statistics_title),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Black)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ── Section: Summary cards ───────────────────────────────────
            item {
                SectionHeader(
                    title = stringResource(R.string.stat_section_summary),
                    icon = Icons.Default.BarChart
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.stat_label_workouts),
                        value = overallStats.totalWorkouts.toString(),
                        icon = Icons.Default.FitnessCenter,
                        gradient = Brush.linearGradient(listOf(Color(0xFF4776E6), Color(0xFF8E54E9)))
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.stat_label_volume),
                        value = formatVolume(overallStats.totalVolumeKg),
                        icon = Icons.Default.MonitorWeight,
                        gradient = Brush.linearGradient(listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)))
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.stat_label_time),
                        value = formatDuration(overallStats.totalDurationMin),
                        icon = Icons.Default.Timer,
                        gradient = Brush.linearGradient(listOf(Color(0xFF11998E), Color(0xFF38EF7D)))
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.stat_label_avg_workout),
                        value = "${overallStats.avgDurationMin} ${stringResource(R.string.stat_minutes_label, 0).replace("0 ", "")}",
                        icon = Icons.Default.AccessTime,
                        gradient = Brush.linearGradient(listOf(Color(0xFFF7971E), Color(0xFFFFD200)))
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.stat_label_streak),
                        value = "${overallStats.longestStreakDays} $daysUnit",
                        icon = Icons.Default.Whatshot,
                        gradient = Brush.linearGradient(listOf(Color(0xFFDA22FF), Color(0xFF9733EE)))
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.stat_label_this_month),
                        value = "${overallStats.thisMonthWorkouts} $workoutsUnit",
                        icon = Icons.Default.CalendarToday,
                        gradient = Brush.linearGradient(listOf(Color(0xFF2193B0), Color(0xFF6DD5ED)))
                    )
                }
            }

            // ── Section: Last 7 days ─────────────────────────────────────
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item {
                SectionHeader(
                    title = stringResource(R.string.stat_section_last7),
                    icon = Icons.Default.DateRange
                )
            }
            item {
                WeekActivityRow(workedDays = workedDays, themeColor = themeColor)
            }

            // ── Section: Muscle distribution pie chart ───────────────────
            if (muscleSetCounts.isNotEmpty()) {
                item { Spacer(modifier = Modifier.height(24.dp)) }
                item {
                    SectionHeader(
                        title = stringResource(R.string.stat_section_muscle_dist),
                        icon = Icons.Default.PieChart
                    )
                }
                item {
                    MuscleDonutSection(
                        slices = muscleSetCounts,
                        total = totalMuscleSets,
                        setsUnit = setsUnit
                    )
                }

                // ── Muscle bar rows ──────────────────────────────────────
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item {
                    SectionHeader(
                        title = stringResource(R.string.stat_section_set_dist),
                        icon = Icons.Default.Layers
                    )
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceDark)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        muscleSetCounts.forEachIndexed { idx, entry ->
                            val color = MUSCLE_COLORS[idx % MUSCLE_COLORS.size]
                            MuscleBarRow(
                                name = muscleGroupRes(entry.key),
                                count = entry.value,
                                maxCount = maxMuscleSets,
                                color = color,
                                percentage = if (totalMuscleSets > 0)
                                    (entry.value.toFloat() / totalMuscleSets * 100).toInt() else 0,
                                setsUnit = setsUnit
                            )
                        }
                    }
                }
            }

            // ── Section: Top exercises ───────────────────────────────────
            if (topExercises.isNotEmpty()) {
                item { Spacer(modifier = Modifier.height(24.dp)) }
                item {
                    SectionHeader(
                        title = stringResource(R.string.stat_section_top_exercises),
                        icon = Icons.Default.EmojiEvents
                    )
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceDark)
                    ) {
                        topExercises.forEachIndexed { idx, (name, sets) ->
                            TopExerciseRow(
                                rank = idx + 1,
                                name = name,
                                sets = sets,
                                setsLabel = stringResource(R.string.stat_sets_label, sets),
                                themeColor = themeColor
                            )
                            if (idx < topExercises.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = Color.White.copy(alpha = 0.07f)
                                )
                            }
                        }
                    }
                }
            }

            // ── Section: Personal Records ────────────────────────────────
            if (personalRecords.isNotEmpty()) {
                item { Spacer(modifier = Modifier.height(24.dp)) }
                item {
                    SectionHeader(
                        title = stringResource(R.string.stat_section_records),
                        icon = Icons.Default.MilitaryTech
                    )
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceDark)
                            .padding(vertical = 8.dp)
                    ) {
                        personalRecords.forEach { record ->
                            Text(
                                text = record,
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // ── Section: Monthly summary ─────────────────────────────────
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item {
                SectionHeader(
                    title = stringResource(R.string.stat_section_monthly),
                    icon = Icons.Default.Assessment
                )
            }
            item {
                MonthlySummaryCard(
                    workouts = overallStats.thisMonthWorkouts,
                    volumeKg = overallStats.thisMonthVolumeKg,
                    workoutsLabel = stringResource(R.string.stat_label_workouts),
                    volumeLabel = stringResource(R.string.stat_label_volume),
                    themeColor = themeColor
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

// ── Composables ─────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = TextGray, modifier = Modifier.size(16.dp))
        Text(
            text = title.uppercase(),
            color = TextGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    gradient: Brush
) {
    Box(
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(gradient)
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
            Column {
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun WeekActivityRow(workedDays: List<Boolean>, themeColor: Color) {
    // Use the locale-aware short day names from strings.xml
    val dayLabels = listOf(
        stringResource(R.string.day_mon),
        stringResource(R.string.day_tue),
        stringResource(R.string.day_wed),
        stringResource(R.string.day_thu),
        stringResource(R.string.day_fri),
        stringResource(R.string.day_sat),
        stringResource(R.string.day_sun)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        workedDays.forEachIndexed { index, active ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(38.dp)
            ) {
                Text(
                    dayLabels.getOrElse(index) { "-" },
                    color = TextGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -(6 - index))
                }
                val dayNum = cal.get(Calendar.DAY_OF_MONTH).toString()
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) themeColor else Color.White.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        dayNum,
                        color = if (active) Color.White else TextGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun MuscleDonutSection(
    slices: List<Map.Entry<MuscleGroup, Int>>,
    total: Int,
    setsUnit: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .padding(16.dp)
    ) {
        var animated by remember { mutableStateOf(false) }
        LaunchedEffect(slices) { animated = true }
        val sweep by animateFloatAsState(
            targetValue = if (animated) 360f else 0f,
            animationSpec = tween(durationMillis = 900),
            label = "pieAnim"
        )

        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(180.dp)) {
                val strokeWidth = 36.dp.toPx()
                val radius = (min(size.width, size.height) - strokeWidth) / 2f
                val topLeft = Offset(center.x - radius, center.y - radius)
                val arcSize = Size(radius * 2, radius * 2)
                var startAngle = -90f

                slices.forEachIndexed { idx, entry ->
                    val fraction = if (total > 0) entry.value.toFloat() / total else 0f
                    val sliceSweep = fraction * sweep
                    drawArc(
                        color = MUSCLE_COLORS[idx % MUSCLE_COLORS.size],
                        startAngle = startAngle,
                        sweepAngle = sliceSweep - 1f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += sliceSweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(total.toString(), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(text = setsUnit, color = TextGray, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend (2-column grid)
        val chunked = slices.chunked(2)
        chunked.forEach { pair ->
            Row(modifier = Modifier.fillMaxWidth()) {
                pair.forEachIndexed { colIdx, entry ->
                    val globalIdx = chunked.indexOf(pair) * 2 + colIdx
                    val color = MUSCLE_COLORS[globalIdx % MUSCLE_COLORS.size]
                    val pct = if (total > 0) (entry.value.toFloat() / total * 100).toInt() else 0
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = muscleGroupRes(entry.key),
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(text = "$pct%", color = TextGray, fontSize = 11.sp)
                    }
                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MuscleBarRow(
    name: String,
    count: Int,
    maxCount: Int,
    color: Color,
    percentage: Int,
    setsUnit: String
) {
    val animatedFraction by animateFloatAsState(
        targetValue = if (maxCount > 0) count.toFloat() / maxCount else 0f,
        animationSpec = tween(700),
        label = "barAnim"
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text("$count $setsUnit • $percentage%", color = TextGray, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun TopExerciseRow(rank: Int, name: String, sets: Int, setsLabel: String, themeColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    when (rank) {
                        1 -> Color(0xFFFFD700)
                        2 -> Color(0xFFC0C0C0)
                        3 -> Color(0xFFCD7F32)
                        else -> Color.White.copy(alpha = 0.08f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                rank.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (rank <= 3) Color.Black else TextGray
            )
        }
        Text(
            name,
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(themeColor.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(setsLabel, color = themeColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MonthlySummaryCard(
    workouts: Int,
    volumeKg: Double,
    workoutsLabel: String,
    volumeLabel: String,
    themeColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MonthlyStat(label = workoutsLabel, value = workouts.toString(), icon = Icons.Default.FitnessCenter, color = themeColor)
        VerticalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.height(56.dp))
        MonthlyStat(label = volumeLabel, value = formatVolume(volumeKg), icon = Icons.Default.MonitorWeight, color = Color(0xFFFF6B6B))
    }
}

@Composable
private fun MonthlyStat(label: String, value: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextGray, fontSize = 12.sp)
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Returns the localised display name for a muscle group.
 * Must be called from a @Composable context to access string resources.
 */
@Composable
private fun muscleGroupRes(mg: MuscleGroup): String = stringResource(
    when (mg) {
        MuscleGroup.ABDOMINALS -> R.string.mg_abdominals
        MuscleGroup.ABDUCTORS -> R.string.mg_abductors
        MuscleGroup.ADDUCTORS -> R.string.mg_adductors
        MuscleGroup.BICEPS -> R.string.mg_biceps
        MuscleGroup.CALVES -> R.string.mg_calves
        MuscleGroup.CARDIO -> R.string.mg_cardio
        MuscleGroup.CHEST -> R.string.mg_chest
        MuscleGroup.FOREARMS -> R.string.mg_forearms
        MuscleGroup.GLUTES -> R.string.mg_glutes
        MuscleGroup.HAMSTRINGS -> R.string.mg_hamstrings
        MuscleGroup.LATS -> R.string.mg_lats
        MuscleGroup.LOWER_BACK -> R.string.mg_lower_back
        MuscleGroup.NECK -> R.string.mg_neck
        MuscleGroup.QUADRICEPS -> R.string.mg_quadriceps
        MuscleGroup.SHOULDERS -> R.string.mg_shoulders
        MuscleGroup.TRAPS -> R.string.mg_traps
        MuscleGroup.TRICEPS -> R.string.mg_triceps
        MuscleGroup.UPPER_BACK -> R.string.mg_upper_back
        MuscleGroup.OTHER -> R.string.mg_other
    }
)

private fun formatVolume(kg: Double): String = when {
    kg >= 1_000_000 -> "${(kg / 1_000_000).toInt()}M kg"
    kg >= 1_000 -> "${(kg / 1_000).toInt()}k kg"
    else -> "${kg.toInt()} kg"
}

private fun formatDuration(minutes: Int): String = when {
    minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
    else -> "${minutes}m"
}
