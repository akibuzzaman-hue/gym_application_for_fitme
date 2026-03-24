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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ateszk0.ostromgep.viewmodel.WorkoutViewModel
import com.ateszk0.ostromgep.ui.theme.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.ateszk0.ostromgep.R

@Composable
fun DashboardProfile(viewModel: WorkoutViewModel, themeColor: Color, onNavigateToExercises: () -> Unit, onNavigateToCalendar: () -> Unit, onNavigateToStatistics: () -> Unit, onNavigateToWorkoutLog: (Long?) -> Unit) {
    val history by viewModel.workoutHistory.collectAsState()
    val chartData = viewModel.getChartData()
    val username by viewModel.username.collectAsState()
    val profUri by viewModel.profilePictureUri.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item { 
            Row(verticalAlignment = Alignment.CenterVertically) { 
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(SurfaceDark), 
                    contentAlignment = Alignment.Center
                ) { 
                    if (profUri.isNullOrEmpty()) {
                        Icon(Icons.Default.Person, null, tint = TextGray, modifier = Modifier.size(40.dp)) 
                    } else {
                        AsyncImage(
                            model = profUri,
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) { 
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(username, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 4.dp)) { 
                        Column { 
                            Text(stringResource(R.string.workouts_label), color = TextGray, fontSize = 12.sp)
                            Text("${history.size}", color = Color.White, fontWeight = FontWeight.Bold) 
                        }
                    } 
                } 
            }
            Spacer(modifier = Modifier.height(24.dp)) 
        }
        item { 
            Text(stringResource(R.string.recent_workouts_volume), color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) { 
                chartData.forEach { heightRatio -> 
                    Box(modifier = Modifier.width(24.dp).fillMaxHeight(heightRatio).background(themeColor, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))) 
                } 
            }
            Spacer(modifier = Modifier.height(24.dp)) 
        }
        item { 
            Text(stringResource(R.string.dashboard_title), color = TextGray)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { 
                DashboardButton(stringResource(R.string.statistics_btn), Icons.Default.BarChart, Modifier.weight(1f)) { onNavigateToStatistics() }
                DashboardButton(stringResource(R.string.exercise_btn_short), Icons.Default.FitnessCenter, Modifier.weight(1f)) { onNavigateToExercises() } 
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { 
                DashboardButton(stringResource(R.string.workouts_btn), Icons.Default.FormatListBulleted, Modifier.weight(1f)) { onNavigateToWorkoutLog(null) }
                DashboardButton(stringResource(R.string.calendar_btn), Icons.Default.CalendarMonth, Modifier.weight(1f)) { onNavigateToCalendar() } 
            }
            Spacer(modifier = Modifier.height(32.dp)) 
        }
        item { 
            Text(stringResource(R.string.workouts_label), color = TextGray)
            Spacer(modifier = Modifier.height(8.dp)) 
        }
        items(history.reversed().take(5)) { workout -> 
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onNavigateToWorkoutLog(workout.timestamp) }, 
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) { 
                Column(modifier = Modifier.padding(16.dp)) { 
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.workout_log_title), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text(sdf.format(java.util.Date(workout.timestamp)), color = TextGray, fontSize = 12.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) { 
                        Column { 
                            Text(stringResource(R.string.time_label), color = TextGray, fontSize = 12.sp)
                            Text("${workout.durationSeconds/60}m", color = Color.White) 
                        }
                        Column { 
                            Text(stringResource(R.string.volume_label), color = TextGray, fontSize = 12.sp)
                            Text("${workout.totalVolume.toInt()} kg", color = Color.White) 
                        } 
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    workout.exercises.take(3).forEach { ex -> 
                        Text(stringResource(R.string.sets_format, ex.sets.count { it.isCompleted }, ex.name), color = TextGray, fontSize = 14.sp) 
                    } 
                    if (workout.exercises.size > 3) {
                        Text(stringResource(R.string.more_exercises_format, workout.exercises.size - 3), color = themeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    }
                } 
            } 
        }
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }

    if (showSettings) {
        com.ateszk0.ostromgep.ui.components.SettingsDialog(viewModel, themeColor) { showSettings = false }
    }
    }
}

@Composable
fun DashboardButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.height(64.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = SurfaceDark)) { 
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) { 
            Icon(icon, null, tint = Color.White)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.Bold) 
        } 
    }
}
