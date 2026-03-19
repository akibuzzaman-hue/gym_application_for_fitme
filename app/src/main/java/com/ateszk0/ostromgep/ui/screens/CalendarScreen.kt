package com.ateszk0.ostromgep.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ateszk0.ostromgep.model.WorkoutHistoryEntry
import com.ateszk0.ostromgep.viewmodel.WorkoutViewModel
import com.ateszk0.ostromgep.ui.theme.*
import java.util.Calendar
import androidx.compose.ui.res.stringResource
import com.ateszk0.ostromgep.R

@Composable
fun CalendarScreen(viewModel: WorkoutViewModel, themeColor: Color, onBack: () -> Unit) {
    val history by viewModel.workoutHistory.collectAsState()
    
    val monthlyWorkouts = history.groupBy { entry ->
        val cal = Calendar.getInstance().apply { timeInMillis = entry.timestamp }
        Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }.toSortedMap(compareByDescending<Pair<Int, Int>> { it.first }.thenByDescending { it.second })

    val displayMonths = if (monthlyWorkouts.isEmpty()) {
        val now = Calendar.getInstance()
        mapOf(Pair(now.get(Calendar.YEAR), now.get(Calendar.MONTH)) to emptyList<WorkoutHistoryEntry>())
    } else {
        monthlyWorkouts
    }

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
            Spacer(modifier = Modifier.weight(1f))
            Text(stringResource(R.string.calendar_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(48.dp)) // To balance the back button
        }
        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(displayMonths.entries.toList()) { (yearMonth, entries) ->
                CalendarMonthView(year = yearMonth.first, month = yearMonth.second, entries = entries, themeColor = themeColor)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun CalendarMonthView(year: Int, month: Int, entries: List<WorkoutHistoryEntry>, themeColor: Color) {
    val monthNames = arrayOf(
        stringResource(R.string.month_jan), 
        stringResource(R.string.month_feb), 
        stringResource(R.string.month_mar), 
        stringResource(R.string.month_apr), 
        stringResource(R.string.month_may), 
        stringResource(R.string.month_jun), 
        stringResource(R.string.month_jul), 
        stringResource(R.string.month_aug), 
        stringResource(R.string.month_sep), 
        stringResource(R.string.month_oct), 
        stringResource(R.string.month_nov), 
        stringResource(R.string.month_dec)
    )
    val currentMonth = "${monthNames[month]} $year"
    
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    var startDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
    if (startDayOfWeek == 0) startDayOfWeek = 7

    val workoutDays = mutableMapOf<Int, String>()
    entries.forEach { entry ->
        val eCal = Calendar.getInstance().apply { timeInMillis = entry.timestamp }
        val day = eCal.get(Calendar.DAY_OF_MONTH)
        val name = entry.exercises.firstOrNull()?.name ?: stringResource(R.string.default_workout_name)
        // keep the shortest name or just the first workout found that day
        workoutDays[day] = name 
    }

    Text(currentMonth, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        listOf(
            stringResource(R.string.day_mon_short), 
            stringResource(R.string.day_tue_short), 
            stringResource(R.string.day_wed_short), 
            stringResource(R.string.day_thu_short), 
            stringResource(R.string.day_fri_short), 
            stringResource(R.string.day_sat_short), 
            stringResource(R.string.day_sun_short)
        ).forEach {
            Text(it, color = TextGray, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    
    val days = (1..daysInMonth).toList()
    val emptyDays = List(startDayOfWeek - 1) { 0 }
    val grid = emptyDays + days
    val rows = grid.chunked(7)
    
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                for (i in 0 until 7) {
                    val day = row.getOrNull(i) ?: 0
                    Box(modifier = Modifier.weight(1f).aspectRatio(0.7f), contentAlignment = Alignment.TopCenter) {
                        if (day > 0) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
                                val isWorkoutDay = workoutDays.containsKey(day)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isWorkoutDay) themeColor else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(day.toString(), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                if (isWorkoutDay) {
                                    Text(
                                        text = workoutDays[day] ?: "", 
                                        color = themeColor, 
                                        fontSize = 10.sp, 
                                        maxLines = 1, 
                                        overflow = TextOverflow.Ellipsis,
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
